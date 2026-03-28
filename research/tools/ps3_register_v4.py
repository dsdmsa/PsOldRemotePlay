#!/usr/bin/env python3
"""
PS3 Registration v4 — MAC-based context + unencrypted attempts
Usage: python3 ps3_register_v4.py <PS3_IP> <PIN> <attempt>

Attempts:
  1 = Phone, ctx=PS3 MAC (fc0fe6d56795) + 2 zero bytes
  2 = Phone, ctx=PS3 MAC reversed byte order
  3 = Phone, ctx=PS3 WiFi MAC (might differ from ethernet MAC)
  4 = UNENCRYPTED body (no encryption at all)
  5 = Phone, ctx=PS3 MAC first 8 bytes padded
  6 = Phone, ctx=zeros BUT body has \n instead of \r\n
"""

import socket, sys, os, json
from Crypto.Cipher import AES

REG_XOR_PHONE = bytes([0xF1,0x16,0xF0,0xDA,0x44,0x2C,0x06,0xC2,0x45,0xB1,0x5E,0x48,0xF9,0x04,0xE3,0xE6])
REG_IV_PHONE  = bytes([0x29,0x0D,0xE9,0x07,0xE2,0x3B,0xE2,0xFC,0x34,0x08,0xCA,0x4B,0xDE,0xE4,0xAF,0x3A])

DEVICE_ID = bytes.fromhex("A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6")
DEVICE_MAC = bytes.fromhex("AABBCCDDEEFF")
PS3_ETH_MAC = bytes.fromhex("FC0FE6D56795")

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

def send_registration(ps3_ip, body_bytes):
    """Send raw registration POST and return response"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((ps3_ip, 9293))

        req = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(body_bytes)}\r\n\r\n"
        sock.sendall(req.encode("ascii") + body_bytes)
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
        print(f"    Response ({len(resp)} bytes): {rt[:400]}")

        # Check for different error codes
        if b"200 OK" in resp:
            print("\n!!! 200 OK — SUCCESS !!!")
            he = resp.index(b"\r\n\r\n") + 4
            rb = resp[he:]
            print(f"    Body: {rb.hex()}")
            with open("ps3_keys.json","w") as f:
                json.dump({"response":rt,"body_hex":rb.hex(),"device_id":DEVICE_ID.hex()}, f, indent=2)
            return resp, True

        # Extract error code — different codes tell us different things
        reason = ""
        for l in rt.split("\r\n"):
            if "Reason" in l:
                reason = l.split(":")[-1].strip()
        print(f"    Error code: {reason}")
        if reason != "80029820":
            print(f"    *** DIFFERENT ERROR CODE! This is progress! ***")
        return resp, False

    except Exception as e:
        print(f"    ERROR: {e}")
        return b"", False

def try_encrypted(ps3_ip, ctx8, ctx_name, client_type="Phone", line_ending="\r\n"):
    print(f"\n>>> Encrypted: Client-Type={client_type}, Context={ctx_name} ({ctx8.hex()}), endings={'CRLF' if line_ending == chr(13)+chr(10) else 'LF'}")

    body = (f"Client-Type: {client_type}{line_ending}"
            f"Client-Id: {DEVICE_ID.hex()}{line_ending}"
            f"Client-Mac: {DEVICE_MAC.hex()}{line_ending}"
            f"Client-Nickname: PsOldRemotePlay{line_ending}")

    km = os.urandom(16)
    aes_key = derive_key_phone(km)
    aes_iv = derive_iv_phone(ctx8)

    plain = body.encode("ascii")
    padded = plain.ljust(((len(plain)+15)//16)*16, b'\x00')

    encrypted = AES.new(aes_key, AES.MODE_CBC, aes_iv).encrypt(padded)
    full_body = encrypted + km

    print(f"    Key:{aes_key.hex()} IV:{aes_iv.hex()}")
    return send_registration(ps3_ip, full_body)

def try_unencrypted(ps3_ip, client_type="Phone"):
    print(f"\n>>> UNENCRYPTED: Client-Type={client_type}")

    body = (f"Client-Type: {client_type}\r\n"
            f"Client-Id: {DEVICE_ID.hex()}\r\n"
            f"Client-Mac: {DEVICE_MAC.hex()}\r\n"
            f"Client-Nickname: PsOldRemotePlay\r\n")

    print(f"    Raw body: {len(body)} bytes")
    return send_registration(ps3_ip, body.encode("ascii"))

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 ps3_register_v4.py <IP> <PIN> [attempt 1-9]")
        print("  1 = Phone, ctx=PS3 ethernet MAC+00+00")
        print("  2 = Phone, ctx=PS3 MAC reversed")
        print("  3 = Phone, ctx=PS3 MAC+01+00 (WiFi MAC = eth+1?)")
        print("  4 = UNENCRYPTED body (Phone)")
        print("  5 = UNENCRYPTED body (PC)")
        print("  6 = Phone, ctx=zeros, LF line endings")
        print("  7 = Phone, ctx=PS3 MAC first 4 + PIN first 4")
        print("  8 = Phone, ctx=all 0xFF")
        print("  9 = Phone, ctx=PS3 MAC XOR PIN bytes")
        sys.exit(1)

    ip = sys.argv[1]
    pin = sys.argv[2]
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 1

    # PS3 ethernet MAC: FC:0F:E6:D5:67:95
    mac8_padded = PS3_ETH_MAC + bytes(2)                              # fc0fe6d5679500 00
    mac8_reversed = bytes(reversed(PS3_ETH_MAC)) + bytes(2)           # 9567d5e60ffc00 00
    mac8_wifi = bytes([PS3_ETH_MAC[0], PS3_ETH_MAC[1], PS3_ETH_MAC[2],
                       PS3_ETH_MAC[3], PS3_ETH_MAC[4], PS3_ETH_MAC[5]+1, 0, 0])  # WiFi often = eth+1
    mac4_pin4 = PS3_ETH_MAC[:4] + pin[:4].encode("ascii")
    all_ff = bytes([0xFF]*8)
    pin_bytes = pin.encode("ascii")[:8].ljust(8, b'\x00')
    mac_xor_pin = bytes([(PS3_ETH_MAC[i % 6] ^ pin_bytes[i]) & 0xFF for i in range(8)])

    if n == 1: try_encrypted(ip, mac8_padded, "PS3-MAC+00+00")
    elif n == 2: try_encrypted(ip, mac8_reversed, "PS3-MAC-reversed")
    elif n == 3: try_encrypted(ip, mac8_wifi, "PS3-WiFi-MAC+00+00")
    elif n == 4: try_unencrypted(ip, "Phone")
    elif n == 5: try_unencrypted(ip, "PC")
    elif n == 6: try_encrypted(ip, bytes(8), "zeros-LF", "Phone", "\n")
    elif n == 7: try_encrypted(ip, mac4_pin4, "MAC4+PIN4")
    elif n == 8: try_encrypted(ip, all_ff, "all-0xFF")
    elif n == 9: try_encrypted(ip, mac_xor_pin, "MAC-XOR-PIN")
    else: print(f"Unknown attempt {n}")

if __name__ == "__main__":
    main()
