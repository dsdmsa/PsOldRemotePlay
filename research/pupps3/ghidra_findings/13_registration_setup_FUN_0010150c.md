# Ghidra Finding: FUN_0010150c — Registration Context Setup

## Address: 0x0010150c (in sysconf_plugin.elf)
## Type: Creates the registration thread and sets up the context structure

## This answers the question of what param_1 + 0x38 is!

```c
int FUN_0010150c(int param_1, undefined8 param_2, undefined4 param_3,
                 undefined4 param_4, undefined4 param_5)
{
    memset(param_1, 0, 0x868);          // Clear entire context (2152 bytes)
    *(param_1 + 0x3c) = param_4;        // PS3 identity pointer
    *(param_1 + 0x28) = param_3;        // Some config value
    *(param_1 + 0x24) = 0xFFFFFFFF;     // Socket = -1 (not connected)
    *(param_1 + 0x38) = param_5;        // ← THE CONTEXT POINTER we've been looking for!

    // Create mutex/event
    iVar1 = FUN_0012f590(param_1 + 8, local_40);
    if (iVar1 == 0) {
        *(param_1 + 0x34) |= 1;  // flag: mutex created

        // Create event port
        iVar1 = FUN_000feb94(param_1 + 0x40);
        if (iVar1 >= 0) {
            *(param_1 + 0x34) |= 2;  // flag: event port created

            // CREATE THE REGISTRATION THREAD
            iVar1 = FUN_0012f4e8(
                param_1,
                &PTR_LAB_0015e880,  // ← Thread function = FUN_00100168 (registration handler!)
                param_1,            // Thread argument = the context
                0x7d3,              // Thread priority
                0x1000,             // Stack size (4KB)
                1,                  // ???
                "cellPremoAcceptThread"  // Thread name
            );
            if (iVar1 == 0) {
                *(param_1 + 0x34) |= 4;  // flag: thread created
                return 0;
            }
        }
    }
    FUN_001013dc(param_1);  // Cleanup on failure
    return iVar1;
}
```

## KEY FINDING: param_5 IS param_1 + 0x38

The context value at `param_1 + 0x38` is **param_5** — the 5th argument passed to this setup function.

To find what param_5 is, we need to find **what calls FUN_0010150c**.

## Context Structure Layout (param_1, total 0x868 = 2152 bytes)

| Offset | Set By | Content |
|--------|--------|---------|
| +0x00 | memset | Zeroed (2152 bytes total) |
| +0x08 | FUN_0012f590 | Mutex handle |
| +0x24 | = 0xFFFFFFFF | Socket (initially invalid) |
| +0x28 | = param_3 | Config value |
| +0x30 | registration handler | Failed attempt counter |
| +0x34 | bitmask | Init flags (1=mutex, 2=event, 4=thread) |
| +0x38 | = **param_5** | **THE IDENTITY/CONFIG POINTER** |
| +0x3c | = param_4 | PS3 identity struct pointer |
| +0x40 | FUN_000feb94 | Event port |
| +0x68 | registration handler | 1024-byte I/O buffer |
| +0x184 | ??? | 16-byte key (for PC/VITA registration) |
| +0x468 | HTTP parser | Parser state |

## NEXT STEP

Find what calls `FUN_0010150c` — specifically what `param_5` is.

In Ghidra: click on `FUN_0010150c` function name → press **Ctrl+Shift+F** to find references → look at the caller to see what the 5th argument is.

Also: `param_1 + 0x184` (the 16-byte key for PC/VITA) — since the context is zeroed initially, this must be set AFTER FUN_0010150c returns and BEFORE the registration HTTP request arrives. Look for what writes to offset 0x184 in the context.
