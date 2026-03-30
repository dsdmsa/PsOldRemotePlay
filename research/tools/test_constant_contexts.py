#!/usr/bin/env python3
"""
Priority 2: Test if IV context is a constant value (not PIN-derived).

This script tests multiple candidate constants:
- Static zeros, all 0xFF
- Derived from static crypto keys (SKEY0/1/2)
- Other common constants

Can run in background/overnight while Priority 1 tests the PS4 formula.

Usage:
    python3 test_constant_contexts.py <PS3_IP> <PIN>

Example:
    python3 test_constant_contexts.py 192.168.1.75 12345678
"""

import socket
import struct
import sys
import base64
import time
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

# Static crypto keys from PremoConstants.kt (correct 16-byte values)
SKEY0 = bytes.fromhex("D1B212EB73866C7B12A75E0C04C6B891")
SKEY1 = bytes.fromhex("1FD5B9FA71B89681B28792E26F38C36F")
SKEY2 = bytes.fromhex("652D8C90DE8717CF4FB3D8D301796B59")

# Registration XOR keys per platform (correct 16-byte values)
REG_XOR_PSP = bytes.fromhex("FDC3F6A64D2ABA7A38926CBC3431E10E")
REG_XOR_PHONE = bytes.fromhex("F116F0DA442C06C245B15E48F904E3E6")
REG_XOR_PC = bytes.fromhex("EC6D706B1E0A9A758CDA782751A3C37B")

# Registration IV bases (correct 16-byte values)
REG_IV_PSP = bytearray([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D, 0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64])
REG_IV_PHONE = bytearray([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC, 0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A])
REG_IV_PC = bytearray([0x5B, 0x64, 0x40, 0xC5, 0x2E, 0x74, 0xC0, 0x46, 0x48, 0x72, 0xC9, 0xC5, 0x49, 0x0C, 0x79, 0x04])

# Candidate constants to test
CANDIDATES = {
    "zeros_8": bytes(8),
    "ff_8": bytes([0xFF] * 8),
    "skey0_first8": SKEY0[:8],
    "skey0_last8": SKEY0[-8:],
    "skey1_first8": SKEY1[:8],
    "skey1_last8": SKEY1[-8:],
    "skey2_first8": SKEY2[:8],
    "skey2_last8": SKEY2[-8:],
    "xor_psp_first8": REG_XOR_PSP[:8],
    "xor_phone_first8": REG_XOR_PHONE[:8],
    "xor_pc_first8": REG_XOR_PC[:8],
    "const_0x01_left": bytes([0x01]) + bytes(7),
    "const_0x01_right": bytes(7) + bytes([0x01]),
    "skey1_xor_phone": bytes([SKEY1[i] ^ REG_XOR_PHONE[i] for i in range(8)]),
    "skey2_xor_pc": bytes([SKEY2[i] ^ REG_XOR_PC[i] for i in range(8)]),
}


def derive_key_phone(key_material):
    """Derive AES key for Phone platform (type 1)."""
    key = bytearray(16)
    for i in range(16):
        key[i] = ((key_material[i] - i - 0x28) ^ REG_XOR_PHONE[i]) & 0xFF
    return bytes(key)


def derive_iv_phone(context_bytes):
    """Derive registration IV for Phone platform with given context."""
    base_iv = bytearray(REG_IV_PHONE)
    # XOR second 8 bytes with context
    for i in range(8):
        base_iv[8 + i] ^= context_bytes[i]
    return bytes(base_iv)


def derive_key_psp(key_material):
    """Derive AES key for PSP platform (type 0)."""
    key = bytearray(16)
    for i in range(16):
        key[i] = ((key_material[i] ^ REG_XOR_PSP[i]) - i - 0x25) & 0xFF
    return bytes(key)


def derive_iv_psp(context_bytes):
    """Derive registration IV for PSP platform with given context."""
    base_iv = bytearray(REG_IV_PSP)
    # XOR first 8 bytes with context
    for i in range(8):
        base_iv[i] ^= context_bytes[i]
    return bytes(base_iv)


def derive_key_pc(key_material):
    """Derive AES key for PC platform (type 2)."""
    key = bytearray(16)
    for i in range(16):
        key[i] = ((key_material[i] ^ REG_XOR_PC[i]) - i - 0x2B) & 0xFF
    return bytes(key)


def derive_iv_pc(context_bytes):
    """Derive registration IV for PC platform with given context."""
    base_iv = bytearray(REG_IV_PC)
    # XOR first 8 bytes with context
    for i in range(8):
        base_iv[i] ^= context_bytes[i]
    return bytes(base_iv)


def test_registration(ps3_ip, context_bytes, platform_type=1):
    """
    Send a test registration request with the given IV context.

    Returns: (success, response_code, response_body)
    """
    try:
        # Generate random 16-byte key material
        import os
        key_material = os.urandom(16)

        # Derive key and IV based on platform
        if platform_type == 0:  # PSP
            aes_key = derive_key_psp(key_material)
            aes_iv = derive_iv_psp(context_bytes)
        elif platform_type == 1:  # Phone
            aes_key = derive_key_phone(key_material)
            aes_iv = derive_iv_phone(context_bytes)
        else:  # PC
            aes_key = derive_key_pc(key_material)
            aes_iv = derive_iv_pc(context_bytes)

        # Build plaintext body
        device_id = "010203040506070809000A0B0C0D0E0F"
        device_mac = "AABBCCDDEEFF"
        body_text = (
            f"Client-Type: {'PC' if platform_type == 1 else 'VITA'}\r\n"
            f"Client-Id: {device_id}\r\n"
            f"Client-Mac: {device_mac}\r\n"
            f"Client-Nickname: TestDevice\r\n"
        )

        # Pad and encrypt
        plaintext = body_text.encode('ascii')
        padded_len = ((len(plaintext) + 15) // 16) * 16
        padded = plaintext + b'\x00' * (padded_len - len(plaintext))

        cipher = AES.new(aes_key, AES.MODE_CBC, aes_iv)
        encrypted = cipher.encrypt(padded)

        # Build full body: encrypted + key_material
        full_body = encrypted + key_material

        # Send HTTP POST
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        sock.connect((ps3_ip, 9293))

        http_request = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(full_body)}\r\n\r\n"
        sock.send(http_request.encode('ascii'))
        sock.send(full_body)

        # Read response
        response = b""
        try:
            while True:
                chunk = sock.recv(4096)
                if not chunk:
                    break
                response += chunk
        except socket.timeout:
            pass
        finally:
            sock.close()

        response_text = response.decode('ascii', errors='replace')

        if "200 OK" in response_text:
            return True, 200, response_text
        elif "403" in response_text:
            return False, 403, response_text
        else:
            return False, 0, response_text[:200]

    except Exception as e:
        return False, -1, str(e)


def main():
    if len(sys.argv) < 3:
        print("Usage: python3 test_constant_contexts.py <PS3_IP> <PIN>")
        print("Example: python3 test_constant_contexts.py 192.168.1.75 12345678")
        print("\nWARNING: PS3 locks registration after 3 failed attempts!")
        print("You MUST restart PS3 (power off/on) between test batches.")
        print("Test batches: 1-5, 6-10, 11-15 (need 2 restarts total)")
        sys.exit(1)

    ps3_ip = sys.argv[1]
    pin = sys.argv[2]

    print(f"Testing constant IV contexts on {ps3_ip} with PIN {pin}")
    print("=" * 80)
    print("⚠️  WARNING: PS3 LOCKOUT DETECTION")
    print("   After 3 failed attempts, PS3 stops accepting registration.")
    print("   The script will detect lockout and tell you when to restart PS3.")
    print("=" * 80)

    results = []
    attempt_count = 0
    locked_out = False

    for name, context in CANDIDATES.items():
        attempt_count += 1
        print(f"\n[Attempt {attempt_count}/15] Testing: {name}")
        print(f"  Context: {context.hex()}")

        if locked_out:
            print(f"  ⚠️  SKIPPED (PS3 locked after 3 failures)")
            results.append((name, context, False, "SKIPPED"))
            continue

        # Test with Phone platform (type 1)
        success, code, response = test_registration(ps3_ip, context, platform_type=1)

        print(f"  Result: HTTP {code}")

        if success:
            print(f"  ✅ SUCCESS! This context works!")
            print(f"     Response preview: {response[:300]}")
            results.append((name, context, True, "SUCCESS"))
        elif code == 403:
            print(f"  ❌ 403 Forbidden (wrong encryption/PIN/type)")
            results.append((name, context, False, "403"))
        elif code == -1:
            print(f"  ⚠️  Connection error: {response[:100]}")
            print(f"  ⚠️  PS3 MAY BE LOCKED OUT (after 3 failures)")
            locked_out = True
            results.append((name, context, False, "LOCKOUT"))
        else:
            print(f"  ❌ Other error: {response[:100]}")
            results.append((name, context, False, str(code)))

        if locked_out:
            print("\n" + "=" * 80)
            print("🔒 PS3 REGISTRATION LOCKED (3 failures reached)")
            print("=" * 80)
            print(f"Tested: {attempt_count}/15 contexts")
            print(f"To continue testing:")
            print(f"  1. Power OFF PS3 completely")
            print(f"  2. Wait 10 seconds")
            print(f"  3. Power ON PS3")
            print(f"  4. Enter registration mode again (show PIN)")
            print(f"  5. Run script again to test remaining contexts")
            break

        # Rate limit: 2 second delay between attempts to avoid triggering lockout
        time.sleep(2)

    print("\n" + "=" * 80)
    print("SUMMARY:")
    print("-" * 80)

    successes = [r for r in results if len(r) >= 3 and r[2]]
    if successes:
        print(f"✅ Found {len(successes)} working context(s):")
        for name, context, _, _ in successes:
            print(f"   {name}: {context.hex()}")
    else:
        print("❌ No working constants found in tested batch.")
        if locked_out:
            print(f"   (Only tested {attempt_count}/15 before lockout)")
            print("   Restart PS3 and run script again to test remaining contexts")
        else:
            print("   This suggests IV context is PIN-derived (not constant).")

    print("\nResults table:")
    print("-" * 80)
    for item in results:
        if len(item) == 3:
            name, context, success = item
            status_str = "SUCCESS"
        else:
            name, context, success, status_str = item

        if success:
            status = f"✅ {status_str:8}"
        else:
            status = f"❌ {status_str:8}"
        print(f"{status} {name:25} {context.hex()}")


if __name__ == "__main__":
    main()
