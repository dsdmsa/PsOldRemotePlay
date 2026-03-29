# Contributing to PsOldRemotePlay

Thank you for your interest in contributing! This project is open to both code contributions and research help.

## Research Priorities

The project is currently blocked on identifying the **8-byte IV context value** used in PS3 device registration. This is a reverse-engineering challenge. Below are high-priority research tasks:

### 🔴 Critical (Will unblock registration)

#### 1. Analyze VAIO Windows Client
**Difficulty:** Medium | **Impact:** Very High

The VAIO Remote Play installer contains binaries that implement the client-side registration protocol. Windows binaries are easier to analyze than PS3 firmware.

**What to do:**
1. Download VAIO Remote Play (any version with PS3 support)
2. Extract the installer (see tools: `tools/re_tools/extract_installer.sh`)
3. Open `VRPMFMGR.dll` in Ghidra (x86 disassembly)
4. Find the registration setup function at offset `0x1000bb60`
5. Trace how `cfContext` parameter is derived
6. Is it from `SuGetMachineIDEx()`? From PIN? A combination?
7. Find the actual derivation formula

**Why this helps:**
- VAIO client likely uses same protocol as PS3
- If `cfContext` is hardware-derived (not PIN), we need to replicate that on our client
- This explains why all PIN-based attempts failed

**Resources:**
- Already extracted: `research/repos/ps3-remote-play/*/extracted/vrp_files/App/`
- Analysis started: `research/dllsun/VRPSDK_ANALYSIS.md`

---

#### 2. PS3MAPI Memory Dump During Registration
**Difficulty:** Medium | **Impact:** Very High | **Requires:** HEN PS3 + webMAN MOD

Read the actual IV context value at runtime.

**What to do:**
1. Put PS3 in registration mode (show PIN on screen)
2. Use PS3MAPI HTTP interface to dump memory at:
   ```
   /getmem.ps3mapi?proc=vsh&addr=01D15818&len=8
   ```
3. Convert returned hex to 8 bytes
4. This is the actual `**(param_1 + 0x38)` value
5. Compare against all 22 known PIN encodings
6. If different, what pattern does it match?

**Why this helps:**
- Definitive answer (no guessing)
- May reveal pattern we haven't tested yet
- Can immediately verify any hypothesis

**Tools:**
- `research/tools/ps3_register_bruteforce_iv.py` — test contexts against real PS3
- PS3MAPI HTTP: `http://ps3-ip/getmem.ps3mapi?proc=vsh&addr=ADDRESS&len=SIZE`

---

#### 3. xRegistry Bypass (Proof of Concept)
**Difficulty:** Easy | **Impact:** High | **Requires:** HEN PS3 + FTP

Prove the entire protocol works by bypassing registration entirely.

**What to do:**
1. FTP to PS3 and download `/dev_flash2/etc/xRegistry.sys`
2. Parse with `tools/xregistry_tool.py` (or ManaGunZ format)
3. Create a fake device entry in `/setting/premo/psp01/`:
   - `key` = random 16 bytes (you choose the pkey)
   - `macAddress` = your device's MAC
   - `id` = any 16-byte device ID
   - `nickname` = "TestDevice"
   - `keyType` = 0
4. Upload modified xRegistry.sys back
5. Reboot PS3
6. Try session connection with your chosen pkey

**Why this helps:**
- Proves video streaming works once pkey is known
- Isolates the registration problem from session/streaming
- Gives us a working pkey to use for development

---

### 🟡 High Priority

#### 4. Parse VAIO Installer Registry
**Difficulty:** Medium | **Impact:** Medium

If the VAIO installer contains device registry entries or configuration files, they might reveal the PIN→crypto mapping.

**What to do:**
1. Extract the VAIO installer (5 layers of extraction)
2. Search for `.xml`, `.ini`, `.reg` config files
3. Look for strings matching known static keys
4. Trace any PIN usage in config files

---

#### 5. PS4 Protocol Comparison
**Difficulty:** Easy | **Impact:** Medium

PS4 registration is fully solved and public. PS3 may follow a similar pattern.

**What to do:**
1. Read `research/pupps3/ghidra_findings/21_RESEARCH_COMPILATION.md` (PS4 section)
2. PS4 uses PIN as 4-byte BE int: `struct.pack('!L', pin)`
3. PS3 code suggests 8-byte context (ld instruction, 64-bit)
4. Could PS3 be: PIN as 4-byte BE + 4 zero bytes?
5. Test this specific encoding

---

### 🟢 Nice to Have

#### 6. PSP Firmware Analysis
**Difficulty:** Hard | **Impact:** Low

PSP Remote Play was the original. Analyzing PSP firmware might reveal hints about PS3 protocol evolution.

**What to do:**
1. Find decrypted PSP firmware
2. Search for remoteplay PRX modules
3. If found, decompile for registration implementation

---

#### 7. Japanese Forum Research
**Difficulty:** Medium | **Impact:** Low

Japanese PS3 forums may have registration protocol discussion.

**What to do:**
1. Search forums for "PREMO" registration protocol
2. Look for any hints about "鍵" (key) or device ID structure
3. Check Sony support pages for PS3 Remote Play troubleshooting

---

## Code Contribution Areas

Once registration is solved, these areas need implementation:

### 🟠 Medium Priority

1. **Video Decoding**
   - Location: `composeApp/src/commonMain/kotlin/ui/`
   - Current: LoggingVideoRenderer (counts packets only)
   - Needed: Actual video frame decoding (H.264)

2. **Audio Streaming**
   - Location: `composeApp/src/commonMain/kotlin/protocol/`
   - Current: Not implemented
   - Needed: Audio packet parsing and playback

3. **Controller Input**
   - Location: `composeApp/src/commonMain/kotlin/protocol/ControllerInputSender.kt`
   - Current: StubControllerInput
   - Needed: Button/stick input serialization and HTTP POST to `/sce/premo/session/pad`

### 🟢 Lower Priority

4. **Android Permissions & Networking**
   - Add WiFi scanning for PS3 discovery
   - Handle network state changes gracefully

5. **UI Enhancements**
   - Settings screen (quality, resolution, bitrate)
   - Connection history
   - On-screen controller overlay

6. **Desktop-Specific Features**
   - Keyboard input mapping
   - Mouse input to PS3 controller
   - Fullscreen mode

## Development Setup

### Prerequisites
- JDK 11+
- Android SDK (minSdk 24)
- Xcode (for iOS)
- Kotlin 2.3.0

### Build
```bash
# Desktop
./gradlew :composeApp:run

# Android
./gradlew :composeApp:assembleDebug

# Tests
./gradlew :composeApp:desktopTest
```

### Code Structure
```
composeApp/src/commonMain/kotlin/
├── protocol/           # Protocol interfaces, crypto, constants
│   ├── PremoConstants.kt
│   ├── PremoCrypto.kt
│   ├── PremoSession.kt
│   ├── Ps3Discovery.kt
│   └── PremoRegistration.kt
├── presentation/       # MVI pattern
│   └── RemotePlayViewModel.kt
├── ui/                 # Compose components
│   ├── ControlPanel.kt
│   ├── VideoSurface.kt
│   └── LogPanel.kt
└── di/                 # Dependency injection
    ├── PlatformDependencies.kt (interface)
    ├── desktopMain/DesktopDependencies.kt
    └── androidMain/AndroidDependencies.kt
```

### Key Interfaces to Implement
- `PremoCrypto` — AES, Base64, random (per-platform)
- `Ps3Discoverer` — UDP discovery (per-platform)
- `PremoSessionHandler` — HTTP session + streaming (per-platform)
- `VideoRenderer` — Video packet decoding
- `ControllerInputSender` — Button input to PS3

## Commit Guidelines

- **Research findings:** `research: <brief description>`
  - Example: `research: VAIO VRPMFMGR.dll analysis reveals cfContext derivation`
- **Code fixes:** `fix: <component> - <description>`
- **Features:** `feat: <component> - <description>`
- **Documentation:** `docs: <what>`

## Testing

### Manual Testing
1. Discover PS3 on local network
2. Establish session (check for 200 OK response)
3. Subscribe to video stream
4. Verify AES decryption of packets

### Test Tools
```bash
# Test registration with specific PIN/context
python3 research/tools/ps3_register_bruteforce_iv.py 192.168.1.75 12345678

# Parse xRegistry.sys
python3 research/tools/xregistry_tool.py /path/to/xRegistry.sys parse

# Analyze protocol captures
research/tools/capture_instructions.md
```

## Questions?

- **Protocol:** See `RESEARCH_STATUS.md` and `CLAUDE.md`
- **Architecture:** See `CLAUDE.md`
- **Firmware analysis:** See `research/pupps3/ghidra_findings/`
- **Tools:** See `research/tools/` and `research/tools/re_tools/`

## License

All contributions are subject to the MIT license. See LICENSE file.

---

**Thank you for helping bring PS3 Remote Play to life!**
