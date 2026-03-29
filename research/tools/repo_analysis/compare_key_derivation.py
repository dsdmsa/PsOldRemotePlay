#!/usr/bin/env python3
"""
compare_key_derivation.py — Compare PS3 and PS4 registration key derivation

Implements key derivation for all 3 PS3 device types (PSP, Phone, PC) and
PS4 patterns (from chiaki-ng). Shows side-by-side comparison and tests
various IV context interpretations.

Usage: ./compare_key_derivation.py [pin]
  pin: 4-8 digit PIN (default: 12345678)
"""

import sys
import struct
import hashlib
import hmac

# ============================================================
# PS3 STATIC KEYS (from PremoConstants.kt / Ghidra)
# ============================================================

PS3_KEYS = {
    'PSP': {
        'xor_key': bytes([0xFD, 0xC3, 0xF6, 0xA6, 0x4D, 0x2A, 0xBA, 0x7A,
                          0x38, 0x92, 0x6C, 0xBC, 0x34, 0x31, 0xE1, 0x0E]),
        'iv_base': bytes([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D,
                          0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64]),
        'key_formula': 'key[i] = (km[i] ^ XOR[i]) - i - 0x25',
        'iv_xor_range': 'first 8 bytes',   # bytes 0..7
        'key_subtract_base': 0x25,
        'key_xor_first': True,  # XOR before subtract
    },
    'Phone': {
        'xor_key': bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                          0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6]),
        'iv_base': bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                          0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A]),
        'key_formula': 'key[i] = (km[i] - i - 0x28) ^ XOR[i]',
        'iv_xor_range': 'last 8 bytes',    # bytes 8..15
        'key_subtract_base': 0x28,
        'key_xor_first': False,  # subtract before XOR
    },
    'PC': {
        'xor_key': bytes([0xEC, 0x6D, 0x70, 0x6B, 0x1E, 0x0A, 0x9A, 0x75,
                          0x8C, 0xDA, 0x78, 0x27, 0x51, 0xA3, 0xC3, 0x7B]),
        'iv_base': bytes([0x5B, 0x64, 0x40, 0xC5, 0x2E, 0x74, 0xC0, 0x46,
                          0x48, 0x72, 0xC9, 0xC5, 0x49, 0x0C, 0x79, 0x04]),
        'key_formula': 'key[i] = (km[i] + i + 0x29) ^ XOR[i]',
        'iv_xor_range': 'first 8 bytes',   # bytes 0..7
        'key_subtract_base': -0x29,  # add, not subtract
        'key_xor_first': False,  # add before XOR
    },
}

# Session keys (for reference)
PS3_SESSION_KEYS = {
    'SKEY0': bytes([0xD1, 0xB2, 0x12, 0xEB, 0x73, 0x86, 0x6C, 0x7B,
                    0x12, 0xA7, 0x5E, 0x0C, 0x04, 0xC6, 0xB8, 0x91]),
    'SKEY1': bytes([0x1F, 0xD5, 0xB9, 0xFA, 0x71, 0xB8, 0x96, 0x81,
                    0xB2, 0x87, 0x92, 0xE2, 0x6F, 0x38, 0xC3, 0x6F]),
    'SKEY2': bytes([0x65, 0x2D, 0x8C, 0x90, 0xDE, 0x87, 0x17, 0xCF,
                    0x4F, 0xB3, 0xD8, 0xD3, 0x01, 0x79, 0x6B, 0x59]),
}


# ============================================================
# PS3 KEY DERIVATION
# ============================================================

def ps3_derive_key(device_type: str, km: bytes) -> bytes:
    """Derive AES key from key material for a PS3 device type."""
    info = PS3_KEYS[device_type]
    xor_key = info['xor_key']
    base = info['key_subtract_base']
    key = bytearray(16)

    for i in range(16):
        if device_type == 'PSP':
            # key[i] = (km[i] ^ XOR[i]) - i - 0x25
            key[i] = ((km[i] ^ xor_key[i]) - i - 0x25) & 0xFF
        elif device_type == 'Phone':
            # key[i] = (km[i] - i - 0x28) ^ XOR[i]
            key[i] = ((km[i] - i - 0x28) & 0xFF) ^ xor_key[i]
        elif device_type == 'PC':
            # key[i] = (km[i] + i + 0x29) ^ XOR[i]
            key[i] = ((km[i] + i + 0x29) & 0xFF) ^ xor_key[i]

    return bytes(key)


def ps3_derive_iv(device_type: str, ctx8: bytes) -> bytes:
    """Derive AES IV from IV base and 8-byte context value."""
    iv = bytearray(PS3_KEYS[device_type]['iv_base'])

    if device_type == 'PSP':
        # XOR first 8 bytes
        for i in range(8):
            iv[i] ^= ctx8[i]
    elif device_type == 'Phone':
        # XOR last 8 bytes
        for i in range(8):
            iv[8 + i] ^= ctx8[i]
    elif device_type == 'PC':
        # XOR first 8 bytes
        for i in range(8):
            iv[i] ^= ctx8[i]

    return bytes(iv)


# ============================================================
# PS4 KEY DERIVATION (from chiaki-ng patterns)
# ============================================================

def ps4_derive_handshake_key(nonce: bytes, morning: bytes) -> bytes:
    """
    PS4 (chiaki-ng): HMAC-SHA256 based key derivation.
    key = HMAC-SHA256(morning, nonce)[:16]
    """
    h = hmac.new(morning, nonce, hashlib.sha256).digest()
    return h[:16]


def ps4_derive_regist_key(pin: str, rpkey: bytes) -> bytes:
    """
    PS4 registration key derivation (chiaki pattern):
    Uses PIN + static key to derive AES key via HMAC/SHA.
    This is a simplified model based on chiaki-ng source.
    """
    pin_bytes = pin.encode('ascii')
    # chiaki uses: key = HMAC-SHA256(rpkey, pin_bytes)[:16]
    h = hmac.new(rpkey, pin_bytes, hashlib.sha256).digest()
    return h[:16]


# ============================================================
# IV CONTEXT INTERPRETATIONS
# ============================================================

def generate_iv_contexts(pin_str: str) -> list:
    """Generate all plausible 8-byte IV context values from PIN."""
    pin_int = int(pin_str)
    contexts = []

    # 1. All zeros (baseline)
    contexts.append(('zeros', bytes(8)))

    # 2. PIN as big-endian int64
    contexts.append(('PIN BE i64', pin_int.to_bytes(8, 'big')))

    # 3. PIN as little-endian int64
    contexts.append(('PIN LE i64', pin_int.to_bytes(8, 'little')))

    # 4. PIN as BE i32 left-padded
    contexts.append(('PIN BE i32 left', b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'big')))

    # 5. PIN as BE i32 right-padded
    contexts.append(('PIN BE i32 right', pin_int.to_bytes(4, 'big') + b'\x00\x00\x00\x00'))

    # 6. PIN as LE i32 left-padded
    contexts.append(('PIN LE i32 left', b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'little')))

    # 7. PIN as LE i32 right-padded
    contexts.append(('PIN LE i32 right', pin_int.to_bytes(4, 'little') + b'\x00\x00\x00\x00'))

    # 8. PIN ASCII bytes zero-padded to 8
    pin_ascii = pin_str.encode('ascii')
    padded = pin_ascii.ljust(8, b'\x00')
    contexts.append(('PIN ASCII left', padded[:8]))

    # 9. PIN ASCII right-padded
    padded_r = pin_ascii.rjust(8, b'\x00')
    contexts.append(('PIN ASCII right', padded_r[:8]))

    # 10. PIN as 2x BE i32 (repeated)
    if pin_int <= 0xFFFFFFFF:
        be32 = pin_int.to_bytes(4, 'big')
        contexts.append(('PIN BE i32 x2', be32 + be32))

    # 11. SHA256(PIN)[:8]
    h = hashlib.sha256(pin_str.encode('ascii')).digest()
    contexts.append(('SHA256(PIN)[:8]', h[:8]))

    # 12. MD5(PIN)[:8]
    h = hashlib.md5(pin_str.encode('ascii')).digest()
    contexts.append(('MD5(PIN)[:8]', h[:8]))

    # 13. PIN bytes reversed
    contexts.append(('PIN ASCII rev', pin_ascii[::-1].ljust(8, b'\x00')[:8]))

    # 14. Each digit as a byte
    digit_bytes = bytes([int(d) for d in pin_str])
    contexts.append(('PIN digits', digit_bytes.ljust(8, b'\x00')[:8]))

    # 15. PIN as BCD
    bcd = bytes([int(pin_str[i:i+2], 10) for i in range(0, len(pin_str)-1, 2)])
    contexts.append(('PIN BCD', bcd.ljust(8, b'\x00')[:8]))

    # 16. PIN XOR'd with itself shifted
    if len(pin_ascii) >= 4:
        xored = bytes([pin_ascii[i % len(pin_ascii)] ^ pin_ascii[(i+1) % len(pin_ascii)]
                        for i in range(8)])
        contexts.append(('PIN XOR shift', xored))

    return contexts


def hex_str(data: bytes) -> str:
    """Format bytes as hex string."""
    return ' '.join(f'{b:02X}' for b in data)


def main():
    pin = sys.argv[1] if len(sys.argv) > 1 else '12345678'

    if pin in ('-h', '--help'):
        print(__doc__.strip())
        sys.exit(0)

    print(f"{'='*70}")
    print(f"PS3/PS4 Registration Key Derivation Comparison")
    print(f"{'='*70}")
    print(f"PIN: {pin}")
    print()

    # Sample key material (would come from PS3 in real handshake)
    km = bytes(range(16))  # Placeholder
    print(f"Sample KM (key material): {hex_str(km)}")
    print()

    # ============================================================
    # PS3 Key Derivation — All 3 device types
    # ============================================================
    print(f"{'='*70}")
    print(f"PS3 KEY DERIVATION")
    print(f"{'='*70}")

    for dtype in ['PSP', 'Phone', 'PC']:
        info = PS3_KEYS[dtype]
        key = ps3_derive_key(dtype, km)
        print(f"\n  --- {dtype} ---")
        print(f"  Formula:   {info['key_formula']}")
        print(f"  XOR key:   {hex_str(info['xor_key'])}")
        print(f"  IV base:   {hex_str(info['iv_base'])}")
        print(f"  IV region: {info['iv_xor_range']}")
        print(f"  Derived key: {hex_str(key)}")

    # ============================================================
    # PS4 Key Derivation (chiaki-ng pattern)
    # ============================================================
    print(f"\n{'='*70}")
    print(f"PS4 KEY DERIVATION (chiaki-ng pattern)")
    print(f"{'='*70}")
    print(f"  Method: HMAC-SHA256 based")
    print(f"  PS4 uses PIN + static RP key -> HMAC-SHA256 -> AES key")
    print(f"  (No per-byte XOR/subtract like PS3)")

    # Demonstrate with placeholder
    rpkey_placeholder = bytes(range(16, 32))
    ps4_key = ps4_derive_regist_key(pin, rpkey_placeholder)
    print(f"  Sample RP key:  {hex_str(rpkey_placeholder)}")
    print(f"  Derived key:    {hex_str(ps4_key)}")

    # ============================================================
    # Side-by-side comparison
    # ============================================================
    print(f"\n{'='*70}")
    print(f"COMPARISON: PS3 vs PS4")
    print(f"{'='*70}")
    print(f"{'Feature':<25} {'PS3':<25} {'PS4':<25}")
    print(f"{'-'*25} {'-'*25} {'-'*25}")
    print(f"{'Key derivation':<25} {'Per-byte XOR+math':<25} {'HMAC-SHA256':<25}")
    print(f"{'IV derivation':<25} {'Base XOR ctx[8]':<25} {'Random/Nonce':<25}")
    print(f"{'Cipher':<25} {'AES-128-CBC':<25} {'AES-128-CBC/GCM':<25}")
    print(f"{'PIN usage':<25} {'IV context (8B)':<25} {'Key input (HMAC)':<25}")
    print(f"{'Static keys':<25} {'3 per device type':<25} {'1 RP key':<25}")
    print(f"{'Device types':<25} {'PSP/Phone/PC':<25} {'PS4/PS5':<25}")

    # ============================================================
    # IV Context test vectors
    # ============================================================
    print(f"\n{'='*70}")
    print(f"IV CONTEXT TEST VECTORS (PIN={pin})")
    print(f"{'='*70}")

    contexts = generate_iv_contexts(pin)

    for dtype in ['PSP', 'Phone', 'PC']:
        print(f"\n  --- {dtype} ---")
        print(f"  {'Context':<22} {'8-byte value':<26} {'Derived IV'}")
        print(f"  {'-'*22} {'-'*26} {'-'*48}")
        for name, ctx in contexts:
            iv = ps3_derive_iv(dtype, ctx)
            print(f"  {name:<22} {hex_str(ctx):<26} {hex_str(iv)}")

    # ============================================================
    # Full test vectors with key + IV
    # ============================================================
    print(f"\n{'='*70}")
    print(f"FULL TEST VECTORS (KM={hex_str(km)}, PIN={pin})")
    print(f"{'='*70}")

    for dtype in ['PSP', 'Phone', 'PC']:
        key = ps3_derive_key(dtype, km)
        print(f"\n  {dtype}:")
        print(f"    Key: {hex_str(key)}")
        # Show IV for top 3 most likely contexts
        for name, ctx in contexts[:7]:
            iv = ps3_derive_iv(dtype, ctx)
            print(f"    IV ({name}): {hex_str(iv)}")


if __name__ == '__main__':
    main()
