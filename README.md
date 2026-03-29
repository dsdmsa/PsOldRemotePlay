# PsOldRemotePlay

An open-source PS3 Remote Play client for Android and Desktop. Streams PS3 video/audio to modern devices using the PREMO protocol reverse-engineered from PS3 firmware.

**Status:** Session protocol fully implemented. Registration key derivation formulas verified. Sole blocker: unknown 8-byte IV context value in device registration.

## Features

- **Cross-platform:** Kotlin Multiplatform (Android + Desktop via Compose Multiplatform)
- **Protocol:** Full PREMO session protocol reverse-engineered from PS3 firmware 4.90
- **Discovery:** UDP SRCH/RESP PS3 discovery on local network
- **UI:** Responsive Compose layout with controls, video surface, and debug logs
- **Crypto:** AES-128-CBC with firmware-extracted static keys

## Quick Start

### Desktop
```bash
./gradlew :composeApp:run
```

### Android
```bash
./gradlew :composeApp:assembleDebug
```

### Build Desktop Only
```bash
./gradlew :composeApp:compileKotlinDesktop
```

## Architecture

**MVI Pattern** with Kotlin Multiplatform + Compose Multiplatform:

```
protocol/        → Protocol layer (interfaces, crypto, constants)
presentation/    → MVI (State, Intent, Effect, ViewModel)
ui/              → Shared Compose UI components
di/              → Platform-specific dependency injection
```

### Key Components

| Component | Purpose | Status |
|-----------|---------|--------|
| UDP Discovery | Find PS3 on network | ✅ Complete |
| HTTP Session | Establish PREMO session | ✅ Complete |
| Session Crypto | AES key derivation | ✅ Complete |
| Video Stream | Receive encrypted video packets | ✅ Partial (decrypt only) |
| Registration | Register new devices | ⚠️ Blocked (IV context unknown) |
| Controller Input | Send button/stick input | 🔲 Stub |

## Protocol Overview

1. **Discovery:** UDP broadcast "SRCH" → PS3 responds with MAC, nickname, NPX ID
2. **Session:** HTTP GET `/sce/premo/session` → PS3 returns nonce
3. **Auth:** Derive AES key/IV from pkey + nonce + firmware keys → create auth token
4. **Stream:** HTTP GET `/sce/premo/session/video` → 32-byte headers + AES-CBC encrypted packets
5. **Registration:** HTTP POST `/sce/premo/regist` with encrypted device info → returns pkey

## The Blocker: Registration IV Context

Device registration requires AES-128-CBC encryption with a key/IV derived from:
- Static keys (✅ extracted and verified)
- Key material (✅ verified)
- **IV context value** (❌ UNKNOWN)

The IV context is an 8-byte value XOR’d into the IV base. All attempts to identify it have failed:
- PIN as big-endian longlong → ❌ fails with error 80029820
- PIN as 4-byte BE int → ❌ fails
- PIN halves swapped → ❌ fails
- Device MAC address → ❌ fails
- PS3 PSID → ❌ fails
- And 17 other encodings → all ❌ fail

**Latest hypothesis:** The context is a **hardware device identifier** (similar to how the VAIO Windows client uses machine ID), not the PIN.

## How to Help

### 🔍 Research Tasks
1. **Analyze VAIO binaries** — Find how cfContext is derived in Windows client
2. **PS3MAPI memory dump** — Read actual IV context value during registration
3. **xRegistry bypass** — Prove the rest of the protocol works by injecting device data
4. **Parse VAIO registry** — Find PIN→crypto mapping in extracted installer
5. **PS4 comparison** — PS4 registration is solved; PS3 may follow similar pattern

See [RESEARCH_STATUS.md](RESEARCH_STATUS.md) for detailed findings.

### 💻 Implementation
1. **Video decode** — Unencrypt AES-CBC payload and decode video frames
2. **Audio stream** — Implement audio packet parsing and playback
3. **Controller input** — Send button/stick input to PS3
4. **UI improvements** — Add settings, connection history, quality controls
5. **Registration fix** — Once IV context is identified, complete the registration handler

### 📱 Platform Support
- Android: minSdk 24, targetSdk 36
- Desktop: JVM 11+
- iOS: Planned via KMP

## Project Structure

- `composeApp/src/commonMain/kotlin/protocol/` — Protocol interfaces and constants
- `composeApp/src/commonMain/kotlin/presentation/` — MVI state/intent/effects
- `composeApp/src/commonMain/kotlin/ui/` — Compose UI components
- `docs/` — Protocol analysis and research summaries
- `tools/` — Python/bash analysis and test scripts

## Dependencies

- Kotlin 2.3.0
- Compose Multiplatform 1.10.0
- Material 3
- kotlinx-coroutines
- kotlinx-datetime
- lifecycle-viewmodel-compose (Android)

## License

MIT (See LICENSE file)

## Resources

- **Research Findings:** See [RESEARCH_STATUS.md](RESEARCH_STATUS.md)
- **Contributing:** See [CONTRIBUTING.md](CONTRIBUTING.md)
- **Developer Notes:** See [CLAUDE.md](CLAUDE.md)

## Related Projects

- [Open-RP](https://github.com/gbraad/open-rp) — Original PSP Remote Play
- [Chiaki-NG](https://github.com/streetpea/chiaki-ng) — PS4/PS5 Remote Play (actively maintained)
- [PS4 Remote Play](https://github.com/kingcreek/ps4-remote-play) — Python PS4 client