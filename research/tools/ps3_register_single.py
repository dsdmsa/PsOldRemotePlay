#!/usr/bin/env python3
"""
PS3 Registration — Single Shot v3
Tries ONE combination per run to avoid PS3 dropping WiFi.

Usage: python3 ps3_register_single.py <PS3_IP> <PIN> <attempt_number>

Attempts 1-9 try different IV contexts with Phone encryption.
Attempts 10+ try with swapped Client-Type strings.
"""

import socket, sys, os, json
from Crypto.Cipher import AES

REG_XOR_PHONE = bytes([0xF1,0x16,0xF0,0xDA,0x44,0x2C,0x06,0xC2,0x45,0xB1,0x5E,0x48,0xF9,0x04,0xE3,0xE6])
REG_IV_PHONE  = bytes([0x29,0x0D,0xE9,0x07,0xE2,0x3B,0xE2,0xFC,0x34,0x08,0xCA,0x4B,0xDE,0xE4,0xAF,0x3A])

DEVICE_ID = bytes.fromhex("A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6")
DEVICE_MAC = bytes.fromhex("AABBCCDDEEFF")

def derive_key_phone(km):
    k = bytearray(16)
    for i in range(16):
        k[i] = ((km[i] - i - 0x28) ^ REG_XOR_PHONE[i]) & 0xFF
    return bytes(k)

def derive_iv_phone(ctx8):
    iv = bytearray(REG_IV_PHONE)
    for i in range(8):
        iv[8+i] ^= ctx8[i]
    return bytes(iv)

def attempt(ps3_ip, pin, ctx8, ctx_name, client_type_str="Phone"):
    print(f"\n>>> Client-Type={client_type_str}, Context={ctx_name} ({ctx8.hex()})")

    body = (f"Client-Type: {client_type_str}\r\n"
            f"Client-Id: {DEVICE_ID.hex()}\r\n"
            f"Client-Mac: {DEVICE_MAC.hex()}\r\n"
            f"Client-Nickname: PsOldRemotePlay\r\n")

    km = os.urandom(16)
    aes_key = derive_key_phone(km)
    aes_iv = derive_iv_phone(ctx8)

    plain = body.encode("ascii")
    padded = plain.ljust(((len(plain)+15)//16)*16, b'\x00')

    encrypted = AES.new(aes_key, AES.MODE_CBC, aes_iv).encrypt(padded)
    full_body = encrypted + km

    print(f"    AES Key: {aes_key.hex()}")
    print(f"    AES IV:  {aes_iv.hex()}")
    print(f"    KeyMat:  {km.hex()}")
    print(f"    Body size: {len(full_body)}")

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((ps3_ip, 9293))

        req = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(full_body)}\r\n\r\n"
        sock.sendall(req.encode("ascii") + full_body)
        print("    Sent! Waiting...")

        resp = b""
        while True:
            try:
                c = sock.recv(4096)
                if not c: break
                resp += c
                if b"\r\n\r\n" in resp:
                    he = resp.index(b"\r\n\r\n") + 4
                    hdr = resp[:he].decode("ascii", errors="replace")
                    cl = 0
                    for l in hdr.split("\r\n"):
                        if l.lower().startswith("content-length:"): cl = int(l.split(":")[1].strip())
                    if len(resp) - he >= cl: break
            except: break
        sock.close()

        rt = resp.decode("ascii", errors="replace")
        print(f"    Response: {rt[:400]}")

        if b"200 OK" in resp:
            print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            print("!!! 200 OK — SUCCESS !!!")
            print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            he = resp.index(b"\r\n\r\n") + 4
            rb = resp[he:]
            print(f"    Body ({len(rb)} bytes hex): {rb.hex()}")
            if len(rb) >= 16:
                try:
                    dec = AES.new(aes_key, AES.MODE_CBC, aes_iv).decrypt(rb)
                    print(f"    Decrypted: {dec.decode('ascii', errors='replace')}")
                except Exception as e:
                    print(f"    Decrypt err: {e}")
                    print(f"    Raw: {rb.decode('ascii', errors='replace')}")
            result = {"raw": rt, "body_hex": rb.hex() if b"200 OK" in resp else ""}
            with open("ps3_keys.json","w") as f:
                json.dump({"response": rt, "device_id": DEVICE_ID.hex(), "device_mac": DEVICE_MAC.hex()}, f, indent=2)
            return True
        elif b"403" in resp:
            # Extract error code
            reason = ""
            for l in rt.split("\r\n"):
                if "Reason" in l: reason = l
            print(f"    REJECTED: {reason}")
            return False
        else:
            print(f"    Unexpected response")
            return False
    except Exception as e:
        print(f"    ERROR: {e}")
        return False

def main():
    if len(sys.argv) < 3:
        print("PS3 Registration — Single Shot v3")
        print("Usage: python3 ps3_register_single.py <IP> <PIN> [attempt]")
        print("")
        print("Attempts:")
        print("  1 = Phone, Client-Type:Phone, ctx=zeros")
        print("  2 = Phone, Client-Type:Phone, ctx=PIN-ascii")
        print("  3 = Phone, Client-Type:Phone, ctx=PIN-be-int")
        print("  4 = Phone, Client-Type:PC, ctx=zeros      (shifted mapping)")
        print("  5 = Phone, Client-Type:PC, ctx=PIN-ascii   (shifted mapping)")
        print("  6 = Phone, Client-Type:PC, ctx=PIN-be-int  (shifted mapping)")
        print("  7 = Phone, Client-Type:Phone, ctx=PIN-le-int")
        print("  8 = Phone, Client-Type:Phone, ctx=SSID-suffix-padded")
        print("  9 = Phone, Client-Type:Phone, ctx=PIN-first4-repeated")
        print("  0 = Try ALL (3 per PS3 session)")
        sys.exit(1)

    ip = sys.argv[1]
    pin = sys.argv[2]
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 0

    # Build context candidates
    zeros = bytes(8)
    pin_ascii = pin.encode("ascii")[:8].ljust(8, b'\x00')
    pin_int_be = int(pin).to_bytes(8, "big") if pin.isdigit() else zeros
    pin_int_le = int(pin).to_bytes(8, "little") if pin.isdigit() else zeros
    pin_first4 = pin[:4].encode("ascii").ljust(8, b'\x00')
    pin_halves_swap = (pin[4:] + pin[:4]).encode("ascii")[:8]

    attempts = [
        # Standard Client-Type: Phone
        (zeros,           "zeros",            "Phone"),   # 1
        (pin_ascii,       "PIN-ascii",        "Phone"),   # 2
        (pin_int_be,      "PIN-be-int",       "Phone"),   # 3
        # Shifted Client-Type: PC (if PS3 menu "Phone" expects Client-Type "PC")
        (zeros,           "zeros",            "PC"),      # 4
        (pin_ascii,       "PIN-ascii",        "PC"),      # 5
        (pin_int_be,      "PIN-be-int",       "PC"),      # 6
        # More context guesses
        (pin_int_le,      "PIN-le-int",       "Phone"),   # 7
        (pin_first4,      "SSID-suffix",      "Phone"),   # 8
        (pin_halves_swap, "PIN-halves-swap",  "Phone"),   # 9
    ]

    if n >= 1 and n <= len(attempts):
        ctx, cname, ctype = attempts[n-1]
        attempt(ip, pin, ctx, cname, ctype)
    elif n == 0:
        # Try first 3 (one PS3 session worth)
        for i in range(min(3, len(attempts))):
            ctx, cname, ctype = attempts[i]
            print(f"\n{'='*50}")
            print(f"  Attempt {i+1}")
            print(f"{'='*50}")
            if attempt(ip, pin, ctx, cname, ctype):
                return
            print("    Failed, next...")
        print("\n3 attempts done. Restart PS3 registration for more.")
        print(f"Next time try: python3 {sys.argv[0]} {ip} <NEW_PIN> 4")
    else:
        print(f"Invalid attempt number: {n}. Use 1-{len(attempts)} or 0 for auto.")

if __name__ == "__main__":
    main()
