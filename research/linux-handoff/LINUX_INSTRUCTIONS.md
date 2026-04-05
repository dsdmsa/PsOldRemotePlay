# PS3 Remote Play - VAIO Registration Hook on Linux

## Goal
Capture the 8-byte AES IV context value from VRPSDK.dll during PS3 Remote Play registration.
This is the ONLY missing piece to make the open-source PS3 Remote Play client work.

## What This Solves
The PS3 Remote Play registration protocol uses AES-128-CBC encryption. We know:
- All static crypto keys (extracted from PS3 firmware)
- Key derivation formulas (verified from decompiled PPC assembly)
- IV base values (3 device types)
- The IV context is 8 bytes XOR'd into the IV base

We do NOT know the exact 8-byte IV context value. 22 PIN encodings tested, all failed.

## What We Have
A custom C tool (`hook_registration.exe`) that:
1. Loads Sony's VRPSDK.dll (Themida-packed COM DLL from VAIO Remote Play)
2. Bypasses the VAIO hardware check (patches Initialize function)
3. Hooks AES SetKey/SetIV/Encrypt functions via int3 breakpoints + VEH
4. Creates COM object and calls StartRegistration
5. Logs the exact AES key and IV used during registration

## What Happened on macOS
- Tool works: VRPSDK.dll loads, Themida unpacks, COM object created, Init bypassed
- BUT: StartRegistration fails with 0x8004A021 ("WiFi adapter not found")
- VRPSDK needs WiFi to connect to PS3's registration AP before doing crypto
- Wine on macOS can't manage WiFi adapters

## Why Linux Is Better
1. Wine on Linux has much better WMI support (reads /sys/class/dmi/)
2. Linux can manage WiFi from command line (iwconfig, nmcli, wpa_supplicant)
3. If Linux has WiFi adapter near PS3, VRPSDK can complete registration
4. We can fake /sys/class/dmi/id/sys_vendor to skip VAIO check entirely

---

## SETUP INSTRUCTIONS

### Step 1: Install Wine (32-bit support required)

```bash
# Ubuntu/Debian:
sudo dpkg --add-architecture i386
sudo apt update
sudo apt install wine32 wine64 winetricks

# Fedora:
sudo dnf install wine.i686 wine.x86_64 winetricks

# Arch:
sudo pacman -S wine wine-mono winetricks
```

Verify: `wine --version` (need Wine 7.0+ ideally)

### Step 2: Extract the package

```bash
tar xzf ps3_vaio_hook_package.tar.gz
cd ps3_vaio_hook
```

### Step 3: Set up Wine prefix

```bash
export WINEPREFIX=~/.wine_vaio
export WINEARCH=win32
wine wineboot --init

# Create VAIO directory
mkdir -p "$WINEPREFIX/drive_c/Program Files/Sony/Remote Play with PlayStation 3/ENG"

# Copy VAIO files
cp vaio_app/* "$WINEPREFIX/drive_c/Program Files/Sony/Remote Play with PlayStation 3/"
cp vaio_app/VRPRes_en.dll "$WINEPREFIX/drive_c/Program Files/Sony/Remote Play with PlayStation 3/ENG/VRPRes.dll"

# Register COM DLLs
cd "$WINEPREFIX/drive_c/Program Files/Sony/Remote Play with PlayStation 3/"
wine regsvr32 VRPSDK.dll
wine regsvr32 VRPMapping.dll
```

### Step 4: Fake Sony manufacturer (OPTIONAL - may avoid needing the binary patch)

```bash
# If you have root, fake the DMI data:
sudo mkdir -p /tmp/fake_dmi
echo "Sony Corporation" | sudo tee /sys/class/dmi/id/sys_vendor
# If that doesn't work (read-only), the hook tool patches it at runtime anyway.

# Alternative: set Wine registry
wine reg add "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\SystemInformation" \
  /v SystemManufacturer /t REG_SZ /d "Sony Corporation" /f
```

### Step 5: Check WiFi adapter

```bash
# List WiFi interfaces
ip link show | grep -i wl
iwconfig 2>/dev/null
nmcli device status
```

If no WiFi adapter exists, see "WITHOUT WIFI" section below.

### Step 6: Put PS3 in registration mode

On PS3: **Settings > Network > Remote Play > Register Device > PC**

The PS3 will:
1. Display an 8-digit PIN (e.g., 12345678)
2. Create a WiFi AP named `PS3REGPC_XXXX` (where XXXX = first 4 digits of PIN)
3. WiFi password = last 4 digits + first 4 digits (e.g., PIN 12345678 -> password 56781234)
4. Wait for a connection on port 9293

### Step 7: Connect to PS3 WiFi AP

```bash
# Method 1: nmcli
nmcli device wifi connect "PS3REGPC_1234" password "56781234"

# Method 2: wpa_supplicant
cat > /tmp/ps3_wifi.conf << 'EOF'
network={
    ssid="PS3REGPC_1234"
    psk="56781234"
    key_mgmt=WPA-PSK
}
EOF
sudo wpa_supplicant -i wlan0 -c /tmp/ps3_wifi.conf -B
sudo dhclient wlan0

# Method 3: iwconfig (for open/WEP - PS3 may use ad-hoc)
sudo iwconfig wlan0 essid "PS3REGPC_1234" key s:56781234
sudo dhclient wlan0
```

Note: PS3's IP on the registration AP is typically **192.168.1.1**

### Step 8: Run the hook tool

```bash
cd ps3_vaio_hook
wine hook_registration.exe <PIN_FROM_PS3>
# Example:
wine hook_registration.exe 12345678
```

### Step 9: Read the output

If successful, you'll see:
```
=== [HOOK] SetKey #1 ===
  AES KEY: XX XX XX XX XX XX XX XX XX XX XX XX XX XX XX XX

=== [HOOK] SetIV #1 ===
  AES IV:  XX XX XX XX XX XX XX XX XX XX XX XX XX XX XX XX
  *** THIS IS THE DERIVED IV - XOR with iv_base to get iv_context! ***
```

The IV context = captured_IV XOR iv_base_PC:
```
iv_base_PC = 5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04
iv_context = first 8 bytes of (captured_IV XOR iv_base_PC)
```

---

## WITHOUT WIFI (LAN-only approach)

If the Linux machine has no WiFi but IS on the same LAN as the PS3, we need to build a "WiFi shim" that:
1. Fakes VRPSDK's WiFi scanning to return a fake PS3 AP
2. Redirects the TCP connection to the PS3's LAN IP instead of WiFi IP

This requires modifying hook_registration.c to also hook:
- WlanScan / WlanGetAvailableNetworkList (fake WiFi results)
- WlanConnect (pretend to connect)
- connect() / WSAConnect (redirect to PS3's LAN IP)

Ask Claude to implement this - provide this document as context.

Alternatively, use a USB WiFi adapter plugged into the Linux machine.

---

## ALTERNATIVE: Run VRP.exe directly (easier if Wine is stable)

Instead of our custom hook tool, you can try running the original VAIO Remote Play:

```bash
cd "$WINEPREFIX/drive_c/Program Files/Sony/Remote Play with PlayStation 3/"
# Apply VRPPatch
cp /path/to/rmp_launcher.EXE .
cp /path/to/rmp_dll.DLL .
# Run via patch launcher
wine rmp_launcher.EXE
```

If the GUI appears and you can initiate registration, run Wireshark in parallel:
```bash
sudo tcpdump -i wlan0 -w /tmp/ps3_registration.pcap port 9293
```

Then apply known-plaintext attack to recover IV from the captured traffic.

---

## ALTERNATIVE: Frida dynamic instrumentation

If Wine + hook tool doesn't work, try Frida:

```bash
pip3 install frida frida-tools

# Start VRPSDK in background
wine VRP.exe &

# Attach Frida
python3 frida_hook_vaio.py
```

The Frida script hooks CryptSetKeyParam (KP_IV) and CryptEncrypt to capture crypto params.

---

## TECHNICAL DETAILS

### VRPSDK.dll COM Interface

**CLSID:** `{1BD8D6AE-6EE1-42D9-A307-252FFAD207AD}`
**ProgID:** `RPSDK.CoreInterface`

**Vtable layout (IDispatch-based):**
```
Slot  Offset  DispID  Method
0     0x00    -       QueryInterface
1     0x04    -       AddRef
2     0x08    -       Release
3     0x0C    -       GetTypeInfoCount
4     0x10    -       GetTypeInfo
5     0x14    -       GetIDsOfNames
6     0x18    -       Invoke
7     0x1C    1       NInitialize(BSTR path, BSTR config)
8     0x20    2       UnInitialize()
9     0x24    3       StartRegistration(BSTR pin, int type, int flags, BSTR addr, BSTR nick)
10    0x28    4       CancelRegistration()
11    0x2C    5       StartRemotePlay(...)
12    0x30    6       StopRemotePlay()
13    0x34    7       SendTurnOffPS3Signal()
14    0x38    8       SendKey(...)
...
17    0x44    15      GetVideoBitrate()
18    0x48    16      SetVideoBitrate(...)
```

### AES Function Addresses (relative to VRPSDK.dll base)

These are in the UNPACKED code section (Section 0), NOT in Themida VM:
```
SetKey           = base + 0x1D60  (prologue: 55 8B EC = push ebp; mov ebp,esp)
SetIV            = base + 0x1010  (prologue: 55 8B EC)
EncryptMultiBlock= base + 0x1E60  (prologue: 55 8B EC)
DllGetClassObject= base + 0x19A30 (exported by name)
Initialize       = base + 0x8FD0  (vtable slot 7)
StartRegistration= base + 0x90D0  (vtable slot 9)
```

### Initialize Function VAIO Check Bypass

The Initialize function at base+0x8FD0 has this structure:
```
Offset 0x65: E8 xx xx xx xx    ; call VAIO_check_function
Offset 0x6A: 85 C0             ; test eax, eax
Offset 0x6C: 74 07             ; jz +7 (skip error if check returned 0)
Offset 0x6E: BB 04 A0 04 80    ; mov ebx, 0x8004A004 (VAIO check failed)
Offset 0x73: EB 70             ; jmp epilogue
Offset 0x75: 6A 01 ...         ; (success path continues)
```

**Fix:** Patch byte at func+0x6C from 0x74 (jz) to 0xEB (jmp unconditional)

Additionally, patch ALL `BB xx A0 04 80` (mov ebx, error) patterns within the function to `BB 00 00 00 00` (mov ebx, 0 = S_OK).

The hook tool does this automatically.

### Error Codes
```
0x8004A001 - First initialization sub-check failed
0x8004A002 - Second sub-check failed
0x8004A003 - Third sub-check failed
0x8004A004 - VAIO hardware check failed (WMI Manufacturer != "Sony Corporation")
0x8004A021 - WiFi adapter not available / scan failed
0x8004AA00 - COM object not initialized ([this+0x24] == NULL)
0x80020009 - DISP_E_EXCEPTION (IDispatch method raised exception)
0x8002000E - DISP_E_BADPARAMCOUNT (wrong number of IDispatch args)
```

### rmp_dll.DLL Patch Mechanism

The 4KB patch DLL works by:
1. On DLL_PROCESS_ATTACH, gets address of `lstrcmpW` from kernelbase.dll
2. Patches lstrcmpW with `JMP` to a hook function
3. Hook function makes "Sony Corporation" string comparisons always return 0 (match)

This is simpler than our binary patching approach but requires the DLL to be loaded.

### Registration Protocol (for context)

```
POST /sce/premo/regist HTTP/1.1
Content-Length: <size>

[AES-128-CBC encrypted body][16 raw key material bytes]
```

Plaintext body:
```
Client-Type: VITA\r\n
Client-Id: <32 hex chars>\r\n
Client-Mac: <12 hex chars>\r\n
Client-Nickname: PsOldRemotePlay\r\n
```

Key derivation (PC type):
```
key[i] = (km[i] ^ REG_XOR_PC[i]) - i - 0x2B
```

IV derivation:
```
iv = REG_IV_PC.copy()
iv[0..7] ^= iv_context   // <-- THIS IS WHAT WE NEED
```

Static keys:
```
REG_XOR_PC = EC 6D 70 6B 1E 0A 9A 75 8C DA 78 27 51 A3 C3 7B
REG_IV_PC  = 5B 64 40 C5 2E 74 C0 46 48 72 C9 C5 49 0C 79 04
```

### PS3 WiFi AP Details

During registration, PS3 creates:
- **SSID:** `PS3REGPC_XXXX` (XXXX = first 4 PIN digits)
- **Password:** Last 4 PIN digits + First 4 PIN digits
- **PS3 IP:** 192.168.1.1 (usually)
- **Port:** 9293
- **Protocol:** HTTP (NOT HTTPS)
- **Security:** WPA-PSK (may vary by firmware)

---

## FILES INCLUDED IN THIS PACKAGE

```
ps3_vaio_hook/
├── LINUX_INSTRUCTIONS.md          # This file
├── hook_registration.c            # Custom hook tool source
├── hook_registration.exe          # Pre-compiled hook tool (PE32 i386)
├── frida_hook_vaio.py             # Frida-based alternative hook
├── vaio_app/                      # VAIO Remote Play application files
│   ├── VRPSDK.dll                 # Main COM DLL (Themida-packed, 1.3MB)
│   ├── VRP.exe                    # Original VAIO GUI app
│   ├── VRPMFMGR.dll               # Media framework
│   ├── VRPMapping.dll              # Controller mapping (COM)
│   ├── UFCore.dll                  # Sony UI framework
│   ├── sonyjvtd.dll                # Sony H.264 decoder
│   ├── Resource.dll                # UI resources
│   ├── VRPRes_en.dll               # English language resources
│   ├── rmp_launcher.EXE            # VRPPatch launcher
│   ├── rmp_dll.DLL                 # VRPPatch (bypasses VAIO check via lstrcmpW hook)
│   └── VRPPatch.dll                # Alternative patch by NTAuthority
├── tools/
│   ├── ps3_register_bruteforce_iv.py  # IV brute-force tester
│   ├── ps3_wifi_connect.sh            # WiFi connection helper script
│   └── test_constant_contexts.py      # Constant IV context tester
└── research_docs/
    ├── 22_VAIO_DLL_ANALYSIS.md        # Complete VAIO RE analysis
    ├── 20_DECOMPILED_REGISTRATION_HANDLER.md  # PS3 firmware decompilation
    └── 08_COMPLETE_PROTOCOL_SUMMARY.md # Full protocol reference
```

---

## IF REGISTRATION SUCCEEDS

Once you capture the AES IV, compute the iv_context:

```python
iv_base_pc = bytes([0x5B,0x64,0x40,0xC5,0x2E,0x74,0xC0,0x46,
                    0x48,0x72,0xC9,0xC5,0x49,0x0C,0x79,0x04])
captured_iv = bytes.fromhex("PASTE_CAPTURED_IV_HERE")
iv_context = bytes(a ^ b for a, b in zip(captured_iv[:8], iv_base_pc[:8]))
print(f"IV Context: {iv_context.hex()}")
```

Then test it against your PS3:
```bash
python3 tools/ps3_register_bruteforce_iv.py --iv-context <hex_value> --ps3-ip <PS3_IP>
```

This value is UNIVERSAL — same for all PS3 consoles, all firmware versions. Once found, the open-source client works for everyone.
