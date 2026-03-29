#!/usr/bin/env bash
#
# find_registration_code.sh — Search all repos for registration protocol patterns
#
# Searches for endpoints, keywords, HTTP methods, and error codes related to
# PS3/PS4 Remote Play device registration.
#
# Usage: ./find_registration_code.sh [repos_dir]

set -euo pipefail

REPOS_DIR="${1:-/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    cat <<'HELP'
find_registration_code.sh — Search repos for registration protocol patterns

Usage: ./find_registration_code.sh [repos_dir]

Searches for:
  - Endpoints: /sce/premo/regist, /sie/ps4/rp/sess/rgst, etc.
  - Keywords: regist, registration, PIN, pin_code, iv_base, iv_context, param_1
  - HTTP methods: POST.*regist, GET.*regist
  - Error codes: 80029820, 80029830, 80029813, etc.
  - Registration flow: Client-Type, PREMO-Regist, key derivation

Results grouped by repo.
HELP
    exit 0
fi

if [[ ! -d "$REPOS_DIR" ]]; then
    echo "ERROR: Repos directory not found: $REPOS_DIR" >&2
    exit 1
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Categories and their grep patterns
declare -A CATEGORIES

# Endpoints
ENDPOINTS=(
    "/sce/premo/regist"
    "/sce/premo/session"
    "/sie/ps4/rp/sess/rgst"
    "/sie/ps4/rp/sess/init"
    "premo/regist"
    "rp/sess/rgst"
)

# Keywords
KEYWORDS=(
    "regist"
    "registration"
    "pin_code"
    "pincode"
    "iv_base"
    "iv_context"
    "param_1"
    "Client-Type"
    "PREMO-Regist"
    "PREMO-RegistKey"
    "regist_key"
    "RegistKey"
    "device_regist"
    "DeviceRegist"
)

# Error codes
ERROR_CODES=(
    "80029820"
    "80029830"
    "80029813"
    "80029519"
    "80029516"
    "80029511"
)

# HTTP patterns
HTTP_PATTERNS=(
    "POST.*regist"
    "GET.*regist"
    "POST.*rgst"
    "GET.*rgst"
)

# Key derivation patterns
KEY_DERIVATION=(
    "derive.*key"
    "key.*deriv"
    "xor.*key"
    "key.*xor"
    "km\[.*\].*xor"
    "REG_XOR"
    "REG_IV"
    "reg_key"
    "reg_iv"
)

search_pattern_group() {
    local group_name="$1"
    shift
    local patterns=("$@")
    local repo_dir="$1_unused"
    local hits=0

    echo -e "\n${MAGENTA}--- $group_name ---${NC}"

    for pattern in "${patterns[@]}"; do
        local results
        results=$(grep -rn --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                       --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                       --include='*.ts' --include='*.go' --include='*.rs' --include='*.swift' \
                       --include='*.m' --include='*.md' --include='*.txt' --include='*.json' \
                       --include='*.xml' --include='*.rb' --include='*.hpp' \
                       -i "$pattern" "$current_repo" 2>/dev/null || true)
        if [[ -n "$results" ]]; then
            echo -e "  ${CYAN}Pattern: ${NC}${pattern}"
            echo "$results" | head -20 | while IFS= read -r line; do
                echo -e "    ${GREEN}${line}${NC}"
                ((hits++)) || true
            done
            local count
            count=$(echo "$results" | wc -l | tr -d ' ')
            if [[ "$count" -gt 20 ]]; then
                echo -e "    ${YELLOW}... and $((count - 20)) more matches${NC}"
            fi
        fi
    done
}

echo -e "${YELLOW}=== PS3/PS4 Remote Play Registration Code Search ===${NC}"
echo -e "Repos directory: $REPOS_DIR"
echo ""

total_hits=0

# Iterate over each repo
for current_repo in "$REPOS_DIR"/*/; do
    repo_name=$(basename "$current_repo")

    # Quick check: does this repo have any registration-related content at all?
    quick_check=$(grep -rl -i "regist\|pin_code\|premo\|remote.play\|80029820" \
                  --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                  --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                  --include='*.ts' --include='*.go' --include='*.md' --include='*.txt' \
                  "$current_repo" 2>/dev/null | head -1 || true)

    if [[ -z "$quick_check" ]]; then
        continue
    fi

    echo -e "\n${YELLOW}${'='*60}${NC}" 2>/dev/null || echo -e "\n${YELLOW}============================================================${NC}"
    echo -e "${YELLOW}Repo: $repo_name${NC}"
    echo -e "${YELLOW}============================================================${NC}"

    # Search each category
    echo -e "\n${MAGENTA}--- ENDPOINTS ---${NC}"
    for pattern in "${ENDPOINTS[@]}"; do
        results=$(grep -rn --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                       --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                       --include='*.ts' --include='*.go' --include='*.rs' --include='*.md' \
                       --include='*.txt' --include='*.json' \
                       "$pattern" "$current_repo" 2>/dev/null || true)
        if [[ -n "$results" ]]; then
            echo -e "  ${CYAN}Pattern: ${NC}${pattern}"
            echo "$results" | head -10 | sed 's/^/    /'
            count=$(echo "$results" | wc -l | tr -d ' ')
            [[ "$count" -gt 10 ]] && echo "    ... and $((count - 10)) more"
            total_hits=$((total_hits + count))
        fi
    done

    echo -e "\n${MAGENTA}--- KEYWORDS ---${NC}"
    for pattern in "${KEYWORDS[@]}"; do
        results=$(grep -rn --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                       --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                       --include='*.ts' --include='*.go' --include='*.rs' \
                       -i "$pattern" "$current_repo" 2>/dev/null || true)
        if [[ -n "$results" ]]; then
            count=$(echo "$results" | wc -l | tr -d ' ')
            echo -e "  ${CYAN}$pattern${NC} ($count matches)"
            echo "$results" | head -5 | sed 's/^/    /'
            [[ "$count" -gt 5 ]] && echo "    ... and $((count - 5)) more"
            total_hits=$((total_hits + count))
        fi
    done

    echo -e "\n${MAGENTA}--- ERROR CODES ---${NC}"
    for pattern in "${ERROR_CODES[@]}"; do
        results=$(grep -rn "$pattern" "$current_repo" 2>/dev/null || true)
        if [[ -n "$results" ]]; then
            echo -e "  ${CYAN}$pattern${NC}"
            echo "$results" | head -10 | sed 's/^/    /'
            count=$(echo "$results" | wc -l | tr -d ' ')
            total_hits=$((total_hits + count))
        fi
    done

    echo -e "\n${MAGENTA}--- HTTP PATTERNS ---${NC}"
    for pattern in "${HTTP_PATTERNS[@]}"; do
        results=$(grep -rn --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                       --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                       -iE "$pattern" "$current_repo" 2>/dev/null || true)
        if [[ -n "$results" ]]; then
            echo -e "  ${CYAN}$pattern${NC}"
            echo "$results" | head -10 | sed 's/^/    /'
            count=$(echo "$results" | wc -l | tr -d ' ')
            total_hits=$((total_hits + count))
        fi
    done

    echo -e "\n${MAGENTA}--- KEY DERIVATION ---${NC}"
    for pattern in "${KEY_DERIVATION[@]}"; do
        results=$(grep -rn --include='*.py' --include='*.c' --include='*.cpp' --include='*.h' \
                       --include='*.java' --include='*.kt' --include='*.cs' --include='*.js' \
                       -iE "$pattern" "$current_repo" 2>/dev/null || true)
        if [[ -n "$results" ]]; then
            echo -e "  ${CYAN}$pattern${NC}"
            echo "$results" | head -10 | sed 's/^/    /'
            count=$(echo "$results" | wc -l | tr -d ' ')
            total_hits=$((total_hits + count))
        fi
    done
done

echo -e "\n${YELLOW}============================================================${NC}"
echo -e "${YELLOW}Total hits across all repos: $total_hits${NC}"
