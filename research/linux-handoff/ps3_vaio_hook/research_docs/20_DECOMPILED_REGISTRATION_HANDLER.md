# Ghidra Finding #20: COMPLETE Decompiled Registration Handler

## Source: `UndefinedFunction_00100168` in `sysconf_plugin.elf`
## Status: FULLY DECOMPILED — All crypto logic visible

This function was previously a 5KB gap in the C export (addresses 0x00100168-0x00100E3C).
Ghidra required manual "Create Function" at 0x00100168 to decompile it.

---

## HIGH-LEVEL FLOW

```
1. Init random seed
2. LOOP:
   a. Wait for events (registration AP active, client connection, shutdown)
   b. On shutdown → return
   c. On timeout → error -4
   d. On client connection:
      - Read HTTP request line
      - If NOT "POST /sce/premo/regist HTTP/1.1" → send 403
      - Parse headers, extract Content-Length
      - Read body (up to 1023 bytes)
      - Determine device type from *(param_1 + 0x38) + 0x20
      - Derive AES key + IV based on device type
      - AES-CBC decrypt the body
      - Parse decrypted text: Client-Type, Client-Id, Client-Mac, Client-Nickname
      - Validate Client-Type matches expected (shifted mapping)
      - If all 4 fields valid → generate pkey, build 200 OK response, encrypt, send
      - Otherwise → send 403 with error code
      - Increment failure counter (max 3 attempts)
```

---

## EXACT KEY DERIVATION (from decompiled C)

### Device Type 0 — PSP (iVar5 = 1)

**Key material source:** Last 16 bytes of HTTP body (client-generated, appended unencrypted)
```c
FUN_0012f670(abStack_94, iVar19 + uStack_98 - 0x10, 0x10);  // copy last 16 bytes
```

**Key derivation loop:**
```c
counter = 0;
for (i = 0; i < 16; i++) {
    temp = (key_material[i] ^ DAT_00150ea0[i]) - counter;
    counter++;
    aes_key[i] = temp - 0x25;
}
```
**Formula:** `aes_key[i] = (km[i] ^ PSP_XOR[i]) - i - 0x25`

**IV:** Copy `DAT_00150eb0` (16 bytes), then XOR **first** 8 bytes:
```c
uStack_80 = uStack_80 ^ **(ulonglong **)(param_1 + 0x38);
```

**Encrypted data:** body[0 .. body_size - 17] (everything except last 16 bytes)

---

### Device Type 1 — Phone (iVar5 = 2)

**Key material source:** Last 16 bytes of HTTP body (client-generated, appended unencrypted)
```c
FUN_0012f670(abStack_94, iVar19 + uStack_98 - 0x10, 0x10);
```

**Key derivation loop:**
```c
counter = 0;
for (i = 0; i < 16; i++) {
    temp = key_material[i] - counter;
    counter++;
    aes_key[i] = (temp - 0x28) ^ DAT_00150ec0[i];  // NOTE: subtract THEN XOR
}
```
**Formula:** `aes_key[i] = (km[i] - i - 0x28) ^ PHONE_XOR[i]`

**IV:** Copy `DAT_00150ed0` (16 bytes), then XOR **second** 8 bytes:
```c
uStack_78 = uStack_78 ^ **(ulonglong **)(param_1 + 0x38);
```

**Encrypted data:** body[0 .. body_size - 17] (everything except last 16 bytes)

---

### Device Type 2 — PC/VITA (iVar5 = 3)

**Key material source:** `param_1 + 0x184` (16 bytes stored in context — PIN-derived)
```c
FUN_0012f670(abStack_94, param_1 + 0x184, 0x10);
```

**Key derivation loop:**
```c
counter = 0;
for (i = 0; i < 16; i++) {
    temp = (key_material[i] ^ DAT_00150ee0[i]) - counter;
    counter++;
    aes_key[i] = temp - 0x2B;
}
```
**Formula:** `aes_key[i] = (km[i] ^ PC_XOR[i]) - i - 0x2B`

**IV:** Copy `DAT_00150ef0` (16 bytes), then XOR **first** 8 bytes:
```c
uStack_80 = uStack_80 ^ **(ulonglong **)(param_1 + 0x38);
```

**Encrypted data:** body[0x1E0 .. body_size] (skip first 480 bytes)
**Minimum body size:** 0x200 (512 bytes)

---

## CLIENT-TYPE SHIFT MAPPING (confirmed from code)

```c
if (iVar5 == 1) pcVar23 = "Phone";     // PSP device → expects "Phone"
else if (iVar5 == 2) pcVar23 = "PC";    // Phone device → expects "PC"
else pcVar23 = "VITA";                   // PC device → expects "VITA"
```

| PS3 Menu Selection | Device Type | iVar5 | Expected Client-Type | Encryption Keys |
|-------------------|-------------|-------|---------------------|-----------------|
| PSP | 0x00 | 1 | "Phone" | DAT_00150ea0 / DAT_00150eb0 |
| Mobile Phone | 0x01 | 2 | "PC" | DAT_00150ec0 / DAT_00150ed0 |
| PC | 0x02 | 3 | "VITA" | DAT_00150ee0 / DAT_00150ef0 |

---

## IV CONTEXT VALUE: `**(ulonglong **)(param_1 + 0x38)`

### Call chain (traced through sysconf_plugin.elf.c):

```
FUN_000fe6b8(device_type, reg_params)
  → reg_params[0] = (longlong)(int)(random % 90000000 + 10000000)  // 8-digit PIN
  → reg_params[0x08] = SSID string "PS3REGIST_%04d"
  → reg_params[0x20] = device_type byte

FUN_000fe4cc(reg_params)
  → FUN_00101000(&server_ctx, 0x244d, 0x7d2, 3, &reg_info, reg_params)
    → FUN_0010150c(server_ctx + 0x40, 0x7d2, 3, &reg_info, reg_params)
      → *(context + 0x38) = reg_params    // POINTER to the reg_params structure
```

### Therefore:
```
*(param_1 + 0x38)         = pointer to reg_params structure
**(ulonglong **)(+0x38)   = first 8 bytes of reg_params = PIN as big-endian longlong
```

For PIN 12345678 → `00 00 00 00 00 BC 61 4E`

### STATUS: TESTED AND FAILED

The PIN-as-big-endian-int was tried in `ps3_register.py` (attempt "PIN as big-endian int")
and `ps3_register_single.py` (attempts 3 and 6). All returned error 80029820.

### Why it might still be correct but the tests fail:
1. **Static keys might be wrong** — extracted from Ghidra, never verified against binary bytes
2. **AES implementation difference** — PS3 might use a non-standard CBC mode
3. **The Ghidra decompiler misinterpreted the `undefined8 param_2`** in `FUN_0010150c`, causing parameter shift
4. **Firmware version mismatch** — keys extracted from 4.90, HEN PS3 runs 4.92
5. **Test environment issue** — WiFi AP timing, connection state, etc.

---

## HTTP REQUEST PARSING DETAILS

1. First line must match `"POST /sce/premo/regist HTTP/1.1\n"` (FUN_0012f3d0 = strncmp)
2. Headers parsed line by line until empty line
3. `Content-Length` header extracted → `uStack_98`
4. Body read in chunks from socket (up to Content-Length bytes)
5. Body max = 1023 bytes (`uStack_98 - 1 < 0x3ff`)

If first line doesn't match → HTTP 403 with PREMO-Version header + Reason code.

---

## HTTP 200 OK RESPONSE (exact format from code)

```
HTTP/1.1 200 OK\r\n
Connection: close\r\n
Content-Length: <padded_body_length>\r\n\r
\n
<AES-CBC encrypted body>
```

### Response body (plaintext before encryption):
```
AP-Ssid: <hex-encoded WiFi AP SSID>\r\n
AP-Key: <hex-encoded WiFi AP WPA key>\r\n
AP-Name: PLAYSTATION(R)3\r\n
PSN-LoginId: <PSN email, if signed in>\r\n
PS3-Mac: <hex-encoded PS3 MAC, 12 chars>\r\n
PS3-Nickname: <PS3 system name>\r\n
PREMO-KeyType: 0\r\n
PREMO-Key: <hex-encoded 16-byte pkey, 32 chars>\r\n
```

- Body padded to 16-byte boundary (zero-padded)
- Encrypted with SAME key and IV used for request decryption
- Content-Length = padded size (formatted as `%10d`)
- `AP-Ssid` and `AP-Key` only included if `FUN_000fed14` succeeds (WiFi AP available)
- `PSN-LoginId` only if PS3 has a signed-in PSN account

### PKEY Generation:
```c
for (i = 0; i < 16; i++) {
    pkey[i] = FUN_0012b8f8();  // random byte
}
```

The pkey is hex-encoded (FUN_0010126c) and sent as PREMO-Key.

---

## HTTP 403 RESPONSE (error format)

```
HTTP/1.1 403 Forbidden\r\n
Connection: close\r\n
Pragma: no-cache\r\n
Content-Length: 0\r\n
PREMO-Version: X.Y\r\n
<error_header>XXXXXXXX\r\n
\r\n
```

Error code is formatted as `%08x` — the hex representation of the internal error.

---

## ERROR CODES

| Internal Value | Hex Code | Meaning |
|---------------|----------|---------|
| -0x7ffd67e0 | 80029820 | Generic parse/decrypt failure (default error) |
| -0x7ffd67fe | 80029802 | Unknown device type |
| -0x7ffd67b1 | 8002984f | Device slot full (triggers special notification) |
| -0x7ffd67df | 80029821 | Socket read error (from FUN_00101668) |

### Failure counter:
- `param_1 + 0x30` counts failed attempts
- After 3 failures (counter > 2): `FUN_000fe458(0x80029820)` — terminates registration
- Counter incremented on EVERY 403 response

---

## DECRYPTED BODY PARSING

After AES-CBC decryption, the plaintext is parsed as colon-separated key-value pairs:

```c
// Parser state machine:
// 1. Scan for ':' delimiter
// 2. Key = text before ':'
// 3. Value = text after ':', trimmed of spaces/tabs
// 4. Line ends at '\r', '\n', or '\0'
// 5. '\r\n' sequences are consumed as line terminators
```

### Field validation:
- **Client-Type**: Must exactly match expected string ("Phone"/"PC"/"VITA")
- **Client-Id**: Hex string, parsed as 8+8 byte halves. If > 16 chars, split at len-16
- **Client-Mac**: Hex string parsed to 6-byte integer, stored big-endian
- **Client-Nickname**: Max 127 chars, null-terminated

All 4 must be present (bitmask `uVar21 == 0x0F`). If Client-Type doesn't match → `goto LAB_00100c10` (immediate error).

---

## WIFI PASSPHRASE STRINGS (from FUN_000ff588)

Found in the registration setup function at line 69459-69462:

```c
// Device type PSP (0) or PC (2):
"xeRFa3VYDHSfNjE_nA{z>pk2xANqicHqQFvij}0WHz[7kzO2Yynp4o4j}U2"

// Device type Phone (1):
"RvnpNXP}xS5qOWfj77guV}0lPAS37hzONG7ZHMNBAwM0mKjt1mkUhHjdbyF"
```

These 60-character strings are the WPA-NONE passphrases for the PS3's ad-hoc registration WiFi AP. They are STATIC across all PS3 consoles.

---

## REGISTRATION STORAGE

After successful registration, `FUN_000fefc4` stores:
- Client-Id (16 bytes) at `DAT_001b6e6c + slot * 0xAC`
- Client-Mac (6 bytes) at `DAT_001b6eec + slot * 0xAC`
- Device type (4 bytes) at `DAT_001b6f04 + slot * 0xAC`
- PKEY (16 bytes) at `DAT_001b6f08 + slot * 0xAC`
- Nickname (0x80 bytes) at `DAT_001b6e6c + slot * 0xAC`

Max 7 device slots (loop 0-6).

Registration state set: `*(*(param_1 + 0x38) + 0x18) = 2`

---

## CRITICAL NEXT STEPS

1. **Verify static keys against binary** — Open sysconf_plugin.elf in hex editor, go to 0x00150ea0-0x00150f00, extract raw bytes
2. **Try PS3MAPI memory dump** — While in registration mode, peek at the actual IV context value
3. **Try xRegistry bypass** — Skip registration entirely by injecting device data via FTP
4. **Verify with VAIO binary** — Analyze Remote Play.exe (not rmp_dll.dll) for the client-side implementation
