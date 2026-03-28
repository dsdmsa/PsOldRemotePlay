# How to Capture PSP-to-PS3 Registration Packets

Thank you so much for offering to help! A real PSP→PS3 registration capture would be incredibly valuable — it's the one thing that can unblock this entire project.

## What We Need

A **Wireshark packet capture** of the moment your PSP Go registers with the PS3 over WiFi. This capture will contain:
- The encrypted registration request (from PSP to PS3)
- The encrypted registration response (from PS3 to PSP)

Combined with the PIN shown on screen, this lets us verify/crack the encryption formula.

---

## What You'll Need

- Your **PSP Go** (any PSP model works, but Go has built-in WiFi)
- Your **PS3** (any model, stock firmware is fine)
- A **laptop** (Windows, Mac, or Linux) with WiFi that supports **monitor mode** — this is the tricky part
- **Wireshark** installed on the laptop

**Important:** The PSP and PS3 communicate over a temporary **ad-hoc WiFi network** (not your home WiFi router). Your laptop needs to eavesdrop on this ad-hoc network.

---

## Overview of What Happens

1. You tell the PS3 to "Register Device" → PS3 creates a temporary WiFi network and shows an 8-digit PIN
2. On the PSP, you enter the PS3's network name and connect
3. The PSP sends an encrypted registration request to the PS3 over this WiFi
4. The PS3 responds with the shared key
5. Your laptop captures all of this with Wireshark

---

## Step-by-Step Instructions

### Step 1: Install Wireshark

**Windows:** Download from https://www.wireshark.org/download.html — install with default options, including **Npcap** (check "Install Npcap" during setup)

**Mac:** `brew install --cask wireshark` or download from the website

**Linux:** `sudo apt install wireshark` (Ubuntu/Debian) or `sudo dnf install wireshark` (Fedora)

---

### Step 2: Prepare the PS3

1. On the PS3, go to: **Settings → Remote Play Settings → Register Device**
2. Select **"PSP"** (since you have a PSP Go)
3. The PS3 will show:
   - A WiFi network name like **`PS3REGIST_XXXX`** (4-digit number)
   - An **8-digit PIN** (e.g., `46823571`)
4. **Write down BOTH the network name and PIN** — we need these!
5. Leave this screen open — don't press anything else on the PS3

---

### Step 3: Start Wireshark Capture

This is the most important part. You need to capture the ad-hoc WiFi traffic.

#### Option A: Linux (Easiest — Recommended)

Linux has the best support for WiFi monitor mode.

```bash
# 1. Find your WiFi interface name
ip link show
# Look for something like wlan0, wlp2s0, etc.

# 2. Put WiFi into monitor mode
sudo ip link set wlan0 down
sudo iw dev wlan0 set type monitor
sudo ip link set wlan0 up

# 3. Find which WiFi channel the PS3 is using
# The PS3 ad-hoc network typically uses channel 1 or 6
sudo iw dev wlan0 set channel 1

# 4. Start Wireshark
sudo wireshark &
# Select your WiFi interface (wlan0) and click Start (the shark fin button)

# 5. In the filter bar at the top, type:
#    tcp.port == 9293
# This filters to only show the registration traffic
```

**Alternative (no monitor mode needed):** If your Linux laptop can JOIN the ad-hoc network, that's even easier:

```bash
# Create a wpa_supplicant config to join the PS3's ad-hoc network
# Replace SSID and PASSWORD below with your values
#
# PASSWORD = PIN with halves swapped!
# Example: PIN 46823571 → password is 35714682

sudo wpa_supplicant -i wlan0 -D nl80211 -c <(cat <<EOF
network={
    ssid="PS3REGIST_XXXX"
    mode=1
    key_mgmt=WPA-NONE
    pairwise=NONE
    group=TKIP
    psk="XXXXXXXX"
    frequency=2412
}
EOF
) &

# Get an IP address
sudo dhclient wlan0
# or manually:
sudo ip addr add 192.168.1.100/24 dev wlan0

# Start Wireshark and capture on wlan0
sudo wireshark &
```

#### Option B: Windows

Windows can't easily do WiFi monitor mode, but there's another way:

**Method 1: Use the PS3's wired network instead**

The PS3 also listens on port 9293 on its **wired Ethernet** during registration! If both the PSP and your PC are on the same network:

1. Connect your PC to the same router as the PS3 (via Ethernet or WiFi)
2. Open Wireshark, select your network interface
3. Set capture filter: `host <PS3_IP> and port 9293`
   (replace `<PS3_IP>` with your PS3's wired IP, e.g., 192.168.1.75)
4. Click Start

**But wait** — the PSP connects via the ad-hoc WiFi, and the traffic goes over WiFi, not through the router. So this only works if the PS3 bridges the traffic to its wired interface (it does for port 9293!).

Actually, the safer bet on Windows is:

**Method 2: Use a USB WiFi adapter that supports monitor mode**

- Adapters with Ralink RT3070 or Atheros AR9271 chipset work well
- Install the adapter, then use Wireshark → select the adapter → check "Monitor Mode" in capture options
- Set to channel 1 (or whichever channel the PS3 uses)

#### Option C: Mac

Recent Macs can't join ad-hoc networks easily, but the built-in WiFi can sniff:

1. Open Terminal
2. Find the channel the PS3 is on:
   ```bash
   /System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -s
   ```
   Look for the PS3REGIST network and note the channel

3. Start a capture on that channel:
   ```bash
   sudo /System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport en0 sniff <channel>
   ```
   This creates a `.pcap` file in `/tmp/`

4. When done (after the PSP registers), press Ctrl+C
5. Open the pcap file in Wireshark:
   ```bash
   open /tmp/airportSniff*.cap
   ```

**Note:** Since the ad-hoc network uses WPA-NONE/TKIP, you may need to add the decryption key in Wireshark:
- Go to **Edit → Preferences → Protocols → IEEE 802.11 → Decryption keys → Edit**
- Add key type: **wpa-pwd**, Key: `PASSWORD:PS3REGIST_XXXX`
- (PASSWORD = PIN with halves swapped, see below)

---

### Step 4: Connect PSP and Register

With Wireshark running and capturing:

1. On your **PSP Go**, go to: **Settings → Network Settings → Ad Hoc Mode**
   - Or: **Settings → Remote Play Settings** (depends on firmware)
2. Select **"New Connection"** or **"Remote Play"**
3. It should find the PS3's network (`PS3REGIST_XXXX`)
4. Enter the **8-digit PIN** shown on the PS3 screen
5. Wait for registration to complete (should take a few seconds)
6. If successful, the PS3 will say "Registration complete"

---

### Step 5: Stop Capture and Save

1. In Wireshark, click the **red square** (Stop) button
2. Go to **File → Save As** → save as `ps3_registration.pcapng`
3. Also note down:
   - The **PIN** that was displayed (8 digits)
   - The **WiFi SSID** (e.g., `PS3REGIST_2774`)
   - The **WiFi password you used** (if any — the PSP handles this automatically)

---

### Step 6: Send Us the Capture

Please share:
1. The `.pcapng` file
2. The 8-digit PIN
3. The WiFi SSID
4. (Optional but helpful) Your PSP's MAC address (Settings → System Information on PSP)

You can upload the pcap to a file sharing service, or if you're comfortable, share it directly.

---

## Ad-Hoc Network Details (Technical)

For reference, here's exactly what the PS3 creates:

| Setting | Value |
|---------|-------|
| Network type | Ad-hoc (IBSS) |
| SSID | `PS3REGIST_XXXX` (PSP mode) or `PS3REGPC_XXXX` (PC mode) |
| Security | WPA-NONE with TKIP group cipher |
| Frequency | 2.4 GHz (usually channel 1 or 6) |
| Password | PIN with halves swapped: PIN `12345678` → password `56781234` |
| IP range | 192.168.1.x (DHCP from PS3) |
| Registration port | **TCP 9293** |
| Protocol | HTTP: `POST /sce/premo/regist HTTP/1.1` |
| Max attempts | 3 per session (then PS3 drops WiFi, restart required) |

---

## What If the PSP Handles Everything Automatically?

The PSP might connect to the PS3's ad-hoc network automatically without you entering a password. That's fine! The PSP knows how to derive the password from the PIN internally. Just make sure Wireshark is capturing before you start the registration on the PSP.

## What If Monitor Mode Doesn't Work?

If you can't get monitor mode working, there's an alternative:

1. Connect a **Linux machine** to the PS3's ad-hoc WiFi (using the wpa_supplicant method above)
2. Run `tcpdump` on that Linux machine while the PSP registers:
   ```bash
   sudo tcpdump -i wlan0 -w ps3_registration.pcap port 9293
   ```
3. The Linux machine acts as a passive listener on the same ad-hoc network

This works because ad-hoc networks are shared — all devices on the network can see each other's traffic (unlike infrastructure WiFi where the AP isolates clients).

## What If You Can Only Capture on the Wired Side?

If your PS3 is connected via Ethernet AND WiFi registration is happening, port 9293 also opens on the wired interface. You can try:

1. Use a **managed switch** with port mirroring, or
2. Run Wireshark on a machine on the same wired network with filter: `host <PS3_wired_IP> and port 9293`

The wired-side capture might not show the PSP's registration traffic (since it goes over WiFi), but it's worth trying — some PS3 firmwares bridge the registration port to both interfaces.

---

## Questions?

If anything is unclear or you run into issues, just let me know. Even a partial capture or failed attempt gives us useful information. The key data is the **raw bytes** of the POST request and response on port 9293.

Thanks again for helping! This community effort is what will make PS3 Remote Play work on modern devices.
