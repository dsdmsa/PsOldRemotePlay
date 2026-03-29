# Research Status

**Last Updated:** 2026-03-29

## Executive Summary

**Session protocol:** 100% reverse-engineered and implemented
**Key derivation formulas:** Verified against PPC assembly
**Static crypto keys:** Hex-verified against firmware binary
**Blocker:** 8-byte IV context value in registration (identity unknown)

## What Works

### ✅ Discovery Protocol (Complete)
- UDP broadcast "SRCH" on port 1119
- PS3 responds with "RESP" (156 bytes: MAC + nickname + NPX ID)
- Works on local network, supports both broadcast and direct IP

### ✅ Session Protocol (Complete)
- HTTP GET `/sce/premo/session` with PREMO-* headers
- PS3 returns 200 OK with session ID and nonce
- Session ID used for subsequent video/audio/control requests
- Nonce XOR key varies by platform (PSP, Phone, PC, VITA)

### ✅ Session Crypto (Complete)
- AES key: `pkey XOR skey0` (both 16 bytes)
- AES IV: `nonce XOR skey2` (both 16 bytes)
- Encryption mode: AES-128-CBC
- All 6 static keys extracted from firmware, verified

### ✅ Video Stream (Partial)
- HTTP GET `/sce/premo/session/video` returns encrypted stream
- 32-byte header per packet: timestamp, sequence, packet type
- Payload: AES-128-CBC encrypted video frames
- Implementation: Reads and decrypts packets (no video decode yet)

### ✅ Static Crypto Keys (Verified)
```
Session Keys (same for all PS3 consoles):
  skey0: D1 B2 12 EB 73 86 6C 7B 12 A7 5E 0C 04 C6 B8 91
  skey1: 1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F
  skey2: 65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59

Platform-specific nonce XOR keys:
  PSP:   65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59
  Phone: 37 63 E5 4D 12 F9 7B 73 62 3A D3 0D 10 C9 91 7E
  PC:    33 72 84 4C A6 6F 2E 2B 20 C7 90 60 33 E8 29 C6
  VITA:  AF 74 B5 4F 38 F8 AF C8 75 77 B2 D5 47 76 3B FD
```

## What Doesn't Work (Yet)

### ⚠️ Device Registration (Blocked)

**Endpoint:** POST `/sce/premo/regist`
**WiFi Mode:** PS3 creates temporary ad-hoc AP (PS3REGIST_XXXX)
**WiFi Password:** Known static string per device type

**Status:** Can establish HTTP connection and send encrypted request body, but:
- All registration attempts fail with error code `80029820` (generic parse/decrypt failure)
- Key derivation formulas are VERIFIED as correct
- Static XOR keys are VERIFIED as correct
- **Unknown:** 8-byte IV context value (traced to `**(param_1 + 0x38)`)

**Latest Finding:** Analysis of VAIO Windows client reveals it uses `cfContext` parameter derived from hardware machine ID (via `SuGetMachineIDEx`), not the PIN. The PS3 likely expects similar hardware-derived context, not PIN-based.

## Key Derivation Formulas (VERIFIED)

### PSP Type (Device type 0)
```
aes_key[i] = (km[i] ^ DAT_00150ea0[i]) - i - 0x25
iv[0:8] XOR = **(param_1 + 0x38)
```
**PPC assembly verified at 0x1004F4**

### Phone Type (Device type 1)
```
aes_key[i] = (km[i] - i - 0x28) ^ DAT_00150ec0[i]
iv[8:16] XOR = **(param_1 + 0x38)
```
**PPC assembly verified at 0x100570**

### PC Type (Device type 2)
```
aes_key[i] = (km[i] ^ DAT_00150ee0[i]) - i - 0x2B
iv[0:8] XOR = **(param_1 + 0x38)
```
**PPC assembly verified at 0x1005D8**

## Testing Approaches Tried (All Failed)

22 different IV context encodings tested against a real PS3:
1. Zeros
2. PIN as 8-byte big-endian longlong
3. PIN as 8-byte little-endian longlong
4. PIN as 4-byte BE int, zero-padded left
5. PIN as 4-byte BE int, zero-padded right
6. PIN as 4-byte LE int, zero-padded
7. PIN as ASCII bytes
8. PIN halves swapped (e.g., 12345678 → 56781234)
9. First 4 digits of PIN as int
10. Last 4 digits of PIN as int
11. PS3 Ethernet MAC + padding
12. PS3 WiFi MAC (Ethernet+1) + padding
13. PS3 MAC first 6 bytes, last 2 padded
14. sockaddr_in structure (AF_INET, port 9293, IP 192.168.1.1)
15. PS3 PSID (first 8 bytes)
16. PS3 PSID (last 8 bytes)
17. Client MAC address + padding
18. Client-Type shifted mapping test (Phone as PSP)
19. Double-XOR pattern test
20. Completely unencrypted body
21-22. Two different PS3s tested (stock and HEN)

## Firmware Analysis

### Reverse Engineered Files
- **premo_plugin.elf** — Session protocol (fully decompiled, verified)
- **sysconf_plugin.elf** — Registration handler (fully decompiled, verified)
- **premo_game_plugin.elf** — In-game streaming variant
- **libsysutil_remoteplay.elf** — System utilities

### Ghidra Findings (21+ Documents)
1. Complete session protocol summary
2. Nonce formula corrections
3. Registration handler function analysis
4. PPC assembly verification
5. Runtime memory analysis via PS3MAPI
6. Key derivation verification
7. PS4 comparison (PS4 registration is solved)
8. VAIO Windows client analysis

## New Hypothesis: Hardware Device ID

### Evidence
1. **VAIO Analysis** — Windows client uses `SuGetMachineIDEx()` (hardware identifier), not PIN
2. **PS4 Comparison** — PS4 uses PIN-based derivation (simpler formula), PS3 may differ
3. **Offset 0x38** — Parameter structure at registration function entry, likely device-specific
4. **Static keys** — If PIN were correct, would have worked across different PIN values (tested multiple)

### Testing Required
1. Determine what `cfContext` in VAIO maps to on PS3
2. Check if PS3 expects device MAC address, OpenPSID, or other identifier
3. Test hardware-derived context (not PIN-based)

## Alternative Path Forward: xRegistry Bypass

Instead of solving registration, bypass it entirely:
1. FTP download `/dev_flash2/etc/xRegistry.sys` from PS3
2. Parse and inject device entry in `/setting/premo/psp01/`
3. Add fake device registration (MAC, ID, nickname, self-chosen pkey)
4. Upload modified xRegistry.sys back
5. Reboot PS3
6. Use self-chosen pkey for session authentication

**Status:** xRegistry.sys parser created (`tools/xregistry_tool.py`)
**Advantage:** Proves entire rest of protocol works without solving IV mystery

## Verification Status

| Item | Source | Verification Method | Status |
|------|--------|-------------------|--------|
| skey0/1/2 | Firmware binary | Hex-matched 48 bytes | ✅ Verified |
| Nonce XOR keys | Firmware binary | Hex-matched 64 bytes | ✅ Verified |
| Key formula PSP | PPC assembly 0x1004F4 | Runtime PE section match | ✅ Verified |
| Key formula Phone | PPC assembly 0x100570 | Runtime PE section match | ✅ Verified |
| Key formula PC | PPC assembly 0x1005D8 | Runtime PE section match | ✅ Verified |
| IV base values | Firmware binary | Hex-matched 48 bytes | ✅ Verified |
| IV double-deref | PPC assembly 0x100620 | Static/runtime code match | ✅ Verified |
| PIN as BE int | Test script | Real PS3 (2 different) | ❌ Failed |
| Registration endpoint | Protocol test | HTTP POST works | ✅ Verified |
| AES-CBC mode | Code analysis | Matches OpenSSL pattern | ✅ Verified |

## Contributing

Want to help solve this? See [CONTRIBUTING.md](CONTRIBUTING.md) for research tasks.

## Related Resources

- **Open-RP:** https://github.com/gbraad/open-rp (PSP implementation, no registration)
- **Chiaki-NG:** https://github.com/streetpea/chiaki-ng (PS4/PS5, registration is solved)
- **PS4 Remote Play:** https://github.com/kingcreek/ps4-remote-play (Python client, good reference)
- **PlayStation Dev Wiki:** https://www.psdevwiki.com/ (PS3 and PS4 protocol documentation)

## Decoded Error Code

`80029820` = Generic parse or decrypt failure (internal error code: `-0x7ffd67e0`)

This is the PS3's catch-all error for registration body decryption failure. It's returned when:
- AES decryption produces invalid plaintext
- Parsed fields don't match expected format
- Client-Type doesn't match expected value
- Any validation fails
