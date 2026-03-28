# Stock PS3 Remote Play — Complete Research

Research on connecting to a STOCK, UNMODIFIED PS3 (no HEN, no CFW, no jailbreak) for Remote Play, with the goal of building an Android app.

---

## 1. Stock PS3 Remote Play Registration — How Official Pairing Works

### Device Types That Can Register

The PS3 "Register Device" menu (Settings > Remote Play Settings > Register Device) allows registering these device categories:

1. **PSP system** — The original and primary device (since FW 1.60)
2. **PS Vita system** — Added in FW 4.00
3. **Mobile phone** — For Sony Xperia devices (region-dependent)
4. **PC** — For Sony VAIO computers (since FW 3.30)

The PS3 manual states: "Types of devices that can be used for remote play and the availability of such devices vary depending on the country or region."

### Registration Methods

**PSP via USB:**
1. Connect PSP to PS3 via USB cable (Type A to Mini-B)
2. PS3 menu: Settings > Remote Play Settings > Register Device > PSP
3. Follow on-screen instructions
4. PS3 generates a random 16-byte **pkey** (shared secret)
5. Both devices store: pkey, MAC addresses, device IDs

**PSP via WiFi (8-digit PIN):**
1. PS3 displays an 8-digit PIN code
2. User enters the PIN on the PSP within 5 minutes
3. Devices establish a direct WiFi connection for the key exchange
4. Same key material is exchanged as USB method

**PS Vita:**
1. PS3 menu: Settings > Remote Play Settings > Register Device > PS Vita System
2. PS3 displays a number on screen
3. Enter the number on the Vita and select Register
4. Requires PS3 FW 4.00+

**VAIO PC (8-digit PIN + WiFi direct):**
1. PS3 menu: Settings > Remote Play Settings > Register Device
2. PS3 displays 8-digit registration number
3. On PC, launch VAIO Remote Play, enter the 8-digit number
4. **The PC disconnects from normal WiFi and connects directly to the PS3** for key exchange
5. After registration, the PC reconnects to normal WiFi
6. Must be completed within 5 minutes

### What Happens During Registration

From the Open-RP source code (`psp/orp.c`), registration stores these values:

On the **PS3 side** (in `/dev_flash2/etc/xRegistry.sys`):
```
/setting/premo/pspXX/
  nickname    — device name
  macAddress  — device MAC (6 bytes)
  id          — device PSPID/OpenPSID (16 bytes)
  keyType     — key type
  key         — the pkey (16 bytes, shared secret)
```

On the **PSP side** (in registry at `/CONFIG/PREMO`):
```
  ps3_name    — PS3 nickname
  ps3_mac     — PS3 MAC address (6 bytes)
  ps3_key     — the pkey (16 bytes, shared secret)
```

The PSP's OpenPSID (hardware-bound 16-byte identifier) is also sent to the PS3 during registration and stored in the `id` field.

### Can a Regular PC Register?

**No.** Only Sony VAIO computers with the official VAIO Remote Play software can register through the PS3's "PC" option. The VAIO software checks WMI classes to verify the system manufacturer is "Sony Corporation" before allowing registration. Non-VAIO PCs require the VRPPatch or a DLL loader to bypass this check.

**Key insight: The PS3 side does NOT check whether the PC is a VAIO.** The VAIO check is entirely on the PC client side. The PS3 simply sees a device requesting registration via the standard PREMO protocol.

### Sources
- [PlayStation Manual — Register Device](https://manuals.playstation.net/document/en/ps3/current/settings/registerdevice.html)
- [PS3 Developer Wiki — Remote Play](https://www.psdevwiki.com/ps3/Remote_Play)
- [Open-RP source: psp/orp.c](https://github.com/gbraad/open-rp/blob/master/psp/orp.c)
- [PS3 Developer Wiki — XRegistry.sys](https://www.psdevwiki.com/ps3/XRegistry.sys)

---

## 2. VAIO Remote Play on Stock PS3 — Confirmed Working

### Does it work with completely stock/unmodified PS3?

**YES.** The patched VAIO Remote Play client (VRPPatch) works with a completely stock PS3 on original firmware. This is confirmed across multiple sources.

The VRPPatch only bypasses ONE check: the WMI manufacturer verification on the **PC side** that ensures the computer is made by "Sony Corporation." Specifically, it intercepts checks against:
- `Win32_BIOS.Vendor`
- `Win32_ComputerSystem.Manufacturer`
- `Win32_ComputerSystemProduct.Vendor`

The PS3 does not care whether the connecting device is an actual VAIO. From the PS3's perspective, any device that speaks the PREMO protocol and presents valid registration credentials is accepted.

### Requirements
- Windows 7 (or newer with compatibility mode set to Windows 7)
- WiFi card for initial pairing (pairing uses WiFi direct to the PS3)
- PS3 firmware 3.30 or higher
- Visual C++ 2008 runtime
- Official Sony Remote Play software installed, then patched

### Limitations on Stock PS3 (VAIO client)

Sony deliberately restricted the VAIO Remote Play client on the PS3 side. The `premo_game_plugin.sprx` on stock firmware whitelists only **6 PS3 applications** for VAIO Remote Play:

1. **NPEA00135** — VidZone
2. **BCES00096** — PlayTV
3. **NPEA90028** — (PlayTV related)
4. **NPEA90029** — (PlayTV related)
5. **NPJA00001** — Mainichi Issho / Everyday Together
6. **NPJA00046** — Toro to Morimori

**However**, XMB navigation, PS1 classics, and media (photos, music, video) all work without restriction on stock firmware.

To unlock all games via VAIO, the PS3's `premo_game_plugin.sprx` needs to be patched — which requires CFW.

### PSP/Vita Do Not Have This VAIO Whitelist Restriction

When connecting with a PSP or Vita (not VAIO), the stock PS3 allows Remote Play for:
- Full XMB navigation
- All officially RP-compatible PS3 games (about 30-40 titles)
- ALL PS1 classics (both digital and disc-based)
- Media content

The 6-app whitelist is specific to VAIO connections. PSP/Vita use a different code path in `premo_plugin.sprx`.

### Sources
- [PSDevWiki — Remote Play (VAIO whitelist)](https://www.psdevwiki.com/ps3/Remote_Play)
- [VRPPatch — Digiex](https://digiex.net/threads/vrppatch-vaio-remote-play-patch-download-for-ps3.13874/)
- [Tech-Recipes — Remote Play on Non-VAIO PC](https://www.tech-recipes.com/entertainment/games/playstation-3-use-remote-play-on-any-non-sony-vaio-windows-7-pc-ps3-firmware-3-30/)
- [Wololo — HOW TO Remote Play on any PC](https://wololo.net/talk/viewtopic.php?t=46708)

---

## 3. PS Vita Registration with Stock PS3

### Does it work?

**YES.** A stock PS Vita can register with a stock PS3 for Remote Play. This is a completely official, Sony-supported feature.

### Requirements
- PS3 firmware **4.00 or higher** (Vita support was added in FW 4.00)
- PS Vita with current system software
- Both devices signed into the **same PSN account**

### Registration Steps
1. On PS3: Settings > Remote Play Settings > Register Device > PS Vita System
2. PS3 displays a registration number on screen
3. On Vita: Enter the number and select Register
4. Registration completes over network

### Connection Methods After Registration
- **Via PS3 WiFi direct** — Vita connects directly to PS3's ad-hoc WiFi
- **Via home WiFi network** — Both on same router
- **Via Internet** — Over WAN with port forwarding

### Vita-Specific Capabilities on Stock PS3

The Vita gets higher-resolution streaming than PSP:
- PSP resolution: 480x272
- Vita resolution: **852x480** (480p)

The Vita DevWiki confirms the SFO attribute flags:
- 0x01 = M4V 480x272 (PSP/Vita)
- 0x05 = AVC 480x272 (PSP/Vita)
- 0x85 = AVC 480p for Vita + 480x272 for PSP
- 0x80 = AVC 480p for Vita only

### Trick: Vita Can Register with PS3 Pre-FW 4.00

The Vita DevWiki notes: "It is possible to pair PSVita with a PS3 on lower Firmware than 4.00 (e.g., 3.41/3.55) by pairing as mobile phone / PC / PSP." This means the registration protocol is fundamentally the same across device types — the PS3's device-type menu is more of a UI choice than a protocol difference.

### Sources
- [Vita Developer Wiki — Remote Play](https://www.psdevwiki.com/vita/Remote_Play)
- [PlayStation Manual — Remote Play via Internet](https://manuals.playstation.net/document/en/ps3/current/remoteplay/remoteinternet.html)
- [PlayStation Vita User's Guide — Remote Play](https://manuals.playstation.net/document/en/psvita/remoteplay/viaprivate_ap.html)
- [Wololo — Remote Play with OFW Vita](https://wololo.net/talk/viewtopic.php?t=35564)

---

## 4. What Games/Features Work on Stock PS3

### Always Available on Stock Firmware (ALL device types)

| Feature | Works? | Notes |
|---------|--------|-------|
| **XMB Navigation** | YES | Full PS3 menu streaming (VSH exec mode) |
| **Media (Photos/Music/Video)** | YES | Browse and play media stored on PS3 |
| **PS Store browsing** | YES | Navigate the PlayStation Store |
| **PS1 Classics (digital)** | YES | ALL digital PS1 games work |
| **PS1 Classics (disc)** | YES | Added in FW 2.0+ via firmware update |
| **Settings navigation** | Partial | Some settings hidden during RP |

### Officially RP-Compatible PS3 Games (Stock Firmware, PSP/Vita)

Games with the Remote Play flag set in their PARAM.SFO work on stock firmware. The officially supported list includes (but is not limited to):

- Anarchy: Rush Hour (PSN)
- Aqua Vita / Aquatopia (PSN)
- Battlefield 3 (BD)
- Bejeweled 2 (PSN)
- Bionic Commando Rearmed (PSN)
- BlazBlue: Calamity Trigger (BD)
- Call of Juarez: Bound in Blood (BD)
- Everybody's Golf: World Tour (BD)
- Fallout 3: GOTY Edition (BD)
- FirstPlay (PSN)
- Fritz Chess (BD)
- God of War Collection (Vita only, update 1.01)
- Gundemonium Recollection (PSN)
- High Stakes on the Vegas Strip: Poker Edition (PSN)
- ICO & Shadow of the Colossus (HD Collection)
- Killzone 3 (BD)
- Lair (BD)
- LEGO Batman (BD)
- LittleBigPlanet 2 (BD)
- Life with PlayStation (PSN)
- Mainichi Issho (PSN)
- Peggle / Peggle Nights (PSN)
- PixelJunk Eden / Monsters / Shooter (PSN)
- PlayTV (BD/PSN)
- SingStar / SingStar Vol. 2 / SingStar ABBA (BD)
- VidZone (PSN)
- Zuma (PSN)

**Total: roughly 30-40 titles** across all regions.

### VAIO PC Whitelist (Much More Restricted)

When connecting via VAIO client, only 6 applications are whitelisted (see Section 2). This is a PS3-side restriction in `premo_game_plugin.sprx`. XMB and media still work.

### What Does NOT Work on Stock Firmware

- **PS2 Classics** — Not supported for Remote Play at all
- **Most PS3 disc games** — Unless they have the RP flag in PARAM.SFO
- **Unlocking all games for RP** — Requires CFW/HEN + webMAN-MOD or patched premo plugins

### Video/Audio Specs During Remote Play

| Parameter | Values |
|-----------|--------|
| Video Resolutions | 320x240, 432x240, 480x272, 852x480 (HD) |
| Video Codecs | M4V/M4HD, AVC, AVC/MP, AVC/CAVLC |
| Video Bitrates | 256, 384, 512, 768, 1024 kbps, 2500 kbps (HD) |
| Video Framerates | 7.5, 10, 15, 30 fps |
| Audio Codecs | M4A, AT3 (ATRAC3) |
| Audio Bitrates | 72, 128, 144 kbps |
| Audio Sample Rate | 48000 Hz |
| Audio Channels | 2 (stereo) |

### Sources
- [PSDevWiki — Remote Play (compatibility table)](https://www.psdevwiki.com/ps3/Remote_Play)
- [Vita DevWiki — Remote Play (compatibility table)](https://www.psdevwiki.com/vita/Remote_Play)
- [PlayStation Manual — About Remote Play](https://manuals.playstation.net/document/en/ps3/current/remoteplay/remoteplay.html)
- [GBAtemp — Remote Play Compatibility List](https://gbatemp.net/threads/remote-play-compatibility-list.321562/)
- [CFW Remote Play Compatibility List](https://comradesnarky.neocities.org/oldarchive/CFWRP)

---

## 5. The Key Extraction Problem on Stock PS3

### The Core Problem

To connect to a PS3 via Remote Play, our Android app needs three per-device secrets established during registration:

| Key | Size | Description |
|-----|------|-------------|
| **pkey** | 16 bytes | Shared secret generated by PS3 during registration |
| **psp_id** (OpenPSID) | 16 bytes | Hardware-bound device identifier |
| **psp_mac** | 6 bytes | Registered device's WiFi MAC address |

These are combined with three **universal static keys** (already known from Open-RP):
```
skey0: D1 B2 12 EB 73 86 6C 7B 12 A7 5E 0C 04 C6 B8 91
skey1: 1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F
skey2: 65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59
```

### Can We Extract Keys from the VAIO PC Side?

**This is the most promising approach for stock PS3.** After registering a VAIO (patched) client with a stock PS3, the keys exist on both sides. On the PS3 they are locked in xRegistry.sys (inaccessible without CFW/HEN). But on the PC...

**Where the VAIO client stores data on Windows:**

Installation directory: `C:\Program Files (x86)\Sony\Remote Play with PlayStation 3`

Key files: `VRP.exe`, `VRPUI.exe`, `VRPSDK.dll` (Themida-protected), `VRPMAPPING.dll`

Windows Registry (likely locations based on newer PS Remote Play):
- `HKEY_CURRENT_USER\Software\Sony Corporation\PS Remote Play`
- `HKEY_LOCAL_MACHINE\Software\Sony Corporation\PS Remote Play`

**The exact storage location of pkey/device ID on the PC side is NOT publicly documented.** This is a critical research gap. The VAIO client is Themida-protected (anti-tamper), making static reverse engineering difficult. However, the registration data MUST be stored somewhere accessible to the client for subsequent connections.

**Possible extraction approaches:**
1. **Process memory dump** — Run the VAIO client, register with PS3, dump process memory during/after registration. Search for the 16-byte pkey.
2. **API monitoring** — Use tools like Process Monitor (ProcMon) to track all file writes and registry writes during registration.
3. **Wireshark capture** — Capture the registration traffic on the WiFi direct connection. The 8-digit PIN likely seeds a key derivation, but the actual protocol needs analysis.
4. **AppData/ProgramData search** — After registration, search `%APPDATA%\Sony`, `%LOCALAPPDATA%\Sony`, `%PROGRAMDATA%\Sony` for new files.
5. **Windows Registry search** — After registration, diff the registry to find new entries under `HKCU\Software\Sony` or `HKLM\Software\Sony`.

### Can We Extract Keys WITHOUT Touching the PS3 at All?

**No.** The pkey is generated by the PS3 during registration. There is no way to obtain it without performing the registration handshake with the PS3 at least once. After that one-time registration, the keys can theoretically be extracted from the client device.

### The Firmware 2.80 Problem

Starting with PS3 firmware 2.80, Sony added **MAC address validation**. The PS3 now checks that the connecting device's MAC address matches the MAC that was registered during pairing. This is why Open-RP stopped working on FW 2.80+ — the PC's MAC didn't match the registered PSP's MAC.

The workaround was MAC address spoofing: change the PC's WiFi MAC to match the registered device's MAC.

**For our Android app:** We would need to either:
- Spoof the MAC address to match the registered device (requires root on Android)
- Register the Android device itself (requires implementing the registration protocol)
- Use the actual registered device's MAC, pkey, and OpenPSID

### Sources
- [Open-RP source: keys/keys.cpp, psp/orp.c](https://github.com/gbraad/open-rp)
- [PSDevWiki — XRegistry.sys](https://www.psdevwiki.com/ps3/XRegistry.sys)
- [GAMERGEN — ORP fonctionnelle sur 2.80](https://gamergen.com/forums/tutoriels-membres-ps3/open-remote-play-orp-fonctionnelle-sur-2-80-t252503.html)
- [PSDevWiki — Remote Play](https://www.psdevwiki.com/ps3/Remote_Play)

---

## 6. PSP Emulator (PPSSPP) for Key Extraction

### Can PPSSPP Fake a PSP Registration?

**Almost certainly not.** Here's why:

**OpenPSID in PPSSPP:**
- PPSSPP implements `sceOpenPSIDGetOpenPSID()` but it is marked **"UNTESTED"** in the source code (`HLE/sceOpenPSID.cpp`)
- The function likely returns a dummy/zero value or a hardcoded fake ID
- Even if PPSSPP returned a valid-looking OpenPSID, the PS3 likely validates it against known PSP hardware ID ranges

**Why it won't work for registration:**
1. **Registration requires WiFi direct or USB** — PPSSPP has no hardware WiFi to do WiFi direct pairing with the PS3
2. **Registration is a PS3-initiated protocol** — The PS3 presents a PIN, then establishes a direct connection. This is not something that happens over TCP/IP in a way an emulator could intercept
3. **MAC address** — PPSSPP doesn't have a PSP WiFi MAC address. Even if it used the host's MAC, it wouldn't be a valid PSP MAC range
4. **No PSP registry** — The registration data is stored in the PSP's system registry (`/CONFIG/PREMO`). PPSSPP doesn't fully emulate PSP system registry persistence

**What about the ORP_Exporter running on PPSSPP?**
The ORP_Exporter homebrew (from Open-RP) runs on a real PSP to export already-registered keys. Even if it ran on PPSSPP, there would be no keys to export because registration never happened through the emulator.

### OpenPSID Technical Details

From the PSP SDK (`pspsdk/src/openpsid/pspopenpsid.h`):
- `sceOpenPSIDGetOpenPSID()` reads the IDPS certificate from IDStorage leaves 0x101/0x102 or 0x121/0x122
- The OpenPSID is a 16-byte unique hardware identifier
- It is cryptographically certified in the PSP's IDStorage — cannot be trivially forged
- The PS3 may validate the OpenPSID format (company code, product code, etc.)

### Sources
- [PPSSPP Issue #4366 — sceOpenPSIDGetOpenPSID](https://github.com/hrydgard/ppsspp/issues/4366)
- [PSP SDK — pspopenpsid.h](https://github.com/pspdev/pspsdk/blob/master/src/openpsid/pspopenpsid.h)
- [PSP DevWiki — IDStorage](https://www.psdevwiki.com/psp/IDStorage)
- [PS-ConsoleId-wiki](https://github.com/TeamFAPS/PS-ConsoleId-wiki)

---

## 7. Official Sony Xperia Remote Play APK

### What Existed

In April 2010 (PS3 FW 3.30+), Sony released official Remote Play support for:
- Sony VAIO PCs (Windows application)
- Sony Xperia smartphones/tablets (Android application)

The Xperia app was exclusive to Sony Xperia devices (not available for general Android phones).

### How Xperia Registration Worked

Based on available documentation, the Xperia phone registered with the PS3 through the standard device registration flow:
1. PS3 menu: Settings > Remote Play Settings > Register Device > **Mobile Phone**
2. The PS3 would display a registration code
3. The Xperia app would take the code and pair via WiFi

The registration protocol was the same PREMO pairing protocol used by PSP/VAIO — an 8-digit PIN exchange followed by key material exchange over a direct connection.

### APK Details

- The original Xperia Remote Play APK was never widely distributed outside Xperia devices
- Package name was likely in the `com.sony.*` or `com.sonyericsson.*` namespace
- The APK was pre-installed on select Xperia models (Xperia X10, etc.) or available through Sony's app store
- **No known public decompilation/analysis of the Xperia RP APK exists in the public domain**

### What We Can Infer

Since the Xperia app registered as a "Mobile Phone" device type:
- It used the same pkey/MAC/device-ID registration scheme
- It likely used the phone's WiFi MAC as the registered MAC
- It likely generated or used some persistent device identifier (possibly IMEI-derived or Android ID) as the equivalent of OpenPSID
- The PS3 stored it in xRegistry under `/setting/premo/pspXX/` just like any other device

### Why This Matters for Our App

If we could obtain and decompile the original Xperia Remote Play APK, we could learn:
- What the app sends as its "device ID" (OpenPSID equivalent)
- How it handles the registration key exchange
- How it stores pkey locally on Android
- The exact PREMO protocol handshake for Android devices

This would be the most direct path to understanding how to build an Android Remote Play client that registers natively with a stock PS3.

### Current Status

The original Xperia PS3 Remote Play app is **defunct**. Sony's current "PS Remote Play" app (com.playstation.remoteplay) only supports PS4 and PS5. The PS3-era Xperia APK would need to be found in APK archives or extracted from an old Xperia device.

### Sources
- [Wikipedia — Remote Play](https://en.wikipedia.org/wiki/Remote_Play)
- [PSX-Place — Remote Play on Android](https://www.psx-place.com/threads/remote-play-on-android.26855/)
- [XDA Forums — PS3 Open Remote Play with Android](https://xdaforums.com/t/ps3-open-remote-play-with-android.842398/)
- [PlayStation Manual — Register Device (FW 3.15)](https://manuals.playstation.net/document/en/ps3/3_15/settings/registerdevice.html)

---

## 8. PS3 Remote Play via Internet on Stock Firmware

### Does Internet Mode Work on Stock PS3?

**YES.** Internet-based Remote Play is a fully supported stock PS3 feature. It has been available since PS3 firmware 1.80.

### Port Requirements

| Port | Protocol | Purpose |
|------|----------|---------|
| **9293** | TCP | Remote Play data (video, audio, control) |
| **9293** | UDP | Remote Play data and Remote Start |

### Setup Requirements

1. **Port forwarding** — Forward TCP 9293 (and optionally UDP 9293) to the PS3's local IP
2. **OR UPnP** — If the router supports UPnP, enable it for automatic port mapping
3. **Remote Play Connection Standby** — PS3 must be set to standby mode for remote connections
4. **Single router** — PS3 connected through two or more routers may cause issues

### Remote Start Feature

Since PS3 FW 2.00, Remote Start allows turning on a PS3 that is in standby mode:
- The PS3 must have "Remote Start" enabled in settings
- Uses UDP port 9293
- The PSP/Vita/device sends a wake-up signal over the internet
- PS3 wakes from standby, enters Remote Play mode

### Internet Connection Flow

1. Client sends UDP `SRCH` packet to PS3's public IP:9293
2. PS3 responds with 156-byte `RESP` packet (MAC, nickname, NPX ID)
3. Client initiates HTTP session on TCP 9293
4. PREMO handshake with `PREMO-Auth`, `PREMO-PSPID` headers
5. Video/audio streams begin

### Limitations

- **NAT type must be compatible** — Strict NAT (NAT Type 3) may prevent connections
- **ISP restrictions** — Some ISPs block or throttle port 9293
- **Dynamic IP** — PS3 uses dynamic IP; user needs to know their public IP or use DDNS
- **Latency** — Internet mode adds significant latency compared to LAN
- **30-second timeout** — If you navigate to a different PS3 application screen, RP disconnects after 30 seconds

### Sources
- [PlayStation Manual — Remote Play via Internet](https://manuals.playstation.net/document/en/ps3/current/remoteplay/remoteinternet.html)
- [PlayStation Manual — Remote Start](https://manuals.playstation.net/document/en/ps3/current/settings/remotestart.html)
- [PortForward — PlayStation 3 Remote Play](https://portforward.com/playstation-3-remote-play/)
- [PSDevWiki — Remote Play](https://www.psdevwiki.com/ps3/Remote_Play)

---

## Summary: Complete Picture for Stock PS3

### What IS Possible on Stock PS3 (No Modifications)

1. **Register a PSP** via USB or WiFi PIN
2. **Register a PS Vita** via on-screen code (FW 4.00+)
3. **Register a VAIO PC** (or patched non-VAIO PC) via WiFi PIN (FW 3.30+)
4. **Register a Sony Xperia phone** as a mobile device
5. **Navigate full XMB** remotely
6. **Play ALL PS1 classics** (digital and disc)
7. **Play ~30-40 officially compatible PS3 games**
8. **Stream media** (photos, music, video)
9. **Connect over the Internet** with port forwarding on TCP/UDP 9293
10. **Remote Start** (wake PS3 from standby)

### What is NOT Possible on Stock PS3

1. **Play arbitrary PS3 games** — Need CFW/HEN + webMAN-MOD to force RP on all games
2. **Play PS2 classics** — Not supported for RP at all
3. **Register non-Sony Android devices** — No official support
4. **Access xRegistry.sys** — Cannot extract keys without CFW/HEN/FTP

### The Critical Path for Our Android App

To build an Android app that works with stock PS3, we need to solve the **registration problem**. The options, ranked by feasibility:

**Option 1: Implement the Registration Protocol Natively (BEST BUT HARDEST)**
- Reverse-engineer how PSP/Vita/VAIO register with the PS3
- Implement the 8-digit PIN exchange and key derivation in our Android app
- Our app presents itself as a PSP/Vita/mobile device
- The PS3 does not deeply validate device type — it just needs valid protocol handshake
- Requires: capturing and analyzing registration traffic with Wireshark during a real VAIO pairing
- **Advantage: Zero dependencies, works with any stock PS3, no key extraction needed**

**Option 2: Extract Keys from VAIO PC After Registration (MODERATE)**
- Register a patched VAIO client with the stock PS3 (one-time)
- Extract the pkey, device ID, and MAC from the PC side
- Hardcode or import these into our Android app
- Spoof the registered MAC address on Android (may require root)
- **Advantage: PS3 stays 100% stock. Extraction is on PC side only.**
- **Disadvantage: Requires one-time PC setup. MAC spoofing may need root.**

**Option 3: Use PS3 HEN for Key Extraction Only (EASIEST TECHNICALLY)**
- Temporarily install HEN on PS3 (works on any PS3 model)
- Register VAIO client, then FTP dump xRegistry.sys
- Extract keys, remove HEN
- **Advantage: Well-documented, all tools exist**
- **Disadvantage: Requires temporary PS3 modification**

**Option 4: Find and Decompile Original Xperia Remote Play APK**
- If we can obtain the original Sony Xperia PS3 Remote Play APK
- Decompile to understand the registration flow from Android
- Re-implement in our app
- **Advantage: Direct Android reference implementation**
- **Disadvantage: APK may be impossible to find**

### Key Open Questions

1. **What exactly does the VAIO client store after registration, and WHERE?** This needs hands-on testing with Process Monitor / Registry diff on a Windows machine.

2. **Does the PS3 validate OpenPSID format on FW 4.90?** If we fabricate an OpenPSID, will the PS3 reject it? Historical evidence suggests FW 2.80+ added validation, but the extent is unknown.

3. **Can we capture the registration WiFi-direct traffic?** During VAIO registration, the PC connects directly to the PS3's ad-hoc WiFi. A third device monitoring that channel could capture the protocol.

4. **Is the Xperia Remote Play APK archived anywhere?** APKMirror, APKPure, WayBack Machine, or old Xperia XDA forums.

5. **What does the PS3 send back during registration?** Does it send the pkey in plaintext over the direct WiFi connection, or is it encrypted with the 8-digit PIN?
