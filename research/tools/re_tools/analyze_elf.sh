#!/usr/bin/env bash
# analyze_elf.sh - Analyze PS3 ELF files (64-bit PPC Big-Endian)
#
# Usage: ./analyze_elf.sh <elf_file> [output_dir]
#
# Runs file identification, section extraction, symbol listing,
# string extraction, crypto constant search, and disassembly of
# known registration handler addresses.
#
# Requires: file, strings, xxd, r2 (radare2), objdump (optional)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_OUTPUT_DIR="$SCRIPT_DIR/output"

# Known registration handler addresses in sysconf_plugin.elf
REG_HANDLER_ADDRS=(0x100168 0x1004F4 0x100570 0x1005D8)

# Known IV base bytes for the 3 device platforms
IV_BASE_PSP="3a9df33b99cf9c0d"
IV_BASE_PHONE="290de907e23be2fc"
IV_BASE_PC="5b6440c52e74c046"

# Known session crypto keys
SKEY0="c99dacf2e9714b3f"
SKEY2="0d2a2a2f2e2c2d26"

usage() {
    echo "Usage: $0 <elf_file> [output_dir]"
    echo ""
    echo "Analyze a PS3 ELF binary for crypto operations and registration handler code."
    echo ""
    echo "Arguments:"
    echo "  elf_file    Path to the PS3 ELF file to analyze"
    echo "  output_dir  Directory for output files (default: $DEFAULT_OUTPUT_DIR)"
    echo ""
    echo "Examples:"
    echo "  $0 sysconf_plugin.elf"
    echo "  $0 /path/to/premo_plugin.elf ./my_output"
    exit 1
}

if [[ $# -lt 1 ]]; then
    usage
fi

ELF_FILE="$1"
OUTPUT_DIR="${2:-$DEFAULT_OUTPUT_DIR}"
BASENAME="$(basename "$ELF_FILE" .elf)"
OUT_PREFIX="$OUTPUT_DIR/${BASENAME}"

if [[ ! -f "$ELF_FILE" ]]; then
    echo "ERROR: File not found: $ELF_FILE"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo "=== Analyzing PS3 ELF: $ELF_FILE ==="
echo "Output prefix: $OUT_PREFIX"
echo ""

# 1. File identification
echo "[1/8] File identification..."
file "$ELF_FILE" | tee "$OUT_PREFIX"_file_id.txt
echo ""

# 2. ELF header details via xxd
echo "[2/8] ELF header (first 128 bytes)..."
xxd -l 128 "$ELF_FILE" | tee "$OUT_PREFIX"_elf_header.txt
echo ""

# 3. Strings extraction
echo "[3/8] Extracting strings..."
strings -a "$ELF_FILE" > "$OUT_PREFIX"_strings.txt
NSTRINGS=$(wc -l < "$OUT_PREFIX"_strings.txt)
echo "  Found $NSTRINGS strings (saved to ${OUT_PREFIX}_strings.txt)"

# Grep for interesting strings
echo "  Crypto-related strings:"
grep -iE 'aes|cbc|encrypt|decrypt|iv|key|xor|register|regist|premo|session|auth|nonce|hash|sha|hmac|ssl|openssl' \
    "$OUT_PREFIX"_strings.txt | head -30 | sed 's/^/    /' || echo "    (none found)"
echo ""

# 4. Symbol table (if present)
echo "[4/8] Symbol table..."
if command -v nm &>/dev/null; then
    nm "$ELF_FILE" 2>/dev/null > "$OUT_PREFIX"_symbols.txt || true
    NSYMS=$(wc -l < "$OUT_PREFIX"_symbols.txt)
    echo "  Found $NSYMS symbols"
    if [[ $NSYMS -gt 0 ]]; then
        echo "  Crypto-related symbols:"
        grep -iE 'aes|cbc|encrypt|decrypt|iv|key|xor|register|regist|premo|session|auth' \
            "$OUT_PREFIX"_symbols.txt | head -20 | sed 's/^/    /' || echo "    (none found)"
    fi
fi
echo ""

# 5. Search for known byte patterns (IV bases, session keys)
echo "[5/8] Searching for known crypto constants..."
search_hex_pattern() {
    local label="$1"
    local hex="$2"
    local file="$3"
    local result
    result=$(python3 -c "
import sys
data = open('$file', 'rb').read()
pat = bytes.fromhex('$hex')
i, found = 0, []
while True:
    idx = data.find(pat, i)
    if idx == -1: break
    found.append(idx)
    i = idx + 1
    if len(found) >= 5: break
for off in found:
    print(f'0x{off:X}')
" 2>/dev/null)
    if [[ -n "$result" ]]; then
        while IFS= read -r off; do
            printf "  %-20s found at file offset %s\n" "$label" "$off"
        done <<< "$result"
    else
        printf "  %-20s not found\n" "$label"
    fi
}

search_hex_pattern "IV_BASE_PSP" "$IV_BASE_PSP" "$ELF_FILE"
search_hex_pattern "IV_BASE_PHONE" "$IV_BASE_PHONE" "$ELF_FILE"
search_hex_pattern "IV_BASE_PC" "$IV_BASE_PC" "$ELF_FILE"
search_hex_pattern "SKEY0" "$SKEY0" "$ELF_FILE"
search_hex_pattern "SKEY2" "$SKEY2" "$ELF_FILE"
echo ""

# 6. radare2 analysis (if available)
if command -v r2 &>/dev/null; then
    echo "[6/8] radare2 analysis..."

    # Basic info
    echo "  Getting binary info..."
    r2 -q -e bin.cache=true -c "iI" "$ELF_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_info.txt || true
    cat "$OUT_PREFIX"_r2_info.txt | sed 's/^/    /'

    # Sections
    echo ""
    echo "  Sections:"
    r2 -q -e bin.cache=true -c "iS" "$ELF_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_sections.txt || true
    cat "$OUT_PREFIX"_r2_sections.txt | sed 's/^/    /'

    # Imports/Exports
    echo ""
    echo "  Imports:"
    r2 -q -e bin.cache=true -c "ii" "$ELF_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_imports.txt || true
    head -30 "$OUT_PREFIX"_r2_imports.txt | sed 's/^/    /'

    echo ""
    echo "  Exports:"
    r2 -q -e bin.cache=true -c "iE" "$ELF_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_exports.txt || true
    head -30 "$OUT_PREFIX"_r2_exports.txt | sed 's/^/    /'

    # Disassemble registration handler addresses
    echo ""
    echo "[7/8] Disassembling registration handler addresses..."
    for addr in "${REG_HANDLER_ADDRS[@]}"; do
        echo ""
        echo "  === Address $addr (64 instructions) ==="
        r2 -q -e bin.cache=true -e asm.arch=ppc -e asm.bits=64 -e cfg.bigendian=true \
            -c "s $addr; pd 64" "$ELF_FILE" 2>/dev/null | tee -a "$OUT_PREFIX"_r2_disasm.txt | sed 's/^/    /'
    done

    # Disassemble wider range around registration handler
    echo ""
    echo "[8/8] Full registration handler region (0x100000-0x101000)..."
    r2 -q -e bin.cache=true -e asm.arch=ppc -e asm.bits=64 -e cfg.bigendian=true \
        -c "s 0x100000; pd 512" "$ELF_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_reghandler_full.txt || true
    NLINES=$(wc -l < "$OUT_PREFIX"_r2_reghandler_full.txt)
    echo "  Saved $NLINES lines to ${OUT_PREFIX}_r2_reghandler_full.txt"

    # Search for XOR instructions in the full disassembly
    echo ""
    echo "  XOR instructions in registration handler:"
    grep -i 'xor' "$OUT_PREFIX"_r2_reghandler_full.txt | sed 's/^/    /' || echo "    (none found)"

else
    echo "[6/8] radare2 not available - skipping disassembly"
    echo "[7/8] Skipped"
    echo "[8/8] Skipped"
fi

echo ""
echo "=== Analysis complete. Output files saved to: $OUTPUT_DIR ==="
ls -la "$OUT_PREFIX"_* 2>/dev/null | sed 's/^/  /'
