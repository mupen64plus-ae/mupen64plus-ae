#include "RingBufferPool.h"
#include <memory>
#include <algorithm>

namespace opengl {


PoolBufferPointer::PoolBufferPointer() :
	m_offset(0),
	m_size(0),
	m_realSize(0),
	m_isValid(false)
{

}

PoolBufferPointer::PoolBufferPointer(size_t _offset, size_t _size, size_t _realSize, bool _isValid) :
	m_offset(_offset),
	m_size(_size),
	m_realSize(0),
	m_isValid(_isValid)
{
}

PoolBufferPointer::PoolBufferPointer(const PoolBufferPointer& other) :
	m_offset(other.m_offset),
	m_size(other.m_size),
	m_realSize(other.m_realSize),
	m_isValid(other.m_isValid)
{
}

PoolBufferPointer& PoolBufferPointer::operator=(const PoolBufferPointer& other)
{
	m_offset = other.m_offset;
	m_size = other.m_size;
	m_realSize = other.m_realSize;
	m_isValid = other.m_isValid;
	return *this;
}

bool PoolBufferPointer::isValid() const
{
	return m_isValid;
}

size_t PoolBufferPointer::getSize() const
{
	return m_size;
}

RingBufferPool::RingBufferPool(size_t _poolSize) :
	m_poolBuffer(_poolSize, 0),
	m_inUseStartOffset(0),
	m_inUseEndOffset(0)
{
}

PoolBufferPointer RingBufferPool::createPoolBuffer(const char* _buffer, size_t _bufferSize)
{
	size_t realBufferSize = _bufferSize;
	size_t remainder = _bufferSize % 4;

	if (remainder != 0)
		realBufferSize = _bufferSize + 4 - remainder;

	size_t remaining = m_inUseStartOffset > m_inUseEndOffset ? static_cast<size_t>(m_inUseStartOffset - m_inUseEndOffset) :
		m_poolBuffer.size() - m_inUseEndOffset + m_inUseStartOffset;

	bool isValid = remaining >= realBufferSize;

	size_t startOffset = 0;

	// We have determined that it fits
	if (isValid) {
		// We don't want to split data between the end of the ring buffer and the start
		// Re-check buffer size if we are going to start at the beginning of the ring buffer
		if (m_inUseEndOffset + realBufferSize > m_poolBuffer.size()) {
			isValid =  realBufferSize < m_inUseStartOffset || m_inUseStartOffset == m_inUseEndOffset;

			if (isValid) {
				startOffset = 0;
				m_inUseEndOffset = realBufferSize;
			} else {
				std::unique_lock<std::mutex> lock(m_mutex);
				m_condition.wait(lock, [this, realBufferSize]{ return realBufferSize < m_inUseStartOffset || m_inUseStartOffset == m_inUseEndOffset; });

				return createPoolBuffer(_buffer, realBufferSize);
			}
		} else {
			startOffset = m_inUseEndOffset;
			m_inUseEndOffset += realBufferSize;
		}
	} else {
	    // Wait until enough space is avalable
        std::unique_lock<std::mutex> lock(m_mutex);
        m_condition.wait(lock, [this, realBufferSize]{
            size_t remaining = m_inUseStartOffset > m_inUseEndOffset ? static_cast<size_t>(m_inUseStartOffset - m_inUseEndOffset) :
                               m_poolBuffer.size() - m_inUseEndOffset + m_inUseStartOffset;
            return remaining >= realBufferSize;
        });

        return createPoolBuffer(_buffer, realBufferSize);
	}

	if (isValid) {
		std::copy_n(_buffer, realBufferSize, &m_poolBuffer[startOffset]);
	}

	return PoolBufferPointer(startOffset, _bufferSize, realBufferSize, isValid);
}

const char* RingBufferPool::getBufferFromPool(PoolBufferPointer _poolBufferPointer)
{
	if (!_poolBufferPointer.isValid()) {
		return nullptr;
	} else {
		return m_poolBuffer.data() + _poolBufferPointer.m_offset;
	}
}

void RingBufferPool::removeBufferFromPool(PoolBufferPointer _poolBufferPointer)
{
	std::unique_lock<std::mutex> lock(m_mutex);
	m_inUseStartOffset = _poolBufferPointer.m_offset + _poolBufferPointer.m_size;
	m_condition.notify_one();
}

}
