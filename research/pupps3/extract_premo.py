#!/usr/bin/env python3
"""
PS3 Firmware Extractor — Extract premo_plugin.sprx from PS3UPDAT.PUP

Handles the full pipeline:
  PUP → update_files.tar → CORE_OS_PACKAGE.pkg → dev_flash/vsh/module/premo_plugin.sprx

Based on the PS3 PUP/PKG format documentation from psdevwiki.
"""

import struct
import os
import sys
import tarfile
import hashlib
from pathlib import Path

try:
    from Crypto.Cipher import AES
except ImportError:
    print("ERROR: pycryptodome not installed. Run: pip3 install pycryptodome")
    sys.exit(1)

# ─────────────────────────────────────────────────────────────
# PS3 Firmware Encryption Keys (public since 2012 LV0 leak)
# These are the keys for decrypting CORE_OS packages
# ─────────────────────────────────────────────────────────────

# PKG AES key for CORE_OS_PACKAGE (from ps3devwiki / scetool keys database)
CORE_OS_KEYS = {
    # erk (encryption round key) for CORE_OS_PACKAGE
    "erk": bytes.fromhex("5D78C4C404575F32E842B6AEF7EAE7FF"),
    "riv": bytes.fromhex("00000000000000000000000000000000"),
}

def log(phase, msg):
    colors = {"ok": "\033[32m", "step": "\033[33m", "err": "\033[31m", "info": "\033[36m"}
    c = colors.get(phase, "")
    print(f"{c}[{phase.upper()}]\033[0m {msg}")


class PUPExtractor:
    """Extract files from a PS3 PUP (PlayStation Update Package)"""

    def __init__(self, pup_path):
        self.pup_path = pup_path
        self.data = open(pup_path, "rb").read()

    def extract(self, output_dir):
        """Extract all sections from the PUP"""
        os.makedirs(output_dir, exist_ok=True)

        # PUP Header
        magic = self.data[0:8]
        # Parse header
        pkg_version = struct.unpack(">Q", self.data[0x08:0x10])[0]
        img_version = struct.unpack(">Q", self.data[0x10:0x18])[0]
        file_count = struct.unpack(">Q", self.data[0x18:0x20])[0]
        header_length = struct.unpack(">Q", self.data[0x20:0x28])[0]
        data_length = struct.unpack(">Q", self.data[0x28:0x30])[0]

        log("info", f"PUP version: {pkg_version}, files: {file_count}")

        # File entries start at offset 0x30
        # Each entry: id(8) + offset(8) + size(8) = 24 bytes
        # Then hash entries follow
        entries = []
        for i in range(file_count):
            off = 0x30 + i * 0x20
            entry_id = struct.unpack(">Q", self.data[off:off+8])[0]
            entry_offset = struct.unpack(">Q", self.data[off+8:off+16])[0]
            entry_size = struct.unpack(">Q", self.data[off+16:off+24])[0]
            entries.append((entry_id, entry_offset, entry_size))

        # Known file IDs
        file_names = {
            0x100: "version.txt",
            0x101: "license.txt",
            0x102: "promo_flags.txt",
            0x103: "update_flags.txt",
            0x104: "patch_build.txt",
            0x200: "ps3swu.self",
            0x201: "vsh.tar",
            0x202: "dots.txt",
            0x203: "patch_data.pkg",
            0x300: "update_files.tar",
        }

        extracted = {}
        for entry_id, offset, size in entries:
            name = file_names.get(entry_id, f"unknown_{entry_id:04x}.bin")
            filepath = os.path.join(output_dir, name)
            with open(filepath, "wb") as f:
                f.write(self.data[offset:offset+size])
            extracted[name] = filepath
            log("ok", f"Extracted {name} ({size:,} bytes)")

        return extracted


def extract_tar_packages(tar_path, output_dir):
    """Extract .pkg files from update_files.tar"""
    os.makedirs(output_dir, exist_ok=True)
    with tarfile.open(tar_path, "r") as tar:
        tar.extractall(output_dir)
    files = os.listdir(output_dir)
    log("ok", f"Extracted {len(files)} files from update_files.tar")
    return files


def find_and_extract_core_os(pkg_dir, output_dir):
    """
    The CORE_OS_PACKAGE.pkg contains a tar of dev_flash.
    It's an SCE-signed package. We need to find the data section and extract it.
    """
    pkg_path = os.path.join(pkg_dir, "CORE_OS_PACKAGE.pkg")
    if not os.path.exists(pkg_path):
        log("err", "CORE_OS_PACKAGE.pkg not found!")
        return None

    data = open(pkg_path, "rb").read()
    log("info", f"CORE_OS_PACKAGE.pkg: {len(data):,} bytes")

    # SCE header: magic "SCE\x00" at offset 0
    if data[:4] != b"SCE\x00":
        log("err", "Not a valid SCE package")
        return None

    # SCE header structure (simplified):
    # 0x00: magic (4)
    # 0x04: version (4)
    # 0x08: key_revision (2)
    # 0x0A: type (2)
    # 0x0C: metadata_offset (4) — usually 0
    # 0x10: header_length (8)
    # 0x18: data_length (8)

    header_length = struct.unpack(">Q", data[0x10:0x18])[0]
    data_length = struct.unpack(">Q", data[0x18:0x20])[0]

    log("info", f"SCE header: {header_length} bytes, data: {data_length} bytes")

    # The actual filesystem data starts after the SCE header + metadata
    # For CORE_OS, the payload is a tar.gz or tar containing dev_flash
    # Try to find tar signature within the data

    os.makedirs(output_dir, exist_ok=True)

    # Strategy: scan for tar signatures in the raw data
    # TAR files have "ustar" at offset 257 within each 512-byte block
    tar_offset = None
    for offset in range(0, min(len(data), 0x200000), 0x10):  # Search first 2MB
        if data[offset:offset+2] == b'\x1f\x8b':  # gzip magic
            tar_offset = offset
            log("info", f"Found gzip data at offset 0x{offset:x}")
            break
        # Check for tar "ustar" magic (at +257 from start of a tar entry)
        if offset + 512 < len(data) and data[offset+257:offset+262] == b'ustar':
            tar_offset = offset
            log("info", f"Found tar data at offset 0x{offset:x}")
            break

    if tar_offset is None:
        # Try treating the data portion directly
        # The encrypted data starts after header_length
        # For newer firmware, the data after the SCE header may be the raw content
        log("info", "No tar signature found. Trying raw data extraction...")

        # Write the raw data section for manual inspection
        raw_path = os.path.join(output_dir, "core_os_raw.bin")
        with open(raw_path, "wb") as f:
            f.write(data[header_length:])
        log("info", f"Raw data written to {raw_path} ({len(data) - header_length:,} bytes)")

        # Check if it's a tar
        if data[header_length + 257:header_length + 262] == b'ustar':
            tar_offset = header_length
            log("ok", "Raw data section IS a tar file!")

    if tar_offset is not None:
        tar_data_path = os.path.join(output_dir, "core_os.tar")
        is_gzip = data[tar_offset:tar_offset+2] == b'\x1f\x8b'

        if is_gzip:
            tar_data_path += ".gz"

        with open(tar_data_path, "wb") as f:
            f.write(data[tar_offset:])

        try:
            mode = "r:gz" if is_gzip else "r"
            with tarfile.open(tar_data_path, mode) as tar:
                tar.extractall(output_dir)
            log("ok", "Extracted CORE_OS tar contents")
            return output_dir
        except Exception as e:
            log("err", f"Failed to extract tar: {e}")
            log("info", "The CORE_OS data may still be encrypted")

    return None


def scan_for_sprx_files(search_dir, output_dir):
    """Search for premo SPRX files in extracted firmware"""
    os.makedirs(output_dir, exist_ok=True)
    found = []

    for root, dirs, files in os.walk(search_dir):
        for f in files:
            if "premo" in f.lower():
                src = os.path.join(root, f)
                dst = os.path.join(output_dir, f)
                with open(src, "rb") as sf:
                    with open(dst, "wb") as df:
                        df.write(sf.read())
                found.append(dst)
                log("ok", f"Found: {f} ({os.path.getsize(src):,} bytes) at {os.path.relpath(src, search_dir)}")

    return found


def try_decrypt_sprx(sprx_path, elf_path, scetool_path, unself_path):
    """Try to decrypt SPRX using available tools"""
    log("step", f"Decrypting {os.path.basename(sprx_path)}...")

    # Try scetool first
    if scetool_path and os.path.exists(scetool_path):
        scetool_dir = os.path.dirname(scetool_path)
        ret = os.system(f'cd "{scetool_dir}" && ./scetool --decrypt "{sprx_path}" "{elf_path}" 2>&1')
        if ret == 0 and os.path.exists(elf_path) and os.path.getsize(elf_path) > 0:
            log("ok", f"Decrypted with scetool: {elf_path} ({os.path.getsize(elf_path):,} bytes)")
            return True

    # Try unself
    if unself_path and os.path.exists(unself_path):
        ret = os.system(f'"{unself_path}" "{sprx_path}" "{elf_path}" 2>&1')
        if ret == 0 and os.path.exists(elf_path) and os.path.getsize(elf_path) > 0:
            log("ok", f"Decrypted with unself: {elf_path} ({os.path.getsize(elf_path):,} bytes)")
            return True

    log("err", f"Could not decrypt {os.path.basename(sprx_path)}")
    return False


def scan_raw_for_elf(data_path, output_dir):
    """
    Last resort: scan raw binary data for ELF headers or SELF/SCE headers
    that might be premo_plugin embedded in the firmware
    """
    log("step", "Scanning raw firmware data for SPRX/ELF signatures...")
    data = open(data_path, "rb").read()
    os.makedirs(output_dir, exist_ok=True)

    found = []

    # Search for SCE headers (SELF/SPRX files start with "SCE\x00")
    offset = 0
    file_idx = 0
    while offset < len(data) - 4:
        # SCE magic
        if data[offset:offset+4] == b'SCE\x00':
            # Read header length and data length
            if offset + 0x20 <= len(data):
                try:
                    hdr_len = struct.unpack(">Q", data[offset+0x10:offset+0x18])[0]
                    dat_len = struct.unpack(">Q", data[offset+0x18:offset+0x20])[0]
                    total = hdr_len + dat_len

                    if 0x1000 < total < 0x1000000:  # Between 4KB and 16MB (reasonable SPRX size)
                        chunk = data[offset:offset+total]
                        out_name = f"sce_file_{file_idx:04d}.sprx"
                        out_path = os.path.join(output_dir, out_name)
                        with open(out_path, "wb") as f:
                            f.write(chunk)
                        found.append((out_path, total, offset))
                        file_idx += 1
                        offset += total
                        continue
                except:
                    pass
        offset += 0x10

    if found:
        log("ok", f"Found {len(found)} SCE/SPRX files in raw data")
        for path, size, off in found:
            log("info", f"  {os.path.basename(path)}: {size:,} bytes at offset 0x{off:x}")

    return found


def main():
    script_dir = Path(__file__).parent
    pup_file = script_dir / "PS3UPDAT.PUP"
    work_dir = script_dir / "extraction"
    output_dir = work_dir / "output"
    tools_dir = work_dir / "tools"

    scetool_path = tools_dir / "scetool" / "scetool"
    unself_path = tools_dir / "ps3tools" / "unself"

    if not pup_file.exists():
        log("err", f"PS3UPDAT.PUP not found at {pup_file}")
        sys.exit(1)

    print(f"\n\033[36m{'='*60}\033[0m")
    print(f"\033[36m  PS3 Firmware Extraction — Python Pipeline\033[0m")
    print(f"\033[36m{'='*60}\033[0m\n")

    # Phase 1: Extract PUP
    log("step", "Phase 1: Extracting PUP...")
    pup_dir = output_dir / "pup_contents"
    extractor = PUPExtractor(str(pup_file))
    extracted = extractor.extract(str(pup_dir))

    # Phase 2: Extract update_files.tar
    update_tar = pup_dir / "update_files.tar"
    if not update_tar.exists():
        log("err", "update_files.tar not found in PUP")
        sys.exit(1)

    log("step", "Phase 2: Extracting update_files.tar...")
    pkg_dir = output_dir / "packages"
    extract_tar_packages(str(update_tar), str(pkg_dir))

    # Phase 3: Extract CORE_OS_PACKAGE
    log("step", "Phase 3: Extracting CORE_OS_PACKAGE.pkg...")
    core_os_dir = output_dir / "core_os_extracted"
    result = find_and_extract_core_os(str(pkg_dir), str(core_os_dir))

    # Phase 4: Find premo files
    final_dir = output_dir / "premo_files"
    os.makedirs(str(final_dir), exist_ok=True)

    premo_found = []
    if result:
        log("step", "Phase 4: Searching for premo SPRX files...")
        premo_found = scan_for_sprx_files(str(core_os_dir), str(final_dir))

    # If we didn't find premo in the tar, scan the raw data
    if not premo_found:
        log("info", "Premo not found in extracted tar. Scanning raw CORE_OS data...")
        raw_file = core_os_dir / "core_os_raw.bin"
        if raw_file.exists():
            sce_files = scan_raw_for_elf(str(raw_file), str(output_dir / "sce_scan"))
            if sce_files:
                log("info", f"Found {len(sce_files)} potential SPRX files. Trying to identify premo...")
                # Try decrypting each one to see if it's valid
                for sce_path, size, offset in sce_files:
                    elf_path = sce_path.replace(".sprx", ".elf")
                    if try_decrypt_sprx(sce_path, elf_path, str(scetool_path), str(unself_path)):
                        premo_found.append(elf_path)

    # If we still don't have premo, try searching ALL extracted content
    if not premo_found:
        log("info", "Searching all extracted content for premo files...")
        for root, dirs, files in os.walk(str(output_dir)):
            for f in files:
                if "premo" in f.lower():
                    src = os.path.join(root, f)
                    premo_found.append(src)
                    log("ok", f"Found: {f} at {os.path.relpath(src, str(output_dir))}")

    # Phase 5: Try to decrypt
    if premo_found:
        log("step", "Phase 5: Attempting decryption...")
        for sprx in premo_found:
            if sprx.endswith(".sprx"):
                elf = sprx.replace(".sprx", ".elf")
                try_decrypt_sprx(sprx, elf, str(scetool_path), str(unself_path))

    # Summary
    print(f"\n\033[36m{'='*60}\033[0m")
    print(f"\033[36m  RESULTS\033[0m")
    print(f"\033[36m{'='*60}\033[0m\n")

    elf_files = list(Path(str(output_dir)).rglob("*.elf"))
    sprx_files = list(Path(str(output_dir)).rglob("*premo*.sprx"))

    if elf_files:
        log("ok", "Decrypted ELF files ready for Ghidra:")
        for f in elf_files:
            print(f"    {f} ({f.stat().st_size:,} bytes)")
    elif sprx_files:
        log("info", "SPRX files found but could not be decrypted:")
        for f in sprx_files:
            print(f"    {f} ({f.stat().st_size:,} bytes)")
        print()
        log("info", "Try manual decryption with:")
        print(f"    cd {tools_dir / 'scetool'}")
        print(f"    ./scetool --decrypt <sprx_file> <output.elf>")
    else:
        log("err", "No premo files found.")
        log("info", "The CORE_OS package may need additional decryption.")
        log("info", f"Check raw data: {core_os_dir}")
        print()
        log("info", "Alternative: try PS3MFW Builder which handles all firmware versions:")
        print("    git clone https://github.com/BitEdits/ps3mfw")
        print("    # Follow its README for firmware extraction")

    print(f"\n\033[36mAll output in: {output_dir}\033[0m\n")


if __name__ == "__main__":
    main()
