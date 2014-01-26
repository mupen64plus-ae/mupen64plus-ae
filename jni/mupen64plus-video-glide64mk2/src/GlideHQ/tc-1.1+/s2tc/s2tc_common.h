/*
 * Copyright (C) 2011  Rudolf Polzer   All Rights Reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * RUDOLF POLZER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#ifndef S2TC_COMMON_H
#define S2TC_COMMON_H

template <class T> inline T min(const T &a, const T &b)
{
	if(b < a)
		return b;
	return a;
}

template <class T> inline T max(const T &a, const T &b)
{
	if(b > a)
		return b;
	return a;
}

inline int byteidx(int bit)
{
	return bit >> 3;
}

inline int bitidx(int bit)
{
	return bit & 7;
}

inline void setbit(unsigned char *arr, int bit, int v = 1)
{
	arr[byteidx(bit)] |= (v << bitidx(bit));
}

inline void xorbit(unsigned char *arr, int bit, int v = 1)
{
	arr[byteidx(bit)] ^= (v << bitidx(bit));
}

inline int testbit(const unsigned char *arr, int bit, int v = 1)
{
	return (arr[byteidx(bit)] & (v << bitidx(bit)));
}

template<class T, int count, int width> class bitarray
{
	T bits;
	public:
	inline bitarray(): bits(0)
	{
	}
	inline int get(size_t i) const
	{
		return (bits >> (i * width)) & ((T(1) << width) - 1);
	}
	inline void set(size_t i, int v)
	{
		size_t shift = i * width;
		T mask = ((T(1) << width) - 1) << shift;
		bits = (bits & ~mask) | (T(v) << shift);
	}
	inline void do_or(size_t i, int v)
	{
		bits |= (T(v) << (i * width));
	}
	inline void do_xor(size_t i, int v)
	{
		bits ^= (T(v) << (i * width));
	}
	inline void clear()
	{
		bits = 0;
	}
	inline unsigned char getbyte(size_t p) const
	{
		return (bits >> (p * 8)) & 0xFF;
	}
	inline size_t nbytes() const
	{
		return (count * width + 7) >> 3;
	}
	inline void tobytes(unsigned char *out) const
	{
		size_t s = nbytes();
		for(size_t i = 0; i < s; ++i)
			out[i] = getbyte(i);
	}
};

template<int count, int width> class bitarray<void, count, width>
{
	unsigned char bits[count];
	public:
	inline bitarray(): bits()
	{
		clear();
	}
	inline int get(size_t i) const
	{
		return bits[i];
	}
	inline void set(size_t i, int v)
	{
		bits[i] = v;
	}
	inline void do_or(size_t i, int v)
	{
		bits[i] |= v;
	}
	inline void do_xor(size_t i, int v)
	{
		bits[i] ^= v;
	}
	inline void clear()
	{
		memset(bits, 0, sizeof(bits));
	}
	inline unsigned char getbyte(size_t p) const
	{
		size_t bitpos_min = p * 8;
		size_t bitpos_max = p * 8 + 7;
		size_t word_min = bitpos_min / width;
		size_t word_max = bitpos_max / width;
		int shift = bitpos_min % width;
		unsigned char out = get(word_min) >> shift;
		for(size_t i = word_min+1; i <= word_max; ++i)
			out |= get(i) << ((i - word_min) * width - shift);
		return out;
	}
	inline size_t nbytes() const
	{
		return (count * width + 7) >> 3;
	}
	inline void tobytes(unsigned char *out) const
	{
		size_t s = nbytes();
		for(size_t i = 0; i < s; ++i)
			out[i] = getbyte(i);
	}
};

#endif
