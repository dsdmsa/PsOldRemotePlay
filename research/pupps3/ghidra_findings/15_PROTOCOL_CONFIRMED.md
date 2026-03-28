# MILESTONE: Protocol Confirmed Working Over Wired Network

## Date: 2026-03-27

## What happened:
- PS3 (wired ethernet at 192.168.1.75) opens TCP port 9293 during registration mode
- Our desktop app sent a test session request
- PS3 responded with 403 Forbidden + error code 80029820 (PSPID not registered)
- This confirms the ENTIRE protocol stack works: TCP connection, HTTP format, PREMO headers

## PS3 Response:
```
HTTP/1.1 403 Forbidden
Connection: close
Pragma: no-cache
Content-Length: 0
PREMO-Version: 0.3
PREMO-Application-Reason: 80029820
```

## Key Finding:
The PS3 listens on port 9293 on the WIRED interface during registration mode.
This means we can potentially register over the wired network WITHOUT WiFi!

## Next Step:
Implement `POST /sce/premo/regist` to register the device over the wired network.
The PS3 shows the 8-digit PIN on screen — we use that for the encryption.
