# Reddit Post — r/ps3homebrew

## Title:
Building a PS3 Remote Play client from scratch — stuck on device registration encryption. Anyone ever reverse-engineered this?

## Body:

**TL;DR:** I'm building an open-source PS3 Remote Play app (like Chiaki but for PS3). I've reverse-engineered 95% of the PREMO protocol from the PS3 firmware. The streaming session protocol is fully cracked. The only thing blocking me is the **device registration encryption** — the AES key derivation formula that `sysconf_plugin.sprx` uses for `POST /sce/premo/regist`. Has anyone worked on this before?

---

### What I've built so far

I'm developing a Kotlin Multiplatform app (Desktop + Android) that connects to a stock, unmodified PS3 for Remote Play. Think of it as Chiaki/chiaki-ng but for PS3 instead of PS4/PS5.

**What works:**
- Desktop POC app with discovery, session handshake, and protocol handling
- PS3 responds to our session requests with proper PREMO headers (confirmed with both stock and HEN PS3)
- Full protocol documentation (all headers, packet formats, response codes)

### What I've reverse-engineered

I decrypted and decompiled `premo_plugin.sprx` and `sysconf_plugin.sprx` from PS3 firmware 4.90 using ps3sce + Ghidra.

**Session protocol (100% complete):**
- UDP discovery (SRCH/RESP)
- HTTP session handshake with all PREMO-* headers
- Nonce generation (corrected from exported C — uses 4 inputs + 2 key tables per platform, not the simplified formula Ghidra's decompiler shows)
- AES-128-CBC stream decryption
- All 10 static crypto keys extracted (skey0/1/2 + platform-specific nonce XOR keys)
- Platform types: PSP, Phone, PC, VITA
- Exec modes: VSH, GAME, PS1, SUBDISPLAY

**Registration protocol (partially complete):**
- Endpoint: `POST /sce/premo/regist HTTP/1.1`
- WiFi AP: PS3 creates ad-hoc network, password = PIN with halves swapped (last4 + first4)
- Body: AES-128-CBC encrypted, contains Client-Type/Id/Mac/Nickname
- Response: AES-encrypted, contains PREMO-Key (the shared pkey), PS3-Mac, PS3-Nickname
- For Phone type: client chooses 16 random bytes as key material (appended unencrypted to body)
- Client-Type mapping is shifted: PS3 menu "Phone" expects Client-Type "PC" in the body
- Static XOR key extracted: `F1 16 F0 DA 44 2C 06 C2 45 B1 5E 48 F9 04 E3 E6` (DAT_00150ec0)
- Static IV base extracted: `29 0D E9 07 E2 3B E2 FC 34 08 CA 4B DE E4 AF 3A` (DAT_00150ed0)
- All 6 registration keys extracted (3 platforms × XOR key + IV base)
- Port 9293 opens on wired interface during registration mode

### Where I'm stuck

The **AES key derivation formula** for registration. Ghidra's decompiler shows:

```
key[i] = (material[i] - i - 0x28) XOR static_key[i]
```

The raw PPC assembly at `0x00100560` in `sysconf_plugin.elf` confirms this:

```asm
lbz   r9, 0x0(r10)      ; load material byte
lbz   r0, 0x0(r11)      ; load static key byte
subf  r9, r7, r9        ; subtract counter
addi  r7, r7, 0x1       ; increment counter
subi  r9, r9, 0x28      ; subtract 0x28
xor   r9, r9, r0        ; XOR with static key
stb   r9, 0x0(r10)      ; store result
```

I've implemented this exactly, but every registration attempt returns **403 Forbidden (error 80029820)**. I've tried:
- Multiple IV context values (zeros, PIN as ASCII/int, PS3 MAC, sockaddr, PSID)
- Both Phone and PSP encryption types
- Client-Type "Phone" and "PC"
- Multiple PS3s (stock + HEN)
- ~30+ different combinations total

The PS3 consistently rejects with the same error code, suggesting the decryption fails and the PS3 can't parse the body.

**The interesting thing:** The `premo_plugin.elf.c` export (not the decompiler window) revealed that the nonce generation formula is MUCH more complex than what Ghidra's decompiler showed:

```c
// Real nonce formula (from exported C, line 10345):
*pbVar4 = ((cVar1 + ')' + *pcVar2 ^ *pbVar7 ^ *pbVar3) - cVar1) + 0xb7 ^ *pbVar6;
```

This uses 4 inputs and 2 static key tables. The registration encryption might have similar hidden complexity that the decompiler simplified.

### What I need

Any ONE of these would unblock the project:

1. **Has anyone reverse-engineered the registration encryption before?** Even partial findings would help.
2. **Does anyone have a Wireshark capture of a real PSP/VAIO/Xperia registering with a PS3?** Seeing the actual encrypted payload alongside known inputs could help derive the formula.
3. **Can anyone familiar with PPC64 assembly verify the key derivation at 0x00100560-0x0010058C?** The assembly LOOKS simple but the context (IV XOR value) might be wrong, or there might be preprocessing I'm missing.
4. **Does anyone know the exact xRegistry.sys binary format for the `/setting/premo/psp01/` entries?** This would let me inject registration data directly on a HEN PS3 and bypass the encryption.
5. **Has anyone used Rebug Toolbox's xRegistry editor to write Remote Play registration data?** This is the backup plan.

### Available resources

Everything is in my GitHub repo (will share link). Key files:
- Decrypted `sysconf_plugin.elf` and `premo_plugin.elf` (from FW 4.90)
- Exported C decompilation (90K lines for sysconf, 18K for premo)
- 19 Ghidra analysis documents
- Complete protocol documentation
- Python registration scripts with all key combinations
- Working desktop app (Kotlin/Compose)

### Why this matters

There's currently **no working PS3 Remote Play client** for any modern device. The original Open-RP project (2009) only implemented the streaming session and required a hacked PSP for key extraction. Chiaki/chiaki-ng explicitly doesn't support PS3 (different protocol). A working open-source client would let people stream their PS3 over the network — including the full XMB UI, not just games.

If anyone has insights, pointers, or wants to collaborate, I'd love to hear from you. Even pointing me to the right person or Discord server would be valuable.

---

**Edit:** I also have a HEN PS3 with webMAN + PS3MAPI (full memory peek/poke access to VSH process). If anyone knows how to use PS3MAPI to call `savePremoPSPInformation()` or write to xRegistry from memory, that would work too.
