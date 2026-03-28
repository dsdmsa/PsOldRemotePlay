# VRPPatch - VAIO Remote Play Patch
Source: https://digiex.net/threads/vrppatch-vaio-remote-play-patch-download-for-ps3.13874/

## Purpose
Enables Sony VAIO Remote Play software to function on non-Sony computers by bypassing manufacturer-specific system checks.

## How It Works
DLL injection to modify the application at runtime. The original application checks various WMI things to see if it runs on a system manufactured by "Sony Corporation." The patch intercepts and patches the verification location within the injected DLL rather than attempting to unpack the Themida-protected VRPSDK.dll file.

## Key Files
- `VRPSDK.dll` — Core SDK, packed with Themida anti-tamper protection
- `VRPMAPPING.dll` — Input mapping
- `VRPUI.exe` — Main UI application

## System Requirements
- Windows 7 (any version)
- WLAN card for initial pairing
- PS3 firmware 3.30 or higher
- Visual C++ 2008 runtime (non-SP1)
- Original VAIO Remote Play software installed from Sony's official site

## Installation Steps
1. Install the official Sony Remote Play application
2. Reboot the system
3. Register COM components via administrator command prompt:
   - `regsvr32 vrpsdk.dll`
   - `regsvr32 vrpmapping.dll`
4. Extract patch files over originals
5. Launch VRPUI.exe

## Download
**File:** VRPPatch.zip (367.3 KB) — 81,667 views
Available from Digiex download center.

## Developer Credits
Created by NTAuthority with references to RichDevX's previous work (2010-06-17).
