#!/usr/bin/env python3
"""
Decrypt VRPSDK.dll packed sections using the XOR+ADD unpacker found in Ghidra decompilation.

From VRPSDK.dll.c:
  FUN_102c6046(unaff_retaddr + -0x12c00a, 0x1000, 0x6cfd7e36)

  for (param_2 = param_2 >> 2; param_2 != 0; param_2 = param_2 - 1):
      *param_1 = *param_1 ^ param_3
      *param_1 = *param_1 + param_4
      param_1 = param_1 + 1

This XORs each DWORD with 0x6cfd7e36, then adds param_4.
param_4 is not explicitly passed in the call - it may be 0 or from a register.
We'll try both XOR-only and XOR+ADD variants.
"""
import struct
import sys
import os

def decrypt_xor_only(data, key=0x6cfd7e36):
    """Decrypt by XORing each DWORD with key."""
    result = bytearray(data)
    for i in range(0, len(result) - 3, 4):
        val = struct.unpack_from('<I', result, i)[0]
        val ^= key
        struct.pack_into('<I', result, i, val & 0xFFFFFFFF)
    return bytes(result)

def decrypt_xor_add(data, key=0x6cfd7e36, add_val=0):
    """Decrypt by XORing each DWORD with key, then adding add_val."""
    result = bytearray(data)
    for i in range(0, len(result) - 3, 4):
        val = struct.unpack_from('<I', result, i)[0]
        val ^= key
        val = (val + add_val) & 0xFFFFFFFF
        struct.pack_into('<I', result, i, val & 0xFFFFFFFF)
    return bytes(result)

def find_pe_sections(data):
    """Parse PE headers to find sections."""
    if data[:2] != b'MZ':
        print("Not a PE file")
        return []

    e_lfanew = struct.unpack_from('<I', data, 0x3C)[0]
    if data[e_lfanew:e_lfanew+4] != b'PE\x00\x00':
        print("Invalid PE signature")
        return []

    num_sections = struct.unpack_from('<H', data, e_lfanew + 6)[0]
    opt_header_size = struct.unpack_from('<H', data, e_lfanew + 20)[0]
    section_offset = e_lfanew + 24 + opt_header_size

    sections = []
    for i in range(num_sections):
        off = section_offset + i * 40
        name = data[off:off+8].rstrip(b'\x00').decode('ascii', errors='replace')
        vsize = struct.unpack_from('<I', data, off + 8)[0]
        vaddr = struct.unpack_from('<I', data, off + 12)[0]
        raw_size = struct.unpack_from('<I', data, off + 16)[0]
        raw_ptr = struct.unpack_from('<I', data, off + 20)[0]
        chars = struct.unpack_from('<I', data, off + 36)[0]
        sections.append({
            'name': name, 'vsize': vsize, 'vaddr': vaddr,
            'raw_size': raw_size, 'raw_ptr': raw_ptr, 'chars': chars
        })
        print(f"  Section {name}: VA=0x{vaddr:08x} RawPtr=0x{raw_ptr:08x} RawSize=0x{raw_size:08x} Chars=0x{chars:08x}")

    return sections

def check_decryption(data, label):
    """Check if decrypted data looks valid (has strings, PE structures, etc.)."""
    # Count printable ASCII strings of length >= 4
    strings = []
    current = b''
    for b in data:
        if 0x20 <= b < 0x7f:
            current += bytes([b])
        else:
            if len(current) >= 6:
                strings.append(current.decode('ascii'))
            current = b''
    if len(current) >= 6:
        strings.append(current.decode('ascii'))

    # Check for interesting strings
    interesting = [s for s in strings if any(kw in s.lower() for kw in
        ['regist', 'premo', 'encrypt', 'aes', 'context', 'pin', 'key', 'sce/',
         'http', 'session', 'client', 'machine', 'bcrypt', 'dll', 'vrp'])]

    print(f"\n{label}:")
    print(f"  Total strings (>=6 chars): {len(strings)}")
    print(f"  Interesting strings: {len(interesting)}")
    if interesting[:30]:
        for s in interesting[:30]:
            print(f"    {s}")
    if len(interesting) > 30:
        print(f"    ... and {len(interesting)-30} more")

    return len(interesting)

def main():
    dll_path = sys.argv[1] if len(sys.argv) > 1 else \
        "/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/extracted/vrp_files/App/VRPSDK.dll"

    if not os.path.exists(dll_path):
        print(f"File not found: {dll_path}")
        sys.exit(1)

    with open(dll_path, 'rb') as f:
        data = f.read()

    print(f"File: {dll_path}")
    print(f"Size: {len(data)} bytes")
    print(f"\nPE Sections:")
    sections = find_pe_sections(data)

    # Try decrypting each code section
    out_dir = os.path.dirname(dll_path) or '.'

    for sect in sections:
        if sect['raw_size'] == 0:
            continue

        start = sect['raw_ptr']
        size = sect['raw_size']
        sect_data = data[start:start+size]

        # Try XOR-only decryption
        decrypted = decrypt_xor_only(sect_data)
        score = check_decryption(decrypted, f"Section {sect['name']} XOR-only (key=0x6cfd7e36)")

        if score > 5:
            out_path = os.path.join(out_dir, f"VRPSDK_{sect['name']}_decrypted.bin")
            with open(out_path, 'wb') as f:
                f.write(decrypted)
            print(f"  -> Saved to {out_path}")

            # Also extract all strings
            str_path = os.path.join(out_dir, f"VRPSDK_{sect['name']}_strings.txt")
            strings = []
            current = b''
            for b in decrypted:
                if 0x20 <= b < 0x7f:
                    current += bytes([b])
                else:
                    if len(current) >= 4:
                        strings.append(current.decode('ascii'))
                    current = b''
            with open(str_path, 'w') as f:
                f.write('\n'.join(strings))
            print(f"  -> Strings saved to {str_path}")

    # Also try decrypting the ENTIRE file (minus PE headers)
    print("\n--- Trying full file decryption (skip first 0x400 bytes) ---")
    full_dec = data[:0x400] + decrypt_xor_only(data[0x400:])
    check_decryption(full_dec, "Full file XOR (skip headers)")

    # Try with different starting points based on the packer code
    # The packer: FUN_102c6046(unaff_retaddr + -0x12c00a, 0x1000, 0x6cfd7e36)
    # 0x1000 >> 2 = 0x400 iterations = 0x1000 bytes decrypted
    # But the start address is relative to return address
    print("\n--- Trying section-by-section with original data check ---")
    check_decryption(data, "Original (no decryption)")

if __name__ == '__main__':
    main()
