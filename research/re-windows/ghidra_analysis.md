# VAIO DLL Ghidra Static Analysis Guide

## Overview

This document describes how to load and analyze the decompressed VAIO DLL section in Ghidra when dynamic hooking (Frida) is not feasible or for deeper code-level understanding of the IV context derivation.

The goal is to find where the 8-byte `cfContext` parameter comes from and trace it back to its source (PIN vs hardware ID vs other).

## File Preparation

### Step 1: Extract and Decompress DLL

If you haven't already extracted the DLL:

```bash
bash research/re-windows/run_vaio_wine.sh
```

This produces `/tmp/vrpsdk_decrypted/VRPSDK.dll`.

### Step 2: Decompress with aplib (if packed)

VAIO DLLs are often packed with aplib. Use the provided decompression script:

```bash
python3 research/tools/unpack_vrpsdk_aplib.py /tmp/vrpsdk_decrypted/VRPSDK.dll
```

Output: `decompressed_section.bin` (or similar) in the same directory.

Alternatively, if aplib fails, try UPX:

```bash
upx -d /tmp/vrpsdk_decrypted/VRPSDK.dll -o /tmp/vrpsdk_decrypted/VRPSDK_unpacked.dll
```

## Loading into Ghidra

### Step 3: Create Ghidra Project

1. **Open Ghidra** (if not installed: `brew install ghidra`)
2. **File > New Project** → Create a new project for VAIO analysis
3. **File > Import File** → Select the decompressed DLL or unpacked DLL
4. **Import dialog:**
   - Detect architecture automatically (should be x86 or x86-64)
   - Language: Intel x86 (32-bit or 64-bit)
   - Format: PE (Windows executable)
   - Click **OK**

### Step 4: Analyze the DLL

When Ghidra asks to analyze, click **Yes** and wait for analysis to complete.

## Finding the Registration Setup Function

### Search Strategy 1: String Cross-References

VAIO registration code likely references hardware/device identification. Search for these strings:

1. **Go to Search > For Strings**
2. Search for patterns:
   - `"SuGetMachineIDEx"` - Hardware machine ID retrieval function
   - `"GetComputerName"` - Computer name (less likely for crypto)
   - `"cfContext"` - If debug symbols present
   - `"IV"` - Initialization vector references
   - `"PIN"` - Personal identification number

3. **Cross-reference the string** (right-click → References):
   - Find which function(s) call `SuGetMachineIDEx`
   - That function likely derives the IV context

### Search Strategy 2: Crypto Function Patterns

Look for calls to standard Windows crypto APIs:

1. **Search > For Strings**: `"CryptEncrypt"` or `"CryptSetKeyParam"`
2. **Cross-reference** to find where IV/key material is set up
3. In those functions, trace back parameters to their source

### Search Strategy 3: Direct Address

According to decompiled analysis, the registration setup function is at offset **0x1000bb60** in some VAIO builds:

1. **Go > Go to Address** → Enter `0x1000bb60`
2. If this is within the DLL, you should see the registration handler
3. Look for:
   - Parameter setup (especially 8-byte context value)
   - Calls to `GetComputerName` or `SuGetMachineIDEx`
   - AES key derivation
   - IV initialization

## Function Signature to Find

Once you locate the registration function, look for this pattern:

```c
// Pseudo-code of what we're looking for:
BOOL StartRegistration(
    const char *PIN,           // PIN string (e.g., "0000")
    LPSTR cfContext,           // 8-byte context value (UNKNOWN SOURCE)
    ...
) {
    // This is the key line:
    cfContext = DeriveContextFromXXX(PIN);  // We need to know what XXX is

    // Then:
    SetupAESKey(PIN, cfContext, ...);
    CryptSetKeyParam(hKey, KP_IV, cfContext, ...);
    // ... rest of encryption
}
```

## Analysis Workflow

### Step 1: Find String References

```
1. Search > For Strings
2. Type: "SuGetMachineIDEx"
3. When found, right-click > References
4. This should point to the function that uses machine ID for IV
```

### Step 2: Analyze the Function

When you find the function:

1. **Look at function parameters** (first 3-4 args on x86-64)
   - Identify which parameter is the PIN (usually a string pointer)
   - Identify which parameter is the context value destination

2. **Trace the context derivation**:
   - Does it call `SuGetMachineIDEx`? → Hardware ID based
   - Does it use the PIN parameter? → PIN based
   - Does it use system time/random data? → Runtime based

3. **Check crypto function calls**:
   - Find where `CryptSetKeyParam` is called
   - Check parameter 3 (pbData) - does it come from the context?
   - This confirms cfContext is used as the IV

### Step 3: Document Findings

Create a findings document with:

```
FINDING: IV Context Source
Offset: 0x...
Function: ...
Source: [Hardware ID / PIN / Other]
Derivation: [Formula if available]
```

## Expected Findings

Based on decompiled code analysis, expect to find:

- **Hardware ID path**: Uses `SuGetMachineIDEx()` to get machine GUID, derives IV from it
- **PIN path**: Uses the PIN string directly as context or derives from it
- **Hybrid path**: Combines PIN + hardware ID

The actual bytes may be:
- Hardware GUID (16 bytes, take first 8)
- MD5/SHA1 hash of hardware GUID (take first 8 bytes)
- XOR combination of PIN and hardware ID
- Direct byte representation of PIN (e.g., "0000" → 0x30303030)

## Debugging Tips

### Enable Debug Info (if available)

Some VAIO DLLs may have partial debug info:

1. **Window > Symbol Table**
2. Look for function names like:
   - `?StartRegistration@...`
   - `?DeriveContext@...`
   - `?GetMachineContext@...`

### Data Flow Analysis

Use Ghidra's data flow features:

1. **Right-click variable** > **Show Uses/Definitions**
2. Trace where the 8-byte context value comes from
3. Look backward until you find the source (function return, parameter, etc.)

### Graph View

For complex functions:

1. **Window > Function Graph**
2. Click on blocks to see data flow between them
3. Focus on blocks that:
   - Call `GetComputerName` or `SuGetMachineIDEx`
   - Set up AES key parameters
   - Initialize crypto context

## If Analysis Stalls

If Ghidra analysis doesn't yield clear results:

1. **Try IDA Pro** (commercial, but better decompiler):
   - Free trial available
   - IDA's decompiler may produce clearer pseudocode

2. **Try Radare2** (free alternative):
   ```bash
   brew install radare2
   r2 -A /tmp/vrpsdk_decrypted/VRPSDK.dll
   ```

3. **Go back to Frida hooking** - dynamic analysis is often more reliable than static

## Validation

Once you identify the IV context derivation:

1. **Test with brute-force script**:
   ```bash
   python3 research/tools/ps3_register_bruteforce_iv.py \
     --context-source hardware_id \
     --test-registration
   ```

2. **Verify against decompiled code**:
   - Compare your findings to existing reverse engineering
   - Check against PS3 firmware PREMO plugin decompilation

## References

- `research/pupps3/ghidra_findings/20_DECOMPILED_REGISTRATION_HANDLER.md` - PS3 firmware PREMO handler analysis
- `research/tools/unpack_vrpsdk_aplib.py` - DLL decompression utility
- `research/tools/ps3_register_bruteforce_iv.py` - IV context brute-force tester
