# Assembly Analysis: IV XOR is Single Dereference, Not Double

## KEY FINDING
The Ghidra decompiler showed `**(ulonglong **)(param_1 + 0x38)` but the actual assembly is:
```asm
lwz r9, 0x38(r9)   ; load pointer from param_1+0x38
ld  r9, 0x0(r9)    ; load 8 bytes from pointer (SINGLE deref)
xor r0, r0, r9     ; XOR into IV
```
This is a SINGLE dereference: context = first 8 bytes at `*(param_1+0x38)`.

## BIGGER FINDING
The premo_plugin.elf.c export revealed the nonce formula is MORE COMPLEX than what the decompiler showed:
```c
// Phone nonce (from exported C):
*pbVar4 = ((cVar1 + ')' + *pcVar2 ^ *pbVar7 ^ *pbVar3) - cVar1) + 0xb7 ^ *pbVar6;
```
This uses 4 inputs and 2 static key tables. The registration encryption likely has similar complexity
that the decompiler simplified incorrectly.

## Status
- The registration key derivation needs re-analysis against the assembly
- HEN remains the guaranteed alternative path
- PS3 firmware version needed for HEN instructions
