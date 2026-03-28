# Ghidra Finding: Platform Strings + Session Creation (FUN_00012c8c)

## Platform Strings — CONFIRMED

| Address | Hex Bytes | String | Platform Code (local_8b) |
|---------|-----------|--------|--------------------------|
| `0x0002c9a0` | 50 53 50 00 | **"PSP"** | 0x00 |
| `0x0002c9a8` | (nearby) | **"Phone"** | 0x01 |
| `0x0002c9b0` | 50 43 00 | **"PC"** | 0x02 |
| `0x0002c9b8` | 56 49 54 41 00 | **"VITA"** | 0x03 |

### Implications for Our App
The PS3 accepts **four platform types** via the `PREMO-Platform-Info` header:
- `PSP` — original PSP
- `Phone` — Sony Ericsson / Xperia phones
- `PC` — VAIO Remote Play client
- `VITA` — PlayStation Vita

**Our Android app should identify as "Phone" (0x01)** — this is the most natural fit and was used by the official Xperia Remote Play APK. "PC" would also work (that's what VAIO uses).

---

## FUN_00012c8c — Session Creation

### Address: 0x00012c8c
### Parameters
```c
int FUN_00012c8c(
    int param_1,      // Server context (contains session slots, registered devices)
    int param_2,      // Parsed request data (PSPID at +0x0, other headers)
    undefined4 param_3, // Socket
    undefined8 param_4, // Device key/ID for validation
    undefined1 param_5, // Auth/compression flag
    undefined4 param_6  // MAC address?
)
```

### Logic Flow

1. **Call FUN_0000d078(param_1)** — Initialize/check session manager

2. **Find a free session slot:**
   ```c
   // param_1 + 0x28 = max session count
   // param_1 + 0x54 = pointer to session slot array
   // Each slot is 0x1F0 (496) bytes
   // FUN_0001a6f8 reads slot status into local_70
   // Loop until local_70[0] == 0 (free slot found)
   ```
   - Session slots are **0x1F0 bytes** each (496 bytes)
   - If no free slot → error `-0x7ffd67c0` (all slots busy)

3. **Generate a unique session ID:**
   ```c
   // FUN_0002140c(auStack_68) — generates random/unique 8-byte session ID
   // FUN_0000d138(param_1, auStack_68) — checks if ID already in use
   // Loops until unique ID found (returns -0x7ffd67fd when unique)
   ```

4. **Rate limiting check (for SIGNIN-ID / internet mode):**
   ```c
   if (param_2 + 0x11f == '\0') {  // No SIGNIN-ID (local network mode)
       // Check rate limiting:
       // param_1 + 0x78 = server start timestamp
       // param_1 + 0x80 = array of 32 recent connection device IDs
       // param_1 + 0x100 = array of 32 recent connection timestamps
       // If same device connected less than 60 seconds ago (0x3c) → reject
       // Error: -0x7ffd67bf
   }
   ```
   - **60-second cooldown** between reconnections from same device (LAN mode only)
   - Internet mode (with SIGNIN-ID) skips this check

5. **Create the actual session:**
   ```c
   FUN_0001adbc(
       slot_ptr,      // iVar5 * 0x1F0 + *(param_1 + 0x54) = session slot address
       param_2,       // Request data (PSPID, headers, etc.)
       auStack_68,    // Generated session ID (8 bytes)
       param_3,       // Socket
       param_4,       // Device key
       *(param_1 + 0x1c), // Server config/key material?
       param_5,       // Auth flag
       param_6        // MAC?
   )
   ```
   **FUN_0001adbc is the actual session initializer** — it likely generates the nonce, prepares encryption keys, and sends the 200 OK response.

6. **Increment session count:** `param_1 + 8 += 1`

### Key Data Structures

**Server Context (param_1):**
| Offset | Size | Purpose |
|--------|------|---------|
| +0x08 | 4 | Active session count |
| +0x1C | 4 | Server key material / config |
| +0x28 | 4 | Max sessions allowed |
| +0x54 | 4 | Pointer to session slot array |
| +0x78 | 8 | Server start timestamp (microseconds) |
| +0x80 | 128 (32×4) | Recent connection device IDs (for rate limiting) |
| +0x100 | 128 (32×4) | Recent connection timestamps |

**Session Slot:** 0x1F0 (496) bytes each

### Important Constants
- Max 32 recent connections tracked for rate limiting
- 60-second cooldown between same-device reconnections (LAN mode)
- Session slots are 496 bytes

---

## Next Priority: FUN_0001adbc

This is the **session initializer** that:
- Generates the PREMO nonce
- Sets up encryption (AES keys from pkey + skey + nonce)
- Sends the HTTP 200 OK response with all session parameters
- This is the MOST CRITICAL function to reverse-engineer

**Navigate in Ghidra: press G → type `0001adbc`**
