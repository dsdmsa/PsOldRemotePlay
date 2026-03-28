# PS3 PREMO Protocol — Complete Reverse Engineering Summary

## What We Now Know (Combined: Open-RP + Ghidra Findings)

---

## STEP 1: Discovery (UDP)

**Client sends:** 4-byte UDP broadcast `SRCH` to port 9293
**PS3 responds:** 156-byte `RESP` packet:
- Bytes 0-3: `RESP`
- Bytes 10-15: PS3 MAC (6 bytes)
- Bytes 16-143: PS3 nickname (128 bytes)
- Bytes 144-155: NPX ID (12 bytes)
- **Also gives us the PS3's IP address** from the UDP reply source

---

## STEP 2: Session Request (HTTP GET)

**Client sends:**
```
GET /sce/premo/session HTTP/1.1\n
PREMO-PSPID: <base64 of 16-byte device ID>\n
PREMO-Version: 0.3\n
PREMO-Mode: PREMO\n
PREMO-Platform-Info: Phone\n
PREMO-UserName: <base64 of device owner name>\n
PREMO-Trans: capable\n
PREMO-Video-Codec-Ability: M4V,AVC,AVC/CAVLC\n
PREMO-Video-Resolution: 480x272\n
PREMO-Video-Bitrate: 512000\n
PREMO-Video-Framerate: 30\n
PREMO-Audio-Codec-Ability: M4A,ATRAC\n
PREMO-Audio-Bitrate: 128000\n
\n
```

**For internet mode, also include:**
```
PREMO-SIGNIN-ID: <base64 of PSN login email>\n
```

---

## STEP 3: PS3 Validates & Responds

### What the PS3 checks (from FUN_0001f7c0):
1. **PSPID** — base64 decoded to 16 bytes, compared against registered device list
2. **Version** — must be compatible (currently 0.3)
3. **Platform-Info** — must be one of: `PSP`, `Phone`, `PC`, `VITA`
4. If no match for PSPID → **403 Forbidden** (error 0x80029820)

### PS3 responds with 200 OK (from FUN_00010b28):
```
HTTP/1.1 200 OK\r\n
SessionID: <16-digit hex>\r\n
Connection: close\r\n
Pragma: no-cache\r\n
Content-Length: 0\r\n
PREMO-Version: 0.3\r\n
PREMO-PS3-Nickname: <base64>\r\n
PREMO-Video-Codec: AVC\r\n
PREMO-Video-Resolution: 480x272\r\n
PREMO-Video-Bitrate: 512000\r\n
PREMO-Video-Framerate: 30\r\n
PREMO-Video-Config: <hex>\r\n
PREMO-Exec-Mode: VSH\r\n
PREMO-Audio-Codec: M4A\r\n
PREMO-Audio-SamplingRate: 48000\r\n
PREMO-Audio-Channels: 2\r\n
PREMO-Audio-Bitrate: 128000\r\n
PREMO-Pad-Complete: on\r\n
PREMO-Pad-Assign: CROSS\r\n
PREMO-OTA: capable\r\n
PREMO-Trans-Mode: peer\r\n
PREMO-Nonce: <base64 of 16-byte nonce>\r\n
PREMO-Touch: off\r\n
\r\n
```

### Nonce Generation (PS3 side, from Ghidra):
```
Platform PSP:   nonce[i] = pkey[i] XOR skey2[i]
Platform Phone: nonce[i] = (i + pkey[i] + 0x29) XOR phone_key[i]
Platform PC:    nonce[i] = (i + pkey[i] + 0x33) XOR pc_key[i]
Platform VITA:  nonce[i] = (i + pkey[i] + 0x3D) XOR vita_key[i]
```

---

## STEP 4: Client Derives Session Keys (from Open-RP)

After receiving the nonce:
```
xor_pkey  = pkey XOR skey0       // AES encryption key (16 bytes)
xor_nonce = nonce XOR skey2      // AES IV (16 bytes)
```

### Auth Token Generation:
```
plaintext = [device_mac (6 bytes), 0x00 × 10 bytes]  // 16 bytes total
auth_token = AES-128-CBC-encrypt(plaintext, key=xor_pkey, iv=xor_nonce)
PREMO-Auth = base64(auth_token)
```

---

## STEP 5: Video Stream (HTTP GET)

**Client sends:**
```
GET /sce/premo/session/video HTTP/1.1\n
SessionID: <from step 3>\n
PREMO-Auth: <base64 auth token from step 4>\n
\n
```

**PS3 responds:** Long-lived HTTP response streaming encrypted video packets.

### Video Packet Format (32-byte header + payload):
```
Offset 0-1:   magic[2]     — stream type identifier
Offset 2-3:   frame        — frame counter (uint16)
Offset 4-7:   clock        — timestamp (uint32)
Offset 8-11:  root[4]      — routing info
Offset 16-17: len          — payload length (uint16, big-endian)
```

### Decryption Rules (from Open-RP):
- magic[1] == 0xFF or 0xFE: H.264 keyframe → decrypt with AES-128-CBC(xor_pkey, xor_nonce)
- magic[1] == 0xFB: MPEG4 keyframe → decrypt when unk6 == 0x0001 or 0x0401
- magic[1] == 0x80: AAC audio → decrypt when unk8 is set
- magic[1] == 0xFC: ATRAC3 audio → decrypt when unk8 is set
- **Reset IV (xor_nonce) before each packet decryption**

### After Decryption:
The payload is raw H.264 NAL units (for AVC codec) or MPEG-4 frames (for M4V).
Feed into Android MediaCodec for hardware decoding.

---

## STEP 6: Audio Stream (HTTP GET)

```
GET /sce/premo/session/audio HTTP/1.1\n
SessionID: <from step 3>\n
PREMO-Auth: <base64 auth token>\n
\n
```

Same packet format, audio-specific magic bytes.

---

## STEP 7: Controller Input (HTTP POST) — NOT NEEDED FOR POC

```
POST /sce/premo/session/pad HTTP/1.1\n
SessionID: <from step 3>\n
Content-Length: <128 × N>\n
PREMO-Pad-Index: 1\n
\n
<AES-encrypted pad state data, 128 bytes per input frame>
```

---

## ALL CRYPTOGRAPHIC KEYS

### Static Keys (same for ALL PS3s, from Open-RP + Ghidra confirmation)
```
skey0: D1 B2 12 EB 73 86 6C 7B 12 A7 5E 0C 04 C6 B8 91
skey1: 1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F
skey2: 65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59
```

### Platform-Specific Nonce XOR Keys (NEW — from Ghidra)
```
PSP:   65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59  (= skey2)
Phone: 37 63 E5 4D 12 F9 7B 73 62 3A D3 0D 10 C9 91 7E
PC:    33 72 84 4C A6 6F 2E 2B 20 C7 90 60 33 E8 29 C6
VITA:  AF 74 B5 4F 38 F8 AF C8 75 77 B2 D5 47 76 3B FD
```

### Per-Device Keys (from registration, stored in PS3 xRegistry)
```
pkey:    16 bytes — shared secret generated during registration
psp_id:  16 bytes — device identifier (sent as PREMO-PSPID)
psp_mac: 6 bytes  — device MAC address (used in auth token)
```

---

## WHAT'S NEEDED FOR THE POC

### We HAVE:
- ✅ Complete protocol flow (discovery → session → video stream)
- ✅ All static crypto keys (skey0, skey1, skey2, platform XOR keys)
- ✅ Packet format (32-byte header + encrypted payload)
- ✅ Decryption algorithm (AES-128-CBC with key=xor_pkey, iv=xor_nonce)
- ✅ Auth token generation algorithm
- ✅ All PREMO HTTP headers and their values
- ✅ PS3 validation logic (what it checks, what causes 403)
- ✅ Video codec details (H.264/AVC, 480x272, 30fps)
- ✅ Android implementation patterns (MediaCodec, OkHttp, javax.crypto)

### We STILL NEED (the blocker):
- ❌ **Per-device keys (pkey, psp_id, psp_mac)** — these come from registration
- The registration protocol is in VSH core, NOT in premo_plugin.sprx
- Workaround options:
  1. Register via patched VAIO client + extract keys (Windows VM needed)
  2. Register via PSP/Vita + extract from xRegistry
  3. Reverse-engineer the registration protocol itself (separate effort)

### For the POC:
**If we can get pkey + psp_id + psp_mac from ANY method, the POC is 100% buildable.**
The simplest path: use the patched VAIO client in a VM to register once, then extract keys.
Everything else is fully understood.
