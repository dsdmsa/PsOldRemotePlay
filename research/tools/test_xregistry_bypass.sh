#!/bin/bash
# xRegistry bypass test script
# Usage: ./test_xregistry_bypass.sh <ps3_ip> <device_mac>
#
# This script tests the xRegistry.sys bypass path for device registration:
# 1. Download xRegistry.sys from PS3 via FTP
# 2. Inject a test device entry with random pkey
# 3. Upload modified file back
# 4. Reboot PS3
# 5. Try session connection with the injected pkey

set -e

PS3_IP="${1:-192.168.1.80}"
DEVICE_MAC="${2:-aabbccddeeff}"
FTP_USER="${FTP_USER:-root}"
FTP_PASS="${FTP_PASS:-}"  # WebMAN-MOD has no password by default

echo "=== xRegistry Bypass Test ==="
echo "PS3 IP: $PS3_IP"
echo "Device MAC: $DEVICE_MAC"
echo ""

# Generate random 16-byte pkey (32 hex chars)
PKEY=$(openssl rand -hex 16)
DEVICE_ID=$(openssl rand -hex 16)

echo "Generated test credentials:"
echo "  PKey: $PKEY"
echo "  DeviceID: $DEVICE_ID"
echo ""

# Verify xRegistry.sys exists locally
if [ ! -f "xRegistry.sys" ]; then
    echo "ERROR: xRegistry.sys not found in current directory"
    echo "Dowload from PS3 first:"
    echo "  ftp $PS3_IP"
    echo "  cd /dev_flash2/etc"
    echo "  get xRegistry.sys"
    echo "  quit"
    exit 1
fi

echo "Step 1: Backing up original xRegistry.sys..."
cp xRegistry.sys xRegistry.sys.bak

echo "Step 2: Injecting test device..."
python3 xregistry_inject.py inject xRegistry.sys xRegistry.sys.new "$PKEY" "$DEVICE_MAC" "$DEVICE_ID" "PsOldRemotePlayTest"

if [ ! -f "xRegistry.sys.new" ]; then
    echo "ERROR: Injection failed"
    exit 1
fi

echo "Step 3: Uploading modified xRegistry.sys to PS3..."
(echo "open $PS3_IP $FTP_USER"
 [ -n "$FTP_PASS" ] && echo "$FTP_PASS" || echo ""
 echo "binary"
 echo "cd /dev_flash2/etc"
 echo "put xRegistry.sys.new xRegistry.sys"
 echo "quit") | ftp -n

echo "Step 4: Rebooting PS3 (will take ~1 minute)..."
echo "Press ENTER after PS3 boots to continue, or Ctrl+C to cancel..."
read

echo ""
echo "=== Test Credentials Ready ==="
echo "Use these in PsOldRemotePlay to test session connection:"
echo "  PS3 IP: $PS3_IP"
echo "  PKey:   $PKEY"
echo "  Device ID: $DEVICE_ID"
echo "  Device MAC: $DEVICE_MAC"
echo ""
echo "The registration step is SKIPPED - device is pre-registered in xRegistry.sys"
echo "Just enter these values in the app and click 'Connect Session'"
