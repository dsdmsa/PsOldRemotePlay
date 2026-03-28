# VAIO Remote Play Client — Key Storage Research

## Critical Finding: NOBODY HAS PUBLICLY DOCUMENTED WHERE THE VAIO CLIENT STORES KEYS

After exhaustive searching across psdevwiki, wololo.net, psx-place, digiex, NeoGAF, GitHub — **no public documentation exists** for where the patched VAIO Remote Play client stores its authentication keys on Windows after registration.

## VAIO Client Architecture

**Installation path:** `C:\Program Files (x86)\Sony\Remote Play with PlayStation 3`

**Key files:**
- `VRPSDK.dll` — Core SDK, **packed with Themida anti-tamper protection**. COM object handling all registration and session logic.
- `VRPMAPPING.dll` — Input mapping COM object.
- `VRPUI.exe` (renamed from `VRP.exe`) — Main UI application.
- `rmp_dll.dll` / `rmp_launcher.exe` — Patched loader files.

**How VAIO check works:** `ICoreInterface::Initialize` queries WMI for `Win32_BIOS.Vendor`, `Win32_ComputerSystem.Manufacturer`, `Win32_ComputerSystemProduct.Vendor`, expecting `"Sony Corporation"`. VRPPatch injects a DLL to patch this. Alternative: use `mofcomp` to inject fake WMI classes (from psdevwiki).

**Themida problem:** VRPSDK.dll is Themida-packed. DLL files packed with Themida cannot be reliably unpacked for static analysis — requires dynamic analysis (runtime debugging).

## Most Likely Storage Locations

### Windows Registry (most probable)
- `HKEY_CURRENT_USER\Software\Sony\Remote Play with PlayStation 3\`
- `HKEY_LOCAL_MACHINE\SOFTWARE\Sony\Remote Play with PlayStation 3\`
- `HKEY_CURRENT_USER\Software\Sony Corporation\VAIO Remote Play\`
- Under a CLSID subkey related to the registered COM object

### AppData / ProgramData
- `%APPDATA%\Sony\Remote Play with PlayStation 3\`
- `%LOCALAPPDATA%\Sony\Remote Play with PlayStation 3\`
- `%PROGRAMDATA%\Sony\Remote Play with PlayStation 3\`

### Installation directory
- Binary config file in `C:\Program Files (x86)\Sony\Remote Play with PlayStation 3\`

## What Keys Must Be Stored

| Key | Size | Purpose |
|-----|------|---------|
| **pkey** | 16 bytes | Shared secret from registration |
| **device ID** | 16 bytes | Sent as `PREMO-PSPID` header (base64) |
| **device MAC** | 6 bytes | Used in auth computation plaintext |
| **PS3 MAC** | 6 bytes | Used for WoL and identification |
| **PS3 hostname/IP** | variable | Connection target |

Keys may be encrypted with Windows DPAPI (`CryptProtectData`).

## How to Find the Storage (Recommended Steps)

1. **ProcMon (Process Monitor)** — Run during VAIO registration. Filter on `VRP.exe`/`VRPUI.exe`. Look for `RegSetValue` and `WriteFile` operations. **This takes 5 minutes.**

2. **RegShot** — Take registry snapshot before and after registration, diff all changes.

3. **API Monitor** — Hook `RegSetValueEx`, `CryptProtectData`, `WriteFile` calls from VRPSDK.dll.

4. If keys are in registry and DPAPI-encrypted: `CryptUnprotectData` on the same Windows user account will decrypt.

5. If keys are in a binary file: compare structure to Open-RP's `.orp` format (well-documented in `orp-conf.h`).

## PPSSPP (PSP Emulator) — NOT Viable

- `sceOpenPSIDGetOpenPSID()` returns dummy data derived from host MAC — NOT a valid OpenPSID
- Registration requires WiFi-direct or USB connectivity that an emulator cannot provide
- After PS3 FW 2.80, PS3 validates device identifiers
- OpenPSID on real hardware is Kirk-signed (ECDSA) from IDStorage — can't be faked in software

Sources: PPSSPP Issue #4366, PS-ConsoleId-wiki, psdevwiki IDStorage

## Xperia APK — Defunct

Sony's official `com.playstation.remoteplay` APK for Xperia phones registered as "mobile phone" device type. It handled registration WITHOUT a PSP. Decompiling it would reveal the registration protocol. However, the APK appears lost — not found on public mirror sites.

## Sources
- psdevwiki: Remote Play — https://www.psdevwiki.com/ps3/Remote_Play
- psdevwiki: Talk:Remote_Play — https://www.psdevwiki.com/ps3/Talk:Remote_Play
- VRPPatch — https://digiex.net/threads/vrppatch-vaio-remote-play-patch-download-for-ps3.13874/
- PS3-Pro/Remote-Play — https://github.com/PS3-Pro/Remote-Play
- PPSSPP Issue #4366 — https://github.com/hrydgard/ppsspp/issues/4366
- RPCS3 OpenPSID PR — https://github.com/RPCS3/rpcs3/pull/17543
- psdevwiki IDStorage — https://www.psdevwiki.com/psp/IDStorage
- Magicmida Themida unpacker — https://github.com/Hendi48/Magicmida
