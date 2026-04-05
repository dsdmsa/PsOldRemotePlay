// screencap.swift — macOS ScreenCaptureKit + VideoToolbox screen capture → H.264 Annex-B → UDP
//
// Compile:
//   swiftc -O -o screencap screencap.swift \
//     -framework ScreenCaptureKit -framework VideoToolbox \
//     -framework CoreMedia -framework CoreVideo -framework CoreGraphics
//
// Usage:
//   ./screencap --target 192.168.1.70 --port 9296 --width 640 --height 448 --fps 30 --bitrate 8000000

import Foundation
import ScreenCaptureKit
import VideoToolbox
import CoreMedia
import CoreVideo
import CoreGraphics

// MARK: - Configuration

struct CaptureConfig {
    var targetHost = "127.0.0.1"
    var targetPort: UInt16 = 9296
    var width = 640
    var height = 448
    var fps = 30
    var bitrate = 8_000_000
    var keyframeInterval = 30
    var displayIndex = 0
    var appFilter = ""  // capture only this app's windows (substring match on name)
}

func parseArgs() -> CaptureConfig {
    var c = CaptureConfig()
    let args = CommandLine.arguments
    var i = 1
    while i < args.count {
        switch args[i] {
        case "--target":  i += 1; c.targetHost = args[i]
        case "--port":    i += 1; c.targetPort = UInt16(args[i]) ?? 9296
        case "--width":   i += 1; c.width = Int(args[i]) ?? 640
        case "--height":  i += 1; c.height = Int(args[i]) ?? 448
        case "--fps":     i += 1; c.fps = Int(args[i]) ?? 30
        case "--bitrate": i += 1; c.bitrate = Int(args[i]) ?? 8_000_000
        case "--keyframe": i += 1; c.keyframeInterval = Int(args[i]) ?? 30
        case "--display": i += 1; c.displayIndex = Int(args[i]) ?? 0
        case "--app":    i += 1; c.appFilter = args[i]
        default: break
        }
        i += 1
    }
    return c
}

// MARK: - Logging

func log(_ msg: String) {
    let fmt = DateFormatter()
    fmt.dateFormat = "HH:mm:ss.SSS"
    let ts = fmt.string(from: Date())
    FileHandle.standardError.write("[\(ts)][SCREENCAP] \(msg)\n".data(using: .utf8)!)
}

// MARK: - UDP Sender (POSIX socket for minimal overhead)

class UDPSender {
    private let sock: Int32
    private var addr: sockaddr_in

    init(host: String, port: UInt16) {
        sock = socket(AF_INET, SOCK_DGRAM, 0)
        var bufSize: Int32 = 2 * 1024 * 1024
        setsockopt(sock, SOL_SOCKET, SO_SNDBUF, &bufSize, socklen_t(MemoryLayout<Int32>.size))
        addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = port.bigEndian
        inet_pton(AF_INET, host, &addr.sin_addr)
    }

    func send(_ data: Data) {
        // Chunk to stay under WiFi MTU — large UDP datagrams get IP-fragmented
        // and WiFi networks commonly drop fragments, causing total packet loss.
        let maxChunk = 1400
        if data.count <= maxChunk {
            sendRaw(data)
        } else {
            var offset = 0
            while offset < data.count {
                let end = min(offset + maxChunk, data.count)
                sendRaw(data[offset..<end])
                offset = end
            }
        }
    }

    private func sendRaw(_ data: Data) {
        data.withUnsafeBytes { buf in
            withUnsafePointer(to: &addr) { addrPtr in
                addrPtr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                    _ = sendto(sock, buf.baseAddress, data.count, 0, sa, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
        }
    }

    deinit { close(sock) }
}

// MARK: - H.264 Encoder (VideoToolbox hardware)

class H264Encoder {
    private var session: VTCompressionSession?
    private let sender: UDPSender
    private let startCode = Data([0x00, 0x00, 0x00, 0x01])
    private var frameCount: Int64 = 0
    private var lastLogTime = Date()
    private var framesInInterval: Int64 = 0

    init(config: CaptureConfig, sender: UDPSender) {
        self.sender = sender

        let encoderSpec: [CFString: Any] = [
            kVTVideoEncoderSpecification_EnableHardwareAcceleratedVideoEncoder: true
        ]

        var session: VTCompressionSession?
        let callback: VTCompressionOutputCallback = { refcon, _, status, _, sampleBuffer in
            guard status == noErr, let sb = sampleBuffer, let refcon = refcon else { return }
            let encoder = Unmanaged<H264Encoder>.fromOpaque(refcon).takeUnretainedValue()
            encoder.onEncoded(sb)
        }

        let refcon = Unmanaged.passUnretained(self).toOpaque()
        let status = VTCompressionSessionCreate(
            allocator: nil,
            width: Int32(config.width),
            height: Int32(config.height),
            codecType: kCMVideoCodecType_H264,
            encoderSpecification: encoderSpec as CFDictionary,
            imageBufferAttributes: nil,
            compressedDataAllocator: nil,
            outputCallback: callback,
            refcon: refcon,
            compressionSessionOut: &session
        )

        guard status == noErr, let s = session else {
            log("Failed to create VTCompressionSession: \(status)")
            return
        }
        self.session = s

        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_RealTime, value: kCFBooleanTrue)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_ProfileLevel,
                             value: kVTProfileLevel_H264_Baseline_AutoLevel)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_AverageBitRate,
                             value: config.bitrate as CFNumber)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_MaxKeyFrameInterval,
                             value: config.keyframeInterval as CFNumber)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_ExpectedFrameRate,
                             value: config.fps as CFNumber)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_AllowFrameReordering,
                             value: kCFBooleanFalse)
        // Zero-frame encoder delay for minimum latency
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_MaxFrameDelayCount,
                             value: 0 as CFNumber)

        // Bitrate limit: 120% of target, measured over 1 second
        let maxBytes = Double(config.bitrate) * 1.2 / 8.0
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_DataRateLimits,
                             value: [maxBytes, 1.0] as CFArray)

        VTCompressionSessionPrepareToEncodeFrames(s)
        log("VideoToolbox H.264 encoder ready (HW accelerated, \(config.width)x\(config.height))")
    }

    func encode(pixelBuffer: CVPixelBuffer, timestamp: CMTime) {
        guard let session = session else { return }
        VTCompressionSessionEncodeFrame(
            session, imageBuffer: pixelBuffer,
            presentationTimeStamp: timestamp, duration: .invalid,
            frameProperties: nil, sourceFrameRefcon: nil, infoFlagsOut: nil
        )
    }

    private func onEncoded(_ sampleBuffer: CMSampleBuffer) {
        let isKeyframe = checkKeyframe(sampleBuffer)
        var packet = Data()

        // Prepend SPS + PPS before every keyframe
        if isKeyframe, let fmt = CMSampleBufferGetFormatDescription(sampleBuffer) {
            appendParameterSets(from: fmt, to: &packet)
        }

        // Convert AVCC → Annex-B
        guard let dataBuffer = CMSampleBufferGetDataBuffer(sampleBuffer) else { return }
        var totalLen = 0
        var dataPtr: UnsafeMutablePointer<Int8>?
        CMBlockBufferGetDataPointer(dataBuffer, atOffset: 0, lengthAtOffsetOut: nil,
                                    totalLengthOut: &totalLen, dataPointerOut: &dataPtr)
        guard let ptr = dataPtr else { return }

        var offset = 0
        while offset < totalLen - 4 {
            var nalLen: UInt32 = 0
            memcpy(&nalLen, ptr + offset, 4)
            nalLen = nalLen.bigEndian
            offset += 4
            guard nalLen > 0, offset + Int(nalLen) <= totalLen else { break }
            packet.append(startCode)
            packet.append(Data(bytes: ptr + offset, count: Int(nalLen)))
            offset += Int(nalLen)
        }

        sender.send(packet)
        updateStats()
    }

    private func checkKeyframe(_ sb: CMSampleBuffer) -> Bool {
        guard let attachments = CMSampleBufferGetSampleAttachmentsArray(sb, createIfNecessary: false)
                as? [[CFString: Any]], let first = attachments.first else {
            return true
        }
        return !(first[kCMSampleAttachmentKey_NotSync] as? Bool ?? false)
    }

    private func appendParameterSets(from fmt: CMFormatDescription, to packet: inout Data) {
        var count = 0
        CMVideoFormatDescriptionGetH264ParameterSetAtIndex(
            fmt, parameterSetIndex: 0, parameterSetPointerOut: nil,
            parameterSetSizeOut: nil, parameterSetCountOut: &count, nalUnitHeaderLengthOut: nil
        )
        for i in 0..<count {
            var ptr: UnsafePointer<UInt8>?
            var size = 0
            let s = CMVideoFormatDescriptionGetH264ParameterSetAtIndex(
                fmt, parameterSetIndex: i, parameterSetPointerOut: &ptr,
                parameterSetSizeOut: &size, parameterSetCountOut: nil, nalUnitHeaderLengthOut: nil
            )
            if s == noErr, let ptr = ptr {
                packet.append(startCode)
                packet.append(ptr, count: size)
            }
        }
    }

    private func updateStats() {
        frameCount += 1
        framesInInterval += 1
        let now = Date()
        let elapsed = now.timeIntervalSince(lastLogTime)
        if elapsed >= 5.0 {
            let fps = Double(framesInInterval) / elapsed
            log("Encoded \(frameCount) frames (%.1f fps)".replacingOccurrences(
                of: "%.1f", with: String(format: "%.1f", fps)))
            framesInInterval = 0
            lastLogTime = now
        }
    }

    func stop() {
        guard let s = session else { return }
        VTCompressionSessionCompleteFrames(s, untilPresentationTimeStamp: .invalid)
        VTCompressionSessionInvalidate(s)
        session = nil
        log("Encoder stopped after \(frameCount) frames")
    }
}

// MARK: - Screen Capture (ScreenCaptureKit)

@available(macOS 12.3, *)
class ScreenCapture: NSObject, SCStreamOutput, SCStreamDelegate {
    private var stream: SCStream?
    private let encoder: H264Encoder
    private let config: CaptureConfig

    init(config: CaptureConfig, encoder: H264Encoder) {
        self.config = config
        self.encoder = encoder
    }

    func start() async throws {
        let content = try await SCShareableContent.current
        guard !content.displays.isEmpty else {
            log("No displays found")
            exit(1)
        }
        let idx = min(config.displayIndex, content.displays.count - 1)
        let display = content.displays[idx]

        // Always use full-display capture — app-specific filters fail silently
        // with GPU-rendered apps (OpenGL/Vulkan/Metal) like PCSX2
        if !config.appFilter.isEmpty {
            let matchingApps = content.applications.filter {
                $0.applicationName.localizedCaseInsensitiveContains(config.appFilter)
            }
            if let app = matchingApps.first {
                log("Target app found: \(app.applicationName) (PID \(app.processID))")
            } else {
                let names = content.applications.prefix(20).map { $0.applicationName }
                log("App '\(config.appFilter)' not found. Available: \(names.joined(separator: ", "))")
            }
        }
        log("Capturing full display \(display.displayID) (\(display.width)x\(display.height))")
        let filter = SCContentFilter(display: display, excludingApplications: [], exceptingWindows: [])

        let sc = SCStreamConfiguration()
        sc.width = config.width
        sc.height = config.height
        sc.minimumFrameInterval = CMTime(value: 1, timescale: CMTimeScale(config.fps))
        sc.pixelFormat = kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange // NV12
        sc.showsCursor = false
        sc.queueDepth = 3

        stream = SCStream(filter: filter, configuration: sc, delegate: self)
        try stream!.addStreamOutput(self, type: .screen,
                                     sampleHandlerQueue: DispatchQueue(label: "cap", qos: .userInteractive))
        try await stream!.startCapture()
        log("Capture started → \(config.targetHost):\(config.targetPort)")
    }

    func stream(_ stream: SCStream, didOutputSampleBuffer sb: CMSampleBuffer, of type: SCStreamOutputType) {
        guard type == .screen, let pb = CMSampleBufferGetImageBuffer(sb) else { return }
        encoder.encode(pixelBuffer: pb, timestamp: CMSampleBufferGetPresentationTimeStamp(sb))
    }

    func stream(_ stream: SCStream, didStopWithError error: Error) {
        log("Stream stopped with error: \(error)")
    }

    func stop() async {
        try? await stream?.stopCapture()
        stream = nil
        encoder.stop()
    }
}

// MARK: - Main

let config = parseArgs()
log("Starting: \(config.width)x\(config.height)@\(config.fps)fps → \(config.targetHost):\(config.targetPort) bitrate=\(config.bitrate/1000)kbps")

let sender = UDPSender(host: config.targetHost, port: config.targetPort)
let encoder = H264Encoder(config: config, sender: sender)

signal(SIGINT)  { _ in log("SIGINT");  exit(0) }
signal(SIGTERM) { _ in log("SIGTERM"); exit(0) }

guard #available(macOS 12.3, *) else {
    log("macOS 12.3+ required for ScreenCaptureKit")
    exit(1)
}

let capture = ScreenCapture(config: config, encoder: encoder)

Task {
    do {
        try await capture.start()
    } catch {
        log("Capture failed: \(error)")
        exit(1)
    }
}

RunLoop.main.run()
