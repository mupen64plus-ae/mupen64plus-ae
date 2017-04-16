#pragma once
#include <vector>
#include <Types.h>
#include <Textures.h>

namespace graphics {

class ColorBufferReader
{
protected:

	struct ReadColorBufferParams {
		s32 x0;
		s32 y0;
		u32 width;
		u32 height;
		bool sync;
		ColorFormatParam colorFormat;
		DatatypeParam colorType;
		u32 colorFormatBytes;
	};

public:
	ColorBufferReader(CachedTexture * _pTexture);
	virtual ~ColorBufferReader() = default;

	virtual const u8 * readPixels(s32 _x0, s32 _y0, u32 _width, u32 _height, u32 _size, bool _sync);
	virtual void cleanUp() = 0;
	const u8* _convertFloatTextureBuffer(const u8* _gpuData, u32 _width, u32 _height, u32 _heightOffset, u32 _stride);
	const u8* _convertIntegerTextureBuffer(const u8* _gpuData, u32 _width, u32 _height,u32 _heightOffset, u32 _stride);

private:

	//Faster float to integer conversion. Taken from here:
	//http://stackoverflow.com/questions/429632/how-to-speed-up-floating-point-to-integer-number-conversion
	inline int _float2int( double d )
	{
		union Cast
		{
			double d;
			long l;
		};
		volatile Cast c;
		c.d = d + 6755399441055744.0;
		return c.l;
	}

	virtual const u8 * _readPixels(const ReadColorBufferParams& _params, u32& _heightOffset, u32& _stride) = 0;
protected:

	CachedTexture * m_pTexture;
	std::vector<u8> m_pixelData;
	std::vector<u8> m_tempPixelData;
};

}
