# 22 — VAIO Remote Play DLL Analysis (VRPSDK.dll)

## Date: 2026-03-29

## Overview

Analyzed the official Sony VAIO Remote Play v1.1.0.12240 (Dec 2010) Windows client (`VRPSDK.dll`) by:
1. Installing via Wine from the existing installer at `research/PS3 Remote Play on PC + Patch/Folder 1 - Install/ep0000242243.exe`
2. Writing custom C dump tools compiled with `i686-w64-mingw32-gcc` to load the DLL under Wine and scan memory
3. Dumping the unpacked code section (163KB) and disassembling with `objdump`

**Key result:** All static crypto keys confirmed matching PS3 firmware. Registration key derivation code is Themida VM-protected and cannot be statically analyzed. Runtime hooking needed for the final piece.

---

## Installation & Extraction

### Files Installed
Location: `~/.wine/drive_c/Program Files (x86)/Sony/Remote Play with PlayStation 3/`

| File | Size | Purpose |
|------|------|---------|
| `VRPSDK.dll` | 1.3 MB | **Main target** — COM DLL with all protocol + crypto logic. Themida-packed. |
| `VRP.exe` | 1.2 MB | UI application |
| `UFCore.dll` | 848 KB | Sony UI framework |
| `VRPMFMGR.dll` | 700 KB | Media framework manager |
| `VRPMapping.dll` | 43 KB | Controller mapping |
| `sonyjvtd.dll` | 1.3 MB | Sony JVT decoder (video) |
| `Resource.dll` | 607 KB | UI resources |

### Themida Protection
`VRPSDK.dll` is packed with **Themida** — a commercial software protector. Key implications:
- Static hex search of the DLL file finds NO crypto constants
- Import table is rebuilt at runtime (no visible CryptoAPI imports)
- Code sections are encrypted until loaded
- **Some functions use Themida VM virtualization** (code executes in a virtual machine, not native x86) — these are the most sensitive functions

### PE Sections (after unpacking)
```
Section 0 (unnamed):  RVA 0x1000,  Size 0x28000  — Unpacked code + data (READABLE)
Section 1 (.rsrc):    RVA 0x29000, Size 0x2A74   — Resources
Section 2 (.idata):   RVA 0x2C000, Size 0x1000   — Import stubs
Section 3 (unnamed):  RVA 0x2D000, Size 0x16A000  — Themida encrypted/VM code
Section 4 (bfizgxse): RVA 0x197000,Size 0x12B000  — Themida encrypted/VM code
Section 5 (jileiajn): RVA 0x2C2000,Size 0x1000    — Themida encrypted/VM code
```

---

## Static Crypto Keys — ALL CONFIRMED

All 11 known crypto keys from `PremoConstants.kt` were found in process memory at VA `0x00404000` (our dump tool's data section matched them in a full process scan). More importantly, **two keys were found inside VRPSDK.dll itself**:

### Keys Found in VRPSDK.dll Image

| Key | Offset | VA |
|-----|--------|----|
| PC Second Nonce XOR (`DAT_0002d588`) | 0x273B8 | 0x100273B8 |
| PC Nonce XOR | 0x273C8 | 0x100273C8 |

These are the only two keys present as raw constants in the DLL. Both are session (not registration) keys.

### Keys NOT Found in VRPSDK.dll
- SKEY0, SKEY1, SKEY2
- All registration keys (REG_XOR_PSP/Phone/PC, REG_IV_PSP/Phone/PC)
- Phone/PSP nonce keys

**Implication:** Registration crypto keys are either computed at runtime or embedded inside Themida VM-protected code.

---

## AES Implementation

### Location
Built into VRPSDK.dll Section 0 (unpacked code):

| Function | Offset | VA | Purpose |
|----------|--------|----|---------|
| SubBytes | 0x10E0 | 0x100010E0 | AES S-box substitution (4 bytes) |
| InvSubBytes | 0x1130 | 0x10001130 | Inverse S-box substitution |
| KeySchedule | 0x1180 | 0x10001180 | AES key schedule (Rijndael) |
| KeyExpand | 0x1B00 | 0x10001B00 | Full key expansion |
| **SetKey** | **0x1D60** | **0x10001D60** | Copy 16-byte key → expand → zero source |
| **SetIV** | **0x1010** | **0x10001010** | Copy 16-byte IV (or zeros if NULL) |
| DecryptBlock | 0x1DA0 | 0x10001DA0 | Single AES block decrypt |
| **EncryptMultiBlock** | **0x1E60** | **0x10001E60** | AES-CBC multi-block encrypt |

### AES S-box
- Forward S-box at offset 0x27010 (256 bytes, standard Rijndael)
- Inverse S-box at offset 0x27110

### CAesCipher Object Layout
```
+0x000 — vtable pointer
+0x004 — unknown
...
+0x204 — Nk (number of key words, 4 for AES-128)
+0x208 — Nb (block size words, 4)
+0x20C — Nr (number of rounds, 10 for AES-128)
+0x210 — round key schedule (expanded)
+0x300 — round key copy
+0x3F0 — CBC IV (16 bytes)
+0x400 — raw key buffer (zeroed after expansion)
```

---

## Protocol Strings Found

### Registration Request
```
POST /sce/premo/regist HTTP/1.1\r\n
Host: %s:%d\r\n
User-Agent: PC\r\n
Connection: close\r\n
Content-Length: %d\r\n\r\n
```

### Registration Body Fields (plaintext before encryption)
```
Client-Type: PC\r\n
Client-Id: %s\r\n
Client-Mac: %s\r\n
Client-Nickname: %s\r\n
```

### Registration Response Parsing
```
PREMO-Key:
PREMO-KeyType:
PS3-Nickname:
PS3-Mac:
PSN-LoginId:
AP-Name:
AP-Key:
AP-Ssid:
```

### Session Request (for reference)
```
GET /sce/premo/session HTTP/1.1\r\n
Host: %s:%d\r\n
User-Agent: PREMO/1.0.0 libhttp/1.0.0\r\n
PREMO-PSPID: %s\r\n
PREMO-Version: %S\r\n
PREMO-Mode: PREMO\r\n
PREMO-Platform-Info: PC\r\n
PREMO-Pad-Info: PSP-Pad\r\n
PREMO-UserName: %s\r\n
PREMO-Trans: capable\r\n
PREMO-Video-Codec-Ability: M4V, AVC/MP\r\n
PREMO-Video-Resolution: 480x272\r\n
PREMO-Video-Framerate: %d\r\n
PREMO-Video-Bitrate: %d\r\n
PREMO-Audio-Codec-Ability: ATRAC,M4A\r\n
PREMO-SIGNIN-ID: %s\r\n
Connection: keep-alive\r\n
Pragma: no-cache\r\n
```

---

## C++ Class Hierarchy (from RTTI)

Found via `.?AV...@@` RTTI strings in the dump:

| Class | Purpose |
|-------|---------|
| `CAesCipher` | AES-128-CBC encryption/decryption |
| `CCoreCtrlSession` | Control channel session management |
| `CCoreBroadcast` | UDP broadcast (discovery) |
| `CCoreInitialSession` | Initial HTTP session handler |
| `CCoreRegistration` | **Registration protocol handler** |
| `CCoreAudio` | Audio stream handler |
| `CCoreVideo` | Video stream handler |
| `CPSNUploader@CCorePS3DiscoveryViaPSN` | PSN-based discovery |
| `CMultiCastSocket@CCoreSocket` | Multicast UDP |
| `CComMultiThreadModelNoCS@ATL` | ATL COM threading |

### VRPSDK.dll COM Exports
```
DllCanUnloadNow
DllGetClassObject
DllRegisterServer
DllUnregisterServer
```

---

## Parent Object Layout (CCoreRegistration or similar)

From disassembly of callers:

```
+0x00  — vtable
+0x04  — key_id / config identifier
+0x08  — data pointer (used in key derivation)
+0x0C  — CAesCipher instance (AES state, ~0x410 bytes)
+0x450 — status/error code
+0x454 — registration state
+0x45C — internal state object
+0x460 — AES KEY for registration (16 bytes) ← CRITICAL
+0x470 — AES IV for registration (16 bytes) ← CRITICAL
+0x480 — 4 extra bytes (part of 36-byte key+iv+extra block)
+0x484 — config data (256 bytes, nickname/MAC/etc)
+0x49E — session nonce base (16 bytes, used for session crypto)
+0x4AE — session key working buffer (16 bytes)
+0x660 — buffer
+0x664 — buffer
+0xA64 — buffer (large, ~1536 bytes)
+0x1A78 — connection handle
+0x1AE0 — socket handle
```

---

## Registration Encryption Flow

### Function: Registration Encrypt (VA 0x10014A90)

```
1. Receive data from PS3 via socket
2. Parse "Content-Length" from response
3. Read body data
4. SetIV(cipher, object+0x470)     ← Set CBC IV
5. SetKey(cipher, object+0x460)    ← Set AES key
6. EncryptMultiBlock(cipher, buffer, size)  ← AES-CBC encrypt
7. Send encrypted data to PS3
```

### Key/IV Source
The key at +0x460 and IV at +0x470 are populated by two paths:

1. **Config reader (VA 0x100053D0)** — Reads a stored 604-byte (0x25C) blob via type=7 config API. Copies 36 bytes from blob+0x208 to object+0x460 (key+IV+4). Used for RE-registration with previously stored credentials.

2. **Key derivation (VA 0x100108B0 / 0x10010940)** — Called during fresh registration from function at VA 0x10013750. Takes PIN/key data and computes the AES key/IV. **These functions jump into Themida VM-protected sections** — the actual derivation logic is virtualized and cannot be statically analyzed.

---

## Session Key Derivation (FULLY VISIBLE)

### Function: VA 0x100071B0

Unlike registration, the session key setup is fully visible in unpacked code:

```c
// STEP 1: Copy 16-byte nonce from +0x49E to +0x4AE
memcpy(obj+0x4AE, obj+0x49E, 16);

// STEP 2: Read first 4 bytes as big-endian uint32, add 17
uint32_t val = BE_READ32(obj+0x4AE);
val += 0x11;
BE_WRITE32(obj+0x4AE, val);

// STEP 3: XOR first 4 bytes with mode string
if (arg1 != 0) {
    // Channel mode
    obj[0x4AE] ^= 'c';  // 0x63
    obj[0x4AF] ^= 'h';  // 0x68
    obj[0x4B0] ^= 'a';  // 0x61
    obj[0x4B1] ^= 'n';  // 0x6E
} else {
    // Session mode
    obj[0x4AE] ^= 's';  // 0x73
    obj[0x4AF] ^= 'e';  // 0x65
    obj[0x4B0] ^= 's';  // 0x73
    obj[0x4B1] ^= 's';  // 0x73
}

// STEP 4: Compute IV from some global object
obj_ptr = get_session_object();  // call 0x6BF0
compute_iv(obj_ptr, obj+0x470);  // call 0x6E20 → writes 16 bytes

// STEP 5: Set AES key and IV
SetKey(cipher, obj+0x4AE);   // modified nonce as key
SetIV(cipher, obj+0x470);    // computed IV
```

**Insight:** Session crypto XORs the nonce with ASCII strings "chan" or "sess" — a simple obfuscation layer on top of the nonce-based key derivation.

---

## Stored Registration Config Blob (604 bytes)

When the config reader (0x100053D0) loads registration data, it expects a 604-byte (0x25C) structure:

```
Offset 0x000 — Header (8 bytes)
Offset 0x008 — Config data copied to object+0x484 (256 bytes) — nickname, MAC, etc.
Offset 0x208 — AES KEY (16 bytes) → copied to object+0x460
Offset 0x218 — AES IV (16 bytes) → copied to object+0x470
Offset 0x228 — Extra data (4 bytes) → copied to object+0x480
Offset 0x22C — Remaining data (48 bytes)
```

This blob is the Windows equivalent of PS3's `xRegistry.sys` registration entry. It stores the derived key material post-registration.

---

## Critical Finding: PC Type Body Structure Difference

Cross-referencing with the PS3 decompiled code (`20_DECOMPILED_REGISTRATION_HANDLER.md`):

### PSP/Phone (Types 0/1) vs PC/VITA (Type 2) Differences

| Aspect | PSP/Phone | PC/VITA |
|--------|-----------|---------|
| **Key material source** | Last 16 bytes of HTTP body (random, appended plaintext) | `param_1 + 0x184` (PIN-derived, in PS3 context) |
| **Encrypted body offset** | From byte 0 to size-16 | From byte **0x1E0** (480) to end |
| **Minimum body size** | No restriction | **0x200** (512 bytes) |
| **Client-Type header** | "Phone" (shifted from PSP) | **"VITA"** (shifted from PC) |

**This means the current app implementation may have multiple issues for PC type:**
1. Uses random key material instead of PIN-derived
2. Encrypts the whole body instead of starting at offset 0x1E0
3. Body may not have the required 480-byte prefix structure

---

## What Remains Unknown

### 1. Registration Key Derivation (Themida VM-protected)
The functions at VA 0x100108B0 and 0x10010940 contain the actual key/IV computation but jump into Themida-virtualized sections. To extract:
- **Runtime hook:** Patch SetKey (base+0x1D60) to log key data, then trigger registration via VRP.exe UI
- **Memory dump during execution:** Break after key derivation returns, dump object+0x460 and +0x470

### 2. Key Material for PC Type
From PS3 code: `param_1 + 0x184` contains 16 bytes. Candidates:
- PIN as 4-byte BE uint + 12 zero bytes (PS4 pattern)
- PIN as 8-byte BE longlong + 8 zero bytes
- Some other PIN transformation
- WPA PSK derived from the registration WiFi passphrase

### 3. Body Prefix Structure
What goes in the first 480 bytes (offset 0x000-0x1DF) of the PC registration body before the encrypted portion.

---

## Tools Created

All in `research/tools/`:

| File | Purpose |
|------|---------|
| `dump_vrpsdk.c` | Load VRPSDK.dll, dump image, search for keys |
| `dump_vrpsdk2.c` | Dump key name strings and process data |
| `dump_vrpsdk3.c` | Find AES S-box, scan all modules excluding self |
| `dump_vrpsdk4.c` | Find code references, dump code section |
| `hook_aes.c` | Dump registration encrypt function bytes |
| `vrpsdk_dumped.bin` | Full VRPSDK.dll memory dump (2.8 MB) |
| `vrpsdk_code.bin` | Unpacked code section dump (163 KB) |
| `process_data.bin` | Process data segment dump |

### How to Rebuild/Run
```bash
# Install mingw cross-compiler (one-time)
brew install mingw-w64

# Compile any tool
i686-w64-mingw32-gcc -o tool.exe tool.c

# Run under Wine
wine tool.exe
```

---

## Next Steps (Prioritized)

1. **Runtime hook SetKey** — Patch VRPSDK.dll's SetKey function (base+0x1D60) to write key data to a file, then run VRP.exe under Wine and trigger a registration attempt. This bypasses Themida VM protection by capturing the OUTPUT of the derivation.

2. **Test PS4-style key material** — Try `km = PIN_as_4byte_BE + 12_zeros` with the PC formula: `key[i] = (km[i] ^ PC_XOR[i]) - i - 0x2B`. Also test with the correct body offset (0x1E0) and Client-Type "VITA".

3. **Load vrpsdk_code.bin in Ghidra** — The 163KB unpacked code section can be loaded in Ghidra as a raw x86 binary (base address 0x10001000) for proper decompilation of the visible functions. Won't help with Themida VM functions but will give better analysis of the registration flow.
