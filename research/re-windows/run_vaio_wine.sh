#!/bin/bash
#
# run_vaio_wine.sh - Run VAIO installer in Wine, extract and copy DLLs
#
# This script:
#   1. Sets up an isolated Wine prefix (~/.wine_vaio)
#   2. Runs the VAIO Remote Play installer
#   3. After installation, locates VRPSDK.dll and VRPMFMGR.dll
#   4. Copies them to /tmp/vrpsdk_decrypted/ for analysis
#
# Usage: bash research/re-windows/run_vaio_wine.sh
#
# The VAIO installer path can be overridden by setting VAIO_INSTALLER_PATH env var:
#   VAIO_INSTALLER_PATH=/path/to/my_installer.exe bash research/re-windows/run_vaio_wine.sh
#

set -e

# Configuration
WINE_PREFIX="${WINE_PREFIX:-$HOME/.wine_vaio}"
OUTPUT_DIR="/tmp/vrpsdk_decrypted"

# Try to locate VAIO installer
if [ -z "$VAIO_INSTALLER_PATH" ]; then
  # Try common locations
  if [ -f "research/repos/ps3-remote-play/vaio_installer.exe" ]; then
    VAIO_INSTALLER_PATH="research/repos/ps3-remote-play/vaio_installer.exe"
  elif [ -f "research/repos/VAIO_RemotePlay_installer.exe" ]; then
    VAIO_INSTALLER_PATH="research/repos/VAIO_RemotePlay_installer.exe"
  else
    echo "[-] VAIO installer not found in expected locations."
    echo "    Please set VAIO_INSTALLER_PATH or place installer at:"
    echo "    research/repos/ps3-remote-play/vaio_installer.exe"
    exit 1
  fi
fi

echo "[+] Using VAIO installer: $VAIO_INSTALLER_PATH"
echo "[+] Wine prefix: $WINE_PREFIX"
echo "[+] Output directory: $OUTPUT_DIR"

# Initialize Wine prefix if needed
if [ ! -d "$WINE_PREFIX" ]; then
  echo "[+] Initializing Wine prefix..."
  WINEPREFIX="$WINE_PREFIX" wine wineboot --init
fi

# Run VAIO installer
echo "[+] Running VAIO installer..."
echo "    This will open a GUI installer window. Install to C:\\Program Files\\VAIO"
echo "    When complete, close the installer window and this script will continue."
echo ""
WINEPREFIX="$WINE_PREFIX" wine "$VAIO_INSTALLER_PATH"

echo ""
echo "[+] Searching for VRPSDK.dll and VRPMFMGR.dll..."

# Typical VAIO installation paths in Wine
SEARCH_PATHS=(
  "$WINE_PREFIX/drive_c/Program Files/VAIO/RemotePlay"
  "$WINE_PREFIX/drive_c/Program Files/VAIO"
  "$WINE_PREFIX/drive_c/Program Files (x86)/VAIO"
  "$WINE_PREFIX/drive_c/Program Files (x86)/VAIO/RemotePlay"
  "$WINE_PREFIX/drive_c/Windows/System32"
  "$WINE_PREFIX/drive_c/Windows/SysWOW64"
)

FOUND_VRPSDK=0
FOUND_VRPMFMGR=0

for path in "${SEARCH_PATHS[@]}"; do
  if [ -f "$path/VRPSDK.dll" ]; then
    echo "[+] Found VRPSDK.dll at: $path"
    FOUND_VRPSDK=1
    VRPSDK_PATH="$path/VRPSDK.dll"
  fi
  if [ -f "$path/VRPMFMGR.dll" ]; then
    echo "[+] Found VRPMFMGR.dll at: $path"
    FOUND_VRPMFMGR=1
    VRPMFMGR_PATH="$path/VRPMFMGR.dll"
  fi
done

if [ "$FOUND_VRPSDK" -eq 0 ] && [ "$FOUND_VRPMFMGR" -eq 0 ]; then
  echo "[-] No DLLs found. Installation may have failed."
  echo "    Manually verify installation at: $WINE_PREFIX/drive_c"
  exit 1
fi

# Create output directory and copy DLLs
mkdir -p "$OUTPUT_DIR"

if [ "$FOUND_VRPSDK" -eq 1 ]; then
  echo "[+] Copying VRPSDK.dll to $OUTPUT_DIR..."
  cp "$VRPSDK_PATH" "$OUTPUT_DIR/VRPSDK.dll"
fi

if [ "$FOUND_VRPMFMGR" -eq 1 ]; then
  echo "[+] Copying VRPMFMGR.dll to $OUTPUT_DIR..."
  cp "$VRPMFMGR_PATH" "$OUTPUT_DIR/VRPMFMGR.dll"
fi

echo ""
echo "[SUCCESS] DLLs extracted to: $OUTPUT_DIR"
echo ""
echo "Next steps:"
echo "  1. Analyze DLLs with Ghidra or IDA Pro"
echo "  2. Use Frida to hook the DLLs at runtime:"
echo "     python3 research/re-windows/frida_hook_vaio.py"
echo ""
echo "  3. Or decompress with aplib:"
echo "     python3 research/tools/unpack_vrpsdk_aplib.py $OUTPUT_DIR/VRPSDK.dll"
