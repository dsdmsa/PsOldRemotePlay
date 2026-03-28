# Ghidra Finding: Registration Encryption Keys (from sysconf_plugin.elf)

## ALL 6 REGISTRATION AES KEYS — EXTRACTED

### PSP XOR Key (DAT_00150ea0) — Device type 0
```
FD C3 F6 A6 4D 2A BA 7A 38 92 6C BC 34 31 E1 0E
```
Derivation: `key[i] = (body[i] XOR static[i]) - i - 0x25`

### PSP IV Base (DAT_00150eb0) — Device type 0
```
3A 9D F3 3B 99 CF 9C 0D BF 58 81 12 6C 18 32 64
```
Final IV: first 8 bytes XOR'd with `*(param_1 + 0x38)` context value

### Phone XOR Key (DAT_00150ec0) — Device type 1
```
F1 16 F0 DA 44 2C 06 C2 45 B1 5E 48 F9 04 E3 E6
```
Derivation: `key[i] = (body[i] - i - 0x28) XOR static[i]` (NOTE: different order than PSP!)

### Phone IV Base (DAT_00150ed0) — Device type 1
```
29 0D E9 07 E2 3B E2 FC 34 08 CA 4B DE E4 AF 3A
```
Final IV: second 8 bytes (uStack_78) XOR'd with `*(param_1 + 0x38)` context value

### PC/VITA XOR Key (DAT_00150ee0) — Device type 2
```
EC 6D 70 6B 1E 0A 9A 75 8C DA 78 27 51 A3 C3 7B
```
Derivation: `key[i] = (body[i] XOR static[i]) - i - 0x2B`

### PC/VITA IV Base (DAT_00150ef0) — Device type 2
```
5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04
```
Final IV: first 8 bytes XOR'd with `*(param_1 + 0x38)` context value

---

## Key Derivation Algorithms Per Device Type

### Type 0 — PSP:
```
key_source = last 16 bytes of payload body
for i in 0..15:
    aes_key[i] = (key_source[i] XOR 0xFDC3F6A64D2ABA7A38926CBC3431E10E[i]) - i - 0x25
iv = 3A9DF33B99CF9C0DBF5881126C183264
iv[0..7] ^= context_value  // *(param_1 + 0x38) first 8 bytes
AES-CBC-decrypt(body[0..len-16], key=aes_key, iv=iv)
```

### Type 1 — Phone:
```
key_source = last 16 bytes of payload body
for i in 0..15:
    aes_key[i] = (key_source[i] - i - 0x28) XOR 0xF116F0DA442C06C245B15E48F904E3E6[i]
iv = 290DE907E23BE2FC3408CA4BDEE4AF3A
iv[8..15] ^= context_value  // *(param_1 + 0x38) — NOTE: second 8 bytes!
AES-CBC-decrypt(body[0..len-16], key=aes_key, iv=iv)
```

### Type 2 — PC/VITA:
```
key_source = param_1 + 0x184  // 16 bytes — likely PIN-derived!
body_offset = 0x1E0  // first 480 bytes are prefix, encrypted data starts after
for i in 0..15:
    aes_key[i] = (key_source[i] XOR 0xEC6D706B1E0A9A758CDA782751A3C37B[i]) - i - 0x2B
iv = 5B6440C52E74C04648729C5490C7904
iv[0..7] ^= context_value
AES-CBC-decrypt(body[0x1E0..], key=aes_key, iv=iv)
```

---

## Important Differences Between Device Types

| Aspect | PSP (0) | Phone (1) | PC/VITA (2) |
|--------|---------|-----------|-------------|
| Key source | Last 16 bytes of body | Last 16 bytes of body | `param_1 + 0x184` (PIN?) |
| XOR order | `(body XOR key) - i - offset` | `(body - i - offset) XOR key` | `(body XOR key) - i - offset` |
| Magic offset | `0x25` | `0x28` | `0x2B` |
| IV XOR target | First 8 bytes | **Second 8 bytes** | First 8 bytes |
| Body offset | 0 | 0 | 0x1E0 (480 bytes prefix) |
| Min body size | 16 bytes | 16 bytes | 512 (0x200) bytes |

---

## For Our Android App (Phone type)

Our app registers as "Phone" (type 1). The registration encryption:

```kotlin
// Key derivation for Phone registration
val phoneXorKey = byteArrayOf(
    0xF1.toByte(), 0x16, 0xF0.toByte(), 0xDA.toByte(),
    0x44, 0x2C, 0x06, 0xC2.toByte(),
    0x45, 0xB1.toByte(), 0x5E, 0x48,
    0xF9.toByte(), 0x04, 0xE3.toByte(), 0xE6.toByte()
)
val phoneIvBase = byteArrayOf(
    0x29, 0x0D, 0xE9.toByte(), 0x07,
    0xE2.toByte(), 0x3B, 0xE2.toByte(), 0xFC.toByte(),
    0x34, 0x08, 0xCA.toByte(), 0x4B,
    0xDE.toByte(), 0xE4.toByte(), 0xAF.toByte(), 0x3A
)

// Encrypt registration body:
// 1. Build plaintext: "Client-Type: Phone\r\nClient-Id: ...\r\nClient-Mac: ...\r\nClient-Nickname: ...\r\n"
// 2. Generate 16 random bytes as key material, append to body
// 3. Derive AES key: aes_key[i] = (random[i] - i - 0x28) XOR phoneXorKey[i]
// 4. Set IV = phoneIvBase, IV[8..15] ^= context_value
// 5. AES-CBC-encrypt(plaintext, key=aes_key, iv=iv)
// 6. POST the encrypted body + the 16 key material bytes
```

## Context Value (`*(param_1 + 0x38)`)

This is an 8-byte value from the PS3's internal context. It likely comes from the registration session setup (possibly derived from the 8-digit PIN displayed on screen, or from the WiFi AP parameters). **This needs further investigation** — it's the last unknown piece.
