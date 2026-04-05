/* hook_registration.c - Hook AES SetKey/SetIV in VRPSDK.dll to capture registration crypto
 *
 * This tool:
 * 1. Loads rmp_dll.DLL first (patches lstrcmpW to bypass VAIO hardware check)
 * 2. Loads VRPSDK.dll (Themida unpacks)
 * 3. Hooks SetKey (base+0x1D60) and SetIV (base+0x1010) via int3 + VEH
 * 4. Creates COM object via DllGetClassObject
 * 5. Calls Initialize, then StartRegistration
 * 6. Logs all AES key/IV parameters
 *
 * Compile: i686-w64-mingw32-gcc -O2 -o hook_registration.exe hook_registration.c -lole32 -loleaut32 -luuid
 * Run:     wine hook_registration.exe <PIN>
 *          wine hook_registration.exe 12345678
 */

#include <windows.h>
#include <stdio.h>
#include <string.h>
#include <initguid.h>

/* ---- Globals for breakpoint hooking ---- */
static DWORD g_vrpsdk_base = 0;
static DWORD g_setkey_addr = 0;  /* VRPSDK base + 0x1D60 */
static DWORD g_setiv_addr = 0;   /* VRPSDK base + 0x1010 */
static DWORD g_encrypt_addr = 0; /* VRPSDK base + 0x1E60 */
static BYTE  g_setkey_orig = 0;
static BYTE  g_setiv_orig = 0;
static BYTE  g_encrypt_orig = 0;
static int   g_setkey_count = 0;
static int   g_setiv_count = 0;
static int   g_encrypt_count = 0;
static FILE  *g_log = NULL;
static BOOL  g_single_step_from_setkey = FALSE;
static BOOL  g_single_step_from_setiv = FALSE;
static BOOL  g_single_step_from_encrypt = FALSE;

/* Log helper */
static void hexdump(const char *label, const BYTE *data, int len) {
    printf("  %s: ", label);
    for (int i = 0; i < len; i++) printf("%02X ", data[i]);
    printf("\n");
    if (g_log) {
        fprintf(g_log, "%s: ", label);
        for (int i = 0; i < len; i++) fprintf(g_log, "%02X", data[i]);
        fprintf(g_log, "\n");
        fflush(g_log);
    }
}

/* ---- VEH: Catches int3 breakpoints and single-step traps ---- */
static LONG WINAPI veh_handler(EXCEPTION_POINTERS *ep) {
    DWORD eip = ep->ContextRecord->Eip;

    if (ep->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT) {
        if (eip == g_setkey_addr) {
            g_setkey_count++;
            DWORD ecx = ep->ContextRecord->Ecx;
            DWORD esp = ep->ContextRecord->Esp;
            /* __thiscall: ECX=this, stack: [ret_addr][key_ptr] */
            DWORD key_ptr = *(DWORD *)(esp + 4);

            printf("\n=== [HOOK] SetKey #%d ===\n", g_setkey_count);
            printf("  this=0x%08X key_ptr=0x%08X\n", ecx, key_ptr);
            if (key_ptr) hexdump("AES KEY", (BYTE *)key_ptr, 16);

            /* Also dump parent object areas where IV might be stored */
            /* Parent = this - 0x0C (CAesCipher is at parent+0x0C) */
            DWORD parent = ecx - 0x0C;
            printf("  Parent object dump around key/IV storage:\n");
            hexdump("  parent+0x460", (BYTE *)(parent + 0x460), 16);
            hexdump("  parent+0x470", (BYTE *)(parent + 0x470), 16);

            /* Restore original byte, single-step past it */
            *(BYTE *)g_setkey_addr = g_setkey_orig;
            ep->ContextRecord->EFlags |= 0x100; /* TF */
            g_single_step_from_setkey = TRUE;
            return EXCEPTION_CONTINUE_EXECUTION;
        }

        if (eip == g_setiv_addr) {
            g_setiv_count++;
            DWORD ecx = ep->ContextRecord->Ecx;
            DWORD esp = ep->ContextRecord->Esp;
            DWORD iv_ptr = *(DWORD *)(esp + 4);

            printf("\n=== [HOOK] SetIV #%d ===\n", g_setiv_count);
            printf("  this=0x%08X iv_ptr=0x%08X\n", ecx, iv_ptr);
            if (iv_ptr) {
                hexdump("AES IV ", (BYTE *)iv_ptr, 16);
                printf("\n  *** THIS IS THE DERIVED IV - XOR with iv_base to get iv_context! ***\n");
            } else {
                printf("  IV ptr is NULL (zeroing IV)\n");
            }

            *(BYTE *)g_setiv_addr = g_setiv_orig;
            ep->ContextRecord->EFlags |= 0x100;
            g_single_step_from_setiv = TRUE;
            return EXCEPTION_CONTINUE_EXECUTION;
        }

        if (eip == g_encrypt_addr) {
            g_encrypt_count++;
            DWORD ecx = ep->ContextRecord->Ecx;
            DWORD esp = ep->ContextRecord->Esp;
            DWORD buf_ptr = *(DWORD *)(esp + 4);
            DWORD buf_size = *(DWORD *)(esp + 8);

            printf("\n=== [HOOK] EncryptMultiBlock #%d ===\n", g_encrypt_count);
            printf("  this=0x%08X buf=0x%08X size=%u\n", ecx, buf_ptr, buf_size);
            if (buf_ptr && buf_size > 0 && buf_size <= 2048) {
                int dump_len = buf_size < 64 ? buf_size : 64;
                hexdump("PLAINTEXT (first 64)", (BYTE *)buf_ptr, dump_len);
            }

            *(BYTE *)g_encrypt_addr = g_encrypt_orig;
            ep->ContextRecord->EFlags |= 0x100;
            g_single_step_from_encrypt = TRUE;
            return EXCEPTION_CONTINUE_EXECUTION;
        }
    }

    if (ep->ExceptionRecord->ExceptionCode == EXCEPTION_SINGLE_STEP) {
        DWORD old;
        if (g_single_step_from_setkey) {
            g_single_step_from_setkey = FALSE;
            VirtualProtect((void *)g_setkey_addr, 1, PAGE_EXECUTE_READWRITE, &old);
            *(BYTE *)g_setkey_addr = 0xCC;
            VirtualProtect((void *)g_setkey_addr, 1, old, &old);
            return EXCEPTION_CONTINUE_EXECUTION;
        }
        if (g_single_step_from_setiv) {
            g_single_step_from_setiv = FALSE;
            VirtualProtect((void *)g_setiv_addr, 1, PAGE_EXECUTE_READWRITE, &old);
            *(BYTE *)g_setiv_addr = 0xCC;
            VirtualProtect((void *)g_setiv_addr, 1, old, &old);
            return EXCEPTION_CONTINUE_EXECUTION;
        }
        if (g_single_step_from_encrypt) {
            g_single_step_from_encrypt = FALSE;
            VirtualProtect((void *)g_encrypt_addr, 1, PAGE_EXECUTE_READWRITE, &old);
            *(BYTE *)g_encrypt_addr = 0xCC;
            VirtualProtect((void *)g_encrypt_addr, 1, old, &old);
            return EXCEPTION_CONTINUE_EXECUTION;
        }
    }

    return EXCEPTION_CONTINUE_SEARCH;
}

/* ---- Set up breakpoints ---- */
static void install_hooks(DWORD base) {
    DWORD old;

    g_setkey_addr = base + 0x1D60;
    g_setiv_addr = base + 0x1010;
    g_encrypt_addr = base + 0x1E60;

    printf("[+] SetKey   at 0x%08X\n", g_setkey_addr);
    printf("[+] SetIV    at 0x%08X\n", g_setiv_addr);
    printf("[+] Encrypt  at 0x%08X\n", g_encrypt_addr);

    /* Save original bytes */
    VirtualProtect((void *)g_setkey_addr, 1, PAGE_EXECUTE_READWRITE, &old);
    g_setkey_orig = *(BYTE *)g_setkey_addr;
    *(BYTE *)g_setkey_addr = 0xCC;
    VirtualProtect((void *)g_setkey_addr, 1, old, &old);

    VirtualProtect((void *)g_setiv_addr, 1, PAGE_EXECUTE_READWRITE, &old);
    g_setiv_orig = *(BYTE *)g_setiv_addr;
    *(BYTE *)g_setiv_addr = 0xCC;
    VirtualProtect((void *)g_setiv_addr, 1, old, &old);

    VirtualProtect((void *)g_encrypt_addr, 1, PAGE_EXECUTE_READWRITE, &old);
    g_encrypt_orig = *(BYTE *)g_encrypt_addr;
    *(BYTE *)g_encrypt_addr = 0xCC;
    VirtualProtect((void *)g_encrypt_addr, 1, old, &old);

    printf("[+] Breakpoints installed (SetKey=0x%02X SetIV=0x%02X Encrypt=0x%02X)\n",
           g_setkey_orig, g_setiv_orig, g_encrypt_orig);
}

/* ---- Bypass VAIO check by hooking lstrcmpW ---- */
/* rmp_dll.DLL hooks lstrcmpW to make "Sony Corporation" check pass.
 * We do the same thing inline: patch lstrcmpW to jump to our handler. */

static BYTE g_lstrcmpw_orig[8];
static DWORD g_lstrcmpw_addr;
static DWORD g_lstrcmpw_trampoline;

/* Our replacement lstrcmpW - returns 0 for Sony checks, calls original for others */
static int __stdcall my_lstrcmpW(const wchar_t *s1, const wchar_t *s2) {
    /* Check if either string contains "Sony" */
    const wchar_t sony[] = L"Sony";
    int is_sony = 0;

    if (s1 && s2) {
        /* Simple substring check */
        for (const wchar_t *p = s1; *p; p++) {
            if (p[0] == 'S' && p[1] == 'o' && p[2] == 'n' && p[3] == 'y') {
                is_sony = 1;
                break;
            }
        }
        if (!is_sony) {
            for (const wchar_t *p = s2; *p; p++) {
                if (p[0] == 'S' && p[1] == 'o' && p[2] == 'n' && p[3] == 'y') {
                    is_sony = 1;
                    break;
                }
            }
        }
    }

    if (is_sony) {
        printf("[BYPASS] lstrcmpW Sony check intercepted - returning match\n");
        return 0; /* Match */
    }

    /* Call original for non-Sony comparisons */
    /* Restore original, call, re-patch */
    DWORD old;
    VirtualProtect((void *)g_lstrcmpw_addr, 8, PAGE_EXECUTE_READWRITE, &old);
    memcpy((void *)g_lstrcmpw_addr, g_lstrcmpw_orig, 8);
    VirtualProtect((void *)g_lstrcmpw_addr, 8, old, &old);

    int result = lstrcmpW(s1, s2);

    /* Re-patch */
    VirtualProtect((void *)g_lstrcmpw_addr, 8, PAGE_EXECUTE_READWRITE, &old);
    BYTE jmp_patch[5];
    jmp_patch[0] = 0xE9;
    *(DWORD *)(jmp_patch + 1) = (DWORD)my_lstrcmpW - g_lstrcmpw_addr - 5;
    memcpy((void *)g_lstrcmpw_addr, jmp_patch, 5);
    VirtualProtect((void *)g_lstrcmpw_addr, 8, old, &old);

    return result;
}

static void install_vaio_bypass(void) {
    HMODULE hKernel = GetModuleHandleA("kernelbase.dll");
    if (!hKernel) hKernel = GetModuleHandleA("kernel32.dll");

    FARPROC pLstrcmpW = GetProcAddress(hKernel, "lstrcmpW");
    if (!pLstrcmpW) {
        printf("[-] lstrcmpW not found\n");
        return;
    }

    g_lstrcmpw_addr = (DWORD)pLstrcmpW;
    printf("[+] lstrcmpW at 0x%08X - installing VAIO bypass\n", g_lstrcmpw_addr);

    DWORD old;
    VirtualProtect((void *)g_lstrcmpw_addr, 8, PAGE_EXECUTE_READWRITE, &old);
    memcpy(g_lstrcmpw_orig, (void *)g_lstrcmpw_addr, 8);

    BYTE jmp_patch[5];
    jmp_patch[0] = 0xE9;
    *(DWORD *)(jmp_patch + 1) = (DWORD)my_lstrcmpW - g_lstrcmpw_addr - 5;
    memcpy((void *)g_lstrcmpw_addr, jmp_patch, 5);

    VirtualProtect((void *)g_lstrcmpw_addr, 8, old, &old);
    printf("[+] VAIO hardware bypass installed\n");
}

/* ---- COM Interface ---- */
/* CLSID: {1BD8D6AE-6EE1-42D9-A307-252FFAD207AD} */
DEFINE_GUID(CLSID_CoreInterface,
    0x1BD8D6AE, 0x6EE1, 0x42D9, 0xA3, 0x07, 0x25, 0x2F, 0xFA, 0xD2, 0x07, 0xAD);

/* IID for IClassFactory: {00000001-0000-0000-C000-000000000046} */
DEFINE_GUID(IID_IClassFactory_,
    0x00000001, 0x0000, 0x0000, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46);

/* IID for IUnknown: {00000000-0000-0000-C000-000000000046} */
DEFINE_GUID(IID_IUnknown_,
    0x00000000, 0x0000, 0x0000, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46);

/* COM vtable: call method at offset using stdcall */
typedef HRESULT (__stdcall *VtableMethod0)(void *this_);
typedef HRESULT (__stdcall *VtableMethod1)(void *this_, void *param1);
typedef HRESULT (__stdcall *VtableMethod2)(void *this_, void *p1, void *p2);
typedef HRESULT (__stdcall *VtableMethod4)(void *this_, void *p1, void *p2, void *p3, void *p4);
typedef HRESULT (__stdcall *VtableMethod5)(void *this_, void *p1, void *p2, void *p3, void *p4, void *p5);

static void *call_vtable(void *obj, int offset) {
    DWORD *vtable = *(DWORD **)obj;
    return (void *)vtable[offset / 4];
}

int main(int argc, char **argv) {
    const char *pin_str = "12345678"; /* Default test PIN */
    if (argc > 1) pin_str = argv[1];

    printf("╔══════════════════════════════════════════════════╗\n");
    printf("║  VRPSDK Registration Hook Tool                   ║\n");
    printf("║  Captures AES Key + IV during registration       ║\n");
    printf("╚══════════════════════════════════════════════════╝\n\n");
    printf("[*] PIN: %s\n", pin_str);

    /* Open log file */
    g_log = fopen("Z:\\Users\\mihailurmanschi\\Work\\PsOldRemotePlay\\research\\tools\\aes_hook_log.txt", "w");
    if (g_log) {
        fprintf(g_log, "=== VRPSDK Registration Hook Log ===\n");
        fprintf(g_log, "PIN: %s\n\n", pin_str);
    }

    /* Step 1: Register VEH (before loading anything) */
    printf("[1] Registering VEH handler...\n");
    AddVectoredExceptionHandler(1, veh_handler);

    /* Step 2: Install VAIO bypass (hook lstrcmpW) */
    printf("[2] Installing VAIO hardware bypass...\n");
    install_vaio_bypass();

    /* Step 3: Load VRPSDK.dll */
    const char *dll_path = "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPSDK.dll";
    printf("[3] Loading VRPSDK.dll (Themida will unpack)...\n");
    HMODULE hVRPSDK = LoadLibraryA(dll_path);
    if (!hVRPSDK) {
        printf("[-] LoadLibrary failed: %lu\n", GetLastError());
        return 1;
    }

    g_vrpsdk_base = (DWORD)hVRPSDK;
    printf("[+] VRPSDK.dll loaded at 0x%08X\n", g_vrpsdk_base);

    /* Step 4: Install AES function hooks */
    printf("[4] Installing AES hooks...\n");
    install_hooks(g_vrpsdk_base);

    /* Step 5: Create COM object via DllGetClassObject */
    printf("[5] Creating COM object...\n");

    typedef HRESULT (__stdcall *DllGetClassObjectFunc)(REFCLSID, REFIID, void **);
    DllGetClassObjectFunc pDllGetClassObject =
        (DllGetClassObjectFunc)GetProcAddress(hVRPSDK, "DllGetClassObject");

    if (!pDllGetClassObject) {
        printf("[-] DllGetClassObject not found\n");
        return 1;
    }
    printf("[+] DllGetClassObject at 0x%p\n", pDllGetClassObject);

    void *pFactory = NULL;
    HRESULT hr = pDllGetClassObject(&CLSID_CoreInterface, &IID_IClassFactory_, &pFactory);
    if (FAILED(hr)) {
        printf("[-] DllGetClassObject failed: 0x%08X\n", (unsigned)hr);
        return 1;
    }
    printf("[+] Got IClassFactory at %p\n", pFactory);

    /* CreateInstance(NULL, IID_IUnknown, &pInterface) */
    void *pInterface = NULL;
    VtableMethod4 pCreateInstance = (VtableMethod4)call_vtable(pFactory, 0x0C);
    hr = pCreateInstance(pFactory, NULL, (void *)&IID_IUnknown_, &pInterface, NULL);
    if (FAILED(hr)) {
        printf("[-] CreateInstance failed: 0x%08X\n", (unsigned)hr);
        /* Try with IID_NULL */
        GUID iid_zero = {0};
        hr = pCreateInstance(pFactory, NULL, (void *)&iid_zero, &pInterface, NULL);
        if (FAILED(hr)) {
            printf("[-] CreateInstance (IID_NULL) also failed: 0x%08X\n", (unsigned)hr);
            return 1;
        }
    }
    printf("[+] Got interface at %p\n", pInterface);

    /* Step 6: Call Initialize (vtable offset 0x1c) */
    printf("[6] Calling Initialize...\n");
    printf("    (VAIO bypass should intercept hardware check)\n");

    /* Initialize takes (iface, BSTR1, BSTR2) */
    /* From VRP.exe: passes executable path and some config string */
    BSTR bstr_empty = SysAllocString(L"");
    BSTR bstr_path = SysAllocString(L"C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3");

    VtableMethod2 pInit = (VtableMethod2)call_vtable(pInterface, 0x1C);
    printf("[*] Calling vtable[0x1C] (Init)...\n");
    hr = pInit(pInterface, bstr_path, bstr_empty);
    printf("[*] Init returned: 0x%08X (%s)\n", (unsigned)hr,
           SUCCEEDED(hr) ? "SUCCESS" : "FAILED");

    if (FAILED(hr)) {
        printf("[!] Init failed with 0x%08X - patching VAIO check branches...\n", (unsigned)hr);
        DWORD *vtable = *(DWORD **)pInterface;
        DWORD init_func = vtable[0x1C / 4];
        printf("    Initialize func at 0x%08X\n", init_func);

        DWORD old_prot;
        BYTE *code = (BYTE *)init_func;

        /* From disassembly analysis of Initialize:
         * Offset 0x6F: 74 07 = jz +7 (skip error if VAIO check returned 0)
         *   → Change to EB 07 = jmp +7 (always skip error)
         * This is the VAIO hardware check branch.
         *
         * Also patch ALL "mov ebx, 0x8004Axxx" patterns (BB xx A0 04 80)
         * to "mov ebx, 0" so any remaining error returns become success.
         */

        /* Patch the entire function region (2KB should be enough) */
        VirtualProtect((void *)init_func, 4096, PAGE_EXECUTE_READWRITE, &old_prot);

        /* From hex dump analysis:
         * +0x50: 85 C0 75 11 = test eax,eax; jnz +0x11 (first sub-check)
         * +0x6C: 74 07 = jz +7 (VAIO check - skip error if returned 0)
         *
         * Fix offset 0x6C: 74 -> EB (jz to jmp, always take success path)
         * Also fix offset 0x52 if needed: ensure first check doesn't block */
        if (code[0x6C] == 0x74 && code[0x6D] == 0x07) {
            printf("    Patching VAIO check at func+0x6C: jz -> jmp (always take success path)\n");
            code[0x6C] = 0xEB; /* jz -> jmp unconditional */
        } else {
            printf("    WARN: Expected 74 07 at +0x6C, found %02X %02X\n",
                   code[0x6C], code[0x6D]);
            /* Try to find 74 07 BB near the expected location */
            for (int i = 0x60; i < 0x80; i++) {
                if (code[i] == 0x74 && code[i+1] == 0x07 && code[i+2] == 0xBB) {
                    printf("    Found jz+7;mov ebx at func+0x%X - patching\n", i);
                    code[i] = 0xEB;
                    break;
                }
            }
        }

        /* Patch ALL "BB xx A0 04 80" (mov ebx, 0x8004A0xx) patterns */
        int patched = 0;
        for (int i = 0; i < 2048 - 5; i++) {
            if (code[i] == 0xBB &&
                code[i+2] == 0xA0 && code[i+3] == 0x04 && code[i+4] == 0x80) {
                printf("    Patching mov ebx, 0x8004A0%02X at func+0x%X -> mov ebx, 0\n",
                       code[i+1], i);
                code[i+1] = 0x00; code[i+2] = 0x00;
                code[i+3] = 0x00; code[i+4] = 0x00;
                patched++;
            }
        }

        /* Also patch "B8 xx A0 04 80" (mov eax, 0x8004A0xx) */
        for (int i = 0; i < 2048 - 5; i++) {
            if (code[i] == 0xB8 &&
                code[i+2] == 0xA0 && code[i+3] == 0x04 && code[i+4] == 0x80) {
                printf("    Patching mov eax, 0x8004A0%02X at func+0x%X -> mov eax, 0\n",
                       code[i+1], i);
                code[i+1] = 0x00; code[i+2] = 0x00;
                code[i+3] = 0x00; code[i+4] = 0x00;
                patched++;
            }
        }

        VirtualProtect((void *)init_func, 4096, old_prot, &old_prot);
        printf("    Patched %d error returns + VAIO jz->jmp\n", patched);

        printf("[*] Retrying Init with all patches...\n");
        hr = pInit(pInterface, bstr_path, bstr_empty);
        printf("[*] Init returned: 0x%08X (%s)\n", (unsigned)hr,
               SUCCEEDED(hr) ? "SUCCESS" : "FAILED");

        /* If still failing, do a broader scan of the entire code section */
        if (FAILED(hr)) {
            printf("[!] Still failing. Scanning entire code section for error patterns...\n");
            DWORD code_base = g_vrpsdk_base + 0x1000;
            DWORD code_size = 0x27000;
            VirtualProtect((void *)code_base, code_size, PAGE_EXECUTE_READWRITE, &old_prot);

            BYTE *section = (BYTE *)code_base;
            int total = 0;
            for (DWORD i = 0; i < code_size - 5; i++) {
                /* BB xx A0 04 80 or B8 xx A0 04 80 */
                if ((section[i] == 0xBB || section[i] == 0xB8) &&
                    section[i+2] == 0xA0 && section[i+3] == 0x04 && section[i+4] == 0x80) {
                    section[i+1] = 0x00; section[i+2] = 0x00;
                    section[i+3] = 0x00; section[i+4] = 0x00;
                    total++;
                }
            }

            VirtualProtect((void *)code_base, code_size, old_prot, &old_prot);
            printf("    Zeroed %d error codes in code section\n", total);

            hr = pInit(pInterface, bstr_path, bstr_empty);
            printf("[*] Init (broad patch) returned: 0x%08X (%s)\n", (unsigned)hr,
                   SUCCEEDED(hr) ? "SUCCESS" : "FAILED");
        }
    }

    /* Step 7: Call StartRegistration at CORRECT vtable offset
     * IDispatch confirmed dispid mapping:
     *   Initialize=1=slot7=0x1C, UnInit=2=slot8, StartReg=3=slot9=0x24
     *   CancelReg=4=slot10, StartRemotePlay=5=slot11=0x2C */
    printf("\n[7] Calling StartRegistration (vtable 0x24, slot 9)...\n");
    int pin_int = atoi(pin_str);
    DWORD status = 0;
    DWORD *vtab = *(DWORD **)pInterface;

    wchar_t pin_wide[16];
    MultiByteToWideChar(CP_ACP, 0, pin_str, -1, pin_wide, 16);
    BSTR bstr_pin = SysAllocString(pin_wide);
    BSTR bstr_ip = SysAllocString(L"192.168.1.1");
    BSTR bstr_nick = SysAllocString(L"PsOldRemotePlay");

    printf("    StartRegistration at 0x%08X\n", vtab[9]);
    int prev_key, prev_iv;

    /* Try via IDispatch::Invoke (safest - handles param marshaling) */
    printf("  IDispatch::Invoke(dispid=3, PIN=%s, type=2)...\n", pin_str);
    {
        typedef HRESULT (__stdcall *InvokeFunc)(void *, DISPID, void *, LCID,
                                                 WORD, DISPPARAMS *, VARIANT *, void *, void *);
        InvokeFunc pInvoke = (InvokeFunc)vtab[6];

        /* Args in REVERSE order for DISPPARAMS */
        VARIANT args[5];
        memset(args, 0, sizeof(args));
        args[0].vt = VT_BSTR; args[0].bstrVal = bstr_nick;
        args[1].vt = VT_BSTR; args[1].bstrVal = bstr_ip;
        args[2].vt = VT_I4;   args[2].lVal = 0;
        args[3].vt = VT_I4;   args[3].lVal = 2;
        args[4].vt = VT_BSTR; args[4].bstrVal = bstr_pin;

        DISPPARAMS dp = {0};
        dp.rgvarg = args;
        dp.cArgs = 5;
        VARIANT result; memset(&result, 0, sizeof(result));
        GUID iid_null = {0};

        prev_key = g_setkey_count; prev_iv = g_setiv_count;
        hr = pInvoke(pInterface, 3, &iid_null, 0x0409, DISPATCH_METHOD, &dp, &result, NULL, NULL);
        printf("  Result: 0x%08X", (unsigned)hr);
        if (g_setkey_count > prev_key) printf(" *** SetKey! ***");
        if (g_setiv_count > prev_iv)   printf(" *** SetIV! ***");
        printf("\n");

        /* Try with fewer args */
        dp.cArgs = 3;
        dp.rgvarg = args + 2;
        prev_key = g_setkey_count; prev_iv = g_setiv_count;
        hr = pInvoke(pInterface, 3, &iid_null, 0x0409, DISPATCH_METHOD, &dp, &result, NULL, NULL);
        printf("  Invoke(3 args): 0x%08X", (unsigned)hr);
        if (g_setkey_count > prev_key) printf(" *** SetKey! ***");
        if (g_setiv_count > prev_iv)   printf(" *** SetIV! ***");
        printf("\n");

        dp.cArgs = 1;
        dp.rgvarg = args + 4;
        prev_key = g_setkey_count; prev_iv = g_setiv_count;
        hr = pInvoke(pInterface, 3, &iid_null, 0x0409, DISPATCH_METHOD, &dp, &result, NULL, NULL);
        printf("  Invoke(1 arg=PIN): 0x%08X", (unsigned)hr);
        if (g_setkey_count > prev_key) printf(" *** SetKey! ***");
        if (g_setiv_count > prev_iv)   printf(" *** SetIV! ***");
        printf("\n");
    }

    /* Also try direct vtable call to slot 9 */
    printf("\n  Direct vtable slot 9 calls:\n");
    typedef HRESULT (__stdcall *RegFunc2)(void *, DWORD, DWORD);
    typedef HRESULT (__stdcall *RegFunc4)(void *, DWORD, DWORD, DWORD, DWORD);
    typedef HRESULT (__stdcall *RegFuncB)(void *, BSTR, DWORD, BSTR, DWORD);

    prev_key = g_setkey_count; prev_iv = g_setiv_count;
    hr = ((RegFunc4)vtab[9])(pInterface, (DWORD)pin_int, 2, (DWORD)bstr_ip, 0);
    printf("  (pin_int, 2, ip, 0): 0x%08X", (unsigned)hr);
    if (g_setkey_count > prev_key) printf(" ***KEY***");
    if (g_setiv_count > prev_iv) printf(" ***IV***");
    printf("\n");

    prev_key = g_setkey_count; prev_iv = g_setiv_count;
    hr = ((RegFuncB)vtab[9])(pInterface, bstr_pin, 2, bstr_ip, 0);
    printf("  (BSTR_pin, 2, ip, 0): 0x%08X", (unsigned)hr);
    if (g_setkey_count > prev_key) printf(" ***KEY***");
    if (g_setiv_count > prev_iv) printf(" ***IV***");
    printf("\n");

    SysFreeString(bstr_pin);
    SysFreeString(bstr_ip);
    SysFreeString(bstr_nick);

    /* Step 8: Summary */
    printf("\n═══════════════════════════════════════════\n");
    printf("  SUMMARY\n");
    printf("  SetKey calls:    %d\n", g_setkey_count);
    printf("  SetIV calls:     %d\n", g_setiv_count);
    printf("  Encrypt calls:   %d\n", g_encrypt_count);
    printf("═══════════════════════════════════════════\n");

    if (g_setiv_count > 0) {
        printf("\n  *** IV was captured! Check output above. ***\n");
        printf("  To recover iv_context:\n");
        printf("    iv_context = captured_IV XOR known_iv_base\n");
        printf("  Then use: python3 research/tools/ps3_register_bruteforce_iv.py --iv-context <value>\n");
    } else {
        printf("\n  No SetIV calls captured. Registration may not have triggered.\n");
        printf("  Try running with rmp_launcher instead, or check log for errors.\n");
    }

    /* Cleanup */
    SysFreeString(bstr_empty);
    SysFreeString(bstr_path);
    SysFreeString(bstr_pin);

    if (g_log) fclose(g_log);
    printf("\nDone.\n");
    return 0;
}
