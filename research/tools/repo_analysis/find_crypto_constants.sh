#!/usr/bin/env bash
#
# find_crypto_constants.sh — Search all repos for known PS3 Remote Play crypto byte patterns
#
# Searches both text files (hex string patterns) and binary files (raw bytes)
# using xxd + grep for binary search and regular grep for text.
#
# Usage: ./find_crypto_constants.sh [repos_dir]
#   repos_dir defaults to /Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos

set -euo pipefail

REPOS_DIR="${1:-/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos}"

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    cat <<'HELP'
find_crypto_constants.sh — Search repos for PS3 Remote Play crypto constants

Usage: ./find_crypto_constants.sh [repos_dir]

Searches for:
  - Registration XOR keys (PSP, Phone, PC)
  - Registration IV bases (PSP, Phone, PC)
  - Session keys (SKEY0, SKEY1, SKEY2)
  - Nonce XOR keys (Phone, PC, Vita)
  - AES S-box prefix (crypto code locator)

Both text (hex strings in various formats) and binary (raw bytes via xxd) searches.
HELP
    exit 0
fi

if [[ ! -d "$REPOS_DIR" ]]; then
    echo "ERROR: Repos directory not found: $REPOS_DIR" >&2
    exit 1
fi

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Define crypto constants: name, hex bytes (space-separated)
declare -A CRYPTO_CONSTANTS
CRYPTO_CONSTANTS=(
    # Registration XOR keys
    ["REG_XOR_PSP"]="FD C3 F6 A6 4D 2A BA 7A 38 92 6C BC 34 31 E1 0E"
    ["REG_XOR_PHONE"]="F1 16 F0 DA 44 2C 06 C2 45 B1 5E 48 F9 04 E3 E6"
    ["REG_XOR_PC"]="EC 6D 70 6B 1E 0A 9A 75 8C DA 78 27 51 A3 C3 7B"
    # Registration IV bases
    ["REG_IV_PSP"]="3A 9D F3 3B 99 CF 9C 0D BF 58 81 12 6C 18 32 64"
    ["REG_IV_PHONE"]="29 0D E9 07 E2 3B E2 FC 34 08 CA 4B DE E4 AF 3A"
    ["REG_IV_PC"]="5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04"
    # Session keys
    ["SKEY0"]="D1 B2 12 EB 73 86 6C 7B 12 A7 5E 0C 04 C6 B8 91"
    ["SKEY1"]="1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F"
    ["SKEY2"]="65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59"
    # Nonce XOR keys
    ["NONCE_XOR_PHONE"]="37 63 E5 4D 12 F9 7B 73 62 3A D3 0D 10 C9 91 7E"
    ["NONCE_XOR_PC"]="33 72 84 4C A6 6F 2E 2B 20 C7 90 60 33 E8 29 C6"
    ["NONCE_XOR_VITA"]="AF 74 B5 4F 38 F8 AF C8 75 77 B2 D5 47 76 3B FD"
    # AES S-box prefix (locates AES implementations)
    ["AES_SBOX"]="63 7C 77 7B F2 6B 6F C5"
)

total_hits=0

search_text_pattern() {
    local name="$1"
    local hex_bytes="$2"
    local hits=0

    # Generate multiple text search patterns
    local no_space="${hex_bytes// /}"                          # FDC3F6A6...
    local lower_no_space=$(echo "$no_space" | tr 'A-F' 'a-f') # fdc3f6a6...
    local comma_sep=$(echo "$hex_bytes" | sed 's/ /, 0x/g; s/^/0x/') # 0xFD, 0xC3...
    local first4="${hex_bytes:0:11}"                           # First 4 bytes with spaces

    echo -e "  ${CYAN}[TEXT]${NC} Searching for $name..."

    # Search with multiple patterns using grep -rn
    for pattern in \
        "$no_space" \
        "$lower_no_space" \
        "$(echo "$hex_bytes" | tr 'A-F' 'a-f')" \
        "$hex_bytes" \
        "$comma_sep" \
        "$(echo "$comma_sep" | tr 'A-F' 'a-f')"; do
        while IFS= read -r line; do
            if [[ -n "$line" ]]; then
                echo -e "    ${GREEN}HIT${NC} $line"
                ((hits++)) || true
            fi
        done < <(grep -rn --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                      --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                      --include='*.ts' --include='*.rb' --include='*.go' --include='*.rs' \
                      --include='*.md' --include='*.txt' --include='*.json' --include='*.xml' \
                      --include='*.swift' --include='*.m' \
                      -l "$pattern" "$REPOS_DIR" 2>/dev/null || true)
    done

    # De-duplicate file-level hits would be nice but keeping it simple
    return $hits
}

search_binary_pattern() {
    local name="$1"
    local hex_bytes="$2"
    local hits=0

    # Convert to raw hex for xxd grep
    local raw_hex=$(echo "$hex_bytes" | tr -d ' ' | tr 'A-F' 'a-f')
    # Take first 8 bytes (16 hex chars) for binary search — enough to be unique
    local search_hex="${raw_hex:0:16}"

    echo -e "  ${CYAN}[BINARY]${NC} Searching for $name (first 8 bytes: $search_hex)..."

    # Find binary files and search with xxd
    while IFS= read -r binfile; do
        local result
        result=$(xxd -p "$binfile" 2>/dev/null | tr -d '\n' | grep -ob "$search_hex" 2>/dev/null || true)
        if [[ -n "$result" ]]; then
            while IFS=: read -r char_offset _match; do
                # xxd -p gives 2 chars per byte, so byte offset = char_offset / 2
                local byte_offset=$(( char_offset / 2 ))
                echo -e "    ${GREEN}HIT${NC} $binfile @ offset 0x$(printf '%x' $byte_offset)"
                # Show context: 32 bytes around the match
                xxd -s $byte_offset -l 32 "$binfile" 2>/dev/null | head -2 | sed 's/^/         /'
                ((hits++)) || true
            done <<< "$result"
        fi
    done < <(find "$REPOS_DIR" \( -name '*.exe' -o -name '*.dll' -o -name '*.elf' -o -name '*.self' \
                -o -name '*.bin' -o -name '*.so' -o -name '*.dylib' -o -name '*.prx' \
                -o -name '*.sprx' -o -name '*.sys' \) -type f 2>/dev/null)

    return $hits
}

echo -e "${YELLOW}=== PS3 Remote Play Crypto Constants Search ===${NC}"
echo -e "Repos directory: $REPOS_DIR"
echo ""

for name in "${!CRYPTO_CONSTANTS[@]}"; do
    hex="${CRYPTO_CONSTANTS[$name]}"
    echo -e "${YELLOW}--- $name: $hex ---${NC}"

    search_text_pattern "$name" "$hex" || hits_t=$?
    search_binary_pattern "$name" "$hex" || hits_b=$?
    total_hits=$(( total_hits + ${hits_t:-0} + ${hits_b:-0} ))

    echo ""
done

echo -e "${YELLOW}=== Summary ===${NC}"
echo -e "Total hits: $total_hits"
echo -e "Repos searched: $(ls -d "$REPOS_DIR"/*/ 2>/dev/null | wc -l | tr -d ' ')"
