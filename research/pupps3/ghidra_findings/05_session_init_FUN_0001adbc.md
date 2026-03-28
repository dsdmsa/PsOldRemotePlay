# Ghidra Finding: FUN_0001adbc — Session Initializer

## Address: 0x0001adbc
## Type: Session slot initialization, codec negotiation, and session start

## Summary

This function initializes a session slot (496 bytes), negotiates video codec/resolution based on the client's capabilities and the PS3's game requirements, copies MAC and auth data, then calls `FUN_0001b8d0` which is the function that actually starts the session (generates nonce, sends 200 OK).

## Parameters

```c
int FUN_0001adbc(
    int param_1,           // Session slot pointer (0x1F0 bytes)
    int param_2,           // Parsed request data from HTTP handler
    undefined8 *param_3,   // Session ID (8 bytes)
    undefined4 param_4,    // Socket
    undefined4 param_5,    // Device key/ID
    uint param_6,          // Game exec mode / capability flags
    undefined1 param_7,    // Compression flag
    undefined4 param_8     // MAC address (6 bytes)
)
```

## Logic Flow

### 1. Copy Session Data into Slot
```c
*(param_1 + 0x44) = param_5;           // Store device key
*(param_1 + 0x08) = 0;                 // Reset state
*(param_1 + 0x04) = *(param_2 + 0x160); // Copy some config value
*(param_1 + 0x18) = *param_3;          // Store session ID (8 bytes)
memcpy(param_1 + 0x58, param_2, 0x194); // Copy ALL parsed headers (404 bytes!)
*(param_1 + 0x1CF) = 1;                // Default video codec = 1 (M4V)
```

### 2. Video Codec Selection (based on param_6 = exec mode)

The `param_6` value determines what codec capabilities are needed:

| param_6 | Meaning | Codec Selection |
|---------|---------|----------------|
| 0x00 | Standard | Check client codec abilities |
| 0x12 | Mode 0x12 | Check client codec abilities |
| 0x13 | Mode 0x13 | Check client codec abilities, special M4HD handling |
| 0x22 | Mode 0x22 | Check client codec abilities |
| 0x23 | Mode 0x23 | REQUIRES M4HD (bit 3), error if not available |

**Codec priority** (param_1 + 0x1CF values):
```
4 = M4HD (highest quality, requires bit 3 in codec ability)
3 = AVC/CAVLC (bit 1)
2 = AVC (bit 2)
1 = M4V (default/fallback)
```

The codec ability bitmask from `PREMO-Video-Codec-Ability` (at param_2 + 0x176):
```
bit 0 (0x01) = M4V
bit 1 (0x02) = AVC/CAVLC
bit 2 (0x04) = AVC
bit 3 (0x08) = M4HD
```

### 3. Audio Codec Selection

```c
// param_2 + 0x187 = audio codec ability bitmask
// bit 0 (0x01) = ATRAC
// If ATRAC supported AND codec is M4V (0x01):
//   param_1 + 0x1E0 = 0x71 ('q') → ATRAC audio
// Else:
//   param_1 + 0x1E0 = 0x72 ('r') → AAC audio
```

### 4. Resolution Clamping

**For M4HD mode (codec = 4):**
- Width: clamped to 160 (0xA0) – 864 (0x360)
- Height: clamped to 120 (0x78) – 480 (0x1E0)

**For non-M4HD modes:**
- Width: clamped to 160 (0xA0) – 480 (0x1E0)
- Height: clamped to 120 (0x78) – 272 (0x110)

Default resolution from HTTP handler: **480×272** (0x1E0 × 0x110)

### 5. Framerate Validation
```c
// param_1 + 0x1D8 = framerate
// Must be one of: 7, 10, 15, 30
// If not → default to 30
```

### 6. Touch / VideoOut Setup
```c
// param_1 + 0x1E1 = touch capability
// Only enabled if param_6 bit 5 (0x20) is set AND client reported touch=capable
FUN_0001db84(*(param_1 + 0x30), touch_enabled);
```

### 7. Store Auth Data
```c
*(param_1 + 0x4E) = param_7;           // Compression flag
memcpy(param_1 + 0x48, param_8, 6);    // Copy MAC address (6 bytes!)
```

### 8. Start Session
```c
FUN_0001a94c(param_4, *(param_1 + 0x04));  // Socket setup
iVar6 = FUN_0001b8d0(
    *(param_1 + 0x38),  // Crypto context?
    param_1 + 0x58,     // Parsed headers (copied earlier)
    param_3,            // Session ID
    param_4             // Socket
);
// FUN_0001b8d0 is THE function that sends the 200 OK response with nonce
if (success) {
    FUN_000215b0(*(param_1 + 0x24), 1);  // Signal session started
}
```

## Session Slot Layout (param_1, 0x1F0 = 496 bytes)

| Offset | Size | Purpose |
|--------|------|---------|
| +0x04 | 4 | Config value from request |
| +0x08 | 4 | Session state (0 = initializing) |
| +0x18 | 8 | Session ID |
| +0x24 | 4 | Event/signal handle |
| +0x30 | 4 | Some handle (touch related) |
| +0x38 | 4 | Crypto context pointer |
| +0x44 | 4 | Device key/ID |
| +0x48 | 6 | Client MAC address |
| +0x4E | 1 | Compression flag |
| +0x4F | 1 | Extra flag |
| +0x58 | 404 (0x194) | Copy of all parsed PREMO headers |
| +0x1CF | 1 | Video codec (1=M4V, 2=AVC, 3=CAVLC, 4=M4HD) |
| +0x1D0 | 2 | Video width (clamped) |
| +0x1D2 | 2 | Video height (clamped) |
| +0x1D8 | 4 | Framerate (7/10/15/30) |
| +0x1DD | 1 | Audio mode flag |
| +0x1E0 | 1 | Audio codec ('q'=ATRAC, 'r'=AAC) |
| +0x1E1 | 1 | Touch capability enabled |
| +0x1E4 | 4 | Encoder function pointer or bitrate |

## Key Insight

The MAC address is stored at `param_1 + 0x48` (6 bytes). This is the client's MAC address, passed from the HTTP handler. It's used later for auth token validation — the PREMO-Auth token contains the encrypted MAC.

## Next Priority: FUN_0001b8d0

This is the function that:
- Takes the crypto context, parsed headers, session ID, and socket
- Generates the PREMO nonce
- Computes the session response
- Sends the HTTP 200 OK with all response headers (including PREMO-Nonce)
- **This is where the crypto happens**

**Navigate: press G → type `0001b8d0`**
