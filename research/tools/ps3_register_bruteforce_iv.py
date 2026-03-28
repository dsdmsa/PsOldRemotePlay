#!/usr/bin/env python3
"""
PS3 Registration — Brute-force IV context value

We now have CONFIRMED from decompiled code:
- Key derivation formulas (verified against PPC assembly)
- Static keys (verified against binary bytes)
- IV base values (verified)
- The IV is XOR'd with 8 bytes from *(*(ctx + 0x38))

This script tries ALL plausible encodings of the PIN as the 8-byte IV context.
For Phone type (PS3 menu "Mobile Phone"):
  - Encryption: Phone keys (DAT_00150ec0/ed0)
  - Client-Type: "PC" (shifted mapping)
  - IV XOR: second 8 bytes of IV base

Usage: python3 ps3_register_bruteforce_iv.py <PS3_IP> <PIN>
"""
import socket, sys, os, struct
from Crypto.Cipher import AES

# VERIFIED keys from binary (DAT_00150ec0, DAT_00150ed0)
PHONE_XOR = bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                   0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6])
PHONE_IV  = bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                   0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A])

# PSP keys (DAT_00150ea0, DAT_00150eb0)
PSP_XOR = bytes([0xFD, 0xC3, 0xF6, 0xA6, 0x4D, 0x2A, 0xBA, 0x7A,
                 0x38, 0x92, 0x6C, 0xBC, 0x34, 0x31, 0xE1, 0x0E])
PSP_IV  = bytes([0x3A, 0x9D, 0xF3, 0x3B, 0x99, 0xCF, 0x9C, 0x0D,
                 0xBF, 0x58, 0x81, 0x12, 0x6C, 0x18, 0x32, 0x64])

DEVICE_ID  = bytes.fromhex("A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6")
DEVICE_MAC = bytes.fromhex("AABBCCDDEEFF")

def derive_key_phone(km):
    """Phone: key[i] = (km[i] - i - 0x28) ^ PHONE_XOR[i]"""
    k = bytearray(16)
    for i in range(16):
        k[i] = ((km[i] - i - 0x28) & 0xFF) ^ PHONE_XOR[i]
    return bytes(k)

def derive_key_psp(km):
    """PSP: key[i] = (km[i] ^ PSP_XOR[i]) - i - 0x25"""
    k = bytearray(16)
    for i in range(16):
        k[i] = ((km[i] ^ PSP_XOR[i]) - i - 0x25) & 0xFF
    return bytes(k)

def derive_iv_phone(ctx8):
    """Phone: XOR second 8 bytes"""
    iv = bytearray(PHONE_IV)
    for i in range(8):
        iv[8 + i] ^= ctx8[i]
    return bytes(iv)

def derive_iv_psp(ctx8):
    """PSP: XOR first 8 bytes"""
    iv = bytearray(PSP_IV)
    for i in range(8):
        iv[i] ^= ctx8[i]
    return bytes(iv)

def generate_contexts(pin_str):
    """Generate all plausible 8-byte IV context values from the PIN"""
    pin_int = int(pin_str)
    contexts = []

    # 1. Zeros (baseline)
    contexts.append((bytes(8), "zeros"))

    # 2. PIN as big-endian longlong (from code analysis)
    contexts.append((pin_int.to_bytes(8, 'big'), "PIN BE longlong"))

    # 3. PIN as little-endian longlong
    contexts.append((pin_int.to_bytes(8, 'little'), "PIN LE longlong"))

    # 4. PIN as big-endian 32-bit, zero-padded left
    contexts.append((b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'big'), "PIN BE32 left-pad"))

    # 5. PIN as big-endian 32-bit, zero-padded right
    contexts.append((pin_int.to_bytes(4, 'big') + b'\x00\x00\x00\x00', "PIN BE32 right-pad"))

    # 6. PIN as little-endian 32-bit, zero-padded right
    contexts.append((pin_int.to_bytes(4, 'little') + b'\x00\x00\x00\x00', "PIN LE32 right-pad"))

    # 7. PIN as little-endian 32-bit, zero-padded left
    contexts.append((b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'little'), "PIN LE32 left-pad"))

    # 8. PIN as ASCII (first 8 chars)
    contexts.append((pin_str.encode()[:8].ljust(8, b'\x00'), "PIN ASCII"))

    # 9. PIN halves swapped as int (e.g., 20883691 -> 36912088)
    swapped = pin_str[4:] + pin_str[:4]
    if swapped.isdigit():
        sw_int = int(swapped)
        contexts.append((sw_int.to_bytes(8, 'big'), "PIN swapped BE"))

    # 10. SSID number (first 4 digits) as big-endian int
    ssid_num = pin_int // 10000
    contexts.append((ssid_num.to_bytes(8, 'big'), "SSID# BE"))

    # 11. SSID number as 4-byte BE + padding
    contexts.append((ssid_num.to_bytes(4, 'big') + b'\x00\x00\x00\x00', "SSID# BE32"))

    # 12. PIN as two 4-digit halves, each as BE16
    h1 = int(pin_str[:4])
    h2 = int(pin_str[4:])
    contexts.append((h1.to_bytes(4, 'big') + h2.to_bytes(4, 'big'), "PIN halves BE32"))

    # 13. PS3 Ethernet MAC (FC:0F:E6:0F:A7:3B) + 2 zero bytes
    ps3_mac = bytes([0xFC, 0x0F, 0xE6, 0x0F, 0xA7, 0x3B])
    contexts.append((ps3_mac + b'\x00\x00', "PS3 MAC+00"))

    # 14. PS3 MAC reversed + 2 zero bytes
    contexts.append((ps3_mac[::-1] + b'\x00\x00', "PS3 MAC rev+00"))

    # 15. PS3 MAC with WiFi offset (+1 on last byte) + 2 zero bytes
    ps3_wifi_mac = ps3_mac[:5] + bytes([ps3_mac[5] + 1])
    contexts.append((ps3_wifi_mac + b'\x00\x00', "PS3 WiFi MAC+00"))

    # 16. sockaddr_in(AF_INET, port=9293, ip=192.168.1.1)
    contexts.append((bytes([0x00, 0x02, 0x24, 0x4D, 0xC0, 0xA8, 0x01, 0x01]), "sockaddr WiFi"))

    # 17. sockaddr_in(AF_INET, port=9293, ip=0.0.0.0)
    contexts.append((bytes([0x00, 0x02, 0x24, 0x4D, 0x00, 0x00, 0x00, 0x00]), "sockaddr ANY"))

    # 18. All 0xFF
    contexts.append((bytes([0xFF] * 8), "all 0xFF"))

    # 19. PIN as signed int (negative if > 2^31)
    signed_val = pin_int if pin_int < 2**31 else pin_int - 2**32
    contexts.append((struct.pack('>q', signed_val), "PIN signed BE64"))

    # 20. First 8 bytes of PIN string repeated
    pin_repeated = (pin_str * 2)[:8].encode()
    contexts.append((pin_repeated, "PIN ASCII repeated"))

    # 21. PS3 MAC first 4 bytes + PIN last 4 digits as bytes
    contexts.append((ps3_mac[:4] + int(pin_str[4:]).to_bytes(4, 'big'), "MAC4+PIN4"))

    # 22. PIN as BCD (binary coded decimal)
    bcd = bytes([int(d) for d in pin_str])
    contexts.append((bcd, "PIN BCD"))

    return contexts

def attempt(ps3_ip, enc_type, client_type, key_fn, iv_fn, ctx8, label, km=None):
    """Try a registration attempt"""
    body = (f"Client-Type: {client_type}\r\n"
            f"Client-Id: {DEVICE_ID.hex()}\r\n"
            f"Client-Mac: {DEVICE_MAC.hex()}\r\n"
            f"Client-Nickname: PsOldRemotePlay\r\n")

    if km is None:
        km = os.urandom(16)
    key = key_fn(km)
    iv = iv_fn(ctx8)

    plain = body.encode("ascii")
    padded = plain + b'\x00' * (16 - len(plain) % 16) if len(plain) % 16 != 0 else plain
    enc = AES.new(key, AES.MODE_CBC, iv).encrypt(padded)
    full = enc + km

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(8)
        s.connect((ps3_ip, 9293))
        req = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(full)}\r\n\r\n"
        s.sendall(req.encode() + full)

        resp = b""
        while True:
            try:
                c = s.recv(4096)
                if not c: break
                resp += c
                if b"\r\n\r\n" in resp: break
            except: break
        s.close()

        if b"200 OK" in resp:
            print(f"\n{'!'*60}")
            print(f"!!! SUCCESS: {enc_type} + {client_type} + {label}")
            print(f"!!! Context: {ctx8.hex()}")
            print(f"!!! Key: {key.hex()}")
            print(f"!!! IV:  {iv.hex()}")
            print(f"{'!'*60}")
            # Try to decrypt response body
            he = resp.index(b"\r\n\r\n") + 4
            rb = resp[he:]
            if len(rb) > 0 and len(rb) % 16 == 0:
                try:
                    dec = AES.new(key, AES.MODE_CBC, iv).decrypt(rb)
                    print(f"Response: {dec.decode('ascii', errors='replace')}")
                except: pass
            return True

        # Extract error
        reason = ""
        for line in resp.decode("ascii", errors="replace").split("\r\n"):
            if "Reason" in line or "80029" in line:
                reason = line.strip()

        # Check for DIFFERENT error code (not 80029820)
        if b"80029820" not in resp and b"403" in resp:
            print(f"  [{label}] DIFFERENT ERROR: {reason}")
        return False

    except Exception as e:
        print(f"  [{label}] Connection error: {e}")
        return False

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 ps3_register_bruteforce_iv.py <PS3_IP> <PIN> [--start N]")
        print()
        print("PS3 must be in registration mode (Settings > Remote Play > Register Device)")
        print("Select 'Mobile Phone' on PS3 menu for Phone encryption tests")
        print("Select 'PSP' on PS3 menu for PSP encryption tests")
        print()
        print("Options:")
        print("  --start N   Skip first N attempts (for resuming after PS3 restart)")
        sys.exit(1)

    ip = sys.argv[1]
    pin = sys.argv[2]

    start_at = 0
    if "--start" in sys.argv:
        idx = sys.argv.index("--start")
        start_at = int(sys.argv[idx + 1])

    print(f"PS3: {ip}, PIN: {pin}")
    if start_at > 0:
        print(f"Skipping first {start_at} attempts (already tried)")
    print(f"Max 3 attempts per registration session — restart PS3 registration between batches!")
    print(f"IMPORTANT: Select 'Mobile Phone' on PS3 menu for Phone tests,")
    print(f"           Select 'PSP' on PS3 for PSP tests.\n")

    contexts = generate_contexts(pin)

    # Interleave most promising candidates across both encryption types
    # Priority order: most likely first
    tests = []
    for ctx, label in contexts:
        tests.append(("Phone", "PC", derive_key_phone, derive_iv_phone, ctx, label))
    for ctx, label in contexts:
        tests.append(("PSP", "Phone", derive_key_psp, derive_iv_psp, ctx, label))

    attempt_count = 0
    session_count = 0
    for enc_name, ctype, key_fn, iv_fn, ctx, label in tests:
        attempt_count += 1

        if attempt_count <= start_at:
            continue

        session_count += 1
        if session_count > 3:
            print(f"\n  --- PAUSED after 3 attempts (total: {attempt_count}) ---")
            print(f"  PS3 locks out after 3 failures per session.")
            print(f"  Restart registration on PS3 (new PIN), then run:")
            print(f"    python3 {sys.argv[0]} <IP> <NEW_PIN> --start {attempt_count - 1}")
            print(f"\n  Remaining ({len(tests) - attempt_count} attempts):")
            for t in tests[attempt_count:attempt_count+10]:
                print(f"    [{t[0]}] {t[5]}: {t[4].hex()}")
            if len(tests) - attempt_count > 10:
                print(f"    ... and {len(tests) - attempt_count - 10} more")
            return

        print(f"  [{attempt_count}] [{enc_name}+{ctype}] {label}: {ctx.hex()}")
        if attempt(ip, enc_name, ctype, key_fn, iv_fn, ctx, label):
            print("\n\nREGISTRATION SUCCESSFUL!")
            return
        print(f"       Failed")

    print(f"\nAll {attempt_count} attempts exhausted.")

if __name__ == "__main__":
    main()
