# Ghidra Finding: FUN_00100168 — THE COMPLETE REGISTRATION PROTOCOL

## Address: 0x00100168 (in sysconf_plugin.elf)
## Type: CRITICAL — The complete device registration/pairing handler

---

## REGISTRATION PROTOCOL — FULLY REVERSE ENGINEERED

### How Registration Works (step by step):

1. PS3 creates a temporary WiFi AP
2. Client connects to that AP
3. Client sends `POST /sce/premo/regist HTTP/1.1` with `Content-Length` header
4. Then sends the request body (encrypted payload containing device info)
5. PS3 decrypts the payload, extracts: Client-Type, Client-Id, Client-Mac, Client-Nickname
6. PS3 generates a random 16-byte pkey
7. PS3 responds with: AP-Ssid, AP-Key, PSN-LoginId, PS3-Mac, PS3-Nickname, PREMO-Key (the pkey)
8. Response body is AES encrypted before sending

---

## REQUEST FORMAT

```
POST /sce/premo/regist HTTP/1.1\n
Content-Length: <body_size>\n
\n
<encrypted_body>
```

### The encrypted body contains (after decryption):
```
Client-Type: <Phone|PC|VITA>
Client-Id: <hex device ID, 16 or 32 hex chars>
Client-Mac: <hex MAC address>
Client-Nickname: <device name, max 127 chars>
```

### Encryption of the request body

The body is AES encrypted. The PS3 decrypts it using keys derived per device type:

**Device Type 0 (cVar12 == '\0') → PSP:**
- Body is the last 16 bytes of the payload
- XOR key from `DAT_00150ea0` (16 bytes)
- Formula: `key[i] = (body[i] XOR static_key[i]) - i - 0x25`
- IV from `DAT_00150eb0` (16 bytes)
- Final IV: `IV ^= *(param_1 + 0x38)` (first 8 bytes XOR'd with some context value)

**Device Type 1 (cVar12 == '\x01') → Phone:**
- XOR key from `DAT_00150ec0` (16 bytes)
- Formula: `key[i] = (body[i] - i) - 0x28 XOR static_key[i]`
- IV from `DAT_00150ed0` (16 bytes)
- Final IV second 8 bytes: `IV_high ^= *(param_1 + 0x38)`

**Device Type 2 (cVar12 == '\x02') → PC/VITA:**
- Body starts at offset 0x1E0 (480 bytes prefix, then encrypted data)
- Also uses `param_1 + 0x184` (16 bytes — likely the PIN-derived key!)
- XOR key from `DAT_00150ee0` (16 bytes)
- Formula: `key[i] = (body[i] XOR static_key[i]) - i - 0x2B`
- IV from `DAT_00150ef0` (16 bytes)
- Final IV: `IV ^= *(param_1 + 0x38)`

**Then decryption:** `FUN_000fe800(output, input, length, &IV, key)` — AES-CBC decrypt

---

## RESPONSE FORMAT

The PS3 builds the 200 OK response:

```
HTTP/1.1 200 OK\r\n
Connection: close\r\n
Content-Length: <encrypted_body_length>\r\n\r
\n
<encrypted_response_body>
```

### The response body (before encryption) contains:
```
AP-Ssid: <WiFi AP SSID>\r\n
AP-Key: <WiFi AP WPA passphrase>\r\n
AP-Name: PLAYSTATION(R)3\r\n
PSN-LoginId: <PSN email if available>\r\n
PS3-Mac: <PS3 MAC address as hex string>\r\n
PS3-Nickname: <PS3 name>\r\n
PREMO-KeyType: 0\r\n
PREMO-Key: <THE PKEY — 16 bytes as hex string>\r\n
```

**The pkey is generated right before the response:**
```c
// Generate 16 random bytes as the pkey
for (i = 0; i < 16; i++) {
    auStack_1f8[i] = FUN_0012b8f8();  // random byte generator
}
```

### Response encryption:
- Body is padded to 16-byte alignment
- Encrypted with `FUN_000fe82c(output, input, length, &IV, key)` — AES-CBC encrypt
- Uses the SAME key/IV derived during request decryption

---

## CRITICAL: Static Keys for Registration Encryption

These are DIFFERENT from the session nonce keys! They're in `sysconf_plugin.elf`:

| Address | Platform | Purpose |
|---------|----------|---------|
| `DAT_00150ea0` | PSP (type 0) | Registration XOR key |
| `DAT_00150eb0` | PSP (type 0) | Registration IV base |
| `DAT_00150ec0` | Phone (type 1) | Registration XOR key |
| `DAT_00150ed0` | Phone (type 1) | Registration IV base |
| `DAT_00150ee0` | PC/VITA (type 2) | Registration XOR key |
| `DAT_00150ef0` | PC/VITA (type 2) | Registration IV base |

**ACTION: Extract these 6 × 16-byte keys from Ghidra!**

---

## Client-Id Parsing

The Client-Id can be:
- 16 hex chars or less → parsed as single 16-byte value
- More than 16 hex chars → split: last 16 chars parsed separately, prefix parsed separately
- Both halves stored in `auStack_130` (8 bytes) and `auStack_128` (8 bytes)

---

## Validation

All 4 fields must be present (checked via bitmask `uVar21 == 0x0F`):
- bit 0 (0x01): Client-Type validated
- bit 1 (0x02): Client-Id parsed
- bit 2 (0x04): Client-Mac parsed
- bit 3 (0x08): Client-Nickname parsed

If any missing → 403 Forbidden

After validation, calls `FUN_000ff318(client_id, device_type)` to check/store the registration.

---

## Device Type Determination

The device type comes from `*(param_1 + 0x38) + 0x20`:
- `0x00` = PSP
- `0x01` = Phone
- `0x02` = PC/VITA

This is set BEFORE the registration handler runs — likely from the PS3's "Register Device" menu selection (user chooses "PSP", "Phone", or "PC").

---

## PIN Code / Key at param_1 + 0x184

For device type 2 (PC/VITA), the code reads 16 bytes from `param_1 + 0x184`:
```c
FUN_0012f670(abStack_94, param_1 + 0x184, 0x10);
```
This is likely the **8-digit PIN displayed on the PS3 screen**, expanded/hashed to 16 bytes. The PIN is used as part of the key derivation for encrypting/decrypting the registration payload.

For device types 0 and 1 (PSP/Phone), the key is derived from the LAST 16 bytes of the payload itself — suggesting the PSP/Phone sends its own key material.

---

## Error Handling

- Max 3 registration attempts per session (`param_1 + 0x30` counter, checked against 2)
- Error `0x80029820` → too many failed attempts
- Error `0x8002984f` → specific registration failure
- Error `0x7ffd67e0` → payload parsing error
- Error `0x7ffd67fe` → unknown device type
- Error `0x7ffd67b1` → triggers special error notification

---

## WHAT THIS MEANS FOR OUR ANDROID APP

The registration protocol is:
1. Connect to PS3's temporary WiFi AP
2. Send `POST /sce/premo/regist HTTP/1.1`
3. Body must be AES-encrypted with platform-specific keys
4. Body contains: our device type ("Phone"), our device ID, our MAC, our nickname
5. PS3 decrypts, validates, generates random pkey
6. PS3 responds with encrypted body containing the pkey + PS3 info
7. We decrypt the response, extract the pkey
8. Store pkey locally — registration complete forever

**We need the 6 static keys from `DAT_00150ea0` through `DAT_00150ef0` to implement this.**

## UPDATE 2026-03-28

**This function has been fully decompiled.** See [20_DECOMPILED_REGISTRATION_HANDLER.md](./20_DECOMPILED_REGISTRATION_HANDLER.md) for the complete C code analysis with exact key derivation loops, IV XOR logic, HTTP parsing, response format, and error handling.

Key findings from the decompiled code:
- Key derivation formulas are SIMPLE (not complex like session nonce) and match our test scripts
- IV context value = PIN as big-endian longlong (confirmed via call chain)
- But all tests still fail — suspect wrong static keys or PPC64 parameter shift issue
- WiFi AP uses static 60-char passphrases (not PIN-derived as previously assumed)
- Response includes AP-Ssid, AP-Key, AP-Name, PSN-LoginId, PS3-Mac, PS3-Nickname, PREMO-KeyType, PREMO-Key

## Next Steps

1. **HEX VERIFY the static keys** — Open sysconf_plugin.elf binary at addresses 0x00150ea0-0x00150f00, extract raw bytes, compare with PremoConstants.kt
2. **PS3MAPI memory dump** — Peek at actual context value during registration
3. **xRegistry bypass** — Inject registration data via FTP to skip crypto entirely
4. **Analyze VAIO Remote Play.exe** — Client-side registration in x86 (much easier than PPC64)
