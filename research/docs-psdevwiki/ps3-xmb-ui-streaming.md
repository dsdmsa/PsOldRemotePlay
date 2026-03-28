# PS3 Full XMB UI Streaming via Remote Play

## Key Finding: YES — Full UI Streaming is Supported and is the DEFAULT

The PS3 PREMO protocol streams the **entire PS3 XMB (XrossMediaBar) interface** by default. When a Remote Play session starts, the user is "greeted by the PS3's XMB interface and able to control the console as if you were sitting in front of it with a PS3 game control."

## Execution Modes (PREMO-Exec-Mode header)

The protocol defines three modes, returned by the PS3 in the session response:

1. **`vsh`** (Visual Shell) — Full PS3 XMB UI streaming. This is the DEFAULT mode when Remote Play starts. VSH is Sony's internal name for the shell environment that loads the XMB, located at `/dev_flash/vsh/module/vsh.self`.
2. **`game`** — In-game streaming. Uses `premo_game_plugin.sprx`.
3. **`ps1`** — PS1 classic game streaming under emulation.

The mode is informational from the client's perspective — the video stream is continuous regardless of mode. The PS3 decides what to render.

Source: Open-RP `orp.cpp` line 2805 (parses PREMO-Exec-Mode header)

## XMB Restrictions During Remote Play

By default, certain XMB settings columns are hidden during Remote Play:
- No Update Option
- Game Settings (limited)
- Chat Settings
- System Settings
- Peripheral Device Settings
- Display Settings
- Remote Play Settings
- Network Settings

**These restrictions can be removed** by patching `premo_plugin.sprx` firmware module (documented on psdevwiki). The psdevwiki has a side-by-side comparison of "Restricted vs. Unrestricted" XMB during remote play.

## What Gets Streamed

Remote Play streams whatever the PS3's GPU framebuffer contains:
- XMB navigation (all columns: Users, Settings, Photo, Music, Video, Game, Network, Friends)
- Game content
- PS1 classics
- Media playback
- PlayStation Store

### Resolution Options
| Client | Resolution |
|--------|-----------|
| PSP | 480x272 |
| Vita | Up to 852x480 |
| VAIO/PC | Up to 852x480 |

### Framerate Options
7.5, 10, 15, or 30 fps (negotiated via PREMO-Video-Framerate header)

### Video Codecs
- M4V (MPEG-4 Part 2)
- AVC (H.264)
- M4HD (higher quality H.264)

### Bitrate Range
256 kbps to 2500 kbps (negotiated via PREMO-Video-Bitrate header)

## Limitations

- **PS2 Classic games CANNOT be played** via Remote Play (they run as a separate lv2 kernel)
- **PS3 games require a `REMOTE_PLAY` flag** in their `PARAM.SFO` — with custom firmware/webMAN-MOD this can be forced for any game
- **While remote play is active, the PS3 does NOT output a useful signal over HDMI/AV**

## Forcing Remote Play on All Games

With custom firmware and webMAN-MOD:
- `/play.ps3` command launches the Remote Play server
- `/play.ps3?col=network&seg=seg_premo` launches from specific XMB location
- The `REMOTE_PLAY` flag can be forced for any game

## Comparison with PS4/PS5 Modern Apps

| Feature | PS3 (PREMO) | PS4/PS5 (Chiaki/PSPlay) |
|---------|-------------|------------------------|
| Full UI streaming | YES (default) | YES |
| Navigate home screen | YES (XMB) | YES (explicit `XMBCOMMAND` in protocol) |
| Launch games | YES | YES |
| All games supported | With CFW only | Most games by default |
| Resolution | Up to 852x480 | Up to 4K |

The chiaki-ng source code confirms PS4/PS5 apps also stream full UI — it has `XMBCOMMAND` message type (in `takion.proto`, line 23) and `chiaki_session_go_home()` function.

## Implications for Our App

Our app can serve as a **complete PS3 remote control**, allowing users to:
- Browse the XMB menus
- Navigate settings
- Access the PlayStation Store
- Launch games
- Manage media (music, photos, videos)
- Use all PS3 features remotely

This is equivalent to what PSPlay and chiaki-ng do for PS4/PS5.

## Sources
- Open-RP source code: `orp.h`, `orp.cpp`
- PS3 Developer Wiki: Remote Play — https://www.psdevwiki.com/ps3/Remote_Play
- PS3 Developer Wiki: Premo Plugin — https://www.psdevwiki.com/ps3/Premo_plugin
- PS3 Developer Wiki: VSH — https://www.psdevwiki.com/ps3/VSH
- ConsoleMods Wiki: PS3:Forcing Remote Play — https://consolemods.org/wiki/PS3:Forcing_Remote_Play
- chiaki-ng source: `takion.proto`, `ctrl.c`
- PlayStation Manual: https://manuals.playstation.net/document/en/ps3/current/remoteplay/remoteplay.html
