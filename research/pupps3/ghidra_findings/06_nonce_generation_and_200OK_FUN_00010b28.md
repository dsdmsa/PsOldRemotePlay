# Ghidra Finding: FUN_00010b28 — THE SESSION RESPONSE (Nonce Generation + 200 OK)

## Address: 0x00010b28
## Type: CRITICAL — Generates nonce, builds complete 200 OK response, contains crypto XOR derivation

---

## Summary

This is the PS3's session response builder. It:
1. Builds the complete HTTP 200 OK response with ALL session headers
2. **Generates the PREMO nonce using XOR key derivation** (the crypto we need to understand)
3. Sends the response to the client
4. Manages the session lifecycle (video/audio/pad stream events)

---

## The 200 OK Response — Complete Header List

The PS3 sends back (in order):

```
HTTP/1.1 200 OK\r\n
SessionID: <16-digit hex session ID>\r\n
Connection: close\r\n
Pragma: no-cache\r\n
Content-Length: 0\r\n
PREMO-Version: X.Y\r\n
PREMO-PS3-Nickname: <base64 encoded nickname>\r\n
PREMO-Video-Codec: <selected codec>\r\n
PREMO-Video-Resolution: WIDTHxHEIGHT\r\n
PREMO-Video-Bitrate: <bitrate>\r\n
PREMO-Video-Bitrate-Ability: <min>-<max>\r\n
PREMO-Video-Framerate: <fps>\r\n
PREMO-Video-Framerate-Ability: <min>-<max>\r\n
PREMO-Video-Config: <hex config string>\r\n
PREMO-Exec-Mode: <VSH|GAME|PS1|SUBDISPLAY|ELSE>\r\n
PREMO-Touch: <on|off>\r\n
PREMO-Audio-Codec: <M4A|AT3>\r\n
PREMO-Audio-SamplingRate: 48000\r\n
PREMO-Audio-Channels: 2\r\n
PREMO-Audio-Bitrate: <bitrate>\r\n
PREMO-Audio-Bitrate-Ability: <min>-<max>\r\n
PREMO-Pad-Complete: <on|off>\r\n
PREMO-Pad-Assign: <CIRCLE|CROSS>\r\n
PREMO-OTA: capable\r\n
PREMO-Trans-Mode: <peer|group>\r\n     (if applicable)
PREMO-Nonce: <base64 encoded nonce>\r\n (if applicable)
\r\n
```

---

## CRITICAL: Exec Mode Values

```c
if (*param_1 == 0x00) pcVar21 = "VSH";        // XMB menu
if (*param_1 & 0x10)  pcVar21 = "GAME";       // PS3 game running
if (*param_1 & 0x20)  pcVar21 = "SUBDISPLAY";  // Sub-display mode (NEW — not in Open-RP!)
if (*param_1 == 0x40) pcVar21 = "PS1";        // PS1 classic
else                  pcVar21 = "ELSE";       // Unknown
```

**New discovery: "SUBDISPLAY" mode (0x20)** — not documented in Open-RP!

---

## CRITICAL: Nonce Generation / XOR Key Derivation

The nonce is generated **only when `param_1[0x17] != 0`** (trans mode is active).

### The XOR Derivation (4 variants based on platform type)

The nonce source is at `param_1 + 0x69` (16 bytes — this is likely the pkey or a derived key).
The nonce output goes to `param_1 + 0x79` (16 bytes).
It's then base64-encoded and sent as `PREMO-Nonce:`.

```c
char platform_type = *(param_1 + 0x1a);  // Platform type from earlier

if (platform_type == 0) {
    // PSP — Simple XOR with static key at DAT_0002d558
    for (i = 0; i < 16; i++) {
        nonce[i] = source_key[i] ^ DAT_0002d558[i];
    }
}
else if (platform_type == 1) {
    // Phone — XOR with index offset + 0x29 + static key at DAT_0002d578
    for (i = 0; i < 16; i++) {
        nonce[i] = (i + source_key[i] + 0x29) ^ DAT_0002d578[i];
    }
}
else if (platform_type == 2) {
    // PC/VAIO — XOR with index offset + 0x33 + static key at DAT_0002d598
    for (i = 0; i < 16; i++) {
        nonce[i] = (i + source_key[i] + 0x33) ^ DAT_0002d598[i];
    }
}
else {
    // VITA (type 3) — XOR with index offset + 0x3D + static key at DAT_0002d5B8
    for (i = 0; i < 16; i++) {
        nonce[i] = (i + source_key[i] + 0x3D) ^ DAT_0002d5B8[i];
    }
}

// Base64 encode the 16-byte nonce
FUN_00022958(nonce, auStack_f0, 16);

// Append to response
snprintf(buf + len, "PREMO-Nonce: %s\r\n", auStack_f0);
```

### Static XOR Keys (MUST EXTRACT FROM GHIDRA)

| Address | Platform | Magic Offset | Purpose |
|---------|----------|--------------|---------|
| `DAT_0002d558` | PSP (0) | none | 16-byte XOR key for PSP nonce |
| `DAT_0002d578` | Phone (1) | `i + 0x29` | 16-byte XOR key for Phone nonce |
| `DAT_0002d598` | PC (2) | `i + 0x33` | 16-byte XOR key for PC nonce |
| `DAT_0002d5B8` | VITA (3) | `i + 0x3D` | 16-byte XOR key for VITA nonce |

**These 4 static keys are 16 bytes each and are ESSENTIAL for implementing the protocol.**
**ACTION: In Ghidra, go to each address and dump the 16 bytes!**

### Magic Offset Pattern
```
PSP:   direct XOR (no offset)
Phone: (byte_index + source + 0x29) XOR key
PC:    (byte_index + source + 0x33) XOR key
VITA:  (byte_index + source + 0x3D) XOR key

Note: 0x29 = ')', 0x33 = '3', 0x3D = '='
The offsets increase by 0x0A (10) each: 0x29, 0x33, 0x3D
```

---

## Audio Codec in Response

```c
if (*(param_1[0x43] + 0x188) == 'r') {
    // 'r' = 0x72 → AAC
    audio_header = "PREMO-Audio-Codec: M4A\r\n";
} else {
    // 'q' = 0x71 → ATRAC3
    audio_header = "PREMO-Audio-Codec: AT3\r\n";
}
```

Audio is always: 48000 Hz, 2 channels.

---

## Video Config String

The `PREMO-Video-Config` header contains a hex-encoded configuration string. For AVC/CAVLC codecs (types 2, 3), it uses pre-built config strings from pointers:
- Different configs for different resolutions (480, 432, other)
- For M4V (type 1), the config is dynamically generated from width/height

---

## Pad Settings

```c
if (*param_1 == 0) {
    // VSH mode: full pad
    "PREMO-Pad-Complete: on"
} else {
    // Game/other: limited pad
    "PREMO-Pad-Complete: off"
}

if (param_1[0x18] == 0) {
    "PREMO-Pad-Assign: CIRCLE"  // Japanese style
} else {
    "PREMO-Pad-Assign: CROSS"   // Western style
}
```

---

## Touch Support

```c
if (*(param_1[0x43] + 0x189) == '\0') {
    "PREMO-Touch: off"  // DAT_0002c3b0
} else {
    "PREMO-Touch: on"   // DAT_0002c3a8
}
```

---

## Trans Mode

```c
if (param_1[0x19] == 0) {
    "PREMO-Trans-Mode: peer"   // Direct/ad-hoc
} else {
    "PREMO-Trans-Mode: group"  // Infrastructure
}
```

---

## 403 Forbidden Response (error case)

When `uStack_100 & 4` (error signal):
```
HTTP/1.1 403 Forbidden\r\n
Connection: close\r\n
Pragma: no-cache\r\n
Content-Length: 0\r\n
PREMO-Application-Reason: XXXXXXXX\r\n
\r\n
```

---

## Session Lifecycle After 200 OK

After sending the response, the function enters an event loop waiting for:
- Video stream ready (bit 3 → 0x08)
- Audio stream ready (bit 4 → 0x10)
- Pad stream ready (bit 5 → 0x20)
- Error/disconnect (bit 2 → 0x04, bit 6 → 0x40)

Timeout: 30 seconds for each stream to connect, 180 seconds initial wait.

---

## Next Steps

### IMMEDIATE: Extract the 4 static XOR keys

In Ghidra, navigate to these addresses and dump 16 bytes each:
1. `0x0002d558` — PSP XOR key
2. `0x0002d578` — Phone XOR key
3. `0x0002d598` — PC XOR key
4. `0x0002d5B8` — VITA XOR key

### THEN: Investigate FUN_00022958
This is the base64 encoder used for the nonce. Address: `0x00022958`

### THEN: Understand the source key at param_1 + 0x69
Where does this 16-byte source key come from? It's likely the pkey loaded from xregistry during registration. Trace back how param_1 + 0x69 gets populated.
