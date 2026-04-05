// keyinject.swift — Injects keyboard input via macOS Accessibility API (AXUIElement).
//
// Compile:
//   swiftc -O -o keyinject keyinject.swift -framework AppKit -framework CoreGraphics
//
// This uses AXUIElement to post keyboard events directly to a target application,
// bypassing focus requirements. This is the same mechanism that macOS "System Events"
// uses, and it works with Qt/SDL apps like PCSX2.
//
// Protocol (stdin lines):
//   P <pid>       — Set target PID
//   D <keycode>   — Key down (CGKeyCode)
//   U <keycode>   — Key up (CGKeyCode)
//   F             — Re-activate target app
//   Q             — Quit

import AppKit
import CoreGraphics
import Foundation

func log(_ msg: String) {
    FileHandle.standardError.write("\(msg)\n".data(using: .utf8)!)
}

guard AXIsProcessTrusted() else {
    log("ERROR: Not trusted for Accessibility. Add this app in System Settings → Privacy → Accessibility")
    exit(1)
}

var targetPid: pid_t = 0
var targetApp: NSRunningApplication?
var targetElement: AXUIElement?

func findPCSX2(hint: pid_t) {
    let apps = NSWorkspace.shared.runningApplications
    targetApp = apps.first(where: {
        let name = $0.localizedName ?? ""
        return name.contains("PCSX2") || name.contains("pcsx2")
    })

    if targetApp == nil {
        // Try hint PID and nearby (PCSX2 may fork)
        for pid in [hint, hint + 1, hint + 2, hint - 1] {
            if let app = NSRunningApplication(processIdentifier: pid),
               !app.isTerminated, app.activationPolicy == .regular {
                targetApp = app
                break
            }
        }
    }

    if let app = targetApp {
        targetPid = app.processIdentifier
        targetElement = AXUIElementCreateApplication(targetPid)
        app.activate(options: .activateIgnoringOtherApps)
        log("TARGET \(targetPid) (\(app.localizedName ?? "?"))")
    } else {
        targetPid = hint
        targetElement = AXUIElementCreateApplication(hint)
        log("TARGET \(hint) (fallback)")
    }
}

func postKey(keyCode: UInt16, down: Bool) {
    guard let event = CGEvent(keyboardEventSource: nil, virtualKey: keyCode, keyDown: down) else {
        log("WARN: Failed to create CGEvent for keyCode \(keyCode)")
        return
    }
    // Set flags to 0 to avoid modifier confusion
    event.flags = []

    // Strategy 1: postToPid — sends directly to the process
    if targetPid > 0 {
        event.postToPid(targetPid)
    }

    // Strategy 2: post to session event tap (reaches frontmost app)
    event.post(tap: .cgSessionEventTap)
}

setbuf(stdout, nil)
log("OK")

// Process commands
while let line = readLine() {
    let trimmed = line.trimmingCharacters(in: .whitespaces)
    if trimmed.isEmpty { continue }
    let parts = trimmed.split(separator: " ", maxSplits: 1)

    switch parts[0] {
    case "P":
        guard parts.count >= 2, let pid = Int32(parts[1]) else { continue }
        findPCSX2(hint: pid)

    case "D", "U":
        guard parts.count >= 2, let kc = UInt16(parts[1]) else { continue }
        // Make sure PCSX2 is active
        if let app = targetApp, !app.isActive {
            app.activate(options: .activateIgnoringOtherApps)
            usleep(5000)
        }
        postKey(keyCode: kc, down: parts[0] == "D")

    case "F":
        targetApp?.activate(options: .activateIgnoringOtherApps)

    case "Q":
        exit(0)

    default:
        break
    }
}
