# Ghidra Finding: Key Protocol Strings in premo_plugin.elf

## Critical Strings to Investigate in Ghidra

### Session/Auth Handling (THE MOST IMPORTANT)
Navigate to these addresses in Ghidra (press G, enter the hex address), then look at cross-references to find the functions that use them:

| Hex Address | String | Why Important |
|-------------|--------|---------------|
| `0x2c9a8` | `PREMO-Auth` | **Authentication token validation** — find what function checks/generates this |
| `0x2d410` | `PREMO-Nonce:` | **Nonce generation** — PS3 generates and sends this to client |
| `0x2ca38` | `PREMO-PSPID` | **Device ID validation** — does PS3 check PSPID format? |
| `0x2c890` | `SessionID` | Session management |
| `0x2c958` | `session-term` | Session termination |

### HTTP Server (PS3 is the SERVER)
| Hex Address | String | Why Important |
|-------------|--------|---------------|
| `0x2c9e8` | `GET /sce/premo/session HTTP/1.1` | **Session establishment handler** |
| `0x2cc68` | `GET /sce/premo/session/video HTTP/1.1` | Video stream handler |
| `0x2ccb8` | `GET /sce/premo/session/audio HTTP/1.1` | Audio stream handler |
| `0x2cd08` | `POST /sce/premo/session/pad HTTP/1.1` | Controller input handler |
| `0x2cd98` | `GET /sce/premo/session/ctrl HTTP/1.1` | Control channel handler |
| `0x2cf10` | `HTTP/1.1 403 Forbidden` | **Auth failure response** — what triggers a 403? |
| `0x2c7f8` | `HTTP/1.1 200 OK` | Success response |

### Discovery
| Hex Address | String | Why Important |
|-------------|--------|---------------|
| `0x2ce08` | `SRCH` | UDP discovery — PS3 listens for this |
| `0x2ce10` | `RESP` | UDP discovery response |

### Thread Names (reveal architecture)
| String | Purpose |
|--------|---------|
| `cellPremoHttpdThread` | HTTP server thread |
| `cellPremoAcceptThread` | Connection accept thread |
| `cellPremoVSessionThread` | Video session thread |
| `cellPremoASessionThread` | Audio session thread |
| `cellPremoPadSessionThread` | Controller input thread |
| `cellPremoCtrlSessionThread` | Control session thread |
| `cellPremoISessionThread` | Initial/handshake session thread? |
| `cellPremoControlThread` | Master control thread |

### Codec/Encoding
| String | Meaning |
|--------|---------|
| `PREMO-Video-Codec: AVC` | H.264 |
| `PREMO-Video-Codec: AVC/CAVLC` | H.264 CAVLC profile |
| `PREMO-Video-Codec: M4V` | MPEG-4 Part 2 |
| `PREMO-Video-Codec: M4HD` | Higher quality H.264 |
| `PREMO-Audio-Codec: AT3` | ATRAC3 |
| `PREMO-Audio-Codec: M4A` | AAC |
| `PREMO-Video-Resolution-Ability: 320x240,432x240,480x272` | Supported resolutions |
| `PREMO-Video-Framerate-Ability: 7.5,10,15,30` | Supported framerates |

### Notable: No Registration Protocol Strings Found
The `premo_plugin.sprx` does NOT contain strings related to:
- WPA/WiFi passphrase
- PIN code
- Key generation/derivation
- xRegistry paths (except `xsetting`)
- `savePremoPSPInformation` / `loadPremoPSPInformation`

**This means the registration/pairing protocol is handled by OTHER modules** (likely VSH core or a separate registration plugin), and `premo_plugin.sprx` only handles the STREAMING session AFTER registration is complete.

## Priority for Ghidra Investigation

1. **Go to `0x2c9a8` (PREMO-Auth)** → find references → this shows how the PS3 validates the client's auth token
2. **Go to `0x2d410` (PREMO-Nonce)** → find references → this shows how the PS3 generates the nonce
3. **Go to `0x2ca38` (PREMO-PSPID)** → find references → this shows if/how the PS3 validates device IDs
4. **Go to `0x2cf10` (403 Forbidden)** → find references → this shows what causes auth rejection
5. **Go to `0x2c9e8` (GET /sce/premo/session)** → find references → the main session handler
