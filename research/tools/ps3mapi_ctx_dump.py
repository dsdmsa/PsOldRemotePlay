#!/usr/bin/env python3
"""
PS3MAPI IV Context Dumper — Dump and identify the 8-byte IV context value from PS3.

Usage:
  python3 ps3mapi_ctx_dump.py <PIN>              # Dump IV context and test against PIN
  python3 ps3mapi_ctx_dump.py <PIN> [ps3_ip]     # Specify PS3 IP (default: 192.168.1.80)

The PS3 must:
  1. Have webMAN-MOD installed with PS3MAPI enabled (port 7887)
  2. Be in registration mode (showing the PIN screen)

This script:
  1. Connects to PS3MAPI telnet (port 7887)
  2. Dumps 8 bytes from the IV context struct (sysconf_plugin BSS)
  3. Tests the dumped bytes against all plausible encodings from the PIN
  4. Reports which encoding matches (or shows raw hex if no match found)
"""

import socket
import struct
import sys
import time
import hashlib
import hmac

# Static keys from sysconf_plugin.elf (all PS3 consoles use these)
SKEY0 = bytes([0xD1, 0xB2, 0x12, 0xEB, 0x73, 0x86, 0x6C, 0x7B,
               0x12, 0xA7, 0x5E, 0x0C, 0x04, 0xC6, 0xB8, 0x91])
SKEY1 = bytes([0x1F, 0xD5, 0xB9, 0xFA, 0x71, 0xB8, 0x96, 0x81,
               0xB2, 0x87, 0x92, 0xE2, 0x6F, 0x38, 0xC3, 0x6F])
SKEY2 = bytes([0x65, 0x2D, 0x8C, 0x90, 0xDE, 0x87, 0x17, 0xCF,
               0x4F, 0xB3, 0xD8, 0xD3, 0x01, 0x79, 0x6B, 0x59])

# WiFi passphrases
WIFI_PASSPHRASE_PSP_PC = b"xeRFa3VYDHSfNjE_nA{z>pk2xANqicHqQFvij}0WHz[7kzO2Yynp4o4j}U2"
WIFI_PASSPHRASE_PHONE  = b"RvnpNXP}xS5qOWfj77guV}0lPAS37hzONG7ZHMNBAwM0mKjt1mkUhHjdbyF"

# Nonce XOR key (VITA)
NONCE_XOR_VITA = bytes([0xAF, 0x74, 0xB5, 0x4F, 0x38, 0xF8, 0xAF, 0xC8,
                        0x75, 0x77, 0xB2, 0xD5, 0x47, 0x76, 0x3B, 0xFD])


class PS3MAPIConnection:
    """Telnet-style connection to PS3MAPI server (port 7887)."""

    def __init__(self, ip, port=7887, timeout=10):
        self.ip = ip
        self.port = port
        self.timeout = timeout
        self.sock = None
        self.buffer = b""

    def connect(self):
        print(f"[*] Connecting to PS3MAPI at {self.ip}:{self.port}...")
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.settimeout(self.timeout)
        try:
            self.sock.connect((self.ip, self.port))
        except Exception as e:
            print(f"[!] Connection failed: {e}")
            raise
        banner = self._recv_line()
        print(f"[+] Connected. Banner: {banner}")
        return banner

    def _recv_line(self):
        """Read until we get a complete response line."""
        while b"\r\n" not in self.buffer and b"\n" not in self.buffer:
            try:
                chunk = self.sock.recv(4096)
                if not chunk:
                    break
                self.buffer += chunk
            except socket.timeout:
                break

        if b"\r\n" in self.buffer:
            line, self.buffer = self.buffer.split(b"\r\n", 1)
        elif b"\n" in self.buffer:
            line, self.buffer = self.buffer.split(b"\n", 1)
        else:
            line = self.buffer
            self.buffer = b""

        return line.decode(errors="replace").strip()

    def send(self, cmd):
        """Send a command and return the response."""
        print(f"  > {cmd}")
        self.sock.send((cmd + "\r\n").encode())
        time.sleep(0.3)
        resp = self._recv_line()
        print(f"  < {resp}")
        return resp

    def send_raw(self, cmd):
        """Send and return raw bytes (for memory reads)."""
        print(f"  > {cmd}")
        self.sock.send((cmd + "\r\n").encode())
        time.sleep(0.5)
        # Read all available data
        data = self.buffer
        self.buffer = b""
        while True:
            try:
                chunk = self.sock.recv(65536)
                if not chunk:
                    break
                data += chunk
            except socket.timeout:
                break
        return data

    def close(self):
        if self.sock:
            try:
                self.send("DISCONNECT")
            except:
                pass
            self.sock.close()


def dump_iv_context(ip, vsh_pid, addresses_to_try):
    """
    Try to dump 8 bytes from the IV context struct using various addresses.

    Args:
        ip: PS3 IP address
        vsh_pid: VSH process ID (from process enumeration)
        addresses_to_try: List of (address, label) tuples to attempt

    Returns:
        Tuple of (raw_bytes, address_used) or (None, None) if all fail
    """
    api = PS3MAPIConnection(ip)
    try:
        api.connect()
        # Attempt auth (not always required)
        for auth_cmd in ["USER anonymous", "PASS anonymous"]:
            try:
                api.send(auth_cmd)
            except:
                pass
    except:
        return None, None

    for addr, label in addresses_to_try:
        try:
            print(f"\n[*] Trying address {label} (0x{addr:016X})...")
            resp = api.send_raw(f"MEMORY GET {vsh_pid} {addr} 8")

            if resp and len(resp) >= 8:
                ctx_bytes = resp[:8]
                print(f"[+] Got 8 bytes from 0x{addr:016X}: {ctx_bytes.hex()}")
                api.close()
                return ctx_bytes, label
            else:
                print(f"    [!] Insufficient data ({len(resp) if resp else 0} bytes)")
        except Exception as e:
            print(f"    [!] Error: {e}")

    api.close()
    return None, None


def generate_test_contexts(pin_str):
    """
    Generate all plausible 8-byte IV context values from the PIN.
    Returns dict mapping label -> bytes.
    """
    pin_int = int(pin_str)
    contexts = {}

    def add(data, label):
        assert len(data) == 8, f"Context must be 8 bytes, got {len(data)} for {label}"
        contexts[label] = data

    # =========================================================================
    # PIN-based interpretations
    # =========================================================================

    add(bytes(8), "all_zeros")
    add(pin_int.to_bytes(8, 'big'), "pin_be64")
    add(pin_int.to_bytes(8, 'little'), "pin_le64")
    add(pin_int.to_bytes(4, 'big') + b'\x00\x00\x00\x00', "pin_be32_left")
    add(b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'big'), "pin_be32_right")
    add(pin_int.to_bytes(4, 'little') + b'\x00\x00\x00\x00', "pin_le32_left")
    add(b'\x00\x00\x00\x00' + pin_int.to_bytes(4, 'little'), "pin_le32_right")

    add(pin_str.encode('ascii')[:8].ljust(8, b'\x00'), "pin_ascii")

    # Halves swapped
    swapped_str = pin_str[4:] + pin_str[:4]
    if swapped_str.isdigit():
        sw_int = int(swapped_str)
        add(sw_int.to_bytes(8, 'big'), "pin_halves_swapped_be64")
        add(sw_int.to_bytes(8, 'little'), "pin_halves_swapped_le64")
    add(swapped_str.encode('ascii')[:8].ljust(8, b'\x00'), "pin_halves_swapped_ascii")

    # Digits as individual bytes
    add(bytes(int(d) for d in pin_str[:8]), "pin_digits_as_bytes")

    # BCD
    bcd = []
    for i in range(0, len(pin_str), 2):
        if i + 1 < len(pin_str):
            bcd.append(int(pin_str[i]) * 16 + int(pin_str[i + 1]))
        else:
            bcd.append(int(pin_str[i]) * 16)
    bcd_bytes = bytes(bcd).ljust(8, b'\x00')[:8]
    add(bcd_bytes, "pin_bcd_left")
    if len(bcd) < 8:
        add(b'\x00' * (8 - len(bcd)) + bytes(bcd), "pin_bcd_right")

    # Reversed
    add(pin_str[::-1].encode('ascii')[:8].ljust(8, b'\x00'), "pin_reversed_ascii")
    rev_int = int(pin_str[::-1])
    add(rev_int.to_bytes(8, 'big'), "pin_reversed_be64")

    # Doubled, NOT, signed
    doubled = (pin_int * 2) & 0xFFFFFFFFFFFFFFFF
    add(doubled.to_bytes(8, 'big'), "pin_doubled_be64")

    notpin = (~pin_int) & 0xFFFFFFFFFFFFFFFF
    add(notpin.to_bytes(8, 'big'), "pin_not_be64")

    notpin32 = (~pin_int) & 0xFFFFFFFF
    add(notpin32.to_bytes(4, 'big') + b'\x00\x00\x00\x00', "pin_not32_left")

    signed_val = pin_int if pin_int < 2**31 else pin_int - 2**32
    add(struct.pack('>q', signed_val), "pin_signed_be64")

    # XOR shift
    pin_xor_shift = pin_int ^ (pin_int << 32)
    pin_xor_shift &= 0xFFFFFFFFFFFFFFFF
    add(pin_xor_shift.to_bytes(8, 'big'), "pin_xor_shifted_be64")

    # Two halves as different sizes
    h1 = int(pin_str[:4])
    h2 = int(pin_str[4:])
    add(h1.to_bytes(2, 'big') + h2.to_bytes(2, 'big') + b'\x00\x00\x00\x00', "pin_2xbe16_left")
    add(h1.to_bytes(4, 'big') + h2.to_bytes(4, 'big'), "pin_2xbe32")

    # =========================================================================
    # Hash-based
    # =========================================================================

    pin_ascii = pin_str.encode('ascii')
    pin_be32 = pin_int.to_bytes(4, 'big')
    pin_be64 = pin_int.to_bytes(8, 'big')

    add(hashlib.sha256(pin_ascii).digest()[:8], "sha256_pin_ascii_8")
    add(hashlib.sha256(pin_be32).digest()[:8], "sha256_pin_be32_8")
    add(hashlib.sha256(pin_be64).digest()[:8], "sha256_pin_be64_8")

    add(hashlib.md5(pin_ascii).digest()[:8], "md5_pin_ascii_8")
    add(hashlib.md5(pin_be32).digest()[:8], "md5_pin_be32_8")

    add(hashlib.sha1(pin_ascii).digest()[:8], "sha1_pin_ascii_8")
    add(hashlib.sha1(pin_be32).digest()[:8], "sha1_pin_be32_8")

    # HMAC
    add(hmac.new(SKEY1, pin_ascii, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey1_pin_ascii_8")
    add(hmac.new(SKEY1, pin_be32, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey1_pin_be32_8")
    add(hmac.new(SKEY1, pin_be64, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey1_pin_be64_8")

    add(hmac.new(SKEY0, pin_ascii, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey0_pin_ascii_8")
    add(hmac.new(SKEY0, pin_be32, hashlib.sha256).digest()[:8],
        "hmac_sha256_skey0_pin_be32_8")

    add(hmac.new(SKEY1, pin_ascii, hashlib.md5).digest()[:8],
        "hmac_md5_skey1_pin_ascii_8")
    add(hmac.new(SKEY1, pin_be32, hashlib.md5).digest()[:8],
        "hmac_md5_skey1_pin_be32_8")

    # =========================================================================
    # WiFi passphrase-based
    # =========================================================================

    add(WIFI_PASSPHRASE_PSP_PC[:8], "wifi_psp_pc_first8")
    add(WIFI_PASSPHRASE_PHONE[:8], "wifi_phone_first8")
    add(hashlib.sha256(WIFI_PASSPHRASE_PSP_PC).digest()[:8], "sha256_wifi_psp_pc_8")
    add(hashlib.sha256(WIFI_PASSPHRASE_PHONE).digest()[:8], "sha256_wifi_phone_8")
    add(WIFI_PASSPHRASE_PSP_PC[-8:], "wifi_psp_pc_last8")
    add(WIFI_PASSPHRASE_PHONE[-8:], "wifi_phone_last8")

    # =========================================================================
    # Hardware / null / constant patterns
    # =========================================================================

    add(bytes([0xFF] * 8), "all_ff")
    add(bytes([0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x00, 0x00]), "sample_mac_padded")
    add(bytes([0x00, 0x02, 0x24, 0x4D, 0x00, 0x00, 0x00, 0x00]), "sockaddr_any")
    add(bytes([0x00, 0x02, 0x24, 0x4D, 0xC0, 0xA8, 0x01, 0x01]), "sockaddr_192_168_1_1")
    add(bytes([0x02, 0x00, 0x24, 0x4D, 0x00, 0x00, 0x00, 0x00]), "sockaddr_le_any")

    # =========================================================================
    # skey-based
    # =========================================================================

    add(SKEY1[:8], "skey1_first8")
    add(SKEY1[8:], "skey1_last8")

    pin4 = pin_int.to_bytes(4, 'big')
    xored = bytes(SKEY1[i] ^ (pin4[i] if i < 4 else 0) for i in range(8))
    add(xored, "skey1_8_xor_pin_be32_left")

    xored2 = bytes(SKEY1[i] ^ (pin4[i - 4] if i >= 4 else 0) for i in range(8))
    add(xored2, "skey1_8_xor_pin_be32_right")

    xored3 = bytes(SKEY1[i] ^ pin_be64[i] for i in range(8))
    add(xored3, "skey1_8_xor_pin_be64")

    xored4 = bytes(SKEY1[i] ^ pin_ascii[i] for i in range(min(8, len(pin_ascii))))
    if len(xored4) < 8:
        xored4 += SKEY1[len(xored4):8]
    add(xored4, "skey1_8_xor_pin_ascii")

    add(SKEY0[:8], "skey0_first8")
    add(SKEY2[:8], "skey2_first8")
    add(bytes(SKEY0[i] ^ SKEY1[i] for i in range(8)), "skey0_xor_skey1_8")

    # =========================================================================
    # VITA-related
    # =========================================================================

    add(NONCE_XOR_VITA[:8], "nonce_xor_vita_first8")
    add(NONCE_XOR_VITA[8:], "nonce_xor_vita_last8")

    # =========================================================================
    # Constants
    # =========================================================================

    add(bytes([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]), "const_1_left")
    add(bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01]), "const_1_right")
    add(bytes([0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00]), "const_1_mid")
    add(bytes([0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25]), "const_0x25_all")
    add(bytes([0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28]), "const_0x28_all")
    add(bytes([0x2B, 0x2B, 0x2B, 0x2B, 0x2B, 0x2B, 0x2B, 0x2B]), "const_0x2B_all")

    return contexts


def find_vsh_pid(ip):
    """
    Connect to PS3MAPI and enumerate processes to find VSH.
    Returns (vsh_pid, sysconf_prx_id) or (None, None) if not found.
    """
    api = PS3MAPIConnection(ip)
    try:
        api.connect()
        # Auth attempts
        for auth_cmd in ["USER anonymous", "PASS anonymous"]:
            try:
                api.send(auth_cmd)
            except:
                pass
    except:
        print("[!] Cannot connect to PS3MAPI")
        return None, None

    try:
        # Get all processes
        print("\n[*] Enumerating processes...")
        resp = api.send("PROCESS GETALLPID")
        pids = []
        parts = resp.split()
        for part in parts:
            for sub in part.split("|"):
                sub = sub.strip()
                try:
                    pid = int(sub, 0)
                    if pid > 0:
                        pids.append(pid)
                except ValueError:
                    continue

        if not pids:
            print("[!] No processes found")
            api.close()
            return None, None

        print(f"[+] Found {len(pids)} processes: {pids}")

        # Find VSH (usually first or has "vsh" in name)
        vsh_pid = None
        for pid in pids:
            try:
                resp = api.send(f"PROCESS GETNAME {pid}")
                name = resp.split(None, 1)[-1] if " " in resp else resp
                if "vsh" in name.lower() or "main" in name.lower():
                    vsh_pid = pid
                    print(f"[+] Found VSH! PID {pid} (0x{pid:X}): {name}")
                    break
            except:
                pass

        if not vsh_pid:
            vsh_pid = pids[0]
            print(f"[*] Assuming VSH is first process: {vsh_pid}")

        api.close()
        return vsh_pid, None

    except Exception as e:
        print(f"[!] Error during enumeration: {e}")
        api.close()
        return None, None


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 ps3mapi_ctx_dump.py <PIN> [ps3_ip]")
        print()
        print("Example:")
        print("  python3 ps3mapi_ctx_dump.py 12345678")
        print("  python3 ps3mapi_ctx_dump.py 12345678 192.168.1.80")
        sys.exit(1)

    pin_str = sys.argv[1]
    ps3_ip = sys.argv[2] if len(sys.argv) > 2 else "192.168.1.80"

    if not pin_str.isdigit() or len(pin_str) != 8:
        print("[!] PIN must be exactly 8 digits")
        sys.exit(1)

    print("=" * 70)
    print("  PS3MAPI IV Context Dumper")
    print(f"  Target: {ps3_ip}")
    print(f"  PIN: {pin_str}")
    print("=" * 70)
    print()
    print("IMPORTANT: PS3 must be in registration mode (showing PIN screen)")
    print()

    # Find VSH PID
    vsh_pid, _ = find_vsh_pid(ps3_ip)
    if not vsh_pid:
        print("[!] Could not find VSH process. Exiting.")
        sys.exit(1)

    # Try various addresses for IV context struct
    # sysconf base is 0x01B60000 + BSS offset
    # Possible offsets: 0x1B5818, 0x1BC000, etc.
    addresses_to_try = [
        (0x01B60000 + 0x1B5818, "sysconf_base + 0x1B5818"),
        (0x01B61B5818, "direct 0x01B61B5818"),
        (0x01BC0000, "BSS guess: 0x01BC0000"),
        (0x01BC1B58, "BSS + 0x1B58"),
        (0x01B70000, "alt: 0x01B70000"),
    ]

    # Dump IV context
    ctx_bytes, addr_label = dump_iv_context(ps3_ip, vsh_pid, addresses_to_try)

    if not ctx_bytes:
        print("\n[!] Could not dump IV context from any address")
        print("\nTry:")
        print("  1. Check that the PS3 is in registration mode")
        print("  2. Verify VSH PID is correct (try: telnet <ip> 7887 -> PROCESS GETNAME <pid>)")
        print("  3. Try different addresses (use ps3mapi_dump.py for full enumeration)")
        sys.exit(1)

    print(f"\n[+] IV context dumped from {addr_label}")
    print(f"[+] Raw hex: {ctx_bytes.hex()}")

    # Generate all test contexts from PIN
    print("\n[*] Generating test vectors for PIN...")
    test_contexts = generate_test_contexts(pin_str)
    print(f"[*] Generated {len(test_contexts)} encoding variants")

    # Try to match
    print("\n[*] Comparing against all encodings...")
    matches = []
    for label, test_bytes in test_contexts.items():
        if test_bytes == ctx_bytes:
            matches.append(label)
            print(f"[!!!] MATCH FOUND: {label}")

    print()
    print("=" * 70)
    if matches:
        print(f"SUCCESS! Found {len(matches)} match(es):")
        for m in matches:
            print(f"  - {m}")
        print()
        print("Recommended next step:")
        print("  Use the matching encoding to derive the registration IV")
        print("  in iv_context_generator.py with this PIN")
    else:
        print("No matches found in test vectors.")
        print()
        print("Raw IV context hex: " + ctx_bytes.hex())
        print()
        print("Possible reasons:")
        print("  1. IV context is derived from something other than the PIN")
        print("  2. The dump address was wrong (wrong memory)")
        print("  3. PS3 wasn't in active registration mode during dump")
    print("=" * 70)


if __name__ == "__main__":
    main()
