/* dump_vrpsdk4.c - Dump code around registration, find key generation */
/* Compile: i686-w64-mingw32-gcc -o dump_vrpsdk4.exe dump_vrpsdk4.c */
/* Run: wine dump_vrpsdk4.exe */

#include <windows.h>
#include <stdio.h>
#include <string.h>

/* Hook CryptEncrypt to intercept AES encryption */
/* Since VRPSDK uses its own AES, we need to hook the AES function directly */

/* AES S-box location = VRPSDK + 0x27010 */
/* Let's find what code references the S-box and key table */

int main(void) {
    const char *dll_path = "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPSDK.dll";
    const char *out_base = "Z:\\Users\\mihailurmanschi\\Work\\PsOldRemotePlay\\research\\tools\\";

    printf("Loading VRPSDK.dll...\n");
    HMODULE hMod = LoadLibraryA(dll_path);
    if (!hMod) {
        printf("LoadLibrary failed: %lu\n", GetLastError());
        return 1;
    }

    IMAGE_DOS_HEADER *dos = (IMAGE_DOS_HEADER *)hMod;
    IMAGE_NT_HEADERS *nt = (IMAGE_NT_HEADERS *)((char *)hMod + dos->e_lfanew);
    DWORD image_size = nt->OptionalHeader.SizeOfImage;
    unsigned char *base = (unsigned char *)hMod;
    DWORD base_addr = (DWORD)hMod;

    printf("VRPSDK.dll at 0x%08lX, size 0x%lX\n", base_addr, image_size);

    /* The S-box is at base+0x27010, which is VA 0x10027010 */
    /* Find code that references this address (as a 32-bit immediate) */
    DWORD sbox_va = base_addr + 0x27010;
    DWORD pc_nonce_va = base_addr + 0x273C8;

    printf("\n=== Finding code references to AES S-box (0x%08lX) ===\n", sbox_va);
    unsigned char sbox_bytes[4];
    memcpy(sbox_bytes, &sbox_va, 4);

    int ref_count = 0;
    for (DWORD i = 0; i + 4 <= image_size; i++) {
        if (memcmp(base + i, sbox_bytes, 4) == 0) {
            printf("  Ref at VRPSDK+0x%lX (VA 0x%08lX) - preceding opcode: %02X\n",
                   i, base_addr + i, (i > 0) ? base[i-1] : 0);
            ref_count++;
            if (ref_count > 20) { printf("  ... (more)\n"); break; }
        }
    }

    printf("\n=== Finding code references to PC_NONCE (0x%08lX) ===\n", pc_nonce_va);
    unsigned char nonce_bytes[4];
    memcpy(nonce_bytes, &pc_nonce_va, 4);

    for (DWORD i = 0; i + 4 <= image_size; i++) {
        if (memcmp(base + i, nonce_bytes, 4) == 0) {
            printf("  Ref at VRPSDK+0x%lX (VA 0x%08lX)\n", i, base_addr + i);
            /* Show surrounding code */
            DWORD start = (i > 32) ? i - 32 : 0;
            printf("    Context: ");
            for (DWORD j = start; j < i + 16 && j < image_size; j++) {
                printf("%02X ", base[j]);
            }
            printf("\n");
        }
    }

    /* Find references to "POST /sce/premo/regist" string (at VRPSDK+0x1FD08) */
    DWORD regist_str_va = base_addr + 0x1FD08;
    printf("\n=== Finding code references to regist string (0x%08lX) ===\n", regist_str_va);
    unsigned char regist_bytes[4];
    memcpy(regist_bytes, &regist_str_va, 4);

    for (DWORD i = 0; i + 4 <= image_size; i++) {
        if (memcmp(base + i, regist_bytes, 4) == 0) {
            printf("  Ref at VRPSDK+0x%lX (VA 0x%08lX)\n", i, base_addr + i);
            /* Show function context (look back for function prologue) */
            DWORD start = (i > 64) ? i - 64 : 0;
            DWORD end = (i + 64 < image_size) ? i + 64 : image_size;
            printf("    Code bytes: ");
            for (DWORD j = start; j < end; j++) printf("%02X ", base[j]);
            printf("\n");
        }
    }

    /* Find references to "Client-Type" string (at VRPSDK+0x1FCC0) */
    DWORD client_type_va = base_addr + 0x1FCC0;
    printf("\n=== Finding code refs to Client-Type (0x%08lX) ===\n", client_type_va);
    unsigned char ct_bytes[4];
    memcpy(ct_bytes, &client_type_va, 4);

    for (DWORD i = 0; i + 4 <= image_size; i++) {
        if (memcmp(base + i, ct_bytes, 4) == 0) {
            printf("  Ref at VRPSDK+0x%lX\n", i);
        }
    }

    /* Dump the entire first code section to a file for analysis */
    printf("\n=== Dumping code section ===\n");
    IMAGE_SECTION_HEADER *sec = IMAGE_FIRST_SECTION(nt);
    for (int i = 0; i < nt->FileHeader.NumberOfSections; i++) {
        printf("  Section %d '%.8s': RVA 0x%lX, VSize 0x%lX, Chars 0x%lX\n",
               i, sec[i].Name, sec[i].VirtualAddress, sec[i].Misc.VirtualSize,
               sec[i].Characteristics);
    }

    /* Dump first section (code) which contains unpacked Themida code */
    char outfile[512];
    sprintf(outfile, "%svrpsdk_code.bin", out_base);
    FILE *f = fopen(outfile, "wb");
    if (f) {
        DWORD code_start = sec[0].VirtualAddress;
        DWORD code_size = sec[0].Misc.VirtualSize;
        fwrite(base + code_start, 1, code_size, f);
        fclose(f);
        printf("  Dumped code section: offset 0x%lX, size 0x%lX\n", code_start, code_size);
    }

    /* Search for the pattern that loads/uses keys in registration:
       The registration code likely:
       1. Gets the PC_XOR key
       2. Gets the PC_IV base key
       3. Computes the actual AES key and IV
       4. Calls the AES encrypt function

       Since the S-box is at 0x27010, the AES encrypt function is code that
       references 0x10027010. Let's find those functions. */

    printf("\n=== AES function analysis ===\n");
    printf("Looking for functions that reference S-box...\n");

    /* Find all refs and group by function (look for nearest preceding 0x55 = push ebp) */
    for (DWORD i = 0; i + 4 <= image_size; i++) {
        if (memcmp(base + i, sbox_bytes, 4) == 0) {
            /* Walk backwards to find function start (push ebp = 0x55, mov ebp,esp = 8B EC) */
            DWORD func_start = 0;
            for (DWORD j = i; j > 0 && j > i - 512; j--) {
                if (base[j] == 0x55 && j+2 < image_size && base[j+1] == 0x8B && base[j+2] == 0xEC) {
                    func_start = j;
                    break;
                }
            }
            if (func_start) {
                printf("  S-box ref at +0x%lX, in function starting at +0x%lX (VA 0x%08lX)\n",
                       i, func_start, base_addr + func_start);
            }
        }
    }

    /* Look for XMBC-style key processing - immediate byte values that match keys */
    /* Search for push 0x6B706DEC (PC_XOR first dword, little-endian) */
    unsigned char push_imm32 = 0x68;
    DWORD pc_xor_dword = 0x6B706DEC;
    unsigned char pc_xor_push[5];
    pc_xor_push[0] = 0x68;
    memcpy(pc_xor_push + 1, &pc_xor_dword, 4);

    printf("\n=== Looking for 'push 0x6B706DEC' (PC_XOR dword) ===\n");
    for (DWORD i = 0; i + 5 <= image_size; i++) {
        if (memcmp(base + i, pc_xor_push, 5) == 0) {
            printf("  Found at VRPSDK+0x%lX\n", i);
        }
    }

    /* Also try mov [reg], immediate patterns */
    printf("\n=== Looking for PC_XOR as mov immediate ===\n");
    unsigned char pc_xor_le[4];
    memcpy(pc_xor_le, &pc_xor_dword, 4);
    for (DWORD i = 0; i + 4 <= image_size; i++) {
        if (memcmp(base + i, pc_xor_le, 4) == 0) {
            /* Check preceding byte for mov opcodes: C7, 89, etc */
            if (i > 0) {
                unsigned char prev = base[i-1];
                unsigned char prev2 = (i > 1) ? base[i-2] : 0;
                if (prev == 0x68 || prev2 == 0xC7 || prev2 == 0x89) {
                    printf("  Found at VRPSDK+0x%lX (preceding: %02X %02X)\n",
                           i, prev2, prev);
                }
            }
        }
    }

    printf("\nDone.\n");
    return 0;
}
