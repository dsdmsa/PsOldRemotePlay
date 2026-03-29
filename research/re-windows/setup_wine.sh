#!/bin/bash
#
# setup_wine.sh - Install Wine + dependencies for VAIO DLL reverse engineering
#
# This script prepares a Mac environment to run Windows binaries (VAIO installer)
# and dynamically hook them for IV context capture.
#
# Usage: bash research/re-windows/setup_wine.sh
#

set -e

echo "[+] Installing Wine via Homebrew..."
brew install --cask wine-stable

echo "[+] Installing winetricks for easy dependency management..."
brew install winetricks

echo "[+] Installing Visual C++ runtime dependencies (vcrun2010, vcrun2019)..."
winetricks vcrun2010 vcrun2019

echo "[+] Installing .NET 4.8 (VAIO may require this)..."
winetricks dotnet48

echo "[+] Installing optional crypto libraries for VAIO compatibility..."
winetricks vcrun2015

echo ""
echo "[SUCCESS] Wine environment ready!"
echo ""
echo "Next steps:"
echo "  1. Create isolated Wine prefix:"
echo "     export WINEPREFIX=~/.wine_vaio"
echo "     wine wineboot --init"
echo ""
echo "  2. Install VAIO installer:"
echo "     WINEPREFIX=~/.wine_vaio wine research/repos/ps3-remote-play/vaio_installer.exe"
echo ""
echo "  3. Hook VAIO process with Frida:"
echo "     python3 research/re-windows/frida_hook_vaio.py"
echo ""
echo "  4. For manual DLL analysis, run:"
echo "     bash research/re-windows/run_vaio_wine.sh"
