#!/usr/bin/env python3
"""
find_iv_xor.py - Search PS3/x86 binaries for the 8-byte XOR operation on IV base values.

The PS3 Remote Play registration handler XORs an 8-byte "context" value into
the IV base before AES-CBC encryption. This script finds the IV base constants
in the binary and examines nearby code for XOR instructions to reveal what
value is being XORed in.

Known IV bases (big-endian):
  PSP:   3A 9D F3 3B 99 CF 9C 0D
  Phone: 29 0D E9 07 E2 3B E2 FC
  PC:    5B 64 40 C5 2E 74 C0 46

Usage:
  ./find_iv_xor.py <binary_file> [--arch ppc|x86] [--verbose]

Requires: Python 3.6+
"""

import argparse
import struct
import sys
import os

# Known IV base values (as bytes, big-endian)
IV_BASES = {
    "PSP":   bytes.fromhex("3a9df33b99cf9c0d"),
    "Phone": bytes.fromhex("290de907e23be2fc"),
    "PC":    bytes.fromhex("5b6440c52e74c046"),
}

# Known registration XOR keys
REG_XOR_KEYS = {
    "PSP":   bytes.fromhex("6da73e0e536cff92"),
    "Phone": bytes.fromhex("73e8dba2b0bb2d0c"),
    "PC":    bytes.fromhex("2d6b67d7c1e069f0"),
}

# Known session keys
SESSION_KEYS = {
    "SKEY0": bytes.fromhex("c99dacf2e9714b3f"),
    "SKEY2": bytes.fromhex("0d2a2a2f2e2c2d26"),
}

# PPC XOR opcodes (extended opcode 316 = 0x13C for xor, within opcode 31)
# Format: rrrrr_sssss_xxxxx_0100111100_0  (bits 21-30 = 316)
PPC_XOR_EXTENDED = 316

# x86 XOR opcodes
X86_XOR_OPCODES = {
    0x30: "xor r/m8, r8",
    0x31: "xor r/m32, r32",
    0x32: "xor r8, r/m8",
    0x33: "xor r32, r/m32",
    0x34: "xor al, imm8",
    0x35: "xor eax, imm32",
}


def find_all_occurrences(data: bytes, pattern: bytes) -> list:
    """Find all offsets of pattern in data."""
    offsets = []
    start = 0
    while True:
        idx = data.find(pattern, start)
        if idx == -1:
            break
        offsets.append(idx)
        start = idx + 1
    return offsets


def analyze_ppc_xor_nearby(data: bytes, offset: int, window: int = 256, verbose: bool = False) -> list:
    """
    Look for PPC XOR instructions near the given offset.
    PPC instructions are 4 bytes, aligned.
    XOR instruction: opcode 31 (bits 0-5), extended opcode 316 (bits 21-30).
    """
    results = []
    start = max(0, offset - window)
    # Align to 4 bytes
    start = start & ~3
    end = min(len(data), offset + window)

    for i in range(start, end - 3, 4):
        insn = struct.unpack(">I", data[i:i+4])[0]
        primary_opcode = (insn >> 26) & 0x3F
        extended_opcode = (insn >> 1) & 0x3FF

        if primary_opcode == 31 and extended_opcode == PPC_XOR_EXTENDED:
            rA = (insn >> 16) & 0x1F
            rS = (insn >> 21) & 0x1F
            rB = (insn >> 11) & 0x1F
            rc = insn & 1
            result = {
                "offset": i,
                "insn_hex": data[i:i+4].hex(),
                "disasm": f"xor{'.' if rc else ''} r{rA}, r{rS}, r{rB}",
                "distance": i - offset,
            }
            results.append(result)
            if verbose:
                print(f"  PPC XOR at 0x{i:08X}: {result['disasm']} (insn: {result['insn_hex']}, "
                      f"distance: {result['distance']:+d} from IV base)")

    return results


def analyze_x86_xor_nearby(data: bytes, offset: int, window: int = 512, verbose: bool = False) -> list:
    """
    Look for x86 XOR instructions near the given offset.
    This is a simplified scanner - x86 is variable length.
    """
    results = []
    start = max(0, offset - window)
    end = min(len(data), offset + window)

    i = start
    while i < end:
        byte = data[i]

        # Check for 0x81 /6 (xor r/m32, imm32) - REX prefix aware
        if byte == 0x81 and i + 1 < end:
            modrm = data[i + 1]
            reg_field = (modrm >> 3) & 7
            if reg_field == 6:  # /6 = XOR
                rm = modrm & 7
                mod = (modrm >> 6) & 3
                result = {
                    "offset": i,
                    "insn_hex": data[i:i+6].hex() if i + 6 <= end else data[i:end].hex(),
                    "disasm": f"xor r/m32, imm32 (modrm=0x{modrm:02x})",
                    "distance": i - offset,
                }
                if mod == 3 and i + 6 <= end:
                    imm = struct.unpack("<I", data[i+2:i+6])[0]
                    result["immediate"] = f"0x{imm:08X}"
                    result["disasm"] = f"xor reg{rm}, 0x{imm:08X}"
                results.append(result)
                if verbose:
                    print(f"  x86 XOR at 0x{i:08X}: {result['disasm']} "
                          f"(distance: {result['distance']:+d} from IV base)")

        # Check for simple XOR opcodes
        elif byte in X86_XOR_OPCODES:
            desc = X86_XOR_OPCODES[byte]
            insn_bytes = data[i:min(i+8, end)]
            result = {
                "offset": i,
                "insn_hex": insn_bytes.hex(),
                "disasm": desc,
                "distance": i - offset,
            }

            # For xor eax, imm32 (opcode 0x35)
            if byte == 0x35 and i + 5 <= end:
                imm = struct.unpack("<I", data[i+1:i+5])[0]
                result["immediate"] = f"0x{imm:08X}"
                result["disasm"] = f"xor eax, 0x{imm:08X}"

            results.append(result)
            if verbose:
                print(f"  x86 XOR at 0x{i:08X}: {result['disasm']} "
                      f"(distance: {result['distance']:+d} from IV base)")

        i += 1

    return results


def search_for_load_sequences(data: bytes, offset: int, arch: str, verbose: bool = False) -> list:
    """
    Look for instruction sequences that load the 8-byte value to XOR with IV.
    On PPC, this would be lis+ori or lis+addi pairs to build a 32-bit constant,
    then two such pairs for the full 64-bit value.
    """
    results = []
    if arch == "ppc":
        start = max(0, offset - 256)
        start = start & ~3
        end = min(len(data), offset + 256)

        for i in range(start, end - 3, 4):
            insn = struct.unpack(">I", data[i:i+4])[0]
            primary = (insn >> 26) & 0x3F

            # lis (addis with rA=0) - opcode 15
            if primary == 15:
                rD = (insn >> 21) & 0x1F
                rA = (insn >> 16) & 0x1F
                imm = insn & 0xFFFF
                if rA == 0:  # lis
                    # Look for ori in next few instructions
                    for j in range(i + 4, min(i + 20, end - 3), 4):
                        insn2 = struct.unpack(">I", data[j:j+4])[0]
                        primary2 = (insn2 >> 26) & 0x3F
                        if primary2 == 24:  # ori
                            rS2 = (insn2 >> 21) & 0x1F
                            rA2 = (insn2 >> 16) & 0x1F
                            imm2 = insn2 & 0xFFFF
                            if rS2 == rD:
                                val32 = (imm << 16) | imm2
                                result = {
                                    "offset": i,
                                    "type": "lis+ori",
                                    "register": f"r{rD}",
                                    "value": f"0x{val32:08X}",
                                    "distance": i - offset,
                                }
                                results.append(result)
                                if verbose:
                                    print(f"  PPC lis+ori at 0x{i:08X}: r{rD} = 0x{val32:08X} "
                                          f"(distance: {result['distance']:+d})")
    return results


def main():
    parser = argparse.ArgumentParser(
        description="Search binary files for the 8-byte XOR operation on IV base values",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s sysconf_plugin.elf --arch ppc
  %(prog)s RemotePlay.dll --arch x86 --verbose
  %(prog)s unknown_binary.bin
        """
    )
    parser.add_argument("binary", help="Path to the binary file to analyze")
    parser.add_argument("--arch", choices=["ppc", "x86"], default=None,
                        help="Architecture (auto-detected if not specified)")
    parser.add_argument("--verbose", "-v", action="store_true",
                        help="Show detailed output for each finding")
    parser.add_argument("--window", "-w", type=int, default=512,
                        help="Byte window around IV base to search for XORs (default: 512)")
    args = parser.parse_args()

    if not os.path.isfile(args.binary):
        print(f"ERROR: File not found: {args.binary}", file=sys.stderr)
        sys.exit(1)

    with open(args.binary, "rb") as f:
        data = f.read()

    print(f"=== IV XOR Finder: {args.binary} ===")
    print(f"File size: {len(data):,} bytes ({len(data)/1024:.1f} KB)")

    # Auto-detect architecture
    arch = args.arch
    if arch is None:
        if data[:4] == b"\x7fELF":
            # ELF - check e_machine
            if len(data) > 20:
                e_machine = struct.unpack(">H", data[18:20])[0]
                if e_machine == 21:  # EM_PPC64
                    arch = "ppc"
                    print("Auto-detected: PPC64 ELF (PS3)")
                elif e_machine == 20:  # EM_PPC
                    arch = "ppc"
                    print("Auto-detected: PPC ELF")
                elif e_machine in (3, 62):  # EM_386, EM_X86_64
                    arch = "x86"
                    print("Auto-detected: x86 ELF")
        elif data[:2] == b"MZ":
            arch = "x86"
            print("Auto-detected: PE/DLL (x86)")
        else:
            print("WARNING: Could not auto-detect architecture, trying both")
    else:
        print(f"Architecture: {arch}")
    print()

    # Search for IV bases
    found_any = False
    all_xor_results = []

    print("--- Searching for IV base constants ---")
    for name, iv_bytes in IV_BASES.items():
        # Search big-endian
        offsets = find_all_occurrences(data, iv_bytes)
        # Also search little-endian (reversed)
        iv_le = iv_bytes[::-1]
        offsets_le = find_all_occurrences(data, iv_le)

        if offsets:
            found_any = True
            for off in offsets:
                print(f"\n[FOUND] IV_BASE_{name} (BE) at offset 0x{off:08X}")
                # Show hex context
                ctx_start = max(0, off - 16)
                ctx_end = min(len(data), off + 24)
                print(f"  Context: ...{data[ctx_start:off].hex()} [{data[off:off+8].hex()}] {data[off+8:ctx_end].hex()}...")

                # Search for XOR instructions
                if arch in (None, "ppc"):
                    print(f"\n  Scanning for PPC XOR instructions (window={args.window}):")
                    xors = analyze_ppc_xor_nearby(data, off, args.window, args.verbose)
                    if xors:
                        all_xor_results.extend(xors)
                        for x in xors:
                            print(f"    0x{x['offset']:08X}: {x['disasm']} ({x['insn_hex']}) "
                                  f"[{x['distance']:+d} bytes from IV]")
                    else:
                        print("    (no PPC XOR found)")

                    # Look for lis+ori load sequences
                    print(f"\n  Scanning for PPC constant-loading sequences:")
                    loads = search_for_load_sequences(data, off, "ppc", args.verbose)
                    if loads:
                        for ld in loads:
                            print(f"    0x{ld['offset']:08X}: {ld['type']} {ld['register']} = {ld['value']} "
                                  f"[{ld['distance']:+d} bytes from IV]")
                    else:
                        print("    (no lis+ori sequences found)")

                if arch in (None, "x86"):
                    print(f"\n  Scanning for x86 XOR instructions (window={args.window}):")
                    xors = analyze_x86_xor_nearby(data, off, args.window, args.verbose)
                    if xors:
                        all_xor_results.extend(xors)
                        for x in xors:
                            line = f"    0x{x['offset']:08X}: {x['disasm']}"
                            if "immediate" in x:
                                line += f" (imm={x['immediate']})"
                            line += f" [{x['distance']:+d} bytes from IV]"
                            print(line)
                    else:
                        print("    (no x86 XOR found)")

        if offsets_le:
            found_any = True
            for off in offsets_le:
                print(f"\n[FOUND] IV_BASE_{name} (LE) at offset 0x{off:08X}")
                ctx_start = max(0, off - 16)
                ctx_end = min(len(data), off + 24)
                print(f"  Context: ...{data[ctx_start:off].hex()} [{data[off:off+8].hex()}] {data[off+8:ctx_end].hex()}...")

        if not offsets and not offsets_le:
            print(f"  IV_BASE_{name}: not found")

    # Search for registration XOR keys
    print("\n--- Searching for registration XOR keys ---")
    for name, key_bytes in REG_XOR_KEYS.items():
        offsets = find_all_occurrences(data, key_bytes)
        offsets_le = find_all_occurrences(data, key_bytes[::-1])
        if offsets:
            for off in offsets:
                print(f"  REG_XOR_{name} (BE) at offset 0x{off:08X}")
        if offsets_le:
            for off in offsets_le:
                print(f"  REG_XOR_{name} (LE) at offset 0x{off:08X}")
        if not offsets and not offsets_le:
            print(f"  REG_XOR_{name}: not found")

    # Search for session keys
    print("\n--- Searching for session keys ---")
    for name, key_bytes in SESSION_KEYS.items():
        offsets = find_all_occurrences(data, key_bytes)
        if offsets:
            for off in offsets:
                print(f"  {name} at offset 0x{off:08X}")
        else:
            print(f"  {name}: not found")

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    if not found_any:
        print("No IV base constants found in this binary.")
        print("This binary may not contain the registration handler.")
    else:
        print(f"Found {len(all_xor_results)} XOR instructions near IV bases")
        if all_xor_results:
            print("\nXOR instructions that could reveal the context value:")
            for x in all_xor_results:
                line = f"  0x{x['offset']:08X}: {x['disasm']}"
                if "immediate" in x:
                    line += f" -> IMMEDIATE VALUE: {x['immediate']}"
                print(line)
            print("\nNOTE: The 8-byte 'context' value XORed into IV is the sole remaining")
            print("blocker for registration. Check the registers/immediates above.")
        else:
            print("\nIV bases found but no nearby XOR instructions detected.")
            print("The XOR may use indirect addressing or be further away.")
            print("Try increasing --window or using radare2 for full disassembly.")


if __name__ == "__main__":
    main()
