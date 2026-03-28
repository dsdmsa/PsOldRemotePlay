#!/usr/bin/env python3
"""
PS3 Remote Play Registration — v2 with ALL THREE key derivation formulas.

DISCOVERY FROM RUNTIME MEMORY DUMP (2026-03-28):
  The PS3 firmware has THREE different key derivation paths in sysconf_plugin,
  one for each platform type. Each uses a different formula, constant, and key set.

  The branch at VA 0x100478 checks r25 (platform type):
    type 1 → PSP path  (Loop 0): (mat XOR key) - cnt - 0x25
    type 2 → Phone path (Loop 1): (mat - cnt - 0x28) XOR key
    else   → PC path   (Loop 2): (mat XOR key) - cnt - 0x2B

Usage:
  python3 ps3_register_v2.py <ps3_ip> <pin> [platform_type]

  platform_type: 1=PSP, 2=Phone(default), 3=PC
"""

import socket
import struct
import sys
import os
from Crypto.Cipher import AES

PS3_IP = sys.argv[1] if len(sys.argv) > 1 else "192.168.1.80"
PIN = sys.argv[2] if len(sys.argv) > 2 else "20883691"
PLATFORM = int(sys.argv[3]) if len(sys.argv) > 3 else 2  # Default to Phone

PORT = 9293

# =========================================================================
# ALL SIX REGISTRATION CRYPTO KEYS (extracted from runtime memory at
# sysconf_plugin base 0x01B60000, ELF VA 0x150EA0-0x150EFF)
# =========================================================================

CRYPTO_KEYS = {
    # Platform type 1 (PSP) — Loop 0
    1: {
        "name": "PSP",
        "xor_key":  bytes([0xFD, 0xC3, 0xF6, 0xA6, 0x4D, 0x2A, 0xBA, 0x7A,
                           0x38, 0x92, 0x6C, 0xBC, 0x34, 0x31, 0xE1, 0x0E]),
        "iv_base":  bytes([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D,
                           0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64]),
        "constant": 0x25,  # 37 decimal
        "formula": "xor_first",  # (mat XOR key) - cnt - const
    },
    # Platform type 2 (Phone) — Loop 1
    2: {
        "name": "Phone",
        "xor_key":  bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                           0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6]),
        "iv_base":  bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                           0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A]),
        "constant": 0x28,  # 40 decimal
        "formula": "sub_first",  # (mat - cnt - const) XOR key
    },
    # Platform type 3 (PC) — Loop 2
    3: {
        "name": "PC",
        "xor_key":  bytes([0xEC, 0x6D, 0x70, 0x6B, 0x1E, 0x0A, 0x9A, 0x75,
                           0x8C, 0xDA, 0x78, 0x27, 0x51, 0xA3, 0xC3, 0x7B]),
        "iv_base":  bytes([0x5B, 0x64, 0x40, 0xC5, 0x2E, 0x74, 0xC0, 0x46,
                           0x48, 0x72, 0xC9, 0xC5, 0x49, 0x0C, 0x79, 0x04]),
        "constant": 0x2B,  # 43 decimal
        "formula": "xor_first",  # (mat XOR key) - cnt - const
    },
}


def derive_key(material: bytes, platform_type: int) -> bytes:
    """Derive the AES key from 16 bytes of key material using the platform-specific formula."""
    params = CRYPTO_KEYS[platform_type]
    xor_key = params["xor_key"]
    const = params["constant"]
    formula = params["formula"]

    result = bytearray(16)
    for i in range(16):
        mat = material[i]
        key = xor_key[i]

        if formula == "sub_first":
            # Phone (type 2): subtract first, then XOR
            # result = (material - counter - constant) XOR key
            val = (mat - i - const) & 0xFF
            val = val ^ key
        else:  # xor_first
            # PSP (type 1) and PC (type 3): XOR first, then subtract
            # result = (material XOR key) - counter - constant
            val = mat ^ key
            val = (val - i - const) & 0xFF

        result[i] = val

    return bytes(result)


def get_iv(platform_type: int) -> bytes:
    """Get the IV base for the given platform type."""
    return CRYPTO_KEYS[platform_type]["iv_base"]


def build_registration_body(device_id: bytes, device_mac: bytes, device_name: str,
                            client_type: str, key_material: bytes = None) -> tuple:
    """Build the plaintext body for registration, returns (body, key_material)."""
    # For Phone type, the client generates 16 random bytes as key material
    if key_material is None:
        key_material = os.urandom(16)

    # Build plaintext body
    body_lines = []
    body_lines.append(f"Client-Type: {client_type}")
    body_lines.append(f"Client-Id: {device_id.hex()}")
    body_lines.append(f"Client-Mac: {device_mac.hex()}")
    body_lines.append(f"Client-Nickname: {device_name}")

    plaintext = "\r\n".join(body_lines) + "\r\n"

    # Pad to AES block size (16 bytes)
    pt_bytes = plaintext.encode("ascii")
    pad_len = 16 - (len(pt_bytes) % 16)
    pt_bytes += bytes([pad_len] * pad_len)  # PKCS7 padding

    return pt_bytes, key_material


def attempt_registration(ps3_ip: str, pin: str, platform_type: int):
    """Attempt a single registration with the given platform type."""
    params = CRYPTO_KEYS[platform_type]
    print(f"\n{'='*60}")
    print(f"REGISTRATION ATTEMPT — Platform: {params['name']} (type {platform_type})")
    print(f"  Formula: {params['formula']}, constant: 0x{params['constant']:02X}")
    print(f"  XOR key: {params['xor_key'].hex()}")
    print(f"  IV base: {params['iv_base'].hex()}")
    print(f"{'='*60}")

    # WiFi password = PIN halves swapped
    wifi_pw = pin[4:] + pin[:4]
    print(f"  PIN: {pin}, WiFi password: {wifi_pw}")

    # Generate key material (16 random bytes for Phone type)
    key_material = os.urandom(16)
    print(f"  Key material: {key_material.hex()}")

    # Derive AES key
    aes_key = derive_key(key_material, platform_type)
    print(f"  Derived AES key: {aes_key.hex()}")

    # Get IV
    iv = get_iv(platform_type)
    print(f"  AES IV: {iv.hex()}")

    # Device info
    device_id = bytes.fromhex("00112233445566778899AABBCCDDEEFF")
    device_mac = bytes.fromhex("001122334455")
    device_name = "PsOldRemotePlay"

    # Client-Type depends on platform:
    # PS3 menu "Phone" → Client-Type "PC" in the body (shifted mapping!)
    # PS3 menu "PC" → Client-Type "Phone" or "PC"?
    client_type_map = {
        1: "PSP",     # PSP type
        2: "PC",      # Phone → sends "PC" (the shifted mapping)
        3: "Phone",   # PC → sends "Phone" (or maybe "PC"?)
    }

    # Try both the mapped type and direct type
    for ct_label, ct_value in [(f"mapped ({client_type_map[platform_type]})", client_type_map[platform_type]),
                                ("direct (Phone)", "Phone"),
                                ("direct (PC)", "PC")]:
        plaintext, _ = build_registration_body(device_id, device_mac, device_name, ct_value, key_material)

        print(f"\n  Trying Client-Type: {ct_label}")
        print(f"  Plaintext ({len(plaintext)} bytes):")
        print(f"    {plaintext[:80]}")

        # Encrypt with AES-128-CBC
        cipher = AES.new(aes_key, AES.MODE_CBC, iv)
        encrypted = cipher.encrypt(plaintext)

        # Build HTTP request
        # For Phone type, append the key material unencrypted
        body = encrypted
        if platform_type == 2:  # Phone
            body = encrypted + key_material

        request = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(body)}\r\n\r\n"
        request_bytes = request.encode("ascii") + body

        print(f"  Encrypted body: {encrypted[:32].hex()}...")
        print(f"  Total body: {len(body)} bytes (encrypted={len(encrypted)} + material={len(key_material) if platform_type == 2 else 0})")

        # Send to PS3
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(10)
            sock.connect((ps3_ip, PORT))
            sock.send(request_bytes)

            response = sock.recv(4096)
            resp_text = response.decode("ascii", errors="replace")
            sock.close()

            print(f"\n  Response ({len(response)} bytes):")
            for line in resp_text.split("\r\n")[:10]:
                print(f"    {line}")

            if "200 OK" in resp_text:
                print(f"\n  *** SUCCESS! ***")
                return True
            elif "403" in resp_text:
                print(f"  → 403 Forbidden (key derivation failed)")
            else:
                print(f"  → Unexpected response")

        except socket.timeout:
            print(f"  → Timeout (no response)")
        except Exception as e:
            print(f"  → Error: {e}")

    return False


def main():
    print("PS3 Remote Play Registration v2")
    print(f"Target: {PS3_IP}:{PORT}")
    print(f"PIN: {PIN}")
    print(f"Requested platform: {PLATFORM}")
    print()

    # Check connectivity
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(3)
        sock.connect((PS3_IP, PORT))
        sock.close()
        print(f"[+] Port {PORT} is OPEN")
    except:
        print(f"[!] Cannot connect to {PS3_IP}:{PORT}")
        print("    Make sure PS3 is in registration mode (showing PIN)")
        return

    # Try the requested platform type
    if attempt_registration(PS3_IP, PIN, PLATFORM):
        return

    # If that failed, try all platform types
    print(f"\n\nPlatform {PLATFORM} failed. Trying all types...")
    for pt in [1, 2, 3]:
        if pt == PLATFORM:
            continue
        if attempt_registration(PS3_IP, PIN, pt):
            return

    print("\n\n[!] All platform types failed.")
    print("    The key derivation or body format may still have issues.")
    print("    Possible remaining problems:")
    print("    1. The Client-Type mapping might be different")
    print("    2. There might be additional fields in the body")
    print("    3. The PKCS7 padding might be wrong (try zero-padding)")
    print("    4. The key material might need special formatting")
    print("    5. The IV might be XOR'd with something before use")


if __name__ == "__main__":
    main()
