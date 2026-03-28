# Ghidra Finding: FUN_0001f7c0 — PREMO HTTP Server Main Request Handler

## Address: 0x0001f7c0
## Type: THE CORE HTTP SERVER — routes all PREMO requests

## Summary

This is the PS3's HTTP server that handles ALL incoming Remote Play connections. It:
1. Receives HTTP requests on port 9293
2. Routes to the correct handler based on the URL path
3. Parses ALL PREMO-* headers
4. Validates auth tokens
5. Returns 403 Forbidden on failure

## Request Routing

```
GET /sce/premo/session       → Session establishment (FUN_00012c8c)
GET /sce/premo/session/video → Video stream start (FUN_0000d4c0)
GET /sce/premo/session/audio → Audio stream start (FUN_0000d378)
POST /sce/premo/session/pad  → Controller input (FUN_0000e420)
GET /sce/premo/session/ctrl  → Control channel (FUN_0001f3b8)
```

## Session Establishment Header Parsing (GET /sce/premo/session)

The session handler parses these headers in order:

| Header | Variable | Processing |
|--------|----------|------------|
| `PREMO-PSPID` | `auStack_200` (16 bytes) | Base64 decoded via FUN_00022a88, max raw length 0x18 (24 chars base64 = 16 bytes) |
| `PREMO-Version` | parsed as major.minor | Validated against `param_1+0x38` — rejects incompatible versions (error 0x80029850) |
| `PREMO-Mode` | `local_1ec` | 0 = "PREMO" mode, 1 = "REMOCON" mode. Rejects anything else. |
| `PREMO-Platform-Info` | `local_8b` | 0x00 = PSP (DAT_0002c9a0), 0x01 = "Phone", 0x02 = DAT_0002c9b0 (Vita?), 0x03 = DAT_0002c9b8 (PC/VAIO?) |
| `PREMO-UserName` | `auStack_1e0` (255 bytes) | Base64 decoded, max raw length 0x155 (341 chars) |
| `PREMO-Trans` | `local_9c` | 2 = "capable", 1 = not capable |
| `PREMO-SIGNIN-ID` | `local_e1` (65 bytes) | Base64 decoded, max raw 0x58 (88 chars). Sets `local_a0 = 1` (internet mode flag) |
| `PREMO-Video-Codec-Ability` | `local_8a` (bitmask) | bit0=M4V, bit1=AVC/CAVLC, bit2=AVC(or AVC/MP or M4HD?), bit3=DAT_0002ca40 |
| `PREMO-Video-Resolution` | `local_88` x `local_86` | Parsed as "WIDTHxHEIGHT" (default: 0x1e0 x 0x110 = 480x272) |
| `PREMO-Video-Bitrate` | `local_84` | Default: 0x7d000 (512000 = 512kbps) |
| `PREMO-Video-Framerate` | `local_80` | Default: 0x1e (30 fps) |
| `PREMO-Video-LetterBox` | `local_7c` | 1 = enabled (DAT_0002c8b0 = "on"?) |
| `PREMO-Audio-Codec-Ability` | `local_79` (bitmask) | bit1=AAC (DAT_0002cae8), bit0=ATRAC |
| `PREMO-Audio-Bitrate` | `local_74` | Integer |
| `PREMO-RTP-Size` | `local_70` | Integer |
| `PREMO-VideoOut-Ctrl` | `local_7a` | 1 = "capable" |
| `PREMO-Touch-Ability` | `local_77` | 1 = "capable" (Vita touch screen) |

## CRITICAL: Platform Info Values

```c
local_8b = 0x00  →  PSP (string at DAT_0002c9a0, likely "PSP")
local_8b = 0x01  →  Phone (string "Phone")
local_8b = 0x02  →  Unknown (DAT_0002c9b0, likely "Vita" or "PC")
local_8b = 0x03  →  Unknown (DAT_0002c9b8, likely "VAIO" or "TV")
```

**ACTION: Check DAT_0002c9a0, DAT_0002c9b0, DAT_0002c9b8 in Ghidra to see the actual platform strings**

## CRITICAL: PSPID Validation

```c
// PSPID is decoded from base64 into auStack_200 (16 bytes)
// Then later compared against registered device list:
puVar13 = (uint *)(param_1 + 0x4c);  // array of registered PSPIDs
// Loops through param_1+0x48 entries
// Compares local_29c against each *puVar13
// If match found: checks param_1+0x3c (PS3 key?) against stored key
// If local_e1[0] == '\0' (no SIGNIN-ID) AND keys don't match → 0x80029820 error
```

**This means the PS3 stores registered device IDs and validates incoming PSPID against them!**

## Session Creation Call

When PSPID is validated, calls:
```c
FUN_00012c8c(param_1+0xc, auStack_200/*PSPID*/, param_1+0x2c/*socket*/,
             param_1+0x3c/*key?*/, auth_flag, auStack_2c8/*MAC?*/, compression_flag)
```

**FUN_00012c8c is the session creation function — investigate this next!**

## Auth Validation for Video/Audio/Pad/Ctrl

For video, audio, and pad endpoints, the PS3 parses:
- `SessionID` → decoded via FUN_0001f37c
- `PREMO-Auth` → base64 decoded (max 24 bytes raw = 16 bytes decoded), stored for validation

**The auth token is NOT validated in this function — it's passed to the stream handlers (FUN_0000d4c0, FUN_0000d378, FUN_0000e420) which validate it.**

## 403 Forbidden Response Format

```
HTTP/1.1 403 Forbidden\r\n
Connection: close\r\n
Pragma: no-cache\r\n
Content-Length: 0\r\n
PREMO-Version: X.Y\r\n
PREMO-Application-Reason: XXXXXXXX\r\n
\r\n
```

The `PREMO-Application-Reason` is an 8-digit hex error code.

## Error Codes Found
- `0x80029820` — PSPID not found in registered devices, OR invalid header value
- `0x80029850` — Version mismatch
- `0x80029804` — Unknown request path

## Key Helper Functions
- `FUN_00027d48(str1, str2)` — String comparison (header name matching)
- `FUN_00029a60(str1, str2)` — String comparison (header value matching), returns 0 on match
- `FUN_00022a88(base64_str, output_buf, len)` — Base64 decode
- `FUN_00022e24(str, end_ptr, base)` — strtol (string to integer)
- `FUN_00029910(str)` — strlen
- `FUN_00029788(haystack, needle, len)` — strstr/memmem (substring search)
- `FUN_000297c0(buf, size, fmt, ...)` — snprintf
- `FUN_000294b0(socket, buf, len, flags)` — send
- `FUN_00029248(socket)` — close socket
- `FUN_0001f2c0(socket, 0, 0x1e)` — setsockopt (30 second timeout?)
- `FUN_0001b7c4(param_1)` — lock/acquire mutex
- `FUN_0001b8a4(param_1)` — unlock/release mutex

## Next Steps
1. **Check DAT_0002c9a0, DAT_0002c9b0, DAT_0002c9b8** — platform strings (PSP, Vita, PC, etc.)
2. **Investigate FUN_00012c8c** — session creation after PSPID validation (generates nonce, sends 200 OK response)
3. **Investigate the PSPID matching loop** — understand how registered devices are stored in memory
4. **Check FUN_0000d4c0** — video stream handler (validates PREMO-Auth token)
