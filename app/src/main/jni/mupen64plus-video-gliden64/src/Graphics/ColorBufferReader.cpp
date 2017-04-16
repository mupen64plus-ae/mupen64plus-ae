
#include "ColorBufferReader.h"
#include "FramebufferTextureFormats.h"
#include "Context.h"
#include "Parameters.h"
#include <algorithm>

namespace graphics {

	ColorBufferReader::ColorBufferReader(CachedTexture * _pTexture)
		: m_pTexture(_pTexture)
	{
		m_pixelData.resize(m_pTexture->textureBytes);
		m_tempPixelData.resize(m_pTexture->textureBytes);
	}

	const u8* ColorBufferReader::_convertFloatTextureBuffer(const u8* _gpuData, u32 _width, u32 _height,
		u32 _heightOffset, u32 _stride)
	{
		std::copy_n(_gpuData, m_tempPixelData.size(), m_tempPixelData.data());
		u8* pixelDataAlloc = m_pixelData.data();
		float* pixelData = reinterpret_cast<float*>(m_tempPixelData.data());
		int colorsPerPixel = 4;
		int widthPixels = _width * colorsPerPixel;
		int stridePixels = _stride * colorsPerPixel;

		for (unsigned int index = 0; index < _height; ++index) {
			for (unsigned int widthIndex = 0; widthIndex < widthPixels; ++widthIndex) {
				u8& dest = *(pixelDataAlloc + index*widthPixels + widthIndex);
				float& src = *(pixelData + (index+_heightOffset)*stridePixels + widthIndex);
				dest = static_cast<u8>(_float2int(src*255.0));
			}
		}

		return pixelDataAlloc;
	}

	const u8* ColorBufferReader::_convertIntegerTextureBuffer(const u8* _gpuData, u32 _width, u32 _height,
		u32 _heightOffset, u32 _stride)
	{
		int colorsPerPixel = 4;
		size_t widthBytes = _width * colorsPerPixel;
		int strideBytes = _stride * colorsPerPixel;

		u8* pixelDataAlloc = m_pixelData.data();
		for (unsigned int lnIndex = 0; lnIndex < _height; ++lnIndex) {
			memcpy(pixelDataAlloc + lnIndex*widthBytes, _gpuData + ((lnIndex+_heightOffset)*strideBytes), widthBytes);
		}

		return pixelDataAlloc;
	}


	const u8 * ColorBufferReader::readPixels(s32 _x0, s32 _y0, u32 _width, u32 _height, u32 _size, bool _sync)
	{
		const FramebufferTextureFormats & fbTexFormat = gfxContext.getFramebufferTextureFormats();

		ReadColorBufferParams params;
		params.x0 = _x0;
		params.y0 = _y0;
		params.width = _width;
		params.height = _height;
		params.sync = _sync;

		if (_size > G_IM_SIZ_8b) {
			params.colorFormat = fbTexFormat.colorFormat;
			params.colorType = fbTexFormat.colorType;
			params.colorFormatBytes = fbTexFormat.colorFormatBytes;
		}
		else {
			params.colorFormat = fbTexFormat.monochromeFormat;
			params.colorType = fbTexFormat.monochromeType;
			params.colorFormatBytes = fbTexFormat.monochromeFormatBytes;
		}

		u32 heightOffset = 0;
		u32 stride = 0;
		const u8* pixelData = _readPixels(params, heightOffset, stride);

		if (pixelData == nullptr)
			return nullptr;

		if(params.colorType == datatype::FLOAT) {
			return _convertFloatTextureBuffer(pixelData, params.width, params.height, 0, m_pTexture->realWidth);
		} else {
			return _convertIntegerTextureBuffer(pixelData, params.width, params.height, 0, m_pTexture->realWidth);
		}
	}
}
