#ifndef CONVERT_H
#define CONVERT_H

#include "Types.h"

namespace gln64Convert {

	extern const volatile unsigned char Five2Eight[32];
	extern const volatile unsigned char Four2Eight[16];
	extern const volatile unsigned char Three2Four[8];
	extern const volatile unsigned char Three2Eight[8];
	extern const volatile unsigned char Two2Eight[4];
	extern const volatile unsigned char One2Four[2];
	extern const volatile unsigned char One2Eight[2];

	static inline void UnswapCopy(void *src, void *dest, u32 numBytes) {
		// copy leading bytes
		int leadingBytes = ((long) src) & 3;
		if (leadingBytes != 0) {
			leadingBytes = 4 - leadingBytes;
			if ((unsigned int) leadingBytes > numBytes)
				leadingBytes = numBytes;
			numBytes -= leadingBytes;

			src = (void *) ((long) src ^ 3);
			for (int i = 0; i < leadingBytes; i++) {
				*(u8 *) (dest) = *(u8 *) (src);
				dest = (void *) ((long) dest + 1);
				src = (void *) ((long) src - 1);
			}
			src = (void *) ((long) src + 5);
		}

		// copy dwords
		int numDWords = numBytes >> 2;
		while (numDWords--) {
			u32 dword = *(u32 *) src;
#ifdef ARM_ASM
			asm("rev %0, %0" : "+r"(dword)::);
#else
			dword = ((dword<<24)|((dword<<8)&0x00FF0000)|((dword>>8)&0x0000FF00)|(dword>>24));
#endif
			*(u32 *) dest = dword;
			dest = (void *) ((long) dest + 4);
			src = (void *) ((long) src + 4);
		}

		// copy trailing bytes
		int trailingBytes = numBytes & 3;
		if (trailingBytes) {
			src = (void *) ((long) src ^ 3);
			for (int i = 0; i < trailingBytes; i++) {
				*(u8 *) (dest) = *(u8 *) (src);
				dest = (void *) ((long) dest + 1);
				src = (void *) ((long) src - 1);
			}
		}
	}

	static inline void DWordInterleave(void *mem, u32 numDWords) {
		int tmp;
		while (numDWords--) {
			tmp = *(int *) ((long) mem + 0);
			*(int *) ((long) mem + 0) = *(int *) ((long) mem + 4);
			*(int *) ((long) mem + 4) = tmp;
			mem = (void *) ((long) mem + 8);
		}
	}

	inline void QWordInterleave(void *mem, u32 numDWords) {
		numDWords >>= 1; // qwords
		while (numDWords--) {
			int tmp0, tmp1;
			tmp0 = *(int *) ((long) mem + 0);
			tmp1 = *(int *) ((long) mem + 4);
			*(int *) ((long) mem + 0) = *(int *) ((long) mem + 8);
			*(int *) ((long) mem + 8) = tmp0;
			*(int *) ((long) mem + 4) = *(int *) ((long) mem + 12);
			*(int *) ((long) mem + 12) = tmp1;
			mem = (void *) ((long) mem + 16);
		}
	}


	inline u32 swapdword(u32 value) {
#ifdef ARM_ASM
		asm("rev %0, %0" : "+r"(value)::);
		return value;
#else
		return ((value & 0xff000000) >> 24) |
			   ((value & 0x00ff0000) >>  8) |
			   ((value & 0x0000ff00) <<  8) |
			   ((value & 0x000000ff) << 24);
#endif
	}

	inline u16 swapword(u16 value) {
#ifdef ARM_ASM
		asm("rev16 %0, %0" : "+r"(value)::);
		return value;
#else
		return (value << 8) | (value >> 8);
#endif
	}

	inline u16 RGBA8888_RGBA4444(u32 color) {
		return ((color & 0x000000f0) << 8) |   // r
			   ((color & 0x0000f000) >> 4) |   // g
			   ((color & 0x00f00000) >> 16) |   // b
			   ((color & 0xf0000000) >> 28);    // a
	}

	inline u32 RGBA5551_RGBA8888(u16 color) {
		color = swapword(color);
		u8 r, g, b, a;
		r = Five2Eight[color >> 11];
		g = Five2Eight[(color >> 6) & 0x001f];
		b = Five2Eight[(color >> 1) & 0x001f];
		a = One2Eight[(color) & 0x0001];
		return (a << 24) | (b << 16) | (g << 8) | r;
	}

// Just swaps the word
	inline u16 RGBA5551_RGBA5551(u16 color) {
		return swapword(color);
	}

	inline u32 IA88_RGBA8888(u16 color) {
		u8 a = color >> 8;
		u8 i = color & 0x00FF;
		return (a << 24) | (i << 16) | (i << 8) | i;
	}

	inline u16 IA88_RGBA4444(u16 color) {
		u8 i = color >> 12;
		u8 a = (color >> 4) & 0x000F;
		return (i << 12) | (i << 8) | (i << 4) | a;
	}

	inline u16 IA44_RGBA4444(u8 color) {
		return ((color & 0xf0) << 8) | ((color & 0xf0) << 4) | (color);
	}

	inline u32 IA44_RGBA8888(u8 color) {
		u8 i = Four2Eight[color >> 4];
		u8 a = Four2Eight[color & 0x0F];
		return (a << 24) | (i << 16) | (i << 8) | i;
	}

	inline u16 IA44_IA88(u8 color) {
		u8 i = Four2Eight[color >> 4];
		u8 a = Four2Eight[color & 0x0F];
		return (a << 8) | i;
	}

	inline u16 IA31_RGBA4444(u8 color) {
		u8 i = Three2Four[color >> 1];
		u8 a = One2Four[color & 0x01];
		return (i << 12) | (i << 8) | (i << 4) | a;
	}

	inline u16 IA31_IA88(u8 color) {
		u8 i = Three2Eight[color >> 1];
		u8 a = One2Eight[color & 0x01];
		return (a << 8) | i;
	}

	inline u32 IA31_RGBA8888(u8 color) {
		u8 i = Three2Eight[color >> 1];
		u8 a = One2Eight[color & 0x01];
		return (i << 24) | (i << 16) | (i << 8) | a;
	}

	inline u16 I8_RGBA4444(u8 color) {
		u8 c = color >> 4;
		return (c << 12) | (c << 8) | (c << 4) | c;
	}

	inline u32 I8_RGBA8888(u8 color) {
		return (color << 24) | (color << 16) | (color << 8) | color;
	}

	inline u16 I4_RGBA4444(u8 color) {
		u16 ret = color & 0x0f;
		ret |= ret << 4;
		ret |= ret << 8;
		return ret;
	}

	inline u8 I4_I8(u8 color) {
		return Four2Eight[color & 0x0f];
	}

	inline u16 I4_IA88(u8 color) {
		u32 c = Four2Eight[color & 0x0f];
		return (c << 8) | c;
	}

	inline u16 I8_IA88(u8 color) {
		return (color << 8) | color;
	}


	inline u16 IA88_IA88(u16 color) {
		u8 a = (color & 0xFF);
		u8 i = (color >> 8);
		return (i << 8) | a;
	}


	inline u32 I4_RGBA8888(u8 color) {
		u8 c = Four2Eight[color];
		c |= c << 4;
		return (c << 24) | (c << 16) | (c << 8) | c;
	}
}

#endif // CONVERT_H

