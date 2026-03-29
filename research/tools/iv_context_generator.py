#!/usr/bin/env python3
"""
PS3 Remote Play Registration — Definitive IV Context Test Vector Generator

Generates ALL plausible 8-byte IV context values for PS3 Remote Play
registration across all three device types. This is the comprehensive
reference for brute-forcing the unknown IV context value.

Background:
  The PS3 registration handler derives AES-128-CBC key and IV from:
    - km: 16 random bytes sent appended to the encrypted body
    - Static XOR keys and IV bases (per device type)
    - An unknown 8-byte "context" value XOR'd into the IV base

  The key derivation formulas are VERIFIED from decompiled PPC code.
  The IV base XOR position is VERIFIED (first 8 bytes for PSP/PC,
  last 8 bytes for Phone). The 8-byte context source is the sole unknown.

Device type shift (CONFIRMED from firmware):
  PS3 menu "PSP"    -> expects Client-Type: Phone  -> uses Phone crypto
  PS3 menu "Phone"  -> expects Client-Type: PC     -> uses PC crypto
  PS3 menu "PC"     -> expects Client-Type: ???     -> uses ??? crypto (possibly VITA)

Usage:
  python3 iv_context_generator.py <PIN>                    # Generate all vectors
  python3 iv_context_generator.py <PIN> --json             # JSON output
  python3 iv_context_generator.py <PIN> --verify <hexdata> # Try decrypting captured data
  python3 iv_context_generator.py <PIN> --summary          # Count only

Requires: pycryptodome (pip install pycryptodome)
"""

import argparse
import hashlib
import hmac
import json
import struct
import sys
from typing import List, Tuple

try:
    from Crypto.Cipher import AES
except ImportError:
    print("ERROR: pycryptodome required. Install with: pip install pycryptodome")
    sys.exit(1)


# =============================================================================
# STATIC CRYPTO KEYS — All verified from Ghidra analysis of sysconf_plugin.elf
# =============================================================================

# Session keys (used in session auth, but skey1 is NEVER used in open-rp)
SKEY0 = bytes([0xD1, 0xB2, 0x12, 0xEB, 0x73, 0x86, 0x6C, 0x7B,
               0x12, 0xA7, 0x5E, 0x0C, 0x04, 0xC6, 0xB8, 0x91])
SKEY1 = bytes([0x1F, 0xD5, 0xB9, 0xFA, 0x71, 0xB8, 0x96, 0x81,
               0xB2, 0x87, 0x92, 0xE2, 0x6F, 0x38, 0xC3, 0x6F])
SKEY2 = bytes([0x65, 0x2D, 0x8C, 0x90, 0xDE, 0x87, 0x17, 0xCF,
               0x4F, 0xB3, 0xD8, 0xD3, 0x01, 0x79, 0x6B, 0x59])

# --- PSP registration keys (DAT_00150ea0, DAT_00150eb0) ---
PSP_XOR = bytes([0xFD, 0xC3, 0xF6, 0xA6, 0x4D, 0x2A, 0xBA, 0x7A,
                 0x38, 0x92, 0x6C, 0xBC, 0x34, 0x31, 0xE1, 0x0E])
PSP_IV  = bytes([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D,
                 0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64])

# --- Phone registration keys (DAT_00150ec0, DAT_00150ed0) ---
PHONE_XOR = bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                   0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6])
PHONE_IV  = bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                   0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A])

# --- PC registration keys (DAT_00150ee0, DAT_00150ef0) ---
PC_XOR = bytes([0xEC, 0x6D, 0x70, 0x6B, 0x1E, 0x0A, 0x9A, 0x75,
                0x8C, 0xDA, 0x78, 0x27, 0x51, 0xA3, 0xC3, 0x7B])
PC_IV  = bytes([0x5B, 0x64, 0x40, 0xC5, 0x2E, 0x74, 0xC0, 0x46,
                0x48, 0x72, 0xC9, 0xC5, 0x49, 0x0C, 0x79, 0x04])

# --- WiFi passphrases (used for ad-hoc connection) ---
WIFI_PASSPHRASE_PSP_PC = b"xeRFa3VYDHSfNjE_nA{z>pk2xANqicHqQFvij}0WHz[7kzO2Yynp4o4j}U2"
WIFI_PASSPHRASE_PHONE  = b"RvnpNXP}xS5qOWfj77guV}0lPAS37hzONG7ZHMNBAwM0mKjt1mkUhHjdbyF"

# --- Nonce XOR keys (for session, not registration — included for completeness) ---
NONCE_XOR_VITA = bytes([0xAF, 0x74, 0xB5, 0x4F, 0x38, 0xF8, 0xAF, 0xC8,
                        0x75, 0x77, 0xB2, 0xD5, 0x47, 0x76, 0x3B, 0xFD])


# =============================================================================
# KEY DERIVATION FUNCTIONS — Verified from decompiled PPC assembly
# =============================================================================

def derive_key_psp(km: bytes) -> bytes:
    """PSP: key[i] = (km[i] ^ PSP_XOR[i]) - i - 0x25"""
    return bytes(((km[i] ^ PSP_XOR[i]) - i - 0x25) & 0xFF for i in range(16))

def derive_key_phone(km: bytes) -> bytes:
    """Phone: key[i] = (km[i] - i - 0x28) ^ PHONE_XOR[i]"""
    return bytes(((km[i] - i - 0x28) & 0xFF) ^ PHONE_XOR[i] for i in range(16))

def derive_key_pc(km: bytes) -> bytes:
    """PC: key[i] = (km[i] ^ PC_XOR[i]) - i - 0x2B"""
    return bytes(((km[i] ^ PC_XOR[i]) - i - 0x2B) & 0xFF for i in range(16))


# =============================================================================
# IV DERIVATION FUNCTIONS
# =============================================================================

def derive_iv_psp(ctx8: bytes) -> bytes:
    """PSP: XOR first 8 bytes of IV base with context"""
    iv = bytearray(PSP_IV)
    for i in range(8):
        iv[i] ^= ctx8[i]
    return bytes(iv)

def derive_iv_phone(ctx8: bytes) -> bytes:
    """Phone: XOR last 8 bytes of IV base with context"""
    iv = bytearray(PHONE_IV)
    for i in range(8):
        iv[8 + i] ^= ctx8[i]
    return bytes(iv)

def derive_iv_pc(ctx8: bytes) -> bytes:
    """PC: XOR first 8 bytes of IV base with context (same pattern as PSP)"""
    iv = bytearray(PC_IV)
    for i in range(8):
        iv[i] ^= ctx8[i]
    return bytes(iv)


# =============================================================================
# PKCS7 PADDING
# =============================================================================

def pkcs7_pad(data: bytes, block_size: int = 16) -> bytes:
    pad_len = block_size - (len(data) % block_size)
    return data + bytes([pad_len] * pad_len)

def pkcs7_unpad(data: bytes) -> bytes:
    if not data:
        return data
    pad_len = data[-1]
    if pad_len < 1 or pad_len > 16:
        return data
    if data[-pad_len:] != bytes([pad_len] * pad_len):
        return data
    return data[:-pad_len]


# =============================================================================
# IV CONTEXT GENERATORS
# =============================================================================

def generate_all_contexts(pin_str: str) -> List[Tuple[bytes, str, str]]:
    """
    Generate all plausible 8-byte IV context values from the PIN.
    Returns list of (context_bytes, label, category).
    """
    pin_int = int(pin_str)
    contexts = []

    def add(data: bytes, label: str, category: str):
        assert len(data) == 8, f"Context must be 8 bytes, got {len(data)} for {label}"
        contexts.append((data, label, category))

    # =========================================================================
    # Category: PIN-based interpretations
    # =========================================================================

    # All zeros (baseline — IV base used as-is)
    add(bytes(8), "all_zeros", "pin")

    # PIN as big-endian int64 (longlong cast from int — code shows this pattern)
    add(pin_int.to_bytes(8, 'big'), "pin_be64", "pin")

    # PIN as little-endian int64
    add(pin_int.to_bytes(8, 'little'), "pin_le64", "pin")

    # PIN as big-endian int32, left-padded (high bytes)
    add(pin_int.to_bytes(4, 'big') + b'\x00\x00\x00\x00', "pin_be32_left", "pin")

    # PIN as big-endian int32, right-padded (low bytes)
    add(b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'big'), "pin_be32_right", "pin")

    # PIN as little-endian int32, left-padded
    add(pin_int.to_bytes(4, 'little') + b'\x00\x00\x00\x00', "pin_le32_left", "pin")

    # PIN as little-endian int32, right-padded
    add(b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'little'), "pin_le32_right", "pin")

    # PIN as ASCII string (8 digits = 8 bytes, perfect fit)
    add(pin_str.encode('ascii')[:8].ljust(8, b'\x00'), "pin_ascii", "pin")

    # PIN halves swapped: e.g., 12345678 -> 56781234
    swapped_str = pin_str[4:] + pin_str[:4]
    if swapped_str.isdigit():
        sw_int = int(swapped_str)
        add(sw_int.to_bytes(8, 'big'), "pin_halves_swapped_be64", "pin")
        add(sw_int.to_bytes(8, 'little'), "pin_halves_swapped_le64", "pin")

    # PIN halves swapped as ASCII
    add(swapped_str.encode('ascii')[:8].ljust(8, b'\x00'), "pin_halves_swapped_ascii", "pin")

    # PIN digits as individual bytes: 12345678 -> 01 02 03 04 05 06 07 08
    add(bytes(int(d) for d in pin_str[:8]), "pin_digits_as_bytes", "pin")

    # PIN as BCD: 12345678 -> 0x12 0x34 0x56 0x78 0x00 0x00 0x00 0x00
    bcd = []
    for i in range(0, len(pin_str), 2):
        if i + 1 < len(pin_str):
            bcd.append(int(pin_str[i]) * 16 + int(pin_str[i + 1]))
        else:
            bcd.append(int(pin_str[i]) * 16)
    bcd_bytes = bytes(bcd).ljust(8, b'\x00')[:8]
    add(bcd_bytes, "pin_bcd_left", "pin")

    # PIN as BCD right-padded
    add(b'\x00' * (8 - len(bytes(bcd))) + bytes(bcd), "pin_bcd_right", "pin") if len(bytes(bcd)) <= 8 else None

    # PIN reversed digits as ASCII: 12345678 -> "87654321"
    add(pin_str[::-1].encode('ascii')[:8].ljust(8, b'\x00'), "pin_reversed_ascii", "pin")

    # PIN reversed digits as int BE64
    rev_int = int(pin_str[::-1])
    add(rev_int.to_bytes(8, 'big'), "pin_reversed_be64", "pin")

    # PIN * 2 as int64
    doubled = (pin_int * 2) & 0xFFFFFFFFFFFFFFFF
    add(doubled.to_bytes(8, 'big'), "pin_doubled_be64", "pin")

    # ~PIN (bitwise NOT) as int64
    notpin = (~pin_int) & 0xFFFFFFFFFFFFFFFF
    add(notpin.to_bytes(8, 'big'), "pin_not_be64", "pin")

    # ~PIN as int32 left-padded
    notpin32 = (~pin_int) & 0xFFFFFFFF
    add(notpin32.to_bytes(4, 'big') + b'\x00\x00\x00\x00', "pin_not32_left", "pin")

    # PIN as signed int64 (negative if > 2^31)
    signed_val = pin_int if pin_int < 2**31 else pin_int - 2**32
    add(struct.pack('>q', signed_val), "pin_signed_be64", "pin")

    # PIN ASCII repeated to fill: "1234567812345678" -> first 8
    pin_rep = (pin_str * 2)[:8].encode('ascii')
    add(pin_rep, "pin_ascii_repeated", "pin")

    # PIN XOR'd with itself shifted: pin ^ (pin << 32)
    pin_xor_shift = pin_int ^ (pin_int << 32)
    pin_xor_shift &= 0xFFFFFFFFFFFFFFFF
    add(pin_xor_shift.to_bytes(8, 'big'), "pin_xor_shifted_be64", "pin")

    # PIN as two BE16 halves + 4 zero bytes
    h1 = int(pin_str[:4])
    h2 = int(pin_str[4:])
    add(h1.to_bytes(2, 'big') + h2.to_bytes(2, 'big') + b'\x00\x00\x00\x00',
        "pin_2xbe16_left", "pin")

    # PIN as two BE32 halves
    add(h1.to_bytes(4, 'big') + h2.to_bytes(4, 'big'), "pin_2xbe32", "pin")

    # First 4 digits as BE32 + last 4 as BE32
    add(int(pin_str[:4]).to_bytes(4, 'big') + int(pin_str[4:]).to_bytes(4, 'big'),
        "pin_halves_2xbe32", "pin")

    # =========================================================================
    # Category: Hash-based interpretations
    # =========================================================================

    pin_ascii = pin_str.encode('ascii')
    pin_be32 = pin_int.to_bytes(4, 'big')
    pin_be64 = pin_int.to_bytes(8, 'big')

    # SHA-256
    add(hashlib.sha256(pin_ascii).digest()[:8], "sha256_pin_ascii_8", "hash")
    add(hashlib.sha256(pin_be32).digest()[:8], "sha256_pin_be32_8", "hash")
    add(hashlib.sha256(pin_be64).digest()[:8], "sha256_pin_be64_8", "hash")

    # MD5
    add(hashlib.md5(pin_ascii).digest()[:8], "md5_pin_ascii_8", "hash")
    add(hashlib.md5(pin_be32).digest()[:8], "md5_pin_be32_8", "hash")

    # SHA-1
    add(hashlib.sha1(pin_ascii).digest()[:8], "sha1_pin_ascii_8", "hash")
    add(hashlib.sha1(pin_be32).digest()[:8], "sha1_pin_be32_8", "hash")

    # HMAC-SHA256 with skey1 (skey1 is loaded but NEVER used in open-rp!)
    add(hmac.new(SKEY1, pin_ascii, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey1_pin_ascii_8", "hash")
    add(hmac.new(SKEY1, pin_be32, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey1_pin_be32_8", "hash")
    add(hmac.new(SKEY1, pin_be64, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey1_pin_be64_8", "hash")

    # HMAC-SHA256 with skey0
    add(hmac.new(SKEY0, pin_ascii, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey0_pin_ascii_8", "hash")
    add(hmac.new(SKEY0, pin_be32, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey0_pin_be32_8", "hash")

    # HMAC-MD5 with skey1
    add(hmac.new(SKEY1, pin_ascii, hashlib.md5).digest()[:8],
        "hmac_md5_skey1_pin_ascii_8", "hash")
    add(hmac.new(SKEY1, pin_be32, hashlib.md5).digest()[:8],
        "hmac_md5_skey1_pin_be32_8", "hash")

    # =========================================================================
    # Category: WiFi passphrase-based
    # =========================================================================

    # First 8 bytes of WiFi passphrase (PSP/PC type)
    add(WIFI_PASSPHRASE_PSP_PC[:8], "wifi_psp_pc_first8", "wifi")

    # First 8 bytes of WiFi passphrase (Phone type)
    add(WIFI_PASSPHRASE_PHONE[:8], "wifi_phone_first8", "wifi")

    # SHA-256 of WiFi passphrase
    add(hashlib.sha256(WIFI_PASSPHRASE_PSP_PC).digest()[:8],
        "sha256_wifi_psp_pc_8", "wifi")
    add(hashlib.sha256(WIFI_PASSPHRASE_PHONE).digest()[:8],
        "sha256_wifi_phone_8", "wifi")

    # Last 8 bytes of WiFi passphrase
    add(WIFI_PASSPHRASE_PSP_PC[-8:], "wifi_psp_pc_last8", "wifi")
    add(WIFI_PASSPHRASE_PHONE[-8:], "wifi_phone_last8", "wifi")

    # =========================================================================
    # Category: Hardware / null
    # =========================================================================

    # All zeros already added above; all 0xFF
    add(bytes([0xFF] * 8), "all_ff", "hardware")

    # Null MAC (00:00:00:00:00:00) + 2 zero bytes
    add(bytes(8), "null_mac", "hardware")  # same as all_zeros, but semantically different

    # Sample client MAC padded (generic placeholder)
    add(bytes([0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x00, 0x00]),
        "sample_mac_padded", "hardware")

    # sockaddr_in patterns (AF_INET=2, port=9293=0x244D)
    add(bytes([0x00, 0x02, 0x24, 0x4D, 0x00, 0x00, 0x00, 0x00]),
        "sockaddr_any", "hardware")
    add(bytes([0x00, 0x02, 0x24, 0x4D, 0xC0, 0xA8, 0x01, 0x01]),
        "sockaddr_192_168_1_1", "hardware")
    add(bytes([0x02, 0x00, 0x24, 0x4D, 0x00, 0x00, 0x00, 0x00]),
        "sockaddr_le_any", "hardware")

    # =========================================================================
    # Category: skey1-based (skey1 is loaded but UNUSED — maybe for registration!)
    # =========================================================================

    # skey1 first 8 bytes directly
    add(SKEY1[:8], "skey1_first8", "skey1")

    # skey1 last 8 bytes
    add(SKEY1[8:], "skey1_last8", "skey1")

    # skey1[:8] XOR PIN_be32 (zero-padded to 8)
    pin4 = pin_int.to_bytes(4, 'big')
    xored = bytes(SKEY1[i] ^ (pin4[i] if i < 4 else 0) for i in range(8))
    add(xored, "skey1_8_xor_pin_be32_left", "skey1")

    xored2 = bytes(SKEY1[i] ^ (pin4[i - 4] if i >= 4 else 0) for i in range(8))
    add(xored2, "skey1_8_xor_pin_be32_right", "skey1")

    # skey1[:8] XOR PIN_be64
    xored3 = bytes(SKEY1[i] ^ pin_be64[i] for i in range(8))
    add(xored3, "skey1_8_xor_pin_be64", "skey1")

    # skey1[:8] XOR PIN_ascii
    xored4 = bytes(SKEY1[i] ^ pin_ascii[i] for i in range(min(8, len(pin_ascii))))
    if len(xored4) < 8:
        xored4 += SKEY1[len(xored4):8]
    add(xored4, "skey1_8_xor_pin_ascii", "skey1")

    # skey0 first 8 bytes
    add(SKEY0[:8], "skey0_first8", "skey1")

    # skey2 first 8 bytes
    add(SKEY2[:8], "skey2_first8", "skey1")

    # skey0[:8] XOR skey1[:8]
    add(bytes(SKEY0[i] ^ SKEY1[i] for i in range(8)), "skey0_xor_skey1_8", "skey1")

    # =========================================================================
    # Category: VITA-related (for "PC menu -> VITA crypto" theory)
    # =========================================================================

    # NONCE_XOR_VITA first 8 bytes (maybe reused for reg?)
    add(NONCE_XOR_VITA[:8], "nonce_xor_vita_first8", "vita")
    add(NONCE_XOR_VITA[8:], "nonce_xor_vita_last8", "vita")

    # =========================================================================
    # Category: Constant patterns
    # =========================================================================

    add(bytes([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]), "const_1_left", "const")
    add(bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01]), "const_1_right", "const")
    add(bytes([0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00]), "const_1_mid", "const")
    add(bytes([0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25]), "const_0x25_all", "const")
    add(bytes([0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28]), "const_0x28_all", "const")
    add(bytes([0x2B, 0x2B, 0x2B, 0x2B, 0x2B, 0x2B, 0x2B, 0x2B]), "const_0x2B_all", "const")

    return contexts


# =============================================================================
# DEVICE TYPE CONFIGURATIONS
# =============================================================================

DEVICE_TYPES = [
    {
        "name": "PSP",
        "menu_label": 'PS3 menu "PSP"',
        "client_type": "Phone",
        "note": "PSP menu -> expects Phone client type -> uses Phone crypto",
        "derive_key": derive_key_phone,
        "derive_iv": derive_iv_phone,
        "xor_key": PHONE_XOR,
        "iv_base": PHONE_IV,
        "iv_xor_position": "last 8 bytes",
        "wifi_passphrase": WIFI_PASSPHRASE_PHONE,
    },
    {
        "name": "Phone",
        "menu_label": 'PS3 menu "Mobile Phone"',
        "client_type": "PC",
        "note": "Phone menu -> expects PC client type -> uses PC crypto",
        "derive_key": derive_key_pc,
        "derive_iv": derive_iv_pc,
        "xor_key": PC_XOR,
        "iv_base": PC_IV,
        "iv_xor_position": "first 8 bytes",
        "wifi_passphrase": WIFI_PASSPHRASE_PSP_PC,
    },
    {
        "name": "PC",
        "menu_label": 'PS3 menu "PC"',
        "client_type": "VITA",
        "note": "PC menu -> expects VITA client type? -> crypto UNKNOWN (possibly VITA keys exist)",
        "derive_key": None,  # Unknown — no VITA registration keys found
        "derive_iv": None,
        "xor_key": None,
        "iv_base": None,
        "iv_xor_position": "unknown",
        "wifi_passphrase": WIFI_PASSPHRASE_PSP_PC,
    },
]

# Also test the "un-shifted" direct mappings in case the shift theory is wrong
DEVICE_TYPES_DIRECT = [
    {
        "name": "PSP_direct",
        "menu_label": 'PS3 menu "PSP" (direct, no shift)',
        "client_type": "PSP",
        "note": "Testing: PSP menu uses PSP crypto directly (no type shift)",
        "derive_key": derive_key_psp,
        "derive_iv": derive_iv_psp,
        "xor_key": PSP_XOR,
        "iv_base": PSP_IV,
        "iv_xor_position": "first 8 bytes",
        "wifi_passphrase": WIFI_PASSPHRASE_PSP_PC,
    },
    {
        "name": "Phone_direct",
        "menu_label": 'PS3 menu "Phone" (direct, no shift)',
        "client_type": "Phone",
        "note": "Testing: Phone menu uses Phone crypto directly (no type shift)",
        "derive_key": derive_key_phone,
        "derive_iv": derive_iv_phone,
        "xor_key": PHONE_XOR,
        "iv_base": PHONE_IV,
        "iv_xor_position": "last 8 bytes",
        "wifi_passphrase": WIFI_PASSPHRASE_PHONE,
    },
    {
        "name": "PC_direct",
        "menu_label": 'PS3 menu "PC" (direct, no shift)',
        "client_type": "PC",
        "note": "Testing: PC menu uses PC crypto directly (no type shift)",
        "derive_key": derive_key_pc,
        "derive_iv": derive_iv_pc,
        "xor_key": PC_XOR,
        "iv_base": PC_IV,
        "iv_xor_position": "first 8 bytes",
        "wifi_passphrase": WIFI_PASSPHRASE_PSP_PC,
    },
]


# =============================================================================
# SAMPLE REGISTRATION BODIES
# =============================================================================

def make_sample_body(client_type: str, client_id: str = "0123456789abcdef0123456789abcdef",
                     client_mac: str = "001122334455",
                     client_nickname: str = "TestDevice") -> bytes:
    """Build a sample registration body matching the PS3 expected format."""
    body = (
        f"Client-Type: {client_type}\r\n"
        f"Client-Id: {client_id}\r\n"
        f"Client-Mac: {client_mac}\r\n"
        f"Client-Nickname: {client_nickname}\r\n"
    )
    return body.encode('ascii')


# =============================================================================
# MAIN GENERATION
# =============================================================================

def generate_test_vectors(pin_str: str, include_direct: bool = True):
    """
    Generate all test vectors for all device types and all IV contexts.
    Returns a list of dicts, each containing everything needed for a test.
    """
    contexts = generate_all_contexts(pin_str)

    # Use a fixed km for reproducibility in test vectors
    km = bytes([0x41] * 16)  # deterministic for vector generation

    vectors = []
    device_configs = DEVICE_TYPES + (DEVICE_TYPES_DIRECT if include_direct else [])

    for dev in device_configs:
        if dev["derive_key"] is None:
            # Unknown crypto (e.g., VITA) — skip encryption but note it
            for ctx, label, category in contexts:
                vectors.append({
                    "device": dev["name"],
                    "menu_label": dev["menu_label"],
                    "client_type": dev["client_type"],
                    "note": dev["note"],
                    "context_label": label,
                    "context_category": category,
                    "context_hex": ctx.hex(),
                    "key_hex": "UNKNOWN",
                    "iv_hex": "UNKNOWN",
                    "encrypted_hex": "UNKNOWN",
                    "km_hex": km.hex(),
                    "plaintext_hex": "UNKNOWN",
                    "skipped": True,
                })
            continue

        key = dev["derive_key"](km)
        body = make_sample_body(dev["client_type"])
        padded = pkcs7_pad(body)

        for ctx, label, category in contexts:
            iv = dev["derive_iv"](ctx)
            cipher = AES.new(key, AES.MODE_CBC, iv)
            encrypted = cipher.encrypt(padded)

            vectors.append({
                "device": dev["name"],
                "menu_label": dev["menu_label"],
                "client_type": dev["client_type"],
                "note": dev["note"],
                "context_label": label,
                "context_category": category,
                "context_hex": ctx.hex(),
                "key_hex": key.hex(),
                "iv_hex": iv.hex(),
                "iv_base_hex": dev["iv_base"].hex(),
                "iv_xor_position": dev["iv_xor_position"],
                "encrypted_hex": encrypted.hex(),
                "km_hex": km.hex(),
                "plaintext_hex": padded.hex(),
                "plaintext_ascii": padded.decode('ascii', errors='replace'),
                "skipped": False,
            })

    return vectors


# =============================================================================
# VERIFY MODE — Try decrypting captured data with all IV variants
# =============================================================================

def verify_captured_data(pin_str: str, captured_hex: str):
    """
    Try all IV context variants to decrypt captured registration data.
    The captured data should be the encrypted body (without the trailing km).
    If the captured data includes km appended, the last 16 bytes are km.
    """
    captured = bytes.fromhex(captured_hex)

    # If captured data is > 16 bytes and length-16 is divisible by 16,
    # assume last 16 bytes are km
    if len(captured) > 16 and (len(captured) - 16) % 16 == 0:
        enc_body = captured[:-16]
        km = captured[-16:]
        print(f"Assuming last 16 bytes are km: {km.hex()}")
        print(f"Encrypted body: {len(enc_body)} bytes")
    else:
        enc_body = captured
        km = None
        print(f"No km detected. Encrypted body: {len(enc_body)} bytes")

    if len(enc_body) % 16 != 0:
        print(f"ERROR: Encrypted body length ({len(enc_body)}) is not a multiple of 16!")
        return

    contexts = generate_all_contexts(pin_str)

    # Known response fields to look for
    KNOWN_FIELDS = [
        b"PREMO-Key", b"PREMO-Nonce", b"Client-Type", b"Client-Id",
        b"Client-Mac", b"Client-Nickname", b"HTTP/", b"200 OK",
        b"\r\n", b"PSP", b"Phone", b"PC",
    ]

    results = []
    device_configs = DEVICE_TYPES + DEVICE_TYPES_DIRECT

    for dev in device_configs:
        if dev["derive_key"] is None:
            continue

        if km is not None:
            key = dev["derive_key"](km)
        else:
            # Without km we cannot derive key; skip
            print(f"Skipping {dev['name']}: no km available to derive key")
            continue

        for ctx, label, category in contexts:
            iv = dev["derive_iv"](ctx)
            try:
                cipher = AES.new(key, AES.MODE_CBC, iv)
                decrypted = cipher.decrypt(enc_body)

                # Score: count how many known fields appear
                score = 0
                for field in KNOWN_FIELDS:
                    if field in decrypted:
                        score += 1

                # Check if it looks like ASCII text
                ascii_ratio = sum(1 for b in decrypted if 0x20 <= b <= 0x7E or b in (0x0D, 0x0A)) / len(decrypted)

                if score >= 2 or ascii_ratio > 0.8:
                    results.append({
                        "device": dev["name"],
                        "context_label": label,
                        "context_hex": ctx.hex(),
                        "key_hex": key.hex(),
                        "iv_hex": iv.hex(),
                        "score": score,
                        "ascii_ratio": ascii_ratio,
                        "decrypted_ascii": decrypted.decode('ascii', errors='replace'),
                        "decrypted_hex": decrypted.hex(),
                    })
            except Exception:
                pass

    if results:
        # Sort by score descending
        results.sort(key=lambda r: (-r["score"], -r["ascii_ratio"]))
        print(f"\n{'='*70}")
        print(f"POTENTIAL MATCHES FOUND: {len(results)}")
        print(f"{'='*70}")
        for r in results:
            print(f"\n  Device: {r['device']}")
            print(f"  Context: {r['context_label']} ({r['context_hex']})")
            print(f"  Key: {r['key_hex']}")
            print(f"  IV:  {r['iv_hex']}")
            print(f"  Score: {r['score']} field matches, {r['ascii_ratio']:.1%} ASCII")
            print(f"  Decrypted: {r['decrypted_ascii'][:200]}")
    else:
        print(f"\nNo matches found across {len(contexts)} contexts x {sum(1 for d in device_configs if d['derive_key'])} device types")
        print(f"Total attempts: {len(contexts) * sum(1 for d in device_configs if d['derive_key'])}")


# =============================================================================
# OUTPUT FORMATTERS
# =============================================================================

def print_text_output(vectors, pin_str):
    """Print human-readable output."""
    print(f"{'='*78}")
    print(f"PS3 Remote Play Registration — IV Context Test Vectors")
    print(f"PIN: {pin_str}")
    print(f"Total vectors: {len(vectors)}")
    print(f"{'='*78}\n")

    # Print static key reference
    print("STATIC KEYS REFERENCE:")
    print(f"  SKEY0: {SKEY0.hex()}")
    print(f"  SKEY1: {SKEY1.hex()}  <-- loaded but UNUSED in open-rp!")
    print(f"  SKEY2: {SKEY2.hex()}")
    print(f"  PSP_XOR:   {PSP_XOR.hex()}")
    print(f"  PSP_IV:    {PSP_IV.hex()}")
    print(f"  PHONE_XOR: {PHONE_XOR.hex()}")
    print(f"  PHONE_IV:  {PHONE_IV.hex()}")
    print(f"  PC_XOR:    {PC_XOR.hex()}")
    print(f"  PC_IV:     {PC_IV.hex()}")
    print()

    # Group by device
    current_device = None
    for v in vectors:
        if v["device"] != current_device:
            current_device = v["device"]
            print(f"\n{'='*78}")
            print(f"DEVICE: {v['device']}  |  {v['menu_label']}")
            print(f"Client-Type header: {v['client_type']}")
            print(f"Note: {v['note']}")
            if not v["skipped"]:
                print(f"IV XOR position: {v.get('iv_xor_position', 'N/A')}")
                print(f"IV base: {v.get('iv_base_hex', 'N/A')}")
                print(f"Derived key (km=41*16): {v['key_hex']}")
            print(f"{'='*78}")

            if v["skipped"]:
                print("  ** SKIPPED: Crypto keys unknown for this device type **\n")
                continue

            # Print category headers
            current_category = None

        if v["skipped"]:
            continue

        if v["context_category"] != current_category:
            current_category = v["context_category"]
            cat_labels = {
                "pin": "PIN-based",
                "hash": "Hash-based",
                "wifi": "WiFi passphrase",
                "hardware": "Hardware / null",
                "skey1": "Session key (skey) based",
                "vita": "VITA-related",
                "const": "Constant patterns",
            }
            print(f"\n  --- {cat_labels.get(current_category, current_category)} ---")

        print(f"  {v['context_label']:40s} ctx={v['context_hex']}  iv={v['iv_hex']}")


def print_json_output(vectors, pin_str):
    """Print JSON output for programmatic use."""
    output = {
        "pin": pin_str,
        "total_vectors": len(vectors),
        "static_keys": {
            "skey0": SKEY0.hex(),
            "skey1": SKEY1.hex(),
            "skey2": SKEY2.hex(),
            "psp_xor": PSP_XOR.hex(),
            "psp_iv": PSP_IV.hex(),
            "phone_xor": PHONE_XOR.hex(),
            "phone_iv": PHONE_IV.hex(),
            "pc_xor": PC_XOR.hex(),
            "pc_iv": PC_IV.hex(),
        },
        "vectors": vectors,
    }
    print(json.dumps(output, indent=2))


def print_summary(vectors, pin_str):
    """Print summary counts."""
    contexts = generate_all_contexts(pin_str)
    print(f"PIN: {pin_str}")
    print(f"Unique IV context values: {len(contexts)}")

    # Count by category
    from collections import Counter
    cat_counts = Counter(c[2] for c in contexts)
    print(f"\nBy category:")
    for cat, count in sorted(cat_counts.items()):
        print(f"  {cat:20s}: {count}")

    # Count by device type
    active = sum(1 for d in DEVICE_TYPES if d["derive_key"] is not None)
    direct = sum(1 for d in DEVICE_TYPES_DIRECT if d["derive_key"] is not None)
    print(f"\nDevice types (shifted):  {len(DEVICE_TYPES)} ({active} with known crypto)")
    print(f"Device types (direct):   {len(DEVICE_TYPES_DIRECT)} ({direct} with known crypto)")
    print(f"\nTotal test vectors: {len(contexts)} contexts x {active + direct} device types = {len(contexts) * (active + direct)}")
    print(f"  (plus {len(contexts)} skipped for unknown VITA crypto)")


# =============================================================================
# ENTRY POINT
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="PS3 Remote Play Registration — IV Context Test Vector Generator",
        epilog=(
            "Examples:\n"
            "  %(prog)s 12345678                     Generate all vectors for PIN 12345678\n"
            "  %(prog)s 12345678 --json               JSON output\n"
            "  %(prog)s 12345678 --summary             Just show counts\n"
            "  %(prog)s 12345678 --verify AABB...      Try decrypting captured data\n"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("pin", help="8-digit PIN from PS3 registration screen")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    parser.add_argument("--summary", action="store_true", help="Print summary counts only")
    parser.add_argument("--verify", metavar="HEXDATA",
                        help="Hex-encoded captured registration data to try decrypting")
    parser.add_argument("--no-direct", action="store_true",
                        help="Skip direct (un-shifted) device type tests")

    args = parser.parse_args()

    # Validate PIN
    if not args.pin.isdigit() or len(args.pin) != 8:
        print(f"ERROR: PIN must be exactly 8 digits, got: {args.pin!r}")
        sys.exit(1)

    if args.verify:
        verify_captured_data(args.pin, args.verify)
        return

    if args.summary:
        print_summary(generate_test_vectors(args.pin, not args.no_direct), args.pin)
        return

    vectors = generate_test_vectors(args.pin, not args.no_direct)

    if args.json:
        print_json_output(vectors, args.pin)
    else:
        print_text_output(vectors, args.pin)

        # Print final summary
        print(f"\n{'='*78}")
        print(f"TOTAL: {len(vectors)} test vectors generated")
        active = sum(1 for v in vectors if not v["skipped"])
        skipped = sum(1 for v in vectors if v["skipped"])
        print(f"  Active (can encrypt): {active}")
        print(f"  Skipped (unknown crypto): {skipped}")
        print(f"{'='*78}")


if __name__ == "__main__":
    main()
