# 20: Runtime Memory Analysis — sysconf_plugin Key Derivation (2026-03-28)

## Method

Used webMAN MOD PS3MAPI HTTP interface (`/getmem.ps3mapi`) to read VSH process memory
on HEN PS3 at 192.168.1.80 (FW 4.92 CEX, webMAN 1.47.47g).

1. Found the known Phone XOR key (`F116F0DA...`) at runtime address **0x01CB0EC0**
2. Calculated sysconf_plugin runtime base: `0x01CB0EC0 - 0x150EC0 = 0x01B60000`
3. Verified by reading registration handler entry at **0x01C60168** — bytes match ELF exactly
4. Dumped full handler (3284 bytes) and compared with static ELF

## Key Finding: Runtime Code == Static ELF

The registration handler code at runtime is **identical** to the decrypted sysconf_plugin.elf,
except for standard relocation fixups (`lis` instructions patched from ELF VA to runtime VA).
The actual algorithm is NOT modified at runtime.

## Critical Discovery: THREE Key Derivation Formulas

The branch at VA 0x100478 checks `r25` (platform type):

```
0x100478: cmpwi cr7, r25, 1     ; type 1?
0x10047C: bne   0x100538         ; no → check type 2
0x100480: b     0x1004C8         ; yes → PSP path (Loop 0)
...
0x100538: cmpwi cr7, r25, 2     ; type 2?
0x10053C: ...
0x100540: bne   0x1004A8         ; no → PC path (Loop 2)
          ; type 2 → Phone path (Loop 1)
```

### Loop 0 — PSP (type 1)

**Formula:** `result[i] = (material[i] XOR key[i]) - counter - 0x25`
- XOR comes FIRST, then subtract
- Constant: **0x25** (37)
- XOR key at 0x150EA0: `FD C3 F6 A6 4D 2A BA 7A 38 92 6C BC 34 31 E1 0E`
- IV base at 0x150EB0: `3A 9D F3 3B 99 CF 9C 0D BF 58 81 12 6C 18 32 64`

PPC assembly:
```
0x1004F4: lbz   r9, 0(r10)       ; r9 = material[i]
0x1004F8: lbz   r0, 0(r11)       ; r0 = key[i]
0x1004FC: xor   r9, r9, r0       ; r9 = material XOR key
0x100500: subf  r9, r7, r9       ; r9 = r9 - counter
0x100504: addi  r7, r7, 1        ; counter++
0x100508: addi  r9, r9, -37      ; r9 -= 0x25
0x10050C: stb   r9, 0(r10)       ; result[i] = r9
```

### Loop 1 — Phone (type 2)

**Formula:** `result[i] = (material[i] - counter - 0x28) XOR key[i]`
- Subtract FIRST, then XOR
- Constant: **0x28** (40)
- XOR key at 0x150EC0: `F1 16 F0 DA 44 2C 06 C2 45 B1 5E 48 F9 04 E3 E6`
- IV base at 0x150ED0: `29 0D E9 07 E2 3B E2 FC 34 08 CA 4B DE E4 AF 3A`

PPC assembly:
```
0x100570: lbz   r9, 0(r10)       ; r9 = material[i]
0x100574: lbz   r0, 0(r11)       ; r0 = key[i]
0x100578: subf  r9, r7, r9       ; r9 = material - counter
0x10057C: addi  r7, r7, 1        ; counter++
0x100580: addi  r9, r9, -40      ; r9 -= 0x28
0x100584: xor   r9, r9, r0       ; r9 = r9 XOR key
0x100588: stb   r9, 0(r10)       ; result[i] = r9
```

### Loop 2 — PC (type 3)

**Formula:** `result[i] = (material[i] XOR key[i]) - counter - 0x2B`
- XOR comes FIRST, then subtract (same order as PSP)
- Constant: **0x2B** (43)
- XOR key at 0x150EE0: `EC 6D 70 6B 1E 0A 9A 75 8C DA 78 27 51 A3 C3 7B`
- IV base at 0x150EF0: `5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04`

PPC assembly:
```
0x1005D8: lbz   r9, 0(r10)       ; r9 = material[i]
0x1005DC: lbz   r0, 0(r11)       ; r0 = key[i]
0x1005E0: xor   r9, r9, r0       ; r9 = material XOR key
0x1005E4: subf  r9, r6, r9       ; r9 = r9 - counter
0x1005E8: addi  r6, r6, 1        ; counter++
0x1005EC: addi  r9, r9, -43      ; r9 -= 0x2B
0x1005F0: stb   r9, 0(r10)       ; result[i] = r9
```

## Post-Key-Derivation: IV XOR

After each key derivation loop, the code:
1. Copies the IV base (16 bytes) to stack at offset 0x230
2. Loads a value from a structure pointed to by r22 (offset 0x38, dereferenced)
3. XORs the first 8 bytes of the IV (at stack offset 0x230 or 0x238) with this loaded value

This means the **IV is NOT used directly** — it's XOR'd with some runtime value.
The runtime value comes from `*(uint64_t*)(*((uint32_t*)(r22 + 0x38)))`.
This r22 pointer is likely a connection/session context structure.

## Complete Static Key Table (VA 0x150EA0-0x150EFF)

| Offset | Platform | Purpose | Bytes |
|--------|----------|---------|-------|
| 0x150EA0 | PSP | XOR key | FD C3 F6 A6 4D 2A BA 7A 38 92 6C BC 34 31 E1 0E |
| 0x150EB0 | PSP | IV base | 3A 9D F3 3B 99 CF 9C 0D BF 58 81 12 6C 18 32 64 |
| 0x150EC0 | Phone | XOR key | F1 16 F0 DA 44 2C 06 C2 45 B1 5E 48 F9 04 E3 E6 |
| 0x150ED0 | Phone | IV base | 29 0D E9 07 E2 3B E2 FC 34 08 CA 4B DE E4 AF 3A |
| 0x150EE0 | PC | XOR key | EC 6D 70 6B 1E 0A 9A 75 8C DA 78 27 51 A3 C3 7B |
| 0x150EF0 | PC | IV base | 5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04 |

## Remaining Unknown: IV XOR Value

The IV base is XOR'd with an 8-byte runtime value before being used as the AES IV.
This value comes from a connection context structure and might be:
- The client's MAC address (padded)
- The PIN (converted to bytes)
- Part of the sockaddr structure
- The PS3's device ID
- Something derived from the WiFi handshake
