# IV XOR Finder Results

Date: 2026-03-28

## Binary Scan Results

### 1. sysconf_plugin.elf (PS3 firmware - registration handler)

**Architecture:** PPC64 ELF (PS3)
**File size:** 2,426,896 bytes

**IV Base Constants: ALL THREE FOUND**

| Platform | Endian | Offset     | Value (hex)          |
|----------|--------|------------|----------------------|
| PSP      | BE     | 0x00150FA0 | 3a9df33b99cf9c0d     |
| Phone    | BE     | 0x00150FC0 | 290de907e23be2fc     |
| PC       | BE     | 0x00150FE0 | 5b6440c52e74c046     |

**XOR Instructions Near IV Bases: NONE FOUND** (even with 2048-byte window)

This is expected -- the IV base values live in a **data table** (at ~0x150F90), while the XOR
code that operates on them is in the `.text` section. The PPC code loads the IV base via
pointer/offset, so the XOR instruction is not "near" the data in the binary layout.

**Registration XOR Keys:** Not found as raw bytes in this binary.
**Session Keys (SKEY0/SKEY2):** Not found as raw bytes in this binary.

#### Data Table Structure (0x150F60 - 0x150FF0)

The registration key table starts with "PSND" magic at 0x150F60, followed by three 32-byte
entries (one per platform). Each entry is:

```
Offset  Content
------  -------
0x150F60: 50534E44 00000000 01000000 00000000   "PSND" + flags
0x150F70: 00310000 00000000 00000000 62c10e26   config (0x31)
0x150F80: 00330000 00000000 00000000 64a141e4   config (0x33)

PSP Entry (0x150F90, 32 bytes):
  +00: fdc3f6a6 4d2aba7a  (8 bytes - registration AES key high?)
  +08: 38926cbc 3431e10e  (8 bytes - registration AES key low?)
  +16: 3a9df33b 99cf9c0d  (8 bytes - IV BASE PSP) [KNOWN]
  +24: bf588112 6c183264  (8 bytes - UNKNOWN: IV context? post-IV data?)

Phone Entry (0x150FB0, 32 bytes):
  +00: f116f0da 442c06c2  (8 bytes)
  +08: 45b15e48 f904e3e6  (8 bytes)
  +16: 290de907 e23be2fc  (8 bytes - IV BASE Phone) [KNOWN]
  +24: 3408ca4b dee4af3a  (8 bytes - UNKNOWN)

PC Entry (0x150FD0, 32 bytes):
  +00: ec6d706b 1e0a9a75  (8 bytes)
  +08: 8cda7827 51a3c37b  (8 bytes)
  +16: 5b6440c5 2e74c046  (8 bytes - IV BASE PC) [KNOWN]
  +24: 4872c9c5 490c7904  (8 bytes - UNKNOWN)

0x150FF0: 48545450 2f312e31 ...  "HTTP/1.1 403 For..." (end of table)
```

**CRITICAL OBSERVATION:** Each entry has an 8-byte value AFTER the IV base:

| Platform | Post-IV 8 bytes   |
|----------|-------------------|
| PSP      | bf5881126c183264  |
| Phone    | 3408ca4bdee4af3a  |
| PC       | 4872c9c5490c7904  |

These could be the IV context values, or related to them.

#### XOR Analysis of Post-IV Values

| Computation              | Result           |
|--------------------------|------------------|
| PSP post8 XOR IV         | 85c57229f5d7ae69 |
| Phone post8 XOR IV       | 1d05234c3cdf4dc6 |
| PC post8 XOR IV          | 131689006778b942 |
| PSP post8 XOR REG_XOR    | d2ffbf1c3f74cdf6 |
| Phone post8 XOR REG_XOR  | 47e011e96e5f8236 |
| PC post8 XOR REG_XOR     | 6519ae1288ec10f4 |

No obvious pattern between platforms from these XOR combinations.

---

### 2. premo_plugin.elf (PS3 firmware - session handler)

**Architecture:** PPC64 ELF (PS3)
**File size:** 291,520 bytes

**Result: NO IV bases, NO registration keys, NO session keys found.**

This binary handles session/streaming, not registration. The registration handler
is in sysconf_plugin.elf.

---

### 3. VRPMFMGR.dll (VAIO Remote Play Manager)

**Architecture:** PE/DLL (x86)
**File size:** 700,928 bytes

**Result: NO IV bases, NO registration keys, NO session keys found.**

Manual xxd searches for all byte patterns (big-endian and little-endian) returned nothing.

---

### 4. VRPSDK.dll (VAIO Remote Play SDK)

**Architecture:** PE/DLL (x86)
**File size:** 1,319,424 bytes

**Result: NO IV bases, NO registration keys, NO session keys found.**

Manual xxd searches for all byte patterns (big-endian and little-endian) returned nothing.

---

### 5. VRP.exe (VAIO Remote Play GUI)

**Architecture:** PE/DLL (x86)
**File size:** 1,182,976 bytes

**Result: NO IV bases, NO registration keys, NO session keys found.**

Manual xxd searches for all byte patterns (big-endian and little-endian) returned nothing.

---

### 6. rmp_dll.dll (patch DLL)

**Architecture:** PE/DLL (x86)
**File size:** 4,096 bytes (very small - likely a stub/hook)

**Result: NO IV bases, NO registration keys, NO session keys found.**

---

## Manual xxd Pattern Search Results

Searched all VAIO binaries for the following patterns with ZERO matches:

**Big-endian patterns searched:**
- `f116 f0da` (Phone entry prefix)
- `ec6d 706b` (PC entry prefix)
- `5b64 40c5` (PC IV base)
- `d1b2 12eb` (?)
- `3a9d f33b` (PSP IV base)

**Little-endian patterns searched:**
- `daf0 16f1` (Phone entry prefix reversed)
- `6b70 6dec` (PC entry prefix reversed)
- `c540 645b` (PC IV base LE)
- `46c0 742e` (PC IV base LE second half)

**Session/registration keys searched:**
- `c99d acf2` / `f2ac 9dc9` / `3f4b 71e9` (SKEY0 variants)
- `0d2a 2a2f` / `2f2a 2a0d` (SKEY2 variants)
- `2d6b 67d7` / `d767 6b2d` / `f069 e0c1` (REG_XOR_PC variants)

**Conclusion:** The VAIO binaries do not contain any of the known crypto constants
in raw form. The keys are either:
1. Computed at runtime (derived from other data)
2. Encrypted/obfuscated within the DLLs
3. Loaded from external configuration files
4. Only present on the PS3 side (VAIO sends PIN, PS3 does all crypto)

---

## Summary and Implications

1. **sysconf_plugin.elf** is the ONLY binary containing IV base constants. They live in a
   data table at offset ~0x150F90, structured as 3 x 32-byte entries.

2. **No XOR instructions found near IV data** because the data section is far from the
   code section in PPC ELF layout. The XOR operation is in the `.text` section and accesses
   the data table via register-indirect addressing.

3. **The 8 bytes AFTER each IV base** (post-IV) are interesting candidates:
   - PSP:   `bf5881126c183264`
   - Phone: `3408ca4bdee4af3a`
   - PC:    `4872c9c5490c7904`
   These are stored directly adjacent to each IV base in the same table entry.

4. **VAIO binaries contain NONE of the known crypto constants.** This strongly suggests
   the VAIO client does NOT perform the IV XOR operation itself -- it sends the PIN to the
   PS3, and the PS3 firmware (sysconf_plugin) does all the key derivation and encryption.

5. **Next steps to find the XOR code:**
   - Use Ghidra/radare2 to find cross-references (XREF) to the IV base addresses in code
   - The code that reads from 0x150FA0 (IV_PSP) will contain the XOR with the context value
   - Look for `ld`/`lwz` instructions that reference the table base address, then follow
     to the XOR
   - The "pre-16" bytes might be the AES key, and "post-8" might be the answer
