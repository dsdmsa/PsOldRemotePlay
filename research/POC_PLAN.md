# POC Implementation Plan

## Strategy: Desktop JVM first → Android later

Build the protocol layer as pure Kotlin (no Android deps). Test on desktop with logging and file dumps. Port to Android only for the video display.

---

## Phase 1: Discovery (Desktop JVM) — ~1 hour

**Goal:** Send UDP "SRCH" and get PS3's response.

**What to build:**
- `Ps3Discovery.kt` — send 4-byte `SRCH` UDP broadcast to port 9293
- Parse 156-byte `RESP`: extract IP, MAC (bytes 10-15), nickname (bytes 16-143)
- Print: "Found PS3: <nickname> at <IP> (MAC: XX:XX:XX:XX:XX:XX)"

**Dependencies:** None (java.net.DatagramSocket)

**Success criteria:** PS3 discovered on LAN, IP and MAC printed.

---

## Phase 2: Session Handshake (Desktop JVM) — ~2 hours

**Goal:** Complete the PREMO session handshake, get 200 OK with nonce.

**What to build:**
- `PremoSession.kt` — HTTP client using raw sockets or java.net.HttpURLConnection
- Build the GET request with all PREMO-* headers:
  ```
  GET /sce/premo/session HTTP/1.1
  PREMO-PSPID: <base64 device ID>
  PREMO-Version: 0.3
  PREMO-Mode: PREMO
  PREMO-Platform-Info: Phone
  PREMO-UserName: <base64 name>
  PREMO-Trans: capable
  PREMO-Video-Codec-Ability: M4V,AVC,AVC/CAVLC
  PREMO-Video-Resolution: 480x272
  PREMO-Video-Bitrate: 512000
  PREMO-Video-Framerate: 30
  PREMO-Audio-Codec-Ability: M4A,ATRAC
  ```
- Parse the 200 OK response: extract SessionID, PREMO-Nonce, codec settings, exec mode
- Print all response headers

**Dependencies:** None (java.net.Socket for raw HTTP)

**BLOCKER:** Need pkey + device ID. For initial testing:
- Option A: Use hardcoded test values from xRegistry dump (if you have any registered device)
- Option B: Skip to Phase 4 (registration) first to get real keys
- Option C: Just send the request and see what error code comes back (403 tells us the PS3 is listening)

**Success criteria:**
- 403 response = PS3 is there, protocol works, just need valid keys
- 200 response = full session established, nonce received

---

## Phase 3: Key Derivation + Auth Token (Desktop JVM) — ~1 hour

**Goal:** Derive session keys and generate PREMO-Auth token.

**What to build:**
- `PremoCrypto.kt` — all crypto operations:
  ```kotlin
  // Key derivation
  val xorPkey = xorBytes(pkey, skey0)       // AES key
  val xorNonce = xorBytes(nonce, skey2)     // AES IV

  // Auth token
  val plaintext = ByteArray(16)
  System.arraycopy(deviceMac, 0, plaintext, 0, 6)  // MAC + zeros
  val authToken = aesEncrypt(plaintext, xorPkey, xorNonce)
  val premoAuth = Base64.encode(authToken)
  ```
- All static keys hardcoded (skey0, skey1, skey2, phone nonce XOR key)

**Dependencies:** javax.crypto (built into JDK)

**Success criteria:** Auth token generated, can be verified against Open-RP's algorithm.

---

## Phase 4: Registration (Desktop JVM) — ~3 hours

**Goal:** Register with a stock PS3 and receive the pkey.

**What to build:**
- `PremoRegistration.kt`:
  1. User starts "Register Device → Phone" on PS3
  2. Desktop app connects to PS3's WiFi AP (manual step — connect WiFi first)
  3. Build registration body:
     ```
     Client-Type: Phone
     Client-Id: <our device ID as hex>
     Client-Mac: <our MAC as hex>
     Client-Nickname: PsOldRemotePlay
     ```
  4. Generate 16 random bytes (our key material)
  5. Derive AES key: `key[i] = (random[i] - i - 0x28) XOR phoneXorKey[i]`
  6. Set IV = phoneIvBase, XOR second 8 bytes with context value (try 0 first)
  7. AES-CBC-encrypt the body
  8. Append the 16 random key bytes to the encrypted body
  9. Send: `POST /sce/premo/regist HTTP/1.1` with Content-Length
  10. Receive and decrypt response
  11. Extract `PREMO-Key` (the pkey!)
  12. Save pkey + PS3 MAC + PS3 nickname locally

**Dependencies:** javax.crypto, manual WiFi connection

**Success criteria:** Receive 200 OK, extract and save pkey. If 403, try different context values.

**IMPORTANT:** If context=0 doesn't work, capture the traffic with Wireshark during a real registration (VAIO/PSP) to determine the correct value.

---

## Phase 5: Video Stream (Desktop JVM) — ~2 hours

**Goal:** Receive and decrypt the video stream, dump to file.

**What to build:**
- `PremoVideoStream.kt`:
  1. Send `GET /sce/premo/session/video` with SessionID + PREMO-Auth
  2. Read 32-byte packet headers continuously
  3. Parse: magic, frame counter, clock, payload length
  4. For encrypted packets (magic[1] == 0xFF/0xFE): AES-CBC decrypt
  5. **Reset IV before each packet** (IV = xorNonce)
  6. Write decrypted H.264 NAL units to a file: `output.h264`

**Dependencies:** javax.crypto, java.net.Socket

**Verification:** `ffplay output.h264` or `ffmpeg -i output.h264 -c copy output.mp4`
If the video plays → the entire protocol works!

**Success criteria:** `output.h264` file plays correctly showing PS3 XMB.

---

## Phase 6: Live Video Display (Desktop JVM) — ~2 hours

**Goal:** Display the video stream in a window in real-time.

**Options:**
- JavaFX MediaPlayer with piped input
- FFmpeg via ProcessBuilder (pipe decrypted H.264 → ffplay)
- JavaCV (OpenCV + FFmpeg Java bindings) — most robust
- VLCJ (VLC Java bindings)

**Simplest approach:** Pipe to ffplay:
```kotlin
val process = ProcessBuilder("ffplay", "-f", "h264", "-")
    .redirectErrorStream(true)
    .start()
val ffplayInput = process.outputStream
// Write decrypted NAL units to ffplayInput
```

**Success criteria:** Live PS3 XMB visible in a window on desktop.

---

## Phase 7: Port to Android — ~3 hours

**Goal:** Move the working protocol to Android with native video decode.

**What changes:**
- Replace raw Socket with OkHttp (readTimeout=0)
- Replace file dump with MediaCodec + SurfaceView
- Add WiFi permissions for discovery
- Add controller input (onKeyDown/onGenericMotionEvent)

**What stays the same (shared KMP module):**
- PremoCrypto.kt
- PremoSession.kt (header building, response parsing)
- Ps3Discovery.kt
- PremoRegistration.kt (crypto + body building)

---

## Project Structure

```
composeApp/
  src/
    commonMain/kotlin/com/my/psoldremoteplay/
      protocol/
        Ps3Discovery.kt          — UDP SRCH/RESP
        PremoSession.kt          — Session handshake + header parsing
        PremoCrypto.kt            — AES, key derivation, auth token
        PremoRegistration.kt      — Device registration
        PremoStreamReader.kt      — Packet reading + decryption
        PremoConstants.kt         — All static keys, magic values
      model/
        Ps3Info.kt                — PS3 discovery result
        SessionInfo.kt            — Session parameters
        StreamPacket.kt           — 32-byte header struct

    jvmMain/kotlin/                — Desktop entry point
      Main.kt                     — CLI tool for testing
      DesktopVideoPlayer.kt       — ffplay pipe or JavaFX

    androidMain/kotlin/            — Android entry point
      MainActivity.kt             — UI with SurfaceView
      VideoDecoder.kt             — MediaCodec wrapper
      ControllerInput.kt          — Gamepad mapping
```

---

## Estimated Timeline

| Phase | What | Time |
|-------|------|------|
| 1 | UDP Discovery | ~1h |
| 2 | Session Handshake | ~2h |
| 3 | Key Derivation + Auth | ~1h |
| 4 | Registration | ~3h |
| 5 | Video Stream (file dump) | ~2h |
| 6 | Live Display (desktop) | ~2h |
| 7 | Port to Android | ~3h |

**Phase 1-3 can run immediately if you have keys (from any registration method).**
**Phase 4 is needed first if you have NO keys at all.**

---

## What to Build First

**If you have NO registered device and NO keys:**
→ Phase 1 (discovery) → Phase 4 (registration) → Phase 2-3 (session) → Phase 5-6 (video)

**The very first thing to test:** Phase 1 (discovery). If your PS3 responds to SRCH, everything else follows.
