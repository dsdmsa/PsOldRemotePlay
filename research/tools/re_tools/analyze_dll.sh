#!/usr/bin/env bash
# analyze_dll.sh - Analyze Windows DLLs (extracted from VAIO Remote Play installer)
#
# Usage: ./analyze_dll.sh <dll_file> [output_dir]
#
# Runs file identification, string extraction, crypto symbol search,
# import/export table extraction, and known byte pattern search.
# Uses radare2 for x86/x64 disassembly if available.
#
# Requires: file, strings, xxd, r2 (radare2)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_OUTPUT_DIR="$SCRIPT_DIR/output"

# Known IV base bytes
IV_BASE_PSP="3a9df33b99cf9c0d"
IV_BASE_PHONE="290de907e23be2fc"
IV_BASE_PC="5b6440c52e74c046"

# Known crypto constants
SKEY0="c99dacf2e9714b3f"
SKEY2="0d2a2a2f2e2c2d26"

# Known registration XOR keys
REG_XOR_PSP="6da73e0e536cff92"
REG_XOR_PHONE="73e8dba2b0bb2d0c"
REG_XOR_PC="2d6b67d7c1e069f0"

usage() {
    echo "Usage: $0 <dll_file> [output_dir]"
    echo ""
    echo "Analyze a Windows DLL binary for crypto operations related to PS3 Remote Play."
    echo ""
    echo "Arguments:"
    echo "  dll_file    Path to the Windows DLL/EXE to analyze"
    echo "  output_dir  Directory for output files (default: $DEFAULT_OUTPUT_DIR)"
    echo ""
    echo "Examples:"
    echo "  $0 RemotePlay.dll"
    echo "  $0 /path/to/premo.dll ./my_output"
    exit 1
}

if [[ $# -lt 1 ]]; then
    usage
fi

DLL_FILE="$1"
OUTPUT_DIR="${2:-$DEFAULT_OUTPUT_DIR}"
BASENAME="$(basename "$DLL_FILE" | sed 's/\.[^.]*$//')"
OUT_PREFIX="$OUTPUT_DIR/${BASENAME}"

if [[ ! -f "$DLL_FILE" ]]; then
    echo "ERROR: File not found: $DLL_FILE"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo "=== Analyzing Windows DLL: $DLL_FILE ==="
echo "Output prefix: $OUT_PREFIX"
echo ""

# 1. File identification
echo "[1/7] File identification..."
file "$DLL_FILE" | tee "$OUT_PREFIX"_file_id.txt
echo ""

# 2. PE header via xxd
echo "[2/7] PE header (first 512 bytes)..."
xxd -l 512 "$DLL_FILE" > "$OUT_PREFIX"_pe_header.txt
echo "  Saved to ${OUT_PREFIX}_pe_header.txt"
# Show MZ signature and PE offset
echo "  MZ signature:"
xxd -l 2 "$DLL_FILE" | sed 's/^/    /'
echo ""

# 3. Strings extraction
echo "[3/7] Extracting strings..."
strings -a "$DLL_FILE" > "$OUT_PREFIX"_strings.txt
NSTRINGS=$(wc -l < "$OUT_PREFIX"_strings.txt)
echo "  Found $NSTRINGS strings"

echo "  Crypto/SSL-related strings:"
grep -iE 'aes|cbc|encrypt|decrypt|iv|key|xor|openssl|ssl|EVP_|AES_|SHA|hmac|cipher|block' \
    "$OUT_PREFIX"_strings.txt | sort -u | head -40 | sed 's/^/    /' || echo "    (none found)"

echo ""
echo "  Registration/session strings:"
grep -iE 'register|regist|premo|session|auth|nonce|remote.?play|sce/' \
    "$OUT_PREFIX"_strings.txt | sort -u | head -20 | sed 's/^/    /' || echo "    (none found)"
echo ""

# 4. Search for known byte patterns
echo "[4/7] Searching for known crypto constants..."
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

search_hex_pattern "IV_BASE_PSP" "$IV_BASE_PSP" "$DLL_FILE"
search_hex_pattern "IV_BASE_PHONE" "$IV_BASE_PHONE" "$DLL_FILE"
search_hex_pattern "IV_BASE_PC" "$IV_BASE_PC" "$DLL_FILE"
search_hex_pattern "SKEY0" "$SKEY0" "$DLL_FILE"
search_hex_pattern "SKEY2" "$SKEY2" "$DLL_FILE"
search_hex_pattern "REG_XOR_PSP" "$REG_XOR_PSP" "$DLL_FILE"
search_hex_pattern "REG_XOR_PHONE" "$REG_XOR_PHONE" "$DLL_FILE"
search_hex_pattern "REG_XOR_PC" "$REG_XOR_PC" "$DLL_FILE"

# Also search little-endian (reversed byte order) for x86
echo ""
echo "  Searching little-endian byte order..."
reverse_hex() {
    local hex="$1"
    local result=""
    for ((i=${#hex}-2; i>=0; i-=2)); do
        result="$result${hex:$i:2}"
    done
    echo "$result"
}
search_hex_pattern "IV_BASE_PSP (LE)" "$(reverse_hex "$IV_BASE_PSP")" "$DLL_FILE"
search_hex_pattern "IV_BASE_PHONE (LE)" "$(reverse_hex "$IV_BASE_PHONE")" "$DLL_FILE"
search_hex_pattern "IV_BASE_PC (LE)" "$(reverse_hex "$IV_BASE_PC")" "$DLL_FILE"
echo ""

# 5. radare2 analysis
if command -v r2 &>/dev/null; then
    echo "[5/7] radare2 analysis..."

    # Binary info
    echo "  Binary info:"
    r2 -q -e bin.cache=true -c "iI" "$DLL_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_info.txt || true
    cat "$OUT_PREFIX"_r2_info.txt | sed 's/^/    /'

    # Sections
    echo ""
    echo "  Sections:"
    r2 -q -e bin.cache=true -c "iS" "$DLL_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_sections.txt || true
    cat "$OUT_PREFIX"_r2_sections.txt | sed 's/^/    /'

    # Imports - focus on crypto
    echo ""
    echo "  All imports:"
    r2 -q -e bin.cache=true -c "ii" "$DLL_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_imports.txt || true
    NIMPORTS=$(wc -l < "$OUT_PREFIX"_r2_imports.txt)
    echo "  Total: $NIMPORTS imports"
    echo "  Crypto-related imports:"
    grep -iE 'aes|cbc|encrypt|decrypt|EVP|SSL|cipher|hash|sha|hmac|crypt' \
        "$OUT_PREFIX"_r2_imports.txt | sed 's/^/    /' || echo "    (none found)"

    # Exports
    echo ""
    echo "  Exports:"
    r2 -q -e bin.cache=true -c "iE" "$DLL_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_exports.txt || true
    head -30 "$OUT_PREFIX"_r2_exports.txt | sed 's/^/    /'

    # Cross-references to crypto constants (if found)
    echo ""
    echo "[6/7] Searching for XOR instructions near crypto constants..."
    # Full analysis + xref search
    r2 -q -e bin.cache=true -c "aaa; /x 3a9df33b99cf9c0d; /x 290de907e23be2fc; /x 5b6440c52e74c046" \
        "$DLL_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_pattern_search.txt || true
    cat "$OUT_PREFIX"_r2_pattern_search.txt | sed 's/^/    /'

    # If patterns found, disassemble around them
    if [[ -s "$OUT_PREFIX"_r2_pattern_search.txt ]]; then
        echo ""
        echo "  Disassembling around found patterns..."
        while IFS= read -r line; do
            if [[ "$line" =~ 0x([0-9a-fA-F]+) ]]; then
                addr="0x${BASH_REMATCH[1]}"
                echo ""
                echo "  === Around $addr ==="
                r2 -q -e bin.cache=true -c "s $addr; pd -10; pd 20" "$DLL_FILE" 2>/dev/null | sed 's/^/    /'
            fi
        done < "$OUT_PREFIX"_r2_pattern_search.txt
    fi

    echo ""
    echo "[7/7] Searching for XOR instructions in .text section..."
    r2 -q -e bin.cache=true -c 'aaa; /ad xor' "$DLL_FILE" 2>/dev/null > "$OUT_PREFIX"_r2_xor_search.txt || true
    NXOR=$(wc -l < "$OUT_PREFIX"_r2_xor_search.txt)
    echo "  Found $NXOR XOR instruction references (saved to ${OUT_PREFIX}_r2_xor_search.txt)"
else
    echo "[5/7] radare2 not available - skipping advanced analysis"
    echo "[6/7] Skipped"
    echo "[7/7] Skipped"
fi

echo ""
echo "=== Analysis complete. Output files saved to: $OUTPUT_DIR ==="
ls -la "$OUT_PREFIX"_* 2>/dev/null | sed 's/^/  /'
