#!/usr/bin/env bash
#
# extract_installer.sh — Extract the VAIO Remote Play Installer
#
# Attempts multiple extraction methods on the Remote Play Installer.exe,
# then runs binary_analyzer.py on all extracted DLLs/EXEs.
#
# Usage: ./extract_installer.sh [installer_path] [output_dir]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_INSTALLER="/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/files/Remote Play Installer.exe"
DEFAULT_OUTPUT="/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/extracted"

INSTALLER="${1:-$DEFAULT_INSTALLER}"
OUTPUT_DIR="${2:-$DEFAULT_OUTPUT}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    cat <<'HELP'
extract_installer.sh — Extract the VAIO Remote Play Installer

Usage: ./extract_installer.sh [installer_path] [output_dir]

Defaults:
  installer: .../cualquiercosa327-Remote-Play/files/Remote Play Installer.exe
  output:    .../cualquiercosa327-Remote-Play/extracted/

Attempts extraction with (in order):
  1. 7z (7-Zip) — handles most installer formats
  2. innoextract — for Inno Setup installers
  3. cabextract — for Microsoft CAB files
  4. unzip — sometimes works on self-extracting archives
  5. binwalk — firmware/binary extraction as fallback

After extraction, runs binary_analyzer.py on all extracted DLLs/EXEs.
HELP
    exit 0
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

if [[ ! -f "$INSTALLER" ]]; then
    echo -e "${RED}ERROR: Installer not found: $INSTALLER${NC}" >&2
    exit 1
fi

echo -e "${YELLOW}=== VAIO Remote Play Installer Extraction ===${NC}"
echo -e "Installer: $INSTALLER"
echo -e "Output:    $OUTPUT_DIR"
echo -e "Size:      $(du -h "$INSTALLER" | cut -f1)"
echo ""

mkdir -p "$OUTPUT_DIR"

# Track what worked
extracted=false

install_if_missing() {
    local tool="$1"
    local brew_name="${2:-$1}"
    if ! command -v "$tool" &>/dev/null; then
        echo -e "${CYAN}Installing $tool via brew...${NC}"
        if command -v brew &>/dev/null; then
            brew install "$brew_name" 2>/dev/null || {
                echo -e "${YELLOW}WARNING: Could not install $tool via brew${NC}"
                return 1
            }
        else
            echo -e "${YELLOW}WARNING: brew not available, cannot install $tool${NC}"
            return 1
        fi
    fi
    return 0
}

# Method 1: 7z
echo -e "${CYAN}--- Method 1: 7z ---${NC}"
if install_if_missing 7z p7zip; then
    out_7z="$OUTPUT_DIR/7z_extract"
    mkdir -p "$out_7z"
    if 7z x -o"$out_7z" -y "$INSTALLER" &>/dev/null; then
        count=$(find "$out_7z" -type f | wc -l | tr -d ' ')
        echo -e "${GREEN}SUCCESS: 7z extracted $count files${NC}"
        extracted=true

        # Try recursive extraction on any nested archives
        find "$out_7z" -type f \( -name '*.cab' -o -name '*.msi' -o -name '*.zip' \) | while read -r nested; do
            nested_dir="${nested%.???}_extracted"
            mkdir -p "$nested_dir"
            echo -e "  ${CYAN}Extracting nested: $(basename "$nested")${NC}"
            7z x -o"$nested_dir" -y "$nested" &>/dev/null || true
        done
    else
        echo -e "${YELLOW}7z extraction failed or empty${NC}"
        rmdir "$out_7z" 2>/dev/null || true
    fi
else
    echo -e "${YELLOW}Skipping 7z (not available)${NC}"
fi

# Method 2: innoextract
echo -e "\n${CYAN}--- Method 2: innoextract ---${NC}"
if install_if_missing innoextract; then
    out_inno="$OUTPUT_DIR/inno_extract"
    mkdir -p "$out_inno"
    if innoextract -d "$out_inno" "$INSTALLER" 2>/dev/null; then
        count=$(find "$out_inno" -type f | wc -l | tr -d ' ')
        echo -e "${GREEN}SUCCESS: innoextract extracted $count files${NC}"
        extracted=true
    else
        echo -e "${YELLOW}Not an Inno Setup installer${NC}"
        rmdir "$out_inno" 2>/dev/null || true
    fi
else
    echo -e "${YELLOW}Skipping innoextract (not available)${NC}"
fi

# Method 3: cabextract
echo -e "\n${CYAN}--- Method 3: cabextract ---${NC}"
if install_if_missing cabextract; then
    out_cab="$OUTPUT_DIR/cab_extract"
    mkdir -p "$out_cab"
    if cabextract -d "$out_cab" "$INSTALLER" 2>/dev/null; then
        count=$(find "$out_cab" -type f | wc -l | tr -d ' ')
        echo -e "${GREEN}SUCCESS: cabextract extracted $count files${NC}"
        extracted=true
    else
        echo -e "${YELLOW}Not a CAB archive${NC}"
        rmdir "$out_cab" 2>/dev/null || true
    fi
else
    echo -e "${YELLOW}Skipping cabextract (not available)${NC}"
fi

# Method 4: unzip
echo -e "\n${CYAN}--- Method 4: unzip ---${NC}"
out_zip="$OUTPUT_DIR/zip_extract"
mkdir -p "$out_zip"
if unzip -o -d "$out_zip" "$INSTALLER" 2>/dev/null; then
    count=$(find "$out_zip" -type f | wc -l | tr -d ' ')
    echo -e "${GREEN}SUCCESS: unzip extracted $count files${NC}"
    extracted=true
else
    echo -e "${YELLOW}Not a ZIP archive${NC}"
    rmdir "$out_zip" 2>/dev/null || true
fi

# Method 5: binwalk
echo -e "\n${CYAN}--- Method 5: binwalk ---${NC}"
if install_if_missing binwalk; then
    out_bw="$OUTPUT_DIR/binwalk_extract"
    mkdir -p "$out_bw"
    if binwalk -e -C "$out_bw" "$INSTALLER" 2>/dev/null; then
        count=$(find "$out_bw" -type f | wc -l | tr -d ' ')
        if [[ "$count" -gt 0 ]]; then
            echo -e "${GREEN}SUCCESS: binwalk extracted $count files${NC}"
            extracted=true
        else
            echo -e "${YELLOW}binwalk found nothing extractable${NC}"
            rmdir "$out_bw" 2>/dev/null || true
        fi
    else
        echo -e "${YELLOW}binwalk extraction failed${NC}"
        rmdir "$out_bw" 2>/dev/null || true
    fi
else
    echo -e "${YELLOW}Skipping binwalk (not available)${NC}"
fi

echo ""

if [[ "$extracted" == false ]]; then
    echo -e "${RED}No extraction method succeeded.${NC}"
    echo -e "You may need to install extraction tools: brew install p7zip innoextract cabextract binwalk"
    exit 1
fi

# List all extracted files
echo -e "${YELLOW}=== Extracted Files ===${NC}"
find "$OUTPUT_DIR" -type f | sort | while read -r f; do
    size=$(du -h "$f" 2>/dev/null | cut -f1)
    echo "  $size  $(basename "$f")  [$f]"
done

# Run binary_analyzer.py on extracted DLLs/EXEs
echo ""
echo -e "${YELLOW}=== Running binary_analyzer.py on extracted binaries ===${NC}"

ANALYZER="$SCRIPT_DIR/binary_analyzer.py"
if [[ -f "$ANALYZER" ]]; then
    report_json="$OUTPUT_DIR/binary_analysis_report.json"
    python3 "$ANALYZER" "$OUTPUT_DIR" -o "$report_json" || {
        echo -e "${YELLOW}WARNING: binary_analyzer.py had errors (partial results may exist)${NC}"
    }
    if [[ -f "$report_json" ]]; then
        echo -e "\n${GREEN}Binary analysis report: $report_json${NC}"
    fi
else
    echo -e "${RED}binary_analyzer.py not found at: $ANALYZER${NC}"
fi

# Also analyze the original files in the same directory
echo ""
echo -e "${YELLOW}=== Analyzing original repo binaries ===${NC}"
orig_dir="$(dirname "$INSTALLER")"
for binfile in "$orig_dir"/*.dll "$orig_dir"/*.exe; do
    if [[ -f "$binfile" ]]; then
        echo -e "\n${CYAN}$(basename "$binfile"):${NC}"
        python3 "$ANALYZER" "$binfile" -q 2>/dev/null || true
    fi
done

echo -e "\n${GREEN}Done. Extracted files in: $OUTPUT_DIR${NC}"
