#!/usr/bin/env python3
"""
frida_hook_vaio.py - Dynamically hook VAIO DLL to capture IV context

This script uses Frida to inject hooks into a running VAIO process and capture:
  1. StartRegistration(PIN, ...) calls and their parameters
  2. Crypto key material and IV context setup
  3. AES encryption/decryption operations
  4. CryptEncrypt calls to log plaintext + ciphertext

The goal is to definitively determine what the 8-byte IV context value is
by hooking the registration flow and logging key material as it's used.

Requirements:
  pip install frida frida-tools

Usage:
  1. Start VAIO installer in Wine:
     WINEPREFIX=~/.wine_vaio wine research/repos/ps3-remote-play/vaio_installer.exe &

  2. Launch this hook script (must be while VAIO process is running):
     python3 research/re-windows/frida_hook_vaio.py

The script will attach to the VAIO process and log all registration attempts.
"""

import frida
import sys
import time
from typing import Optional

# Frida JavaScript injection code - hooks into VAIO DLL
HOOK_SCRIPT = """
var MODULE_NAME = "VRPSDK.dll";
var base = null;

// Try to find VRPSDK.dll module
var modules = Process.enumerateModules();
for (var i = 0; i < modules.length; i++) {
    if (modules[i].name.indexOf("VRPSDK") !== -1) {
        console.log("[+] Found module: " + modules[i].name + " @ " + modules[i].base);
        base = modules[i].base;
        break;
    }
}

if (!base) {
    console.log("[-] VRPSDK.dll not loaded yet. Waiting for it...");
    var count = 0;
    while (!base && count < 30) {
        count += 1;
        var modules = Process.enumerateModules();
        for (var i = 0; i < modules.length; i++) {
            if (modules[i].name.indexOf("VRPSDK") !== -1 ||
                modules[i].name.indexOf("VRPMFMGR") !== -1) {
                base = modules[i].base;
                console.log("[+] Found module after " + count + " iterations: " + modules[i].name);
                break;
            }
        }
        if (!base) {
            console.log("[*] Waiting for DLL load...");
            send({"type": "status", "message": "Waiting for VRPSDK.dll to load..."});
        }
    }
}

if (!base) {
    console.log("[-] VRPSDK.dll never loaded. Is VAIO running?");
    send({"type": "error", "message": "VRPSDK.dll not found"});
} else {
    console.log("[+] Ready to hook VRPSDK.dll");
}

// Hook CryptEncrypt from advapi32 (Windows crypto API)
var CryptEncrypt = Module.findExportByName("advapi32.dll", "CryptEncrypt");
if (CryptEncrypt) {
    console.log("[+] Hooking CryptEncrypt @ " + CryptEncrypt);
    Interceptor.attach(CryptEncrypt, {
        onEnter: function(args) {
            // CryptEncrypt signature:
            // BOOL CryptEncrypt(
            //   HCRYPTKEY  hKey,
            //   HCRYPTHASH hHash,
            //   BOOL       Final,
            //   DWORD      dwFlags,
            //   BYTE       *pbData,
            //   DWORD      *pdwDataLen,
            //   DWORD      dwBufLen
            // );
            this.hKey = args[0];
            this.pbData = args[4];
            this.pdwDataLen = args[5];
            this.dwBufLen = args[6];

            // Try to read plaintext data (before encryption)
            try {
                var dataLen = this.pdwDataLen.readU32();
                if (dataLen > 0 && dataLen < 2048) {
                    var plaintext = Memory.readByteArray(this.pbData, dataLen);
                    send({
                        "type": "crypto",
                        "operation": "CryptEncrypt_enter",
                        "hKey": this.hKey.toString(),
                        "dataLen": dataLen,
                        "plaintext": plaintext
                    });
                }
            } catch(e) {
                console.log("[-] Error reading CryptEncrypt plaintext: " + e);
            }
        },
        onLeave: function(retval) {
            // After encryption, read ciphertext
            try {
                if (this.pdwDataLen && this.pbData) {
                    var dataLen = this.pdwDataLen.readU32();
                    if (dataLen > 0 && dataLen < 2048) {
                        var ciphertext = Memory.readByteArray(this.pbData, dataLen);
                        send({
                            "type": "crypto",
                            "operation": "CryptEncrypt_leave",
                            "hKey": this.hKey.toString(),
                            "dataLen": dataLen,
                            "ciphertext": ciphertext,
                            "retval": retval.toString()
                        });
                    }
                }
            } catch(e) {
                console.log("[-] Error reading CryptEncrypt ciphertext: " + e);
            }
        }
    });
} else {
    console.log("[-] CryptEncrypt not found (may not be called by VAIO)");
}

// Hook CryptSetKeyParam to capture key material
var CryptSetKeyParam = Module.findExportByName("advapi32.dll", "CryptSetKeyParam");
if (CryptSetKeyParam) {
    console.log("[+] Hooking CryptSetKeyParam");
    Interceptor.attach(CryptSetKeyParam, {
        onEnter: function(args) {
            // CryptSetKeyParam signature:
            // BOOL CryptSetKeyParam(
            //   HCRYPTKEY hKey,
            //   DWORD     dwParam,  // KP_IV = 4, KP_SALT = 2, KP_EFFECTIVE_KEYLEN = 19
            //   const BYTE *pbData,
            //   DWORD     dwFlags
            // );
            var hKey = args[0];
            var dwParam = args[1].toInt32();
            var pbData = args[2];
            var paramName = "UNKNOWN";

            if (dwParam === 2) paramName = "KP_SALT";
            else if (dwParam === 4) paramName = "KP_IV";
            else if (dwParam === 19) paramName = "KP_EFFECTIVE_KEYLEN";

            // For IV and SALT, try to read the data
            if (dwParam === 2 || dwParam === 4) {
                try {
                    var data = Memory.readByteArray(pbData, 16);
                    send({
                        "type": "crypto_param",
                        "operation": "CryptSetKeyParam",
                        "hKey": hKey.toString(),
                        "dwParam": dwParam,
                        "paramName": paramName,
                        "data": data
                    });
                } catch(e) {
                    console.log("[-] Error reading CryptSetKeyParam data: " + e);
                }
            }
        }
    });
} else {
    console.log("[-] CryptSetKeyParam not found");
}

// Hook memcpy/memset (common in crypto code) for PIN/context detection
var memcpy = Module.findExportByName("ntdll.dll", "memcpy");
if (!memcpy) memcpy = Module.findExportByName("msvcrt.dll", "memcpy");

if (memcpy) {
    console.log("[+] Hooking memcpy for data flow analysis");
    var call_count = 0;
    Interceptor.attach(memcpy, {
        onEnter: function(args) {
            // memcpy(dest, src, size)
            var dest = args[0];
            var src = args[1];
            var size = args[2].toInt32();

            // Only log "interesting" sizes (8 bytes for context, PIN-related)
            if (size === 8 || size === 16 || size === 32) {
                try {
                    var data = Memory.readByteArray(src, size);
                    // Only send every 100th call to reduce spam
                    call_count += 1;
                    if (call_count % 100 === 0) {
                        send({
                            "type": "memcpy",
                            "dest": dest.toString(),
                            "src": src.toString(),
                            "size": size,
                            "data": data
                        });
                    }
                } catch(e) {
                    // Ignore - memory may not be readable
                }
            }
        }
    });
} else {
    console.log("[*] memcpy not hooked (not critical)");
}

console.log("[SUCCESS] VAIO hooks installed. Waiting for registration calls...");
console.log("[INFO] Look for 'KP_IV' in crypto_param messages - that's the IV context!");
send({"type": "ready", "message": "Hooks ready, waiting for registration..."});
"""

class VaioFridaHooker:
    def __init__(self):
        self.session: Optional[frida.Session] = None
        self.script: Optional[frida.Script] = None

    def find_wine_process(self) -> Optional[int]:
        """Find VAIO process running under Wine."""
        try:
            processes = frida.enumerate_processes()
            for proc in processes:
                name = proc.name.lower()
                # Look for VAIO installer or VAIO Remote Play process
                if "vaio" in name or "vrpsdk" in name or "vaio" in proc.name:
                    print(f"[+] Found VAIO process: {proc.name} (PID: {proc.pid})")
                    return proc.pid
                # Also look for common Windows process names
                if "wineserver" in name or "explorer.exe" in name or "setup.exe" in name:
                    print(f"[*] Found potential Wine process: {proc.name} (PID: {proc.pid})")

            # If no specific VAIO process found, list Wine processes
            print("\n[*] Available processes:")
            for proc in processes:
                if any(x in proc.name.lower() for x in ["wine", "setup", "installer", "vaio"]):
                    print(f"    {proc.pid}: {proc.name}")

            return None
        except Exception as e:
            print(f"[-] Error enumerating processes: {e}")
            return None

    def attach_and_hook(self, pid: int):
        """Attach to process and install hooks."""
        print(f"[+] Attaching to process {pid}...")
        try:
            self.session = frida.attach(pid)
        except Exception as e:
            print(f"[-] Failed to attach: {e}")
            print("    Make sure VAIO process is running and you have permissions")
            return False

        print("[+] Creating Frida script...")
        self.script = self.session.create_script(HOOK_SCRIPT)
        self.script.on("message", self._on_message)

        print("[+] Loading script...")
        self.script.load()

        return True

    def _on_message(self, message, data):
        """Handle messages from Frida script."""
        if message["type"] == "send":
            payload = message["payload"]
            msg_type = payload.get("type")

            if msg_type == "status":
                print(f"[*] {payload.get('message')}")
            elif msg_type == "ready":
                print(f"[SUCCESS] {payload.get('message')}")
            elif msg_type == "error":
                print(f"[-] {payload.get('message')}")
            elif msg_type == "crypto_param":
                operation = payload.get("operation")
                dwParam = payload.get("dwParam")
                paramName = payload.get("paramName")
                data_val = payload.get("data")

                # Convert bytes to hex
                data_hex = ""
                if data_val:
                    try:
                        # data is already bytes
                        data_hex = bytes(data_val).hex()
                    except:
                        data_hex = str(data_val)

                if paramName == "KP_IV":
                    print(f"\n[!!!] CRITICAL: {operation}")
                    print(f"      IV context (8-16 bytes): {data_hex}")
                    print(f"      hKey: {payload.get('hKey')}")
                    print(f"      This is likely the missing IV context value!\n")
                else:
                    print(f"[CRYPTO] {paramName}: {data_hex}")

            elif msg_type == "crypto":
                operation = payload.get("operation")
                data_val = payload.get("data")
                data_hex = ""
                if data_val:
                    try:
                        data_hex = bytes(data_val).hex()
                    except:
                        data_hex = str(data_val)

                dataLen = payload.get("dataLen", 0)
                print(f"[CRYPTO] {operation}: {dataLen} bytes, {data_hex[:64]}")

            elif msg_type == "memcpy":
                data_val = payload.get("data")
                data_hex = ""
                if data_val:
                    try:
                        data_hex = bytes(data_val).hex()
                    except:
                        data_hex = str(data_val)
                size = payload.get("size")
                print(f"[MEMCPY] {size} bytes: {data_hex}")

    def run(self):
        """Main loop - wait for events."""
        print("\n[*] Waiting for registration events...")
        print("[*] Press Ctrl+C to stop\n")
        try:
            while True:
                time.sleep(0.1)
        except KeyboardInterrupt:
            print("\n\n[+] Detaching...")
            if self.session:
                self.session.detach()
            print("[SUCCESS] Done")

def main():
    print("""
╔════════════════════════════════════════════════════════════════╗
║ VAIO DLL Frida Hook - IV Context Capture                      ║
║                                                                ║
║ This will hook VAIO crypto operations and log:                ║
║  - CryptEncrypt plaintext/ciphertext                          ║
║  - CryptSetKeyParam calls (especially KP_IV)                 ║
║  - Memory operations on key material                         ║
║                                                                ║
║ Goal: Find the 8-byte IV context value for registration      ║
╚════════════════════════════════════════════════════════════════╝
    """)

    hooker = VaioFridaHooker()

    # Find VAIO process
    pid = hooker.find_wine_process()
    if not pid:
        print("\n[-] No VAIO process found.")
        print("[*] Make sure VAIO is running:")
        print("    WINEPREFIX=~/.wine_vaio wine vaio_installer.exe")
        print("\n[*] Then run this script again")
        sys.exit(1)

    # Attach and hook
    if not hooker.attach_and_hook(pid):
        print("[-] Failed to attach hooks")
        sys.exit(1)

    # Wait for events
    hooker.run()

if __name__ == "__main__":
    main()
