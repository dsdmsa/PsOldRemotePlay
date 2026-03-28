#!/usr/bin/env python3
"""
PS3 Registration v5 — Network address context candidates
Based on Ghidra finding: context = PS3's socket/network address

Usage: python3 ps3_register_v5.py <PS3_IP> <PIN> <attempt 1-9>
  1 = sockaddr_in(AF_INET, 9293, 192.168.1.1) — PS3 WiFi AP IP
  2 = Just IP 192.168.1.1 padded
  3 = PS3 WiFi MAC (eth+1) = FC:0F:E6:D5:67:96
  4 = PS3 ethernet MAC = FC:0F:E6:D5:67:95
  5 = sockaddr_in(AF_INET, 9293, PS3 wired IP)
  6 = Client-Type: PC + sockaddr
  7 = sockaddr_in big-endian port
  8 = sockaddr_in with port 0
  9 = Just zeros (re-verify baseline)
"""
import socket, sys, os
from Crypto.Cipher import AES

REG_XOR_PHONE = bytes([0xF1,0x16,0xF0,0xDA,0x44,0x2C,0x06,0xC2,0x45,0xB1,0x5E,0x48,0xF9,0x04,0xE3,0xE6])
REG_IV_PHONE  = bytes([0x29,0x0D,0xE9,0x07,0xE2,0x3B,0xE2,0xFC,0x34,0x08,0xCA,0x4B,0xDE,0xE4,0xAF,0x3A])

DEVICE_ID = bytes.fromhex("A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6")
DEVICE_MAC = bytes.fromhex("AABBCCDDEEFF")

def derive_key(km):
    k = bytearray(16)
    for i in range(16): k[i] = ((km[i] - i - 0x28) ^ REG_XOR_PHONE[i]) & 0xFF
    return bytes(k)

def derive_iv(ctx8):
    iv = bytearray(REG_IV_PHONE)
    for i in range(8): iv[8+i] ^= ctx8[i]
    return bytes(iv)

def attempt(ps3_ip, ctx8, label, ctype="Phone"):
    print(f"\n>>> {label}: ctx={ctx8.hex()}, Client-Type={ctype}")

    body = (f"Client-Type: {ctype}\r\n"
            f"Client-Id: {DEVICE_ID.hex()}\r\n"
            f"Client-Mac: {DEVICE_MAC.hex()}\r\n"
            f"Client-Nickname: PsOldRemotePlay\r\n")

    km = os.urandom(16)
    key = derive_key(km)
    iv = derive_iv(ctx8)

    plain = body.encode("ascii")
    padded = plain.ljust(((len(plain)+15)//16)*16, b'\x00')
    enc = AES.new(key, AES.MODE_CBC, iv).encrypt(padded)

    print(f"    Key:{key.hex()} IV:{iv.hex()}")

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(10)
        s.connect((ps3_ip, 9293))
        req = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(enc)+16}\r\n\r\n"
        s.sendall(req.encode() + enc + km)
        print("    Sent!")

        resp = b""
        while True:
            try:
                c = s.recv(4096)
                if not c: break
                resp += c
                if b"\r\n\r\n" in resp:
                    he = resp.index(b"\r\n\r\n")+4
                    hdr = resp[:he].decode("ascii",errors="replace")
                    cl = 0
                    for l in hdr.split("\r\n"):
                        if l.lower().startswith("content-length:"): cl=int(l.split(":")[1].strip())
                    if len(resp)-he>=cl: break
            except: break
        s.close()

        rt = resp.decode("ascii",errors="replace")
        # Extract error code
        reason = ""
        for l in rt.split("\r\n"):
            if "Reason" in l: reason = l.split(":")[-1].strip()

        if b"200 OK" in resp:
            print(f"    !!! 200 OK — SUCCESS !!!")
            he = resp.index(b"\r\n\r\n")+4
            rb = resp[he:]
            try:
                dec = AES.new(key,AES.MODE_CBC,iv).decrypt(rb)
                print(f"    Decrypted: {dec.decode('ascii',errors='replace')}")
            except: print(f"    Body hex: {rb.hex()}")
            return True
        else:
            print(f"    Result: {reason}")
            if reason != "80029820":
                print(f"    *** DIFFERENT ERROR! Progress! ***")
            return False
    except Exception as e:
        print(f"    ERROR: {e}")
        return False

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 ps3_register_v5.py <IP> <PIN> [1-9]")
        sys.exit(1)

    ip = sys.argv[1]
    pin = sys.argv[2]
    n = int(sys.argv[3]) if len(sys.argv)>3 else 0

    # sockaddr_in: family(2) + port(2) + addr(4) = 8 bytes
    # PS3 WiFi AP typically uses 192.168.1.1
    sa_wifi = bytes([0x00,0x02, 0x24,0x4D, 0xC0,0xA8,0x01,0x01])
    ip_wifi = bytes([0xC0,0xA8,0x01,0x01, 0x00,0x00,0x00,0x00])
    wifi_mac = bytes([0xFC,0x0F,0xE6,0xD5,0x67,0x96,0x00,0x00])  # eth+1
    eth_mac  = bytes([0xFC,0x0F,0xE6,0xD5,0x67,0x95,0x00,0x00])
    sa_wired = bytes([0x00,0x02, 0x24,0x4D, 0xC0,0xA8,0x01,0x4B])  # 192.168.1.75
    sa_be_port = bytes([0x00,0x02, 0x4D,0x24, 0xC0,0xA8,0x01,0x01]) # swapped port bytes
    sa_noport = bytes([0x00,0x02, 0x00,0x00, 0xC0,0xA8,0x01,0x01])

    attempts = [
        (sa_wifi,    "sockaddr(WiFi 192.168.1.1:9293)", "Phone"),
        (ip_wifi,    "IP 192.168.1.1 padded",           "Phone"),
        (wifi_mac,   "WiFi MAC (eth+1)",                 "Phone"),
        (eth_mac,    "Ethernet MAC",                     "Phone"),
        (sa_wired,   "sockaddr(Wired 192.168.1.75:9293)","Phone"),
        (sa_wifi,    "sockaddr(WiFi)+PC type",           "PC"),
        (sa_be_port, "sockaddr(BE port swap)",           "Phone"),
        (sa_noport,  "sockaddr(port=0)",                 "Phone"),
        (bytes(8),   "zeros (baseline)",                 "Phone"),
    ]

    if 1 <= n <= len(attempts):
        ctx, label, ct = attempts[n-1]
        attempt(ip, ctx, label, ct)
    elif n == 0:
        for i,(ctx,label,ct) in enumerate(attempts[:3]):
            print(f"\n{'='*50}\n  Attempt {i+1}/3\n{'='*50}")
            if attempt(ip, ctx, label, ct): return
    else:
        print(f"Invalid: use 1-{len(attempts)}")

if __name__ == "__main__":
    main()
