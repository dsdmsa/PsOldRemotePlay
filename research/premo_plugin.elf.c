typedef unsigned char   undefined;

typedef unsigned char    byte;
typedef unsigned int    dword;
typedef unsigned long long    qword;
typedef unsigned int    uint;
typedef unsigned char    undefined1;
typedef unsigned short    undefined2;
typedef unsigned int    undefined4;
typedef unsigned long long    undefined8;
typedef unsigned short    word;
typedef struct Elf64_Phdr Elf64_Phdr, *PElf64_Phdr;

typedef enum Elf_ProgramHeaderType_PPC64 {
    PT_NULL=0,
    PT_LOAD=1,
    PT_DYNAMIC=2,
    PT_INTERP=3,
    PT_NOTE=4,
    PT_SHLIB=5,
    PT_PHDR=6,
    PT_TLS=7,
    PT_GNU_EH_FRAME=1685382480,
    PT_GNU_STACK=1685382481,
    PT_GNU_RELRO=1685382482
} Elf_ProgramHeaderType_PPC64;

struct Elf64_Phdr {
    enum Elf_ProgramHeaderType_PPC64 p_type;
    dword p_flags;
    qword p_offset;
    qword p_vaddr;
    qword p_paddr;
    qword p_filesz;
    qword p_memsz;
    qword p_align;
};

typedef struct Elf64_Ehdr Elf64_Ehdr, *PElf64_Ehdr;

struct Elf64_Ehdr {
    byte e_ident_magic_num;
    char e_ident_magic_str[3];
    byte e_ident_class;
    byte e_ident_data;
    byte e_ident_version;
    byte e_ident_osabi;
    byte e_ident_abiversion;
    byte e_ident_pad[7];
    word e_type;
    word e_machine;
    dword e_version;
    qword e_entry;
    qword e_phoff;
    qword e_shoff;
    dword e_flags;
    word e_ehsize;
    word e_phentsize;
    word e_phnum;
    word e_shentsize;
    word e_shnum;
    word e_shstrndx;
};



undefined4 DAT_0002ec90;
undefined DAT_0002e780;
undefined DAT_0002e7d0;
undefined8 DAT_0002ecd8;
undefined4 DAT_0002ecc8;
undefined4 DAT_0002ecb0;
undefined4 DAT_0002e128;
undefined1 DAT_0002ecec;
undefined1 DAT_0002eced;
undefined1 DAT_0002ecee;
undefined1 DAT_0002ecef;
undefined1 DAT_0002ecf1;
undefined1 DAT_0002ecf0;
undefined1 DAT_0002ecf8;
undefined *PTR_PTR_0002e168;
undefined *PTR_PTR_0002e150;
int DAT_0002ece8;
char DAT_0002ed09;
longlong DAT_0002ed00;
undefined4 DAT_0002e430;
uint *DAT_0002f078;
undefined4 DAT_0002f068;
undefined4 DAT_0002f06c;
int DAT_0002eef8;
int DAT_0002f070;
undefined4 DAT_0002f074;
undefined DAT_0002eee8;
int DAT_0002f0ac;
int DAT_0002f094;
int DAT_0002f0b0;
int DAT_0002f098;
int DAT_0002f074;
uint DAT_0002f088;
uint DAT_0002f094;
uint DAT_0002f0b8;
uint DAT_0002f09c;
uint DAT_0002f0ac;
uint DAT_0002f0b4;
uint DAT_0002f0b0;
uint DAT_0002f090;
uint DAT_0002f0c0;
uint DAT_0002f098;
uint DAT_0002f0bc;
uint DAT_0002f08c;
int DAT_0002f0a0;
int DAT_0002f0a4;
int DAT_0002f0a8;
undefined4 DAT_0002f0e0;
undefined DAT_0002e188;
undefined DAT_0002e1d8;
undefined DAT_0002e228;
int DAT_0002f0f0;
uint DAT_0002f0ec;
uint DAT_0002f0f4;
undefined4 DAT_0002f0e4;
undefined4 DAT_0002f0f8;
undefined DAT_0002dc2c;
undefined4 DAT_0002f0c4;
undefined DAT_0002f0d0;
int DAT_0002f0f8;
int DAT_0002f104;
int DAT_0002f068;
undefined4 DAT_0002f0fc;
pointer PTR_LAB_0002ea90;
pointer PTR_PTR_0002e428;
undefined *PTR_PTR_0002e358;
int DAT_0002f0e0;
undefined4 DAT_0002f104;
undefined4 DAT_0002f108;
uint DAT_0002e430;
pointer PTR_LAB_0002ea40;
undefined *DAT_0002f0c4;
undefined *DAT_0002f0c8;
uint DAT_0002f108;
int DAT_0002f0ec;
int DAT_0002f0f4;
int DAT_0002f0e4;
int DAT_0002f0e8;
undefined4 DAT_0002f0cc;
uint DAT_0002e348;
int DAT_0002f0d8;
int *DAT_0002f078;
int DAT_0002f0dc;
undefined DAT_0002dd9c;
undefined DAT_0002deac;
undefined4 DAT_0002f0a0;
undefined4 DAT_0002f0f0;
undefined *PTR_PTR_0002e280;
undefined4 DAT_0002e588;
undefined *PTR_PTR_0002e440;
undefined4 DAT_0002f114;
undefined4 DAT_0002f118;
undefined4 DAT_0002f11c;
undefined4 DAT_0002f1d4;
undefined4 DAT_0002f110;
uint DAT_0002f1cc;
int DAT_0002f1cc;
uint DAT_0002f1d0;
char DAT_0002f121;
char DAT_0002f128;
char DAT_0002f129;
char DAT_0002f12a;
char DAT_0002f12b;
byte DAT_0002f128;
byte DAT_0002f129;
pointer PTR_FUN_0002eb30;
undefined DAT_0002f130;
undefined4 DAT_0002f1d8;
undefined DAT_0002ce18;
undefined4 DAT_0002f10c;
undefined DAT_0002f14c;
undefined DAT_0002f168;
undefined DAT_0002f184;
undefined DAT_0002f1c4;
undefined1 DAT_0002f12b;
undefined DAT_0002f1c8;
undefined1 DAT_0002f128;
undefined1 DAT_0002f129;
undefined1 DAT_0002f12a;
undefined1 DAT_0002f120;
undefined1 DAT_0002f121;
undefined4 DAT_0002f1cc;
pointer PTR_LAB_0002eb38;
undefined DAT_0002f1a0;
char DAT_0002f120;
uint DAT_0002f1d8;
int DAT_0002f114;
int DAT_0002f11c;
pointer PTR_LAB_0002eb48;
int DAT_0002f110;
pointer PTR_LAB_0002eb40;
pointer PTR_s_NPEA00135_0002e58c;
pointer PTR_s_NPJA00001_0002e59c;
pointer PTR_s_00000001274d40158d8d40f047602200_0002e5b4;
pointer PTR_LAB_0002eb50;
undefined LAB_00010600;
pointer PTR_LAB_0002eb58;
undefined DAT_0002d5c8;
char DAT_0002f1dc;
undefined DAT_0002c1a8;
pointer PTR_LAB_0002eb60;
undefined FUN_00028800;
undefined DAT_00001285;
undefined LAB_00001770;
undefined LAB_00004ad4;
pointer PTR_LAB_0002eb68;
undefined FUN_0000ea60;
char DAT_0002f1dd;
pointer PTR_LAB_0002eb70;
pointer PTR_LAB_0002eb78;
undefined LAB_00011940;
undefined1 DAT_0002d548;
undefined1 DAT_0002d568;
undefined1 DAT_0002d578;
undefined1 DAT_0002d588;
undefined1 DAT_0002d598;
undefined1 DAT_0002d5a8;
undefined1 DAT_0002d5b8;
undefined FUN_00023280;
pointer PTR_LAB_0002eb80;
undefined UNK_00001001;
pointer PTR_LAB_0002eb88;
undefined DAT_0002c700;
pointer PTR_LAB_0002eb90;
undefined DAT_0002c770;
undefined DAT_00001006;
pointer PTR_LAB_0002eb98;
pointer PTR_LAB_0002eba0;
string s_HTTP/1.1_200_OK_0002e630;
string s_Connection:_close_0002e648;
string s_Pragma:_no-cache_0002e660;
string s_Content-Length:_0_0002e678;
string s_HTTP/1.1_403_Forbidden_0002e690;
string s_Connection:_close_0002e6b0;
string s_Pragma:_no-cache_0002e6c8;
string s_Content-Length:_0_0002e6e0;
pointer PTR_LAB_0002eba8;
pointer PTR_LAB_0002ebb0;
undefined DAT_00001005;
undefined DAT_0002c850;
undefined DAT_0002c8b0;
undefined DAT_0002ca18;
undefined DAT_0002ca30;
undefined DAT_0002cb70;
undefined DAT_0002ca40;
undefined DAT_0002c9a0;
undefined DAT_0002c9b0;
undefined DAT_0002c9b8;
undefined DAT_0002cae8;
undefined DAT_0002cd18;
undefined DAT_0002cd20;
undefined4 DAT_0002f264;
undefined4 DAT_0002f260;
int DAT_0002f260;
uint DAT_0002f260;
undefined *PTR_s_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef_0002e6f8;
undefined DAT_0002d3a8;
pointer PTR_LAB_00002114+1_0002d4f8;
pointer PTR_LAB_00004128+1_0002d520;
undefined1 DAT_0002f980;
undefined1 DAT_0002f981;
undefined1 DAT_0002f982;
undefined1 DAT_0002f983;
undefined1 DAT_0002f984;
undefined1 DAT_0002f985;
undefined DAT_0002f986;
undefined1 DAT_0002f99c;
undefined DAT_0002f268;
undefined4 DAT_0002f8fc;
undefined1 DAT_0002f8f8;
undefined DAT_0002f900;
char DAT_0002f8f8;
undefined *PTR_FUN_0002dc04;
undefined *PTR_FUN_0002dc08;
undefined *PTR_FUN_0002dc0c;
undefined *PTR_FUN_0002dc10;
undefined *PTR_FUN_0002dc18;
undefined *PTR_FUN_0002dc20;
undefined *PTR_FUN_0002dc24;
undefined *PTR_FUN_0002d700;
undefined *PTR_FUN_0002d704;
undefined *PTR_FUN_0002d708;
undefined *PTR_FUN_0002d70c;
undefined *PTR_FUN_0002d710;
undefined *PTR_FUN_0002d714;
undefined *PTR_FUN_0002d718;
undefined *PTR_FUN_0002d99c;
undefined *PTR_FUN_0002d9a0;
undefined *PTR_FUN_0002d9a4;
undefined *PTR_FUN_0002d9a8;
undefined *PTR_FUN_0002d9ac;
undefined *PTR_FUN_0002d9b0;
undefined *PTR_FUN_0002d9b4;
undefined *PTR_FUN_0002d9b8;
undefined *PTR_FUN_0002d9bc;
undefined *PTR_FUN_0002d9c0;
undefined *PTR_FUN_0002d9c4;
undefined *PTR_FUN_0002d9c8;
undefined *PTR_FUN_0002d9cc;
undefined *PTR_FUN_0002d9d0;
undefined *PTR_FUN_0002d9d4;
undefined *PTR_FUN_0002d9d8;
undefined *PTR_FUN_0002d9dc;
undefined *PTR_FUN_0002d9e0;
undefined *PTR_FUN_0002d9e4;
undefined *PTR_FUN_0002d9e8;
undefined *PTR_FUN_0002d9ec;
undefined *PTR_FUN_0002d9f0;
undefined *PTR_FUN_0002d9f4;
undefined *PTR_FUN_0002d9f8;
undefined *PTR_FUN_0002d9fc;
undefined *PTR_FUN_0002da00;
undefined *PTR_FUN_0002da04;
undefined *PTR_FUN_0002da08;
undefined *PTR_FUN_0002da0c;
undefined *PTR_FUN_0002db6c;
undefined *PTR_FUN_0002db70;
undefined *PTR_FUN_0002db74;
undefined *PTR_FUN_0002db7c;
undefined *PTR_FUN_0002db80;
undefined *PTR_FUN_0002db88;
undefined *PTR_FUN_0002db8c;
undefined *PTR_FUN_0002db90;
undefined *PTR_FUN_0002db94;
undefined *PTR_FUN_0002db98;
undefined *PTR_FUN_0002db9c;
undefined *PTR_FUN_0002dba0;
undefined *PTR_FUN_0002dba4;
undefined *PTR_FUN_0002dba8;
undefined *PTR_FUN_0002dbac;
undefined *PTR_FUN_0002dbb0;
undefined *PTR_FUN_0002db38;
undefined *PTR_FUN_0002db3c;
undefined *PTR_FUN_0002db40;
undefined *PTR_FUN_0002db44;
undefined *PTR_FUN_0002db48;
undefined *PTR_FUN_0002db4c;
undefined *PTR_FUN_0002db54;
undefined *PTR_FUN_0002db58;
undefined *PTR_FUN_0002db5c;
undefined *PTR_FUN_0002db60;
undefined *PTR_FUN_0002db64;
undefined *PTR_FUN_0002db68;
undefined *PTR_FUN_0002d728;
undefined *PTR_FUN_0002d730;
undefined *PTR_FUN_0002d73c;
undefined *PTR_FUN_0002d740;
undefined *PTR_FUN_0002d744;
undefined *PTR_FUN_0002d74c;
undefined *PTR_FUN_0002d754;
undefined *PTR_FUN_0002d75c;
undefined *PTR_FUN_0002d760;
undefined *PTR_FUN_0002d768;
undefined *PTR_FUN_0002d774;
undefined *PTR_FUN_0002d780;
undefined *PTR_FUN_0002d788;
undefined *PTR_FUN_0002d790;
undefined *PTR_FUN_0002d798;
undefined *PTR_FUN_0002d7a0;
undefined *PTR_FUN_0002d7a4;
undefined *PTR_FUN_0002d7a8;
undefined *PTR_FUN_0002d7ac;
undefined *PTR_FUN_0002d7b0;
undefined *PTR_FUN_0002d7bc;
undefined *PTR_FUN_0002d7c0;
undefined *PTR_FUN_0002d7c4;
undefined *PTR_FUN_0002d7c8;
undefined *PTR_FUN_0002d7d4;
undefined *PTR_FUN_0002d7d8;
undefined *PTR_FUN_0002d7e4;
undefined *PTR_FUN_0002d7e8;
undefined *PTR_FUN_0002d7f8;
undefined *PTR_FUN_0002d804;
undefined *PTR_FUN_0002d81c;
undefined *PTR_FUN_0002d828;
undefined *PTR_FUN_0002d83c;
undefined *PTR_FUN_0002d840;
undefined *PTR_FUN_0002d844;
undefined *PTR_FUN_0002d848;
undefined *PTR_FUN_0002d858;
undefined *PTR_FUN_0002d85c;
undefined *PTR_FUN_0002d86c;
undefined *PTR_FUN_0002d870;
undefined *PTR_FUN_0002d874;
undefined *PTR_FUN_0002d878;
undefined *PTR_FUN_0002d87c;
undefined *PTR_FUN_0002d880;
undefined *PTR_FUN_0002d88c;
undefined *PTR_FUN_0002d894;
undefined *PTR_FUN_0002d898;
undefined *PTR_FUN_0002d8a4;
undefined *PTR_FUN_0002d8a8;
undefined *PTR_FUN_0002d8ac;
undefined *PTR_FUN_0002d8b4;
undefined *PTR_FUN_0002d8c0;
undefined *PTR_FUN_0002d8c8;
undefined *PTR_FUN_0002d8dc;
undefined *PTR_FUN_0002d8e4;
undefined *PTR_FUN_0002d8e8;
undefined *PTR_FUN_0002d8ec;
undefined *PTR_FUN_0002d8f4;
undefined *PTR_FUN_0002d900;
undefined *PTR_FUN_0002d904;
undefined *PTR_FUN_0002d908;
undefined *PTR_FUN_0002d90c;
undefined *PTR_FUN_0002d910;
undefined *PTR_FUN_0002d918;
undefined *PTR_FUN_0002d924;
undefined *PTR_FUN_0002d92c;
undefined *PTR_FUN_0002d930;
undefined *PTR_FUN_0002d934;
undefined *PTR_FUN_0002d940;
undefined *PTR_FUN_0002d948;
undefined *PTR_FUN_0002d950;
undefined *PTR_FUN_0002d958;
undefined *PTR_FUN_0002d960;
undefined *PTR_FUN_0002d968;
undefined *PTR_FUN_0002d96c;
undefined *PTR_FUN_0002d970;
undefined *PTR_FUN_0002d978;
undefined *PTR_FUN_0002d980;
undefined *PTR_FUN_0002d984;
undefined *PTR_FUN_0002d994;
undefined *PTR_FUN_0002d998;
undefined *PTR_FUN_0002da10;
undefined *PTR_FUN_0002da14;
undefined *PTR_FUN_0002da18;
undefined *PTR_FUN_0002da1c;
undefined *PTR_FUN_0002da20;
undefined *PTR_FUN_0002da24;
undefined *PTR_FUN_0002da28;
undefined *PTR_FUN_0002da2c;
undefined *PTR_FUN_0002da30;
undefined *PTR_FUN_0002da34;
undefined *PTR_FUN_0002da38;
undefined *PTR_FUN_0002da3c;
undefined *PTR_FUN_0002da40;
undefined *PTR_FUN_0002da44;
undefined *PTR_FUN_0002da48;
undefined *PTR_FUN_0002da4c;
undefined *PTR_FUN_0002da50;
undefined *PTR_FUN_0002da54;
undefined *PTR_FUN_0002da58;
undefined *PTR_FUN_0002da5c;
undefined *PTR_FUN_0002db34;
undefined *PTR_FUN_0002d5f8;
undefined *PTR_FUN_0002d5fc;
undefined *PTR_FUN_0002d600;
undefined *PTR_FUN_0002dbb8;
undefined *PTR_FUN_0002dbbc;
undefined *PTR_FUN_0002dbc0;
undefined *PTR_FUN_0002dbc4;
undefined *PTR_FUN_0002dbc8;
undefined *PTR_FUN_0002dbd4;
undefined *PTR_FUN_0002dbd8;
undefined *PTR_FUN_0002dbf0;
undefined *PTR_FUN_0002d618;
undefined *PTR_FUN_0002d61c;
undefined *PTR_FUN_0002d620;
undefined *PTR_FUN_0002d624;
undefined *PTR_FUN_0002d628;
undefined *PTR_FUN_0002d62c;
undefined *PTR_FUN_0002d630;
undefined *PTR_FUN_0002d634;
undefined *PTR_FUN_0002d638;
undefined *PTR_FUN_0002d63c;
undefined *PTR_FUN_0002d640;
undefined *PTR_FUN_0002d644;
undefined *PTR_FUN_0002d648;
undefined *PTR_FUN_0002d64c;
undefined *PTR_FUN_0002d650;
undefined *PTR_FUN_0002d654;
undefined *PTR_FUN_0002d658;
undefined *PTR_FUN_0002d65c;
undefined *PTR_FUN_0002d664;
undefined *PTR_FUN_0002d668;
undefined *PTR_FUN_0002d66c;
undefined *PTR_FUN_0002d670;
undefined *PTR_FUN_0002d674;
undefined *PTR_FUN_0002d678;
undefined *PTR_FUN_0002d67c;
undefined *PTR_FUN_0002d5e0;
undefined *PTR_FUN_0002d5e4;
undefined *PTR_FUN_0002d5e8;
undefined *PTR_FUN_0002d5ec;
undefined *PTR_FUN_0002d5f0;
undefined *PTR_FUN_0002d5f4;
undefined *PTR_FUN_0002d69c;
undefined *PTR_FUN_0002d6a0;
undefined *PTR_FUN_0002d6a4;
undefined *PTR_FUN_0002d6a8;
undefined *PTR_FUN_0002d6ac;
undefined *PTR_FUN_0002d6b0;
undefined *PTR_FUN_0002d6b4;
undefined *PTR_FUN_0002d6b8;
undefined *PTR_FUN_0002d6bc;
undefined *PTR_FUN_0002d6c0;
undefined *PTR_FUN_0002d6c4;
undefined *PTR_FUN_0002d6c8;
undefined *PTR_FUN_0002d6cc;
undefined *PTR_FUN_0002d6d0;
undefined *PTR_FUN_0002d6d4;
undefined *PTR_FUN_0002d6d8;
undefined *PTR_FUN_0002d6dc;
undefined *PTR_FUN_0002d6e0;
undefined *PTR_FUN_0002d680;
undefined *PTR_FUN_0002d684;
undefined *PTR_FUN_0002d688;
undefined *PTR_FUN_0002d68c;
undefined *PTR_FUN_0002d690;
undefined *PTR_FUN_0002d694;
undefined *PTR_FUN_0002d698;
undefined *PTR_FUN_0002d604;
undefined *PTR_FUN_0002d608;
undefined *PTR_FUN_0002d60c;
undefined *PTR_FUN_0002d610;
undefined *PTR_FUN_0002d614;
undefined *PTR_FUN_0002dadc;
undefined *PTR_FUN_0002dae0;
undefined *PTR_FUN_0002dae4;
undefined *PTR_FUN_0002dae8;
undefined *PTR_FUN_0002daec;
undefined *PTR_FUN_0002daf0;
undefined *PTR_FUN_0002daf4;
undefined *PTR_FUN_0002daf8;
undefined *PTR_FUN_0002dafc;
undefined *PTR_FUN_0002db00;
undefined *PTR_FUN_0002db04;
undefined *PTR_FUN_0002db08;
undefined *PTR_FUN_0002db0c;
undefined *PTR_FUN_0002db10;
undefined *PTR_FUN_0002db14;
undefined *PTR_FUN_0002db18;
undefined *PTR_FUN_0002db1c;
undefined *PTR_FUN_0002db20;
undefined *PTR_FUN_0002db24;
undefined *PTR_FUN_0002db28;
undefined *PTR_FUN_0002db2c;
undefined *PTR_FUN_0002db30;
undefined *PTR_FUN_0002d660;
undefined *PTR_FUN_0002d6e4;
undefined *PTR_FUN_0002d6e8;
undefined *PTR_FUN_0002d6ec;
undefined *PTR_FUN_0002d6f0;
undefined *PTR_FUN_0002d6f4;
undefined *PTR_FUN_0002d6f8;
undefined *PTR_FUN_0002d6fc;
undefined *PTR_FUN_0002dad8;
undefined *PTR_FUN_0002da60;
undefined *PTR_FUN_0002da64;
undefined *PTR_FUN_0002da68;
undefined *PTR_FUN_0002da6c;
undefined *PTR_FUN_0002da70;
undefined *PTR_FUN_0002da74;
undefined *PTR_FUN_0002da78;
undefined *PTR_FUN_0002da7c;
undefined *PTR_FUN_0002da80;
undefined *PTR_FUN_0002da84;
undefined *PTR_FUN_0002da88;
undefined *PTR_FUN_0002da8c;
undefined *PTR_FUN_0002da90;
undefined *PTR_FUN_0002da94;
undefined *PTR_FUN_0002da98;
undefined *PTR_FUN_0002da9c;
undefined *PTR_FUN_0002daa0;
undefined *PTR_FUN_0002daa4;
undefined *PTR_FUN_0002daa8;
undefined *PTR_FUN_0002daac;
undefined *PTR_FUN_0002dab0;
undefined *PTR_FUN_0002dab4;
undefined *PTR_FUN_0002dab8;
undefined *PTR_FUN_0002dabc;
undefined *PTR_FUN_0002dac0;
undefined *PTR_FUN_0002dac4;
undefined *PTR_FUN_0002dac8;
undefined *PTR_FUN_0002dacc;
undefined *PTR_FUN_0002dad0;
undefined *PTR_FUN_0002dad4;

void FUN_000000c8(void)

{
  int in_r2;
  
  if (*(int *)(in_r2 + -0x7ff0) != 0) {
    FUN_000000e8(*(undefined4 *)(in_r2 + -0x7ff4),*(undefined4 *)(in_r2 + -0x7fec));
  }
  if (**(int **)(in_r2 + -0x7fe8) != 0) {
    if (*(undefined4 **)(in_r2 + -0x7fe4) != (undefined4 *)0x0) {
      (*(code *)**(undefined4 **)(in_r2 + -0x7fe4))();
    }
  }
  return;
}



void FUN_000000e8(void)

{
  int in_r2;
  
  FUN_000000e8();
  if (**(int **)(in_r2 + -0x7fe8) != 0) {
    if (*(undefined4 **)(in_r2 + -0x7fe4) != (undefined4 *)0x0) {
      (*(code *)**(undefined4 **)(in_r2 + -0x7fe4))();
    }
  }
  return;
}



void FUN_00000138(void)

{
  undefined4 *puVar1;
  int in_r2;
  undefined4 *puVar2;
  
  puVar2 = (undefined4 *)(*(int *)(in_r2 + -0x7fe0) + -4);
  puVar1 = (undefined4 *)*puVar2;
  while (puVar1 != (undefined4 *)0xffffffff) {
    puVar2 = puVar2 + -1;
    (*(code *)*puVar1)();
    puVar1 = (undefined4 *)*puVar2;
  }
  return;
}



undefined4 FUN_00000250(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4 FUN_00000270(undefined4 param_1)

{
  syscall();
  return param_1;
}



void FUN_00000388(int param_1,int param_2)

{
  int iVar1;
  
  iVar1 = param_1 + 4;
  if (0xf < *(uint *)(param_1 + 0x18)) {
    iVar1 = *(int *)(param_1 + 4);
  }
  *(int *)(param_1 + 0x14) = param_2;
  *(undefined1 *)(iVar1 + param_2) = 0;
  return;
}



undefined8 FUN_000003b4(void)

{
  return 0xfffffffe;
}



bool FUN_000003c4(int param_1,uint param_2)

{
  bool bVar1;
  uint uVar2;
  uint uVar3;
  
  uVar3 = param_1 + 4;
  bVar1 = false;
  uVar2 = uVar3;
  if (0xf < *(uint *)(param_1 + 0x18)) {
    uVar2 = *(uint *)(param_1 + 4);
  }
  if (uVar2 <= param_2) {
    if (0xf < *(uint *)(param_1 + 0x18)) {
      uVar3 = *(uint *)(param_1 + 4);
    }
    bVar1 = param_2 < uVar3 + *(int *)(param_1 + 0x14);
  }
  return bVar1;
}



void FUN_0000041c(undefined8 param_1,undefined8 param_2)

{
  FUN_00027e98(param_2);
  return;
}



undefined4 FUN_0000048c(undefined8 param_1,undefined4 param_2)

{
  int iVar1;
  undefined4 uVar2;
  
  iVar1 = FUN_000277d0();
  uVar2 = 0;
  if (iVar1 != 0) {
    uVar2 = FUN_00025a80(iVar1,param_2);
  }
  return uVar2;
}



undefined8 FUN_0000051c(void)

{
  FUN_00025d90(DAT_0002ec90,&DAT_0002e7d0,0,0xffffffffffffffff,0xffffffffffffffff,&DAT_0002e780);
  return 0;
}



undefined8 FUN_000006e8(undefined8 param_1)

{
  int iVar2;
  undefined8 uVar1;
  
  uVar1 = param_1;
  while( true ) {
    iVar2 = FUN_000266c0(uVar1);
    uVar1 = 16000;
    if ((-1 < iVar2) && ((iVar2 < 2 || ((iVar2 == 3 && (0 < (int)param_1)))))) break;
    syscall();
  }
  return 0;
}



void FUN_00000824(int param_1)

{
  *(uint *)(param_1 + 4) = *(uint *)(param_1 + 4) & 0xfffffffd;
  FUN_000266f8();
  return;
}



void FUN_00000854(int param_1)

{
  *(uint *)(param_1 + 4) = *(uint *)(param_1 + 4) | 2;
  FUN_000266f8();
  return;
}



void FUN_0000093c(int param_1,int param_2)

{
  *(uint *)(param_1 + 4) = *(uint *)(param_1 + 4) | 1;
  if (param_2 == 0) {
    FUN_000266f8();
  }
  return;
}



void FUN_000009e8(void)

{
  DAT_0002ecd8 = FUN_00029b40();
  return;
}



undefined1 FUN_00000c08(void)

{
  undefined1 uVar1;
  
  uVar1 = FUN_00025c40();
  return uVar1;
}



void FUN_00000c2c(int *param_1)

{
  (*(code *)**(undefined4 **)(*param_1 + 0x84))();
  FUN_00025c40(param_1,0x1000003);
  return;
}



void FUN_00000c84(void)

{
  undefined4 uVar1;
  int iVar2;
  
  uVar1 = FUN_00026730(DAT_0002ecc8,"premo_brightness_page");
  iVar2 = FUN_00027418(uVar1,"brightness_down",0);
  if (iVar2 != 0) {
    FUN_00000c2c(0,iVar2);
    FUN_000009e8();
    DAT_0002ecb0 = 0;
  }
  return;
}



void FUN_00000f7c(undefined4 param_1)

{
  int iVar1;
  
  iVar1 = FUN_000253f0();
  (*(code *)**(undefined4 **)(iVar1 + 0x24))(param_1);
  return;
}



void FUN_00001374(undefined1 *param_1)

{
  if (param_1 != (undefined1 *)0x0) {
    DAT_0002ecec = *param_1;
    DAT_0002eced = param_1[1];
    DAT_0002ecee = param_1[2];
    DAT_0002ecef = param_1[3];
    DAT_0002ecf1 = param_1[5];
    DAT_0002ecf0 = param_1[4];
  }
  DAT_0002ecf8 = (char)DAT_0002e128;
  return;
}



undefined4 FUN_00001470(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027bc0();
  return uVar1;
}



void FUN_00001494(int param_1,int param_2,int param_3)

{
  undefined4 uVar1;
  
  if (param_2 != 0) {
    if (0xf < *(uint *)(param_1 + 0x18)) {
      uVar1 = *(undefined4 *)(param_1 + 4);
      if (param_3 != 0) {
        FUN_00001470(param_1 + 4,uVar1,param_3);
      }
      FUN_0000041c(param_1,uVar1,*(int *)(param_1 + 0x18) + 1);
    }
  }
  *(undefined4 *)(param_1 + 0x18) = 0xf;
  FUN_00000388(param_1,param_3);
  return;
}



void FUN_00001548(undefined8 param_1)

{
  FUN_00001494(param_1,1,0);
  return;
}



void FUN_00001570(int param_1)

{
  FUN_00001548(param_1 + 8);
  return;
}



undefined4 FUN_000019c8(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027a70();
  return uVar1;
}



undefined4 FUN_000019ec(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027b18();
  return uVar1;
}



int FUN_00001a10(int param_1,uint param_2,uint param_3)

{
  int iVar1;
  uint uVar2;
  uint uVar3;
  
  if (*(uint *)(param_1 + 0x14) < param_2) {
    FUN_00027db8(param_1);
  }
  iVar1 = param_1 + 4;
  uVar2 = *(int *)(param_1 + 0x14) - param_2;
  uVar3 = uVar2;
  if (param_3 < uVar2) {
    uVar3 = param_3;
  }
  if (uVar3 != 0) {
    if (0xf < *(uint *)(param_1 + 0x18)) {
      iVar1 = *(int *)(param_1 + 4);
    }
    FUN_000019ec(iVar1 + param_2,iVar1 + param_2 + uVar3,uVar2 - uVar3);
    FUN_00000388(param_1,*(int *)(param_1 + 0x14) - uVar3);
  }
  return param_1;
}



undefined4 FUN_00001b00(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027ed0(param_1,1);
  return uVar1;
}



undefined4 FUN_00001b28(undefined8 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00001b00(param_2,0);
  return uVar1;
}



void FUN_00001b54(int param_1,ulonglong param_2,int param_3)

{
  uint uVar1;
  ulonglong uVar2;
  ulonglong uVar3;
  ulonglong uVar4;
  undefined4 uVar5;
  int iVar6;
  ulonglong uVar7;
  
  uVar7 = param_2 | 0xf;
  uVar4 = FUN_000003b4(param_1);
  if ((param_2 & 0xffffffff | 0xf) <= (uVar4 & 0xffffffff)) {
    uVar1 = *(uint *)(param_1 + 0x18);
    uVar2 = (ulonglong)(uVar1 >> 1);
    uVar3 = param_2 & 0xffffffff;
    param_2 = uVar7;
    if (((uVar3 | 0xf) / 3 < uVar2) && ((ulonglong)uVar1 <= (uVar4 - uVar2 & 0xffffffff))) {
      param_2 = uVar1 + uVar2;
    }
  }
  uVar5 = FUN_00001b28(param_1,param_2 + 1 & 0xffffffff);
  if (param_3 != 0) {
    iVar6 = param_1 + 4;
    if (0xf < *(uint *)(param_1 + 0x18)) {
      iVar6 = *(int *)(param_1 + 4);
    }
    FUN_00001470(uVar5,iVar6,param_3);
  }
  FUN_00001494(param_1,1,0);
  *(int *)(param_1 + 0x18) = (int)param_2;
  *(undefined4 *)(param_1 + 4) = uVar5;
  FUN_00000388(param_1,param_3);
  return;
}



uint FUN_00001c5c(int param_1,uint param_2,int param_3)

{
  uint uVar1;
  
  uVar1 = FUN_000003b4(param_1);
  if (uVar1 < param_2) {
    FUN_00027b88(param_1);
  }
  if (*(uint *)(param_1 + 0x18) < param_2) {
    FUN_00001b54(param_1,param_2,*(undefined4 *)(param_1 + 0x14));
  }
  else if ((param_3 == 0) || (0xf < param_2)) {
    if (param_2 == 0) {
      FUN_00000388(param_1,0);
    }
  }
  else {
    uVar1 = param_2;
    if (*(uint *)(param_1 + 0x14) < param_2) {
      uVar1 = *(uint *)(param_1 + 0x14);
    }
    FUN_00001494(param_1,1,uVar1);
  }
  return ((int)param_2 >> 0x1f) - ((int)param_2 >> 0x1f ^ param_2) >> 0x1f;
}



int FUN_00001d58(int param_1,int param_2,uint param_3,uint param_4)

{
  uint uVar1;
  char cVar3;
  int iVar2;
  int iVar4;
  
  if (*(uint *)(param_2 + 0x14) < param_3) {
    FUN_00027db8(param_1);
  }
  uVar1 = *(int *)(param_2 + 0x14) - param_3;
  if (uVar1 < param_4) {
    param_4 = uVar1;
  }
  if (param_1 == param_2) {
    FUN_00001a10(param_2,param_3 + param_4,0xffffffff);
    FUN_00001a10(param_2,0,param_3);
  }
  else {
    cVar3 = FUN_00001c5c(param_1,param_4,0);
    if (cVar3 != '\0') {
      iVar2 = param_1 + 4;
      if (0xf < *(uint *)(param_1 + 0x18)) {
        iVar2 = *(int *)(param_1 + 4);
      }
      iVar4 = param_2 + 4;
      if (0xf < *(uint *)(param_2 + 0x18)) {
        iVar4 = *(int *)(param_2 + 4);
      }
      FUN_00001470(iVar2,iVar4 + param_3,param_4);
      FUN_00000388(param_1,param_4);
    }
  }
  return param_1;
}



int FUN_00001e90(int param_1,int param_2,undefined4 param_3)

{
  char cVar1;
  int iVar2;
  
  cVar1 = FUN_000003c4(param_1,param_2);
  iVar2 = param_1 + 4;
  if (cVar1 == '\0') {
    cVar1 = FUN_00001c5c(param_1,param_3,0,param_3);
    iVar2 = param_1 + 4;
    if (cVar1 != '\0') {
      if (0xf < *(uint *)(param_1 + 0x18)) {
        iVar2 = *(int *)(param_1 + 4);
      }
      FUN_00001470(iVar2,param_2,param_3);
      FUN_00000388(param_1,param_3);
    }
  }
  else {
    if (0xf < *(uint *)(param_1 + 0x18)) {
      iVar2 = *(int *)(param_1 + 4);
    }
    param_1 = FUN_00001d58(param_1,param_1,param_2 - iVar2);
  }
  return param_1;
}



undefined4 FUN_00001f78(undefined4 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000019c8(param_2);
  uVar1 = FUN_00001e90(param_1,param_2,uVar1);
  return uVar1;
}



undefined4 FUN_00001fc8(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00001f78();
  return uVar1;
}



void FUN_00001fec(int param_1)

{
  FUN_00001fc8(param_1 + 4);
  return;
}



void FUN_00002014(undefined8 param_1,undefined4 param_2)

{
  FUN_00001494(param_1,0,0);
  FUN_00001f78(param_1,param_2);
  return;
}



void FUN_00002060(undefined8 param_1,undefined4 param_2)

{
  FUN_00001494(param_1,0,0);
  FUN_00001d58(param_1,param_2,0,0xffffffff);
  return;
}



void FUN_000020b8(undefined4 *param_1)

{
  *param_1 = &PTR_PTR_0002e168;
  FUN_00002060(param_1 + 1);
  return;
}



void FUN_000020f0(undefined4 *param_1)

{
  FUN_000020b8();
  *param_1 = &PTR_PTR_0002e150;
  return;
}



undefined4 FUN_00002128(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027ed0(param_1 * 0xc,4);
  return uVar1;
}



undefined4 FUN_00002158(undefined8 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00002128(param_2,0);
  return uVar1;
}



undefined4 *
FUN_00002184(undefined8 param_1,undefined4 param_2,undefined4 param_3,undefined4 *param_4)

{
  undefined4 *puVar1;
  
  puVar1 = (undefined4 *)FUN_00002158(param_1,1);
  puVar1[2] = *param_4;
  *puVar1 = param_2;
  puVar1[1] = param_3;
  return puVar1;
}



void FUN_000021e4(undefined8 param_1,longlong param_2,longlong param_3)

{
  FUN_00026fb8((double)param_2,(double)param_3);
  return;
}



undefined4
FUN_000023fc(undefined8 param_1,undefined8 param_2,undefined8 param_3,undefined8 param_4,
            undefined8 param_5)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000258f8(param_1,param_2,param_4,param_5);
  return uVar1;
}



void FUN_00002990(void)

{
  FUN_000250e0();
  return;
}



undefined4 FUN_000029b0(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00025118();
  return uVar1;
}



void FUN_000029d4(void)

{
  longlong lVar1;
  int iVar2;
  
  if ((DAT_0002ece8 == 4) && (DAT_0002ed09 == '\0')) {
    lVar1 = FUN_00029b40();
    if (150000000 < lVar1 - DAT_0002ed00) {
      DAT_0002ed09 = '\x01';
      iVar2 = FUN_00023774();
      if (iVar2 != 0) {
        FUN_00025380(1);
      }
      FUN_00024470();
      FUN_00025268(1);
    }
  }
  return;
}



uint FUN_00002b68(uint param_1)

{
  int iVar1;
  undefined1 auStack_90 [4];
  uint local_8c;
  
  iVar1 = FUN_00029750(auStack_90);
  if (iVar1 == 0) {
    if (1 < local_8c) {
      return 1;
    }
    if (local_8c == 1) {
      return (((int)param_1 >> 0x1f ^ param_1) - ((int)param_1 >> 0x1f)) - 1 >> 0x1f;
    }
  }
  return 0;
}



void FUN_00003d4c(undefined8 param_1)

{
  FUN_000297f8(param_1,0);
  return;
}



void FUN_00003d70(undefined4 *param_1,undefined8 param_2)

{
  *param_1 = (int)param_2;
  FUN_00003d4c(param_2);
  return;
}



void FUN_00003e78(undefined4 *param_1)

{
  *param_1 = &PTR_PTR_0002e168;
  FUN_00001548(param_1 + 1);
  FUN_00027a00(param_1);
  return;
}



void FUN_00003f04(undefined4 *param_1)

{
  *param_1 = &PTR_PTR_0002e150;
  FUN_00003e78();
  return;
}



void FUN_00003f34(int param_1,uint param_2)

{
  undefined1 auStack_60 [28];
  undefined1 auStack_44 [36];
  
  if (0x3fffffffU - *(int *)(param_1 + 8) < param_2) {
    FUN_00002014(auStack_60,"list<T> too long");
    FUN_000020f0(auStack_44,auStack_60);
    FUN_00027a38(auStack_44);
    FUN_00003f04(auStack_44);
    FUN_00001548(auStack_60);
  }
  *(uint *)(param_1 + 8) = *(int *)(param_1 + 8) + param_2;
  return;
}



void FUN_00003fe0(undefined8 param_1,int param_2,undefined8 param_3)

{
  int iVar1;
  
  iVar1 = FUN_00002184(param_1,param_2,*(undefined4 *)(param_2 + 4),param_3);
  FUN_00003f34(param_1,1);
  *(int *)(param_2 + 4) = iVar1;
  **(int **)(iVar1 + 4) = iVar1;
  return;
}



void FUN_00004048(int param_1,undefined8 param_2)

{
  FUN_00003fe0(param_1,*(undefined4 *)(param_1 + 4),param_2);
  return;
}



void FUN_00004070(void)

{
  FUN_00029830();
  return;
}



void FUN_00004090(undefined4 *param_1)

{
  FUN_00004070(*param_1);
  return;
}



void FUN_000040b4(undefined4 *param_1)

{
  undefined1 local_30 [4];
  undefined1 auStack_2c [12];
  
  param_1[1] = 0;
  *param_1 = 0x24;
  FUN_00003d70(auStack_2c,0x10);
  FUN_00004048(4,local_30);
  FUN_00004090(auStack_2c);
  return;
}



undefined4 FUN_000050e8(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4 FUN_00005108(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4 FUN_00005128(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4
FUN_00005148(undefined4 param_1,undefined8 param_2,undefined8 param_3,undefined8 param_4,
            undefined8 param_5)

{
  undefined8 *puVar1;
  
  syscall();
  puVar1 = (undefined8 *)param_2;
  puVar1[3] = param_5;
  *puVar1 = param_2;
  puVar1[1] = param_3;
  puVar1[2] = param_4;
  return param_1;
}



undefined4 FUN_00005184(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4 FUN_000051a4(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4 FUN_000051c4(undefined4 param_1)

{
  syscall();
  return param_1;
}



undefined4 FUN_000051e4(void)

{
  return DAT_0002e430;
}



void FUN_0000521c(int param_1)

{
  *(uint *)(param_1 + 0x3c) = *(uint *)(param_1 + 0x3c) & 0xfffffffe;
  return;
}



undefined4 FUN_0000522c(int param_1)

{
  undefined4 uVar1;
  undefined1 auStack_10 [16];
  
  uVar1 = 0;
  if (*(longlong *)(param_1 + 0x28) != 0xffffffff) {
    uVar1 = FUN_000050e8(*(longlong *)(param_1 + 0x28),auStack_10);
  }
  return uVar1;
}



void FUN_00005274(int param_1,undefined1 param_2,uint param_3,int param_4,int param_5,
                 undefined1 param_6)

{
  *(uint *)(param_1 + 0x50) = param_3;
  *(undefined1 *)(param_1 + 0x4d) = param_2;
  *(undefined1 *)(param_1 + 0x4e) = param_6;
  *(int *)(param_1 + 0x5c) = param_4;
  if (param_5 == 0) {
    *(int *)(param_1 + 0x54) = param_4;
    *(undefined4 *)(param_1 + 0x58) = 0;
  }
  else {
    *(uint *)(param_1 + 0x54) = (param_3 >> 4) * 9;
    *(uint *)(param_1 + 0x58) = param_4 + (param_3 >> 4) * -9 >> 1;
  }
  *(undefined1 *)(param_1 + 0x4c) = 1;
  return;
}



void FUN_000052bc(int param_1)

{
  FUN_000051a4(*(undefined4 *)(param_1 + 0x34));
  return;
}



void FUN_000052e0(int param_1)

{
  *(uint *)(param_1 + 0x3c) = *(uint *)(param_1 + 0x3c) & 0xfffffffe;
  FUN_000052bc(param_1,0xff,0,0);
  return;
}



undefined4 FUN_00005300(int param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00005148(*(undefined4 *)(param_1 + 0x30),param_2,0);
  return uVar1;
}



undefined4 FUN_0000532c(undefined8 param_1,uint param_2)

{
  longlong lVar1;
  
  lVar1 = 500;
  do {
    if (param_2 <= *DAT_0002f078) {
      return 1;
    }
    syscall();
    lVar1 = lVar1 + -1;
  } while (lVar1 != 0);
  return 0;
}



void FUN_00005524(void)

{
  FUN_00024ee8(0x38210070,8,DAT_0002f068);
  FUN_00024cf0(0x38210070,0,DAT_0002f06c);
  return;
}



void FUN_0000557c(int *param_1)

{
  int iVar1;
  
  FUN_000271e8();
  iVar1 = param_1[1];
  param_1[1] = iVar1 + -1;
  if (iVar1 + -1 < 1) {
    FUN_00025ff8();
    (*(code *)**(undefined4 **)(*param_1 + 4))(param_1);
  }
  else {
    FUN_00025ff8();
  }
  return;
}



void FUN_000055f8(int *param_1)

{
  if (*param_1 != 0) {
    FUN_0000557c(*param_1);
  }
  return;
}



void FUN_00005628(int param_1)

{
  FUN_000271e8();
  *(int *)(param_1 + 4) = *(int *)(param_1 + 4) + 1;
  FUN_00025ff8();
  return;
}



void FUN_00005664(int *param_1)

{
  if (*param_1 != 0) {
    FUN_00005628(*param_1);
  }
  return;
}



void FUN_00005694(undefined4 *param_1,undefined4 *param_2)

{
  *param_1 = *param_2;
  FUN_00005664();
  return;
}



void FUN_000056c0(int param_1)

{
  undefined4 uVar1;
  undefined4 local_30;
  undefined4 local_2c;
  undefined4 local_28 [4];
  
  if (*(int *)(param_1 + 0x60) == 0) {
    uVar1 = FUN_00027140();
    FUN_00005694(&local_30,uVar1);
    FUN_00026ff0(local_30);
    FUN_000055f8(&local_30);
  }
  else {
    FUN_00026570(&local_2c,local_28);
    uVar1 = FUN_00026c38(local_2c);
    FUN_00025e00(uVar1,0,local_28[0]);
  }
  return;
}



void FUN_00005748(int param_1,ulonglong param_2)

{
  uint uVar1;
  int iVar2;
  ulonglong uVar3;
  int iVar4;
  undefined1 local_40 [16];
  
  uVar1 = (int)*(uint *)(param_1 + 0x60) >> 0x1f;
  iVar4 = ((int)(uVar1 - (uVar1 ^ *(uint *)(param_1 + 0x60))) >> 0x1f & 0xf4f1fdf1U) + 0x524f4f54;
  iVar2 = FUN_0000c510(*(undefined4 *)(param_1 + 0x44),&DAT_0002eee8,local_40,iVar4);
  if (-1 < iVar2) {
    uVar3 = 0;
    if (DAT_0002eef8 == 0x1f) {
      DAT_0002f070 = 1;
      if (*(int *)(param_1 + 0x60) != 0) {
        uVar3 = param_2 >> 0xc & 1;
      }
      FUN_0000c26c(*(undefined4 *)(param_1 + 0x44),iVar4,DAT_0002f074,uVar3,
                   *(undefined2 *)(param_1 + 0x40));
      *(short *)(param_1 + 0x40) = *(short *)(param_1 + 0x40) + 1;
    }
    else if (DAT_0002f070 != 0) {
      FUN_0000c20c(*(undefined4 *)(param_1 + 0x44),iVar4);
      DAT_0002f070 = 0;
    }
  }
  return;
}



void FUN_00005870(int param_1)

{
  int iVar1;
  undefined8 uVar2;
  int iVar3;
  
  if (*(char *)(param_1 + 0x48) == '\x02') {
    iVar1 = DAT_0002f0ac * DAT_0002f094;
    iVar3 = DAT_0002f0b0 * 2 * DAT_0002f098;
    FUN_00027ae0(DAT_0002f074,0x10,iVar1);
    uVar2 = 0x80;
    iVar1 = iVar1 + DAT_0002f074;
  }
  else {
    uVar2 = 0;
    iVar3 = DAT_0002f0ac * DAT_0002f094;
    iVar1 = DAT_0002f074;
  }
  FUN_00027ae0(iVar1,uVar2,iVar3);
  return;
}



void FUN_00005924(void)

{
  undefined1 local_90 [64];
  
  FUN_00027ae0(local_90,0,0x3c);
  FUN_00024a50(0x38210070,local_90);
  return;
}



void FUN_00005a38(int param_1)

{
  uint uVar1;
  uint uVar2;
  uint uVar3;
  undefined *puVar4;
  int iVar5;
  
  if (*(char *)(param_1 + 0x4c) != '\0') {
    uVar1 = *(uint *)(param_1 + 0x50);
    *(undefined1 *)(param_1 + 0x48) = *(undefined1 *)(param_1 + 0x4d);
    *(undefined1 *)(param_1 + 0x4a) = *(undefined1 *)(param_1 + 0x4e);
    *(undefined4 *)(param_1 + 0x38) = *(undefined4 *)(param_1 + 0x5c);
    uVar2 = *(uint *)(param_1 + 0x54);
    uVar3 = *(uint *)(param_1 + 0x58);
    DAT_0002f088 = uVar1;
    DAT_0002f094 = uVar2;
    DAT_0002f0b8 = uVar3;
    if (*(char *)(param_1 + 0x48) == '\x02') {
      DAT_0002f08c = uVar1 >> 1;
      *(undefined1 *)(param_1 + 0x4b) = 1;
      DAT_0002f0b0 = DAT_0002f08c + 0x3f & 0xffffffc0;
      DAT_0002f0ac = uVar1 + 0x3f & 0xffffffc0;
      DAT_0002f090 = DAT_0002f088 >> 1;
      DAT_0002f09c = DAT_0002f094 >> 1;
      DAT_0002f098 = uVar2 >> 1;
      DAT_0002f0bc = uVar3 >> 1;
      DAT_0002f0c0 = DAT_0002f0b8 >> 1;
      DAT_0002f0a4 = DAT_0002f0ac * *(int *)(param_1 + 0x38) + DAT_0002f0a0;
      DAT_0002f0a8 = DAT_0002f0a4 + DAT_0002f0b0 * (*(uint *)(param_1 + 0x38) >> 1);
      DAT_0002f0b4 = DAT_0002f0b0;
    }
    else {
      *(undefined1 *)(param_1 + 0x4b) = 0;
      DAT_0002f0ac = uVar1 * 4 + 0x3f & 0xffffffc0;
    }
    iVar5 = DAT_0002f0ac * DAT_0002f0b8;
    FUN_00027ae0(DAT_0002f074,0,iVar5);
    FUN_00027ae0((DAT_0002f094 + DAT_0002f0b8) * DAT_0002f0ac + DAT_0002f074,0,iVar5);
    if ((*(char *)(param_1 + 0x4a) == '\0') && (*(int *)(param_1 + 0x60) != 0x40)) {
      if (*(int *)(param_1 + 0x60) == 0) {
        puVar4 = &DAT_0002e188;
      }
      else {
        puVar4 = &DAT_0002e1d8;
      }
    }
    else {
      puVar4 = &DAT_0002e228;
    }
    FUN_00027bc0(DAT_0002f0e0,puVar4,0x50);
    *(undefined1 *)(param_1 + 0x4c) = 0;
  }
  return;
}



void FUN_00005c54(int param_1,undefined4 *param_2,int *param_3,int *param_4,undefined4 *param_5,
                 undefined4 *param_6,undefined4 *param_7)

{
  short sVar1;
  undefined4 uVar2;
  int local_70;
  undefined4 local_6c [5];
  
  if (*(int *)(param_1 + 0x60) == 0) {
    uVar2 = FUN_00027140();
    FUN_00005694(&local_70,uVar2);
    uVar2 = FUN_000276f0(local_70,1);
    uVar2 = FUN_000261f0(local_70,uVar2);
    FUN_000249e0(uVar2,param_6);
    FUN_000273e0(local_70);
    *param_2 = 0x85;
    sVar1 = *(short *)(local_70 + 0x1a);
    *param_3 = (int)*(short *)(local_70 + 0x18);
    *param_4 = (int)sVar1;
    *param_5 = *(undefined4 *)(local_70 + 0x38);
    *param_7 = 0;
    FUN_000055f8(&local_70);
  }
  else {
    FUN_00026570(local_6c,param_5);
    *(undefined1 *)(param_1 + 0x4f) = 0;
    switch(local_6c[0]) {
    case 3:
      uVar2 = 0x85;
      *(undefined1 *)(param_1 + 0x4f) = 1;
      break;
    default:
      uVar2 = 0xffffffff;
      break;
    case 5:
      uVar2 = 0x85;
      break;
    case 7:
      uVar2 = 0x9a;
      break;
    case 8:
      uVar2 = 0x9b;
      break;
    case 0x10:
      uVar2 = 0x86;
      break;
    case 0x12:
      uVar2 = 0x87;
      break;
    case 0x14:
      uVar2 = 0x88;
    }
    *param_2 = uVar2;
    FUN_00024a88(0x38210070,1,2,1);
    FUN_00026ae8(param_3,param_4);
    *param_6 = 0;
    *param_7 = 1;
  }
  return;
}



void FUN_00005e88(int param_1)

{
  uint uVar1;
  longlong lVar2;
  uint local_60;
  undefined1 local_5c [4];
  undefined1 local_58 [4];
  undefined1 local_54 [4];
  undefined1 local_50 [4];
  undefined1 local_4c [4];
  undefined1 local_48 [32];
  
  FUN_00005c54(param_1,&local_60,local_50,local_54,local_58,local_5c,local_4c);
  FUN_00024cb8(0x38210070,(&DAT_0002f0e4)[*(char *)(param_1 + 0x4b)] & 0xff,1,0,0xf00,0);
  FUN_00024d98(0x38210070,(&DAT_0002f0e4)[*(char *)(param_1 + 0x4b)] & 0xff,3,3,3,0,1,0);
  uVar1 = (int)(local_60 ^ 0x9b) >> 0x1f;
  lVar2 = 1 - ((longlong)((ulonglong)(uVar1 - (uVar1 ^ local_60 ^ 0x9b)) << 0x20) >> 0x3f);
  FUN_00024c10(0x38210070,(&DAT_0002f0e4)[*(char *)(param_1 + 0x4b)] & 0xff,0,lVar2,lVar2,1);
  FUN_00024e78(0x38210070,(&DAT_0002f0e4)[*(char *)(param_1 + 0x4b)] & 0xff,local_48);
  FUN_00024bd8(0x38210070,DAT_0002f0ec & 0xff,0,0x14,3,2,0,DAT_0002f0f0);
  FUN_00024bd8(0x38210070,DAT_0002f0f4 & 0xff,0,0x14,2,2,0,DAT_0002f0f0 + 0xc);
  return;
}



void FUN_000060a4(int param_1)

{
  FUN_00024ee8(0x38210070,&DAT_0002dc2c,DAT_0002f0f8);
  FUN_00024cf0(0x38210070,(&DAT_0002f0c4)[*(char *)(param_1 + 0x4b)],
               *(undefined4 *)(&DAT_0002f0d0 + *(char *)(param_1 + 0x4b) * 4));
  FUN_00024eb0(0x38210070,0);
  return;
}



undefined4 FUN_0000612c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00026110();
  return uVar1;
}



undefined4 FUN_00006150(int param_1)

{
  bool bVar1;
  undefined4 uVar2;
  int iVar3;
  undefined4 *puVar4;
  int *piVar5;
  int iVar6;
  undefined4 local_50;
  int local_4c [5];
  
  puVar4 = &DAT_0002f0c4;
  iVar6 = 0;
  piVar5 = &DAT_0002f0fc;
  do {
    uVar2 = *puVar4;
    puVar4 = puVar4 + 1;
    FUN_00024ac0(uVar2,&local_50,local_4c);
    iVar3 = FUN_0000612c(2,*(undefined4 *)(param_1 + 100),0x40,local_4c[0]);
    *piVar5 = iVar3;
    if (iVar3 == 0) {
      return 0x80029801;
    }
    FUN_00027bc0(iVar3,local_50,local_4c[0]);
    FUN_000249e0(*piVar5,&DAT_0002f0d0 + iVar6 * 4);
    bVar1 = iVar6 != 1;
    iVar6 = iVar6 + 1;
    piVar5 = piVar5 + 1;
  } while (bVar1);
  FUN_00024ac0(&DAT_0002dc2c,&local_50,local_4c);
  DAT_0002f0f8 = FUN_0000612c(2,*(undefined4 *)(param_1 + 100),0x40,local_4c[0]);
  uVar2 = 0x80029801;
  if (DAT_0002f0f8 != 0) {
    FUN_00027bc0(DAT_0002f0f8,local_50,local_4c[0]);
    FUN_00024ac0(0,&local_50,local_4c);
    DAT_0002f104 = FUN_0000612c(2,*(undefined4 *)(param_1 + 100),0x40,local_4c[0] + 0x3f);
    uVar2 = 0x80029801;
    if (DAT_0002f104 != 0) {
      FUN_00027bc0(DAT_0002f104,local_50,local_4c[0]);
      FUN_00024ac0(8,&local_50,local_4c);
      DAT_0002f068 = FUN_0000612c(2,*(undefined4 *)(param_1 + 100),0x40,local_4c[0] + 0x3f);
      uVar2 = 0x80029801;
      if (DAT_0002f068 != 0) {
        FUN_00027bc0(DAT_0002f068,local_50,local_4c[0]);
        FUN_000249e0(DAT_0002f104,&DAT_0002f06c);
        uVar2 = 0;
      }
    }
  }
  return uVar2;
}



int FUN_00006388(int param_1,undefined4 param_2)

{
  uint uVar1;
  int iVar2;
  
  *(undefined4 *)(param_1 + 0x44) = param_2;
  uVar1 = (int)*(uint *)(param_1 + 0x60) >> 0x1f;
  iVar2 = FUN_000298a0(param_1 + 0x28,&PTR_LAB_0002ea90,param_1,
                       ((int)(uVar1 - (uVar1 ^ *(uint *)(param_1 + 0x60))) >> 0x1f & 0xfffffe70U) +
                       0x1fa,0x1000,1,"premo_copy_thread");
  if (-1 < iVar2) {
    *(uint *)(param_1 + 0x3c) = *(uint *)(param_1 + 0x3c) | 1;
  }
  return iVar2;
}



void FUN_000064ec(undefined4 *param_1)

{
  *param_1 = &PTR_PTR_0002e428;
  return;
}



void FUN_00006580(undefined4 *param_1)

{
  *param_1 = &PTR_PTR_0002e358;
  FUN_000064ec(param_1 + 5);
  return;
}



void FUN_000065b8(void)

{
  FUN_00026998();
  return;
}



void FUN_000065d8(int param_1)

{
  int *piVar1;
  int *piVar2;
  
  FUN_000052e0();
  FUN_0000522c(param_1);
  syscall();
  syscall();
  syscall();
  FUN_00005128(*(undefined4 *)(param_1 + 0x30),0);
  if (DAT_0002f0e0 != 0) {
    FUN_000065b8(DAT_0002f0e0);
    DAT_0002f0e0 = 0;
  }
  piVar1 = &DAT_0002f0fc;
  do {
    piVar2 = piVar1 + 1;
    if (*piVar1 != 0) {
      FUN_000065b8(*piVar1);
      *piVar1 = 0;
    }
    piVar1 = piVar2;
  } while (piVar2 != &DAT_0002f104);
  if (DAT_0002f0f8 != 0) {
    FUN_000065b8(DAT_0002f0f8);
    DAT_0002f0f8 = 0;
  }
  if (DAT_0002f104 != 0) {
    FUN_000065b8(DAT_0002f104);
    DAT_0002f104 = 0;
  }
  if (DAT_0002f068 != 0) {
    FUN_000065b8(DAT_0002f068);
    DAT_0002f068 = 0;
  }
  if (DAT_0002f074 != 0) {
    FUN_00026e30(DAT_0002f074,DAT_0002f108,&PTR_LAB_0002ea40,2);
  }
  DAT_0002e430 = DAT_0002e430 | 1;
  return;
}



int FUN_000067c8(int param_1,undefined4 param_2,undefined4 param_3,int param_4)

{
  uint uVar1;
  int iVar2;
  undefined4 uVar3;
  undefined4 uVar4;
  int iVar5;
  undefined1 local_50 [16];
  
  *(int *)(param_1 + 0x60) = param_4;
  if (param_4 == 0) {
    *(undefined4 *)(param_1 + 100) = 0;
  }
  else {
    *(undefined4 *)(param_1 + 100) = 1;
  }
  iVar2 = FUN_00005108(param_1 + 0x30,local_50,0,10);
  if ((-1 < iVar2) && (iVar2 = FUN_00005184(param_1 + 0x34,1,0), -1 < iVar2)) {
    iVar2 = FUN_000051c4(*(undefined4 *)(param_1 + 0x34),*(undefined4 *)(param_1 + 0x30));
    if (-1 < iVar2) {
      DAT_0002f0c4 = &DAT_0002dd9c;
      DAT_0002f0c8 = &DAT_0002deac;
      DAT_0002f0e0 = FUN_0000612c(2,*(undefined4 *)(param_1 + 100),0x40,0x50);
      iVar2 = -0x7ffd67ff;
      if ((DAT_0002f0e0 != 0) && (iVar2 = FUN_000249e0(DAT_0002f0e0,&DAT_0002f0f0), -1 < iVar2)) {
        FUN_00005274(param_1,*(undefined1 *)(param_1 + 0x48),param_2,param_3,0,0);
        FUN_00005a38(param_1);
        DAT_0002f074 = *(undefined4 *)(*(int *)(param_1 + 0xc) + 0x29c);
        DAT_0002f108 = DAT_0002f0ac * DAT_0002f094 + 0xfffffU & 0xfff00000;
        FUN_00025b98(DAT_0002f074,DAT_0002f108);
        iVar2 = FUN_000249e0(DAT_0002f074,&DAT_0002f0a0);
        if (-1 < iVar2) {
          FUN_00024e40(&DAT_0002dc2c);
          FUN_00024e40(DAT_0002f0c4);
          FUN_00024e40(DAT_0002f0c8);
          iVar2 = FUN_00006150(param_1);
          if (-1 < iVar2) {
            uVar3 = FUN_00024d60(&DAT_0002dc2c,"a2v.objCoord");
            uVar4 = FUN_00024d60(&DAT_0002dc2c,"a2v.texCoord");
            iVar5 = FUN_00024f20(&DAT_0002dc2c,uVar3);
            DAT_0002f0ec = iVar5 + -0x841;
            iVar5 = FUN_00024f20(&DAT_0002dc2c,uVar4);
            DAT_0002f0f4 = iVar5 + -0x841;
            uVar3 = FUN_00024d60(DAT_0002f0c4,"texture");
            uVar4 = FUN_00024d60(DAT_0002f0c8,"texture");
            iVar5 = FUN_00024f20(DAT_0002f0c4,uVar3);
            DAT_0002f0e4 = iVar5 + -0x800;
            iVar5 = FUN_00024f20(DAT_0002f0c8,uVar4);
            DAT_0002f0e8 = iVar5 + -0x800;
            DAT_0002f0cc = FUN_00024d60(DAT_0002f0c8,"yuvCoeff");
            uVar1 = (int)*(uint *)(param_1 + 0x60) >> 0x1f;
            DAT_0002e348 = ((int)(uVar1 - (uVar1 ^ *(uint *)(param_1 + 0x60))) >> 0x1f & 0xffffffabU
                           ) + 0x9b;
            DAT_0002f078 = (int *)FUN_00024f58(DAT_0002e348 & 0xff);
            *DAT_0002f078 = DAT_0002f0d8;
            DAT_0002e430 = 0;
            DAT_0002f0dc = DAT_0002f0d8;
            DAT_0002f0d8 = DAT_0002f0d8 + 1;
          }
        }
      }
    }
  }
  return iVar2;
}



void FUN_00006b3c(undefined4 *param_1,undefined8 param_2)

{
  FUN_00027338(param_1,param_2,0,0);
  *param_1 = &PTR_PTR_0002e280;
  *(undefined1 *)((int)param_1 + 0x4f) = 0;
  *(undefined1 *)(param_1 + 0x12) = 0;
  *(undefined1 *)((int)param_1 + 0x49) = 0;
  *(undefined1 *)((int)param_1 + 0x4a) = 0;
  *(undefined1 *)((int)param_1 + 0x4b) = 0;
  *(undefined1 *)(param_1 + 0x13) = 0;
  param_1[0x11] = 0;
  param_1[0xc] = 0;
  param_1[0xd] = 0;
  param_1[0xe] = 0;
  param_1[0xf] = 0;
  *(undefined2 *)(param_1 + 0x10) = 0;
  *(undefined8 *)(param_1 + 10) = 0xffffffff;
  return;
}



undefined4 FUN_00007150(void)

{
  return DAT_0002e588;
}



void FUN_00007160(int param_1,undefined1 param_2)

{
  if (*(int *)(param_1 + 0x298) == 0) {
    return;
  }
  *(undefined1 *)(*(int *)(param_1 + 0x298) + 0x49) = param_2;
  return;
}



void FUN_00007220(int param_1,undefined1 param_2,undefined4 param_3,undefined4 param_4,
                 undefined1 param_5,undefined1 param_6)

{
  if (*(int *)(param_1 + 0x298) != 0) {
    FUN_00005274(*(int *)(param_1 + 0x298),param_2,param_3,param_4,param_5,param_6);
  }
  return;
}



void FUN_00007264(int param_1)

{
  if (*(int *)(param_1 + 0x298) != 0) {
    FUN_0000521c(*(int *)(param_1 + 0x298));
  }
  return;
}



undefined4 FUN_00007294(int param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  uVar1 = 0;
  *(undefined4 *)(param_1 + 0x294) = param_2;
  if (*(int *)(param_1 + 0x298) != 0) {
    uVar1 = FUN_00006388(*(int *)(param_1 + 0x298),param_2);
  }
  return uVar1;
}



int FUN_00007368(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  int iVar2;
  
  iVar2 = -0x7ffd67fe;
  if (*(int *)(param_1 + 0x290) == 0) {
    iVar1 = FUN_00027e60(0x68);
    FUN_00006b3c(iVar1,param_1);
    iVar2 = -0x7ffd67ff;
    *(int *)(param_1 + 0x298) = iVar1;
    if ((iVar1 != 0) && (iVar2 = FUN_000067c8(iVar1,param_2,param_3,param_4), -1 < iVar2)) {
      FUN_00027760(param_1,*(undefined4 *)(param_1 + 0x298));
      DAT_0002e588 = 0;
    }
  }
  return iVar2;
}



void FUN_00007460(undefined4 *param_1,undefined8 param_2,undefined4 param_3,undefined8 param_4)

{
  FUN_000262d0(param_1,param_2,param_4,0,0);
  param_1[0xa6] = 0;
  *param_1 = &PTR_PTR_0002e440;
  param_1[0xa5] = 0;
  param_1[0xa7] = param_3;
  param_1[0xa4] = (int)param_4;
  return;
}



undefined4
FUN_000074d0(int *param_1,undefined8 param_2,undefined8 param_3,undefined8 param_4,
            undefined8 param_5)

{
  undefined4 uVar1;
  undefined4 in_stack_0000007c;
  undefined1 local_10 [16];
  
  uVar1 = (*(code *)**(undefined4 **)(*param_1 + 0x13c))
                    (param_1,param_2,param_5,local_10,in_stack_0000007c);
  return uVar1;
}



undefined4
FUN_0000752c(int *param_1,undefined8 param_2,undefined8 param_3,undefined8 param_4,
            undefined8 param_5,undefined8 param_6,undefined8 param_7,undefined8 param_8)

{
  undefined4 uVar1;
  undefined4 in_stack_00000074;
  undefined4 in_stack_0000007c;
  
  uVar1 = (*(code *)**(undefined4 **)(*param_1 + 0x140))
                    (param_1,param_2,param_5,param_6,param_7,param_8,in_stack_00000074,
                     in_stack_0000007c);
  return uVar1;
}



// WARNING: Removing unreachable block (ram,0x00007598)

double FUN_00007584(int param_1)

{
  if (param_1 < 0x480000be) {
    return (double)*(float *)(param_1 * 4 + 0x60000000);
  }
  return 0.0;
}



// WARNING: Removing unreachable block (ram,0x000075d4)

double FUN_000075c0(int param_1)

{
  if (param_1 < 0x48000126) {
    return (double)*(float *)(param_1 * 4 + 0x7c0803a6);
  }
  return 0.0;
}



double FUN_000075fc(double param_1,ulonglong param_2)

{
  double dVar1;
  double dVar2;
  
  dVar1 = (double)FUN_00007584(param_2 & 0xffff);
  dVar2 = (double)FUN_000075c0(param_2 >> 0x10 & 0xffff);
  return (double)(float)(dVar2 * param_1 + dVar1);
}



void FUN_00007650(undefined8 param_1,undefined4 param_2,undefined4 param_3)

{
  DAT_0002f114 = param_2;
  DAT_0002f118 = param_3;
  DAT_0002f11c = 0;
  return;
}



void FUN_00007670(undefined8 param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  DAT_0002f114 = param_2;
  DAT_0002f118 = param_3;
  DAT_0002f11c = param_4;
  return;
}



void FUN_0000768c(void)

{
  DAT_0002f114 = 9;
  DAT_0002f11c = 0;
  return;
}



void FUN_000076a8(undefined8 param_1,undefined4 param_2)

{
  DAT_0002f1d4 = param_2;
  return;
}



undefined4 FUN_000076b4(void)

{
  return DAT_0002f114;
}



undefined4 FUN_000076c4(void)

{
  return DAT_0002f110;
}



uint FUN_000076d4(void)

{
  return ((int)DAT_0002f1cc >> 0x1f) - ((int)DAT_0002f1cc >> 0x1f ^ DAT_0002f1cc) >> 0x1f;
}



void FUN_000076f4(int param_1,int param_2)

{
  int iVar1;
  
  iVar1 = param_1 + 4;
  if (7 < *(uint *)(param_1 + 0x18)) {
    iVar1 = *(int *)(param_1 + 4);
  }
  *(int *)(param_1 + 0x14) = param_2;
  *(undefined2 *)(param_2 * 2 + iVar1) = 0;
  return;
}



bool FUN_00007724(int param_1,uint param_2)

{
  bool bVar1;
  uint uVar2;
  uint uVar3;
  
  uVar3 = param_1 + 4;
  bVar1 = false;
  uVar2 = uVar3;
  if (7 < *(uint *)(param_1 + 0x18)) {
    uVar2 = *(uint *)(param_1 + 4);
  }
  if (uVar2 <= param_2) {
    if (7 < *(uint *)(param_1 + 0x18)) {
      uVar3 = *(uint *)(param_1 + 4);
    }
    bVar1 = param_2 < uVar3 + *(int *)(param_1 + 0x14) * 2;
  }
  return bVar1;
}



undefined8 FUN_00007780(void)

{
  return 0x7ffffffe;
}



undefined4 FUN_0000778c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027cd8();
  return uVar1;
}



void FUN_000077b0(undefined8 param_1,undefined8 param_2)

{
  FUN_00027e98(param_2);
  return;
}



void FUN_000077d4(int param_1,int param_2,int param_3)

{
  undefined4 uVar1;
  
  if (param_2 != 0) {
    if (7 < *(uint *)(param_1 + 0x18)) {
      uVar1 = *(undefined4 *)(param_1 + 4);
      if (param_3 != 0) {
        FUN_0000778c(param_1 + 4,uVar1,param_3);
      }
      FUN_000077b0(param_1,uVar1,*(int *)(param_1 + 0x18) + 1);
    }
  }
  *(undefined4 *)(param_1 + 0x18) = 7;
  FUN_000076f4(param_1,param_3);
  return;
}



void FUN_00007888(undefined8 param_1)

{
  FUN_000077d4(param_1,1,0);
  return;
}



void FUN_000078b0(undefined8 param_1)

{
  FUN_000077d4(param_1,0,0);
  return;
}



int * FUN_00007964(int *param_1,int param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  if (iVar1 != param_2) {
    if (iVar1 != 0) {
      FUN_0000557c(iVar1);
    }
    *param_1 = param_2;
    FUN_00005664(param_1);
  }
  return param_1;
}



undefined4 FUN_000079dc(undefined8 param_1,undefined4 *param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00007964(param_1,*param_2);
  return uVar1;
}



void FUN_00007a04(undefined4 *param_1,undefined4 param_2)

{
  *param_1 = param_2;
  FUN_00005664();
  return;
}



void FUN_00007ba8(undefined8 param_1,int param_2)

{
  int iVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  
  if (param_2 == 0) {
    if (DAT_0002f1cc != 0) {
      FUN_00025428(DAT_0002f1cc);
      DAT_0002f1cc = 0;
    }
  }
  else if (DAT_0002f1cc == 0) {
    iVar1 = FUN_000277d0("premo_plugin");
    if (iVar1 != 0) {
      uVar2 = FUN_00026810(iVar1,"msg_press_ps_button");
      uVar3 = FUN_00026730(iVar1,"premo_dialog_page");
      DAT_0002f1cc = FUN_00025690(uVar3,uVar2,1,0);
      FUN_00025658(0,DAT_0002f1cc);
      *(undefined1 *)(DAT_0002f1cc + 0x10) = 1;
    }
  }
  return;
}



void FUN_00007c8c(void)

{
  undefined4 uVar1;
  undefined4 uVar2;
  char *pcVar3;
  
  uVar1 = FUN_000277d0("premo_plugin");
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    if (DAT_0002f121 == '\0') {
      if ((((DAT_0002f128 == '\0') && (DAT_0002f129 == '\0')) && (DAT_0002f12a == '\0')) &&
         (DAT_0002f12b == '\0')) {
        return;
      }
      DAT_0002f121 = '\x01';
      uVar2 = FUN_00026730(uVar1,"btnbar");
      pcVar3 = "anim_btnbar_show";
    }
    else {
      if (DAT_0002f128 != '\0') {
        return;
      }
      if (DAT_0002f129 != '\0') {
        return;
      }
      if (DAT_0002f12a != '\0') {
        return;
      }
      if (DAT_0002f12b != '\0') {
        return;
      }
      DAT_0002f121 = DAT_0002f12b;
      uVar2 = FUN_00026730(uVar1,"btnbar");
      pcVar3 = "anim_btnbar_hide";
    }
  }
  else if (DAT_0002f121 == '\0') {
    if (((DAT_0002f128 == '\0') && (DAT_0002f129 == '\0')) &&
       ((DAT_0002f12a == '\0' && (DAT_0002f12b == '\0')))) {
      return;
    }
    DAT_0002f121 = '\x01';
    uVar2 = FUN_00026730(uVar1,"btnbar_fake16_9");
    pcVar3 = "anim_btnbar_show_fake16_9";
  }
  else {
    if (DAT_0002f128 != '\0') {
      return;
    }
    if (DAT_0002f129 != '\0') {
      return;
    }
    if (DAT_0002f12a != '\0') {
      return;
    }
    if (DAT_0002f12b != '\0') {
      return;
    }
    DAT_0002f121 = DAT_0002f12b;
    uVar2 = FUN_00026730(uVar1,"btnbar_fake16_9");
    pcVar3 = "anim_btnbar_hide_fake16_9";
  }
  FUN_00026848(uVar1,uVar2,pcVar3);
  return;
}



byte FUN_00007e84(undefined4 param_1,uint param_2)

{
  byte bVar1;
  byte bVar2;
  byte bVar3;
  undefined4 uVar4;
  int iVar5;
  undefined4 uVar6;
  char *pcVar7;
  byte bVar8;
  byte bVar9;
  
  uVar4 = FUN_000277d0("premo_plugin");
  bVar9 = (byte)param_2;
  bVar2 = bVar9;
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    iVar5 = FUN_00025e70();
    bVar1 = DAT_0002f129;
    bVar8 = DAT_0002f128;
    bVar3 = DAT_0002f129;
    if (iVar5 == 0) {
      if (param_2 == DAT_0002f128) goto LAB_00008054;
      uVar6 = FUN_00026730(uVar4,"cross_label");
      if ((param_2 & 0xff) == 0) {
        pcVar7 = "anim_cross_hide";
      }
      else {
        pcVar7 = "anim_cross_show";
      }
      goto LAB_00007fdc;
    }
    bVar2 = DAT_0002f128;
    bVar8 = 0;
    if ((iVar5 != 1) || (bVar3 = bVar9, bVar8 = DAT_0002f129, param_2 == DAT_0002f129))
    goto LAB_00008054;
    uVar6 = FUN_00026730(uVar4,"circle_label");
    bVar8 = bVar1;
    if ((param_2 & 0xff) == 0) {
      pcVar7 = "anim_circle_hide";
    }
    else {
      pcVar7 = "anim_circle_show";
    }
  }
  else {
    iVar5 = FUN_00025e70();
    bVar1 = DAT_0002f129;
    bVar8 = DAT_0002f128;
    bVar3 = DAT_0002f129;
    if (iVar5 == 0) {
      if (param_2 == DAT_0002f128) goto LAB_00008054;
      uVar6 = FUN_00026730(uVar4,"cross_label_fake16_9");
      if ((param_2 & 0xff) == 0) {
        pcVar7 = "anim_cross_hide_fake16_9";
      }
      else {
        pcVar7 = "anim_cross_show_fake16_9";
      }
LAB_00007fdc:
      FUN_00026848(uVar4,uVar6,pcVar7);
      bVar3 = DAT_0002f129;
      goto LAB_00008054;
    }
    bVar2 = DAT_0002f128;
    bVar8 = 0;
    if ((iVar5 != 1) || (bVar3 = bVar9, bVar8 = DAT_0002f129, param_2 == DAT_0002f129))
    goto LAB_00008054;
    uVar6 = FUN_00026730(uVar4,"circle_label_fake16_9");
    bVar8 = bVar1;
    if ((param_2 & 0xff) == 0) {
      pcVar7 = "anim_circle_hide_fake16_9";
    }
    else {
      pcVar7 = "anim_circle_show_fake16_9";
    }
  }
  FUN_00026848(uVar4,uVar6,pcVar7);
  bVar2 = DAT_0002f128;
  bVar3 = bVar9;
LAB_00008054:
  DAT_0002f129 = bVar3;
  DAT_0002f128 = bVar2;
  FUN_00007c8c(param_1);
  return bVar8;
}



undefined4 FUN_00008084(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027c68();
  return uVar1;
}



int FUN_000080a8(int param_1,uint param_2,uint param_3)

{
  int iVar1;
  uint uVar2;
  uint uVar3;
  
  if (*(uint *)(param_1 + 0x14) < param_2) {
    FUN_00027db8(param_1);
  }
  uVar2 = *(int *)(param_1 + 0x14) - param_2;
  uVar3 = uVar2;
  if (param_3 < uVar2) {
    uVar3 = param_3;
  }
  if (uVar3 != 0) {
    iVar1 = param_1 + 4;
    if (7 < *(uint *)(param_1 + 0x18)) {
      iVar1 = *(int *)(param_1 + 4);
    }
    FUN_00008084(iVar1 + param_2 * 2,iVar1 + (uVar3 + param_2) * 2,uVar2 - uVar3);
    FUN_000076f4(param_1,*(int *)(param_1 + 0x14) - uVar3);
  }
  return param_1;
}



undefined4 FUN_00008194(ulonglong param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027ed0((param_1 & 0x7fffffff) << 1,2);
  return uVar1;
}



undefined4 FUN_000081c0(undefined8 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00008194(param_2,0);
  return uVar1;
}



void FUN_000081ec(int param_1,ulonglong param_2,int param_3)

{
  uint uVar1;
  ulonglong uVar2;
  ulonglong uVar3;
  ulonglong uVar4;
  undefined4 uVar5;
  int iVar6;
  ulonglong uVar7;
  
  uVar7 = param_2 | 7;
  uVar4 = FUN_00007780(param_1);
  if ((param_2 & 0xffffffff | 7) <= (uVar4 & 0xffffffff)) {
    uVar1 = *(uint *)(param_1 + 0x18);
    uVar2 = (ulonglong)(uVar1 >> 1);
    uVar3 = param_2 & 0xffffffff;
    param_2 = uVar7;
    if (((uVar3 | 7) / 3 < uVar2) && ((ulonglong)uVar1 <= (uVar4 - uVar2 & 0xffffffff))) {
      param_2 = uVar1 + uVar2;
    }
  }
  uVar5 = FUN_000081c0(param_1,param_2 + 1 & 0xffffffff);
  if (param_3 != 0) {
    iVar6 = param_1 + 4;
    if (7 < *(uint *)(param_1 + 0x18)) {
      iVar6 = *(int *)(param_1 + 4);
    }
    FUN_0000778c(uVar5,iVar6,param_3);
  }
  FUN_000077d4(param_1,1,0);
  *(int *)(param_1 + 0x18) = (int)param_2;
  *(undefined4 *)(param_1 + 4) = uVar5;
  FUN_000076f4(param_1,param_3);
  return;
}



uint FUN_000082f4(int param_1,uint param_2,int param_3)

{
  uint uVar1;
  
  uVar1 = FUN_00007780(param_1);
  if (uVar1 < param_2) {
    FUN_00027b88(param_1);
  }
  if (*(uint *)(param_1 + 0x18) < param_2) {
    FUN_000081ec(param_1,param_2,*(undefined4 *)(param_1 + 0x14));
  }
  else if ((param_3 == 0) || (7 < param_2)) {
    if (param_2 == 0) {
      FUN_000076f4(param_1,0);
    }
  }
  else {
    uVar1 = param_2;
    if (*(uint *)(param_1 + 0x14) < param_2) {
      uVar1 = *(uint *)(param_1 + 0x14);
    }
    FUN_000077d4(param_1,1,uVar1);
  }
  return ((int)param_2 >> 0x1f) - ((int)param_2 >> 0x1f ^ param_2) >> 0x1f;
}



int FUN_000083f0(int param_1,int param_2,uint param_3,uint param_4)

{
  uint uVar1;
  char cVar3;
  int iVar2;
  int iVar4;
  
  if (*(uint *)(param_2 + 0x14) < param_3) {
    FUN_00027db8(param_1);
  }
  uVar1 = *(int *)(param_2 + 0x14) - param_3;
  if (uVar1 < param_4) {
    param_4 = uVar1;
  }
  if (param_1 == param_2) {
    FUN_000080a8(param_2,param_3 + param_4,0xffffffff);
    FUN_000080a8(param_2,0,param_3);
  }
  else {
    cVar3 = FUN_000082f4(param_1,param_4,0);
    if (cVar3 != '\0') {
      iVar2 = param_1 + 4;
      if (7 < *(uint *)(param_1 + 0x18)) {
        iVar2 = *(int *)(param_1 + 4);
      }
      iVar4 = param_2 + 4;
      if (7 < *(uint *)(param_2 + 0x18)) {
        iVar4 = *(int *)(param_2 + 4);
      }
      FUN_0000778c(iVar2,iVar4 + param_3 * 2,param_4);
      FUN_000076f4(param_1,param_4);
    }
  }
  return param_1;
}



int FUN_0000852c(int param_1,int param_2,undefined4 param_3)

{
  char cVar1;
  int iVar2;
  
  cVar1 = FUN_00007724(param_1,param_2);
  iVar2 = param_1 + 4;
  if (cVar1 == '\0') {
    cVar1 = FUN_000082f4(param_1,param_3,0,param_3);
    iVar2 = param_1 + 4;
    if (cVar1 != '\0') {
      if (7 < *(uint *)(param_1 + 0x18)) {
        iVar2 = *(int *)(param_1 + 4);
      }
      FUN_0000778c(iVar2,param_2,param_3);
      FUN_000076f4(param_1,param_3);
    }
  }
  else {
    if (7 < *(uint *)(param_1 + 0x18)) {
      iVar2 = *(int *)(param_1 + 4);
    }
    param_1 = FUN_000083f0(param_1,param_1,param_2 - iVar2 >> 1);
  }
  return param_1;
}



undefined4 FUN_00008618(undefined8 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000083f0(param_1,param_2,0,0xffffffff);
  return uVar1;
}



undefined4 FUN_00008648(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00008618();
  return uVar1;
}



undefined4 FUN_0000866c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027b50();
  return uVar1;
}



undefined4 FUN_00008690(undefined4 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0000866c(param_2);
  uVar1 = FUN_0000852c(param_1,param_2,uVar1);
  return uVar1;
}



void FUN_000086e0(undefined8 param_1,undefined4 param_2)

{
  FUN_000077d4(param_1,0,0);
  FUN_00008690(param_1,param_2);
  return;
}



undefined4 FUN_0000872c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00008690();
  return uVar1;
}



undefined4 FUN_00008750(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027d10();
  return uVar1;
}



uint FUN_00008774(int param_1,uint param_2,uint param_3,undefined4 param_4,uint param_5)

{
  uint uVar1;
  int iVar2;
  uint uVar3;
  
  if (*(uint *)(param_1 + 0x14) < param_2) {
    FUN_00027db8(param_1);
  }
  uVar3 = *(int *)(param_1 + 0x14) - param_2;
  if (param_3 < uVar3) {
    uVar3 = param_3;
  }
  if (uVar3 != 0) {
    iVar2 = param_1 + 4;
    if (7 < *(uint *)(param_1 + 0x18)) {
      iVar2 = *(int *)(param_1 + 4);
    }
    uVar1 = uVar3;
    if (param_5 < uVar3) {
      uVar1 = param_5;
    }
    uVar1 = FUN_00008750(iVar2 + param_2 * 2,param_4,uVar1);
    if (uVar1 != 0) {
      return uVar1;
    }
  }
  uVar1 = 0xffffffff;
  if (param_5 <= uVar3) {
    uVar1 = (int)(uVar3 ^ param_5) >> 0x1f;
    uVar1 = uVar1 - (uVar1 ^ uVar3 ^ param_5) >> 0x1f;
  }
  return uVar1;
}



undefined4 FUN_00008888(int param_1,undefined8 param_2)

{
  undefined4 uVar1;
  undefined4 uVar2;
  
  uVar2 = *(undefined4 *)(param_1 + 0x14);
  uVar1 = FUN_0000866c(param_2);
  uVar2 = FUN_00008774(param_1,0,uVar2,param_2,uVar1);
  return uVar2;
}



uint FUN_000088ec(void)

{
  uint uVar1;
  
  uVar1 = FUN_00008888();
  return (((int)uVar1 >> 0x1f ^ uVar1) - ((int)uVar1 >> 0x1f)) - 1 >> 0x1f;
}



byte FUN_00008924(void)

{
  byte bVar1;
  
  bVar1 = FUN_000088ec();
  return bVar1 ^ 1;
}



void FUN_0000894c(void)

{
  undefined4 *puVar1;
  undefined4 uVar2;
  int *piVar3;
  int *piVar4;
  int *piVar5;
  int *piVar6;
  int *piVar7;
  int *piVar8;
  int *piVar9;
  int *piVar10;
  undefined8 uVar11;
  code *pcVar12;
  double dVar13;
  double dVar14;
  double dVar15;
  double dVar16;
  double dVar17;
  double dVar18;
  double dVar19;
  undefined8 local_190;
  undefined8 local_188;
  float local_180 [4];
  float local_170 [4];
  float local_160 [4];
  undefined8 local_150;
  undefined8 local_148;
  undefined8 local_140;
  undefined8 local_138;
  undefined8 local_130;
  undefined8 local_128;
  undefined8 local_120;
  undefined8 local_118;
  undefined8 local_110;
  undefined8 local_108;
  undefined8 local_100;
  undefined8 local_f8;
  undefined8 local_f0;
  undefined8 local_e8;
  float local_e0 [8];
  
  uVar2 = FUN_000277d0("premo_plugin");
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    piVar3 = (int *)FUN_00026730(uVar2,"rectangle_shadow");
    piVar4 = (int *)FUN_00026730(uVar2,"cross_shadow");
    piVar5 = (int *)FUN_00026730(uVar2,"circle_shadow");
    piVar6 = (int *)FUN_00026730(uVar2,"triangle_shadow");
    piVar7 = (int *)FUN_00026730(uVar2,"rectangle_label");
    piVar8 = (int *)FUN_00026730(uVar2,"cross_label");
    piVar9 = (int *)FUN_00026730(uVar2,"circle_label");
    piVar10 = (int *)FUN_00026730(uVar2,"triangle_label");
    if ((piVar7[8] & 0x1000U) != 0) {
      return;
    }
    if ((piVar8[8] & 0x1000U) != 0) {
      return;
    }
    if ((piVar9[8] & 0x1000U) != 0) {
      return;
    }
    if ((piVar10[8] & 0x1000U) != 0) {
      return;
    }
    FUN_00026928(local_180);
    FUN_00026928(local_170);
    FUN_00026928(local_160);
    FUN_00026928(local_e0);
    (*(code *)**(undefined4 **)(*piVar7 + 0xd0))(piVar7,0x16,local_180);
    (*(code *)**(undefined4 **)(*piVar8 + 0xd0))(piVar8,0x16,local_170);
    (*(code *)**(undefined4 **)(*piVar9 + 0xd0))(piVar9,0x16,local_160);
    (*(code *)**(undefined4 **)(*piVar10 + 0xd0))(piVar10,0x16,local_e0);
    dVar13 = (double)FUN_000075fc(0,0xe03);
    dVar14 = (double)FUN_000075fc(0,0xdfd);
    dVar15 = (double)FUN_000075fc(0,0xdf9);
    dVar16 = (double)FUN_000075fc(0,0xdfa);
    dVar17 = (double)FUN_000075fc(0,0xdfb);
    dVar18 = (double)FUN_000075fc(0,0xdfc);
    dVar19 = (double)(float)((double)(float)(dVar18 - (double)local_e0[0]) - dVar13);
    dVar18 = (double)(float)((double)((float)(dVar15 + dVar13) + local_180[0]) + dVar14);
    if (dVar18 <= dVar16) {
      dVar18 = dVar16;
    }
    dVar16 = (double)(float)((double)((float)(dVar18 + dVar13) + local_170[0]) + dVar14);
    if (dVar17 < dVar16) {
      dVar17 = dVar16;
    }
    dVar13 = (double)(float)((double)((float)(dVar17 + dVar13) + local_160[0]) + dVar14);
    if ((dVar19 < dVar13) &&
       (dVar17 = (double)(float)(dVar17 - (double)(float)(dVar13 - dVar19)), dVar17 < dVar16)) {
      dVar18 = (double)(float)(dVar18 - (double)(float)(dVar16 - dVar17));
    }
    puVar1 = *(undefined4 **)(*piVar3 + 0x58);
    FUN_000261b8(dVar15,0,0,&local_120);
    (*(code *)*puVar1)(piVar3,0,0xdfe,0,local_120,local_118);
    puVar1 = *(undefined4 **)(*piVar4 + 0x58);
    FUN_000261b8(dVar18,0,0,&local_130);
    (*(code *)*puVar1)(piVar4,0,0xdfe,0,local_130,local_128);
    puVar1 = *(undefined4 **)(*piVar5 + 0x58);
    FUN_000261b8(dVar17,0,0,&local_140);
    (*(code *)*puVar1)(piVar5,0,0xdfe,0,local_140,local_138);
    puVar1 = *(undefined4 **)(*piVar6 + 0x58);
    FUN_000261b8(dVar19,0,0,&local_190);
    pcVar12 = (code *)*puVar1;
    uVar11 = 0xdfe;
    local_110 = local_190;
    local_108 = local_188;
  }
  else {
    piVar3 = (int *)FUN_00026730(uVar2,"rectangle_shadow_fake16_9");
    piVar4 = (int *)FUN_00026730(uVar2,"cross_shadow_fake16_9");
    piVar5 = (int *)FUN_00026730(uVar2,"circle_shadow_fake16_9");
    piVar6 = (int *)FUN_00026730(uVar2,"triangle_shadow_fake16_9");
    piVar7 = (int *)FUN_00026730(uVar2,"rectangle_label_fake16_9");
    piVar8 = (int *)FUN_00026730(uVar2,"cross_label_fake16_9");
    piVar9 = (int *)FUN_00026730(uVar2,"circle_label_fake16_9");
    piVar10 = (int *)FUN_00026730(uVar2,"triangle_label_fake16_9");
    if ((piVar7[8] & 0x1000U) != 0) {
      return;
    }
    if ((piVar8[8] & 0x1000U) != 0) {
      return;
    }
    if ((piVar9[8] & 0x1000U) != 0) {
      return;
    }
    if ((piVar10[8] & 0x1000U) != 0) {
      return;
    }
    FUN_00026928(local_e0);
    FUN_00026928(local_160);
    FUN_00026928(local_170);
    FUN_00026928(local_180);
    (*(code *)**(undefined4 **)(*piVar7 + 0xd0))(piVar7,0x16,local_e0);
    (*(code *)**(undefined4 **)(*piVar8 + 0xd0))(piVar8,0x16,local_160);
    (*(code *)**(undefined4 **)(*piVar9 + 0xd0))(piVar9,0x16,local_170);
    (*(code *)**(undefined4 **)(*piVar10 + 0xd0))(piVar10,0x16,local_180);
    dVar13 = (double)FUN_000075fc(0,0xe11);
    dVar14 = (double)FUN_000075fc(0,0xe0b);
    dVar15 = (double)FUN_000075fc(0,0xe07);
    dVar16 = (double)FUN_000075fc(0,0xe08);
    dVar17 = (double)FUN_000075fc(0,0xe09);
    dVar18 = (double)FUN_000075fc(0,0xe0a);
    dVar19 = (double)(float)((double)(float)(dVar18 - (double)local_180[0]) - dVar13);
    dVar18 = (double)(float)((double)((float)(dVar15 + dVar13) + local_e0[0]) + dVar14);
    if (dVar18 <= dVar16) {
      dVar18 = dVar16;
    }
    dVar16 = (double)(float)((double)((float)(dVar18 + dVar13) + local_160[0]) + dVar14);
    if (dVar17 < dVar16) {
      dVar17 = dVar16;
    }
    dVar13 = (double)(float)((double)((float)(dVar17 + dVar13) + local_170[0]) + dVar14);
    if ((dVar19 < dVar13) &&
       (dVar17 = (double)(float)(dVar17 - (double)(float)(dVar13 - dVar19)), dVar17 < dVar16)) {
      dVar18 = (double)(float)(dVar18 - (double)(float)(dVar16 - dVar17));
    }
    puVar1 = *(undefined4 **)(*piVar3 + 0x58);
    FUN_000261b8(dVar15,0,0,&local_150);
    (*(code *)*puVar1)(piVar3,0,0xe0c,0,local_150,local_148);
    puVar1 = *(undefined4 **)(*piVar4 + 0x58);
    FUN_000261b8(dVar18,0,0,&local_f0);
    (*(code *)*puVar1)(piVar4,0,0xe0c,0,local_f0,local_e8);
    puVar1 = *(undefined4 **)(*piVar5 + 0x58);
    FUN_000261b8(dVar17,0,0,&local_100);
    (*(code *)*puVar1)(piVar5,0,0xe0c,0,local_100,local_f8);
    puVar1 = *(undefined4 **)(*piVar6 + 0x58);
    FUN_000261b8(dVar19,0,0,&local_110);
    pcVar12 = (code *)*puVar1;
    uVar11 = 0xe0c;
  }
  (*pcVar12)(piVar6,0,uVar11,0,local_110,local_108);
  return;
}



void FUN_00009288(undefined8 param_1,int param_2,undefined4 param_3,undefined4 param_4,
                 undefined4 param_5)

{
  undefined4 *puVar1;
  undefined4 uVar2;
  char cVar4;
  int *piVar3;
  undefined1 auStack_60 [40];
  
  uVar2 = FUN_000277d0("premo_plugin");
  FUN_00008648(param_5,&DAT_0002f130 + param_2 * 0x1c);
  cVar4 = FUN_00008924(param_5,param_4);
  if (cVar4 != '\0') {
    piVar3 = (int *)FUN_00026730(uVar2,param_3);
    if (piVar3 != (int *)0x0) {
      FUN_00026960(piVar3,0x2000c,&PTR_FUN_0002eb30,0);
      FUN_00027028(piVar3);
      puVar1 = *(undefined4 **)(*piVar3 + 0x118);
      FUN_000086e0(auStack_60,param_4);
      (*(code *)*puVar1)(piVar3,auStack_60,0);
      FUN_00007888(auStack_60);
      if ((piVar3[8] & 0x1000U) == 0) {
        FUN_0000894c(0,0,0);
      }
    }
  }
  FUN_0000872c(&DAT_0002f130 + param_2 * 0x1c,param_4);
  return;
}



undefined4 FUN_000093ec(undefined4 param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  undefined8 uVar2;
  char *pcVar3;
  
  FUN_000078b0(param_1);
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    iVar1 = FUN_00025e70();
    if (iVar1 == 0) {
      pcVar3 = "cross_label";
      uVar2 = 0;
    }
    else {
      if (iVar1 != 1) {
        return param_1;
      }
      pcVar3 = "circle_label";
      uVar2 = 1;
    }
  }
  else {
    iVar1 = FUN_00025e70();
    if (iVar1 == 0) {
      pcVar3 = "cross_label_fake16_9";
      uVar2 = 0;
    }
    else {
      if (iVar1 != 1) {
        return param_1;
      }
      pcVar3 = "circle_label_fake16_9";
      uVar2 = 1;
    }
  }
  FUN_00009288(param_2,uVar2,pcVar3,param_3,param_1);
  return param_1;
}



undefined1 FUN_000094f8(void)

{
  undefined1 uVar1;
  
  uVar1 = FUN_00025c40();
  return uVar1;
}



void FUN_0000951c(int *param_1)

{
  undefined4 *puVar1;
  undefined1 auStack_40 [24];
  
  puVar1 = *(undefined4 **)(*param_1 + 0x90);
  FUN_000261b8(auStack_40);
  (*(code *)*puVar1)(param_1,auStack_40);
  FUN_00025c40(param_1,0x1000004);
  return;
}



void FUN_0000959c(int *param_1)

{
  undefined1 local_30 [24];
  
  (*(code *)**(undefined4 **)(*param_1 + 0x80))(param_1,local_30);
  FUN_00025c40(param_1,0x1000002);
  return;
}



void FUN_00009608(undefined8 param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4,
                 char *param_5,uint param_6)

{
  undefined4 uVar1;
  int *piVar2;
  undefined4 local_b0;
  undefined4 local_ac;
  undefined4 local_a8;
  undefined4 local_a4;
  undefined4 local_a0;
  float local_9c;
  undefined4 local_98;
  undefined1 auStack_84 [16];
  undefined1 local_74 [20];
  undefined1 local_60 [8];
  float local_58;
  
  if (*param_5 == '\0') {
    FUN_00025ee0(param_2,param_3,0,0,3,0,0);
    uVar1 = FUN_000258c0(param_2,param_3);
    piVar2 = (int *)FUN_00026730(param_2,param_4);
    uVar1 = FUN_000271b0(uVar1,0);
    if ((param_6 & 1) != 0) {
      FUN_00026928(&local_a4);
      (*(code *)**(undefined4 **)(*piVar2 + 0x60))(piVar2,&local_ac,&local_b0,&local_a8,&local_a4);
      FUN_00026928(local_74);
      FUN_000261b8(0,0,0,auStack_84);
      FUN_00026b90(local_60,uVar1,0,auStack_84);
      (*(code *)**(undefined4 **)(*piVar2 + 0x58))
                (piVar2,local_ac,local_b0,local_a8,CONCAT44(local_a4,local_a0),
                 CONCAT44(local_58 * 0.2 + local_9c,local_98));
      FUN_0000752c(0x4069000000000000,0,piVar2,0x100000c);
    }
    FUN_00000c2c(0,piVar2);
    FUN_000074d0(0x4069000000000000,0,0x3ff0000000000000,0,0,0x3ff0000000000000,piVar2,0x1000003);
    FUN_000094f8(piVar2,0x3039);
    *(undefined4 *)(param_5 + 4) = param_2;
    *param_5 = '\x01';
    FUN_00001fc8(param_5 + 8,param_3);
  }
  return;
}



void FUN_000098f0(undefined2 *param_1,uint param_2,undefined4 param_3)

{
  undefined4 uVar1;
  int iVar2;
  int iVar3;
  int *piVar4;
  char *pcVar5;
  undefined1 auStack_70 [4];
  undefined1 auStack_6c [4];
  undefined1 auStack_68 [48];
  
  DAT_0002f110 = 1;
  DAT_0002f10c = 0;
  DAT_0002f114 = 1;
  DAT_0002f118 = 0;
  DAT_0002f12b = 0;
  DAT_0002f128 = 0;
  DAT_0002f129 = 0;
  DAT_0002f12a = 0;
  DAT_0002f11c = 0;
  DAT_0002f120 = 0;
  DAT_0002f121 = 0;
  DAT_0002f1d0 = param_2;
  DAT_0002f1d8 = param_3;
  FUN_0000872c(&DAT_0002f130,&DAT_0002ce18);
  FUN_0000872c(&DAT_0002f14c,&DAT_0002ce18);
  FUN_0000872c(&DAT_0002f168,&DAT_0002ce18);
  FUN_0000872c(&DAT_0002f184,&DAT_0002ce18);
  *param_1 = 0;
  DAT_0002f1cc = 0;
  uVar1 = FUN_000277d0("premo_plugin");
  FUN_00025e38(auStack_6c,uVar1,"tex_premo_bar");
  FUN_000079dc(&DAT_0002f1c4,auStack_6c);
  FUN_000055f8(auStack_6c);
  FUN_00025e38(auStack_70,uVar1,"tex_premo_bar_shadow");
  FUN_000079dc(&DAT_0002f1c8,auStack_70);
  FUN_000055f8(auStack_70);
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    FUN_00025ee0(uVar1,"premo_page_btnnavi",0,0,3,0,0);
    iVar2 = FUN_00026730(uVar1,"premo_page_btnnavi");
    if (iVar2 != 0) {
      FUN_00000c2c(0,iVar2);
    }
    iVar2 = FUN_00026730(uVar1,"btnbar");
    if (iVar2 != 0) {
      FUN_00000c2c(0x3ff0000000000000,iVar2);
    }
    piVar4 = (int *)FUN_00026730(uVar1,"cross_shadow");
    if (piVar4 != (int *)0x0) {
      (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
    }
    piVar4 = (int *)FUN_00026730(uVar1,"circle_shadow");
    if (piVar4 != (int *)0x0) {
      (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
    }
    piVar4 = (int *)FUN_00026730(uVar1,"triangle_shadow");
    if (piVar4 != (int *)0x0) {
      (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
    }
    pcVar5 = "rectangle_shadow";
  }
  else {
    FUN_00025ee0(uVar1,"premo_page_btnnavi_fake16_9",0,0,3,0,0);
    iVar2 = FUN_00026730(uVar1,"premo_page_btnnavi_fake16_9");
    if (iVar2 != 0) {
      iVar3 = FUN_000271b0(iVar2,0);
      if (iVar3 != 0) {
        FUN_000021e4(iVar3,0x280,0x1e0);
      }
      FUN_00000c2c(0,iVar2);
    }
    iVar2 = FUN_00026730(uVar1,"btnbar_fake16_9");
    if (iVar2 != 0) {
      FUN_00000c2c(0x3ff0000000000000,iVar2);
    }
    piVar4 = (int *)FUN_00026730(uVar1,"cross_shadow_fake16_9");
    if (piVar4 != (int *)0x0) {
      (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
    }
    piVar4 = (int *)FUN_00026730(uVar1,"circle_shadow_fake16_9");
    if (piVar4 != (int *)0x0) {
      (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
    }
    piVar4 = (int *)FUN_00026730(uVar1,"triangle_shadow_fake16_9");
    if (piVar4 != (int *)0x0) {
      (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
    }
    pcVar5 = "rectangle_shadow_fake16_9";
  }
  piVar4 = (int *)FUN_00026730(uVar1,pcVar5);
  if (piVar4 != (int *)0x0) {
    (*(code *)**(undefined4 **)(*piVar4 + 0xa8))(piVar4,7,0xb);
  }
  iVar2 = FUN_000277d0("system_plugin");
  if (iVar2 != 0) {
    uVar1 = FUN_00026810(iVar2,"msg_back");
    FUN_000093ec(auStack_68,param_1,uVar1);
    FUN_00007888(auStack_68);
  }
  return;
}



void FUN_00009df4(int param_1,int param_2)

{
  *(undefined1 *)(param_1 + 0xf9) = 0;
  if (param_2 != 0) {
    *(undefined1 *)(param_1 + 0xfa) = 0;
  }
  FUN_000264c8();
  return;
}



void FUN_00009e2c(int param_1,int param_2)

{
  *(undefined1 *)(param_1 + 0xf9) = 1;
  if (param_2 != 0) {
    *(undefined1 *)(param_1 + 0xfa) = 1;
  }
  FUN_000264c8();
  return;
}



void FUN_00009e64(undefined4 param_1,int *param_2,int *param_3,undefined4 param_4,undefined4 param_5
                 ,ulonglong param_6)

{
  undefined4 *puVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  int iVar4;
  int *piVar5;
  int *piVar6;
  int *piVar7;
  int *piVar8;
  int *piVar9;
  undefined8 uVar10;
  char *pcVar11;
  char *pcVar12;
  undefined1 *puVar13;
  code *pcVar14;
  undefined1 auStack_1a0 [4];
  undefined1 auStack_19c [4];
  undefined1 auStack_198 [4];
  undefined1 auStack_194 [4];
  undefined8 local_190;
  undefined8 local_188;
  undefined8 local_180;
  undefined8 local_178;
  undefined8 local_170;
  undefined8 local_168;
  undefined8 local_160;
  undefined8 local_158;
  undefined8 local_150;
  undefined8 local_148;
  undefined8 local_140;
  undefined8 local_138;
  undefined8 local_130;
  undefined8 local_128;
  undefined8 local_120;
  undefined8 local_118;
  undefined1 auStack_110 [28];
  undefined1 auStack_f4 [28];
  undefined1 auStack_d8 [28];
  undefined1 auStack_bc [44];
  
  uVar2 = FUN_000277d0("premo_plugin");
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    FUN_00025ee0(uVar2,"premo_page_infobar",0,0,3,0,0);
    piVar5 = (int *)FUN_00026730(uVar2,"infobar");
    piVar6 = (int *)FUN_00026730(uVar2,"infoicon");
    piVar7 = (int *)FUN_00026730(uVar2,"infoicon_shadow");
    piVar8 = (int *)FUN_00026730(uVar2,"infotext1");
    piVar9 = (int *)FUN_00026730(uVar2,"infotext2");
    if ((param_6 & 1) == 0) {
      puVar1 = *(undefined4 **)(*piVar5 + 0x58);
      FUN_00025888(0,&local_120);
      (*(code *)*puVar1)(piVar5,0xb,0xdda,0,local_120,local_118);
      puVar1 = *(undefined4 **)(*piVar5 + 0x5c);
      FUN_00025888(0,&local_130);
      pcVar14 = (code *)*puVar1;
      uVar10 = 0xdd9;
      local_150 = local_130;
      local_148 = local_128;
    }
    else {
      puVar1 = *(undefined4 **)(*piVar5 + 0x58);
      FUN_00025888(0,&local_160);
      (*(code *)*puVar1)(piVar5,0xb,0xddc,0,local_160,local_158);
      puVar1 = *(undefined4 **)(*piVar5 + 0x5c);
      FUN_00025888(0,&local_150);
      pcVar14 = (code *)*puVar1;
      uVar10 = 0xddb;
    }
    (*pcVar14)(piVar5,7,uVar10,0,local_150,local_148);
    if ((param_6 & 2) == 0) {
      if ((param_6 & 4) != 0) {
        puVar13 = auStack_194;
        FUN_0000959c(0,0,0,0x3fe0000000000000,piVar5);
        puVar1 = *(undefined4 **)(*piVar5 + 0x110);
        FUN_00007a04(puVar13,0);
        goto LAB_0000a650;
      }
      FUN_00009df4(piVar5,0);
    }
    else {
      puVar13 = auStack_1a0;
      FUN_0000959c(0x3ff0000000000000,0x3ff0000000000000,0x3ff0000000000000,0x3ff0000000000000,
                   piVar5);
      puVar1 = *(undefined4 **)(*piVar5 + 0x110);
      FUN_00025e38(puVar13,uVar2,"tex_infobar");
LAB_0000a650:
      (*(code *)*puVar1)(piVar5,puVar13,0);
      FUN_000055f8(puVar13);
      FUN_00009e2c(piVar5,0);
    }
    (*(code *)**(undefined4 **)(*piVar7 + 0xa8))(piVar7,7,0xb);
    (*(code *)**(undefined4 **)(*piVar8 + 0xa8))(piVar8,0x13,0x20);
    FUN_00026960(piVar9,0x2000c,&PTR_LAB_0002eb38,0);
    (*(code *)**(undefined4 **)(*piVar6 + 0x110))(piVar6,param_2,0);
    if (*param_2 == 0) {
      FUN_00009df4(piVar6,1);
    }
    else {
      FUN_00009e2c(piVar6,1);
    }
    (*(code *)**(undefined4 **)(*piVar7 + 0x110))(piVar7,param_3,0);
    if (*param_3 == 0) {
      FUN_00009df4(piVar7,1);
    }
    else {
      FUN_00009e2c(piVar7,1);
    }
    puVar1 = *(undefined4 **)(*piVar8 + 0x118);
    FUN_000086e0(auStack_110,param_4);
    (*(code *)*puVar1)(piVar8,auStack_110,0);
    FUN_00007888(auStack_110);
    puVar1 = *(undefined4 **)(*piVar9 + 0x118);
    FUN_000086e0(auStack_bc,param_5);
    (*(code *)*puVar1)(piVar9,auStack_bc,0);
    FUN_00007888(auStack_bc);
    if (((param_6 & 0x10) == 0) && ((piVar9[8] & 0x1000U) != 0)) {
      FUN_00009df4(piVar8,1);
    }
    if (DAT_0002f120 != '\0') {
      return;
    }
    pcVar11 = "premo_page_infobar";
    pcVar12 = "infobar";
    goto LAB_0000a8b0;
  }
  FUN_00025ee0(uVar2,"premo_page_infobar_fake16_9",0,0,3,0,0);
  uVar3 = FUN_00026730(uVar2,"premo_page_infobar_fake16_9");
  iVar4 = FUN_000271b0(uVar3,0);
  if (iVar4 != 0) {
    FUN_000021e4(iVar4,0x280,0x1e0);
  }
  piVar5 = (int *)FUN_00026730(uVar2,"infobar_fake16_9");
  piVar6 = (int *)FUN_00026730(uVar2,"infoicon_fake16_9");
  piVar7 = (int *)FUN_00026730(uVar2,"infoicon_shadow_fake16_9");
  piVar8 = (int *)FUN_00026730(uVar2,"infotext1_fake16_9");
  piVar9 = (int *)FUN_00026730(uVar2,"infotext2_fake16_9");
  if ((param_6 & 1) == 0) {
    puVar1 = *(undefined4 **)(*piVar5 + 0x58);
    FUN_00025888(0,&local_190);
    (*(code *)*puVar1)(piVar5,0xdd8,0xde9,0,local_190,local_188);
    puVar1 = *(undefined4 **)(*piVar5 + 0x5c);
    FUN_00025888(0,&local_180);
    pcVar14 = (code *)*puVar1;
    uVar10 = 0xde8;
    local_170 = local_180;
    local_168 = local_178;
  }
  else {
    puVar1 = *(undefined4 **)(*piVar5 + 0x58);
    FUN_00025888(0,&local_140);
    (*(code *)*puVar1)(piVar5,0xdd8,0xdeb,0,local_140,local_138);
    puVar1 = *(undefined4 **)(*piVar5 + 0x5c);
    FUN_00025888(0,&local_170);
    pcVar14 = (code *)*puVar1;
    uVar10 = 0xdea;
  }
  (*pcVar14)(piVar5,0xdd7,uVar10,0,local_170,local_168);
  if ((param_6 & 2) == 0) {
    if ((param_6 & 4) != 0) {
      puVar13 = auStack_198;
      FUN_0000959c(0,0,0,0x3fe0000000000000,piVar5);
      puVar1 = *(undefined4 **)(*piVar5 + 0x110);
      FUN_00007a04(puVar13,0);
      goto LAB_0000a190;
    }
    FUN_00009df4(piVar5,0);
  }
  else {
    puVar13 = auStack_19c;
    FUN_0000959c(0x3ff0000000000000,0x3ff0000000000000,0x3ff0000000000000,0x3ff0000000000000,piVar5)
    ;
    puVar1 = *(undefined4 **)(*piVar5 + 0x110);
    FUN_00025e38(puVar13,uVar2,"tex_infobar");
LAB_0000a190:
    (*(code *)*puVar1)(piVar5,puVar13,0);
    FUN_000055f8(puVar13);
    FUN_00009e2c(piVar5,0);
  }
  (*(code *)**(undefined4 **)(*piVar7 + 0xa8))(piVar7,7,0xb);
  (*(code *)**(undefined4 **)(*piVar8 + 0xa8))(piVar8,0x13,0x20);
  FUN_00026960(piVar9,0x2000c,&PTR_LAB_0002eb38,0);
  (*(code *)**(undefined4 **)(*piVar6 + 0x110))(piVar6,param_2,0);
  if (*param_2 == 0) {
    FUN_00009df4(piVar6,1);
  }
  else {
    FUN_00009e2c(piVar6,1);
  }
  (*(code *)**(undefined4 **)(*piVar7 + 0x110))(piVar7,param_3,0);
  if (*param_3 == 0) {
    FUN_00009df4(piVar7,1);
  }
  else {
    FUN_00009e2c(piVar7,1);
  }
  puVar1 = *(undefined4 **)(*piVar8 + 0x118);
  FUN_000086e0(auStack_d8,param_4);
  (*(code *)*puVar1)(piVar8,auStack_d8,0);
  FUN_00007888(auStack_d8);
  puVar1 = *(undefined4 **)(*piVar9 + 0x118);
  FUN_000086e0(auStack_f4,param_5);
  (*(code *)*puVar1)(piVar9,auStack_f4,0);
  FUN_00007888(auStack_f4);
  if (((param_6 & 0x10) == 0) && ((piVar9[8] & 0x1000U) != 0)) {
    FUN_00009df4(piVar8,1);
  }
  if (DAT_0002f120 != '\0') {
    return;
  }
  pcVar11 = "premo_page_infobar_fake16_9";
  pcVar12 = "infobar_fake16_9";
LAB_0000a8b0:
  FUN_00009608(param_1,uVar2,pcVar11,pcVar12,&DAT_0002f1a0,param_6 >> 3 & 1);
  DAT_0002f120 = 1;
  return;
}



void FUN_0000ab1c(undefined4 param_1)

{
  undefined4 uVar1;
  undefined4 uVar2;
  undefined1 *puVar3;
  undefined8 uVar4;
  undefined4 uVar5;
  undefined1 auStack_400 [28];
  undefined1 auStack_3e4 [28];
  undefined1 auStack_3c8 [28];
  undefined1 auStack_3ac [28];
  undefined1 auStack_390 [28];
  undefined1 auStack_374 [28];
  undefined1 auStack_358 [28];
  undefined1 auStack_33c [28];
  undefined1 auStack_320 [28];
  undefined1 auStack_304 [28];
  undefined1 auStack_2e8 [28];
  undefined1 auStack_2cc [28];
  undefined1 auStack_2b0 [28];
  undefined1 auStack_294 [28];
  undefined1 auStack_278 [28];
  undefined1 auStack_25c [28];
  undefined1 auStack_240 [28];
  undefined1 auStack_224 [28];
  undefined1 auStack_208 [16];
  undefined1 local_1f8 [456];
  
  uVar1 = FUN_000277d0("premo_plugin");
  DAT_0002f10c = 0;
  if ((DAT_0002f1d0 & 0x1000) == 0) {
    uVar5 = 0xd;
    if (4 < (DAT_0002f1d8 & 0xffff) - 3) {
      uVar5 = 0xf;
    }
  }
  else {
    uVar5 = 0xc;
    if (4 < (DAT_0002f1d8 & 0xffff) - 3) {
      uVar5 = 0xe;
    }
  }
  if (DAT_0002f114 == 0) {
    puVar3 = auStack_358;
    uVar2 = FUN_00026810(uVar1,"msg_connect_from_psp_see_manual");
    FUN_00026dc0(puVar3,uVar2,param_1,0,0,0,0,0);
    uVar1 = FUN_00026730(uVar1,"premo_dialog_page");
    DAT_0002f10c = FUN_00025460(uVar1,puVar3,3,0,uVar5,0,0,0xffffffffffffffff);
    FUN_000254d0(0,DAT_0002f10c);
LAB_0000ae64:
    FUN_00025498(DAT_0002f10c,&PTR_LAB_0002eb48,param_1);
LAB_0000b350:
    FUN_00007888(puVar3);
    return;
  }
  if (DAT_0002f114 == 1) {
    uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
    puVar3 = auStack_374;
    uVar1 = FUN_00026810(uVar1,"msg_connect_from_psp");
  }
  else if (DAT_0002f114 == 2) {
    uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
    puVar3 = auStack_390;
    uVar1 = FUN_00026810(uVar1,"msg_psp_connection_running");
  }
  else {
    if (DAT_0002f114 != 10) {
      if (DAT_0002f114 == 0xb) {
        FUN_00025230(auStack_208);
        puVar3 = auStack_33c;
        uVar2 = FUN_00026810(uVar1,"msg_timer_application_running_auto_off_press_ps");
        FUN_00026dc0(puVar3,uVar2,local_1f8,0,0,0,0,0);
        uVar1 = FUN_00026730(uVar1,"premo_dialog_page");
        DAT_0002f10c = FUN_00025460(uVar1,puVar3,3,0,uVar5,0,0,0xffffffffffffffff);
        FUN_000254d0(0,DAT_0002f10c);
        goto LAB_0000ae64;
      }
      if (DAT_0002f114 == 4) {
        if (DAT_0002f11c == 0) {
          uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
          puVar3 = auStack_3e4;
          uVar1 = FUN_00026810(uVar1,"msg_remoteplay_register_psp_pls");
          goto LAB_0000b104;
        }
        uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
        puVar3 = auStack_3c8;
        uVar1 = FUN_00026810(uVar1,"msg_remoteplay_register_psp_pls");
LAB_0000b0b8:
        FUN_000086e0(puVar3,uVar1);
        uVar4 = 2;
        uVar1 = DAT_0002f1d4;
      }
      else {
        if (DAT_0002f114 == 5) {
          if (DAT_0002f11c != 0) {
            uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
            puVar3 = auStack_320;
            uVar1 = FUN_00026810(uVar1,"msg_to_operate_atrac_activate_pls");
            goto LAB_0000b0b8;
          }
          uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
          puVar3 = auStack_304;
          uVar1 = FUN_00026810(uVar1,"msg_to_operate_atrac_activate_pls");
        }
        else if (DAT_0002f114 == 6) {
          if (DAT_0002f11c != 0) {
            uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
            puVar3 = auStack_240;
            uVar1 = FUN_00026810(uVar1,"msg_error_disable_internet_prompt");
            goto LAB_0000b0b8;
          }
          uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
          puVar3 = auStack_25c;
          uVar1 = FUN_00026810(uVar1,"msg_error_disable_internet_prompt");
        }
        else if (DAT_0002f114 == 7) {
          if (DAT_0002f11c != 0) {
            uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
            puVar3 = auStack_278;
            uVar1 = FUN_00026810(uVar1,"msg_error_no_netcable");
            goto LAB_0000b0b8;
          }
          uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
          puVar3 = auStack_294;
          uVar1 = FUN_00026810(uVar1,"msg_error_no_netcable");
        }
        else {
          if (DAT_0002f114 != 8) {
            if (DAT_0002f114 == 9) {
              if (DAT_0002f11c == 0) {
                uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
                uVar1 = FUN_00026810(uVar1,"msg_wait");
                puVar3 = auStack_400;
                FUN_000086e0(puVar3,uVar1);
                uVar4 = 3;
                uVar1 = 0;
              }
              else {
                uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
                uVar1 = FUN_00026810(uVar1,"msg_wait");
                puVar3 = auStack_2e8;
                FUN_000086e0(puVar3,uVar1);
                uVar4 = 2;
                uVar1 = DAT_0002f1d4;
              }
              DAT_0002f10c = FUN_00025460(uVar2,puVar3,uVar4,uVar1,uVar5,0,0,0xffffffffffffffff);
              FUN_00007888(puVar3);
              FUN_000254d0(0,DAT_0002f10c);
              FUN_00025498(DAT_0002f10c,&PTR_LAB_0002eb48,param_1);
              FUN_00007e84(param_1,0);
              return;
            }
            FUN_000078b0(auStack_224);
            uVar2 = FUN_00026810(uVar1,"msg_error");
            FUN_0000872c(auStack_224,uVar2);
            FUN_00025620(auStack_224,DAT_0002f118);
            if (DAT_0002f11c == 0) {
              uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
              uVar4 = 3;
              uVar1 = 0;
            }
            else {
              uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
              uVar4 = 2;
              uVar1 = DAT_0002f1d4;
            }
            DAT_0002f10c = FUN_00025460(uVar2,auStack_224,uVar4,uVar1,uVar5,0,0,0xffffffffffffffff);
            FUN_000254d0(0,DAT_0002f10c);
            FUN_00025498(DAT_0002f10c,&PTR_LAB_0002eb48,param_1);
            puVar3 = auStack_224;
            goto LAB_0000b350;
          }
          if (DAT_0002f11c != 0) {
            uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
            puVar3 = auStack_2b0;
            uVar1 = FUN_00026810(uVar1,"msg_error_connect_net_prompt");
            goto LAB_0000b0b8;
          }
          uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
          puVar3 = auStack_2cc;
          uVar1 = FUN_00026810(uVar1,"msg_error_connect_net_prompt");
        }
LAB_0000b104:
        FUN_000086e0(puVar3,uVar1);
        uVar4 = 3;
        uVar1 = 0;
      }
      DAT_0002f10c = FUN_00025460(uVar2,puVar3,uVar4,uVar1,uVar5,0,0,0xffffffffffffffff);
      FUN_00007888(puVar3);
      FUN_000254d0(0,DAT_0002f10c);
      goto LAB_0000ad44;
    }
    uVar2 = FUN_00026730(uVar1,"premo_dialog_page");
    puVar3 = auStack_3ac;
    uVar1 = FUN_00026810(uVar1,"msg_connect_from_psp_turn_off_auto");
  }
  FUN_000086e0(puVar3,uVar1);
  DAT_0002f10c = FUN_00025460(uVar2,puVar3,3,0,uVar5,0,0,0xffffffffffffffff);
  FUN_00007888(puVar3);
  FUN_000254d0(0,DAT_0002f10c);
LAB_0000ad44:
  FUN_00025498(DAT_0002f10c,&PTR_LAB_0002eb48,param_1);
  return;
}



void FUN_0000b380(undefined4 param_1)

{
  undefined4 uVar1;
  undefined1 auStack_40 [4];
  undefined1 auStack_3c [20];
  
  uVar1 = FUN_000277d0("premo_plugin");
  if (DAT_0002f110 == 1) {
    FUN_0000ab1c(param_1);
    FUN_00005694(auStack_3c,&DAT_0002f1c4);
    FUN_00005694(auStack_40,&DAT_0002f1c8);
    uVar1 = FUN_00026810(uVar1,"msg_psp_connection");
    FUN_00009e64(param_1,auStack_3c,auStack_40,uVar1,&DAT_0002ce18,0);
    FUN_000055f8(auStack_40);
    FUN_000055f8(auStack_3c);
    FUN_00007e84(param_1,1);
    DAT_0002f110 = 0;
  }
  else {
    FUN_00025498(DAT_0002f10c,&PTR_LAB_0002eb40,param_1);
    FUN_00025508(DAT_0002f10c);
  }
  return;
}



void FUN_0000b49c(undefined2 *param_1,undefined4 param_2,int param_3)

{
  undefined1 local_20 [4];
  undefined1 local_1c [20];
  
  if (param_3 == 0) {
    *param_1 = 0;
  }
  else {
    FUN_00024f90(param_3,local_20,param_1,local_1c);
  }
  DAT_0002f114 = param_2;
  DAT_0002f11c = 0;
  return;
}



undefined8 FUN_0000b514(void)

{
  while (DAT_0002f110 != 1) {
    syscall();
  }
  return 1000;
}



ulonglong FUN_0000b578(int param_1)

{
  return (longlong)param_1 * 0x41000 + 0x28fffU & 0xffff0000;
}



undefined4 FUN_0000b594(int *param_1)

{
  syscall();
  return *(undefined4 *)(*param_1 + 4);
}



undefined4 FUN_0000b5c0(int param_1)

{
  syscall();
  return **(undefined4 **)(param_1 + 4);
}



undefined4 FUN_0000b5ec(int param_1)

{
  syscall();
  return *(undefined4 *)(param_1 + 4);
}



undefined4 FUN_0000b614(int param_1)

{
  syscall();
  return *(undefined4 *)(param_1 + 8);
}



undefined4 FUN_0000b63c(int param_1,int param_2)

{
  int iVar1;
  undefined4 uVar2;
  undefined **ppuVar3;
  undefined **ppuVar4;
  
  if ((param_1 == 0) || (param_1 == 3)) {
LAB_0000b6f4:
    uVar2 = 1;
  }
  else {
    if (param_2 != 0) {
      if (param_1 == 1) {
        ppuVar3 = &PTR_s_NPEA00135_0002e58c;
        do {
          ppuVar4 = ppuVar3 + 1;
          iVar1 = FUN_00029a60(param_2,*ppuVar3);
          if (iVar1 == 0) goto LAB_0000b6f4;
          ppuVar3 = ppuVar4;
        } while (ppuVar4 != &PTR_s_NPJA00001_0002e59c);
      }
      else if (param_1 == 2) {
        ppuVar3 = &PTR_s_NPJA00001_0002e59c;
        do {
          ppuVar4 = ppuVar3 + 1;
          iVar1 = FUN_00029a60(param_2,*ppuVar3);
          if (iVar1 == 0) goto LAB_0000b6f4;
          ppuVar3 = ppuVar4;
        } while (ppuVar4 != &PTR_s_00000001274d40158d8d40f047602200_0002e5b4);
      }
    }
    uVar2 = 0;
  }
  return uVar2;
}



void FUN_0000b71c(int *param_1)

{
  FUN_0000d0cc(*(undefined4 *)(*param_1 + 0x48));
  return;
}



undefined4 FUN_0000b744(int *param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0000c6e4(*(undefined4 *)(*param_1 + 0x4c));
  return uVar1;
}



int FUN_0000b770(int *param_1)

{
  int iVar1;
  int iVar2;
  
  iVar2 = 0;
  if (((param_1 != (int *)0x0) && (*param_1 != 0)) &&
     (iVar1 = *(int *)(*param_1 + 0x4c), iVar1 != 0)) {
    iVar2 = FUN_0000c830(iVar1);
    if (-1 < iVar2) {
      FUN_000227fc(*(undefined4 *)(*param_1 + 0x4c));
      *(undefined4 *)(*param_1 + 0x4c) = 0;
    }
  }
  return iVar2;
}



undefined4 FUN_0000b7f8(int *param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0000c8a8(*(undefined4 *)(*param_1 + 0x4c));
  return uVar1;
}



undefined4 FUN_0000b824(int *param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  uVar1 = 0;
  if (param_1 != (int *)0x0) {
    if ((*param_1 != 0) && (*(int *)(*param_1 + 0x48) != 0)) {
      FUN_0000b594(param_1);
      uVar1 = FUN_00013158(*(undefined4 *)(*param_1 + 0x48),param_2);
      syscall();
    }
  }
  return uVar1;
}



undefined1 * FUN_0000b8b8(int *param_1,undefined4 param_2,uint *param_3)

{
  uint uVar1;
  int iVar2;
  int iVar3;
  undefined1 *puVar4;
  undefined4 uVar5;
  undefined1 auStack_40 [4];
  uint local_3c;
  
  syscall();
  if ((int)*(undefined1 **)(*param_1 + 4) < 0) {
    return *(undefined1 **)(*param_1 + 4);
  }
  if (((((((*param_3 < 4) && (-1 < (int)param_3[1])) && (param_3[2] != 0)) &&
        ((param_3[6] - 1 < 4 || (param_3[6] == 0xff)))) &&
       ((param_3[0xb] == 1 && ((-1 < (int)param_3[0xc] && (param_3[0xd] < 2)))))) &&
      ((uVar1 = param_3[0xe], uVar1 == 7 || (((uVar1 == 10 || (uVar1 == 0xf)) || (uVar1 == 0x1e)))))
      ) && (param_3[0x10] < 3)) {
    if ((1 < param_3[0x11] - 0x71) && (puVar4 = (undefined1 *)0x80029804, param_3[0x11] != 0x10ff))
    goto LAB_0000ba40;
    puVar4 = auStack_40;
    syscall();
    if (0xfff < local_3c) {
      if ((-1 < (int)puVar4) &&
         (puVar4 = (undefined1 *)FUN_0000d73c(*(undefined4 *)(*param_1 + 0x48),param_2,param_3),
         -1 < (int)puVar4)) {
        iVar2 = *param_1;
        uVar5 = FUN_0002285c(0x90);
        iVar3 = *param_1;
        puVar4 = (undefined1 *)0x80029801;
        *(undefined4 *)(iVar2 + 0x4c) = uVar5;
        if (*(int *)(iVar3 + 0x4c) != 0) {
          puVar4 = (undefined1 *)FUN_0000c6a8(*(int *)(iVar3 + 0x4c),param_1,param_3[0x12]);
        }
      }
      goto LAB_0000ba40;
    }
  }
  puVar4 = (undefined1 *)0x80029804;
LAB_0000ba40:
  syscall();
  return puVar4;
}



undefined4 FUN_0000baa0(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0000d8a4();
  return uVar1;
}



undefined4 FUN_0000bac4(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0000d8d8();
  return uVar1;
}



undefined4 FUN_0000bb10(int *param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0000d90c(*(undefined4 *)(*param_1 + 0x48));
  return uVar1;
}



undefined4 FUN_0000bb3c(int *param_1)

{
  int iVar1;
  undefined4 uVar2;
  
  uVar2 = 0;
  if (param_1 != (int *)0x0) {
    if (*param_1 != 0) {
      iVar1 = *(int *)(*param_1 + 0x48);
      if (iVar1 != 0) {
        FUN_0000d970(iVar1);
        FUN_000227fc(*(undefined4 *)(*param_1 + 0x48));
        *(undefined4 *)(*param_1 + 0x48) = 0;
      }
      syscall();
      if (*param_1 != 0) {
        FUN_000227fc(*param_1);
        *param_1 = 0;
      }
    }
    uVar2 = FUN_0002288c();
  }
  return uVar2;
}



undefined1 * FUN_0000bbe0(int *param_1,int param_2,undefined4 param_3)

{
  int iVar1;
  undefined4 uVar2;
  undefined1 *puVar3;
  int iVar4;
  undefined1 auStack_70 [8];
  undefined1 auStack_68 [8];
  undefined1 auStack_60 [4];
  uint local_5c;
  undefined4 local_58;
  undefined4 local_54;
  undefined8 local_50;
  undefined4 local_48;
  undefined8 local_40;
  
  uVar2 = FUN_00027bf8(0);
  FUN_000279c8(uVar2);
  if (*(short *)(param_2 + 4) != 0x244d) {
    return (undefined1 *)0x80029804;
  }
  if (3 < *(uint *)(param_2 + 8)) {
    return (undefined1 *)0x80029804;
  }
  iVar4 = *(int *)(param_2 + 0xc);
  if ((((iVar4 == 0) || (iVar4 == 0x11)) || (iVar4 == 0x12)) ||
     (((iVar4 == 0x13 || (iVar4 == 0x21)) ||
      ((iVar4 == 0x22 || ((iVar4 == 0x23 || (iVar4 == 0x40)))))))) {
    if ((*(int *)(param_2 + 0x14) < 0) ||
       ((((*(int *)(param_2 + 0x18) < 0 || (0xbff < *(uint *)(param_2 + 0x1c))) ||
         (0xff < *(uint *)(param_2 + 0x20))) ||
        ((*(int *)(param_2 + 0x24) == 0 ||
         (*(uint *)(param_2 + 0x28) < (*(int *)(param_2 + 0x18) * 0x41000 + 0x28fffU & 0xffff0000)))
        )))) {
      return (undefined1 *)0x80029804;
    }
    syscall();
    if (local_5c < 0x1000) {
      return (undefined1 *)0x80029804;
    }
    if ((int)auStack_60 < 0) {
      return auStack_60;
    }
  }
  puVar3 = (undefined1 *)
           FUN_000228dc(*(undefined4 *)(param_2 + 0x24),*(undefined4 *)(param_2 + 0x28));
  if ((int)puVar3 < 0) {
    return puVar3;
  }
  FUN_000227b4(auStack_70);
  iVar4 = FUN_00022828(0x40,0x50);
  *param_1 = iVar4;
  if (iVar4 == 0) {
    FUN_0002288c();
    return (undefined1 *)0x80029801;
  }
  FUN_00029ad0(iVar4,0,0x50);
  FUN_00029b08(*param_1 + 8,param_2,0x40);
  local_58 = 2;
  local_54 = 0x200;
  local_48 = 0;
  local_50 = 0;
  local_40 = 0x5f70705f63303200;
  puVar3 = (undefined1 *)(*param_1 + 4);
  syscall();
  if (-1 < (int)puVar3) {
    iVar4 = *param_1;
    uVar2 = FUN_0002285c(0x180,&local_58);
    iVar1 = *param_1;
    *(undefined4 *)(iVar4 + 0x48) = uVar2;
    if (*(int *)(iVar1 + 0x48) == 0) {
      puVar3 = (undefined1 *)0x80029801;
    }
    else {
      puVar3 = (undefined1 *)FUN_0000dc3c(*(int *)(iVar1 + 0x48),param_2,param_3);
      if (-1 < (int)puVar3) {
        FUN_000227b4(auStack_68);
        return puVar3;
      }
    }
  }
  syscall();
  iVar4 = *param_1;
  if (*(int *)(iVar4 + 0x48) != 0) {
    FUN_0000d970(*(int *)(iVar4 + 0x48));
    FUN_000227fc(*(undefined4 *)(*param_1 + 0x48));
    iVar4 = *param_1;
    *(undefined4 *)(iVar4 + 0x48) = 0;
    if (iVar4 == 0) goto LAB_0000be90;
  }
  FUN_000227fc(iVar4);
  *param_1 = 0;
LAB_0000be90:
  FUN_0002288c();
  return puVar3;
}



int FUN_0000bec8(int *param_1)

{
  int iVar1;
  int local_20 [4];
  
  iVar1 = FUN_0000b594();
  if (-1 < iVar1) {
    iVar1 = FUN_0000e2bc(*(undefined4 *)(*param_1 + 0x48),local_20);
    if (-1 < iVar1) {
      iVar1 = local_20[0];
    }
    syscall();
  }
  return iVar1;
}



undefined4 FUN_0000bf3c(int *param_1,undefined4 param_2)

{
  FUN_0000b594();
  FUN_0000e74c(*(undefined4 *)(*param_1 + 0x48),param_2);
  syscall();
  return *(undefined4 *)(*param_1 + 4);
}



undefined4 FUN_0000bf94(int *param_1,undefined2 param_2)

{
  FUN_0000b594();
  FUN_0000e7a0(*(undefined4 *)(*param_1 + 0x48),param_2);
  syscall();
  return *(undefined4 *)(*param_1 + 4);
}



undefined4 FUN_0000bfec(int *param_1)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  uVar1 = FUN_0000d284(*(undefined4 *)(*param_1 + 0x48));
  syscall();
  return uVar1;
}



undefined4 FUN_0000c044(int *param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  uVar1 = FUN_00012ec4(*(undefined4 *)(*param_1 + 0x48),param_2,param_3);
  syscall();
  return uVar1;
}



undefined4 FUN_0000c0b4(int *param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  undefined1 auStack_40 [24];
  
  FUN_0000b594();
  uVar1 = FUN_00012fc8(*(undefined4 *)(*param_1 + 0x48),param_2,param_3);
  FUN_000227b4(auStack_40);
  syscall();
  return uVar1;
}



undefined4 FUN_0000c12c(int *param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  uVar1 = FUN_0000e4cc(*(undefined4 *)(*param_1 + 0x48),param_2);
  syscall();
  return uVar1;
}



undefined4 FUN_0000c18c(int *param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  uVar1 = FUN_0000e558(*(undefined4 *)(*param_1 + 0x48),param_2,param_3,param_4);
  syscall();
  return uVar1;
}



undefined4 FUN_0000c20c(int *param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  uVar1 = FUN_0000e604(*(undefined4 *)(*param_1 + 0x48),param_2);
  syscall();
  return uVar1;
}



int FUN_0000c26c(int *param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4,
                undefined2 param_5)

{
  int iVar1;
  
  iVar1 = FUN_0000b594();
  if (-1 < iVar1) {
    iVar1 = FUN_0000e690(*(undefined4 *)(*param_1 + 0x48),param_2,param_3,param_4,param_5);
    syscall();
  }
  return iVar1;
}



undefined4
FUN_0000c310(int *param_1,char param_2,int param_3,undefined4 param_4,undefined4 param_5,
            undefined4 param_6,undefined1 param_7,char param_8)

{
  int iVar1;
  uint uVar2;
  undefined4 uVar3;
  char in_stack_00000077;
  
  FUN_0000b594();
  iVar1 = *param_1;
  uVar2 = (int)*(char *)(iVar1 + 0x45) >> 0x1f;
  if ((param_2 != '\0') && ((param_2 != '\x01' || (*(char *)(iVar1 + 0x44) != '\0')))) {
    uVar3 = 0x80029804;
    if (param_3 == 0) goto LAB_0000c438;
    if (((((param_8 == '\0') || (param_8 == '\x02')) || (param_8 == '\x03')) || (param_8 == '\x01'))
       && (in_stack_00000077 == '\0')) {
      uVar3 = FUN_0000e7f4(*(undefined4 *)(iVar1 + 0x48),param_3,
                           ((byte)((int)(((uVar2 ^ (int)*(char *)(iVar1 + 0x45)) - uVar2) + -1) >>
                                  0x1f) & 0xfe) + 3,param_6,param_5,param_4,param_7,param_8);
      goto LAB_0000c438;
    }
  }
  uVar3 = 0x80029843;
LAB_0000c438:
  syscall();
  return uVar3;
}



undefined4 FUN_0000c47c(int *param_1,int param_2,int param_3)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  if (param_3 != 0) {
    if (param_2 != 0) {
      uVar1 = FUN_0000e9b4(*(undefined4 *)(*param_1 + 0x48),param_2,param_3);
      goto LAB_0000c4dc;
    }
  }
  uVar1 = 0x80029804;
LAB_0000c4dc:
  syscall();
  return uVar1;
}



undefined4 FUN_0000c510(int *param_1,undefined4 param_2,undefined4 param_3,int param_4)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  if (param_4 == 0) {
    uVar1 = FUN_0000eb98(*(undefined4 *)(*param_1 + 0x48),param_2,param_3);
  }
  else {
    uVar1 = FUN_0000ea60(*(undefined4 *)(*param_1 + 0x48),param_2,param_3,param_4);
  }
  syscall();
  return uVar1;
}



undefined4 FUN_0000c5bc(int *param_1,int param_2)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  if (param_2 == 0) {
    uVar1 = FUN_0000ec8c(*(undefined4 *)(*param_1 + 0x48));
  }
  else {
    uVar1 = FUN_0000eb0c(*(undefined4 *)(*param_1 + 0x48),param_2);
  }
  syscall();
  return uVar1;
}



undefined4 FUN_0000c638(int *param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  
  FUN_0000b594();
  uVar1 = FUN_0000ed40(*(undefined4 *)(*param_1 + 0x48),param_2,param_3);
  syscall();
  return uVar1;
}



undefined4 FUN_0000c6a8(undefined4 *param_1,undefined4 param_2,int param_3)

{
  undefined4 uVar1;
  
  *param_1 = 1;
  param_1[10] = 0x400;
  param_1[0x10] = param_2;
  param_1[0x21] = 0;
  uVar1 = 0x80029830;
  if (param_3 == 2) {
    uVar1 = 0;
    param_1[9] = 2;
  }
  return uVar1;
}



undefined8 FUN_0000c6e4(int param_1,longlong param_2)

{
  if (param_2 == 0) {
    *(undefined4 *)(param_1 + 0x84) = 0;
  }
  else {
    *(uint *)(param_1 + 0x84) = *(uint *)(param_1 + 0x84) | 1;
  }
  *(undefined4 *)(param_1 + 0x88) = 1;
  return 0;
}



uint FUN_0000c714(uint *param_1)

{
  uint uVar1;
  
  if ((*param_1 & 0x100) != 0) {
    FUN_000227fc(param_1[0xd]);
  }
  if (((*param_1 & 0x10) == 0) || (uVar1 = FUN_00028950(param_1 + 7,param_1[8]), -1 < (int)uVar1)) {
    uVar1 = 0;
  }
  if ((*param_1 & 8) != 0) {
    syscall();
    if (((int)param_1[6] < 0) && (-1 < (int)uVar1)) {
      uVar1 = param_1[6];
    }
  }
  if ((*param_1 & 4) != 0) {
    syscall();
    if (((int)param_1[6] < 0) && (-1 < (int)uVar1)) {
      uVar1 = param_1[6];
    }
  }
  if ((*param_1 & 2) != 0) {
    syscall();
    if (((int)param_1[5] < 0) && (-1 < (int)uVar1)) {
      uVar1 = param_1[5];
    }
  }
  *param_1 = 0;
  return uVar1;
}



undefined4 FUN_0000c830(uint *param_1)

{
  uint uVar1;
  undefined4 uVar2;
  undefined1 auStack_20 [16];
  
  uVar1 = *param_1;
  *param_1 = uVar1 | 0x400;
  if ((uVar1 & 0x200) != 0) {
    syscall();
  }
  *param_1 = *param_1 & 0xfffff9ff;
  uVar2 = FUN_0000c714(param_1,auStack_20);
  return uVar2;
}



uint * FUN_0000c8a8(uint *param_1,uint param_2)

{
  uint *puVar1;
  uint uVar2;
  undefined1 auStack_50 [32];
  
  puVar1 = (uint *)0x80029832;
  if ((*param_1 & 1) != 0) {
    puVar1 = param_1 + 5;
    syscall();
    if (-1 < (int)puVar1) {
      puVar1 = param_1 + 6;
      *param_1 = *param_1 | 2;
      syscall();
      if (-1 < (int)puVar1) {
        puVar1 = (uint *)param_1[6];
        *param_1 = *param_1 | 4;
        syscall();
        if (-1 < (int)puVar1) {
          *param_1 = *param_1 | 8;
          puVar1 = (uint *)FUN_000289c0(auStack_50,param_1[5],0,1);
          if ((-1 < (int)puVar1) &&
             (puVar1 = (uint *)FUN_00028988(auStack_50,param_1 + 7,param_1 + 8), -1 < (int)puVar1))
          {
            *param_1 = *param_1 | 0x10;
            param_1[0xb] = 0;
            param_1[0xc] = param_1[10] >> 8;
            uVar2 = FUN_0002285c(param_1[10] << 4);
            param_1[0xd] = uVar2;
            if (uVar2 == 0) {
              param_1[0xe] = 0;
              puVar1 = (uint *)0x80029801;
            }
            else {
              param_1[0xe] = 0;
              *param_1 = *param_1 | 0x100;
              param_1[0xf] = param_2;
              puVar1 = (uint *)FUN_000298a0(param_1 + 2,&PTR_LAB_0002eb50,param_1,0x65,0x2400,1,
                                            "cellPremoStartSkimAudio");
              if (-1 < (int)puVar1) {
                *param_1 = *param_1 | 0x200;
                return puVar1;
              }
            }
          }
        }
      }
    }
    FUN_0000c714(param_1);
  }
  return puVar1;
}



int FUN_0000cac0(int param_1,undefined4 param_2)

{
  undefined4 uVar1;
  int iVar2;
  int iVar3;
  int iVar4;
  int iVar5;
  int *piVar6;
  undefined1 local_1da0 [4];
  undefined1 local_1d9c [4];
  undefined1 auStack_1d98 [4];
  undefined4 auStack_1d94 [367];
  undefined1 auStack_17d8 [16];
  int local_17c8;
  
  if (*(int *)(param_1 + 0x3c) == 0) {
    iVar2 = *(int *)(param_1 + 0x18);
    syscall();
    if ((iVar2 < 0) && (iVar2 == -0x7ffefff6)) {
      iVar2 = -0x7ffd67f7;
    }
  }
  else {
    uVar1 = *(undefined4 *)(param_1 + 0x40);
    iVar2 = FUN_0000c638(uVar1,auStack_1d98,local_1da0);
    piVar6 = (int *)(param_1 + 0x44);
    iVar4 = 0;
    iVar5 = 0;
    if (-1 < iVar2) {
      for (; iVar4 < 0x10; iVar4 = iVar4 + 1) {
        iVar3 = FUN_0000c510(uVar1,auStack_17d8,local_1d9c,
                             *(undefined4 *)((int)auStack_1d94 + iVar5));
        if (iVar3 < 0) {
          if (-1 < iVar2) {
            iVar2 = iVar3;
          }
        }
        else {
          if (local_17c8 == 0x1f) {
            iVar3 = FUN_0000c18c(uVar1,*(undefined4 *)((int)auStack_1d94 + iVar5),param_2,0);
            if ((((iVar3 < 0) && (iVar3 != -0x7ffd67f7)) && (iVar3 != -0x7ffd67f9)) && (-1 < iVar2))
            {
              iVar2 = iVar3;
            }
            iVar3 = 1;
          }
          else {
            if (*piVar6 == 0) goto LAB_0000cc60;
            iVar3 = FUN_0000c12c(uVar1,*(undefined4 *)((int)auStack_1d94 + iVar5));
            if ((iVar3 < 0) && (-1 < iVar2)) {
              iVar2 = iVar3;
            }
            iVar3 = 0;
          }
          *piVar6 = iVar3;
        }
LAB_0000cc60:
        piVar6 = piVar6 + 1;
        iVar5 = iVar5 + 0x5c;
      }
    }
  }
  return iVar2;
}



undefined4 FUN_0000d078(int param_1)

{
  syscall();
  return **(undefined4 **)(param_1 + 4);
}



undefined4 FUN_0000d0a4(int param_1)

{
  syscall();
  return *(undefined4 *)(param_1 + 8);
}



void FUN_0000d0cc(int param_1,undefined4 param_2)

{
  int iVar1;
  
  for (iVar1 = 0; iVar1 < *(int *)(param_1 + 0x28); iVar1 = iVar1 + 1) {
    FUN_0001a220(iVar1 * 0x1f0 + *(int *)(param_1 + 0x54),param_2);
  }
  return;
}



int FUN_0000d138(int param_1,longlong *param_2)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_1a0 [8];
  longlong local_198;
  
  iVar1 = 0;
  do {
    iVar2 = iVar1;
    if (*(int *)(param_1 + 0x28) <= iVar2) {
      return -0x7ffd67fd;
    }
    FUN_0001a720(iVar2 * 0x1f0 + *(int *)(param_1 + 0x54),auStack_1a0);
    iVar1 = iVar2 + 1;
  } while (local_198 != *param_2);
  return iVar2;
}



int FUN_0000d1d8(int param_1,undefined4 param_2)

{
  int iVar1;
  int iVar2;
  int iVar3;
  undefined1 auStack_1a0 [84];
  undefined1 auStack_14c [300];
  
  iVar1 = 0;
  do {
    iVar3 = iVar1;
    if (*(int *)(param_1 + 0x28) <= iVar3) {
      return -0x7ffd67fd;
    }
    FUN_0001a720(iVar3 * 0x1f0 + *(int *)(param_1 + 0x54),auStack_1a0);
    iVar2 = FUN_00029de0(param_2,auStack_14c,0x10);
    iVar1 = iVar3 + 1;
  } while (iVar2 != 0);
  return iVar3;
}



// WARNING: Removing unreachable block (ram,0x0000d2cc)
// WARNING: Removing unreachable block (ram,0x0000d2fc)
// WARNING: Removing unreachable block (ram,0x0000d30c)
// WARNING: Removing unreachable block (ram,0x0000d310)
// WARNING: Removing unreachable block (ram,0x0000d320)
// WARNING: Removing unreachable block (ram,0x0000d328)
// WARNING: Removing unreachable block (ram,0x0000d330)
// WARNING: Removing unreachable block (ram,0x0000d334)

undefined4 FUN_0000d284(int param_1)

{
  undefined4 uVar1;
  undefined1 local_30 [16];
  
  uVar1 = FUN_00020b88(*(undefined4 *)(param_1 + 0x60),local_30);
  return uVar1;
}



int FUN_0000d378(int param_1,int param_2,ulonglong param_3)

{
  ulonglong uVar1;
  int iVar2;
  int iVar3;
  
  iVar2 = FUN_0000b5c0();
  if (iVar2 < 0) {
    return iVar2;
  }
  iVar2 = FUN_0000d138(param_1,param_2);
  if (-1 < iVar2) {
    iVar2 = iVar2 * 0x1f0 + *(int *)(param_1 + 0x54);
    if (((*(char *)(param_1 + 0x4c) == '\0') && (*(short *)(param_2 + 0x20) == 0)) &&
       (iVar3 = FUN_0001a29c(iVar2,param_2 + 8), iVar3 < 0)) goto LAB_0000d484;
    iVar3 = FUN_0001a1f4(iVar2);
    if (iVar3 != 0) {
      if (*(int *)(param_2 + 0x18) == 0) {
        *(undefined4 *)(param_2 + 0x18) = *(undefined4 *)(iVar2 + 0x1e4);
      }
      FUN_0001332c(iVar3,param_2);
      uVar1 = (ulonglong)((int)param_3 >> 0x1f);
      FUN_00013268(iVar3,((uVar1 ^ param_3) - uVar1) - 1 >> 0x1f & 1);
      iVar3 = FUN_0001aac0(iVar2,param_2,(int)param_3);
      goto LAB_0000d484;
    }
  }
  iVar3 = -1;
LAB_0000d484:
  syscall();
  return iVar3;
}



int FUN_0000d4c0(int param_1,int param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_0000b5c0();
  if (iVar1 < 0) {
    return iVar1;
  }
  iVar1 = FUN_0000d138(param_1,param_2);
  iVar2 = -0x7ffd67e0;
  if (-1 < iVar1) {
    iVar1 = iVar1 * 0x1f0 + *(int *)(param_1 + 0x54);
    if (*(char *)(param_1 + 0x4c) == '\0') {
      if ((*(short *)(param_2 + 0x30) == 0) && (iVar2 = FUN_0001a29c(iVar1,param_2 + 8), iVar2 < 0))
      goto LAB_0000d580;
    }
    iVar2 = FUN_0001abf4(iVar1,param_2,param_3,param_4);
  }
LAB_0000d580:
  syscall();
  return iVar2;
}



int FUN_0000d5c0(int param_1)

{
  int iVar1;
  int iVar2;
  int iVar3;
  
  iVar1 = FUN_0000d138();
  if (-1 < iVar1) {
    iVar3 = iVar1 * 0x1f0;
    iVar1 = FUN_0001ad24(iVar3 + *(int *)(param_1 + 0x54),0);
    iVar2 = FUN_0001a1f4(iVar3 + *(int *)(param_1 + 0x54));
    if (iVar2 != 0) {
      FUN_0001a1e8(iVar3 + *(int *)(param_1 + 0x54));
      iVar1 = FUN_0001349c(iVar2,iVar3 + *(int *)(param_1 + 0x54));
      if (iVar1 < 0) {
        return iVar1;
      }
    }
    *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + -1;
  }
  return iVar1;
}



undefined4 FUN_0000d68c(undefined4 param_1,int param_2,int *param_3)

{
  undefined4 uVar1;
  int iVar2;
  int iVar3;
  int local_40 [6];
  
  iVar3 = 0;
  while( true ) {
    iVar2 = FUN_00021610(param_1);
    if (iVar2 <= iVar3) {
      return 0;
    }
    uVar1 = FUN_00021664(param_1,iVar3);
    FUN_00013248(uVar1,local_40);
    if (local_40[0] == param_2) break;
    iVar3 = iVar3 + 1;
  }
  *param_3 = iVar3;
  return uVar1;
}



int FUN_0000d73c(int param_1,undefined4 param_2,int param_3)

{
  int iVar1;
  int iVar2;
  int iVar3;
  
  iVar1 = FUN_0000d078();
  if (iVar1 < 0) {
    return iVar1;
  }
  iVar1 = -0x7ffd67fe;
  if (param_3 != 0) {
    iVar1 = -0x7ffd67ff;
    iVar2 = FUN_00019a18(*(undefined4 *)(param_3 + 8),*(undefined4 *)(param_3 + 0xc),
                         *(undefined4 *)(param_3 + 0x10));
    if (iVar2 != 0) {
      iVar3 = FUN_00019910(iVar2,200);
      iVar1 = -0x7ffd67ff;
      if (iVar3 != 0) {
        iVar1 = FUN_00013b90(iVar3,param_2,param_3,*(undefined4 *)(param_1 + 0x6c),
                             *(undefined4 *)(param_1 + 100),*(undefined4 *)(param_1 + 0x2c),
                             *(undefined4 *)(param_1 + 0x30),iVar2);
        if (-1 < iVar1) {
          iVar1 = FUN_00021738(*(undefined4 *)(param_1 + 0x50),iVar3);
          if (-1 < iVar1) goto LAB_0000d850;
          FUN_00013b44(iVar3);
        }
        FUN_000227fc(iVar3);
      }
      FUN_00019938(iVar2);
    }
  }
LAB_0000d850:
  syscall();
  return iVar1;
}



ulonglong FUN_0000d8a4(int param_1)

{
  longlong lVar1;
  
  lVar1 = FUN_00014a10(param_1 + 0x18);
  return lVar1 + 0xffffU & 0xffff0000;
}



ulonglong FUN_0000d8d8(int param_1)

{
  longlong lVar1;
  
  lVar1 = FUN_000185f8(param_1 + 0x44);
  return lVar1 + 0x4ffffU & 0xffff0000;
}



undefined4 FUN_0000d90c(int param_1)

{
  undefined4 uVar1;
  int iVar2;
  
  uVar1 = 0;
  for (iVar2 = 0; iVar2 < *(int *)(param_1 + 0x28); iVar2 = iVar2 + 1) {
    uVar1 = FUN_0001b14c(iVar2 * 0x1f0 + *(int *)(param_1 + 0x54));
  }
  return uVar1;
}



undefined4 FUN_0000d970(int *param_1)

{
  int iVar1;
  int iVar2;
  undefined4 uVar3;
  undefined8 uVar4;
  int iVar5;
  undefined1 local_50 [8];
  undefined1 local_48 [8];
  undefined1 local_40 [8];
  undefined1 auStack_38 [16];
  
  uVar4 = 0;
  if (param_1[0x1a] != 0) {
    uVar4 = FUN_00022614(param_1[0x1a],local_50,local_48,local_40);
  }
  if ((undefined8 *)*param_1 != (undefined8 *)0x0) {
    uVar4 = *(undefined8 *)*param_1;
    syscall();
    FUN_000227fc(*param_1,auStack_38);
    *param_1 = 0;
  }
  if (param_1[0x18] != 0) {
    uVar4 = FUN_00020c88(param_1[0x18]);
    FUN_000227fc(param_1[0x18]);
    param_1[0x18] = 0;
  }
  if (param_1[0x17] != 0) {
    FUN_0001e8f0(param_1[0x17]);
    uVar4 = FUN_0001e988(param_1[0x17]);
    FUN_000227fc(param_1[0x17]);
    param_1[0x17] = 0;
  }
  uVar3 = (undefined4)uVar4;
  iVar5 = 0;
  if (param_1[0x15] != 0) {
    while( true ) {
      uVar3 = (undefined4)uVar4;
      if (param_1[10] <= iVar5) break;
      iVar2 = iVar5 * 0x1f0;
      iVar5 = iVar5 + 1;
      uVar4 = FUN_0001b188(iVar2 + param_1[0x15]);
    }
    FUN_000227fc(param_1[0x15]);
    param_1[0x15] = 0;
  }
  iVar5 = param_1[0x14];
  if (iVar5 != 0) {
    while (iVar2 = FUN_00021610(iVar5), 0 < iVar2) {
      iVar2 = FUN_00021664(iVar5,0);
      if (iVar2 != 0) {
        iVar1 = FUN_00013240(iVar2);
        FUN_00013b44(iVar2);
        if (iVar1 != 0) {
          FUN_00019874(iVar1,iVar2);
          FUN_00019938(iVar1);
        }
      }
      FUN_00021928(iVar5,0);
    }
    uVar3 = FUN_000217e0(param_1[0x14]);
    FUN_000227fc(param_1[0x14]);
    param_1[0x14] = 0;
  }
  if (param_1[0x1a] != 0) {
    FUN_000225d8(param_1[0x1a]);
    uVar3 = FUN_00022588(param_1[0x1a]);
    FUN_000227fc(param_1[0x1a]);
    param_1[0x1a] = 0;
  }
  if (param_1[0x19] != 0) {
    uVar3 = FUN_000226a8(param_1[0x19]);
    FUN_000227fc(param_1[0x19]);
    param_1[0x19] = 0;
  }
  if (param_1[0x16] != 0) {
    uVar3 = FUN_00021448(param_1[0x16]);
    FUN_000227fc(param_1[0x16]);
    param_1[0x16] = 0;
  }
  if (param_1[0x1b] != 0) {
    FUN_00029718(param_1[0x1b]);
    FUN_000227fc(param_1[0x1b]);
    param_1[0x1b] = 0;
  }
  if ((undefined4 *)param_1[1] != (undefined4 *)0x0) {
    uVar3 = *(undefined4 *)param_1[1];
    syscall();
    FUN_000227fc(param_1[1]);
    param_1[1] = 0;
  }
  return uVar3;
}



int FUN_0000dc3c(int *param_1,uint *param_2,undefined4 param_3)

{
  uint uVar1;
  undefined8 uVar2;
  int iVar3;
  undefined4 uVar4;
  int iVar5;
  longlong lVar6;
  int iVar7;
  undefined1 local_260 [32];
  undefined1 auStack_240 [512];
  
  FUN_00029ad0(param_1,0,0x180);
  param_1[0x1c] = -1;
  uVar2 = FUN_00029b40();
  *(undefined8 *)(param_1 + 0x1e) = uVar2;
  iVar5 = -0x7ffd67ff;
  FUN_00029b08(param_1 + 4,param_2,0x40);
  iVar3 = FUN_0002285c(4);
  param_1[1] = iVar3;
  if (iVar3 == 0) goto LAB_0000e1ec;
  iVar5 = param_1[1];
  syscall();
  if (iVar5 != 0) {
    FUN_000227fc(param_1[1],local_260);
    param_1[1] = 0;
    goto LAB_0000e1ec;
  }
  if (param_2[0xb] == 0) {
    iVar3 = FUN_00022828(0x80,0x1000);
    iVar5 = -0x7ffd67ff;
    param_1[0x1b] = iVar3;
    if (iVar3 == 0) goto LAB_0000e1ec;
    FUN_00029638(auStack_240,2,0x330000,1,param_2[8],param_2[7] + 2,1);
    uVar4 = FUN_00029910("CellPremoSpurs");
    FUN_000295c8(auStack_240,"CellPremoSpurs",uVar4);
    if (param_2[0xc] != 0xffffffff) {
      FUN_00029600(auStack_240,param_2[0xc]);
    }
    uVar1 = param_2[3];
    if ((((uVar1 != 0) && (uVar1 != 0x40)) && (uVar1 != 0x13)) && ((uVar1 & 0x20) == 0)) {
      iVar5 = FUN_000296a8(auStack_240,1,1);
      iVar3 = param_1[0x1b];
      if (-1 < iVar5) {
        iVar5 = FUN_00029670(iVar3,auStack_240);
        if (-1 < iVar5) {
          lVar6 = 0xb54;
          if (0x2fffff < *param_2) {
            lVar6 = 0x5be;
          }
          iVar5 = FUN_000296e0(param_1[0x1b],&LAB_00010600,0x14d5,lVar6,0x14d2U - lVar6 & 0xfffffffe
                              );
          if (iVar5 != 0) goto LAB_0000e1ec;
          param_1[0x1c] = 0;
          goto LAB_0000dec8;
        }
        iVar3 = param_1[0x1b];
      }
      FUN_000227fc(iVar3);
      param_1[0x1b] = 0;
      goto LAB_0000e1ec;
    }
    iVar5 = FUN_00029670(param_1[0x1b],auStack_240);
    if (iVar5 < 0) {
      FUN_000227fc(param_1[0x1b]);
      param_1[0x1b] = 0;
      goto LAB_0000e1ec;
    }
  }
  else {
    param_1[0x1b] = param_2[0xb];
  }
LAB_0000dec8:
  iVar3 = FUN_0002285c(8);
  iVar5 = -0x7ffd67ff;
  param_1[0x16] = iVar3;
  if (iVar3 != 0) {
    iVar5 = FUN_00021520(iVar3);
    if (iVar5 < 0) {
      FUN_000227fc(param_1[0x16]);
      param_1[0x16] = 0;
    }
    else {
      iVar3 = FUN_0002285c(4);
      iVar5 = -0x7ffd67ff;
      param_1[0x19] = iVar3;
      if (iVar3 != 0) {
        iVar5 = FUN_0002272c(iVar3);
        if (iVar5 < 0) {
          FUN_000227fc(param_1[0x19]);
          param_1[0x19] = 0;
        }
        else {
          iVar3 = FUN_0002285c(8);
          iVar5 = -0x7ffd67ff;
          param_1[0x1a] = iVar3;
          if (iVar3 != 0) {
            iVar5 = FUN_0002265c(iVar3);
            iVar3 = param_1[0x1a];
            if (-1 < iVar5) {
              iVar5 = FUN_000225ac(iVar3,param_1[0x19]);
              if (-1 < iVar5) {
                iVar3 = FUN_0002285c(0x14);
                iVar5 = -0x7ffd67ff;
                param_1[0x14] = iVar3;
                if (iVar3 != 0) {
                  iVar5 = FUN_00021834(iVar3,0x10);
                  if (iVar5 < 0) {
                    FUN_000227fc(param_1[0x14]);
                    param_1[0x14] = 0;
                  }
                  else {
                    param_1[2] = 0;
                    iVar5 = -0x7ffd67ff;
                    iVar3 = FUN_0002285c(param_1[10] * 0x1f0);
                    param_1[0x15] = iVar3;
                    if (iVar3 != 0) {
                      iVar7 = 0;
                      FUN_00029ad0(iVar3,0,param_1[10] * 0x1f0);
                      do {
                        if (param_1[10] <= iVar7) {
                          iVar3 = FUN_0002285c(0x48);
                          iVar5 = -0x7ffd67ff;
                          param_1[0x17] = iVar3;
                          if (iVar3 == 0) break;
                          iVar5 = FUN_0001eac8(iVar3,param_1,param_1[0x19],
                                               *(undefined2 *)(param_1 + 5),param_1[0xb],param_1[10]
                                               ,param_1[6],param_3);
                          iVar3 = param_1[0x17];
                          if (-1 < iVar5) {
                            iVar5 = FUN_0001e95c(iVar3);
                            if (-1 < iVar5) {
                              iVar3 = FUN_0002285c(0x1c);
                              iVar5 = -0x7ffd67ff;
                              param_1[0x18] = iVar3;
                              if (iVar3 != 0) {
                                iVar5 = FUN_00020d18(iVar3,*(undefined2 *)(param_1 + 5),param_3,
                                                     *(undefined1 *)(param_1 + 0x13));
                                if (iVar5 < 0) {
                                  FUN_000227fc(param_1[0x18]);
                                  param_1[0x18] = 0;
                                }
                                else {
                                  iVar3 = FUN_0002285c(8);
                                  iVar5 = -0x7ffd67ff;
                                  *param_1 = iVar3;
                                  if (iVar3 != 0) {
                                    iVar5 = FUN_000298a0(iVar3,&PTR_LAB_0002eb58,param_1,
                                                         param_1[0xb],0x1000,1,
                                                         "cellPremoControlThread");
                                    if (-1 < iVar5) {
                                      return iVar5;
                                    }
                                    FUN_000227fc(*param_1);
                                    *param_1 = 0;
                                  }
                                }
                              }
                              break;
                            }
                            FUN_0001e988(param_1[0x17]);
                            iVar3 = param_1[0x17];
                          }
                          FUN_000227fc(iVar3);
                          param_1[0x17] = 0;
                          break;
                        }
                        iVar5 = iVar7 * 0x1f0;
                        iVar7 = iVar7 + 1;
                        iVar5 = FUN_0001b28c(iVar5 + param_1[0x15],param_1[0x19],param_1[0xb],
                                             param_1[7],param_1[8],param_3);
                      } while (-1 < iVar5);
                    }
                  }
                }
                goto LAB_0000e1ec;
              }
              FUN_00022588(param_1[0x1a]);
              iVar3 = param_1[0x1a];
            }
            FUN_000227fc(iVar3);
            param_1[0x1a] = 0;
          }
        }
      }
    }
  }
LAB_0000e1ec:
  FUN_0000d970(param_1);
  return iVar5;
}



int FUN_0000e2bc(int param_1,undefined4 param_2)

{
  int iVar1;
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar1 = FUN_000214c4(*(undefined4 *)(param_1 + 0x58),param_2);
    syscall();
  }
  return iVar1;
}



int FUN_0000e32c(int param_1,int param_2,undefined4 param_3)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d138(param_1,param_2 + 0x28);
    iVar1 = -1;
    if (-1 < iVar2) {
      if ((*(char *)(param_1 + 0x4c) != '\0') ||
         (iVar1 = FUN_0001a26c(*(int *)(param_1 + 0x54) + iVar2 * 0x1f0,param_2 + 0x30,
                               *(undefined4 *)(param_2 + 0x40)), -1 < iVar1)) {
        iVar1 = FUN_0001a99c(iVar2 * 0x1f0 + *(int *)(param_1 + 0x54),param_2,param_3);
      }
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000e420(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d138(param_1,param_2);
    iVar1 = -1;
    if (-1 < iVar2) {
      iVar1 = FUN_0001a9fc(iVar2 * 0x1f0 + *(int *)(param_1 + 0x54),param_2,param_3);
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000e4cc(int param_1,undefined4 param_2)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_30 [24];
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_2,auStack_30);
    iVar1 = -0x7ffd67fd;
    if (iVar2 != 0) {
      iVar1 = FUN_000136f8(iVar2);
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000e558(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_40 [24];
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_2,auStack_40);
    iVar1 = -0x7ffd67fd;
    if (iVar2 != 0) {
      iVar1 = FUN_00013738(iVar2,param_3,param_4);
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000e604(int param_1,undefined4 param_2)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_30 [24];
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_2,auStack_30);
    iVar1 = -0x7ffd67fd;
    if (iVar2 != 0) {
      iVar1 = FUN_0001381c(iVar2);
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000e690(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4,
                undefined2 param_5)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_40 [16];
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_2,auStack_40);
    iVar1 = -0x7ffd67fd;
    if (iVar2 != 0) {
      iVar1 = FUN_0001385c(iVar2,param_3,param_4,param_5);
    }
    syscall();
  }
  return iVar1;
}



undefined4 FUN_0000e74c(int param_1,undefined4 param_2)

{
  FUN_0000b5c0();
  FUN_0001e82c(*(undefined4 *)(param_1 + 0x5c),param_2);
  syscall();
  return **(undefined4 **)(param_1 + 4);
}



undefined4 FUN_0000e7a0(int param_1,undefined2 param_2)

{
  FUN_0000b5c0();
  FUN_0001e898(*(undefined4 *)(param_1 + 0x5c),param_2);
  syscall();
  return **(undefined4 **)(param_1 + 4);
}



int FUN_0000e7f4(int param_1,undefined4 param_2,undefined1 param_3,undefined *param_4,
                undefined4 param_5,undefined4 param_6,undefined1 param_7,undefined1 param_8)

{
  int iVar1;
  int iVar3;
  int iVar4;
  int iVar5;
  ulonglong uVar2;
  undefined4 uVar6;
  undefined1 in_stack_0000007f;
  
  iVar3 = FUN_0000b5c0();
  if (-1 < iVar3) {
    iVar4 = FUN_0000d1d8(param_1,param_2);
    if (-1 < iVar4) {
      iVar5 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_6,ZEXT48(&stack0x00000000) - 0x80);
      if (iVar5 == 0) {
        iVar4 = -0x7ffd67fd;
      }
      else {
        FUN_00013564(iVar5,param_3,*(undefined4 *)(param_1 + 0x48),in_stack_0000007f);
        uVar2 = ZEXT48(&stack0x00000000) - 0x78 & 0xffffffff;
        if (*(int *)(param_1 + 0x48) == 1) {
          iVar1 = *(int *)(param_1 + 0x54);
          uVar2 = FUN_0001325c(iVar5);
          uVar2 = uVar2 & 0xffffffff;
          param_4 = &DAT_0002d5c8;
          uVar6 = 1;
        }
        else {
          FUN_0002353c(uVar2);
          iVar1 = *(int *)(param_1 + 0x54);
          uVar6 = *(undefined4 *)(param_1 + 0x48);
        }
        FUN_0001a2cc(iVar4 * 0x1f0 + iVar1,param_3,uVar6,param_4,uVar2,param_5,param_7,param_8);
        iVar4 = iVar3;
      }
    }
    iVar3 = iVar4;
    syscall();
  }
  return iVar3;
}



int FUN_0000e9b4(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d1d8(param_1,param_3);
    if (-1 < iVar2) {
      FUN_0001a720(iVar2 * 0x1f0 + *(int *)(param_1 + 0x54),param_2);
      iVar2 = iVar1;
    }
    iVar1 = iVar2;
    syscall();
  }
  return iVar1;
}



int FUN_0000ea60(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_40 [24];
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_4,auStack_40);
    iVar1 = -0x7ffd67fc;
    if (iVar2 != 0) {
      iVar1 = FUN_0001363c(iVar2,param_2,param_3);
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000eb0c(int param_1,undefined4 param_2)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_30 [24];
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_2,auStack_30);
    iVar1 = -0x7ffd67fd;
    if (iVar2 != 0) {
      iVar1 = thunk_FUN_00013350(iVar2);
    }
    syscall();
  }
  return iVar1;
}



int FUN_0000eb98(int param_1,int param_2,int *param_3)

{
  int iVar1;
  int iVar2;
  int iVar3;
  int iVar4;
  int local_50 [6];
  
  iVar1 = FUN_0000b5c0();
  iVar2 = 0;
  iVar3 = 0;
  if (-1 < iVar1) {
    while ((iVar3 < *(int *)(param_1 + 0x28) && (iVar2 < *param_3))) {
      iVar4 = iVar3 * 0x1f0;
      iVar3 = iVar3 + 1;
      FUN_0001a6f8(iVar4 + *(int *)(param_1 + 0x54),local_50);
      if (local_50[0] == 1) {
        iVar2 = iVar2 + 1;
        FUN_0001a720(iVar4 + *(int *)(param_1 + 0x54),param_2);
        param_2 = param_2 + 0x178;
      }
    }
    syscall();
  }
  *param_3 = iVar2;
  return iVar1;
}



int FUN_0000ec8c(int param_1)

{
  uint uVar1;
  int iVar2;
  int iVar3;
  longlong lVar4;
  uint local_30 [6];
  
  iVar2 = FUN_0000b5c0();
  if (-1 < iVar2) {
    lVar4 = 0;
    iVar3 = 0;
    while( true ) {
      iVar2 = (int)lVar4;
      if (*(int *)(param_1 + 0x28) <= iVar3) break;
      iVar2 = iVar3 * 0x1f0;
      iVar3 = iVar3 + 1;
      FUN_0001a6f8(iVar2 + *(int *)(param_1 + 0x54),local_30);
      uVar1 = (int)(local_30[0] ^ 1) >> 0x1f;
      lVar4 = lVar4 - ((longlong)((ulonglong)(((uVar1 ^ local_30[0] ^ 1) - uVar1) - 1) << 0x20) >>
                      0x3f);
    }
    syscall();
  }
  return iVar2;
}



int FUN_0000ed40(int param_1,int param_2,int *param_3)

{
  int iVar1;
  int iVar2;
  int iVar3;
  int iVar4;
  
  iVar1 = FUN_0000b5c0();
  if (-1 < iVar1) {
    iVar2 = FUN_00021610(*(undefined4 *)(param_1 + 0x50));
    iVar4 = *param_3;
    if (iVar2 < *param_3) {
      iVar4 = iVar2;
    }
    for (iVar2 = 0; iVar2 < iVar4; iVar2 = iVar2 + 1) {
      iVar3 = FUN_00021664(*(undefined4 *)(param_1 + 0x50),iVar2);
      if (iVar3 == 0) break;
      FUN_000138ec(iVar3,param_2);
      param_2 = param_2 + 0x5c;
    }
    *param_3 = iVar2;
    syscall();
  }
  return iVar1;
}



int FUN_00012c8c(int param_1,int param_2,undefined4 param_3,undefined8 param_4,undefined1 param_5,
                undefined4 param_6)

{
  int iVar2;
  int iVar3;
  longlong lVar1;
  int *piVar4;
  int iVar5;
  longlong lVar6;
  int local_70 [2];
  undefined1 auStack_68 [8];
  
  iVar2 = FUN_0000d078(param_1);
  iVar3 = 0;
  if (-1 < iVar2) {
    do {
      iVar5 = iVar3;
      if (*(int *)(param_1 + 0x28) <= iVar5) goto LAB_00012e5c;
      FUN_0001a6f8(iVar5 * 0x1f0 + *(int *)(param_1 + 0x54),local_70);
      iVar3 = iVar5 + 1;
    } while (local_70[0] != 0);
    if (iVar5 < 0) {
LAB_00012e5c:
      iVar2 = -0x7ffd67c0;
    }
    else {
      do {
        iVar2 = FUN_0002140c(auStack_68);
        if (iVar2 < 0) goto LAB_00012e70;
        iVar3 = FUN_0000d138(param_1,auStack_68);
      } while (iVar3 != -0x7ffd67fd);
      if (*(char *)(param_2 + 0x11f) == '\0') {
        lVar1 = FUN_00029b40();
        piVar4 = (int *)(param_1 + 0x80);
        iVar3 = 0;
        lVar6 = 0x20;
        do {
          iVar2 = *piVar4;
          piVar4 = piVar4 + 1;
          if (iVar2 == (int)param_4) {
            if (((ulonglong)(lVar1 - *(longlong *)(param_1 + 0x78)) / 1000000 -
                 (ulonglong)*(uint *)(param_1 + iVar3 * 4 + 0x100) & 0xffffffff) < 0x3c) {
              *(undefined4 *)(param_1 + iVar3 * 4 + 0x80) = 0;
              goto LAB_00012e08;
            }
            break;
          }
          iVar3 = iVar3 + 1;
          lVar6 = lVar6 + -1;
        } while (lVar6 != 0);
        iVar2 = -0x7ffd67bf;
      }
      else {
LAB_00012e08:
        iVar2 = FUN_0001adbc(iVar5 * 0x1f0 + *(int *)(param_1 + 0x54),param_2,auStack_68,param_3,
                             param_4,*(undefined4 *)(param_1 + 0x1c),param_5,param_6);
        if (-1 < iVar2) {
          *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + 1;
        }
      }
    }
LAB_00012e70:
    syscall();
  }
  return iVar2;
}



int FUN_00012ec4(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_0000d078();
  if (iVar1 < 0) {
    return iVar1;
  }
  iVar1 = FUN_0000d1d8(param_1,param_2);
  if (-1 < iVar1) {
    iVar2 = iVar1 * 0x1f0;
    iVar1 = FUN_0001a1f4(iVar2 + *(int *)(param_1 + 0x54));
    if (iVar1 != 0) {
      FUN_0001a1e8(iVar2 + *(int *)(param_1 + 0x54));
      iVar1 = FUN_0001349c(iVar1,iVar2 + *(int *)(param_1 + 0x54));
      if (iVar1 < 0) goto LAB_00012f90;
    }
    iVar1 = FUN_0001ad24(iVar2 + *(int *)(param_1 + 0x54),param_3);
    *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + -1;
  }
LAB_00012f90:
  syscall();
  return iVar1;
}



int FUN_00012fc8(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int iVar2;
  int iVar3;
  undefined1 auStack_210 [4];
  int local_20c [2];
  uint local_204;
  int local_200;
  uint local_1b0 [4];
  int local_1a0;
  
  iVar1 = FUN_0000d078();
  if (-1 < iVar1) {
    iVar1 = FUN_0000d1d8(param_1,param_2);
    if (-1 < iVar1) {
      iVar3 = iVar1 * 0x1f0;
      iVar1 = -0x7ffd67fc;
      FUN_0001a720(iVar3 + *(int *)(param_1 + 0x54),local_1b0);
      if (local_1a0 == 1) {
        iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_3,auStack_210);
        iVar1 = -0x7ffd67fd;
        if (iVar2 != 0) {
          FUN_000138ec(iVar2,local_20c);
          iVar1 = -0x7ffd67c0;
          if (local_20c[0] < local_200) {
            if (((local_204 == 1) && (local_1b0[0] == 0)) ||
               (iVar1 = -0x7ffd67b0, local_204 <= local_1b0[0])) {
              iVar1 = FUN_0001328c(iVar2,iVar3 + *(int *)(param_1 + 0x54) + 0x58);
              if (-1 < iVar1) {
                iVar1 = FUN_00013938(iVar2,iVar3 + *(int *)(param_1 + 0x54));
                if (-1 < iVar1) {
                  iVar1 = FUN_0001a80c(iVar3 + *(int *)(param_1 + 0x54),iVar2);
                }
              }
            }
          }
        }
      }
    }
    syscall();
  }
  return iVar1;
}



int FUN_00013158(int param_1,undefined4 param_2)

{
  int iVar1;
  int iVar2;
  int iVar3;
  undefined4 local_30 [4];
  
  iVar1 = FUN_0000d078();
  if (-1 < iVar1) {
    iVar2 = FUN_0000d68c(*(undefined4 *)(param_1 + 0x50),param_2,local_30);
    iVar1 = -0x7ffd67fd;
    if (iVar2 != 0) {
      iVar1 = FUN_00021928(*(undefined4 *)(param_1 + 0x50),local_30[0]);
      if (-1 < iVar1) {
        iVar3 = FUN_00013240(iVar2);
        iVar1 = FUN_00013b44(iVar2);
        if ((-1 < iVar1) && (iVar3 != 0)) {
          FUN_00019874(iVar3,iVar2);
          FUN_00019938(iVar3);
        }
      }
    }
    syscall();
  }
  return iVar1;
}



undefined4 FUN_00013240(int param_1)

{
  return *(undefined4 *)(param_1 + 0xb4);
}



void FUN_00013248(int param_1,undefined4 *param_2)

{
  *param_2 = *(undefined4 *)(param_1 + 0x40);
  return;
}



undefined4 FUN_00013254(int param_1)

{
  return *(undefined4 *)(param_1 + 0x44);
}



int FUN_0001325c(int param_1)

{
  return param_1 + 0xb8;
}



void FUN_00013268(int param_1)

{
  FUN_00014260(*(undefined4 *)(param_1 + 0x9c));
  return;
}



int FUN_0001328c(int param_1,undefined4 param_2)

{
  int iVar1;
  undefined1 auStack_70 [16];
  undefined1 auStack_60 [56];
  
  FUN_000148f4(*(undefined4 *)(param_1 + 0x9c),auStack_60);
  FUN_00017d44(*(undefined4 *)(param_1 + 0xa0),auStack_70);
  FUN_000199b0(*(undefined4 *)(param_1 + 0xb4));
  iVar1 = FUN_00017cd4(*(undefined4 *)(param_1 + 0xa0),auStack_70,param_2);
  if (-1 < iVar1) {
    iVar1 = FUN_00014414(*(undefined4 *)(param_1 + 0x9c),auStack_60,param_2);
  }
  return iVar1;
}



void FUN_0001332c(int param_1)

{
  FUN_00017e20(*(undefined4 *)(param_1 + 0xa0));
  return;
}



undefined4 FUN_00013350(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00018ecc(*(undefined4 *)(param_1 + 0xa8));
  return uVar1;
}



undefined4 thunk_FUN_00013350(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00018ecc(*(undefined4 *)(param_1 + 0xa8));
  return uVar1;
}



undefined4 FUN_0001337c(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x10,0);
  return uVar1;
}



undefined4 FUN_000133ac(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x10);
  return uVar1;
}



int FUN_000133d8(int param_1)

{
  int iVar1;
  undefined4 uVar2;
  int iVar3;
  int iVar4;
  
  iVar1 = FUN_0001337c(param_1);
  iVar4 = 0;
  if (-1 < iVar1) {
    do {
      iVar3 = FUN_00018ecc(*(undefined4 *)(param_1 + 0xa8));
      if (iVar3 <= iVar4) break;
      uVar2 = FUN_00019188(*(undefined4 *)(param_1 + 0xa8),iVar4);
      *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + -1;
      FUN_00018f1c(*(undefined4 *)(param_1 + 0xa8),uVar2);
      iVar1 = FUN_0001ad24(uVar2,0);
      iVar4 = iVar4 + 1;
    } while (-1 < iVar1);
    FUN_000133ac(param_1);
  }
  return iVar1;
}



int thunk_FUN_000133d8(int param_1)

{
  int iVar1;
  undefined4 uVar2;
  int iVar3;
  int iVar4;
  
  iVar1 = FUN_0001337c(param_1);
  iVar4 = 0;
  if (-1 < iVar1) {
    do {
      iVar3 = FUN_00018ecc(*(undefined4 *)(param_1 + 0xa8));
      if (iVar3 <= iVar4) break;
      uVar2 = FUN_00019188(*(undefined4 *)(param_1 + 0xa8),iVar4);
      *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + -1;
      FUN_00018f1c(*(undefined4 *)(param_1 + 0xa8),uVar2);
      iVar1 = FUN_0001ad24(uVar2,0);
      iVar4 = iVar4 + 1;
    } while (-1 < iVar1);
    FUN_000133ac(param_1);
  }
  return iVar1;
}



int FUN_0001349c(int param_1,undefined4 param_2)

{
  int iVar1;
  
  iVar1 = FUN_0001337c();
  if (iVar1 == 0) {
    *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + -1;
    FUN_00018f1c(*(undefined4 *)(param_1 + 0xa8),param_2);
    FUN_000133ac(param_1);
  }
  return iVar1;
}



void FUN_00013564(int param_1,undefined1 param_2,undefined8 param_3,undefined8 param_4)

{
  FUN_0001451c(*(undefined4 *)(param_1 + 0x9c),param_2,param_3,&DAT_0002d5c8,param_1 + 0xb8,param_4)
  ;
  FUN_00017e44(*(undefined4 *)(param_1 + 0xa0),param_2,param_3,&DAT_0002d5c8,param_1 + 0xb8);
  return;
}



void FUN_000135f4(int param_1)

{
  FUN_00017de0(*(undefined4 *)(param_1 + 0xa0));
  return;
}



void FUN_00013618(int param_1)

{
  FUN_000146a0(*(undefined4 *)(param_1 + 0x9c));
  return;
}



undefined4 FUN_0001363c(int param_1,int param_2,int *param_3)

{
  int iVar1;
  int iVar2;
  int iVar3;
  
  iVar1 = FUN_00018ecc(*(undefined4 *)(param_1 + 0xa8));
  iVar3 = 0;
  while ((iVar3 < iVar1 && (iVar3 < *param_3))) {
    iVar2 = FUN_00019188(*(undefined4 *)(param_1 + 0xa8),iVar3);
    if (iVar2 == 0) {
      return 0x80029802;
    }
    FUN_0001a720(iVar2,param_2);
    iVar3 = iVar3 + 1;
    param_2 = param_2 + 0x178;
  }
  *param_3 = iVar3;
  return 0;
}



undefined4 FUN_000136f8(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0x80029803;
  if (*(int *)(param_1 + 0xa0) != 0) {
    uVar1 = FUN_00017ecc(*(int *)(param_1 + 0xa0));
  }
  return uVar1;
}



int FUN_00013738(int param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  int iVar2;
  int iVar3;
  undefined4 uVar4;
  
  iVar3 = -0x7ffd67fd;
  if (*(int *)(param_1 + 0xa0) != 0) {
    iVar2 = FUN_00013350();
    iVar3 = -0x7ffd67f9;
    if ((iVar2 != 0) && (iVar3 = FUN_00018658(*(undefined4 *)(param_1 + 0xa4),param_2), -1 < iVar3))
    {
      iVar2 = FUN_00018014(*(undefined4 *)(param_1 + 0xa0));
      iVar3 = -0x7ffd67f7;
      if (iVar2 == 0) {
        DAT_0002f1dc = DAT_0002f1dc + '\x01';
      }
      else {
        uVar1 = *(undefined4 *)(param_1 + 0xa0);
        uVar4 = FUN_00019ebc(*(undefined4 *)(param_1 + 0xac));
        iVar3 = FUN_00017f24(uVar1,param_2,param_3,uVar4);
      }
    }
  }
  FUN_00019ea8(*(undefined4 *)(param_1 + 0xac));
  return iVar3;
}



undefined4 FUN_0001381c(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0x80029803;
  if (*(int *)(param_1 + 0x9c) != 0) {
    FUN_000145a8(*(int *)(param_1 + 0x9c));
    uVar1 = 0;
  }
  return uVar1;
}



undefined4 FUN_0001385c(int param_1,undefined4 param_2,undefined4 param_3,undefined2 param_4)

{
  int iVar1;
  undefined4 uVar2;
  
  uVar2 = 0x80029803;
  if (*(int *)(param_1 + 0x9c) != 0) {
    iVar1 = FUN_00013350();
    uVar2 = 0x80029807;
    if (iVar1 != 0) {
      uVar2 = FUN_00014758(*(undefined4 *)(param_1 + 0x9c),param_2,param_3,param_4);
    }
  }
  return uVar2;
}



void FUN_000138ec(int param_1,undefined4 *param_2)

{
  param_2[1] = *(undefined4 *)(param_1 + 0x40);
  *param_2 = *(undefined4 *)(param_1 + 8);
  FUN_00029b08(param_2 + 2,param_1 + 0x44,0x54);
  return;
}



int FUN_00013938(int param_1,undefined4 param_2)

{
  int iVar1;
  
  iVar1 = FUN_0001337c();
  if (iVar1 == 0) {
    *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + 1;
    FUN_00019130(*(undefined4 *)(param_1 + 0xa8),param_2);
    FUN_000133ac(param_1);
  }
  return iVar1;
}



void FUN_000139b0(int param_1,ulonglong param_2)

{
  if (*(int *)(param_1 + 0xa4) != 0) {
    if ((param_2 & 0x20) != 0) {
      FUN_00018830(*(int *)(param_1 + 0xa4));
    }
    FUN_00019874(*(undefined4 *)(param_1 + 0xb4),*(undefined4 *)(param_1 + 0xa4));
    *(undefined4 *)(param_1 + 0xa4) = 0;
  }
  if (*(int *)(param_1 + 0xa0) != 0) {
    if ((param_2 & 0x10) != 0) {
      FUN_00018174(*(int *)(param_1 + 0xa0));
    }
    FUN_00019874(*(undefined4 *)(param_1 + 0xb4),*(undefined4 *)(param_1 + 0xa0));
    *(undefined4 *)(param_1 + 0xa0) = 0;
  }
  if (*(int *)(param_1 + 0x9c) != 0) {
    if ((param_2 & 8) != 0) {
      FUN_0001480c(*(int *)(param_1 + 0x9c));
    }
    FUN_00019874(*(undefined4 *)(param_1 + 0xb4),*(undefined4 *)(param_1 + 0x9c));
    *(undefined4 *)(param_1 + 0x9c) = 0;
  }
  if (*(int *)(param_1 + 0x98) != 0) {
    if ((param_2 & 2) != 0) {
      FUN_00021448(*(int *)(param_1 + 0x98));
    }
    FUN_00019874(*(undefined4 *)(param_1 + 0xb4),*(undefined4 *)(param_1 + 0x98));
    *(undefined4 *)(param_1 + 0x98) = 0;
  }
  if (*(int *)(param_1 + 0xa8) != 0) {
    if ((param_2 & 4) != 0) {
      FUN_00018fd4(*(int *)(param_1 + 0xa8));
    }
    FUN_00019874(*(undefined4 *)(param_1 + 0xb4),*(undefined4 *)(param_1 + 0xa8));
    *(undefined4 *)(param_1 + 0xa8) = 0;
  }
  if (*(int *)(param_1 + 0xac) != 0) {
    FUN_00019874(*(undefined4 *)(param_1 + 0xb4),*(int *)(param_1 + 0xac));
    *(undefined4 *)(param_1 + 0xac) = 0;
  }
  if ((param_2 & 0x40) != 0) {
    FUN_00029d00(param_1 + 0x28);
  }
  if ((param_2 & 1) != 0) {
    FUN_00029d00(param_1 + 0x10);
  }
  return;
}



undefined4 FUN_00013b44(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000133d8();
  FUN_000139b0(param_1,0xffffffff);
  return uVar1;
}



int FUN_00013b90(undefined4 *param_1,undefined4 param_2,int param_3,undefined4 param_4,
                undefined4 param_5,undefined4 param_6,undefined4 param_7,undefined8 param_8)

{
  int iVar1;
  int iVar2;
  undefined8 uVar3;
  undefined1 auStack_80 [8];
  undefined1 auStack_78 [8];
  undefined1 local_70 [16];
  
  FUN_00019b38(param_8,auStack_80);
  FUN_00029ad0(param_1,0,200);
  param_1[0x10] = param_2;
  *param_1 = param_6;
  param_1[1] = param_7;
  param_1[0x2c] = param_4;
  param_1[0x2d] = (int)param_8;
  FUN_00029b08(param_1 + 0x11,param_3,0x54);
  iVar1 = FUN_00029948(param_1 + 4,local_70);
  uVar3 = 0;
  if (iVar1 == 0) {
    iVar1 = FUN_00029948(param_1 + 10,local_70);
    uVar3 = 1;
    if (-1 < iVar1) {
      iVar2 = FUN_00019910(param_1[0x2d],8);
      uVar3 = 0x41;
      iVar1 = -0x7ffd67ff;
      param_1[0x26] = iVar2;
      if (iVar2 != 0) {
        iVar1 = FUN_00021520(iVar2);
        uVar3 = 0x41;
        if (-1 < iVar1) {
          iVar2 = FUN_00019910(param_1[0x2d],8);
          uVar3 = 0x43;
          iVar1 = -0x7ffd67ff;
          param_1[0x2b] = iVar2;
          if (iVar2 != 0) {
            FUN_00019e94(iVar2);
            iVar1 = -0x7ffd67ff;
            iVar2 = FUN_00019910(param_1[0x2d],0x10);
            uVar3 = 0x43;
            param_1[0x2a] = iVar2;
            if (iVar2 != 0) {
              iVar1 = FUN_00019034(iVar2,param_1[0x2d],0x10);
              uVar3 = 0x43;
              if (-1 < iVar1) {
                iVar2 = FUN_00019910(param_1[0x2d],0x6c);
                uVar3 = 0x47;
                iVar1 = -0x7ffd67ff;
                param_1[0x27] = iVar2;
                if (iVar2 != 0) {
                  iVar1 = FUN_00014970(iVar2,param_3 + 0x18,param_5,param_6,param_7,param_2,
                                       param_1[0x2b],param_1[0x2c]);
                  uVar3 = 0x47;
                  if (-1 < iVar1) {
                    iVar2 = FUN_00019910(param_1[0x2d],0xf8);
                    uVar3 = 0x4f;
                    iVar1 = -0x7ffd67ff;
                    param_1[0x28] = iVar2;
                    if (iVar2 != 0) {
                      iVar1 = FUN_0001826c(iVar2,param_3 + 0x44,param_5,param_6,param_7,param_2,
                                           param_1[0x2c],param_1[0x2a]);
                      uVar3 = 0x4f;
                      if (-1 < iVar1) {
                        iVar2 = FUN_00019910(param_1[0x2d],0x30);
                        uVar3 = 0x5f;
                        iVar1 = -0x7ffd67ff;
                        param_1[0x29] = iVar2;
                        if (iVar2 != 0) {
                          iVar1 = FUN_0001885c(iVar2,*(undefined4 *)(param_1[0x28] + 0x24),0x400);
                          uVar3 = 0x5f;
                          if (-1 < iVar1) {
                            FUN_0002353c(param_1 + 0x2e);
                            FUN_00019b38(param_1[0x2d],auStack_78);
                            return iVar1;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  FUN_000139b0(param_1,uVar3);
  return iVar1;
}



void FUN_00013f34(uint *param_1,int param_2)

{
  uint uVar1;
  
  uVar1 = 0xfa000;
  if (param_2 != 1) {
    uVar1 = 0xbb800;
    if (1 < param_2 - 2U) {
      if (param_2 != 4) {
        return;
      }
      uVar1 = 2500000;
    }
  }
  if (*param_1 <= uVar1) {
    return;
  }
  *param_1 = uVar1;
  return;
}



undefined4 FUN_00013fd8(uint *param_1,undefined4 param_2)

{
  uint uVar1;
  uint uVar2;
  undefined4 uVar3;
  uint uVar4;
  
  uVar2 = *param_1;
  if (uVar2 == 1) {
    uVar2 = FUN_00023634("/dev_flash/sys/external/libm4venc.sprx",param_1[0xf]);
    param_1[0x10] = uVar2;
    if (uVar2 != 0) {
      uVar2 = FUN_00019910(param_1[1],0xd8);
      param_1[2] = uVar2;
      if (uVar2 == 0) {
        return 0x80029801;
      }
      uVar3 = FUN_00015258(uVar2,param_2,param_1[6],param_1[7],param_1[8],param_1[9],param_1[10],
                           param_1[0xc]);
      return uVar3;
    }
  }
  else if (uVar2 - 2 < 2) {
    uVar1 = (int)(uVar2 ^ 3) >> 0x1f;
    uVar4 = FUN_00023634("/dev_flash/sys/external/libavcenc_small.sprx",param_1[0xf]);
    param_1[0x10] = uVar4;
    if (uVar4 != 0) {
      uVar4 = FUN_00019910(param_1[1],0xe8);
      param_1[3] = uVar4;
      if (uVar4 == 0) {
        return 0x80029801;
      }
      uVar3 = FUN_000168a4(uVar4,param_2,param_1[6],param_1[7],param_1[8],param_1[9],
                           ((int)(uVar1 - (uVar1 ^ uVar2 ^ 3)) >> 0x1f) + 2,param_1[10]);
      return uVar3;
    }
  }
  else if (uVar2 == 4) {
    uVar2 = FUN_00023634("/dev_flash/sys/external/libm4hdenc.sprx",param_1[0xf]);
    param_1[0x10] = uVar2;
    if (uVar2 != 0) {
      uVar2 = FUN_00019910(param_1[1],0xe8);
      param_1[4] = uVar2;
      if (uVar2 == 0) {
        return 0x80029801;
      }
      uVar3 = FUN_000176d8(uVar2,param_2,param_1[6],param_1[7],param_1[8],param_1[9],param_1[10],
                           param_1[0xb]);
      return uVar3;
    }
  }
  else if (uVar2 != 0xff) {
    return 0x80029804;
  }
  return 0;
}



void FUN_000141f4(int *param_1,undefined4 param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  if (iVar1 == 1) {
    FUN_00014e60(param_1[2],param_2);
  }
  else if (iVar1 - 2U < 2) {
    FUN_0001651c(param_1[3],param_2);
  }
  else if (iVar1 == 4) {
    FUN_000170a4(param_1[4],param_2);
  }
  return;
}



void FUN_00014260(int *param_1,undefined1 param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  if (iVar1 == 1) {
    FUN_00014ad4(param_1[2],param_2,param_1[0xe]);
  }
  else if (iVar1 - 2U < 2) {
    FUN_000164a0(param_1[3],param_2,param_1[0xe]);
  }
  else if (iVar1 == 4) {
    FUN_00017028(param_1[4],param_2,param_1[0xe]);
  }
  return;
}



void FUN_00014344(int *param_1)

{
  undefined1 uVar1;
  int iVar2;
  
  iVar2 = *param_1;
  if (iVar2 == 1) {
    FUN_00014ab4(param_1[2],*(undefined1 *)((int)param_1 + 0x45),param_1[0x12],param_1 + 0x13,
                 param_1 + 0x17);
  }
  else if (iVar2 - 2U < 2) {
    FUN_00016480(param_1[3],*(undefined1 *)((int)param_1 + 0x45),param_1[0x12],param_1 + 0x13,
                 param_1 + 0x17);
  }
  else if (iVar2 == 4) {
    FUN_00017008(param_1[4],*(undefined1 *)((int)param_1 + 0x45),param_1[0x12],param_1 + 0x13,
                 param_1 + 0x17);
  }
  uVar1 = *(undefined1 *)(param_1 + 0x11);
  iVar2 = *param_1;
  if (iVar2 == 1) {
    FUN_00014aa8(param_1[2],uVar1);
  }
  else if (iVar2 - 2U < 2) {
    FUN_00016478(param_1[3],uVar1);
  }
  else if (iVar2 == 4) {
    FUN_00017000(param_1[4],uVar1);
  }
  return;
}



undefined4 FUN_00014414(uint *param_1,uint *param_2,int param_3)

{
  ushort uVar1;
  ushort uVar2;
  undefined4 uVar3;
  uint uVar4;
  
  uVar1 = *(ushort *)(param_3 + 0x178);
  uVar2 = *(ushort *)(param_3 + 0x17a);
  *param_2 = (uint)*(byte *)(param_3 + 0x177);
  param_2[1] = (uint)uVar1;
  param_2[2] = (uint)uVar2;
  FUN_00013f34(param_3 + 0x17c,*(undefined1 *)(param_3 + 0x177));
  uVar4 = *(uint *)(param_1[5] + 0x10);
  param_2[6] = *(uint *)(param_3 + 0x17c);
  param_2[8] = *(uint *)(param_3 + 0x180);
  if (uVar4 == 0) {
    if (param_2[10] < 2) {
      uVar4 = (int)((uint)*(ushort *)(param_3 + 0x178) * 4 + 0x3f & 0xffffffc0) >> 2;
    }
    else {
      uVar4 = param_2[1] + 0x3f & 0xffffffc0;
    }
  }
  param_2[3] = uVar4;
  *param_1 = (uint)*(byte *)(param_3 + 0x177);
  uVar3 = FUN_00013fd8(param_1,param_2);
  param_1[0xe] = *(uint *)(param_3 + 400);
  FUN_00014344(param_1);
  return uVar3;
}



void FUN_0001451c(int param_1,undefined1 param_2,undefined4 param_3,undefined8 param_4,
                 undefined4 param_5,undefined1 param_6)

{
  *(undefined1 *)(param_1 + 0x45) = param_2;
  *(undefined4 *)(param_1 + 0x48) = param_3;
  FUN_00029b08(param_1 + 0x4c,param_4,0x10);
  FUN_00029b08(param_1 + 0x5c,param_5,0x10);
  *(undefined1 *)(param_1 + 0x44) = param_6;
  FUN_00014344(param_1);
  return;
}



void FUN_000145a8(int *param_1)

{
  int iVar1;
  
  iVar1 = *param_1;
  if (iVar1 == 1) {
    FUN_00014a8c(param_1[2]);
  }
  else if (iVar1 - 2U < 2) {
    FUN_00016464(param_1[3]);
  }
  else if (iVar1 == 4) {
    FUN_00016fec(param_1[4]);
  }
  return;
}



void FUN_00014604(int *param_1,undefined4 *param_2,undefined4 *param_3)

{
  int iVar1;
  
  iVar1 = *param_1;
  if (iVar1 == 1) {
    FUN_00014a78(param_1[2],param_2,param_3);
  }
  else if (iVar1 - 2U < 2) {
    FUN_00016450(param_1[3],param_2,param_3);
  }
  else if (iVar1 == 4) {
    FUN_00016fd8(param_1[4],param_2,param_3);
  }
  else if (iVar1 == 0xff) {
    iVar1 = param_1[5];
    *param_2 = *(undefined4 *)(iVar1 + 4);
    *param_3 = *(undefined4 *)(iVar1 + 8);
  }
  return;
}



void FUN_000146a0(int *param_1,uint param_2)

{
  int iVar1;
  uint uStack00000038;
  
  iVar1 = *param_1;
  uStack00000038 = param_2;
  if (param_2 < 0x1f400) {
    uStack00000038 = 0x1f400;
  }
  FUN_00013f34(&stack0x00000038,iVar1);
  if (iVar1 == 1) {
    FUN_00014f00(param_1[2],uStack00000038,uStack00000038);
  }
  else if (iVar1 - 2U < 2) {
    FUN_0001654c(param_1[3],uStack00000038,uStack00000038);
  }
  else if (iVar1 == 4) {
    FUN_00017140(param_1[4],uStack00000038,uStack00000038);
  }
  return;
}



uint FUN_00014758(uint *param_1,undefined4 param_2,undefined4 param_3,undefined2 param_4)

{
  uint uVar1;
  uint uVar2;
  
  uVar1 = *param_1;
  if (uVar1 == 1) {
    uVar2 = FUN_00014f34(param_1[2],param_2,param_3,param_4);
  }
  else {
    uVar2 = (int)(uVar1 ^ 0xff) >> 0x1f;
    if (uVar1 - 2 < 2) {
      uVar2 = FUN_00016580(param_1[3],param_2,param_3,param_4);
    }
    else {
      uVar2 = (int)(uVar2 - (uVar2 ^ uVar1 ^ 0xff)) >> 0x1f & 0x80029804;
      if (uVar1 == 4) {
        uVar2 = FUN_00017174(param_1[4],param_2,param_3,param_4);
      }
    }
  }
  return uVar2;
}



undefined4 FUN_0001480c(int param_1)

{
  undefined4 uVar1;
  
  if (*(int *)(param_1 + 8) == 0) {
    uVar1 = 0;
    if (*(int *)(param_1 + 0xc) == 0) {
      if (*(int *)(param_1 + 0x10) != 0) {
        uVar1 = FUN_0001744c(*(int *)(param_1 + 0x10));
        FUN_00019874(*(undefined4 *)(param_1 + 4),*(undefined4 *)(param_1 + 0x10));
        *(undefined4 *)(param_1 + 0x10) = 0;
      }
    }
    else {
      uVar1 = FUN_000167e4(*(int *)(param_1 + 0xc));
      FUN_00019874(*(undefined4 *)(param_1 + 4),*(undefined4 *)(param_1 + 0xc));
      *(undefined4 *)(param_1 + 0xc) = 0;
    }
  }
  else {
    uVar1 = FUN_00015198(*(int *)(param_1 + 8));
    FUN_00019874(*(undefined4 *)(param_1 + 4),*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0;
  }
  if (*(int *)(param_1 + 0x40) != 0) {
    FUN_000235f4(*(int *)(param_1 + 0x40));
    *(undefined4 *)(param_1 + 0x40) = 0;
  }
  return uVar1;
}



void FUN_000148f4(int *param_1,undefined4 param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  if (iVar1 == 1) {
    FUN_00014e60(param_1[2],param_2);
  }
  else if (iVar1 - 2U < 2) {
    FUN_0001651c(param_1[3],param_2);
  }
  else if (iVar1 == 4) {
    FUN_000170a4(param_1[4],param_2);
  }
  FUN_0001480c(param_1);
  return;
}



int FUN_00014970(undefined4 *param_1,undefined4 *param_2,undefined4 param_3,undefined4 param_4,
                undefined4 param_5,undefined4 param_6,undefined4 param_7,undefined4 param_8)

{
  int iVar1;
  undefined4 in_stack_00000074;
  undefined4 in_stack_0000007c;
  undefined4 in_stack_00000084;
  undefined4 in_stack_0000008c;
  
  *param_1 = *param_2;
  param_1[7] = param_4;
  param_1[8] = param_5;
  param_1[9] = param_6;
  param_1[1] = in_stack_0000007c;
  param_1[3] = 0;
  param_1[0xf] = in_stack_0000008c;
  param_1[2] = 0;
  param_1[0xb] = in_stack_00000084;
  param_1[6] = param_3;
  param_1[10] = param_7;
  param_1[0xc] = param_8;
  param_1[0xd] = in_stack_00000074;
  param_1[5] = param_2;
  iVar1 = FUN_00013fd8();
  if (iVar1 < 0) {
    FUN_0001480c(param_1);
  }
  return iVar1;
}



undefined4 FUN_00014a10(int *param_1)

{
  int iVar1;
  undefined4 uVar2;
  
  iVar1 = *param_1;
  if (iVar1 == 1) {
    uVar2 = FUN_000163dc();
  }
  else if (iVar1 - 2U < 2) {
    uVar2 = FUN_00016f88(param_1);
  }
  else {
    uVar2 = 0;
    if (iVar1 == 4) {
      uVar2 = FUN_00017b34(param_1);
    }
  }
  return uVar2;
}



void FUN_00014a78(int param_1,undefined4 *param_2,undefined4 *param_3)

{
  *param_2 = *(undefined4 *)(param_1 + 0x34);
  *param_3 = *(undefined4 *)(param_1 + 0x38);
  return;
}



undefined8 FUN_00014a8c(int param_1)

{
  *(undefined4 *)(param_1 + 0x20) = 1;
  *(undefined4 *)(param_1 + 0x1c) = *(undefined4 *)(param_1 + 8);
  return 0;
}



undefined8 FUN_00014aa8(int param_1,undefined1 param_2)

{
  *(undefined1 *)(param_1 + 0xbc) = param_2;
  return 0;
}



void FUN_00014ab4(int param_1,undefined1 param_2,undefined4 param_3,undefined4 param_4,
                 undefined4 param_5)

{
  *(undefined4 *)(param_1 + 0xd0) = param_5;
  *(undefined1 *)(param_1 + 0xc4) = param_2;
  *(undefined4 *)(param_1 + 200) = param_3;
  *(undefined4 *)(param_1 + 0xcc) = param_4;
  return;
}



void FUN_00014ad4(int param_1,undefined1 param_2,int param_3)

{
  uint uVar1;
  int iVar2;
  
  uVar1 = 0x560;
  if (param_3 != 0) {
    iVar2 = FUN_00021404(*(undefined4 *)(param_1 + 0x78));
    uVar1 = param_3 - iVar2;
    if (uVar1 < 500) {
      uVar1 = 500;
    }
    else if (0x57e < uVar1) {
      uVar1 = 0x57e;
    }
  }
  *(undefined1 *)(param_1 + 0xbe) = param_2;
  *(uint *)(param_1 + 0xc0) = uVar1;
  return;
}



int FUN_00014b50(int *param_1,undefined4 param_2,undefined4 param_3,uint *param_4,uint param_5,
                int param_6,undefined8 param_7,uint param_8)

{
  int iVar1;
  int iVar2;
  uint uVar3;
  int iVar4;
  int iVar5;
  uint uVar6;
  
  iVar4 = 0;
  while( true ) {
    if ((int)param_5 < 1) break;
    uVar6 = param_8;
    if (param_8 == 0) {
      uVar6 = param_5;
    }
    *param_4 = uVar6;
    FUN_00021188(param_2,param_1 + 3,param_3,param_4,param_8 == 0);
    iVar2 = FUN_00021404(param_2);
    iVar5 = (int)(param_1 + 3) + iVar2;
    FUN_00029b08(iVar5,param_6,uVar6);
    param_5 = param_5 - uVar6;
    param_6 = param_6 + uVar6;
    iVar4 = iVar4 + iVar2 + uVar6 + 0xc;
    uVar3 = 0;
    iVar1 = 0;
    if (*(char *)(param_4 + 9) != '\0') {
      uVar3 = uVar6 & 0xfffffff0;
      iVar1 = iVar2;
    }
    *param_1 = iVar2 + uVar6;
    param_1[1] = iVar1;
    param_1[2] = uVar3;
    param_1 = (int *)(iVar5 + uVar6);
  }
  return iVar4;
}



undefined1 *
FUN_00014cd4(undefined4 *param_1,undefined4 param_2,undefined4 param_3,int param_4,uint param_5,
            undefined4 param_6,undefined8 param_7,int param_8)

{
  undefined1 *puVar1;
  int iVar2;
  undefined1 *puVar3;
  int iVar4;
  int iVar5;
  uint uVar6;
  undefined4 *puVar7;
  
  puVar7 = param_1 + 3;
  FUN_00029ad0(puVar7,0,8);
  FUN_00021188(param_2,param_1 + 5,param_3,param_4,1);
  iVar2 = FUN_00021404(param_2);
  iVar5 = (int)(param_1 + 5) + iVar2;
  FUN_00029b08(iVar5,param_6,param_5);
  puVar1 = (undefined1 *)(iVar5 + param_5);
  iVar4 = 0;
  puVar3 = (undefined1 *)0x80029810;
  uVar6 = 0;
  if (*(char *)(param_4 + 0x24) != '\0') {
    uVar6 = param_5 & 0xfffffff0;
    iVar4 = iVar5 - (int)puVar7;
  }
  *puVar1 = 0xd;
  puVar1[1] = 10;
  if ((int)(param_5 + iVar2) <= param_8) {
    FUN_00029bb0(puVar7,&DAT_0002c1a8,param_5 + iVar2);
    *(undefined1 *)((int)param_1 + 0x13) = 10;
    *(undefined1 *)((int)param_1 + 0x12) = 0xd;
    *param_1 = puVar1 + (2 - (int)puVar7);
    puVar3 = puVar1 + (2 - (int)puVar7) + 0xc;
    param_1[1] = iVar4;
    param_1[2] = uVar6;
  }
  return puVar3;
}



undefined8 FUN_00014e60(int param_1,undefined4 *param_2)

{
  undefined4 uVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  
  FUN_00027ae0(param_2,0,0x2c);
  uVar1 = *(undefined4 *)(param_1 + 0x34);
  uVar2 = *(undefined4 *)(param_1 + 0x38);
  uVar3 = *(undefined4 *)(param_1 + 0x3c);
  *param_2 = *(undefined4 *)(param_1 + 0x30);
  param_2[1] = uVar1;
  param_2[2] = uVar2;
  param_2[3] = uVar3;
  uVar1 = *(undefined4 *)(param_1 + 0x44);
  uVar2 = *(undefined4 *)(param_1 + 0x48);
  uVar3 = *(undefined4 *)(param_1 + 0x4c);
  param_2[4] = *(undefined4 *)(param_1 + 0x40);
  param_2[5] = uVar1;
  param_2[6] = uVar2;
  param_2[7] = uVar3;
  uVar1 = *(undefined4 *)(param_1 + 0x50);
  uVar2 = *(undefined4 *)(param_1 + 0x54);
  param_2[10] = *(undefined4 *)(param_1 + 0x58);
  param_2[8] = uVar1;
  param_2[9] = uVar2;
  return 0;
}



void FUN_00014f00(int param_1,undefined4 param_2,undefined4 param_3)

{
  *(undefined4 *)(param_1 + 0xa8) = param_2;
  *(undefined4 *)(param_1 + 0xac) = param_3;
  FUN_000224d4(*(undefined4 *)(param_1 + 0x7c),2);
  return;
}



int FUN_00014f34(int param_1,undefined4 param_2,int param_3,uint param_4)

{
  undefined4 uVar1;
  int iVar2;
  int iVar3;
  int local_40 [6];
  
  iVar2 = FUN_00019230(*(undefined4 *)(param_1 + 0x80),local_40);
  if (-1 < iVar2) {
    iVar3 = *(int *)(param_1 + 0x38) * *(int *)(param_1 + 0x3c) * 4;
    *(int *)(local_40[0] + 0x88000) = iVar3;
    uVar1 = *(undefined4 *)(param_1 + 4);
    *(uint *)(local_40[0] + 0x88014) = param_3 << 0x10 | param_4 & 0xffff;
    *(undefined4 *)(local_40[0] + 0x88004) = uVar1;
    FUN_00029b08(local_40[0],param_2,iVar3);
    FUN_00019380(*(undefined4 *)(param_1 + 0x80),0x88018);
    *(int *)(param_1 + 4) = *(int *)(param_1 + 4) + 1;
  }
  return iVar2;
}



void FUN_00015000(int param_1,undefined8 param_2)

{
  if (*(int *)(param_1 + 0xb8) != -1) {
    syscall();
  }
  if (*(int *)(param_1 + 0xb4) != -1) {
    param_2 = 1;
    syscall();
    *(undefined4 *)(param_1 + 0xb4) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0xb8) != -1) {
    syscall();
    *(undefined4 *)(param_1 + 0xb8) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0x7c) != 0) {
    FUN_00022324(*(int *)(param_1 + 0x7c),param_2);
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x7c));
    *(undefined4 *)(param_1 + 0x7c) = 0;
  }
  if (*(int *)(param_1 + 0x78) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x78));
    *(undefined4 *)(param_1 + 0x78) = 0;
  }
  if (*(int *)(param_1 + 0x84) != 0) {
    FUN_00019404(*(int *)(param_1 + 0x84));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x84));
    *(undefined4 *)(param_1 + 0x84) = 0;
  }
  if (*(int *)(param_1 + 0x80) != 0) {
    FUN_00019404(*(int *)(param_1 + 0x80));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x80));
    *(undefined4 *)(param_1 + 0x80) = 0;
  }
  if (*(int *)(param_1 + 0xa0) != 0) {
    FUN_00019c44(*(int *)(param_1 + 0xa0));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0xa0));
    *(undefined4 *)(param_1 + 0xa0) = 0;
  }
  if (*(int *)(param_1 + 0x90) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x90));
    FUN_00022588(*(undefined4 *)(param_1 + 0x90));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x90));
    *(undefined4 *)(param_1 + 0x90) = 0;
  }
  if (*(int *)(param_1 + 0x9c) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x9c));
    *(undefined4 *)(param_1 + 0x9c) = 0;
  }
  return;
}



undefined4 FUN_00015198(int param_1)

{
  undefined4 uVar1;
  undefined1 auStack_30 [24];
  
  FUN_000224d4(*(undefined4 *)(param_1 + 0x7c),0x100);
  FUN_00019330(*(undefined4 *)(param_1 + 0x80));
  FUN_00019330(*(undefined4 *)(param_1 + 0x84));
  syscall();
  uVar1 = FUN_00028b80(*(undefined4 *)(param_1 + 0x5c),auStack_30);
  if (*(int *)(param_1 + 100) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 100));
    *(undefined4 *)(param_1 + 100) = 0;
  }
  if (*(int *)(param_1 + 0x74) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x74));
    *(undefined4 *)(param_1 + 0x74) = 0;
  }
  FUN_00015000(param_1);
  return uVar1;
}



int FUN_00015258(int param_1,int param_2,undefined4 param_3,int param_4,undefined8 param_5,
                undefined4 param_6,undefined4 param_7,undefined4 param_8)

{
  uint uVar1;
  uint uVar2;
  undefined4 uVar3;
  int iVar4;
  int iVar5;
  undefined4 in_stack_00000074;
  undefined4 in_stack_0000007c;
  undefined1 auStack_f0 [8];
  undefined4 local_e8;
  undefined4 local_e4;
  undefined4 local_e0;
  undefined4 local_dc;
  undefined4 local_d8;
  undefined4 local_d4;
  undefined1 local_d0;
  undefined4 local_c8 [4];
  int local_b8;
  undefined4 local_b4;
  int local_b0;
  undefined4 local_ac;
  undefined1 local_a8;
  undefined1 local_a7;
  undefined1 local_a6;
  undefined1 local_a5;
  undefined1 local_a4;
  undefined1 local_a3;
  undefined1 local_a2;
  undefined1 local_a1;
  undefined4 local_a0;
  undefined4 local_9c;
  int local_94;
  undefined4 local_90;
  undefined4 local_8c;
  undefined4 local_88;
  undefined4 local_84;
  undefined4 *local_80;
  undefined4 *local_7c;
  undefined4 local_78;
  undefined4 local_74;
  undefined4 local_70;
  undefined4 local_6c;
  undefined4 local_68;
  undefined4 local_64;
  undefined4 local_60;
  undefined4 local_5c;
  int local_58;
  undefined4 local_54;
  
  FUN_00029ad0(param_1,0,0xd8);
  *(undefined4 *)(param_1 + 0xb0) = in_stack_0000007c;
  *(undefined4 *)(param_1 + 0xb8) = 0xffffffff;
  *(undefined4 *)(param_1 + 0xb4) = 0xffffffff;
  *(undefined4 *)(param_1 + 0xa8) = 0;
  *(undefined4 *)(param_1 + 0xac) = 0;
  FUN_00029b08(param_1 + 0x30,param_2,0x2c);
  *(undefined4 *)(param_1 + 0x94) = param_6;
  *(undefined4 *)(param_1 + 0x98) = param_7;
  *(undefined4 *)(param_1 + 0xa4) = param_8;
  *(undefined4 *)(param_1 + 0x88) = in_stack_00000074;
  iVar4 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x30);
  *(int *)(param_1 + 0x7c) = iVar4;
  if (iVar4 == 0) {
    iVar4 = -0x7ffd67ff;
    goto LAB_00015854;
  }
  iVar4 = FUN_0002237c(iVar4);
  if (iVar4 < 0) goto LAB_00015854;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0xc);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x78) = iVar5;
  if (iVar5 == 0) goto LAB_00015854;
  FUN_000212e0(iVar5,0x7b,param_6);
  iVar4 = -0x7ffd67ff;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x40);
  *(int *)(param_1 + 0x84) = iVar5;
  if (iVar5 == 0) goto LAB_00015854;
  iVar4 = FUN_00021404(*(undefined4 *)(param_1 + 0x78));
  iVar4 = FUN_000195dc(iVar5,iVar4 + 0x20010,0x40,*(undefined4 *)(param_1 + 0xb0));
  if (iVar4 < 0) goto LAB_00015854;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x40);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x80) = iVar5;
  if ((iVar5 == 0) ||
     (iVar4 = FUN_000195dc(iVar5,0x88018,0x80,*(undefined4 *)(param_1 + 0xb0)), iVar4 < 0))
  goto LAB_00015854;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x30);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0xa0) = iVar5;
  if (iVar5 == 0) goto LAB_00015854;
  local_64 = *(undefined4 *)(param_1 + 0x38);
  uVar2 = *(uint *)(param_1 + 0x58) ^ 1;
  local_68 = *(undefined4 *)(param_1 + 0x34);
  uVar1 = (int)uVar2 >> 0x1f;
  local_60 = *(undefined4 *)(param_1 + 0x3c);
  local_6c = 0;
  local_58 = ((int)(uVar1 - (uVar1 ^ uVar2)) >> 0x1f) + 2;
  local_54 = 3;
  local_5c = local_64;
  iVar4 = FUN_00019cc4(iVar5,param_8,&local_6c,*(undefined4 *)(param_1 + 0xb0));
  if (iVar4 < 0) goto LAB_00015854;
  iVar4 = *(int *)(param_2 + 0x20);
  uVar3 = 4;
  if (iVar4 == 7) {
LAB_000154ac:
    *(undefined4 *)(param_1 + 0xd4) = uVar3;
  }
  else {
    if (iVar4 != 10) {
      uVar3 = 2;
      if (iVar4 != 0xf) {
        uVar3 = 1;
      }
      goto LAB_000154ac;
    }
    *(undefined4 *)(param_1 + 0xd4) = 3;
  }
  if (*(int *)(param_1 + 0x54) == 0) {
    local_88 = 0x101;
  }
  else {
    local_88 = 0x100;
  }
  local_84 = 0x11;
  local_80 = &local_a0;
  local_7c = &local_e4;
  local_74 = 0;
  local_78 = 0;
  local_70 = 1;
  iVar5 = FUN_00028de8(&local_88);
  iVar4 = -0x7ffd67f0;
  if (iVar5 == 0) {
    local_94 = *(int *)(param_1 + 0x48);
    local_a0 = *(undefined4 *)(param_1 + 0x34);
    local_9c = *(undefined4 *)(param_1 + 0x38);
    if (local_94 < 0xfa001) {
      if (local_94 < 0x1f400) {
        local_94 = 0x1f400;
      }
    }
    else {
      local_94 = 0xfa000;
    }
    local_90 = *(undefined4 *)(param_1 + 0xd4);
    local_8c = 0x1e;
    local_e4 = 1;
    local_e0 = 0;
    iVar4 = -0x7ffd67f0;
    local_dc = 0x100000;
    iVar5 = FUN_00028db0(&local_88,local_c8);
    if (iVar5 != 0) goto LAB_0001566c;
    *(undefined4 *)(param_1 + 0x68) = local_c8[0];
    iVar5 = FUN_00019898(*(undefined4 *)(param_1 + 0xb0),0x80,local_c8[0]);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 100) = iVar5;
    if (iVar5 == 0) goto LAB_0001566c;
    local_b4 = *(undefined4 *)(param_1 + 0x68);
    local_ac = *(undefined4 *)(param_1 + 0xa4);
    local_a1 = 2;
    local_a8 = 2;
    local_a7 = 2;
    local_a6 = 2;
    local_a5 = 2;
    local_a4 = 2;
    local_a3 = 2;
    local_a2 = 2;
    local_b8 = iVar5;
    local_b0 = param_4 + 4;
    iVar5 = FUN_00028d78(&local_88,&local_b8,auStack_f0,param_1 + 0x5c);
    iVar4 = -0x7ffd67f0;
    if (iVar5 != 0) goto LAB_0001566c;
    *(undefined4 *)(param_1 + 0x70) = local_e8;
    iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),local_e8);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 0x74) = iVar5;
    if (iVar5 == 0) goto LAB_0001568c;
  }
  else {
LAB_0001566c:
    if (*(int *)(param_1 + 0x74) != 0) {
      FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x74));
      *(undefined4 *)(param_1 + 0x74) = 0;
    }
LAB_0001568c:
    if (*(int *)(param_1 + 100) != 0) {
      FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 100));
      *(undefined4 *)(param_1 + 100) = 0;
    }
    if (iVar4 < 0) goto LAB_00015854;
  }
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),8);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x90) = iVar5;
  if (((iVar5 != 0) && (iVar4 = FUN_0002265c(iVar5), -1 < iVar4)) &&
     (iVar4 = FUN_000225ac(*(undefined4 *)(param_1 + 0x90),param_3), -1 < iVar4)) {
    iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0xc);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 0x9c) = iVar5;
    if (iVar5 != 0) {
      FUN_00019ec4(iVar5,0x1e);
      local_d8 = 2;
      iVar4 = param_1 + 0xb4;
      local_d4 = 1;
      local_d0 = 0;
      syscall();
      if (-1 < iVar4) {
        iVar4 = param_1 + 0xb8;
        syscall();
        if (-1 < iVar4) {
          iVar4 = *(int *)(param_1 + 0xb8);
          syscall();
          if (-1 < iVar4) {
            iVar4 = *(int *)(param_1 + 0xb8);
            syscall();
            if ((-1 < iVar4) &&
               (iVar4 = FUN_000298a0(param_1 + 0x28,&PTR_LAB_0002eb60,param_1,param_4 + 4,0x1000,1,
                                     "cellPremoMpeg4EncThread"), -1 < iVar4)) {
              syscall();
              FUN_000224d4(*(undefined4 *)(param_1 + 0x7c),1);
              return iVar4;
            }
          }
        }
      }
    }
  }
LAB_00015854:
  FUN_00015000(param_1);
  return iVar4;
}



undefined4 FUN_00015894(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  uVar1 = FUN_00022614(*(undefined4 *)(param_1 + 0x90),local_20,local_18,local_10);
  return uVar1;
}



undefined4 FUN_000158e0(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  uVar1 = FUN_00022614(*(undefined4 *)(param_1 + 0x90),local_20,local_18,local_10);
  return uVar1;
}



undefined4 FUN_0001592c(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  uVar1 = FUN_00022614(*(undefined4 *)(param_1 + 0x84),local_20,local_18,local_10);
  return uVar1;
}



undefined4 FUN_00015978(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  uVar1 = FUN_00022614(*(undefined4 *)(param_1 + 0x3c),local_20,local_18,local_10);
  return uVar1;
}



undefined4 FUN_000159c4(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  uVar1 = FUN_00022614(param_1 + 0xe0,local_20,local_18,local_10);
  return uVar1;
}



void FUN_00015a18(int param_1)

{
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  FUN_00022614(*(undefined4 *)(param_1 + 0xdc),local_20,local_18,local_10);
  return;
}



int FUN_00015a60(int param_1)

{
  undefined4 uVar1;
  undefined4 uVar2;
  int iVar3;
  int iVar4;
  int iVar5;
  undefined4 local_e0;
  undefined1 local_dc [12];
  undefined1 auStack_d0 [4];
  int local_cc;
  int local_c8;
  int local_c4;
  undefined1 auStack_c0 [36];
  undefined1 local_9c [40];
  undefined1 auStack_74 [84];
  
  iVar3 = FUN_00028aa0(*(undefined4 *)(param_1 + 0x5c),auStack_d0);
  if (iVar3 == 0) {
    if (local_cc != 0) {
      iVar4 = FUN_00028bf0(*(undefined4 *)(param_1 + 0x5c),auStack_74);
      if (iVar4 != 0) {
        return iVar4;
      }
      *(int *)(param_1 + 0xc) = *(int *)(param_1 + 0xc) + 1;
    }
    if (local_c4 != 0) {
      iVar4 = FUN_00028c60(*(undefined4 *)(param_1 + 0x5c),auStack_c0);
      if (iVar4 != 0) {
        return iVar4;
      }
      iVar4 = FUN_00028d40(*(undefined4 *)(param_1 + 0x5c),auStack_c0);
      if (iVar4 != 0) {
        return iVar4;
      }
      *(int *)(param_1 + 0x10) = *(int *)(param_1 + 0x10) + 1;
    }
    if (local_c8 != 0) {
      if (*(uint *)(param_1 + 0x14) < *(uint *)(param_1 + 0x1c)) {
        FUN_00028d08(*(undefined4 *)(param_1 + 0x5c));
      }
      else {
        uVar1 = *(undefined4 *)(param_1 + 0x70);
        uVar2 = *(undefined4 *)(param_1 + 0x74);
        *(undefined4 *)(param_1 + 0x1c) = 0;
        iVar3 = FUN_00028b10(*(undefined4 *)(param_1 + 0x5c),local_dc);
        if (iVar3 != 0) {
          FUN_00028d08(*(undefined4 *)(param_1 + 0x5c));
          return iVar3;
        }
        iVar3 = FUN_00019230(*(undefined4 *)(param_1 + 0x84),&local_e0);
        if (iVar3 < 0) {
          return iVar3;
        }
        if (*(int *)(param_1 + 0x18) == 0) {
          *(undefined4 *)(param_1 + 0x18) = 1;
        }
        if (*(char *)(param_1 + 0xbe) == '\0') {
          iVar4 = FUN_00014cd4(local_e0,*(undefined4 *)(param_1 + 0x78),
                               *(undefined4 *)(param_1 + 0x60),local_9c,uVar1,uVar2,
                               *(undefined4 *)(param_1 + 200),0x14800);
        }
        else {
          iVar4 = FUN_00014b50(local_e0,*(undefined4 *)(param_1 + 0x78),
                               *(undefined4 *)(param_1 + 0x60),local_9c,uVar1,uVar2,
                               *(undefined4 *)(param_1 + 200),*(undefined4 *)(param_1 + 0xc0));
        }
        iVar5 = iVar4;
        if (iVar4 < 0) {
          iVar5 = 0;
          iVar3 = iVar4;
        }
        FUN_00019380(*(undefined4 *)(param_1 + 0x84),iVar5);
      }
      *(int *)(param_1 + 0x14) = *(int *)(param_1 + 0x14) + 1;
    }
  }
  return iVar3;
}



int FUN_00015d08(int param_1)

{
  int iVar1;
  bool bVar2;
  int iVar3;
  int iVar4;
  int iVar5;
  char *pcVar6;
  int iVar7;
  byte *pbVar8;
  char *pcVar9;
  byte *pbVar10;
  uint uVar11;
  uint uVar12;
  ulonglong uVar13;
  byte *local_160;
  undefined1 local_15c [12];
  undefined1 auStack_150 [4];
  int local_14c;
  int local_148;
  int local_144;
  undefined1 local_140 [8];
  char *local_138;
  int local_134;
  byte local_130;
  char local_12f;
  char local_12e;
  char local_12d;
  char local_12c;
  undefined1 auStack_128 [36];
  undefined1 local_104 [40];
  undefined1 auStack_dc [28];
  int local_c0;
  
  iVar3 = FUN_00028678(*(undefined4 *)(param_1 + 0x5c),auStack_150,1);
  if (iVar3 != 0) {
    return -0x7ffd67f0;
  }
  if (local_14c != 0) {
    iVar3 = FUN_00028368(*(undefined4 *)(param_1 + 0x5c),auStack_dc);
    if (iVar3 != 0) {
      return -0x7ffd67f0;
    }
    *(int *)(param_1 + 0x10) = *(int *)(param_1 + 0x10) + 1;
  }
  if (local_144 != 0) {
    iVar3 = FUN_00028640(*(undefined4 *)(param_1 + 0x5c),auStack_128);
    if (iVar3 != 0) {
      return -0x7ffd67f0;
    }
    iVar3 = FUN_00028598(*(undefined4 *)(param_1 + 0x5c),auStack_128);
    if (iVar3 != 0) {
      return -0x7ffd67f0;
    }
    *(int *)(param_1 + 0x14) = *(int *)(param_1 + 0x14) + 1;
  }
  iVar3 = 0;
  if (local_148 == 0) {
    return 0;
  }
  if (*(uint *)(param_1 + 0x18) < *(uint *)(param_1 + 0x20)) {
    FUN_00028410();
  }
  else {
    iVar5 = *(int *)(param_1 + 0x70);
    local_138 = *(char **)(param_1 + 0x74);
    *(undefined4 *)(param_1 + 0x20) = 0;
    iVar3 = FUN_00028608(*(undefined4 *)(param_1 + 0x5c),local_15c);
    if (iVar3 != 0) {
      FUN_00028410(*(undefined4 *)(param_1 + 0x5c));
      return -0x7ffd67f0;
    }
    iVar3 = FUN_00019230(*(undefined4 *)(param_1 + 0x84),&local_160);
    if (iVar3 < 0) {
      return iVar3;
    }
    bVar2 = false;
    if ((*(byte *)(param_1 + 0xc4) != 0) &&
       ((local_c0 == 4 || ((*(byte *)(param_1 + 0xc4) & 2) != 0)))) {
      bVar2 = true;
    }
    if (*(int *)(param_1 + 0x1c) == 0) {
      *(undefined4 *)(param_1 + 0x1c) = 1;
    }
    if (*(char *)(param_1 + 0xbe) == '\0') {
      iVar5 = FUN_00014cd4(local_160,*(undefined4 *)(param_1 + 0x78),*(undefined4 *)(param_1 + 0x60)
                           ,local_104,iVar5,local_138,*(undefined4 *)(param_1 + 200),FUN_00028800);
    }
    else {
      iVar4 = FUN_00021404(*(undefined4 *)(param_1 + 0x78));
      iVar1 = *(int *)(param_1 + 0xc0);
      pcVar9 = local_138 + iVar5;
      iVar5 = 0;
      FUN_00029ad0(local_140,0,0x18);
      while (local_138 = local_138 + local_134, local_138 < pcVar9) {
        if ((local_12f == '\0') || (local_12d != '\0')) {
          local_130 = 0;
          pcVar6 = pcVar9 + (-2 - (int)local_138);
          if ((pcVar9 + -3 < local_138) || (pcVar9 == (char *)0x3)) {
            pcVar6 = (char *)0x1;
          }
          while (pcVar6 = pcVar6 + -1, pcVar6 != (char *)0x0) {
            if (((*local_138 == '\0') && (local_138[1] == '\0')) && (local_138[2] == '\x01')) {
              local_130 = local_138[3];
              uVar11 = local_130 & 0x1f;
              if (((1 < uVar11 - 7) && (uVar11 != 6)) && ((uVar11 != 9 && (uVar11 != 0xb)))) break;
            }
            local_138 = local_138 + 1;
          }
          if (local_130 == 0) break;
          local_138 = local_138 + 4;
          local_12e = '\x01';
          local_12f = '\x01';
          uVar13 = (ulonglong)(iVar1 + 1);
          local_12d = '\0';
          pcVar6 = local_138;
          if ((local_138 + iVar1 < local_138) || (local_138 + iVar1 == (char *)0x0)) {
            uVar13 = 1;
          }
          while( true ) {
            uVar13 = uVar13 - 1;
            if (uVar13 == 0) break;
            if (pcVar9 + -4 <= pcVar6) {
              local_12e = '\0';
              local_12f = '\0';
              pcVar6 = pcVar9;
              if (((local_130 & 0x1f) == 1) || ((local_130 & 0x1f) == 5)) {
                local_12c = '\x01';
              }
              break;
            }
            if (((*pcVar6 == '\0') && (pcVar6[1] == '\0')) && (pcVar6[2] == '\x01')) {
              local_12e = '\0';
              local_12f = '\0';
              break;
            }
            pcVar6 = pcVar6 + 1;
          }
          local_134 = (int)pcVar6 - (int)local_138;
        }
        else {
          local_12e = '\0';
          uVar13 = (ulonglong)(iVar1 + 1);
          pcVar6 = local_138;
          if ((local_138 + iVar1 < local_138) || (local_138 + iVar1 == (char *)0x0)) {
            uVar13 = 1;
          }
          while( true ) {
            uVar13 = uVar13 - 1;
            if (uVar13 == 0) break;
            if (pcVar9 + -3 <= pcVar6) {
              local_12d = '\x01';
              pcVar6 = pcVar9;
              if (((local_130 & 0x1f) == 1) || ((local_130 & 0x1f) == 5)) {
                local_12c = '\x01';
              }
              break;
            }
            if (((*pcVar6 == '\0') && (pcVar6[1] == '\0')) && (pcVar6[2] == '\x01')) {
              local_12d = '\x01';
              break;
            }
            pcVar6 = pcVar6 + 1;
          }
          local_134 = (int)pcVar6 - (int)local_138;
        }
        pbVar10 = local_160 + 0xc + iVar4;
        if (local_12f == '\0') {
          pbVar8 = pbVar10 + 1;
          *pbVar10 = local_130;
          uVar11 = local_134 + 1;
        }
        else {
          pbVar8 = pbVar10 + 2;
          *pbVar10 = 0x3c;
          pbVar10[1] = local_130 & 0x1f |
                       (byte)((ulonglong)
                              ((longlong)((int)local_12c >> 0x1f) -
                              ((longlong)((int)local_12c >> 0x1f) ^ (longlong)local_12c)) >> 0x18)
                       >> 1 & 0x40 | ((byte)(local_12e + -1 >> 0x1f) & 0x80) + 0x80;
          uVar11 = local_134 + 2;
        }
        FUN_00021188(*(undefined4 *)(param_1 + 0x78),local_160 + 0xc,*(undefined4 *)(param_1 + 0x60)
                     ,local_104,local_12c);
        FUN_00029b08(pbVar8,local_138,local_134);
        if (bVar2) {
          uVar12 = uVar11 & 0xfffffff0;
          iVar7 = iVar4;
          if (*(int *)(param_1 + 200) == 1) {
            iVar3 = FUN_000235c8(pbVar10,pbVar10,uVar12,*(undefined4 *)(param_1 + 0xcc),
                                 *(undefined4 *)(param_1 + 0xd0));
            if (iVar3 < 0) {
              return iVar3;
            }
          }
        }
        else {
          uVar12 = 0;
          iVar7 = 0;
        }
        iVar5 = iVar4 + uVar11 + iVar5 + 0xc;
        *(int *)(local_160 + 4) = iVar7;
        *(uint *)(local_160 + 8) = uVar12;
        *(uint *)local_160 = iVar4 + uVar11;
        local_160 = pbVar8 + local_134;
      }
    }
    FUN_00019380(*(undefined4 *)(param_1 + 0x84),iVar5);
  }
  *(int *)(param_1 + 0x18) = *(int *)(param_1 + 0x18) + 1;
  return iVar3;
}



int FUN_000163dc(int param_1)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_00019b78(0);
  iVar2 = FUN_00020f2c(0x7b);
  iVar2 = iVar1 + 0xa8090 + iVar2;
  iVar1 = iVar2 + 0x256c00;
  if (*(int *)(param_1 + 0x24) != 0) {
    iVar1 = iVar2 + 0x28e800;
  }
  return iVar1 + 0x24000;
}



void FUN_00016450(int param_1,undefined4 *param_2,undefined4 *param_3)

{
  *param_2 = *(undefined4 *)(param_1 + 0x34);
  *param_3 = *(undefined4 *)(param_1 + 0x38);
  return;
}



void FUN_00016464(int param_1)

{
  *(undefined4 *)(param_1 + 0x20) = *(undefined4 *)(param_1 + 0xc);
  *(undefined4 *)(param_1 + 0x24) = 1;
  return;
}



void FUN_00016478(int param_1,undefined1 param_2)

{
  *(undefined1 *)(param_1 + 0xbc) = param_2;
  return;
}



void FUN_00016480(int param_1,undefined1 param_2,undefined4 param_3,undefined4 param_4,
                 undefined4 param_5)

{
  *(undefined4 *)(param_1 + 0xd0) = param_5;
  *(undefined1 *)(param_1 + 0xc4) = param_2;
  *(undefined4 *)(param_1 + 200) = param_3;
  *(undefined4 *)(param_1 + 0xcc) = param_4;
  return;
}



void FUN_000164a0(int param_1,undefined1 param_2,int param_3)

{
  uint uVar1;
  int iVar2;
  
  uVar1 = 0x560;
  if (param_3 != 0) {
    iVar2 = FUN_00021404(*(undefined4 *)(param_1 + 0x78));
    uVar1 = param_3 - iVar2;
    if (uVar1 < 500) {
      uVar1 = 500;
    }
    else if (0x57e < uVar1) {
      uVar1 = 0x57e;
    }
  }
  *(undefined1 *)(param_1 + 0xbe) = param_2;
  *(uint *)(param_1 + 0xc0) = uVar1;
  return;
}



void FUN_0001651c(int param_1,undefined8 param_2)

{
  FUN_00029b08(param_2,param_1 + 0x30,0x2c);
  return;
}



void FUN_0001654c(int param_1,undefined4 param_2,undefined4 param_3)

{
  *(undefined4 *)(param_1 + 0xa8) = param_2;
  *(undefined4 *)(param_1 + 0xac) = param_3;
  FUN_000224d4(*(undefined4 *)(param_1 + 0x7c),2);
  return;
}



int FUN_00016580(int param_1,undefined4 param_2,int param_3,uint param_4)

{
  undefined4 uVar1;
  int iVar2;
  int iVar3;
  int local_40 [6];
  
  iVar2 = FUN_00019230(*(undefined4 *)(param_1 + 0x80),local_40);
  if (-1 < iVar2) {
    iVar3 = *(int *)(param_1 + 0x38) * *(int *)(param_1 + 0x3c) * 4;
    *(int *)(local_40[0] + 0x88000) = iVar3;
    uVar1 = *(undefined4 *)(param_1 + 8);
    *(uint *)(local_40[0] + 0x88008) = param_3 << 0x10 | param_4 & 0xffff;
    *(undefined4 *)(local_40[0] + 0x88004) = uVar1;
    FUN_00029b08(local_40[0],param_2,iVar3);
    FUN_00019380(*(undefined4 *)(param_1 + 0x80),0x8800c);
    *(int *)(param_1 + 8) = *(int *)(param_1 + 8) + 1;
  }
  return iVar2;
}



void FUN_0001664c(int param_1,undefined8 param_2)

{
  if (*(int *)(param_1 + 0xb8) != -1) {
    syscall();
  }
  if (*(int *)(param_1 + 0xb4) != -1) {
    param_2 = 1;
    syscall();
    *(undefined4 *)(param_1 + 0xb4) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0xb8) != -1) {
    syscall();
    *(undefined4 *)(param_1 + 0xb8) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0x7c) != 0) {
    FUN_00022324(*(int *)(param_1 + 0x7c),param_2);
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x7c));
    *(undefined4 *)(param_1 + 0x7c) = 0;
  }
  if (*(int *)(param_1 + 0x78) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x78));
    *(undefined4 *)(param_1 + 0x78) = 0;
  }
  if (*(int *)(param_1 + 0x84) != 0) {
    FUN_00019404(*(int *)(param_1 + 0x84));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x84));
    *(undefined4 *)(param_1 + 0x84) = 0;
  }
  if (*(int *)(param_1 + 0x80) != 0) {
    FUN_00019404(*(int *)(param_1 + 0x80));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x80));
    *(undefined4 *)(param_1 + 0x80) = 0;
  }
  if (*(int *)(param_1 + 0xa0) != 0) {
    FUN_00019c44(*(int *)(param_1 + 0xa0));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0xa0));
    *(undefined4 *)(param_1 + 0xa0) = 0;
  }
  if (*(int *)(param_1 + 0x90) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x90));
    FUN_00022588(*(undefined4 *)(param_1 + 0x90));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x90));
    *(undefined4 *)(param_1 + 0x90) = 0;
  }
  if (*(int *)(param_1 + 0x9c) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x9c));
    *(undefined4 *)(param_1 + 0x9c) = 0;
  }
  return;
}



undefined4 FUN_000167e4(int param_1)

{
  undefined4 uVar1;
  undefined1 auStack_30 [24];
  
  FUN_000224d4(*(undefined4 *)(param_1 + 0x7c),0x100);
  FUN_00019330(*(undefined4 *)(param_1 + 0x80));
  FUN_00019330(*(undefined4 *)(param_1 + 0x84));
  syscall();
  uVar1 = FUN_000284b8(*(undefined4 *)(param_1 + 0x5c),auStack_30);
  if (*(int *)(param_1 + 100) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 100));
    *(undefined4 *)(param_1 + 100) = 0;
  }
  if (*(int *)(param_1 + 0x74) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x74));
    *(undefined4 *)(param_1 + 0x74) = 0;
  }
  FUN_0001664c(param_1);
  return uVar1;
}



int FUN_000168a4(int param_1,int param_2,undefined4 param_3,int param_4,undefined8 param_5,
                undefined4 param_6,undefined4 param_7,undefined4 param_8)

{
  uint uVar1;
  uint uVar2;
  undefined1 *puVar3;
  int iVar4;
  int iVar5;
  undefined8 uVar6;
  undefined4 in_stack_00000074;
  undefined4 in_stack_0000007c;
  undefined4 in_stack_00000084;
  undefined4 local_120;
  undefined4 local_11c;
  undefined1 auStack_118 [8];
  undefined4 local_110;
  undefined4 local_10c;
  undefined4 local_108;
  undefined1 local_104;
  undefined4 local_f8 [4];
  undefined4 local_e8;
  undefined4 local_e4;
  int local_dc;
  undefined4 local_d8;
  code *local_d4;
  int local_d0;
  undefined4 local_cc;
  int local_c8;
  undefined4 local_c4;
  undefined1 local_c0;
  undefined1 local_bf;
  undefined1 local_be;
  undefined1 local_bd;
  undefined1 local_bc;
  undefined1 local_bb;
  undefined1 local_ba;
  undefined1 local_b9;
  undefined4 local_b8;
  undefined4 local_b4;
  undefined4 *local_b0;
  undefined4 *local_ac;
  undefined4 *local_a8;
  undefined4 local_a4;
  undefined4 local_a0;
  undefined4 local_9c;
  undefined4 local_98;
  undefined4 local_94;
  undefined4 local_90;
  undefined4 local_8c;
  int local_88;
  undefined4 local_84;
  undefined4 local_80;
  undefined4 local_7c;
  undefined4 local_78;
  undefined4 local_74;
  undefined *local_70;
  undefined4 local_6c;
  undefined1 *local_68;
  undefined4 local_64;
  
  FUN_00029ad0(param_1,0,0xe8);
  *(undefined4 *)(param_1 + 0xb0) = in_stack_00000084;
  *(undefined4 *)(param_1 + 0xac) = 0;
  *(undefined4 *)(param_1 + 0xb8) = 0xffffffff;
  *(undefined4 *)(param_1 + 0xe0) = 0;
  *(undefined4 *)(param_1 + 0xa8) = 0;
  *(undefined4 *)(param_1 + 0xb4) = 0xffffffff;
  *(undefined8 *)(param_1 + 0xd8) = 0;
  *(undefined4 *)(param_1 + 4) = param_7;
  FUN_00029b08(param_1 + 0x30,param_2,0x2c);
  *(undefined4 *)(param_1 + 0x94) = param_6;
  *(undefined4 *)(param_1 + 0x98) = param_8;
  *(undefined4 *)(param_1 + 0x88) = in_stack_0000007c;
  *(undefined4 *)(param_1 + 0xa4) = in_stack_00000074;
  iVar4 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x30);
  *(int *)(param_1 + 0x7c) = iVar4;
  if (iVar4 == 0) {
    iVar4 = -0x7ffd67ff;
    goto LAB_00016f48;
  }
  iVar4 = FUN_0002237c(iVar4);
  if (iVar4 < 0) goto LAB_00016f48;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0xc);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x78) = iVar5;
  if (iVar5 == 0) goto LAB_00016f48;
  uVar6 = 0x7f;
  if (*(int *)(param_1 + 4) != 2) {
    uVar6 = 0x7e;
  }
  FUN_000212e0(iVar5,uVar6,param_6);
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x40);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x84) = iVar5;
  if (iVar5 == 0) goto LAB_00016f48;
  iVar4 = FUN_00021404(*(undefined4 *)(param_1 + 0x78));
  iVar4 = FUN_000195dc(iVar5,iVar4 + 0x28010,0x40,*(undefined4 *)(param_1 + 0xb0));
  if (iVar4 < 0) goto LAB_00016f48;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x40);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x80) = iVar5;
  if ((iVar5 == 0) ||
     (iVar4 = FUN_000195dc(iVar5,0x8800c,0x80,*(undefined4 *)(param_1 + 0xb0)), iVar4 < 0))
  goto LAB_00016f48;
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x30);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0xa0) = iVar5;
  if (iVar5 == 0) goto LAB_00016f48;
  local_94 = *(undefined4 *)(param_1 + 0x38);
  uVar2 = *(uint *)(param_1 + 0x58) ^ 1;
  local_98 = *(undefined4 *)(param_1 + 0x34);
  uVar1 = (int)uVar2 >> 0x1f;
  local_90 = *(undefined4 *)(param_1 + 0x3c);
  local_9c = 1;
  local_88 = ((int)(uVar1 - (uVar1 ^ uVar2)) >> 0x1f) + 2;
  local_84 = 3;
  local_8c = local_94;
  iVar4 = FUN_00019cc4(iVar5,in_stack_00000074,&local_9c,*(undefined4 *)(param_1 + 0xb0));
  if (iVar4 < 0) goto LAB_00016f48;
  iVar4 = *(int *)(param_2 + 0x20);
  puVar3 = (undefined1 *)0x1f40;
  if (((iVar4 != 7) && (puVar3 = &LAB_00001770, iVar4 != 10)) &&
     (puVar3 = (undefined1 *)0xfa0, iVar4 != 0xf)) {
    puVar3 = (undefined1 *)0x7d2;
  }
  *(undefined1 **)(param_1 + 0xe4) = puVar3;
  local_80 = 0xa781;
  local_70 = &DAT_00001285;
  local_78 = 0xa771;
  local_68 = &LAB_00004ad4;
  if (*(int *)(param_1 + 4) == 2) {
    local_b8 = 0x111;
  }
  else {
    local_b8 = 0x101;
  }
  if (((*(int *)(param_1 + 0x34) < 0x141) && (*(int *)(param_1 + 0x38) < 0xf1)) &&
     (*(int *)(param_1 + 0x48) < 0xbb801)) {
    local_b4 = 1;
  }
  else {
    local_b4 = 0x21;
  }
  local_b0 = &local_e8;
  local_ac = &local_120;
  local_a8 = &local_80;
  iVar4 = -0x7ffd67f0;
  local_a4 = 3;
  local_a0 = 1;
  iVar5 = FUN_00028560(&local_b8);
  if (iVar5 == 0) {
    local_dc = *(int *)(param_1 + 0x48);
    local_e8 = *(undefined4 *)(param_1 + 0x34);
    local_e4 = *(undefined4 *)(param_1 + 0x38);
    if (local_dc < 0xbb801) {
      if (local_dc < 0x1f400) {
        local_dc = 0x1f400;
      }
    }
    else {
      local_dc = 0xbb800;
    }
    local_d8 = *(undefined4 *)(param_1 + 0xe4);
    local_7c = 0x9fe98;
    local_74 = 0xa0000;
    local_6c = 8;
    local_120 = 1;
    local_d4 = FUN_0000ea60;
    local_64 = 0xbb800;
    local_11c = 0;
    iVar5 = FUN_00028528(&local_b8,local_f8);
    iVar4 = -0x7ffd67f0;
    if (iVar5 != 0) goto LAB_00016d68;
    *(undefined4 *)(param_1 + 0x68) = local_f8[0];
    iVar5 = FUN_00019898(*(undefined4 *)(param_1 + 0xb0),0x80,local_f8[0]);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 100) = iVar5;
    if (iVar5 == 0) goto LAB_00016d68;
    local_cc = *(undefined4 *)(param_1 + 0x68);
    local_c4 = *(undefined4 *)(param_1 + 0xa4);
    local_b9 = 2;
    local_c0 = 2;
    local_bf = 2;
    local_be = 2;
    local_bd = 2;
    local_bc = 2;
    local_bb = 2;
    local_ba = 2;
    local_d0 = iVar5;
    local_c8 = param_4 + 4;
    iVar5 = FUN_000286b0(&local_b8,&local_d0,auStack_118,param_1 + 0x5c);
    iVar4 = -0x7ffd67f0;
    if (iVar5 != 0) goto LAB_00016d68;
    *(undefined4 *)(param_1 + 0x70) = local_110;
    iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),local_110);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 0x74) = iVar5;
    if (iVar5 == 0) goto LAB_00016d88;
  }
  else {
LAB_00016d68:
    if (*(int *)(param_1 + 0x74) != 0) {
      FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x74));
      *(undefined4 *)(param_1 + 0x74) = 0;
    }
LAB_00016d88:
    if (*(int *)(param_1 + 100) != 0) {
      FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 100));
      *(undefined4 *)(param_1 + 100) = 0;
    }
    if (iVar4 < 0) goto LAB_00016f48;
  }
  iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),8);
  iVar4 = -0x7ffd67ff;
  *(int *)(param_1 + 0x90) = iVar5;
  if (((iVar5 != 0) && (iVar4 = FUN_0002265c(iVar5), -1 < iVar4)) &&
     (iVar4 = FUN_000225ac(*(undefined4 *)(param_1 + 0x90),param_3), -1 < iVar4)) {
    iVar5 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0xc);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 0x9c) = iVar5;
    if (iVar5 != 0) {
      FUN_00019ec4(iVar5,FUN_0000ea60);
      local_10c = 2;
      local_108 = 1;
      local_104 = 0;
      syscall();
      iVar4 = param_1 + 0xb8;
      syscall();
      if (-1 < iVar4) {
        iVar4 = *(int *)(param_1 + 0xb8);
        syscall();
        if (-1 < iVar4) {
          iVar4 = *(int *)(param_1 + 0xb8);
          syscall();
          if ((-1 < iVar4) &&
             (iVar4 = FUN_000298a0(param_1 + 0x28,&PTR_LAB_0002eb68,param_1,param_4 + 4,0x1000,1,
                                   "cellPremoAvcEncThread"), -1 < iVar4)) {
            syscall();
            FUN_000224d4(*(undefined4 *)(param_1 + 0x7c),1);
            return iVar4;
          }
        }
      }
    }
  }
LAB_00016f48:
  FUN_0001664c(param_1);
  return iVar4;
}



int FUN_00016f88(void)

{
  int iVar1;
  int iVar2;
  
  iVar1 = FUN_00019b78(1);
  iVar2 = FUN_00020f2c(0x7e);
  return iVar2 + iVar1 + 0x480e30;
}



void FUN_00016fd8(int param_1,undefined4 *param_2,undefined4 *param_3)

{
  *param_2 = *(undefined4 *)(param_1 + 0x2c);
  *param_3 = *(undefined4 *)(param_1 + 0x30);
  return;
}



void FUN_00016fec(int param_1)

{
  *(undefined4 *)(param_1 + 0x14) = *(undefined4 *)(param_1 + 4);
  *(undefined4 *)(param_1 + 0x18) = 1;
  return;
}



void FUN_00017000(int param_1,undefined1 param_2)

{
  *(undefined1 *)(param_1 + 0xbc) = param_2;
  return;
}



void FUN_00017008(int param_1,undefined1 param_2,undefined4 param_3,undefined4 param_4,
                 undefined4 param_5)

{
  *(undefined4 *)(param_1 + 0xd0) = param_5;
  *(undefined1 *)(param_1 + 0xc4) = param_2;
  *(undefined4 *)(param_1 + 200) = param_3;
  *(undefined4 *)(param_1 + 0xcc) = param_4;
  return;
}



void FUN_00017028(int param_1,undefined1 param_2,int param_3)

{
  uint uVar1;
  int iVar2;
  
  uVar1 = 0x560;
  if (param_3 != 0) {
    iVar2 = FUN_00021404(*(undefined4 *)(param_1 + 0x6c));
    uVar1 = param_3 - iVar2;
    if (uVar1 < 500) {
      uVar1 = 500;
    }
    else if (0x57e < uVar1) {
      uVar1 = 0x57e;
    }
  }
  *(undefined1 *)(param_1 + 0xbe) = param_2;
  *(uint *)(param_1 + 0xc0) = uVar1;
  return;
}



void FUN_000170a4(int param_1,undefined4 *param_2)

{
  undefined4 uVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  
  FUN_00027ae0(param_2,0,0x2c);
  uVar1 = *(undefined4 *)(param_1 + 0x2c);
  uVar2 = *(undefined4 *)(param_1 + 0x30);
  uVar3 = *(undefined4 *)(param_1 + 0x34);
  *param_2 = *(undefined4 *)(param_1 + 0x28);
  param_2[1] = uVar1;
  param_2[2] = uVar2;
  param_2[3] = uVar3;
  uVar1 = *(undefined4 *)(param_1 + 0x3c);
  uVar2 = *(undefined4 *)(param_1 + 0x40);
  uVar3 = *(undefined4 *)(param_1 + 0x44);
  param_2[4] = *(undefined4 *)(param_1 + 0x38);
  param_2[5] = uVar1;
  param_2[6] = uVar2;
  param_2[7] = uVar3;
  uVar1 = *(undefined4 *)(param_1 + 0x48);
  uVar2 = *(undefined4 *)(param_1 + 0x4c);
  param_2[10] = *(undefined4 *)(param_1 + 0x50);
  param_2[8] = uVar1;
  param_2[9] = uVar2;
  return;
}



void FUN_00017140(int param_1,undefined4 param_2,undefined4 param_3)

{
  *(undefined4 *)(param_1 + 0x9c) = param_2;
  *(undefined4 *)(param_1 + 0xa0) = param_3;
  FUN_000224d4(*(undefined4 *)(param_1 + 0x70),2);
  return;
}



int FUN_00017174(int *param_1,undefined4 param_2,int param_3,uint param_4)

{
  int iVar1;
  int iVar2;
  int iVar3;
  int local_40 [6];
  
  iVar2 = FUN_000191f0(param_1[0x1d]);
  iVar3 = 0;
  if (iVar2 < 1) {
    iVar3 = FUN_00019230(param_1[0x1d],local_40);
    if (-1 < iVar3) {
      iVar2 = param_1[0x2b];
      *(int *)(local_40[0] + 0x9d800) = iVar2;
      iVar1 = *param_1;
      *(uint *)(local_40[0] + 0x9d808) = param_3 << 0x10 | param_4 & 0xffff;
      *(int *)(local_40[0] + 0x9d804) = iVar1;
      FUN_00029b08(local_40[0],param_2,iVar2);
      FUN_00019380(param_1[0x1d],0x9d80c);
      *param_1 = *param_1 + 1;
    }
  }
  else {
    DAT_0002f1dd = DAT_0002f1dd + '\x01';
  }
  return iVar3;
}



undefined4 FUN_0001725c(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00028ec8(*(undefined4 *)(param_1 + 0x54));
  if (*(int *)(param_1 + 0x68) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x68));
    *(undefined4 *)(param_1 + 0x68) = 0;
  }
  if (*(int *)(param_1 + 0x5c) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x5c));
    *(undefined4 *)(param_1 + 0x5c) = 0;
  }
  return uVar1;
}



void FUN_000172dc(int param_1,undefined8 param_2)

{
  if (*(int *)(param_1 + 0xb8) != -1) {
    syscall();
  }
  if (*(int *)(param_1 + 0xb4) != -1) {
    param_2 = 1;
    syscall();
    *(undefined4 *)(param_1 + 0xb4) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0xb8) != -1) {
    syscall();
    *(undefined4 *)(param_1 + 0xb8) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0x70) != 0) {
    FUN_00022324(*(int *)(param_1 + 0x70),param_2);
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x70));
    *(undefined4 *)(param_1 + 0x70) = 0;
  }
  if (*(int *)(param_1 + 0x6c) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x6c));
    *(undefined4 *)(param_1 + 0x6c) = 0;
  }
  if (*(int *)(param_1 + 0x78) != 0) {
    FUN_00019404(*(int *)(param_1 + 0x78));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x78));
    *(undefined4 *)(param_1 + 0x78) = 0;
  }
  if (*(int *)(param_1 + 0x74) != 0) {
    FUN_00019404(*(int *)(param_1 + 0x74));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x74));
    *(undefined4 *)(param_1 + 0x74) = 0;
  }
  if (*(int *)(param_1 + 0x84) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x84));
    FUN_00022588(*(undefined4 *)(param_1 + 0x84));
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(undefined4 *)(param_1 + 0x84));
    *(undefined4 *)(param_1 + 0x84) = 0;
  }
  if (*(int *)(param_1 + 0x90) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0xb0),*(int *)(param_1 + 0x90));
    *(undefined4 *)(param_1 + 0x90) = 0;
  }
  return;
}



undefined4 FUN_0001744c(int param_1)

{
  undefined4 uVar1;
  undefined1 auStack_30 [16];
  
  FUN_000224d4(*(undefined4 *)(param_1 + 0x70),0x100);
  FUN_00019330(*(undefined4 *)(param_1 + 0x74));
  FUN_00019330(*(undefined4 *)(param_1 + 0x78));
  syscall();
  uVar1 = FUN_0001725c(param_1,auStack_30);
  FUN_000172dc(param_1);
  return uVar1;
}



int FUN_000174c0(int param_1)

{
  int iVar1;
  int iVar2;
  undefined1 local_c0 [16];
  undefined4 local_b0 [4];
  undefined1 local_a0 [16];
  undefined1 local_90 [24];
  undefined1 local_78 [24];
  undefined1 local_60 [32];
  
  FUN_00029ad0(local_60,0,0x18);
  FUN_00029ad0(local_90,0,0x18);
  FUN_00029ad0(local_a0,0,0x10);
  FUN_00029ad0(local_b0,0,0x10);
  iVar1 = FUN_00028e58(local_60,local_b0);
  if (-1 < iVar1) {
    *(undefined4 *)(param_1 + 0x60) = local_b0[0];
    iVar2 = FUN_00019898(*(undefined4 *)(param_1 + 0xb0),0x80,local_b0[0]);
    iVar1 = -0x7ffd67ff;
    *(int *)(param_1 + 0x5c) = iVar2;
    if ((iVar2 != 0) &&
       (iVar1 = FUN_00028e90(local_60,local_78,local_c0,param_1 + 0x54), iVar1 == 0)) {
      *(undefined4 *)(param_1 + 100) = 0x4bf00;
      iVar1 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x4bf00);
      *(int *)(param_1 + 0x68) = iVar1;
      if (iVar1 != 0) {
        return 0;
      }
      iVar1 = -0x7ffd67ff;
    }
  }
  FUN_0001725c(param_1);
  return iVar1;
}



int FUN_000176d8(int param_1,int param_2,undefined4 param_3,int param_4,int param_5,
                undefined4 param_6,undefined4 param_7,undefined4 param_8)

{
  undefined1 *puVar1;
  int iVar2;
  int iVar3;
  uint uVar4;
  undefined4 in_stack_00000074;
  undefined4 in_stack_0000007c;
  undefined4 in_stack_00000084;
  
  FUN_00029ad0(param_1,0,0xe8);
  *(undefined4 *)(param_1 + 0xb0) = in_stack_00000084;
  *(undefined4 *)(param_1 + 0xa0) = 0;
  *(undefined4 *)(param_1 + 0x9c) = 0;
  *(undefined4 *)(param_1 + 0xb8) = 0xffffffff;
  *(undefined4 *)(param_1 + 0xb4) = 0xffffffff;
  FUN_00029b08(param_1 + 0x28,param_2,0x2c);
  uVar4 = *(uint *)(param_1 + 0x38);
  *(undefined4 *)(param_1 + 0x98) = in_stack_00000074;
  *(undefined4 *)(param_1 + 0x88) = param_6;
  *(undefined4 *)(param_1 + 0x8c) = param_7;
  *(undefined4 *)(param_1 + 0x94) = param_8;
  *(undefined4 *)(param_1 + 0x7c) = in_stack_0000007c;
  *(int *)(param_1 + 0xd8) = param_4;
  *(int *)(param_1 + 0xdc) = param_5;
  if (uVar4 == 0) {
    uVar4 = *(uint *)(param_1 + 0x2c);
    *(uint *)(param_1 + 0xa4) = uVar4 + 0x3f & 0xffffffc0;
    *(uint *)(param_1 + 0xa8) =
         ((int)uVar4 >> 1) + (uint)((int)uVar4 < 0 && (uVar4 & 1) != 0) + 0x3f & 0xffffffc0;
  }
  else {
    *(uint *)(param_1 + 0xa4) = uVar4;
    *(uint *)(param_1 + 0xa8) = ((int)uVar4 >> 1) + (uint)((int)uVar4 < 0 && (uVar4 & 1) != 0);
  }
  iVar2 = -0x7ffd67fc;
  uVar4 = (*(int *)(param_1 + 0xa4) + *(int *)(param_1 + 0xa8)) * *(int *)(param_1 + 0x30);
  *(uint *)(param_1 + 0xac) = uVar4;
  if (uVar4 < 0x9d801) {
    iVar2 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x30);
    *(int *)(param_1 + 0x70) = iVar2;
    if (iVar2 == 0) {
      iVar2 = -0x7ffd67ff;
    }
    else {
      iVar2 = FUN_0002237c(iVar2);
      if (-1 < iVar2) {
        iVar3 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0xc);
        iVar2 = -0x7ffd67ff;
        *(int *)(param_1 + 0x6c) = iVar3;
        if (iVar3 != 0) {
          FUN_000212e0(iVar3,0x7a,param_6);
          iVar2 = -0x7ffd67ff;
          iVar3 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x40);
          *(int *)(param_1 + 0x78) = iVar3;
          if (iVar3 != 0) {
            iVar2 = FUN_00021404(*(undefined4 *)(param_1 + 0x6c));
            iVar2 = FUN_000195dc(iVar3,iVar2 + 0x4bf10,0x40,*(undefined4 *)(param_1 + 0xb0));
            if (-1 < iVar2) {
              iVar3 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0x40);
              iVar2 = -0x7ffd67ff;
              *(int *)(param_1 + 0x74) = iVar3;
              if ((iVar3 != 0) &&
                 (iVar2 = FUN_000195dc(iVar3,0x9d80c,0x80,*(undefined4 *)(param_1 + 0xb0)),
                 -1 < iVar2)) {
                iVar2 = *(int *)(param_2 + 0x20);
                puVar1 = (undefined1 *)0x1f40;
                if ((iVar2 != 7) &&
                   ((puVar1 = &LAB_00001770, iVar2 != 10 &&
                    (puVar1 = (undefined1 *)0xfa0, iVar2 != 0xf)))) {
                  puVar1 = (undefined1 *)0x7d0;
                }
                *(undefined1 **)(param_1 + 0xd4) = puVar1;
                iVar2 = FUN_000174c0(param_1,param_4 + 4,param_5 + 2);
                if (-1 < iVar2) {
                  iVar3 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),8);
                  iVar2 = -0x7ffd67ff;
                  *(int *)(param_1 + 0x84) = iVar3;
                  if (((iVar3 != 0) && (iVar2 = FUN_0002265c(iVar3), -1 < iVar2)) &&
                     (iVar2 = FUN_000225ac(*(undefined4 *)(param_1 + 0x84),param_3), -1 < iVar2)) {
                    iVar3 = FUN_000198e8(*(undefined4 *)(param_1 + 0xb0),0xc);
                    iVar2 = -0x7ffd67ff;
                    *(int *)(param_1 + 0x90) = iVar3;
                    if (iVar3 != 0) {
                      FUN_00019ec4(iVar3,FUN_0000ea60);
                      iVar2 = param_1 + 0xb4;
                      syscall();
                      if (-1 < iVar2) {
                        iVar2 = param_1 + 0xb8;
                        syscall();
                        if (-1 < iVar2) {
                          iVar2 = *(int *)(param_1 + 0xb8);
                          syscall();
                          if (-1 < iVar2) {
                            iVar2 = *(int *)(param_1 + 0xb8);
                            syscall();
                            if ((-1 < iVar2) &&
                               (iVar2 = FUN_000298a0(param_1 + 0x20,&PTR_LAB_0002eb70,param_1,
                                                     param_4 + 4,0x1000,1,"cellPremoM4hdEncThread"),
                               -1 < iVar2)) {
                              syscall();
                              FUN_000224d4(*(undefined4 *)(param_1 + 0x70),1);
                              return iVar2;
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    FUN_000172dc(param_1);
  }
  return iVar2;
}



int FUN_00017b34(void)

{
  int iVar1;
  
  iVar1 = FUN_00020f2c(0x7a);
  return iVar1 + 0x4712c6;
}



undefined4 FUN_00017b70(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  undefined4 uVar2;
  
  if (*(int *)(param_1 + 0x20) == 0x72) {
    iVar1 = FUN_00023634("/dev_flash/sys/external/libaacenc_spurs.sprx",param_4);
    *(int *)(param_1 + 0xe8) = iVar1;
    uVar2 = 0x80029801;
    if (iVar1 != 0) {
      iVar1 = FUN_00019910(*(undefined4 *)(param_1 + 0xec),0x74);
      *(int *)(param_1 + 0x30) = iVar1;
      uVar2 = 0x80029801;
      if (iVar1 != 0) {
        uVar2 = FUN_00018974(iVar1,*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0xa0),
                             param_3,*(undefined4 *)(param_1 + 0x24),*(undefined4 *)(param_1 + 0x28)
                             ,*(undefined4 *)(param_1 + 0x2c),param_4);
      }
    }
  }
  else {
    uVar2 = 0;
    if (*(int *)(param_1 + 0x20) != 0x10ff) {
      iVar1 = FUN_00023634("/dev_flash/sys/internal/libat3enc_spurs.sprx",param_4);
      *(int *)(param_1 + 0xe8) = iVar1;
      uVar2 = 0x80029801;
      if (iVar1 != 0) {
        iVar1 = FUN_00019910(*(undefined4 *)(param_1 + 0xec),0x1c);
        *(int *)(param_1 + 0x34) = iVar1;
        uVar2 = 0x80029801;
        if (iVar1 != 0) {
          uVar2 = FUN_00018cdc(iVar1,*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0xa0)
                               ,param_2,param_3,*(undefined4 *)(param_1 + 0x24),
                               *(undefined4 *)(param_1 + 0x28),*(undefined4 *)(param_1 + 0x2c));
        }
      }
    }
  }
  return uVar2;
}



void FUN_00017cd4(int param_1,undefined8 param_2,int param_3)

{
  byte bVar1;
  
  bVar1 = *(byte *)(param_3 + 0x188);
  *(uint *)(param_1 + 0x20) = (uint)bVar1;
  FUN_000212e0(*(undefined4 *)(param_1 + 0x80),(-(bVar1 ^ 0x71) >> 0x1d & 4) + 0x7c,
               *(undefined4 *)(param_1 + 0x9c));
  FUN_00017b70(param_1,*(int *)(param_1 + 0xdc) + 3,*(int *)(param_1 + 0xe0) + 1,
               *(undefined4 *)(param_1 + 0xd8));
  return;
}



void FUN_00017d44(int param_1)

{
  if (*(int *)(param_1 + 0x30) == 0) {
    if (*(int *)(param_1 + 0x34) != 0) {
      FUN_00018b88(*(int *)(param_1 + 0x34));
      FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0x34));
      *(undefined4 *)(param_1 + 0x34) = 0;
    }
  }
  else {
    FUN_000188d0(*(int *)(param_1 + 0x30));
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0x30));
    *(undefined4 *)(param_1 + 0x30) = 0;
  }
  if (*(int *)(param_1 + 0xe8) != 0) {
    FUN_000235f4(*(int *)(param_1 + 0xe8));
    *(undefined4 *)(param_1 + 0xe8) = 0;
  }
  return;
}



void FUN_00017de0(int param_1,int param_2)

{
  if (*(int *)(param_1 + 0x2c) != param_2) {
    *(int *)(param_1 + 0xd4) = param_2;
    FUN_000224d4(*(undefined4 *)(param_1 + 0x8c),2);
  }
  return;
}



void FUN_00017e20(int param_1,int param_2)

{
  *(bool *)(param_1 + 0xf0) = *(short *)(param_2 + 0x20) != 0;
  FUN_00017de0(param_1,*(undefined4 *)(param_2 + 0x18));
  return;
}



void FUN_00017e44(int param_1,int param_2,undefined4 param_3,undefined4 param_4,undefined4 param_5)

{
  if (param_2 == 0) {
    *(byte *)(param_1 + 0xa9) = *(byte *)(param_1 + 0xa9) & 0xfe;
  }
  else {
    *(byte *)(param_1 + 0xa9) = *(byte *)(param_1 + 0xa9) | 1;
  }
  *(undefined4 *)(param_1 + 0xac) = param_3;
  FUN_00029b08(param_1 + 0xc4,param_5,0x10);
  FUN_00029b08(param_1 + 0xb4,param_4,0x10);
  return;
}



int FUN_00017ecc(int param_1)

{
  int iVar1;
  undefined1 auStack_20 [16];
  
  iVar1 = FUN_00019230(*(undefined4 *)(param_1 + 0x84),auStack_20);
  if (-1 < iVar1) {
    FUN_00019380(*(undefined4 *)(param_1 + 0x84),0);
  }
  return iVar1;
}



int FUN_00017f24(int *param_1,undefined4 param_2,undefined4 param_3,int param_4)

{
  int iVar1;
  undefined4 local_40 [4];
  
  iVar1 = FUN_00019230(param_1[0x21],local_40);
  if (-1 < iVar1) {
    if ((param_1[5] & 2U) == 0) {
      FUN_00019fa0(param_1 + 0xe,param_4 + -0x780,0);
      param_1[5] = param_1[5] | 2;
    }
    FUN_00019fa0(param_1 + 0xe,param_4,param_3);
    FUN_00029b08(local_40[0],param_2,param_1[9] << 0xc);
    FUN_00019380(param_1[0x21],0x2000);
    *param_1 = *param_1 + 1;
  }
  return iVar1;
}



uint FUN_00018014(int param_1)

{
  int iVar1;
  
  iVar1 = FUN_000191e0(*(undefined4 *)(param_1 + 0x84));
  return (uint)((iVar1 >> 0x1f) - iVar1) >> 0x1f;
}



void FUN_00018048(int param_1)

{
  if ((*(uint *)(param_1 + 0x14) & 1) != 0) {
    FUN_0001a13c(param_1 + 0x38);
  }
  if (*(int *)(param_1 + 0x8c) != 0) {
    FUN_00022324(*(int *)(param_1 + 0x8c));
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0x8c));
    *(undefined4 *)(param_1 + 0x8c) = 0;
  }
  if (*(int *)(param_1 + 0x80) != 0) {
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(int *)(param_1 + 0x80));
    *(undefined4 *)(param_1 + 0x80) = 0;
  }
  if (*(int *)(param_1 + 0x84) != 0) {
    FUN_00019464(*(int *)(param_1 + 0x84));
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0x84));
    *(undefined4 *)(param_1 + 0x84) = 0;
  }
  if (*(int *)(param_1 + 0x88) != 0) {
    FUN_00019464(*(int *)(param_1 + 0x88));
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0x88));
    *(undefined4 *)(param_1 + 0x88) = 0;
  }
  if (*(int *)(param_1 + 0x98) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x98));
    FUN_00022588(*(undefined4 *)(param_1 + 0x98));
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(undefined4 *)(param_1 + 0x98));
    *(undefined4 *)(param_1 + 0x98) = 0;
  }
  if (*(int *)(param_1 + 0xa4) != 0) {
    FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(int *)(param_1 + 0xa4));
    *(undefined4 *)(param_1 + 0xa4) = 0;
  }
  return;
}



undefined8 FUN_00018174(int param_1)

{
  undefined1 auStack_20 [16];
  
  FUN_000224d4(*(undefined4 *)(param_1 + 0x8c),0x100);
  FUN_00019330(*(undefined4 *)(param_1 + 0x84));
  FUN_00019330(*(undefined4 *)(param_1 + 0x88));
  syscall();
  if (*(int *)(param_1 + 0x20) == 0x72) {
    FUN_000188d0(*(undefined4 *)(param_1 + 0x30),auStack_20);
    if (*(int *)(param_1 + 0x30) != 0) {
      FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(int *)(param_1 + 0x30));
      *(undefined4 *)(param_1 + 0x30) = 0;
    }
  }
  else if (*(int *)(param_1 + 0x20) != 0x10ff) {
    FUN_00018b88(*(undefined4 *)(param_1 + 0x34));
    if (*(int *)(param_1 + 0x34) != 0) {
      FUN_00019874(*(undefined4 *)(param_1 + 0xec),*(int *)(param_1 + 0x34));
      *(undefined4 *)(param_1 + 0x34) = 0;
    }
  }
  if (*(int *)(param_1 + 0xe8) != 0) {
    FUN_000235f4(*(int *)(param_1 + 0xe8));
    *(undefined4 *)(param_1 + 0xe8) = 0;
  }
  FUN_00018048(param_1);
  return 0;
}



int FUN_0001826c(int param_1,undefined4 param_2,undefined4 param_3,int param_4,int param_5,
                undefined4 param_6,undefined4 param_7,undefined4 param_8)

{
  uint uVar1;
  uint uVar2;
  int iVar3;
  int iVar4;
  undefined4 in_stack_00000074;
  undefined4 in_stack_0000007c;
  undefined4 in_stack_00000084;
  
  FUN_00029ad0(param_1,0,0xf8);
  FUN_00029b08(param_1 + 0x20,param_2,0x10);
  *(undefined4 *)(param_1 + 0xa0) = param_7;
  *(undefined4 *)(param_1 + 0x90) = param_8;
  *(int *)(param_1 + 0xdc) = param_4;
  *(int *)(param_1 + 0xe0) = param_5;
  *(undefined4 *)(param_1 + 0xe4) = in_stack_0000007c;
  *(undefined4 *)(param_1 + 0x9c) = param_6;
  *(undefined4 *)(param_1 + 0xec) = in_stack_00000074;
  *(undefined4 *)(param_1 + 0xd8) = in_stack_00000084;
  iVar3 = FUN_00019910(in_stack_00000074,0x30);
  *(int *)(param_1 + 0x8c) = iVar3;
  if (iVar3 == 0) {
    iVar3 = -0x7ffd67ff;
  }
  else {
    iVar3 = FUN_0002237c(iVar3);
    if (-1 < iVar3) {
      iVar4 = FUN_00019910(*(undefined4 *)(param_1 + 0xec),0xc);
      iVar3 = -0x7ffd67ff;
      *(int *)(param_1 + 0x80) = iVar4;
      if (iVar4 != 0) {
        uVar2 = *(uint *)(param_1 + 0x20) ^ 0x71;
        uVar1 = (int)uVar2 >> 0x1f;
        iVar3 = -0x7ffd67ff;
        FUN_000212e0(iVar4,(uVar1 - (uVar1 ^ uVar2) >> 0x1d & 4) + 0x7c,param_6);
        iVar4 = FUN_00019910(*(undefined4 *)(param_1 + 0xec),0x40);
        *(int *)(param_1 + 0x84) = iVar4;
        if ((iVar4 != 0) &&
           (iVar3 = FUN_0001968c(iVar4,0x2000,0x40,*(undefined4 *)(param_1 + 0xec)), -1 < iVar3)) {
          iVar4 = FUN_00019910(*(undefined4 *)(param_1 + 0xec),0x40);
          iVar3 = -0x7ffd67ff;
          *(int *)(param_1 + 0x88) = iVar4;
          if (iVar4 != 0) {
            iVar3 = FUN_00021404(*(undefined4 *)(param_1 + 0x80));
            iVar3 = FUN_0001968c(iVar4,iVar3 + 0x610,0x40,*(undefined4 *)(param_1 + 0xec));
            if ((-1 < iVar3) && (iVar3 = FUN_0001a160(param_1 + 0x38), -1 < iVar3)) {
              *(uint *)(param_1 + 0x14) = *(uint *)(param_1 + 0x14) | 1;
              iVar3 = FUN_00017b70(param_1,param_4 + 3,param_5 + 1,in_stack_00000084);
              if (-1 < iVar3) {
                iVar4 = FUN_00019910(*(undefined4 *)(param_1 + 0xec),8);
                iVar3 = -0x7ffd67ff;
                *(int *)(param_1 + 0x98) = iVar4;
                if (((iVar4 != 0) && (iVar3 = FUN_0002265c(iVar4), -1 < iVar3)) &&
                   (iVar3 = FUN_000225ac(*(undefined4 *)(param_1 + 0x98),param_3), -1 < iVar3)) {
                  iVar4 = FUN_000198c0(*(undefined4 *)(param_1 + 0xec),0x80,0x600);
                  iVar3 = -0x7ffd67ff;
                  *(int *)(param_1 + 0xa4) = iVar4;
                  if (iVar4 != 0) {
                    iVar3 = FUN_000298a0(param_1 + 0x18,&PTR_LAB_0002eb78,param_1,param_4 + 3,0x1000
                                         ,1,"cellPremoAEncThread");
                    FUN_000224d4(*(undefined4 *)(param_1 + 0x8c),1);
                    return iVar3;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  FUN_00018048(param_1);
  return iVar3;
}



int FUN_00018580(int param_1)

{
  int iVar1;
  int iVar2;
  undefined1 local_40 [8];
  undefined1 local_38 [8];
  undefined1 local_30 [24];
  
  iVar1 = FUN_00022614(*(undefined4 *)(param_1 + 0x98),local_40,local_38,local_30);
  iVar2 = FUN_000224d4(*(undefined4 *)(param_1 + 0x8c),4);
  if (-1 < iVar1) {
    iVar1 = iVar2;
  }
  return iVar1;
}



int FUN_000185f8(void)

{
  int iVar1;
  uint uVar2;
  uint uVar3;
  undefined1 local_20 [16];
  
  iVar1 = FUN_00021404(local_20);
  uVar2 = FUN_000188c4();
  uVar3 = FUN_00018b7c();
  if (uVar3 < uVar2) {
    uVar3 = uVar2;
  }
  return iVar1 + 0x2dd4 + uVar3;
}



int FUN_00018658(uint *param_1,float *param_2)

{
  float fVar1;
  float fVar2;
  uint uVar3;
  uint uVar4;
  int iVar5;
  float *pfVar6;
  int iVar7;
  float *pfVar8;
  ulonglong uVar9;
  
  uVar3 = *param_1;
  uVar4 = param_1[1];
  iVar5 = FUN_000297f8(param_1 + 6,0);
  if (-1 < iVar5) {
    fVar1 = (float)param_1[3];
    iVar5 = uVar3 * uVar4;
    if (param_1[2] == 0) {
      if (fVar1 != 1.0) {
        uVar9 = (ulonglong)(iVar5 + 1);
        iVar7 = 0;
        if (iVar5 < 0) {
          uVar9 = 1;
        }
        while( true ) {
          pfVar6 = (float *)(iVar7 + (int)param_2);
          iVar7 = iVar7 + 4;
          uVar9 = uVar9 - 1;
          if (uVar9 == 0) break;
          *pfVar6 = *pfVar6 * fVar1;
        }
      }
    }
    else {
      uVar3 = param_1[1];
      fVar2 = (float)param_1[4];
      uVar4 = *param_1;
      fVar1 = fVar1 - fVar2;
      pfVar6 = param_2;
      for (iVar7 = 0; iVar7 < (int)uVar3; iVar7 = iVar7 + 1) {
        uVar9 = (ulonglong)uVar4 + 1 & 0xffffffff;
        pfVar8 = pfVar6;
        if ((int)uVar4 < 0) {
          uVar9 = 1;
        }
        while( true ) {
          uVar9 = uVar9 - 1;
          if (uVar9 == 0) break;
          *pfVar8 = *pfVar8 * fVar2;
          pfVar8 = pfVar8 + 1;
        }
        fVar2 = fVar2 + fVar1 / (float)(longlong)(int)uVar3;
        pfVar6 = pfVar6 + uVar4;
      }
      param_1[2] = 0;
    }
    uVar9 = (ulonglong)(iVar5 + 1);
    iVar7 = 0;
    if (iVar5 < 0) {
      uVar9 = 1;
    }
    while( true ) {
      pfVar6 = (float *)(iVar7 + (int)param_2);
      uVar9 = uVar9 - 1;
      if (uVar9 == 0) break;
      if (*pfVar6 <= 1.0) {
        if (*pfVar6 < -1.0) {
          *pfVar6 = -1.0;
        }
      }
      else {
        *pfVar6 = 1.0;
      }
      iVar7 = iVar7 + 4;
    }
    iVar5 = FUN_00029830(param_1 + 6);
  }
  return iVar5;
}



undefined4 FUN_00018830(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029d00(param_1 + 0x18);
  return uVar1;
}



undefined4 FUN_0001885c(undefined4 *param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  undefined1 local_10 [16];
  
  *param_1 = param_2;
  param_1[1] = param_3;
  param_1[2] = 0;
  param_1[4] = 0x3f800000;
  param_1[3] = 0x3f800000;
  uVar1 = FUN_00029948(param_1 + 6,local_10);
  return uVar1;
}



undefined8 FUN_000188c4(void)

{
  return 0xb9610;
}



void FUN_000188d0(int param_1)

{
  if (*(int *)(param_1 + 0x6c) != 0) {
    FUN_00028870();
  }
  if (*(int *)(param_1 + 0x68) != 0) {
    FUN_00019874(*(undefined4 *)(param_1 + 0x70),*(int *)(param_1 + 0x68));
  }
  return;
}



undefined4 FUN_00018920(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00028838();
  return uVar1;
}



undefined4
FUN_00018948(undefined8 param_1,undefined8 param_2,undefined4 param_3,undefined8 param_4,
            undefined8 param_5)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000287c8(param_1,param_2,param_3,param_4,param_5,0);
  return uVar1;
}



int FUN_00018974(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4,int param_5,
                undefined4 param_6,undefined4 param_7)

{
  int iVar1;
  int iVar2;
  undefined1 *puVar3;
  longlong lVar4;
  undefined4 local_f0;
  undefined1 auStack_ec [12];
  undefined1 auStack_e0 [8];
  undefined8 local_d8;
  undefined8 local_d0 [3];
  int local_b8;
  undefined4 local_b4;
  undefined4 local_b0;
  undefined4 local_ac;
  undefined4 local_a8;
  undefined4 local_a4;
  undefined4 local_a0;
  undefined4 local_9c;
  undefined8 *local_98;
  undefined1 *local_94;
  undefined4 local_90;
  undefined4 local_8c;
  undefined4 local_7c;
  undefined4 local_78;
  undefined4 local_74;
  undefined4 local_70;
  undefined4 local_6c;
  undefined4 local_68;
  undefined4 local_64;
  
  local_d8 = 0x100000000;
  local_d0[1] = 0;
  local_d0[2] = 1;
  local_d0[0] = 0x200000002;
  *(undefined4 *)(param_1 + 0x68) = 0;
  *(undefined4 *)(param_1 + 0x70) = param_2;
  *(undefined4 *)(param_1 + 0x6c) = 0;
  iVar1 = 0;
  if (param_5 < 3) {
    iVar1 = 0;
    lVar4 = 0x80;
    do {
      puVar3 = (undefined1 *)((int)&local_b8 + iVar1);
      iVar1 = iVar1 + 1;
      *puVar3 = 0;
      lVar4 = lVar4 + -1;
    } while (lVar4 != 0);
    local_94 = auStack_e0 + param_5 * 8;
    local_98 = local_d0 + param_5;
    local_ac = 0;
    local_6c = 2;
    local_68 = 1;
    local_64 = 0;
    local_a8 = 0;
    local_a4 = 1;
    local_a0 = 0;
    local_9c = 1;
    local_90 = 0;
    local_8c = 0;
    local_7c = 0;
    local_78 = 0;
    local_74 = 0;
    local_70 = 1;
    local_b8 = param_5;
    local_b4 = param_6;
    local_b0 = param_7;
    iVar1 = FUN_00028800(&local_f0);
    if (-1 < iVar1) {
      iVar1 = -0x7ffd67ff;
      iVar2 = FUN_00019910(*(undefined4 *)(param_1 + 0x70),local_f0);
      *(int *)(param_1 + 0x68) = iVar2;
      if (iVar2 != 0) {
        iVar1 = 0;
        lVar4 = 8;
        do {
          puVar3 = auStack_e0 + iVar1;
          iVar1 = iVar1 + 1;
          *puVar3 = 1;
          lVar4 = lVar4 + -1;
        } while (lVar4 != 0);
        iVar1 = FUN_000288a8(param_1,param_4,*(undefined4 *)(param_1 + 0x68),param_3,auStack_e0,1);
        if (-1 < iVar1) {
          *(undefined4 *)(param_1 + 0x6c) = 1;
          iVar1 = FUN_00028758(param_1,&local_b8);
          if ((-1 < iVar1) && (iVar1 = FUN_00028790(param_1,3,auStack_ec), -1 < iVar1)) {
            return iVar1;
          }
        }
      }
    }
  }
  FUN_000188d0(param_1);
  return iVar1;
}



undefined8 FUN_00018b7c(void)

{
  return 0x38958;
}



void FUN_00018b88(int *param_1)

{
  if (param_1[2] != 0) {
    FUN_00029018(param_1[1]);
  }
  if (*param_1 != 0) {
    FUN_00019874(param_1[6],*param_1);
    *param_1 = 0;
  }
  if (param_1[1] != 0) {
    FUN_00019874(param_1[6],param_1[1]);
    param_1[1] = 0;
  }
  return;
}



undefined4 FUN_00018c04(int param_1,undefined8 param_2,undefined4 *param_3)

{
  undefined4 uVar1;
  undefined4 uVar2;
  int local_20 [4];
  
  uVar2 = FUN_00029050(0,param_2,0,local_20,*(undefined4 *)(param_1 + 4));
  if (local_20[0] == 0) {
    uVar1 = *(undefined4 *)(param_1 + 0x14);
  }
  else {
    uVar1 = 0;
  }
  *param_3 = uVar1;
  return uVar2;
}



undefined4
FUN_00018c6c(int param_1,undefined8 param_2,undefined8 param_3,undefined8 param_4,
            undefined4 *param_5)

{
  undefined4 uVar1;
  undefined1 auStack_20 [16];
  
  uVar1 = FUN_00029050(param_2,param_4,param_3,auStack_20,*(undefined4 *)(param_1 + 4));
  if (*(int *)(param_1 + 0xc) == 0) {
    *param_5 = 0;
    *(undefined4 *)(param_1 + 0xc) = 1;
  }
  else {
    *param_5 = *(undefined4 *)(param_1 + 0x14);
  }
  return uVar1;
}



int FUN_00018cdc(int *param_1,int param_2,int param_3,undefined4 param_4,undefined4 param_5,
                int param_6,undefined8 param_7,undefined1 *param_8)

{
  uint uVar1;
  int iVar2;
  int iVar3;
  longlong lVar4;
  undefined4 local_50 [4];
  
  uVar1 = (int)((uint)param_8 ^ 0x1bd50) >> 0x1f;
  param_1[6] = param_2;
  param_1[3] = 0;
  param_1[4] = param_6;
  param_1[2] = 0;
  *param_1 = 0;
  iVar2 = ((int)(uVar1 - (uVar1 ^ (uint)param_8 ^ 0x1bd50)) >> 0x1f & 0x50U) + 0x130;
  if (param_8 == &LAB_00011940) {
    iVar2 = 0xc0;
  }
  param_1[5] = iVar2;
  FUN_00028fa8(local_50);
  iVar2 = FUN_000198c0(param_1[6],0x80,local_50[0]);
  iVar3 = -0x7ffd67ff;
  *param_1 = iVar2;
  if (iVar2 != 0) {
    iVar2 = FUN_00019910(param_1[6],0x80);
    iVar3 = -0x7ffd67ff;
    param_1[1] = iVar2;
    if (iVar2 != 0) {
      *(bool *)(iVar2 + 0x24) = param_3 == 0;
      iVar2 = param_1[1];
      lVar4 = 8;
      *(int *)(iVar2 + 0x28) = param_3;
      *(undefined4 *)(iVar2 + 0x58) = 1;
      iVar2 = 0;
      do {
        *(undefined1 *)(param_1[1] + iVar2 + 0x50) = 1;
        lVar4 = lVar4 + -1;
        iVar2 = iVar2 + 1;
      } while (lVar4 != 0);
      iVar2 = param_1[1];
      iVar3 = *param_1;
      *(undefined4 *)(iVar2 + 0x48) = param_4;
      *(undefined4 *)(iVar2 + 0x4c) = param_5;
      iVar3 = FUN_00028fe0(iVar3,param_5,iVar2);
      if (-1 < iVar3) {
        uVar1 = (int)((uint)param_8 ^ 72000) >> 0x1f;
        param_1[2] = 1;
        iVar3 = FUN_00029088(param_6,((uVar1 ^ (uint)param_8 ^ 72000) - uVar1) - 1 >> 0x1f,
                             param_1[5],0x84,param_1[1]);
        if (-1 < iVar3) {
          return iVar3;
        }
      }
    }
  }
  FUN_00018b88(param_1);
  return iVar3;
}



undefined4 FUN_00018ecc(int param_1)

{
  undefined4 uVar1;
  
  FUN_0000b5ec();
  uVar1 = FUN_00021610(*(undefined4 *)(param_1 + 8));
  syscall();
  return uVar1;
}



int FUN_00018f1c(int param_1,int param_2)

{
  int iVar1;
  int iVar2;
  int iVar3;
  
  syscall();
  iVar3 = 0;
  iVar1 = FUN_00021610(*(undefined4 *)(param_1 + 8),0);
  do {
    if (iVar1 <= iVar3) {
      iVar3 = -1;
LAB_00018fa0:
      syscall();
      return iVar3;
    }
    iVar2 = FUN_00021664(*(undefined4 *)(param_1 + 8),iVar3);
    if (iVar2 == param_2) {
      FUN_00021928(*(undefined4 *)(param_1 + 8),iVar3);
      goto LAB_00018fa0;
    }
    iVar3 = iVar3 + 1;
  } while( true );
}



undefined8 FUN_00018fd4(int param_1)

{
  if (*(int *)(param_1 + 8) != 0) {
    FUN_000217e0(*(int *)(param_1 + 8));
    FUN_00019874(*(undefined4 *)(param_1 + 0xc),*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0;
  }
  syscall();
  return 0;
}



undefined4 * FUN_00019034(undefined4 *param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  undefined4 *puVar2;
  
  FUN_00029ad0(param_1,0,0x10);
  puVar2 = param_1 + 1;
  *param_1 = param_3;
  param_1[3] = param_2;
  syscall();
  if (puVar2 == (undefined4 *)0x0) {
    iVar1 = FUN_00019910(param_1[3],0x14);
    puVar2 = (undefined4 *)0x80029801;
    param_1[2] = iVar1;
    if ((iVar1 != 0) && (puVar2 = (undefined4 *)FUN_00021834(iVar1,param_3), -1 < (int)puVar2)) {
      return puVar2;
    }
  }
  FUN_00018fd4(param_1);
  return puVar2;
}



undefined4 FUN_00019130(int param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  FUN_0000b5ec();
  uVar1 = FUN_00021738(*(undefined4 *)(param_1 + 8),param_2);
  syscall();
  return uVar1;
}



undefined4 FUN_00019188(int param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  FUN_0000b5ec();
  uVar1 = FUN_00021664(*(undefined4 *)(param_1 + 8),param_2);
  syscall();
  return uVar1;
}



int FUN_000191e0(int param_1)

{
  return 1 - *(int *)(param_1 + 0x2c);
}



undefined4 FUN_000191f0(int param_1)

{
  return *(undefined4 *)(param_1 + 0x2c);
}



undefined4 FUN_000191fc(int param_1)

{
  return *(undefined4 *)(param_1 + 0x34);
}



undefined4 FUN_00019208(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1,0);
  return uVar1;
}



undefined8 FUN_00019230(int param_1,undefined4 *param_2)

{
  FUN_00019208();
  *param_2 = *(undefined4 *)(param_1 + 0x28);
  return 0;
}



undefined4 FUN_00019274(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830();
  return uVar1;
}



undefined4 thunk_FUN_00019274(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830();
  return uVar1;
}



undefined4 FUN_0001929c(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029da8(param_1 + 0x20);
  return uVar1;
}



void FUN_000192c8(int param_1)

{
  *(undefined4 *)(param_1 + 0x34) = 0;
  *(undefined4 *)(param_1 + 0x2c) = 0;
  FUN_0001929c();
  FUN_00019274(param_1);
  return;
}



undefined4 FUN_00019304(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029da8(param_1 + 0x18);
  return uVar1;
}



undefined8 FUN_00019330(int param_1)

{
  FUN_00019208();
  *(undefined4 *)(param_1 + 0x38) = 1;
  FUN_00019274(param_1);
  FUN_00019304(param_1);
  FUN_0001929c(param_1);
  return 0;
}



void FUN_00019380(int param_1,undefined4 param_2)

{
  *(undefined4 *)(param_1 + 0x34) = param_2;
  *(undefined4 *)(param_1 + 0x2c) = 1;
  FUN_00019304();
  FUN_00019274(param_1);
  return;
}



void FUN_000193bc(int param_1)

{
  FUN_00029868(param_1 + 0x18);
  FUN_00029868(param_1 + 0x20);
  FUN_00029d00(param_1);
  return;
}



undefined8 FUN_00019404(int param_1)

{
  if (*(int *)(param_1 + 0x28) != 0) {
    FUN_00019850(*(undefined4 *)(param_1 + 0x3c),*(int *)(param_1 + 0x28));
    *(undefined4 *)(param_1 + 0x28) = 0;
  }
  FUN_000193bc(param_1);
  return 0;
}



undefined8 FUN_00019464(int param_1)

{
  if (*(int *)(param_1 + 0x28) != 0) {
    FUN_00019874(*(undefined4 *)(param_1 + 0x3c),*(int *)(param_1 + 0x28));
    *(undefined4 *)(param_1 + 0x28) = 0;
  }
  FUN_000193bc(param_1);
  return 0;
}



int FUN_000194c4(int param_1)

{
  int iVar1;
  undefined1 local_40 [8];
  undefined1 local_38 [8];
  undefined1 local_30 [16];
  
  iVar1 = FUN_00029948(param_1,local_30);
  if (-1 < iVar1) {
    iVar1 = FUN_00029d70(param_1 + 0x18,param_1,local_40);
    if (-1 < iVar1) {
      iVar1 = FUN_00029d70(param_1 + 0x20,param_1,local_38);
    }
  }
  return iVar1;
}



int FUN_000195dc(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  int iVar2;
  
  FUN_00029ad0(param_1,0,0x40);
  *(undefined4 *)(param_1 + 0x3c) = param_4;
  *(undefined4 *)(param_1 + 0x30) = param_2;
  iVar1 = FUN_000194c4(param_1);
  if (-1 < iVar1) {
    iVar2 = FUN_00019898(*(undefined4 *)(param_1 + 0x3c),param_3,param_2);
    *(int *)(param_1 + 0x28) = iVar2;
    if (iVar2 != 0) {
      return iVar1;
    }
    iVar1 = -0x7ffd67ff;
  }
  FUN_00019404(param_1);
  return iVar1;
}



int FUN_0001968c(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  int iVar1;
  int iVar2;
  
  FUN_00029ad0(param_1,0,0x40);
  *(undefined4 *)(param_1 + 0x3c) = param_4;
  *(undefined4 *)(param_1 + 0x30) = param_2;
  iVar1 = FUN_000194c4(param_1);
  if (-1 < iVar1) {
    iVar2 = FUN_000198c0(*(undefined4 *)(param_1 + 0x3c),param_3,param_2);
    *(int *)(param_1 + 0x28) = iVar2;
    if (iVar2 != 0) {
      return iVar1;
    }
    iVar1 = -0x7ffd67ff;
  }
  FUN_00019464(param_1);
  return iVar1;
}



int FUN_0001973c(int param_1)

{
  uint uVar1;
  
  FUN_00019208(param_1);
  while( true ) {
    if ((0 < *(int *)(param_1 + 0x2c)) || (*(int *)(param_1 + 0x38) != 0)) break;
    FUN_000298d8(param_1 + 0x18,0);
  }
  FUN_00019274(param_1);
  uVar1 = (int)*(uint *)(param_1 + 0x38) >> 0x1f;
  return ((int)(((uVar1 ^ *(uint *)(param_1 + 0x38)) - uVar1) + -1) >> 0x1f & 0x7ffd67faU) +
         0x80029806;
}



undefined4 FUN_000197e0(int param_1,undefined4 *param_2,undefined4 *param_3)

{
  undefined4 uVar1;
  undefined4 uVar2;
  
  uVar2 = 0x80029803;
  if (*(int *)(param_1 + 0x2c) != 0) {
    FUN_00019208();
    uVar1 = *(undefined4 *)(param_1 + 0x34);
    *param_2 = *(undefined4 *)(param_1 + 0x28);
    uVar2 = 0;
    *param_3 = uVar1;
  }
  return uVar2;
}



void FUN_00019850(int param_1)

{
  FUN_00029b78(*(undefined4 *)(param_1 + 8));
  return;
}



void FUN_00019874(undefined4 *param_1)

{
  FUN_00029b78(*param_1);
  return;
}



undefined4 FUN_00019898(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029a28(*(undefined4 *)(param_1 + 8));
  return uVar1;
}



undefined4 FUN_000198c0(undefined4 *param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029a28(*param_1);
  return uVar1;
}



undefined4 FUN_000198e8(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029980(*(undefined4 *)(param_1 + 8));
  return uVar1;
}



undefined4 FUN_00019910(undefined4 *param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029980(*param_1);
  return uVar1;
}



undefined4 FUN_00019938(int *param_1)

{
  int iVar1;
  undefined4 uVar2;
  
  if (param_1[2] != 0) {
    FUN_00029c20(param_1[2]);
  }
  iVar1 = *param_1;
  uVar2 = 0;
  if (iVar1 != 0) {
    FUN_00029b78(iVar1,param_1);
    uVar2 = FUN_00029c20(iVar1);
  }
  return uVar2;
}



void FUN_000199b0(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [24];
  
  if (*(int *)(param_1 + 8) != 0) {
    FUN_00029c20(*(int *)(param_1 + 8));
    uVar1 = FUN_00029c90("_cellPremoGroupHeap2",*(undefined4 *)(param_1 + 0x10),0xffffffff80000000,
                         local_20);
    *(undefined4 *)(param_1 + 8) = uVar1;
  }
  return;
}



int * FUN_00019a18(int param_1,int param_2,int param_3)

{
  int iVar1;
  int iVar2;
  int *piVar3;
  undefined1 local_50 [24];
  
  if (param_1 != 0) {
    iVar1 = FUN_00029c90("_cellPremoGroupHeap",param_2,0xffffffff80000000,local_50);
    if ((iVar1 != 0) &&
       (iVar2 = FUN_00029c90("_cellPremoGroupHeap2",param_3,0xffffffff80000000,local_50), iVar2 != 0
       )) {
      piVar3 = (int *)FUN_00029980(iVar1,0x14);
      if (piVar3 != (int *)0x0) {
        *piVar3 = iVar1;
        piVar3[2] = iVar2;
        piVar3[4] = param_3;
        piVar3[1] = param_2;
        piVar3[3] = param_1 + param_2;
        return piVar3;
      }
      FUN_00029c20(iVar1);
      return (int *)0x0;
    }
  }
  return (int *)0x0;
}



undefined8 FUN_00019b38(undefined4 *param_1,undefined4 *param_2)

{
  undefined4 uVar1;
  
  uVar1 = *param_1;
  *param_2 = param_1[1];
  uVar1 = FUN_00029cc8(uVar1);
  param_2[1] = uVar1;
  return 0;
}



undefined8 FUN_00019b78(void)

{
  return 0x42f00;
}



undefined4 FUN_00019b84(int param_1)

{
  undefined4 uVar1;
  undefined1 local_60 [36];
  undefined1 local_3c [52];
  
  if (*(int *)(param_1 + 0xc) == 0) {
    uVar1 = FUN_00028a68(*(undefined4 *)(param_1 + 4),local_60);
  }
  else {
    uVar1 = FUN_000283d8(*(undefined4 *)(param_1 + 4),local_3c);
  }
  return uVar1;
}



undefined4 FUN_00019c44(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0;
  if (*(int *)(param_1 + 0x2c) != 0) {
    if (*(int *)(param_1 + 0xc) == 0) {
      uVar1 = FUN_00028cd0(*(undefined4 *)(param_1 + 4));
    }
    else {
      uVar1 = FUN_00028480(*(undefined4 *)(param_1 + 4));
    }
    FUN_00019850(*(undefined4 *)(param_1 + 0x28),*(undefined4 *)(param_1 + 0x2c));
    *(undefined4 *)(param_1 + 0x2c) = 0;
  }
  return uVar1;
}



int FUN_00019cc4(undefined4 *param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  undefined4 uVar1;
  int iVar2;
  undefined1 local_60 [24];
  undefined1 local_48 [32];
  
  FUN_00029ad0(param_1,0,0x30);
  param_1[10] = param_4;
  *param_1 = param_2;
  FUN_00029b08(param_1 + 3,param_3,0x1c);
  if (param_1[3] == 0) {
    uVar1 = FUN_00028bb8();
    iVar2 = FUN_00019898(param_1[10],0x80,uVar1);
    param_1[0xb] = iVar2;
    if (iVar2 == 0) {
      return -0x7ffd67ff;
    }
    iVar2 = FUN_00028a30(local_60,param_1 + 1);
  }
  else {
    uVar1 = FUN_000283a0();
    iVar2 = FUN_00019898(param_1[10],0x80,uVar1);
    param_1[0xb] = iVar2;
    if (iVar2 == 0) {
      return -0x7ffd67ff;
    }
    iVar2 = FUN_00028448(local_48,param_1 + 1);
  }
  if ((iVar2 < 0) && (param_1[0xb] != 0)) {
    if (param_1[3] == 0) {
      iVar2 = FUN_00028cd0(param_1[1]);
    }
    else {
      iVar2 = FUN_00028480(param_1[1]);
    }
    FUN_00019850(param_1[10],param_1[0xb]);
    param_1[0xb] = 0;
  }
  return iVar2;
}



void FUN_00019e94(undefined4 *param_1)

{
  param_1[1] = 0x780;
  *param_1 = 0;
  return;
}



void FUN_00019ea8(int *param_1)

{
  *param_1 = *param_1 + param_1[1];
  return;
}



undefined4 FUN_00019ebc(undefined4 *param_1)

{
  return *param_1;
}



void FUN_00019ec4(undefined4 *param_1,undefined4 param_2)

{
  param_1[2] = param_2;
  param_1[1] = 0;
  *param_1 = 0;
  return;
}



void FUN_00019ed8(int *param_1,int *param_2)

{
  int iVar1;
  
  iVar1 = param_2[1];
  *param_1 = *param_1 + *param_2;
  param_1[1] = param_1[1] + iVar1;
  while ((uint)param_1[2] <= (uint)param_1[1]) {
    param_1[1] = param_1[1] - param_1[2];
    *param_1 = *param_1 + 1;
  }
  return;
}



void FUN_00019f24(undefined8 param_1,undefined8 param_2)

{
  FUN_00029b08(param_2,param_1,0xc);
  return;
}



undefined4 FUN_00019f54(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1,0);
  return uVar1;
}



undefined4 FUN_00019f7c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830();
  return uVar1;
}



void FUN_00019fa0(int param_1,undefined4 param_2,undefined4 param_3)

{
  FUN_00019f54();
  if (3 < *(int *)(param_1 + 0x40)) {
    *(int *)(param_1 + 0x40) = *(int *)(param_1 + 0x40) + -1;
    *(uint *)(param_1 + 0x38) = *(int *)(param_1 + 0x38) + 1U & 3;
  }
  *(undefined4 *)(param_1 + *(int *)(param_1 + 0x3c) * 8 + 0x18) = param_2;
  *(undefined4 *)(param_1 + *(int *)(param_1 + 0x3c) * 8 + 0x1c) = param_3;
  *(int *)(param_1 + 0x40) = *(int *)(param_1 + 0x40) + 1;
  *(uint *)(param_1 + 0x3c) = *(int *)(param_1 + 0x3c) + 1U & 3;
  FUN_00019f7c(param_1);
  return;
}



undefined4 FUN_0001a064(int param_1,undefined4 *param_2,undefined4 *param_3)

{
  undefined4 uVar1;
  
  FUN_00019f54(param_1);
  uVar1 = 0xffffffff;
  if (*(int *)(param_1 + 0x40) != 0) {
    uVar1 = 0;
    *param_2 = *(undefined4 *)(param_1 + *(int *)(param_1 + 0x38) * 8 + 0x18);
    *param_3 = *(undefined4 *)(param_1 + *(int *)(param_1 + 0x38) * 8 + 0x1c);
    *(int *)(param_1 + 0x40) = *(int *)(param_1 + 0x40) + -1;
    *(uint *)(param_1 + 0x38) = *(int *)(param_1 + 0x38) + 1U & 3;
  }
  FUN_00019f7c(param_1);
  return uVar1;
}



undefined4 FUN_0001a13c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029d00();
  return uVar1;
}



int FUN_0001a160(int param_1)

{
  int iVar1;
  undefined1 local_20 [24];
  
  iVar1 = FUN_00029948(param_1,local_20);
  if (iVar1 == 0) {
    *(undefined4 *)(param_1 + 0x38) = 0;
    *(undefined4 *)(param_1 + 0x40) = 0;
    *(undefined4 *)(param_1 + 0x3c) = 0;
  }
  return iVar1;
}



void FUN_0001a1e8(int param_1)

{
  *(undefined4 *)(param_1 + 0x3c) = 0;
  return;
}



undefined4 FUN_0001a1f4(int param_1)

{
  return *(undefined4 *)(param_1 + 0x3c);
}



void FUN_0001a1fc(int param_1)

{
  FUN_0001db8c(*(undefined4 *)(param_1 + 0x30));
  return;
}



void FUN_0001a220(int param_1)

{
  FUN_0001db7c(*(undefined4 *)(param_1 + 0x30));
  return;
}



undefined4 FUN_0001a244(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001cc18(*(undefined4 *)(param_1 + 0x2c));
  return uVar1;
}



undefined4 FUN_0001a26c(int param_1,undefined8 param_2,undefined8 param_3)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001e234(*(undefined4 *)(param_1 + 0x34),param_2,param_3,param_1 + 0x50);
  return uVar1;
}



undefined4 FUN_0001a29c(int param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001bfe8(*(undefined4 *)(param_1 + 0x28),param_2,param_1 + 0x50);
  return uVar1;
}



void FUN_0001a2cc(int param_1,uint param_2,int param_3,int param_4,int param_5,undefined4 param_6,
                 undefined1 param_7,uint param_8)

{
  char cVar1;
  char *pcVar2;
  byte *pbVar3;
  byte *pbVar4;
  int iVar5;
  byte *pbVar6;
  byte *pbVar7;
  undefined1 uVar8;
  longlong lVar9;
  undefined1 in_stack_00000077;
  undefined1 local_70 [8];
  byte abStack_68 [24];
  
  if ((*(char *)(param_1 + 0x177) == '\0') || (*(char *)(param_1 + 0x4e) != '\0')) {
    if (param_8 < 2) {
      iVar5 = FUN_00029de0(param_1 + 0x48,param_6,6);
      if ((iVar5 == 0) || (iVar5 = FUN_00029de0(param_1 + 0x48,local_70,6), iVar5 == 0))
      goto LAB_0001a388;
    }
    else {
LAB_0001a388:
      if ((param_8 != 0) || (*(char *)(param_1 + 0x4f) != '\0')) goto LAB_0001a39c;
    }
    uVar8 = 1;
  }
  else {
LAB_0001a39c:
    uVar8 = 0;
  }
  if ((param_2 == 0) || (param_3 != 0)) {
    FUN_00029b08(abStack_68,param_4,0x10);
  }
  else if ((param_8 & 0xff) == 0) {
    iVar5 = 0;
    lVar9 = 0x10;
    pbVar7 = &DAT_0002d548;
    do {
      pbVar6 = (byte *)(iVar5 + param_4);
      pbVar3 = abStack_68 + iVar5;
      iVar5 = iVar5 + 1;
      *pbVar3 = *pbVar6 ^ *pbVar7;
      lVar9 = lVar9 + -1;
      pbVar7 = pbVar7 + 1;
    } while (lVar9 != 0);
  }
  else if ((param_8 & 0xff) == 1) {
    lVar9 = 0x10;
    iVar5 = 0;
    pbVar7 = &DAT_0002d578;
    pbVar6 = &DAT_0002d568;
    do {
      pcVar2 = (char *)(iVar5 + param_5);
      pbVar3 = (byte *)(iVar5 + param_4);
      cVar1 = (char)iVar5;
      pbVar4 = abStack_68 + iVar5;
      iVar5 = iVar5 + 1;
      *pbVar4 = ((cVar1 + ')' + *pcVar2 ^ *pbVar7 ^ *pbVar3) - cVar1) + 0xb7 ^ *pbVar6;
      lVar9 = lVar9 + -1;
      pbVar7 = pbVar7 + 1;
      pbVar6 = pbVar6 + 1;
    } while (lVar9 != 0);
  }
  else if ((param_8 & 0xff) == 2) {
    lVar9 = 0x10;
    iVar5 = 0;
    pbVar7 = &DAT_0002d598;
    pbVar6 = &DAT_0002d588;
    do {
      pcVar2 = (char *)(iVar5 + param_5);
      pbVar3 = (byte *)(iVar5 + param_4);
      cVar1 = (char)iVar5;
      pbVar4 = abStack_68 + iVar5;
      iVar5 = iVar5 + 1;
      *pbVar4 = ((cVar1 + '3' + *pcVar2 ^ *pbVar7 ^ *pbVar3) - cVar1) - 0x3f ^ *pbVar6;
      lVar9 = lVar9 + -1;
      pbVar7 = pbVar7 + 1;
      pbVar6 = pbVar6 + 1;
    } while (lVar9 != 0);
  }
  else {
    lVar9 = 0x10;
    iVar5 = 0;
    pbVar7 = &DAT_0002d5b8;
    pbVar6 = &DAT_0002d5a8;
    do {
      pcVar2 = (char *)(iVar5 + param_5);
      pbVar3 = (byte *)(iVar5 + param_4);
      cVar1 = (char)iVar5;
      pbVar4 = abStack_68 + iVar5;
      iVar5 = iVar5 + 1;
      *pbVar4 = ((cVar1 + '=' + *pcVar2 ^ *pbVar7 ^ *pbVar3) - cVar1) - 0x2f ^ *pbVar6;
      lVar9 = lVar9 + -1;
      pbVar7 = pbVar7 + 1;
      pbVar6 = pbVar6 + 1;
    } while (lVar9 != 0);
  }
  param_2 = param_2 & 0xff;
  FUN_0001b5b4(*(undefined4 *)(param_1 + 0x38),param_2,param_3,param_5,param_8 & 0xff,
               in_stack_00000077);
  FUN_0001c080(*(undefined4 *)(param_1 + 0x28),param_2,param_3,abStack_68,param_5);
  FUN_0001c0f4(*(undefined4 *)(param_1 + 0x28),uVar8);
  FUN_0001ccd4(*(undefined4 *)(param_1 + 0x2c),param_2,param_3,abStack_68,param_5,uVar8);
  FUN_0001e1cc(*(undefined4 *)(param_1 + 0x34),param_7,param_3,abStack_68,param_5,uVar8);
  FUN_0001db9c(*(undefined4 *)(param_1 + 0x30),param_7,param_3,abStack_68,param_5,uVar8);
  FUN_00029b08(param_1 + 0x50,param_6,6);
  return;
}



void FUN_0001a6d4(int param_1)

{
  FUN_0001e19c(*(undefined4 *)(param_1 + 0x34));
  return;
}



undefined4 FUN_0001a6f8(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000214c4(*(undefined4 *)(param_1 + 0x24));
  return uVar1;
}



void FUN_0001a720(int param_1,undefined4 *param_2)

{
  ushort uVar1;
  undefined4 uVar2;
  undefined8 uVar3;
  
  uVar3 = *(undefined8 *)(param_1 + 0x18);
  *param_2 = *(undefined4 *)(param_1 + 0x68);
  uVar2 = *(undefined4 *)(param_1 + 8);
  *(undefined8 *)(param_2 + 2) = uVar3;
  param_2[1] = uVar2;
  FUN_0001a6f8(param_1,param_2 + 4);
  FUN_00029b08(param_2 + 5,param_1 + 0x177,0x40);
  FUN_00029b08(param_2 + 0x15,param_1 + 0x58,0x10);
  FUN_00029b08(param_2 + 0x19,param_1 + 0x78,0x100);
  *(undefined1 *)(param_2 + 0x59) = *(undefined1 *)(param_1 + 0x1bc);
  *(undefined1 *)((int)param_2 + 0x173) = *(undefined1 *)(param_1 + 0x1de);
  uVar1 = *(ushort *)(param_1 + 0x1d2);
  param_2[0x5a] = (uint)*(ushort *)(param_1 + 0x1d0);
  param_2[0x5b] = (uint)uVar1;
  *(undefined1 *)(param_2 + 0x5c) = *(undefined1 *)(param_1 + 0x1dc);
  *(undefined1 *)((int)param_2 + 0x171) = *(undefined1 *)(param_1 + 0x1dd);
  *(undefined1 *)((int)param_2 + 0x172) = *(undefined1 *)(param_1 + 0x1cd);
  return;
}



undefined4 * FUN_0001a80c(int param_1,int param_2)

{
  undefined4 uVar1;
  undefined4 *puVar2;
  uint local_180;
  undefined4 local_17c;
  undefined4 local_178;
  undefined1 auStack_174 [28];
  undefined4 local_158;
  undefined4 local_154;
  undefined4 local_148;
  undefined1 auStack_144 [16];
  undefined1 auStack_134 [268];
  
  FUN_0001a6f8(param_1,&local_180);
  puVar2 = (undefined4 *)0x0;
  if ((local_180 & 2) == 0) {
    uVar1 = *(undefined4 *)(param_2 + 0x9c);
    *(int *)(param_1 + 0x3c) = param_2;
    FUN_000141f4(uVar1,auStack_174);
    FUN_00014604(*(undefined4 *)(param_2 + 0x9c),&local_17c,&local_178);
    uVar1 = FUN_00013254(param_2);
    local_148 = *(undefined4 *)(param_1 + 8);
    FUN_00029b08(auStack_144,param_1 + 0x58,0x10);
    FUN_00029b08(auStack_134,param_1 + 0x78,0x100);
    puVar2 = &local_148;
    syscall();
    if (puVar2 == (undefined4 *)0x0) {
      FUN_0001db2c(*(undefined4 *)(param_1 + 0x30),*(undefined4 *)(param_1 + 0x40));
      puVar2 = (undefined4 *)
               FUN_0001b678(*(undefined4 *)(param_1 + 0x38),local_17c,local_178,local_154,local_158,
                            uVar1);
      if (-1 < (int)puVar2) {
        puVar2 = (undefined4 *)FUN_000215b0(*(undefined4 *)(param_1 + 0x24),2);
      }
    }
  }
  else {
    *(int *)(param_1 + 0x3c) = param_2;
  }
  return puVar2;
}



undefined4 FUN_0001a94c(undefined8 param_1)

{
  undefined4 uVar1;
  undefined1 local_10 [16];
  
  uVar1 = FUN_000292f0(param_1,0,4,local_10,4);
  return uVar1;
}



undefined4 FUN_0001a99c(int param_1,undefined4 param_2,undefined8 param_3)

{
  undefined4 uVar1;
  
  FUN_0001a94c(param_3,*(undefined4 *)(param_1 + 4));
  uVar1 = FUN_0001e360(*(undefined4 *)(param_1 + 0x34),param_2,param_3);
  return uVar1;
}



undefined4 FUN_0001a9fc(int param_1,undefined4 param_2,undefined8 param_3)

{
  undefined4 uVar1;
  
  FUN_0001a94c(param_3,*(undefined4 *)(param_1 + 4));
  FUN_0001dd10(*(undefined4 *)(param_1 + 0x30),param_2,param_3);
  FUN_0001b5f4(*(undefined4 *)(param_1 + 0x38));
  uVar1 = FUN_000215b0(*(undefined4 *)(param_1 + 0x24),0x10);
  return uVar1;
}



undefined4 FUN_0001aa70(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001cd40(*(undefined4 *)(param_1 + 0x2c));
  return uVar1;
}



undefined4 FUN_0001aa98(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001ce58(*(undefined4 *)(param_1 + 0x2c));
  return uVar1;
}



int FUN_0001aac0(int param_1,undefined4 param_2,int param_3)

{
  int iVar1;
  byte in_cr0;
  byte in_cr1;
  byte in_cr2;
  byte in_cr3;
  byte unaff_cr4;
  byte in_cr5;
  byte in_cr6;
  byte in_cr7;
  uint uStack00000008;
  uint local_30 [4];
  
  uStack00000008 =
       (uint)(in_cr0 & 0xf) << 0x1c | (uint)(in_cr1 & 0xf) << 0x18 | (uint)(in_cr2 & 0xf) << 0x14 |
       (uint)(in_cr3 & 0xf) << 0x10 | (uint)(unaff_cr4 & 0xf) << 0xc | (uint)(in_cr5 & 0xf) << 8 |
       (uint)(in_cr6 & 0xf) << 4 | (uint)(in_cr7 & 0xf);
  FUN_0001a6f8(param_1,local_30);
  iVar1 = -0x7ffd67fc;
  if ((local_30[0] & 2) != 0) {
    if (param_3 != 0) {
      FUN_0001a94c(param_3,*(undefined4 *)(param_1 + 4));
    }
    iVar1 = FUN_0001d030(*(undefined4 *)(param_1 + 0x2c),param_2,param_1 + 0x18,param_3);
    if (-1 < iVar1) {
      FUN_0001b64c(*(undefined4 *)(param_1 + 0x38));
      iVar1 = FUN_000215b0(*(undefined4 *)(param_1 + 0x24),8);
      if (param_3 != 0) {
        FUN_0001b5f4(*(undefined4 *)(param_1 + 0x38));
        iVar1 = FUN_000215b0(*(undefined4 *)(param_1 + 0x24),0x10);
      }
    }
  }
  return iVar1;
}



undefined4 FUN_0001aba4(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001c160(*(undefined4 *)(param_1 + 0x28));
  return uVar1;
}



undefined4 FUN_0001abcc(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_0001c20c(*(undefined4 *)(param_1 + 0x28));
  return uVar1;
}



int FUN_0001abf4(int param_1,undefined4 param_2,int param_3,ulonglong param_4)

{
  int iVar1;
  byte bVar2;
  ulonglong uVar3;
  uint local_40 [6];
  
  FUN_0001a6f8(param_1,local_40);
  if ((local_40[0] & 2) == 0) {
    return -0x7ffd67fc;
  }
  if (param_3 != 0) {
    FUN_0001a94c(param_3,*(undefined4 *)(param_1 + 4));
  }
  if (*(char *)(param_1 + 0x4e) != '\0') {
    uVar3 = (ulonglong)((int)param_4 >> 0x1f);
    bVar2 = (byte)(uVar3 - (uVar3 ^ param_4) >> 0x18) >> 7;
    if ((*(char *)(param_1 + 0x1cd) == '\0') && ((int)param_4 != 0x40)) goto LAB_0001ac94;
  }
  bVar2 = 0;
LAB_0001ac94:
  FUN_0001c0f4(*(undefined4 *)(param_1 + 0x28),bVar2);
  iVar1 = FUN_0001c424(*(undefined4 *)(param_1 + 0x28),param_2,param_1 + 0x18,param_3,
                       *(undefined4 *)(param_1 + 4));
  if (-1 < iVar1) {
    FUN_0001b620(*(undefined4 *)(param_1 + 0x38));
    iVar1 = FUN_000215b0(*(undefined4 *)(param_1 + 0x24),4);
  }
  return iVar1;
}



void FUN_0001ad00(int param_1)

{
  FUN_0001e314(*(undefined4 *)(param_1 + 0x34));
  return;
}



int FUN_0001ad24(int param_1)

{
  int iVar1;
  int iVar2;
  
  *(undefined8 *)(param_1 + 0x18) = 0;
  FUN_0001b6bc(*(undefined4 *)(param_1 + 0x38));
  FUN_0001cb10(*(undefined4 *)(param_1 + 0x28));
  FUN_0001da54(*(undefined4 *)(param_1 + 0x2c));
  FUN_0001dc24(*(undefined4 *)(param_1 + 0x30));
  FUN_0001e314(*(undefined4 *)(param_1 + 0x34));
  iVar1 = *(int *)(param_1 + 0x40);
  iVar2 = 0;
  if (iVar1 != -1) {
    syscall();
    *(undefined4 *)(param_1 + 0x40) = 0xffffffff;
    iVar2 = iVar1;
  }
  FUN_0002146c(*(undefined4 *)(param_1 + 0x24),0);
  return iVar2;
}



int FUN_0001adbc(int param_1,int param_2,undefined8 *param_3,undefined4 param_4,undefined4 param_5,
                uint param_6,undefined1 param_7,undefined4 param_8)

{
  byte bVar1;
  undefined1 uVar5;
  code *pcVar2;
  undefined2 uVar3;
  ushort uVar4;
  int iVar6;
  undefined8 uVar7;
  undefined1 in_stack_00000077;
  
  uVar7 = *param_3;
  *(undefined4 *)(param_1 + 0x44) = param_5;
  *(undefined4 *)(param_1 + 8) = 0;
  *(undefined4 *)(param_1 + 4) = *(undefined4 *)(param_2 + 0x160);
  *(undefined8 *)(param_1 + 0x18) = uVar7;
  FUN_00029b08(param_1 + 0x58,param_2,0x194);
  *(undefined1 *)(param_1 + 0x1cf) = 1;
  if (param_6 == 0) {
    if (((*(byte *)(param_2 + 0x176) & 8) != 0) && (*(char *)(param_2 + 0x11f) == '\0')) {
LAB_0001aed4:
      uVar5 = 4;
      goto LAB_0001af00;
    }
LAB_0001ae80:
    if ((*(byte *)(param_2 + 0x176) & 4) == 0) {
      if ((*(byte *)(param_2 + 0x176) & 2) == 0) goto LAB_0001af08;
      uVar5 = 3;
    }
    else {
      uVar5 = 2;
    }
    *(undefined1 *)(param_1 + 0x1cf) = uVar5;
LAB_0001af08:
    if (param_6 != 0x23) goto LAB_0001af48;
    if ((*(byte *)(param_2 + 0x176) & 8) == 0) {
      return -0x7ffd67ba;
    }
    if (*(char *)(param_2 + 0x189) == '\0') {
      return -0x7ffd67b9;
    }
    *(undefined1 *)(param_1 + 0x1cf) = 4;
LAB_0001af54:
    uVar5 = 2;
  }
  else {
    if ((param_6 == 0x12) || (param_6 == 0x22)) goto LAB_0001ae80;
    if (param_6 != 0x13) goto LAB_0001af08;
    bVar1 = *(byte *)(param_2 + 0x176);
    if (((bVar1 & 8) != 0) && (*(char *)(param_2 + 0x11f) == '\0')) goto LAB_0001aed4;
    if ((bVar1 & 4) == 0) {
      if ((bVar1 & 2) != 0) {
        uVar5 = 3;
        goto LAB_0001af00;
      }
    }
    else {
      uVar5 = 2;
LAB_0001af00:
      *(undefined1 *)(param_1 + 0x1cf) = uVar5;
    }
LAB_0001af48:
    if (*(char *)(param_1 + 0x1cf) == '\x04') goto LAB_0001af54;
    uVar5 = 1;
  }
  *(undefined1 *)(param_1 + 0x1dd) = uVar5;
  if (((*(byte *)(param_2 + 0x187) & 1) == 0) || (*(char *)(param_1 + 0x1cf) != '\x01')) {
    uVar5 = 0x72;
  }
  else {
    uVar5 = 0x71;
  }
  *(undefined1 *)(param_1 + 0x1e0) = uVar5;
  if (*(int *)(param_1 + 0x1e4) == 0) {
    if (*(char *)(param_1 + 0x1e0) == 'q') {
      pcVar2 = FUN_00023280;
    }
    else {
      pcVar2 = (code *)0x1f400;
    }
    *(code **)(param_1 + 0x1e4) = pcVar2;
  }
  if (*(char *)(param_1 + 0x1cf) == '\x04') {
    if (*(ushort *)(param_1 + 0x1d0) < 0x361) {
      if (*(ushort *)(param_1 + 0x1d0) < 0xa0) {
        uVar3 = 0xa0;
        goto LAB_0001aff0;
      }
    }
    else {
      uVar3 = 0x360;
LAB_0001aff0:
      *(undefined2 *)(param_1 + 0x1d0) = uVar3;
    }
    uVar4 = *(ushort *)(param_1 + 0x1d2);
    if (uVar4 < 0x1e1) {
LAB_0001b040:
      if (0x77 < uVar4) goto LAB_0001b050;
      uVar3 = 0x78;
    }
    else {
      uVar3 = 0x1e0;
    }
  }
  else {
    if (*(ushort *)(param_1 + 0x1d0) < 0x1e1) {
      if (*(ushort *)(param_1 + 0x1d0) < 0xa0) {
        uVar3 = 0xa0;
        goto LAB_0001b028;
      }
    }
    else {
      uVar3 = 0x1e0;
LAB_0001b028:
      *(undefined2 *)(param_1 + 0x1d0) = uVar3;
    }
    uVar4 = *(ushort *)(param_1 + 0x1d2);
    if (uVar4 < 0x111) goto LAB_0001b040;
    uVar3 = 0x110;
  }
  *(undefined2 *)(param_1 + 0x1d2) = uVar3;
LAB_0001b050:
  iVar6 = *(int *)(param_1 + 0x1d8);
  if ((((iVar6 != 7) && (iVar6 != 10)) && (iVar6 != 0xf)) && (iVar6 != 0x1e)) {
    *(undefined4 *)(param_1 + 0x1d8) = 0x1e;
  }
  if (((param_6 & 0x20) == 0) || (*(char *)(param_1 + 0x1e1) != '\x01')) {
    *(undefined1 *)(param_1 + 0x1e1) = 0;
  }
  FUN_0001db84(*(undefined4 *)(param_1 + 0x30),*(undefined1 *)(param_1 + 0x1e1));
  *(undefined1 *)(param_1 + 0x4e) = param_7;
  FUN_00029b08(param_1 + 0x48,param_8,6);
  *(undefined1 *)(param_1 + 0x4f) = in_stack_00000077;
  FUN_0002146c(*(undefined4 *)(param_1 + 0x24),0);
  FUN_0001a94c(param_4,*(undefined4 *)(param_1 + 4));
  iVar6 = FUN_0001b8d0(*(undefined4 *)(param_1 + 0x38),param_1 + 0x58,param_3,param_4);
  if (-1 < iVar6) {
    iVar6 = FUN_000215b0(*(undefined4 *)(param_1 + 0x24),1);
  }
  return iVar6;
}



undefined4 FUN_0001b14c(int param_1)

{
  undefined4 uVar1;
  
  FUN_0001c2cc(*(undefined4 *)(param_1 + 0x28));
  uVar1 = FUN_0001cf90(*(undefined4 *)(param_1 + 0x2c));
  return uVar1;
}



undefined8 FUN_0001b188(int param_1)

{
  if (*(int *)(param_1 + 0x34) != 0) {
    FUN_0001e40c(*(int *)(param_1 + 0x34));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x34));
    *(undefined4 *)(param_1 + 0x34) = 0;
  }
  if (*(int *)(param_1 + 0x30) != 0) {
    FUN_0001de40(*(int *)(param_1 + 0x30));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x30));
    *(undefined4 *)(param_1 + 0x30) = 0;
  }
  if (*(int *)(param_1 + 0x2c) != 0) {
    FUN_0001d158(*(int *)(param_1 + 0x2c));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x2c));
    *(undefined4 *)(param_1 + 0x2c) = 0;
  }
  if (*(int *)(param_1 + 0x28) != 0) {
    FUN_0001c568(*(int *)(param_1 + 0x28));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x28));
    *(undefined4 *)(param_1 + 0x28) = 0;
  }
  if (*(int *)(param_1 + 0x38) != 0) {
    FUN_0001bbb0(*(int *)(param_1 + 0x38));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x38));
    *(undefined4 *)(param_1 + 0x38) = 0;
  }
  if (*(int *)(param_1 + 0x24) != 0) {
    FUN_00021448(*(int *)(param_1 + 0x24));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x24));
    *(undefined4 *)(param_1 + 0x24) = 0;
  }
  return 0;
}



int FUN_0001b28c(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4,
                undefined4 param_5,undefined4 param_6)

{
  bool bVar1;
  bool bVar2;
  bool bVar3;
  bool bVar4;
  bool bVar5;
  int iVar6;
  int iVar7;
  undefined1 auStack_50 [8];
  undefined1 auStack_48 [16];
  
  FUN_000227b4(auStack_50);
  FUN_00029ad0(param_1,0,0x1f0);
  *(undefined4 *)(param_1 + 0x40) = 0xffffffff;
  *(undefined4 *)(param_1 + 0xc) = param_4;
  *(undefined4 *)(param_1 + 0x10) = param_5;
  iVar6 = FUN_0002285c(8);
  *(int *)(param_1 + 0x24) = iVar6;
  if (iVar6 == 0) {
    bVar5 = false;
    bVar4 = false;
    bVar3 = false;
    bVar2 = false;
    bVar1 = false;
    iVar6 = -0x7ffd67ff;
  }
  else {
    bVar5 = false;
    bVar4 = false;
    bVar3 = false;
    bVar2 = false;
    bVar1 = false;
    iVar6 = FUN_00021520(iVar6);
    if (-1 < iVar6) {
      iVar7 = FUN_0002285c(0x118);
      bVar5 = true;
      bVar4 = false;
      bVar3 = false;
      bVar2 = false;
      bVar1 = false;
      iVar6 = -0x7ffd67ff;
      *(int *)(param_1 + 0x28) = iVar7;
      if ((iVar7 != 0) && (iVar6 = FUN_0001c6a0(iVar7,param_2,param_3), -1 < iVar6)) {
        iVar7 = FUN_0002285c(0x108);
        bVar5 = true;
        bVar4 = true;
        bVar3 = false;
        bVar2 = false;
        bVar1 = false;
        iVar6 = -0x7ffd67ff;
        *(int *)(param_1 + 0x2c) = iVar7;
        if ((iVar7 != 0) && (iVar6 = FUN_0001d290(iVar7,param_2,param_3), -1 < iVar6)) {
          iVar7 = FUN_0002285c(0x140);
          bVar5 = true;
          bVar4 = true;
          bVar3 = true;
          bVar2 = false;
          bVar1 = false;
          iVar6 = -0x7ffd67ff;
          *(int *)(param_1 + 0x30) = iVar7;
          if ((iVar7 != 0) &&
             (iVar6 = FUN_0001df38(iVar7,param_2,param_3,*(undefined4 *)(param_1 + 0xc),
                                   *(undefined4 *)(param_1 + 0x10)), -1 < iVar6)) {
            iVar7 = FUN_0002285c(0xb0);
            bVar5 = true;
            bVar4 = true;
            bVar3 = true;
            bVar2 = true;
            bVar1 = false;
            iVar6 = -0x7ffd67ff;
            *(int *)(param_1 + 0x34) = iVar7;
            if ((iVar7 != 0) && (iVar6 = FUN_0001e508(iVar7,param_2,param_3), -1 < iVar6)) {
              bVar5 = true;
              bVar4 = true;
              bVar3 = true;
              bVar2 = true;
              bVar1 = true;
              iVar7 = FUN_0002285c(0x110);
              *(int *)(param_1 + 0x38) = iVar7;
              if (iVar7 == 0) goto LAB_0001b494;
              iVar6 = FUN_0001bce4(iVar7,param_2,param_3,*(undefined4 *)(param_1 + 0xc),
                                   *(undefined4 *)(param_1 + 0x10),param_6);
              if (-1 < iVar6) {
                FUN_000227b4(auStack_48);
                return iVar6;
              }
            }
          }
        }
      }
    }
  }
  if (*(int *)(param_1 + 0x38) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x38));
    *(undefined4 *)(param_1 + 0x38) = 0;
  }
LAB_0001b494:
  if (*(int *)(param_1 + 0x34) != 0) {
    if (bVar1) {
      FUN_0001e40c(*(int *)(param_1 + 0x34));
    }
    FUN_000227fc(*(undefined4 *)(param_1 + 0x34));
    *(undefined4 *)(param_1 + 0x34) = 0;
  }
  if (*(int *)(param_1 + 0x30) != 0) {
    if (bVar2) {
      FUN_0001de40(*(int *)(param_1 + 0x30));
    }
    FUN_000227fc(*(undefined4 *)(param_1 + 0x30));
    *(undefined4 *)(param_1 + 0x30) = 0;
  }
  if (*(int *)(param_1 + 0x2c) != 0) {
    if (bVar3) {
      FUN_0001d158(*(int *)(param_1 + 0x2c));
    }
    FUN_000227fc(*(undefined4 *)(param_1 + 0x2c));
    *(undefined4 *)(param_1 + 0x2c) = 0;
  }
  if (*(int *)(param_1 + 0x28) != 0) {
    if (bVar4) {
      FUN_0001c568(*(int *)(param_1 + 0x28));
    }
    FUN_000227fc(*(undefined4 *)(param_1 + 0x28));
    *(undefined4 *)(param_1 + 0x28) = 0;
  }
  if (*(int *)(param_1 + 0x24) != 0) {
    if (bVar5) {
      FUN_00021448(*(int *)(param_1 + 0x24));
    }
    FUN_000227fc(*(undefined4 *)(param_1 + 0x24));
    *(undefined4 *)(param_1 + 0x24) = 0;
  }
  return iVar6;
}



void FUN_0001b5b4(int param_1,undefined4 param_2,undefined4 param_3,undefined8 param_4,
                 undefined1 param_5)

{
  *(undefined4 *)(param_1 + 0x5c) = param_2;
  *(undefined4 *)(param_1 + 100) = param_3;
  *(undefined1 *)(param_1 + 0x68) = param_5;
  FUN_00029b08(param_1 + 0x69,param_4,0x10);
  return;
}



undefined8 FUN_0001b5f4(int param_1)

{
  FUN_000224d4(*(undefined4 *)(param_1 + 0x38),0x20);
  return 0;
}



undefined8 FUN_0001b620(int param_1)

{
  FUN_000224d4(*(undefined4 *)(param_1 + 0x38),0x10);
  return 0;
}



undefined8 FUN_0001b64c(int param_1)

{
  FUN_000224d4(*(undefined4 *)(param_1 + 0x38),8);
  return 0;
}



undefined8
FUN_0001b678(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4,undefined4 param_5
            ,undefined4 param_6)

{
  *(undefined4 *)(param_1 + 0x44) = param_2;
  *(undefined4 *)(param_1 + 0x48) = param_3;
  *(undefined4 *)(param_1 + 0x4c) = param_4;
  *(undefined4 *)(param_1 + 0x50) = param_5;
  *(undefined4 *)(param_1 + 0x54) = param_6;
  FUN_000224d4(*(undefined4 *)(param_1 + 0x38),2);
  return 0;
}



void FUN_0001b6bc(int param_1,undefined4 param_2)

{
  *(undefined4 *)(param_1 + 0x58) = param_2;
  FUN_000224d4(*(undefined4 *)(param_1 + 0x38),4);
  FUN_0002231c(*(undefined4 *)(param_1 + 0x38),0,2);
  return;
}



undefined4 FUN_0001b704(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x18,0);
  return uVar1;
}



undefined4 FUN_0001b734(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x20,0);
  return uVar1;
}



undefined4 FUN_0001b764(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x18,0);
  return uVar1;
}



undefined4 FUN_0001b794(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x10,0);
  return uVar1;
}



undefined4 FUN_0001b7c4(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x10,0);
  return uVar1;
}



undefined4 FUN_0001b7f4(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x18);
  return uVar1;
}



undefined4 FUN_0001b820(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x20);
  return uVar1;
}



undefined4 FUN_0001b84c(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x18);
  return uVar1;
}



undefined4 FUN_0001b878(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x10);
  return uVar1;
}



undefined4 FUN_0001b8a4(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x10);
  return uVar1;
}



undefined4 FUN_0001b8d0(int param_1,undefined4 param_2,undefined8 *param_3,undefined4 param_4)

{
  undefined4 uVar1;
  
  FUN_0001b704();
  if (*(int *)(param_1 + 8) == -1) {
    *(undefined4 *)(param_1 + 8) = param_4;
    FUN_0001b7f4(param_1);
    *(undefined8 *)(param_1 + 0x10) = *param_3;
    *(undefined4 *)(param_1 + 0x10c) = param_2;
    FUN_000224d4(*(undefined4 *)(param_1 + 0x38),1);
    uVar1 = 0;
    *(undefined4 *)(param_1 + 0x58) = 0;
  }
  else {
    FUN_0001b7f4(param_1);
    uVar1 = 0x80029802;
  }
  return uVar1;
}



undefined4 FUN_0001b978(int param_1)

{
  undefined4 uVar1;
  
  FUN_0001b704(param_1);
  uVar1 = 0;
  if (-1 < *(int *)(param_1 + 8)) {
    FUN_000293d0(*(int *)(param_1 + 8),2);
    uVar1 = FUN_00029248(*(undefined4 *)(param_1 + 8));
  }
  FUN_0001b7f4(param_1);
  return uVar1;
}



undefined8 FUN_0001b9f4(int param_1)

{
  undefined1 local_20 [16];
  
  FUN_0001b734();
  if (-1 < *(int *)(param_1 + 8)) {
    FUN_000293d0(*(int *)(param_1 + 8),2);
    FUN_000292f0(*(undefined4 *)(param_1 + 8),0xffff,0x80,local_20,8);
    FUN_00029248(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0xffffffff;
  }
  FUN_0001b820(param_1);
  return 0;
}



undefined8 FUN_0001ba94(int param_1)

{
  FUN_0001b764();
  if (-1 < *(int *)(param_1 + 8)) {
    FUN_000293d0(*(int *)(param_1 + 8),2);
    FUN_00029248(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0xffffffff;
  }
  FUN_0001b84c(param_1);
  return 0;
}



undefined8 FUN_0001bb04(int param_1)

{
  FUN_0001b794();
  if (-1 < *(int *)(param_1 + 0x28)) {
    FUN_000293d0(*(int *)(param_1 + 0x28),2);
    FUN_00029248(*(undefined4 *)(param_1 + 0x28));
    *(undefined4 *)(param_1 + 0x28) = 0xffffffff;
  }
  FUN_0001b878(param_1);
  return 0;
}



undefined4 FUN_0001bb74(undefined8 param_1)

{
  undefined4 uVar1;
  
  FUN_000293d0(param_1,2);
  uVar1 = FUN_00029248(param_1);
  return uVar1;
}



undefined4 FUN_0001bbb0(int param_1)

{
  undefined4 uVar1;
  undefined1 auStack_30 [16];
  
  FUN_000224d4(*(undefined4 *)(param_1 + 0x38),0x40);
  FUN_0001b704(param_1);
  uVar1 = 0;
  if (-1 < *(int *)(param_1 + 8)) {
    uVar1 = FUN_00029328(*(int *)(param_1 + 8),0);
  }
  FUN_0001b7f4(param_1);
  syscall();
  if (-1 < *(int *)(param_1 + 8)) {
    uVar1 = FUN_0001b978(param_1,auStack_30);
    *(undefined4 *)(param_1 + 8) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0x38) != 0) {
    FUN_00022324(*(int *)(param_1 + 0x38));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x38));
    *(undefined4 *)(param_1 + 0x38) = 0;
  }
  if (*(int *)(param_1 + 0x40) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x40));
    *(undefined4 *)(param_1 + 0x40) = 0;
  }
  if (*(int *)(param_1 + 0x3c) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x3c));
    FUN_00022588(*(undefined4 *)(param_1 + 0x3c));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x3c));
    *(undefined4 *)(param_1 + 0x3c) = 0;
  }
  FUN_00029d00(param_1 + 0x18);
  return uVar1;
}



int FUN_0001bce4(undefined4 *param_1,undefined4 param_2,int param_3,undefined4 param_4,
                undefined4 param_5,int param_6)

{
  int iVar1;
  int iVar2;
  undefined1 local_50 [16];
  
  FUN_00029ad0(param_1,0,0x110);
  param_1[2] = 0xffffffff;
  param_1[1] = param_3 + 5;
  *param_1 = param_4;
  param_1[0x18] = param_5;
  iVar1 = FUN_00029948(param_1 + 6,local_50);
  if (iVar1 == 0) {
    iVar1 = FUN_0002285c(0x30);
    param_1[0xe] = iVar1;
    if (iVar1 == 0) {
      iVar1 = -0x7ffd67ff;
    }
    else {
      iVar1 = FUN_0002237c(iVar1);
      if (-1 < iVar1) {
        iVar2 = FUN_0002285c(0xa00);
        iVar1 = -0x7ffd67ff;
        param_1[0x10] = iVar2;
        if (iVar2 != 0) {
          iVar2 = FUN_0002285c(8);
          iVar1 = -0x7ffd67ff;
          param_1[0xf] = iVar2;
          if (((iVar2 != 0) && (iVar1 = FUN_0002265c(iVar2), -1 < iVar1)) &&
             (iVar1 = FUN_000225ac(param_1[0xf],param_2), -1 < iVar1)) {
            FUN_00029b08((int)param_1 + 0x89,param_6 + 0x24,0x80);
            iVar1 = FUN_000298a0(param_1 + 0xc,&PTR_LAB_0002eb80,param_1,param_1[1],0x4000,1,
                                 "cellPremoISessionThread");
            return iVar1;
          }
        }
      }
    }
  }
  FUN_00029d00(param_1 + 6);
  if (param_1[0xe] != 0) {
    FUN_00022324(param_1[0xe]);
    FUN_000227fc(param_1[0xe]);
    param_1[0xe] = 0;
  }
  if (param_1[0x10] != 0) {
    FUN_000227fc(param_1[0x10]);
    param_1[0x10] = 0;
  }
  if (param_1[0xf] != 0) {
    FUN_000225d8(param_1[0xf]);
    FUN_00022588(param_1[0xf]);
    FUN_000227fc(param_1[0xf]);
    param_1[0xf] = 0;
  }
  return iVar1;
}



undefined4 FUN_0001bf34(int param_1)

{
  uint uVar1;
  undefined4 *puVar2;
  undefined4 uVar3;
  int iVar4;
  longlong lVar5;
  uint auStack_80 [32];
  
  uVar3 = 0;
  if (-1 < *(int *)(param_1 + 8)) {
    iVar4 = 0;
    lVar5 = 0x20;
    do {
      puVar2 = (undefined4 *)((int)auStack_80 + iVar4);
      iVar4 = iVar4 + 4;
      *puVar2 = 0;
      lVar5 = lVar5 + -1;
    } while (lVar5 != 0);
    uVar1 = *(uint *)(param_1 + 8);
    auStack_80[(int)uVar1 >> 5] = auStack_80[(int)uVar1 >> 5] | 1 << (uVar1 & 0x1f);
    uVar3 = FUN_000291a0(uVar1 + 1,auStack_80,0,0);
  }
  return uVar3;
}



undefined4 FUN_0001bfe8(int param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  int iVar2;
  
  uVar1 = 0;
  if (*(char *)(param_1 + 0xf1) != '\0') {
    uVar1 = FUN_0002359c(param_2,param_2,0x10,param_1 + 0xf8,param_1 + 0x108);
    iVar2 = FUN_00029de0(param_2,param_3,6);
    if (iVar2 != 0) {
      uVar1 = 0x80029843;
    }
  }
  return uVar1;
}



void FUN_0001c080(int param_1,undefined1 param_2,undefined4 param_3,undefined4 param_4,
                 undefined8 param_5)

{
  *(undefined1 *)(param_1 + 0xf1) = param_2;
  *(undefined4 *)(param_1 + 0xf4) = param_3;
  FUN_00029b08(param_1 + 0x108,param_5,0x10);
  FUN_00029b08(param_1 + 0xf8,param_4,0x10);
  *(undefined1 *)(param_1 + 0xf2) = 0;
  return;
}



void FUN_0001c0f4(int param_1,byte param_2)

{
  undefined4 uVar2;
  ulonglong uVar1;
  
  uVar2 = FUN_00027bf8(0);
  FUN_000279c8(uVar2);
  uVar1 = FUN_00029280();
  *(byte *)(param_1 + 0xf2) = *(byte *)(param_1 + 0xf2) | param_2;
  *(char *)(param_1 + 0xf3) = (char)uVar1 + (char)((uVar1 & 0xffffffff) / 0x1e) * -0x1e + '\x14';
  return;
}



int FUN_0001c160(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int local_30;
  undefined4 local_2c [5];
  
  iVar1 = FUN_000220a4(param_1 + 0x38,&local_30);
  if (-1 < iVar1) {
    if (local_30 == 1) {
      iVar1 = FUN_00021c5c(param_1 + 0x68,local_2c);
      if (-1 < iVar1) {
        FUN_00029b08(local_2c[0],param_2,param_3);
        iVar1 = FUN_00021b90(param_1 + 0x68,param_3);
      }
    }
  }
  return iVar1;
}



int FUN_0001c20c(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int local_30;
  undefined4 local_2c [5];
  
  iVar1 = FUN_000220a4(param_1 + 0x38,&local_30);
  if ((-1 < iVar1) && (param_1 = param_1 + 0x68, local_30 == 1)) {
    iVar1 = FUN_00021ff8(param_1);
    if (-1 < iVar1) {
      iVar1 = FUN_00021a34(param_1,local_2c);
      if (-1 < iVar1) {
        FUN_00029b08(local_2c[0],param_2,param_3);
        iVar1 = FUN_00021b90(param_1,param_3);
      }
    }
  }
  return iVar1;
}



undefined8 FUN_0001c2cc(int param_1)

{
  FUN_000224d4(param_1 + 0x38,4);
  return 0;
}



undefined4 FUN_0001c2fc(int param_1)

{
  undefined4 uVar1;
  
  FUN_0001b734(param_1);
  uVar1 = 0;
  if (-1 < *(int *)(param_1 + 8)) {
    uVar1 = FUN_00029328(*(int *)(param_1 + 8),0);
  }
  FUN_0001b820(param_1);
  return uVar1;
}



void FUN_0001c36c(int param_1)

{
  FUN_0001b764();
  if (-1 < *(int *)(param_1 + 8)) {
    FUN_00029328(*(int *)(param_1 + 8),0);
  }
  FUN_0001b84c(param_1);
  return;
}



undefined4 FUN_0001c3b4(int param_1)

{
  undefined4 uVar1;
  
  FUN_0001b794(param_1);
  uVar1 = 0;
  if (-1 < *(int *)(param_1 + 0x28)) {
    uVar1 = FUN_00029328(*(int *)(param_1 + 0x28),0);
  }
  FUN_0001b878(param_1);
  return uVar1;
}



undefined4 FUN_0001c424(int param_1,undefined4 param_2,undefined8 *param_3,int param_4,int param_5)

{
  undefined8 uVar1;
  undefined4 uVar2;
  int local_50;
  undefined1 local_4c [4];
  undefined1 local_48 [8];
  
  FUN_000220a4(param_1 + 0x38,&local_50);
  uVar2 = 0x80029802;
  if (local_50 == 0) {
    uVar1 = *param_3;
    *(int *)(param_1 + 8) = param_4;
    *(undefined8 *)(param_1 + 0xe8) = uVar1;
    uVar2 = 0;
    if (param_4 != 0) {
      uVar2 = FUN_000292f0(param_4,0xffff,&UNK_00001001,local_4c,4);
      if (param_5 == 0) {
        uVar2 = FUN_000292f0(*(undefined4 *)(param_1 + 8),6,1,local_48,4);
      }
    }
    FUN_00029b08(param_1 + 0xa8,param_2,0x38);
    *(undefined1 *)(param_1 + 0xf0) = 1;
    FUN_00021bd4(param_1 + 0x68);
    FUN_000224d4(param_1 + 0x38,1);
  }
  return uVar2;
}



undefined8 FUN_0001c568(uint *param_1,undefined1 *param_2)

{
  undefined1 auStack_30 [24];
  
  if ((*param_1 & 0x20) != 0) {
    FUN_000224d4(param_1 + 0xe,0x10);
    FUN_00021afc(param_1 + 0x1a);
    FUN_0001c2fc(param_1);
    param_2 = auStack_30;
    syscall();
  }
  if (0 < (int)param_1[3]) {
    FUN_00029248(param_1[3],param_2);
    param_1[3] = 0;
  }
  if ((*param_1 & 2) != 0) {
    FUN_00021cac(param_1 + 0x1a);
  }
  if ((*param_1 & 4) != 0) {
    FUN_00022324(param_1 + 0xe);
  }
  if ((*param_1 & 0x10) != 0) {
    FUN_000225d8(param_1 + 0x38);
  }
  if ((*param_1 & 8) != 0) {
    FUN_00022588(param_1 + 0x38);
  }
  if ((*param_1 & 1) != 0) {
    FUN_00029d00(param_1 + 8);
  }
  *param_1 = 0;
  return 0;
}



uint FUN_0001c6a0(uint *param_1,undefined4 param_2,int param_3)

{
  uint uVar1;
  undefined1 local_40 [16];
  
  FUN_00029ad0(param_1,0,0x118);
  param_1[2] = 0xffffffff;
  param_1[1] = param_3 + 8;
  uVar1 = FUN_00029948(param_1 + 8,local_40);
  if (-1 < (int)uVar1) {
    *param_1 = *param_1 | 1;
    uVar1 = FUN_00021d2c(param_1 + 0x1a,0x4b800,0);
    if (-1 < (int)uVar1) {
      *param_1 = *param_1 | 2;
      uVar1 = FUN_0002237c(param_1 + 0xe);
      if (-1 < (int)uVar1) {
        *param_1 = *param_1 | 4;
        uVar1 = FUN_0002265c(param_1 + 0x38);
        if (-1 < (int)uVar1) {
          *param_1 = *param_1 | 8;
          uVar1 = FUN_000225ac(param_1 + 0x38,param_2);
          if (-1 < (int)uVar1) {
            *param_1 = *param_1 | 0x10;
            uVar1 = FUN_00029398(2,2,0);
            if (-1 < (int)uVar1) {
              param_1[3] = uVar1;
              uVar1 = FUN_000298a0(param_1 + 6,&PTR_LAB_0002eb88,param_1,param_1[1],0x1000,1,
                                   "cellPremoVSessionThread");
              if (-1 < (int)uVar1) {
                *param_1 = *param_1 | 0x20;
                return uVar1;
              }
            }
          }
        }
      }
    }
  }
  FUN_0001c568(param_1);
  return uVar1;
}



int FUN_0001c89c(int param_1)

{
  undefined2 uVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  ulonglong *puVar4;
  ulonglong *puVar5;
  ulonglong uVar6;
  ulonglong uVar7;
  ulonglong uVar8;
  undefined1 *puVar9;
  ulonglong uVar10;
  ulonglong uVar11;
  int iVar12;
  longlong lVar13;
  ulonglong unaff_r22;
  ulonglong unaff_r23;
  ulonglong unaff_r24;
  ulonglong unaff_r25;
  ulonglong unaff_r26;
  ulonglong unaff_r27;
  ulonglong unaff_r28;
  ulonglong unaff_r29;
  ulonglong unaff_r30;
  ulonglong unaff_r31;
  ulonglong in_LR;
  
  uVar10 = ZEXT48(&stack0x00000000);
  puVar4 = (ulonglong *)(uVar10 - 0xf0);
  *puVar4 = uVar10;
  puVar4[0x17] = unaff_r25;
  puVar4[0x1d] = unaff_r31;
  puVar4[0x14] = unaff_r22;
  puVar4[0x16] = unaff_r24;
  puVar4[0x1a] = unaff_r28;
  puVar4[0x1b] = unaff_r29;
  puVar4[0x20] = in_LR;
  puVar4[0x15] = unaff_r23;
  puVar4[0x18] = unaff_r26;
  puVar4[0x19] = unaff_r27;
  puVar4[0x1c] = unaff_r30;
  uVar11 = FUN_00020f2c(0x7d);
  uVar6 = uVar11 + 0x20 & 0xffffffff;
  iVar12 = 0;
  lVar13 = (uVar10 - 0xf0) - (uVar6 + 0x1e & 0xfffffffffffffff0);
  puVar5 = (ulonglong *)lVar13;
  *puVar5 = *puVar4;
  uVar7 = lVar13 + 0x7fU & 0xfffffff0;
  if (-1 < *(int *)(param_1 + 8)) {
    uVar8 = uVar10 - 0x70 & 0xffffffff;
    FUN_000212e0(uVar8,0x7d,0x52455354);
    *(undefined4 *)((int)puVar4 + 0x74) = 0;
    *(undefined4 *)((int)puVar4 + 0x7c) = 0x1e;
    *(undefined4 *)(puVar4 + 0xf) = 0x80029812;
    FUN_00029ad0(uVar7,0,uVar6);
    if (*(int *)(param_1 + 8) == 0) {
      FUN_00021324(uVar8,uVar7,uVar10 - 0x7c);
      uVar2 = *(undefined4 *)(param_1 + 0xc);
      uVar3 = *(undefined4 *)(param_1 + 0xd4);
      uVar1 = *(undefined2 *)(param_1 + 0xd8);
      *(undefined1 *)((int)puVar4 + 0x8d) = 2;
      *(undefined4 *)(puVar4 + 0x12) = uVar3;
      *(undefined2 *)((int)puVar4 + 0x8e) = uVar1;
      iVar12 = FUN_00029360(uVar2,uVar7,uVar11 & 0xffffffff,0,uVar10 - 100 & 0xffffffff,0x10);
    }
    else {
      iVar12 = FUN_00021324(uVar8,uVar7 + 8 & 0xffffffff,uVar10 - 0x7c);
      if (iVar12 < 0) goto LAB_0001cac4;
      lVar13 = uVar7 + 8 + uVar11;
      puVar9 = (undefined1 *)lVar13;
      *puVar9 = 0xd;
      puVar9[1] = 10;
      puVar9[2] = 0x30;
      puVar9[3] = 0xd;
      puVar9[4] = 10;
      puVar9[5] = 0xd;
      puVar9[6] = 10;
      FUN_00029bb0(uVar7,&DAT_0002c700,uVar11 & 0xffffffff);
      *(undefined1 *)((int)uVar7 + 6) = 0xd;
      *(undefined1 *)((int)uVar7 + 7) = 10;
      uVar2 = *(undefined4 *)(param_1 + 8);
      *(undefined4 *)(puVar4 + 0xe) = 1;
      FUN_000292f0(uVar2,6,1,uVar10 - 0x80,4);
      iVar12 = FUN_000294b0(*(undefined4 *)(param_1 + 8),uVar7,(lVar13 + 7) - uVar7 & 0xffffffff,0);
    }
    if (-1 < iVar12) {
      syscall();
    }
  }
LAB_0001cac4:
  *puVar4 = *puVar5;
  return iVar12;
}



void FUN_0001cb10(int param_1)

{
  bool bVar1;
  int iVar2;
  int iVar3;
  uint local_30 [6];
  
  iVar3 = 0;
  do {
    iVar2 = param_1 + 0x38;
    FUN_000220a4(iVar2,local_30);
    if ((local_30[0] & 4) == 0) break;
    syscall();
    bVar1 = iVar3 != 1999;
    iVar3 = iVar3 + 1;
  } while (bVar1);
  FUN_000224d4(iVar2,2);
  FUN_0001c2fc(param_1);
  FUN_00021afc(param_1 + 0x68);
  FUN_0002231c(iVar2,0,2);
  return;
}



undefined4 FUN_0001cbbc(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1 + 0x20,0);
  return uVar1;
}



undefined4 FUN_0001cbec(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830(param_1 + 0x20);
  return uVar1;
}



undefined8 FUN_0001cc18(int param_1)

{
  undefined1 local_20 [4];
  undefined1 local_1c [12];
  
  FUN_0001cbbc(param_1);
  if (-1 < *(int *)(param_1 + 8)) {
    FUN_000292f0(*(int *)(param_1 + 8),6,1,local_20,4);
    FUN_000292f0(*(undefined4 *)(param_1 + 8),0xffff,&UNK_00001001,local_1c,4);
  }
  FUN_0001cbec(param_1);
  return 0;
}



void FUN_0001ccd4(int param_1,undefined1 param_2,undefined4 param_3,undefined4 param_4,
                 undefined8 param_5,undefined1 param_6)

{
  *(undefined4 *)(param_1 + 0xe4) = param_3;
  *(undefined1 *)(param_1 + 0xe0) = param_2;
  *(undefined1 *)(param_1 + 0xe1) = param_6;
  FUN_00029b08(param_1 + 0xf8,param_5,0x10);
  FUN_00029b08(param_1 + 0xe8,param_4,0x10);
  return;
}



int FUN_0001cd40(int param_1,undefined4 param_2,undefined4 param_3,int param_4,int param_5)

{
  int iVar1;
  int local_50;
  int local_4c [5];
  
  iVar1 = FUN_000220a4(param_1 + 0x38,&local_50);
  if (-1 < iVar1) {
    if (local_50 == 1) {
      iVar1 = FUN_00021c5c(param_1 + 0x68,local_4c);
      if (-1 < iVar1) {
        FUN_00029b08(local_4c[0],param_2,param_3);
        if ((param_5 != 0) && (*(char *)(param_1 + 0xe0) != '\0')) {
          if (*(int *)(param_1 + 0xe4) == 0) {
            FUN_000235c8(param_4 + local_4c[0],param_4 + local_4c[0],param_5,param_1 + 0xe8,
                         param_1 + 0xf8);
          }
        }
        iVar1 = FUN_00021b90(param_1 + 0x68,param_3);
      }
    }
  }
  return iVar1;
}



int FUN_0001ce58(int param_1,undefined4 param_2,undefined4 param_3,int param_4,int param_5)

{
  int iVar1;
  int iVar2;
  int local_50;
  int local_4c [3];
  
  iVar1 = FUN_000220a4(param_1 + 0x38,&local_50);
  if (-1 < iVar1) {
    iVar2 = param_1 + 0x68;
    if (local_50 == 1) {
      iVar1 = FUN_00021ff8(iVar2);
      if (-1 < iVar1) {
        iVar1 = FUN_00021a34(iVar2,local_4c);
        if (-1 < iVar1) {
          FUN_00029b08(local_4c[0],param_2,param_3);
          if ((param_5 != 0) && (*(char *)(param_1 + 0xe0) != '\0')) {
            if (*(int *)(param_1 + 0xe4) == 0) {
              FUN_000235c8(param_4 + local_4c[0],param_4 + local_4c[0],param_5,param_1 + 0xe8,
                           param_1 + 0xf8);
            }
          }
          iVar1 = FUN_00021b90(iVar2,param_3);
        }
      }
    }
  }
  return iVar1;
}



undefined8 FUN_0001cf90(int param_1)

{
  FUN_000224d4(param_1 + 0x38,4);
  return 0;
}



undefined4 FUN_0001cfc0(int param_1)

{
  undefined4 uVar1;
  
  FUN_0001cbbc(param_1);
  uVar1 = 0;
  if (-1 < *(int *)(param_1 + 8)) {
    uVar1 = FUN_00029328(*(int *)(param_1 + 8),0);
  }
  FUN_0001cbec(param_1);
  return uVar1;
}



undefined4 FUN_0001d030(int param_1,undefined4 param_2,undefined8 *param_3,int param_4)

{
  undefined8 uVar1;
  undefined4 uVar2;
  int local_50;
  undefined1 local_4c [4];
  undefined1 local_48 [16];
  
  FUN_000220a4(param_1 + 0x38,&local_50);
  uVar2 = 0x80029802;
  if (local_50 == 0) {
    uVar1 = *param_3;
    *(int *)(param_1 + 8) = param_4;
    *(undefined8 *)(param_1 + 0xd8) = uVar1;
    uVar2 = 0;
    if (param_4 != 0) {
      FUN_000292f0(param_4,6,1,local_4c,4);
      uVar2 = FUN_000292f0(*(undefined4 *)(param_1 + 8),0xffff,&UNK_00001001,local_48,4);
    }
    FUN_00029b08(param_1 + 0xa8,param_2,0x28);
    *(undefined4 *)(param_1 + 0x10) = 1;
    FUN_00021bd4(param_1 + 0x68);
    FUN_000224d4(param_1 + 0x38,1);
  }
  return uVar2;
}



undefined8 FUN_0001d158(uint *param_1,undefined1 *param_2)

{
  undefined1 auStack_30 [24];
  
  if ((*param_1 & 0x20) != 0) {
    FUN_000224d4(param_1 + 0xe,0x10);
    FUN_00021afc(param_1 + 0x1a);
    FUN_0001cfc0(param_1);
    param_2 = auStack_30;
    syscall();
  }
  if (0 < (int)param_1[3]) {
    FUN_00029248(param_1[3],param_2);
    param_1[3] = 0;
  }
  if ((*param_1 & 0x10) != 0) {
    FUN_000225d8(param_1 + 0x34);
  }
  if ((*param_1 & 8) != 0) {
    FUN_00022588(param_1 + 0x34);
  }
  if ((*param_1 & 4) != 0) {
    FUN_00022324(param_1 + 0xe);
  }
  if ((*param_1 & 2) != 0) {
    FUN_00021cac(param_1 + 0x1a);
  }
  if ((*param_1 & 1) != 0) {
    FUN_00029d00(param_1 + 8);
  }
  *param_1 = 0;
  return 0;
}



uint FUN_0001d290(uint *param_1,undefined4 param_2,int param_3)

{
  uint uVar1;
  undefined1 local_40 [16];
  
  FUN_00029ad0(param_1,0,0x108);
  param_1[2] = 0xffffffff;
  param_1[1] = param_3 + 7;
  uVar1 = FUN_00029948(param_1 + 8,local_40);
  if (-1 < (int)uVar1) {
    *param_1 = *param_1 | 1;
    uVar1 = FUN_00021d2c(param_1 + 0x1a,0x628,0x40);
    if (-1 < (int)uVar1) {
      *param_1 = *param_1 | 2;
      uVar1 = FUN_0002237c(param_1 + 0xe);
      if (-1 < (int)uVar1) {
        *param_1 = *param_1 | 4;
        uVar1 = FUN_0002265c(param_1 + 0x34);
        if (-1 < (int)uVar1) {
          *param_1 = *param_1 | 8;
          uVar1 = FUN_000225ac(param_1 + 0x34,param_2);
          if (-1 < (int)uVar1) {
            *param_1 = *param_1 | 0x10;
            uVar1 = FUN_00029398(2,2,0);
            if (-1 < (int)uVar1) {
              param_1[3] = uVar1;
              uVar1 = FUN_000298a0(param_1 + 6,&PTR_LAB_0002eb90,param_1,param_1[1],0x1000,1,
                                   "cellPremoASessionThread");
              *param_1 = *param_1 | 0x20;
              return uVar1;
            }
          }
        }
      }
    }
  }
  FUN_0001d158(param_1);
  return uVar1;
}



undefined8 FUN_0001d480(int param_1)

{
  FUN_0001cbbc();
  if (-1 < *(int *)(param_1 + 8)) {
    FUN_000293d0(*(int *)(param_1 + 8),2);
    FUN_00029248(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0xffffffff;
  }
  FUN_0001cbec(param_1);
  return 0;
}



int FUN_0001d4f0(int param_1)

{
  undefined2 uVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  ulonglong *puVar4;
  ulonglong *puVar5;
  ulonglong uVar6;
  ulonglong uVar7;
  ulonglong uVar8;
  undefined1 *puVar9;
  ulonglong uVar10;
  ulonglong uVar11;
  int iVar12;
  longlong lVar13;
  ulonglong unaff_r22;
  ulonglong unaff_r23;
  ulonglong unaff_r24;
  ulonglong unaff_r25;
  ulonglong unaff_r26;
  ulonglong unaff_r27;
  ulonglong unaff_r28;
  ulonglong unaff_r29;
  ulonglong unaff_r30;
  ulonglong unaff_r31;
  ulonglong in_LR;
  
  uVar10 = ZEXT48(&stack0x00000000);
  puVar4 = (ulonglong *)(uVar10 - 0xf0);
  *puVar4 = uVar10;
  puVar4[0x17] = unaff_r25;
  puVar4[0x1d] = unaff_r31;
  puVar4[0x14] = unaff_r22;
  puVar4[0x16] = unaff_r24;
  puVar4[0x1a] = unaff_r28;
  puVar4[0x1b] = unaff_r29;
  puVar4[0x20] = in_LR;
  puVar4[0x15] = unaff_r23;
  puVar4[0x18] = unaff_r26;
  puVar4[0x19] = unaff_r27;
  puVar4[0x1c] = unaff_r30;
  uVar11 = FUN_00020f2c(0x7d);
  uVar6 = uVar11 + 0x20 & 0xffffffff;
  iVar12 = 0;
  lVar13 = (uVar10 - 0xf0) - (uVar6 + 0x1e & 0xfffffffffffffff0);
  puVar5 = (ulonglong *)lVar13;
  *puVar5 = *puVar4;
  uVar7 = lVar13 + 0x7fU & 0xfffffff0;
  if (-1 < *(int *)(param_1 + 8)) {
    uVar8 = uVar10 - 0x80 & 0xffffffff;
    FUN_000212e0(uVar8,0x7d,0x52455354);
    *(undefined4 *)((int)puVar4 + 0x7c) = 0;
    *(undefined4 *)((int)puVar4 + 0x84) = 0x1e;
    *(undefined4 *)(puVar4 + 0x10) = 0x80029812;
    FUN_00029ad0(uVar7,0,uVar6);
    if (*(int *)(param_1 + 8) == 0) {
      FUN_00021324(uVar8,uVar7,uVar10 - 0x74);
      uVar2 = *(undefined4 *)(param_1 + 0xc);
      uVar3 = *(undefined4 *)(param_1 + 0xc4);
      uVar1 = *(undefined2 *)(param_1 + 200);
      *(undefined1 *)((int)puVar4 + 0x89) = 2;
      *(undefined4 *)((int)puVar4 + 0x8c) = uVar3;
      *(undefined2 *)((int)puVar4 + 0x8a) = uVar1;
      iVar12 = FUN_00029360(uVar2,uVar7,uVar11 & 0xffffffff,0,uVar10 - 0x68 & 0xffffffff,0x10);
    }
    else {
      iVar12 = FUN_00021324(uVar8,uVar7 + 8 & 0xffffffff,uVar10 - 0x74);
      if (iVar12 < 0) goto LAB_0001d6f4;
      lVar13 = uVar7 + 8 + uVar11;
      puVar9 = (undefined1 *)lVar13;
      *puVar9 = 0xd;
      puVar9[1] = 10;
      puVar9[2] = 0x30;
      puVar9[3] = 0xd;
      puVar9[4] = 10;
      puVar9[5] = 0xd;
      puVar9[6] = 10;
      FUN_00029bb0(uVar7,&DAT_0002c770,uVar11 & 0xffffffff);
      *(undefined1 *)((int)uVar7 + 6) = 0xd;
      *(undefined1 *)((int)uVar7 + 7) = 10;
      iVar12 = FUN_000294b0(*(undefined4 *)(param_1 + 8),uVar7,(lVar13 + 7) - uVar7 & 0xffffffff,0);
    }
    if (-1 < iVar12) {
      syscall();
    }
  }
LAB_0001d6f4:
  *puVar4 = *puVar5;
  return iVar12;
}



undefined4 FUN_0001d740(int param_1)

{
  undefined4 uVar1;
  undefined1 local_20 [8];
  undefined1 local_18 [8];
  undefined1 local_10 [16];
  
  uVar1 = FUN_00022614(param_1 + 0xd0,local_20,local_18,local_10);
  return uVar1;
}



void FUN_0001da54(int param_1)

{
  bool bVar1;
  int iVar2;
  int iVar3;
  uint local_30 [6];
  
  iVar3 = 0;
  do {
    iVar2 = param_1 + 0x38;
    FUN_000220a4(iVar2,local_30);
    if ((local_30[0] & 4) == 0) break;
    syscall();
    bVar1 = iVar3 != 999;
    iVar3 = iVar3 + 1;
  } while (bVar1);
  FUN_000224d4(iVar2,2);
  FUN_0001cfc0(param_1);
  FUN_00021afc(param_1 + 0x68);
  FUN_0002231c(iVar2,0,2);
  return;
}



undefined4 FUN_0001db00(uint param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0;
  if ((0x3bff < param_1 - 1) || ((param_1 & 0x7f) != 0)) {
    uVar1 = 0x80029804;
  }
  return uVar1;
}



void FUN_0001db2c(int param_1,undefined4 param_2)

{
  *(undefined4 *)(param_1 + 0xf0) = param_2;
  return;
}



undefined4
FUN_0001db34(undefined4 param_1,undefined2 param_2,undefined2 param_3,undefined4 *param_4)

{
  *(undefined2 *)(param_4 + 1) = param_2;
  *(undefined2 *)((int)param_4 + 6) = param_3;
  *(undefined2 *)((int)param_4 + 0x12) = 0x80;
  *param_4 = 8;
  *(undefined2 *)(param_4 + 3) = 0x80;
  *(undefined2 *)((int)param_4 + 0xe) = 0x80;
  *(undefined2 *)(param_4 + 4) = 0x80;
  syscall();
  return param_1;
}



void FUN_0001db7c(int param_1,undefined4 param_2)

{
  *(undefined4 *)(param_1 + 0x134) = param_2;
  return;
}



void FUN_0001db84(int param_1,undefined1 param_2)

{
  *(undefined1 *)(param_1 + 0x139) = param_2;
  return;
}



void FUN_0001db8c(int param_1)

{
  *(undefined4 *)(param_1 + 0xfc) = 0x1e;
  FUN_0001c36c();
  return;
}



void FUN_0001db9c(int param_1,undefined1 param_2,undefined8 param_3,undefined4 param_4,
                 undefined8 param_5,undefined1 param_6)

{
  *(undefined1 *)(param_1 + 0x100) = param_2;
  *(undefined1 *)(param_1 + 0x101) = param_6;
  FUN_00029b08(param_1 + 0x102,param_5,0x10);
  FUN_00029b08(param_1 + 0x112,param_4,0x10);
  FUN_00029b08(param_1 + 0x124,param_1 + 0x112,0x10);
  return;
}



void FUN_0001dc24(int param_1)

{
  FUN_000224d4(*(undefined4 *)(param_1 + 0x30),4);
  FUN_0001c36c(param_1);
  FUN_0002231c(*(undefined4 *)(param_1 + 0x30),0,2);
  return;
}



undefined4 FUN_0001dc70(int param_1)

{
  undefined4 uVar1;
  uint local_30 [2];
  undefined1 local_28 [24];
  
  if (*(int *)(param_1 + 0xfc) != 0) {
    FUN_000220a4(*(undefined4 *)(param_1 + 0x30),local_30);
    if ((local_30[0] & 0x104) == 0) {
      uVar1 = FUN_000292f0(*(undefined4 *)(param_1 + 8),0xffff,&DAT_00001006,local_28,0x10);
      *(undefined4 *)(param_1 + 0xfc) = 0;
      return uVar1;
    }
  }
  return 0x80010204;
}



int FUN_0001dd10(int param_1,undefined8 *param_2,undefined4 param_3)

{
  int iVar1;
  
  if ((*(char *)(param_2 + 2) != '\0') ||
     (iVar1 = FUN_0001db00(*(undefined4 *)(param_2 + 1)), iVar1 == 0)) {
    FUN_0001b764(param_1);
    if (*(int *)(param_1 + 8) == -1) {
      *(undefined4 *)(param_1 + 8) = param_3;
      FUN_0001b84c(param_1);
      *(undefined8 *)(param_1 + 0xe8) = *param_2;
      FUN_00029b08(param_1 + 0x124,param_1 + 0x112,0x10);
      *(int *)(param_1 + 0x124) =
           *(int *)((int)param_2 + 0xc) * 0x11 + *(int *)(param_1 + 0x124) + -0x11;
      FUN_00029b08(param_1 + 0xb8,param_2,0x18);
      *(undefined4 *)(param_1 + 0xe0) = *(undefined4 *)(param_2 + 1);
      *(undefined1 *)(param_1 + 0x138) = *(undefined1 *)(param_2 + 2);
      *(undefined4 *)(param_1 + 0xf4) = 0;
      FUN_0001dc70(param_1);
      FUN_000224d4(*(undefined4 *)(param_1 + 0x30),1);
      iVar1 = 0;
    }
    else {
      FUN_0001b84c(param_1);
      iVar1 = -0x7ffd67fe;
    }
  }
  return iVar1;
}



undefined8 FUN_0001de40(int param_1)

{
  undefined1 auStack_30 [16];
  
  FUN_000224d4(*(undefined4 *)(param_1 + 0x30),0x100);
  FUN_0001c36c(param_1);
  syscall();
  FUN_00029d00(param_1 + 0x18,auStack_30);
  if (*(int *)(param_1 + 0xd0) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0xd0));
    *(undefined4 *)(param_1 + 0xd0) = 0;
  }
  if (*(int *)(param_1 + 0xd4) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0xd4));
    *(undefined4 *)(param_1 + 0xd4) = 0;
  }
  if (*(int *)(param_1 + 0x30) != 0) {
    FUN_00022324(*(int *)(param_1 + 0x30));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x30));
    *(undefined4 *)(param_1 + 0x30) = 0;
  }
  if (*(int *)(param_1 + 0xdc) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0xdc));
    FUN_00022588(*(undefined4 *)(param_1 + 0xdc));
    FUN_000227fc(*(undefined4 *)(param_1 + 0xdc));
    *(undefined4 *)(param_1 + 0xdc) = 0;
  }
  return 0;
}



int FUN_0001df38(undefined4 *param_1,undefined4 param_2,int param_3,undefined4 param_4,
                undefined4 param_5)

{
  int iVar1;
  int iVar2;
  undefined1 local_50 [24];
  
  FUN_00029ad0(param_1,0,0x140);
  param_1[2] = 0xffffffff;
  param_1[1] = param_3 + 1;
  *param_1 = param_4;
  param_1[0x3e] = param_5;
  iVar1 = FUN_00029948(param_1 + 6,local_50);
  if (iVar1 == 0) {
    iVar1 = FUN_0002285c(0x100);
    param_1[0x34] = iVar1;
    if (iVar1 == 0) {
      iVar1 = -0x7ffd67ff;
    }
    else {
      iVar2 = FUN_0002285c(0x110);
      iVar1 = -0x7ffd67ff;
      param_1[0x35] = iVar2;
      if (iVar2 != 0) {
        iVar2 = FUN_0002285c(0x30);
        iVar1 = -0x7ffd67ff;
        param_1[0xc] = iVar2;
        if ((iVar2 != 0) && (iVar1 = FUN_0002237c(iVar2), -1 < iVar1)) {
          iVar2 = FUN_0002285c(8);
          iVar1 = -0x7ffd67ff;
          param_1[0x37] = iVar2;
          if ((iVar2 != 0) &&
             ((iVar1 = FUN_0002265c(iVar2), -1 < iVar1 &&
              (iVar1 = FUN_000225ac(param_1[0x37],param_2), -1 < iVar1)))) {
            iVar1 = FUN_000298a0(param_1 + 4,&PTR_LAB_0002eb98,param_1,param_1[1],0x1000,1,
                                 "cellPremoPadSessionThread");
            return iVar1;
          }
        }
      }
    }
  }
  FUN_00029d00(param_1 + 6);
  if (param_1[0x34] != 0) {
    FUN_000227fc(param_1[0x34]);
    param_1[0x34] = 0;
  }
  if (param_1[0x35] != 0) {
    FUN_000227fc(param_1[0x35]);
    param_1[0x35] = 0;
  }
  if (param_1[0xc] != 0) {
    FUN_00022324(param_1[0xc]);
    FUN_000227fc(param_1[0xc]);
    param_1[0xc] = 0;
  }
  if (param_1[0x37] != 0) {
    FUN_000225d8(param_1[0x37]);
    FUN_00022588(param_1[0x37]);
    FUN_000227fc(param_1[0x37]);
    param_1[0x37] = 0;
  }
  return iVar1;
}



void FUN_0001e19c(int param_1,undefined8 param_2)

{
  FUN_00029b08(param_2,param_1 + 0x30,0x48);
  return;
}



void FUN_0001e1cc(int param_1,undefined1 param_2,undefined8 param_3,undefined4 param_4,
                 undefined8 param_5,undefined1 param_6)

{
  *(undefined1 *)(param_1 + 0x8e) = param_2;
  *(undefined1 *)(param_1 + 0x8f) = param_6;
  FUN_00029b08(param_1 + 0x90,param_5,0x10);
  FUN_00029b08(param_1 + 0xa0,param_4,0x10);
  return;
}



undefined4 FUN_0001e234(int param_1,undefined4 param_2,uint param_3,undefined4 param_4)

{
  undefined4 uVar1;
  int iVar2;
  
  uVar1 = 0;
  if (*(char *)(param_1 + 0x8e) != '\0') {
    iVar2 = *(int *)(param_1 + 0xa0);
    *(uint *)(param_1 + 0xa0) = iVar2 + 0x11U ^ param_3;
    uVar1 = FUN_0002359c(param_2,param_2,0x10,param_1 + 0xa0,param_1 + 0x90);
    *(int *)(param_1 + 0xa0) = iVar2;
    iVar2 = FUN_00029de0(param_2,param_4,6);
    if (iVar2 == 0) {
      FUN_00029b08(param_1 + 0x88,param_4,6);
    }
    else {
      uVar1 = 0x80029843;
    }
  }
  return uVar1;
}



void FUN_0001e314(undefined4 *param_1)

{
  FUN_000224d4(*param_1,4);
  FUN_0001c3b4(param_1);
  FUN_0002231c(*param_1,0,2);
  return;
}



undefined4 FUN_0001e360(undefined4 *param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uVar1;
  
  FUN_0001b794(param_1);
  if (param_1[10] == -1) {
    param_1[10] = param_3;
    FUN_0001b878(param_1);
    FUN_00029b08(param_1 + 0xc,param_2,0x48);
    FUN_000224d4(*param_1,1);
    uVar1 = 0;
  }
  else {
    FUN_0001b878(param_1);
    uVar1 = 0x80029802;
  }
  return uVar1;
}



undefined4 FUN_0001e40c(int *param_1)

{
  undefined4 uVar1;
  undefined1 auStack_30 [16];
  
  FUN_000224d4(*param_1,0x100);
  uVar1 = FUN_0001c3b4(param_1);
  syscall();
  FUN_00029d00(param_1 + 4,auStack_30);
  if (param_1[0x20] != 0) {
    FUN_000227fc(param_1[0x20]);
    param_1[0x20] = 0;
  }
  if (param_1[0x21] != 0) {
    FUN_000227fc(param_1[0x21]);
    param_1[0x21] = 0;
  }
  if (param_1[2] != 0) {
    FUN_000225d8(param_1[2]);
    FUN_00022588(param_1[2]);
    FUN_000227fc(param_1[2]);
    param_1[2] = 0;
  }
  if (*param_1 != 0) {
    FUN_00022324(*param_1);
    FUN_000227fc(*param_1);
    *param_1 = 0;
  }
  return uVar1;
}



int FUN_0001e508(int *param_1,undefined4 param_2,int param_3)

{
  int iVar1;
  int iVar2;
  undefined1 local_40 [16];
  
  FUN_00029ad0(param_1,0,0xb0);
  param_1[10] = -1;
  param_1[0xb] = param_3 + 6;
  iVar1 = FUN_0002285c(0x100);
  param_1[0x20] = iVar1;
  iVar1 = FUN_00029948(param_1 + 4,local_40);
  if (iVar1 == 0) {
    if (param_1[0x20] == 0) {
      iVar1 = -0x7ffd67ff;
    }
    else {
      iVar2 = FUN_0002285c(0x110);
      iVar1 = -0x7ffd67ff;
      param_1[0x21] = iVar2;
      if (iVar2 != 0) {
        iVar2 = FUN_0002285c(0x30);
        iVar1 = -0x7ffd67ff;
        *param_1 = iVar2;
        if ((iVar2 != 0) && (iVar1 = FUN_0002237c(iVar2), -1 < iVar1)) {
          iVar2 = FUN_0002285c(8);
          iVar1 = -0x7ffd67ff;
          param_1[2] = iVar2;
          if ((iVar2 != 0) &&
             ((iVar1 = FUN_0002265c(iVar2), -1 < iVar1 &&
              (iVar1 = FUN_000225ac(param_1[2],param_2), -1 < iVar1)))) {
            iVar1 = FUN_000298a0(param_1 + 0x1e,&PTR_LAB_0002eba0,param_1,param_1[0xb],0x1000,1,
                                 "cellPremoCtrlSessionThread");
            return iVar1;
          }
        }
      }
    }
  }
  FUN_00029d00(param_1 + 4);
  if (param_1[0x20] != 0) {
    FUN_000227fc(param_1[0x20]);
    param_1[0x20] = 0;
  }
  if (param_1[0x21] != 0) {
    FUN_000227fc(param_1[0x21]);
    param_1[0x21] = 0;
  }
  if (param_1[2] != 0) {
    FUN_000225d8(param_1[2]);
    FUN_00022588(param_1[2]);
    FUN_000227fc(param_1[2]);
    param_1[2] = 0;
  }
  if (*param_1 != 0) {
    FUN_00022324(*param_1);
    FUN_000227fc(*param_1);
    *param_1 = 0;
  }
  return iVar1;
}



undefined4 FUN_0001e758(int param_1,int param_2)

{
  undefined4 uVar1;
  undefined4 uVar2;
  undefined4 uVar3;
  char *pcVar4;
  char *pcVar5;
  char *pcVar6;
  char *pcVar7;
  
  FUN_00029ad0(*(undefined4 *)(param_1 + 0x80),0,0x100);
  pcVar4 = s_HTTP_1_1_200_OK_0002e630;
  pcVar5 = s_Connection__close_0002e648;
  pcVar6 = s_Pragma__no_cache_0002e660;
  pcVar7 = s_Content_Length__0_0002e678;
  if (param_2 != 0) {
    pcVar4 = s_HTTP_1_1_403_Forbidden_0002e690;
    pcVar5 = s_Connection__close_0002e6b0;
    pcVar6 = s_Pragma__no_cache_0002e6c8;
    pcVar7 = s_Content_Length__0_0002e6e0;
  }
  FUN_00029bb0(*(undefined4 *)(param_1 + 0x80),"%s%s%s%s\r\n",pcVar4,pcVar5,pcVar6,pcVar7);
  uVar3 = *(undefined4 *)(param_1 + 0x80);
  uVar1 = *(undefined4 *)(param_1 + 0x28);
  uVar2 = FUN_00029910(uVar3);
  uVar3 = FUN_000294b0(uVar1,uVar3,uVar2,0);
  return uVar3;
}



void FUN_0001e82c(int param_1,undefined4 param_2)

{
  int iVar1;
  
  for (iVar1 = 0; iVar1 < *(int *)(param_1 + 0x18); iVar1 = iVar1 + 1) {
    FUN_0001ee48(iVar1 * 0x60 + *(int *)(param_1 + 0x1c),param_2);
  }
  return;
}



void FUN_0001e898(int param_1,uint param_2)

{
  if (((param_2 != 0) && (param_2 != *(ushort *)(param_1 + 0x44))) &&
     (param_2 != *(ushort *)(param_1 + 0x40))) {
    *(short *)(param_1 + 0x42) = (short)param_2;
    FUN_00029328(*(undefined4 *)(param_1 + 0x3c),0);
  }
  return;
}



undefined4 FUN_0001e8f0(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000224d4(*(undefined4 *)(param_1 + 8),2);
  if (-1 < *(int *)(param_1 + 0x3c)) {
    FUN_00029328(*(int *)(param_1 + 0x3c),0);
  }
  FUN_00029328(*(undefined4 *)(param_1 + 0xc),0);
  return uVar1;
}



undefined4 FUN_0001e95c(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000224d4(*(undefined4 *)(param_1 + 8),1);
  return uVar1;
}



undefined4 FUN_0001e988(int param_1)

{
  undefined4 uVar1;
  int iVar2;
  
  FUN_000224d4(*(undefined4 *)(param_1 + 8),0x10);
  uVar1 = 0;
  if (-1 < *(int *)(param_1 + 0x3c)) {
    uVar1 = FUN_0001bb74(*(int *)(param_1 + 0x3c));
    *(undefined4 *)(param_1 + 0x3c) = 0xffffffff;
  }
  if (-1 < *(int *)(param_1 + 0xc)) {
    uVar1 = FUN_0001bb74(*(int *)(param_1 + 0xc));
    *(undefined4 *)(param_1 + 0xc) = 0xffffffff;
  }
  syscall();
  iVar2 = 0;
  if (*(int *)(param_1 + 0x1c) != 0) {
    for (; iVar2 < *(int *)(param_1 + 0x18); iVar2 = iVar2 + 1) {
      uVar1 = FUN_0001ef68(iVar2 * 0x60 + *(int *)(param_1 + 0x1c));
    }
    FUN_000227fc(*(int *)(param_1 + 0x1c));
    *(undefined4 *)(param_1 + 0x1c) = 0;
  }
  if (*(int *)(param_1 + 8) != 0) {
    uVar1 = FUN_00022324(*(int *)(param_1 + 8));
    FUN_000227fc(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0;
  }
  if (*(int *)(param_1 + 0x24) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x24));
    uVar1 = FUN_00022588(*(undefined4 *)(param_1 + 0x24));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x24));
    *(undefined4 *)(param_1 + 0x24) = 0;
  }
  return uVar1;
}



int FUN_0001eac8(int param_1,undefined4 param_2,undefined4 param_3,undefined2 param_4,int param_5,
                int param_6,undefined4 param_7,undefined4 param_8)

{
  int iVar1;
  int iVar2;
  undefined1 auStack_70 [8];
  undefined1 auStack_68 [16];
  
  FUN_000227b4(auStack_70);
  FUN_00029ad0(param_1,0,0x48);
  *(undefined4 *)(param_1 + 0x10) = param_2;
  *(undefined2 *)(param_1 + 0x44) = param_4;
  *(int *)(param_1 + 0x14) = param_5 + 9;
  *(int *)(param_1 + 0x18) = param_6;
  *(undefined4 *)(param_1 + 0x20) = param_3;
  *(undefined4 *)(param_1 + 0x38) = param_7;
  *(undefined4 *)(param_1 + 0x3c) = 0xffffffff;
  *(undefined2 *)(param_1 + 0x42) = 0;
  *(undefined4 *)(param_1 + 0xc) = 0xffffffff;
  *(undefined2 *)(param_1 + 0x40) = 0;
  iVar2 = 0;
  iVar1 = FUN_0002285c(param_6 * 0x60);
  *(int *)(param_1 + 0x1c) = iVar1;
  if (iVar1 == 0) {
    iVar2 = 0;
  }
  else {
    for (; iVar2 < param_6; iVar2 = iVar2 + 1) {
      FUN_0001f090(iVar2 * 0x60 + *(int *)(param_1 + 0x1c),param_2,param_5,param_7,param_8);
    }
    iVar1 = FUN_0002285c(0x30);
    iVar2 = -0x7ffd67ff;
    *(int *)(param_1 + 8) = iVar1;
    if ((iVar1 != 0) && (iVar2 = FUN_0002237c(iVar1), -1 < iVar2)) {
      iVar1 = FUN_0002285c(8);
      iVar2 = -0x7ffd67ff;
      *(int *)(param_1 + 0x24) = iVar1;
      if ((iVar1 != 0) &&
         ((iVar2 = FUN_0002265c(iVar1), -1 < iVar2 &&
          (iVar2 = FUN_000225ac(*(undefined4 *)(param_1 + 0x24),param_3), -1 < iVar2)))) {
        iVar2 = FUN_00029398(2,1,0);
        *(int *)(param_1 + 0xc) = iVar2;
        if (-1 < iVar2) {
          FUN_00029ad0(param_1 + 0x28,0,0x10);
          *(undefined1 *)(param_1 + 0x29) = 2;
          *(undefined2 *)(param_1 + 0x2a) = *(undefined2 *)(param_1 + 0x44);
          *(undefined4 *)(param_1 + 0x2c) = 0;
          iVar2 = FUN_00029440(*(undefined4 *)(param_1 + 0xc),param_1 + 0x28,0x10);
          if ((-1 < iVar2) && (iVar2 = FUN_00029168(*(undefined4 *)(param_1 + 0xc),5), -1 < iVar2))
          {
            iVar2 = FUN_00029398(2,1,0);
            *(int *)(param_1 + 0x3c) = iVar2;
            if (iVar2 < 0) goto LAB_0001ed54;
            iVar2 = FUN_000298a0(param_1,&PTR_LAB_0002eba8,param_1,*(undefined4 *)(param_1 + 0x14),
                                 0x1000,1,"cellPremoHttpdThread");
            if (-1 < iVar2) {
              FUN_000227b4(auStack_68);
              return iVar2;
            }
          }
        }
      }
    }
  }
  if (-1 < *(int *)(param_1 + 0x3c)) {
    FUN_0001bb74(*(int *)(param_1 + 0x3c));
    *(undefined4 *)(param_1 + 0x3c) = 0xffffffff;
  }
LAB_0001ed54:
  if (-1 < *(int *)(param_1 + 0xc)) {
    FUN_0001bb74(*(int *)(param_1 + 0xc));
    *(undefined4 *)(param_1 + 0xc) = 0xffffffff;
  }
  if (*(int *)(param_1 + 0x24) != 0) {
    FUN_000225d8(*(int *)(param_1 + 0x24));
    FUN_00022588(*(undefined4 *)(param_1 + 0x24));
    FUN_000227fc(*(undefined4 *)(param_1 + 0x24));
    *(undefined4 *)(param_1 + 0x24) = 0;
  }
  if (*(int *)(param_1 + 8) != 0) {
    FUN_00022324(*(int *)(param_1 + 8));
    FUN_000227fc(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0;
  }
  iVar1 = 0;
  if (*(int *)(param_1 + 0x1c) != 0) {
    for (; iVar1 < *(int *)(param_1 + 0x18); iVar1 = iVar1 + 1) {
      FUN_0001ef68(iVar1 * 0x60 + *(int *)(param_1 + 0x1c));
    }
    FUN_000227fc(*(int *)(param_1 + 0x1c));
    *(undefined4 *)(param_1 + 0x1c) = 0;
  }
  return iVar2;
}



void FUN_0001ee48(int param_1,undefined4 param_2)

{
  *(undefined4 *)(param_1 + 0x44) = param_2;
  return;
}



int FUN_0001ee50(char *param_1,int param_2,undefined4 *param_3)

{
  char cVar1;
  bool bVar2;
  int iVar3;
  
  iVar3 = 0;
  bVar2 = true;
  do {
    cVar1 = *param_1;
    if (cVar1 == '\0') {
LAB_0001eeb8:
      if (iVar3 == 0) {
        iVar3 = 1;
        *param_3 = param_1;
      }
      return iVar3;
    }
    if (bVar2) {
      if ((cVar1 != ' ') && (cVar1 != '\t')) {
        if (iVar3 < param_2) {
          *param_3 = param_1;
          iVar3 = iVar3 + 1;
          param_3 = param_3 + 1;
          goto LAB_0001ee84;
        }
        goto LAB_0001eeb8;
      }
    }
    else {
LAB_0001ee84:
      bVar2 = false;
      if (cVar1 == ',') {
        bVar2 = true;
        *param_1 = '\0';
      }
    }
    param_1 = param_1 + 1;
  } while( true );
}



undefined4 FUN_0001eed0(int param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)

{
  undefined4 uVar1;
  
  *(undefined4 *)(param_1 + 0x2c) = param_2;
  *(undefined4 *)(param_1 + 0x3c) = param_3;
  *(undefined4 *)(param_1 + 0x40) = param_4;
  uVar1 = FUN_000224d4(*(undefined4 *)(param_1 + 8),1);
  return uVar1;
}



undefined8 FUN_0001ef0c(int param_1)

{
  if (-1 < param_1) {
    FUN_00029328(param_1,0);
  }
  return 0;
}



undefined4 FUN_0001ef40(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000220a4(*(undefined4 *)(param_1 + 8));
  return uVar1;
}



undefined4 FUN_0001ef68(int param_1)

{
  int iVar1;
  undefined4 uVar2;
  
  uVar2 = FUN_000224d4(*(undefined4 *)(param_1 + 8),0x10);
  FUN_0001b7c4(param_1);
  if (-1 < *(int *)(param_1 + 0x2c)) {
    FUN_0001ef0c(*(int *)(param_1 + 0x2c));
  }
  FUN_0001b8a4(param_1);
  syscall();
  iVar1 = *(int *)(param_1 + 0x2c);
  if (-1 < iVar1) {
    FUN_000293d0(iVar1,2);
    uVar2 = FUN_00029248(iVar1);
    *(undefined4 *)(param_1 + 0x2c) = 0xffffffff;
  }
  FUN_00029d00(param_1 + 0x10);
  if (*(int *)(param_1 + 8) != 0) {
    FUN_00022324(*(int *)(param_1 + 8));
    FUN_000227fc(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0;
  }
  if (*(int *)(param_1 + 0x30) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x30));
    *(undefined4 *)(param_1 + 0x30) = 0;
  }
  if (*(int *)(param_1 + 0x34) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x34));
    *(undefined4 *)(param_1 + 0x34) = 0;
  }
  return uVar2;
}



int FUN_0001f090(int param_1,undefined4 param_2,int param_3,undefined4 param_4,int param_5)

{
  bool bVar1;
  undefined4 uVar2;
  undefined4 *puVar3;
  int iVar4;
  int iVar5;
  int iVar6;
  undefined1 local_40 [16];
  
  FUN_00029ad0(param_1,0,0x60);
  *(undefined4 *)(param_1 + 0x38) = param_4;
  *(undefined4 *)(param_1 + 0x2c) = 0xffffffff;
  uVar2 = *(undefined4 *)(param_5 + 0x18);
  iVar4 = 0;
  iVar5 = 0;
  *(undefined4 *)(param_1 + 0xc) = param_2;
  *(undefined4 *)(param_1 + 0x48) = uVar2;
  while( true ) {
    puVar3 = (undefined4 *)(iVar5 + param_5);
    iVar6 = param_1 + iVar5;
    bVar1 = *(int *)(param_5 + 0x18) <= iVar4;
    iVar5 = iVar5 + 4;
    iVar4 = iVar4 + 1;
    if (bVar1) break;
    *(undefined4 *)(iVar6 + 0x4c) = *puVar3;
    *(undefined4 *)(iVar6 + 0x54) = puVar3[2];
  }
  iVar4 = FUN_0002285c(0x200);
  *(int *)(param_1 + 0x30) = iVar4;
  if (iVar4 == 0) {
    iVar4 = -0x7ffd67ff;
  }
  else {
    iVar5 = FUN_0002285c(0x110);
    iVar4 = -0x7ffd67ff;
    *(int *)(param_1 + 0x34) = iVar5;
    if ((iVar5 != 0) && (iVar4 = FUN_00029948(param_1 + 0x10,local_40), iVar4 == 0)) {
      iVar5 = FUN_0002285c(0x30);
      iVar4 = -0x7ffd67ff;
      *(int *)(param_1 + 8) = iVar5;
      if ((iVar5 != 0) && (iVar4 = FUN_0002237c(iVar5), -1 < iVar4)) {
        iVar4 = FUN_000298a0(param_1,&PTR_LAB_0002ebb0,param_1,param_3 + 10,0x1800,1,
                             "cellPremoAcceptThread");
        return iVar4;
      }
    }
  }
  FUN_00029d00(param_1 + 0x10);
  if (*(int *)(param_1 + 8) != 0) {
    FUN_00022324(*(int *)(param_1 + 8));
    FUN_000227fc(*(undefined4 *)(param_1 + 8));
    *(undefined4 *)(param_1 + 8) = 0;
  }
  if (*(int *)(param_1 + 0x30) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x30));
    *(undefined4 *)(param_1 + 0x30) = 0;
  }
  if (*(int *)(param_1 + 0x34) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x34));
    *(undefined4 *)(param_1 + 0x34) = 0;
  }
  return iVar4;
}



undefined4 FUN_0001f2c0(int param_1)

{
  undefined4 uVar1;
  undefined1 local_30 [16];
  
  uVar1 = 0;
  if (-1 < param_1) {
    FUN_000292f0(param_1,0xffff,&DAT_00001006,local_30,0x10);
    uVar1 = FUN_000292f0(param_1,0xffff,&DAT_00001005,local_30,0x10);
  }
  return uVar1;
}



undefined8 FUN_0001f37c(undefined8 param_1,undefined8 *param_2)

{
  undefined8 uVar1;
  undefined1 auStack_30 [24];
  
  uVar1 = FUN_00022e2c(param_1,auStack_30,10);
  *param_2 = uVar1;
  return 0;
}



// WARNING: Removing unreachable block (ram,0x0001f490)

int FUN_0001f3b8(int param_1)

{
  char cVar1;
  int iVar2;
  int iVar3;
  uint uVar4;
  undefined4 uVar5;
  undefined4 local_c0;
  undefined1 local_bc [4];
  undefined1 local_b8 [4];
  undefined1 local_b4 [4];
  undefined1 local_b0 [8];
  undefined4 local_a8;
  undefined4 local_a4;
  undefined1 auStack_a0 [16];
  char local_90 [28];
  int local_74;
  undefined1 auStack_60 [16];
  undefined1 auStack_50 [24];
  
  FUN_00027ae0(local_90,0,0x48);
  while( true ) {
    FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
    iVar2 = FUN_00020a50(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),0x200);
    if (iVar2 == 0) {
      if ((local_90[0] == '\x03') && (local_74 == 0)) {
        FUN_000294e8(*(undefined4 *)(param_1 + 0x2c),auStack_a0,local_bc);
      }
      FUN_0001b7c4(param_1);
      FUN_0001f2c0(*(undefined4 *)(param_1 + 0x2c),0,0x1e);
      iVar2 = FUN_0000e32c(*(undefined4 *)(param_1 + 0xc),local_90,*(undefined4 *)(param_1 + 0x2c));
      FUN_0001b8a4(param_1);
      return iVar2;
    }
    if (iVar2 < 0) {
      return iVar2;
    }
    iVar3 = FUN_00029590(&local_a8,*(undefined4 *)(param_1 + 0x30),iVar2,0,0,local_b8,local_b4);
    if (iVar3 < 0) {
      return iVar3;
    }
    iVar2 = FUN_00029590(&local_a8,*(undefined4 *)(param_1 + 0x30),iVar2,
                         *(undefined4 *)(param_1 + 0x34),0,local_b8,local_b4);
    if (iVar2 < 0) break;
    iVar2 = FUN_00027d48(local_a8,"SessionID");
    if (iVar2 == 0) {
      FUN_0001f37c(local_a4,local_b0);
    }
    else {
      iVar2 = FUN_00027d48(local_a8,&DAT_0002c850);
      if (iVar2 == 0) {
        iVar2 = FUN_00029a60(local_a4,"change-bitrate");
        cVar1 = '\x01';
        if (iVar2 != 0) {
          iVar2 = FUN_00029a60(local_a4,"session-term");
          cVar1 = '\x02';
          if ((iVar2 != 0) &&
             (iVar2 = FUN_00029a60(local_a4,"av-start"), cVar1 = local_90[0], iVar2 == 0)) {
            cVar1 = '\x03';
          }
        }
        local_90[0] = cVar1;
        FUN_00029b08(auStack_50,local_a4,4);
      }
      else {
        iVar2 = FUN_00027d48(local_a8,"Bitrate");
        if (iVar2 == 0) {
          FUN_00022e24(local_a4,&local_c0,10);
        }
        else {
          iVar2 = FUN_00027d48(local_a8,"Max-Bitrate");
          if (iVar2 == 0) {
            FUN_00022e24(local_a4,&local_c0,10);
          }
          else {
            iVar2 = FUN_00027d48(local_a8,"Audio-Nagle");
            if (iVar2 == 0) {
              FUN_00029a60(local_a4,&DAT_0002c8b0);
            }
            else {
              iVar2 = FUN_00027d48(local_a8,"PREMO-Auth");
              if (iVar2 == 0) {
                uVar4 = FUN_00029910(local_a4);
                if (0x18 < uVar4) {
                  return -0x7ffd67e0;
                }
                uVar5 = FUN_00029910(local_a4);
                FUN_00022a88(local_a4,auStack_60,uVar5);
              }
              else {
                iVar2 = FUN_00027d48(local_a8,"PREMO-Video-Port");
                if (iVar2 == 0) {
                  FUN_00022e24(local_a4,&local_c0,10);
                }
                else {
                  iVar2 = FUN_00027d48(local_a8,"PREMO-Audio-Port");
                  if (iVar2 == 0) {
                    FUN_00022e24(local_a4,&local_c0,10);
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  return iVar2;
}



// WARNING: Removing unreachable block (ram,0x000209d4)

uint FUN_0001f7c0(int param_1)

{
  uint uVar1;
  undefined4 *puVar2;
  char cVar3;
  uint uVar4;
  int iVar5;
  uint uVar6;
  int iVar7;
  char *pcVar8;
  int iVar9;
  undefined4 uVar10;
  undefined8 uVar11;
  ulonglong uVar12;
  uint *puVar13;
  int iVar14;
  int iVar15;
  ulonglong uVar16;
  undefined1 uVar17;
  int iVar18;
  undefined1 *puVar19;
  uint local_2e0;
  undefined1 local_2dc [4];
  undefined1 local_2d8 [4];
  char *local_2d4;
  undefined1 local_2d0 [4];
  char *local_2cc;
  undefined1 auStack_2c8 [8];
  undefined4 local_2c0;
  undefined4 local_2bc;
  undefined1 local_2b8 [8];
  undefined4 auStack_2b0 [4];
  undefined1 auStack_2a0 [4];
  uint local_29c;
  uint local_290 [3];
  undefined1 auStack_284 [4];
  undefined4 uStack_280;
  undefined1 local_278 [8];
  undefined4 local_270;
  undefined4 local_26c;
  undefined1 local_268;
  undefined1 local_260 [8];
  undefined1 auStack_258 [16];
  undefined4 local_248;
  undefined1 local_238 [8];
  undefined1 auStack_230 [48];
  undefined1 auStack_200 [16];
  uint local_1f0;
  undefined4 local_1ec;
  undefined1 auStack_1e0 [255];
  char local_e1 [65];
  undefined4 local_a0;
  undefined1 local_9c;
  char local_8b;
  byte local_8a;
  undefined1 local_89;
  undefined2 local_88;
  undefined2 local_86;
  undefined4 local_84;
  undefined4 local_80;
  undefined1 local_7c;
  undefined1 local_7a;
  byte local_79;
  undefined1 local_77;
  undefined4 local_74;
  undefined4 local_70;
  
  uVar4 = FUN_0000e2bc(*(undefined4 *)(param_1 + 0xc),&local_2e0);
  if ((uVar4 == 0) && (uVar4 = local_2e0, local_2e0 == 1)) {
    FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
    uVar4 = FUN_00020a50(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),0x200);
    if (0 < (int)uVar4) {
      iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),"GET /sce/premo/session HTTP/1.1\n",uVar4
                          );
      if ((iVar5 == 0) ||
         (iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),"GET /sce/premo/session HTTP/1.0\n",
                               uVar4), iVar5 == 0)) {
        FUN_00029ad0(auStack_200,0,0x194);
        local_88 = 0x1e0;
        local_86 = 0x110;
        uVar16 = 1;
        iVar18 = 0;
        iVar15 = -1;
        local_84 = 0x7d000;
        local_80 = 0x1e;
        local_89 = 1;
        iVar5 = -1;
        while( true ) {
          uVar17 = (undefined1)uVar16;
          FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
          uVar6 = FUN_00020a50(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),0x200
                              );
          if (uVar6 == 0) break;
          uVar4 = uVar6;
          if ((int)uVar6 < 0) goto LAB_00020924;
          uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,0,0,local_2dc,
                               local_2d0);
          if (((int)uVar4 < 0) ||
             (uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,
                                   *(undefined4 *)(param_1 + 0x34),0,local_2dc,local_2d0),
             (int)uVar4 < 0)) goto LAB_00020924;
          iVar7 = FUN_00027d48(local_2c0,"PREMO-PSPID");
          iVar14 = iVar5;
          if (iVar7 == 0) {
            uVar4 = FUN_00029910(local_2bc);
            if (0x18 < uVar4) goto LAB_000209c8;
            puVar19 = auStack_200;
            uVar11 = 0x10;
LAB_0001fba8:
            FUN_00029ad0(puVar19,0,uVar11);
            uVar10 = FUN_00029910(local_2bc);
            uVar4 = FUN_00022a88(local_2bc,puVar19,uVar10);
            uVar6 = local_1f0;
            cVar3 = local_8b;
            if ((int)uVar4 < 0) goto LAB_00020924;
          }
          else {
            iVar7 = FUN_00027d48(local_2c0,"PREMO-Version");
            if (iVar7 == 0) {
              iVar5 = FUN_00022e24(local_2bc,&local_2d4,10);
              pcVar8 = local_2d4 + 1;
              uVar6 = 0;
              if (*local_2d4 == '.') {
                uVar6 = FUN_00022e24(local_2d4 + 1,0,10);
              }
              if ((iVar5 != 0) ||
                 (((local_2d4 = pcVar8, cVar3 = local_8b, *(uint *)(param_1 + 0x38) != 1 ||
                   (uVar6 != 0)) && (uVar6 < *(uint *)(param_1 + 0x38))))) {
                uVar4 = 0x80029850;
                goto LAB_00020924;
              }
            }
            else {
              iVar7 = FUN_00027d48(local_2c0,"PREMO-Mode");
              if (iVar7 == 0) {
                iVar5 = FUN_00029a60(local_2bc,"PREMO");
                if (iVar5 == 0) {
                  local_1ec = 0;
                  uVar6 = local_1f0;
                  cVar3 = local_8b;
                }
                else {
                  iVar5 = FUN_00029a60(local_2bc,"REMOCON");
                  if (iVar5 != 0) goto LAB_000209c8;
                  local_1ec = 1;
                  uVar6 = local_1f0;
                  cVar3 = local_8b;
                }
              }
              else {
                iVar7 = FUN_00027d48(local_2c0,"PREMO-Platform-Info");
                if (iVar7 == 0) {
                  iVar5 = FUN_00029a60(local_2bc,&DAT_0002c9a0);
                  if (iVar5 == 0) {
                    local_8b = '\0';
                    uVar6 = local_1f0;
                    cVar3 = local_8b;
                  }
                  else {
                    iVar5 = FUN_00029a60(local_2bc,"Phone");
                    uVar6 = local_1f0;
                    cVar3 = '\x01';
                    if (iVar5 != 0) {
                      iVar5 = FUN_00029a60(local_2bc,&DAT_0002c9b0);
                      if (iVar5 == 0) {
                        uVar6 = local_1f0;
                        cVar3 = '\x02';
                      }
                      else {
                        iVar5 = FUN_00029a60(local_2bc,&DAT_0002c9b8);
                        if (iVar5 != 0) goto LAB_000209c8;
                        uVar6 = local_1f0;
                        cVar3 = '\x03';
                      }
                    }
                  }
                }
                else {
                  iVar7 = FUN_00027d48(local_2c0,"PREMO-UserName");
                  if (iVar7 == 0) {
                    uVar4 = FUN_00029910(local_2bc);
                    if (uVar4 < 0x155) {
                      puVar19 = auStack_1e0;
                      uVar11 = 0xff;
                      goto LAB_0001fba8;
                    }
                    goto LAB_000209c8;
                  }
                  iVar7 = FUN_00027d48(local_2c0,"PREMO-Trans");
                  if (iVar7 == 0) {
                    iVar5 = FUN_00029a60(local_2bc,"capable");
                    local_9c = 2;
                    uVar6 = local_1f0;
                    cVar3 = local_8b;
                    if (iVar5 != 0) {
                      local_9c = 1;
                    }
                  }
                  else {
                    iVar7 = FUN_00027d48(local_2c0,"PREMO-SIGNIN-ID");
                    if (iVar7 == 0) {
                      uVar4 = FUN_00029910(local_2bc);
                      if (0x58 < uVar4) goto LAB_000209c8;
                      FUN_00029ad0(local_e1,0,0x40);
                      uVar10 = FUN_00029910(local_2bc);
                      uVar4 = FUN_00022a88(local_2bc,local_e1,uVar10);
                      if ((int)uVar4 < 0) goto LAB_00020924;
                      local_a0 = 1;
                      uVar6 = local_1f0;
                      cVar3 = local_8b;
                    }
                    else {
                      iVar7 = FUN_00027d48(local_2c0,"PREMO-Video-Codec-Ability");
                      if (iVar7 == 0) {
                        local_8a = 0;
                        iVar5 = FUN_0001ee50(*(int *)(param_1 + 0x30) + 0x1a,4,auStack_2b0);
                        iVar7 = 0;
                        for (iVar18 = 0; uVar6 = local_1f0, cVar3 = local_8b, iVar18 < iVar5;
                            iVar18 = iVar18 + 1) {
                          puVar2 = (undefined4 *)((int)auStack_2b0 + iVar7);
                          iVar9 = FUN_00029a60(*puVar2,&DAT_0002ca18);
                          if (iVar9 == 0) {
                            local_8a = local_8a | 1;
LAB_0001fd8c:
                          }
                          else {
                            iVar9 = FUN_00029a60(*puVar2,"AVC/CAVLC");
                            if (iVar9 == 0) {
                              local_8a = local_8a | 2;
                              goto LAB_0001fd8c;
                            }
                            iVar9 = FUN_00029a60(*puVar2,&DAT_0002ca30);
                            if ((iVar9 == 0) || (iVar9 = FUN_00029a60(*puVar2,"AVC/MP"), iVar9 == 0)
                               ) {
                              local_8a = local_8a | 4;
                              goto LAB_0001fd8c;
                            }
                            iVar9 = FUN_00029a60(*puVar2,&DAT_0002ca40);
                            if (iVar9 == 0) {
                              local_8a = local_8a | 8;
                              goto LAB_0001fd8c;
                            }
                          }
                          iVar7 = iVar7 + 4;
                        }
                      }
                      else {
                        iVar7 = FUN_00027d48(local_2c0,"PREMO-Video-Resolution");
                        if ((iVar7 == 0) ||
                           (iVar7 = FUN_00027d48(local_2c0,"PREMO-Video-Resolution-Ability"),
                           iVar7 == 0)) {
                          local_88 = FUN_00022e24(local_2bc,&local_2cc,10);
                          if (*local_2cc != 'x') goto LAB_000209f4;
                          local_86 = FUN_00022e24(local_2cc + 1,&local_2d4,10);
                          local_2cc = local_2cc + 1;
                          uVar6 = local_1f0;
                          cVar3 = local_8b;
                        }
                        else {
                          iVar7 = FUN_00027d48(local_2c0,"PREMO-Video-Bitrate");
                          if (iVar7 == 0) {
                            local_84 = FUN_00022e24(local_2bc,&local_2d4,10);
                            uVar6 = local_1f0;
                            cVar3 = local_8b;
                          }
                          else {
                            iVar7 = FUN_00027d48(local_2c0,"PREMO-Video-Framerate");
                            if (iVar7 == 0) {
                              local_80 = FUN_00022e24(local_2bc,&local_2d4,10);
                              uVar6 = local_1f0;
                              cVar3 = local_8b;
                            }
                            else {
                              iVar7 = FUN_00027d48(local_2c0,"PREMO-Video-LetterBox");
                              if (iVar7 == 0) {
                                iVar5 = FUN_00029a60(local_2bc,&DAT_0002c8b0);
                                uVar6 = local_1f0;
                                cVar3 = local_8b;
                                if (iVar5 == 0) {
                                  local_7c = 1;
                                }
                                else {
                                  local_7c = 0;
                                }
                              }
                              else {
                                iVar7 = FUN_00027d48(local_2c0,"PREMO-Audio-Codec-Ability");
                                if (iVar7 == 0) {
                                  local_79 = 0;
                                  iVar5 = FUN_0001ee50(*(int *)(param_1 + 0x30) + 0x1a,4,auStack_2b0
                                                      );
                                  iVar7 = 0;
                                  for (iVar18 = 0; uVar6 = local_1f0, cVar3 = local_8b,
                                      iVar18 < iVar5; iVar18 = iVar18 + 1) {
                                    iVar9 = FUN_00029a60(*(undefined4 *)((int)auStack_2b0 + iVar7),
                                                         &DAT_0002cae8);
                                    if (iVar9 == 0) {
                                      local_79 = local_79 | 2;
LAB_0001ff60:
                                    }
                                    else {
                                      iVar9 = FUN_00029a60(*(undefined4 *)((int)auStack_2b0 + iVar7)
                                                           ,"ATRAC");
                                      if (iVar9 == 0) {
                                        local_79 = local_79 | 1;
                                        goto LAB_0001ff60;
                                      }
                                    }
                                    iVar7 = iVar7 + 4;
                                  }
                                }
                                else {
                                  iVar7 = FUN_00027d48(local_2c0,"PREMO-Audio-Bitrate");
                                  if (iVar7 == 0) {
                                    local_74 = FUN_00022e24(local_2bc,&local_2d4,10);
                                    uVar6 = local_1f0;
                                    cVar3 = local_8b;
                                  }
                                  else {
                                    iVar7 = FUN_00027d48(local_2c0,"PREMO-RTP-Size");
                                    if (iVar7 == 0) {
                                      local_70 = FUN_00022e24(local_2bc,&local_2d4,10);
                                      uVar6 = local_1f0;
                                      cVar3 = local_8b;
                                    }
                                    else {
                                      iVar7 = FUN_00027d48(local_2c0,"PREMO-VideoOut-Ctrl");
                                      if (iVar7 == 0) {
                                        iVar5 = FUN_00029a60(local_2bc,"capable");
                                        uVar6 = local_1f0;
                                        cVar3 = local_8b;
                                        if (iVar5 == 0) {
                                          local_7a = 1;
                                        }
                                        else {
                                          local_7a = 0;
                                        }
                                      }
                                      else {
                                        iVar7 = FUN_00027d48(local_2c0,"PREMO-Touch-Ability");
                                        if (iVar7 == 0) {
                                          iVar5 = FUN_00029a60(local_2bc,"capable");
                                          uVar6 = local_1f0;
                                          cVar3 = local_8b;
                                          if (iVar5 == 0) {
                                            local_77 = 1;
                                          }
                                          else {
                                            local_77 = 0;
                                          }
                                        }
                                        else {
                                          iVar7 = FUN_00027d48(local_2c0,"Accept-Encoding");
                                          if (iVar7 == 0) {
                                            uVar16 = (longlong)
                                                     *(char *)(*(int *)(param_1 + 0x30) + 0x10) ^
                                                     0x20;
                                            uVar12 = (ulonglong)((int)uVar16 >> 0x1f);
                                            uVar16 = (((uVar12 ^ uVar16) - uVar12) + -1 << 0x20) >>
                                                     0x3f;
                                            uVar6 = local_1f0;
                                            cVar3 = local_8b;
                                          }
                                          else {
                                            iVar7 = FUN_00027d48(local_2c0,"User-Agent");
                                            iVar14 = iVar18;
                                            uVar6 = local_1f0;
                                            cVar3 = local_8b;
                                            if ((iVar7 != 0) &&
                                               (iVar7 = FUN_00027d48(local_2c0,&DAT_0002cb70),
                                               iVar14 = iVar5, uVar6 = local_1f0, cVar3 = local_8b,
                                               iVar7 == 0)) {
                                              iVar15 = iVar18;
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          local_8b = cVar3;
          local_1f0 = uVar6;
          iVar18 = iVar18 + 1;
          iVar5 = iVar14;
        }
        FUN_000290c0(*(undefined4 *)(param_1 + 0x2c),auStack_2a0,local_2d8);
        puVar13 = (uint *)(param_1 + 0x4c);
        uVar16 = (ulonglong)*(uint *)(param_1 + 0x48) + 1 & 0xffffffff;
        iVar18 = 0;
        if ((int)*(uint *)(param_1 + 0x48) < 0) {
          uVar16 = 1;
        }
        while (uVar16 = uVar16 - 1, uVar16 != 0) {
          uVar6 = *puVar13;
          puVar13 = puVar13 + 1;
          if (uVar6 == local_29c) {
            uVar1 = *(uint *)(param_1 + iVar18 * 4 + 0x54);
            uVar6 = uVar6 & uVar1;
            uVar1 = *(uint *)(param_1 + 0x3c) & uVar1;
            if ((local_e1[0] == '\0') && (uVar4 = 0x80029820, uVar6 != uVar1)) goto LAB_000209ac;
            if ((iVar5 != 0) || (iVar15 != 4)) {
              uVar17 = 0;
            }
            uStack_280 = 0;
            local_290[0] = *(uint *)(param_1 + 0x3c);
            iVar5 = FUN_00029130(0,0x10,local_290,0x14);
            FUN_00029b08(auStack_2c8,auStack_284,6);
            if (iVar5 != 0) {
              FUN_00029ad0(auStack_2c8,0,6);
            }
            if (local_8b == '\0') {
              if (local_8a == 0) {
                local_8a = 5;
              }
              if (local_79 == 0) {
                local_79 = 3;
              }
            }
            FUN_0001b7c4(param_1);
            FUN_0001f2c0(*(undefined4 *)(param_1 + 0x2c),0,0x1e);
            uVar6 = uVar6 ^ uVar1;
            uVar16 = (ulonglong)((int)uVar6 >> 0x1f);
            uVar4 = FUN_00012c8c(*(undefined4 *)(param_1 + 0xc),auStack_200,
                                 *(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x3c),
                                 ((uVar16 ^ uVar6) - uVar16) - 1 >> 0x1f & 1,auStack_2c8,uVar17);
            goto LAB_000208b4;
          }
          iVar18 = iVar18 + 1;
        }
        uVar4 = 0x80029820;
      }
      else {
        iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                             "GET /sce/premo/session/video HTTP/1.1\n",uVar4);
        if ((iVar5 == 0) ||
           (iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                                 "GET /sce/premo/session/video HTTP/1.0\n",uVar4), iVar5 == 0)) {
          FUN_00029ad0(local_238,0,0x38);
          while( true ) {
            FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
            uVar6 = FUN_00020a50(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),
                                 0x200);
            if (uVar6 == 0) break;
            uVar4 = uVar6;
            if ((((int)uVar6 < 0) ||
                (uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30)), (int)uVar4 < 0))
               || (uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,
                                        *(undefined4 *)(param_1 + 0x34),0,local_2d8,local_2dc),
                  (int)uVar4 < 0)) goto LAB_00020924;
            iVar5 = FUN_00027d48(local_2c0,"SessionID");
            if (iVar5 == 0) {
              FUN_0001f37c(local_2bc,local_2b8);
            }
            else {
              iVar5 = FUN_00027d48(local_2c0,"PREMO-Auth");
              if (iVar5 == 0) {
                uVar4 = FUN_00029910(local_2bc);
                if (0x18 < uVar4) goto LAB_000209c8;
                uVar10 = FUN_00029910(local_2bc);
                FUN_00022a88(local_2bc,auStack_230,uVar10);
              }
            }
          }
          FUN_0001b7c4(param_1);
          FUN_0001f2c0(*(undefined4 *)(param_1 + 0x2c),0,0x1e);
          uVar4 = FUN_0000d4c0(*(undefined4 *)(param_1 + 0xc),local_238,
                               *(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x40));
LAB_000208b4:
          FUN_0001b8a4(param_1);
        }
        else {
          iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                               "GET /sce/premo/session/audio HTTP/1.1\n",uVar4);
          if ((iVar5 == 0) ||
             (iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                                   "GET /sce/premo/session/audio HTTP/1.0\n",uVar4), iVar5 == 0)) {
            FUN_00029ad0(local_260,0,0x28);
            while( true ) {
              FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
              uVar6 = FUN_00020a50(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),
                                   0x200);
              if (uVar6 == 0) break;
              uVar4 = uVar6;
              if ((int)uVar6 < 0) goto LAB_00020924;
              uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,0,0,local_2dc,
                                   local_2d8);
              if (((int)uVar4 < 0) ||
                 (uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,
                                       *(undefined4 *)(param_1 + 0x34),0,local_2dc,local_2d8),
                 (int)uVar4 < 0)) goto LAB_00020924;
              iVar5 = FUN_00027d48(local_2c0,"SessionID");
              if (iVar5 == 0) {
                FUN_0001f37c(local_2bc,local_2b8);
              }
              else {
                iVar5 = FUN_00027d48(local_2c0,"PREMO-Auth");
                if (iVar5 == 0) {
                  uVar4 = FUN_00029910(local_2bc);
                  if (0x18 < uVar4) goto LAB_000209c8;
                  uVar10 = FUN_00029910(local_2bc);
                  FUN_00022a88(local_2bc,auStack_258,uVar10);
                }
                else {
                  iVar5 = FUN_00027d48(local_2c0,"PREMO-Audio-Bitrate");
                  if ((iVar5 == 0) &&
                     (uVar10 = FUN_00022e24(local_2bc,&local_2d4,10), local_2d4 != (char *)0x0)) {
                    local_248 = uVar10;
                  }
                }
              }
            }
            FUN_0001b7c4(param_1);
            FUN_0001f2c0(*(undefined4 *)(param_1 + 0x2c),0,0x1e);
            uVar4 = FUN_0000d378(*(undefined4 *)(param_1 + 0xc),local_260,
                                 *(undefined4 *)(param_1 + 0x2c));
            goto LAB_000208b4;
          }
          iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                               "POST /sce/premo/session/pad HTTP/1.1\n",uVar4);
          if ((iVar5 == 0) ||
             (iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                                   "POST /sce/premo/session/pad HTTP/1.0\n",uVar4), iVar5 == 0)) {
            FUN_00029ad0(local_278,0,0x18);
            local_26c = 1;
            while( true ) {
              FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
              uVar6 = FUN_00020a50(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),
                                   0x200);
              if (uVar6 == 0) break;
              uVar4 = uVar6;
              if ((int)uVar6 < 0) goto LAB_00020924;
              uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,0,0,local_2d8,
                                   local_2dc);
              if (((int)uVar4 < 0) ||
                 (uVar4 = FUN_00029590(&local_2c0,*(undefined4 *)(param_1 + 0x30),uVar6,
                                       *(undefined4 *)(param_1 + 0x34),0,local_2d8,local_2dc),
                 (int)uVar4 < 0)) goto LAB_00020924;
              iVar5 = FUN_00027d48(local_2c0,"SessionID");
              if (iVar5 == 0) {
                FUN_0001f37c(local_2bc,local_2b8);
              }
              else {
                iVar5 = FUN_00027d48(local_2c0,"Content-Length");
                if (iVar5 == 0) {
                  local_270 = FUN_00022c78(local_2bc);
                }
                else {
                  iVar5 = FUN_00027d48(local_2c0,"PREMO-Pad-Index");
                  if (iVar5 == 0) {
                    local_26c = FUN_00022c78(local_2bc);
                  }
                  else {
                    iVar5 = FUN_00027d48(local_2c0,"Transfer-Encoding");
                    if (iVar5 == 0) {
                      iVar5 = FUN_00029a60(local_2bc,"chunked");
                      if (iVar5 == 0) {
                        local_268 = 1;
                      }
                      else {
                        local_268 = 0;
                      }
                    }
                  }
                }
              }
            }
            FUN_0001b7c4(param_1);
            FUN_0001f2c0(*(undefined4 *)(param_1 + 0x2c),0,0x1e);
            uVar4 = FUN_0000e420(*(undefined4 *)(param_1 + 0xc),local_278,
                                 *(undefined4 *)(param_1 + 0x2c));
            goto LAB_000208b4;
          }
          iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                               "GET /sce/premo/session/ctrl HTTP/1.1\n",uVar4);
          if (iVar5 != 0) {
            iVar5 = FUN_00029788(*(undefined4 *)(param_1 + 0x30),
                                 "GET /sce/premo/session/ctrl HTTP/1.0\n",uVar4);
            uVar4 = 0x80029804;
            if (iVar5 != 0) goto LAB_00020924;
          }
          uVar4 = FUN_0001f3b8(param_1);
        }
        if (-1 < (int)uVar4) goto LAB_000209f4;
      }
    }
  }
LAB_00020924:
  FUN_00029ad0(*(undefined4 *)(param_1 + 0x30),0,0x200);
  uVar10 = FUN_000297c0(*(undefined4 *)(param_1 + 0x30),0x200,"%s%s%s%s%s%d.%d\r\n%s%08x\r\n\r\n",
                        "HTTP/1.1 403 Forbidden\r\n","Connection: close\r\n","Pragma: no-cache\r\n",
                        "Content-Length: 0\r\n","PREMO-Version: ");
  FUN_000294b0(*(undefined4 *)(param_1 + 0x2c),*(undefined4 *)(param_1 + 0x30),uVar10,0);
LAB_000209ac:
  FUN_00029248(*(undefined4 *)(param_1 + 0x2c));
LAB_000209f4:
  FUN_0001b7c4(param_1);
  *(undefined4 *)(param_1 + 0x2c) = 0xffffffff;
  FUN_0001b8a4(param_1);
  return uVar4;
LAB_000209c8:
  uVar4 = 0x80029820;
  goto LAB_00020924;
}



int FUN_00020a50(undefined4 param_1,char *param_2,int param_3)

{
  bool bVar1;
  uint uVar2;
  int iVar3;
  uint *puVar4;
  char *pcVar5;
  int iVar6;
  char local_40 [24];
  
  iVar6 = 0;
  pcVar5 = param_2;
  while( true ) {
    iVar3 = FUN_00029520(param_1,local_40,1,0);
    bVar1 = param_3 + -1 <= iVar6;
    if (iVar3 != 1) {
      iVar6 = -0x7ffd67df;
      if (iVar3 != 0) {
        puVar4 = (uint *)FUN_00029210();
        uVar2 = (int)(*puVar4 ^ 0x23) >> 0x1f;
        iVar6 = ((int)(uVar2 - (uVar2 ^ *puVar4 ^ 0x23)) >> 0x1f & 0xffffffe0U) + 0x80029822;
      }
      return iVar6;
    }
    if (local_40[0] == '\n') break;
    iVar6 = iVar6 + 1;
    if (bVar1) {
      return -0x7ffd67e0;
    }
    *pcVar5 = local_40[0];
    pcVar5 = pcVar5 + 1;
  }
  if (0 < iVar6) {
    uVar2 = (int)((int)param_2[iVar6 + -1] ^ 0xdU) >> 0x1f;
    iVar6 = iVar6 + ((int)(((uVar2 ^ (int)param_2[iVar6 + -1] ^ 0xdU) - uVar2) + -1) >> 0x1f);
  }
  param_2[iVar6] = '\0';
  return iVar6;
}



uint FUN_00020b88(undefined4 *param_1,undefined4 *param_2)

{
  uint uVar1;
  int *piVar2;
  int iVar3;
  undefined1 local_30 [16];
  
  uVar1 = FUN_000290f8(*param_1,param_1[5],0x5dc,0x80,param_1 + 1,local_30);
  if ((int)uVar1 < 0) {
    piVar2 = (int *)FUN_00029210();
    if (*piVar2 == 0x23) {
      uVar1 = 0;
    }
  }
  else if (((3 < uVar1) && (iVar3 = FUN_00029de0(param_1[5],&DAT_0002cd18,4), iVar3 == 0)) &&
          (*param_2 = param_1[2], param_1[2] != 0)) {
    uVar1 = FUN_00029360(*param_1,param_1[6],0x9c,0,param_1 + 1,0x10);
    uVar1 = uVar1 & (int)~uVar1 >> 0x1f;
  }
  return uVar1;
}



undefined4 FUN_00020c88(int *param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0;
  if (-1 < *param_1) {
    uVar1 = FUN_00029248(*param_1);
    *param_1 = -1;
  }
  if (param_1[6] != 0) {
    FUN_000227fc(param_1[6]);
    param_1[6] = 0;
  }
  if (param_1[5] != 0) {
    FUN_000227fc(param_1[5]);
    param_1[5] = 0;
  }
  return uVar1;
}



int FUN_00020d18(int *param_1,undefined2 param_2,int param_3,byte param_4)

{
  int iVar1;
  undefined1 local_60 [4];
  undefined1 auStack_5c [8];
  undefined1 auStack_54 [20];
  
  FUN_000227b4(auStack_54);
  FUN_00029ad0(param_1,0,0x1c);
  iVar1 = FUN_00029398(2,2,0);
  *param_1 = iVar1;
  if ((-1 < iVar1) && (iVar1 = FUN_000292f0(iVar1,0,4,local_60,4), -1 < iVar1)) {
    *(undefined1 *)((int)param_1 + 5) = 2;
    *(undefined2 *)((int)param_1 + 6) = param_2;
    param_1[2] = 0;
    iVar1 = FUN_0002285c(0x5dc);
    param_1[5] = iVar1;
    iVar1 = FUN_0002285c(0x9c);
    param_1[6] = iVar1;
    if ((param_1[5] == 0) || (iVar1 == 0)) {
      iVar1 = -0x7ffd67ff;
    }
    else {
      FUN_00029ad0(iVar1,0,0x9c);
      FUN_00029b08(param_1[6],&DAT_0002cd20,4);
      *(undefined1 *)(param_1[6] + 4) = 1;
      *(undefined1 *)(param_1[6] + 5) = *(undefined1 *)(param_3 + 0x22);
      *(undefined1 *)(param_1[6] + 6) = *(undefined1 *)(param_3 + 0x23);
      *(undefined1 *)(param_1[6] + 7) = 1;
      *(byte *)(param_1[6] + 8) = *(byte *)(param_1[6] + 8) | (byte)(-(ulonglong)param_4 >> 0x3f);
      FUN_00029b08(param_1[6] + 10,param_3 + 0x1c,6);
      FUN_00029b08(param_1[6] + 0x10,param_3 + 0x24,0x80);
      FUN_00029d38(param_1[6] + 0x90,"NPXS01003_00",0xc);
      iVar1 = FUN_00029440(*param_1,param_1 + 1,0x10);
      if (-1 < iVar1) {
        FUN_000227b4(auStack_5c);
        return iVar1;
      }
    }
  }
  FUN_00020c88(param_1);
  return iVar1;
}



undefined8 FUN_00020f2c(void)

{
  return 0x20;
}



void FUN_00020f34(undefined4 *param_1,byte *param_2,undefined8 param_3,int param_4,char param_5)

{
  byte bVar1;
  undefined1 local_30 [4];
  undefined1 local_2c [20];
  
  *param_2 = *param_2 & 0x3f | 0x80;
  *param_2 = (byte)((((ulonglong)*param_2 << 0x20) >> 0x26) << 6) |
             (byte)((((ulonglong)*param_2 & 0x1f) << 0x1a) >> 0x18) >> 2;
  *param_2 = (byte)((((ulonglong)*param_2 << 0x20) >> 0x25) << 5) |
             (byte)((((ulonglong)*param_2 & 0xf) << 0x1b) >> 0x18) >> 3;
  *param_2 = *param_2 & 0xf0;
  param_2[1] = param_2[1] & 0x7f | param_5 << 7;
  if (param_4 == 0) {
    bVar1 = param_2[1] & 0x80 | (byte)*param_1 & 0x7f;
  }
  else {
    bVar1 = param_2[1] & 0x80 | (byte)param_4 & 0x7f;
  }
  param_2[1] = bVar1;
  *(short *)(param_1 + 1) = *(short *)(param_1 + 1) + 1;
  FUN_00029b08(param_2 + 2,local_30,2);
  FUN_00029b08(param_2 + 4,local_2c,4);
  FUN_00029b08(param_2 + 8,(int)param_1 + 6,4);
  return;
}



undefined8 FUN_00021054(undefined8 param_1,int param_2,undefined8 param_3,int param_4)

{
  undefined1 local_30 [4];
  undefined1 local_2c [12];
  
  FUN_00020f34(param_1,param_2,param_3,~((int)(*(byte *)(param_4 + 0x19) - 1) >> 0x1f) & 0x81);
  FUN_00029b08(param_2 + 0xc,local_30,2);
  FUN_00029b08(param_2 + 0xe,local_2c,4);
  FUN_00029b08(param_2 + 0x12,local_2c,4);
  *(undefined1 *)(param_2 + 0x16) = 0;
  *(undefined1 *)(param_2 + 0x17) = 0;
  if (*(int *)(param_4 + 8) != 0) {
    *(byte *)(param_2 + 0x17) = *(byte *)(param_2 + 0x17) | 4;
  }
  *(char *)(param_2 + 0x18) = (char)*(undefined4 *)(param_4 + 0xc);
  *(char *)(param_2 + 0x19) = (char)*(undefined4 *)(param_4 + 0x10);
  *(undefined1 *)(param_2 + 0x1a) = 0;
  if (*(char *)(param_4 + 0x18) != '\0') {
    *(byte *)(param_2 + 0x1a) = *(byte *)(param_2 + 0x1a) | 1;
  }
  *(undefined1 *)(param_2 + 0x1b) = 0;
  FUN_00029b08(param_2 + 0x1c,local_2c,4);
  return 0;
}



undefined8 FUN_00021188(undefined8 param_1,int param_2,undefined8 param_3,int param_4)

{
  undefined1 local_30 [4];
  undefined1 local_2c [12];
  
  FUN_00020f34(param_1,param_2,param_3,~((int)(*(byte *)(param_4 + 0x25) - 1) >> 0x1f) & 0x81);
  FUN_00029b08(param_2 + 0xc,local_30,2);
  FUN_00029b08(param_2 + 0xe,local_2c,4);
  FUN_00029b08(param_2 + 0x12,local_2c,4);
  *(undefined1 *)(param_2 + 0x16) = 0;
  if (*(char *)(param_4 + 0x24) != '\0') {
    *(byte *)(param_2 + 0x16) = *(byte *)(param_2 + 0x16) | 1;
  }
  *(char *)(param_2 + 0x17) = (char)*(undefined4 *)(param_4 + 0xc);
  if (*(int *)(param_4 + 8) != 0) {
    *(byte *)(param_2 + 0x17) = *(byte *)(param_2 + 0x17) | 4;
  }
  if (*(char *)(param_4 + 0x26) != '\0') {
    *(byte *)(param_2 + 0x17) = *(byte *)(param_2 + 0x17) | 8;
  }
  *(char *)(param_2 + 0x18) = (char)*(undefined4 *)(param_4 + 0x10);
  *(char *)(param_2 + 0x19) = (char)*(undefined4 *)(param_4 + 0x14);
  *(char *)(param_2 + 0x1a) = (char)*(undefined4 *)(param_4 + 0x18);
  *(char *)(param_2 + 0x1b) = (char)*(undefined4 *)(param_4 + 0x1c);
  FUN_00029b08(param_2 + 0x1c,local_2c,4);
  return 0;
}



void FUN_000212e0(undefined4 *param_1,undefined4 param_2,undefined4 param_3)

{
  undefined4 uStack00000040;
  
  *param_1 = param_2;
  *(undefined2 *)(param_1 + 1) = 0;
  uStack00000040 = param_3;
  FUN_00029b08((int)param_1 + 6,&stack0x00000040,4);
  return;
}



undefined8 FUN_00021324(undefined8 param_1,int param_2)

{
  undefined1 local_40 [4];
  undefined1 local_3c [20];
  
  FUN_00020f34(param_1,param_2,0,0,1);
  FUN_00029b08(param_2 + 0xc,local_40,2);
  FUN_00029b08(param_2 + 0xe,local_3c,4);
  FUN_00029b08(param_2 + 0x12,local_3c,4);
  FUN_00029b08(param_2 + 0x16,local_3c,4);
  FUN_00029ad0(param_2 + 0x1a,0,6);
  return 0;
}



undefined8 FUN_00021404(void)

{
  return 0x20;
}



undefined8 FUN_0002140c(ulonglong *param_1)

{
  ulonglong uVar1;
  
  uVar1 = FUN_00029280();
  *param_1 = uVar1 & 0xffffffff | 1;
  return 0;
}



undefined4 FUN_00021448(undefined4 *param_1)

{
  syscall();
  return *param_1;
}



int FUN_0002146c(int *param_1,int param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  syscall();
  if (iVar1 == 0) {
    param_1[1] = param_2;
    iVar1 = *param_1;
    syscall();
  }
  return iVar1;
}



int FUN_000214c4(int *param_1,int *param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  syscall();
  if (iVar1 == 0) {
    *param_2 = param_1[1];
    iVar1 = *param_1;
    syscall();
  }
  return iVar1;
}



int FUN_00021520(int param_1)

{
  FUN_00029ad0(param_1,0,8);
  *(undefined4 *)(param_1 + 4) = 0;
  syscall();
  return param_1;
}



int FUN_000215b0(int *param_1,uint param_2)

{
  int iVar1;
  
  iVar1 = *param_1;
  syscall();
  if (iVar1 == 0) {
    iVar1 = *param_1;
    param_1[1] = param_1[1] | param_2;
    syscall();
  }
  return iVar1;
}



undefined4 FUN_00021610(int param_1)

{
  return *(undefined4 *)(param_1 + 4);
}



int FUN_0002161c(int param_1,uint param_2)

{
  int iVar1;
  ulonglong uVar2;
  
  iVar1 = 0;
  if ((int)param_2 < *(int *)(param_1 + 4)) {
    iVar1 = *(int *)(param_1 + 0xc);
    uVar2 = (ulonglong)param_2 + 1 & 0xffffffff;
    if ((int)param_2 < 0) {
      uVar2 = 1;
    }
    while( true ) {
      uVar2 = uVar2 - 1;
      if (uVar2 == 0) break;
      iVar1 = *(int *)(iVar1 + 4);
    }
  }
  return iVar1;
}



undefined4 FUN_00021664(undefined8 param_1,undefined4 param_2)

{
  undefined4 *puVar1;
  undefined4 uVar2;
  
  FUN_0000b614();
  uVar2 = 0;
  puVar1 = (undefined4 *)FUN_0002161c(param_1,param_2);
  if (puVar1 != (undefined4 *)0x0) {
    uVar2 = *puVar1;
  }
  syscall();
  return uVar2;
}



int * FUN_000216d0(uint *param_1)

{
  int *piVar1;
  int iVar2;
  int iVar3;
  int *piVar4;
  ulonglong uVar5;
  
  iVar3 = 0;
  uVar5 = (ulonglong)*param_1 + 1 & 0xffffffff;
  piVar1 = (int *)0x0;
  if ((int)*param_1 < 0) {
    uVar5 = 1;
  }
  while( true ) {
    iVar2 = iVar3 * 8;
    iVar3 = iVar3 + 1;
    uVar5 = uVar5 - 1;
    if (uVar5 == 0) break;
    piVar4 = (int *)(iVar2 + param_1[4]);
    if ((*piVar4 == 0) && (piVar4[1] == 0)) {
      piVar1 = piVar4;
    }
  }
  return piVar1;
}



int FUN_00021738(int param_1,undefined4 param_2)

{
  int iVar1;
  undefined4 *puVar2;
  int iVar3;
  
  FUN_0000d0a4();
  iVar3 = -1;
  puVar2 = (undefined4 *)FUN_000216d0(param_1);
  if (puVar2 != (undefined4 *)0x0) {
    *puVar2 = param_2;
    puVar2[1] = 0;
    iVar3 = *(int *)(param_1 + 0xc);
    if (*(int *)(param_1 + 0xc) == 0) {
      *(undefined4 **)(param_1 + 0xc) = puVar2;
    }
    else {
      do {
        iVar1 = iVar3;
        iVar3 = *(int *)(iVar1 + 4);
      } while (iVar3 != 0);
      *(undefined4 **)(iVar1 + 4) = puVar2;
    }
    iVar3 = *(int *)(param_1 + 4);
    *(int *)(param_1 + 4) = iVar3 + 1;
  }
  syscall();
  return iVar3;
}



undefined8 FUN_000217e0(int param_1)

{
  syscall();
  if (*(int *)(param_1 + 0x10) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x10));
    *(undefined4 *)(param_1 + 0x10) = 0;
  }
  return 0;
}



int * FUN_00021834(int *param_1,int param_2)

{
  int iVar1;
  int *piVar2;
  undefined1 local_40 [40];
  
  *param_1 = param_2;
  param_1[3] = 0;
  param_1[1] = 0;
  piVar2 = param_1 + 2;
  syscall();
  if (piVar2 == (int *)0x0) {
    iVar1 = FUN_0002285c(param_2 << 3,local_40);
    param_1[4] = iVar1;
    if (iVar1 != 0) {
      FUN_00029ad0(iVar1,0,param_2 << 3);
      return (int *)0x0;
    }
    piVar2 = (int *)0x80029801;
  }
  syscall();
  return piVar2;
}



undefined4 FUN_00021928(int param_1,uint param_2)

{
  undefined4 *puVar1;
  undefined4 *puVar2;
  undefined4 *puVar3;
  undefined4 uVar4;
  ulonglong uVar5;
  
  FUN_0000d0a4();
  uVar4 = 0xffffffff;
  if ((int)param_2 < *(int *)(param_1 + 4)) {
    puVar3 = *(undefined4 **)(param_1 + 0xc);
    puVar1 = (undefined4 *)puVar3[1];
    if (param_2 == 0) {
      puVar3[1] = 0;
      *puVar3 = 0;
      *(undefined4 **)(param_1 + 0xc) = puVar1;
    }
    else {
      uVar5 = (ulonglong)param_2;
      if ((int)(param_2 - 1) < 0) {
        uVar5 = 1;
      }
      while (puVar2 = puVar1, uVar5 = uVar5 - 1, uVar5 != 0) {
        puVar1 = (undefined4 *)puVar2[1];
        puVar3 = puVar2;
      }
      *puVar2 = 0;
      puVar3[1] = puVar2[1];
      puVar2[1] = 0;
    }
    uVar4 = 0;
    *(int *)(param_1 + 4) = *(int *)(param_1 + 4) + -1;
  }
  syscall();
  return uVar4;
}



undefined4 FUN_00021a0c(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000297f8(param_1,0);
  return uVar1;
}



int FUN_00021a34(int param_1,undefined4 *param_2)

{
  int iVar1;
  
  iVar1 = FUN_00021a0c();
  if (-1 < iVar1) {
    *param_2 = *(undefined4 *)(param_1 + 0x28);
  }
  return iVar1;
}



undefined4 FUN_00021a80(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029da8(param_1 + 0x20);
  return uVar1;
}



undefined4 FUN_00021aac(int param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029da8(param_1 + 0x18);
  return uVar1;
}



undefined4 FUN_00021ad8(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029830();
  return uVar1;
}



undefined8 FUN_00021afc(int param_1)

{
  *(undefined4 *)(param_1 + 0x38) = 1;
  FUN_00021a0c();
  FUN_00021aac(param_1);
  FUN_00021a80(param_1);
  FUN_00021ad8(param_1);
  return 0;
}



undefined8 FUN_00021b4c(int param_1)

{
  *(undefined4 *)(param_1 + 0x34) = 0;
  *(undefined4 *)(param_1 + 0x2c) = 0;
  FUN_00021a80();
  FUN_00021ad8(param_1);
  return 0;
}



undefined8 FUN_00021b90(int param_1,undefined4 param_2)

{
  *(undefined4 *)(param_1 + 0x34) = param_2;
  *(undefined4 *)(param_1 + 0x2c) = 1;
  FUN_00021aac();
  FUN_00021ad8(param_1);
  return 0;
}



int FUN_00021bd4(int param_1)

{
  int iVar1;
  
  iVar1 = FUN_00021a0c();
  if (-1 < iVar1) {
    *(undefined4 *)(param_1 + 0x34) = 0;
    *(undefined4 *)(param_1 + 0x38) = 0;
    *(undefined4 *)(param_1 + 0x2c) = 0;
    FUN_00021ad8(param_1);
  }
  return iVar1;
}



undefined4 FUN_00021c38(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029be8();
  return uVar1;
}



int FUN_00021c5c(int param_1,undefined4 *param_2)

{
  undefined4 uVar1;
  int iVar2;
  
  iVar2 = FUN_00021c38();
  uVar1 = 0;
  if (iVar2 == 0) {
    uVar1 = *(undefined4 *)(param_1 + 0x28);
  }
  *param_2 = uVar1;
  return iVar2;
}



undefined8 FUN_00021cac(int param_1)

{
  if (*(int *)(param_1 + 0x28) != 0) {
    FUN_000227fc(*(int *)(param_1 + 0x28));
    *(undefined4 *)(param_1 + 0x28) = 0;
  }
  FUN_00029868(param_1 + 0x18);
  FUN_00029868(param_1 + 0x20);
  FUN_00029d00(param_1);
  return 0;
}



int FUN_00021d2c(int param_1,undefined4 param_2,undefined4 param_3)

{
  int iVar1;
  int iVar2;
  undefined1 local_60 [8];
  undefined1 local_58 [8];
  undefined1 local_50 [16];
  
  FUN_00029ad0(param_1,0,0x40);
  *(undefined4 *)(param_1 + 0x30) = param_2;
  iVar1 = FUN_00029948(param_1,local_50);
  if (-1 < iVar1) {
    iVar1 = FUN_00029d70(param_1 + 0x18,param_1,local_60);
    if ((-1 < iVar1) && (iVar1 = FUN_00029d70(param_1 + 0x20,param_1,local_58), -1 < iVar1)) {
      iVar2 = FUN_00022828(param_3,param_2);
      *(int *)(param_1 + 0x28) = iVar2;
      if (iVar2 != 0) {
        return iVar1;
      }
      iVar1 = -0x7ffd67ff;
    }
  }
  FUN_00021cac(param_1);
  return iVar1;
}



int FUN_00021eb8(int param_1,undefined4 param_2)

{
  int iVar1;
  
  iVar1 = FUN_00021a0c(param_1);
  if (-1 < iVar1) {
    do {
      if ((0 < *(int *)(param_1 + 0x2c)) || (*(int *)(param_1 + 0x38) != 0)) break;
      iVar1 = FUN_000298d8(param_1 + 0x18,param_2);
    } while (-1 < iVar1);
    FUN_00021ad8(param_1);
    if (*(int *)(param_1 + 0x38) != 0) {
      iVar1 = -0x7ffd67fa;
    }
  }
  return iVar1;
}



int FUN_00021f6c(int param_1,undefined4 *param_2,undefined4 *param_3)

{
  undefined4 uVar1;
  int iVar2;
  
  iVar2 = FUN_00021a0c();
  if (-1 < iVar2) {
    if (*(int *)(param_1 + 0x2c) == 0) {
      FUN_00021ad8(param_1);
      iVar2 = -0x7ffd67fd;
    }
    else {
      uVar1 = *(undefined4 *)(param_1 + 0x28);
      *param_3 = *(undefined4 *)(param_1 + 0x34);
      *param_2 = uVar1;
    }
  }
  return iVar2;
}



int FUN_00021ff8(int param_1)

{
  int iVar1;
  
  iVar1 = FUN_00021a0c(param_1);
  if (-1 < iVar1) {
    while (((iVar1 == 0 && (1 - *(int *)(param_1 + 0x2c) < 1)) && (*(int *)(param_1 + 0x38) == 0)))
    {
      iVar1 = FUN_000298d8(param_1 + 0x20,0);
    }
    FUN_00021ad8(param_1);
    if (*(int *)(param_1 + 0x38) != 0) {
      iVar1 = -0x7ffd67fa;
    }
  }
  return iVar1;
}



int FUN_000220a4(undefined4 *param_1,undefined4 *param_2)

{
  int iVar1;
  
  iVar1 = param_1[2];
  syscall();
  if (iVar1 == 0) {
    *param_2 = *param_1;
    iVar1 = param_1[2];
    syscall();
  }
  return iVar1;
}



uint FUN_00022100(uint *param_1,uint param_2)

{
  uint uVar1;
  
  uVar1 = param_1[2];
  syscall();
  if (uVar1 == 0) {
    uVar1 = param_1[2];
    *param_1 = *param_1 & param_2;
    syscall();
    if (uVar1 == 0) {
      FUN_000297f8(param_1 + 4,0);
      FUN_00029da8(param_1 + 10);
      FUN_00029830(param_1 + 4);
    }
  }
  return uVar1;
}



int FUN_000221b4(uint *param_1,uint param_2,int param_3,undefined8 param_4)

{
  int iVar1;
  
  iVar1 = FUN_000297f8(param_1 + 4,0);
  if (-1 < iVar1) {
    if (param_3 == 0) {
      while (((iVar1 == 0 && ((param_2 & *param_1) == 0)) && (param_1[1] != 1))) {
        iVar1 = FUN_000298d8(param_1 + 10,param_4);
      }
    }
    else if (param_3 == 1) {
      while (((iVar1 == 0 && ((param_2 & *param_1) != param_2)) && (param_1[1] != 1))) {
        iVar1 = FUN_000298d8(param_1 + 10,param_4);
      }
    }
    else {
      while (((iVar1 == 0 && (*param_1 != param_2)) && (param_1[1] != 1))) {
        iVar1 = FUN_000298d8(param_1 + 10,param_4);
      }
    }
    FUN_00029830(param_1 + 4);
    if (param_1[1] == 1) {
      iVar1 = -0x7ffd67fa;
    }
  }
  return iVar1;
}



void FUN_0002231c(void)

{
  FUN_000221b4();
  return;
}



undefined8 FUN_00022324(int param_1)

{
  FUN_00029868(param_1 + 0x28);
  FUN_00029d00(param_1 + 0x10);
  syscall();
  return 0;
}



int FUN_0002237c(int param_1)

{
  int iVar1;
  undefined1 local_60 [8];
  undefined1 local_58 [56];
  
  FUN_00029ad0(param_1,0,0x30);
  iVar1 = param_1 + 8;
  syscall();
  if (iVar1 == 0) {
    iVar1 = FUN_00029948(param_1 + 0x10,local_58);
    if ((iVar1 == 0) && (iVar1 = FUN_00029d70(param_1 + 0x28,param_1 + 0x10,local_60), -1 < iVar1))
    {
      return iVar1;
    }
  }
  FUN_00022324(param_1);
  return iVar1;
}



uint FUN_000224d4(uint *param_1,uint param_2)

{
  uint uVar1;
  
  uVar1 = param_1[2];
  syscall();
  if (uVar1 == 0) {
    uVar1 = param_1[2];
    *param_1 = *param_1 | param_2;
    syscall();
    if (uVar1 == 0) {
      FUN_000297f8(param_1 + 4,0);
      FUN_00029da8(param_1 + 10);
      FUN_00029830(param_1 + 4);
    }
  }
  return uVar1;
}



undefined4 FUN_00022588(undefined4 *param_1)

{
  syscall();
  return *param_1;
}



undefined4 FUN_000225ac(undefined4 *param_1,undefined4 param_2)

{
  param_1[1] = param_2;
  syscall();
  return *param_1;
}



undefined4 FUN_000225d8(undefined4 *param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0x80029805;
  if (param_1[1] != 0) {
    uVar1 = *param_1;
    syscall();
  }
  return uVar1;
}



undefined4 FUN_00022614(undefined4 *param_1)

{
  undefined4 uVar1;
  
  uVar1 = 0x80029805;
  if (param_1[1] != 0) {
    uVar1 = *param_1;
    syscall();
  }
  return uVar1;
}



undefined4 FUN_0002265c(undefined8 param_1)

{
  FUN_00029ad0(param_1,0,8);
  syscall();
  return (int)param_1;
}



undefined4 FUN_000226a8(undefined4 *param_1)

{
  syscall();
  syscall();
  return *param_1;
}



undefined4
FUN_000226e8(undefined4 *param_1,undefined8 param_2,undefined8 param_3,undefined8 param_4,
            undefined8 param_5)

{
  undefined4 uVar1;
  undefined8 *puVar2;
  
  uVar1 = *param_1;
  syscall();
  puVar2 = (undefined8 *)param_2;
  puVar2[3] = param_5;
  *puVar2 = param_2;
  puVar2[1] = 0;
  puVar2[2] = param_4;
  return uVar1;
}



undefined4 FUN_0002272c(undefined8 param_1)

{
  FUN_00029ad0(param_1,0,4);
  syscall();
  return (int)param_1;
}



undefined8 FUN_000227b4(undefined4 *param_1)

{
  undefined4 uVar1;
  
  *param_1 = DAT_0002f264;
  uVar1 = FUN_00029cc8(DAT_0002f260);
  param_1[1] = uVar1;
  return 0;
}



void FUN_000227fc(undefined8 param_1)

{
  FUN_00029b78(DAT_0002f260,param_1);
  return;
}



undefined4 FUN_00022828(undefined8 param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029a28(DAT_0002f260,param_1,param_2);
  return uVar1;
}



undefined4 FUN_0002285c(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00029980(DAT_0002f260,param_1);
  return uVar1;
}



undefined4 FUN_0002288c(void)

{
  undefined4 uVar1;
  
  uVar1 = 0;
  if (DAT_0002f260 != 0) {
    uVar1 = FUN_00029c20(DAT_0002f260);
    DAT_0002f260 = 0;
  }
  return uVar1;
}



int FUN_000228dc(undefined8 param_1,undefined8 param_2)

{
  undefined1 local_10 [16];
  
  DAT_0002f264 = (undefined4)param_2;
  DAT_0002f260 = FUN_00029c90("_cellPremoMyHeap",param_2,0xffffffff80000000,local_10);
  return ((int)(((int)DAT_0002f260 >> 0x1f) - ((int)DAT_0002f260 >> 0x1f ^ DAT_0002f260)) >> 0x1f &
         0x7ffd67feU) + 0x80029802;
}



int FUN_00022958(byte *param_1,undefined1 *param_2,uint param_3)

{
  byte bVar1;
  byte bVar2;
  byte *pbVar3;
  undefined *puVar4;
  undefined1 *puVar5;
  undefined1 *puVar6;
  ulonglong uVar7;
  undefined1 uVar8;
  longlong lVar9;
  
  puVar4 = PTR_s_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef_0002e6f8;
  lVar9 = (ulonglong)param_3 / 3 + 1;
  puVar5 = param_2;
  while( true ) {
    lVar9 = lVar9 + -1;
    if (lVar9 == 0) break;
    bVar1 = *param_1;
    param_3 = param_3 - 3;
    bVar2 = param_1[2];
    pbVar3 = param_1 + 1;
    param_1 = param_1 + 3;
    uVar7 = (ulonglong)*pbVar3 << 8 | (ulonglong)bVar1 << 0x10 | (ulonglong)bVar2;
    *puVar5 = puVar4[(uint)uVar7 >> 0x12];
    puVar5[1] = puVar4[(uint)(uVar7 >> 0xc) & 0x3f];
    puVar5[2] = puVar4[(uint)(uVar7 >> 6) & 0x3f];
    puVar5[3] = puVar4[bVar2 & 0x3f];
    puVar5 = puVar5 + 4;
  }
  puVar6 = puVar5;
  if (param_3 != 0) {
    uVar7 = (ulonglong)*param_1 << 0x10;
    if (param_3 == 2) {
      uVar7 = uVar7 | (ulonglong)param_1[1] << 8;
    }
    *puVar5 = puVar4[(uint)uVar7 >> 0x12];
    puVar5[1] = puVar4[(uint)(uVar7 >> 0xc) & 0x3f];
    if (param_3 == 2) {
      uVar8 = puVar4[(uint)uVar7 >> 6 & 0x3c];
    }
    else {
      uVar8 = 0x3d;
    }
    puVar6 = puVar5 + 4;
    puVar5[3] = 0x3d;
    puVar5[2] = uVar8;
  }
  return (int)puVar6 - (int)param_2;
}



int FUN_00022a88(char *param_1,int param_2,uint param_3)

{
  char cVar1;
  char *pcVar2;
  char *pcVar3;
  char *pcVar4;
  int iVar5;
  ulonglong uVar6;
  longlong lVar7;
  undefined1 *puVar8;
  
  if ((param_3 & 3) == 0) {
    iVar5 = 0;
    lVar7 = (ulonglong)(param_3 - 1 >> 2) + 1;
    if (param_3 == 0) {
      lVar7 = 1;
    }
    while( true ) {
      puVar8 = (undefined1 *)(param_2 + iVar5);
      lVar7 = lVar7 + -1;
      if (lVar7 == 0) break;
      cVar1 = *param_1;
      param_3 = param_3 - 4;
      iVar5 = iVar5 + 3;
      if ((&DAT_0002d3a8)[cVar1] == -1) {
        return -2;
      }
      pcVar2 = param_1 + 1;
      if ((&DAT_0002d3a8)[*pcVar2] == -1) {
        return -2;
      }
      pcVar3 = param_1 + 2;
      if ((&DAT_0002d3a8)[*pcVar3] == -1) {
        return -2;
      }
      pcVar4 = param_1 + 3;
      param_1 = param_1 + 4;
      uVar6 = ((longlong)(char)(&DAT_0002d3a8)[*pcVar3] & 0x3ffffffU) << 6 |
              ((longlong)(char)(&DAT_0002d3a8)[cVar1] & 0x3fffU) << 0x12 |
              ((longlong)(char)(&DAT_0002d3a8)[*pcVar2] & 0xfffffU) << 0xc |
              (longlong)(char)(&DAT_0002d3a8)[*pcVar4];
      if ((&DAT_0002d3a8)[*pcVar4] == -1) {
        return -2;
      }
      puVar8[2] = (char)uVar6;
      *puVar8 = (char)(uVar6 >> 0x10);
      puVar8[1] = (char)(uVar6 >> 8);
    }
    if (param_3 == 4) {
      if (((&DAT_0002d3a8)[*param_1] != -1) && ((&DAT_0002d3a8)[param_1[1]] != -1)) {
        uVar6 = ((longlong)(char)(&DAT_0002d3a8)[*param_1] & 0x3fffU) << 0x12 |
                ((longlong)(char)(&DAT_0002d3a8)[param_1[1]] & 0xfffffU) << 0xc;
        if (param_1[2] == 0x3d) {
          if (param_1[3] == '=') {
            *puVar8 = (char)(uVar6 >> 0x10);
            return iVar5 + 1;
          }
        }
        else if ((&DAT_0002d3a8)[param_1[2]] != -1) {
          uVar6 = uVar6 | ((longlong)(char)(&DAT_0002d3a8)[param_1[2]] & 0x3ffffffU) << 6;
          if (param_1[3] == 0x3d) {
            puVar8[1] = (char)(uVar6 >> 8);
            *puVar8 = (char)(uVar6 >> 0x10);
            return iVar5 + 2;
          }
          if ((&DAT_0002d3a8)[param_1[3]] != -1) {
            uVar6 = uVar6 | (longlong)(char)(&DAT_0002d3a8)[param_1[3]];
            puVar8[2] = (char)uVar6;
            *puVar8 = (char)(uVar6 >> 0x10);
            puVar8[1] = (char)(uVar6 >> 8);
            return iVar5 + 3;
          }
        }
      }
      return -2;
    }
  }
  return -1;
}



undefined4 FUN_00022c78(undefined8 param_1)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00023278(param_1,0,10);
  return uVar1;
}



uint FUN_00022ca4(char *param_1,undefined4 *param_2,undefined4 param_3,undefined4 *param_4)

{
  ulonglong uVar1;
  uint uVar2;
  undefined4 *puVar3;
  char cVar4;
  char *pcVar5;
  undefined4 auStack_50 [4];
  
  pcVar5 = param_1;
  if (param_2 == (undefined4 *)0x0) {
    param_2 = auStack_50;
  }
  while (uVar1 = FUN_000299f0(*pcVar5), (uVar1 & 8) != 0) {
    pcVar5 = pcVar5 + 1;
  }
  cVar4 = *pcVar5;
  if ((cVar4 == '-') || (cVar4 == '+')) {
    pcVar5 = pcVar5 + 1;
  }
  else {
    cVar4 = '+';
  }
  uVar2 = FUN_00022fd0(pcVar5,param_2,param_3,param_4);
  if (pcVar5 == (char *)*param_2) {
    *param_2 = param_1;
LAB_00022d70:
    if (uVar2 != 0) goto LAB_00022da8;
  }
  else if (param_1 == (char *)*param_2) goto LAB_00022d70;
  if (cVar4 == '+') {
    if (-1 < (int)uVar2) {
      return uVar2;
    }
  }
  else {
    if (cVar4 != '-') {
      return uVar2;
    }
    if (uVar2 < 0x80000001) {
      return -uVar2;
    }
  }
LAB_00022da8:
  puVar3 = (undefined4 *)FUN_00027aa8();
  *puVar3 = 0x8001001c;
  if (param_4 != (undefined4 *)0x0) {
    *param_4 = 1;
  }
  uVar2 = (int)((int)cVar4 ^ 0x2dU) >> 0x1f;
  return ((int)(uVar2 - (uVar2 ^ (int)cVar4 ^ 0x2dU)) >> 0x1f) + 0x80000000;
}



void FUN_00022e24(void)

{
  FUN_00022ca4();
  return;
}



void FUN_00022e2c(void)

{
  FUN_00022fc8();
  return;
}



ulonglong FUN_00022e4c(char *param_1,undefined4 *param_2,undefined4 param_3,undefined4 *param_4)

{
  bool bVar1;
  ulonglong uVar2;
  ulonglong uVar3;
  undefined4 *puVar4;
  char cVar5;
  char *pcVar6;
  undefined4 auStack_50 [4];
  
  pcVar6 = param_1;
  if (param_2 == (undefined4 *)0x0) {
    param_2 = auStack_50;
  }
  while (uVar2 = FUN_000299f0(*pcVar6), (uVar2 & 8) != 0) {
    pcVar6 = pcVar6 + 1;
  }
  cVar5 = *pcVar6;
  if ((cVar5 == '-') || (cVar5 == '+')) {
    pcVar6 = pcVar6 + 1;
  }
  else {
    cVar5 = '+';
  }
  uVar2 = FUN_00023280(pcVar6,param_2,param_3,param_4);
  if (pcVar6 == (char *)*param_2) {
    *param_2 = param_1;
LAB_00022f18:
    if (uVar2 != 0) goto LAB_00022f54;
  }
  else if (param_1 == (char *)*param_2) goto LAB_00022f18;
  if (cVar5 == '+') {
    if (-1 < (longlong)uVar2) {
      return uVar2;
    }
  }
  else {
    if (cVar5 != '-') {
      return uVar2;
    }
    uVar3 = -uVar2;
    bVar1 = uVar2 < 0x8000000000000001;
    uVar2 = uVar3;
    if (bVar1) {
      return uVar3;
    }
  }
LAB_00022f54:
  puVar4 = (undefined4 *)FUN_00027aa8(uVar2);
  *puVar4 = 0x8001001c;
  if (param_4 != (undefined4 *)0x0) {
    *param_4 = 1;
  }
  uVar2 = 0x8000000000000000;
  if (cVar5 != '-') {
    uVar2 = 0x7fffffffffffffff;
  }
  return uVar2;
}



void FUN_00022fc8(void)

{
  FUN_00022e4c();
  return;
}



uint FUN_00022fd0(char *param_1,undefined4 *param_2,uint param_3,undefined4 *param_4)

{
  uint uVar1;
  ulonglong uVar2;
  undefined4 uVar3;
  int iVar4;
  undefined4 *puVar5;
  uint uVar6;
  char cVar7;
  int iVar8;
  uint uVar9;
  char *pcVar10;
  char *pcVar11;
  char *pcVar12;
  
  pcVar11 = param_1;
  if (param_4 != (undefined4 *)0x0) {
    *param_4 = 0;
  }
  while (uVar2 = FUN_000299f0(*pcVar11), (uVar2 & 8) != 0) {
    pcVar11 = pcVar11 + 1;
  }
  cVar7 = *pcVar11;
  if ((cVar7 == '-') || (cVar7 == '+')) {
    pcVar11 = pcVar11 + 1;
  }
  else {
    cVar7 = '+';
  }
  if (((-1 < (int)param_3) && (param_3 != 1)) && ((int)param_3 < 0x25)) {
    pcVar12 = pcVar11;
    if ((int)param_3 < 1) {
      param_3 = 10;
      if ((*pcVar11 == '0') && ((pcVar11[1] == 'x' || (param_3 = 8, pcVar11[1] == 'X')))) {
        pcVar11 = pcVar11 + 2;
        param_3 = 0x10;
        pcVar12 = pcVar11;
      }
    }
    else if (((param_3 == 0x10) && (*pcVar11 == '0')) &&
            ((pcVar11[1] == 'x' || (pcVar11[1] == 'X')))) {
      pcVar11 = pcVar11 + 2;
      pcVar12 = pcVar11;
    }
    for (; *pcVar11 == '0'; pcVar11 = pcVar11 + 1) {
    }
    iVar8 = 0;
    pcVar10 = pcVar11;
    uVar1 = 0;
    uVar6 = 0;
    while( true ) {
      uVar9 = uVar1;
      uVar3 = FUN_00029a98(*pcVar10);
      iVar4 = FUN_000299b8("0123456789abcdefghijklmnopqrstuvwxyz",uVar3,param_3);
      if (iVar4 == 0) break;
      iVar8 = iVar4 + -0x2d4a8;
      pcVar10 = pcVar10 + 1;
      uVar1 = uVar9 * param_3 + (int)(char)iVar8;
      uVar6 = uVar9;
    }
    if (pcVar12 != pcVar10) {
      if (((int)(pcVar10 +
                (-(int)*(char *)((int)&PTR_LAB_00002114_1_0002d4f8 + param_3) - (int)pcVar11)) < 0)
         || ((((int)(pcVar10 +
                    (-(int)*(char *)((int)&PTR_LAB_00002114_1_0002d4f8 + param_3) - (int)pcVar11)) <
               1 && (uVar1 = uVar9 - (int)(char)iVar8, uVar1 <= uVar9)) &&
             (uVar1 / param_3 == uVar6)))) {
        if (cVar7 == '-') {
          uVar9 = -uVar9;
        }
      }
      else {
        puVar5 = (undefined4 *)FUN_00027aa8();
        uVar9 = 0xffffffff;
        *puVar5 = 0x8001001c;
        if (param_4 != (undefined4 *)0x0) {
          *param_4 = 1;
        }
      }
      if (param_2 == (undefined4 *)0x0) {
        return uVar9;
      }
      *param_2 = pcVar10;
      return uVar9;
    }
  }
  if (param_2 != (undefined4 *)0x0) {
    *param_2 = param_1;
  }
  return 0;
}



void FUN_00023278(void)

{
  FUN_00022fd0();
  return;
}



ulonglong FUN_00023280(byte *param_1,undefined4 *param_2,int param_3,undefined4 *param_4)

{
  ulonglong uVar1;
  undefined4 uVar2;
  int iVar3;
  undefined4 *puVar4;
  byte bVar5;
  int iVar6;
  ulonglong uVar7;
  byte *pbVar8;
  byte *pbVar9;
  byte *pbVar10;
  
  pbVar9 = param_1;
  if (param_4 != (undefined4 *)0x0) {
    *param_4 = 0;
  }
  while (bVar5 = *pbVar9, (*(ushort *)((uint)bVar5 * 2 + 0x4e800020) & 0x144) != 0) {
    pbVar9 = pbVar9 + 1;
  }
  if ((bVar5 == 0x2d) || (bVar5 == 0x2b)) {
    pbVar9 = pbVar9 + 1;
  }
  else {
    bVar5 = 0x2b;
  }
  if (((-1 < param_3) && (param_3 != 1)) && (param_3 < 0x25)) {
    pbVar10 = pbVar9;
    if (param_3 < 1) {
      param_3 = 10;
      if ((*pbVar9 == 0x30) && ((pbVar9[1] == 0x78 || (param_3 = 8, pbVar9[1] == 0x58)))) {
        pbVar9 = pbVar9 + 2;
        param_3 = 0x10;
        pbVar10 = pbVar9;
      }
    }
    else if (((param_3 == 0x10) && (*pbVar9 == 0x30)) &&
            ((pbVar9[1] == 0x78 || (pbVar9[1] == 0x58)))) {
      pbVar9 = pbVar9 + 2;
      pbVar10 = pbVar9;
    }
    for (; *pbVar9 == 0x30; pbVar9 = pbVar9 + 1) {
    }
    uVar7 = 0;
    iVar6 = 0;
    pbVar8 = pbVar9;
    while( true ) {
      uVar2 = FUN_00029a98(*pbVar8);
      iVar3 = FUN_000299b8("0123456789abcdefghijklmnopqrstuvwxyz",uVar2,param_3);
      if (iVar3 == 0) break;
      iVar6 = iVar3 + -0x2d4d0;
      pbVar8 = pbVar8 + 1;
      uVar7 = uVar7 * (longlong)param_3 + (longlong)(char)iVar6;
    }
    if (pbVar10 != pbVar8) {
      if (((int)(pbVar8 + (-(int)*(char *)((int)&PTR_LAB_00004128_1_0002d520 + param_3) -
                          (int)pbVar9)) < 0) ||
         ((((int)(pbVar8 + (-(int)*(char *)((int)&PTR_LAB_00004128_1_0002d520 + param_3) -
                           (int)pbVar9)) < 1 &&
           (uVar1 = uVar7 - (longlong)(char)iVar6, uVar1 <= uVar7)) &&
          (uVar1 / (ulonglong)(longlong)param_3 == uVar7)))) {
        if (bVar5 == 0x2d) {
          uVar7 = -uVar7;
        }
      }
      else {
        puVar4 = (undefined4 *)FUN_00027aa8();
        uVar7 = 0xffffffffffffffff;
        *puVar4 = 0x8001001c;
        if (param_4 != (undefined4 *)0x0) {
          *param_4 = 1;
        }
      }
      if (param_2 == (undefined4 *)0x0) {
        return uVar7;
      }
      *param_2 = pbVar8;
      return uVar7;
    }
  }
  if (param_2 != (undefined4 *)0x0) {
    *param_2 = param_1;
  }
  return 0;
}



void FUN_0002353c(int param_1)

{
  bool bVar1;
  int iVar2;
  undefined1 local_20 [16];
  
  iVar2 = 0;
  do {
    FUN_00029280();
    FUN_00029b08(param_1 + iVar2,local_20,4);
    bVar1 = iVar2 != 0xc;
    iVar2 = iVar2 + 4;
  } while (bVar1);
  return;
}



undefined4 FUN_0002359c(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00024e08();
  return uVar1;
}



undefined4 FUN_000235c8(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00024c80();
  return uVar1;
}



void FUN_000235f4(int param_1)

{
  if (param_1 != 0) {
    FUN_00025cb0(param_1);
    FUN_00027e98(param_1);
  }
  return;
}



undefined4 FUN_00023634(undefined4 param_1)

{
  undefined4 uVar1;
  undefined1 local_30 [16];
  
  uVar1 = FUN_00027e60(4);
  FUN_00027290(uVar1,param_1,0,0,local_30);
  return uVar1;
}



undefined4 FUN_00023690(int param_1,int param_2)

{
  undefined4 uVar1;
  undefined1 local_10 [16];
  
  uVar1 = 0xffffffff;
  if (param_2 != 0) {
    uVar1 = (*(code *)**(undefined4 **)(param_2 + 0x30))(param_1 + 0x24,local_10);
  }
  return uVar1;
}



undefined8 FUN_000236f4(void)

{
  return 0;
}



int FUN_000236fc(void)

{
  int iVar1;
  char *pcVar2;
  longlong lVar3;
  
  iVar1 = 0;
  lVar3 = 7;
  pcVar2 = &DAT_0002f980;
  do {
    if ((((*pcVar2 != '\0') || (pcVar2[1] != '\0')) || (pcVar2[2] != '\0')) ||
       (((pcVar2[3] != '\0' || (pcVar2[4] != '\0')) || (pcVar2[5] != '\0')))) {
      iVar1 = iVar1 + 1;
    }
    lVar3 = lVar3 + -1;
    pcVar2 = pcVar2 + 0xac;
  } while (lVar3 != 0);
  return iVar1;
}



undefined8 FUN_00023774(void)

{
  return 1;
}



undefined4 FUN_000238cc(int param_1)

{
  undefined1 auStack_20 [32];
  
  if (param_1 != 0) {
    (*(code *)**(undefined4 **)(param_1 + 8))(auStack_20);
  }
  return 0;
}



undefined4
FUN_00023934(undefined4 param_1,undefined1 *param_2,undefined1 *param_3,undefined1 *param_4,
            undefined1 *param_5)

{
  bool bVar1;
  undefined1 uVar2;
  undefined1 uVar3;
  undefined1 uVar4;
  int iVar5;
  undefined1 *puVar6;
  int iVar7;
  int iVar8;
  
  puVar6 = &DAT_0002f99c;
  iVar8 = 0;
  do {
    iVar7 = iVar8 * 0xac;
    iVar5 = FUN_00027d80(&DAT_0002f986 + iVar7,param_1,0x10);
    bVar1 = iVar8 != 6;
    iVar8 = iVar8 + 1;
    if (iVar5 == 0) {
      uVar2 = (&DAT_0002f981)[iVar7];
      uVar3 = (&DAT_0002f982)[iVar7];
      uVar4 = (&DAT_0002f983)[iVar7];
      *param_2 = (&DAT_0002f980)[iVar7];
      param_2[1] = uVar2;
      param_2[2] = uVar3;
      param_2[3] = uVar4;
      uVar2 = (&DAT_0002f984)[iVar7];
      param_2[5] = (&DAT_0002f985)[iVar7];
      param_2[4] = uVar2;
      uVar2 = puVar6[1];
      uVar3 = puVar6[2];
      uVar4 = puVar6[3];
      *param_3 = *puVar6;
      param_3[1] = uVar2;
      param_3[2] = uVar3;
      param_3[3] = uVar4;
      uVar2 = puVar6[5];
      uVar3 = puVar6[6];
      uVar4 = puVar6[7];
      param_3[4] = puVar6[4];
      param_3[5] = uVar2;
      param_3[6] = uVar3;
      param_3[7] = uVar4;
      uVar2 = puVar6[10];
      uVar3 = puVar6[0xb];
      uVar4 = puVar6[9];
      param_3[8] = puVar6[8];
      param_3[9] = uVar4;
      param_3[10] = uVar2;
      param_3[0xb] = uVar3;
      uVar2 = puVar6[0xc];
      uVar3 = puVar6[0xd];
      uVar4 = puVar6[0xe];
      param_3[0xf] = puVar6[0xf];
      param_3[0xc] = uVar2;
      param_3[0xd] = uVar3;
      param_3[0xe] = uVar4;
      *param_4 = (char)*(undefined4 *)(iVar7 + 0x2f998);
      *param_5 = *(undefined1 *)(iVar7 + 0x2f99a);
      return 0;
    }
    puVar6 = puVar6 + 0xac;
  } while (bVar1);
  return 0x80029842;
}



undefined4 FUN_00023ae8(void)

{
  undefined4 uVar1;
  
  uVar1 = FUN_00027d80();
  return uVar1;
}



uint FUN_00023b0c(int param_1,uint param_2,uint param_3,undefined4 param_4,uint param_5)

{
  uint uVar1;
  int iVar2;
  uint uVar3;
  
  if (*(uint *)(param_1 + 0x14) < param_2) {
    FUN_00027db8(param_1);
  }
  uVar3 = *(int *)(param_1 + 0x14) - param_2;
  if (param_3 < uVar3) {
    uVar3 = param_3;
  }
  if (uVar3 != 0) {
    iVar2 = param_1 + 4;
    if (0xf < *(uint *)(param_1 + 0x18)) {
      iVar2 = *(int *)(param_1 + 4);
    }
    uVar1 = uVar3;
    if (param_5 < uVar3) {
      uVar1 = param_5;
    }
    uVar1 = FUN_00023ae8(iVar2 + param_2,param_4,uVar1);
    if (uVar1 != 0) {
      return uVar1;
    }
  }
  uVar1 = 0xffffffff;
  if (param_5 <= uVar3) {
    uVar1 = (int)(uVar3 ^ param_5) >> 0x1f;
    uVar1 = uVar1 - (uVar1 ^ uVar3 ^ param_5) >> 0x1f;
  }
  return uVar1;
}



undefined4 FUN_00023c1c(int param_1,undefined8 param_2)

{
  undefined4 uVar1;
  
  uVar1 = FUN_000019c8(param_2);
  uVar1 = FUN_00023b0c(param_1,0,*(undefined4 *)(param_1 + 0x14),param_2,uVar1);
  return uVar1;
}



uint FUN_00023c7c(void)

{
  uint uVar1;
  
  uVar1 = FUN_00023c1c();
  return (((int)uVar1 >> 0x1f ^ uVar1) - ((int)uVar1 >> 0x1f)) - 1 >> 0x1f;
}



undefined8 FUN_00023cb4(int param_1)

{
  bool bVar1;
  char *pcVar2;
  int iVar3;
  char cVar5;
  int iVar4;
  undefined1 uVar6;
  int iVar7;
  char *local_30 [6];
  
  *(undefined1 *)(param_1 + 0x23) = 0;
  *(undefined1 *)(param_1 + 0x22) = 0;
  iVar3 = FUN_000251f8(&DAT_0002f268);
  if (iVar3 == 0) {
    iVar7 = 0;
    iVar3 = 0x2f278;
    do {
      cVar5 = FUN_00023c7c(&DAT_0002f268 + iVar7 * 0x54,"release");
      iVar4 = iVar3 + 0x10;
      pcVar2 = local_30[0];
      if (cVar5 != '\0') {
        if (0xf < *(uint *)(iVar3 + 0x24)) {
          iVar4 = *(int *)(iVar3 + 0x10);
        }
        uVar6 = FUN_00027df0(iVar4,local_30,10);
        *(undefined1 *)(param_1 + 0x22) = uVar6;
        pcVar2 = local_30[0] + 1;
        if (*local_30[0] == '.') {
          iVar3 = FUN_00027df0(local_30[0] + 1,0,10);
          *(char *)(param_1 + 0x23) = (char)(iVar3 / 100);
          return 0;
        }
      }
      local_30[0] = pcVar2;
      bVar1 = iVar7 != 0x13;
      iVar7 = iVar7 + 1;
      iVar3 = iVar3 + 0x54;
    } while (bVar1);
  }
  return 0;
}



int FUN_00023dc8(int param_1)

{
  int iVar1;
  undefined1 local_110;
  undefined1 local_10f;
  undefined1 local_10e;
  undefined1 local_10d;
  undefined1 local_10c;
  undefined1 local_10b;
  
  iVar1 = FUN_00024900(2,&local_110);
  if (-1 < iVar1) {
    *(undefined1 *)(param_1 + 0x1c) = local_110;
    *(undefined1 *)(param_1 + 0x21) = local_10b;
    *(undefined1 *)(param_1 + 0x1d) = local_10f;
    *(undefined1 *)(param_1 + 0x1e) = local_10e;
    *(undefined1 *)(param_1 + 0x1f) = local_10d;
    *(undefined1 *)(param_1 + 0x20) = local_10c;
  }
  return iVar1;
}



longlong FUN_00023e3c(void)

{
  ulonglong uVar1;
  ulonglong uVar2;
  
  uVar2 = FUN_00024628(1,&DAT_0002f8fc);
  uVar1 = (ulonglong)((int)uVar2 >> 0x1f);
  uVar1 = uVar1 - (uVar1 ^ uVar2);
  return (longlong)(uVar1 << 0x20 | uVar1 >> 0x20) >> 0x3f;
}



undefined8 FUN_00023e7c(void)

{
  FUN_00024660(DAT_0002f8fc);
  return 0;
}



int FUN_00023ea8(int param_1,int param_2)

{
  int iVar1;
  uint uVar2;
  int iVar3;
  undefined1 auStack_130 [256];
  
  iVar3 = 0;
  do {
    iVar1 = FUN_00024900(0x10,auStack_130);
    if (-1 < iVar1) {
      FUN_00029408(auStack_130,param_1 + *(int *)(param_1 + 0x18) * 4);
      iVar3 = FUN_00024900(0x11,auStack_130);
      if (iVar3 < 0) {
        return -0x7ffd67ea;
      }
      FUN_00029408(auStack_130,param_1 + *(int *)(param_1 + 0x18) * 4 + 8);
      iVar1 = *(int *)(param_1 + 0x18);
      *(int *)(param_1 + 0x18) = iVar1 + 1;
      *(undefined4 *)(param_1 + iVar1 * 4 + 0x10) = 0;
      return iVar3;
    }
    syscall();
    iVar3 = iVar3 + 1;
  } while (iVar3 < param_2);
  if (iVar1 == -0x7fecfef8) {
    iVar3 = -0x7ffd67ec;
  }
  else {
    uVar2 = FUN_00028100(500000);
    iVar3 = -0x7ffd67eb - ((int)~uVar2 >> 0x1f);
  }
  return iVar3;
}



uint FUN_00023ff4(undefined4 *param_1)

{
  bool bVar1;
  undefined4 uVar3;
  int iVar4;
  undefined8 uVar2;
  int iVar5;
  uint uVar6;
  int local_130;
  undefined1 auStack_12c [16];
  undefined1 local_11c [84];
  undefined1 auStack_c8 [16];
  undefined1 auStack_b8 [28];
  char local_9c;
  
  FUN_00027ae0(param_1,0,0xa4);
  uVar3 = FUN_00024698();
  FUN_00023690(param_1,uVar3);
  FUN_00023cb4(param_1);
  FUN_00023dc8(param_1);
  iVar4 = FUN_000238cc(uVar3);
  if (iVar4 == 0) {
    uVar2 = 0x14;
    goto LAB_00024228;
  }
  FUN_00023e3c();
  iVar4 = FUN_00024890(&local_130);
  if ((iVar4 == 0) && (local_130 == 2)) {
LAB_00024188:
    uVar2 = FUN_000248c8(1,auStack_12c);
    if (-1 < (int)uVar2) {
      FUN_00029408(auStack_12c,param_1 + param_1[6]);
      uVar2 = FUN_000248c8(2,auStack_12c);
      if (-1 < (int)uVar2) {
        uVar2 = FUN_00029408(auStack_12c,param_1 + param_1[6] + 2);
        iVar4 = param_1[6];
        param_1[6] = iVar4 + 1;
        param_1[iVar4 + 4] = 1;
      }
    }
  }
  else {
    uVar2 = FUN_00024970(0x4000,0x7d7);
    if (-1 < (int)uVar2) {
      FUN_00027ae0(local_11c,0,0xfc);
      uVar2 = FUN_000252a0(&DAT_0002f8fc,local_11c);
      if (((int)uVar2 == 0) && (local_9c != '\0')) {
        if (0 < (int)param_1[6]) {
          uVar3 = FUN_000292b8(*param_1);
          FUN_00027c30(auStack_c8,uVar3,0x10);
          uVar3 = FUN_000292b8(param_1[2]);
          FUN_00027c30(auStack_b8,uVar3,0x10);
        }
        uVar2 = FUN_00024820(local_11c);
        if (-1 < (int)uVar2) {
          iVar4 = 0;
          while( true ) {
            iVar5 = FUN_00024890(&local_130);
            bVar1 = iVar4 == 0x14;
            uVar2 = 500000;
            iVar4 = iVar4 + 1;
            if ((-1 < iVar5) && (local_130 == 2)) goto LAB_00024188;
            if (bVar1) break;
            syscall();
          }
          if ((-1 < iVar5) && (local_130 == 2)) goto LAB_00024188;
        }
      }
    }
  }
  FUN_00023e7c(uVar2);
  uVar2 = 1;
LAB_00024228:
  uVar6 = FUN_00023ea8(param_1,uVar2);
  DAT_0002f8f8 = 1;
  return uVar6 & (int)(param_1[6] | param_1[6] - 1) >> 0x1f;
}



undefined4 FUN_00024270(undefined4 param_1)

{
  int iVar1;
  undefined4 uVar2;
  
  iVar1 = FUN_000247b0();
  uVar2 = 0x80029802;
  if (iVar1 != 0) {
    uVar2 = (*(code *)**(undefined4 **)(iVar1 + 0x24))(param_1);
  }
  return uVar2;
}



undefined4 FUN_000242d8(void)

{
  int iVar1;
  undefined4 uVar2;
  
  iVar1 = FUN_000247b0();
  uVar2 = 0x80029802;
  if (iVar1 != 0) {
    uVar2 = (*(code *)**(undefined4 **)(iVar1 + 4))(&DAT_0002f900);
  }
  return uVar2;
}



undefined4 FUN_00024338(void)

{
  int iVar1;
  undefined4 uVar2;
  
  iVar1 = FUN_00024740();
  uVar2 = 0x80029802;
  if (iVar1 != 0) {
    (*(code *)**(undefined4 **)(iVar1 + 0x14))();
    uVar2 = 0;
  }
  return uVar2;
}



undefined4 FUN_00024390(void)

{
  int iVar1;
  undefined4 uVar2;
  
  iVar1 = FUN_00024740();
  uVar2 = 0x80029802;
  if (iVar1 != 0) {
    (*(code *)**(undefined4 **)(iVar1 + 0x10))();
    uVar2 = 0;
  }
  return uVar2;
}



undefined8 FUN_000243e8(void)

{
  undefined4 uVar1;
  int iVar2;
  int local_10 [4];
  
  if (DAT_0002f8f8 != '\0') {
    uVar1 = FUN_00024698();
    iVar2 = FUN_000238cc(uVar1);
    if (iVar2 != 0) {
      FUN_00024938();
      while ((iVar2 = FUN_00024890(local_10), -1 < iVar2 && (local_10[0] != 0))) {
        syscall();
      }
      FUN_00024858(1000);
    }
    DAT_0002f8f8 = '\0';
  }
  return 0;
}



int FUN_00024470(void)

{
  int iVar1;
  longlong local_10;
  longlong local_8;
  
  iVar1 = FUN_00027fb0(0x5c,&local_10,0,0);
  if (-1 < iVar1) {
    syscall();
    iVar1 = FUN_00027fe8(0x5d,local_8 - local_10,0,0);
  }
  return iVar1;
}



undefined4 FUN_000244e4(undefined8 param_1,undefined4 param_2)

{
  undefined4 uVar1;
  
  FUN_00027f78();
  uVar1 = FUN_00028250(param_2,0,0);
  return uVar1;
}



int FUN_00024524(int *param_1,undefined4 *param_2,undefined2 *param_3,undefined2 *param_4)

{
  int iVar1;
  int local_f0;
  int local_ec;
  undefined2 local_e8;
  undefined2 local_e6;
  undefined1 auStack_e0 [12];
  undefined4 local_d4;
  
  iVar1 = FUN_00027f40(auStack_e0);
  if (iVar1 == 0) {
    if (param_2 != (undefined4 *)0x0) {
      *param_2 = local_d4;
    }
    iVar1 = FUN_00028020(&local_f0);
    if (iVar1 == 0) {
      if ((local_ec == 3) && (local_f0 != 2)) {
        *param_1 = 2;
      }
      else {
        *param_1 = local_ec;
        if (param_3 != (undefined2 *)0x0) {
          *param_3 = local_e8;
        }
        if (param_4 != (undefined2 *)0x0) {
          *param_4 = local_e6;
        }
      }
    }
  }
  return iVar1;
}



void FUN_00024628(void)

{
  (**(code **)PTR_FUN_0002dc04)();
  return;
}



void FUN_00024660(void)

{
  (**(code **)PTR_FUN_0002dc08)();
  return;
}



void FUN_00024698(void)

{
  (**(code **)PTR_FUN_0002dc0c)();
  return;
}



void FUN_000246d0(void)

{
  (**(code **)PTR_FUN_0002dc10)();
  return;
}



void FUN_00024740(void)

{
  (**(code **)PTR_FUN_0002dc18)();
  return;
}



void FUN_000247b0(void)

{
  (**(code **)PTR_FUN_0002dc20)();
  return;
}



void FUN_000247e8(void)

{
  (**(code **)PTR_FUN_0002dc24)();
  return;
}



void FUN_00024820(void)

{
  (**(code **)PTR_FUN_0002d700)();
  return;
}



void FUN_00024858(void)

{
  (**(code **)PTR_FUN_0002d704)();
  return;
}



void FUN_00024890(void)

{
  (**(code **)PTR_FUN_0002d708)();
  return;
}



void FUN_000248c8(void)

{
  (**(code **)PTR_FUN_0002d70c)();
  return;
}



void FUN_00024900(void)

{
  (**(code **)PTR_FUN_0002d710)();
  return;
}



void FUN_00024938(void)

{
  (**(code **)PTR_FUN_0002d714)();
  return;
}



void FUN_00024970(void)

{
  (**(code **)PTR_FUN_0002d718)();
  return;
}



void FUN_000249a8(void)

{
  (**(code **)PTR_FUN_0002d99c)();
  return;
}



void FUN_000249e0(void)

{
  (**(code **)PTR_FUN_0002d9a0)();
  return;
}



void FUN_00024a18(void)

{
  (**(code **)PTR_FUN_0002d9a4)();
  return;
}



void FUN_00024a50(void)

{
  (**(code **)PTR_FUN_0002d9a8)();
  return;
}



void FUN_00024a88(void)

{
  (**(code **)PTR_FUN_0002d9ac)();
  return;
}



void FUN_00024ac0(void)

{
  (**(code **)PTR_FUN_0002d9b0)();
  return;
}



void FUN_00024af8(void)

{
  (**(code **)PTR_FUN_0002d9b4)();
  return;
}



void FUN_00024b30(void)

{
  (**(code **)PTR_FUN_0002d9b8)();
  return;
}



void FUN_00024b68(void)

{
  (**(code **)PTR_FUN_0002d9bc)();
  return;
}



void FUN_00024ba0(void)

{
  (**(code **)PTR_FUN_0002d9c0)();
  return;
}



void FUN_00024bd8(void)

{
  (**(code **)PTR_FUN_0002d9c4)();
  return;
}



void FUN_00024c10(void)

{
  (**(code **)PTR_FUN_0002d9c8)();
  return;
}



void FUN_00024c48(void)

{
  (**(code **)PTR_FUN_0002d9cc)();
  return;
}



void FUN_00024c80(void)

{
  (**(code **)PTR_FUN_0002d9d0)();
  return;
}



void FUN_00024cb8(void)

{
  (**(code **)PTR_FUN_0002d9d4)();
  return;
}



void FUN_00024cf0(void)

{
  (**(code **)PTR_FUN_0002d9d8)();
  return;
}



void FUN_00024d28(void)

{
  (**(code **)PTR_FUN_0002d9dc)();
  return;
}



void FUN_00024d60(void)

{
  (**(code **)PTR_FUN_0002d9e0)();
  return;
}



void FUN_00024d98(void)

{
  (**(code **)PTR_FUN_0002d9e4)();
  return;
}



void FUN_00024dd0(void)

{
  (**(code **)PTR_FUN_0002d9e8)();
  return;
}



void FUN_00024e08(void)

{
  (**(code **)PTR_FUN_0002d9ec)();
  return;
}



void FUN_00024e40(void)

{
  (**(code **)PTR_FUN_0002d9f0)();
  return;
}



void FUN_00024e78(void)

{
  (**(code **)PTR_FUN_0002d9f4)();
  return;
}



void FUN_00024eb0(void)

{
  (**(code **)PTR_FUN_0002d9f8)();
  return;
}



void FUN_00024ee8(void)

{
  (**(code **)PTR_FUN_0002d9fc)();
  return;
}



void FUN_00024f20(void)

{
  (**(code **)PTR_FUN_0002da00)();
  return;
}



void FUN_00024f58(void)

{
  (**(code **)PTR_FUN_0002da04)();
  return;
}



void FUN_00024f90(void)

{
  (**(code **)PTR_FUN_0002da08)();
  return;
}



void FUN_00024fc8(void)

{
  (**(code **)PTR_FUN_0002da0c)();
  return;
}



void FUN_00025000(void)

{
  (**(code **)PTR_FUN_0002db6c)();
  return;
}



void FUN_00025038(void)

{
  (**(code **)PTR_FUN_0002db70)();
  return;
}



void FUN_00025070(void)

{
  (**(code **)PTR_FUN_0002db74)();
  return;
}



void FUN_000250e0(void)

{
  (**(code **)PTR_FUN_0002db7c)();
  return;
}



void FUN_00025118(void)

{
  (**(code **)PTR_FUN_0002db80)();
  return;
}



void FUN_00025188(void)

{
  (**(code **)PTR_FUN_0002db88)();
  return;
}



void FUN_000251c0(void)

{
  (**(code **)PTR_FUN_0002db8c)();
  return;
}



void FUN_000251f8(void)

{
  (**(code **)PTR_FUN_0002db90)();
  return;
}



void FUN_00025230(void)

{
  (**(code **)PTR_FUN_0002db94)();
  return;
}



void FUN_00025268(void)

{
  (**(code **)PTR_FUN_0002db98)();
  return;
}



void FUN_000252a0(void)

{
  (**(code **)PTR_FUN_0002db9c)();
  return;
}



void FUN_000252d8(void)

{
  (**(code **)PTR_FUN_0002dba0)();
  return;
}



void FUN_00025310(void)

{
  (**(code **)PTR_FUN_0002dba4)();
  return;
}



void FUN_00025348(void)

{
  (**(code **)PTR_FUN_0002dba8)();
  return;
}



void FUN_00025380(void)

{
  (**(code **)PTR_FUN_0002dbac)();
  return;
}



void FUN_000253b8(void)

{
  (**(code **)PTR_FUN_0002dbb0)();
  return;
}



void FUN_000253f0(void)

{
  (**(code **)PTR_FUN_0002db38)();
  return;
}



void FUN_00025428(void)

{
  (**(code **)PTR_FUN_0002db3c)();
  return;
}



void FUN_00025460(void)

{
  (**(code **)PTR_FUN_0002db40)();
  return;
}



void FUN_00025498(void)

{
  (**(code **)PTR_FUN_0002db44)();
  return;
}



void FUN_000254d0(void)

{
  (**(code **)PTR_FUN_0002db48)();
  return;
}



void FUN_00025508(void)

{
  (**(code **)PTR_FUN_0002db4c)();
  return;
}



void FUN_00025578(void)

{
  (**(code **)PTR_FUN_0002db54)();
  return;
}



void FUN_000255b0(void)

{
  (**(code **)PTR_FUN_0002db58)();
  return;
}



void FUN_000255e8(void)

{
  (**(code **)PTR_FUN_0002db5c)();
  return;
}



void FUN_00025620(void)

{
  (**(code **)PTR_FUN_0002db60)();
  return;
}



void FUN_00025658(void)

{
  (**(code **)PTR_FUN_0002db64)();
  return;
}



void FUN_00025690(void)

{
  (**(code **)PTR_FUN_0002db68)();
  return;
}



void FUN_00025770(void)

{
  (**(code **)PTR_FUN_0002d728)();
  return;
}



void FUN_000257e0(void)

{
  (**(code **)PTR_FUN_0002d730)();
  return;
}



void FUN_00025888(void)

{
  (**(code **)PTR_FUN_0002d73c)();
  return;
}



void FUN_000258c0(void)

{
  (**(code **)PTR_FUN_0002d740)();
  return;
}



void FUN_000258f8(void)

{
  (**(code **)PTR_FUN_0002d744)();
  return;
}



void FUN_00025968(void)

{
  (**(code **)PTR_FUN_0002d74c)();
  return;
}



void FUN_000259d8(void)

{
  (**(code **)PTR_FUN_0002d754)();
  return;
}



void FUN_00025a48(void)

{
  (**(code **)PTR_FUN_0002d75c)();
  return;
}



void FUN_00025a80(void)

{
  (**(code **)PTR_FUN_0002d760)();
  return;
}



void FUN_00025af0(void)

{
  (**(code **)PTR_FUN_0002d768)();
  return;
}



void FUN_00025b98(void)

{
  (**(code **)PTR_FUN_0002d774)();
  return;
}



void FUN_00025c40(void)

{
  (**(code **)PTR_FUN_0002d780)();
  return;
}



void FUN_00025cb0(void)

{
  (**(code **)PTR_FUN_0002d788)();
  return;
}



void FUN_00025d20(void)

{
  (**(code **)PTR_FUN_0002d790)();
  return;
}



void FUN_00025d90(void)

{
  (**(code **)PTR_FUN_0002d798)();
  return;
}



void FUN_00025e00(void)

{
  (**(code **)PTR_FUN_0002d7a0)();
  return;
}



void FUN_00025e38(void)

{
  (**(code **)PTR_FUN_0002d7a4)();
  return;
}



void FUN_00025e70(void)

{
  (**(code **)PTR_FUN_0002d7a8)();
  return;
}



void FUN_00025ea8(void)

{
  (**(code **)PTR_FUN_0002d7ac)();
  return;
}



void FUN_00025ee0(void)

{
  (**(code **)PTR_FUN_0002d7b0)();
  return;
}



void FUN_00025f88(void)

{
  (**(code **)PTR_FUN_0002d7bc)();
  return;
}



void FUN_00025fc0(void)

{
  (**(code **)PTR_FUN_0002d7c0)();
  return;
}



void FUN_00025ff8(void)

{
  (**(code **)PTR_FUN_0002d7c4)();
  return;
}



void FUN_00026030(void)

{
  (**(code **)PTR_FUN_0002d7c8)();
  return;
}



void FUN_000260d8(void)

{
  (**(code **)PTR_FUN_0002d7d4)();
  return;
}



void FUN_00026110(void)

{
  (**(code **)PTR_FUN_0002d7d8)();
  return;
}



void FUN_000261b8(void)

{
  (**(code **)PTR_FUN_0002d7e4)();
  return;
}



void FUN_000261f0(void)

{
  (**(code **)PTR_FUN_0002d7e8)();
  return;
}



void FUN_000262d0(void)

{
  (**(code **)PTR_FUN_0002d7f8)();
  return;
}



void FUN_00026378(void)

{
  (**(code **)PTR_FUN_0002d804)();
  return;
}



void FUN_000264c8(void)

{
  (**(code **)PTR_FUN_0002d81c)();
  return;
}



void FUN_00026570(void)

{
  (**(code **)PTR_FUN_0002d828)();
  return;
}



void FUN_00026688(void)

{
  (**(code **)PTR_FUN_0002d83c)();
  return;
}



void FUN_000266c0(void)

{
  (**(code **)PTR_FUN_0002d840)();
  return;
}



void FUN_000266f8(void)

{
  (**(code **)PTR_FUN_0002d844)();
  return;
}



void FUN_00026730(void)

{
  (**(code **)PTR_FUN_0002d848)();
  return;
}



void FUN_00026810(void)

{
  (**(code **)PTR_FUN_0002d858)();
  return;
}



void FUN_00026848(void)

{
  (**(code **)PTR_FUN_0002d85c)();
  return;
}



void FUN_00026928(void)

{
  (**(code **)PTR_FUN_0002d86c)();
  return;
}



void FUN_00026960(void)

{
  (**(code **)PTR_FUN_0002d870)();
  return;
}



void FUN_00026998(void)

{
  (**(code **)PTR_FUN_0002d874)();
  return;
}



void FUN_000269d0(void)

{
  (**(code **)PTR_FUN_0002d878)();
  return;
}



void FUN_00026a08(void)

{
  (**(code **)PTR_FUN_0002d87c)();
  return;
}



void FUN_00026a40(void)

{
  (**(code **)PTR_FUN_0002d880)();
  return;
}



void FUN_00026ae8(void)

{
  (**(code **)PTR_FUN_0002d88c)();
  return;
}



void FUN_00026b58(void)

{
  (**(code **)PTR_FUN_0002d894)();
  return;
}



void FUN_00026b90(void)

{
  (**(code **)PTR_FUN_0002d898)();
  return;
}



void FUN_00026c38(void)

{
  (**(code **)PTR_FUN_0002d8a4)();
  return;
}



void FUN_00026c70(void)

{
  (**(code **)PTR_FUN_0002d8a8)();
  return;
}



void FUN_00026ca8(void)

{
  (**(code **)PTR_FUN_0002d8ac)();
  return;
}



void FUN_00026d18(void)

{
  (**(code **)PTR_FUN_0002d8b4)();
  return;
}



void FUN_00026dc0(void)

{
  (**(code **)PTR_FUN_0002d8c0)();
  return;
}



void FUN_00026e30(void)

{
  (**(code **)PTR_FUN_0002d8c8)();
  return;
}



void FUN_00026f48(void)

{
  (**(code **)PTR_FUN_0002d8dc)();
  return;
}



void FUN_00026fb8(void)

{
  (**(code **)PTR_FUN_0002d8e4)();
  return;
}



void FUN_00026ff0(void)

{
  (**(code **)PTR_FUN_0002d8e8)();
  return;
}



void FUN_00027028(void)

{
  (**(code **)PTR_FUN_0002d8ec)();
  return;
}



void FUN_00027098(void)

{
  (**(code **)PTR_FUN_0002d8f4)();
  return;
}



void FUN_00027140(void)

{
  (**(code **)PTR_FUN_0002d900)();
  return;
}



void FUN_00027178(void)

{
  (**(code **)PTR_FUN_0002d904)();
  return;
}



void FUN_000271b0(void)

{
  (**(code **)PTR_FUN_0002d908)();
  return;
}



void FUN_000271e8(void)

{
  (**(code **)PTR_FUN_0002d90c)();
  return;
}



void FUN_00027220(void)

{
  (**(code **)PTR_FUN_0002d910)();
  return;
}



void FUN_00027290(void)

{
  (**(code **)PTR_FUN_0002d918)();
  return;
}



void FUN_00027338(void)

{
  (**(code **)PTR_FUN_0002d924)();
  return;
}



void FUN_000273a8(void)

{
  (**(code **)PTR_FUN_0002d92c)();
  return;
}



void FUN_000273e0(void)

{
  (**(code **)PTR_FUN_0002d930)();
  return;
}



void FUN_00027418(void)

{
  (**(code **)PTR_FUN_0002d934)();
  return;
}



void FUN_000274c0(void)

{
  (**(code **)PTR_FUN_0002d940)();
  return;
}



void FUN_00027530(void)

{
  (**(code **)PTR_FUN_0002d948)();
  return;
}



void FUN_000275a0(void)

{
  (**(code **)PTR_FUN_0002d950)();
  return;
}



void FUN_00027610(void)

{
  (**(code **)PTR_FUN_0002d958)();
  return;
}



void FUN_00027680(void)

{
  (**(code **)PTR_FUN_0002d960)();
  return;
}



void FUN_000276f0(void)

{
  (**(code **)PTR_FUN_0002d968)();
  return;
}



void FUN_00027728(void)

{
  (**(code **)PTR_FUN_0002d96c)();
  return;
}



void FUN_00027760(void)

{
  (**(code **)PTR_FUN_0002d970)();
  return;
}



void FUN_000277d0(void)

{
  (**(code **)PTR_FUN_0002d978)();
  return;
}



void FUN_00027840(void)

{
  (**(code **)PTR_FUN_0002d980)();
  return;
}



void FUN_00027878(void)

{
  (**(code **)PTR_FUN_0002d984)();
  return;
}



void FUN_00027958(void)

{
  (**(code **)PTR_FUN_0002d994)();
  return;
}



void FUN_00027990(void)

{
  (**(code **)PTR_FUN_0002d998)();
  return;
}



void FUN_000279c8(void)

{
  (**(code **)PTR_FUN_0002da10)();
  return;
}



void FUN_00027a00(void)

{
  (**(code **)PTR_FUN_0002da14)();
  return;
}



void FUN_00027a38(void)

{
  (**(code **)PTR_FUN_0002da18)();
  return;
}



void FUN_00027a70(void)

{
  (**(code **)PTR_FUN_0002da1c)();
  return;
}



void FUN_00027aa8(void)

{
  (**(code **)PTR_FUN_0002da20)();
  return;
}



void FUN_00027ae0(void)

{
  (**(code **)PTR_FUN_0002da24)();
  return;
}



void FUN_00027b18(void)

{
  (**(code **)PTR_FUN_0002da28)();
  return;
}



void FUN_00027b50(void)

{
  (**(code **)PTR_FUN_0002da2c)();
  return;
}



void FUN_00027b88(void)

{
  (**(code **)PTR_FUN_0002da30)();
  return;
}



void FUN_00027bc0(void)

{
  (**(code **)PTR_FUN_0002da34)();
  return;
}



void FUN_00027bf8(void)

{
  (**(code **)PTR_FUN_0002da38)();
  return;
}



void FUN_00027c30(void)

{
  (**(code **)PTR_FUN_0002da3c)();
  return;
}



void FUN_00027c68(void)

{
  (**(code **)PTR_FUN_0002da40)();
  return;
}



void FUN_00027ca0(void)

{
  (**(code **)PTR_FUN_0002da44)();
  return;
}



void FUN_00027cd8(void)

{
  (**(code **)PTR_FUN_0002da48)();
  return;
}



void FUN_00027d10(void)

{
  (**(code **)PTR_FUN_0002da4c)();
  return;
}



void FUN_00027d48(void)

{
  (**(code **)PTR_FUN_0002da50)();
  return;
}



void FUN_00027d80(void)

{
  (**(code **)PTR_FUN_0002da54)();
  return;
}



void FUN_00027db8(void)

{
  (**(code **)PTR_FUN_0002da58)();
  return;
}



void FUN_00027df0(void)

{
  (**(code **)PTR_FUN_0002da5c)();
  return;
}



void FUN_00027e28(void)

{
  (**(code **)PTR_FUN_0002db34)();
  return;
}



void FUN_00027e60(void)

{
  (**(code **)PTR_FUN_0002d5f8)();
  return;
}



void FUN_00027e98(void)

{
  (**(code **)PTR_FUN_0002d5fc)();
  return;
}



void FUN_00027ed0(void)

{
  (**(code **)PTR_FUN_0002d600)();
  return;
}



void FUN_00027f40(void)

{
  (**(code **)PTR_FUN_0002dbb8)();
  return;
}



void FUN_00027f78(void)

{
  (**(code **)PTR_FUN_0002dbbc)();
  return;
}



void FUN_00027fb0(void)

{
  (**(code **)PTR_FUN_0002dbc0)();
  return;
}



void FUN_00027fe8(void)

{
  (**(code **)PTR_FUN_0002dbc4)();
  return;
}



void FUN_00028020(void)

{
  (**(code **)PTR_FUN_0002dbc8)();
  return;
}



void FUN_000280c8(void)

{
  (**(code **)PTR_FUN_0002dbd4)();
  return;
}



void FUN_00028100(void)

{
  (**(code **)PTR_FUN_0002dbd8)();
  return;
}



void FUN_00028250(void)

{
  (**(code **)PTR_FUN_0002dbf0)();
  return;
}



void FUN_00028368(void)

{
  (**(code **)PTR_FUN_0002d618)();
  return;
}



void FUN_000283a0(void)

{
  (**(code **)PTR_FUN_0002d61c)();
  return;
}



void FUN_000283d8(void)

{
  (**(code **)PTR_FUN_0002d620)();
  return;
}



void FUN_00028410(void)

{
  (**(code **)PTR_FUN_0002d624)();
  return;
}



void FUN_00028448(void)

{
  (**(code **)PTR_FUN_0002d628)();
  return;
}



void FUN_00028480(void)

{
  (**(code **)PTR_FUN_0002d62c)();
  return;
}



void FUN_000284b8(void)

{
  (**(code **)PTR_FUN_0002d630)();
  return;
}



void FUN_000284f0(void)

{
  (**(code **)PTR_FUN_0002d634)();
  return;
}



void FUN_00028528(void)

{
  (**(code **)PTR_FUN_0002d638)();
  return;
}



void FUN_00028560(void)

{
  (**(code **)PTR_FUN_0002d63c)();
  return;
}



void FUN_00028598(void)

{
  (**(code **)PTR_FUN_0002d640)();
  return;
}



void FUN_000285d0(void)

{
  (**(code **)PTR_FUN_0002d644)();
  return;
}



void FUN_00028608(void)

{
  (**(code **)PTR_FUN_0002d648)();
  return;
}



void FUN_00028640(void)

{
  (**(code **)PTR_FUN_0002d64c)();
  return;
}



void FUN_00028678(void)

{
  (**(code **)PTR_FUN_0002d650)();
  return;
}



void FUN_000286b0(void)

{
  (**(code **)PTR_FUN_0002d654)();
  return;
}



void FUN_000286e8(void)

{
  (**(code **)PTR_FUN_0002d658)();
  return;
}



void FUN_00028720(void)

{
  (**(code **)PTR_FUN_0002d65c)();
  return;
}



void FUN_00028758(void)

{
  (**(code **)PTR_FUN_0002d664)();
  return;
}



void FUN_00028790(void)

{
  (**(code **)PTR_FUN_0002d668)();
  return;
}



void FUN_000287c8(void)

{
  (**(code **)PTR_FUN_0002d66c)();
  return;
}



void FUN_00028800(void)

{
  (**(code **)PTR_FUN_0002d670)();
  return;
}



void FUN_00028838(void)

{
  (**(code **)PTR_FUN_0002d674)();
  return;
}



void FUN_00028870(void)

{
  (**(code **)PTR_FUN_0002d678)();
  return;
}



void FUN_000288a8(void)

{
  (**(code **)PTR_FUN_0002d67c)();
  return;
}



void FUN_000288e0(void)

{
  (**(code **)PTR_FUN_0002d5e0)();
  return;
}



void FUN_00028918(void)

{
  (**(code **)PTR_FUN_0002d5e4)();
  return;
}



void FUN_00028950(void)

{
  (**(code **)PTR_FUN_0002d5e8)();
  return;
}



void FUN_00028988(void)

{
  (**(code **)PTR_FUN_0002d5ec)();
  return;
}



void FUN_000289c0(void)

{
  (**(code **)PTR_FUN_0002d5f0)();
  return;
}



void FUN_000289f8(void)

{
  (**(code **)PTR_FUN_0002d5f4)();
  return;
}



void FUN_00028a30(void)

{
  (**(code **)PTR_FUN_0002d69c)();
  return;
}



void FUN_00028a68(void)

{
  (**(code **)PTR_FUN_0002d6a0)();
  return;
}



void FUN_00028aa0(void)

{
  (**(code **)PTR_FUN_0002d6a4)();
  return;
}



void FUN_00028ad8(void)

{
  (**(code **)PTR_FUN_0002d6a8)();
  return;
}



void FUN_00028b10(void)

{
  (**(code **)PTR_FUN_0002d6ac)();
  return;
}



void FUN_00028b48(void)

{
  (**(code **)PTR_FUN_0002d6b0)();
  return;
}



void FUN_00028b80(void)

{
  (**(code **)PTR_FUN_0002d6b4)();
  return;
}



void FUN_00028bb8(void)

{
  (**(code **)PTR_FUN_0002d6b8)();
  return;
}



void FUN_00028bf0(void)

{
  (**(code **)PTR_FUN_0002d6bc)();
  return;
}



void FUN_00028c28(void)

{
  (**(code **)PTR_FUN_0002d6c0)();
  return;
}



void FUN_00028c60(void)

{
  (**(code **)PTR_FUN_0002d6c4)();
  return;
}



void FUN_00028c98(void)

{
  (**(code **)PTR_FUN_0002d6c8)();
  return;
}



void FUN_00028cd0(void)

{
  (**(code **)PTR_FUN_0002d6cc)();
  return;
}



void FUN_00028d08(void)

{
  (**(code **)PTR_FUN_0002d6d0)();
  return;
}



void FUN_00028d40(void)

{
  (**(code **)PTR_FUN_0002d6d4)();
  return;
}



void FUN_00028d78(void)

{
  (**(code **)PTR_FUN_0002d6d8)();
  return;
}



void FUN_00028db0(void)

{
  (**(code **)PTR_FUN_0002d6dc)();
  return;
}



void FUN_00028de8(void)

{
  (**(code **)PTR_FUN_0002d6e0)();
  return;
}



void FUN_00028e20(void)

{
  (**(code **)PTR_FUN_0002d680)();
  return;
}



void FUN_00028e58(void)

{
  (**(code **)PTR_FUN_0002d684)();
  return;
}



void FUN_00028e90(void)

{
  (**(code **)PTR_FUN_0002d688)();
  return;
}



void FUN_00028ec8(void)

{
  (**(code **)PTR_FUN_0002d68c)();
  return;
}



void FUN_00028f00(void)

{
  (**(code **)PTR_FUN_0002d690)();
  return;
}



void FUN_00028f38(void)

{
  (**(code **)PTR_FUN_0002d694)();
  return;
}



void FUN_00028f70(void)

{
  (**(code **)PTR_FUN_0002d698)();
  return;
}



void FUN_00028fa8(void)

{
  (**(code **)PTR_FUN_0002d604)();
  return;
}



void FUN_00028fe0(void)

{
  (**(code **)PTR_FUN_0002d608)();
  return;
}



void FUN_00029018(void)

{
  (**(code **)PTR_FUN_0002d60c)();
  return;
}



void FUN_00029050(void)

{
  (**(code **)PTR_FUN_0002d610)();
  return;
}



void FUN_00029088(void)

{
  (**(code **)PTR_FUN_0002d614)();
  return;
}



void FUN_000290c0(void)

{
  (**(code **)PTR_FUN_0002dadc)();
  return;
}



void FUN_000290f8(void)

{
  (**(code **)PTR_FUN_0002dae0)();
  return;
}



void FUN_00029130(void)

{
  (**(code **)PTR_FUN_0002dae4)();
  return;
}



void FUN_00029168(void)

{
  (**(code **)PTR_FUN_0002dae8)();
  return;
}



void FUN_000291a0(void)

{
  (**(code **)PTR_FUN_0002daec)();
  return;
}



void FUN_000291d8(void)

{
  (**(code **)PTR_FUN_0002daf0)();
  return;
}



void FUN_00029210(void)

{
  (**(code **)PTR_FUN_0002daf4)();
  return;
}



void FUN_00029248(void)

{
  (**(code **)PTR_FUN_0002daf8)();
  return;
}



void FUN_00029280(void)

{
  (**(code **)PTR_FUN_0002dafc)();
  return;
}



void FUN_000292b8(void)

{
  (**(code **)PTR_FUN_0002db00)();
  return;
}



void FUN_000292f0(void)

{
  (**(code **)PTR_FUN_0002db04)();
  return;
}



void FUN_00029328(void)

{
  (**(code **)PTR_FUN_0002db08)();
  return;
}



void FUN_00029360(void)

{
  (**(code **)PTR_FUN_0002db0c)();
  return;
}



void FUN_00029398(void)

{
  (**(code **)PTR_FUN_0002db10)();
  return;
}



void FUN_000293d0(void)

{
  (**(code **)PTR_FUN_0002db14)();
  return;
}



void FUN_00029408(void)

{
  (**(code **)PTR_FUN_0002db18)();
  return;
}



void FUN_00029440(void)

{
  (**(code **)PTR_FUN_0002db1c)();
  return;
}



void FUN_00029478(void)

{
  (**(code **)PTR_FUN_0002db20)();
  return;
}



void FUN_000294b0(void)

{
  (**(code **)PTR_FUN_0002db24)();
  return;
}



void FUN_000294e8(void)

{
  (**(code **)PTR_FUN_0002db28)();
  return;
}



void FUN_00029520(void)

{
  (**(code **)PTR_FUN_0002db2c)();
  return;
}



void FUN_00029558(void)

{
  (**(code **)PTR_FUN_0002db30)();
  return;
}



void FUN_00029590(void)

{
  (**(code **)PTR_FUN_0002d660)();
  return;
}



void FUN_000295c8(void)

{
  (**(code **)PTR_FUN_0002d6e4)();
  return;
}



void FUN_00029600(void)

{
  (**(code **)PTR_FUN_0002d6e8)();
  return;
}



void FUN_00029638(void)

{
  (**(code **)PTR_FUN_0002d6ec)();
  return;
}



void FUN_00029670(void)

{
  (**(code **)PTR_FUN_0002d6f0)();
  return;
}



void FUN_000296a8(void)

{
  (**(code **)PTR_FUN_0002d6f4)();
  return;
}



void FUN_000296e0(void)

{
  (**(code **)PTR_FUN_0002d6f8)();
  return;
}



void FUN_00029718(void)

{
  (**(code **)PTR_FUN_0002d6fc)();
  return;
}



void FUN_00029750(void)

{
  (**(code **)PTR_FUN_0002dad8)();
  return;
}



void FUN_00029788(void)

{
  (**(code **)PTR_FUN_0002da60)();
  return;
}



void FUN_000297c0(void)

{
  (**(code **)PTR_FUN_0002da64)();
  return;
}



void FUN_000297f8(void)

{
  (**(code **)PTR_FUN_0002da68)();
  return;
}



void FUN_00029830(void)

{
  (**(code **)PTR_FUN_0002da6c)();
  return;
}



void FUN_00029868(void)

{
  (**(code **)PTR_FUN_0002da70)();
  return;
}



void FUN_000298a0(void)

{
  (**(code **)PTR_FUN_0002da74)();
  return;
}



void FUN_000298d8(void)

{
  (**(code **)PTR_FUN_0002da78)();
  return;
}



void FUN_00029910(void)

{
  (**(code **)PTR_FUN_0002da7c)();
  return;
}



void FUN_00029948(void)

{
  (**(code **)PTR_FUN_0002da80)();
  return;
}



void FUN_00029980(void)

{
  (**(code **)PTR_FUN_0002da84)();
  return;
}



void FUN_000299b8(void)

{
  (**(code **)PTR_FUN_0002da88)();
  return;
}



void FUN_000299f0(void)

{
  (**(code **)PTR_FUN_0002da8c)();
  return;
}



void FUN_00029a28(void)

{
  (**(code **)PTR_FUN_0002da90)();
  return;
}



void FUN_00029a60(void)

{
  (**(code **)PTR_FUN_0002da94)();
  return;
}



void FUN_00029a98(void)

{
  (**(code **)PTR_FUN_0002da98)();
  return;
}



void FUN_00029ad0(void)

{
  (**(code **)PTR_FUN_0002da9c)();
  return;
}



void FUN_00029b08(void)

{
  (**(code **)PTR_FUN_0002daa0)();
  return;
}



void FUN_00029b40(void)

{
  (**(code **)PTR_FUN_0002daa4)();
  return;
}



void FUN_00029b78(void)

{
  (**(code **)PTR_FUN_0002daa8)();
  return;
}



void FUN_00029bb0(void)

{
  (**(code **)PTR_FUN_0002daac)();
  return;
}



void FUN_00029be8(void)

{
  (**(code **)PTR_FUN_0002dab0)();
  return;
}



void FUN_00029c20(void)

{
  (**(code **)PTR_FUN_0002dab4)();
  return;
}



void FUN_00029c58(void)

{
  (**(code **)PTR_FUN_0002dab8)();
  return;
}



void FUN_00029c90(void)

{
  (**(code **)PTR_FUN_0002dabc)();
  return;
}



void FUN_00029cc8(void)

{
  (**(code **)PTR_FUN_0002dac0)();
  return;
}



void FUN_00029d00(void)

{
  (**(code **)PTR_FUN_0002dac4)();
  return;
}



void FUN_00029d38(void)

{
  (**(code **)PTR_FUN_0002dac8)();
  return;
}



void FUN_00029d70(void)

{
  (**(code **)PTR_FUN_0002dacc)();
  return;
}



void FUN_00029da8(void)

{
  (**(code **)PTR_FUN_0002dad0)();
  return;
}



void FUN_00029de0(void)

{
  (**(code **)PTR_FUN_0002dad4)();
  return;
}


