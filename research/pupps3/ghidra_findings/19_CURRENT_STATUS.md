# Current Status — End of Session

## What Works
- Desktop app builds and runs (KMP + Compose Desktop)
- UDP/TCP discovery works — PS3 responds to session requests
- Protocol is confirmed — PS3 returns proper 403 with PREMO headers
- WiFi connection to PS3's ad-hoc AP works from Linux (password = PIN halves swapped)
- Registration endpoint accepts our POST requests over WiFi
- HEN PS3 (192.168.1.80) has webMAN + PS3MAPI with full memory access
- webMAN's /play.ps3 briefly opens port 9293 on HEN PS3

## What's Blocked
- Registration encryption formula is WRONG — all attempts return 80029820
- The Ghidra decompiler simplified the crypto formula (premo_plugin export shows it's more complex)
- xRegistry.sys format is not fully understood for direct writes

## HEN PS3 Info
- IP: 192.168.1.80
- MAC: FC:0F:E6:0F:A7:3B
- PSID: 2B2EA6F22A3E8026B4B1D0DBE4914C16
- IDPS: 000000010084000B14107977566B3E2B
- FW: 4.92 CEX HEN 3.4.0, webMAN 1.47.47g
- PS3MAPI: can peek/poke VSH process memory

## Next Steps (Priority Order)
1. Use PS3MAPI memory peek to read registration context value while PS3 is in registration mode
2. Or: use PS3MAPI to call savePremoPSPInformation directly with our data
3. Or: write a small PS3 homebrew SPRX that registers a device and dumps all keys
4. Once registered on HEN PS3: test full streaming pipeline
5. Apply findings to stock PS3 registration
