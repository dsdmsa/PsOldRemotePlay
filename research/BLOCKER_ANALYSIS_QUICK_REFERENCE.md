# IV Context Blocker — Quick Reference Card
**Last Updated:** March 29, 2026 | **Status:** SOLVED APPROACH EXISTS (not yet verified)

---

## The Core Problem

```
Registration fails with error 80029820 (Generic decrypt failure)
Reason: Unknown 8-byte IV context value in AES-CBC encryption
Evidence: 22 different PIN encodings tested, all rejected
```

## What's Happening Inside PS3

```
1. User shows PIN: "12345678"
2. Client sends: [AES-encrypted body] + [16 random key material bytes]
3. PS3 decrypts using:
   - Static XOR key (16 bytes from firmware)
   - Derived AES key = f(material, XOR_key, platform_type)
   - IV base (16 bytes from firmware)
   - UNKNOWN context (8 bytes, probably PIN-derived)
   - Derived AES IV = IV_base XOR context
4. PS3 checks: Content, Client-Type, MAC, Device-ID
5. If all valid → return 200 OK with pkey
6. If decrypt fails → 403 Forbidden (error code 80029820)
```

## What We Need to Find

| Parameter | Type | Size | Status | Example |
|-----------|------|------|--------|---------|
| static XOR key | bytes | 16 | ✓ Known | `F1 16 F0 DA ...` |
| key material | random | 16 | ✓ Known (we send it) | `A3 B7 22 ...` |
| IV base | bytes | 16 | ✓ Known | `29 0D E9 07 ...` |
| **context** | **bytes** | **8** | **✗ UNKNOWN** | **?????** |

**The context is the 8 mystery bytes.**

## Theories Ranked by Likelihood

| # | Theory | How to Test | Time | Confidence |
|---|--------|------------|------|-----------|
| **1** | PIN as 4-byte BE + 4 zeros (PS4 style) | Quick code change | 5 min | 15% |
| **2** | Constant value (all-zeros, 0xFF, skey part) | Automated test | 1 hr | 25% |
| **3** | PIN encoding we haven't tried yet | Modify generator | 1-2 hrs | 35% |
| **4** | In HEN PS3 memory during registration | Memory dump | 30 min | 70% (if HEN) |
| **5** | Revealed by VAIO client binary (Ghidra) | Unpack + analyze | 6 hrs | 70% |
| **6** | Brute-force all PIN encodings | Automated search | 1-11 days | 95% |

## 5-Minute Test (DO THIS FIRST)

**Try PS4 Remote Play formula:**

```python
def pin_to_context_ps4_style(pin_str):
    pin_int = int(pin_str)
    # PS4 uses: 4-byte big-endian PIN + 4 zeros
    return struct.pack('!L', pin_int) + b'\x00' * 4

# For PIN "12345678":
# Expected context: 00 BC 61 4E 00 00 00 00
```

In `JvmPremoRegistration.kt`:
```kotlin
private fun pinToContextBytes(pin: String): ByteArray {
    val pinInt = pin.toLongOrNull()?.toInt() ?: 0
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

Run registration test. **If you get 200 OK → SOLVED.**

## Constant Context Test (1 Hour)

**If PS4 formula fails, test these constants:**

```python
candidates = {
    "all_zeros": b"\x00" * 8,
    "all_ff": b"\xFF" * 8,
    "skey1_first8": bytes.fromhex("1FD5B9FA71B89681"),
    "skey1_last8": bytes.fromhex("B287 92E26F38C36F"),
    "port_9293": struct.pack(">H", 9293) + b"\x00" * 6,
    "const_0x01": b"\x01" + b"\x00" * 7,
    "const_0x01_r": b"\x00" * 7 + b"\x01",
}
```

Create `test_constants.py`:
```python
from iv_context_generator import *

candidates = {  # listed above }

for name, context in candidates.items():
    print(f"Testing {name}: {context.hex()}")
    if test_registration(context, platform_type=1):
        print(f"  ✓ FOUND IT: {name}")
        return
    print(f"  ✗ Rejected (403)")
```

## HEN PS3 Memory Dump (30 minutes, if available)

**If HEN PS3 running at 192.168.1.80:**

```bash
# While PS3 is in registration mode (showing PIN):

# Option A: HTTP API (confirmed working)
curl "http://192.168.1.80/getmem.ps3mapi?proc=vsh&addr=01D157A0&len=128"

# Option B: Manual calculation
# sysconf_plugin base: 0x01B60000
# Dump from: 0x01D157A0 (ctx) or 0x01D15818 (ctx+0x38)
```

Expected output: 128 bytes of hex. Extract 8 bytes and match against PIN encodings in `iv_context_generator.py`.

If matches → **VERIFIED.**

## Code Modification Checklist

Once context is found:

- [ ] Update `JvmPremoRegistration.pinToContextBytes()` with correct formula
- [ ] Add comment with: formula, PIN example, context example
- [ ] Add comment with: how we discovered it (memory dump / VAIO / guess / etc)
- [ ] Run full test: should get 200 OK instead of 403
- [ ] Extract pkey from response
- [ ] Test pkey with session auth (should work)
- [ ] Test on 2nd PS3 (same firmware)
- [ ] Update STATUS_AND_NEXT_STEPS.md: "IV context SOLVED"
- [ ] Delete this file (problem is solved)

## Files to Reference

| File | Use |
|------|-----|
| `JvmPremoRegistration.kt` | Modify this when context found |
| `iv_context_generator.py` | Source of tested encodings |
| `20_DECOMPILED_REGISTRATION_HANDLER.md` | Firmware source of truth |
| `ps3_register_bruteforce_iv.py` | Brute-force fallback |
| `ps3mapi_ctx_dump.py` | Memory dump helper |

## Success Criteria

✓ Client sends registration request
✓ PS3 responds with "HTTP/1.1 200 OK"
✓ Response body (encrypted) contains "PREMO-Key: ..."
✓ Decrypted response yields 16-byte pkey
✓ pkey works in session authentication
✓ Same context works on different PS3 (optional but good)

## Failure Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| 403 Forbidden (80029820) | Wrong IV context | Try next encoding in Tier 1-2 |
| 200 OK but can't decrypt response | Right context, wrong key/IV order | Verify Phone vs PSP vs PC formulas |
| Response is empty | Body too small or truncated | Ensure Content-Length matches |
| Session auth fails after registration | pkey extraction wrong | Check hex parsing |

## Estimated Outcome

- **Best case:** PS4 formula works → 5 minutes
- **Most likely:** Constant context test succeeds → 1-6 hours
- **With HEN:** Memory dump confirms it → 30 minutes
- **Worst case:** Brute-force required → 1-11 days (but guaranteed success)

**Expected time to solution: < 24 hours** with 95% confidence.

---

Keep this card handy. Once you find the context, document it here and mark problem as SOLVED.

