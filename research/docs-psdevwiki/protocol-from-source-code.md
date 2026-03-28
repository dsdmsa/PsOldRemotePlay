# PS3 PREMO Protocol — Extracted from Open-RP Source Code

## Protocol Basics
- Protocol name: **PREMO**
- HTTP/1.1 based
- Port: **TCP/UDP 9293**
- User-Agent: `premo/1.0.0 libhttp/1.0.0`
- Protocol version: 0.3

## Endpoints
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/sce/premo/session` | Session creation/handshake |
| GET | `/sce/premo/session/ctrl` | Control channel (bitrate, termination) |
| GET | `/sce/premo/session/video` | Long-lived video stream |
| GET | `/sce/premo/session/audio` | Long-lived audio stream |
| POST | `/sce/premo/session/pad` | Controller input |

## Discovery
1. Client broadcasts UDP `SRCH` (4 bytes) to port 9293
2. PS3 responds with 156-byte `RESP` packet:
   - Bytes 0-3: ID (`RESP`)
   - Bytes 4-9: Unknown
   - Bytes 10-15: PS3 MAC address (6 bytes)
   - Bytes 16-143: PS3 nickname (128 bytes)
   - Bytes 144-155: NPX identifier (12 bytes)

## Video/Audio Specifications
- Frame resolution: **480x272** (PSP native)
- Overlay height: 32 pixels additional
- Clock frequency: 90,000 Hz
- Max packet queue: 2
- Audio buffer length: 1024 samples
- Max pad states per POST: 60
- Pad state size: 128 bytes
- Session ID length: 16 bytes

## Bitrate Options
- 256 kbps
- 384 kbps
- 512 kbps
- 768 kbps
- 1024 kbps

## Execution Modes
The `PREMO-Exec-Mode` header supports:
- **VSH** — Full PS3 XMB UI streaming
- **game** — In-game streaming
- **PS1** — PS1 classic game streaming

## Controller Input (PSP Pad Mapping)
| Button | Offset |
|--------|--------|
| Home | 0x0101 |
| Select | 0x0501 |
| L3 | 0x0502 |
| R3 | 0x0504 |
| Start | 0x0508 |
| D-Pad Up | 0x0510 |
| D-Pad Right | 0x0520 |
| D-Pad Down | 0x0540 |
| D-Pad Left | 0x0580 |
| R2 | 0x0701 |
| L2 | 0x0702 |
| L1 | 0x0704 |
| R1 | 0x0708 |
| Triangle | 0x0710 |
| Circle | 0x0720 |
| Cross | 0x0740 |
| Square | 0x0780 |

## Analog Stick Offsets
| Axis | Offset |
|------|--------|
| Right X | 0x08 |
| Right Y | 0x0A |
| Left X | 0x0C |
| Left Y | 0x0E |

## Key Events
- Key Up: `0x10000000`
- Key Down: `0x20000000`
- Timestamp offset: `0x40`
- Event ID offset: `0x48`

## Encryption Keys (all 16 bytes / AES-128-CBC)
- `skey0`, `skey1`, `skey2` — Session keys from registration
- `pkey` — Private key from PSP registration
- `psp_id` — PSP OpenPSID (hardware-bound)
- `nonce` — Per-session server nonce

### Key Derivation
1. `xor_pkey = pkey XOR skey0`
2. `xor_nonce = nonce XOR skey2`
3. For bitrate change: XOR first 4 bytes of `xor_pkey` with `{'c','h','a','n'+17}`
4. For session term: XOR first 4 bytes of `xor_pkey` with `{'s','e','s','s'+17}`
5. Plaintext = 16 bytes (PSP MAC in first 6, rest zeros)
6. AES-128-CBC encrypt with key=`xor_pkey`, IV=`xor_nonce`
7. Base64-encode result → `PREMO-Auth` header value

## Stream Packet Header (32 bytes)
```c
struct orpStreamPacketHeader_t {
    Uint8 magic[2];   // Stream type identifier
    Uint16 frame;     // Frame counter
    Uint32 clock;     // Timestamp
    Uint8 root[4];    // Routing info
    Uint16 unk2;
    Uint16 unk3;
    Uint16 len;       // Payload length (big-endian)
    Uint16 unk4;
    Uint16 unk5;
    Uint16 unk6;      // Used for encryption flags
    Uint16 unk7;
    Uint16 unk8;      // Used for audio encryption flag
    Uint16 unk9;
    Uint16 unk10;
};
```

## Stream Type Magic Values
| magic[1] | Type |
|----------|------|
| 0xFF | H.264 video keyframe (decrypt) |
| 0xFE | H.264 video keyframe (decrypt) |
| 0xFB | MPEG4 video keyframe (decrypt when unk6 == 0x0001 or 0x0401) |
| 0x80 | AAC audio (decrypt when unk8 is set) |
| 0xFC | ATRAC3 audio (decrypt when unk8 is set) |

## PREMO HTTP Headers
- PREMO-Application-Reason
- PREMO-Audio-Bitrate / PREMO-Audio-Bitrate-Ability
- PREMO-Audio-Channels
- PREMO-Audio-ClockFrequency
- PREMO-Audio-Codec / PREMO-Audio-Config
- PREMO-Audio-SamplingRate
- PREMO-Auth
- PREMO-Ctrl-Bitrate / PREMO-Ctrl-MaxBitrate / PREMO-Ctrl-Mode
- PREMO-Exec-Mode (VSH / game / PS1)
- PREMO-Mode (PREMO / REMOCON)
- PREMO-Nonce
- PREMO-Pad-Assign / PREMO-Pad-Complete / PREMO-Pad-Index / PREMO-Pad-Info
- PREMO-Platform-Info (e.g. "PSP")
- PREMO-Power-Control
- PREMO-PS3-Nickname
- PREMO-PSPID
- PREMO-SIGNIN-ID (for internet mode)
- PREMO-Trans / PREMO-Trans-Mode
- PREMO-UserName
- PREMO-Version
- PREMO-Video-Bitrate / PREMO-Video-Bitrate-Ability
- PREMO-Video-ClockFrequency
- PREMO-Video-Codec / PREMO-Video-Config
- PREMO-Video-Framerate / PREMO-Video-Framerate-Ability
- PREMO-Video-Resolution / PREMO-Video-Resolution-Ability
- SessionID

## Config File Format (.orp)
- Magic: `ORP` (3 bytes)
- Version: 2
- 32-bit flags
- 8 reserved bytes
- 3 × 16-byte session keys (skey0, skey1, skey2)
- Records: PS3 hostname, nickname, MAC, PSP MAC, PSP ID, owner name, private key, PSN login
