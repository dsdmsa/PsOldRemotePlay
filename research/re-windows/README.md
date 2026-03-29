# VAIO DLL Reverse Engineering on Mac via Wine

This directory contains tools and guides for reverse engineering the VAIO Remote Play DLL on macOS using Wine (Windows emulation) and dynamic/static analysis.

## Files

- **setup_wine.sh** — Install Wine, winetricks, and required runtime dependencies (VC++ 2010/2019, .NET 4.8)
- **run_vaio_wine.sh** — Execute VAIO installer in Wine, extract VRPSDK.dll and VRPMFMGR.dll to /tmp/vrpsdk_decrypted/
- **frida_hook_vaio.py** — Dynamically hook VAIO DLL at runtime to capture:
  - StartRegistration() calls and parameters
  - CryptSetKeyParam() calls (especially KP_IV - the IV context)
  - CryptEncrypt plaintext/ciphertext for key derivation analysis
- **ghidra_analysis.md** — Step-by-step guide for static analysis of decompressed DLL in Ghidra

## Quick Start

### 1. Setup Wine Environment

```bash
bash research/re-windows/setup_wine.sh
```

This installs Wine + dependencies. Coffee break warning: winetricks downloads ~2GB.

### 2. Extract VAIO DLL

Place your VAIO Remote Play installer at:
- `research/repos/ps3-remote-play/vaio_installer.exe`, or
- Set environment variable: `VAIO_INSTALLER_PATH=/path/to/installer.exe`

Then run:

```bash
bash research/re-windows/run_vaio_wine.sh
```

DLLs will be extracted to `/tmp/vrpsdk_decrypted/VRPSDK.dll` and `/tmp/vrpsdk_decrypted/VRPMFMGR.dll`.

### 3. Dynamic Analysis with Frida (Recommended)

First, install Frida:

```bash
pip install frida frida-tools
```

Then, start VAIO installer in background:

```bash
WINEPREFIX=~/.wine_vaio wine /path/to/vaio_installer.exe &
```

While VAIO is running, launch the Frida hook:

```bash
python3 research/re-windows/frida_hook_vaio.py
```

The script will:
- Find the VAIO process
- Inject hooks into CryptEncrypt, CryptSetKeyParam, memcpy
- Log all encryption operations and key setup
- **When you see `KP_IV: xxxxxxxx`, that's the IV context value!**

### 4. Static Analysis with Ghidra (Fallback)

If dynamic analysis doesn't work:

1. Follow `ghidra_analysis.md` to decompress and load the DLL in Ghidra
2. Search for `SuGetMachineIDEx` string cross-references
3. Analyze the registration setup function at offset 0x1000bb60
4. Trace IV context derivation back to its source

## What We're Looking For

The PS3 registration protocol requires an 8-byte IV (Initialization Vector) context value. We've verified:
- The registration key derivation formulas (SKEY0, SKEY2 constants)
- The PIN is involved, but not as the direct IV
- The IV comes from somewhere in VAIO's startup flow

**Goal**: Determine whether IV context is:
- Hardware machine ID (via SuGetMachineIDEx)
- Derived from PIN (via hash/encoding)
- Some combination
- Runtime value

## Expected Output

### Dynamic Analysis Success

```
[+] Found module: VRPSDK.dll @ 0x10000000
[+] Hooking CryptEncrypt
[+] Hooking CryptSetKeyParam
[SUCCESS] Hooks ready, waiting for registration...

[CRYPTO] CryptSetKeyParam: KP_IV
[!!!] CRITICAL: CryptSetKeyParam
      IV context (8-16 bytes): a1b2c3d4e5f6a7b8
      hKey: 0x...
      This is likely the missing IV context value!
```

### Static Analysis Success

When analyzing in Ghidra, you'll find a function that:
1. Calls `SuGetMachineIDEx` to get hardware GUID
2. Derives 8 bytes from the GUID (or PIN, or combo)
3. Passes result to `CryptSetKeyParam(..., KP_IV, context, ...)`
4. Uses that context to encrypt the registration request

## Validation

Once you have the IV context, validate it with:

```bash
python3 research/tools/ps3_register_bruteforce_iv.py \
  --iv-context a1b2c3d4e5f6a7b8 \
  --test-pin 0000 \
  --test-registration
```

If registration succeeds, you've found it!

## Troubleshooting

**Wine won't install**
- Ensure you have Homebrew: `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`
- Try: `brew tap homebrew/cask && brew install wine-stable`

**VAIO installer won't run**
- Try with specific Wine prefix: `WINEPREFIX=~/.wine_vaio wine cmd /c "path\\to\\installer.exe"`
- Check logs: `WINE_CPU_TOPOLOGY=4:2 wine installer.exe` (more cores)

**Frida can't attach**
- Ensure process is running: `ps aux | grep wine`
- Try listing processes: `python3 -c "import frida; print([p.name for p in frida.enumerate_processes()])"`
- May need elevated privileges: `sudo python3 research/re-windows/frida_hook_vaio.py`

**Ghidra decompression issues**
- Try UPX: `brew install upx && upx -d VRPSDK.dll -o VRPSDK_unpacked.dll`
- Manual hex editing if section is XORed (check first 4 bytes for MZ header)

## References

- `research/pupps3/ghidra_findings/20_DECOMPILED_REGISTRATION_HANDLER.md` — PS3 firmware PREMO registration
- `research/tools/ps3_register_bruteforce_iv.py` — IV brute-force tester (22 encoding variants)
- `research/STATUS_AND_NEXT_STEPS.md` — Overall project status and blockers
