# PS4 Formula Test — IV Context Solver

**Status:** Two parallel tests ready to run
**Goal:** Find the 8-byte IV context value for PS3 registration
**Expected Result:** One method should find it within 1 hour

---

## Test 1: PS4 Formula (5 minutes) — QUICK WIN

**Hypothesis:** PS3 uses same IV encoding as PS4 (4-byte BE PIN + 4 zeros)

### Code Change
✅ Already implemented in `JvmPremoRegistration.kt` line 196-203:
```kotlin
private fun pinToContextBytes(pin: String): ByteArray {
    val pinInt = pin.toLongOrNull() ?: 0L
    val bytes = ByteArray(8)
    // PS4 formula: PIN as 4-byte BE uint + 4 zeros
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

### How to Test
1. Wait for build to complete
2. Run desktop app:
   ```bash
   ./gradlew :composeApp:run
   ```
3. In the UI:
   - Enter PS3 IP (192.168.1.75)
   - Put PS3 in registration mode (show PIN on TV screen)
   - Enter PIN (8 digits)
   - Click "Register"
4. Check logs for result:
   - ✅ `200 OK` = **IV CONTEXT SOLVED!**
   - ❌ `403 Forbidden` = Wrong encoding, proceed to Test 2

### Expected Log Output (Success)
```
[REGIST] === STARTING REGISTRATION ===
[REGIST] PS3: 192.168.1.75, PIN: 12345678, Platform: Phone
[REGIST] Derived AES key: ...
[REGIST] Derived AES IV: ...
[REGIST] IV context (from PIN): 00BC614E00000000   (4-byte BE PIN)
[REGIST] Plaintext body: ...
[REGIST] Encrypted body: ...
[REGIST] Request sent, waiting for response...
[REGIST] Raw response: HTTP/1.1 200 OK ...
[REGIST] === GOT 200 OK! Attempting to decrypt response ===
[REGIST] Decrypted response: PREMO-Key: ... PS3-Mac: ...
[REGIST] === REGISTRATION SUCCESSFUL ===
[REGIST] PKey: ...
```

### Expected Log Output (Failure)
```
[REGIST] === STARTING REGISTRATION ===
...
[REGIST] Raw response: HTTP/1.1 403 Forbidden ...
[REGIST] Got 403 Forbidden — registration rejected
[REGIST] This could mean: wrong encryption, wrong PIN, or wrong platform type
```

---

## Test 2: Constant Context Search (Parallel/Overnight)

**Hypothesis:** IV context is a constant value (not PIN-derived)

### Ready to Run
✅ Script created: `research/tools/test_constant_contexts.py`

### What It Does
Tests 15 candidate constants in the background:
- All zeros, all 0xFF
- Derived from static keys (SKEY0/1/2)
- Other combinations

### How to Run (While Test 1 Runs)
```bash
# In a separate terminal
python3 research/tools/test_constant_contexts.py 192.168.1.75 12345678
```

### Expected Output (Success)
```
✅ Testing: skey1_first8
   Context: 1fd5b9fa71b89681
   Result: HTTP 200
   ✅ SUCCESS! This context works!
```

### Expected Output (Failure)
```
✅ OK skey1_first8
✅ OK skey2_first8
❌ FAIL const_0x01_left
❌ FAIL zeros_8
...

SUMMARY:
❌ No working constants found.
   This suggests IV context is PIN-derived (not constant).
```

---

## Next Steps Based on Results

| Result | Timeline | Action |
|--------|----------|--------|
| Test 1 = 200 OK | 5 min | **SOLVED!** Update code, test session, ship |
| Test 1 = 403, Test 2 = found constant | < 1h | Found alternative encoding, implement |
| Both fail | < 6h | Priority 3: VAIO DLL analysis (definitive) |

---

## Important Notes

### WiFi AP Connection
If test fails with "connection refused":
1. Check PS3 is in registration mode (PIN shown on screen)
2. Verify WiFi network exists: `networksetup -listallnetworkservices`
3. Ensure you're connected to `PS3REGIST_XXXX` AP (passphrase in firmware: `xeRFa3VYDHSfNjE_nA{z>pk2xANqicHqQFvij}0WHz[7kzO2Yynp4o4j}U2`)

### Device ID & MAC
The app pre-fills these with placeholders. You can use any values for registration test — they're not tied to specific hardware.

### PIN Format
- 8 digits only
- Shown on TV screen during registration mode
- Different each time PS3 enters registration mode

---

## Files Modified

1. ✅ `JvmPremoRegistration.kt` — PS4 formula implemented
2. ✅ `test_constant_contexts.py` — Constant search script
3. ✅ Compilation errors fixed (OnScreenController, KeyboardController, PremoSession interface)

---

## Success Criteria

**MINIMAL:** HTTP 200 OK response from PS3
**FULL:** Extract PREMO-Key from response and verify it works with session auth
**PUBLISHED:** Test on 2+ PS3 units to confirm IV is universal or device-specific

---

Good luck! 🚀
