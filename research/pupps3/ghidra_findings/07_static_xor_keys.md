# Ghidra Finding: Static XOR Keys for Nonce Derivation

## ALL 4 PLATFORM-SPECIFIC NONCE XOR KEYS — EXTRACTED

### PSP Key (DAT_0002d558) — Platform type 0
```
65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59
```
**NOTE: This is IDENTICAL to skey2 from Open-RP!** Confirmed the protocol is consistent.
Derivation: `nonce[i] = source_key[i] XOR psp_xor_key[i]`

### Phone Key (DAT_0002d578) — Platform type 1
```
37 63 E5 4D 12 F9 7B 73 62 3A D3 0D 10 C9 91 7E
```
Derivation: `nonce[i] = (i + source_key[i] + 0x29) XOR phone_xor_key[i]`

### PC Key (DAT_0002d598) — Platform type 2
```
33 72 84 4C A6 6F 2E 2B 20 C7 90 60 33 E8 29 C6
```
Derivation: `nonce[i] = (i + source_key[i] + 0x33) XOR pc_xor_key[i]`

### VITA Key (DAT_0002d5b8) — Platform type 3
```
AF 74 B5 4F 38 F8 AF C8 75 77 B2 D5 47 76 3B FD
```
Derivation: `nonce[i] = (i + source_key[i] + 0x3D) XOR vita_xor_key[i]`

---

## Also Noted Nearby: Additional Key Material

### DAT_0002d568 (16 bytes) — referenced by FUN_0001a2cc
```
DE 61 A1 C9 BA E1 AC 56 4B 05 61 91 81 2D 67 25
```
Purpose: Unknown — possibly used in auth token validation or stream encryption setup.

### DAT_0002d588 (16 bytes) — referenced by FUN_0001a2cc
```
A0 02 09 84 6C 75 60 0D 57 C2 6B 00 4A 28 59 F1
```
Purpose: Unknown — possibly another key derivation constant.

### DAT_0002d5a8 (16 bytes) — referenced by FUN_0001a2cc
```
08 11 0C F9 0A C8 A9 09 3B 46 CF BF 5A EA C7 55
```
Purpose: Unknown — possibly related to stream decryption.

### DAT_0002d5c8 (start visible) — referenced by FUN_0000e7f4, FUN_00013564
```
1F D5 B9 FA ...
```
**NOTE: `1F D5 B9 FA` matches the start of skey1 from Open-RP!**
Full skey1: `1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F`

---

## Summary: Key Material Map

| Address | Size | Identity | Used By |
|---------|------|----------|---------|
| `0x0002d558` | 16 | **skey2 / PSP XOR key** | Nonce derivation (PSP) |
| `0x0002d568` | 16 | Unknown key | FUN_0001a2cc (auth?) |
| `0x0002d578` | 16 | **Phone XOR key** | Nonce derivation (Phone) |
| `0x0002d588` | 16 | Unknown key | FUN_0001a2cc (auth?) |
| `0x0002d598` | 16 | **PC XOR key** | Nonce derivation (PC) |
| `0x0002d5a8` | 16 | Unknown key | FUN_0001a2cc (auth?) |
| `0x0002d5b8` | 16 | **VITA XOR key** | Nonce derivation (VITA) |
| `0x0002d5c8` | 16 | **skey1** (confirmed start) | Stream encryption? |

The keys are arranged in pairs: [nonce_xor_key, unknown_key] × 4 platforms, then skey1.

---

## For Our Android App (Phone platform)

To generate the correct nonce response when connecting as "Phone":
```kotlin
val phoneXorKey = byteArrayOf(
    0x37, 0x63, 0xE5.toByte(), 0x4D, 0x12, 0xF9.toByte(), 0x7B, 0x73,
    0x62, 0x3A, 0xD3.toByte(), 0x0D, 0x10, 0xC9.toByte(), 0x91.toByte(), 0x7E
)

// The PS3 computes the nonce as:
// nonce[i] = (i + source_key[i] + 0x29) XOR phoneXorKey[i]
// where source_key is the pkey from registration

// The CLIENT receives this nonce in the PREMO-Nonce header
// and must use it to derive the AES session keys (as documented in Open-RP)
```
