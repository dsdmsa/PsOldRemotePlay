# VRPSDK.dll Unpacking Analysis Report

## Summary

VRPSDK.dll is protected with a **two-layer packing scheme**:
1. **Custom XOR+ADD packer** with aPLib compression (fully unpacked)
2. **Themida/WinLicense** virtual machine code protection (cannot be statically unpacked)

The actual registration code is protected by Themida VM bytecode and cannot be statically decompiled without running the DLL in a Windows environment.

## DLL Structure

| Section | VAddr | File Offset | File Size | VSize | Purpose |
|---------|-------|-------------|-----------|-------|---------|
| (unnamed) | 0x10001000 | 0x1000 | 0x12200 | 0x28000 | Original code (zeroed by packer) |
| .rsrc | 0x10029000 | 0x13200 | 0x2c00 | 0x3000 | Resources, type library |
| .idata | 0x1002c000 | 0x15e00 | 0x200 | 0x1000 | Minimal imports (lstrcpy, InitCommonControls) |
| (unnamed rwx) | 0x1002d000 | 0x16000 | 0x200 | 0x16d000 | Runtime: receives decompressed Themida VM |
| ehleidof | 0x1019a000 | 0x16200 | 0x12be00 | 0x12c000 | XOR+ADD encrypted aPLib compressed payload |
| wovitjaj | 0x102c6000 | 0x142000 | 0x200 | 0x1000 | Packer entry point |

## Packing Layers

### Layer 1: Custom XOR+ADD + aPLib (UNPACKED)

**Entry point:** 0x102c6000 (wovitjaj section)

**Anti-debug:** Checks if byte at 0x102c600a == 0xCC (int3). If a debugger replaced it, skips decryption.

**First stage decrypt:**
- Target: First 0x1000 bytes of ehleidof section (VA 0x1019a000)
- Algorithm: For each DWORD: `val ^= 0x6cfd7e36; val += 0x3640736d`
- Result: aPLib decompressor + compressed data header

**Second stage (aPLib decompression):**
- Decrypted code at 0x1019a000 is an aPLib decompressor
- Source: ehleidof + 0x1f8 (VA 0x1019a1f8)
- Destination: section3 + 0x14 (VA 0x1002d014)
- Decompressed size: 1,493,862 bytes (1.4 MB)
- After decompression, patches entry to JMP 0x1002d014

**OEP patching:**
- Patches byte at offset 0x62 in decompressor to `JMP 0x1f3`
- 0x1f3 contains `JMP 0x1002d014` (relative to VA 0x1019a1f3)
- On subsequent DLL loads, immediately jumps to decompressed code

### Layer 2: Themida/WinLicense VM (NOT UNPACKED)

The decompressed 1.4 MB payload is a **Themida/WinLicense** virtual machine. Evidence:

1. **VM Opcodes:** Repetitive patterns like `ZXZXP`, `PPR`, `QY`, `aZX` throughout the code section
2. **VxD Names:** `IFSMGR`, `VKD`, `VMM`, `VWIN32`, `VXDLDR` at offset 0x826c3
3. **Protector strings:** "Exception Information", "Please, contact the software developers", diagnostic counters (CheckIN/OUT, ProcIN/OUT, ExitIN/OUT, TPin, HWIn, IntV)
4. **Import thunks:** Only `USER32.dll`, `ADVAPI32.dll`, `NTDLL.dll` visible (Themida resolves real imports at runtime)
5. **High entropy:** 7.7 for decompressed data (VM bytecode is dense)

## Extracted Information

### From .rsrc section (type library, always cleartext):
- COM interface: `ICoreInterface` in `XRPSDKLib`
- Methods: `NInitialize`, `UnInitialize`, `StartRegistration`, `CancelRegistration`
- Methods: `StartRemotePlay`, `StopRemotePlay`, `SendTurnOffPS3Signal`, `SendKey`
- Methods: `SetPSNLoginPassword`, `GetPSNLoginPassword`, `DeletePSNLoginPassword`
- Methods: `SetPSNLoginID`, `GetPSNLoginID`, `DeletePSNLoginID`
- Methods: `GetVideoBitrate`, `SetVideoBitrate`, `GetResponseSpeed`, `SetResponseSpeed`
- Methods: `GetPS3NickName`, `GetPCNickName`
- Parameters: `mixerName`, `exePath`, `connectionType`, `device`, `PIN`, `pcNickName`, `hEvent`, `hwnd`, `padData`, `password`, `loginID`, `bitRate`, `responseSpeed`, `ps3NickName`

### From decompressed payload:
- `%userappdata%\RestartApp.exe` (restart mechanism)
- `USER32.dll`, `ADVAPI32.dll`, `NTDLL.dll` (system DLL imports)
- Themida diagnostic strings (protector metadata)

## Tools Used

| Tool | Status | Notes |
|------|--------|-------|
| radare2 6.1.2 | Installed | Primary disassembler, works on x86 PE |
| rizin 0.8.2 | Installed | Radare2 fork, works for x86 analysis |
| pefile (Python) | Installed | PE file parser |
| aplib (Python) | Installed | aPLib decompression library |
| RetDec | Available via brew | Not installed (won't help with Themida) |
| Ghidra headless | Not found | Not installed on this system |

## Generated Files

| File | Description |
|------|-------------|
| `ehleidof_raw.bin` | Raw encrypted ehleidof section (1.2 MB) |
| `ehleidof_stage1_off0.bin` | First 0x1000 bytes decrypted (aPLib decompressor code) |
| `compressed_payload.bin` | aPLib compressed data from offset 0x1f8 |
| `decompressed_section.bin` | Full aPLib-decompressed Themida VM (1.4 MB) |
| `unpacked_code.bin` | Same as decompressed_section.bin |
| `vrpsdk_memory_image.bin` | Full reconstructed memory image (2.8 MB) |
| `decompressed_strings.txt` | All extracted strings from decompressed data |
| `strings_*.txt` | String dumps from various decryption attempts |

## Conclusion and Next Steps

**Static analysis has reached its limit.** The actual VRPSDK registration code is protected by Themida VM bytecode, which requires dynamic analysis (running the code) to unpack.

### Options for further analysis:

1. **Wine + x32dbg/OllyDbg:** Run VRPSDK.dll under Wine, attach a debugger, let Themida unpack, then dump the process memory. This would give the real code.

2. **Windows VM:** Run the official PS3 Remote Play Windows app in a VM with x32dbg. Set breakpoints on WinHTTP/crypto APIs to intercept the registration flow.

3. **Network capture:** Instead of reversing the DLL, capture the actual network traffic during registration using Wireshark/mitmproxy. The HTTP registration request/response can be analyzed directly.

4. **Scylla + x32dbg (Windows):** Use Scylla to reconstruct the IAT and dump the unpacked binary from a running process.

5. **Alternative approach:** Since the registration protocol is already mostly understood from PS3 firmware analysis, and only the 8-byte IV context value is missing, focus on the PS3 side (xRegistry bypass, memory analysis) rather than the Windows client.
