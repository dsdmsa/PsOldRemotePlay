#!/bin/bash
# Package all files needed for the Linux VAIO hook approach
# Run from the repo root: bash research/linux-handoff/package.sh

set -e
REPO="/Users/mihailurmanschi/Work/PsOldRemotePlay"
OUT="/Users/mihailurmanschi/Work/PsOldRemotePlay/research/linux-handoff/ps3_vaio_hook"

echo "[+] Creating package directory..."
rm -rf "$OUT"
mkdir -p "$OUT/vaio_app" "$OUT/tools" "$OUT/research_docs"

echo "[+] Copying instructions..."
cp "$REPO/research/linux-handoff/LINUX_INSTRUCTIONS.md" "$OUT/"

echo "[+] Copying hook tool source + binary..."
cp "$REPO/research/tools/hook_registration.c" "$OUT/"
cp "$REPO/research/tools/hook_registration.exe" "$OUT/"

echo "[+] Copying Frida hook script..."
cp "$REPO/research/re-windows/frida_hook_vaio.py" "$OUT/"

echo "[+] Copying VAIO app files..."
SRC="$REPO/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/extracted/vrp_files/App"
cp "$SRC/VRPSDK.dll" "$OUT/vaio_app/"
cp "$SRC/VRP.exe" "$OUT/vaio_app/"
cp "$SRC/VRPMFMGR.dll" "$OUT/vaio_app/"
cp "$SRC/VRPMapping.dll" "$OUT/vaio_app/"
cp "$SRC/UFCore.dll" "$OUT/vaio_app/"
cp "$SRC/sonyjvtd.dll" "$OUT/vaio_app/"
cp "$SRC/Resource.dll" "$OUT/vaio_app/"

# English language resources
cp "$REPO/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/extracted/vrp_files/ShortcutsCreate/en-us/VRPRes.dll" \
   "$OUT/vaio_app/VRPRes_en.dll"

# VRPPatch files
cp "$REPO/research/PS3 Remote Play on PC + Patch/Folder 2 - Patch/rmp_launcher.EXE" "$OUT/vaio_app/"
cp "$REPO/research/PS3 Remote Play on PC + Patch/Folder 2 - Patch/rmp_dll.DLL" "$OUT/vaio_app/"
cp "$REPO/research/PS3 Remote Play on PC + Patch/Folder 3 - Only If the first patch dont work/VRPPatch.dll" "$OUT/vaio_app/"

echo "[+] Copying Python tools..."
cp "$REPO/research/tools/ps3_register_bruteforce_iv.py" "$OUT/tools/" 2>/dev/null || true
cp "$REPO/research/tools/test_constant_contexts.py" "$OUT/tools/" 2>/dev/null || true
cp "$REPO/research/tools/ps3_wifi_connect.sh" "$OUT/tools/" 2>/dev/null || true

echo "[+] Copying research docs..."
cp "$REPO/research/pupps3/ghidra_findings/22_VAIO_DLL_ANALYSIS.md" "$OUT/research_docs/" 2>/dev/null || true
cp "$REPO/research/pupps3/ghidra_findings/20_DECOMPILED_REGISTRATION_HANDLER.md" "$OUT/research_docs/" 2>/dev/null || true
cp "$REPO/research/pupps3/ghidra_findings/08_COMPLETE_PROTOCOL_SUMMARY.md" "$OUT/research_docs/" 2>/dev/null || true

echo "[+] Copying additional dump/hook C sources..."
cp "$REPO/research/tools/dump_vrpsdk.c" "$OUT/tools/" 2>/dev/null || true
cp "$REPO/research/tools/dump_vrpsdk3.c" "$OUT/tools/" 2>/dev/null || true
cp "$REPO/research/tools/hook_aes.c" "$OUT/tools/" 2>/dev/null || true

echo "[+] Copying rmp_dll decompiled source..."
cp "$REPO/research/rmp_dll.dll.c" "$OUT/research_docs/" 2>/dev/null || true

echo "[+] Creating tarball..."
cd "$REPO/research/linux-handoff"
tar czf ps3_vaio_hook_package.tar.gz ps3_vaio_hook/

SIZE=$(du -sh ps3_vaio_hook_package.tar.gz | cut -f1)
echo ""
echo "========================================="
echo "  Package created: ps3_vaio_hook_package.tar.gz ($SIZE)"
echo "  Location: $REPO/research/linux-handoff/ps3_vaio_hook_package.tar.gz"
echo "========================================="
echo ""
echo "Transfer to Linux:"
echo "  scp research/linux-handoff/ps3_vaio_hook_package.tar.gz user@linux-host:~/"
echo ""
echo "On Linux:"
echo "  tar xzf ps3_vaio_hook_package.tar.gz"
echo "  cat ps3_vaio_hook/LINUX_INSTRUCTIONS.md"
