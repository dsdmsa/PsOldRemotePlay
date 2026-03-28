#!/usr/bin/env python3
"""
PS3 Remote Play Registration Tool
Run this on a machine connected to the PS3's ad-hoc WiFi.

Usage: python3 ps3_register.py 192.168.1.75 78831915
"""

import socket
import sys
import os
import json
import hashlib
from datetime import datetime

try:
    from Crypto.Cipher import AES
except ImportError:
    print("Installing pycryptodome...")
    os.system("pip3 install pycryptodome")
    from Crypto.Cipher import AES

# ── Static keys from Ghidra reverse engineering ──

# Registration XOR keys (from sysconf_plugin.elf)
REG_XOR_PHONE = bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                       0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6])
REG_IV_PHONE  = bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                       0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A])

REG_XOR_PSP   = bytes([0xFD, 0xC3, 0xF6, 0xA6, 0x4D, 0x2A, 0xBA, 0x7A,
                       0x38, 0x92, 0x6C, 0xBC, 0x34, 0x31, 0xE1, 0x0E])
REG_IV_PSP    = bytes([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D,
                       0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64])

REG_XOR_PC    = bytes([0xEC, 0x6D, 0x70, 0x6B, 0x1E, 0x0A, 0x9A, 0x75,
                       0x8C, 0xDA, 0x78, 0x27, 0x51, 0xA3, 0xC3, 0x7B])
REG_IV_PC     = bytes([0x5B, 0x64, 0x40, 0xC5, 0x2E, 0x74, 0xC0, 0x46,
                       0x48, 0x72, 0xC9, 0xC5, 0x49, 0x0C, 0x79, 0x04])

PORT = 9293

def log(tag, msg):
    ts = datetime.now().strftime("%H:%M:%S.%f")[:-3]
    print(f"[{ts}][{tag}] {msg}")

def derive_key_phone(key_material):
    """Phone: key[i] = (material[i] - i - 0x28) XOR static[i]"""
    key = bytearray(16)
    for i in range(16):
        key[i] = ((key_material[i] - i - 0x28) ^ REG_XOR_PHONE[i]) & 0xFF
    return bytes(key)

def derive_key_psp(key_material):
    """PSP: key[i] = (material[i] XOR static[i]) - i - 0x25"""
    key = bytearray(16)
    for i in range(16):
        key[i] = ((key_material[i] ^ REG_XOR_PSP[i]) - i - 0x25) & 0xFF
    return bytes(key)

def derive_key_pc(key_material):
    """PC: key[i] = (material[i] XOR static[i]) - i - 0x2B"""
    key = bytearray(16)
    for i in range(16):
        key[i] = ((key_material[i] ^ REG_XOR_PC[i]) - i - 0x2B) & 0xFF
    return bytes(key)

def derive_iv(platform, context_bytes=None):
    """Derive IV with optional context XOR"""
    if context_bytes is None:
        context_bytes = bytes(8)

    if platform == "Phone":
        iv = bytearray(REG_IV_PHONE)
        for i in range(8):
            iv[8 + i] ^= context_bytes[i]  # Phone: XOR second 8 bytes
    elif platform == "PSP":
        iv = bytearray(REG_IV_PSP)
        for i in range(8):
            iv[i] ^= context_bytes[i]
    else:  # PC
        iv = bytearray(REG_IV_PC)
        for i in range(8):
            iv[i] ^= context_bytes[i]
    return bytes(iv)

def try_register(ps3_ip, pin, platform, device_id, device_mac, device_name, context_bytes):
    """Attempt registration with given parameters"""
    platform_names = {"Phone": "Phone", "PSP": "PSP", "PC": "PC"}
    pname = platform_names.get(platform, platform)

    log("REG", f"--- Trying platform={pname}, context={context_bytes.hex()} ---")

    # Build plaintext body
    body_text = (
        f"Client-Type: {pname}\r\n"
        f"Client-Id: {device_id.hex()}\r\n"
        f"Client-Mac: {device_mac.hex()}\r\n"
        f"Client-Nickname: {device_name}\r\n"
    )
    log("REG", f"Body: {body_text.strip()}")

    # Generate 16 random bytes as key material
    key_material = os.urandom(16)
    log("REG", f"Key material: {key_material.hex()}")

    # Derive AES key
    if platform == "Phone":
        aes_key = derive_key_phone(key_material)
    elif platform == "PSP":
        aes_key = derive_key_psp(key_material)
    else:
        aes_key = derive_key_pc(key_material)
    log("REG", f"AES key: {aes_key.hex()}")

    # Derive IV
    aes_iv = derive_iv(platform, context_bytes)
    log("REG", f"AES IV: {aes_iv.hex()}")

    # Pad plaintext to 16-byte boundary
    plain_bytes = body_text.encode("ascii")
    pad_len = ((len(plain_bytes) + 15) // 16) * 16
    padded = plain_bytes.ljust(pad_len, b'\x00')
    log("REG", f"Plaintext: {len(plain_bytes)} bytes, padded to {pad_len}")

    # Encrypt
    cipher = AES.new(aes_key, AES.MODE_CBC, aes_iv)
    encrypted = cipher.encrypt(padded)

    # Full body = encrypted + key material (16 bytes appended unencrypted)
    full_body = encrypted + key_material
    log("REG", f"Full body: {len(full_body)} bytes")

    # Send HTTP POST
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((ps3_ip, PORT))

        http_req = (
            f"POST /sce/premo/regist HTTP/1.1\r\n"
            f"Content-Length: {len(full_body)}\r\n"
            f"\r\n"
        ).encode("ascii")

        sock.sendall(http_req + full_body)
        log("REG", "Request sent, waiting for response...")

        # Read response
        response = b""
        while True:
            try:
                chunk = sock.recv(4096)
                if not chunk:
                    break
                response += chunk
                # Check if we have complete response
                if b"\r\n\r\n" in response:
                    hdr_end = response.index(b"\r\n\r\n") + 4
                    headers = response[:hdr_end].decode("ascii", errors="replace")
                    # Find Content-Length
                    cl = 0
                    for line in headers.split("\r\n"):
                        if line.lower().startswith("content-length:"):
                            cl = int(line.split(":")[1].strip())
                    body_received = len(response) - hdr_end
                    if body_received >= cl:
                        break
            except socket.timeout:
                break

        sock.close()
        resp_text = response.decode("ascii", errors="replace")
        log("REG", f"Response ({len(response)} bytes):\n{resp_text[:500]}")
        log("REG", f"Response hex: {response[:200].hex()}")

        if b"200 OK" in response:
            log("REG", "=== GOT 200 OK! ===")

            # Extract body
            if b"\r\n\r\n" in response:
                hdr_end = response.index(b"\r\n\r\n") + 4
                resp_body = response[hdr_end:]
                log("REG", f"Response body: {len(resp_body)} bytes")
                log("REG", f"Body hex: {resp_body.hex()}")

                # Try to decrypt
                if len(resp_body) >= 16:
                    try:
                        decipher = AES.new(aes_key, AES.MODE_CBC, aes_iv)
                        decrypted = decipher.decrypt(resp_body)
                        dec_text = decrypted.decode("ascii", errors="replace")
                        log("REG", f"Decrypted response:\n{dec_text}")

                        # Parse fields
                        result = {}
                        for line in dec_text.split("\r\n"):
                            if ":" in line:
                                k, v = line.split(":", 1)
                                result[k.strip()] = v.strip()
                                log("REG", f"  {k.strip()}: {v.strip()}")

                        return result
                    except Exception as e:
                        log("ERR", f"Decryption failed: {e}")
                        log("REG", "Raw body (might not be encrypted):")
                        log("REG", resp_body.decode("ascii", errors="replace"))

            return {"raw": resp_text}

        elif b"403" in response:
            log("REG", "Got 403 — rejected")
            return None
        else:
            log("REG", "Unexpected response")
            return None

    except Exception as e:
        log("ERR", f"Connection failed: {e}")
        return None


def main():
    if len(sys.argv) < 3:
        print("Usage: python3 ps3_register.py <PS3_IP> <8-digit-PIN>")
        print("Example: python3 ps3_register.py 192.168.1.75 78831915")
        sys.exit(1)

    ps3_ip = sys.argv[1]
    pin = sys.argv[2]

    log("MAIN", f"PS3 IP: {ps3_ip}")
    log("MAIN", f"PIN: {pin}")

    # Generate a device identity
    device_id = os.urandom(16)
    device_mac = os.urandom(6)
    device_name = "PsOldRemotePlay"

    log("MAIN", f"Device ID: {device_id.hex()}")
    log("MAIN", f"Device MAC: {device_mac.hex()}")
    log("MAIN", "")

    # Context byte candidates
    pin_bytes = pin.encode("ascii")[:8].ljust(8, b'\x00')
    pin_int = int(pin) if pin.isdigit() else 0
    pin_be = pin_int.to_bytes(8, "big")

    context_candidates = [
        (bytes(8), "zeros"),
        (pin_bytes, "PIN as ASCII"),
        (pin_be, "PIN as big-endian int"),
        (bytes.fromhex(pin.ljust(16, '0'))[:8], "PIN as hex bytes"),
    ]

    # Try each platform with each context
    platforms = ["Phone", "PSP", "PC"]

    for platform in platforms:
        for ctx_bytes, ctx_name in context_candidates:
            log("MAIN", f"\n{'='*60}")
            log("MAIN", f"Platform: {platform}, Context: {ctx_name} ({ctx_bytes.hex()})")
            log("MAIN", f"{'='*60}")

            result = try_register(ps3_ip, pin, platform, device_id, device_mac, device_name, ctx_bytes)

            if result is not None:
                log("MAIN", "\n" + "!" * 60)
                log("MAIN", "  REGISTRATION SUCCESSFUL!")
                log("MAIN", "!" * 60)

                pkey = result.get("PREMO-Key", "")
                log("MAIN", f"  PREMO-Key (pkey): {pkey}")
                log("MAIN", f"  PS3-Mac: {result.get('PS3-Mac', '')}")
                log("MAIN", f"  PS3-Nickname: {result.get('PS3-Nickname', '')}")
                log("MAIN", f"  AP-Ssid: {result.get('AP-Ssid', '')}")
                log("MAIN", f"  Device ID: {device_id.hex()}")
                log("MAIN", f"  Device MAC: {device_mac.hex()}")

                # Save to file
                save_data = {
                    "pkey": pkey,
                    "device_id": device_id.hex(),
                    "device_mac": device_mac.hex(),
                    "ps3_mac": result.get("PS3-Mac", ""),
                    "ps3_nickname": result.get("PS3-Nickname", ""),
                    "platform": platform,
                    "context": ctx_name,
                    "all_fields": result,
                }
                with open("ps3_registration.json", "w") as f:
                    json.dump(save_data, f, indent=2)
                log("MAIN", "\nSaved to ps3_registration.json")
                log("MAIN", "\nUse these values in the desktop app:")
                log("MAIN", f"  PKey: {pkey}")
                log("MAIN", f"  Device ID: {device_id.hex()}")
                log("MAIN", f"  Device MAC: {device_mac.hex()}")
                return

    log("MAIN", "\n" + "=" * 60)
    log("MAIN", "All combinations failed.")
    log("MAIN", "The registration encryption may need further investigation.")
    log("MAIN", "Check the output above for any 200 OK responses.")
    log("MAIN", "=" * 60)


if __name__ == "__main__":
    main()
