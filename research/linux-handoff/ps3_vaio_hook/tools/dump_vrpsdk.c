/* dump_vrpsdk.c - Load VRPSDK.dll via Wine, let Themida unpack, dump memory */
/* Also scans entire process address space for known crypto keys */
/* Compile: i686-w64-mingw32-gcc -o dump_vrpsdk.exe dump_vrpsdk.c */
/* Run: wine dump_vrpsdk.exe */

#include <windows.h>
#include <stdio.h>
#include <string.h>

/* Known crypto keys to search for (first 8 bytes each) */
typedef struct { const char *name; unsigned char data[16]; int len; } KeyPattern;

static KeyPattern keys[] = {
    {"PC_XOR",       {0xEC,0x6D,0x70,0x6B,0x1E,0x0A,0x9A,0x75,0x8C,0xDA,0x78,0x27,0x51,0xA3,0xC3,0x7B}, 16},
    {"PC_IV",        {0x5B,0x64,0x40,0xC5,0x2E,0x74,0xC0,0x46,0x48,0x72,0xC9,0xC5,0x49,0x0C,0x79,0x04}, 16},
    {"PC_NONCE",     {0x33,0x72,0x84,0x4C,0xA6,0x6F,0x2E,0x2B,0x20,0xC7,0x90,0x60,0x33,0xE8,0x29,0xC6}, 16},
    {"PHONE_XOR",    {0xF1,0x16,0xF0,0xDA,0x44,0x2C,0x06,0xC2,0x45,0xB1,0x5E,0x48,0xF9,0x04,0xE3,0xE6}, 16},
    {"PHONE_IV",     {0x29,0x0D,0xE9,0x07,0xE2,0x3B,0xE2,0xFC,0x34,0x08,0xCA,0x4B,0xDE,0xE4,0xAF,0x3A}, 16},
    {"PSP_XOR",      {0xFD,0xC3,0xF6,0xA6,0x4D,0x2A,0xBA,0x7A,0x38,0x92,0x6C,0xBC,0x34,0x31,0xE1,0x0E}, 16},
    {"PSP_IV",       {0x3A,0x9D,0xF3,0x3B,0x99,0xCF,0x9C,0x0D,0xBF,0x58,0x81,0x12,0x6C,0x18,0x32,0x64}, 16},
    {"SKEY0",        {0xD1,0xB2,0x12,0xEB,0x73,0x86,0x6C,0x7B,0x12,0xA7,0x5E,0x0C,0x04,0xC6,0xB8,0x91}, 16},
    {"SKEY1",        {0x1F,0xD5,0xB9,0xFA,0x71,0xB8,0x96,0x81,0xB2,0x87,0x92,0xE2,0x6F,0x38,0xC3,0x6F}, 16},
    {"SKEY2",        {0x65,0x2D,0x8C,0x90,0xDE,0x87,0x17,0xCF,0x4F,0xB3,0xD8,0xD3,0x01,0x79,0x6B,0x59}, 16},
    {"PHONE_NONCE",  {0x37,0x63,0xE5,0x4D,0x12,0xF9,0x7B,0x73,0x62,0x3A,0xD3,0x0D,0x10,0xC9,0x91,0x7E}, 16},
    /* Also search for just first 4 bytes */
    {"PC_XOR_4",     {0xEC,0x6D,0x70,0x6B}, 4},
    {"SKEY0_4",      {0xD1,0xB2,0x12,0xEB}, 4},
    {"SKEY2_4",      {0x65,0x2D,0x8C,0x90}, 4},
};
#define NUM_KEYS (sizeof(keys)/sizeof(keys[0]))

void search_memory_range(const char *region_name, unsigned char *base, SIZE_T size) {
    for (int k = 0; k < NUM_KEYS; k++) {
        for (SIZE_T i = 0; i + keys[k].len <= size; i++) {
            if (memcmp(base + i, keys[k].data, keys[k].len) == 0) {
                printf("  FOUND %s at %s+0x%lX (VA 0x%p)\n",
                       keys[k].name, region_name, (unsigned long)i, base + i);
                /* Print 32 bytes of context */
                printf("    Context: ");
                SIZE_T ctx_start = (i > 16) ? i - 16 : 0;
                for (SIZE_T j = ctx_start; j < i + 32 && j < size; j++) {
                    printf("%02X ", base[j]);
                }
                printf("\n");
            }
        }
    }
}

/* Also load and dump all other Sony DLLs */
void load_and_dump_dll(const char *name, const char *path) {
    printf("\nLoading %s...\n", name);
    HMODULE h = LoadLibraryA(path);
    if (!h) {
        printf("  Failed: %lu\n", GetLastError());
        return;
    }
    IMAGE_DOS_HEADER *dos = (IMAGE_DOS_HEADER *)h;
    IMAGE_NT_HEADERS *nt = (IMAGE_NT_HEADERS *)((char *)h + dos->e_lfanew);
    DWORD sz = nt->OptionalHeader.SizeOfImage;
    printf("  Loaded at 0x%p, size 0x%lX (%lu bytes)\n", h, sz, sz);
    search_memory_range(name, (unsigned char *)h, sz);
}

int main(void) {
    const char *base_path = "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\";
    char path[512];

    /* Load VRPSDK.dll first (main target) */
    sprintf(path, "%sVRPSDK.dll", base_path);
    printf("Loading VRPSDK.dll...\n");
    HMODULE hVRPSDK = LoadLibraryA(path);
    if (!hVRPSDK) {
        printf("LoadLibrary VRPSDK.dll failed: %lu\n", GetLastError());
        return 1;
    }
    IMAGE_DOS_HEADER *dos = (IMAGE_DOS_HEADER *)hVRPSDK;
    IMAGE_NT_HEADERS *nt = (IMAGE_NT_HEADERS *)((char *)hVRPSDK + dos->e_lfanew);
    DWORD vrpsdk_size = nt->OptionalHeader.SizeOfImage;
    printf("VRPSDK.dll at 0x%p, size 0x%lX\n", hVRPSDK, vrpsdk_size);

    /* Dump VRPSDK.dll to file */
    const char *out_path = "Z:\\Users\\mihailurmanschi\\Work\\PsOldRemotePlay\\research\\tools\\vrpsdk_dumped.bin";
    FILE *f = fopen(out_path, "wb");
    if (f) {
        char *base = (char *)hVRPSDK;
        for (DWORD off = 0; off < vrpsdk_size; off += 4096) {
            MEMORY_BASIC_INFORMATION mbi;
            if (VirtualQuery(base + off, &mbi, sizeof(mbi)) &&
                mbi.State == MEM_COMMIT &&
                !(mbi.Protect & (PAGE_NOACCESS | PAGE_GUARD))) {
                fwrite(base + off, 1, 4096, f);
            } else {
                char zeros[4096] = {0};
                fwrite(zeros, 1, 4096, f);
            }
        }
        fclose(f);
        printf("Dumped VRPSDK.dll to file\n");
    }

    /* Search VRPSDK.dll image */
    printf("\n=== Searching VRPSDK.dll image ===\n");
    search_memory_range("VRPSDK", (unsigned char *)hVRPSDK, vrpsdk_size);

    /* Load and search other Sony DLLs */
    const char *other_dlls[] = {"sonyjvtd.dll", "UFCore.dll", "VRPMapping.dll", "VRPMFMGR.dll", "Resource.dll"};
    for (int i = 0; i < 5; i++) {
        sprintf(path, "%s%s", base_path, other_dlls[i]);
        load_and_dump_dll(other_dlls[i], path);
    }

    /* Now scan ENTIRE process address space */
    printf("\n=== Scanning entire process memory ===\n");
    unsigned char *addr = NULL;
    MEMORY_BASIC_INFORMATION mbi;
    int regions = 0;

    while (VirtualQuery(addr, &mbi, sizeof(mbi))) {
        if (mbi.State == MEM_COMMIT &&
            !(mbi.Protect & (PAGE_NOACCESS | PAGE_GUARD)) &&
            (mbi.Protect & (PAGE_READONLY | PAGE_READWRITE | PAGE_EXECUTE_READ | PAGE_EXECUTE_READWRITE | PAGE_WRITECOPY | PAGE_EXECUTE_WRITECOPY))) {
            char region_name[64];
            sprintf(region_name, "0x%p[%luKB]", mbi.BaseAddress, (unsigned long)(mbi.RegionSize / 1024));
            /* Only search with full 16-byte keys to reduce noise */
            for (int k = 0; k < 11; k++) { /* first 11 are 16-byte keys */
                for (SIZE_T i = 0; i + 16 <= mbi.RegionSize; i++) {
                    unsigned char *p = (unsigned char *)mbi.BaseAddress + i;
                    if (memcmp(p, keys[k].data, 16) == 0) {
                        printf("  FOUND %s at VA 0x%p (region %s)\n",
                               keys[k].name, p, region_name);
                        printf("    Context: ");
                        SIZE_T ctx_start = (i > 16) ? i - 16 : 0;
                        for (SIZE_T j = ctx_start; j < i + 48 && j < mbi.RegionSize; j++) {
                            printf("%02X ", ((unsigned char *)mbi.BaseAddress)[j]);
                        }
                        printf("\n");
                    }
                }
            }
            regions++;
        }
        addr = (unsigned char *)mbi.BaseAddress + mbi.RegionSize;
        if ((DWORD)addr < (DWORD)mbi.BaseAddress) break; /* overflow */
    }
    printf("Scanned %d committed regions\n", regions);

    /* Try to enumerate exports from VRPSDK to understand the interface */
    printf("\n=== VRPSDK.dll exports ===\n");
    IMAGE_DATA_DIRECTORY *expDir = &nt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_EXPORT];
    if (expDir->VirtualAddress && expDir->Size) {
        IMAGE_EXPORT_DIRECTORY *exp = (IMAGE_EXPORT_DIRECTORY *)((char *)hVRPSDK + expDir->VirtualAddress);
        DWORD *names = (DWORD *)((char *)hVRPSDK + exp->AddressOfNames);
        for (DWORD i = 0; i < exp->NumberOfNames && i < 50; i++) {
            char *name = (char *)hVRPSDK + names[i];
            printf("  %s\n", name);
        }
    } else {
        printf("  No export directory found\n");
    }

    printf("\nDone.\n");
    return 0;
}
