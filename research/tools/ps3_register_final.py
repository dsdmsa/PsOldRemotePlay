#!/usr/bin/env python3
"""
PS3 Registration — FINAL based on complete code re-analysis

KEY FINDING from code:
  PS3 menu "PSP"    → type=0 → iVar5=1 → PSP XOR key  → expects Client-Type: "Phone"
  PS3 menu "Phone"  → type=1 → iVar5=2 → Phone XOR key → expects Client-Type: "PC"
  PS3 menu "PC"     → type=2 → iVar5=3 → PC XOR key   → expects Client-Type: "VITA"

So when user selects "Mobile Phone" on PS3: encryption=Phone, Client-Type must be "PC"
When user selects "PSP": encryption=PSP, Client-Type must be "Phone"

Usage: python3 ps3_register_final.py <PS3_IP> <PIN> <attempt>
  1 = PSP_encrypt + Client-Type:"Phone" + ctx=zeros (if PS3 menu was PSP-like)
  2 = Phone_encrypt + Client-Type:"PC" + ctx=zeros (if PS3 menu was Phone)
  3 = PSP_encrypt + Client-Type:"Phone" + ctx=PIN-ascii
  4 = Phone_encrypt + Client-Type:"PC" + ctx=PIN-ascii
  5 = PSP_encrypt + Client-Type:"Phone" + ctx=sockaddr
  6 = Phone_encrypt + Client-Type:"PC" + ctx=sockaddr
"""
import socket, sys, os
from Crypto.Cipher import AES

# Keys from Ghidra
REG_XOR = {
    "PSP":   bytes([0xFD,0xC3,0xF6,0xA6,0x4D,0x2A,0xBA,0x7A,0x38,0x92,0x6C,0xBC,0x34,0x31,0xE1,0x0E]),
    "Phone": bytes([0xF1,0x16,0xF0,0xDA,0x44,0x2C,0x06,0xC2,0x45,0xB1,0x5E,0x48,0xF9,0x04,0xE3,0xE6]),
}
REG_IV = {
    "PSP":   bytes([0x3A,0x9D,0xF3,0x3B,0x99,0xCF,0x9C,0x0D,0xBF,0x58,0x81,0x12,0x6C,0x18,0x32,0x64]),
    "Phone": bytes([0x29,0x0D,0xE9,0x07,0xE2,0x3B,0xE2,0xFC,0x34,0x08,0xCA,0x4B,0xDE,0xE4,0xAF,0x3A]),
}

DEVICE_ID = bytes.fromhex("A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6")
DEVICE_MAC = bytes.fromhex("AABBCCDDEEFF")

def derive_key(km, enc_type):
    k = bytearray(16)
    xor = REG_XOR[enc_type]
    if enc_type == "PSP":
        # PSP: key[i] = (material[i] XOR static[i]) - i - 0x25
        for i in range(16):
            k[i] = (((km[i] ^ xor[i]) & 0xFF) - i - 0x25) & 0xFF
    else:
        # Phone: key[i] = (material[i] - i - 0x28) XOR static[i]
        for i in range(16):
            k[i] = (((km[i] - i - 0x28) & 0xFF) ^ xor[i]) & 0xFF
    return bytes(k)

def derive_iv(enc_type, ctx8):
    iv = bytearray(REG_IV[enc_type])
    if enc_type == "Phone":
        for i in range(8): iv[8+i] ^= ctx8[i]  # Phone: XOR second 8 bytes
    else:
        for i in range(8): iv[i] ^= ctx8[i]     # PSP: XOR first 8 bytes
    return bytes(iv)

def attempt(ps3_ip, enc_type, client_type, ctx8, label):
    print(f"\n>>> Encrypt={enc_type}, Client-Type={client_type}, Ctx={label} ({ctx8.hex()})")

    body = (f"Client-Type: {client_type}\r\n"
            f"Client-Id: {DEVICE_ID.hex()}\r\n"
            f"Client-Mac: {DEVICE_MAC.hex()}\r\n"
            f"Client-Nickname: PsOldRemotePlay\r\n")

    km = os.urandom(16)
    key = derive_key(km, enc_type)
    iv = derive_iv(enc_type, ctx8)

    plain = body.encode("ascii")
    padded = plain.ljust(((len(plain)+15)//16)*16, b'\x00')
    enc = AES.new(key, AES.MODE_CBC, iv).encrypt(padded)

    print(f"    Key:{key.hex()}")
    print(f"    IV: {iv.hex()}")
    print(f"    KM: {km.hex()}")
    print(f"    Body plaintext ({len(plain)}b): {body.strip()[:60]}...")

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(10)
        s.connect((ps3_ip, 9293))
        full = enc + km
        req = f"POST /sce/premo/regist HTTP/1.1\r\nContent-Length: {len(full)}\r\n\r\n"
        s.sendall(req.encode() + full)
        print(f"    Sent {len(full)} bytes!")

        resp = b""
        while True:
            try:
                c = s.recv(4096)
                if not c: break
                resp += c
                if b"\r\n\r\n" in resp:
                    he = resp.index(b"\r\n\r\n")+4
                    hdr = resp[:he].decode("ascii",errors="replace")
                    cl=0
                    for l in hdr.split("\r\n"):
                        if l.lower().startswith("content-length:"): cl=int(l.split(":")[1].strip())
                    if len(resp)-he>=cl: break
            except: break
        s.close()

        rt = resp.decode("ascii",errors="replace")
        reason=""
        for l in rt.split("\r\n"):
            if "Reason" in l: reason=l.split(":")[-1].strip()

        if b"200 OK" in resp:
            print(f"\n{'!'*60}")
            print(f"!!! 200 OK — REGISTRATION SUCCESSFUL !!!")
            print(f"{'!'*60}")
            he=resp.index(b"\r\n\r\n")+4
            rb=resp[he:]
            print(f"    Body ({len(rb)}b): {rb.hex()[:100]}")
            try:
                dec=AES.new(key,AES.MODE_CBC,iv).decrypt(rb)
                print(f"    Decrypted: {dec.decode('ascii',errors='replace')}")
            except Exception as e: print(f"    Decrypt err: {e}")
            import json
            with open("ps3_keys.json","w") as f:
                json.dump({"response":rt,"device_id":DEVICE_ID.hex(),"device_mac":DEVICE_MAC.hex(),
                           "enc_type":enc_type,"client_type":client_type,"ctx":ctx8.hex()},f,indent=2)
            return True
        else:
            print(f"    Error: {reason}")
            if reason != "80029820": print(f"    *** DIFFERENT ERROR CODE! ***")
            return False
    except Exception as e:
        print(f"    ERROR: {e}")
        return False

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 ps3_register_final.py <IP> <PIN> [attempt 1-6, or 0 for auto-3]")
        print("  1=PSP+Phone+zeros  2=Phone+PC+zeros  3=PSP+Phone+PIN")
        print("  4=Phone+PC+PIN     5=PSP+Phone+sock   6=Phone+PC+sock")
        sys.exit(1)

    ip=sys.argv[1]; pin=sys.argv[2]
    n=int(sys.argv[3]) if len(sys.argv)>3 else 0

    zeros=bytes(8)
    pin_ascii=pin.encode("ascii")[:8].ljust(8,b'\x00')
    sa_wifi=bytes([0x00,0x02,0x24,0x4D,0xC0,0xA8,0x01,0x01])

    # enc_type, client_type, context, label
    attempts=[
        ("PSP",  "Phone", zeros,     "zeros"),      # 1: If PS3 treats Mobile Phone as PSP type
        ("Phone","PC",    zeros,     "zeros"),       # 2: If PS3 uses Phone type, shifted Client-Type
        ("PSP",  "Phone", pin_ascii, "PIN-ascii"),   # 3
        ("Phone","PC",    pin_ascii, "PIN-ascii"),   # 4
        ("PSP",  "Phone", sa_wifi,   "sockaddr"),    # 5
        ("Phone","PC",    sa_wifi,   "sockaddr"),    # 6
    ]

    if 1<=n<=len(attempts):
        et,ct,ctx,lbl=attempts[n-1]
        attempt(ip,et,ct,ctx,lbl)
    elif n==0:
        # Auto: try first 3
        for i in range(min(3,len(attempts))):
            et,ct,ctx,lbl=attempts[i]
            print(f"\n{'='*50}\n  Attempt {i+1}/3\n{'='*50}")
            if attempt(ip,et,ct,ctx,lbl): return
            print("    Failed.")
        print(f"\n3 done. Restart PS3, try: python3 {sys.argv[0]} {ip} <PIN> 4")
    else:
        print(f"Use 1-{len(attempts)} or 0")

if __name__=="__main__":
    main()
