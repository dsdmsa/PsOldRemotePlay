# Registration Status — Blocked on IV Context Value

## What works:
- WiFi connection to PS3's ad-hoc AP (via Linux wpa_supplicant WPA-NONE)
- WiFi password = PIN with halves swapped (last4 + first4)
- TCP port 9293 reachable on PS3 via WiFi
- PS3 processes our POST /sce/premo/regist requests
- PS3 responds with 403 (decryption/validation fails)

## What's wrong:
The AES-CBC IV has an unknown 8-byte XOR value: `**(param_1 + 0x38)`
This is a double-pointer dereference to PS3 internal state.
Without the correct IV, the PS3 can't decrypt our registration body.

## Tried IV contexts (all failed):
- All zeros
- PIN as ASCII bytes
- PIN as big-endian integer
- PIN as little-endian integer
- PIN first 4 digits padded
- PIN halves swapped
- SSID suffix padded

## Also tried:
- Client-Type: "Phone" and "PC" (testing shifted mapping theory)
- Multiple PINs/sessions

## Next steps:
1. **HEN installation** (guaranteed, no WiFi needed) — install via PS3 browser, FTP keys
2. **Deeper Ghidra analysis** of what sets param_1+0x38 in sysconf_plugin
3. **Capture real traffic** from a working VAIO/PSP client if one becomes available
