# Final Research Status — What We Have, What's Missing, What to Do

## WHAT WE FULLY KNOW (enough for POC)

### Session Protocol (premo_plugin.elf) — 100% reverse-engineered
- Complete HTTP request/response format
- All PREMO-* headers and their values
- PSPID validation logic
- Nonce generation (4 platform-specific XOR derivations)
- All 10 static crypto keys (skey0/1/2 + 4 nonce XOR keys + 3 auth keys)
- Video/audio stream packet format (32-byte header)
- AES-128-CBC decryption (key=pkey XOR skey0, iv=nonce XOR skey2)

### Registration Protocol (sysconf_plugin.elf) — 90% reverse-engineered
- Endpoint: `POST /sce/premo/regist HTTP/1.1`
- Request body: AES-encrypted, contains Client-Type/Id/Mac/Nickname
- Response body: AES-encrypted, contains PREMO-Key (the pkey!), PS3-Mac, PS3-Nickname, AP credentials
- All 6 registration static keys (3 XOR keys + 3 IV bases)
- Platform-specific key derivation formulas
- For Phone type: CLIENT chooses the AES key (last 16 bytes of body)

### ONE REMAINING UNKNOWN
The 8-byte context value at `*(param_1 + 0x38)` that's XOR'd into the IV.

**But for PHONE type (our target), this may not matter because:**
1. The AES key comes from bytes WE generate (last 16 bytes of body)
2. The IV XOR affects only the second 8 bytes of the IV (Phone-specific behavior)
3. We can try with zeros first — if the context is set from a global that starts at 0, it works
4. If it doesn't work, we can brute-force 8 bytes (or capture one registration with Wireshark to determine it)

## RECOMMENDED POC APPROACH

### Phase 1: Build the streaming client (we have everything needed)
1. Hardcode known test keys (from VAIO client or any other source)
2. Implement: UDP discovery → session handshake → video decrypt → MediaCodec display
3. This validates the entire streaming pipeline

### Phase 2: Add self-registration (try with context=0)
1. Connect to PS3's WiFi AP during registration mode
2. Send `POST /sce/premo/regist` with Phone-type encrypted body
3. If context=0 works → done
4. If not → capture traffic to determine the context value

### Phase 3: Handle the context value (if needed)
Options:
- Wireshark capture during one real registration
- Try common values: PS3 MAC padded to 8 bytes, all zeros, all ones
- The value may be sent during WiFi AP setup or derived from the AP SSID number
