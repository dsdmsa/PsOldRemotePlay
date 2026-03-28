# Android MVP Implementation Research -- PREMO Protocol Client

Practical research for the simplest path to get a PS3 Remote Play video feed displaying on an Android device.

---

## 1. Android MediaCodec for H.264 Decode

### The Simplest Setup

The path of least resistance is `MediaCodec` + `SurfaceView`. No `MediaExtractor`, no `MediaPlayer`, no container format needed. You feed raw H.264 NAL units directly.

### Key Classes

- `android.media.MediaCodec` -- the hardware decoder
- `android.media.MediaFormat` -- describes the stream (resolution, codec)
- `android.view.SurfaceView` -- displays decoded frames via hardware compositor
- `android.view.SurfaceHolder` -- provides the `Surface` to MediaCodec

### Minimal Pattern (Kotlin)

```kotlin
// 1. Create decoder
val codec = MediaCodec.createDecoderByType("video/avc")

// 2. Create format -- PS3 PREMO sends 480x272
val format = MediaFormat.createVideoFormat("video/avc", 480, 272)

// 3. Configure with a Surface from SurfaceView
// surfaceView.holder.surface must be valid (wait for surfaceCreated callback)
codec.configure(format, surfaceView.holder.surface, null, 0)

// 4. Start
codec.start()

// 5. Feed loop (run on background thread)
while (running) {
    // Get a free input buffer
    val inputIndex = codec.dequeueInputBuffer(10_000) // 10ms timeout
    if (inputIndex >= 0) {
        val inputBuffer = codec.getInputBuffer(inputIndex)!!
        inputBuffer.clear()

        // Copy one NAL unit into the buffer
        // IMPORTANT: include the 0x00 0x00 0x00 0x01 start code prefix
        val nalData = getNextNalUnit() // your network read
        inputBuffer.put(nalData)

        // Flag: set BUFFER_FLAG_CODEC_CONFIG for SPS/PPS NAL units
        val flags = if (isSpsOrPps(nalData)) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0
        codec.queueInputBuffer(inputIndex, 0, nalData.size, presentationTimeUs, flags)
    }

    // Drain output -- renders to Surface automatically
    val info = MediaCodec.BufferInfo()
    val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
    if (outputIndex >= 0) {
        // true = render this buffer to the Surface
        codec.releaseOutputBuffer(outputIndex, true)
    }
    // Handle INFO_OUTPUT_FORMAT_CHANGED, INFO_TRY_AGAIN_LATER -- ignore for POC
}
```

### Critical Details for PREMO

1. **SPS/PPS first**: The first NAL units you feed MUST be SPS (NAL type 7) and PPS (NAL type 8). Queue them with `BUFFER_FLAG_CODEC_CONFIG`. MediaCodec will not decode any frames until it has these.

2. **NAL unit framing**: The PREMO stream packet has a 32-byte header with a `len` field (big-endian `Uint16` at offset 16). The payload after this header is the raw H.264 data. You need to detect NAL boundaries by the start code `0x00 0x00 0x00 0x01` or `0x00 0x00 0x01`.

3. **One NAL per buffer**: Feed one complete NAL unit per `queueInputBuffer` call. This is the safest approach.

4. **NAL type detection**: `nalData[4] & 0x1F` gives the NAL type (assuming 4-byte start code):
   - 7 = SPS
   - 8 = PPS
   - 5 = IDR (keyframe)
   - 1 = non-IDR slice

5. **Threading**: Both `dequeueInputBuffer` and `dequeueOutputBuffer` block. Run the decode loop on a dedicated thread (or use `Dispatchers.IO`).

6. **Resolution**: PREMO sends 480x272 (PSP native). The `SurfaceView` will scale this to fill its bounds automatically.

### Sources
- [MediaCodec API Reference](https://developer.android.com/reference/android/media/MediaCodec)
- [Video streaming algorithms with Kotlin: H.264 NAL unit parsing](https://www.tunjid.com/articles/video-streaming-algorithms-with-kotlin-h264-nal-unit-parsing-64b8040829d921c57159857c)
- [H264-App -- minimal Android H.264 decoder](https://github.com/Seb-C89/H264-App)
- [H264-Android-Decoder -- MediaCodec + SurfaceTexture](https://github.com/Kirkify/H264-Android-Decoder)
- [MediaCodec decode h264 example (Gist)](https://gist.github.com/waveacme/4e783c5b687c4aedd33b)
- [Android MediaCodec stuff (bigflake.com)](https://bigflake.com/mediacodec/)
- [MediaCodecExample (Kotlin)](https://github.com/taehwandev/MediaCodecExample)

---

## 2. AES-128-CBC Decryption on Android

### The Simplest Approach

Use `javax.crypto.Cipher` directly. It is built into Android, requires zero dependencies, and handles AES-128-CBC natively.

### Key Classes

- `javax.crypto.Cipher` -- the encryption/decryption engine
- `javax.crypto.spec.SecretKeySpec` -- wraps a raw 16-byte key
- `javax.crypto.spec.IvParameterSpec` -- wraps a raw 16-byte IV

### Minimal Pattern (Kotlin)

```kotlin
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

fun decryptAes128Cbc(
    ciphertext: ByteArray,
    key: ByteArray,       // 16 bytes
    iv: ByteArray         // 16 bytes
): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    val keySpec = SecretKeySpec(key, "AES")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(ciphertext)
}
```

### PREMO-Specific Key Derivation (from Open-RP source)

```kotlin
// The three session keys are FIXED (hardcoded in the protocol):
val skey0 = byteArrayOf(
    0xD1.toByte(), 0xB2.toByte(), 0x12.toByte(), 0xEB.toByte(),
    0x73.toByte(), 0x86.toByte(), 0x6C.toByte(), 0x7B.toByte(),
    0x12.toByte(), 0xA7.toByte(), 0x5E.toByte(), 0x0C.toByte(),
    0x04.toByte(), 0xC6.toByte(), 0xB8.toByte(), 0x91.toByte()
)
val skey2 = byteArrayOf(
    0x65.toByte(), 0x2D.toByte(), 0x8C.toByte(), 0x90.toByte(),
    0xDE.toByte(), 0x87.toByte(), 0x17.toByte(), 0xCF.toByte(),
    0x4F.toByte(), 0xB3.toByte(), 0xD8.toByte(), 0xD3.toByte(),
    0x01.toByte(), 0x79.toByte(), 0x6B.toByte(), 0x59.toByte()
)

// Key derivation for PREMO-Auth header:
// 1. xor_pkey = pkey XOR skey0
fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
    return ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
}

val xorPkey = xorBytes(pkey, skey0)   // AES key
val xorNonce = xorBytes(nonce, skey2) // AES IV

// 2. Plaintext = PSP MAC (6 bytes) + 10 zero bytes
val plaintext = ByteArray(16)
System.arraycopy(pspMac, 0, plaintext, 0, 6)

// 3. Encrypt to get auth token
val authToken = encryptAes128Cbc(plaintext, xorPkey, xorNonce)
val premoAuth = Base64.encodeToString(authToken, Base64.NO_WRAP)
```

### Streaming Decryption for Video/Audio Packets

For the PREMO video/audio stream, you decrypt individual packets (not a continuous stream). Each encrypted packet from the PREMO stream is a standalone AES-CBC block:

```kotlin
// Use NoPadding because PREMO packets are already block-aligned (16-byte multiples)
// The IV for stream decryption is xorNonce (same as auth derivation)
// The key is xorPkey (same as auth derivation)
fun decryptStreamPacket(payload: ByteArray, xorPkey: ByteArray, xorNonce: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(xorPkey, "AES"), IvParameterSpec(xorNonce))
    return cipher.doFinal(payload)
}
```

### When to Decrypt (from protocol analysis)

Not every packet is encrypted. Check the stream packet header:
- **Video**: decrypt when `magic[1]` is `0xFF` or `0xFE` (H.264 keyframes), or when `magic[1]` is `0xFB` and `unk6 == 0x0001` or `unk6 == 0x0401`
- **Audio**: decrypt when `unk8` field is set

### Sources
- [Cipher API Reference (Android)](https://developer.android.com/reference/kotlin/javax/crypto/Cipher)
- [AES128 CBC mode in Java, Kotlin and Ruby (Gist)](https://gist.github.com/kobeumut/17932fd08b5b6dd7ee153a85865f4c54)
- [Kotlin AES Encryption and Decryption (Baeldung)](https://www.baeldung.com/kotlin/advanced-encryption-standard)
- [Kotlin AES Encryption and Decryption (Gist)](https://gist.github.com/flymrc/31f0efe6cf927ff8d8e79d4c9ca9eb5c)
- [AES Encryption in Android (Medium)](https://medium.com/@appdevinsights/implementation-of-aes-encryption-in-android-dca250525b4)

---

## 3. HTTP Long-Lived Connection on Android

### The Problem

PREMO uses long-lived HTTP GET responses for video and audio. The PS3 responds to `GET /sce/premo/session/video` with an HTTP response that never ends -- it keeps streaming bytes indefinitely. You need to read bytes as they arrive, not wait for the response to "complete."

### Simplest Approach: OkHttp + `response.body.byteStream()`

OkHttp is the standard HTTP client for Android. The key is to use `response.body!!.byteStream()` which gives you a `java.io.InputStream` you can read from indefinitely.

### Minimal Pattern (Kotlin)

```kotlin
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

val client = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)    // No read timeout -- stream runs forever
    .connectTimeout(10, TimeUnit.SECONDS)
    .build()

fun startVideoStream(ps3Ip: String, sessionId: String) {
    // Run on IO thread
    val request = Request.Builder()
        .url("http://$ps3Ip:9293/sce/premo/session/video")
        .header("User-Agent", "premo/1.0.0 libhttp/1.0.0")
        .header("SessionID", sessionId)
        .header("PREMO-Auth", authToken)
        // ... other PREMO headers
        .build()

    val response = client.newCall(request).execute()
    val stream: InputStream = response.body!!.byteStream()

    // Read 32-byte packet headers, then payload
    val headerBuf = ByteArray(32)
    while (running) {
        // Read exactly 32 bytes for the stream packet header
        stream.readNBytes(headerBuf, 0, 32)

        // Parse the header (big-endian)
        val payloadLen = ((headerBuf[16].toInt() and 0xFF) shl 8) or
                         (headerBuf[17].toInt() and 0xFF)

        // Read the payload
        val payload = ByteArray(payloadLen)
        stream.readNBytes(payload, 0, payloadLen)

        // Decrypt if needed, then feed to MediaCodec
        processPacket(headerBuf, payload)
    }
    response.close()
}

// Helper to read exactly N bytes (InputStream.read may return fewer)
fun InputStream.readNBytes(buf: ByteArray, off: Int, len: Int) {
    var remaining = len
    var offset = off
    while (remaining > 0) {
        val read = this.read(buf, offset, remaining)
        if (read == -1) throw java.io.EOFException("Stream ended")
        offset += read
        remaining -= read
    }
}
```

### Important Notes

1. **`readTimeout(0)`**: Critical. Without this, OkHttp will kill the connection after 10 seconds of no data (default timeout). Setting to 0 disables the read timeout entirely.

2. **Buffering**: OkHttp does buffer internally (~8KB by default via Okio). For a POC this is fine. The buffer means you might get data in slightly larger chunks, but the bytes arrive in order and `read()` will block until data is available. If you need lower latency, you can access `response.body!!.source()` (Okio `BufferedSource`) for more control.

3. **Thread**: The `execute()` call and the read loop MUST run on a background thread. Use `Dispatchers.IO` or a dedicated thread.

4. **Alternative -- raw sockets**: For absolute minimum latency, you could skip OkHttp and use `java.net.Socket` directly, manually writing the HTTP GET request and reading the response. But for a POC, OkHttp is simpler.

5. **Multiple streams**: You need separate long-lived connections for video (`/sce/premo/session/video`) and audio (`/sce/premo/session/audio`). Each runs on its own thread.

### Sources
- [OkHttp Recipes](https://square.github.io/okhttp/recipes/)
- [ResponseBody (OkHttp 3.14.0)](https://square.github.io/okhttp/3.x/okhttp/okhttp3/ResponseBody.html)
- [OkHttp streaming issue #7263](https://github.com/square/okhttp/issues/7263)
- [OkHttp response body without copying buffer #2869](https://github.com/square/okhttp/issues/2869)
- [Using OkHttp (CodePath)](https://guides.codepath.org/android/Using-OkHttp)
- [OkHttp Overview](https://square.github.io/okhttp/)

---

## 4. PS3 Remote Play Key Extraction Without a PSP

### What Keys Are Needed

The PREMO protocol requires these values to authenticate (from `orp-conf.h`):

| Value | Size | Description |
|-------|------|-------------|
| `pkey` | 16 bytes | Private key -- generated during PSP-to-PS3 registration |
| `psp_id` | 16 bytes | OpenPSID -- hardware-bound identifier of the registered device |
| `psp_mac` | 6 bytes | MAC address of the registered device |
| `skey0` | 16 bytes | Session key 0 -- **hardcoded constant** (same for all PS3s) |
| `skey1` | 16 bytes | Session key 1 -- **hardcoded constant** |
| `skey2` | 16 bytes | Session key 2 -- **hardcoded constant** |

The skeys are hardcoded in the Open-RP source and are the same for every PS3. The challenge is getting `pkey`, `psp_id`, and `psp_mac`.

### How Open-RP Originally Got the Keys

The Open-RP project included a PSP homebrew app (`psp/orp.c`) that ran on a hacked PSP. It:
1. Registered the PSP with the PS3 normally (via XMB > Settings > Remote Play Settings > Register Device)
2. Then ran the homebrew app which read the keys from the PSP's system registry at `/CONFIG/PREMO`:
   - `ps3_name` -- PS3 nickname
   - `ps3_mac` -- PS3 MAC address
   - `ps3_key` -- the `pkey` (private key)
   - The PSP's OpenPSID via `sceOpenPSIDGetOpenPSID()`
   - The PSP's MAC via `sceWlanGetEtherAddr()`
3. Exported everything to `ms0:/export.orp`

### Options Without a Hacked PSP

#### Option A: PS3 CFW -- Read from PS3 filesystem (MOST PROMISING)

On a PS3 with Custom Firmware (CFW) or HEN, the registration data is stored on the PS3 side too. The PS3 stores paired device information that includes the pkey and device identifiers.

**Where to look on CFW PS3:**
- The PREMO plugin (`premo_plugin.sprx`) handles registration
- Registration data is stored in the PS3's flash/registry system
- webMAN-MOD can access the PS3's filesystem via FTP or its web interface
- Tools like `erk_dumper` (by flatz, integrated in Rebug Toolbox) can dump per-console keys

**Practical approach:**
1. Install CFW or HEN on the PS3
2. Register ANY device (a real PSP, a PS Vita, or even the VAIO Remote Play client)
3. Use FTP access to browse `/dev_flash2/` and `/dev_hdd0/` for PREMO registration data
4. Or: write a PS3 homebrew that reads the registration info from the PS3's registry (mirror of what the PSP homebrew does, but on the PS3 side)

#### Option B: Use VAIO Remote Play to Register, Then Extract Keys

1. Install the patched VAIO Remote Play client on a Windows PC (using VRPPatch from `ps3-pro-remote-play/`)
2. Register the PC with the PS3 via the VAIO client
3. The VAIO client stores keys in the Windows registry or in its application data
4. Extract the keys from there
5. Use these keys in your Android app (substitute the VAIO's "device ID" as the psp_id)

#### Option C: Fabricate Keys by Emulating Registration (HARDEST)

The registration protocol between PSP and PS3 has been partially reverse-engineered. In theory you could:
1. Emulate the PSP side of the registration handshake
2. Generate your own `psp_id` (or use a random one)
3. Complete registration, receiving a `pkey` from the PS3

This is the hardest approach and would require deep understanding of the registration handshake, which is not fully documented in Open-RP.

#### Option D: PS Vita (OFW) + CFW PS3

A stock (non-hacked) PS Vita can register with a CFW PS3 for Remote Play over local network. After registration:
1. The Vita stores the keys internally
2. A hacked Vita could export them (similar to the PSP approach)
3. Or extract from the PS3 side as described in Option A

### Recommendation for POC

**Option A (PS3 CFW filesystem extraction) or Option B (VAIO client extraction)** are the most practical. For a first POC, you can hardcode the extracted keys. A proper registration flow can come later.

### Sources
- [PS3 Developer Wiki -- Remote Play](https://www.psdevwiki.com/ps3/Remote_Play)
- [PS3 Developer Wiki -- Per Console Keys](https://www.psdevwiki.com/ps3/Per_Console_Keys)
- [ConsoleMods Wiki -- Forcing Remote Play](https://consolemods.org/wiki/PS3:Forcing_Remote_Play)
- [GBAtemp -- Remote Play with CFW](https://gbatemp.net/threads/remote-play-with-cfw.210247/)
- [wololo.net -- Remote Play with OFW Vita and CFW PS3](https://wololo.net/talk/viewtopic.php?t=35564)
- [PlayStation Manual -- Register Device](https://manuals.playstation.net/document/en/ps3/current/settings/registerdevice.html)
- Open-RP source: `psp/orp.c` (PSP key exporter), `keys/keys.cpp` (hardcoded skeys), `orp-conf.h` (config format)

---

## 5. Android Bluetooth/USB Controller Input

### How Android Handles Controllers

Android has built-in gamepad support since API 12 (Android 3.1). Controllers appear as `InputDevice` objects and generate standard `KeyEvent` and `MotionEvent` callbacks. No special library needed.

### DualSense (PS5) Support

- Works natively via Bluetooth on Android 12+ (API 31+)
- Pairs like any Bluetooth device (hold Create + PS button for pairing mode)
- Recognized as a standard gamepad -- all buttons map to standard Android keycodes
- Touchpad and advanced haptics do NOT work on Android
- On Android 10-11, connects but may have incomplete button mapping

### Key Classes

- `android.view.InputDevice` -- represents a connected controller
- `android.view.KeyEvent` -- button presses (digital)
- `android.view.MotionEvent` -- analog stick/trigger movement
- `InputDevice.SOURCE_GAMEPAD` -- source flag for gamepad buttons
- `InputDevice.SOURCE_JOYSTICK` -- source flag for analog sticks

### Minimal Pattern (Kotlin, in your Activity)

```kotlin
// Detect connected gamepads
fun getGameControllers(): List<InputDevice> {
    return InputDevice.getDeviceIds()
        .mapNotNull { InputDevice.getDevice(it) }
        .filter { it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD }
}

// Handle button presses
override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A     -> sendPadButton(ORP_PAD_PSP_X)       // Cross
            KeyEvent.KEYCODE_BUTTON_B     -> sendPadButton(ORP_PAD_PSP_CIRCLE)  // Circle
            KeyEvent.KEYCODE_BUTTON_X     -> sendPadButton(ORP_PAD_PSP_SQUARE)  // Square
            KeyEvent.KEYCODE_BUTTON_Y     -> sendPadButton(ORP_PAD_PSP_TRI)     // Triangle
            KeyEvent.KEYCODE_BUTTON_L1    -> sendPadButton(ORP_PAD_PSP_L1)
            KeyEvent.KEYCODE_BUTTON_R1    -> sendPadButton(ORP_PAD_PSP_R1)
            KeyEvent.KEYCODE_BUTTON_L2    -> sendPadButton(ORP_PAD_PSP_L2)
            KeyEvent.KEYCODE_BUTTON_R2    -> sendPadButton(ORP_PAD_PSP_R2)
            KeyEvent.KEYCODE_BUTTON_START -> sendPadButton(ORP_PAD_PSP_START)
            KeyEvent.KEYCODE_BUTTON_SELECT-> sendPadButton(ORP_PAD_PSP_SELECT)
            KeyEvent.KEYCODE_DPAD_UP      -> sendPadButton(ORP_PAD_PSP_DPUP)
            KeyEvent.KEYCODE_DPAD_DOWN    -> sendPadButton(ORP_PAD_PSP_DPDOWN)
            KeyEvent.KEYCODE_DPAD_LEFT    -> sendPadButton(ORP_PAD_PSP_DPLEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT   -> sendPadButton(ORP_PAD_PSP_DPRIGHT)
            KeyEvent.KEYCODE_BUTTON_THUMBL-> sendPadButton(ORP_PAD_PSP_L3)
            KeyEvent.KEYCODE_BUTTON_THUMBR-> sendPadButton(ORP_PAD_PSP_R3)
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }
    return super.onKeyDown(keyCode, event)
}

override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    // Same structure, send button-up event
    if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
        sendPadButtonUp(keyCode)
        return true
    }
    return super.onKeyUp(keyCode, event)
}

// Handle analog sticks
override fun onGenericMotionEvent(event: MotionEvent): Boolean {
    if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
        // Left stick
        val lx = event.getAxisValue(MotionEvent.AXIS_X)     // -1.0 to 1.0
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)     // -1.0 to 1.0
        // Right stick
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)     // -1.0 to 1.0
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)    // -1.0 to 1.0

        // Convert to 0-255 range for PREMO pad state
        sendAnalogStick(
            lx = ((lx + 1f) * 127.5f).toInt().coerceIn(0, 255),
            ly = ((ly + 1f) * 127.5f).toInt().coerceIn(0, 255),
            rx = ((rx + 1f) * 127.5f).toInt().coerceIn(0, 255),
            ry = ((ry + 1f) * 127.5f).toInt().coerceIn(0, 255)
        )
        return true
    }
    return super.onGenericMotionEvent(event)
}
```

### USB Controllers

USB controllers (wired) work identically. Android treats them the same as Bluetooth controllers -- same `KeyEvent`/`MotionEvent` callbacks, same keycodes. Plug in via USB-C/OTG and it just works.

### Compatible Controllers (tested on Android)

| Controller | Connection | Android Support |
|-----------|-----------|----------------|
| PS5 DualSense | Bluetooth/USB | Android 12+ native |
| PS4 DualShock 4 | Bluetooth/USB | Android 10+ native |
| Xbox Series | Bluetooth/USB | Android 10+ native |
| Xbox One | Bluetooth/USB | Android 9+ |
| Generic USB/BT gamepads | USB/Bluetooth | Android 3.1+ (HID) |
| 8BitDo controllers | Bluetooth/USB | Excellent support |

### Sources
- [Handle controller actions (Android Developers)](https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input)
- [Handle controller actions (Games SDK)](https://developer.android.com/games/sdk/game-controller/controller-input)
- [DualSense on Android (How-To Geek)](https://www.howtogeek.com/737593/how-to-connect-a-ps5-controller-to-an-android-phone/)
- [DualSense pairing guide (PlayStation)](https://www.playstation.com/en-us/support/hardware/pair-dualsense-controller-bluetooth/)
- [Moonlight Android -- ControllerHandler.java](https://github.com/moonlight-stream/moonlight-android/blob/master/app/src/main/java/com/limelight/binding/input/ControllerHandler.java) (excellent real-world reference)
- [DualSense technical info (GitHub)](https://github.com/nondebug/dualsense)

---

## 6. UDP Broadcast Discovery on Android

### The PREMO Discovery Protocol

The PS3 discovery is dead simple:
1. Send `SRCH` (4 bytes: `0x53 0x52 0x43 0x48`) as a UDP broadcast to port 9293
2. PS3 responds with a 156-byte `RESP` packet containing its MAC, nickname, and NPX ID

### Key Classes

- `java.net.DatagramSocket` -- sends/receives UDP packets
- `java.net.DatagramPacket` -- wraps a byte buffer for UDP
- `java.net.InetAddress` -- for the broadcast address

### Minimal Pattern (Kotlin)

```kotlin
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class Ps3Info(
    val ip: String,
    val mac: ByteArray,
    val nickname: String,
    val npxId: ByteArray
)

// Must run on background thread (network on main thread = NetworkOnMainThreadException)
fun discoverPs3(timeoutMs: Int = 3000): Ps3Info? {
    val socket = DatagramSocket()
    socket.broadcast = true
    socket.soTimeout = timeoutMs

    try {
        // Send SRCH broadcast
        val srch = "SRCH".toByteArray(Charsets.US_ASCII)
        val broadcastAddr = InetAddress.getByName("255.255.255.255")
        val sendPacket = DatagramPacket(srch, srch.size, broadcastAddr, 9293)
        socket.send(sendPacket)

        // Receive RESP (156 bytes)
        val recvBuf = ByteArray(256)
        val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
        socket.receive(recvPacket) // blocks until timeout or data

        // Parse response
        val data = recvPacket.data
        val magic = String(data, 0, 4, Charsets.US_ASCII)
        if (magic != "RESP") return null

        val mac = data.copyOfRange(10, 16)
        val nickname = String(data, 16, 128, Charsets.US_ASCII).trimEnd('\u0000')
        val npxId = data.copyOfRange(144, 156)

        return Ps3Info(
            ip = recvPacket.address.hostAddress ?: "",
            mac = mac,
            nickname = nickname,
            npxId = npxId
        )
    } catch (e: java.net.SocketTimeoutException) {
        return null // No PS3 found
    } finally {
        socket.close()
    }
}
```

### Android-Specific Gotchas

1. **WiFi must be on the same subnet**: The Android device and PS3 must be on the same LAN. UDP broadcasts do not cross subnets/VLANs.

2. **WiFi Multicast Lock**: On some Android versions, you may need a `WifiManager.MulticastLock` to receive broadcast/multicast UDP:
   ```kotlin
   val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
   val multicastLock = wifiManager.createMulticastLock("ps3discovery")
   multicastLock.setReferenceCounted(true)
   multicastLock.acquire()
   // ... do discovery ...
   multicastLock.release()
   ```

3. **Permissions needed** in `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
   <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
   ```

4. **Background restrictions**: UDP broadcast reception does not work reliably when the app is in the background due to Android's battery optimizations. Fine for a POC where the app is in the foreground.

5. **Directed broadcast alternative**: If `255.255.255.255` does not work on some devices, compute the subnet broadcast address:
   ```kotlin
   fun getBroadcastAddress(context: Context): InetAddress {
       val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
       val dhcp = wifi.dhcpInfo
       val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
       val quads = ByteArray(4)
       for (k in 0..3) quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
       return InetAddress.getByAddress(quads)
   }
   ```

### Sources
- [DatagramSocket API (Android)](https://developer.android.com/reference/kotlin/java/net/DatagramSocket)
- [Android UDP Client Example](https://msdalp.github.io/2014/03/09/Udp-on-Android/)
- [Android UDP Broadcast Listener Service (Gist)](https://gist.github.com/finnjohnsen/3654994)
- [Kotlin UDP discussion](https://discuss.kotlinlang.org/t/how-to-send-a-short-string-using-udp/16689)
- [Android Datagram/UDP Server Example](http://android-er.blogspot.com/2016/06/android-datagramudp-server-example.html)

---

## Summary: Minimal POC Architecture

```
[PS3 with CFW]
     |
     |  1. UDP broadcast "SRCH" --> PS3 responds with "RESP" (discovery)
     |  2. HTTP GET /sce/premo/session (handshake + auth headers)
     |  3. HTTP GET /sce/premo/session/video (long-lived, streamed response)
     |  4. HTTP POST /sce/premo/session/pad (controller input, periodic)
     v
[Android App]
     |
     +-- DiscoveryManager    --> DatagramSocket, send "SRCH", parse "RESP"
     +-- SessionManager      --> OkHttp, session handshake, PREMO-Auth header
     +-- VideoStreamReader   --> OkHttp streaming GET, read 32-byte headers + payload
     +-- StreamDecryptor     --> javax.crypto.Cipher (AES-128-CBC), decrypt flagged packets
     +-- VideoDecoder        --> MediaCodec("video/avc") + SurfaceView, feed NAL units
     +-- ControllerInput     --> onKeyDown/onKeyUp/onGenericMotionEvent -> POST /pad
```

### Dependencies for POC

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Everything else is Android SDK built-in:
    // - MediaCodec (android.media)
    // - SurfaceView (android.view)
    // - Cipher (javax.crypto)
    // - DatagramSocket (java.net)
    // - InputDevice/KeyEvent/MotionEvent (android.view)
}
```

Only ONE external dependency (OkHttp) needed. Everything else is built into Android.
