# PS3 Remote Play: Streaming Implementation & xRegistry Bypass Guide

Comprehensive reference for implementing video/audio streaming and controller input
beyond the session handshake, plus the xRegistry bypass path for registration.

Source: open-rp (dashhacker), xregistry-parser, Ghidra analysis, existing project code.

---

## Table of Contents

1. [Session Flow Overview](#1-session-flow-overview)
2. [Key Derivation (Already Implemented)](#2-key-derivation)
3. [Video Stream Protocol](#3-video-stream-protocol)
4. [Audio Stream Protocol](#4-audio-stream-protocol)
5. [Stream Decryption](#5-stream-decryption)
6. [Controller Input (Pad) Protocol](#6-controller-input-pad-protocol)
7. [Control Channel](#7-control-channel)
8. [Video Decoding Options for KMP](#8-video-decoding-options-for-kmp)
9. [Audio Decoding Options for KMP](#9-audio-decoding-options-for-kmp)
10. [xRegistry Bypass Path](#10-xregistry-bypass-path)
11. [Implementation Checklist](#11-implementation-checklist)

---

## 1. Session Flow Overview

After the session handshake at `GET /sce/premo/session` returns 200 OK with headers,
the client must:

1. **Derive keys** from nonce + pkey (already done)
2. **Set initial bitrate** via `GET /sce/premo/session/ctrl` with `Mode: change-bitrate`
3. **Open video stream**: `GET /sce/premo/session/video` (HTTP 1.1, chunked transfer)
4. **Open audio stream**: `GET /sce/premo/session/audio` (HTTP 1.1, chunked transfer)
   (250ms delay between video and audio connection in open-rp)
5. **Open pad connection**: `POST /sce/premo/session/pad` (raw TCP, 128-byte packets)
6. **Run event loop**: Decode video/audio, send controller input, handle bitrate changes

All four connections (ctrl, video, audio, pad) run concurrently to port 9293.

---

## 2. Key Derivation

**Already implemented in the project.** Summary for reference:

```
xor_pkey  = pkey XOR SKEY0    (16 bytes, byte-by-byte)
xor_nonce = nonce XOR SKEY2   (16 bytes, byte-by-byte)
```

For platform-specific nonce XOR keys, use the appropriate constant from `PremoConstants.kt`
(SKEY2 for PSP, NONCE_XOR_PHONE for Phone, etc.).

**Auth token generation:**
```
plaintext = device_mac (6 bytes) + 10 zero bytes = 16 bytes
ciphertext = AES-128-CBC-encrypt(plaintext, key=xor_pkey, iv=xor_nonce)
auth_token = base64(ciphertext)
```

**Auth variants** (for control channel, public/internet mode only):
- `change-bitrate`: XOR first 4 bytes of xor_pkey with {0x63, 0x68, 0x61, 0x7F}
- `session-term`: XOR first 4 bytes of xor_pkey with {0x73, 0x65, 0x73, 0x84}
- Then generate auth token as above with modified xor_pkey

In LAN/private mode, auth headers on ctrl requests are omitted entirely.

---

## 3. Video Stream Protocol

### 3.1 Connection

```
GET /sce/premo/session/video HTTP/1.1
Host: <ps3_ip>:9293
User-Agent: premo/1.0.0 libhttp/1.0.0
Accept: */*;q=0.01
Accept-Encoding:""
Accept-Charset: iso-8859-1;q=0.01
PREMO-Video-Codec: <codec from session response, e.g. "AVC">
PREMO-Video-Resolution: 480x272
PREMO-Auth: <auth_token>
SessionID: <session_id>
Connection: Keep-Alive
```

The PS3 responds with HTTP 200 and begins chunked transfer encoding.

### 3.2 Chunked Transfer Format

Each chunk arrives as:
```
XXXXXX\r\n        (6 hex digits = chunk length, e.g. "000400")
[32-byte header][payload]
\r\n              (trailing CRLF, sometimes absent)
```

Chunks may arrive split across TCP reads. Reassembly logic must:
- Buffer partial data between reads
- Strip leading 0x0D 0x0A
- Parse 6-character hex chunk length
- Wait until full chunk_len bytes are available
- Extract 32-byte header + payload

### 3.3 Stream Packet Header (32 bytes)

All multi-byte fields are **big-endian**.

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0 | 1 | magic[0] | Always `0x80` |
| 1 | 1 | magic[1] | Stream type identifier (see below) |
| 2 | 2 | frame | Frame counter (big-endian uint16) |
| 4 | 4 | clock | Presentation timestamp in clock frequency units (big-endian uint32) |
| 8 | 4 | root | Unknown, 4 bytes |
| 12 | 2 | unk2 | Unknown |
| 14 | 2 | unk3 | Unknown |
| 16 | 2 | len | Payload length in bytes (big-endian uint16) |
| 18 | 2 | unk4 | Unknown |
| 20 | 2 | unk5 | Unknown |
| 22 | 2 | unk6 | Video encryption flag |
| 24 | 2 | unk7 | Unknown |
| 26 | 2 | unk8 | Audio encryption flag |
| 28 | 2 | unk9 | Unknown |
| 30 | 2 | unk10 | Unknown |

### 3.4 Magic Byte Values (magic[1])

| Value | Type | Description |
|-------|------|-------------|
| `0xFF` | Video | H.264/AVC |
| `0xFE` | Video | H.264/AVC (alternate, same handling as 0xFF) |
| `0xFB` | Video | MPEG4 (used by some PS1 games, e.g. PixelJunk) |
| `0x80` | Audio | AAC (M4A) |
| `0xFC` | Audio | ATRAC3 |
| `0xFD` | Signal | Restore/reset (discard, re-initialize decoders) |
| `0x81` | Signal | PS3 shutdown/power-off |

### 3.5 NAL Unit Format

**The PS3 sends Annex B format.**

After decryption, H.264 keyframe payloads start with `0x00` (the beginning of a
NAL start code `00 00 00 01` or `00 00 01`). open-rp validates that byte[0] == 0x00
after decryption as a corruption check. The raw data is fed directly to
`avcodec_decode_video2()` without any NAL unit reformatting, confirming Annex B.

This is important because:
- **FFmpeg** expects Annex B (start codes) -- works directly
- **Android MediaCodec** also accepts Annex B for H.264 -- works directly
- No AVCC-to-Annex-B conversion needed

### 3.6 Video Frame Resolution

Default: **480x272** (PSP native resolution). This is the only resolution open-rp
requests. The PS3 may support other resolutions per `PREMO-Video-Resolution-Ability`
header in the session response, but 480x272 is standard.

### 3.7 Clock Synchronization

- Clock frequency = value from `PREMO-Video-ClockFrequency` header (typically 90000)
- Each packet has a `clock` field (big-endian uint32) in clock units
- Convert to seconds: `clock_value / clock_frequency`
- open-rp maintains a master clock from wall time and syncs video to audio clock
- Drift formula: `drift_ms = (video_clock - audio_clock) / clock_freq * 1000`
- If video is ahead, delay display by drift amount

---

## 4. Audio Stream Protocol

### 4.1 Connection

```
GET /sce/premo/session/audio HTTP/1.1
Host: <ps3_ip>:9293
User-Agent: premo/1.0.0 libhttp/1.0.0
Accept: */*;q=0.01
Accept-Encoding:""
Accept-Charset: iso-8859-1;q=0.01
PREMO-Audio-Codec: <codec from session response, e.g. "M4A">
PREMO-Audio-Bitrate: 128000
PREMO-Auth: <auth_token>
SessionID: <session_id>
Connection: Keep-Alive
```

Same chunked transfer format as video. Same 32-byte header structure.

### 4.2 AAC Audio (magic[1] = 0x80)

- Codec: AAC (identified as "M4A" in PREMO headers)
- Parameters from session response headers:
  - `PREMO-Audio-Channels`: typically 2 (stereo)
  - `PREMO-Audio-SamplingRate`: typically 48000
  - `PREMO-Audio-Bitrate`: typically 128000
  - `PREMO-Audio-ClockFrequency`: typically 90000
- Output format: 16-bit signed PCM (AUDIO_S16SYS in open-rp)

### 4.3 ATRAC3 Audio (magic[1] = 0xFC)

- Codec: ATRAC3 (Sony proprietary, identified as "ATRAC" in PREMO headers)
- Requires extra codec configuration:
  - block_align = 192 * channels
  - extradata: 14-byte struct (see open-rp orpInitAudioCodec)
- Less common than AAC; some PS1/legacy games may use it
- FFmpeg has an ATRAC3 decoder

### 4.4 Audio Decoding Flow

1. Receive chunked packet
2. Parse 32-byte header
3. Decrypt if encrypted (see section 5)
4. Feed payload to audio decoder (FFmpeg avcodec_decode_audio3 or equivalent)
5. Get PCM samples
6. Queue for playback
7. Use clock field for A/V sync

---

## 5. Stream Decryption

### 5.1 Algorithm

**AES-128-CBC decrypt** for all stream types.

- **Key**: `xor_pkey` (pkey XOR SKEY0), set once as AES decrypt key
- **IV**: Fresh copy of `xor_nonce` (nonce XOR SKEY2) for EVERY packet
- **Data length**: `payload_size - (payload_size % 16)` -- only full 16-byte blocks
  are decrypted. Any trailing bytes less than 16 remain as plaintext.

The IV MUST be re-initialized from xor_nonce before each packet because AES-CBC
modifies the IV buffer in place during decryption.

### 5.2 When to Decrypt

Not all packets are encrypted. The encryption flags in the header determine this:

| Stream Type | Condition for Encryption |
|-------------|------------------------|
| H.264 (0xFF, 0xFE) | `unk6 == 0x0104` (big-endian, i.e. bytes 22-23 = 01 04) |
| MPEG4 (0xFB) | `unk6 == 0x0100` OR `unk6 == 0x0104` (big-endian) |
| AAC (0x80) | `unk8 != 0` (bytes 26-27 are non-zero) |
| ATRAC3 (0xFC) | `unk8 != 0` (bytes 26-27 are non-zero) |

**Note on endianness**: open-rp uses `SDL_SwapLE16(0x0401)` which produces `0x0104` on
a big-endian platform. The wire bytes at offset 22-23 should be checked accordingly.
Since the header is big-endian, treat unk6 as big-endian uint16; encrypted H.264
keyframes have unk6 = `0x0401` as stored in memory on a little-endian system, which
is bytes `04 01` on the wire. Check raw bytes: `header[22] == 0x04 && header[23] == 0x01`
for H.264, or `header[22] == 0x00 && header[23] == 0x01` also for MPEG4.

**Practical simplification**: In practice, for H.264 the encrypted packets are
I-frames (keyframes). P-frames are typically not encrypted. Only keyframes need
decryption. After decryption, byte[0] of the H.264 payload MUST be 0x00 (start of
NAL start code). If it is not, decryption failed (wrong key, wrong IV, or corruption).

### 5.3 Pseudocode

```kotlin
fun decryptStreamPacket(payload: ByteArray, xorPkey: ByteArray, xorNonce: ByteArray): ByteArray {
    val iv = xorNonce.copyOf()  // FRESH copy every packet
    val decryptLen = payload.size - (payload.size % 16)
    if (decryptLen == 0) return payload

    val result = payload.copyOf()
    val decrypted = aes128CbcDecrypt(
        data = result.sliceArray(0 until decryptLen),
        key = xorPkey,
        iv = iv
    )
    System.arraycopy(decrypted, 0, result, 0, decryptLen)
    return result
}
```

---

## 6. Controller Input (Pad) Protocol

### 6.1 Connection

Open a raw TCP connection to `<ps3_ip>:9293` and send HTTP headers:

```
POST /sce/premo/session/pad HTTP/1.1
User-Agent: premo/1.0.0 libhttp/1.0.0
Accept: */*;q=0.01
Accept-Encoding:
Accept-Charset: iso-8859-1;q=0.01
Host: <ps3_ip>:9293
PREMO-Pad-Index: 1
SessionID: <session_id>
Connection: Keep-Alive
Content-Length: 7680
\r\n
```

Content-Length = `ORP_PADSTATE_MAX * ORP_PADSTATE_LEN` = 60 * 128 = 7680.

After sending headers, continuously send 128-byte pad state packets.

### 6.2 Pad Batching Protocol

Every **60 packets** (ORP_PADSTATE_MAX = 60):
1. Read an 80-byte reply from the PS3
2. Re-send the full HTTP headers (same as above)
3. Continue sending 128-byte pad packets

This creates a repeating cycle: send 60 pad packets -> read reply -> resend headers -> repeat.

### 6.3 Pad Encryption

**LAN/private mode**: Pad data sent as plaintext (no encryption).

**Public/internet mode**: AES-128-CBC encrypt each 128-byte packet.
- Key: `xor_pkey` (same key as stream decryption, but set for ENCRYPT)
- IV: Initialized ONCE from `xor_nonce`, then **chains across all packets**
  (unlike stream decryption where IV is reset per packet)
- The IV for packet N+1 is the last 16 bytes of the ciphertext of packet N

### 6.4 128-Byte Pad State Structure

Initial state (all zeros except noted):

```
Offset 0x00: 00 00 00 74 00 00 00 00   (byte 3 = 0x74 constant)
Offset 0x08: 00 80 00 80 00 80 00 80   (analog sticks centered)
Offset 0x10: 00 00 00 00 00 00 00 00
Offset 0x18: 00 00 00 00 00 00 00 00
Offset 0x20: 00 00 00 00 00 00 00 00
Offset 0x28: 02 00 02 00 02 00 02 00   (accelerometer defaults)
Offset 0x30: 00 00 00 00 00 00 00 00
Offset 0x38: 00 00 00 00 00 00 00 00
Offset 0x40: 00 00 00 00 00 00 00 00   (timestamp at 0x40)
Offset 0x48: 00 00 00 00 00 00 00 00   (event_id at 0x48)
Offset 0x50: 00 01 00 00 00 00 00 00   (byte 0x51 = 0x01)
Offset 0x58: 00 00 00 00 00 00 00 00
Offset 0x60: 00 00 00 00 00 00 00 00
Offset 0x68: 00 00 00 00 00 00 00 00
Offset 0x70: 00 00 00 00 00 00 00 00
Offset 0x78: 00 00 00 00 00 00 00 00
```

Home button variant: byte 0x01 = 0x01 (rest same as above, sticks at 0x80 center).

### 6.5 Field Map

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0x00 | 1 | - | Always 0x00 |
| 0x01 | 1 | home | 0x01 for PS/Home button, 0x00 otherwise |
| 0x02 | 1 | - | 0x00 |
| 0x03 | 1 | type | Always 0x74 |
| 0x04 | 1 | - | 0x00 |
| 0x05 | 1 | buttons_lo | Select=0x01, L3=0x02, R3=0x04, Start=0x08, DPadUp=0x10, DPadRight=0x20, DPadDown=0x40, DPadLeft=0x80 |
| 0x06 | 1 | - | 0x00 |
| 0x07 | 1 | buttons_hi | L2=0x01, R2=0x02, L1=0x04, R1=0x08, Triangle=0x10, Circle=0x20, Cross=0x40, Square=0x80 |
| 0x08-0x09 | 2 BE | rx_axis | Right analog X (0x0080 = center) |
| 0x0A-0x0B | 2 BE | ry_axis | Right analog Y (0x0080 = center) |
| 0x0C-0x0D | 2 BE | lx_axis | Left analog X (0x0080 = center) |
| 0x0E-0x0F | 2 BE | ly_axis | Left analog Y (0x0080 = center) |
| 0x28-0x2F | 8 | accel | Accelerometer (4x uint16 LE, default 0x0200 each) |
| 0x40-0x43 | 4 BE | timestamp | Ticks since connection / 16 |
| 0x48-0x4B | 4 BE | event_id | Incrementing event counter |
| 0x51 | 1 | - | Always 0x01 |

### 6.6 Button Encoding (PSP-style offsets)

The defines in open-rp use a packed format: `0xOOBB` where `OO` is the byte offset
in the pad state array and `BB` is the bitmask.

```
SELECT:    offset=0x05, mask=0x01
L3:        offset=0x05, mask=0x02
R3:        offset=0x05, mask=0x04
START:     offset=0x05, mask=0x08
DPAD_UP:   offset=0x05, mask=0x10
DPAD_RIGHT:offset=0x05, mask=0x20
DPAD_DOWN: offset=0x05, mask=0x40
DPAD_LEFT: offset=0x05, mask=0x80
L2:        offset=0x07, mask=0x01
R2:        offset=0x07, mask=0x02
L1:        offset=0x07, mask=0x04
R1:        offset=0x07, mask=0x08
TRIANGLE:  offset=0x07, mask=0x10
CIRCLE:    offset=0x07, mask=0x20
CROSS:     offset=0x07, mask=0x40
SQUARE:    offset=0x07, mask=0x80
HOME:      offset=0x01, mask=0x01
```

To press a button: `pad[offset] |= mask`
To release a button: `pad[offset] &= ~mask`

### 6.7 Analog Sticks

- Center value: 0x0080 (big-endian uint16 = 128)
- Range: 0x0000 to 0x00FF (0 to 255)
- Left stick X: offset 0x0C (2 bytes BE)
- Left stick Y: offset 0x0E (2 bytes BE)
- Right stick X: offset 0x08 (2 bytes BE)
- Right stick Y: offset 0x0A (2 bytes BE)

### 6.8 Timestamp and Event ID

- **event_id** (offset 0x48, 4 bytes BE): Incrementing counter, starts at 0.
  Incremented for every button/stick event. Home button sends id=0.
- **timestamp** (offset 0x40, 4 bytes BE): `(current_ticks - start_ticks) / 16`
  where ticks are milliseconds since pad connection established.

---

## 7. Control Channel

### 7.1 Bitrate Change

```
GET /sce/premo/session/ctrl HTTP/1.1
Host: <ps3_ip>:9293
User-Agent: premo/1.0.0 libhttp/1.0.0
Mode: change-bitrate
Bitrate: <value>
Max-Bitrate: <max_value>
PREMO-Auth: <auth_change_bitrate>   (public mode only)
SessionID: <session_id>
Connection: Keep-Alive
```

Bitrate values: 256000, 384000, 512000, 768000, 1024000.

### 7.2 Session Termination

```
GET /sce/premo/session/ctrl HTTP/1.1
Host: <ps3_ip>:9293
User-Agent: premo/1.0.0 libhttp/1.0.0
Mode: session-term
PREMO-Auth: <auth_session_term>   (public mode only)
SessionID: <session_id>
Connection: Keep-Alive
```

### 7.3 Auth Tokens for Control

Each control operation uses a different auth token derived from a modified xor_pkey:

| Operation | XOR bytes for first 4 of xor_pkey | Hex |
|-----------|----------------------------------|-----|
| Normal (stream/video/audio) | (none) | - |
| change-bitrate | 'c','h','a','n'+17 | 63 68 61 7F |
| session-term | 's','e','s','s'+17 | 73 65 73 84 |

These are only needed in public/internet mode. In LAN mode, omit PREMO-Auth.

---

## 8. Video Decoding Options for KMP

### 8.1 Desktop (JVM)

**Option A: JavaCV (recommended for quick start)**
- JavaCV wraps FFmpeg with Java bindings
- Gradle: `implementation("org.bytedeco:javacv-platform:1.5.9")`
- Supports H.264 and MPEG4 out of the box
- Can decode Annex B H.264 directly via `avcodec_decode_video2` equivalent
- Output: `Frame` object with pixel data, convertible to BufferedImage
- Display: Render BufferedImage to Compose Canvas or use SwsContext for YUV->RGB

**Option B: FFmpeg via JNI (better performance)**
- Write thin JNI wrapper around libavcodec
- More work but lower overhead and more control
- Can integrate with hardware acceleration (VAAPI, VideoToolbox on Mac)

**Option C: Pure Java decoder (not recommended)**
- H.264 software decoders exist in Java but are slow and incomplete

**Recommended for Desktop**: JavaCV for initial implementation, migrate to JNI
wrapper for production.

### 8.2 Android

**MediaCodec (hardware H.264 -- strongly recommended)**
- `MediaCodec.createDecoderByType("video/avc")` for H.264
- Feed Annex B NAL units directly -- MediaCodec accepts start codes natively
- Configure with: width=480, height=272, mime="video/avc"
- Use Surface output for efficient rendering to SurfaceView/TextureView
- Hardware accelerated on all Android devices with API 24+

**MediaCodec flow:**
1. Create decoder: `MediaCodec.createDecoderByType("video/avc")`
2. Configure with MediaFormat (width, height)
3. Start decoder
4. For each decrypted video packet:
   a. Dequeue input buffer
   b. Copy Annex B data into buffer
   c. Queue input buffer with presentationTimeUs from clock field
   d. Dequeue output buffer
   e. Render or process frame
5. Stop/release on disconnect

**SPS/PPS handling**: The first keyframe from the PS3 should contain SPS and PPS
NAL units prepended to the IDR frame. MediaCodec needs these before it can decode.
They arrive naturally with the first encrypted keyframe after stream start.

### 8.3 Common Considerations

- Frame resolution: 480x272 (low -- will need upscaling for display)
- Frame rate: Typically 30fps (from PREMO-Video-Framerate)
- Latency target: Decode and display as fast as possible; no buffering beyond 1-2 frames
- PS3 sends both I-frames (keyframes, encrypted) and P-frames (delta, often unencrypted)
- On restore event (magic 0xFD), flush decoder and wait for next keyframe

---

## 9. Audio Decoding Options for KMP

### 9.1 Desktop (JVM)

**Option A: JavaCV / FFmpeg (recommended)**
- Same JavaCV dependency handles audio too
- `avcodec_decode_audio3` equivalent for AAC decoding
- Output: PCM samples (16-bit signed)
- Playback: `javax.sound.sampled.SourceDataLine` for PCM output
  - Format: 48000 Hz, 16-bit, stereo, signed, system byte order
  - Buffer size: 1024 samples works well

**Option B: javax.sound + AAC library**
- Java Sound API can play raw PCM
- Need separate AAC decoder (e.g., JAAD library) -- less maintained

### 9.2 Android

**MediaCodec AAC decoder:**
- `MediaCodec.createDecoderByType("audio/mp4a-latm")` for AAC
- Configure MediaFormat with sample rate (48000), channels (2), AAC profile
- Feed raw AAC frames from decrypted audio packets
- Output PCM to AudioTrack for playback

**AudioTrack playback:**
```kotlin
val audioTrack = AudioTrack.Builder()
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .build())
    .setAudioFormat(AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(48000)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
        .build())
    .setBufferSizeInBytes(4096)
    .setTransferMode(AudioTrack.MODE_STREAM)
    .build()
```

### 9.3 ATRAC3 Handling

ATRAC3 is rare but possible (some PS1 games). Only FFmpeg has an ATRAC3 decoder.
On Android, would need to bundle FFmpeg's ATRAC3 decoder via JNI.
For initial implementation, support AAC only and log a warning for ATRAC3 packets.

### 9.4 A/V Synchronization

- Both video and audio packets have `clock` fields in their 32-byte headers
- Clock frequency from session headers (typically 90000 for both)
- Maintain a master clock from wall time: `master = (elapsed_ms) * 90000 / 1000`
- Audio is the sync master: video waits if it's ahead of audio
- If video drifts > drift threshold, adjust video display delay
- On decoder errors (5+ consecutive), reinitialize the decoder

---

## 10. xRegistry Bypass Path

### 10.1 What is xRegistry.sys

`xRegistry.sys` is the PS3's system registry file located at:
```
/dev_flash2/etc/xRegistry.sys
```

It stores all system settings including Remote Play device registrations.
On HEN/CFW PS3s, it can be accessed via FTP (e.g., webMAN's FTP server).

### 10.2 Binary Format

**File structure (3 sections):**

| Region | Offset | Size | Description |
|--------|--------|------|-------------|
| Header | 0x0000 | 16 bytes | Magic + version |
| Key Table | 0x0010 | ~0xFFE0 | Hierarchical key names |
| Key Table Footer | 0xFFF0 | 16 bytes | Footer magic |
| Value Table | 0x10000 | up to 0x10000 | Key-value data |

**Header (16 bytes):**
```
BC AD AD BC 00 00 00 90 00 00 00 02 BC AD AD BC
```

**Key Table Footer (at offset 0xFFF0):**
```
4D 26 00 7A 4D 26 00 62 00 04 00 00 00 00 00 00
```

### 10.3 Key Table Entry Format

Each key entry:
```
[2 bytes BE] Unknown (varies, e.g. 25 A0)
[2 bytes BE] Key name length
[1 byte]     Key type: 0=Unlocked, 1=Locked, 2=Hidden, 3=Directory
[N bytes]    Key name (UTF-8)
[1 byte]     Null terminator (0x00)
```

Key table terminated by: `AA BB CC DD EE`

Keys form a hierarchical path system, e.g.:
```
/setting/premo/psp01/key
/setting/premo/psp01/mac
/setting/premo/psp01/id
```

### 10.4 Value Table Entry Format (starts at 0x10000)

Each value entry:
```
[2 bytes BE] Unknown1
[2 bytes BE] KeyOffset (relative to key table start at 0x10)
[2 bytes BE] Unknown2
[2 bytes BE] Value length
[1 byte]     Value type: 0=Boolean, 1=Integer(4 bytes BE), 2=String/Binary
[N bytes]    Value data
[1 byte]     Null terminator (0x00)
```

Value table terminated by: `AA BB CC DD EE` (same 5-byte terminator).

### 10.5 Remote Play Registration Entries

The PS3 stores registered device data under the path `/setting/premo/psp01/`:

| Key Path | Type | Size | Description |
|----------|------|------|-------------|
| `/setting/premo/psp01/key` | Binary (type 2) | 16 bytes | The pkey (private key) |
| `/setting/premo/psp01/mac` | Binary (type 2) | 6 bytes | Device MAC address |
| `/setting/premo/psp01/id` | Binary (type 2) | 16 bytes | Device OpenPSID / PSPID |

Additional slots may exist as `psp02`, `psp03`, etc.

### 10.6 Bypass Procedure (Step by Step)

**Prerequisites:**
- PS3 with HEN (Hybrid Firmware Enable) or CFW
- FTP access enabled (webMAN-MOD or similar)
- A computer on the same network

**Step 1: Download xRegistry.sys**
```bash
# Connect via FTP to PS3
ftp <ps3_ip>
# Navigate and download
cd /dev_flash2/etc/
get xRegistry.sys
```

**Step 2: Generate Registration Credentials**

You need to prepare 3 values:
- **pkey**: 16 random bytes (or a known test key)
- **device_id (PSPID)**: 16 bytes identifying your device (can be random for testing)
- **device_mac**: 6-byte MAC address of the device that will connect

**Step 3: Locate or Create the Key Entries**

Parse xRegistry.sys to find existing `/setting/premo/psp01/key` entry.
If Remote Play has never been used, the keys may not exist and would need to be created.

If they exist, note the:
- Key offset in the key table (for the KeyOffset field in value entries)
- Value offset in the value table

**Step 4: Modify the Value Table**

For each of the 3 values (key, mac, id), find the corresponding value entry
in the value table and overwrite the value bytes:

- `/setting/premo/psp01/key`: Write 16 bytes of your chosen pkey
- `/setting/premo/psp01/mac`: Write 6 bytes of your device MAC
- `/setting/premo/psp01/id`: Write 16 bytes of your device ID

**Step 5: Upload Modified xRegistry.sys**
```bash
ftp <ps3_ip>
cd /dev_flash2/etc/
put xRegistry.sys
```

**Step 6: Reboot PS3**

The PS3 reads xRegistry.sys at boot. Reboot for changes to take effect.

**Step 7: Connect**

Configure PsOldRemotePlay with:
- The same pkey you injected
- The same device_id (PSPID)
- The same device_mac
- PS3 IP address

The session handshake should now succeed because the PS3 will find your
credentials in its registry.

### 10.7 Checksums

**There are NO checksums in xRegistry.sys.** The file format has no integrity
verification. The parser code reads entries sequentially with no hash or CRC
validation. This means direct binary editing is safe as long as the format
structure is preserved.

### 10.8 Important Caveats

1. **Creating new entries**: If `/setting/premo/psp01/` does not exist, you must
   add key entries to the key table AND value entries to the value table. This
   requires shifting data and is more complex than modifying existing entries.

2. **Existing registration**: If a device is already registered, you can simply
   overwrite the existing pkey/mac/id values -- much simpler.

3. **Do a trial registration first**: Register any device (even a phone via PS3's
   "Register Device" menu) to create the psp01 slot, then overwrite the values.

4. **Backup**: Always backup xRegistry.sys before modifying. A corrupted file
   can reset PS3 settings.

5. **File size**: The file is typically around 128KB (0x20000 bytes). The value
   table extends to offset 0x20000 max.

### 10.9 Writing a Kotlin xRegistry Editor

Based on the parser analysis, a minimal editor needs:

```kotlin
// Pseudocode for xRegistry binary editor
class XRegistryEditor(val data: ByteArray) {
    val HEADER = byteArrayOf(0xBC.toByte(), 0xAD.toByte(), 0xAD.toByte(), 0xBC.toByte(),
                              0x00, 0x00, 0x00, 0x90.toByte(), 0x00, 0x00, 0x00, 0x02,
                              0xBC.toByte(), 0xAD.toByte(), 0xAD.toByte(), 0xBC.toByte())
    val KEY_TABLE_START = 0x10
    val KEY_TABLE_FOOTER_OFFSET = 0xFFF0
    val VALUE_TABLE_START = 0x10000

    fun findValueForKey(keyPath: String): Int? {
        // 1. Search key table for keyPath, get its relative offset
        // 2. Search value table for entry with matching KeyOffset
        // 3. Return absolute offset of value data
    }

    fun writeValue(absoluteOffset: Int, newValue: ByteArray) {
        System.arraycopy(newValue, 0, data, absoluteOffset, newValue.size)
    }
}
```

---

## 11. Implementation Checklist

### Phase 1: Stream Reception (get data flowing)
- [ ] Implement chunked HTTP response parser for video/audio streams
- [ ] Parse 32-byte stream packet headers
- [ ] Identify packet types by magic[1]
- [ ] Handle restore (0xFD) and shutdown (0x81) signals
- [ ] Implement stream decryption (AES-128-CBC with xor_pkey/xor_nonce)
- [ ] Validate decryption success (H.264 keyframe byte[0] == 0x00)

### Phase 2: Video Decoding
- [ ] Desktop: Integrate JavaCV or FFmpeg JNI for H.264 decoding
- [ ] Android: Set up MediaCodec H.264 decoder with Surface output
- [ ] Feed decrypted Annex B NAL units to decoder
- [ ] Handle SPS/PPS from first keyframe
- [ ] Render decoded frames to Compose UI (Canvas on desktop, SurfaceView on Android)
- [ ] Handle MPEG4 fallback (0xFB magic) for PS1 games

### Phase 3: Audio Decoding
- [ ] Desktop: Integrate JavaCV/FFmpeg for AAC decoding + javax.sound playback
- [ ] Android: Set up MediaCodec AAC decoder + AudioTrack playback
- [ ] Implement A/V clock synchronization
- [ ] Handle ATRAC3 (optional, low priority)

### Phase 4: Controller Input
- [ ] Implement TCP connection for POST /sce/premo/session/pad
- [ ] Build 128-byte pad state buffer with correct initial values
- [ ] Map ControllerState (existing interface) to PSP pad byte format
- [ ] Implement 60-packet batching with reply read and header resend
- [ ] Implement pad encryption for public mode (AES-CBC chained)
- [ ] Handle timestamp and event_id fields

### Phase 5: Control Channel
- [ ] Implement bitrate change via /sce/premo/session/ctrl
- [ ] Implement session termination
- [ ] Generate per-operation auth tokens (change-bitrate, session-term)

### Phase 6: xRegistry Bypass Tool
- [ ] Parse xRegistry.sys binary format
- [ ] Find /setting/premo/psp01/ entries
- [ ] Implement value overwrite (pkey, mac, id)
- [ ] FTP upload/download integration (or manual file exchange)
- [ ] Add UI for bypass flow in the app

---

## Appendix A: Complete PREMO Header List

From open-rp, all known PREMO HTTP headers:

| Header | Direction | Description |
|--------|-----------|-------------|
| PREMO-Application-Reason | Response | Error reason code |
| PREMO-Audio-Bitrate | Both | Audio bitrate (e.g. 128000) |
| PREMO-Audio-Bitrate-Ability | Response | Supported bitrates |
| PREMO-Audio-Channels | Response | Channel count (e.g. 2) |
| PREMO-Audio-ClockFrequency | Response | Audio clock freq (e.g. 90000) |
| PREMO-Audio-Codec | Both | Codec name (M4A, ATRAC) |
| PREMO-Audio-Config | Response | Audio config data |
| PREMO-Audio-SamplingRate | Response | Sample rate (e.g. 48000) |
| PREMO-Auth | Request | Auth token (base64) |
| PREMO-Exec-Mode | Response | Current PS3 mode (VSH, PS3, PS1) |
| PREMO-Mode | Request | Protocol mode (PREMO) |
| PREMO-Nonce | Response | Session nonce (base64, 16 bytes) |
| PREMO-Pad-Assign | Both | Pad assignment |
| PREMO-Pad-Complete | Response | Pad setup complete |
| PREMO-Pad-Index | Request | Pad index (1) |
| PREMO-Pad-Info | Request | Pad type (PSP-Pad) |
| PREMO-Platform-Info | Request | Platform (PSP, Phone, PC) |
| PREMO-Power-Control | Request | Power control commands |
| PREMO-PS3-Nickname | Response | PS3 name (base64) |
| PREMO-PSPID | Request | Device ID (base64, 16 bytes) |
| PREMO-SIGNIN-ID | Request | PSN sign-in ID (public mode) |
| PREMO-Trans | Request | "capable" |
| PREMO-Trans-Mode | Response | Transfer mode |
| PREMO-UserName | Request | Device owner (base64) |
| PREMO-Version | Request | Protocol version (0.3) |
| PREMO-Video-Bitrate | Both | Video bitrate |
| PREMO-Video-Bitrate-Ability | Response | Supported bitrates |
| PREMO-Video-ClockFrequency | Response | Video clock freq (e.g. 90000) |
| PREMO-Video-Codec | Both | Codec name (AVC, M4V) |
| PREMO-Video-Config | Response | Video config data |
| PREMO-Video-Framerate | Both | Frame rate (e.g. 30) |
| PREMO-Video-Framerate-Ability | Response | Supported framerates |
| PREMO-Video-Resolution | Both | Resolution (e.g. 480x272) |
| PREMO-Video-Resolution-Ability | Response | Supported resolutions |
| SessionID | Both | Session identifier |
| Mode | Request (ctrl) | "change-bitrate" or "session-term" |
| Bitrate | Request (ctrl) | Target bitrate for change |
| Max-Bitrate | Request (ctrl) | Maximum bitrate for change |

## Appendix B: Existing Project Interfaces to Implement

The project already defines these interfaces that need real implementations:

| Interface | File | What's needed |
|-----------|------|---------------|
| `VideoRenderer` | `protocol/VideoRenderer.kt` | Decode H.264 packets, render to UI |
| `ControllerInputSender` | `protocol/ControllerInput.kt` | Build pad packets, TCP connection |
| `PremoSessionHandler.startVideoStream()` | `protocol/PremoSession.kt` | Chunked HTTP, decrypt, demux |
| `PremoSessionHandler.startAudioStream()` | `protocol/PremoSession.kt` | Same for audio stream |

The `StreamPacket` data class already has the right shape (magic, frame, clock,
rawHeader, payload) matching the 32-byte header structure.
