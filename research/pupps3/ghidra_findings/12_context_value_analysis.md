---
# Ghidra Finding: Analysis of param_1 + 0x38 Context Value
# UPDATED 2026-03-28: Complete decompiled code analysis confirms PIN hypothesis
---

## What We Know (CONFIRMED from decompiled UndefinedFunction_00100168)

The registration handler accesses a structure via `*(param_1 + 0x38)`:

```c
// Device type (0=PSP, 1=Phone, 2=PC/VITA)
cVar12 = *(char *)(*(int *)(param_1 + 0x38) + 0x20);

// IV XOR value — PSP and PC/VITA: first 8 bytes of IV
uStack_80 = uStack_80 ^ **(ulonglong **)(param_1 + 0x38);

// IV XOR value — Phone: second 8 bytes of IV
uStack_78 = uStack_78 ^ **(ulonglong **)(param_1 + 0x38);

// Registration state (set to 2 on success)
*(undefined4 *)(*(int *)(param_1 + 0x38) + 0x18) = 2;
```

## The Structure at *(param_1 + 0x38)

| Offset | Size | Content | Source |
|--------|------|---------|--------|
| **+0x00** | **8** | **PIN as big-endian longlong** | `FUN_000fe6b8` line 68897 |
| +0x08 | 16 | SSID string "PS3REGIST_XXXX" or "PS3REGPC_XXXX" | `FUN_000fe6b8` line 68901 |
| +0x18 | 4 | Registration state (set to 2 on success) | Registration handler |
| +0x1C | 6 | PS3 MAC address | (accessed via param_1 + 0x3c) |
| +0x20 | 1 | Device type (0=PSP, 1=Phone, 2=PC/VITA) | `FUN_000fe6b8` line 68891 |
| +0x24 | var | PS3 nickname string | Registration info struct |

## DEFINITIVE: PIN Storage at Offset 0x00

From `FUN_000fe6b8` (sysconf_plugin.elf.c line 68881-68908):

```c
int FUN_000fe6b8(int param_1, longlong *param_2)
{
    FUN_0012f638(param_2, 0, 0x28);                    // memset 40 bytes to zero
    *(char *)(param_2 + 4) = (char)param_1;             // device type at byte offset 0x20
    uVar1 = FUN_0012bdc8(0);                            // init random
    FUN_0012b8c0(uVar1);                                // seed random
    iVar2 = FUN_0012b8f8();                             // generate random int
    uVar3 = iVar2 % 90000000 + 10000000;               // 8-digit PIN: 10000000-99999999
    *param_2 = (longlong)(int)uVar3;                    // Store PIN at offset 0x00 as longlong!
    pcVar4 = (param_1 != 1) ? "PS3REGIST_%04d" : "PS3REGPC_%04d";
    FUN_0012f408(param_2 + 1, 0x10, pcVar4, uVar3 / 10000);  // SSID at offset 0x08
}
```

## Call Chain (VERIFIED from C decompilation)

```
FUN_000fe6b8(type, reg_params)         → reg_params[0x00] = (longlong)PIN
    ↓
FUN_000fe4cc(reg_params)               → param_1 = reg_params
    ↓
FUN_00101000(..., &reg_info, reg_params)  → param_6 = reg_params
    ↓
FUN_0010150c(ctx, ..., &reg_info, reg_params)  → param_5 = reg_params
    → *(ctx + 0x38) = param_5 = reg_params     // line 69885
    → *(ctx + 0x3c) = param_4 = &reg_info      // line 69882
    ↓
UndefinedFunction_00100168(ctx)
    → *(ctx + 0x38) points to reg_params
    → **(ulonglong **)(ctx + 0x38) reads first 8 bytes = PIN as longlong
```

## PIN as Big-Endian Longlong (Example)

PIN = 12345678
- (int)12345678 = 0x00BC614E
- (longlong)(int)12345678 = 0x0000000000BC614E
- Big-endian bytes: `00 00 00 00 00 BC 61 4E`

PIN = 99999999 (maximum)
- (int)99999999 = 0x05F5E0FF
- Big-endian bytes: `00 00 00 00 05 F5 E0 FF`

**First 4 bytes are ALWAYS zero** (PIN fits in 32 bits, stored as 64-bit).

## IV XOR Effect

### Phone (second 8 bytes XOR'd):
```
IV base:    29 0D E9 07 E2 3B E2 FC | 34 08 CA 4B DE E4 AF 3A
XOR value:  (not applied)            | 00 00 00 00 00 BC 61 4E
Result:     29 0D E9 07 E2 3B E2 FC | 34 08 CA 4B DE 58 CE 74
                                       unchanged      ^^^^^^^^^ only last 3-4 bytes change
```

### PSP/PC (first 8 bytes XOR'd):
```
IV base:    3A 9D F3 3B 99 CF 9C 0D | BF 58 81 12 6C 18 32 64
XOR value:  00 00 00 00 00 BC 61 4E | (not applied)
Result:     3A 9D F3 3B 99 73 FD 43 | BF 58 81 12 6C 18 32 64
                         ^^^^^^^^^ only last 3-4 bytes change
```

## TEST STATUS: FAILED

PIN-as-big-endian-int was tested in:
- `ps3_register.py` → context_candidates includes `pin_int.to_bytes(8, "big")`
- `ps3_register_single.py` → attempt 3 (Phone+Phone+PIN-be) and attempt 6 (Phone+PC+PIN-be)

All returned HTTP 403 with error code 80029820.

## POSSIBLE REASONS FOR FAILURE

1. **Static keys not verified** — The 6 registration keys (DAT_00150ea0-DAT_00150ef0) were extracted from Ghidra but never cross-checked against raw binary bytes. If even one byte is wrong, decryption fails.

2. **Parameter shift in FUN_0010150c** — The second parameter is `undefined8` (8 bytes). On PPC64, if this consumes two register slots, all subsequent parameters shift, and `param_5` would NOT be `reg_params` but something else entirely.

3. **Firmware version difference** — Keys extracted from FW 4.90, HEN PS3 runs FW 4.92. Keys might differ between versions.

4. **Test environment** — The PS3 may need to be in a specific state (WiFi AP active, registration UI showing PIN) for the handler to be active. Tests might have been run at the wrong time.

5. **The decompiler is wrong about the XOR** — The actual assembly might do additional operations not visible in the decompiled C.

## RECOMMENDED VERIFICATION

1. **Hex dump DAT_00150ea0-DAT_00150f00** from the actual sysconf_plugin.elf binary
2. **PS3MAPI peek** at the context value during registration to see the actual 8 bytes
3. **Try with zeros** as a baseline — if zeros work, the PIN mapping is wrong; if zeros also fail, the keys or formula are wrong
4. **Capture real registration** — use any working device to get known-good encrypted body
