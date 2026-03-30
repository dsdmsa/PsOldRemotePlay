# Ghidra Reverse Engineering Findings — premo_plugin.elf (FW 4.90)

## Summary

The `premo_plugin.sprx` is the PS3-side Remote Play module. It handles:
- **Streaming sessions** (video, audio, controller input) — FULLY documented here
- **UI dialogs** during Remote Play (connect prompts, errors)
- **HTTP server** on port 9293 (the PREMO protocol server)

It does **NOT** handle device registration/pairing — that's in VSH core modules. However, it DOES validate registered devices during session establishment, which tells us exactly what our Android app needs to send.

---

## Findings

### [01 — Dialog Handler (FUN_0000ab1c)](./01_dialog_handler_FUN_0000ab1c.md)
XMB UI state machine showing all Remote Play dialog messages. Reveals the user-facing flow: connect prompts, registration requests, errors, wait states. State variable `DAT_0002f114` controls which dialog is shown.

### [02 — Key Strings and Addresses](./02_key_strings_and_addresses.md)
Complete map of all protocol-relevant strings in the binary with their hex addresses. Includes all PREMO-* headers, HTTP endpoints, thread names, codec strings. Critical for navigating Ghidra.

### [03 — HTTP Request Handler (FUN_0001f7c0)](./03_http_handler_FUN_0001f7c0.md)
**THE MOST IMPORTANT FINDING.** The PS3's PREMO HTTP server main handler. Routes all requests, parses all headers, validates PSPID against registered devices, dispatches to stream handlers. Reveals:
- 4 platform types: PSP (0x00), Phone (0x01), type 0x02, type 0x03
- PSPID validated against a registered device list
- Session creation via FUN_00012c8c
- 403 Forbidden response format with error codes
- Default video: 480x272 @ 30fps @ 512kbps

---

### [04 — Platform Strings + Session Create (FUN_00012c8c)](./04_platform_strings_and_session_create.md)
Platform types confirmed: PSP="PSP", Phone="Phone", PC="PC", VITA="VITA". Session creation uses 496-byte session slots, generates random session IDs, has 60-second reconnect cooldown. Delegates to FUN_0001adbc.

### [05 — Session Initializer (FUN_0001adbc)](./05_session_init_FUN_0001adbc.md)
Codec negotiation: 4 video codecs (M4V, AVC, CAVLC, M4HD), 2 audio (ATRAC, AAC). Resolution clamping: max 480x272 for non-M4HD, 864x480 for M4HD. Valid framerates: 7/10/15/30. Stores MAC address (6 bytes). Delegates to FUN_0001b8d0 → FUN_00010b28.

### [06 — Nonce Generation & 200 OK Response (FUN_00010b28)](./06_nonce_generation_and_200OK_FUN_00010b28.md)
**THE CROWN JEWEL.** Complete 200 OK response builder with ALL headers. Contains **the nonce XOR key derivation** — 4 different derivations per platform type (PSP/Phone/PC/VITA) using static XOR keys at DAT_0002d558/d578/d598/d5B8 plus platform-specific magic offsets (0x29/0x33/0x3D). New exec mode "SUBDISPLAY" discovered. Static XOR keys need extraction from Ghidra.

### [10 — Registration Handler (FUN_00100168 in sysconf_plugin)](./10_REGISTRATION_HANDLER_FUN_00100168.md)
**THE REGISTRATION PROTOCOL — FULLY REVERSE ENGINEERED.** Found in sysconf_plugin.elf. Registration is `POST /sce/premo/regist HTTP/1.1` with AES-encrypted body containing Client-Type/Id/Mac/Nickname. PS3 generates random 16-byte pkey and responds with encrypted body containing PREMO-Key, PS3-Mac, PS3-Nickname, AP-Ssid, AP-Key. Different encryption per device type (PSP/Phone/PC-VITA).

### [11 — Registration Encryption Keys (EXTRACTED)](./11_registration_encryption_keys.md)
All 6 registration AES keys (3 XOR keys + 3 IV bases) for PSP, Phone, and PC/VITA types. Includes key derivation algorithms per device type with exact formulas. One remaining unknown: the 8-byte context value XOR'd into the IV.

### [07 — Static XOR Keys (EXTRACTED)](./07_static_xor_keys.md)
All 4 platform-specific nonce XOR keys dumped from binary. PSP key = skey2 (confirmed). Phone/PC/VITA keys are NEW discoveries. Also found skey1 at DAT_0002d5c8 and 3 unknown 16-byte keys interleaved (likely auth validation keys).

### [08 — COMPLETE PROTOCOL SUMMARY](./08_COMPLETE_PROTOCOL_SUMMARY.md)
**Full protocol documentation** combining Open-RP knowledge + all Ghidra findings. Step-by-step: discovery → session → key derivation → video stream → decryption. All crypto keys listed. POC feasibility assessment.

### [12 — Context Value Analysis (UPDATED)](./12_context_value_analysis.md)
Deep analysis of `*(param_1 + 0x38)` — the 8-byte IV XOR context value. Traces through call chain to `FUN_000fe6b8` which stores `(longlong)(int)PIN` at offset 0. Updated with definitive call chain proof and test results showing PIN-as-BE-int was already tried but failed.

### [20 — DECOMPILED Registration Handler (COMPLETE CODE)](./20_DECOMPILED_REGISTRATION_HANDLER.md)
**BREAKTHROUGH: Full decompiled C code** of `UndefinedFunction_00100168` — the 5KB function that was previously a gap in the C export. Contains EXACT key derivation loops for all 3 device types, IV XOR logic, HTTP request/response parsing, AES encrypt/decrypt calls, field validation, error codes, and pkey generation. Two static WiFi passphrase strings discovered. The remaining blocker: PIN-as-BE-int context value was already tested and failed — either the static keys are wrong, or there's a parameter shift issue.

### [21 — Research Compilation (ALL FINDINGS)](./21_RESEARCH_COMPILATION.md)
**Comprehensive compilation** of all research: xRegistry.sys bypass procedure, open-rp source analysis (registration completely skipped), PS3 AES-CBC IV behavior, WiFi AP details, PS3MAPI capabilities/limitations on HEN, ELF segment layout, BSS memory mapping behavior, and prioritized action plan.

### [22 — VAIO DLL Analysis (VRPSDK.dll)](./22_VAIO_DLL_ANALYSIS.md)
**VAIO Remote Play Windows client reverse engineering.** Installed v1.1 via Wine, built custom C dump tools to bypass Themida packing. All 11 static crypto keys confirmed matching PS3 firmware. AES implementation fully mapped (S-box at +0x27010, SetKey at 0x1D60, SetIV at 0x1010). Session key derivation visible (nonce+17, XOR "sess"/"chan"). Registration key derivation jumps into **Themida VM-protected code** — cannot be statically analyzed. RTTI reveals `CCoreRegistration`, `CAesCipher` classes. Registration object stores key at +0x460, IV at +0x470. **Critical finding:** PC type registration differs from PSP/Phone — key material is PIN-derived (not random), body encrypted from offset 0x1E0 (not 0), minimum 512-byte body. Tools in `research/tools/dump_vrpsdk*.c`.

---

## Still Need to Investigate

| Priority | Address | What | Why |
|----------|---------|------|-----|
| **1** | `0x00012c8c` | FUN_00012c8c — Session creation | Generates nonce, sends 200 OK, establishes session |
| **2** | `0x0002c9a0` | DAT_0002c9a0 — Platform string | What string = PSP? Check 0x0002c9b0 and 0x0002c9b8 too |
| **3** | `0x0000d4c0` | FUN_0000d4c0 — Video stream handler | How auth is validated for video streams |
| **4** | `0x0001f3b8` | FUN_0001f3b8 — Control channel handler | Bitrate changes, session termination |
| **5** | `0x0000e420` | FUN_0000e420 — Pad input handler | Controller input processing |
| **6** | `0x0000d378` | FUN_0000d378 — Audio stream handler | Audio stream setup |

## Architecture Diagram (from findings so far)

```
Client (PSP/Phone/Vita/VAIO)          PS3 (premo_plugin.sprx)
  │                                      │
  │  UDP "SRCH" broadcast ──────────────>│ cellPremoAcceptThread
  │<──────────────── 156-byte "RESP" ────│
  │                                      │
  │  GET /sce/premo/session ────────────>│ FUN_0001f7c0 (HTTP handler)
  │  Headers: PREMO-PSPID, Version,      │   ├─ Parse all PREMO-* headers
  │    Mode, Platform-Info, UserName,     │   ├─ Validate PSPID against registered list
  │    Trans, SIGNIN-ID, codec abilities  │   ├─ Check version compatibility
  │                                      │   └─ Call FUN_00012c8c (session create)
  │<──── 200 OK + Nonce + SessionID ─────│
  │                                      │
  │  GET /sce/premo/session/video ──────>│ FUN_0000d4c0 (video handler)
  │  Headers: SessionID, PREMO-Auth      │   └─ Validate auth, start H.264/MPEG4 stream
  │<──── Encrypted video stream ─────────│
  │                                      │
  │  GET /sce/premo/session/audio ──────>│ FUN_0000d378 (audio handler)
  │  Headers: SessionID, PREMO-Auth      │   └─ Validate auth, start AAC/ATRAC3 stream
  │<──── Encrypted audio stream ─────────│
  │                                      │
  │  POST /sce/premo/session/pad ───────>│ FUN_0000e420 (pad handler)
  │  Headers: SessionID, Content-Length,  │   └─ Decrypt and process controller input
  │    PREMO-Pad-Index, Transfer-Encoding │
  │                                      │
  │  GET /sce/premo/session/ctrl ───────>│ FUN_0001f3b8 (control handler)
  │                                      │   └─ Bitrate changes, session-term
```

## Key Global Variables

| Address | Name | Purpose |
|---------|------|---------|
| `0x0002f114` | DAT_0002f114 | UI dialog state |
| `0x0002f118` | DAT_0002f118 | Error code for UI |
| `0x0002f10c` | DAT_0002f10c | Dialog handle |
| `0x0002f11c` | DAT_0002f11c | Sub-state flag (button count) |
| `0x0002f1d0` | DAT_0002f1d0 | Device type flags (bit 0x1000) |
| `0x0002f1d4` | DAT_0002f1d4 | Cancel button action |
| `0x0002f1d8` | DAT_0002f1d8 | Version/capability field |
| `0x0002f128` | DAT_0002f128 | Cross button visibility |
| `0x0002f129` | DAT_0002f129 | Circle button visibility |

## Key Helper Functions (identified)

| Address | Signature | Purpose |
|---------|-----------|---------|
| `0x00027d48` | FUN_00027d48(str1, str2) | strcmp — header name comparison |
| `0x00029a60` | FUN_00029a60(str1, str2) | strcmp — header value comparison (0=match) |
| `0x00022a88` | FUN_00022a88(b64_str, out, len) | Base64 decode |
| `0x00022e24` | FUN_00022e24(str, endptr, base) | strtol — string to integer |
| `0x00029910` | FUN_00029910(str) | strlen |
| `0x00029788` | FUN_00029788(haystack, needle, len) | strstr/memmem |
| `0x000297c0` | FUN_000297c0(buf, size, fmt, ...) | snprintf |
| `0x000294b0` | FUN_000294b0(sock, buf, len, flags) | send |
| `0x00029248` | FUN_00029248(sock) | close socket |
| `0x00029ad0` | FUN_00029ad0(buf, val, len) | memset |
| `0x00029b08` | FUN_00029b08(dst, src, len) | memcpy |
| `0x000277d0` | FUN_000277d0(name) | Get plugin resource handle |
| `0x00026810` | FUN_00026810(handle, key) | Load localized string |
| `0x00026730` | FUN_00026730(handle, page) | Get dialog page |
| `0x0001b7c4` | FUN_0001b7c4(ctx) | Lock mutex |
| `0x0001b8a4` | FUN_0001b8a4(ctx) | Unlock mutex |
| `0x0001f37c` | FUN_0001f37c(str, out) | Parse SessionID |
| `0x0001f2c0` | FUN_0001f2c0(sock, 0, 30) | setsockopt (30s timeout) |
| `0x0001ee50` | FUN_0001ee50(str, delim, out) | Split string (comma-separated codec list) |
| `0x00020a50` | FUN_00020a50(sock, buf, len) | recv (read from socket) |
| `0x00029590` | FUN_00029590(...) | HTTP header parser |
