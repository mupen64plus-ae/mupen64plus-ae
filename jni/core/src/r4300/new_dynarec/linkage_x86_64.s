/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - linkage_x86_64.s                                        *
 *   Copyright (C) 2009-2011 Ari64                                         *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	.file	"linkage_x86_64.s"
	.bss
	.align 4
//.globl rdram
//rdram = 0x80000000
	.section	.rodata
	.text
.globl dyna_linker
	.type	dyna_linker, @function
dyna_linker:
	/* eax = virtual target address */
	/* ebx = instruction to patch */
	mov	%eax, %edi
	mov	%eax, %ecx
	shr	$12, %edi
	cmp	$0xC0000000, %eax
	cmovge	tlb_LUT_r(,%edi,4), %ecx
	test	%ecx, %ecx
	cmovz	%eax, %ecx
	xor	$0x80000000, %ecx
	mov	$2047, %edx
	shr	$12, %ecx
	and	%ecx, %edx
	or	$2048, %edx
	cmp	%edx, %ecx
	cmova	%edx, %ecx
	/* jump_in lookup */
	movq	jump_in(,%ecx,8), %r12
.A1:
	test	%r12, %r12
	je	.A3
	mov	(%r12), %edi
	xor	%eax, %edi
	or	4(%r12), %edi
	je	.A2
	movq	16(%r12), %r12
	jmp	.A1
.A2:
	mov	(%ebx), %edi
	mov	%esi, %ebp
	lea	4(%ebx,%edi,1), %esi
	mov	%eax, %edi
	call	add_link
	mov	8(%r12), %edi
	mov	%ebp, %esi
	lea	-4(%edi), %edx
	subl	%ebx, %edx
	movl	%edx, (%ebx)
	jmp	*%rdi
.A3:
	/* hash_table lookup */
	mov	%eax, %edi
	mov	%eax, %edx
	shr	$16, %edi
	shr	$12, %edx
	xor	%eax, %edi
	and	$2047, %edx
	movzwl	%di, %edi
	shl	$4, %edi
	cmp	$2048, %ecx
	cmovc	%edx, %ecx
	cmp	hash_table(%edi), %eax
	jne	.A5
.A4:
	mov	hash_table+4(%edi), %edx
	jmp	*%rdx
.A5:
	cmp	hash_table+8(%edi), %eax
	lea	8(%edi), %edi
	je	.A4
	/* jump_dirty lookup */
	movq	jump_dirty(,%ecx,8), %r12
.A6:
	test	%r12, %r12
	je	.A8
	mov	(%r12), %ecx
	xor	%eax, %ecx
	or	4(%r12), %ecx
	je	.A7
	movq	16(%r12), %r12
	jmp	.A6
.A7:
	movl	8(%r12), %edx
	/* hash_table insert */
	mov	hash_table-8(%edi), %ebx
	mov	hash_table-4(%edi), %ecx
	mov	%eax, hash_table-8(%edi)
	mov	%edx, hash_table-4(%edi)
	mov	%ebx, hash_table(%edi)
	mov	%ecx, hash_table+4(%edi)
	jmp	*%rdx
.A8:
	mov	%eax, %edi
	mov	%eax, %ebp /* Note: assumes %rbx and %rbp are callee-saved */
	mov	%esi, %r12d
	call	new_recompile_block
	test	%eax, %eax
	mov	%ebp, %eax
	mov	%r12d, %esi
	je	dyna_linker
	/* pagefault */
	mov	%eax, %ebx
	mov	$0x08, %ecx
	.size	dyna_linker, .-dyna_linker

.globl exec_pagefault
	.type	exec_pagefault, @function
exec_pagefault:
	/* eax = instruction pointer */
	/* ebx = fault address */
	/* ecx = cause */
	mov	reg_cop0+48, %edx
	mov	reg_cop0+16, %edi
	or	$2, %edx
	mov	%ebx, reg_cop0+32 /* BadVAddr */
	and	$0xFF80000F, %edi
	mov	%edx, reg_cop0+48 /* Status */
	mov	%ecx, reg_cop0+52 /* Cause */
	mov	%eax, reg_cop0+56 /* EPC */
	mov	%ebx, %ecx
	shr	$9, %ebx
	and	$0xFFFFE000, %ecx
	and	$0x007FFFF0, %ebx
	mov	%ecx, reg_cop0+40 /* EntryHI */
	or	%ebx, %edi
	mov	%edi, reg_cop0+16 /* Context */
	mov	%esi, %ebx
	mov	$0x80000000, %edi
	call	get_addr_ht
	mov	%ebx, %esi
	jmp	*%rax
	.size	exec_pagefault, .-exec_pagefault

/* Special dynamic linker for the case where a page fault
   may occur in a branch delay slot */
.globl dyna_linker_ds
	.type	dyna_linker_ds, @function
dyna_linker_ds:
	mov	%eax, %edi
	mov	%eax, %ecx
	shr	$12, %edi
	cmp	$0xC0000000, %eax
	cmovge	tlb_LUT_r(,%edi,4), %ecx
	test	%ecx, %ecx
	cmovz	%eax, %ecx
	xor	$0x80000000, %ecx
	mov	$2047, %edx
	shr	$12, %ecx
	and	%ecx, %edx
	or	$2048, %edx
	cmp	%edx, %ecx
	cmova	%edx, %ecx
	/* jump_in lookup */
	movq	jump_in(,%ecx,8), %r12
.B1:
	test	%r12, %r12
	je	.B3
	mov	(%r12), %edi
	xor	%eax, %edi
	or	4(%r12), %edi
	je	.B2
	movq	16(%r12), %r12
	jmp	.B1
.B2:
	mov	(%ebx), %edi
	mov	%esi, %r13d
	lea	4(%ebx,%edi,1), %esi
	mov	%eax, %edi
	call	add_link
	mov	8(%r12), %edi
	mov	%r13d, %esi
	lea	-4(%edi), %edx
	subl	%ebx, %edx
	movl	%edx, (%ebx)
	jmp	*%rdi
.B3:
	/* hash_table lookup */
	mov	%eax, %edi
	mov	%eax, %edx
	shr	$16, %edi
	shr	$12, %edx
	xor	%eax, %edi
	and	$2047, %edx
	movzwl	%di, %edi
	shl	$4, %edi
	cmp	$2048, %ecx
	cmovc	%edx, %ecx
	cmp	hash_table(%edi), %eax
	jne	.B5
.B4:
	mov	hash_table+4(%edi), %edx
	jmp	*%rdx
.B5:
	cmp	hash_table+8(%edi), %eax
	lea	8(%edi), %edi
	je	.B4
	/* jump_dirty lookup */
	movq	jump_dirty(,%ecx,8), %r12
.B6:
	test	%r12, %r12
	je	.B8
	mov	(%r12), %ecx
	xor	%eax, %ecx
	or	4(%r12), %ecx
	je	.B7
	movq	16(%r12), %r12
	jmp	.B6
.B7:
	movl	8(%r12), %edx
	/* hash_table insert */
	mov	hash_table-8(%edi), %ebx
	mov	hash_table-4(%edi), %ecx
	mov	%eax, hash_table-8(%edi)
	mov	%edx, hash_table-4(%edi)
	mov	%ebx, hash_table(%edi)
	mov	%ecx, hash_table+4(%edi)
	jmp	*%rdx
.B8:
	mov	%eax, %edi
	mov	%eax, %r12d /* Note: assumes %rbx and %rbp are callee-saved */
	and	$0xFFFFFFF8, %edi
	mov	%esi, %r13d
	inc	%edi
	call	new_recompile_block
	test	%eax, %eax
	mov	%r12d, %eax
	mov	%r13d, %esi
	je	dyna_linker_ds
	/* pagefault */
	and	$0xFFFFFFF8, %eax
	mov	$0x80000008, %ecx /* High bit set indicates pagefault in delay slot */
	mov	%eax, %ebx
	sub	$4, %eax
	jmp	exec_pagefault
	.size	dyna_linker_ds, .-dyna_linker_ds

.globl jump_vaddr_eax
	.type	jump_vaddr_eax, @function
jump_vaddr_eax:
	mov	%eax, %edi
	jmp	jump_vaddr_edi
	.size	jump_vaddr_eax, .-jump_vaddr_eax
.globl jump_vaddr_ecx
	.type	jump_vaddr_ecx, @function
jump_vaddr_ecx:
	mov	%ecx, %edi
	jmp	jump_vaddr_edi
	.size	jump_vaddr_ecx, .-jump_vaddr_ecx
.globl jump_vaddr_edx
	.type	jump_vaddr_edx, @function
jump_vaddr_edx:
	mov	%edx, %edi
	jmp	jump_vaddr_edi
	.size	jump_vaddr_edx, .-jump_vaddr_edx
.globl jump_vaddr_ebx
	.type	jump_vaddr_ebx, @function
jump_vaddr_ebx:
	mov	%ebx, %edi
	jmp	jump_vaddr_edi
	.size	jump_vaddr_ebx, .-jump_vaddr_ebx
.globl jump_vaddr_ebp
	.type	jump_vaddr_ebp, @function
jump_vaddr_ebp:
	mov	%ebp, %edi
	.size	jump_vaddr_ebp, .-jump_vaddr_ebp
.globl jump_vaddr_edi
	.type	jump_vaddr_edi, @function
jump_vaddr_edi:
	mov	%edi, %eax
	.size	jump_vaddr_edi, .-jump_vaddr_edi

.globl jump_vaddr
	.type	jump_vaddr, @function
jump_vaddr:
  /* Check hash table */
	shr	$16, %eax
	xor	%edi, %eax
	movzwl	%ax, %eax
	shl	$4, %eax
	cmp	hash_table(%eax), %edi
	jne	.C2
.C1:
	mov	hash_table+4(%eax), %edi
	jmp	*%rdi
.C2:
	cmp	hash_table+8(%eax), %edi
	lea	8(%eax), %eax
	je	.C1
  /* No hit on hash table, call compiler */
	mov	%esi, cycle_count /* CCREG */
	call	get_addr
	mov	cycle_count, %esi
	jmp	*%rax
	.size	jump_vaddr, .-jump_vaddr

.globl verify_code_ds
	.type	verify_code_ds, @function
verify_code_ds:
	nop
	.size	verify_code_ds, .-verify_code_ds

.globl verify_code_vm
	.type	verify_code_vm, @function
verify_code_vm:
	/* eax = source (virtual address) */
	/* ebx = target */
	/* ecx = length */
	cmp	$0xC0000000, %eax
	jl	verify_code
	mov	%eax, %edx
	lea	-1(%eax,%ecx,1), %r9d
	shr	$12, %edx
	shr	$12, %r9d
	mov	memory_map(,%edx,4), %edi
	test	%edi, %edi
	js	.D4
	lea	(%eax,%edi,4), %eax
	mov	%edi, %r8d
.D1:
	xor	memory_map(,%edx,4), %edi
	shl	$2, %edi
	jne	.D4
	mov	%r8d, %edi
	inc	%edx
	cmp	%r9d, %edx
	jbe	.D1
	.size	verify_code_vm, .-verify_code_vm

.globl verify_code
	.type	verify_code, @function
verify_code:
	/* eax = source */
	/* ebx = target */
	/* ecx = length */
	/* r12d = instruction pointer */
	mov	-4(%eax,%ecx,1), %edi
	xor	-4(%ebx,%ecx,1), %edi
	jne	.D4
	mov	%ecx, %edx
	add	$-4, %ecx
	je	.D3
	test	$4, %edx
	cmove	%edx, %ecx
.D2:
	mov	-8(%eax,%ecx,1), %rdi
	cmp	-8(%ebx,%ecx,1), %rdi
	jne	.D4
	add	$-8, %ecx
	jne	.D2
.D3:
	ret
.D4:
	add	$8, %rsp /* pop return address, we're not returning */
	mov	%r12d, %edi
	mov	%esi, %ebx
	call	get_addr
	mov	%ebx, %esi
	jmp	*%rax
	.size	verify_code, .-verify_code

.globl cc_interrupt
	.type	cc_interrupt, @function
cc_interrupt:
	add	last_count, %esi
	add	$-8, %rsp /* Align stack */
	mov	%esi, reg_cop0+36 /* Count */
	shr	$19, %esi
	movl	$0, pending_exception
	and	$0x7f, %esi
	cmpl	$0, restore_candidate(,%esi,4)
	jne	.E4
.E1:
	call	gen_interupt
	mov	reg_cop0+36, %esi
	mov	next_interupt, %eax
	mov	pending_exception, %ebx
	mov	stop, %ecx
	add	$8, %rsp
	mov	%eax, last_count
	sub	%eax, %esi
	test	%ecx, %ecx
	jne	.E3
	test	%ebx, %ebx
	jne	.E2
	ret
.E2:
	mov	pcaddr, %edi
	mov	%esi, cycle_count /* CCREG */
	call	get_addr_ht
	mov	cycle_count, %esi
	add	$8, %rsp /* pop return address */
	jmp	*%rax
.E3:
	pop	%rbp /* pop return address and discard it */
	pop	%rbp /* pop junk */
	pop	%r15 /* restore callee-save registers */
	pop	%r14
	pop	%r13
	pop	%r12
	pop	%rbx
	pop	%rbp
	ret	     /* exit dynarec */
.E4:
	/* Move 'dirty' blocks to the 'clean' list */
	mov	restore_candidate(,%esi,4), %ebx
	mov	%esi, %ebp
	movl	$0, restore_candidate(,%esi,4)
	shl	$5, %ebp
.E5:
	shr	$1, %ebx
	jnc	.E6
	mov	%ebp, %edi
	call	clean_blocks
.E6:
	inc	%ebp
	test	$31, %ebp
	jne	.E5
	jmp	.E1
	.size	cc_interrupt, .-cc_interrupt

.globl do_interrupt
	.type	do_interrupt, @function
do_interrupt:
	mov	pcaddr, %edi
	call	get_addr_ht
	mov	reg_cop0+36, %esi
	mov	next_interupt, %ebx
	mov	%ebx, last_count
	sub	%ebx, %esi
	add	$2, %esi
	jmp	*%rax
	.size	do_interrupt, .-do_interrupt

.globl fp_exception
	.type	fp_exception, @function
fp_exception:
	mov	$0x1000002c, %edx
.E7:
	mov	reg_cop0+48, %ebx
	or	$2, %ebx
	mov	%ebx, reg_cop0+48 /* Status */
	mov	%edx, reg_cop0+52 /* Cause */
	mov	%eax, reg_cop0+56 /* EPC */
	mov	%esi, %ebx
	mov	$0x80000180, %edi
	call	get_addr_ht
	mov	%ebx, %esi
	jmp	*%rax
	.size	fp_exception, .-fp_exception

.globl fp_exception_ds
	.type	fp_exception_ds, @function
fp_exception_ds:
	mov	$0x9000002c, %edx /* Set high bit if delay slot */
	jmp	.E7
	.size	fp_exception_ds, .-fp_exception_ds

.globl jump_syscall
	.type	jump_syscall, @function
jump_syscall:
	mov	$0x20, %edx
	mov	reg_cop0+48, %ebx
	or	$2, %ebx
	mov	%ebx, reg_cop0+48 /* Status */
	mov	%edx, reg_cop0+52 /* Cause */
	mov	%eax, reg_cop0+56 /* EPC */
	mov	%esi, %ebx
	mov	$0x80000180, %edi
	call	get_addr_ht
	mov	%ebx, %esi
	jmp	*%rax
	.size	jump_syscall, .-jump_syscall

.globl jump_eret
	.type	jump_eret, @function
jump_eret:
	mov	reg_cop0+48, %ebx /* Status */
	add	last_count, %esi
	and	$0xFFFFFFFD, %ebx
	mov	%esi, reg_cop0+36 /* Count */
	mov	%ebx, reg_cop0+48 /* Status */
	call	check_interupt
	mov	next_interupt, %eax
	mov	reg_cop0+36, %esi
	mov	%eax, last_count
	sub	%eax, %esi
	mov	reg_cop0+56, %edi /* EPC */
	jns	.E11
.E8:
	mov	%esi, %r12d
	mov	$248, %ebx
	xor	%esi, %esi
.E9:
	mov	reg(%ebx), %ecx
	mov	reg+4(%ebx), %edx
	sar	$31, %ecx
	xor	%ecx, %edx
	neg	%edx
	adc	%esi, %esi
	sub	$8, %ebx
	jne	.E9
	mov	hi(%ebx), %ecx
	mov	hi+4(%ebx), %edx
	sar	$31, %ecx
	xor	%ecx, %edx
	jne	.E10
	mov	lo(%ebx), %ecx
	mov	lo+4(%ebx), %edx
	sar	$31, %ecx
	xor	%ecx, %edx
.E10:
	neg	%edx
	adc	%esi, %esi
	call	get_addr_32
	mov	%r12d, %esi
	jmp	*%rax
.E11:
	mov	%edi, pcaddr
	call	cc_interrupt
	mov	pcaddr, %edi
	jmp	.E8
	.size	jump_eret, .-jump_eret

.globl new_dyna_start
	.type	new_dyna_start, @function
new_dyna_start:
	push	%rbp
	push	%rbx
	push	%r12
	push	%r13
	push	%r14
	push	%r15
	mov	$0xa4000040, %edi
	call	new_recompile_block
	add	$-8, %rsp /* align stack */
	movl	next_interupt, %edi
	movl	reg_cop0+36, %esi
	movl	%edi, last_count
	subl	%edi, %esi
	jmp	0x70000000
	.size	new_dyna_start, .-new_dyna_start

/* Note: Assumes %rbx, %rbp are callee-saved */
.globl invalidate_block_eax
	.type	invalidate_block_eax, @function
invalidate_block_eax:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	mov	%eax, %edi
	jmp	invalidate_block_call
	.size	invalidate_block_eax, .-invalidate_block_eax
.globl invalidate_block_ecx
	.type	invalidate_block_ecx, @function
invalidate_block_ecx:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	mov	%ecx, %edi
	jmp	invalidate_block_call
	.size	invalidate_block_ecx, .-invalidate_block_ecx
.globl invalidate_block_edx
	.type	invalidate_block_edx, @function
invalidate_block_edx:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	mov	%edx, %edi
	jmp	invalidate_block_call
	.size	invalidate_block_edx, .-invalidate_block_edx
.globl invalidate_block_ebx
	.type	invalidate_block_ebx, @function
invalidate_block_ebx:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	mov	%ebx, %edi
	jmp	invalidate_block_call
	.size	invalidate_block_ebx, .-invalidate_block_ebx
.globl invalidate_block_ebp
	.type	invalidate_block_ebp, @function
invalidate_block_ebp:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	mov	%ebp, %edi
	jmp	invalidate_block_call
	.size	invalidate_block_ebp, .-invalidate_block_ebp
.globl invalidate_block_esi
	.type	invalidate_block_esi, @function
invalidate_block_esi:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	mov	%esi, %edi
	jmp	invalidate_block_call
	.size	invalidate_block_esi, .-invalidate_block_esi
.globl invalidate_block_edi
	.type	invalidate_block_edi, @function
invalidate_block_edi:
	push	%rax
	push	%rcx
	push	%rdx
	push	%rsi
	push	%rdi
	.size	invalidate_block_edi, .-invalidate_block_edi
.globl invalidate_block_call
	.type	invalidate_block_call, @function
invalidate_block_call:
	call	invalidate_block
	pop	%rdi
	pop	%rsi
	pop	%rdx
	pop	%rcx
	pop	%rax
	ret
	.size	invalidate_block_call, .-invalidate_block_call

.globl write_rdram_new
	.type	write_rdram_new, @function
write_rdram_new:
	mov	address, %edi
	mov	word, %ecx
	and	$0x7FFFFFFF, %edi
	mov	%ecx, rdram(%rdi)
	jmp	.E12
	.size	write_rdram_new, .-write_rdram_new

.globl write_rdramb_new
	.type	write_rdramb_new, @function
write_rdramb_new:
	mov	address, %edi
	xor	$3, %edi
	movb	byte, %cl
	and	$0x7FFFFFFF, %edi
	movb	%cl, rdram(%rdi)
	jmp	.E12
	.size	write_rdramb_new, .-write_rdramb_new

.globl write_rdramh_new
	.type	write_rdramh_new, @function
write_rdramh_new:
	mov	address, %edi
	xor	$2, %edi
	movw	hword, %cx
	and	$0x7FFFFFFF, %edi
	movw	%cx, rdram(%rdi)
	jmp	.E12
	.size	write_rdramh_new, .-write_rdramh_new

.globl write_rdramd_new
	.type	write_rdramd_new, @function
write_rdramd_new:
	mov	address, %edi
	mov	dword+4, %ecx
	mov	dword, %edx
	and	$0x7FFFFFFF, %edi
	mov	%ecx, rdram(%rdi)
	mov	%edx, rdram+4(%rdi)
	jmp	.E12
	.size	write_rdramd_new, .-write_rdramd_new

.globl do_invalidate
	.type	do_invalidate, @function
do_invalidate:
	mov	address, %edi
	mov	%edi, %ebx /* Return ebx to caller */
.E12:
	shr	$12, %edi
	mov	%edi, %r12d /* Return r12 to caller */
	cmpb	$1, invalid_code(%edi)
	je	.E13
	call	invalidate_block
.E13:
	ret
	.size	do_invalidate, .-do_invalidate

.globl read_nomem_new
	.type	read_nomem_new, @function
read_nomem_new:
	mov	address, %edi
	mov	%edi, %ebx
	shr	$12, %edi
	mov	memory_map(,%edi,4),%edi
	mov	$0x8, %eax
	test	%edi, %edi
	js	tlb_exception
	mov	(%ebx,%edi,4), %ecx
	mov	%ecx, readmem_dword
	ret
	.size	read_nomem_new, .-read_nomem_new

.globl read_nomemb_new
	.type	read_nomemb_new, @function
read_nomemb_new:
	mov	address, %edi
	mov	%edi, %ebx
	shr	$12, %edi
	mov	memory_map(,%edi,4),%edi
	mov	$0x8, %eax
	test	%edi, %edi
	js	tlb_exception
	xor	$3, %ebx
	movzbl	(%ebx,%edi,4), %ecx
	mov	%ecx, readmem_dword
	ret
	.size	read_nomemb_new, .-read_nomemb_new

.globl read_nomemh_new
	.type	read_nomemh_new, @function
read_nomemh_new:
	mov	address, %edi
	mov	%edi, %ebx
	shr	$12, %edi
	mov	memory_map(,%edi,4),%edi
	mov	$0x8, %eax
	test	%edi, %edi
	js	tlb_exception
	xor	$2, %ebx
	movzwl	(%ebx,%edi,4), %ecx
	mov	%ecx, readmem_dword
	ret
	.size	read_nomemh_new, .-read_nomemh_new

.globl read_nomemd_new
	.type	read_nomemd_new, @function
read_nomemd_new:
	mov	address, %edi
	mov	%edi, %ebx
	shr	$12, %edi
	mov	memory_map(,%edi,4),%edi
	mov	$0x8, %eax
	test	%edi, %edi
	js	tlb_exception
	mov	4(%ebx,%edi,4), %ecx
	mov	(%ebx,%edi,4), %edx
	mov	%ecx, readmem_dword
	mov	%edx, readmem_dword+4
	ret
	.size	read_nomemd_new, .-read_nomemd_new

.globl write_nomem_new
	.type	write_nomem_new, @function
write_nomem_new:
	call	do_invalidate
	mov	memory_map(,%r12d,4),%edi
	mov	word, %ecx
	mov	$0xc, %eax
	shl	$2, %edi
	jc	tlb_exception
	mov	%ecx, (%ebx,%edi)
	ret
	.size	write_nomem_new, .-write_nomem_new

.globl write_nomemb_new
	.type	write_nomemb_new, @function
write_nomemb_new:
	call	do_invalidate
	mov	memory_map(,%r12d,4),%edi
	movb	byte, %cl
	mov	$0xc, %eax
	shl	$2, %edi
	jc	tlb_exception
	xor	$3, %ebx
	movb	%cl, (%ebx,%edi)
	ret
	.size	write_nomemb_new, .-write_nomemb_new

.globl write_nomemh_new
	.type	write_nomemh_new, @function
write_nomemh_new:
	call	do_invalidate
	mov	memory_map(,%r12d,4),%edi
	movw	hword, %cx
	mov	$0xc, %eax
	shl	$2, %edi
	jc	tlb_exception
	xor	$2, %ebx
	movw	%cx, (%ebx,%edi)
	ret
	.size	write_nomemh_new, .-write_nomemh_new

.globl write_nomemd_new
	.type	write_nomemd_new, @function
write_nomemd_new:
	call	do_invalidate
	mov	memory_map(,%r12d,4),%edi
	mov	dword+4, %edx
	mov	dword, %ecx
	mov	$0xc, %eax
	shl	$2, %edi
	jc	tlb_exception
	mov	%edx, (%ebx,%edi)
	mov	%ecx, 4(%ebx,%edi)
	ret
	.size	write_nomemd_new, .-write_nomemd_new

.globl tlb_exception
	.type	tlb_exception, @function
tlb_exception:
	/* eax = cause */
	/* ebx = address */
	/* ebp = instr addr + flags */
	mov	8(%rsp), %ebp
	mov	reg_cop0+48, %esi
	mov	%ebp, %ecx
	mov	%ebp, %edx
	mov	%ebp, %edi
	shl	$31, %ebp
	shr	$12, %ecx
	or	%ebp, %eax
	sar	$29, %ebp
	and	$0xFFFFFFFC, %edx
	mov	memory_map(,%ecx,4), %ecx
	or	$2, %esi
	mov	(%edx, %ecx, 4), %ecx
	add	%ebp, %edx
	mov	%esi, reg_cop0+48 /* Status */
	mov	%eax, reg_cop0+52 /* Cause */
	mov	%edx, reg_cop0+56 /* EPC */
	add	$0x48, %rsp
	mov	$0x6000022, %edx
	mov	%ecx, %ebp
	movswl	%cx, %eax
	shr	$26, %ecx
	shr	$21, %ebp
	sub	%eax, %ebx
	and	$0x1f, %ebp
	ror	%cl, %edx
	mov	reg_cop0+16, %esi
	cmovc	reg(,%ebp,8), %ebx
	and	$0xFF80000F, %esi
	mov	%ebx, reg(,%ebp,8)
	add	%ebx, %eax
	sar	$31, %ebx
	mov	%eax, reg_cop0+32 /* BadVAddr */
	shr	$9, %eax
	test	$2, %edi
	cmove	reg+4(,%ebp,8), %ebx
	and	$0x007FFFF0, %eax
	mov	$0x80000180, %edi
	mov	%ebx, reg+4(,%ebp,8)
	or	%eax, %esi
	mov	%esi, reg_cop0+16 /* Context */
	call	get_addr_ht
	movl	next_interupt, %edi
	movl	reg_cop0+36, %esi /* Count */
	movl	%edi, last_count
	subl	%edi, %esi
	jmp	*%rax
	.size	tlb_exception, .-tlb_exception
