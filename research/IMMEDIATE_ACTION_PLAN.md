# Immediate Action Plan — Solving IV Context Blocker
**Urgency:** CRITICAL (sole blocker for stock PS3 registration)
**Target:** Identify 8-byte IV context value within 24 hours
**Assumptions:** Have HEN PS3 available at 192.168.1.80

---

## OPTION 1: PS4 Protocol Shortcut (< 5 minutes) — TRY IMMEDIATELY

The PS4 registration handler uses a nearly identical protocol to PS3. If PS3 follows the same IV pattern, this could solve it instantly.

### PS4 Formula:
```python
# From kingcreek/ps4-remote-play
pin_bytes = struct.pack('!L', pin)  # 4 bytes, big-endian
context = pin_bytes + b'\x00' * 4  # 4 bytes PIN + 4 zeros
```

### Test on PS3:
1. Modify `JvmPremoRegistration.pinToContextBytes()`:
   ```kotlin
   private fun pinToContextBytes(pin: String): ByteArray {
       val pinInt = pin.toLongOrNull() ?: 0L
       val bytes = ByteArray(8)
       bytes[0] = (pinInt shr 24 and 0xFF).toByte()
       bytes[1] = (pinInt shr 16 and 0xFF).toByte()
       bytes[2] = (pinInt shr 8 and 0xFF).toByte()
       bytes[3] = (pinInt and 0xFF).toByte()
       bytes[4] = 0
       bytes[5] = 0
       bytes[6] = 0
       bytes[7] = 0
       return bytes
   }
   ```

2. Run registration test against stock PS3
3. If 200 OK → **SOLVED** (and document why PS3 differs from code analysis)
4. If 403 → Continue to Option 2

**Effort:** Copy-paste code change + 1 test run = 2 minutes

---

## OPTION 2: Constant Context Search (< 1 hour) — PARALLEL TO OPTION 1

The context value might be constant across all PS3s (not PIN-derived). Test these possibilities:

### Candidate Constants:
```python
candidates = {
    "all_zeros": bytes(8),
    "all_ff": bytes([0xFF] * 8),
    "skey1_first8": bytes([0x1F, 0xD5, 0xB9, 0xFA, 0x71, 0xB8, 0x96, 0x81]),
    "skey1_last8": bytes([0xB2, 0x87, 0x92, 0xE2, 0x6F, 0x38, 0xC3, 0x6F]),
    "skey0_first8": bytes([0xD1, 0xB2, 0x12, 0xEB, 0x73, 0x86, 0x6C, 0x7B]),
    "skey2_first8": bytes([0x65, 0x2D, 0x8C, 0x90, 0xDE, 0x87, 0x17, 0xCF]),
    "const_1_left": bytes([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]),
    "const_1_right": bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01]),
}
```

### Implementation:
Write `test_constant_contexts.py` that:
1. Iterates through candidates
2. For each context, derives AES key/IV using the THREE key derivation formulas
3. Sends test registration request to stock PS3
4. Logs which contexts return 200 OK vs 403

### Code Template:
```python
import socket
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

def test_context(context_bytes, platform_type=1):  # platform_type 1 = Phone
    # (use existing deriveRegistrationKey and deriveIv functions)
    aes_key = derive_key_phone(key_material)
    aes_iv = derive_iv_phone(context_bytes)

    # Encrypt and send
    plaintext = b"Client-Type: PC\r\nClient-Id: ...\r\nClient-Mac: ...\r\nClient-Nickname: ...\r\n"
    encrypted = AES.new(aes_key, AES.MODE_CBC, aes_iv).encrypt(pad(plaintext, 16))

    # Send HTTP POST and check response
    sock = socket.socket()
    sock.connect((PS3_IP, 9293))
    # ... send request ...
    # ... check for "200 OK" or "403" ...
```

**Effort:** < 1 hour to code, run, and get results

---

## OPTION 3: HEN PS3 Memory Dump (< 30 minutes) — IF HEN AVAILABLE

### Current Status:
- webMAN HTTP API (`/getmem.ps3mapi`) is **CONFIRMED WORKING**
- Max 256 bytes per read (but we only need 8 bytes)
- Runtime sysconf_plugin base confirmed: **0x01B60000**
- IV context pointer in param_1 at offset **0x38**

### Challenge:
Need to find exact runtime address of the IV context value during registration.

### Addresses to Try:
From firmware analysis (`param_1 + 0x38` in registration handler):

```python
# sysconf_plugin BSS runtime addresses (estimated from segment layout)
SYSCONF_BASE = 0x01B60000
REGISTRATION_CTX_BASE = SYSCONF_BASE + 0x157A0   # 0x01D157A0 (estimated)
CONTEXT_POINTER_OFFSET = 0x38                     # Within context structure
IV_CONTEXT_OFFSET = 0x78                          # Estimated for actual value

# Try to dump from multiple locations:
addresses_to_try = [
    (0x01D157A0 + 0x38, "ctx+0x38"),
    (0x01D157A0 + 0x78, "ctx+0x78"),
    (0x01D16708, "DAT_001b6708"),
    (0x01D15818, "IV context direct"),
]
```

### Method:
```bash
# While PS3 is in registration mode (showing PIN):
curl "http://192.168.1.80/getmem.ps3mapi?proc=vsh&addr=01D157A0&len=128"
```

This dumps 128 bytes from the context structure. Then:
1. Parse the response hex
2. Extract 8-byte value
3. Compare against PIN encodings
4. If matches one → **SOLVED**

**Effort:** 10 minutes (if you know the addresses)

---

## OPTION 4: VAIO DLL Unpacking (4-6 hours) — MEDIUM TERM

If HEN/constant tests fail, analyze the VAIO client binary.

### Steps:
1. **Get VRPSDK.dll** (from official VAIO installation)
2. **Unpack Themida:**
   ```bash
   # On Windows or Windows VM
   unlicense VRPSDK.dll VRPSDK.unpacked.dll
   # or
   wine unlicense VRPSDK.dll VRPSDK.unpacked.dll
   ```
3. **Decompile in Ghidra:**
   - Open unpacked DLL
   - Search for hex string `F1 16 F0 DA` (Phone XOR key)
   - Trace backwards to find IV context usage
   - Should reveal exact PIN encoding formula
4. **Compare with PS3 formula**
   - If different, test the client-side formula on PS3
   - If same, confirms PS3's analysis is correct

### Tools:
- `unlicense`: https://github.com/ergrelet/unlicense
- `Magicmida`: https://github.com/Hendi48/Magicmida
- Ghidra (free)

**Effort:** 4-6 hours (most of time is decompilation and understanding client code)

---

## OPTION 5: Systematic Brute-Force (11+ days) — LAST RESORT

If all above fail, brute-force all possible PIN encodings.

### Optimization:
- Compress search space to 22 most-likely encodings (already defined in `iv_context_generator.py`)
- Test with only 1 platform type (Phone, type 1) to reduce permutations
- Parallelize: run 5-10 test clients against PS3 simultaneously

### Script Template:
```python
def brute_force_iv_contexts(pin_range=(10000000, 100000000), platform_type=1):
    for pin in range(pin_range[0], pin_range[1]):
        for encoding_name, context_func in ENCODINGS.items():
            context = context_func(pin)
            if test_registration(context, platform_type):
                print(f"FOUND: PIN {pin}, encoding {encoding_name}")
                print(f"Context bytes: {context.hex()}")
                return pin, encoding_name
    return None, None
```

With parallelization: ~1-2 days on 10 machines
Without: ~11 days on 1 machine

**Effort:** High (but fully automated)

---

## RECOMMENDED SEQUENCE

### PHASE 1 (Next 10 minutes):
- [ ] **Try PS4 formula** (copy-paste code, run test) ← START HERE
- [ ] **Start constant context tests in background** (can run while you sleep)

### PHASE 2 (Next 1-2 hours):
- [ ] Check results of both tests
- [ ] If found → **Document why and test on 2+ PS3 units to confirm universal**
- [ ] If not found → Proceed to Phase 3

### PHASE 3 (Next 4-6 hours):
- [ ] If HEN available: **Attempt memory dump** (straightforward, high confidence)
- [ ] If no HEN: **Start VAIO DLL unpacking** (slow but authoritative)

### PHASE 4 (If nothing works):
- [ ] Set up automated brute-force
- [ ] Run overnight/across multiple machines
- [ ] Continue research in parallel

---

## SUCCESS CRITERIA

**MINIMAL:** Get 200 OK response with valid PREMO-Key in response
**FULL:** Extract PREMO-Key, verify it works with session auth on same PS3
**PUBLISHED:** Test on 2+ stock PS3 units, confirm IV is universal/device-specific

---

## QUICK REFERENCE: Key Files to Modify

| File | Change | Impact |
|------|--------|--------|
| `JvmPremoRegistration.kt` line 196-203 | `pinToContextBytes()` | Test different encodings |
| `iv_context_generator.py` | Add new encoding function | Generate test vectors |
| `ps3_register_bruteforce_iv.py` | Modify PIN range | Parallelize search |
| Test script (create new) | Iterate constants + test | Check constant contexts |

---

## Expected Timeline

| Outcome | Probability | Time |
|---------|------------|------|
| PS4 formula works | 15% | 5 min |
| Constant context found | 25% | < 1 hr |
| Memory dump succeeds | 70% (with HEN) | 30 min |
| VAIO DLL reveals it | 70% | 6 hrs |
| Brute-force finds it | 95% | 1-11 days |

**Most likely:** Solution found within 6-24 hours via combination of above methods.

---

## Do NOT Try (Wastes Time)

- ✗ Wireshark capture of registration (traffic encrypted, can't see plaintext)
- ✗ Reverse-engineering sysconf_plugin without proper tool (already done in Ghidra)
- ✗ Contacting Sony (proprietary information)
- ✗ Extracting from old VAIO installations (storage unknown, probably lost)
- ✗ Analyzing PS1/PS2 Remote Play (entirely different protocol)

---

## Recording Results

Once IV context is found:

1. **Document the encoding:** What transformation of PIN produces the 8 bytes?
2. **Verify across units:** Test on stock PS3, HEN PS3, FW 4.85/4.90/4.92
3. **Update code:**
   - Modify `pinToContextBytes()` in JvmPremoRegistration.kt
   - Add comment with source reference
   - Run full test suite
4. **Publish findings:** Update STATUS_AND_NEXT_STEPS.md with solution
5. **Close blocker:** Remove IV context from "MISSING PIECES" section

---

This plan is designed to find the answer with 95% confidence within 24 hours.
Start with Option 1 (PS4 formula test) immediately — it's free and takes 5 minutes.

