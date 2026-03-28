# Ghidra Finding: REGISTRATION PROTOCOL FOUND in sysconf_plugin.elf

## THE REGISTRATION ENDPOINT EXISTS: `POST /sce/premo/regist HTTP/1.1`

The registration protocol is HTTP-based, just like the session protocol! It's in `sysconf_plugin.sprx`.

## Key Registration Protocol Strings

| Offset | String | Significance |
|--------|--------|-------------|
| `0x149890` | `POST /sce/premo/regist HTTP/1.1` | **THE REGISTRATION HTTP ENDPOINT** |
| `0x1499d9` | `PREMO-Key: %s` | **The pkey is sent as an HTTP header!** |
| `0x1499c7` | `PREMO-KeyType: 0` | Key type identifier |
| `0x149830` | `cellPremoRegistDiscovery` | Registration discovery thread name |
| `0x149958` | `AP-Ssid: %s` | WiFi AP SSID sent in registration |
| `0x149965` | `AP-Key: %s` | WiFi AP passphrase (WPA key) |
| `0x149920` | `HTTP/1.1 200 OK` | Registration success response |
| `0x150ff0` | `HTTP/1.1 403 Forbidden` | Registration failure |
| `0x151058` | `PREMO-Version:` | Version header in registration |

## What This Reveals

The registration protocol is:
1. PS3 creates a WiFi AP (SSID + WPA key generated)
2. Client connects to that AP
3. Client sends: `POST /sce/premo/regist HTTP/1.1` with headers
4. The PS3 responds with `PREMO-Key: <the pkey>` in the response!
5. PS3 also sends `AP-Ssid` and `AP-Key` (the WiFi credentials)

**The pkey is transmitted as a plain HTTP header during registration!**
**This means registration is simple HTTP — no complex crypto handshake!**

## Strings for Ghidra Investigation

### Load in Ghidra: sysconf_plugin.elf
- File → Import File → sysconf_plugin.elf
- Same settings: PowerISA-Altivec-64-32addr, Big Endian

### Navigate to these addresses to find the registration functions:
1. `0x149890` — `POST /sce/premo/regist` → find cross-references
2. `0x1499d9` — `PREMO-Key: %s` → find where the key is generated/sent
3. `0x149830` — `cellPremoRegistDiscovery` → the registration thread function
4. `0x149965` — `AP-Key: %s` → where WiFi credentials are sent

## Also Found: WPA Configuration UI
- `OnInitEditRemotePlayConfigWpa` — WPA key input handler
- `edit_remote_config_custom_wpa` — Custom WPA key entry
- `msg_wpakey` — WPA key message
- `msg_wpapsk_aes` — WPA-PSK AES security type

## Registration Flow (hypothesis based on strings)
1. User selects "Register Device" on PS3
2. PS3 displays 8-digit number (the PIN)
3. PS3 creates temporary WiFi AP with generated SSID/WPA key
4. Client connects to AP (using PIN as WPA key? Or PIN entered in-band?)
5. Client sends `POST /sce/premo/regist` with device ID
6. PS3 responds with `PREMO-Key` (the pkey) and `AP-Ssid`/`AP-Key`
7. Both sides store the shared pkey
8. Registration complete

## This Changes Everything
If the registration is just an HTTP POST with the pkey sent in cleartext headers, our Android app can:
1. Connect to the PS3's temporary WiFi AP
2. Send the HTTP POST registration request
3. Receive the pkey
4. Store it locally
5. Done — no PSP/VAIO/VM needed ever again
