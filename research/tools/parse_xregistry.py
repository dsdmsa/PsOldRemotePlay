#!/usr/bin/env python3
"""
PS3 xRegistry.sys Parser — Extract and inject Remote Play registration keys
"""
import struct, sys, os

def find_all(data, needle):
    """Find all occurrences of needle in data"""
    results = []
    start = 0
    while True:
        pos = data.find(needle, start)
        if pos == -1: break
        results.append(pos)
        start = pos + 1
    return results

def read_xreg_value(data, path_str):
    """Find a registry path and try to read its associated value"""
    path_bytes = path_str.encode('ascii')
    positions = find_all(data, path_bytes)
    results = []
    for pos in positions:
        # The value data is typically stored at an offset referenced before or after the path
        # xRegistry format: each entry has a header with offset, type, and path
        # Look at bytes before the path for metadata
        if pos >= 4:
            # Check 4 bytes before path start for potential offset/metadata
            pre = data[pos-4:pos]
            # After the path null terminator, look for value data
            null_pos = data.find(b'\x00', pos + len(path_bytes))
            if null_pos > 0 and null_pos < pos + len(path_bytes) + 2:
                after = data[null_pos+1:null_pos+33]
                results.append({
                    'path': path_str,
                    'offset': pos,
                    'pre_bytes': pre.hex(),
                    'after_bytes': after.hex() if after else '',
                    'after_printable': after.decode('ascii', errors='replace') if after else ''
                })
    return results

def find_data_section(data, path_str):
    """
    xRegistry entries reference data stored elsewhere.
    The format has entry headers that point to data offsets.
    Let's find the data area and extract values.
    """
    # The xRegistry has a header section with path entries and a data section
    # Data section typically starts after all path entries
    # Each path entry contains: some header bytes, type byte, path string
    # The data is stored at offsets referenced in the header
    pass

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 parse_xregistry.py <xRegistry.sys> [inject]")
        sys.exit(1)

    xreg_path = sys.argv[1]
    data = open(xreg_path, 'rb').read()
    print(f"xRegistry.sys: {len(data)} bytes")

    # Find all premo-related paths
    premo_paths = [
        "/setting/premo/remoteBoot",
        "/setting/premo/bootCount",
        "/setting/premo/audioConfig",
    ]

    # Find PSP slots
    for slot in range(1, 9):
        slot_name = f"psp{slot:02d}"
        base = f"/setting/premo/{slot_name}"
        for field in ['nickname', 'macAddress', 'id', 'keyType', 'key']:
            path = f"{base}/{field}"
            if path.encode('ascii') in data:
                premo_paths.append(path)

    print(f"\n=== Found {len(premo_paths)} premo registry entries ===\n")

    # For each path, find where it is and dump surrounding bytes
    for path in premo_paths:
        path_bytes = path.encode('ascii')
        pos = data.find(path_bytes)
        if pos == -1: continue

        # Read the entry header (bytes before the path)
        # xRegistry entry format varies, but typically:
        # [2-byte hash/id] [2-byte size] [1-byte type] [path string] [null]
        # The actual data is in a data section referenced by offset

        # Look at the 4 bytes right before the path
        entry_start = pos
        while entry_start > 0 and data[entry_start-1] != 0x00:
            entry_start -= 1
            if pos - entry_start > 20: break

        header = data[max(0,pos-6):pos]
        after_null = data.find(b'\x00', pos)

        # The type byte is typically 1 byte before the path or part of header
        # Type 01 = int, 02 = binary/string, 03 = directory
        if len(header) >= 2:
            type_byte = header[-1]
        else:
            type_byte = 0

        print(f"{path}")
        print(f"  Offset: 0x{pos:x}")
        print(f"  Header (6 bytes before): {header.hex()}")
        print(f"  Type byte: 0x{type_byte:02x} ({'int' if type_byte==1 else 'bin/str' if type_byte==2 else 'dir' if type_byte==3 else '?'})")

        # For data entries (type 1 or 2), the value is stored in a data section
        # The header contains an offset into the data section
        # Let's look at what appears to be offset bytes
        if len(header) >= 4:
            potential_offset = struct.unpack('>H', header[-3:-1])[0] if len(header) >= 3 else 0
            print(f"  Potential data offset: 0x{potential_offset:04x}")

        print()

    # Now let's try a different approach: look for the actual data values
    # The xRegistry data section is typically at the end of the file
    # Let's search for known patterns

    print("=== Searching for registration data values ===\n")

    # Search for any 16-byte sequences that look like keys (high entropy)
    # near the premo section
    premo_start = data.find(b'/setting/premo')
    if premo_start > 0:
        # Dump the entire premo section
        premo_end = data.find(b'/setting/', premo_start + 100)
        while premo_end > 0 and b'premo' in data[premo_end:premo_end+50]:
            premo_end = data.find(b'/setting/', premo_end + 10)

        if premo_end == -1:
            premo_end = min(premo_start + 2000, len(data))

        print(f"Premo section: offset 0x{premo_start:x} to 0x{premo_end:x} ({premo_end-premo_start} bytes)")

    # Let's also look for the data area that the entries point to
    # xRegistry has a header at offset 0 with total size and entry count
    print(f"\n=== xRegistry Header ===")
    print(f"First 32 bytes: {data[:32].hex()}")

    # Try to find the data section by looking for large blocks of non-path data
    # after all the path entries
    last_path = data.rfind(b'/setting/')
    if last_path > 0:
        null_after = data.find(b'\x00', last_path)
        data_section_start = null_after + 1 if null_after > 0 else last_path + 100
        print(f"\nLast /setting/ path at offset: 0x{last_path:x}")
        print(f"Estimated data section start: 0x{data_section_start:x}")
        print(f"Data section size: {len(data) - data_section_start} bytes")

        # Dump first 256 bytes of data section
        print(f"\nData section first 256 bytes:")
        for i in range(0, min(256, len(data) - data_section_start), 16):
            offset = data_section_start + i
            hex_str = ' '.join(f'{data[offset+j]:02x}' for j in range(min(16, len(data)-offset)))
            ascii_str = ''.join(chr(data[offset+j]) if 32 <= data[offset+j] < 127 else '.' for j in range(min(16, len(data)-offset)))
            print(f"  {offset:06x}: {hex_str:<48} {ascii_str}")

if __name__ == "__main__":
    main()
