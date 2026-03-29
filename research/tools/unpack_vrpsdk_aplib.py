#!/usr/bin/env python3
"""
VRPSDK.dll Full Unpacker - aPLib decompression

The packer works as follows:
1. Entry at wovitjaj section (0x102c6000)
2. XOR+ADD decrypts first 0x1000 bytes of ehleidof section
3. Decrypted code is an aPLib decompressor that:
   a. Finds the PE image base by scanning for MZ header
   b. Source = ehleidof compressed data (after the decompressor code)
   c. Destination = image_base + 0x2d014 (section3 area)
   d. Decompresses aPLib data
   e. Patches OEP with a JMP to decompressed code

The decompressor code is at offsets 0x00-0x1ec in the decrypted ehleidof.
The aPLib compressed data starts at offset 0x1f8 (after stage1 setup code).
Actually, looking more carefully:
  - At 0x62: mov eax, 0x2d014; add eax, edi  -> dest = image_base + 0x2d014
  - At 0x69: mov ecx, 0x19a1f8; add ecx, edi -> src = image_base + 0x19a1f8
  - These are relative to image base (edi)

The compressed data is at vaddr = image_base + 0x19a1f8
Since ehleidof starts at vaddr = image_base + 0x19a000,
the compressed data offset within ehleidof = 0x19a1f8 - 0x19a000 = 0x1f8

The decompressed data goes to image_base + 0x2d014
Section3 starts at vaddr = image_base + 0x2d000
So it decompresses to section3 + 0x14
"""

import struct
import os
import sys

DLL_PATH = "/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos/ps3-remote-play/cualquiercosa327-Remote-Play/extracted/vrp_files/App/VRPSDK.dll"
OUTPUT_DIR = "/Users/mihailurmanschi/Work/PsOldRemotePlay/research/tools/unpack_results"

XOR_KEY = 0x6cfd7e36
ADD_VAL = 0x3640736d

EHLEIDOF_PADDR = 0x16200
EHLEIDOF_SIZE = 0x12be00
EHLEIDOF_VADDR = 0x1019a000

# From the decompressor code analysis:
# Source offset within ehleidof = 0x1f8
# Destination: section3 at vaddr offset 0x14 from section3 start
COMPRESSED_OFFSET = 0x1f8  # offset within ehleidof where compressed data starts

# Section info
SECTION3_PADDR = 0x16000
SECTION3_VADDR = 0x1002d000
SECTION3_VSIZE = 0x16d000  # virtual size (decompressed size should fit in this)

# Also check the alternate code path:
# At 0x72: mov eax, 0x1002d014 (absolute, when edi=0 / image base = 0x10000000)
# At 0x77: mov ecx, 0x1019a1f8 (absolute source)


def decrypt_first_page(ehleidof_data):
    """Decrypt first 0x1000 bytes of ehleidof with XOR+ADD."""
    data = bytearray(ehleidof_data[:0x1000])
    for i in range(0x1000 // 4):
        pos = i * 4
        val = struct.unpack_from('<I', data, pos)[0]
        val ^= XOR_KEY
        val = (val + ADD_VAL) & 0xFFFFFFFF
        struct.pack_into('<I', data, pos, val)
    return bytes(data)


def aplib_decompress(src):
    """
    aPLib decompression - pure Python implementation.
    Based on the standard aPLib algorithm.
    """
    src_idx = 0
    dst = bytearray()
    tag = 0
    bits_left = 0

    def getbit():
        nonlocal tag, bits_left, src_idx
        bits_left -= 1
        if bits_left < 0:
            tag = src[src_idx]
            src_idx += 1
            bits_left = 7
        bit = (tag >> 7) & 1
        tag = (tag << 1) & 0xFF
        return bit

    def getgamma():
        result = 1
        while True:
            result = (result << 1) + getbit()
            if not getbit():
                break
        return result

    # First byte is literal
    dst.append(src[src_idx])
    src_idx += 1

    lastoffset = 0

    while src_idx < len(src):
        if getbit():
            if getbit():
                if getbit():
                    # Short match (1 byte offset, 1 byte copy)
                    offs = src[src_idx]
                    src_idx += 1
                    length = 0
                    if offs == 0:
                        break  # End of stream
                    offs >>= 1
                    length = 2 + (offs & 0)  # Wait, let me re-check
                    # Actually: shr al, 1; if CF then length=3 else length=2
                    # But the actual aPLib uses: read byte, shr 1, if zero -> end
                    # offset = byte >> 1, length = 2 + carry
                    b = src[src_idx - 1]  # re-read
                    src_idx -= 1  # back up
                    b = src[src_idx]
                    src_idx += 1
                    if b == 0:
                        break
                    length = 2 + (b & 1)
                    offs = b >> 1
                    if offs == 0:
                        break
                    # Copy from recent output
                    for i in range(length):
                        dst.append(dst[-offs])
                    lastoffset = offs
                else:
                    # Long match
                    offs = (getgamma() - 2) << 8
                    offs += src[src_idx]
                    src_idx += 1
                    lastoffset = offs

                    length = getgamma()
                    if offs >= 0x7d00:
                        length += 1
                    if offs >= 0x500:
                        length += 1
                    if offs < 0x80:
                        length += 2

                    for i in range(length):
                        dst.append(dst[-offs - 1])
            else:
                # Match with last offset
                length = getgamma()
                offs = lastoffset
                for i in range(length):
                    dst.append(dst[-offs - 1])
        else:
            # Literal byte
            dst.append(src[src_idx])
            src_idx += 1

    return bytes(dst)


def aplib_decompress_v2(src):
    """
    aPLib decompression - more careful implementation matching the disassembly.
    """
    try:
        import aplib as aplib_mod
        result = aplib_mod.decompress(src)
        return result
    except Exception as e:
        print(f"  aplib module decompress failed: {e}")

    # Fallback to manual
    return None


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


def entropy(data):
    """Calculate Shannon entropy."""
    if not data:
        return 0
    freq = [0] * 256
    for b in data:
        freq[b] += 1
    import math
    ent = 0.0
    n = len(data)
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
    print(f"DLL size: {len(dll_data)} bytes")

    # Extract ehleidof section
    ehleidof = dll_data[EHLEIDOF_PADDR:EHLEIDOF_PADDR + EHLEIDOF_SIZE]
    print(f"ehleidof section: {len(ehleidof)} bytes")

    # Step 1: Decrypt first 0x1000 bytes
    print("\n=== Step 1: Decrypt first 0x1000 bytes of ehleidof ===")
    decrypted_page = decrypt_first_page(ehleidof)

    # Verify it looks like valid code
    print(f"  First 4 bytes: {decrypted_page[:4].hex()} (expected: b8010000 = mov eax, 1)")
    assert decrypted_page[0] == 0xb8 and decrypted_page[1] == 0x01, "Decryption seems wrong!"
    print("  Decryption verified OK!")

    # Step 2: Extract compressed data
    # The compressed data starts at offset 0x1f8 within ehleidof
    # But the first 0x1000 bytes are decrypted - the rest of ehleidof is NOT encrypted
    # (only the first page was XOR+ADD encrypted, the rest is raw aPLib compressed data)

    # Actually wait - let me reconsider. The XOR+ADD only decrypts 0x1000 bytes.
    # The compressed data at offset 0x1f8 straddles the decrypted region.
    # Bytes 0x1f8-0xfff are in the decrypted page, bytes 0x1000+ are raw from ehleidof.

    # Build the full source: decrypted first page + rest of raw ehleidof
    full_data = bytearray(decrypted_page) + bytearray(ehleidof[0x1000:])

    compressed_data = bytes(full_data[COMPRESSED_OFFSET:])
    print(f"\n=== Step 2: Extract compressed data ===")
    print(f"  Compressed data starts at offset 0x{COMPRESSED_OFFSET:x} within ehleidof")
    print(f"  Compressed data size: {len(compressed_data)} bytes ({len(compressed_data)/1024:.1f} KB)")
    print(f"  First 32 bytes of compressed data: {compressed_data[:32].hex()}")
    print(f"  Entropy of compressed data: {entropy(compressed_data[:0x10000]):.4f}")

    # Save compressed data
    with open(os.path.join(OUTPUT_DIR, "compressed_payload.bin"), "wb") as f:
        f.write(compressed_data)
    print(f"  Saved to compressed_payload.bin")

    # Step 3: Try aPLib decompression using the Python library
    print(f"\n=== Step 3: aPLib Decompression ===")

    # Try with the aplib module first
    decompressed = None
    try:
        import aplib as aplib_mod
        print("  Using aplib Python module...")
        # The aplib module might expect the data with or without header
        # Try raw decompression
        try:
            decompressed = aplib_mod.decompress(compressed_data)
            print(f"  SUCCESS! Decompressed to {len(decompressed)} bytes ({len(decompressed)/1024:.1f} KB)")
        except Exception as e:
            print(f"  Raw decompress failed: {e}")
            # Try with safe mode or different offsets
            for skip in [0, 1, 2, 4, 8]:
                try:
                    decompressed = aplib_mod.decompress(compressed_data[skip:])
                    print(f"  SUCCESS at offset +{skip}! Decompressed to {len(decompressed)} bytes")
                    break
                except Exception as e2:
                    pass
    except ImportError:
        print("  aplib module not available")

    if decompressed is None:
        print("  Library decompression failed, trying manual...")
        try:
            decompressed = aplib_decompress(compressed_data)
            if decompressed:
                print(f"  Manual decompress got {len(decompressed)} bytes")
        except Exception as e:
            print(f"  Manual decompress failed: {e}")

    if decompressed is None:
        # Also try: maybe the compressed data is the ENTIRE ehleidof after 0x1f8,
        # but without the first page decryption applied (raw bytes from disk)
        print("\n  Trying with raw (non-decrypted) ehleidof data from offset 0x1f8...")
        raw_compressed = ehleidof[COMPRESSED_OFFSET:]
        try:
            import aplib as aplib_mod
            decompressed = aplib_mod.decompress(raw_compressed)
            print(f"  SUCCESS with raw data! Decompressed to {len(decompressed)} bytes")
        except Exception as e:
            print(f"  Raw data decompress also failed: {e}")

    if decompressed is not None and len(decompressed) > 100:
        # Save decompressed data
        outpath = os.path.join(OUTPUT_DIR, "decompressed_section.bin")
        with open(outpath, "wb") as f:
            f.write(decompressed)
        print(f"\n  Saved decompressed data to {outpath}")
        print(f"  Decompressed size: {len(decompressed)} bytes ({len(decompressed)/1024:.1f} KB)")
        print(f"  Entropy: {entropy(decompressed):.4f}")

        # Analyze decompressed data
        print(f"\n=== Step 4: Analyze decompressed data ===")

        # Check for PE headers
        if decompressed[:2] == b'MZ':
            print("  Found MZ header at start!")

        # Find strings
        ascii_strings = find_strings(decompressed, min_len=6)
        wide_strings = find_wide_strings(decompressed, min_len=5)

        print(f"  ASCII strings: {len(ascii_strings)}")
        print(f"  Wide strings: {len(wide_strings)}")

        # Look for interesting strings
        interesting = ["regist", "premo", "/sce/", "BCrypt", "PREMO-", "Client-Type",
                       "HTTP", "session", "video", "audio", "register", "nonce",
                       "AES", "crypto", "socket", "connect", "kernel32", "ntdll",
                       "ws2_32", "winhttp", "advapi32", "bcrypt", "LoadLibrary",
                       "GetProcAddress", "VirtualProtect", "CreateFile",
                       "Remote Play", "xRegistry", "PIN", "SKEY",
                       "pkey", "IV", "CBC", "encrypt", "decrypt"]

        print("\n  Interesting strings found:")
        for keyword in interesting:
            for offset, s in ascii_strings:
                if keyword.lower() in s.lower():
                    print(f"    [{keyword}] 0x{offset:06x}: {s}")
                    break
            for offset, s in wide_strings:
                if keyword.lower() in s.lower():
                    print(f"    [{keyword}] 0x{offset:06x}: {s} (wide)")
                    break

        # Dump first 50 interesting strings
        print(f"\n  First 50 ASCII strings (min 8 chars):")
        long_strings = [(o, s) for o, s in ascii_strings if len(s) >= 8]
        for offset, s in long_strings[:50]:
            print(f"    0x{offset:06x}: {s}")

        print(f"\n  First 30 wide strings:")
        for offset, s in wide_strings[:30]:
            print(f"    0x{offset:06x}: {s}")

        # Save all strings
        with open(os.path.join(OUTPUT_DIR, "decompressed_strings.txt"), "w") as f:
            f.write("=== ASCII strings (min 6 chars) ===\n")
            for offset, s in ascii_strings:
                f.write(f"0x{offset:06x}: {s}\n")
            f.write(f"\n=== Wide strings (min 5 chars) ===\n")
            for offset, s in wide_strings:
                f.write(f"0x{offset:06x}: {s}\n")
        print(f"\n  All strings saved to decompressed_strings.txt")

        # Search for crypto constants
        print(f"\n  Searching for crypto constants...")
        crypto_search = {
            "SKEY0 d1b212eb": bytes.fromhex("eb12b2d1"),  # LE
            "SKEY1 0fc213f8": bytes.fromhex("f813c20f"),
            "SKEY2 f116f0da": bytes.fromhex("daf016f1"),
            "PC XOR ec6d706b": bytes.fromhex("6b706dec"),
            "PSP XOR key": bytes.fromhex("daf016f1"),
            "Phone XOR b62e7572": bytes.fromhex("72752eb6"),
        }
        for label, pattern in crypto_search.items():
            idx = decompressed.find(pattern)
            if idx >= 0:
                context = decompressed[max(0,idx-8):idx+16]
                print(f"    {label} found at 0x{idx:06x}: {context.hex()}")

        # Build a reconstructed DLL
        print(f"\n=== Step 5: Reconstruct unpacked DLL ===")
        # The decompressed data goes to vaddr 0x1002d014 (section3 + 0x14)
        # We can overlay it onto the DLL
        reconstructed = bytearray(dll_data)

        # Also apply the first-page decryption to ehleidof in the reconstructed DLL
        for i in range(0x1000):
            reconstructed[EHLEIDOF_PADDR + i] = decrypted_page[i]

        # The decompressed data should go at section3's file offset + 0x14
        # But section3 only has 0x200 bytes on disk with vsize 0x16d000
        # We need to expand it. For analysis purposes, let's just save the raw decompressed data.

        # Actually, let's create a proper reconstructed file by:
        # - Taking the original section0 (which IS the code section, but encrypted by the same scheme?)
        # - OR: the decompressed data IS the original code that should replace section0

        # Let's check: section0 is at paddr 0x1000, vaddr 0x10001000, size 0x12200
        # section3 is at paddr 0x16000, vaddr 0x1002d000, vsize 0x16d000
        # If decompressed goes to section3, the original code lives there

        # Save as a flat binary for analysis with radare2
        outpath = os.path.join(OUTPUT_DIR, "unpacked_code.bin")
        with open(outpath, "wb") as f:
            f.write(decompressed)
        print(f"  Saved unpacked code to {outpath}")
        print(f"  This code should be loaded at VA 0x1002d014")
        print(f"  To analyze in radare2:")
        print(f"    r2 -a x86 -b 32 -m 0x1002d014 {outpath}")

    else:
        print("\n  DECOMPRESSION FAILED - could not extract unpacked data")
        print("  The aPLib data may use a variant or the offset might be wrong")

        # Let's try all reasonable offsets
        print("\n  Trying various offsets for compressed data...")
        import aplib as aplib_mod
        for offset in range(0x1e0, 0x220):
            try:
                test_data = bytes(full_data[offset:])
                result = aplib_mod.decompress(test_data)
                if len(result) > 1000:
                    print(f"  SUCCESS at offset 0x{offset:x}! Got {len(result)} bytes")
                    with open(os.path.join(OUTPUT_DIR, f"decomp_off_{offset:x}.bin"), "wb") as f:
                        f.write(result)
                    break
            except:
                pass

        # Also try offsets within raw (non-decrypted) data
        print("  Trying offsets in raw ehleidof data...")
        for offset in range(0x1e0, 0x220):
            try:
                test_data = ehleidof[offset:]
                result = aplib_mod.decompress(test_data)
                if len(result) > 1000:
                    print(f"  SUCCESS with raw at offset 0x{offset:x}! Got {len(result)} bytes")
                    with open(os.path.join(OUTPUT_DIR, f"decomp_raw_off_{offset:x}.bin"), "wb") as f:
                        f.write(result)
                    break
            except:
                pass


if __name__ == "__main__":
    main()
