# PS3 Remote Play Key Extraction — Complete Research

## What Keys Are Needed

| Key | Size | Type | Source |
|-----|------|------|--------|
| **skey0** | 16 bytes | Static constant | Hardcoded in protocol — same for ALL PS3s |
| **skey1** | 16 bytes | Static constant | Hardcoded in protocol — same for ALL PS3s |
| **skey2** | 16 bytes | Static constant | Hardcoded in protocol — same for ALL PS3s |
| **pkey** | 16 bytes | Per-device | Generated during registration, stored on both PSP and PS3 |
| **psp_id** (OpenPSID) | 16 bytes | Per-device | Hardware-bound PSP identifier |
| **psp_mac** | 6 bytes | Per-device | Registered device's MAC address |
| **ps3_mac** | 6 bytes | Per-console | PS3's MAC address |
| **nonce** | 16 bytes | Per-session | Fresh, sent by PS3 in `PREMO-Nonce` header |

### Static Keys (Already Known — NOT a blocker)

From `keys/keys.cpp` in Open-RP source:
```
skey0: D1 B2 12 EB 73 86 6C 7B 12 A7 5E 0C 04 C6 B8 91
skey1: 1F D5 B9 FA 71 B8 96 81 B2 87 92 E2 6F 38 C3 6F
skey2: 65 2D 8C 90 DE 87 17 CF 4F B3 D8 D3 01 79 6B 59
```

### The Blocker: pkey, psp_id, psp_mac

These are the per-device secrets established during registration. The pkey is a shared secret between the registered device and the PS3.

## Key Derivation (from CreateKeys() in orp.cpp:1585)

1. `xor_pkey = pkey XOR skey0` — becomes the AES encryption key
2. `xor_nonce = nonce XOR skey2` — becomes the AES IV
3. Plaintext = `[psp_mac (6 bytes), 0x00 * 10 bytes]`
4. AES-128-CBC encrypt(plaintext, key=xor_pkey, IV=xor_nonce)
5. Base64(result) → `PREMO-Auth` header
6. Same xor_pkey/xor_nonce used for all stream packet decryption

## How Registration Works

1. PSP connects to PS3 (USB cable or WiFi with 8-digit PIN)
2. PS3 generates random 16-byte **pkey**
3. PS3 stores in xRegistry: PSP's MAC, PSP's OpenPSID, pkey at `/setting/premo/pspXX/key`
4. PSP stores: PS3's MAC, nickname, pkey at `/CONFIG/PREMO/ps3_key`
5. Both sides now share pkey as pre-shared secret

**Note:** After firmware 2.80, PS3 may validate PSPID against known valid PSP hardware IDs.

## Extraction Methods — Ranked by Simplicity

### Option A: PS3 HEN + VAIO Client + FTP (SIMPLEST, NO PSP NEEDED)

1. Install PS3 HEN (works on ANY PS3 model, any firmware via PS3Xploit)
2. Install webMAN-MOD (provides FTP server)
3. Install patched VAIO Remote Play on a Windows PC (from PS3-Pro/Remote-Play or VRPPatch)
4. Register the PC with PS3 via the VAIO client — this creates key entry in xRegistry
5. FTP to PS3, download `/dev_flash2/etc/xRegistry.sys`
6. Parse with xRegistry.sys Editor v0.75, extract from `/setting/premo/pspXX/`:
   - `key` → pkey
   - `id` → device PSPID
   - `macAddress` → device MAC
7. Hardcode these values in the Android app for POC

**Pros:** No PSP needed. HEN is easy. All tools exist.
**Cons:** Requires Windows PC for VAIO client. Requires PS3 with HEN.

**Tools:**
- xRegistry.sys Editor: https://ps3.brewology.com/downloads/download.php?id=10308&mcid=4
- xReg Plus v1.0 (Rebug homebrew, dumps to USB): Team Rebug
- PS3 HEN: https://consolemods.org/wiki/PS3:PS3HEN

### Option B: PS3 with HEN + Previously Registered Device

If a PSP/Vita was EVER registered with this PS3, the keys are already in xRegistry.sys. Just dump via FTP — no need to register again.

### Option C: PSP with CFW + ORP_Exporter (Original Method)

Run the pre-built EBOOT.PBP from `psp/ORP_Exporter/` on a homebrew-enabled PSP (or PS Vita with Adrenaline). Exports `ms0:/export.orp` with all keys.

**Pros:** Battle-tested, well-documented.
**Cons:** Requires a PSP/Vita with CFW.

### Option D: VAIO Client + Wireshark (Partial)

1. Run patched VAIO client, register with PS3
2. Capture session traffic with Wireshark
3. `PREMO-PSPID` and `PREMO-Auth` headers visible in plaintext HTTP
4. From auth + derivation algorithm, may be possible to recover pkey

**Limitation:** Registration traffic itself may use unknown protocol. Auth values are derived from key, not the key itself.

### Option E: Reverse-Engineer Registration Protocol (Hardest)

Capture full USB/WiFi registration traffic, reverse-engineer premo_plugin.sprx, implement custom registration. Would eliminate all hardware dependencies but significant effort.

## PS3 xRegistry Structure

Registration data stored at `/dev_flash2/etc/xRegistry.sys`:
```
/setting/premo/pspXX/
  ├── nickname    — device name
  ├── macAddress  — device MAC (6 bytes)
  ├── id          — device PSPID (16 bytes)
  ├── keyType     — key type
  └── key         — the pkey (16 bytes, the shared secret)
```

## Open Questions

1. **Does PS3 validate PSPID against a hardware database?** After FW 2.80, fabricated PSPIDs may be rejected. If so, must use real PSPID from xRegistry dump.
2. **What does the VAIO client store as device ID?** When VAIO registers, what goes in the `id` field? Can our client reuse it?
3. **Is pkey random or derived?** If purely random and stored on both sides, extracting from either side is sufficient.
4. **Can PS3 HEN homebrew read registry directly?** Rather than FTP+PC parser, a PS3 homebrew could call registry APIs to dump keys to USB.

## Sources
- psdevwiki: Remote Play — https://www.psdevwiki.com/ps3/Remote_Play
- psdevwiki: XRegistry.sys — https://www.psdevwiki.com/ps3/XRegistry.sys
- psdevwiki: Premo plugin — https://www.psdevwiki.com/ps3/Premo_plugin
- PS3HEN — https://consolemods.org/wiki/PS3:PS3HEN
- xRegistry.sys Editor — https://ps3.brewology.com/downloads/download.php?id=10308&mcid=4
- Open-RP source: `keys/keys.cpp`, `psp/orp.c`, `orp.cpp`, `orp-conf.h`
