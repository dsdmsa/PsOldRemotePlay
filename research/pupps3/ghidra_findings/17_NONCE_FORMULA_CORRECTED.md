# CRITICAL CORRECTION: Nonce/Key Derivation is More Complex Than Initially Thought

## From premo_plugin.elf.c export (NOT the decompiler window)

The exported C code reveals the nonce derivation uses **4 inputs**, not 2:

### PSP (type 0) — Simple XOR
```c
nonce[i] = pkey[i] ^ DAT_0002d548[i]
```
Note: DAT_0002d548, NOT DAT_0002d558! (The address was wrong in our earlier analysis)

### Phone (type 1) — Complex formula
```c
nonce[i] = ((i + 0x29 + param_5[i] ^ DAT_0002d578[i] ^ pkey[i]) - i) + 0xb7 ^ DAT_0002d568[i]
```
Where:
- `pkey[i]` = param_4[i] = the private key
- `param_5[i]` = **additional data** (likely MAC address or device-specific)
- `DAT_0002d578[i]` = Phone XOR key 1 (already extracted)
- `DAT_0002d568[i]` = Phone XOR key 2 (the "unknown" key we noted!)

### PC (type 2) — Complex formula
```c
nonce[i] = ((i + 0x33 + param_5[i] ^ DAT_0002d598[i] ^ pkey[i]) - i) - 0x3f ^ DAT_0002d588[i]
```

### VITA (type 3) — Complex formula
```c
nonce[i] = ((i + 0x3D + param_5[i] ^ DAT_0002d5b8[i] ^ pkey[i]) - i) - 0x2f ^ DAT_0002d5a8[i]
```

## What param_5 Is

From line 10386: `FUN_0001b5b4(*(param_1+0x38), param_2, param_3, param_5, param_8, ...)`
And line 10393: `FUN_00029b08(param_1+0x50, param_6, 6)` — copies 6 bytes (MAC!) to param_1+0x50

param_5 is passed as the 5th argument to the nonce function. Looking at the caller, it's likely the **client's MAC address** or the session-specific data.

## Impact on Registration

The registration encryption in sysconf_plugin likely uses the SAME complex formula, not the simple XOR we implemented. The "unknown context value" isn't an IV XOR — it's a **second data input to the key derivation formula**.

## Static Keys (CORRECTED addresses)

| Platform | Key 1 (XOR) | Key 2 (outer XOR) |
|----------|-------------|-------------------|
| PSP | DAT_0002d548 | (none — simple XOR) |
| Phone | DAT_0002d578 | DAT_0002d568 |
| PC | DAT_0002d598 | DAT_0002d588 |
| VITA | DAT_0002d5b8 | DAT_0002d5a8 |

## NEXT STEP
Re-read the sysconf_plugin registration handler with this new understanding.
The registration key derivation likely has a similar multi-input formula.
The "IV context XOR" might actually be a different operation entirely.
