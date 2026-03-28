# PS3 Remote Play Project — Complete Status Report

## Project Goal
Build an Android/Desktop app that connects to ANY PS3 (stock, unmodified) for Remote Play streaming — similar to what Chiaki does for PS4/PS5.

---

## What We've Built

### Desktop POC App (KMP + Compose Desktop)
- Location: `composeApp/src/desktopMain/`
- Run: `./gradlew :composeApp:run`
- 3-panel UI: Controls | Video Surface | Logs
- Features: UDP/TCP discovery, session handshake, registration attempt
- Confirmed working: PS3 responds to our HTTP requests with proper PREMO headers

### Shared Protocol Layer (commonMain)
- `protocol/PremoConstants.kt` — All static crypto keys
- `protocol/PremoCrypto.kt` — AES, key derivation, auth token generation
- `protocol/PremoSession.kt` — Session request/response building and parsing
- `protocol/Ps3Discovery.kt` — UDP SRCH/RESP discovery
- `protocol/PremoLogger.kt` — Logging interface

---

## What We've Reverse Engineered (COMPLETE)

### Session Protocol (100% understood)
From `premo_plugin.sprx` (Ghidra + exported C analysis):
- Discovery: UDP broadcast `SRCH` → PS3 responds with `RESP` (156 bytes)
- Session: `GET /sce/premo/session HTTP/1.1` with PREMO-* headers
- Video stream: `GET /sce/premo/session/video` (long-lived HTTP, AES-encrypted packets)
- Audio stream: `GET /sce/premo/session/audio`
- Controller: `POST /sce/premo/session/pad` (encrypted input)
- Control: `GET /sce/premo/session/ctrl` (bitrate changes, session termination)
- Stream packet: 32-byte header + AES-128-CBC encrypted payload
- Nonce generation formula (corrected from exported C — see below)
- Platform types: PSP (0), Phone (1), PC (2), VITA (3)
- Exec modes: VSH (XMB UI), GAME, PS1, SUBDISPLAY
- All response headers documented

### Session Crypto Keys (ALL extracted from firmware)
```
skey0: D1 B2 12 EB 73 86 6C 7B 12 A7 5E 0C 04 C6 B8 91
skey1: 1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F
skey2: 65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59

Nonce XOR keys per platform:
PSP:   65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59 (= skey2)
Phone: 37 63 E5 4D 12 F9 7B 73 62 3A D3 0D 10 C9 91 7E
PC:    33 72 84 4C A6 6F 2E 2B 20 C7 90 60 33 E8 29 C6
VITA:  AF 74 B5 4F 38 F8 AF C8 75 77 B2 D5 47 76 3B FD

Second nonce XOR keys (from exported C, NOT from decompiler):
PSP:   (simple XOR, no second key — uses DAT_0002d548)
Phone: DAT_0002d568 = DE 61 A1 C9 BA E1 AC 56 4B 05 61 91 81 2D 67 25
PC:    DAT_0002d588 = A0 02 09 84 6C 75 60 0D 57 C2 6B 00 4A 28 59 F1
VITA:  DAT_0002d5a8 = 08 11 0C F9 0A C8 A9 09 3B 46 CF BF 5A EA C7 55
```

### Registration Protocol (90% understood — encryption formula is WRONG)
From `sysconf_plugin.sprx` (Ghidra analysis):
- Endpoint: `POST /sce/premo/regist HTTP/1.1`
- PS3 creates WiFi AP: SSID = `PS3REGIST_XXXX` (or `PS3REGPC_XXXX` for PC)
- WiFi password = **PIN with halves swapped** (last 4 digits + first 4 digits)
- WiFi uses **ad-hoc/IBSS mode** (modern Macs/phones can't connect, Linux with wpa_supplicant WPA-NONE works)
- Request body: AES-encrypted, contains `Client-Type`, `Client-Id`, `Client-Mac`, `Client-Nickname`
- Response body: AES-encrypted, contains `PREMO-Key` (the pkey!), `PS3-Mac`, `PS3-Nickname`, `AP-Ssid`, `AP-Key`
- PS3 generates random 16-byte pkey and sends it in response
- Max 3 registration attempts before PS3 drops WiFi
- Port 9293 opens on WIRED interface during registration mode
- Client-Type mapping is SHIFTED: PSP menu→expects "Phone", Phone menu→expects "PC", PC menu→expects "VITA"

### Registration Crypto Keys (extracted but formula is WRONG)
```
Registration XOR keys per platform:
PSP:   FD C3 F6 A6 4D 2A BA 7A 38 92 6C BC 34 31 E1 0E (DAT_00150ea0)
Phone: F1 16 F0 DA 44 2C 06 C2 45 B1 5E 48 F9 04 E3 E6 (DAT_00150ec0)
PC:    EC 6D 70 6B 1E 0A 9A 75 8C DA 78 27 51 A3 C3 7B (DAT_00150ee0)

Registration IV bases:
PSP:   3A 9D F3 3B 99 CF 9C 0D BF 58 81 12 6C 18 32 64 (DAT_00150eb0)
Phone: 29 0D E9 07 E2 3B E2 FC 34 08 CA 4B DE E4 AF 3A (DAT_00150ed0)
PC:    5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04 (DAT_00150ef0)
```

---

## THE BLOCKER: Registration Encryption

### UPDATE 2026-03-28: Full Decompiled Code Now Available

`UndefinedFunction_00100168` has been fully decompiled (see `research/pupps3/ghidra_findings/20_DECOMPILED_REGISTRATION_HANDLER.md`). The key derivation formulas are **simpler than feared** — NOT using 4 inputs like the session nonce. The registration handler is a straightforward AES-CBC encrypt/decrypt with per-device-type key/IV derivation.

### Key Derivation Formulas (CONFIRMED from decompiled C)

**PSP (type 0):** `aes_key[i] = (km[i] ^ DAT_00150ea0[i]) - i - 0x25`
**Phone (type 1):** `aes_key[i] = (km[i] - i - 0x28) ^ DAT_00150ec0[i]`
**PC/VITA (type 2):** `aes_key[i] = (km[i] ^ DAT_00150ee0[i]) - i - 0x2B`

These match our test scripts exactly.

### IV Derivation (CONFIRMED)
- PSP/PC: IV base XOR'd on **first** 8 bytes with `**(ulonglong **)(param_1 + 0x38)`
- Phone: IV base XOR'd on **second** 8 bytes with `**(ulonglong **)(param_1 + 0x38)`

### Context Value = PIN as Big-Endian Longlong (CONFIRMED by call chain)
Traced through: `FUN_000fe6b8` → `FUN_000fe4cc` → `FUN_00101000` → `FUN_0010150c`
The 8-byte context = `(longlong)(int)(random_PIN)` stored in the registration parameter structure.

### ALL PREVIOUSLY TRIED — STILL FAILING

Despite confirming the formulas match our test scripts, all attempts return 80029820.

**Remaining suspects (prioritized):**
1. ~~**Static keys not verified**~~ **ELIMINATED 2026-03-28** — Hex-verified all 96 bytes at DAT_00150ea0-DAT_00150f00 against raw sysconf_plugin.elf binary. All 6 keys match PremoConstants.kt exactly.
2. **Parameter shift** — `FUN_0010150c` has `undefined8 param_2`. On PPC64, if this consumes 2 register slots, all subsequent params shift and `*(ctx + 0x38)` would NOT be the PIN structure.
3. **Firmware version** — Keys extracted from 4.90, tests run on 4.92. Sony may have changed the offset where the context pointer is stored.
4. **WiFi passphrase** — Two 60-char static strings discovered in `FUN_000ff588`. If the test is connecting with wrong WiFi credentials, the PS3 won't be in the right state.
5. **VAIO VRP.exe needed** — The installer uses Sony's proprietary packaging format. Need Wine or Windows to extract. The x86 client-side registration code would definitively show how the IV is computed.

### WiFi AP Passphrase Discovery
```
PSP/PC type: "xeRFa3VYDHSfNjE_nA{z>pk2xANqicHqQFvij}0WHz[7kzO2Yynp4o4j}U2"
Phone type:  "RvnpNXP}xS5qOWfj77guV}0lPAS37hzONG7ZHMNBAwM0mKjt1mkUhHjdbyF"
```

### What We Tried (ALL FAILED with error 80029820)
- Context = zeros (all zeros)
- Context = PIN as ASCII bytes
- Context = PIN as big-endian integer ← **should be correct per code analysis**
- Context = PIN as little-endian integer
- Context = PIN first 4 digits padded
- Context = PIN halves swapped
- Context = PS3 ethernet MAC + padding
- Context = PS3 WiFi MAC (ethernet+1) + padding
- Context = sockaddr_in(AF_INET, port 9293, IP 192.168.1.1)
- Context = PS3 PSID (first 8 bytes)
- Context = PS3 PSID (last 8 bytes)
- Client-Type "Phone" and "PC" (testing shifted mapping)
- PSP encryption formula with Phone expected Client-Type
- Phone encryption formula with PC expected Client-Type
- Double-XOR: key = ((material - i - 0x28) XOR key1) XOR key2
- Completely unencrypted body (no AES at all)
- Two different PS3s tested (stock and HEN)

---

## UPDATE 2026-03-28: Key Derivation Formulas ARE Correct

The full registration handler `UndefinedFunction_00100168` has been decompiled and the PPC assembly verified at runtime via PS3MAPI. The formulas are SIMPLE (not complex like session nonce):

```
PSP:   key[i] = (km[i] ^ PSP_XOR[i]) - i - 0x25     ← verified PPC assembly at 0x1004F4
Phone: key[i] = (km[i] - i - 0x28) ^ PHONE_XOR[i]    ← verified PPC assembly at 0x100570
PC:    key[i] = (km[i] ^ PC_XOR[i]) - i - 0x2B        ← verified PPC assembly at 0x1005D8
```

**These match the app's `JvmPremoRegistration.kt` implementation exactly.**

### What IS Still Unknown: IV Context Value

The sole remaining unknown is the 8-byte value XOR'd into the IV. From code tracing:
- Stored at `*(uint64_t*)(*(uint32_t*)(ctx + 0x38))`
- Call chain: `FUN_000fe6b8` stores `(longlong)(int)PIN` at the structure's offset 0
- BSS memory at those addresses is only readable when PS3 is in registration mode

### What Would Solve This

1. **PS3MAPI memory peek during registration** — Read `0x01D15818` while PS3 shows PIN on screen
2. **xRegistry bypass** — Inject pkey directly into `/setting/premo/psp01/key` via FTP (format documented, no checksums)
3. **Brute-force IV context** — Run `ps3_register_bruteforce_iv.py` with 22 PIN encodings (3 attempts per session)
4. **VAIO binary analysis** — Extract VRP.exe via Wine, open in Ghidra (x86), search for `F1 16 F0 DA`

### Bug Fixed in App

`JvmPremoRegistration.kt` had wrong Client-Type mapping — sent "Phone"/"PC" instead of "PC"/"VITA" (the shifted mapping confirmed from decompiled code). Fixed with new `clientTypeHeader()` function.

---

## App Implementation Status

| Component | Status | Notes |
|-----------|--------|-------|
| UDP Discovery | COMPLETE | Broadcast + direct IP, both platforms |
| Session Handshake | COMPLETE | HTTP GET with PREMO-* headers |
| Session Crypto | COMPLETE | XOR key derivation + auth token |
| Registration (Desktop) | PARTIAL | Key derivation correct, IV context unknown, Client-Type bug FIXED |
| Registration (Android) | STUB | Uses StubRegistration |
| Video Streaming | PARTIAL | Reads 32-byte headers + payload, no decoding |
| Audio Streaming | TODO | Not implemented |
| Controller Input | STUB | Interface only |
| Video Decode | STUB | LoggingVideoRenderer (counts packets) |
| UI | COMPLETE | Responsive Compose layout |

---

## Available Resources

### Decrypted PS3 Firmware Files
Location: `research/pupps3/decrypted_elfs/`
- `premo_plugin.elf` (285 KB) — Session handler, fully analyzed
- `premo_game_plugin.elf` (302 KB) — In-game streaming
- `libsysutil_remoteplay.elf` (15 KB) — System utility
- `sysconf_plugin.elf` (2.3 MB) — **Registration handler (the one we need)**
- `basic_plugins.elf`, `xmb_plugin.elf`, etc.

### Exported Decompiled C
- `/Users/mihailurmanschi/sysconf_plugin.elf.c` (90K lines)
- `/Users/mihailurmanschi/Work/PsOldRemotePlay/research/premo_plugin.elf.c` (18K lines)

### Ghidra Findings
Location: `research/pupps3/ghidra_findings/`
- 19 finding documents covering the full reverse engineering journey
- Complete protocol documentation in `08_COMPLETE_PROTOCOL_SUMMARY.md`
- Registration handler analysis in `10_REGISTRATION_HANDLER_FUN_00100168.md`
- Nonce formula correction in `17_NONCE_FORMULA_CORRECTED.md`
- Assembly analysis in `18_ASSEMBLY_ANALYSIS.md`

### HEN PS3 Access
- IP: 192.168.1.80, webMAN + PS3MAPI
- FTP: `curl -o file "ftp://192.168.1.80/path"`
- Web: `http://192.168.1.80/`
- Memory peek/poke via PS3MAPI
- Can install PKGs, load SPRX plugins

### Stock PS3
- IP: 192.168.1.75
- Port 9293 opens during registration mode
- Confirmed responding to PREMO protocol

### Linux Machine
- Has WiFi with wpa_supplicant
- Successfully connects to PS3's ad-hoc WiFi
- Used for registration attempts

---

## Hardware Setup
```
Mac (192.168.1.128) ─── Router ─── Stock PS3 (192.168.1.75, wired)
                          │
                          ├─── HEN PS3 (192.168.1.80, wired)
                          │
Linux Server ─────────────┘
  (has WiFi for PS3 ad-hoc connection)
```

---

## Research Files Index
- `research/INDEX.md` — Master index of all research
- `research/POC_PLAN.md` — Implementation plan
- `research/STATUS_AND_NEXT_STEPS.md` — This file
- `research/pupps3/ghidra_findings/` — All reverse engineering findings
- `research/tools/` — Python/bash scripts for registration, WiFi, parsing
- `research/open-rp-domsblog/` — Original Open-RP source code (session protocol reference)
