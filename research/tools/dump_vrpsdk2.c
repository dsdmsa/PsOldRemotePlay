/* dump_vrpsdk2.c - Dump key name strings and trace registration code */
/* Compile: i686-w64-mingw32-gcc -o dump_vrpsdk2.exe dump_vrpsdk2.c */
/* Run: wine dump_vrpsdk2.exe */

#include <windows.h>
#include <stdio.h>
#include <string.h>

int main(void) {
    const char *dll_path = "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPSDK.dll";
    const char *out_path = "Z:\\Users\\mihailurmanschi\\Work\\PsOldRemotePlay\\research\\tools\\";

    printf("Loading VRPSDK.dll...\n");
    HMODULE hMod = LoadLibraryA(dll_path);
    if (!hMod) {
        printf("LoadLibrary failed: %lu\n", GetLastError());
        return 1;
    }

    /* Dump the key name strings at 0x00405000 */
    printf("\n=== Key name strings at 0x00405000+ ===\n");
    unsigned char *p = (unsigned char *)0x00405000;
    MEMORY_BASIC_INFORMATION mbi;
    if (VirtualQuery(p, &mbi, sizeof(mbi)) && mbi.State == MEM_COMMIT) {
        /* Dump 256 bytes as hex + ascii */
        for (int i = 0; i < 256; i += 16) {
            printf("  0x%08X: ", (unsigned)(size_t)(p + i));
            for (int j = 0; j < 16; j++) printf("%02X ", p[i+j]);
            printf(" |");
            for (int j = 0; j < 16; j++) {
                char c = p[i+j];
                printf("%c", (c >= 32 && c < 127) ? c : '.');
            }
            printf("|\n");
        }
    } else {
        printf("  Cannot read 0x00405000\n");
    }

    /* Dump the full key table at 0x00404000 */
    printf("\n=== Full key table at 0x00404000 ===\n");
    p = (unsigned char *)0x00404000;
    if (VirtualQuery(p, &mbi, sizeof(mbi)) && mbi.State == MEM_COMMIT) {
        for (int i = 0; i < 512; i += 16) {
            printf("  0x%08X: ", (unsigned)(size_t)(p + i));
            for (int j = 0; j < 16; j++) printf("%02X ", p[i+j]);
            printf(" |");
            for (int j = 0; j < 16; j++) {
                char c = p[i+j];
                printf("%c", (c >= 32 && c < 127) ? c : '.');
            }
            printf("|\n");
        }
    }

    /* Now dump the ENTIRE process data segment around 0x00400000-0x00410000 */
    /* This should contain the unpacked Themida code's global data */
    printf("\n=== Dumping 0x00400000-0x00410000 to process_data.bin ===\n");
    char outfile[512];
    sprintf(outfile, "%sprocess_data.bin", out_path);
    FILE *f = fopen(outfile, "wb");
    if (f) {
        for (unsigned addr = 0x00400000; addr < 0x00410000; addr += 4096) {
            if (VirtualQuery((void*)addr, &mbi, sizeof(mbi)) &&
                mbi.State == MEM_COMMIT &&
                !(mbi.Protect & (PAGE_NOACCESS | PAGE_GUARD))) {
                fwrite((void*)addr, 1, 4096, f);
            } else {
                char zeros[4096] = {0};
                fwrite(zeros, 1, 4096, f);
            }
        }
        fclose(f);
        printf("  Done.\n");
    }

    /* Search for "pin" and "PIN" strings in the dump around the registration code */
    printf("\n=== Searching for PIN/pin/context/iv references ===\n");
    IMAGE_DOS_HEADER *dos = (IMAGE_DOS_HEADER *)hMod;
    IMAGE_NT_HEADERS *nt = (IMAGE_NT_HEADERS *)((char *)hMod + dos->e_lfanew);
    DWORD image_size = nt->OptionalHeader.SizeOfImage;
    unsigned char *base = (unsigned char *)hMod;

    const char *search_terms[] = {"pin", "PIN", "context", "iv_ctx", "regist_iv",
                                   "pkey", "PKEY", "wpa_key", "WPA", "passphrase",
                                   "PIN_CODE", "pin_code", "regist_key", "reg_key", NULL};

    for (int t = 0; search_terms[t]; t++) {
        int slen = strlen(search_terms[t]);
        for (DWORD i = 0; i + slen < image_size; i++) {
            if (memcmp(base + i, search_terms[t], slen) == 0) {
                /* Verify it's a plausible string (surrounded by printable or null) */
                if (i > 0 && base[i-1] != 0 && (base[i-1] < 32 || base[i-1] > 126)) continue;
                char after = base[i + slen];
                if (after != 0 && (after < 32 || after > 126)) continue;

                /* Print context */
                DWORD start = (i > 32) ? i - 32 : 0;
                DWORD end = (i + slen + 64 < image_size) ? i + slen + 64 : image_size;
                printf("  '%s' at VRPSDK+0x%lX: ", search_terms[t], i);
                for (DWORD j = start; j < end; j++) {
                    char c = base[j];
                    printf("%c", (c >= 32 && c < 127) ? c : '.');
                }
                printf("\n");
            }
        }
    }

    /* Look for the registration body format string - how is the encrypted body built? */
    printf("\n=== Registration-related format strings ===\n");
    const char *reg_terms[] = {"Client-Type", "Client-Id", "Client-Mac", "Content-Length",
                               "regist", "PREMO-Key", "encrypt", NULL};
    for (int t = 0; reg_terms[t]; t++) {
        int slen = strlen(reg_terms[t]);
        for (DWORD i = 0; i + slen < image_size; i++) {
            if (memcmp(base + i, reg_terms[t], slen) == 0) {
                DWORD start = (i > 8) ? i - 8 : 0;
                DWORD end = (i + slen + 80 < image_size) ? i + slen + 80 : image_size;
                printf("  0x%lX: ", i);
                for (DWORD j = start; j < end; j++) {
                    char c = base[j];
                    printf("%c", (c >= 32 && c < 127) ? c : '.');
                }
                printf("\n");
            }
        }
    }

    printf("\nDone.\n");
    return 0;
}
