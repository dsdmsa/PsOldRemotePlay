# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PsOldRemotePlay** — PS3 Remote Play client for Android/Desktop. Streams PS3 video/audio to modern devices using the PREMO protocol (reverse-engineered from PS3 firmware 4.90).

**Status:** Session protocol fully implemented. Registration key derivation formulas VERIFIED (all 3 device types confirmed from decompiled code + PPC assembly). The sole remaining blocker is the 8-byte IV context value (traced to PIN as big-endian longlong via code, but tests fail). xRegistry bypass path available.

## Build Commands

```bash
./gradlew :composeApp:run                        # Run desktop app
./gradlew :composeApp:compileKotlinDesktop        # Compile desktop only
./gradlew :composeApp:compileDebugKotlinAndroid   # Compile Android
./gradlew :composeApp:assembleDebug               # Build Android APK
./gradlew :composeApp:desktopTest                 # Run desktop tests
```

## Architecture

**MVI (Model-View-Intent)** with Kotlin Multiplatform + Compose Multiplatform. Interface-based DI.

### Layers

- **protocol/** (commonMain) — Protocol interfaces, data classes, constants, crypto keys. Platform-agnostic.
- **presentation/** (commonMain) — `RemotePlayState`, `RemotePlayIntent`, `RemotePlayEffect`, `RemotePlayViewModel` (MVI loop).
- **ui/** (commonMain) — Shared Compose UI with responsive layout. Components: `ControlPanel`, `VideoSurface`, `LogPanel`.
- **di/** — `PlatformDependencies` interface in commonMain. `DesktopDependencies` and `AndroidDependencies` wire platform implementations.

### Key Interfaces (protocol/)

| Interface | Purpose | Desktop Impl | Android Impl |
|-----------|---------|-------------|--------------|
| `PremoCrypto` | AES-128-CBC, Base64, random | `JvmPremoCrypto` | `AndroidPremoCrypto` |
| `Ps3Discoverer` | UDP SRCH/RESP discovery | `JvmPs3Discoverer` | `AndroidPs3Discoverer` |
| `PremoSessionHandler` | HTTP session + streaming | `JvmPremoSession` | `AndroidPremoSession` |
| `PremoRegistration` | Device registration | `JvmPremoRegistration` | `StubRegistration` |
| `VideoRenderer` | Video packet handling | `LoggingVideoRenderer` | `LoggingVideoRenderer` |
| `ControllerInputSender` | Controller input to PS3 | `StubControllerInput` | `StubControllerInput` |

### Protocol Flow

1. **Discovery:** UDP broadcast "SRCH" → PS3 responds "RESP" (156 bytes: MAC + nickname + NPX ID)
2. **Session:** HTTP GET `/sce/premo/session` with PREMO-* headers → 200 OK with nonce
3. **Auth:** Derive AES key/IV from pkey + nonce + SKEY0/SKEY2 → encrypt MAC → base64 auth token
4. **Stream:** HTTP GET `/sce/premo/session/video` with SessionID + auth → 32-byte headers + payload
5. **Registration (IV UNKNOWN):** POST `/sce/premo/regist` with AES-encrypted body → formulas verified, IV context value unsolved
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
- `research/pupps3/ghidra_findings/08_COMPLETE_PROTOCOL_SUMMARY.md` — Full protocol reference
- `research/tools/ps3_register_bruteforce_iv.py` — IV context brute-force script (22 encodings)
- `research/tools/` — Python/bash scripts for testing
