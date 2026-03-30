/* hook_aes.c - Hook AES SetKey in VRPSDK.dll to capture encryption key/IV */
/* Replaces SetKey prologue with a trampoline to our logging hook */
/* Compile: i686-w64-mingw32-gcc -o hook_aes.exe hook_aes.c */
/* Run: wine hook_aes.exe */

#include <windows.h>
#include <stdio.h>
#include <string.h>

/* Globals for hook */
static FILE *g_log = NULL;
static int g_call_count = 0;

/* The SetKey function at VRPSDK+0xD60 (VA depends on load address):
   - ECX = this (CAesCipher* at parent+0x0C)
   - [ESP+4] = key pointer (16 bytes)
   - Copies key to this+0x400, expands key, zeros source

   But we want to also capture the IV. Looking at the caller pattern:
   The callers set up both key and IV in the parent object before calling SetKey.

   Actually, the IV for CBC mode should be stored separately.
   Let me hook at a higher level - the function that calls both SetKey and EncryptMultiBlock.
*/

/* Original bytes that we'll overwrite */
static unsigned char g_orig_bytes_setkey[6];
static unsigned char g_orig_bytes_encrypt[6];
static DWORD g_setkey_addr;
static DWORD g_encrypt_addr;

/* Hook for SetKey - called via thiscall (ECX = this, stack has key ptr) */
__attribute__((naked)) void hook_setkey(void) {
    __asm__ volatile (
        "push %%ebp\n"
        "mov %%esp, %%ebp\n"
        "pusha\n"
        "pushf\n"
        /* Save this and key pointer for logging */
        "push 8(%%ebp)\n"   /* key pointer */
        "push %%ecx\n"       /* this */
        "call _log_setkey\n"
        "add $8, %%esp\n"
        "popf\n"
        "popa\n"
        "pop %%ebp\n"
        /* Restore original bytes and call original */
        "jmp *%0\n"
        : : "m" (g_setkey_addr)
    );
}

void __cdecl log_setkey(DWORD this_ptr, DWORD key_ptr) {
    if (!g_log) return;
    g_call_count++;
    fprintf(g_log, "\n=== SetKey call #%d ===\n", g_call_count);
    fprintf(g_log, "  this = 0x%08X\n", this_ptr);
    fprintf(g_log, "  key_ptr = 0x%08X\n", key_ptr);

    /* Dump the key (16 bytes) */
    unsigned char *key = (unsigned char *)key_ptr;
    fprintf(g_log, "  AES Key: ");
    for (int i = 0; i < 16; i++) fprintf(g_log, "%02X ", key[i]);
    fprintf(g_log, "\n");

    /* The IV should be near the key in the parent object.
       The parent object layout seems to be:
       +0x0C = CAesCipher (this)
       +0x460 = key buffer
       The IV for CBC is typically stored separately.
       Let me dump the parent object area to find the IV. */

    /* The parent object = this - 0x0C (since caller does lea ecx, [esi+0xC]) */
    unsigned char *parent = (unsigned char *)(this_ptr - 0x0C);
    fprintf(g_log, "  Parent object dump (+0x440 to +0x480):\n    ");
    for (int i = 0x440; i < 0x480; i++) {
        fprintf(g_log, "%02X ", parent[i]);
        if ((i - 0x440 + 1) % 16 == 0) fprintf(g_log, "\n    ");
    }

    /* Also dump the area around the key in the parent */
    fprintf(g_log, "  Parent +0x00 to +0x20:\n    ");
    for (int i = 0; i < 0x20; i++) {
        fprintf(g_log, "%02X ", parent[i]);
        if ((i + 1) % 16 == 0) fprintf(g_log, "\n    ");
    }

    fflush(g_log);
}

/* Since inline asm hooks are complex, let's use a simpler approach:
   Patch the SetKey to write key data to a shared buffer, then proceed normally */

int main(void) {
    const char *dll_path = "C:\\Program Files (x86)\\Sony\\Remote Play with PlayStation 3\\VRPSDK.dll";
    const char *log_path = "Z:\\Users\\mihailurmanschi\\Work\\PsOldRemotePlay\\research\\tools\\aes_hook_log.txt";

    g_log = fopen(log_path, "w");
    if (!g_log) {
        printf("Cannot open log file\n");
        return 1;
    }

    printf("Loading VRPSDK.dll...\n");
    HMODULE hMod = LoadLibraryA(dll_path);
    if (!hMod) {
        printf("LoadLibrary failed: %lu\n", GetLastError());
        return 1;
    }

    DWORD base = (DWORD)hMod;
    printf("VRPSDK.dll at 0x%08lX\n", base);

    /* SetKey is at base + 0x1D60 */
    /* Instead of hooking, let's use a different approach:
       Find and call the registration function via COM, then
       scan memory for the key/IV. */

    /* Actually, the simplest approach:
       Find the CAesCipher vtable, and look for the IV storage.
       The AES CBC mode needs an IV. Let me find where the IV is stored
       by looking at the EncryptMultiBlock function (0x10001E60).

       The EncryptMultiBlock function at 0x1E60 takes:
       - [ebp+8] = buffer
       - [ebp+C] = size
       And uses CBC mode where each block is XOR'd with the previous ciphertext.
       The first block is XOR'd with the IV.

       Let me disassemble the EncryptMultiBlock to find the IV location.
    */

    printf("\n=== Analyzing EncryptMultiBlock for IV location ===\n");
    unsigned char *code = (unsigned char *)base;

    /* The function at 0x1E60 (relative to code section start) */
    /* Dump the function bytes */
    DWORD func_offset = 0x1E60;
    printf("EncryptMultiBlock (base+0x%lX):\n", func_offset);
    for (int i = 0; i < 256; i++) {
        printf("%02X ", code[func_offset + i]);
        if ((i + 1) % 16 == 0) printf("\n");
    }
    printf("\n");

    /* The CBC encrypt typically does:
       for each block:
         block ^= iv (or previous ciphertext)
         encrypt(block)
         iv = encrypted_block

       So the IV is either a parameter or stored in the object.
       Let's look at the callers more carefully.
    */

    /* Let me check the caller at 0x10002CAA (function 0x100028E0) */
    /* Before the call to EncryptMultiBlock, there should be parameter setup */
    printf("\n=== Code before EncryptMultiBlock call at 0x2CAA ===\n");
    for (int i = 0x2C60; i < 0x2CC0; i++) {
        printf("%02X ", code[i]);
        if ((i - 0x2C60 + 1) % 16 == 0) printf("\n");
    }
    printf("\n");

    /* More useful: let me look at the complete encrypt flow for registration.
       Function 0x10014A90 calls SetKey at 0x10014BA0 and EncryptMultiBlock at 0x10014BB0.
       This is likely the registration encryption function. */
    printf("\n=== Registration encrypt function at 0x14A90 ===\n");
    printf("Full function dump (0x14A90 to 0x14C40):\n");
    for (int i = 0x14A90; i < 0x14C40; i++) {
        printf("%02X ", code[i]);
        if ((i - 0x14A90 + 1) % 16 == 0) printf("\n");
    }
    printf("\n");

    fclose(g_log);
    printf("Done.\n");
    return 0;
}
