#!/usr/bin/env python3
"""
PS3MAPI Memory Dumper — Dump sysconf_plugin registration handler from VSH memory.

Usage:
  python3 ps3mapi_dump.py [ps3_ip]

The PS3 must have webMAN-MOD installed and be reachable on the network.
Run this WHILE the PS3 is in registration mode (showing the PIN screen).

Steps:
  Phase 1: Enumerate processes and modules via PS3MAPI (port 7887)
  Phase 2: Find sysconf_plugin.sprx and its text segment base address
  Phase 3: Dump the registration handler memory range
  Phase 4: Save as binary for Ghidra import
"""

import socket
import struct
import sys
import os
import time
import json
import urllib.request
import urllib.error

PS3_IP = sys.argv[1] if len(sys.argv) > 1 else "192.168.1.80"
PS3MAPI_PORT = 7887
OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))

# Target: registration handler function in sysconf_plugin
# These are the virtual addresses from the decrypted ELF (Ghidra)
TARGET_START = 0x00100168  # UndefinedFunction_00100168 (registration handler)
TARGET_END   = 0x00100E3C  # End of handler
TARGET_SIZE  = TARGET_END - TARGET_START  # 3284 bytes

# Also dump the key derivation area
KEY_DERIV_START = 0x00100560  # Key derivation loop
KEY_DERIV_END   = 0x0010058C  # End of key derivation
# And the static key data
STATIC_DATA_START = 0x00150EC0  # DAT_00150ec0 (XOR key)
STATIC_DATA_END   = 0x00150EF0  # DAT_00150ed0 + 16 (IV base)


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
        self.sock.connect((self.ip, self.port))
        banner = self._recv_line()
        print(f"[+] Banner: {banner}")
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

    def _recv_all_lines(self, max_lines=50):
        """Read multiple response lines (for multi-line responses)."""
        lines = []
        for _ in range(max_lines):
            line = self._recv_line()
            if not line:
                break
            lines.append(line)
            # If we got a status code line, that's the end
            if line and line[0].isdigit():
                break
        return lines

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


def phase1_ps3mapi(ip):
    """Phase 1: Enumerate processes and modules via PS3MAPI telnet."""
    print("\n" + "=" * 60)
    print("PHASE 1: PS3MAPI Process & Module Enumeration")
    print("=" * 60)

    api = PS3MAPIConnection(ip)
    try:
        api.connect()
    except Exception as e:
        print(f"[!] Cannot connect to PS3MAPI port {PS3MAPI_PORT}: {e}")
        print("    Is webMAN-MOD installed? Is PS3MAPI enabled in webMAN settings?")
        return None

    # Some PS3MAPI implementations need authentication
    for auth_cmd in [
        "USER anonymous", "PASS anonymous",
        "USER root", "PASS root",
    ]:
        try:
            resp = api.send(auth_cmd)
            if "230" in resp or "OK" in resp.upper():
                print(f"[+] Authenticated: {resp}")
                break
        except:
            pass

    # Get PS3 info
    try:
        resp = api.send("PS3 GETFWVERSION")
        print(f"[+] FW Version: {resp}")
        resp = api.send("PS3 GETFWTYPE")
        print(f"[+] FW Type: {resp}")
    except:
        pass

    # List all processes
    print("\n[*] Listing processes...")
    results = {"processes": [], "vsh_pid": None, "modules": [], "sysconf_info": None}

    resp = api.send("PROCESS GETALLPID")
    if "200" not in resp:
        print(f"[!] Unexpected response: {resp}")
        # Try alternative command format
        resp = api.send("PS3 GETPID")

    # Parse PIDs — format varies: "200 pid1|pid2|pid3" or similar
    pids = []
    parts = resp.split()
    for part in parts:
        for sub in part.split("|"):
            sub = sub.strip()
            try:
                pid = int(sub, 0)  # Accept decimal or hex
                if pid > 0:
                    pids.append(pid)
            except ValueError:
                continue

    print(f"[+] Found {len(pids)} processes: {pids}")

    # Get process names
    vsh_pid = None
    for pid in pids:
        try:
            resp = api.send(f"PROCESS GETNAME {pid}")
            name = resp.split(None, 1)[-1] if " " in resp else resp
            print(f"    PID {pid} (0x{pid:X}): {name}")
            results["processes"].append({"pid": pid, "name": name})
            if "vsh" in name.lower() or "main" in name.lower():
                vsh_pid = pid
                print(f"    ^^^ This looks like VSH!")
        except Exception as e:
            print(f"    PID {pid}: error — {e}")

    if vsh_pid is None and pids:
        # VSH is typically the first or lowest-PID process
        vsh_pid = pids[0]
        print(f"\n[*] Assuming VSH is PID {vsh_pid} (first process)")

    if vsh_pid is None:
        print("[!] No processes found. PS3MAPI may not be fully functional.")
        api.close()
        return results

    results["vsh_pid"] = vsh_pid
    print(f"\n[+] Using VSH PID: {vsh_pid} (0x{vsh_pid:X})")

    # List modules for VSH
    print(f"\n[*] Listing modules for PID {vsh_pid}...")
    resp = api.send(f"MODULE GETALLPRXID {vsh_pid}")

    prx_ids = []
    parts = resp.split()
    for part in parts:
        for sub in part.split("|"):
            sub = sub.strip()
            try:
                prx_id = int(sub, 0)
                prx_ids.append(prx_id)
            except ValueError:
                continue

    print(f"[+] Found {len(prx_ids)} modules")

    sysconf_id = None
    sysconf_segments = None

    for prx_id in prx_ids:
        try:
            resp = api.send(f"MODULE GETNAME {vsh_pid} {prx_id}")
            name = resp.split(None, 1)[-1] if " " in resp else resp

            resp2 = api.send(f"MODULE GETFILENAME {vsh_pid} {prx_id}")
            filename = resp2.split(None, 1)[-1] if " " in resp2 else resp2

            module_info = {"prx_id": prx_id, "name": name, "filename": filename}

            # Get segment info
            try:
                resp3 = api.send(f"MODULE GETSEGMENTS {vsh_pid} {prx_id}")
                module_info["segments_raw"] = resp3
                # Parse segments: "200 base1:size1|base2:size2"
                segments = []
                seg_parts = resp3.split()
                for sp in seg_parts:
                    for seg in sp.split("|"):
                        if ":" in seg:
                            try:
                                base_s, size_s = seg.split(":", 1)
                                base = int(base_s, 0)
                                size = int(size_s, 0)
                                segments.append({"base": base, "size": size})
                            except ValueError:
                                pass
                module_info["segments"] = segments
            except:
                pass

            results["modules"].append(module_info)

            is_sysconf = "sysconf" in name.lower() or "sysconf" in filename.lower()
            marker = " <<<< TARGET" if is_sysconf else ""
            print(f"    PRX {prx_id}: {name} [{filename}]{marker}")
            if "segments" in module_info:
                for i, seg in enumerate(module_info["segments"]):
                    print(f"        Segment {i}: base=0x{seg['base']:016X} size=0x{seg['size']:X}")

            if is_sysconf:
                sysconf_id = prx_id
                sysconf_segments = module_info.get("segments", [])
                results["sysconf_info"] = module_info

        except Exception as e:
            print(f"    PRX {prx_id}: error — {e}")

    if sysconf_id is not None:
        print(f"\n[+] Found sysconf_plugin! PRX ID: {sysconf_id}")
    else:
        print("\n[!] sysconf_plugin NOT FOUND in module list!")
        print("    This could mean:")
        print("    - The module name is different than expected")
        print("    - PS3MAPI can't enumerate all modules")
        print("    - Try looking at the full module list above")

    api.close()
    return results


def phase2_webman_recon(ip):
    """Phase 2: Gather info via webMAN's HTTP interface as backup."""
    print("\n" + "=" * 60)
    print("PHASE 2: webMAN HTTP Reconnaissance")
    print("=" * 60)

    urls = [
        f"http://{ip}/cpursx.ps3",
        f"http://{ip}/setup.ps3mapi",
        f"http://{ip}/home.ps3mapi",
    ]

    for url in urls:
        try:
            print(f"\n[*] Fetching {url}...")
            req = urllib.request.Request(url, headers={"User-Agent": "PsOldRemotePlay/1.0"})
            resp = urllib.request.urlopen(req, timeout=5)
            html = resp.read().decode(errors="replace")
            # Save raw HTML
            fname = url.split("/")[-1].replace(".", "_") + ".html"
            fpath = os.path.join(OUTPUT_DIR, fname)
            with open(fpath, "w") as f:
                f.write(html)
            print(f"    Saved to {fpath} ({len(html)} bytes)")
            # Print relevant snippets
            for line in html.split("\n"):
                line_clean = line.strip()
                if any(kw in line_clean.lower() for kw in ["sysconf", "premo", "process", "module", "pid", "segment"]):
                    print(f"    >> {line_clean[:120]}")
        except Exception as e:
            print(f"    Error: {e}")


def phase3_memory_dump(ip, vsh_pid, text_base, data_base=None):
    """Phase 3: Dump memory ranges via PS3MAPI."""
    print("\n" + "=" * 60)
    print("PHASE 3: Memory Dump")
    print("=" * 60)

    # Calculate runtime addresses
    # The ELF virtual addresses need to be mapped to runtime addresses
    # For PS3 SPRX: runtime_addr = segment_base + (target_va - segment_va_start)
    #
    # Common PS3 SPRX text segment VA starts: 0x10000, 0x100000, etc.
    # We'll try to figure out the ELF base from the target addresses
    #
    # If TARGET_START = 0x00100168 and the ELF text segment starts at 0x00100000
    # Then offset from segment start = 0x168
    # Runtime address = text_base + 0x168

    # Possible ELF text bases (we'll try the most likely)
    elf_text_bases = [0x00100000, 0x00000000, 0x00010000]

    print(f"[*] VSH PID: {vsh_pid}")
    print(f"[*] Runtime text segment base: 0x{text_base:016X}")
    print(f"[*] Target ELF addresses: 0x{TARGET_START:08X} - 0x{TARGET_END:08X}")

    api = PS3MAPIConnection(ip)
    try:
        api.connect()
        # Auth
        api.send("USER anonymous")
        api.send("PASS anonymous")
    except Exception as e:
        print(f"[!] Connection failed: {e}")
        return

    dump_results = []

    for elf_base in elf_text_bases:
        if TARGET_START < elf_base:
            continue

        offset = TARGET_START - elf_base
        runtime_addr = text_base + offset
        dump_size = TARGET_SIZE

        print(f"\n[*] Trying ELF base 0x{elf_base:08X}:")
        print(f"    Offset from base: 0x{offset:X}")
        print(f"    Runtime address: 0x{runtime_addr:016X}")
        print(f"    Dump size: {dump_size} bytes (0x{dump_size:X})")

        try:
            # PS3MAPI MEMORY GET command
            resp = api.send_raw(f"MEMORY GET {vsh_pid} {runtime_addr} {dump_size}")

            if resp and len(resp) > 10:
                # Save raw dump
                fname = f"sysconf_reghandler_base{elf_base:08X}.bin"
                fpath = os.path.join(OUTPUT_DIR, fname)
                with open(fpath, "wb") as f:
                    f.write(resp)
                print(f"    [+] Saved {len(resp)} bytes to {fpath}")
                dump_results.append({
                    "elf_base": elf_base,
                    "runtime_addr": runtime_addr,
                    "file": fpath,
                    "size": len(resp)
                })

                # Quick sanity check: PPC instructions should be 4-byte aligned
                # and common opcodes include 0x38 (li), 0x7C (various), 0x48 (b/bl)
                if len(resp) >= 4:
                    first_word = struct.unpack(">I", resp[:4])[0]
                    print(f"    First instruction word: 0x{first_word:08X}")
                    opcode = (first_word >> 26) & 0x3F
                    print(f"    PPC opcode: {opcode} (valid opcodes: 14=addi, 31=various, 18=b/bl)")
            else:
                print(f"    [!] Empty or short response ({len(resp) if resp else 0} bytes)")

        except Exception as e:
            print(f"    [!] Memory read failed: {e}")

    # Also try to dump the static data section (XOR key + IV)
    if data_base:
        for elf_base in [0x00150000, 0x00100000, 0x00000000]:
            if STATIC_DATA_START < elf_base:
                continue
            offset = STATIC_DATA_START - elf_base
            runtime_addr = data_base + offset
            dump_size = STATIC_DATA_END - STATIC_DATA_START

            print(f"\n[*] Dumping static data (XOR key + IV) from data segment:")
            print(f"    ELF base: 0x{elf_base:08X}, runtime: 0x{runtime_addr:016X}")

            try:
                resp = api.send_raw(f"MEMORY GET {vsh_pid} {runtime_addr} {dump_size}")
                if resp and len(resp) > 0:
                    fname = f"sysconf_static_data_base{elf_base:08X}.bin"
                    fpath = os.path.join(OUTPUT_DIR, fname)
                    with open(fpath, "wb") as f:
                        f.write(resp)
                    print(f"    [+] Saved {len(resp)} bytes to {fpath}")
                    if len(resp) >= 32:
                        print(f"    XOR key: {resp[:16].hex()}")
                        print(f"    IV base: {resp[16:32].hex()}")
            except Exception as e:
                print(f"    [!] Error: {e}")

    api.close()
    return dump_results


def phase3_webman_peek(ip, vsh_pid, text_base, elf_base=0x00100000):
    """Alternative Phase 3: Read memory via webMAN's HTTP peek interface."""
    print("\n" + "=" * 60)
    print("PHASE 3 (alt): Memory Read via webMAN HTTP")
    print("=" * 60)

    offset = TARGET_START - elf_base
    runtime_addr = text_base + offset

    print(f"[*] Runtime address: 0x{runtime_addr:016X}")
    print(f"[*] Reading {TARGET_SIZE} bytes in 4-byte chunks...")

    # webMAN's peek reads 4 bytes at a time
    # URL format: http://<ip>/peek.lv2?<hex_address>
    # But that's for LV2 (kernel) memory, not process memory
    # For process memory: http://<ip>/getmem.ps3mapi?proc=<pid>&addr=<hex>&len=<hex>

    url = f"http://{ip}/getmem.ps3mapi?proc={vsh_pid}&addr={runtime_addr:X}&len={TARGET_SIZE:X}"
    print(f"[*] Trying: {url}")

    try:
        req = urllib.request.Request(url, headers={"User-Agent": "PsOldRemotePlay/1.0"})
        resp = urllib.request.urlopen(req, timeout=30)
        data = resp.read()
        fpath = os.path.join(OUTPUT_DIR, "sysconf_reghandler_webman.bin")
        with open(fpath, "wb") as f:
            f.write(data)
        print(f"[+] Saved {len(data)} bytes to {fpath}")
        return data
    except Exception as e:
        print(f"[!] webMAN getmem failed: {e}")

    # Fallback: try peek.lv2 in a loop (slower, 4 bytes at a time, and this is LV2 memory not process memory)
    print("\n[*] Trying peek.lv2 approach (LV2 address space, might not work for process memory)...")
    result = bytearray()
    for addr_offset in range(0, min(TARGET_SIZE, 256), 4):  # Only try first 256 bytes as test
        addr = runtime_addr + addr_offset
        peek_url = f"http://{ip}/peek.lv2?{addr:016X}"
        try:
            req = urllib.request.Request(peek_url, headers={"User-Agent": "PsOldRemotePlay/1.0"})
            resp = urllib.request.urlopen(req, timeout=5)
            html = resp.read().decode(errors="replace")
            # Parse the value from the HTML response
            # webMAN returns HTML with the value in it
            for line in html.split("\n"):
                if "0x" in line.lower() or any(c in "0123456789abcdefABCDEF" for c in line.strip()):
                    print(f"    0x{addr:016X}: {line.strip()[:40]}")
                    break
        except Exception as e:
            print(f"    0x{addr:016X}: error — {e}")
            break

    return None


def phase4_analysis_helper(dump_file):
    """Phase 4: Analyze dump and create Ghidra import script."""
    print("\n" + "=" * 60)
    print("PHASE 4: Analysis Helper")
    print("=" * 60)

    if not dump_file or not os.path.exists(dump_file):
        print("[!] No dump file to analyze.")
        return

    with open(dump_file, "rb") as f:
        data = f.read()

    print(f"[*] Dump size: {len(data)} bytes")
    print(f"[*] First 64 bytes (hex):")
    for i in range(0, min(len(data), 64), 16):
        hex_str = " ".join(f"{b:02X}" for b in data[i:i+16])
        ascii_str = "".join(chr(b) if 32 <= b < 127 else "." for b in data[i:i+16])
        print(f"    {i:04X}: {hex_str:48s} {ascii_str}")

    # Look for known patterns
    known_xor_key = bytes([0xF1, 0x16, 0xF0, 0xDA, 0x44, 0x2C, 0x06, 0xC2,
                           0x45, 0xB1, 0x5E, 0x48, 0xF9, 0x04, 0xE3, 0xE6])
    known_iv = bytes([0x29, 0x0D, 0xE9, 0x07, 0xE2, 0x3B, 0xE2, 0xFC,
                      0x34, 0x08, 0xCA, 0x4B, 0xDE, 0xE4, 0xAF, 0x3A])

    if known_xor_key in data:
        offset = data.index(known_xor_key)
        print(f"\n[!!!] Found known XOR key at offset 0x{offset:X}!")
    if known_iv in data:
        offset = data.index(known_iv)
        print(f"[!!!] Found known IV base at offset 0x{offset:X}!")

    # Check if this looks like valid PPC code
    ppc_opcodes = 0
    for i in range(0, len(data) - 3, 4):
        word = struct.unpack(">I", data[i:i+4])[0]
        opcode = (word >> 26) & 0x3F
        # Common PPC opcodes: 14(addi), 15(addis), 18(b/bl), 19(bclr etc),
        # 21(rlwinm), 24(ori), 31(various), 32(lwz), 34(lbz), 36(stw), 38(stb), 40(lhz)
        if opcode in [14, 15, 18, 19, 21, 24, 31, 32, 34, 36, 38, 40, 44, 46]:
            ppc_opcodes += 1

    total_words = len(data) // 4
    ratio = ppc_opcodes / total_words if total_words > 0 else 0
    print(f"\n[*] PPC opcode analysis: {ppc_opcodes}/{total_words} words look like valid PPC ({ratio:.1%})")
    if ratio > 0.5:
        print("[+] This looks like valid PPC code!")
    elif ratio > 0.2:
        print("[?] Partially valid PPC — might be code + data mixed")
    else:
        print("[!] Doesn't look like PPC code. Wrong address or wrong ELF base?")

    # Create Ghidra import helper
    ghidra_script = f"""# Ghidra Import Instructions for sysconf_plugin memory dump
#
# 1. File → Import File → select the .bin file
# 2. Language: PowerPC:BE:64:A2-64addr (or PowerPC:BE:64:default)
# 3. Options → Base Address: 0x{TARGET_START:08X}
# 4. After import, press 'D' to disassemble at the start
# 5. Look for the key derivation at offset 0x{(KEY_DERIV_START - TARGET_START):X} from start
#    (Ghidra address: 0x{KEY_DERIV_START:08X})
#
# Key areas to examine:
#   0x00100168 — Registration handler entry point
#   0x00100560 — Key derivation loop (the formula we need)
#   0x0010058C — End of key derivation
#
# The formula from static ELF analysis was:
#   key[i] = (material[i] - i - 0x28) XOR static_key[i]
#
# But this might differ at runtime. Compare with the static ELF analysis.
"""
    script_path = os.path.join(OUTPUT_DIR, "ghidra_import_notes.txt")
    with open(script_path, "w") as f:
        f.write(ghidra_script)
    print(f"\n[+] Ghidra import notes saved to: {script_path}")


def main():
    print("=" * 60)
    print("  PS3MAPI Memory Dumper — sysconf_plugin Registration Handler")
    print(f"  Target: {PS3_IP}")
    print("=" * 60)
    print()
    print("IMPORTANT: The PS3 should be in registration mode (showing PIN)")
    print("           so sysconf_plugin is actively handling registration.")
    print()

    # Quick connectivity check
    print("[*] Checking PS3 connectivity...")
    for port, name in [(80, "webMAN HTTP"), (PS3MAPI_PORT, "PS3MAPI"), (21, "FTP")]:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(3)
            s.connect((PS3_IP, port))
            s.close()
            print(f"    Port {port} ({name}): OPEN")
        except:
            print(f"    Port {port} ({name}): CLOSED/FILTERED")

    # Phase 1: PS3MAPI enumeration
    results = phase1_ps3mapi(PS3_IP)

    # Save enumeration results
    if results:
        results_path = os.path.join(OUTPUT_DIR, "ps3mapi_enumeration.json")
        # Convert to JSON-serializable
        json_results = json.dumps(results, indent=2, default=str)
        with open(results_path, "w") as f:
            f.write(json_results)
        print(f"\n[+] Enumeration saved to {results_path}")

    # Phase 2: webMAN HTTP recon
    phase2_webman_recon(PS3_IP)

    # Phase 3: Memory dump
    if results and results.get("sysconf_info"):
        sysconf = results["sysconf_info"]
        segments = sysconf.get("segments", [])
        vsh_pid = results["vsh_pid"]

        if len(segments) >= 1:
            text_base = segments[0]["base"]
            data_base = segments[1]["base"] if len(segments) >= 2 else None
            dump_results = phase3_memory_dump(PS3_IP, vsh_pid, text_base, data_base)

            if dump_results:
                for dr in dump_results:
                    phase4_analysis_helper(dr["file"])
            else:
                # Try webMAN HTTP approach
                phase3_webman_peek(PS3_IP, vsh_pid, text_base)
        else:
            print("\n[!] No segment info for sysconf_plugin.")
            print("    Try reading segments manually:")
            print(f"    MODULE GETSEGMENTS {vsh_pid} {sysconf.get('prx_id', '?')}")
    else:
        print("\n[!] sysconf_plugin not found. Manual investigation needed.")
        print()
        print("Try these manual steps:")
        print(f"  1. telnet {PS3_IP} {PS3MAPI_PORT}")
        print("  2. Type: PROCESS GETALLPID")
        print("  3. For each PID: PROCESS GETNAME <pid>")
        print("  4. Find VSH, then: MODULE GETALLPRXID <vsh_pid>")
        print("  5. For each module: MODULE GETNAME <vsh_pid> <prx_id>")
        print("  6. Find sysconf, then: MODULE GETSEGMENTS <vsh_pid> <prx_id>")
        print()
        print("Or try webMAN's web interface:")
        print(f"  http://{PS3_IP}/home.ps3mapi")
        print(f"  http://{PS3_IP}/cpursx.ps3")

    print("\n" + "=" * 60)
    print("DONE. Check the output files in:", OUTPUT_DIR)
    print("=" * 60)


if __name__ == "__main__":
    main()
