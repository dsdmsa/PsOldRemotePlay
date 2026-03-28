#!/usr/bin/env python3
"""
PS3 xRegistry.sys — Read premo entries and inject registration data.

Format: Each index entry = 2-byte ID (BE) + 2-byte name_len (BE) + 1-byte type + name_string
  type: 0=ACTIVE, 1=INACTIVE, 2=HIDDEN, 3=DIR

Data section at offset 0x10000: Each entry = 2-byte unk + 2-byte ref_id + 2-byte unk2 + 2-byte data_len + 1-byte data_type + data
  data_type: 0=BOOL, 1=INT, 2=STR/BINARY

Usage:
  python3 xregistry_inject.py read <xRegistry.sys>
  python3 xregistry_inject.py inject <xRegistry.sys> <output.sys> <pkey_hex> <mac_hex> <device_id_hex> <nickname>
"""
import struct, sys, os

def parse_index(data):
    """Parse index entries from area 1 (offset 0-0x10000)"""
    entries = []
    pos = 16  # skip magic
    while pos + 5 <= 0x10000:
        eid = struct.unpack_from('>H', data, pos)[0]
        nlen = struct.unpack_from('>H', data, pos + 2)[0]
        etype = data[pos + 4]

        if eid == 0xAABB and nlen == 0xCCDD:
            break
        if nlen == 0 or nlen > 500 or pos + 5 + nlen > 0x10000:
            break

        name = data[pos + 5: pos + 5 + nlen].decode('utf-8', errors='replace').rstrip('\x00')
        entries.append({
            'id': eid, 'name': name, 'type': etype,
            'idx_offset': pos, 'name_offset': pos + 5
        })
        pos += 5 + nlen

    return entries, pos  # pos = end of last entry

def parse_data(data):
    """Parse data entries from area 2 (offset 0x10000-0x20000)"""
    entries = {}
    pos = 0x10000
    while pos + 9 <= 0x20000:
        d_unk1 = struct.unpack_from('>H', data, pos)[0]
        d_ref = struct.unpack_from('>H', data, pos + 2)[0]
        d_unk2 = struct.unpack_from('>H', data, pos + 4)[0]
        d_len = struct.unpack_from('>H', data, pos + 6)[0]
        d_type = data[pos + 8]

        if d_unk1 == 0xAABB and d_ref == 0xCCDD:
            break
        if d_len > 4096 or pos + 9 + d_len > 0x20000:
            break

        entries[d_ref] = {
            'unk1': d_unk1, 'unk2': d_unk2,
            'type': d_type, 'length': d_len,
            'value': data[pos + 9: pos + 9 + d_len],
            'hdr_offset': pos, 'data_offset': pos + 9
        }
        pos += 9 + d_len

    return entries, pos

def read_registry(filepath):
    """Read and display premo registration entries"""
    data = open(filepath, 'rb').read()
    index, _ = parse_index(data)
    data_entries, _ = parse_data(data)

    print(f"xRegistry.sys: {len(data)} bytes, {len(index)} index entries, {len(data_entries)} data entries\n")

    for entry in index:
        name = entry['name']
        if '/setting/premo' not in name:
            continue

        eid = entry['id']
        etype = entry['type']
        type_str = {0: "val", 1: "inact", 2: "hide", 3: "DIR"}.get(etype, f"t{etype}")

        if eid in data_entries:
            d = data_entries[eid]
            val = d['value']
            field = name.split('/')[-1]

            if field == 'key' and len(val) > 0:
                display = val.hex().upper()
            elif field == 'macAddress' and len(val) >= 6:
                display = ':'.join(f'{b:02X}' for b in val[:6])
            elif field == 'id' and len(val) > 0:
                display = val.hex().upper()
            elif field == 'nickname':
                display = f'"{val.decode("utf-8", errors="replace").rstrip(chr(0))}"'
            elif field == 'keyType' and d['type'] == 1 and len(val) >= 4:
                display = str(struct.unpack_from('>I', val)[0])
            elif d['type'] == 1 and len(val) >= 4:
                display = str(struct.unpack_from('>I', val)[0])
            else:
                display = val.hex() if len(val) <= 32 else val[:32].hex() + "..."

            print(f"  [{type_str}] {name} = {display}")
            print(f"         id=0x{eid:04X}, data@0x{d['data_offset']:05X}, len={d['length']}")
        else:
            print(f"  [{type_str}] {name} (no data)")

def inject_registration(in_path, out_path, pkey_hex, mac_hex, devid_hex, nickname):
    """Inject registration data into psp01 slot"""
    data = bytearray(open(in_path, 'rb').read())
    index, _ = parse_index(data)
    data_entries, _ = parse_data(data)

    pkey = bytes.fromhex(pkey_hex)
    mac = bytes.fromhex(mac_hex)
    devid = bytes.fromhex(devid_hex)

    assert len(pkey) == 16, f"pkey must be 16 bytes, got {len(pkey)}"
    assert len(mac) == 6, f"MAC must be 6 bytes, got {len(mac)}"
    assert len(devid) == 16, f"device ID must be 16 bytes, got {len(devid)}"

    # Find psp01 entries
    modified = 0
    for entry in index:
        name = entry['name']
        eid = entry['id']

        if eid not in data_entries:
            continue

        d = data_entries[eid]
        field = name.split('/')[-1]
        slot = name.split('/')[-2] if '/' in name else ''

        if slot != 'psp01':
            continue

        if field == 'key' and d['length'] >= 16:
            offset = d['data_offset']
            data[offset:offset + 16] = pkey
            print(f"  Wrote pkey at 0x{offset:05X}: {pkey.hex().upper()}")
            modified += 1

        elif field == 'macAddress' and d['length'] >= 6:
            offset = d['data_offset']
            data[offset:offset + 6] = mac
            print(f"  Wrote MAC at 0x{offset:05X}: {':'.join(f'{b:02X}' for b in mac)}")
            modified += 1

        elif field == 'id' and d['length'] >= 16:
            offset = d['data_offset']
            data[offset:offset + 16] = devid
            print(f"  Wrote device ID at 0x{offset:05X}: {devid.hex().upper()}")
            modified += 1

        elif field == 'nickname':
            offset = d['data_offset']
            nick_bytes = nickname.encode('utf-8')[:d['length'] - 1] + b'\x00'
            nick_padded = nick_bytes.ljust(d['length'], b'\x00')
            data[offset:offset + d['length']] = nick_padded
            print(f"  Wrote nickname at 0x{offset:05X}: \"{nickname}\"")
            modified += 1

        elif field == 'keyType' and d['length'] >= 4:
            offset = d['data_offset']
            data[offset:offset + 4] = struct.pack('>I', 0)  # keyType = 0
            print(f"  Wrote keyType=0 at 0x{offset:05X}")
            modified += 1

    # Also activate the entries (change type from INACTIVE/HIDDEN to ACTIVE)
    for entry in index:
        name = entry['name']
        if '/setting/premo/psp01' in name and entry['type'] in [1, 2]:
            # Change type byte to 0 (ACTIVE)
            type_offset = entry['idx_offset'] + 4
            old_type = data[type_offset]
            data[type_offset] = 0x00  # ACTIVE
            print(f"  Activated {name} (was type {old_type})")
            modified += 1

    if modified == 0:
        print("ERROR: No psp01 entries found to modify!")
        return False

    open(out_path, 'wb').write(bytes(data))
    print(f"\nWrote {out_path} ({modified} modifications)")
    return True

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage:")
        print("  python3 xregistry_inject.py read <xRegistry.sys>")
        print("  python3 xregistry_inject.py inject <input.sys> <output.sys> <pkey_hex> <mac_hex> <devid_hex> <nickname>")
        print()
        print("Example:")
        print("  python3 xregistry_inject.py inject xRegistry.sys xRegistry_patched.sys \\")
        print("    A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6 \\")
        print("    AABBCCDDEEFF \\")
        print("    1122334455667788990011223344556677 \\")
        print("    PsOldRemotePlay")
        sys.exit(1)

    cmd = sys.argv[1]

    if cmd == 'read':
        read_registry(sys.argv[2])
    elif cmd == 'inject':
        if len(sys.argv) < 8:
            print("inject needs: <input> <output> <pkey> <mac> <devid> <nickname>")
            sys.exit(1)
        inject_registration(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7])
    else:
        print(f"Unknown command: {cmd}")
