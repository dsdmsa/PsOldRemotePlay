#!/usr/bin/env bash
# run_all_analysis.sh - Master script to run all RE analysis on PS3 binaries
#
# Usage: ./run_all_analysis.sh [--elfs-dir <dir>] [--dlls-dir <dir>] [--output <dir>]
#
# Runs analyze_elf.sh on all PS3 ELFs, analyze_dll.sh on all Windows DLLs,
# and find_iv_xor.py on all binaries. Saves all output to the output directory.
#
# Defaults:
#   ELFs dir: /Users/mihailurmanschi/Work/PsOldRemotePlay/research/pupps3/decrypted_elfs
#   DLLs dir: (none - specify with --dlls-dir)
#   Output:   ./output/
#
# Requires: bash, python3, and tools used by sub-scripts

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_ELFS_DIR="/Users/mihailurmanschi/Work/PsOldRemotePlay/research/pupps3/decrypted_elfs"
DEFAULT_OUTPUT_DIR="$SCRIPT_DIR/output"

ELFS_DIR="$DEFAULT_ELFS_DIR"
DLLS_DIR=""
OUTPUT_DIR="$DEFAULT_OUTPUT_DIR"
SKIP_ELFS=false
SKIP_DLLS=false
SKIP_IV=false
PRIORITY_ELFS=("sysconf_plugin.elf" "premo_plugin.elf" "premo_game_plugin.elf" "libsysutil_remoteplay.elf")

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Master analysis script - runs all RE tools on PS3/Windows binaries."
    echo ""
    echo "Options:"
    echo "  --elfs-dir <dir>   Directory with PS3 ELF files (default: $DEFAULT_ELFS_DIR)"
    echo "  --dlls-dir <dir>   Directory with Windows DLL files (default: none)"
    echo "  --output <dir>     Output directory (default: $DEFAULT_OUTPUT_DIR)"
    echo "  --skip-elfs        Skip ELF analysis"
    echo "  --skip-dlls        Skip DLL analysis"
    echo "  --skip-iv          Skip IV XOR search"
    echo "  --priority-only    Only analyze priority ELFs (sysconf, premo, libsysutil)"
    echo "  -h, --help         Show this help"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Analyze all default ELFs"
    echo "  $0 --priority-only                    # Only priority ELFs"
    echo "  $0 --dlls-dir /path/to/vaio/dlls      # Also analyze Windows DLLs"
    echo "  $0 --skip-elfs --dlls-dir /path/dlls   # Only DLLs"
    exit 0
}

PRIORITY_ONLY=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --elfs-dir) ELFS_DIR="$2"; shift 2 ;;
        --dlls-dir) DLLS_DIR="$2"; shift 2 ;;
        --output) OUTPUT_DIR="$2"; shift 2 ;;
        --skip-elfs) SKIP_ELFS=true; shift ;;
        --skip-dlls) SKIP_DLLS=true; shift ;;
        --skip-iv) SKIP_IV=true; shift ;;
        --priority-only) PRIORITY_ONLY=true; shift ;;
        -h|--help) usage ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

mkdir -p "$OUTPUT_DIR"

TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
SUMMARY_FILE="$OUTPUT_DIR/analysis_summary_${TIMESTAMP}.txt"

echo "=============================================="
echo " PS3 Remote Play Binary Analysis Suite"
echo " $(date)"
echo "=============================================="
echo ""
echo "ELFs dir:  $ELFS_DIR"
echo "DLLs dir:  ${DLLS_DIR:-'(none)'}"
echo "Output:    $OUTPUT_DIR"
echo "Summary:   $SUMMARY_FILE"
echo ""

# Start summary
{
    echo "PS3 Remote Play Binary Analysis Summary"
    echo "Generated: $(date)"
    echo "=============================================="
    echo ""
} > "$SUMMARY_FILE"

TOTAL_PASS=0
TOTAL_FAIL=0

# ---- Phase 1: ELF Analysis ----
if [[ "$SKIP_ELFS" == false ]] && [[ -d "$ELFS_DIR" ]]; then
    echo "==== Phase 1: PS3 ELF Analysis ===="
    echo ""

    elf_files=()
    if [[ "$PRIORITY_ONLY" == true ]]; then
        for pelf in "${PRIORITY_ELFS[@]}"; do
            if [[ -f "$ELFS_DIR/$pelf" ]]; then
                elf_files+=("$ELFS_DIR/$pelf")
            fi
        done
    else
        while IFS= read -r -d '' f; do
            elf_files+=("$f")
        done < <(find "$ELFS_DIR" -name "*.elf" -print0 | sort -z)
    fi

    echo "Found ${#elf_files[@]} ELF files to analyze"
    echo ""

    {
        echo "PHASE 1: PS3 ELF Analysis"
        echo "  Source: $ELFS_DIR"
        echo "  Files: ${#elf_files[@]}"
        echo ""
    } >> "$SUMMARY_FILE"

    for elf in "${elf_files[@]}"; do
        basename_elf="$(basename "$elf")"
        echo "--- Analyzing: $basename_elf ---"
        elf_output="$OUTPUT_DIR/elf_${basename_elf%.elf}"
        mkdir -p "$elf_output" 2>/dev/null || true

        if "$SCRIPT_DIR/analyze_elf.sh" "$elf" "$elf_output" > "$elf_output/analysis.log" 2>&1; then
            echo "  OK - output in $elf_output/"
            echo "  [PASS] $basename_elf" >> "$SUMMARY_FILE"
            TOTAL_PASS=$((TOTAL_PASS + 1))
        else
            echo "  FAILED - see $elf_output/analysis.log"
            echo "  [FAIL] $basename_elf" >> "$SUMMARY_FILE"
            TOTAL_FAIL=$((TOTAL_FAIL + 1))
        fi
    done
    echo ""
else
    if [[ "$SKIP_ELFS" == true ]]; then
        echo "==== Phase 1: Skipped (--skip-elfs) ===="
    else
        echo "==== Phase 1: Skipped (ELFs dir not found: $ELFS_DIR) ===="
    fi
    echo "" >> "$SUMMARY_FILE"
fi

# ---- Phase 2: DLL Analysis ----
if [[ "$SKIP_DLLS" == false ]] && [[ -n "$DLLS_DIR" ]] && [[ -d "$DLLS_DIR" ]]; then
    echo "==== Phase 2: Windows DLL Analysis ===="
    echo ""

    dll_files=()
    while IFS= read -r -d '' f; do
        dll_files+=("$f")
    done < <(find "$DLLS_DIR" \( -name "*.dll" -o -name "*.exe" \) -print0 | sort -z)

    echo "Found ${#dll_files[@]} DLL/EXE files to analyze"
    echo ""

    {
        echo ""
        echo "PHASE 2: Windows DLL Analysis"
        echo "  Source: $DLLS_DIR"
        echo "  Files: ${#dll_files[@]}"
        echo ""
    } >> "$SUMMARY_FILE"

    for dll in "${dll_files[@]}"; do
        basename_dll="$(basename "$dll")"
        echo "--- Analyzing: $basename_dll ---"
        dll_output="$OUTPUT_DIR/dll_${basename_dll%.*}"
        mkdir -p "$dll_output" 2>/dev/null || true

        if "$SCRIPT_DIR/analyze_dll.sh" "$dll" "$dll_output" > "$dll_output/analysis.log" 2>&1; then
            echo "  OK - output in $dll_output/"
            echo "  [PASS] $basename_dll" >> "$SUMMARY_FILE"
            TOTAL_PASS=$((TOTAL_PASS + 1))
        else
            echo "  FAILED - see $dll_output/analysis.log"
            echo "  [FAIL] $basename_dll" >> "$SUMMARY_FILE"
            TOTAL_FAIL=$((TOTAL_FAIL + 1))
        fi
    done
    echo ""
else
    if [[ "$SKIP_DLLS" == true ]]; then
        echo "==== Phase 2: Skipped (--skip-dlls) ===="
    elif [[ -z "$DLLS_DIR" ]]; then
        echo "==== Phase 2: Skipped (no --dlls-dir specified) ===="
    else
        echo "==== Phase 2: Skipped (DLLs dir not found: $DLLS_DIR) ===="
    fi
fi

# ---- Phase 3: IV XOR Search ----
if [[ "$SKIP_IV" == false ]]; then
    echo "==== Phase 3: IV XOR Pattern Search ===="
    echo ""

    {
        echo ""
        echo "PHASE 3: IV XOR Pattern Search"
        echo ""
    } >> "$SUMMARY_FILE"

    # Run on ELFs
    if [[ -d "$ELFS_DIR" ]]; then
        elf_files_iv=()
        if [[ "$PRIORITY_ONLY" == true ]]; then
            for pelf in "${PRIORITY_ELFS[@]}"; do
                if [[ -f "$ELFS_DIR/$pelf" ]]; then
                    elf_files_iv+=("$ELFS_DIR/$pelf")
                fi
            done
        else
            while IFS= read -r -d '' f; do
                elf_files_iv+=("$f")
            done < <(find "$ELFS_DIR" -name "*.elf" -print0 | sort -z)
        fi

        for elf in "${elf_files_iv[@]}"; do
            basename_elf="$(basename "$elf")"
            echo "--- IV XOR search: $basename_elf ---"
            iv_out="$OUTPUT_DIR/iv_xor_${basename_elf%.elf}.txt"
            if python3 "$SCRIPT_DIR/find_iv_xor.py" "$elf" --arch ppc --verbose > "$iv_out" 2>&1; then
                echo "  OK - output in $iv_out"
                # Check if any IV bases were found
                if grep -q "\[FOUND\]" "$iv_out"; then
                    echo "  *** IV BASE FOUND in $basename_elf ***"
                    echo "  [IV FOUND] $basename_elf" >> "$SUMMARY_FILE"
                else
                    echo "  [IV NOT FOUND] $basename_elf" >> "$SUMMARY_FILE"
                fi
            else
                echo "  FAILED"
                echo "  [IV FAIL] $basename_elf" >> "$SUMMARY_FILE"
            fi
        done
    fi

    # Run on DLLs
    if [[ -n "$DLLS_DIR" ]] && [[ -d "$DLLS_DIR" ]]; then
        while IFS= read -r -d '' dll; do
            basename_dll="$(basename "$dll")"
            echo "--- IV XOR search: $basename_dll ---"
            iv_out="$OUTPUT_DIR/iv_xor_${basename_dll%.*}.txt"
            if python3 "$SCRIPT_DIR/find_iv_xor.py" "$dll" --arch x86 --verbose > "$iv_out" 2>&1; then
                echo "  OK - output in $iv_out"
                if grep -q "\[FOUND\]" "$iv_out"; then
                    echo "  *** IV BASE FOUND in $basename_dll ***"
                    echo "  [IV FOUND] $basename_dll" >> "$SUMMARY_FILE"
                else
                    echo "  [IV NOT FOUND] $basename_dll" >> "$SUMMARY_FILE"
                fi
            else
                echo "  FAILED"
                echo "  [IV FAIL] $basename_dll" >> "$SUMMARY_FILE"
            fi
        done < <(find "$DLLS_DIR" \( -name "*.dll" -o -name "*.exe" \) -print0 | sort -z)
    fi
    echo ""
else
    echo "==== Phase 3: Skipped (--skip-iv) ===="
fi

# ---- Final Summary ----
{
    echo ""
    echo "=============================================="
    echo "TOTALS: $TOTAL_PASS passed, $TOTAL_FAIL failed"
    echo "=============================================="
} >> "$SUMMARY_FILE"

echo "=============================================="
echo " Analysis Complete"
echo " Passed: $TOTAL_PASS  Failed: $TOTAL_FAIL"
echo " Summary: $SUMMARY_FILE"
echo " Output:  $OUTPUT_DIR/"
echo "=============================================="

# Print any IV findings prominently
if grep -q "\[IV FOUND\]" "$SUMMARY_FILE" 2>/dev/null; then
    echo ""
    echo "***** IV BASE CONSTANTS FOUND IN: *****"
    grep "\[IV FOUND\]" "$SUMMARY_FILE" | sed 's/^/  /'
    echo ""
    echo "Check the iv_xor_*.txt files for XOR instruction details."
fi
