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
#define S2TC_LICENSE_IDENTIFIER s2tc_algorithm_license
#include "s2tc_license.h"

#include <math.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>
#include <algorithm>
#include <iostream>

#include "s2tc_algorithm.h"
#include "s2tc_common.h"

namespace
{
	template<class T> struct color_type_info
	{
	};
	template<> struct color_type_info<unsigned char>
	{
		static const unsigned char min_value = 0;
		static const unsigned char max_value = 255;
	};

	struct color_t
	{
		signed char r, g, b;
	};
	static color_t make_color_t()
	{
		color_t retColor = { 0, 0, 0 };
		return retColor;
	}
	static color_t make_color_t(signed char r_, signed char g_, signed char b_)
	{
		color_t retColor = { r_, g_, b_ };
		return retColor;
	}
	static color_t make_color_t(int i)
	{
		color_t retColor = { (signed char)(i >> 3), (signed char)(i >> 2), (signed char)(i >> 3) };
		return retColor;
	}
	static bool operator==(const color_t &a, const color_t &b)
	{
		return a.r == b.r && a.g == b.g && a.b == b.b;
	}
	static bool operator<(const color_t &a, const color_t &b)
	{
		signed char d;
		d = a.r - b.r;
		if(d)
			return d < 0;
		d = a.g - b.g;
		if(d)
			return d < 0;
		d = a.b - b.b;
		return d < 0;
	}
	static color_t &operator--(color_t &c)
	{
		if(c.b > 0)
		{
			--c.b;
		}
		else if(c.g > 0)
		{
			c.b = 31;
			--c.g;
		}
		else if(c.r > 0)
		{
			c.b = 31;
			c.g = 63;
			--c.r;
		}
		else
		{
			c.b = 31;
			c.g = 63;
			c.r = 31;
		}
		return c;
	}
	static color_t &operator++(color_t &c)
	{
		if(c.b < 31)
		{
			++c.b;
		}
		else if(c.g < 63)
		{
			c.b = 0;
			++c.g;
		}
		else if(c.r < 31)
		{
			c.b = 0;
			c.g = 0;
			++c.r;
		}
		else
		{
			c.b = 0;
			c.g = 0;
			c.r = 0;
		}
		return c;
	}
	template<> struct color_type_info<color_t>
	{
		static const color_t min_value;
		static const color_t max_value;
	};
	const color_t color_type_info<color_t>::min_value = { 0, 0, 0 };
	const color_t color_type_info<color_t>::max_value = { 31, 63, 31 };

	struct bigcolor_t
	{
		int r, g, b;

		inline bigcolor_t(): r(0), g(0), b(0)
		{
		}

		inline bigcolor_t &operator+=(const color_t &c)
		{
			r += c.r;
			g += c.g;
			b += c.b;
			return *this;
		}

		inline bigcolor_t &operator+=(int v)
		{
			r += v;
			g += v;
			b += v;
			return *this;
		}

		inline bigcolor_t operator+(int v)
		{
			bigcolor_t out = *this;
			out += v;
			return out;
		}

		inline bigcolor_t &operator/=(int v)
		{
			r /= v;
			g /= v;
			b /= v;
			return *this;
		}

		inline bigcolor_t operator/(int v)
		{
			bigcolor_t out = *this;
			out /= v;
			return out;
		}

		inline bigcolor_t &operator<<=(int v)
		{
			r <<= v;
			g <<= v;
			b <<= v;
			return *this;
		}

		inline bigcolor_t operator<<(int v)
		{
			bigcolor_t out = *this;
			out <<= v;
			return out;
		}

		inline operator color_t()
		{
			color_t out;
			out.r = r & 31;
			out.g = g & 63;
			out.b = b & 31;
			return out;
		}
	};

	std::ostream &operator<<(std::ostream &ost, const color_t &c)
	{
		return ost << "make_color_t(" << int(c.r) << ", " << int(c.g) << ", " << int(c.b) << ")";
	}

	std::ostream &operator<<(std::ostream &ost, const bigcolor_t &c)
	{
		return ost << "bigcolor_t(" << c.r << ", " << c.g << ", " << c.b << ")";
	}

	// 16 differences must fit in int
	// i.e. a difference must be lower than 2^27

	// shift right, rounded
#define SHRR(a,n) (((a) + (1 << ((n)-1))) >> (n))

	inline int color_dist_avg(const color_t &a, const color_t &b)
	{
		int dr = a.r - b.r; // multiplier: 31 (-1..1)
		int dg = a.g - b.g; // multiplier: 63 (-1..1)
		int db = a.b - b.b; // multiplier: 31 (-1..1)
		return ((dr*dr) << 2) + dg*dg + ((db*db) << 2);
	}

	inline int color_dist_wavg(const color_t &a, const color_t &b)
	{
		int dr = a.r - b.r; // multiplier: 31 (-1..1)
		int dg = a.g - b.g; // multiplier: 63 (-1..1)
		int db = a.b - b.b; // multiplier: 31 (-1..1)
		return ((dr*dr) << 2) + ((dg*dg) << 2) + (db*db);
		// weighted 4:16:1
	}

	inline int color_dist_yuv(const color_t &a, const color_t &b)
	{
		int dr = a.r - b.r; // multiplier: 31 (-1..1)
		int dg = a.g - b.g; // multiplier: 63 (-1..1)
		int db = a.b - b.b; // multiplier: 31 (-1..1)
		int y = dr * 30*2 + dg * 59 + db * 11*2; // multiplier: 6259
		int u = dr * 202 - y; // * 0.5 / (1 - 0.30)
		int v = db * 202 - y; // * 0.5 / (1 - 0.11)
		return ((y*y) << 1) + SHRR(u*u, 3) + SHRR(v*v, 4);
		// weight for u: sqrt(2^-4) / (0.5 / (1 - 0.30)) = 0.350
		// weight for v: sqrt(2^-5) / (0.5 / (1 - 0.11)) = 0.315
	}

	inline int color_dist_rgb(const color_t &a, const color_t &b)
	{
		int dr = a.r - b.r; // multiplier: 31 (-1..1)
		int dg = a.g - b.g; // multiplier: 63 (-1..1)
		int db = a.b - b.b; // multiplier: 31 (-1..1)
		int y = dr * 21*2 + dg * 72 + db * 7*2; // multiplier: 6272
		int u = dr * 202 - y; // * 0.5 / (1 - 0.21)
		int v = db * 202 - y; // * 0.5 / (1 - 0.07)
		return ((y*y) << 1) + SHRR(u*u, 3) + SHRR(v*v, 4);
		// weight for u: sqrt(2^-4) / (0.5 / (1 - 0.21)) = 0.395
		// weight for v: sqrt(2^-5) / (0.5 / (1 - 0.07)) = 0.328
	}

	inline int color_dist_srgb(const color_t &a, const color_t &b)
	{
		int dr = a.r * (int) a.r - b.r * (int) b.r; // multiplier: 31*31
		int dg = a.g * (int) a.g - b.g * (int) b.g; // multiplier: 63*63
		int db = a.b * (int) a.b - b.b * (int) b.b; // multiplier: 31*31
		int y = dr * 21*2*2 + dg * 72 + db * 7*2*2; // multiplier: 393400
		int u = dr * 409 - y; // * 0.5 / (1 - 0.30)
		int v = db * 409 - y; // * 0.5 / (1 - 0.11)
		int sy = SHRR(y, 3) * SHRR(y, 4);
		int su = SHRR(u, 3) * SHRR(u, 4);
		int sv = SHRR(v, 3) * SHRR(v, 4);
		return SHRR(sy, 4) + SHRR(su, 8) + SHRR(sv, 9);
		// weight for u: sqrt(2^-4) / (0.5 / (1 - 0.30)) = 0.350
		// weight for v: sqrt(2^-5) / (0.5 / (1 - 0.11)) = 0.315
	}

	inline int srgb_get_y(const color_t &a)
	{
		// convert to linear
		int r = a.r * (int) a.r;
		int g = a.g * (int) a.g;
		int b = a.b * (int) a.b;
		// find luminance
		int y = 37 * (r * 21*2*2 + g * 72 + b * 7*2*2); // multiplier: 14555800
		// square root it (!)
		y = (int) (sqrtf((float) y) + 0.5f); // now in range 0 to 3815
		return y;
	}

	inline int color_dist_srgb_mixed(const color_t &a, const color_t &b)
	{
		// get Y
		int ay = srgb_get_y(a);
		int by = srgb_get_y(b);
		// get UV
		int au = a.r * 191 - ay;
		int av = a.b * 191 - ay;
		int bu = b.r * 191 - by;
		int bv = b.b * 191 - by;
		// get differences
		int y = ay - by;
		int u = au - bu;
		int v = av - bv;
		return ((y*y) << 3) + SHRR(u*u, 1) + SHRR(v*v, 2);
		// weight for u: ???
		// weight for v: ???
	}

	inline int color_dist_normalmap(const color_t &a, const color_t &b)
	{
		float ca[3], cb[3], n;
		ca[0] = a.r / 31.0f * 2 - 1;
		ca[1] = a.g / 63.0f * 2 - 1;
		ca[2] = a.b / 31.0f * 2 - 1;
		cb[0] = b.r / 31.0f * 2 - 1;
		cb[1] = b.g / 63.0f * 2 - 1;
		cb[2] = b.b / 31.0f * 2 - 1;
		n = ca[0] * ca[0] + ca[1] * ca[1] + ca[2] * ca[2];
		if(n > 0)
		{
			n = 1.0f / sqrtf(n);
			ca[0] *= n;
			ca[1] *= n;
			ca[2] *= n;
		}
		n = cb[0] * cb[0] + cb[1] * cb[1] + cb[2] * cb[2];
		if(n > 0)
		{
			n = 1.0f / sqrtf(n);
			cb[0] *= n;
			cb[1] *= n;
			cb[2] *= n;
		}

		return
			(int) (100000 *
			(
				(cb[0] - ca[0]) * (cb[0] - ca[0])
				+
				(cb[1] - ca[1]) * (cb[1] - ca[1])
				+
				(cb[2] - ca[2]) * (cb[2] - ca[2])
			))
			;
		// max value: 1000 * (4 + 4 + 4) = 6000
	}

	typedef int ColorDistFunc(const color_t &a, const color_t &b);

	inline int alpha_dist(unsigned char a, unsigned char b)
	{
		return (a - (int) b) * (a - (int) b);
	}

	template <class T, class F>
	// n: input count
	// m: total color count (including non-counted inputs)
	// m >= n
	inline void reduce_colors_inplace(T *c, int n, int m, F dist)
	{
		int i, j, k;
		int bestsum = -1;
		int besti = 0;
		int bestj = 1;
		int *pDists = new int[m*n];
		// first the square
		for(i = 0; i < n; ++i)
		{
			pDists[i*n+i] = 0;
			for(j = i+1; j < n; ++j)
			{
				int d = dist(c[i], c[j]);
				pDists[i*n+j] = pDists[j*n+i] = d;
			}
		}
		// then the box
		for(; i < m; ++i)
		{
			for(j = 0; j < n; ++j)
			{
				int d = dist(c[i], c[j]);
				pDists[i*n+j] = d;
			}
		}
		for(i = 0; i < m; ++i)
			for(j = i+1; j < m; ++j)
			{
				int sum = 0;
				for(k = 0; k < n; ++k)
				{
					int di = pDists[i*n+k];
					int dj = pDists[j*n+k];
					int m  = min(di, dj);
					sum += m;
				}
				if(bestsum < 0 || sum < bestsum)
				{
					bestsum = sum;
					besti = i;
					bestj = j;
				}
			}
		T c0 = c[besti];
		c[1] = c[bestj];
		c[0] = c0;
		delete[] pDists;
	}
	template <class T, class F>
	inline void reduce_colors_inplace_2fixpoints(T *c, int n, int m, F dist, const T &fix0, const T &fix1)
	{
		// TODO fix this for ramp encoding!
		int i, j, k;
		int bestsum = -1;
		int besti = 0;
		int bestj = 1;
		int *pDists = new int[(m+2)*n];
		// first the square
		for(i = 0; i < n; ++i)
		{
			pDists[i*n+i] = 0;
			for(j = i+1; j < n; ++j)
			{
				int d = dist(c[i], c[j]);
				pDists[i*n+j] = pDists[j*n+i] = d;
			}
		}
		// then the box
		for(; i < m; ++i)
		{
			for(j = 0; j < n; ++j)
			{
				int d = dist(c[i], c[j]);
				pDists[i*n+j] = d;
			}
		}
		// then the two extra rows
		for(j = 0; j < n; ++j)
		{
			int d = dist(fix0, c[j]);
			pDists[m*n+j] = d;
		}
		for(j = 0; j < n; ++j)
		{
			int d = dist(fix1, c[j]);
			pDists[(m + 1)*n+j] = d;
		}
		for(i = 0; i < m; ++i)
			for(j = i+1; j < m; ++j)
			{
				int sum = 0;
				for(k = 0; k < n; ++k)
				{
					int di = pDists[i*n+k];
					int dj = pDists[j*n+k];
					int d0 = pDists[m*n+k];
					int d1 = pDists[(m+1)*n+k];
					int m  = min(min(di, dj), min(d0, d1));
					sum += m;
				}
				if(bestsum < 0 || sum < bestsum)
				{
					bestsum = sum;
					besti = i;
					bestj = j;
				}
			}
		if(besti != 0)
			c[0] = c[besti];
		if(bestj != 1)
			c[1] = c[bestj];
		delete[] pDists;
	}

	enum CompressionMode
	{
		MODE_NORMAL,
		MODE_FAST
	};

	template<ColorDistFunc ColorDist> inline int refine_component_encode(int comp)
	{
		return comp;
	}
	template<> inline int refine_component_encode<color_dist_srgb>(int comp)
	{
		return comp * comp;
	}
	template<> inline int refine_component_encode<color_dist_srgb_mixed>(int comp)
	{
		return comp * comp;
	}

	template<ColorDistFunc ColorDist> inline int refine_component_decode(int comp)
	{
		return comp;
	}
	template<> inline int refine_component_decode<color_dist_srgb>(int comp)
	{
		return (int) (sqrtf((float) comp) + 0.5f);
	}
	template<> inline int refine_component_decode<color_dist_srgb_mixed>(int comp)
	{
		return (int) (sqrtf((float) comp) + 0.5f);
	}

	template <class T, class Big, int scale_l>
	struct s2tc_evaluate_colors_result_t;

	template <class T, class Big>
	struct s2tc_evaluate_colors_result_t<T, Big, 1>
	{
		// uses:
		//   Big << int
		//   Big / int
		//   Big + int
		//   Big += T
		int n0, n1;
		Big S0, S1;
		inline s2tc_evaluate_colors_result_t():
			n0(), n1(), S0(), S1()
		{
		}
		inline void add(int l, T a)
		{
			if(l)
			{
				++n1;
				S1 += a;
			}
			else
			{
				++n0;
				S0 += a;
			}
		}
		inline bool evaluate(T &a, T &b)
		{
			if(!n0 && !n1)
				return false;
			if(n0)
				a = ((S0 << 1) + n0) / (n0 << 1);
			if(n1)
				b = ((S1 << 1) + n1) / (n1 << 1);
			return true;
		}
	};

	template <class T, class Big, int scale_l>
	struct s2tc_evaluate_colors_result_t
	{
		// a possible implementation of inferred color/alpha values
		// refining would go here
	};

	template <class T>
	struct s2tc_evaluate_colors_result_null_t
	{
		inline void add(int l, T a)
		{
		}
	};

	template<class T> T get(const unsigned char *buf)
	{
		T c;
		c.r = buf[0];
		c.g = buf[1];
		c.b = buf[2];
		return c;
	}
	template<> unsigned char get<unsigned char>(const unsigned char *buf)
	{
		return buf[3]; // extract alpha
	}

	template<class T, class Big, int bpp, bool have_trans, bool have_0_255, int n_input, class Dist, class Eval, class Arr>
	inline unsigned int s2tc_try_encode_block(
			Arr &out,
			Eval &res,
			Dist ColorDist,
			const unsigned char *in, int iw, int w, int h,
			const T colors_ref[])
	{
		unsigned int score = 0;
		for(int x = 0; x < w; ++x) for(int y = 0; y < h; ++y)
		{
			int i = y * 4 + x;
			const unsigned char *pix = &in[(y * iw + x) * 4];

			if(have_trans)
			{
				if(pix[3] == 0)
				{
					out.do_or(i, (1 << bpp) - 1);
					continue;
				}
			}

			T color(get<T>(pix));
			int best = 0;
			int bestdist = ColorDist(color, colors_ref[0]);
			for(int k = 1; k < n_input; ++k)
			{
				int dist = ColorDist(color, colors_ref[k]);
				if(dist < bestdist)
				{
					bestdist = dist;
					best = k;
				}
			}
			if(have_0_255)
			{
				int dist_0 = ColorDist(color, color_type_info<T>::min_value);
				if(dist_0 <= bestdist)
				{
					bestdist = dist_0;
					out.do_or(i, (1 << bpp) - 2);
					score += bestdist;
					continue;
				}
				int dist_255 = ColorDist(color, color_type_info<T>::max_value);
				if(dist_255 <= bestdist)
				{
					bestdist = dist_255;
					out.do_or(i, (1 << bpp) - 1);
					score += bestdist;
					continue;
				}
			}

			// record
			res.add(best, color);
			out.do_or(i, best);
			score += bestdist;
		}
		return score;
	}

	// REFINE_LOOP: refine, take result over only if score improved, loop until it did not
	inline void s2tc_dxt5_encode_alpha_refine_loop(bitarray<uint64_t, 16, 3> &out, const unsigned char *in, int iw, int w, int h, unsigned char &a0, unsigned char &a1)
	{
		bitarray<uint64_t, 16, 3> out2;
		unsigned char a0next = a0, a1next = a1;
		unsigned int s = 0x7FFFFFFF;
		for(;;)
		{
			unsigned char ramp[2] = {
				a0next,
				a1next
			};
			s2tc_evaluate_colors_result_t<unsigned char, int, 1> r2;
			unsigned int s2 = s2tc_try_encode_block<unsigned char, int, 3, false, true, 2>(out2, r2, alpha_dist, in, iw, w, h, ramp);
			if(s2 < s)
			{
				out = out2;
				s = s2;
				a0 = a0next;
				a1 = a1next;
				if(!r2.evaluate(a0next, a1next))
					break;
			}
			else
				break;
			out2.clear();
		}

		if(a1 == a0)
		{
			if(a0 == 255)
				--a1;
			else
				++a1;
			for(int i = 0; i < 16; ++i) switch(out.get(i))
			{
				case 1:
					out.set(i, 0);
					break;
			}
		}

		if(a1 < a0)
		{
			std::swap(a0, a1);
			for(int i = 0; i < 16; ++i) switch(out.get(i))
			{
				case 0:
					out.set(i, 1);
					break;
				case 1:
					out.set(i, 0);
					break;
				case 6:
				case 7:
					break;
				default:
					out.set(i, 7 - out.get(i));
					break;
			}
		}
	}

	// REFINE_ALWAYS: refine, do not check
	inline void s2tc_dxt5_encode_alpha_refine_always(bitarray<uint64_t, 16, 3> &out, const unsigned char *in, int iw, int w, int h, unsigned char &a0, unsigned char &a1)
	{
		unsigned char ramp[2] = {
			a0,
			a1
		};
		s2tc_evaluate_colors_result_t<unsigned char, int, 1> r2;
		s2tc_try_encode_block<unsigned char, int, 3, false, true, 2>(out, r2, alpha_dist, in, iw, w, h, ramp);
		r2.evaluate(a0, a1);

		if(a1 == a0)
		{
			if(a0 == 255)
				--a1;
			else
				++a1;
			for(int i = 0; i < 16; ++i) switch(out.get(i))
			{
				case 1:
					out.set(i, 0);
					break;
			}
		}

		if(a1 < a0)
		{
			std::swap(a0, a1);
			for(int i = 0; i < 16; ++i) switch(out.get(i))
			{
				case 0:
					out.set(i, 1);
					break;
				case 1:
					out.set(i, 0);
					break;
				case 6:
				case 7:
					break;
				default:
					out.set(i, 7 - out.get(i));
					break;
			}
		}
	}

	// REFINE_NEVER: do not refine
	inline void s2tc_dxt5_encode_alpha_refine_never(bitarray<uint64_t, 16, 3> &out, const unsigned char *in, int iw, int w, int h, unsigned char &a0, unsigned char &a1)
	{
		if(a1 < a0)
			std::swap(a0, a1);
		unsigned char ramp[6] = {
			a0,
			a1
		};
		s2tc_evaluate_colors_result_null_t<unsigned char> r2;
		s2tc_try_encode_block<unsigned char, int, 3, false, true, 2>(out, r2, alpha_dist, in, iw, w, h, ramp);
	}

	// REFINE_LOOP: refine, take result over only if score improved, loop until it did not
	template<ColorDistFunc ColorDist, bool have_trans>
	inline void s2tc_dxt1_encode_color_refine_loop(bitarray<uint32_t, 16, 2> &out, const unsigned char *in, int iw, int w, int h, color_t &c0, color_t &c1)
	{
		bitarray<uint32_t, 16, 2> out2;
		color_t c0next = c0, c1next = c1;
		unsigned int s = 0x7FFFFFFF;
		for(;;)
		{
			color_t ramp[2] = {
				c0next,
				c1next
			};
			s2tc_evaluate_colors_result_t<color_t, bigcolor_t, 1> r2;
			unsigned int s2 = s2tc_try_encode_block<color_t, bigcolor_t, 2, have_trans, false, 2>(out2, r2, ColorDist, in, iw, w, h, ramp);
			if(s2 < s)
			{
				out = out2;
				s = s2;
				c0 = c0next;
				c1 = c1next;
				if(!r2.evaluate(c0next, c1next))
					break;
			}
			else
				break;
			out2.clear();
		}

		if(c0 == c1)
		{
			if(c0 == color_type_info<color_t>::max_value)
				--c1;
			else
				++c1;
			for(int i = 0; i < 16; ++i)
				if(!(out.get(i) == 1))
					out.set(i, 0);
		}

		if(have_trans ? c1 < c0 : c0 < c1)
		{
			std::swap(c0, c1);
			for(int i = 0; i < 16; ++i)
				if(!(out.get(i) & 2))
					out.do_xor(i, 1);
		}
	}

	// REFINE_ALWAYS: refine, do not check
	template<ColorDistFunc ColorDist, bool have_trans>
	inline void s2tc_dxt1_encode_color_refine_always(bitarray<uint32_t, 16, 2> &out, const unsigned char *in, int iw, int w, int h, color_t &c0, color_t &c1)
	{
		color_t ramp[2] = {
			c0,
			c1
		};
		s2tc_evaluate_colors_result_t<color_t, bigcolor_t, 1> r2;
		s2tc_try_encode_block<color_t, bigcolor_t, 2, have_trans, false, 2>(out, r2, ColorDist, in, iw, w, h, ramp);
		r2.evaluate(c0, c1);

		if(c0 == c1)
		{
			if(c0 == color_type_info<color_t>::max_value)
				--c1;
			else
				++c1;
			for(int i = 0; i < 16; ++i)
				if(!(out.get(i) == 1))
					out.set(i, 0);
		}

		if(have_trans ? c1 < c0 : c0 < c1)
		{
			std::swap(c0, c1);
			for(int i = 0; i < 16; ++i)
				if(!(out.get(i) & 2))
					out.do_xor(i, 1);
		}
	}

	// REFINE_NEVER: do not refine
	template<ColorDistFunc ColorDist, bool have_trans>
	inline void s2tc_dxt1_encode_color_refine_never(bitarray<uint32_t, 16, 2> &out, const unsigned char *in, int iw, int w, int h, color_t &c0, color_t &c1)
	{
		if(have_trans ? c1 < c0 : c0 < c1)
			std::swap(c0, c1);
		color_t ramp[2] = {
			c0,
			c1
		};
		s2tc_evaluate_colors_result_null_t<color_t> r2;
		s2tc_try_encode_block<color_t, bigcolor_t, 2, have_trans, false, 2>(out, r2, ColorDist, in, iw, w, h, ramp);
	}

	inline void s2tc_dxt3_encode_alpha(bitarray<uint64_t, 16, 4> &out, const unsigned char *in, int iw, int w, int h)
	{
		for(int x = 0; x < w; ++x) for(int y = 0; y < h; ++y)
		{
			int i = y * 4 + x;
			const unsigned char *pix = &in[(y * iw + x) * 4];
			out.do_or(i, pix[3]);
		}
	}

	template<DxtMode dxt, ColorDistFunc ColorDist, CompressionMode mode, RefinementMode refine>
	inline void s2tc_encode_block(unsigned char *out, const unsigned char *rgba, int iw, int w, int h, int nrandom)
	{
		color_t *c = new color_t[16 + (nrandom >= 0 ? nrandom : 0)];
		unsigned char *ca = new unsigned char[16 + (nrandom >= 0 ? nrandom : 0)];
		int x, y;

		if(mode == MODE_FAST)
		{
			// FAST: trick from libtxc_dxtn: just get brightest and darkest colors, and encode using these

			color_t c0 = make_color_t(0, 0, 0);

			// dummy values because we don't know whether the first pixel will write
			c[0].r = 31;
			c[0].g = 63;
			c[0].b = 31;
			c[1].r = 0;
			c[1].g = 0;
			c[1].b = 0;
			int dmin = 0x7FFFFFFF;
			int dmax = 0;
			if(dxt == DXT5)
			{
				ca[0] = rgba[3];
				ca[1] = ca[0];
			}

			for(x = 0; x < w; ++x)
				for(y = 0; y < h; ++y)
				{
					c[2].r = rgba[(x + y * iw) * 4 + 0];
					c[2].g = rgba[(x + y * iw) * 4 + 1];
					c[2].b = rgba[(x + y * iw) * 4 + 2];
					ca[2]  = rgba[(x + y * iw) * 4 + 3];
					if (dxt == DXT1)
						if(ca[2] == 0)
							continue;
					// MODE_FAST doesn't work for normalmaps, so this works

					int d = ColorDist(c[2], c0);
					if(d > dmax)
					{
						dmax = d;
						c[1] = c[2];
					}
					if(d < dmin)
					{
						dmin = d;
						c[0] = c[2];
					}

					if(dxt == DXT5)
					{
						if(ca[2] != 255)
						{
							if(ca[2] > ca[1])
								ca[1] = ca[2];
							if(ca[2] < ca[0])
								ca[0] = ca[2];
						}
					}
				}
		}
		else
		{
			int n = 0, m = 0;

			for(x = 0; x < w; ++x)
				for(y = 0; y < h; ++y)
				{
					c[n].r = rgba[(x + y * iw) * 4 + 0];
					c[n].g = rgba[(x + y * iw) * 4 + 1];
					c[n].b = rgba[(x + y * iw) * 4 + 2];
					ca[n]  = rgba[(x + y * iw) * 4 + 3];
					if (dxt == DXT1)
						if(ca[n] == 0)
							continue;
					++n;
				}
			if(n == 0)
			{
				n = 1;
				c[0].r = 0;
				c[0].g = 0;
				c[0].b = 0;
				ca[0] = 0;
			}
			m = n;

			if(nrandom > 0)
			{
				color_t mins = c[0];
				color_t maxs = c[0];
				unsigned char mina = (dxt == DXT5) ? ca[0] : 0;
				unsigned char maxa = (dxt == DXT5) ? ca[0] : 0;
				for(x = 1; x < n; ++x)
				{
					mins.r = min(mins.r, c[x].r);
					mins.g = min(mins.g, c[x].g);
					mins.b = min(mins.b, c[x].b);
					maxs.r = max(maxs.r, c[x].r);
					maxs.g = max(maxs.g, c[x].g);
					maxs.b = max(maxs.b, c[x].b);
					if(dxt == DXT5)
					{
						mina = min(mina, ca[x]);
						maxa = max(maxa, ca[x]);
					}
				}
				color_t len = make_color_t(maxs.r - mins.r + 1, maxs.g - mins.g + 1, maxs.b - mins.b + 1);
				int lena = (dxt == DXT5) ? (maxa - (int) mina + 1) : 0;
				for(x = 0; x < nrandom; ++x)
				{
					c[m].r = mins.r + rand() % len.r;
					c[m].g = mins.g + rand() % len.g;
					c[m].b = mins.b + rand() % len.b;
					if(dxt == DXT5)
						ca[m] = mina + rand() % lena;
					++m;
				}
			}
			else
			{
				// hack for last miplevel
				if(n == 1)
				{
					c[1] = c[0];
					m = n = 2;
				}
			}

			reduce_colors_inplace(c, n, m, ColorDist);
			if(dxt == DXT5)
				reduce_colors_inplace_2fixpoints(ca, n, m, alpha_dist, (unsigned char) 0, (unsigned char) 255);
		}

		// equal colors are BAD
		if(c[0] == c[1])
		{
			if(c[0] == color_type_info<color_t>::max_value)
				--c[1];
			else
				++c[1];
		}

		if(dxt == DXT5)
		{
			if(ca[0] == ca[1])
			{
				if(ca[0] == 255)
					--ca[1];
				else
					++ca[1];
			}
		}

		switch(dxt)
		{
			case DXT1:
				{
					bitarray<uint32_t, 16, 2> colorblock;
					switch(refine)
					{
						case REFINE_NEVER:
							s2tc_dxt1_encode_color_refine_never<ColorDist, true>(colorblock, rgba, iw, w, h, c[0], c[1]);
							break;
						case REFINE_ALWAYS:
							s2tc_dxt1_encode_color_refine_always<ColorDist, true>(colorblock, rgba, iw, w, h, c[0], c[1]);
							break;
						case REFINE_LOOP:
							s2tc_dxt1_encode_color_refine_loop<ColorDist, true>(colorblock, rgba, iw, w, h, c[0], c[1]);
							break;
					}
					out[0] = ((c[0].g & 0x07) << 5) | c[0].b;
					out[1] = (c[0].r << 3) | (c[0].g >> 3);
					out[2] = ((c[1].g & 0x07) << 5) | c[1].b;
					out[3] = (c[1].r << 3) | (c[1].g >> 3);
					colorblock.tobytes(&out[4]);
				}
				break;
			case DXT3:
				{
					bitarray<uint32_t, 16, 2> colorblock;
					bitarray<uint64_t, 16, 4> alphablock;
					switch(refine)
					{
						case REFINE_NEVER:
							s2tc_dxt1_encode_color_refine_never<ColorDist, false>(colorblock, rgba, iw, w, h, c[0], c[1]);
							break;
						case REFINE_ALWAYS:
							s2tc_dxt1_encode_color_refine_always<ColorDist, false>(colorblock, rgba, iw, w, h, c[0], c[1]);
							break;
						case REFINE_LOOP:
							s2tc_dxt1_encode_color_refine_loop<ColorDist, false>(colorblock, rgba, iw, w, h, c[0], c[1]);
							break;
					}
					s2tc_dxt3_encode_alpha(alphablock, rgba, iw, w, h);
					alphablock.tobytes(&out[0]);
					out[8] = ((c[0].g & 0x07) << 5) | c[0].b;
					out[9] = (c[0].r << 3) | (c[0].g >> 3);
					out[10] = ((c[1].g & 0x07) << 5) | c[1].b;
					out[11] = (c[1].r << 3) | (c[1].g >> 3);
					colorblock.tobytes(&out[12]);
				}
				break;
			case DXT5:
				{
					bitarray<uint32_t, 16, 2> colorblock;
					bitarray<uint64_t, 16, 3> alphablock;
					switch(refine)
					{
						case REFINE_NEVER:
							s2tc_dxt1_encode_color_refine_never<ColorDist, false>(colorblock, rgba, iw, w, h, c[0], c[1]);
							s2tc_dxt5_encode_alpha_refine_never(alphablock, rgba, iw, w, h, ca[0], ca[1]);
							break;
						case REFINE_ALWAYS:
							s2tc_dxt1_encode_color_refine_always<ColorDist, false>(colorblock, rgba, iw, w, h, c[0], c[1]);
							s2tc_dxt5_encode_alpha_refine_always(alphablock, rgba, iw, w, h, ca[0], ca[1]);
							break;
						case REFINE_LOOP:
							s2tc_dxt1_encode_color_refine_loop<ColorDist, false>(colorblock, rgba, iw, w, h, c[0], c[1]);
							s2tc_dxt5_encode_alpha_refine_loop(alphablock, rgba, iw, w, h, ca[0], ca[1]);
							break;
					}
					out[0] = ca[0];
					out[1] = ca[1];
					alphablock.tobytes(&out[2]);
					out[8] = ((c[0].g & 0x07) << 5) | c[0].b;
					out[9] = (c[0].r << 3) | (c[0].g >> 3);
					out[10] = ((c[1].g & 0x07) << 5) | c[1].b;
					out[11] = (c[1].r << 3) | (c[1].g >> 3);
					colorblock.tobytes(&out[12]);
				}
				break;
		}
		delete [] c;
		delete [] ca;
	}

	// compile time dispatch magic
	template<DxtMode dxt, ColorDistFunc ColorDist, CompressionMode mode>
	inline s2tc_encode_block_func_t s2tc_encode_block_func(RefinementMode refine)
	{
		switch(refine)
		{
			case REFINE_NEVER:
				return s2tc_encode_block<dxt, ColorDist, mode, REFINE_NEVER>;
			case REFINE_LOOP:
				return s2tc_encode_block<dxt, ColorDist, mode, REFINE_LOOP>;
			default:
			case REFINE_ALWAYS:
				return s2tc_encode_block<dxt, ColorDist, mode, REFINE_ALWAYS>;
		}
	}

	// these color dist functions do not need the refinement check, as they always improve the situation
	template<ColorDistFunc ColorDist> struct supports_fast
	{
		static const bool value = true;
	};
	template<> struct supports_fast<color_dist_normalmap>
	{
		static const bool value = false;
	};

	template<DxtMode dxt, ColorDistFunc ColorDist>
	inline s2tc_encode_block_func_t s2tc_encode_block_func(int nrandom, RefinementMode refine)
	{
		if(!supports_fast<ColorDist>::value || nrandom >= 0)
			return s2tc_encode_block_func<dxt, ColorDist, MODE_NORMAL>(refine);
		else
			return s2tc_encode_block_func<dxt, ColorDist, MODE_FAST>(refine);
	}

	template<ColorDistFunc ColorDist>
	inline s2tc_encode_block_func_t s2tc_encode_block_func(DxtMode dxt, int nrandom, RefinementMode refine)
	{
		switch(dxt)
		{
			case DXT1:
				return s2tc_encode_block_func<DXT1, ColorDist>(nrandom, refine);
				break;
			case DXT3:
				return s2tc_encode_block_func<DXT3, ColorDist>(nrandom, refine);
				break;
			default:
			case DXT5:
				return s2tc_encode_block_func<DXT5, ColorDist>(nrandom, refine);
				break;
		}
	}
};

s2tc_encode_block_func_t s2tc_encode_block_func(DxtMode dxt, ColorDistMode cd, int nrandom, RefinementMode refine)
{
	switch(cd)
	{
		case RGB:
			return s2tc_encode_block_func<color_dist_rgb>(dxt, nrandom, refine);
			break;
		case YUV:
			return s2tc_encode_block_func<color_dist_yuv>(dxt, nrandom, refine);
			break;
		case SRGB:
			return s2tc_encode_block_func<color_dist_srgb>(dxt, nrandom, refine);
			break;
		case SRGB_MIXED:
			return s2tc_encode_block_func<color_dist_srgb_mixed>(dxt, nrandom, refine);
			break;
		case AVG:
			return s2tc_encode_block_func<color_dist_avg>(dxt, nrandom, refine);
			break;
		default:
		case WAVG:
			return s2tc_encode_block_func<color_dist_wavg>(dxt, nrandom, refine);
			break;
		case NORMALMAP:
			return s2tc_encode_block_func<color_dist_normalmap>(dxt, nrandom, refine);
			break;
	}
}

namespace
{
	inline int diffuse(int *diff, int src, int shift)
	{
		const int maxval = (1 << (8 - shift)) - 1;
		src += *diff;
		int ret = max(0, min(src >> shift, maxval));
		// simulate decoding ("loop filter")
		int loop = (ret << shift) | (ret >> (8 - 2 * shift));
		*diff = src - loop;
		return ret;
	}
	inline int diffuse1(int *diff, int src)
	{
		src += *diff;
		int ret = (src >= 128);
		// simulate decoding ("loop filter")
		int loop = ret ? 255 : 0;
		*diff = src - loop;
		return ret;
	}

	inline int floyd(int *thisrow, int *downrow, int src, int shift)
	{
		const int maxval = (1 << (8 - shift)) - 1;
		src = (src << 4) | (src >> 4);
		src += thisrow[1];
		int ret = max(0, min(src >> (shift + 4), maxval));
		// simulate decoding ("loop filter")
		int loop = (ret * 4095 / maxval);
		int err = src - loop;
		int e7 = (err * 7 + 8) / 16;
		err -= e7;
		int e3 = (err * 3 + 4) / 9;
		err -= e3;
		int e5 = (err * 5 + 3) / 6;
		err -= e5;
		int e1 = err;
		thisrow[2] += e7;
		downrow[0] += e3;
		downrow[1] += e5;
		downrow[2] += e1;
		return ret;
	}

	inline int floyd1(int *thisrow, int *downrow, int src)
	{
		src = (src << 4) | (src >> 4);
		src += thisrow[1];
		int ret = (src >= 2048);
		// simulate decoding ("loop filter")
		int loop = ret ? 4095 : 0;
		int err = src - loop;
		int e7 = (err * 7 + 8) / 16;
		err -= e7;
		int e3 = (err * 3 + 4) / 9;
		err -= e3;
		int e5 = (err * 5 + 3) / 6;
		err -= e5;
		int e1 = err;
		thisrow[2] += e7;
		downrow[0] += e3;
		downrow[1] += e5;
		downrow[2] += e1;
		return ret;
	}

	template<int srccomps, int alphabits, DitherMode dither>
	inline void rgb565_image(unsigned char *out, const unsigned char *rgba, int w, int h)
	{
		int x, y;
		switch(dither)
		{
			case DITHER_NONE:
				{
					for(y = 0; y < h; ++y)
						for(x = 0; x < w; ++x)
						{
							out[(x + y * w) * 4 + 0] = rgba[(x + y * w) * srccomps + 0] >> 3;
							out[(x + y * w) * 4 + 1] = rgba[(x + y * w) * srccomps + 1] >> 2;
							out[(x + y * w) * 4 + 2] = rgba[(x + y * w) * srccomps + 2] >> 3;
						}
					if(srccomps == 4)
					{
						if(alphabits == 1)
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = rgba[(x + y * w) * srccomps + 3] >> 7;
						}
						else if(alphabits == 8)
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = rgba[(x + y * w) * srccomps + 3]; // no conversion
						}
						else
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = rgba[(x + y * w) * srccomps + 3] >> (8 - alphabits);
						}
					}
					else
					{
						for(y = 0; y < h; ++y)
							for(x = 0; x < w; ++x)
								out[(x + y * w) * 4 + 3] = (1 << alphabits) - 1;
					}
				}
				break;
			case DITHER_SIMPLE:
				{
					int x, y;
					int diffuse_r = 0;
					int diffuse_g = 0;
					int diffuse_b = 0;
					int diffuse_a = 0;
					for(y = 0; y < h; ++y)
						for(x = 0; x < w; ++x)
						{
							out[(x + y * w) * 4 + 0] = diffuse(&diffuse_r, rgba[(x + y * w) * srccomps + 0], 3);
							out[(x + y * w) * 4 + 1] = diffuse(&diffuse_g, rgba[(x + y * w) * srccomps + 1], 2);
							out[(x + y * w) * 4 + 2] = diffuse(&diffuse_b, rgba[(x + y * w) * srccomps + 2], 3);
						}
					if(srccomps == 4)
					{
						if(alphabits == 1)
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = diffuse1(&diffuse_a, rgba[(x + y * w) * srccomps + 3]);
						}
						else if(alphabits == 8)
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = rgba[(x + y * w) * srccomps + 3]; // no conversion
						}
						else
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = diffuse(&diffuse_a, rgba[(x + y * w) * srccomps + 3], 8 - alphabits);
						}
					}
					else
					{
						for(y = 0; y < h; ++y)
							for(x = 0; x < w; ++x)
								out[(x + y * w) * 4 + 3] = (1 << alphabits) - 1;
					}
				}
				break;
			case DITHER_FLOYDSTEINBERG:
				{
					int x, y;
					int pw = w+2;
					int *downrow = new int[6*pw];
					memset(downrow, 0, 6*pw*sizeof(int));
					int *thisrow_r, *thisrow_g, *thisrow_b, *thisrow_a;
					int *downrow_r, *downrow_g, *downrow_b, *downrow_a;
					for(y = 0; y < h; ++y)
					{
						thisrow_r = downrow + ((y&1)?3:0) * pw;
						downrow_r = downrow + ((y&1)?0:3) * pw;
						memset(downrow_r, 0, sizeof(*downrow_r) * (3*pw));
						thisrow_g = thisrow_r + pw;
						thisrow_b = thisrow_g + pw;
						downrow_g = downrow_r + pw;
						downrow_b = downrow_g + pw;
						for(x = 0; x < w; ++x)
						{
							out[(x + y * w) * 4 + 0] = floyd(&thisrow_r[x], &downrow_r[x], rgba[(x + y * w) * srccomps + 0], 3);
							out[(x + y * w) * 4 + 1] = floyd(&thisrow_g[x], &downrow_g[x], rgba[(x + y * w) * srccomps + 1], 2);
							out[(x + y * w) * 4 + 2] = floyd(&thisrow_b[x], &downrow_b[x], rgba[(x + y * w) * srccomps + 2], 3);
						}
					}
					if(srccomps == 4)
					{
						if(alphabits == 1)
						{
							for(y = 0; y < h; ++y)
							{
								thisrow_a = downrow + (y&1) * pw;
								downrow_a = downrow + !(y&1) * pw;
								memset(downrow_a, 0, sizeof(*downrow_a) * pw);
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = floyd1(&thisrow_a[x], &downrow_a[x], rgba[(x + y * w) * srccomps + 3]);
							}
						}
						else if(alphabits == 8)
						{
							for(y = 0; y < h; ++y)
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = rgba[(x + y * w) * srccomps + 3]; // no conversion
						}
						else
						{
							for(y = 0; y < h; ++y)
							{
								thisrow_a = downrow + (y&1) * pw;
								downrow_a = downrow + !(y&1) * pw;
								memset(downrow_a, 0, sizeof(*downrow_a) * pw);
								for(x = 0; x < w; ++x)
									out[(x + y * w) * 4 + 3] = floyd(&thisrow_a[x], &downrow_a[x], rgba[(x + y * w) * srccomps + 3], 8 - alphabits);
							}
						}
					}
					else
					{
						for(y = 0; y < h; ++y)
							for(x = 0; x < w; ++x)
								out[(x + y * w) * 4 + 3] = (1 << alphabits) - 1;
					}
					delete[]  downrow;
				}
				break;
		}
	}

	template<int srccomps, int alphabits>
	inline void rgb565_image(unsigned char *out, const unsigned char *rgba, int w, int h, DitherMode dither)
	{
		switch(dither)
		{
			case DITHER_NONE:
				rgb565_image<srccomps, alphabits, DITHER_NONE>(out, rgba, w, h);
				break;
			default:
			case DITHER_SIMPLE:
				rgb565_image<srccomps, alphabits, DITHER_SIMPLE>(out, rgba, w, h);
				break;
			case DITHER_FLOYDSTEINBERG:
				rgb565_image<srccomps, alphabits, DITHER_FLOYDSTEINBERG>(out, rgba, w, h);
				break;
		}
	}

	template<int srccomps>
	inline void rgb565_image(unsigned char *out, const unsigned char *rgba, int w, int h, int alphabits, DitherMode dither)
	{
		switch(alphabits)
		{
			case 1:
				rgb565_image<srccomps, 1>(out, rgba, w, h, dither);
				break;
			case 4:
				rgb565_image<srccomps, 4>(out, rgba, w, h, dither);
				break;
			default:
			case 8:
				rgb565_image<srccomps, 8>(out, rgba, w, h, dither);
				break;
		}
	}
};

void rgb565_image(unsigned char *out, const unsigned char *rgba, int w, int h, int srccomps, int alphabits, DitherMode dither)
{
	switch(srccomps)
	{
		case 3:
			rgb565_image<3>(out, rgba, w, h, alphabits, dither);
			break;
		case 4:
		default:
			rgb565_image<4>(out, rgba, w, h, alphabits, dither);
			break;
	}
}
