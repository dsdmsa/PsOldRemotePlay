# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PsRemotePlay** — Modular PlayStation streaming platform for Android/Desktop. Currently supports PS3 Remote Play via PREMO protocol (reverse-engineered from PS3 firmware 4.90). Architecture supports future PS4/PS5/PS2 streaming modules.

**Status:** Session protocol fully implemented. Registration key derivation formulas VERIFIED (all 3 device types confirmed from decompiled code + PPC assembly). VAIO DLL analysis complete (2026-03-29): all keys confirmed, but key derivation is Themida VM-protected. **Critical finding:** PC type uses PIN-derived key material (not random) and encrypts body from offset 0x1E0 (not 0). Current app likely has multiple bugs for PC registration. xRegistry bypass path available.

## Build Commands

```bash
# PS3 Remote Play
./gradlew :app:ps3:run                            # Run PS3 desktop app
./gradlew :app:ps3:assembleDebug                  # Build PS3 Android APK

# PS2 Streaming
./gradlew :app:ps2server:run                      # Run PS2 streaming server (desktop)
./gradlew :app:ps2client:run                      # Run PS2 client (desktop test)
./gradlew :app:ps2client:assembleDebug            # Build PS2 Android client APK

# Module compilation
./gradlew :feature:ps3:compileKotlinDesktop       # Compile PS3 feature module
./gradlew :feature:ps2:compileKotlinDesktop       # Compile PS2 feature module
./gradlew :core:streaming:compileKotlinDesktop    # Compile core streaming module
```

## Architecture

**MVI (Model-View-Intent)** with Kotlin Multiplatform + Compose Multiplatform. Interface-based DI. Multi-module architecture.

### Module Structure

```
:core:streaming     — Shared interfaces: Crypto, Logger, VideoRenderer, AudioRenderer, codecs, input, upscale
:core:ui            — Shared Compose components: VideoSurface, LogPanel, OnScreenController
:feature:ps3        — PS3 PREMO protocol, Ps3ViewModel, PS3-specific UI, platform impls
:feature:ps2        — PS2 streaming protocol, server/client ViewModels, UI, platform impls
:app:ps3            — PS3 Remote Play application (entry points, dependency wiring)
:app:ps2server      — PS2 Streaming Server (desktop only, launches PCSX2 + FFmpeg + TCP server)
:app:ps2client      — PS2 Streaming Client (Android + desktop for testing + iOS stubs)
```

**Dependency graph:** `app:ps3 → feature:ps3 → core:streaming` and `feature:ps3 → core:ui → core:streaming`

### Package Convention

- `com.my.psremoteplay.core.streaming` — shared streaming interfaces
- `com.my.psremoteplay.core.ui.components` — shared Compose components
- `com.my.psremoteplay.feature.ps3.protocol` — PREMO protocol
- `com.my.psremoteplay.feature.ps3.presentation` — PS3 MVI (Ps3State, Ps3Intent, Ps3ViewModel)
- `com.my.psremoteplay.feature.ps3.ui` — PS3 screens (Ps3Screen, Ps3ControlPanel)
- `com.my.psremoteplay.feature.ps3.di` — PS3 DI (Ps3Dependencies, DesktopPs3Dependencies, AndroidPs3Dependencies)
- `com.my.psremoteplay.feature.ps3.platform` — PS3 platform impls (JvmPremoSession, etc.)

### DI Architecture

**Core layer:** `StreamingDependencies` (crypto, videoDecoder, audioDecoder, videoRenderer, audioRenderer, upscaleFilter, logger)
**Feature layer:** `Ps3Dependencies` (streaming + discoverer, sessionHandler, registration, controllerInput)
**Platform wiring:** `DesktopPs3Dependencies`, `AndroidPs3Dependencies` implement `Ps3Dependencies`

### Key Interfaces

| Module | Interface | Purpose |
|--------|-----------|---------|
| core:streaming | `Crypto` | AES-128-CBC, Base64, random |
| core:streaming | `VideoRenderer` | Video frame display (StateFlow<ImageBitmap?>) |
| core:streaming | `ControllerInputSender` | Gamepad input (generic params) |
| feature:ps3 | `Ps3Discoverer` | UDP SRCH/RESP discovery |
| feature:ps3 | `PremoSessionHandler` | HTTP session + streaming |
| feature:ps3 | `PremoRegistration` | Device registration |

### Protocol Flow

1. **Discovery:** UDP broadcast "SRCH" → PS3 responds "RESP" (156 bytes: MAC + nickname + NPX ID)
2. **Session:** HTTP GET `/sce/premo/session` with PREMO-* headers → 200 OK with nonce
3. **Auth:** Derive AES key/IV from pkey + nonce + SKEY0/SKEY2 → encrypt MAC → base64 auth token
4. **Stream:** HTTP GET `/sce/premo/session/video` with SessionID + auth → 32-byte headers + payload
5. **Registration (MULTIPLE ISSUES):** POST `/sce/premo/regist` with AES-encrypted body → formulas verified, IV context value unsolved. VAIO analysis revealed: PC type uses PIN-derived km (not random), body encrypted from offset 0x1E0, min 512-byte body, Client-Type="VITA"
6. **Bypass:** xRegistry.sys injection at `/setting/premo/psp01/key` via FTP to HEN PS3

### Static Crypto Keys

All in `PremoConstants.kt`. Same for all PS3 consoles. Session keys (SKEY0/1/2), nonce XOR keys (per platform: PSP/Phone/PC/VITA), registration keys (3 platforms × XOR key + IV base).

## Key Configuration

- **Kotlin** 2.3.0, **Compose Multiplatform** 1.10.0, **Material 3**
- **Android**: minSdk 24, targetSdk 36, JVM 11
- **Dependencies**: kotlinx-coroutines, kotlinx-datetime, lifecycle-viewmodel-compose

## Research

- `research/STATUS_AND_NEXT_STEPS.md` — Current status, blocker, action plan
- `research/pupps3/ghidra_findings/` — 21+ Ghidra analysis documents
- `research/pupps3/ghidra_findings/20_DECOMPILED_REGISTRATION_HANDLER.md` — Full decompiled registration handler
- `research/pupps3/ghidra_findings/20_RUNTIME_MEMORY_ANALYSIS.md` — PS3MAPI runtime memory findings
- `research/pupps3/ghidra_findings/21_RESEARCH_COMPILATION.md` — ALL research compiled (xRegistry bypass, PS4 comparison, BSS mapping)
- `research/pupps3/ghidra_findings/22_VAIO_DLL_ANALYSIS.md` — VAIO Windows client RE (Themida unpacking, AES mapping, body structure findings)
- `research/pupps3/ghidra_findings/08_COMPLETE_PROTOCOL_SUMMARY.md` — Full protocol reference
- `research/tools/ps3_register_bruteforce_iv.py` — IV context brute-force script (22 encodings)
- `research/tools/dump_vrpsdk*.c` — VAIO DLL dump/analysis tools (compile with mingw, run with Wine)
- `research/tools/vrpsdk_dumped.bin` — Unpacked VRPSDK.dll memory dump (2.8 MB)
- `research/tools/vrpsdk_code.bin` — Unpacked code section (163 KB, loadable in Ghidra at base 0x10001000)
- `research/tools/` — Python/bash scripts for testing
