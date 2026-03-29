#!/usr/bin/env python3
"""
binary_analyzer.py — Analyze binary files for crypto patterns and structure

Analyzes PE/ELF/SELF binaries: extracts strings (ASCII + wide), searches for
known crypto constant byte patterns, finds AES S-box, and locates function
prologues for x86/x64/PPC architectures.

Usage:
  ./binary_analyzer.py <file_or_dir>         Analyze file(s), print to stdout
  ./binary_analyzer.py <file_or_dir> -o out.json   Output to JSON

Options:
  -h, --help     Show this help
  -o, --output   Write results to JSON file
  -q, --quiet    Suppress progress output
  --strings-only Only extract strings
"""

import os
import sys
import json
import struct
import argparse
from pathlib import Path
from typing import Optional

BINARY_EXTENSIONS = {'.exe', '.dll', '.elf', '.self', '.bin', '.so', '.dylib',
                     '.prx', '.sprx', '.sys', '.eboot', '.o', '.ko', '.drv'}

# Known crypto constants (name -> bytes)
CRYPTO_CONSTANTS = {
    'REG_XOR_PSP':   bytes([0xFD, 0xC3, 0xF6, 0xA6, 0x4D, 0x2A, 0xBA, 0x7A,
                            0x38, 0x92, 0x6C, 0xBC, 0x34, 0x31, 0xE1, 0x0E]),
    'REG_XOR_PHONE': bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                            0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6]),
    'REG_XOR_PC':    bytes([0xEC, 0x6D, 0x70, 0x6B, 0x1E, 0x0A, 0x9A, 0x75,
                            0x8C, 0xDA, 0x78, 0x27, 0x51, 0xA3, 0xC3, 0x7B]),
    'REG_IV_PSP':    bytes([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D,
                            0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64]),
    'REG_IV_PHONE':  bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                            0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A]),
    'REG_IV_PC':     bytes([0x5B, 0x64, 0x40, 0xC5, 0x2E, 0x74, 0xC0, 0x46,
                            0x48, 0x72, 0xC9, 0xC5, 0x49, 0x0C, 0x79, 0x04]),
    'SKEY0':         bytes([0xD1, 0xB2, 0x12, 0xEB, 0x73, 0x86, 0x6C, 0x7B,
                            0x12, 0xA7, 0x5E, 0x0C, 0x04, 0xC6, 0xB8, 0x91]),
    'SKEY1':         bytes([0x1F, 0xD5, 0xB9, 0xFA, 0x71, 0xB8, 0x96, 0x81,
                            0xB2, 0x87, 0x92, 0xE2, 0x6F, 0x38, 0xC3, 0x6F]),
    'SKEY2':         bytes([0x65, 0x2D, 0x8C, 0x90, 0xDE, 0x87, 0x17, 0xCF,
                            0x4F, 0xB3, 0xD8, 0xD3, 0x01, 0x79, 0x6B, 0x59]),
    'NONCE_XOR_PHONE': bytes([0x37, 0x63, 0xE5, 0x4D, 0x12, 0xF9, 0x7B, 0x73,
                              0x62, 0x3A, 0xD3, 0x0D, 0x10, 0xC9, 0x91, 0x7E]),
    'NONCE_XOR_PC':    bytes([0x33, 0x72, 0x84, 0x4C, 0xA6, 0x6F, 0x2E, 0x2B,
                              0x20, 0xC7, 0x90, 0x60, 0x33, 0xE8, 0x29, 0xC6]),
    'NONCE_XOR_VITA':  bytes([0xAF, 0x74, 0xB5, 0x4F, 0x38, 0xF8, 0xAF, 0xC8,
                              0x75, 0x77, 0xB2, 0xD5, 0x47, 0x76, 0x3B, 0xFD]),
}

# AES S-box first 16 bytes
AES_SBOX_PREFIX = bytes([0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5,
                         0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76])

# Function prologues
PROLOGUES = {
    'x86_push_ebp':    bytes([0x55, 0x8B, 0xEC]),
    'x86_push_rbp':    bytes([0x55, 0x48, 0x89, 0xE5]),
    'x64_sub_rsp':     bytes([0x48, 0x83, 0xEC]),
    'x64_mov_rbx':     bytes([0x48, 0x89, 0x5C, 0x24]),
    'ppc_mflr_r0':     bytes([0x7C, 0x08, 0x02, 0xA6]),
    'ppc_stw_r0':      bytes([0x90, 0x01, 0x00]),
    'arm_push_lr':     bytes([0x00, 0x48, 0x2D, 0xE9]),  # PUSH {LR} (ARM)
}

MIN_STRING_LEN = 6


def detect_format(data: bytes) -> dict:
    """Detect binary format from headers."""
    info = {'format': 'unknown', 'bits': 0, 'arch': 'unknown'}

    if len(data) < 4:
        return info

    # PE (MZ header)
    if data[:2] == b'MZ':
        info['format'] = 'PE'
        if len(data) > 0x3C + 4:
            pe_offset = struct.unpack_from('<I', data, 0x3C)[0]
            if pe_offset < len(data) - 6 and data[pe_offset:pe_offset+4] == b'PE\x00\x00':
                machine = struct.unpack_from('<H', data, pe_offset + 4)[0]
                info['arch'] = {0x14c: 'x86', 0x8664: 'x64', 0x1c0: 'ARM',
                                0xaa64: 'ARM64'}.get(machine, f'0x{machine:04x}')
                magic_offset = pe_offset + 24
                if magic_offset < len(data) - 2:
                    opt_magic = struct.unpack_from('<H', data, magic_offset)[0]
                    info['bits'] = 64 if opt_magic == 0x20b else 32

    # ELF
    elif data[:4] == b'\x7fELF':
        info['format'] = 'ELF'
        info['bits'] = 64 if data[4] == 2 else 32
        if len(data) > 18:
            e_machine = struct.unpack_from('<H', data, 18)[0]
            info['arch'] = {3: 'x86', 0x3E: 'x64', 0x14: 'PPC',
                            0x15: 'PPC64', 0x28: 'ARM', 0xB7: 'ARM64',
                            0x08: 'MIPS'}.get(e_machine, f'0x{e_machine:04x}')

    # SCE header (PS3 SELF/SPRX)
    elif data[:4] == b'SCE\x00' or data[:4] == b'\x00\x00\x00\x02':
        info['format'] = 'SCE/SELF'
        info['arch'] = 'PPC64'
        info['bits'] = 64

    return info


def extract_strings(data: bytes, min_len: int = MIN_STRING_LEN) -> list:
    """Extract ASCII and wide (UTF-16LE) strings."""
    strings = []

    # ASCII strings
    current = bytearray()
    start = 0
    for i, b in enumerate(data):
        if 32 <= b < 127:
            if not current:
                start = i
            current.append(b)
        else:
            if len(current) >= min_len:
                strings.append({
                    'offset': start,
                    'value': current.decode('ascii'),
                    'encoding': 'ascii',
                    'length': len(current)
                })
            current = bytearray()
    if len(current) >= min_len:
        strings.append({
            'offset': start,
            'value': current.decode('ascii'),
            'encoding': 'ascii',
            'length': len(current)
        })

    # Wide strings (UTF-16LE): printable char followed by 0x00
    current = bytearray()
    start = 0
    for i in range(0, len(data) - 1, 2):
        lo, hi = data[i], data[i+1]
        if hi == 0 and 32 <= lo < 127:
            if not current:
                start = i
            current.append(lo)
        else:
            if len(current) >= min_len:
                strings.append({
                    'offset': start,
                    'value': current.decode('ascii'),
                    'encoding': 'utf16le',
                    'length': len(current)
                })
            current = bytearray()

    return strings


def find_pattern(data: bytes, pattern: bytes) -> list:
    """Find all occurrences of a byte pattern in data."""
    results = []
    start = 0
    while True:
        pos = data.find(pattern, start)
        if pos == -1:
            break
        # Get context: 16 bytes before and after
        ctx_start = max(0, pos - 16)
        ctx_end = min(len(data), pos + len(pattern) + 16)
        context_hex = data[ctx_start:ctx_end].hex()
        results.append({
            'offset': pos,
            'offset_hex': f'0x{pos:x}',
            'context': context_hex,
        })
        start = pos + 1
    return results


def count_prologues(data: bytes) -> dict:
    """Count function prologues to estimate architecture usage."""
    counts = {}
    for name, pattern in PROLOGUES.items():
        count = 0
        start = 0
        while True:
            pos = data.find(pattern, start)
            if pos == -1:
                break
            count += 1
            start = pos + 1
        if count > 0:
            counts[name] = count
    return counts


def analyze_file(filepath: str, quiet: bool = False, strings_only: bool = False) -> dict:
    """Analyze a single binary file."""
    result = {
        'file': filepath,
        'size': os.path.getsize(filepath),
    }

    try:
        with open(filepath, 'rb') as f:
            data = f.read()
    except (OSError, PermissionError) as e:
        result['error'] = str(e)
        return result

    if not quiet:
        print(f"  Analyzing: {filepath} ({len(data)} bytes)")

    # Format detection
    result['format'] = detect_format(data)

    # Strings
    if not quiet:
        print(f"    Extracting strings...")
    strings = extract_strings(data)
    result['strings_count'] = len(strings)
    # Filter interesting strings
    interesting_keywords = ['aes', 'cbc', 'encrypt', 'decrypt', 'key', 'iv', 'nonce',
                           'regist', 'premo', 'remote', 'play', 'session', 'auth',
                           'pin', 'cert', 'ssl', 'tls', 'http', 'sce/', 'sie/',
                           'password', 'secret', 'token', 'hash', 'sha', 'md5',
                           'base64', 'xor', 'cipher']
    interesting = [s for s in strings
                   if any(kw in s['value'].lower() for kw in interesting_keywords)]
    result['interesting_strings'] = interesting[:500]  # Cap

    if strings_only:
        result['all_strings'] = strings
        return result

    # Crypto constants
    if not quiet:
        print(f"    Searching for crypto constants...")
    crypto_hits = {}
    for name, pattern in CRYPTO_CONSTANTS.items():
        hits = find_pattern(data, pattern)
        if hits:
            crypto_hits[name] = hits
            if not quiet:
                print(f"      FOUND {name} at {len(hits)} location(s)")
    result['crypto_constants'] = crypto_hits

    # AES S-box
    sbox_hits = find_pattern(data, AES_SBOX_PREFIX)
    if sbox_hits:
        result['aes_sbox_locations'] = sbox_hits
        if not quiet:
            print(f"      FOUND AES S-box at {len(sbox_hits)} location(s)")

    # Function prologues
    if not quiet:
        print(f"    Counting function prologues...")
    result['function_prologues'] = count_prologues(data)

    return result


def main():
    parser = argparse.ArgumentParser(
        description='Analyze binary files for crypto patterns and structure')
    parser.add_argument('target', help='File or directory to analyze')
    parser.add_argument('-o', '--output', help='Output JSON file path')
    parser.add_argument('-q', '--quiet', action='store_true', help='Suppress progress')
    parser.add_argument('--strings-only', action='store_true', help='Only extract strings')
    args = parser.parse_args()

    target = args.target
    if not os.path.exists(target):
        print(f"ERROR: Path not found: {target}", file=sys.stderr)
        sys.exit(1)

    # Collect files
    files = []
    if os.path.isfile(target):
        files.append(target)
    else:
        for root, dirs, filenames in os.walk(target):
            dirs[:] = [d for d in dirs if not d.startswith('.')]
            for fname in filenames:
                ext = os.path.splitext(fname)[1].lower()
                if ext in BINARY_EXTENSIONS:
                    files.append(os.path.join(root, fname))

    if not files:
        print(f"No binary files found in: {target}")
        sys.exit(0)

    if not args.quiet:
        print(f"Found {len(files)} binary file(s) to analyze\n")

    results = []
    for filepath in sorted(files):
        result = analyze_file(filepath, args.quiet, args.strings_only)
        results.append(result)

    # Print summary to stdout
    print(f"\n{'='*60}")
    print(f"ANALYSIS SUMMARY")
    print(f"{'='*60}")
    for r in results:
        print(f"\n{r['file']} ({r['size']} bytes)")
        if 'error' in r:
            print(f"  ERROR: {r['error']}")
            continue
        fmt = r.get('format', {})
        print(f"  Format: {fmt.get('format', '?')} {fmt.get('arch', '?')} {fmt.get('bits', '?')}-bit")
        print(f"  Strings: {r.get('strings_count', 0)} total, {len(r.get('interesting_strings', []))} interesting")

        if r.get('crypto_constants'):
            print(f"  CRYPTO CONSTANTS FOUND:")
            for name, hits in r['crypto_constants'].items():
                for h in hits:
                    print(f"    {name} @ {h['offset_hex']}")

        if r.get('aes_sbox_locations'):
            print(f"  AES S-BOX found at: {', '.join(h['offset_hex'] for h in r['aes_sbox_locations'])}")

        prologues = r.get('function_prologues', {})
        if prologues:
            print(f"  Function prologues: {', '.join(f'{k}={v}' for k, v in sorted(prologues.items(), key=lambda x: -x[1]))}")

        if r.get('interesting_strings'):
            print(f"  Notable strings (first 20):")
            for s in r['interesting_strings'][:20]:
                print(f"    0x{s['offset']:08x} [{s['encoding']}] {s['value']}")

    # JSON output
    if args.output:
        with open(args.output, 'w') as f:
            json.dump(results, f, indent=2)
        print(f"\nFull results written to: {args.output}")
    else:
        default_out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                    "binary_analysis_report.json")
        with open(default_out, 'w') as f:
            json.dump(results, f, indent=2)
        print(f"\nFull results written to: {default_out}")


if __name__ == '__main__':
    main()
