# Ghidra Finding: FUN_0000ab1c — Remote Play Dialog/UI State Handler

## Address: 0x0000ab1c
## Type: UI dialog state machine

## Analysis

This is the **Remote Play UI dialog handler**. It displays different messages to the user based on the state variable `DAT_0002f114`. This is the XMB-side UI flow — not the protocol itself, but it reveals the **state machine** of the registration process.

## State Machine (DAT_0002f114 values)

| State | Message String | Meaning |
|-------|---------------|---------|
| 0 | `msg_connect_from_psp_see_manual` | Initial state — "connect PSP, see manual" |
| 1 | `msg_connect_from_psp` | Waiting for PSP connection |
| 2 | `msg_psp_connection_running` | PSP connection in progress |
| 4 | `msg_remoteplay_register_psp_pls` | **REGISTRATION** — "please register PSP" |
| 5 | `msg_to_operate_atrac_activate_pls` | ATRAC activation needed |
| 6 | `msg_error_disable_internet_prompt` | Error: disable internet |
| 7 | `msg_error_no_netcable` | Error: no network cable |
| 8 | `msg_error_connect_net_prompt` | Error: connect to network |
| 9 | `msg_wait` | Waiting/processing |
| 10 | `msg_connect_from_psp_turn_off_auto` | Turn off auto-off during connection |
| 11 | `msg_timer_application_running_auto_off_press_ps` | Timer/auto-off warning |
| default | `msg_error` + error code | Generic error with `DAT_0002f118` error code |

## Key Variables

- `DAT_0002f114` — Current dialog state
- `DAT_0002f118` — Error code (used in default/error case)
- `DAT_0002f11c` — Sub-state flag (0 = one-button dialog, non-0 = two-button dialog with `DAT_0002f1d4`)
- `DAT_0002f10c` — Dialog handle/ID
- `DAT_0002f1d0` — Device type flags (bit 0x1000 differentiates device categories)
- `DAT_0002f1d4` — Second button action parameter
- `DAT_0002f1d8` — Version/capability field (lower 16 bits compared against threshold)

## Key Functions Called

- `FUN_000277d0("premo_plugin")` — Get plugin resource handle
- `FUN_00026810(handle, msg_key)` — Load localized message string
- `FUN_00026730(handle, "premo_dialog_page")` — Get dialog page template
- `FUN_00026dc0(...)` — Format message with parameters
- `FUN_000086e0(buf, msg)` — Set dialog message text
- `FUN_00025460(page, msg, button_count, ...)` — Create dialog with N buttons
- `FUN_000254d0(0, dialog)` — Show dialog
- `FUN_00025498(dialog, callback, param)` — Set dialog callback (PTR_LAB_0002eb48)
- `FUN_00007888(buf)` — Cleanup/free buffer
- `FUN_00007e84(param, 0)` — Special action after wait state (state 9)

## Important Observations

1. **State 4 is the registration prompt** — `msg_remoteplay_register_psp_pls`
2. **DAT_0002f11c** controls whether it's a simple prompt (1 button) or a prompt with cancel (2 buttons)
3. **The callback `PTR_LAB_0002eb48`** handles user responses to all dialogs — investigating this callback will reveal what happens AFTER the user triggers registration
4. **`FUN_00007e84`** is called only in state 9 (wait) — likely starts the actual connection/registration process

## Next Steps
- Investigate `PTR_LAB_0002eb48` (dialog callback) — this routes user actions
- Investigate what SETS `DAT_0002f114` to different values — find the caller/state machine driver
- Find `FUN_00007e84` — may initiate the actual network registration handshake

## Raw Decompiled Code

```c
void FUN_0000ab1c(undefined4 param_1)
{
  // [full decompiled code as provided by user]
  // ... see user's paste above
}
```
