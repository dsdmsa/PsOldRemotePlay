#!/usr/bin/env python3
"""
WiFi L2 Probe for PS3 Registration AP

Diagnoses connectivity issues with PS3's registration AP (192.168.52.1).
Sends ARP WHO-HAS, DHCP DISCOVER, and monitors for any L2 frames from PS3 MAC.

Usage:
    sudo python3 wifi_l2_probe.py <interface> [--ps3-mac <MAC>] [--timeout <seconds>]

Example:
    sudo python3 wifi_l2_probe.py wlp2s0
    sudo python3 wifi_l2_probe.py wlp2s0 --ps3-mac 4C:0F:6E:9D:3B:21 --timeout 10
"""

import sys
import argparse
import time
from typing import Optional, Tuple

try:
    from scapy.all import (
        ARP, DHCP, BOOTP, Ether, IP, UDP, Raw,
        srp, sniff, get_if_hwaddr, conf,
        IPPROTO_UDP, get_if_list
    )
except ImportError:
    print("ERROR: scapy not installed. Install with: pip install scapy")
    sys.exit(1)


def mac_to_string(mac_bytes) -> str:
    """Convert MAC bytes to colon-separated string."""
    if isinstance(mac_bytes, str):
        return mac_bytes
    return ":".join(f"{b:02x}" for b in mac_bytes)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Probe PS3 registration AP for L2 connectivity issues"
    )
    parser.add_argument(
        "interface",
        help="Network interface (e.g., wlp2s0)"
    )
    parser.add_argument(
        "--ps3-mac",
        default="4C:0F:6E:9D:3B:21",
        help="PS3 MAC address (default: 4C:0F:6E:9D:3B:21)"
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=5,
        help="Timeout in seconds for each probe (default: 5)"
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Print verbose frame details"
    )
    return parser.parse_args()


def validate_interface(iface: str) -> bool:
    """Check if interface exists."""
    if iface not in get_if_list():
        print(f"ERROR: Interface '{iface}' not found")
        print(f"Available interfaces: {', '.join(get_if_list())}")
        return False
    return True


def get_local_mac(iface: str) -> str:
    """Get the MAC address of the interface."""
    try:
        return get_if_hwaddr(iface)
    except Exception as e:
        print(f"ERROR: Could not get MAC for {iface}: {e}")
        sys.exit(1)


def send_arp_who_has(iface: str, target_ip: str = "192.168.52.1", timeout: int = 5) -> Tuple[bool, Optional[str]]:
    """
    Send ARP WHO-HAS to target IP and listen for response.
    Returns (got_response, responder_mac)
    """
    print(f"\n[1] Sending ARP WHO-HAS to {target_ip}...")
    local_mac = get_local_mac(iface)

    arp_packet = Ether(dst="ff:ff:ff:ff:ff:ff") / ARP(
        op="who-has",
        pdst=target_ip,
        hwsrc=local_mac
    )

    try:
        result = srp(arp_packet, iface=iface, timeout=timeout, verbose=0)
        if result[0]:
            responder_mac = result[0][0][1].hwsrc
            print(f"    -> Received ARP REPLY from {responder_mac}")
            return True, responder_mac
        else:
            print(f"    -> Timeout - no ARP response")
            return False, None
    except Exception as e:
        print(f"    -> Error sending ARP: {e}")
        return False, None


def send_dhcp_discover(iface: str, timeout: int = 5) -> bool:
    """
    Send DHCP DISCOVER to 255.255.255.255 and listen for DHCP OFFER.
    Returns True if any DHCP server responds.
    """
    print(f"\n[2] Sending DHCP DISCOVER to 255.255.255.255...")
    local_mac = get_local_mac(iface)

    dhcp_packet = Ether(dst="ff:ff:ff:ff:ff:ff") / IP(
        src="0.0.0.0",
        dst="255.255.255.255"
    ) / UDP(sport=68, dport=67) / BOOTP(
        chaddr=local_mac,
        xid=0x12345678
    ) / DHCP(
        options=[("message-type", 1), "end"]  # DHCP DISCOVER
    )

    try:
        result = srp(dhcp_packet, iface=iface, timeout=timeout, verbose=0)
        if result[0]:
            for sent, received in result[0]:
                src_mac = received.src
                print(f"    -> Received DHCP response from {src_mac}")
            return True
        else:
            print(f"    -> Timeout - no DHCP server found")
            return False
    except Exception as e:
        print(f"    -> Error sending DHCP: {e}")
        return False


def monitor_frames(iface: str, ps3_mac: str, timeout: int = 5, verbose: bool = False) -> bool:
    """
    Listen for ANY frame from PS3 MAC and print EtherType.
    Returns True if frame received.
    """
    print(f"\n[3] Monitoring for frames from {ps3_mac.upper()} ({timeout}s timeout)...")

    def packet_callback(pkt):
        if pkt.haslayer(Ether):
            src = pkt[Ether].src.upper()
            if src == ps3_mac.upper():
                etype = pkt[Ether].type
                print(f"    -> Received EtherType 0x{etype:04x} from {src}")

                # Decode common EtherTypes
                etype_names = {
                    0x0800: "IPv4",
                    0x0806: "ARP",
                    0x86DD: "IPv6",
                    0x88CC: "LLDP",
                    0x888E: "802.1X",
                    0x88A2: "AoE",
                }

                if etype in etype_names:
                    print(f"       ({etype_names[etype]})")

                # Show IP details if present
                if pkt.haslayer(IP):
                    src_ip = pkt[IP].src
                    dst_ip = pkt[IP].dst
                    proto = pkt[IP].proto
                    print(f"       IPv4: {src_ip} -> {dst_ip} (proto={proto})")

                # Show raw hex for unknown types
                if etype not in etype_names and verbose:
                    raw_data = bytes(pkt[Ether].payload)[:32]
                    print(f"       Raw: {raw_data.hex()}")

                return True
        return False

    try:
        sniff(
            iface=iface,
            prn=packet_callback,
            timeout=timeout,
            verbose=0,
            store=False
        )
        print(f"    -> Monitoring complete")
        return False
    except Exception as e:
        print(f"    -> Error monitoring: {e}")
        return False


def probe_link_local(iface: str, timeout: int = 5) -> bool:
    """
    Probe link-local address range (169.254.x.x) with ARP WHO-HAS.
    Returns True if any response.
    """
    print(f"\n[4] Probing link-local range (169.254.x.x)...")
    local_mac = get_local_mac(iface)

    # Try a few link-local addresses
    test_ips = [
        "169.254.1.1",
        "169.254.169.254",
        "169.254.255.255"
    ]

    found_response = False
    for target_ip in test_ips:
        arp_packet = Ether(dst="ff:ff:ff:ff:ff:ff") / ARP(
            op="who-has",
            pdst=target_ip,
            hwsrc=local_mac
        )

        try:
            result = srp(arp_packet, iface=iface, timeout=2, verbose=0)
            if result[0]:
                responder_mac = result[0][0][1].hwsrc
                print(f"    -> Response from {target_ip}: {responder_mac}")
                found_response = True
        except Exception:
            pass

    if not found_response:
        print(f"    -> No responses in link-local range")

    return found_response


def main():
    args = parse_args()

    print("=" * 70)
    print("PS3 Registration AP L2 Probe")
    print("=" * 70)
    print(f"Interface: {args.interface}")
    print(f"PS3 MAC: {args.ps3_mac}")
    print(f"Timeout: {args.timeout}s")
    print(f"Target IP: 192.168.52.1")
    print("=" * 70)

    # Validate interface
    if not validate_interface(args.interface):
        sys.exit(1)

    # Check if running as root (needed for raw packets)
    if sys.platform != "win32":
        import os
        if os.geteuid() != 0:
            print("\nWARNING: This script should be run with sudo for raw packet access")
            print("Re-run as: sudo python3 wifi_l2_probe.py ...\n")

    print("\nPrerequisites:")
    print("1. You should already be connected to PS3 AP (check: iwconfig)")
    print("2. PS3 AP should be broadcasting (not hidden)")
    print("3. Run with sudo for raw packet access\n")

    # Run probes
    arp_response, arp_mac = send_arp_who_has(args.interface, timeout=args.timeout)
    dhcp_response = send_dhcp_discover(args.interface, timeout=args.timeout)
    monitor_frames(args.interface, args.ps3_mac, timeout=args.timeout, verbose=args.verbose)
    linklocal_response = probe_link_local(args.interface, timeout=args.timeout)

    # Summary
    print("\n" + "=" * 70)
    print("SUMMARY")
    print("=" * 70)
    print(f"ARP WHO-HAS to 192.168.52.1:      {'YES' if arp_response else 'NO'}")
    print(f"DHCP server present:               {'YES' if dhcp_response else 'NO'}")
    print(f"Link-local (169.254.x.x) response: {'YES' if linklocal_response else 'NO'}")

    print("\nINTERPRETATION:")
    if not arp_response and not dhcp_response:
        print("- No standard IPv4/ARP responses detected")
        print("- PS3 AP may use custom L2 protocol or be in isolated mode")
        print("- Next: Check with tcpdump -i [iface] -X to see all frames")
        print("  Command: sudo tcpdump -i {args.interface} -X -vv")
    elif arp_response:
        print("- PS3 AP responds to standard ARP (L3 should work)")
        print("- Check if 192.168.52.1 is reachable: ping 192.168.52.1")
    elif dhcp_response:
        print("- DHCP server found but ARP failed")
        print("- Possible: IPv4 isolation, custom routing, or interface config issue")

    print("=" * 70)


if __name__ == "__main__":
    main()
