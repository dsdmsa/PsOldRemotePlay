# Registering with a Stock PS3 from macOS/Linux -- No Windows Machine

Research on running the VAIO Remote Play client without Windows, bypassing it entirely, and the simplest path to register with a stock PS3 from macOS or Linux.

---

## 1. VAIO Remote Play in Wine/CrossOver on macOS/Linux

### Can the Patched VAIO Client Run Under Wine?

**Almost certainly NOT -- Themida is the blocker.**

The VAIO Remote Play client's core DLL (`VRPSDK.dll`) is packed with Themida anti-tamper protection. This is the single biggest obstacle to running it under Wine.

**Wine's COM support is adequate.** Wine includes `regsvr32` (the DLL Registration Server), and registering COM objects via `regsvr32 vrpsdk.dll` and `regsvr32 vrpmapping.dll` is a supported operation. Wine's COM implementation handles `DllRegisterServer` / `DllUnregisterServer` calls, and users have reported success registering various COM DLLs. The VAIO client's WMI manufacturer check (querying `Win32_BIOS.Vendor`, etc.) could potentially be patched or faked under Wine, since Wine's WMI implementation is incomplete and could be configured to return "Sony Corporation."

**However, Themida kills it.** Themida's anti-debug and anti-VM detection is known to be incompatible with Wine:

- Themida detects Wine as a debugger/VM environment and deliberately crashes the application (invalid memory read, anti-debug exceptions).
- Themida checks low-level Windows internals (PEB flags, NtQueryInformationProcess, timing-based detections) that Wine does not faithfully reproduce.
- The Themida developers themselves have historically needed to do compatibility work for Wine -- it is not something end users can easily fix.
- Tools like Themidie (x64dbg plugin) and ScyllaHide bypass Themida on real Windows but are not applicable under Wine.
- The `unlicense` tool (GitHub: ergrelet/unlicense) can dynamically unpack Themida 2.x/3.x binaries, but it requires running the binary on real Windows first to dump the unpacked DLL.

**CrossOver (commercial Wine) does not help.** CodeWeavers' compatibility database lists PS4 Remote Play (the modern app) with mixed results, but the PS3-era VAIO Remote Play is not listed at all. CrossOver uses the same Wine core and would face identical Themida issues.

**Theoretical workaround -- unpack Themida first:**
1. Use `unlicense` or `Magicmida` on a real Windows machine (or VM) to dump an unpacked `VRPSDK.dll`.
2. Replace the packed DLL with the unpacked one.
3. Run under Wine without Themida's anti-debug triggers.

This defeats the purpose of "no Windows machine" unless you can borrow one temporarily, but it could work if combined with a VM approach (see Section 2).

### Verdict

**Wine/CrossOver is NOT a viable path** for the VAIO client due to Themida. Do not pursue this unless someone first unpacks VRPSDK.dll on real Windows.

### Sources
- [Wine regsvr32 man page](https://gitlab.winehq.org/wine/wine/-/wikis/Man-Pages/regsvr32)
- [Registering DLLs with Wine](https://gist.github.com/rponte/b1f5dbeae466feab35d8)
- [Themida anti-debug analysis](https://medium.com/@reversing/analysis-of-oreans-themida-x86-version-2-3-5-10-anti-debugger-detections-8328ebd858c8)
- [Themidie bypass plugin](https://github.com/VenTaz/Themidie)
- [unlicense -- Themida unpacker](https://github.com/ergrelet/unlicense)
- [Magicmida -- Themida unpacker](https://github.com/Hendi48/Magicmida)
- [CodeWeavers -- PS Remote Play](https://www.codeweavers.com/compatibility/crossover/ps4-remote-play)

---

## 2. VAIO Remote Play in a Windows VM

### Can a Free Windows 7 VM Be Used?

**YES, but with significant caveats on Apple Silicon Macs.**

#### VM Options

| Platform | macOS Intel | macOS Apple Silicon | Linux |
|----------|-------------|---------------------|-------|
| **VirtualBox** | YES (free, x86 native) | NO (no x86 guest support on ARM) | YES (free, x86 native) |
| **UTM/QEMU** | YES (native x86) | YES (x86 emulated, SLOW) | YES (native x86) |
| **Parallels** | YES | YES (ARM Windows only, no x86 Win7) | N/A |
| **VMware Fusion** | YES | ARM Windows only | YES (Workstation) |

#### Windows 7 ISO Availability

Microsoft has removed Windows 7 ISOs from their download site. Options:
- **Internet Archive** -- Win7 ISOs are available on archive.org (legal gray area for evaluation use).
- **Windows 7 Evaluation** -- Microsoft previously offered 90-day evaluation ISOs; these are no longer officially available but are mirrored.
- **Windows 10/11** -- Microsoft still provides Windows 10/11 evaluation VMs. The VAIO client may work on Win10 in Windows 7 compatibility mode.

#### Apple Silicon (M1/M2/M3/M4) Specific Issues

- **UTM has a Windows 7 template** in its VM Gallery (`mac.getutm.app/gallery/windows-7`). It uses QEMU x86 emulation on ARM. Users report it works but is **extremely slow** (x86 emulation on ARM, no hardware acceleration).
- VirtualBox does NOT support x86 guests on Apple Silicon.
- Parallels and VMware Fusion only support ARM Windows (11), not x86 Windows 7.
- **Recommendation for Apple Silicon:** Use UTM with x86 emulation. Expect very slow performance, but the VAIO registration only needs to run once.

#### Intel Mac / Linux

- **VirtualBox** is the simplest free option. Install Win7 x86, install the VAIO client, register with PS3.
- Works well; this is a proven setup.

### The WiFi Passthrough Problem

**This is the CRITICAL issue for VM-based registration.**

During VAIO registration with a PS3:
1. The PS3 creates a WiFi access point (SSID like `PS3_XXXXXX` with WPA encryption).
2. The VAIO client **disconnects the PC from normal WiFi** and connects directly to the PS3's WiFi AP.
3. Key exchange happens over this direct WiFi connection.
4. After registration, the PC reconnects to normal WiFi.

**In a VM, the guest OS does not have direct WiFi hardware access.** VirtualBox/UTM provide network via NAT or bridged ethernet -- neither gives the VM the ability to scan for WiFi networks or connect to arbitrary SSIDs.

**Solutions:**

1. **USB WiFi adapter passthrough** (BEST):
   - Buy a cheap USB WiFi dongle ($10-15).
   - Pass it through to the VM using VirtualBox/UTM USB device filters.
   - The VM sees it as a native WiFi adapter.
   - VirtualBox: Settings > USB > Add Filter > select the USB WiFi adapter.
   - The host must NOT have a driver loaded for the dongle (uninstall/disable host driver first).
   - The VM's Windows 7 will use the USB WiFi adapter to connect to the PS3's AP.

2. **WiFi bridging from host** (UNRELIABLE):
   - On macOS, VirtualBox bridged mode with AirPort has inconsistent results.
   - WiFi specifications technically do not allow bridging in the standard way.
   - The VAIO client needs to switch WiFi networks mid-registration -- a bridged connection won't support this.

3. **Manual network trickery** (ADVANCED):
   - Connect the Mac/Linux host to the PS3's WiFi AP manually.
   - Bridge that connection to the VM.
   - The VAIO client may not detect this correctly since it expects to control the WiFi adapter.

**Bottom line:** A **USB WiFi adapter** passed through to the VM is the only reliable approach. This adds $10-15 to the cost but makes VM-based registration feasible.

### Step-by-Step for Linux with VirtualBox

1. Download and install VirtualBox.
2. Obtain a Windows 7 ISO (Internet Archive or evaluation).
3. Create a Win7 VM, install Windows.
4. Install VirtualBox Guest Additions + Extension Pack (for USB 2.0/3.0 passthrough).
5. Plug in a USB WiFi adapter. Add a USB filter for it in VM settings.
6. Boot the VM. Install WiFi adapter drivers in Windows 7.
7. Install the VAIO Remote Play software + VRPPatch.
8. Register COM objects: `regsvr32 vrpsdk.dll`, `regsvr32 vrpmapping.dll`.
9. On PS3: Settings > Remote Play Settings > Register Device.
10. In the VAIO client, enter the 8-digit PIN. The client will use the USB WiFi adapter to connect to the PS3's AP.
11. Registration completes. Extract keys (see key extraction research).

### Verdict

**VM + USB WiFi adapter is a viable but heavyweight approach.** It requires a one-time investment of ~$10-15 for a USB WiFi dongle and time setting up the VM. Works on Linux (Intel) or macOS (Intel). On Apple Silicon, UTM's x86 emulation is slow but may work for the one-time registration.

### Sources
- [UTM -- Windows 7 Gallery](https://mac.getutm.app/gallery/windows-7)
- [VirtualBox Downloads](https://www.virtualbox.org/wiki/Downloads)
- [VirtualBox USB WiFi Passthrough](https://doctorpapadopoulos.com/how-to-install-virtualbox-usb-wi-fi-adapter-inside-vms/)
- [VirtualBox WiFi Forum Discussion](https://forums.virtualbox.org/viewtopic.php?t=107872)

---

## 3. Wireshark Packet Capture of PS3 Registration

### Can We Capture Registration Traffic?

**YES, but the capture setup is non-trivial because registration uses a private WiFi AP, not the normal network.**

#### How Registration Works at the Network Level

1. PS3 enters registration mode and creates a **WiFi access point** (infrastructure mode, not ad-hoc).
   - SSID format: `PS3_XXXXXX` (numbers derived from PS3's MAC/serial).
   - Security: WPA-PSK.
   - The WPA key is **automatically generated** and stored in xRegistry on the PS3 side.
2. The registering device (VAIO/PSP) connects to this AP.
3. An HTTP-based key exchange happens over TCP on this private WiFi network.
4. After registration, the PS3 AP shuts down and both devices return to normal.

#### Capture Strategy 1: Monitor Mode on a Third Device

Use a Mac or Linux machine with a WiFi adapter that supports **monitor mode** to sniff the PS3's registration AP traffic.

**Problem:** The traffic is WPA-encrypted at the WiFi layer. To decrypt:
- You need the WPA-PSK key (the one the PS3 auto-generated for its AP).
- You also need to capture the WPA 4-way handshake (the EAPOL frames when the registering device joins).
- If you have the WPA-PSK, Wireshark can decrypt WPA traffic in real-time.

**The WPA key is the unknown.** During VAIO/PSP WiFi registration, the 8-digit PIN displayed on the PS3 is likely used to derive or communicate the WPA key for the temporary AP. But the exact mechanism is undocumented.

**Possible approach:**
1. Put your Mac's WiFi in monitor mode (macOS: `sudo /usr/libexec/airportd en0 sniff [channel]`).
2. While another device (PSP/Vita/VAIO) registers, capture all traffic on the PS3's AP channel.
3. You will see the encrypted WiFi frames + the EAPOL handshake.
4. If the WPA key is derived from the 8-digit PIN (e.g., PIN padded/hashed), you could brute-force it (only 10^8 = 100 million possibilities for a numeric PIN -- feasible).

#### Capture Strategy 2: Be the Registering Device and Capture Locally

If running the VAIO client in a VM:
1. The VM connects to the PS3's WiFi AP via USB WiFi adapter.
2. On the **host**, you cannot capture the USB WiFi adapter's traffic (it's passed through to the VM).
3. **Inside the VM**, run Wireshark to capture on the WiFi adapter.
4. This captures the decrypted application-layer traffic (HTTP/PREMO).
5. This is the **most practical approach** -- you see the actual key exchange in plaintext.

#### Capture Strategy 3: PSP/Vita Registration via Infrastructure Network

When a PSP or Vita connects to a PS3 **via the home network** (not WiFi direct), all traffic goes through the router. You can:
1. Set up ARP spoofing (e.g., with `bettercap`) on your Mac/Linux to man-in-the-middle the PSP/Vita traffic.
2. Capture with Wireshark on the Mac/Linux machine.
3. See the PREMO session handshake in plaintext HTTP.

**However:** This captures SESSION traffic (connection after registration), not REGISTRATION traffic. Registration uses the PS3's own WiFi AP, not the home network.

#### What Would We Learn from Captures?

From a **session** capture (post-registration):
- The full PREMO HTTP handshake (already documented in Open-RP source code).
- `PREMO-Auth`, `PREMO-PSPID`, `PREMO-Nonce` headers.
- Video/audio stream format confirmation.

From a **registration** capture:
- How the 8-digit PIN maps to a WiFi password or key derivation seed.
- The exact key exchange protocol: what data is sent, how `pkey` is transmitted or derived.
- Whether the PS3 validates the device type or OpenPSID during registration.
- Whether registration uses HTTP (like sessions) or a custom protocol.

### Verdict

**In-VM Wireshark capture during VAIO registration is the most practical way to capture the registration protocol.** This requires the VM + USB WiFi setup from Section 2, but yields the actual registration traffic in plaintext. This is a high-value investment -- understanding the registration protocol unlocks the ability to build a native registration tool.

### Sources
- [PSX-Place -- Packet Capture on PS3](https://www.psx-place.com/threads/packet-capture-on-ps3-2019-windows-10-edition.25082/)
- [Wireshark -- Sniff PS3/PS4 Packets](https://osqa-ask.wireshark.org/questions/32644/sniff-packets-on-a-ps3-4/)
- [NextGenUpdate -- Sniff PS3/PS4 Network Connections Wirelessly](https://nextgenupdate.com/forums/ps4/900269-how-sniff-ps3-ps4-network-connections-wireless.html)
- [Network Analysis of Sony Remote Play (IEEE)](https://ieeexplore.ieee.org/document/7543706/)
- [bettercap WiFi Module](https://www.bettercap.org/modules/wifi/)

---

## 4. Reverse Engineering the Registration Protocol Directly

### What We Already Know

From the Open-RP source code and PSDevWiki, the **post-registration session protocol** is fully documented:

- HTTP/1.1 based, port 9293 (TCP/UDP).
- Discovery: UDP `SRCH` broadcast, PS3 responds with 156-byte `RESP` packet.
- Session: `GET /sce/premo/session` with headers `PREMO-Auth`, `PREMO-PSPID`, `PREMO-Nonce`.
- Encryption: AES-128-CBC with `xor_pkey = pkey XOR skey0`, `IV = nonce XOR skey2`.
- Auth: `PREMO-Auth` = Base64(AES-CBC-Encrypt(PSP_MAC padded to 16 bytes, key=xor_pkey, IV=xor_nonce)).

**What is NOT documented:** The initial registration handshake -- how `pkey`, `psp_id`, and `psp_mac` are exchanged in the first place.

### The Registration Protocol -- What We Can Infer

From the PSP side (Open-RP `psp/orp.c`), registration stores these values in `/CONFIG/PREMO`:
- `ps3_name` -- PS3 nickname
- `ps3_mac` -- PS3 MAC address (6 bytes)
- `ps3_key` -- the pkey (16 bytes)

From the PS3 side (xRegistry at `/setting/premo/pspXX/`):
- `nickname` -- device name
- `macAddress` -- device MAC (6 bytes)
- `id` -- device OpenPSID (16 bytes)
- `keyType` -- key type
- `key` -- the pkey (16 bytes)

**The pkey is generated by the PS3** during registration. It is random (not derived from the PIN). The PIN is used to authenticate/secure the WiFi connection for the key exchange, not to derive the key itself.

### premo_plugin.sprx Analysis

The PS3-side registration handler lives in `premo_plugin.sprx` (located at `/dev_flash/vsh/module/premo_plugin.sprx`). Separately, `premo_game_plugin.sprx` handles the game whitelist for VAIO connections.

**Existing disassembly:** The GitHub repository `skrptktty/ps3-firmware-beginners-luck` contains a disassembly of `premo_game_plugin.sprx.asm` from the kmeaw CFW (old firmware ~3.55). This contains the VAIO game whitelist logic but NOT the registration handler (that's in `premo_plugin.sprx`).

**PSDevWiki documents premo_plugin.sprx functions:**
- Audio/video configuration parameters.
- Execution modes (VSH, game, PS1).
- Codec selection (M4V, AVC, M4HD).
- TitleID whitelist check (for VAIO connections).
- References to xRegistry paths `/setting/premo/audioConfig`, `/setting/premo/pspXX/`.

**No public decompilation of the registration handler exists.** The registration-specific code within `premo_plugin.sprx` has not been publicly analyzed or documented.

### How to Decompile premo_plugin.sprx

1. **Extract from firmware:** PS3 firmware PUP files can be extracted using PUP Extractor tools. The SPRX files within are encrypted.
2. **Decrypt:** Use SCETool (or its Linux clone OSCETool) with the correct firmware keys to decrypt the SPRX to an ELF.
   - Keys for firmwares up to 4.92 are available on PSDevWiki.
   - Command: `scetool --decrypt premo_plugin.sprx premo_plugin.elf`
3. **Decompile:** Load the ELF into Ghidra with the PPU/Cell BE processor module. The PS3 uses PowerPC64 (Cell PPU).
   - Ghidra has excellent PPC64 decompilation support.
   - RPCS3 project has Ghidra scripts for PS3 module analysis.
4. **Analyze:** Search for:
   - xRegistry API calls (read/write of `/setting/premo/` paths).
   - WiFi AP setup functions.
   - Key generation (look for random number generation, AES calls).
   - PIN validation logic.
   - Device type handling code.

### Verdict

**Decompiling premo_plugin.sprx in Ghidra is the most direct way to understand the registration protocol**, and it can be done entirely on macOS or Linux. This requires: extracting PS3 firmware, decrypting the SPRX, and reverse engineering PowerPC64 code. It is a significant effort but yields definitive answers about what the PS3 expects during registration.

### Sources
- [Open-RP source: psp/orp.c](https://github.com/gbraad/open-rp/blob/master/psp/orp.c)
- [PSDevWiki -- Premo plugin](https://www.psdevwiki.com/ps3/Premo_plugin)
- [PSDevWiki -- Dev Tools (SCETool)](https://www.psdevwiki.com/ps3/Dev_Tools)
- [SCETool on GitHub](https://github.com/naehrwert/scetool)
- [OSCETool -- Linux SCETool clone](https://github.com/spacemanspiff/oscetool)
- [PSDevWiki -- Keys](https://www.psdevwiki.com/ps3/Keys)
- [ps3-firmware-beginners-luck (disassembled SPRX files)](https://github.com/skrptktty/ps3-firmware-beginners-luck)
- [PSX-Place -- SPRX Decryption](https://www.psx-place.com/threads/sprx-decryption.28442/)
- [PSX-Place -- Best Way to View SPRX Source Code](https://www.psx-place.com/threads/solved-best-way-to-view-file-source-code-selfs-sprx-eboots-ect.21943/)

---

## 5. Building Our Own Registration Tool

### What Would a Custom Registration Tool Need to Do?

To emulate a PSP/VAIO during registration with a stock PS3:

1. **Connect to the PS3's WiFi AP** -- The PS3 creates a temporary AP. Our tool must join it.
2. **Authenticate using the 8-digit PIN** -- The PIN displayed on PS3 is used somehow to authenticate the registration. It may serve as a WPA passphrase or seed a challenge-response.
3. **Exchange identity information** -- Send our device's MAC address and a device ID (OpenPSID equivalent).
4. **Receive the pkey** -- The PS3 generates and sends the 16-byte shared secret.
5. **Store the registration data** -- Save pkey, PS3 MAC, PS3 nickname locally.

### What We Need to Fake

| Item | PSP Value | What We Could Use |
|------|-----------|-------------------|
| **MAC address** | PSP WiFi MAC (00:xx:xx:xx:xx:xx) | Any MAC; after FW 2.80 PS3 validates it on reconnect, but we'd use the same MAC for both registration and session |
| **OpenPSID** | 16-byte hardware-bound ID from IDStorage | Unknown if PS3 validates format. Could try: random 16 bytes, or a known-good PSPID format from PS-ConsoleId-wiki |
| **Device type** | Depends on registration menu choice | PS3's "Register Device" menu has PSP/Vita/Mobile/PC options. Each may trigger slightly different server behavior |
| **Protocol handshake** | Unknown | This is the key unknown. Must be reverse engineered from premo_plugin.sprx or captured via Wireshark |

### The OpenPSID Validation Question

After PS3 firmware 2.80, Sony added device validation. The key question: **does the PS3 validate the OpenPSID format, or just store whatever it receives?**

From PS-ConsoleId-wiki (TeamFAPS), OpenPSID has structure:
- 2 bytes: Company code
- 2 bytes: Product code
- 12 bytes: Serial/unique data

If the PS3 checks that the company/product codes match known Sony device ranges, we would need a valid-looking PSPID. If it just stores the value, any 16 bytes work.

**The Vita pairing hint:** PSDevWiki notes that "It is possible to pair PSVita with a PS3 on lower Firmware than 4.00 by pairing as mobile phone / PC / PSP." This means the PS3's device-type menu is largely cosmetic -- the underlying registration protocol is similar or identical across device types. This is encouraging for building a universal registration tool.

### Platform Options for a Custom Registration Tool

| Platform | WiFi Control | Feasibility |
|----------|-------------|-------------|
| **Android phone** | Can programmatically scan and join WiFi networks (with location permission) | BEST -- portable, has WiFi, can be written in Kotlin/Java |
| **Linux laptop** | Full WiFi control via `wpa_supplicant` / `nmcli` | GOOD -- full control, easy to develop |
| **macOS** | Limited WiFi control (CoreWLAN framework, requires entitlements) | MODERATE -- Apple restricts programmatic WiFi joining |
| **Raspberry Pi** | Full Linux WiFi control | GOOD -- cheap, dedicated tool |

### Minimum Viable Registration Tool

If we reverse engineer the registration protocol (from Section 4), a registration tool would be:

```
1. Scan for PS3's WiFi AP (SSID matching PS3_*)
2. Connect to it (using PIN-derived WPA key or PIN in-band)
3. Perform PREMO registration handshake (HTTP on port 9293?)
4. Receive pkey from PS3
5. Save {pkey, ps3_mac, device_mac, device_id} to config file
6. Disconnect from PS3 AP
7. Export config in Open-RP format (.orp file)
```

This tool would then feed keys to our Android Remote Play client or Open-RP.

### Verdict

**Building a custom registration tool is the ideal end goal** but requires first understanding the registration protocol (Section 4). An Android app is the best platform since it already has WiFi control and is the target platform for our Remote Play client. The registration could be built directly into the Android app as a "pair with PS3" feature.

### Sources
- [PS-ConsoleId-wiki](https://github.com/TeamFAPS/PS-ConsoleId-wiki)
- [PSDevWiki -- OpenPSID](https://www.psdevwiki.com/ps3/OpenPSID)
- [PSP Developer Wiki -- IDStorage](https://www.psdevwiki.com/psp/IDStorage)
- [Vita DevWiki -- Remote Play (cross-device pairing)](https://www.psdevwiki.com/vita/Remote_Play)

---

## 6. Can the Android App Itself Handle Registration?

### The 8-Digit PIN Protocol

When a PS3 is put into registration mode:
1. PS3 displays an 8-digit PIN on the TV screen.
2. PS3 creates a WiFi access point (SSID like `PS3_XXXXXX`).
3. The registering device enters the PIN and connects to the PS3's AP.
4. Key exchange happens.
5. Both devices store the shared secrets.

**The 8-digit PIN is NOT a PSN authentication token.** It is a local, temporary code for the WiFi registration. No internet connection is required. No PSN account validation happens during registration (PSN is only checked when using Remote Play over the Internet).

### Could an Android App Register Directly?

**YES, in principle.** Android can:
- Scan for WiFi networks programmatically (`WifiManager.startScan()`).
- Connect to a specific SSID with a specific WPA key (`WifiNetworkSpecifier` on Android 10+, or `WifiConfiguration` on older).
- Open TCP/HTTP connections on the local network.
- Generate and store cryptographic keys.

The Android app would:
1. Prompt user to put PS3 in registration mode.
2. Scan for `PS3_*` SSID.
3. Connect using the PIN (as WPA key? or as part of the HTTP handshake? -- unknown).
4. Perform the registration handshake.
5. Store pkey, PS3 MAC, device ID locally.
6. Reconnect to normal WiFi.
7. Begin Remote Play session using the registered credentials.

### The Missing Piece: How is the PIN Used?

Two possibilities:

**Possibility A: PIN is the WPA-PSK passphrase.** The PS3's temporary AP uses the 8-digit PIN as the WPA passphrase. The registering device connects to the AP using the PIN as WiFi password, then the key exchange happens in cleartext HTTP over the local connection. This is the simplest mechanism and is consistent with how consumer electronics pairing typically works.

**Possibility B: PIN is used in an in-band challenge-response.** The PS3's AP uses a fixed/derivable WPA key, and the PIN is sent as part of an HTTP handshake to authenticate the registration. This is more complex but provides an additional layer of authentication.

If **Possibility A** is correct, the Android app's registration flow becomes straightforward:
1. Connect to PS3 AP using PIN as WPA password.
2. Make HTTP request to PS3's IP (likely 192.168.x.1) on port 9293.
3. Send device info, receive pkey.

Determining which model is correct requires either Wireshark capture (Section 3) or premo_plugin.sprx decompilation (Section 4).

### The Xperia Precedent

The Sony Ericsson Aino (2009) and later Sony Xperia phones could register with PS3 natively using the "Mobile Phone" device type. This proves that a phone CAN register with a PS3 over WiFi without USB. The Aino was NOT an Android phone (it ran a proprietary OS with Java ME), so the registration protocol is not Android-specific -- it's a network protocol any device can implement.

The PS3 manual for firmware 3.15+ confirms: "Register Device" > "Mobile Phone" exists as an option. The registration flow is identical to VAIO: PS3 displays a number, enter it on the phone.

**No public decompilation or analysis of the Xperia/Aino Remote Play client exists.** The Xperia APK was not on Android (the Aino used Java ME). Later Xperia Android phones may have had a Remote Play APK, but it has not been found on APK archive sites. The current `com.playstation.remoteplay` APK on APKPure/APKMirror is the PS4/PS5 version only.

### Verdict

**An Android app that handles its own registration is the ultimate goal and is technically feasible.** It eliminates ALL dependencies (no Windows, no PSP, no VAIO). The blocker is understanding how the PIN maps to WiFi authentication and what the registration handshake looks like at the protocol level. Solving this (via Wireshark capture or SPRX decompilation) unlocks a completely self-contained Android PS3 Remote Play client.

### Sources
- [PS3 Manual -- Register Device (FW 3.15)](https://manuals.playstation.net/document/en/ps3/3_15/settings/registerdevice.html)
- [Sony Ericsson Aino -- PS3 Remote Play Phone](https://newatlas.com/sony-ericsson-aino/12804/)
- [NeoGAF -- Sony Ericsson Aino Discussion](https://www.neogaf.com/threads/sony-eriscsson-aino-phone-with-ps3-remote-play.363145/)
- [PlayStation Blog -- Introducing Aino](https://blog.playstation.com/archive/2009/09/01/introducing-aino-the-mobile-phone-with-remote-play)
- [PSX-Place -- Remote Play on Android](https://www.psx-place.com/threads/remote-play-on-android.26855/)

---

## 7. PS3 Firmware Decryption for premo_plugin Analysis

### Tool Chain for Extracting and Decompiling premo_plugin.sprx

**All of these tools run on macOS and Linux.** No Windows required.

#### Step 1: Obtain PS3 Firmware Update

Download `PS3UPDAT.PUP` from PlayStation's update server (any firmware version 3.30+ to 4.91). These are publicly available unencrypted downloads.

#### Step 2: Extract PUP File

**Tool:** PUP Extractor / `pup_extractor`
- Extracts the PUP into individual update packages (`.pkg` files).
- The dev_flash contents are in one of these packages.
- Available on: PSDevWiki Dev Tools page, various GitHub repos.

**Alternative:** RPCS3 (PS3 emulator) can extract PUP files. RPCS3 runs natively on macOS and Linux.
- Command: Place `PS3UPDAT.PUP` in RPCS3's update directory, and RPCS3 will install/extract it.

#### Step 3: Decrypt SPRX Files

**Tool:** SCETool / OSCETool
- [SCETool](https://github.com/naehrwert/scetool) -- Original, compiles on Linux/macOS.
- [OSCETool](https://github.com/spacemanspiff/oscetool) -- Linux-focused clone, same usage.
- Requires firmware-specific decryption keys (available on PSDevWiki Keys page for all firmware versions through 4.92).

```bash
# Set environment for keys
export PS3=/path/to/keys/directory

# Decrypt SPRX to ELF
scetool --decrypt premo_plugin.sprx premo_plugin.elf
```

**SELF/SPRX Decrypter v0.6** (by Heden DeLiGhT) is another option for decrypting SPRX files.

#### Step 4: Decompile in Ghidra

**Tool:** [Ghidra](https://ghidra-sre.org/) (NSA's reverse engineering tool, free, runs on macOS/Linux)
- Import the decrypted ELF file.
- Set processor to **PowerPC 64-bit, Big Endian** (Cell PPU).
- Ghidra will auto-analyze and generate decompiled C pseudocode.
- Search for string references: `"premo"`, `"register"`, `"/setting/premo/"`, `"key"`, `"psp"`, `"wlan"`, `"pin"`.
- Look for xRegistry API calls that write to `/setting/premo/pspXX/key`.
- Look for random number generation (the pkey generation).
- Look for WiFi AP setup functions.
- Look for HTTP server setup on port 9293.

#### Step 5: Cross-Reference with premo_game_plugin.sprx

The existing disassembly at `skrptktty/ps3-firmware-beginners-luck` contains `premo_game_plugin.sprx.asm` from kmeaw CFW (~FW 3.55). While this is the game whitelist plugin (not the registration handler), it shows how premo plugins interact with xRegistry and the VSH module system. Cross-referencing shared function calls and data structures will accelerate analysis of `premo_plugin.sprx`.

### What We Expect to Find

Based on everything we know, `premo_plugin.sprx` likely contains:

1. **WiFi AP creation code** -- Sets up the PS3 as a WiFi access point during registration mode.
2. **PIN generation** -- Generates the random 8-digit PIN and displays it via VSH.
3. **PIN usage** -- Either uses PIN as WPA-PSK passphrase, or validates it in an HTTP handshake.
4. **pkey generation** -- Calls a PRNG/crypto function to generate 16 random bytes.
5. **Key exchange handler** -- An HTTP or custom protocol server that receives device info and sends pkey.
6. **xRegistry writer** -- Stores the registered device's MAC, OpenPSID, and pkey in `/setting/premo/pspXX/`.
7. **Device type handler** -- May or may not differentiate between PSP/Vita/VAIO/Mobile device types.

### Verdict

**This is a fully macOS/Linux workflow.** Every tool in the chain (PUP extractor, SCETool, Ghidra) runs natively on macOS and Linux. No Windows needed. The output would be a complete understanding of the registration protocol, enabling us to build the custom registration tool from Section 5.

### Sources
- [PSDevWiki -- Dev Tools](https://www.psdevwiki.com/ps3/Dev_Tools)
- [PSDevWiki -- Keys](https://www.psdevwiki.com/ps3/Keys)
- [SCETool (GitHub)](https://github.com/naehrwert/scetool)
- [OSCETool (GitHub)](https://github.com/spacemanspiff/oscetool)
- [SELF/SPRX Decrypter v0.6](https://store.brewology.com/ahomebrew.php?brewid=223)
- [PUP Extractor](https://ps3.brewology.com/downloads/download.php?id=2173&mcid=4)
- [PSX-Place -- 4.84 Keys for SCETool](https://www.psx-place.com/threads/solved-4-84-keys-for-scetool-fw-files-to-decrypt.22781/)
- [Ghidra](https://ghidra-sre.org/)

---

## 8. Alternative: Capture Traffic from a Phone (Xperia/Aino)

### Mobile Device Registration Protocol

The Sony Ericsson Aino (2009, Java ME phone) and Sony Xperia smartphones could register with PS3 as "Mobile Phone" device type. Key facts:

- **The Aino was NOT Android.** It was a feature phone running Sony Ericsson's proprietary OS with Java ME capabilities. Its Remote Play feature was built into the phone's native firmware, not an installable APK.
- **Xperia Android phones** later received Remote Play support (starting ~2010 with PS3 FW 3.30). The APK was pre-installed or available via Sony's app store, NOT the public Play Store.
- **Registration was identical in user flow:** PS3 displays a number, enter it on the phone.

### Is the "Mobile Phone" Protocol Different from PSP/VAIO?

**Likely not.** Evidence:
- PSDevWiki states that a Vita can register as "mobile phone" or "PSP" on older firmware, confirming the underlying protocol is device-agnostic.
- The PS3 stores all registered devices in the same xRegistry structure (`/setting/premo/pspXX/`) regardless of device type.
- The same fields (MAC, ID, keyType, key) are stored for all device types.
- The PREMO session protocol is identical regardless of how the device registered.

The device type selection in the PS3 menu likely only affects:
- UI text shown on the PS3 during registration.
- Possibly the default video resolution/codec offered.
- The VAIO-specific game whitelist (only applied to devices registered as "PC").

### Can Any Android Phone Register (Not Just Xperia)?

The Xperia APK's device-specific restriction was enforced **on the phone side**, not the PS3 side (similar to how the VAIO check is PC-side only). If we could obtain and decompile the Xperia Remote Play APK, we could remove the device check and run it on any Android phone. However:

- The APK has not been found on public archive sites (APKPure, APKMirror, Uptodown all only have the PS4/PS5 `com.playstation.remoteplay`).
- The Aino's implementation was in native code, not an APK at all.
- A third-party "PS3 Remote Play 2019" app exists on APKCombo but appears to be unofficial/fake.

### Using an Xperia Phone as a Capture Device

If you can find a used Sony Xperia phone with the original Remote Play APK still installed:
1. Register it with the PS3 using the "Mobile Phone" option.
2. Use `tcpdump` on the rooted Xperia (or ARP spoofing from another device) to capture the registration traffic.
3. Analyze the capture to understand the registration protocol.

**However**, this requires finding a specific old phone with specific software -- arguably harder than setting up a VM.

### Verdict

**The "mobile phone" registration path confirms that the protocol is device-agnostic**, which is encouraging. But finding an actual Xperia with the original PS3 Remote Play APK is impractical. The knowledge that the protocol is the same across device types means we only need to reverse engineer it once (via premo_plugin.sprx decompilation or Wireshark capture from any device type).

### Sources
- [Sony Ericsson Aino -- New Atlas](https://newatlas.com/sony-ericsson-aino/12804/)
- [Wikipedia -- Sony Ericsson Aino](https://en.wikipedia.org/wiki/Sony_Ericsson_Aino)
- [PlayStation Blog -- Introducing Aino](https://blog.playstation.com/archive/2009/09/01/introducing-aino-the-mobile-phone-with-remote-play)
- [NeoGAF -- Aino Discussion](https://www.neogaf.com/threads/sony-eriscsson-aino-phone-with-ps3-remote-play.363145/)
- [XDA Forums -- PS3 Remote Play on Android](https://xdaforums.com/t/ps3-remote-play-on-android.705139/)
- [PSDevWiki -- Vita Remote Play (cross-device pairing)](https://www.psdevwiki.com/vita/Remote_Play)

---

## Summary: Ranked Approaches for No-Windows Registration

### Approach 1: Decompile premo_plugin.sprx + Build Custom Registration Tool
**Difficulty: HIGH | Windows needed: NO | Hardware needed: NONE beyond Mac/Linux**

1. Download PS3 firmware PUP.
2. Extract and decrypt premo_plugin.sprx (SCETool on macOS/Linux).
3. Decompile in Ghidra (macOS/Linux).
4. Understand the registration protocol completely.
5. Build a registration tool (Android app, Python script, or native tool).
6. Register directly with PS3 from Mac/Linux/Android.

**Pros:** Completely self-contained, no Windows ever, produces reusable knowledge, enables building the registration into our Android app.
**Cons:** Significant reverse engineering effort (days to weeks). Requires PowerPC64 RE skills.
**This is the BEST long-term investment.**

### Approach 2: Windows VM + USB WiFi Adapter (One-Time Registration)
**Difficulty: MODERATE | Windows needed: YES (in VM) | Hardware needed: USB WiFi dongle (~$10-15)**

1. Install VirtualBox (Linux) or UTM (macOS) with Windows 7/10.
2. Pass through a USB WiFi adapter to the VM.
3. Run patched VAIO client, register with PS3.
4. Extract keys from the VAIO client (ProcMon/RegShot/Wireshark inside VM).
5. Use keys in our Android app.

**Pros:** Uses known-working software, reliable.
**Cons:** Requires USB WiFi dongle. Requires running Windows (even in a VM). Slow on Apple Silicon. One-time-only -- keys extracted are specific to one PS3.
**This is the MOST PRACTICAL short-term solution.**

### Approach 3: Windows VM + Wireshark Capture (Understand the Protocol)
**Difficulty: MODERATE-HIGH | Windows needed: YES (in VM) | Hardware needed: USB WiFi dongle**

Same VM setup as Approach 2, but run Wireshark inside the VM during registration to capture the full protocol. This yields understanding of the registration handshake, enabling Approach 1 without Ghidra work.

**Pros:** Captures the actual protocol in action. Combined with Approach 2, you get both working keys AND protocol understanding.
**Cons:** Same hardware requirements as Approach 2.
**Do this ALONGSIDE Approach 2 -- zero additional cost.**

### Approach 4: Borrow a Windows Machine Briefly
**Difficulty: LOW | Windows needed: YES (someone else's) | Hardware needed: NONE**

If a friend, library, or workplace has a Windows laptop with WiFi:
1. Install the VAIO client + VRPPatch (portable, no reboot needed after initial install).
2. Register with PS3 (takes 2 minutes).
3. Run ProcMon/RegShot to find where keys are stored.
4. Extract keys.
5. Uninstall everything.

**Pros:** Simplest approach, no VM complexity, no hardware to buy.
**Cons:** Requires access to someone else's Windows PC with WiFi.

### Approach 5: Find and Analyze the Xperia Remote Play Client
**Difficulty: UNKNOWN | Windows needed: NO | Hardware needed: possibly an old Xperia phone**

Search harder for the Xperia PS3 Remote Play APK/firmware. If found, decompile to understand the Android/Java registration flow.

**Pros:** Would give us a direct Android reference implementation.
**Cons:** The APK may genuinely be lost. The Aino version was native code (not Java). Very uncertain.

---

## Key Open Questions (Prioritized)

1. **How does the 8-digit PIN relate to WiFi authentication?** Is it the WPA passphrase for the PS3's temporary AP, or is it sent in-band? This is the single most important unknown.

2. **Does the PS3 validate OpenPSID format during registration?** If not, we can use any 16-byte value as our device ID. If yes, we need a valid-looking PSPID.

3. **Is the registration protocol HTTP-based (like sessions)?** If the registration handshake uses HTTP on port 9293 with PREMO headers, it would be consistent with the session protocol and much easier to implement.

4. **Where exactly does the VAIO client store keys on Windows?** Still undocumented. ProcMon/RegShot during registration would answer this definitively.

5. **Can premo_plugin.sprx be successfully decompiled in Ghidra?** PS3 SPRX decompilation is well-established for other modules -- premo_plugin should be no different, but it has not been done publicly.

---

## Recommended Action Plan

**Phase 1 (Immediate, no hardware needed):**
- Download PS3 firmware PUP (4.90 or 4.91).
- Extract and decrypt premo_plugin.sprx using SCETool on macOS/Linux.
- Load into Ghidra and begin reverse engineering the registration handler.
- Search for string references to "register", "premo", "pin", "key", "wlan", "psp".

**Phase 2 (If Ghidra analysis is slow, parallel track):**
- Set up a Windows VM (VirtualBox on Linux, or UTM on macOS).
- Obtain a USB WiFi adapter.
- Install patched VAIO client in the VM.
- Run Wireshark inside the VM AND ProcMon simultaneously.
- Register with PS3 and capture everything.
- Extract keys AND understand the protocol from the capture.

**Phase 3 (Build the tool):**
- Implement registration protocol in the Android app based on findings from Phase 1 or 2.
- Test registration with a stock PS3.
- If successful, the Android app becomes fully self-contained -- no Windows, no PSP, no VAIO needed at all.
