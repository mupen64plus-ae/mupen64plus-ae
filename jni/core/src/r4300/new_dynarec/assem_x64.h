#define HOST_REGS 8
#define HOST_CCREG 6
#define HOST_BTREG 5
#define EXCLUDE_REG 4

//#define IMM_PREFETCH 1
#define HOST_IMM_ADDR32 1
#define INVERTED_CARRY 1
#define DESTRUCTIVE_WRITEBACK 1
#define DESTRUCTIVE_SHIFT 1

#define USE_MINI_HT 1

#define BASE_ADDR 0x70000000 // Code generator target address
#define TARGET_SIZE_2 25 // 2^25 = 32 megabytes
#define JUMP_TABLE_SIZE 0 // Not needed for x86

#define ROM_COPY ((void *)0x78000000) // For Goldeneye hack

/* x86-64 calling convention:
   func(rdi, rsi, rdx, rcx, r8, r9) {return rax;}
   callee-save: %rbp %rbx %r12-%r15 */

#define ARG1_REG 7 /* RDI */
#define ARG2_REG 6 /* RSI */
