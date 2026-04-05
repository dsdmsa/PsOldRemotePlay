/* dump_vrpsdk3.c - Find AES S-box and trace encryption in VRPSDK.dll */
/* Also scan process memory EXCLUDING our own module */
/* Compile: i686-w64-mingw32-gcc -o dump_vrpsdk3.exe dump_vrpsdk3.c */
/* Run: wine dump_vrpsdk3.exe */

#include <windows.h>
#include <stdio.h>
#include <string.h>

/* AES S-box (first 32 bytes are enough to uniquely identify) */
static const unsigned char aes_sbox_start[32] = {
    0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5,
    0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76,
    0xCA, 0x82, 0xC9, 0x7D, 0xFA, 0x59, 0x47, 0xF0,
    0xAD, 0xD4, 0xA2, 0xAF, 0x9C, 0xA4, 0x72, 0xC0
};

/* Inverse AES S-box (first 32 bytes) */
static const unsigned char aes_inv_sbox_start[32] = {
    0x52, 0x09, 0x6A, 0xD5, 0x30, 0x36, 0xA5, 0x38,
    0xBF, 0x40, 0xA3, 0x9E, 0x81, 0xF3, 0xD7, 0xFB,
    0x7C, 0xE3, 0x39, 0x82, 0x9B, 0x2F, 0xFF, 0x87,
    0x34, 0x8E, 0x43, 0x44, 0xC4, 0xDE, 0xE9, 0xCB
};

/* Known keys to verify (16 bytes each) */
static const unsigned char PC_XOR[16] = {0xEC,0x6D,0x70,0x6B,0x1E,0x0A,0x9A,0x75,0x8C,0xDA,0x78,0x27,0x51,0xA3,0xC3,0x7B};
static const unsigned char SKEY0[16] = {0xD1,0xB2,0x12,0xEB,0x73,0x86,0x6C,0x7B,0x12,0xA7,0x5E,0x0C,0x04,0xC6,0xB8,0x91};
static const unsigned char PC_NONCE[16] = {0x33,0x72,0x84,0x4C,0xA6,0x6F,0x2E,0x2B,0x20,0xC7,0x90,0x60,0x33,0xE8,0x29,0xC6};

int main(void) {
    const char *dll_path = "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPSDK.dll";

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
    HMODULE hSelf = GetModuleHandleA(NULL);

    printf("VRPSDK.dll at 0x%p, size 0x%lX\n", hMod, image_size);
    printf("Our EXE at 0x%p\n", hSelf);

    /* 1. Search for AES S-box in VRPSDK.dll */
    printf("\n=== Searching VRPSDK.dll for AES S-box ===\n");
    for (DWORD i = 0; i + 256 <= image_size; i++) {
        if (memcmp(base + i, aes_sbox_start, 32) == 0) {
            printf("  FOUND AES S-box at VRPSDK+0x%lX (VA 0x%p)\n", i, base+i);
            /* Show surrounding area */
            printf("  Preceding 32 bytes:\n    ");
            DWORD start = (i > 32) ? i - 32 : 0;
            for (DWORD j = start; j < i; j++) printf("%02X ", base[j]);
            printf("\n  Following 256 bytes (full S-box):\n    ");
            for (int j = 0; j < 256; j++) {
                printf("%02X ", base[i+j]);
                if ((j+1) % 16 == 0) printf("\n    ");
            }
        }
        if (memcmp(base + i, aes_inv_sbox_start, 32) == 0) {
            printf("  FOUND AES Inverse S-box at VRPSDK+0x%lX\n", i);
        }
    }

    /* 2. Search ALL loaded DLLs for AES S-box */
    printf("\n=== Searching all loaded modules for AES S-box ===\n");
    {
        const char *dlls[] = {
            "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\sonyjvtd.dll",
            "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\UFCore.dll",
            "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPMFMGR.dll",
            "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPMapping.dll",
            NULL
        };
        for (int d = 0; dlls[d]; d++) {
            HMODULE h = LoadLibraryA(dlls[d]);
            if (!h) continue;
            IMAGE_DOS_HEADER *d2 = (IMAGE_DOS_HEADER *)h;
            IMAGE_NT_HEADERS *n2 = (IMAGE_NT_HEADERS *)((char *)h + d2->e_lfanew);
            DWORD sz = n2->OptionalHeader.SizeOfImage;
            unsigned char *b = (unsigned char *)h;
            const char *name = strrchr(dlls[d], '\\');
            name = name ? name+1 : dlls[d];
            for (DWORD i = 0; i + 256 <= sz; i++) {
                if (memcmp(b + i, aes_sbox_start, 32) == 0) {
                    printf("  FOUND AES S-box in %s at offset 0x%lX\n", name, i);
                }
                if (memcmp(b + i, aes_inv_sbox_start, 32) == 0) {
                    printf("  FOUND AES Inv S-box in %s at offset 0x%lX\n", name, i);
                }
            }
        }
    }

    /* 3. Scan entire process memory for all 16-byte keys, SKIPPING our own module */
    printf("\n=== Scanning process memory (excluding our EXE) ===\n");
    {
        IMAGE_DOS_HEADER *sd = (IMAGE_DOS_HEADER *)hSelf;
        IMAGE_NT_HEADERS *sn = (IMAGE_NT_HEADERS *)((char *)hSelf + sd->e_lfanew);
        DWORD self_size = sn->OptionalHeader.SizeOfImage;
        unsigned char *self_start = (unsigned char *)hSelf;
        unsigned char *self_end = self_start + self_size;

        MEMORY_BASIC_INFORMATION mbi;
        unsigned char *addr = NULL;
        struct { const char *name; const unsigned char *data; } keys[] = {
            {"PC_XOR", PC_XOR}, {"SKEY0", SKEY0}, {"PC_NONCE", PC_NONCE}
        };

        while (VirtualQuery(addr, &mbi, sizeof(mbi))) {
            if (mbi.State == MEM_COMMIT &&
                !(mbi.Protect & (PAGE_NOACCESS | PAGE_GUARD)) &&
                (mbi.Protect & (PAGE_READONLY | PAGE_READWRITE | PAGE_EXECUTE_READ |
                                PAGE_EXECUTE_READWRITE | PAGE_WRITECOPY | PAGE_EXECUTE_WRITECOPY))) {
                unsigned char *region_start = (unsigned char *)mbi.BaseAddress;
                unsigned char *region_end = region_start + mbi.RegionSize;

                /* Skip our own EXE module */
                if (region_start >= self_start && region_start < self_end) {
                    addr = region_end;
                    if ((DWORD)addr < (DWORD)region_start) break;
                    continue;
                }

                for (int k = 0; k < 3; k++) {
                    for (SIZE_T i = 0; i + 16 <= mbi.RegionSize; i++) {
                        if (memcmp(region_start + i, keys[k].data, 16) == 0) {
                            printf("  FOUND %s at VA 0x%p (region 0x%p, size %luKB, protect 0x%lX)\n",
                                   keys[k].name, region_start+i, mbi.BaseAddress,
                                   (unsigned long)(mbi.RegionSize/1024), mbi.Protect);
                        }
                    }
                }
            }
            addr = (unsigned char *)mbi.BaseAddress + mbi.RegionSize;
            if ((DWORD)addr < (DWORD)mbi.BaseAddress) break;
        }
    }

    /* 4. Look at PE sections of VRPSDK.dll to understand layout */
    printf("\n=== VRPSDK.dll PE sections ===\n");
    {
        IMAGE_SECTION_HEADER *sec = IMAGE_FIRST_SECTION(nt);
        for (int i = 0; i < nt->FileHeader.NumberOfSections; i++) {
            printf("  Section '%.8s': VA 0x%lX, Size 0x%lX, Flags 0x%lX\n",
                   sec[i].Name, sec[i].VirtualAddress, sec[i].Misc.VirtualSize,
                   sec[i].Characteristics);
        }
    }

    /* 5. Search for WPA key string (the registration uses WiFi WPA key) */
    printf("\n=== Searching for WPA/WiFi/WLAN references ===\n");
    const char *terms[] = {"WPA", "wpa", "WLAN", "wlan", "WiFi", "wifi",
                           "passphrase", "AP-Key", "AP-Ssid", "WpaKey", NULL};
    for (int t = 0; terms[t]; t++) {
        int slen = strlen(terms[t]);
        for (DWORD i = 0; i + slen < image_size; i++) {
            if (memcmp(base + i, terms[t], slen) == 0) {
                /* Check it's a string boundary */
                if (i > 0 && base[i-1] != 0 && base[i-1] != ' ' && base[i-1] != '\n'
                    && base[i-1] != '-' && base[i-1] != ':') continue;
                DWORD end = i + slen;
                if (end < image_size && base[end] != 0 && base[end] != ' '
                    && base[end] != '-' && base[end] != ':' && base[end] != '\r'
                    && base[end] != '\n' && (base[end] < 'A' || base[end] > 'z')) continue;
                printf("  0x%lX: ", i);
                DWORD s = (i > 16) ? i - 16 : 0;
                DWORD e = (end + 48 < image_size) ? end + 48 : image_size;
                for (DWORD j = s; j < e; j++) {
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
