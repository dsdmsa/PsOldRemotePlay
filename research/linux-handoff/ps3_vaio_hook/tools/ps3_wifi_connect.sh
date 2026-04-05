#!/bin/bash
#
# PS3 Ad-Hoc WiFi Connect Script v2
# Tries EVERY possible password combination to connect to PS3 registration WiFi
#
# Usage: sudo ./ps3_wifi_connect.sh PS3REGPC_4726 47261234
#
# The script will try multiple password formats, restore network on exit.
#

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${GREEN}[OK]${NC} $1"; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; }

LOGFILE="/tmp/ps3_connect_log.txt"
exec > >(tee -a "$LOGFILE") 2>&1
echo "" > "$LOGFILE"

# Always restore network on exit
cleanup() {
    echo ""
    info "Restoring network..."
    sudo killall wpa_supplicant 2>/dev/null
    sudo killall dhclient 2>/dev/null
    sudo ip link set "$IFACE" down 2>/dev/null
    sudo iw "$IFACE" set type managed 2>/dev/null
    sudo ip link set "$IFACE" up 2>/dev/null
    sudo systemctl start NetworkManager 2>/dev/null
    log "Network restored. Log saved to $LOGFILE"
}
trap cleanup EXIT

# ── Input ──
SSID="${1:-}"
PIN="${2:-}"

if [ -z "$SSID" ] || [ -z "$PIN" ]; then
    echo -e "${CYAN}PS3 WiFi Connect Script v2${NC}"
    echo ""
    echo "Usage: sudo $0 <SSID> <8-digit-PIN>"
    echo "Example: sudo $0 PS3REGPC_4726 47261234"
    echo ""
    read -p "Enter PS3 WiFi SSID: " SSID
    read -p "Enter 8-digit PIN from PS3 screen: " PIN
fi

if [ -z "$SSID" ] || [ -z "$PIN" ]; then
    err "Both SSID and PIN are required"
    exit 1
fi

if [ "$EUID" -ne 0 ]; then
    err "Run as root: sudo $0 $SSID $PIN"
    exit 1
fi

# ── Derive password candidates ──
# SSID pattern: PS3REGPC_XXXX or PS3REGIST_XXXX
# The _XXXX suffix = first 4 digits of PIN
SSID_SUFFIX=$(echo "$SSID" | grep -oE '[0-9]+$')
PIN_FIRST4="${PIN:0:4}"
PIN_LAST4="${PIN:4:4}"

echo ""
echo -e "${CYAN}══════════════════════════════════════${NC}"
echo -e "${CYAN}  PS3 WiFi Connect — Trying All Combos${NC}"
echo -e "${CYAN}══════════════════════════════════════${NC}"
echo ""
info "SSID: $SSID"
info "PIN: $PIN"
info "SSID suffix: $SSID_SUFFIX"
info "PIN first 4: $PIN_FIRST4"
info "PIN last 4: $PIN_LAST4"
echo ""

# Build list of passwords to try
PASSWORDS=(
    "$PIN"                          # Full 8-digit PIN as-is
    "${PIN_LAST4}${PIN_FIRST4}"     # PIN reversed halves
    "$SSID_SUFFIX$PIN_LAST4"        # SSID suffix + last 4
    "$PIN_LAST4$SSID_SUFFIX"        # Last 4 + SSID suffix
    "${PIN}0000"                    # PIN padded
    "00000000"                      # All zeros
    "12345678"                      # Common default
    "$SSID"                         # SSID itself as password
)

# Also try open (no password)
info "Will try ${#PASSWORDS[@]} password candidates + open network"
for i in "${!PASSWORDS[@]}"; do
    info "  Candidate $((i+1)): ${PASSWORDS[$i]}"
done
echo ""

# ── Find WiFi interface ──
IFACE=""
for dev in /sys/class/net/*/wireless; do
    [ -d "$dev" ] && IFACE=$(echo "$dev" | cut -d'/' -f5) && break
done
[ -z "$IFACE" ] && IFACE=$(iwconfig 2>/dev/null | grep "IEEE 802.11" | head -1 | awk '{print $1}')
if [ -z "$IFACE" ]; then
    err "No WiFi interface found!"
    exit 1
fi
log "WiFi interface: $IFACE"

# ── Stop interfering services ──
info "Stopping NetworkManager..."
systemctl stop NetworkManager 2>/dev/null
killall wpa_supplicant 2>/dev/null
killall dhclient 2>/dev/null
sleep 1

# ── Function: try connecting with a specific config ──
try_connect() {
    local CONFIG_FILE="$1"
    local LABEL="$2"
    local TIMEOUT="${3:-12}"

    killall wpa_supplicant 2>/dev/null
    sleep 0.5

    ip link set "$IFACE" down 2>/dev/null
    iw "$IFACE" set type managed 2>/dev/null
    ip link set "$IFACE" up

    local TMPLOG="/tmp/ps3_wpa_attempt.txt"
    echo "" > "$TMPLOG"

    timeout "$TIMEOUT" wpa_supplicant -i "$IFACE" -c "$CONFIG_FILE" -D nl80211 2>&1 | tee "$TMPLOG" &
    local PID=$!
    sleep 8

    # Check if connected
    if iw "$IFACE" link 2>/dev/null | grep -q "Connected"; then
        log "$LABEL — CONNECTED!"
        kill $PID 2>/dev/null
        return 0
    fi

    # Check for specific failures
    if grep -q "WRONG_KEY" "$TMPLOG"; then
        warn "$LABEL — wrong key"
    elif grep -q "CTRL-EVENT-CONNECTED" "$TMPLOG"; then
        log "$LABEL — connected (event)!"
        kill $PID 2>/dev/null
        return 0
    elif grep -q "Associated" "$TMPLOG"; then
        # Associated but then disconnected = wrong password
        warn "$LABEL — associated but WPA handshake failed"
    elif grep -q "not found" "$TMPLOG" || grep -q "No such" "$TMPLOG"; then
        warn "$LABEL — network not found"
    else
        warn "$LABEL — failed (unknown reason)"
    fi

    kill $PID 2>/dev/null
    wait $PID 2>/dev/null
    return 1
}

# ── Try WPA-PSK with each password ──
CONNECTED=false
for i in "${!PASSWORDS[@]}"; do
    PASS="${PASSWORDS[$i]}"
    if [ ${#PASS} -lt 8 ]; then
        info "Skipping '$PASS' (too short for WPA, need 8+ chars)"
        continue
    fi

    echo ""
    info "━━━ Trying password $((i+1))/${#PASSWORDS[@]}: '$PASS' ━━━"

    # WPA-PSK AES
    cat > /tmp/ps3try.conf << EOF
network={
    ssid="$SSID"
    psk="$PASS"
    key_mgmt=WPA-PSK
    proto=WPA WPA2
    pairwise=CCMP TKIP
    group=CCMP TKIP
}
EOF
    if try_connect /tmp/ps3try.conf "WPA-PSK '$PASS'"; then
        CONNECTED=true
        break
    fi

    # Also try WPA-NONE (ad-hoc specific) on freq 2437 (channel 6, where PS3 was seen)
    cat > /tmp/ps3try.conf << EOF
ctrl_interface=/var/run/wpa_supplicant
ap_scan=2

network={
    ssid="$SSID"
    mode=1
    frequency=2437
    proto=WPA
    key_mgmt=WPA-NONE
    pairwise=NONE
    group=CCMP
    psk="$PASS"
}
EOF
    if try_connect /tmp/ps3try.conf "WPA-NONE ad-hoc '$PASS'" 10; then
        CONNECTED=true
        break
    fi
done

# ── Try open network (no password) ──
if [ "$CONNECTED" = false ]; then
    echo ""
    info "━━━ Trying OPEN network (no password) ━━━"

    cat > /tmp/ps3try.conf << EOF
network={
    ssid="$SSID"
    key_mgmt=NONE
}
EOF
    if try_connect /tmp/ps3try.conf "Open (no password)"; then
        CONNECTED=true
    fi

    # Open ad-hoc
    if [ "$CONNECTED" = false ]; then
        cat > /tmp/ps3try.conf << EOF
ctrl_interface=/var/run/wpa_supplicant
ap_scan=2

network={
    ssid="$SSID"
    mode=1
    frequency=2437
    key_mgmt=NONE
}
EOF
        if try_connect /tmp/ps3try.conf "Open ad-hoc"; then
            CONNECTED=true
        fi
    fi
fi

# ── If connected, get IP and test ──
if [ "$CONNECTED" = true ]; then
    echo ""
    echo -e "${GREEN}══════════════════════════════════════${NC}"
    echo -e "${GREEN}  WIFI CONNECTED!${NC}"
    echo -e "${GREEN}══════════════════════════════════════${NC}"
    echo ""

    info "Getting IP via DHCP..."
    timeout 10 dhclient -v "$IFACE" 2>&1 || {
        warn "DHCP failed. Trying static IPs..."
        for STATIC in "192.168.1.2/24" "172.24.0.2/16" "10.0.0.2/24" "192.168.0.2/24"; do
            ip addr flush dev "$IFACE" 2>/dev/null
            ip addr add "$STATIC" dev "$IFACE"
            GW=$(echo "$STATIC" | sed 's/\.[0-9]*\//\.1\//' | cut -d'/' -f1)
            if ping -c 1 -W 2 "$GW" >/dev/null 2>&1; then
                log "Static IP $STATIC works, gateway $GW reachable"
                break
            fi
        done
    }

    info "IP addresses on $IFACE:"
    ip addr show "$IFACE" | grep inet

    info "Testing PS3 connectivity..."
    for IP in 192.168.1.1 192.168.1.75 172.24.0.1 10.0.0.1 192.168.0.1; do
        if ping -c 1 -W 2 "$IP" >/dev/null 2>&1; then
            log "PS3 at $IP is reachable!"
            if nc -z -w 3 "$IP" 9293 2>/dev/null; then
                echo ""
                echo -e "${GREEN}████████████████████████████████████████${NC}"
                echo -e "${GREEN}  SUCCESS! PS3 at $IP port 9293 OPEN${NC}"
                echo -e "${GREEN}  Use this IP in your Mac app to register${NC}"
                echo -e "${GREEN}████████████████████████████████████████${NC}"
                echo ""
                info "Keeping alive. Press Ctrl+C when done."
                while ping -c 1 -W 5 "$IP" >/dev/null 2>&1; do sleep 3; done
                exit 0
            else
                warn "PS3 at $IP but port 9293 closed"
            fi
        fi
    done
    warn "Connected to WiFi but can't reach PS3"
else
    echo ""
    echo -e "${RED}══════════════════════════════════════${NC}"
    echo -e "${RED}  COULD NOT CONNECT${NC}"
    echo -e "${RED}══════════════════════════════════════${NC}"
    echo ""
    echo "None of the password combinations worked."
    echo ""
    echo "Possible issues:"
    echo "  1. PS3 exited registration mode (start it again)"
    echo "  2. WiFi chip firmware blocks ad-hoc connections"
    echo "  3. The WPA key format is different than expected"
    echo ""
    echo "Last scan results:"
    ip link set "$IFACE" down 2>/dev/null
    iw "$IFACE" set type managed 2>/dev/null
    ip link set "$IFACE" up
    sleep 2
    iw "$IFACE" scan 2>/dev/null | grep -B2 -A10 "PS3" || echo "  (PS3 network not found in scan)"
fi

echo ""
info "Full log saved to: $LOGFILE"
