#!/usr/bin/env python3
"""
xregistry_tool.py — Parse, read, and write PS3 xRegistry.sys files

The xRegistry.sys is a binary registry file used by PS3 to store settings.
Format: header magic BC AD AD BC, fixed size 128KB (131072 bytes).

Usage:
  ./xregistry_tool.py <xregistry.sys>                    List all entries
  ./xregistry_tool.py <xregistry.sys> --list              List all entries
  ./xregistry_tool.py <xregistry.sys> --read <path>       Read a specific key
  ./xregistry_tool.py <xregistry.sys> --search <term>     Search for paths
  ./xregistry_tool.py <xregistry.sys> --premo             Show all premo entries
  ./xregistry_tool.py <xregistry.sys> --inject <path> <hex_value> -o <output>
                                                          Write a value
  ./xregistry_tool.py <xregistry.sys> --dump-raw          Hex dump of raw structure

The premo registration key path is: /setting/premo/psp01/key
"""

import os
import sys
import struct
import argparse
from typing import Optional

XREG_MAGIC = b'\xBC\xAD\xAD\xBC'
XREG_SIZE = 131072  # 128KB

# Entry types (from reverse engineering)
ENTRY_TYPE_DIR = 1       # Directory node
ENTRY_TYPE_INT = 2       # Integer value (4 bytes)
ENTRY_TYPE_BIN = 3       # Binary blob


class XRegistryEntry:
    """Represents a single xRegistry entry."""
    def __init__(self, offset: int, entry_type: int, path: str,
                 data_offset: int = 0, data_size: int = 0, data: bytes = b''):
        self.offset = offset
        self.entry_type = entry_type
        self.path = path
        self.data_offset = data_offset
        self.data_size = data_size
        self.data = data

    def type_name(self) -> str:
        return {1: 'DIR', 2: 'INT', 3: 'BIN'}.get(self.entry_type, f'UNK({self.entry_type})')

    def value_str(self) -> str:
        if self.entry_type == ENTRY_TYPE_INT and len(self.data) >= 4:
            val = struct.unpack('>I', self.data[:4])[0]
            return f'{val} (0x{val:08x})'
        elif self.entry_type == ENTRY_TYPE_BIN and self.data:
            hex_str = self.data.hex()
            if len(hex_str) > 64:
                hex_str = hex_str[:64] + '...'
            # Also try ASCII
            try:
                ascii_str = self.data.decode('ascii').rstrip('\x00')
                if ascii_str.isprintable() and len(ascii_str) > 2:
                    return f'{hex_str} (ascii: "{ascii_str}")'
            except (UnicodeDecodeError, ValueError):
                pass
            return hex_str
        elif self.entry_type == ENTRY_TYPE_DIR:
            return '<directory>'
        return '<empty>'


class XRegistryParser:
    """Parse PS3 xRegistry.sys binary format."""

    def __init__(self, filepath: str):
        self.filepath = filepath
        with open(filepath, 'rb') as f:
            self.data = bytearray(f.read())
        self.entries = []
        self._parse()

    def _parse(self):
        """Parse the xRegistry structure."""
        data = self.data

        # Verify magic
        if data[:4] != XREG_MAGIC:
            # Try scanning for the magic
            pos = data.find(XREG_MAGIC)
            if pos == -1:
                print(f"WARNING: xRegistry magic {XREG_MAGIC.hex()} not found at offset 0",
                      file=sys.stderr)
                # Fall back to string scanning
                self._parse_by_paths()
                return
            else:
                print(f"NOTE: Magic found at offset 0x{pos:x} (expected 0x0)", file=sys.stderr)

        # Parse header
        # Typical header: magic(4) + version(2) + entry_count(2) + data_offset(4) + ...
        # The exact format varies; we'll try the common layout
        if len(data) < 16:
            print("ERROR: File too small", file=sys.stderr)
            return

        # Try to read entry table
        # Format hypothesis: after header, entries are sequential with:
        #   [2B entry_id] [2B parent_id] [1B type] [path_string\0] [4B data_offset] [4B data_size]
        # Alternative: entries contain inline data

        # Let's try multiple parsing strategies

        # Strategy 1: Find all paths and extract their context
        self._parse_by_paths()

    def _parse_by_paths(self):
        """Parse by finding all /setting/ paths in the file."""
        data = self.data
        pos = 0

        while pos < len(data):
            # Find next path-like string
            idx = data.find(b'/setting/', pos)
            if idx == -1:
                idx = data.find(b'/setting2/', pos)
            if idx == -1:
                break

            # Read the full path (null-terminated)
            end = data.find(b'\x00', idx)
            if end == -1 or end - idx > 256:
                pos = idx + 1
                continue

            path = data[idx:end].decode('ascii', errors='replace')

            # Look at bytes before the path for entry metadata
            # Typical: a few header bytes before the path string
            entry_header_start = max(0, idx - 8)
            header_bytes = data[entry_header_start:idx]

            # Try to determine entry type from header
            entry_type = 0
            data_offset = 0
            data_size = 0
            entry_data = b''

            # The byte immediately before the path often indicates type
            if idx > 0:
                type_byte = data[idx - 1]
                if type_byte in (1, 2, 3):
                    entry_type = type_byte

            # Look for data after the null terminator
            after_null = end + 1
            if after_null < len(data):
                # For INT types, next 4 bytes might be the value
                if entry_type == ENTRY_TYPE_INT and after_null + 4 <= len(data):
                    entry_data = bytes(data[after_null:after_null + 4])
                    data_size = 4
                # For BIN types, there might be a size prefix
                elif entry_type == ENTRY_TYPE_BIN and after_null + 4 <= len(data):
                    potential_size = struct.unpack('>I', data[after_null:after_null+4])[0]
                    if 0 < potential_size < 1024:
                        data_offset = after_null + 4
                        data_size = potential_size
                        if data_offset + data_size <= len(data):
                            entry_data = bytes(data[data_offset:data_offset + data_size])

                # If no type detected, grab some context bytes
                if not entry_data and after_null + 32 <= len(data):
                    entry_data = bytes(data[after_null:after_null + 32])
                    data_size = 32

            entry = XRegistryEntry(
                offset=idx,
                entry_type=entry_type,
                path=path,
                data_offset=data_offset,
                data_size=data_size,
                data=entry_data,
            )
            self.entries.append(entry)

            pos = end + 1

    def list_entries(self, filter_path: Optional[str] = None) -> list:
        """List all entries, optionally filtered by path prefix."""
        if filter_path:
            return [e for e in self.entries if filter_path in e.path]
        return self.entries

    def read_entry(self, path: str) -> Optional[XRegistryEntry]:
        """Read a specific entry by exact path."""
        for e in self.entries:
            if e.path == path:
                return e
        return None

    def search_entries(self, term: str) -> list:
        """Search entries by path substring."""
        term_lower = term.lower()
        return [e for e in self.entries if term_lower in e.path.lower()]

    def inject_value(self, path: str, value_hex: str, output_path: str) -> bool:
        """Inject a value at a given path and write to output file."""
        value_bytes = bytes.fromhex(value_hex.replace(' ', ''))

        entry = self.read_entry(path)
        if entry is None:
            print(f"ERROR: Path not found: {path}", file=sys.stderr)
            print("Available premo paths:", file=sys.stderr)
            for e in self.search_entries('premo'):
                print(f"  {e.path}", file=sys.stderr)
            return False

        # Find where to write the data
        # The data location depends on the entry format
        # For now, we'll write at the data_offset if known,
        # or right after the null terminator of the path
        write_offset = entry.data_offset if entry.data_offset else (
            entry.offset + len(entry.path.encode('ascii')) + 1
        )

        if write_offset + len(value_bytes) > len(self.data):
            print(f"ERROR: Write would exceed file bounds", file=sys.stderr)
            return False

        # Create modified copy
        modified = bytearray(self.data)
        modified[write_offset:write_offset + len(value_bytes)] = value_bytes

        with open(output_path, 'wb') as f:
            f.write(modified)

        print(f"Injected {len(value_bytes)} bytes at offset 0x{write_offset:x}")
        print(f"Path: {path}")
        print(f"Value: {value_hex}")
        print(f"Written to: {output_path}")
        return True

    def dump_raw(self, around_path: Optional[str] = None):
        """Hex dump of raw structure around entries."""
        if around_path:
            entries = self.search_entries(around_path)
        else:
            entries = self.entries[:20]  # First 20

        for entry in entries:
            start = max(0, entry.offset - 16)
            end = min(len(self.data), entry.offset + len(entry.path) + 48)
            print(f"\n--- {entry.path} (offset 0x{entry.offset:x}) ---")
            for i in range(start, end, 16):
                chunk = self.data[i:min(i+16, end)]
                hex_part = ' '.join(f'{b:02x}' for b in chunk)
                ascii_part = ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk)
                marker = ' <<' if entry.offset <= i < entry.offset + len(entry.path) else ''
                print(f"  {i:06x}: {hex_part:<48} {ascii_part}{marker}")


def main():
    parser = argparse.ArgumentParser(
        description='PS3 xRegistry.sys parser and editor',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s xRegistry.sys --list
  %(prog)s xRegistry.sys --premo
  %(prog)s xRegistry.sys --read /setting/premo/psp01/key
  %(prog)s xRegistry.sys --search premo
  %(prog)s xRegistry.sys --inject /setting/premo/psp01/key AABB...  -o modified.sys
  %(prog)s xRegistry.sys --dump-raw
""")
    parser.add_argument('xregistry', help='Path to xRegistry.sys file')
    parser.add_argument('--list', action='store_true', help='List all entries')
    parser.add_argument('--read', metavar='PATH', help='Read a specific key path')
    parser.add_argument('--search', metavar='TERM', help='Search for paths containing term')
    parser.add_argument('--premo', action='store_true', help='Show all premo (Remote Play) entries')
    parser.add_argument('--inject', nargs=2, metavar=('PATH', 'HEX_VALUE'),
                        help='Inject a hex value at a path')
    parser.add_argument('-o', '--output', help='Output file for inject operation')
    parser.add_argument('--dump-raw', action='store_true', help='Hex dump of raw structure')
    parser.add_argument('--dump-around', metavar='TERM', help='Hex dump around matching paths')

    args = parser.parse_args()

    if not os.path.isfile(args.xregistry):
        print(f"ERROR: File not found: {args.xregistry}", file=sys.stderr)
        sys.exit(1)

    xreg = XRegistryParser(args.xregistry)

    file_size = os.path.getsize(args.xregistry)
    print(f"xRegistry: {args.xregistry}")
    print(f"Size: {file_size} bytes ({file_size // 1024}KB)")
    print(f"Expected: {XREG_SIZE} bytes ({XREG_SIZE // 1024}KB)")
    has_magic = xreg.data[:4] == XREG_MAGIC
    print(f"Magic: {'OK' if has_magic else 'NOT FOUND'} ({xreg.data[:4].hex()})")
    print(f"Entries found: {len(xreg.entries)}")
    print()

    if args.read:
        entry = xreg.read_entry(args.read)
        if entry:
            print(f"Path: {entry.path}")
            print(f"Type: {entry.type_name()}")
            print(f"Offset: 0x{entry.offset:x}")
            print(f"Data size: {entry.data_size}")
            print(f"Value: {entry.value_str()}")
            if entry.data:
                print(f"Raw hex: {entry.data.hex()}")
        else:
            print(f"Path not found: {args.read}")
            # Show similar paths
            similar = xreg.search_entries(args.read.split('/')[-1])
            if similar:
                print("Similar paths:")
                for e in similar:
                    print(f"  {e.path}")
            sys.exit(1)

    elif args.search:
        results = xreg.search_entries(args.search)
        print(f"Search results for '{args.search}': {len(results)} entries\n")
        for e in results:
            print(f"  [{e.type_name():3s}] {e.path}")
            if e.data:
                print(f"        Value: {e.value_str()}")

    elif args.premo:
        results = xreg.search_entries('premo')
        print(f"Remote Play (premo) entries: {len(results)}\n")
        for e in results:
            print(f"  [{e.type_name():3s}] {e.path}")
            if e.data and e.entry_type != ENTRY_TYPE_DIR:
                print(f"        Offset: 0x{e.offset:x}")
                print(f"        Value: {e.value_str()}")
                print()

    elif args.inject:
        path, hex_value = args.inject
        output = args.output
        if not output:
            base, ext = os.path.splitext(args.xregistry)
            output = f"{base}_modified{ext}"
            print(f"No output specified, using: {output}")

        success = xreg.inject_value(path, hex_value, output)
        if not success:
            sys.exit(1)

    elif args.dump_raw or args.dump_around:
        xreg.dump_raw(args.dump_around)

    else:
        # Default: list all entries
        entries = xreg.list_entries()
        print(f"All entries ({len(entries)}):\n")
        for e in entries:
            val = e.value_str() if e.entry_type != ENTRY_TYPE_DIR else ''
            print(f"  0x{e.offset:05x} [{e.type_name():3s}] {e.path}")
            if val and val != '<directory>':
                print(f"          {val}")


if __name__ == '__main__':
    main()
