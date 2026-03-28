# PS3 Firmware Extraction & premo_plugin.sprx Decompilation

## VERDICT: Fully feasible on macOS/Linux. All tools exist, all keys are public.

---

## Why This Matters

The `premo_plugin.sprx` on the PS3 handles Remote Play registration. If we decompile it, we understand the registration protocol and can implement it natively in our Android app — no Windows, no PSP, no VAIO needed.

## Filesystem Location

- `/dev_flash/vsh/module/premo_plugin.sprx` — handles registration/pairing from XMB
- `/dev_flash/vsh/module/premo_game_plugin.sprx` — handles Remote Play during gameplay
- Plugin ID: `0x1A` in the XMB system
- Launched via XMB segment `seg_premo`

## What We Already Know from webman-mod Source

| File | Key Information |
|------|----------------|
| `webman-mod/vsh/xregistry.h` | `savePremoPSPInformation(void*)` and `loadPremoPSPInformation(void*)` — **0x4B8 byte structure** for PSP pairing data |
| `webman-mod/vsh/xmb_plugin.h` | `premo_plugin = 0x1A` (XMB plugin ID) |
| `PS3 Patches.txt` lines 549-557 | Binary patches at offsets **0xB7E4 / 0xC9E4** changing `li r3, 0` → `li r3, 1` (enables Windows Remote Play) |

The 0x4B8-byte structure and the `savePremoPSPInformation`/`loadPremoPSPInformation` functions are the key targets for understanding registration.

---

## Step-by-Step Pipeline: PUP → Decrypted ELF → Ghidra

### Step 1: Download PS3 Firmware

Official firmware from Sony: https://www.playstation.com/en-us/support/hardware/ps3/system-software/
Current latest: 4.91.2 (`PS3UPDAT.PUP`)

### Step 2: Extract the PUP File

**Option A: fail0verflow ps3tools (C, recommended)**
```bash
git clone https://github.com/daryl317/fail0verflow-PS3-tools
cd fail0verflow-PS3-tools && make
./pupunpack PS3UPDAT.PUP extracted/
```
- GitHub: https://github.com/daryl317/fail0verflow-PS3-tools
- Also: https://github.com/manusan/ps3tools
- Compiles on macOS/Linux with `make` (requires zlib)

**Option B: PFU-PupFileUnpacker (Python, cross-platform)**
- GitHub: https://github.com/seregonwar/PFU-PupFileUnpacker

**Option C: PS3MFW Builder (Tcl, full extraction)**
- GitHub: https://github.com/BitEdits/ps3mfw
- Fully extracts PUP → tar → dev_flash

### Step 3: Extract dev_flash from tar

```bash
tar xf extracted/update_files.tar -C extracted/
# Result: extracted/dev_flash/vsh/module/premo_plugin.sprx
```

### Step 4: Decrypt SPRX → ELF

**Why this works:** PS3 LV0 keys were leaked in 2012. All firmware encryption keys for ALL firmware versions are publicly known.

**Tool: scetool (original)**
```bash
git clone https://github.com/naehrwert/scetool
cd scetool && make
# Place keys in data/ directory
./scetool --decrypt premo_plugin.sprx premo_plugin.elf
./scetool --decrypt premo_game_plugin.sprx premo_game_plugin.elf
```
- GitHub: https://github.com/naehrwert/scetool
- Forks: https://github.com/ErikPshat/scetool, https://github.com/Zarh/scetool

**Tool: ps3sce (modern open-source replacement)**
```bash
git clone https://github.com/BitEdits/ps3sce
# Same syntax: ./ps3sce --decrypt premo_plugin.sprx premo_plugin.elf
```
- GitHub: https://github.com/BitEdits/ps3sce

**Tool: fail0verflow unself**
```bash
./unself premo_plugin.sprx premo_plugin.elf
```

**Output:** Standard PowerPC 64-bit Big Endian ELF file

### Step 5: Decompile in Ghidra

**Ghidra natively supports PS3 binaries.** No plugins required (but helpers exist).

```
1. Import premo_plugin.elf into Ghidra
2. Processor: PowerISA-Altivec-64-32addr (Big Endian)
   (auto-detected when you uncheck "recommended only")
3. Run auto-analysis
4. Install PS3 Ghidra scripts and run syscall define script
5. Focus on functions near offset 0xB7E4 and the save/loadPremoPSPInformation handlers
```

**PS3 Ghidra Scripts:**
- https://github.com/clienthax/Ps3GhidraScripts — syscall naming, TOC resolution, OPD parsing
- https://github.com/ZephyrCodesStuff/ps3-ghidra-scripts — alternative

**Critical Ghidra fix:** Edit `Ghidra/Processors/PowerPC/data/languages/ppc_64_32.cspec`, add `<register name="r2"/>` to the `<unaffected>` list. This prevents r2 (TOC pointer) from being treated as volatile, dramatically improving decompilation quality.

**Known limitations:**
- Some Cell/SPU vector instructions (`lvlx`) not supported — may cause gaps
- Function names will be generic unless you cross-reference with webman-mod headers

---

## Shortcut: Pre-Disassembled Firmware Files

The repo [skrptktty/ps3-firmware-beginners-luck](https://github.com/skrptktty/ps3-firmware-beginners-luck) already contains:
- `kmeaw/kmeaw_exploded/update_files/dev_flash/vsh/module/premo_game_plugin.sprx.asm` — disassembly of premo_game_plugin
- Various exploded firmware files from FW 3.55 and 4.11
- Includes ps3tools for building/extracting

This is an older firmware (3.55 era), but the registration protocol is unlikely to have changed significantly.

---

## All Required Tools

| Tool | Repository | Purpose | Platform |
|------|-----------|---------|----------|
| fail0verflow ps3tools | https://github.com/daryl317/fail0verflow-PS3-tools | PUP extraction, SELF→ELF | macOS/Linux |
| scetool | https://github.com/naehrwert/scetool | SPRX/SELF decryption | macOS/Linux |
| ps3sce | https://github.com/BitEdits/ps3sce | Modern scetool replacement | macOS/Linux |
| PS3MFW Builder | https://github.com/BitEdits/ps3mfw | Full PUP extraction | macOS/Linux |
| PFU-PupFileUnpacker | https://github.com/seregonwar/PFU-PupFileUnpacker | Python PUP extractor | Any (Python) |
| Ps3GhidraScripts | https://github.com/clienthax/Ps3GhidraScripts | Ghidra PS3 extension | Any (Ghidra) |
| ps3-ghidra-scripts | https://github.com/ZephyrCodesStuff/ps3-ghidra-scripts | Alt Ghidra PS3 scripts | Any (Ghidra) |
| Firmware samples | https://github.com/skrptktty/ps3-firmware-beginners-luck | Pre-exploded FW with premo ASM | N/A |
| Ghidra | https://ghidra-sre.org/ | Decompiler | macOS/Linux/Windows |

---

## What to Look For in Ghidra

1. **Registration handler** — functions near offset 0xB7E4 (the patch point for Windows Remote Play enabler)
2. **`savePremoPSPInformation`** — writes the 0x4B8-byte pairing structure to xRegistry
3. **`loadPremoPSPInformation`** — reads it back for session validation
4. **Key generation** — where does the PS3 generate the random pkey during registration?
5. **Device validation** — does the PS3 check the PSPID format/signature? (FW 2.80+ blocked non-Sony devices)
6. **PIN handling** — how is the 8-digit PIN used in the WiFi registration flow?
7. **Crypto functions** — AES, ECDSA, or other crypto used during the handshake

## Sources
- psdevwiki: Remote Play — https://www.psdevwiki.com/ps3/Remote_Play
- psdevwiki: Premo plugin — https://www.psdevwiki.com/ps3/Premo_plugin
- psdevwiki: SPRX format — https://www.psdevwiki.com/ps3/SPRX
- webman-mod source — https://github.com/aldostools/webMAN-MOD
