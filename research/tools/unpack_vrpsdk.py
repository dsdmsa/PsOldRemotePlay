#!/usr/bin/env python3
"""
VRPSDK.dll Unpacker
Unpacks the XOR+ADD encrypted "ehleidof" section from VRPSDK.dll.

The packer (in the "wovitjaj" section) works as follows:
  1. Entry at 0x102c6000 (wovitjaj section)
  2. Anti-debug: checks if byte at 0x102c600a == 0xCC (int3 breakpoint)
  3. Calculates pointer to ehleidof+1 (0x1019a001)
  4. Calls FUN_102c6046(ptr=0x1019a001, size=0x1000, xor_key=0x6cfd7e36, add_val=0x3640736d)
  5. The decrypt loop: for each DWORD: *ptr ^= xor_key; *ptr += add_val;
  6. After decrypting the first 0x1000 bytes, jumps to decrypted code which likely
     decrypts the rest of ehleidof and then the original code sections.

Packer function FUN_102c6046:
  esi = ptr, ecx = size >> 2 (DWORD count), eax = xor_key, ebx = add_val
  loop: xor [esi], eax; add [esi], ebx; esi += 4; ecx--; until ecx == 0
"""

import struct
import sys
import os

DLL_PATH = "/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/extracted/vrp_files/App/VRPSDK.dll"
OUTPUT_DIR = "/Users/mihailurmanschi/Work/PsOldRemotePlay/research/tools/unpack_results"

XOR_KEY = 0x6cfd7e36
ADD_VAL = 0x3640736d

# Section info from radare2 analysis
SECTIONS = {
    "section0": {"paddr": 0x1000, "vaddr": 0x10001000, "size": 0x12200, "vsize": 0x28000, "name": "(unnamed)"},
    "rsrc":     {"paddr": 0x13200, "vaddr": 0x10029000, "size": 0x2c00, "vsize": 0x3000, "name": ".rsrc"},
    "idata":    {"paddr": 0x15e00, "vaddr": 0x1002c000, "size": 0x200, "vsize": 0x1000, "name": ".idata"},
    "section3": {"paddr": 0x16000, "vaddr": 0x1002d000, "size": 0x200, "vsize": 0x16d000, "name": "(unnamed rwx)"},
    "ehleidof": {"paddr": 0x16200, "vaddr": 0x1019a000, "size": 0x12be00, "vsize": 0x12c000, "name": "ehleidof"},
    "wovitjaj": {"paddr": 0x142000, "vaddr": 0x102c6000, "size": 0x200, "vsize": 0x1000, "name": "wovitjaj"},
}


def decrypt_dwords(data, xor_key, add_val, offset=0, count=None):
    """Decrypt data using XOR+ADD on DWORDs, starting at byte offset within data."""
    result = bytearray(data)
    if count is None:
        # Decrypt all complete DWORDs from offset to end
        count = (len(data) - offset) // 4

    for i in range(count):
        pos = offset + i * 4
        if pos + 4 > len(result):
            break
        val = struct.unpack_from('<I', result, pos)[0]
        val ^= xor_key
        val = (val + add_val) & 0xFFFFFFFF
        struct.pack_into('<I', result, pos, val)
    return bytes(result)


def decrypt_xor_only(data, xor_key, offset=0, count=None):
    """Decrypt data using XOR only on DWORDs."""
    result = bytearray(data)
    if count is None:
        count = (len(data) - offset) // 4

    for i in range(count):
        pos = offset + i * 4
        if pos + 4 > len(result):
            break
        val = struct.unpack_from('<I', result, pos)[0]
        val ^= xor_key
        struct.pack_into('<I', result, pos, val)
    return bytes(result)


def decrypt_sub(data, xor_key, sub_val, offset=0, count=None):
    """Decrypt data using SUB then XOR (reverse of XOR then ADD)."""
    result = bytearray(data)
    if count is None:
        count = (len(data) - offset) // 4

    for i in range(count):
        pos = offset + i * 4
        if pos + 4 > len(result):
            break
        val = struct.unpack_from('<I', result, pos)[0]
        val = (val - sub_val) & 0xFFFFFFFF
        val ^= xor_key
        struct.pack_into('<I', result, pos, val)
    return bytes(result)


def find_strings(data, min_len=4):
    """Find ASCII strings in data."""
    strings = []
    current = b""
    start = 0
    for i, b in enumerate(data):
        if 0x20 <= b < 0x7F:
            if not current:
                start = i
            current += bytes([b])
        else:
            if len(current) >= min_len:
                strings.append((start, current.decode('ascii', errors='replace')))
            current = b""
    if len(current) >= min_len:
        strings.append((start, current.decode('ascii', errors='replace')))
    return strings


def find_wide_strings(data, min_len=4):
    """Find UTF-16LE strings in data."""
    strings = []
    i = 0
    while i < len(data) - 1:
        start = i
        chars = []
        while i < len(data) - 1:
            lo, hi = data[i], data[i+1]
            if hi == 0 and 0x20 <= lo < 0x7F:
                chars.append(chr(lo))
                i += 2
            else:
                break
        if len(chars) >= min_len:
            strings.append((start, ''.join(chars)))
        else:
            i += 2 if i == start else 0
        if i == start:
            i += 2
    return strings


KNOWN_PATTERNS = [
    b"regist", b"premo", b"/sce/", b"BCrypt", b"PREMO-", b"Client-Type",
    b"HTTP", b"GET ", b"POST", b"Content", b"AES", b"session",
    b"video", b"audio", b"crypto", b"register", b"nonce",
    b"kernel32", b"ntdll", b"ws2_32", b"winhttp", b"advapi32",
    b"LoadLibrary", b"GetProcAddress", b"VirtualProtect",
    b"CreateFile", b"socket", b"connect", b"send", b"recv",
    b"MZ", b"This program", b".text", b".rdata", b".data",
    b"push ebp", b"\x55\x8b\xec",  # push ebp; mov ebp, esp
    b"\x8b\xff\x55\x8b\xec",  # mov edi, edi; push ebp; mov ebp, esp (MSVC)
]


def check_known_patterns(data, label=""):
    """Check if decrypted data contains known strings/patterns."""
    found = []
    for pat in KNOWN_PATTERNS:
        positions = []
        start = 0
        while True:
            idx = data.find(pat, start)
            if idx == -1:
                break
            positions.append(idx)
            start = idx + 1
        if positions:
            try:
                pat_str = pat.decode('ascii', errors='replace')
            except:
                pat_str = pat.hex()
            found.append((pat_str, len(positions), positions[:5]))
    return found


def count_valid_x86(data, max_check=256):
    """Heuristic: count how many bytes look like valid x86 code prologues."""
    score = 0
    prologue_patterns = [
        b"\x55\x8b\xec",  # push ebp; mov ebp, esp
        b"\x8b\xff\x55",  # mov edi, edi; push ebp
        b"\x83\xec",      # sub esp, imm8
        b"\x56\x57",      # push esi; push edi
        b"\x53\x56",      # push ebx; push esi
        b"\x6a",          # push imm8
        b"\x68",          # push imm32
        b"\xff\x15",      # call [imm32]
        b"\xff\x25",      # jmp [imm32]
        b"\xc2",          # ret imm16
        b"\xc3",          # ret
        b"\xcc",          # int3
    ]
    for pat in prologue_patterns:
        idx = 0
        while idx < min(len(data), max_check * 16):
            pos = data.find(pat, idx)
            if pos == -1 or pos >= min(len(data), max_check * 16):
                break
            score += 1
            idx = pos + 1
    return score


def entropy(data):
    """Calculate Shannon entropy of data."""
    if not data:
        return 0
    freq = [0] * 256
    for b in data:
        freq[b] += 1
    ent = 0.0
    n = len(data)
    import math
    for f in freq:
        if f > 0:
            p = f / n
            ent -= p * math.log2(p)
    return ent


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    print(f"Reading {DLL_PATH}")
    with open(DLL_PATH, "rb") as f:
        dll_data = f.read()
    print(f"DLL size: {len(dll_data)} bytes ({len(dll_data)/1024:.1f} KB)")

    # Extract sections
    ehleidof = dll_data[SECTIONS["ehleidof"]["paddr"]:
                        SECTIONS["ehleidof"]["paddr"] + SECTIONS["ehleidof"]["size"]]
    wovitjaj = dll_data[SECTIONS["wovitjaj"]["paddr"]:
                        SECTIONS["wovitjaj"]["paddr"] + SECTIONS["wovitjaj"]["size"]]
    section0 = dll_data[SECTIONS["section0"]["paddr"]:
                        SECTIONS["section0"]["paddr"] + SECTIONS["section0"]["size"]]
    section3 = dll_data[SECTIONS["section3"]["paddr"]:
                        SECTIONS["section3"]["paddr"] + SECTIONS["section3"]["size"]]

    print(f"\nehleidof section: {len(ehleidof)} bytes, entropy: {entropy(ehleidof):.4f}")
    print(f"wovitjaj section: {len(wovitjaj)} bytes, entropy: {entropy(wovitjaj):.4f}")
    print(f"section0 (code):  {len(section0)} bytes, entropy: {entropy(section0):.4f}")
    print(f"section3 (rwx):   {len(section3)} bytes, entropy: {entropy(section3):.4f}")

    # Save raw ehleidof
    with open(os.path.join(OUTPUT_DIR, "ehleidof_raw.bin"), "wb") as f:
        f.write(ehleidof)
    print(f"\nSaved raw ehleidof to {OUTPUT_DIR}/ehleidof_raw.bin")

    # ========================================================================
    # STAGE 1: Decrypt first 0x1000 bytes of ehleidof (starting at offset 1)
    # This matches the packer call: decrypt(ehleidof+1, 0x1000, 0x6cfd7e36, 0x3640736d)
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 1: Decrypt first 0x1000 bytes at ehleidof+1")
    print(f"  XOR key: 0x{XOR_KEY:08x}")
    print(f"  ADD val: 0x{ADD_VAL:08x}")
    print("="*80)

    stage1 = decrypt_dwords(ehleidof, XOR_KEY, ADD_VAL, offset=1, count=0x1000 // 4)

    with open(os.path.join(OUTPUT_DIR, "ehleidof_stage1.bin"), "wb") as f:
        f.write(stage1)

    patterns = check_known_patterns(stage1)
    if patterns:
        print("\n  Known patterns found in stage1:")
        for pat, count, positions in patterns:
            print(f"    '{pat}' x{count} at {positions}")
    else:
        print("  No known patterns found in stage1")

    strings = find_strings(stage1[1:0x1001], min_len=5)
    if strings:
        print(f"\n  ASCII strings in decrypted region ({len(strings)} found):")
        for offset, s in strings[:30]:
            print(f"    0x{offset:06x}: {s}")
    else:
        print("  No ASCII strings found in decrypted stage1 region")

    wstrings = find_wide_strings(stage1[1:0x1001], min_len=4)
    if wstrings:
        print(f"\n  Wide strings in decrypted region ({len(wstrings)} found):")
        for offset, s in wstrings[:20]:
            print(f"    0x{offset:06x}: {s}")

    x86_score = count_valid_x86(stage1[1:0x1001], max_check=1024)
    print(f"\n  x86 prologue heuristic score: {x86_score}")
    print(f"  Entropy of decrypted first 0x1000: {entropy(stage1[1:0x1001]):.4f}")

    # ========================================================================
    # STAGE 1b: Try reverse operation (SUB then XOR) - in case packing is the
    # reverse of what we think
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 1b: Try reverse (SUB 0x3640736d then XOR 0x6cfd7e36) at offset 1")
    print("="*80)

    stage1b = decrypt_sub(ehleidof, XOR_KEY, ADD_VAL, offset=1, count=0x1000 // 4)
    patterns = check_known_patterns(stage1b)
    if patterns:
        print("  Known patterns found:")
        for pat, count, positions in patterns:
            print(f"    '{pat}' x{count} at {positions}")

    strings = find_strings(stage1b[1:0x1001], min_len=5)
    if strings:
        print(f"  ASCII strings ({len(strings)} found):")
        for offset, s in strings[:20]:
            print(f"    0x{offset:06x}: {s}")
    else:
        print("  No ASCII strings found")

    x86_score = count_valid_x86(stage1b[1:0x1001], max_check=1024)
    print(f"  x86 heuristic score: {x86_score}")
    print(f"  Entropy: {entropy(stage1b[1:0x1001]):.4f}")

    with open(os.path.join(OUTPUT_DIR, "ehleidof_stage1b_sub_xor.bin"), "wb") as f:
        f.write(stage1b)

    # ========================================================================
    # STAGE 2: Try XOR-only on entire ehleidof (no ADD)
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 2: XOR-only 0x6cfd7e36 on entire ehleidof")
    print("="*80)

    stage2 = decrypt_xor_only(ehleidof, XOR_KEY)
    patterns = check_known_patterns(stage2)
    if patterns:
        print("  Known patterns found:")
        for pat, count, positions in patterns:
            print(f"    '{pat}' x{count} at {positions}")

    strings = find_strings(stage2, min_len=6)
    if strings:
        print(f"  ASCII strings ({len(strings)} found, showing first 30):")
        for offset, s in strings[:30]:
            print(f"    0x{offset:06x}: {s}")
    else:
        print("  No meaningful ASCII strings")

    print(f"  Entropy: {entropy(stage2):.4f}")

    with open(os.path.join(OUTPUT_DIR, "ehleidof_xor_only.bin"), "wb") as f:
        f.write(stage2)

    # ========================================================================
    # STAGE 3: Decrypt ENTIRE ehleidof with XOR+ADD at offset 0
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 3: Full XOR+ADD on entire ehleidof at offset 0")
    print("="*80)

    stage3 = decrypt_dwords(ehleidof, XOR_KEY, ADD_VAL, offset=0)
    patterns = check_known_patterns(stage3)
    if patterns:
        print("  Known patterns found:")
        for pat, count, positions in patterns:
            print(f"    '{pat}' x{count} at {positions}")

    strings = find_strings(stage3, min_len=6)
    if strings:
        print(f"  ASCII strings ({len(strings)} found, showing first 30):")
        for offset, s in strings[:30]:
            print(f"    0x{offset:06x}: {s}")
    else:
        print("  No meaningful ASCII strings")

    print(f"  Entropy: {entropy(stage3):.4f}")

    with open(os.path.join(OUTPUT_DIR, "ehleidof_full_xor_add.bin"), "wb") as f:
        f.write(stage3)

    # ========================================================================
    # STAGE 3b: Full SUB then XOR on entire ehleidof at offset 0
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 3b: Full SUB+XOR (reverse) on entire ehleidof at offset 0")
    print("="*80)

    stage3b = decrypt_sub(ehleidof, XOR_KEY, ADD_VAL, offset=0)
    patterns = check_known_patterns(stage3b)
    if patterns:
        print("  Known patterns found:")
        for pat, count, positions in patterns:
            print(f"    '{pat}' x{count} at {positions}")

    strings = find_strings(stage3b, min_len=6)
    if strings:
        print(f"  ASCII strings ({len(strings)} found, showing first 30):")
        for offset, s in strings[:30]:
            print(f"    0x{offset:06x}: {s}")
    else:
        print("  No meaningful ASCII strings")

    print(f"  Entropy: {entropy(stage3b):.4f}")

    with open(os.path.join(OUTPUT_DIR, "ehleidof_full_sub_xor.bin"), "wb") as f:
        f.write(stage3b)

    # ========================================================================
    # STAGE 4: Check if the second-stage code at 0x102c60a8+ decrypts ehleidof
    # with different parameters. Analyze the obfuscated code after the first decrypt.
    # The code at 0x102c60ad computes: edx += 0xce26c3b6
    # Then uses edx as size, does shr ecx,4; shr ecx,1; xor ecx, 0xa1fc1b6f
    # and subtracts 0x7fe13f6d from value on stack...
    # This is obfuscated computation of the second-stage parameters.
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 4: Analyzing second-stage packer parameters")
    print("="*80)

    # After the first decrypt returns, we're at 0x102c603c:
    # add eax, 0    -> eax still = 0x1019a001
    # mov [esp+8], eax  -> save return address
    # pop ebx; pop eax; ret -> returns to 0x1019a001 (the just-decrypted code!)
    #
    # But wait - the code at 0x102c60a8 (jbe 0x102c6033) would loop back and
    # do another decrypt call if the condition is met. Let me trace what happens
    # after the ret at 0x102c6045...
    #
    # Actually the flow is:
    # 0x102c6037: call FUN_102c6046 (decrypt first 0x1000 of ehleidof)
    # 0x102c603c: add eax, 0 (nop)
    # 0x102c603f: mov [esp+8], eax  (eax = 0x1019a001)
    # 0x102c6043: pop ebx
    # 0x102c6044: pop eax
    # 0x102c6045: ret  -> pops 0x1019a001 and jumps there!
    #
    # So after decrypting the first 0x1000 bytes, it JUMPS INTO the decrypted code!
    # The decrypted code at ehleidof+1 is expected to be x86 that decrypts the rest.

    print("  After stage1 decrypt, execution jumps to ehleidof+1 (0x1019a001)")
    print("  The decrypted first 0x1000 bytes should contain a second-stage loader")
    print("  that decrypts the rest of the ehleidof section.")
    print()

    # Let's check if decrypted stage1 contains calls to VirtualProtect or similar
    # Let's disassemble the first bytes of the decrypted region
    print("  First 64 bytes of stage1-decrypted ehleidof+1:")
    chunk = stage1[1:65]
    hex_lines = []
    for i in range(0, len(chunk), 16):
        hex_part = ' '.join(f'{b:02x}' for b in chunk[i:i+16])
        ascii_part = ''.join(chr(b) if 0x20 <= b < 0x7f else '.' for b in chunk[i:i+16])
        hex_lines.append(f"    {i:04x}: {hex_part:<48s} {ascii_part}")
    print('\n'.join(hex_lines))

    print("\n  First 64 bytes of stage1b (SUB+XOR) ehleidof+1:")
    chunk = stage1b[1:65]
    hex_lines = []
    for i in range(0, len(chunk), 16):
        hex_part = ' '.join(f'{b:02x}' for b in chunk[i:i+16])
        ascii_part = ''.join(chr(b) if 0x20 <= b < 0x7f else '.' for b in chunk[i:i+16])
        hex_lines.append(f"    {i:04x}: {hex_part:<48s} {ascii_part}")
    print('\n'.join(hex_lines))

    # ========================================================================
    # STAGE 5: Try brute-force different ADD values with XOR 0x6cfd7e36
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 5: Brute-force ADD values (0-255) with XOR 0x6cfd7e36")
    print("="*80)

    best_score = 0
    best_add = 0
    for add_val in range(256):
        test = decrypt_dwords(ehleidof, XOR_KEY, add_val, offset=0, count=0x1000 // 4)
        score = count_valid_x86(test[:0x1000], max_check=4096)
        strs = find_strings(test[:0x1000], min_len=6)
        score += len(strs) * 5
        if score > best_score:
            best_score = score
            best_add = add_val
            if score > 10:
                print(f"  ADD=0x{add_val:02x}: score={score}, strings={len(strs)}")

    print(f"\n  Best ADD value: 0x{best_add:02x} with score {best_score}")

    # Also try with offset=1
    best_score_o1 = 0
    best_add_o1 = 0
    for add_val in range(256):
        test = decrypt_dwords(ehleidof, XOR_KEY, add_val, offset=1, count=0x1000 // 4)
        score = count_valid_x86(test[1:0x1001], max_check=4096)
        strs = find_strings(test[1:0x1001], min_len=6)
        score += len(strs) * 5
        if score > best_score_o1:
            best_score_o1 = score
            best_add_o1 = add_val
            if score > 10:
                print(f"  [offset=1] ADD=0x{add_val:02x}: score={score}, strings={len(strs)}")

    print(f"  Best ADD (offset=1): 0x{best_add_o1:02x} with score {best_score_o1}")

    # ========================================================================
    # STAGE 6: Try the known ADD=0x3640736d on section0 (the unnamed code section)
    # Maybe section0 is also encrypted?
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 6: Check if section0 (code section) is also encrypted")
    print("="*80)

    print(f"  Raw section0 entropy: {entropy(section0):.4f}")
    s0_patterns = check_known_patterns(section0)
    if s0_patterns:
        print("  Known patterns in raw section0:")
        for pat, count, positions in s0_patterns:
            print(f"    '{pat}' x{count} at {positions}")
    s0_strings = find_strings(section0, min_len=6)
    print(f"  ASCII strings in raw section0: {len(s0_strings)}")
    for offset, s in s0_strings[:10]:
        print(f"    0x{offset:06x}: {s}")

    s0_dec = decrypt_dwords(section0, XOR_KEY, ADD_VAL, offset=0)
    print(f"\n  Decrypted section0 entropy: {entropy(s0_dec):.4f}")
    s0_patterns = check_known_patterns(s0_dec)
    if s0_patterns:
        print("  Known patterns in decrypted section0:")
        for pat, count, positions in s0_patterns:
            print(f"    '{pat}' x{count} at {positions}")
    s0_strings = find_strings(s0_dec, min_len=6)
    print(f"  ASCII strings in decrypted section0: {len(s0_strings)}")
    for offset, s in s0_strings[:10]:
        print(f"    0x{offset:06x}: {s}")

    # ========================================================================
    # STAGE 7: Search for crypto constants in all variations
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 7: Search for crypto constants in raw and decrypted data")
    print("="*80)

    crypto_consts = {
        "SKEY0 (d1b212eb)": bytes.fromhex("d1b212eb"),
        "SKEY0 (LE)":       bytes.fromhex("eb12b2d1"),
        "PC XOR (ec6d706b)": bytes.fromhex("ec6d706b"),
        "PC XOR (LE)":       bytes.fromhex("6b706dec"),
        "PSP XOR (f116f0da)": bytes.fromhex("f116f0da"),
        "PSP XOR (LE)":       bytes.fromhex("daf016f1"),
        "Phone XOR (b62e7572)": bytes.fromhex("b62e7572"),
        "Phone XOR (LE)":       bytes.fromhex("72752eb6"),
        "AES const (637c7763)": bytes.fromhex("637c7763"),
        "AES const (LE)":       bytes.fromhex("63777c63"),
    }

    for label, const in crypto_consts.items():
        # Search in raw ehleidof
        idx = ehleidof.find(const)
        if idx != -1:
            print(f"  {label} found in raw ehleidof at offset 0x{idx:06x}")

        # Search in each decrypted variant
        for name, data in [("stage3 (xor+add)", stage3), ("stage3b (sub+xor)", stage3b),
                           ("stage2 (xor-only)", stage2)]:
            idx = data.find(const)
            if idx != -1:
                print(f"  {label} found in {name} at offset 0x{idx:06x}")

    # ========================================================================
    # STAGE 8: Dump hex of key areas for manual inspection
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 8: Hex dump of ehleidof header (first 256 bytes)")
    print("="*80)

    chunk = ehleidof[:256]
    for i in range(0, len(chunk), 16):
        hex_part = ' '.join(f'{b:02x}' for b in chunk[i:i+16])
        ascii_part = ''.join(chr(b) if 0x20 <= b < 0x7f else '.' for b in chunk[i:i+16])
        print(f"  {i:04x}: {hex_part:<48s} {ascii_part}")

    # ========================================================================
    # STAGE 9: Look for second-stage call pattern
    # The decrypted first 0x1000 bytes (at ehleidof+1) should contain another
    # call to a decrypt function. Search for the XOR key pattern.
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 9: Search for XOR key 0x6cfd7e36 in decrypted stage1 code")
    print("="*80)

    xor_bytes = struct.pack('<I', XOR_KEY)
    add_bytes = struct.pack('<I', ADD_VAL)

    for label, data in [("stage1 (xor+add,off=1)", stage1),
                         ("stage1b (sub+xor,off=1)", stage1b),
                         ("stage3 (full xor+add)", stage3),
                         ("stage3b (full sub+xor)", stage3b)]:
        idx = data.find(xor_bytes)
        while idx != -1:
            print(f"  XOR key found in {label} at offset 0x{idx:06x}")
            # Show surrounding bytes
            start = max(0, idx - 8)
            end = min(len(data), idx + 12)
            context = data[start:end]
            hex_str = ' '.join(f'{b:02x}' for b in context)
            print(f"    context: {hex_str}")
            idx = data.find(xor_bytes, idx + 1)

        idx = data.find(add_bytes)
        while idx != -1 and idx < 0x1100:  # Only check in first 0x1100 bytes for stage1
            print(f"  ADD key found in {label} at offset 0x{idx:06x}")
            start = max(0, idx - 8)
            end = min(len(data), idx + 12)
            context = data[start:end]
            hex_str = ' '.join(f'{b:02x}' for b in context)
            print(f"    context: {hex_str}")
            idx = data.find(add_bytes, idx + 1)

    # ========================================================================
    # STAGE 10: Try decrypting with parameters from the second-stage obfuscated code
    # From 0x102c60ad onwards, there's obfuscated parameter computation.
    # Let's try computing what the second stage might use.
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 10: Compute possible second-stage decrypt parameters")
    print("="*80)

    # From disasm at 0x102c60ad:
    # add edx, 0xce26c3b6  (edx was... something from earlier context)
    # mov eax, edx
    # mov edi, eax
    # mov ecx, edi
    # shr ecx, 4
    # shr ecx, 1  (total: shr 5)
    # xor ecx, 0xa1fc1b6f
    # push ecx  (this could be the ADD value for second call?)
    # push edi  (this could be the size?)
    # mov edi, 0x7fe13f6d
    # sub [esp+4], edi  -> modifies the pushed edi value: size = edi - 0x7fe13f6d
    # ...then adds 0x7fe13f6d to eax

    # The second stage code might use different XOR/ADD values for the rest of ehleidof
    # Let's try some educated guesses based on the constants we see:
    alt_params = [
        (0x6cfd7e36, 0x00000000, "XOR=0x6cfd7e36, ADD=0"),
        (0x6cfd7e36, 0x3640736d, "XOR=0x6cfd7e36, ADD=0x3640736d (original)"),
        (0x3640736d, 0x6cfd7e36, "XOR=0x3640736d, ADD=0x6cfd7e36 (swapped)"),
        (0xa1fc1b6f, 0x7fe13f6d, "XOR=0xa1fc1b6f, ADD=0x7fe13f6d (stage2 consts)"),
        (0x7fe13f6d, 0xa1fc1b6f, "XOR=0x7fe13f6d, ADD=0xa1fc1b6f (stage2 swapped)"),
        (0xce26c3b6, 0x00000000, "XOR=0xce26c3b6, ADD=0"),
        (0xce26c3b6, 0x3640736d, "XOR=0xce26c3b6, ADD=0x3640736d"),
    ]

    for xk, av, label in alt_params:
        test = decrypt_dwords(ehleidof, xk, av, offset=0, count=0x2000 // 4)
        pats = check_known_patterns(test[:0x2000])
        strs = find_strings(test[:0x2000], min_len=6)
        score = count_valid_x86(test[:0x2000], max_check=8192)
        ent = entropy(test[:0x2000])
        if pats or len(strs) > 5 or score > 20:
            print(f"  {label}")
            print(f"    patterns: {len(pats)}, strings: {len(strs)}, x86_score: {score}, entropy: {ent:.4f}")
            if pats:
                for p, c, pos in pats:
                    print(f"    pattern '{p}' x{c}")
            if strs:
                for o, s in strs[:10]:
                    print(f"    string at 0x{o:06x}: {s}")

    # ========================================================================
    # STAGE 11: Full strings dump from all decrypted variants
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 11: Saving all decrypted variants and string dumps")
    print("="*80)

    for name, data in [("raw", ehleidof), ("xor_add", stage3), ("sub_xor", stage3b),
                       ("xor_only", stage2)]:
        strs = find_strings(data, min_len=6)
        wstrs = find_wide_strings(data, min_len=5)
        with open(os.path.join(OUTPUT_DIR, f"strings_{name}.txt"), "w") as f:
            f.write(f"=== ASCII strings (min 6 chars) from {name} ===\n")
            for offset, s in strs:
                f.write(f"0x{offset:06x}: {s}\n")
            f.write(f"\n=== Wide strings (min 5 chars) from {name} ===\n")
            for offset, s in wstrs:
                f.write(f"0x{offset:06x}: {s}\n")
        print(f"  {name}: {len(strs)} ASCII + {len(wstrs)} wide strings -> strings_{name}.txt")

    # ========================================================================
    # STAGE 12: Try to find if ehleidof contains a compressed/packed PE
    # Look for MZ/PE signatures after various transforms
    # ========================================================================
    print("\n" + "="*80)
    print("STAGE 12: Search for MZ/PE signatures in all variants")
    print("="*80)

    mz_sig = b"MZ"
    pe_sig = b"PE\x00\x00"

    for name, data in [("raw", ehleidof), ("xor_add", stage3), ("sub_xor", stage3b),
                       ("xor_only", stage2)]:
        mz_pos = []
        pe_pos = []
        idx = 0
        while True:
            idx = data.find(mz_sig, idx)
            if idx == -1:
                break
            # Check if next bytes look PE-ish
            if idx + 64 < len(data):
                e_lfanew = struct.unpack_from('<I', data, idx + 0x3c)[0] if idx + 0x40 <= len(data) else 0
                if e_lfanew < 0x400 and idx + e_lfanew + 4 <= len(data):
                    check = data[idx + e_lfanew:idx + e_lfanew + 4]
                    if check == pe_sig:
                        mz_pos.append((idx, e_lfanew, True))
                    else:
                        mz_pos.append((idx, e_lfanew, False))
                else:
                    mz_pos.append((idx, e_lfanew, False))
            idx += 1
        if mz_pos:
            valid = [p for p in mz_pos if p[2]]
            print(f"  {name}: {len(mz_pos)} MZ signatures, {len(valid)} with valid PE header")
            for pos, elf, valid in mz_pos[:5]:
                status = "VALID PE" if valid else "no PE"
                print(f"    MZ at 0x{pos:06x}, e_lfanew=0x{elf:04x} ({status})")

    # ========================================================================
    # Summary
    # ========================================================================
    print("\n" + "="*80)
    print("SUMMARY")
    print("="*80)
    print(f"""
Files saved to {OUTPUT_DIR}/:
  ehleidof_raw.bin           - Raw encrypted section
  ehleidof_stage1.bin        - First 0x1000 decrypted (XOR+ADD at offset 1)
  ehleidof_stage1b_sub_xor.bin - First 0x1000 with reverse operation
  ehleidof_xor_only.bin      - XOR-only on entire section
  ehleidof_full_xor_add.bin  - Full XOR+ADD on entire section
  ehleidof_full_sub_xor.bin  - Full SUB+XOR on entire section
  strings_*.txt              - String dumps from each variant

Key findings:
  Entry point: 0x102c6000 (wovitjaj section = packer stub)
  Anti-debug: checks for int3 at 0x102c600a
  Stage 1 decrypt: ehleidof+1, size=0x1000, XOR=0x6cfd7e36, ADD=0x3640736d
  After stage 1: jumps to decrypted code at ehleidof+1 (0x1019a001)
  Stage 2: the decrypted 0x1000 bytes contain a second-stage decryptor
           that likely decrypts the remaining ~1.2MB of ehleidof.

Next steps:
  1. Load ehleidof_stage1.bin in radare2 to disassemble the second-stage code
  2. The decrypted first 0x1000 bytes should reveal the algorithm for the rest
  3. Consider using Wine + OllyDbg to run the DLL and dump unpacked memory
  4. Try x32dbg under Wine to break at the second-stage decrypt
""")


if __name__ == "__main__":
    main()
