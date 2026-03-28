# PS3 Remote Play Research Index

Reference materials for building a PS3 Remote Play Android client.

**PROJECT STATUS:** See [STATUS_AND_NEXT_STEPS.md](./STATUS_AND_NEXT_STEPS.md) for complete status, what's working, what's blocked, and what help is needed.

---

## Source Code Repositories — PS3 Remote Play Protocol

### [open-rp-original/](./open-rp-original/)
**Open Remote Play — Original** (C/C++, GPL-2.0)
The definitive open-source PS3 Remote Play client. Full PREMO protocol implementation including discovery, session management, video/audio decoding, controller input, and AES encryption. Created ~2009 by dashhacker after reverse-engineering the PSP-to-PS3 protocol. **This is the single most important reference.**
- Source: https://github.com/shoobyban/open-rp

### [open-rp-gbraad/](./open-rp-gbraad/)
**Open Remote Play — gbraad fork** (C/C++, GPL-2.0)
Auto-exported from Google Code. 214 commits. Includes the `keys/` directory with key extraction tools. Most complete history.
- Source: https://github.com/gbraad/open-rp

### [open-rp-domsblog/](./open-rp-domsblog/)
**Open Remote Play — domsblog fork** (C/C++, GPL-2.0)
Fork with 243 commits — the most active fork. 96.4% C, 3.4% C++.
- Source: https://github.com/domsblog/open-rp

### [open-remote-py/](./open-remote-py/)
**open-remote-py** (Python, placeholder)
Nascent Python PS3 Remote Play client. Created November 2024. Contains only README — no implementation yet. Shows recent interest in the project.
- Source: https://github.com/queenkjuul/open-remote-py

### [ps3-pro-remote-play/](./ps3-pro-remote-play/)
**PS3-Pro/Remote-Play** (Binary patches)
Patched Sony VAIO Remote Play binaries (`VRPSDK.dll`, `VRPMAPPING.dll`) to work on non-VAIO PCs. Not a protocol implementation — useful for running the official VAIO client on any Windows PC for testing/Wireshark captures.
- Source: https://github.com/PS3-Pro/Remote-Play

### [chiaki-ng/](./chiaki-ng/)
**chiaki-ng** (C++/Qt, PS4/PS5 only)
Active fork of Chiaki for PS4/PS5 Remote Play. Does NOT support PS3 (different protocol), but is an excellent **architectural reference** for building a modern cross-platform remote play client: session management, video decode pipeline, controller input, audio handling.
- Source: https://github.com/streetpea/chiaki-ng

### [webman-mod/](./webman-mod/)
**webMAN-MOD** (C, PS3 homebrew plugin)
PS3 custom firmware plugin. Includes `/play.ps3` command to launch the Remote Play server from XMB. Essential for forcing Remote Play on all games (not just officially supported ones). Contains `premo_plugin.sprx` integration.
- Source: https://github.com/aldostools/webMAN-MOD

### [ps3fromwin/](./ps3fromwin/)
**PS3FromWin** (Batch scripts)
Automation for waking PS3 via Bluetooth from Windows through a Linux intermediary and capturing HDMI. Not network remote play, but shows alternative approach to PS3 remote access.
- Source: https://github.com/Vegz78/PS3FromWin

---

## Protocol Documentation

### [docs-psdevwiki/protocol-from-source-code.md](./docs-psdevwiki/protocol-from-source-code.md)
**PREMO Protocol Complete Reference** — Extracted from Open-RP source code analysis. Contains: all HTTP endpoints, discovery packet format, stream packet headers (32-byte struct), encryption key derivation (AES-128-CBC), controller input mapping (PSP pad + DualShock 3), video/audio codec details, all PREMO HTTP headers, config file format (.orp).

### [docs-psdevwiki/ps3-remote-play.html](./docs-psdevwiki/ps3-remote-play.html)
**PS3 Developer Wiki — Remote Play** — Community-maintained technical documentation on PS3 Remote Play internals.
- Source: https://www.psdevwiki.com/ps3/Remote_Play

### [docs-psdevwiki/premo-plugin.html](./docs-psdevwiki/premo-plugin.html)
**PS3 Developer Wiki — Premo Plugin** — Documentation on the PS3-side `premo_plugin.sprx` and `premo_game_plugin.sprx` modules. Codec modes (m4v, avc, m4hd), video parameters, plugin system.
- Source: https://www.psdevwiki.com/ps3/Premo_plugin

### [docs-psdevwiki/vita-remote-play.html](./docs-psdevwiki/vita-remote-play.html)
**Vita Developer Wiki — Remote Play** — Documentation on PS Vita's Remote Play implementation. May reveal protocol differences/improvements from the PSP era.
- Source: https://www.psdevwiki.com/vita/Remote_Play

---

## Research: Full PS3 UI (XMB) Streaming

### [docs-psdevwiki/ps3-xmb-ui-streaming.md](./docs-psdevwiki/ps3-xmb-ui-streaming.md)
**PS3 Full XMB UI Streaming Analysis** — Confirms that **YES**, the PS3 streams the entire XMB UI by default via "VSH" exec mode. The protocol has three modes: VSH (full UI), game, and PS1. Some XMB settings are hidden during remote play but can be unlocked by patching `premo_plugin.sprx`. Our app can serve as a complete PS3 remote control — browse menus, navigate settings, access the store, launch games. Also compares with PS4/PS5 Chiaki approach.

---

## Research: Upscaling & Frame Generation

### [docs-upscaling-framegeneration/fsr-and-frame-generation-research.md](./docs-upscaling-framegeneration/fsr-and-frame-generation-research.md)
**FSR Upscaling & Frame Generation — Full Research** — Comprehensive analysis covering: FSR 1.0 for video (feasible, proven in mpv/YouTube/VLC), mobile-optimized shader implementations, Snapdragon GSR, Arm ASR, frame interpolation options (SVPlayer MEMC, Mob-FGSR, RIFE), ML upscaling limitations on mobile, and the recommended decode → deblock → FSR upscale → display pipeline with latency budget.

---

## Upscaling & Frame Generation Source Code

### [fidelityfx-fsr1/](./fidelityfx-fsr1/)
**AMD FidelityFX FSR 1.0** (C/HLSL/GLSL, MIT license)
The original FSR 1.0 release with EASU + RCAS shaders. Lightweight, no dependencies on game engine. **Most directly applicable to our video upscaling use case.**
- Source: https://github.com/GPUOpen-Effects/FidelityFX-FSR

### [fidelityfx-sdk/](./fidelityfx-sdk/)
**AMD FidelityFX SDK** (C/C++, MIT license)
Full SDK containing FSR 1.0, 2.x, 3.0, and 4.0 (ML). Larger codebase, DX12-focused. Reference for all FSR versions.
- Source: https://github.com/GPUOpen-LibrariesAndSDKs/FidelityFX-SDK

### [fsr-mobile-shaders/](./fsr-mobile-shaders/)
**FSR Mobile-Optimized GLSL Shaders** (MIT license)
Two key files ready to use on Android:
- `fsr_mobile_optimized.glsl` — Single-pass EASU+RCAS combined, half-precision math, OpenGL ES 3 compatible. Source: https://gist.github.com/atyuwen/78d6e810e6d0f7fd4aa6207d416f2eeb
- `fsr_mpv_video.glsl` — FSR 1 adapted specifically for video playback (mpv), works on luma plane, supports up to 4x upscale. Source: https://gist.github.com/agyild/82219c545228d70c5604f865ce0b0ce5

### [snapdragon-gsr/](./snapdragon-gsr/)
**Snapdragon Game Super Resolution** (GLSL, BSD-3-Clause)
Qualcomm's upscaler optimized for Adreno GPUs. SGSR 1 is a single-pass adaptive upscaler (similar to FSR 1) that could work on video frames.
- Source: https://github.com/SnapdragonGameStudios/snapdragon-gsr

### [arm-asr/](./arm-asr/)
**Arm Accuracy Super Resolution** (MIT license)
Based on AMD FSR 2.2, optimized for Mali/Immortalis mobile GPUs with 50% reduced GPU load. Note: requires motion vectors (temporal), so not directly usable on video frames.
- Source: https://github.com/arm/accuracy-super-resolution

### [libplacebo/](./libplacebo/)
**libplacebo** (C, LGPL-2.1)
GPU-accelerated video processing library used by mpv and VLC. Supports Vulkan (including Android), high-quality upscaling algorithms, can load custom FSR shaders. Production-tested for video.
- Source: https://github.com/haasn/libplacebo

### [rife-frame-interpolation/](./rife-frame-interpolation/)
**RIFE — Real-Time Intermediate Flow Estimation** (Python/PyTorch)
Neural network-based frame interpolation. Can double framerate (30fps → 60fps). Too heavy for mobile real-time, but excellent quality reference. Available via SVP desktop offloading.
- Source: https://github.com/megvii-research/ECCV2022-RIFE

---

## MVP / POC Implementation Research

### [android-mvp-implementation-research.md](./android-mvp-implementation-research.md)
**Android MVP Implementation Guide** — Practical research for the simplest path to get a PS3 video feed on Android. Covers: MediaCodec H.264 decode (feed raw NAL units to SurfaceView), AES-128-CBC decryption (javax.crypto.Cipher, zero dependencies), OkHttp long-lived streaming (readTimeout=0 + byteStream), UDP discovery (DatagramSocket broadcast), and controller input (onKeyDown/onGenericMotionEvent for PS5 DualSense, Xbox, generic gamepads). **Only 1 external dependency needed: OkHttp.**

### [docs-psdevwiki/key-extraction-research.md](./docs-psdevwiki/key-extraction-research.md)
**Key Extraction Research** — Deep analysis of how to obtain PS3 Remote Play encryption keys (pkey, psp_id, psp_mac) without a hacked PSP. Simplest path: PS3 HEN + patched VAIO client + FTP dump of xRegistry.sys. The three session keys (skey0/1/2) are universal constants already known. Includes xRegistry structure, all extraction methods ranked, and open questions.

### [stock-ps3-remote-play-research.md](./stock-ps3-remote-play-research.md)
**Stock PS3 Remote Play — Complete Research** — Comprehensive analysis of what is possible with a completely STOCK, UNMODIFIED PS3 (no HEN, no CFW, no jailbreak). Covers: official registration protocol (PSP/Vita/VAIO/Xperia), VAIO client on stock PS3 (confirmed working, but limited to 6 apps + XMB + PS1), Vita registration (stock-to-stock), compatible games list (~30-40 PS3 titles + all PS1), key extraction from VAIO PC side (critical gap — exact storage unknown), PPSSPP emulator limitations (won't work), Xperia APK analysis (defunct, not found), Internet mode (TCP/UDP 9293, stock supported), and four ranked approaches for building an Android client targeting stock PS3.

### [no-windows-registration-research.md](./no-windows-registration-research.md)
**No-Windows Registration Research** — How to register with a stock PS3 from macOS or Linux with NO Windows machine. Covers: Wine (blocked by Themida), Windows VM (works with USB WiFi passthrough), Wireshark capture, reverse engineering premo_plugin.sprx, custom registration tool, Android self-registration via 8-digit PIN, PS3 firmware decryption toolchain (all macOS/Linux native). **Best long-term: decompile premo_plugin.sprx in Ghidra. Best short-term: Windows VM + USB WiFi.**

### [ps3-firmware-extraction-research.md](./ps3-firmware-extraction-research.md)
**PS3 Firmware Extraction & Decompilation Guide** — Step-by-step pipeline to extract and decompile `premo_plugin.sprx` on macOS/Linux: download PUP → pupunpack → ps3sce decrypt (all FW keys public since 2012) → Ghidra (PowerPC64). Successfully completed — see Ghidra findings below.

### [pupps3/ghidra_findings/](./pupps3/ghidra_findings/)
**Ghidra Reverse Engineering Findings** — Active decompilation of premo_plugin.elf (FW 4.90). Key discoveries:
- **[00_FINDINGS_INDEX.md](./pupps3/ghidra_findings/00_FINDINGS_INDEX.md)** — Master index with architecture diagram, all identified functions, global variables, and investigation priorities
- **[01_dialog_handler](./pupps3/ghidra_findings/01_dialog_handler_FUN_0000ab1c.md)** — XMB UI state machine (12 dialog states for the Remote Play flow)
- **[02_key_strings](./pupps3/ghidra_findings/02_key_strings_and_addresses.md)** — All protocol strings with hex addresses for Ghidra navigation
- **[03_http_handler](./pupps3/ghidra_findings/03_http_handler_FUN_0001f7c0.md)** — **THE CORE FINDING**: PS3's PREMO HTTP server. Routes all requests, parses all headers, validates PSPID against registered devices, creates sessions. Reveals 4 platform types (PSP/Phone/type2/type3), PSPID validation loop, session creation dispatch, 403 error format.

### [pupps3/decrypted_elfs/](./pupps3/decrypted_elfs/)
**Decrypted PS3 Firmware ELFs** — Ready for Ghidra analysis:
- `premo_plugin.elf` (285 KB) — Main Remote Play module (session handling, HTTP server, UI)
- `premo_game_plugin.elf` (302 KB) — In-game streaming module
- `libsysutil_remoteplay.elf` (15 KB) — System utility library

---

## VAIO Remote Play Patch

### [docs-vaio-patch/vrppatch-info.md](./docs-vaio-patch/vrppatch-info.md)
**VRPPatch Details** — How the VAIO Remote Play patch bypasses Sony's hardware check via DLL injection. Key files: `VRPSDK.dll` (Themida-protected), `VRPMAPPING.dll`, `VRPUI.exe`. Useful for running Sony's official client on non-VAIO PCs for protocol analysis/Wireshark captures.
- Source: https://digiex.net/threads/vrppatch-vaio-remote-play-patch-download-for-ps3.13874/

### [docs-vaio-patch/vaio-key-storage-research.md](./docs-vaio-patch/vaio-key-storage-research.md)
**VAIO Client Key Storage Research** — Where does the VAIO client store keys on Windows after registration? Answer: **nobody has publicly documented this**. Includes most likely storage locations (Registry, AppData), how to find them (ProcMon/RegShot during registration), PPSSPP limitations (can't fake OpenPSID), and Xperia APK status (defunct/lost).

---

## Chiaki PS3 Discussion

### [docs-chiaki-ps3-issue/chiaki-ps3-issue-162.md](./docs-chiaki-ps3-issue/chiaki-ps3-issue-162.md)
**Chiaki Issue #162** — Maintainer confirms PS3 protocol is "entirely different" from PS4. Points to Open-RP as reference. PS3 support was never implemented.
- Source: https://github.com/thestr4ng3r/chiaki/issues/162

---

## Forum Discussions & Community Resources

### [docs-forums/xda-ps3-remote-play-android.md](./docs-forums/xda-ps3-remote-play-android.md)
**XDA Forums — PS3 Open Remote Play with Android** (2010) — Early attempts at Android PS3 Remote Play. No working solution achieved. Main barrier: Sony's authentication.
- Source: https://xdaforums.com/t/ps3-open-remote-play-with-android.842398/

### [docs-forums/psx-place-remote-play-android.html](./docs-forums/psx-place-remote-play-android.html)
**PSX-Place — Remote Play on Android** — Community discussion about PS3 Remote Play on Android devices.
- Source: https://www.psx-place.com/threads/remote-play-on-android.26855/

### [docs-forums/psx-place-remote-play-android-2.html](./docs-forums/psx-place-remote-play-android-2.html)
**PSX-Place — Remote Play for Android** — Second thread with additional approaches.
- Source: https://www.psx-place.com/threads/remote-play-for-android.24555/

### [docs-forums/wikipedia-remote-play-ps3.md](./docs-forums/wikipedia-remote-play-ps3.md)
**Wikipedia — Remote Play (PS3 sections)** — History, supported devices, capabilities, and limitations.
- Source: https://en.wikipedia.org/wiki/Remote_Play

---

## Official Sony Documentation

### [docs-forums/ps3-manual-remote-play-internet.html](./docs-forums/ps3-manual-remote-play-internet.html)
**PlayStation Manual — Remote Play via Internet** — Official documentation on internet setup (port forwarding TCP 9293).
- Source: https://manuals.playstation.net/document/en/ps3/current/remoteplay/remoteinternet.html

### [docs-forums/ps3-manual-register-device.html](./docs-forums/ps3-manual-register-device.html)
**PlayStation Manual — Register Device** — Official documentation on the device pairing process.
- Source: https://manuals.playstation.net/document/en/ps3/current/settings/registerdevice.html

---

## Forcing Remote Play on Custom Firmware

### [docs-consolemods/forcing-remote-play.html](./docs-consolemods/forcing-remote-play.html)
**ConsoleMods Wiki — PS3: Forcing Remote Play** — Methods to force Remote Play on all PS3 games using custom firmware and webMAN-MOD.
- Source: https://consolemods.org/wiki/PS3:Forcing_Remote_Play

---

## Key External Links (not downloaded)

| Resource | URL | Notes |
|----------|-----|-------|
| Mob-FGSR | https://mob-fgsr.github.io/ | Mobile frame gen + super resolution (SIGGRAPH 2024) |
| Mob-FGSR Code | https://github.com/Mob-FGSR/MobFGSR | Source code for mobile frame generation |
| WebSR | https://github.com/sb2702/websr | WebGPU-based ML upscaling for video (MIT) |
| Anime4K | https://github.com/bloc97/Anime4K | Real-time upscaling shaders (MIT) |
| SVPlayer Android | https://play.google.com/store/apps/details?id=com.svpteam.svp | Proven 30→60fps MEMC on Android |
| SVP FAQ (Android) | https://www.svp-team.com/wiki/FAQ_(Android) | SVPlayer requirements and setup |
| ALVR Upscaling Wiki | https://github.com/alvr-org/ALVR/wiki/Real-time-video-upscaling-experiments | Real-world VR streaming upscaling tests |
| ALVR FSR Discussion | https://github.com/alvr-org/ALVR/issues/780 | FSR on video stream quality analysis |
| FSR 1 Demystified | https://jntesteves.github.io/shadesofnoice/graphics/shaders/upscaling/2021/09/11/amd-fsr-demystified.html | Technical deep-dive into FSR 1 algorithm |
| FSR 2 OpenGL Port | https://github.com/JuanDiegoMontoya/FidelityFX-FSR2-OpenGL | OpenGL FSR 2 reference (MIT) |
| Google Code Archive | https://code.google.com/archive/p/open-rp | Original Open-RP project page |
| Port Forward Guide | https://portforward.com/playstation-3-remote-play/ | PS3 Remote Play port forwarding |
| NeoGAF Thread | https://www.neogaf.com/threads/psp-remote-play-reverse-engineered-ps3-on-your-computer.361116/ | Original reverse engineering announcement |
