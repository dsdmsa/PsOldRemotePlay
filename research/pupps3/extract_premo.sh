#!/bin/bash
#
# PS3 Firmware Extraction Pipeline
# Extracts and decrypts premo_plugin.sprx from PS3UPDAT.PUP
#
# What this script does automatically:
#   Phase 1: Clone & build tools (pupunpack, scetool)
#   Phase 2: Extract PUP → tar → dev_flash filesystem
#   Phase 3: Decrypt SPRX → ELF (ready for Ghidra)
#
# What YOU do manually after this:
#   Phase 4: Install Ghidra + load the ELF (see instructions at the end)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORK_DIR="$SCRIPT_DIR/extraction"
TOOLS_DIR="$WORK_DIR/tools"
OUTPUT_DIR="$WORK_DIR/output"
PUP_FILE="$SCRIPT_DIR/PS3UPDAT.PUP"

# Colors for logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_phase() { echo -e "\n${CYAN}════════════════════════════════════════════════════════════${NC}"; echo -e "${CYAN}  PHASE $1: $2${NC}"; echo -e "${CYAN}════════════════════════════════════════════════════════════${NC}\n"; }
log_step()  { echo -e "${GREEN}[STEP]${NC} $1"; }
log_info()  { echo -e "${YELLOW}[INFO]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC} $1"; }

# ─────────────────────────────────────────────────────────────
# Preflight checks
# ─────────────────────────────────────────────────────────────
echo -e "${CYAN}PS3 Firmware Extraction Pipeline${NC}"
echo "Working directory: $WORK_DIR"
echo ""

if [ ! -f "$PUP_FILE" ]; then
    log_error "PS3UPDAT.PUP not found at $PUP_FILE"
    exit 1
fi
log_ok "Found PUP file: $(du -h "$PUP_FILE" | cut -f1) — $(basename "$PUP_FILE")"

# Check required tools
for cmd in git make python3 tar; do
    if ! command -v $cmd &>/dev/null; then
        log_error "Missing required tool: $cmd"
        exit 1
    fi
done
log_ok "Build tools available (git, make, python3, tar)"

# Check OpenSSL (needed for scetool on some systems)
if [ -d "/opt/homebrew/opt/openssl@3" ]; then
    export OPENSSL_DIR="/opt/homebrew/opt/openssl@3"
    log_ok "OpenSSL@3 found at $OPENSSL_DIR"
elif [ -d "/opt/homebrew/opt/openssl@1.1" ]; then
    export OPENSSL_DIR="/opt/homebrew/opt/openssl@1.1"
    log_ok "OpenSSL@1.1 found at $OPENSSL_DIR"
fi

mkdir -p "$TOOLS_DIR" "$OUTPUT_DIR"

# ─────────────────────────────────────────────────────────────
log_phase 1 "CLONE & BUILD TOOLS"
# ─────────────────────────────────────────────────────────────

# --- Tool 1: fail0verflow ps3tools (for pupunpack + unself) ---
PS3TOOLS_DIR="$TOOLS_DIR/ps3tools"
if [ -f "$PS3TOOLS_DIR/pupunpack" ]; then
    log_ok "ps3tools already built, skipping"
else
    log_step "Cloning fail0verflow ps3tools..."
    rm -rf "$PS3TOOLS_DIR"
    git clone --depth 1 https://github.com/manusan/ps3tools.git "$PS3TOOLS_DIR" 2>&1 | tail -1

    log_step "Building ps3tools (pupunpack, unself, readself)..."
    cd "$PS3TOOLS_DIR"

    # Fix Makefile for macOS clang
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' 's/gcc/cc/g' Makefile 2>/dev/null || true
    fi

    make 2>&1 | tail -5

    if [ -f "$PS3TOOLS_DIR/pupunpack" ]; then
        log_ok "ps3tools built successfully: pupunpack, unself, readself"
    else
        log_error "ps3tools build failed. Check output above."
        exit 1
    fi
    cd "$SCRIPT_DIR"
fi

# --- Tool 2: scetool (for SPRX decryption) ---
SCETOOL_DIR="$TOOLS_DIR/scetool"
if [ -f "$SCETOOL_DIR/scetool" ]; then
    log_ok "scetool already built, skipping"
else
    log_step "Cloning scetool..."
    rm -rf "$SCETOOL_DIR"
    git clone --depth 1 https://github.com/naehrwert/scetool.git "$SCETOOL_DIR" 2>&1 | tail -1

    log_step "Building scetool..."
    cd "$SCETOOL_DIR"

    # Fix Makefile for macOS clang
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' 's/gcc/cc/g' Makefile 2>/dev/null || true
        # Add zlib include path if needed
        if [ -d "/opt/homebrew/opt/zlib" ]; then
            sed -i '' "s|-lz|-L/opt/homebrew/opt/zlib/lib -I/opt/homebrew/opt/zlib/include -lz|g" Makefile 2>/dev/null || true
        fi
    fi

    make 2>&1 | tail -5

    if [ -f "$SCETOOL_DIR/scetool" ]; then
        log_ok "scetool built successfully"
    else
        log_error "scetool build failed. Trying with explicit flags..."
        cc -o scetool *.c -lz -I/opt/homebrew/opt/zlib/include -L/opt/homebrew/opt/zlib/lib 2>&1 | tail -5
        if [ -f "$SCETOOL_DIR/scetool" ]; then
            log_ok "scetool built with fallback flags"
        else
            log_error "scetool build failed. Will try unself from ps3tools as fallback."
        fi
    fi
    cd "$SCRIPT_DIR"
fi

# --- Keys for scetool ---
KEYS_DIR="$SCETOOL_DIR/data"
if [ -f "$KEYS_DIR/keys" ]; then
    log_ok "scetool keys already present"
else
    log_step "Downloading scetool keys from kid23/PS3_Projects..."
    mkdir -p "$KEYS_DIR"
    cd "$TOOLS_DIR"

    if [ ! -d "$TOOLS_DIR/PS3_Projects" ]; then
        git clone --depth 1 --filter=blob:none --sparse https://github.com/kid23/PS3_Projects.git 2>&1 | tail -1
        cd PS3_Projects
        git sparse-checkout set tools/data 2>&1 | tail -1
    fi

    if [ -f "$TOOLS_DIR/PS3_Projects/tools/data/keys" ]; then
        cp "$TOOLS_DIR/PS3_Projects/tools/data/keys" "$KEYS_DIR/"
        cp "$TOOLS_DIR/PS3_Projects/tools/data/ldr_curves" "$KEYS_DIR/" 2>/dev/null || true
        cp "$TOOLS_DIR/PS3_Projects/tools/data/vsh_curves" "$KEYS_DIR/" 2>/dev/null || true
        log_ok "Keys installed: $(ls "$KEYS_DIR/" | tr '\n' ' ')"
    else
        log_error "Could not download keys. scetool decryption may fail."
        log_info "You can manually place keys/ldr_curves/vsh_curves in: $KEYS_DIR/"
    fi
    cd "$SCRIPT_DIR"
fi

# ─────────────────────────────────────────────────────────────
log_phase 2 "EXTRACT PUP → DEV_FLASH"
# ─────────────────────────────────────────────────────────────

PUP_EXTRACTED="$OUTPUT_DIR/pup_extracted"
DEVFLASH_DIR="$OUTPUT_DIR/dev_flash"

if [ -d "$DEVFLASH_DIR/vsh/module" ]; then
    log_ok "dev_flash already extracted, skipping"
else
    # Step 1: Unpack PUP
    log_step "Unpacking PUP file..."
    mkdir -p "$PUP_EXTRACTED"
    "$PS3TOOLS_DIR/pupunpack" "$PUP_FILE" "$PUP_EXTRACTED" 2>&1

    log_ok "PUP unpacked: $(ls "$PUP_EXTRACTED/" | wc -l | tr -d ' ') files"
    ls -la "$PUP_EXTRACTED/" 2>/dev/null | head -10

    # Step 2: Find and extract the update_files tar
    log_step "Looking for update_files.tar..."
    # PUP contains numbered files — the largest is usually update_files.tar
    UPDATE_TAR=""
    for f in "$PUP_EXTRACTED"/*; do
        if file "$f" 2>/dev/null | grep -qi "tar\|POSIX"; then
            UPDATE_TAR="$f"
            log_ok "Found tar archive: $(basename "$f") ($(du -h "$f" | cut -f1))"
            break
        fi
    done

    # If file detection didn't work, try the largest file
    if [ -z "$UPDATE_TAR" ]; then
        UPDATE_TAR=$(ls -S "$PUP_EXTRACTED"/* 2>/dev/null | head -1)
        log_info "Trying largest file as tar: $(basename "$UPDATE_TAR")"
    fi

    if [ -z "$UPDATE_TAR" ] || [ ! -f "$UPDATE_TAR" ]; then
        log_error "Could not find update_files.tar in extracted PUP"
        log_info "Contents of $PUP_EXTRACTED:"
        ls -la "$PUP_EXTRACTED/"
        exit 1
    fi

    # Step 3: Extract tar → dev_flash
    log_step "Extracting dev_flash from tar..."
    mkdir -p "$DEVFLASH_DIR"
    tar xf "$UPDATE_TAR" -C "$OUTPUT_DIR/" 2>/dev/null || {
        # Some PUPs have a nested structure
        log_info "Standard tar extract didn't yield dev_flash, trying alternate extraction..."
        tar xf "$UPDATE_TAR" -C "$OUTPUT_DIR/" --strip-components=1 2>/dev/null || true
    }

    # Check if dev_flash was extracted
    if [ ! -d "$DEVFLASH_DIR/vsh/module" ]; then
        # Maybe it's under a subdirectory
        FOUND_VSH=$(find "$OUTPUT_DIR" -path "*/vsh/module" -type d 2>/dev/null | head -1)
        if [ -n "$FOUND_VSH" ]; then
            DEVFLASH_PARENT=$(dirname "$(dirname "$FOUND_VSH")")
            if [ "$DEVFLASH_PARENT" != "$DEVFLASH_DIR" ]; then
                log_info "Found dev_flash at: $DEVFLASH_PARENT"
                mv "$DEVFLASH_PARENT" "$DEVFLASH_DIR" 2>/dev/null || true
            fi
        fi
    fi

    if [ -d "$DEVFLASH_DIR/vsh/module" ]; then
        log_ok "dev_flash extracted successfully"
    else
        log_error "Could not find vsh/module in extracted files"
        log_info "Searching for premo files..."
        find "$OUTPUT_DIR" -name "*premo*" 2>/dev/null
        log_info "Directory structure:"
        find "$OUTPUT_DIR" -maxdepth 4 -type d 2>/dev/null | head -30
        exit 1
    fi
fi

# Step 4: Find premo SPRX files
log_step "Locating premo SPRX files..."
PREMO_PLUGIN=$(find "$OUTPUT_DIR" -name "premo_plugin.sprx" 2>/dev/null | head -1)
PREMO_GAME=$(find "$OUTPUT_DIR" -name "premo_game_plugin.sprx" 2>/dev/null | head -1)

if [ -n "$PREMO_PLUGIN" ]; then
    log_ok "Found: $PREMO_PLUGIN ($(du -h "$PREMO_PLUGIN" | cut -f1))"
    cp "$PREMO_PLUGIN" "$OUTPUT_DIR/premo_plugin.sprx"
else
    log_error "premo_plugin.sprx NOT FOUND in extracted firmware"
    log_info "Available VSH modules:"
    ls "$DEVFLASH_DIR/vsh/module/"*.sprx 2>/dev/null | head -20
    exit 1
fi

if [ -n "$PREMO_GAME" ]; then
    log_ok "Found: $PREMO_GAME ($(du -h "$PREMO_GAME" | cut -f1))"
    cp "$PREMO_GAME" "$OUTPUT_DIR/premo_game_plugin.sprx"
else
    log_info "premo_game_plugin.sprx not found (may be loaded separately)"
fi

# ─────────────────────────────────────────────────────────────
log_phase 3 "DECRYPT SPRX → ELF"
# ─────────────────────────────────────────────────────────────

PREMO_ELF="$OUTPUT_DIR/premo_plugin.elf"
PREMO_GAME_ELF="$OUTPUT_DIR/premo_game_plugin.elf"

decrypt_sprx() {
    local input="$1"
    local output="$2"
    local name="$(basename "$input")"

    if [ -f "$output" ]; then
        log_ok "$name already decrypted, skipping"
        return 0
    fi

    # Try scetool first
    if [ -f "$SCETOOL_DIR/scetool" ] && [ -f "$KEYS_DIR/keys" ]; then
        log_step "Decrypting $name with scetool..."
        cd "$SCETOOL_DIR"
        if ./scetool --decrypt "$input" "$output" 2>&1; then
            if [ -f "$output" ] && [ -s "$output" ]; then
                log_ok "Decrypted with scetool: $output ($(du -h "$output" | cut -f1))"
                cd "$SCRIPT_DIR"
                return 0
            fi
        fi
        cd "$SCRIPT_DIR"
        log_info "scetool failed, trying unself..."
    fi

    # Fallback: unself from ps3tools
    if [ -f "$PS3TOOLS_DIR/unself" ]; then
        log_step "Decrypting $name with unself..."
        if "$PS3TOOLS_DIR/unself" "$input" "$output" 2>&1; then
            if [ -f "$output" ] && [ -s "$output" ]; then
                log_ok "Decrypted with unself: $output ($(du -h "$output" | cut -f1))"
                return 0
            fi
        fi
        log_info "unself also failed"
    fi

    log_error "Could not decrypt $name with any available tool"
    log_info "You may need to manually supply keys or use a different decryption tool"
    return 1
}

# Decrypt premo_plugin
decrypt_sprx "$OUTPUT_DIR/premo_plugin.sprx" "$PREMO_ELF"

# Decrypt premo_game_plugin if present
if [ -f "$OUTPUT_DIR/premo_game_plugin.sprx" ]; then
    decrypt_sprx "$OUTPUT_DIR/premo_game_plugin.sprx" "$PREMO_GAME_ELF"
fi

# ─────────────────────────────────────────────────────────────
# Also dump readself info
# ─────────────────────────────────────────────────────────────
if [ -f "$PS3TOOLS_DIR/readself" ]; then
    log_step "Reading SELF/SPRX metadata..."
    "$PS3TOOLS_DIR/readself" "$OUTPUT_DIR/premo_plugin.sprx" > "$OUTPUT_DIR/premo_plugin_selfinfo.txt" 2>&1 || true
    if [ -s "$OUTPUT_DIR/premo_plugin_selfinfo.txt" ]; then
        log_ok "SELF metadata saved to premo_plugin_selfinfo.txt"
    fi
fi

# ─────────────────────────────────────────────────────────────
log_phase 4 "RESULTS"
# ─────────────────────────────────────────────────────────────

echo ""
echo -e "${GREEN}Output files in: $OUTPUT_DIR/${NC}"
echo "───────────────────────────────────────────"
ls -lh "$OUTPUT_DIR"/premo_*.{sprx,elf,txt} 2>/dev/null
echo ""

if [ -f "$PREMO_ELF" ]; then
    echo -e "${GREEN}SUCCESS!${NC} premo_plugin.elf is ready for Ghidra."
    echo ""
    file "$PREMO_ELF"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  MANUAL STEP: Load into Ghidra"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "  1. Install Ghidra (if not installed):"
    echo "     brew install --cask ghidra"
    echo ""
    echo "  2. Install PS3 Ghidra scripts:"
    echo "     git clone https://github.com/clienthax/Ps3GhidraScripts.git"
    echo "     In Ghidra: File → Install Extensions → select the cloned folder"
    echo ""
    echo "  3. Import the ELF:"
    echo "     File → Import File → select: $PREMO_ELF"
    echo "     Processor: PowerISA-Altivec-64-32addr (Big Endian)"
    echo "     (Uncheck 'recommended only' to see this option)"
    echo ""
    echo "  4. Fix TOC pointer (improves decompilation):"
    echo "     Edit: Ghidra/Processors/PowerPC/data/languages/ppc_64_32.cspec"
    echo "     Add <register name=\"r2\"/> to the <unaffected> list"
    echo ""
    echo "  5. Run analysis:"
    echo "     Analysis → Auto Analyze (accept defaults)"
    echo "     Then run PS3 syscall script from Script Manager"
    echo ""
    echo "  6. What to look for:"
    echo "     - Functions near offset 0xB7E4 (registration check)"
    echo "     - savePremoPSPInformation / loadPremoPSPInformation"
    echo "     - The 0x4B8-byte pairing data structure"
    echo "     - AES/crypto functions used during handshake"
    echo "     - PIN validation logic"
    echo "     - Device ID validation (PSPID checking)"
    echo ""
else
    echo -e "${YELLOW}Decryption did not produce an ELF file.${NC}"
    echo ""
    echo "The SPRX files were extracted but could not be decrypted."
    echo "This means the keys may not cover this firmware version."
    echo ""
    echo "Try manually:"
    echo "  1. Check if scetool keys cover FW $(strings "$PUP_FILE" | grep -oE '[0-9]\.[0-9]{2}' | head -1 || echo 'unknown')"
    echo "  2. Try ps3dec: pip3 install ps3dec"
    echo "  3. The raw SPRX is at: $OUTPUT_DIR/premo_plugin.sprx"
fi

echo ""
echo "Done."
