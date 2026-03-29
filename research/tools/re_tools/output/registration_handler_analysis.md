# Registration Handler IV XOR Analysis
## radare2 disassembly of sysconf_plugin.elf (FUN_00100168)
## Date: 2026-03-28

---

## 1. FUNCTION ENTRY AND r22 ORIGIN

The function at 0x100168 is the registration handler thread (`cellPremoAcceptThread`).

```asm
0x00100168  stdu r1, -0x2b0(r1)     ; allocate 0x2b0 (688) byte stack frame
0x0010016c  mflr r0
0x00100170  std r22, 0x260(r1)      ; save callee-saved r22
0x00100178  mr r0, r3               ; r0 = first argument (context pointer)
...
0x001001b0  mr r22, r0              ; r22 = context pointer (param_1)
```

**r22 = param_1 = the registration context structure** (2152 bytes, allocated by FUN_0010150c).

---

## 2. THE STRUCTURE AT *(r22 + 0x38)

From FUN_0010150c (context setup):
```c
*(context + 0x38) = param_5;   // = reg_params pointer
```

From FUN_000fe6b8 (reg_params initialization):
```c
FUN_0012f638(reg_params, 0, 0x28);              // memset(reg_params, 0, 40)
*(char*)(reg_params + 0x20) = (char)device_type; // 0=PSP, 1=Phone, 2=PC
*reg_params = (longlong)(int)(random % 90000000 + 10000000); // PIN at offset 0
FUN_0012f408(reg_params + 0x08, 0x10, fmt, pin/10000);      // SSID at offset 8
```

### reg_params structure layout:
| Offset | Size | Content |
|--------|------|---------|
| +0x00 | 8 | PIN as big-endian longlong (e.g., `00 00 00 00 00 BC 61 4E` for PIN 12345678) |
| +0x08 | 16 | SSID string: "PS3REGIST_%04d" or "PS3REGPC_%04d" |
| +0x18 | 4 | Registration state (set to 2 on success) |
| +0x20 | 1 | Device type byte (0=PSP, 1=Phone, 2=PC/VITA) |

---

## 3. DEVICE TYPE CHECK (0x100408)

```asm
0x00100408  clrldi r9, r22, 0x20    ; r9 = (uint32_t)param_1
0x0010040c  lwz r9, 0x38(r9)        ; r9 = *(uint32_t*)(param_1 + 0x38) = reg_params ptr
0x00100410  lbz r0, 0x20(r9)        ; r0 = reg_params[0x20] = device_type byte
0x00100414  extsb r0, r0            ; sign-extend byte
0x00100418  cmpwi cr7, r0, 2        ; compare with 2 (PC/VITA)
0x0010041c  beq cr7, 0x100440       ; if type == 2 → PC/VITA path
0x00100420  cmpwi cr7, r0, 0        ; compare with 0 (PSP)
0x00100424  li r25, 1               ; r25 = iVar5 = 1 (for PSP)
0x00100428  beq cr7, 0x100d78       ; if type == 0 → PSP path
0x0010042c  cmpwi cr7, r0, 1        ; compare with 1 (Phone)
0x00100438  bne cr7, 0x100c18       ; if type != 1 → error
```

Device type mapping (r25 = iVar5):
- PSP (type 0) -> r25 = 1
- Phone (type 1) -> r25 = 2 (set elsewhere)
- PC/VITA (type 2) -> r25 = 3

---

## 4. ALL XOR OPERATIONS TRACED

### 4a. PSP KEY DERIVATION XOR (0x1004FC)

**Path:** r25 == 1, entered at 0x1004c8

```asm
; Setup: r8 = DAT_00150ea0 (PSP XOR key table)
0x001004c8  lis r9, 0x15
0x001004d0  addi r9, r9, 0xea0      ; r9 = 0x150ea0
0x001004d4  li r7, 0                ; counter = 0
0x001004d8  mr r8, r9               ; r8 = XOR table pointer
0x001004dc  li r9, 0x10             ; loop 16 times
0x001004e0  mtctr r9

; Loop body:
0x001004e4  clrldi r10, r3, 0x20    ; r10 = key_material pointer
0x001004e8  clrldi r11, r8, 0x20    ; r11 = XOR table pointer
0x001004f4  lbz r9, 0(r10)          ; r9 = key_material[i]
0x001004f8  lbz r0, 0(r11)          ; r0 = DAT_00150ea0[i]
0x001004fc  xor r9, r9, r0          ; r9 = key_material[i] ^ PSP_XOR[i]
0x00100500  subf r9, r7, r9         ; r9 = r9 - counter
0x00100504  addi r7, r7, 1          ; counter++
0x00100508  addi r9, r9, -0x25      ; r9 = r9 - 0x25
0x0010050c  stb r9, 0(r10)          ; result[i] = r9
0x00100510  bdnz 0x1004e4           ; loop
```

**Formula: `key[i] = (km[i] ^ DAT_00150ea0[i]) - i - 0x25`**

Data at 0x150ea0 (verified from binary):
`fd c3 f6 a6 4d 2a ba 7a 38 92 6c bc 34 31 e1 0e`

---

### 4b. PHONE KEY DERIVATION XOR (0x100584)

**Path:** r25 == 2, entered at 0x100544

```asm
; Setup: r8 = DAT_00150ec0 (Phone XOR key table)
0x00100544  lis r9, 0x15
0x0010054c  addi r9, r9, 0xec0      ; r9 = 0x150ec0
0x00100558  li r7, 0                ; counter = 0
0x0010055c  mr r8, r9               ; r8 = XOR table pointer

; Loop body (DIFFERENT ORDER from PSP):
0x00100570  lbz r9, 0(r10)          ; r9 = key_material[i]
0x00100574  lbz r0, 0(r11)          ; r0 = DAT_00150ec0[i]
0x00100578  subf r9, r7, r9         ; r9 = key_material[i] - counter (subtract FIRST)
0x0010057c  addi r7, r7, 1          ; counter++
0x00100580  addi r9, r9, -0x28      ; r9 = r9 - 0x28
0x00100584  xor r9, r9, r0          ; r9 = r9 ^ DAT_00150ec0[i] (XOR LAST)
0x00100588  stb r9, 0(r10)          ; result[i] = r9
```

**Formula: `key[i] = (km[i] - i - 0x28) ^ DAT_00150ec0[i]`**

CRITICAL: Note the operation order differs from PSP:
- PSP: XOR first, then subtract
- Phone: subtract first, then XOR

Data at 0x150ec0:
`f1 16 f0 da 44 2c 06 c2 45 b1 5e 48 f9 04 e3 e6`

---

### 4c. PC/VITA KEY DERIVATION XOR (0x1005E0)

**Path:** r25 == 3 (also default), entered at 0x1004a8 -> 0x1005c8

```asm
; Setup: r8 = DAT_00150ee0 (PC XOR key table), r6 = counter
0x001004a8  lis r9, 0x15
0x001004b0  addi r9, r9, 0xee0      ; r9 = 0x150ee0
0x001004b4  li r6, 0                ; counter = 0
0x001004bc  addi r7, r1, 0x21c      ; r7 = key_material (from stack)
0x001004c0  mr r8, r9               ; r8 = XOR table pointer
; Jump to loop at 0x1005c8

; Loop body:
0x001005c8  clrldi r10, r7, 0x20    ; r10 = key_material pointer
0x001005cc  clrldi r11, r8, 0x20    ; r11 = XOR table pointer
0x001005d8  lbz r9, 0(r10)          ; r9 = key_material[i]
0x001005dc  lbz r0, 0(r11)          ; r0 = DAT_00150ee0[i]
0x001005e0  xor r9, r9, r0          ; r9 = key_material[i] ^ PC_XOR[i] (XOR FIRST)
0x001005e4  subf r9, r6, r9         ; r9 = r9 - counter
0x001005e8  addi r6, r6, 1          ; counter++
0x001005ec  addi r9, r9, -0x2b      ; r9 = r9 - 0x2b
0x001005f0  stb r9, 0(r10)          ; result[i] = r9
0x001005f4  bdnz 0x1005c8           ; loop
```

**Formula: `key[i] = (km[i] ^ DAT_00150ee0[i]) - i - 0x2B`**

Data at 0x150ee0:
`ec 6d 70 6b 1e 0a 9a 75 8c da 78 27 51 a3 c3 7b`

---

## 5. THE CRITICAL IV XOR OPERATIONS

### 5a. PSP IV XOR (first 8 bytes of IV)

**After PSP key derivation, flows to 0x100514:**

```asm
; Copy IV base to stack
0x00100514  addi r3, r1, 0x230      ; dest = stack at 0x230 (16-byte IV buffer)
0x00100518  lis r4, 0x15
0x00100520  addi r4, r4, 0xeb0      ; src = DAT_00150eb0 (PSP IV base)
0x0010051c  li r5, 0x10             ; len = 16
0x0010052c  bl 0x12f670             ; memcpy(stack+0x230, DAT_00150eb0, 16)

; Load context pointer
0x00100530  clrldi r9, r22, 0x20    ; r9 = param_1 (context ptr)
0x00100534  b 0x100614              ; → shared IV XOR code
```

PSP IV base at 0x150eb0:
`3a 9d f3 3b 99 cf 9c 0d bf 58 81 12 6c 18 32 64`

Then falls into the SHARED IV XOR at 0x100614 (see section 5d below).

---

### 5b. PHONE IV XOR (second 8 bytes of IV!)

**After Phone key derivation, flows to 0x100590:**

```asm
; Copy IV base to stack
0x00100590  addi r3, r1, 0x230      ; dest = stack at 0x230
0x00100594  lis r4, 0x15
0x0010059c  addi r4, r4, 0xed0      ; src = DAT_00150ed0 (Phone IV base)
0x001005a0  li r5, 0x10             ; len = 16
0x001005a8  bl 0x12f670             ; memcpy(stack+0x230, DAT_00150ed0, 16)

; XOR SECOND 8 bytes of IV (offset 0x238 = 0x230 + 8)
0x001005ac  clrldi r9, r22, 0x20    ; r9 = param_1
0x001005b0  ld r0, 0x238(r1)        ; r0 = IV bytes [8..15] (second half!)
0x001005b4  lwz r9, 0x38(r9)        ; r9 = *(param_1 + 0x38) = reg_params ptr
0x001005b8  ld r9, 0(r9)            ; r9 = first 8 bytes of reg_params = PIN value
0x001005bc  xor r0, r0, r9          ; r0 = IV[8..15] ^ PIN
0x001005c0  std r0, 0x238(r1)       ; store XOR'd bytes back to IV[8..15]
0x001005c4  b 0x100628              ; skip to AES decrypt
```

Phone IV base at 0x150ed0:
`29 0d e9 07 e2 3b e2 fc 34 08 ca 4b de e4 af 3a`

**PHONE XORs the SECOND 8 bytes (offset 8-15) of the IV with the context value.**

---

### 5c. PC/VITA IV XOR (first 8 bytes of IV)

**After PC key derivation loop, flows to 0x1005f8:**

```asm
; Copy IV base to stack
0x001005f8  addi r3, r1, 0x230      ; dest = stack at 0x230
0x001005fc  lis r4, 0x15
0x00100604  addi r4, r4, 0xef0      ; src = DAT_00150ef0 (PC IV base)
0x00100608  li r5, 0x10             ; len = 16
0x0010060c  bl 0x12f670             ; memcpy(stack+0x230, DAT_00150ef0, 16)

; Load context pointer (does NOT jump; falls through)
0x00100610  clrldi r9, r22, 0x20    ; r9 = param_1
; Falls through to 0x100614
```

PC IV base at 0x150ef0:
`5b 64 40 c5 2e 74 c0 46 48 72 c9 c5 49 0c 79 04`

---

### 5d. THE SHARED IV XOR CODE (0x100614) - Used by PSP and PC/VITA

```asm
0x00100610  clrldi r9, r22, 0x20    ; r9 = (uint32_t)param_1  [only for PC path]
0x00100614  lwz r9, 0x38(r9)        ; r9 = *(uint32_t*)(param_1 + 0x38) = reg_params ptr
0x00100618  ld r0, 0x230(r1)        ; r0 = IV bytes [0..7] (first half)
0x0010061c  ld r9, 0(r9)            ; r9 = *(uint64_t*)reg_params = first 8 bytes = PIN
0x00100620  xor r0, r0, r9          ; r0 = IV[0..7] ^ PIN
0x00100624  std r0, 0x230(r1)       ; store XOR'd bytes back to IV[0..7]
```

**PSP and PC/VITA XOR the FIRST 8 bytes (offset 0-7) of the IV with the context value.**

---

## 6. DETAILED POINTER CHAIN ANALYSIS

The assembly at 0x100614-0x10061c performs this pointer chain:

```
r9 = r22                    ; param_1 (context struct, 2152 bytes)
     ↓ clrldi (32-bit clean)
r9 = *(uint32_t*)(r9 + 0x38)  ; lwz loads 4-byte POINTER from context+0x38
                               ; This is the reg_params pointer (set by FUN_0010150c)
     ↓
r9 = *(uint64_t*)(r9 + 0x00)  ; ld loads 8-byte VALUE from reg_params+0x00
                               ; This is the PIN stored as (longlong)(int)PIN
```

NOTE: `lwz` at 0x100614 loads a **32-bit pointer** (standard for PS3 SPU/PPU 32-bit addressing).
Then `ld` at 0x10061c loads **64 bits** from the pointed-to address.

The Ghidra decompiler represents this as `**(ulonglong **)(param_1 + 0x38)` which is correct:
1. First `*` = dereference param_1+0x38 to get the pointer (lwz)
2. Second `*` = dereference that pointer to get the 8-byte value (ld)

**This is indeed a double dereference, NOT single as previously claimed in finding #18.**
The `lwz` at 0x100614 is a load (dereference #1), and `ld` at 0x10061c is another load (dereference #2).

---

## 7. CRITICAL QUESTION: IS THE CONTEXT VALUE THE PIN?

### Evidence FOR the PIN hypothesis:

1. **Call chain is clear:** FUN_000fe6b8 stores `(longlong)(int)PIN` at reg_params+0x00
2. **FUN_0010150c stores reg_params at context+0x38:** `*(context + 0x38) = param_5 = reg_params`
3. **The handler reads reg_params[0x00] via the same pointer chain:** device type read at 0x20 uses the same `lwz r9, 0x38(r9)` pattern
4. **The Ghidra decompiled C confirms it:** Line shows `*reg_params = (longlong)(int)uVar3`

### Evidence AGAINST / alternative possibilities:

1. **Tests failed:** PIN-as-big-endian-int was tried and returned error 80029820

2. **Could param_5 in FUN_0010150c actually be something else?**
   FUN_0010150c is declared as: `int FUN_0010150c(int param_1, undefined8 param_2, undefined4 param_3, undefined4 param_4, undefined4 param_5)`

   On PPC64 calling convention, `undefined8 param_2` consumes ONE register (r4).
   `undefined4 param_3` = r5, `undefined4 param_4` = r6, `undefined4 param_5` = r7.

   The caller at FUN_00101000:
   ```
   FUN_0010150c(server_ctx + 0x40, port, config, &reg_info, reg_params)
   ```
   r3 = server_ctx+0x40, r4 = port (0x7d2), r5 = config (3), r6 = &reg_info, r7 = reg_params

   This mapping looks correct. No parameter shift issue on PPC64 since `undefined8` fits in one 64-bit register.

3. **Could the value have been overwritten between FUN_000fe6b8 and the handler?**
   The handler runs in a separate thread. Between PIN generation and registration receipt, the reg_params struct could theoretically be modified. However, there is no evidence of this in the code.

4. **Could it NOT be a PIN at all?** Other candidates:
   - **Hardware ID:** No. The structure is zeroed by memset and filled only with PIN, SSID, and device type.
   - **MAC address:** No. MAC is at a different offset (context+0x3c area, not in reg_params).
   - **Timestamp:** No. The value is explicitly computed as `random % 90000000 + 10000000`.
   - **Session token:** No. The value is generated once at registration start, not per-connection.

### CONCLUSION: The context value IS the PIN.

The PIN is stored as `(longlong)(int)PIN` at reg_params+0x00 in big-endian format on PPC.

For PIN 12345678: the 8 XOR bytes are `00 00 00 00 00 BC 61 4E`.

---

## 8. WHY DO TESTS FAIL?

Since the context value IS confirmed as the PIN, the failure must be in one of these areas:

### 8a. Static key verification

Binary hex dumps of all 6 keys (verified via radare2 `px` command):

| Address | Name | Hex Bytes |
|---------|------|-----------|
| 0x150ea0 | PSP XOR Key | `fd c3 f6 a6 4d 2a ba 7a 38 92 6c bc 34 31 e1 0e` |
| 0x150eb0 | PSP IV Base | `3a 9d f3 3b 99 cf 9c 0d bf 58 81 12 6c 18 32 64` |
| 0x150ec0 | Phone XOR Key | `f1 16 f0 da 44 2c 06 c2 45 b1 5e 48 f9 04 e3 e6` |
| 0x150ed0 | Phone IV Base | `29 0d e9 07 e2 3b e2 fc 34 08 ca 4b de e4 af 3a` |
| 0x150ee0 | PC XOR Key | `ec 6d 70 6b 1e 0a 9a 75 8c da 78 27 51 a3 c3 7b` |
| 0x150ef0 | PC IV Base | `5b 64 40 c5 2e 74 c0 46 48 72 c9 c5 49 0c 79 04` |

### 8b. Key derivation formula correctness

All three formulas verified against assembly:

| Device | Formula | Constant |
|--------|---------|----------|
| PSP (type 0, iVar5=1) | `key[i] = (km[i] ^ XOR[i]) - i - C` | C = 0x25 |
| Phone (type 1, iVar5=2) | `key[i] = (km[i] - i - C) ^ XOR[i]` | C = 0x28 |
| PC/VITA (type 2, iVar5=3) | `key[i] = (km[i] ^ XOR[i]) - i - C` | C = 0x2B |

**IMPORTANT:** Phone uses a different operation order (subtract-then-XOR) vs PSP/PC (XOR-then-subtract).

### 8c. IV XOR position

| Device | Which 8 bytes of IV are XOR'd | Stack offset |
|--------|-------------------------------|--------------|
| PSP | First 8 bytes (IV[0..7]) | 0x230(r1) |
| Phone | Second 8 bytes (IV[8..15]) | 0x238(r1) |
| PC/VITA | First 8 bytes (IV[0..7]) | 0x230(r1) |

### 8d. Key material source

| Device | Key material source |
|--------|-------------------|
| PSP (iVar5=1) | Last 16 bytes of HTTP body (client-appended) |
| Phone (iVar5=2) | Last 16 bytes of HTTP body (client-appended) |
| PC/VITA (iVar5=3) | context+0x184 (pre-shared, possibly derived from PIN display on PS3) |

### 8e. Encrypted data range

| Device | Encrypted portion of body |
|--------|--------------------------|
| PSP | body[0 .. body_size - 17] (exclude last 16 bytes) |
| Phone | body[0 .. body_size - 17] (exclude last 16 bytes) |
| PC/VITA | body[0x1E0 .. body_size] (skip first 480 bytes, minimum 512 total) |

### 8f. Most likely failure causes (ordered by probability)

1. **Firmware version mismatch:** Keys extracted from FW 4.90 binary, but the HEN PS3 may run FW 4.91/4.92. If Sony changed the static keys between versions, all registration attempts fail. This is the most likely cause and easiest to verify -- dump the actual sysconf_plugin.sprx from the HEN PS3 and compare key bytes.

2. **Key material (client random bytes) not being generated/sent correctly** in the test scripts. The client is supposed to append 16 random bytes to the encrypted body.

3. **AES-CBC padding issue:** The body needs to be padded to 16-byte boundary. The test scripts may be using PKCS7 padding while the PS3 expects zero-padding (confirmed by code: "body padded to 16-byte boundary zero-padded").

4. **Content-Length header format:** PS3 uses `%10d` format (10-digit padded). Test scripts may use a different format.

5. **Byte overflow in key derivation:** The subtract operation can produce negative values. In C/assembly, the `stb` instruction stores only the low 8 bits, making this effectively modular arithmetic (mod 256). Test scripts must match this behavior.

---

## 9. SUMMARY OF ALL XORS IN THE HANDLER

| Address | Operation | Purpose | Registers |
|---------|-----------|---------|-----------|
| 0x1004FC | `xor r9, r9, r0` | PSP key derivation: `km[i] ^ PSP_XOR[i]` | r9=km byte, r0=XOR table byte |
| 0x100584 | `xor r9, r9, r0` | Phone key derivation: `(km[i]-i-0x28) ^ PHONE_XOR[i]` | r9=adjusted km, r0=XOR table byte |
| 0x1005BC | `xor r0, r0, r9` | Phone IV: `IV[8..15] ^ PIN_value` | r0=IV 2nd half, r9=PIN as uint64 |
| 0x1005E0 | `xor r9, r9, r0` | PC key derivation: `km[i] ^ PC_XOR[i]` | r9=km byte, r0=XOR table byte |
| 0x100620 | `xor r0, r0, r9` | PSP/PC IV: `IV[0..7] ^ PIN_value` | r0=IV 1st half, r9=PIN as uint64 |

---

## 10. FINAL VERDICT

**The 8-byte context value at offset 0x38 IS the PIN, stored as `(longlong)(int)PIN` in big-endian format.**

There is no plausible alternative interpretation:
- The reg_params structure is freshly zeroed and only filled with PIN (offset 0), SSID (offset 8), and device type (offset 0x20).
- No other code modifies reg_params[0x00] between generation and use.
- The value is demonstrably a random 8-digit number (10000000-99999999).

The test failures must be attributed to other factors -- most likely firmware version key differences, encryption padding, or test script implementation issues, NOT a wrong understanding of what the IV XOR context value is.
