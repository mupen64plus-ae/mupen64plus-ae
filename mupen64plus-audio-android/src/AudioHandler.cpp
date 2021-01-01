#include "AudioHandler.h"
#include <thread>

void AudioHandler::DebugMessage(int level, const char *message, ...) {
	char msgbuf[1024];
	va_list args;

	if (mDebugCallback == nullptr || mDebugContext == nullptr)
		return;

	va_start(args, message);
	vsprintf(msgbuf, message, args);

	(*mDebugCallback)(mDebugContext, level, msgbuf);

	va_end(args);
}

AudioHandler &AudioHandler::get() {
	static AudioHandler audio;
	return audio;
}

AudioHandler::AudioHandler() :
		mOutStream(nullptr),
		mSoundBufferPool(1024 * 1024 * 50),
		mPlaybackPaused(false),
		mFeedTimes{},
		mGameTimes{}{
	reset();

	mSoundTouch.setSampleRate(mInputFreq);
	mSoundTouch.setChannels(numberOfChannels);
}

void AudioHandler::closeAudio() {
	if (mOutStream != nullptr) {
		mOutStream->close();
		mOutStream = nullptr;
	}
	/* Delete working buffer */
	if (mWorkingBuffer != nullptr) {
		delete[] mWorkingBuffer;
		mWorkingBuffer = nullptr;
	}
}

void AudioHandler::createWorkingBuffer() {
	auto workingBufferBytes = (unsigned int) (workingBufferSize * hwSamplesBytes);

	DebugMessage(M64MSG_VERBOSE, "Allocating memory for working audio buffer: %i bytes.",
				 workingBufferBytes);

	mWorkingBuffer = new unsigned char[workingBufferBytes];

	std::memset(mWorkingBuffer, 0, workingBufferBytes);

	mWorkingBufferValidBytes = 0;
}

void AudioHandler::initializeAudio(int _freq) {

	/* Sometimes a bad frequency is requested so ignore it */
	if (_freq < 4000)
		return;

	/* This is important for the sync */
	mInputFreq = _freq;

	if (mSamplingRateSelection == 0) {
		if ((_freq / 1000) <= 11) {
			mOutputFreq = 11025;
		} else if ((_freq / 1000) <= 22) {
			mOutputFreq = 22050;
		} else if ((_freq / 1000) <= 32) {
			mOutputFreq = 32000;
		} else {
			mOutputFreq = 44100;
		}
	} else {
		mOutputFreq = mSamplingRateSelection;
	}

	/* Close everything because InitializeAudio can be called more than once */
	closeAudio();

	oboe::DefaultStreamValues::SampleRate = (int32_t) mOutputFreq;
	oboe::DefaultStreamValues::FramesPerBurst = (int32_t) mHardwareBufferSize;

	oboe::AudioStreamBuilder builder;
	// The builder set methods can be chained for convenience.
	builder.setSharingMode(oboe::SharingMode::Exclusive);
	builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
	builder.setChannelCount(numberOfChannels);
	builder.setSampleRate(mOutputFreq);

	if (mForceSles != 0) {
		builder.setAudioApi(oboe::AudioApi::OpenSLES);
	}

#ifdef FP_ENABLED
	builder.setFormat(oboe::AudioFormat::Float);
#else
	builder.setFormat(oboe::AudioFormat::I16);
#endif

	builder.setCallback(this);
	if (builder.openManagedStream(mOutStream) == oboe::Result::OK) {

		if (mOutStream->getAudioApi() == oboe::AudioApi::AAudio) {
			mOutputFreq = mOutStream->getSampleRate();
			mHardwareBufferSize = mOutStream->getFramesPerBurst();
		}
		DebugMessage(M64MSG_INFO, "Requesting frequency: %iHz and buffer size %d", mOutputFreq,
					 mOutStream->getFramesPerBurst());
	} else {
		mOutStream.reset(nullptr);
	}

	/* Create working buffer */
	createWorkingBuffer();

	mSoundTouch.setSampleRate(mInputFreq);
	mSoundTouch.setChannels(numberOfChannels);
	mSoundTouch.setSetting(SETTING_USE_QUICKSEEK, 1);
	mSoundTouch.setSetting(SETTING_USE_AA_FILTER, 1);
	double speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
	mSoundTouch.setTempo(speedFactor);
	mSoundTouch.setRate((double) mInputFreq / (double) mOutputFreq);

	reset();

	if (mOutStream != nullptr) {
		mOutStream->requestStart();
	}
}

void AudioHandler::pushData(const int16_t *_data, int _samples,
							std::chrono::duration<double> timeSinceStart) {
    if (mPlaybackPaused) {
    	return;
    }

	static int failedToStartCount = 0;
	if (mOutStream == nullptr || mOutStream->getState() != oboe::StreamState::Started) {

		if (failedToStartCount++ == 100) {
			mForceSles = 1;
			initializeAudio(mInputFreq);
			DebugMessage(M64MSG_WARNING, "Reinitializing audio");
			failedToStartCount = 0;
		}
		return;
	}

	unsigned int numBytes = _samples * numberOfChannels*sizeof(int16_t);

	PoolBufferPointer data = mSoundBufferPool.createPoolBuffer(
			reinterpret_cast<const char *>(_data), numBytes);

	//Add data to the queue
	QueueData theQueueData;
	theQueueData.data = data;
	theQueueData.samples = _samples;
	theQueueData.timeSinceStart = timeSinceStart.count();

	mAudioConsumerQueue.enqueue(theQueueData);
}

oboe::DataCallbackResult
AudioHandler::onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {

	mPrimingTimeMs += static_cast<int>(static_cast<double>(numFrames) / mOutputFreq * 1000);

	if (mPrimingTimeMs > mTargetBuffersMs){
		mPrimeComplete = true;
	}

	if (mTimeStretchEnabled) {
		if (!audioProviderStretch(audioData, numFrames)) {
			injectSilence(audioData, numFrames);
		}
	} else {
		if (!mPrimeComplete) {
			injectSilence(audioData, numFrames);
		} else if (!audioProviderNoStretch(audioData, numFrames)) {
			injectSilence(audioData, numFrames);
			mPrimeComplete = false;
			mPrimingTimeMs = 0;
		}
	}
	return oboe::DataCallbackResult::Continue;
}

bool AudioHandler::audioProviderStretch(void *outAudioData, int32_t outNumFrames) {

	static bool drainQueue = false;

	//Sound queue ran dry, device is running slow
	int ranDry;

	//adjustment used when a device running too slow
	double slowAdjustment = 1.0;
	double currAdjustment = 1.0;

	//how quickly to return to original speed
	static const double minSlowValue = 0.1;
	static const double maxSlowValue = 10.0;
	static const float maxRateOffset = 0.5;
	static const float defaultSampleLength = 0.008336;

	static double prevTime = 0;

	int queueLength = static_cast<int>(static_cast<float>(mSoundTouch.numSamples())/static_cast<float>(mOutputFreq)*1000);

	int workingBufferValidBytes = 0;
	QueueData currQueueData;

	while (mAudioConsumerQueue.try_dequeue(currQueueData)) {
		auto shortData = reinterpret_cast<const int16_t *>(mSoundBufferPool.getBufferFromPool(
				currQueueData.data));
		workingBufferValidBytes = convertBufferToHwBuffer(shortData, currQueueData.samples, mWorkingBuffer,
														  workingBufferValidBytes);
		mSoundBufferPool.removeBufferFromPool(currQueueData.data);

		//Figure out how much to slow down by
		double timeDiff = currQueueData.timeSinceStart - prevTime;
		float temp = static_cast<float>(currQueueData.samples) / static_cast<float>(mInputFreq);

		prevTime = currQueueData.timeSinceStart;

		// Ignore really big numbers since that can happen because prevTime initializes to zero or negative
		// if the game was paused
		if (timeDiff > 0 && timeDiff < 1.0) {
			mFeedTimes[mFeedTimeIndex] = timeDiff;
			mAverageFeedTimeMs = getAverageTime(mFeedTimes.data(), mFeedTimesSet ? windowSize : (mFeedTimeIndex + 1));

			mGameTimes[mFeedTimeIndex] = static_cast<float>(currQueueData.samples) / static_cast<float>(mInputFreq);
			mAverageGameTimeMs = getAverageTime(mGameTimes.data(),mFeedTimesSet ? windowSize : (mFeedTimeIndex +  1));


			++mFeedTimeIndex;
			if (mFeedTimeIndex >= windowSize) {
				mFeedTimeIndex = 0;

				if (!mFeedTimesSet) {
					mFeedTimesSet = true;
				}
			}
		}
	}

	double bufferMultiplier = static_cast<double>(mOutputFreq) / mInputFreq;
	double currentGameRate = mAverageGameTimeMs / mAverageFeedTimeMs;
	int queueSizeOffset = static_cast<int>(mAverageGameTimeMs - defaultSampleLength);

	if (queueSizeOffset < 0) {
		queueSizeOffset = 0;
	}

	int minQueueSize = static_cast<int>(mTargetBuffersMs * bufferMultiplier) + queueSizeOffset;
	ranDry = queueLength < minQueueSize;

	static const int bufferLimit = static_cast<int>(static_cast<double>(maxBufferSizeMs - 20) * bufferMultiplier) + queueSizeOffset;

	int maxQueueSize = static_cast<int> ((mTargetBuffersMs + 60.0 + queueSizeOffset) * bufferMultiplier);

	if (maxQueueSize > bufferLimit) {
		maxQueueSize = bufferLimit;
	}

	bool samplesAdded;

	if (!mPrimeComplete || !mFeedTimesSet) {
		double speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
		mSoundTouch.setTempo(speedFactor);
		if (queueLength < minQueueSize) {
			processAudioSoundTouchNoOutput(workingBufferValidBytes);
		} else {
			processAudioSoundTouch(workingBufferValidBytes, outAudioData, outNumFrames);
		}
		samplesAdded = false;

	} else {

		//Game is running too fast speed up audio
		if ((queueLength > maxQueueSize || drainQueue) && !ranDry) {
			drainQueue = true;
			currAdjustment = currentGameRate +
							 static_cast<float> (queueLength - minQueueSize) /
							 static_cast<float> (maxQueueSize - minQueueSize) *
							 maxRateOffset;
		}
		//Device can't keep up with the game
		else if (ranDry) {
			drainQueue = false;
			currAdjustment = currentGameRate +
							 static_cast<float> (queueLength - minQueueSize) /
							 static_cast<float> (maxQueueSize - minQueueSize) *
							 maxRateOffset;
		//Good case
		} else if (queueLength < maxQueueSize) {
			currAdjustment = currentGameRate;
		}

		// Bounds checking
		currAdjustment = std::max(minSlowValue, currAdjustment);
		currAdjustment = std::min(maxSlowValue, currAdjustment);

		//Adjust tempo in x% increments so it's more steady
		slowAdjustment = currAdjustment;
		static const int increments = 4;
		double temp2 = round((slowAdjustment * 100) / increments);
		temp2 *= increments;
		slowAdjustment = temp2 / 100;

		mSoundTouch.setTempo(slowAdjustment);

		samplesAdded = processAudioSoundTouch(workingBufferValidBytes, outAudioData, outNumFrames);
	}

//	if (!samplesAdded)
/*
	{
	 DebugMessage(M64MSG_ERROR, "hw_length=%d, dry=%d, drain=%d, slow_adj=%f, curr_adj=%f, temp=%f, feed_time=%f, game_time=%f, min_size=%d, max_size=%d, pos=%d",
				  queueLength, ranDry, drainQueue, slowAdjustment, currAdjustment, currentGameRate, mAverageFeedTimeMs, mAverageGameTimeMs, minQueueSize, maxQueueSize, mFeedTimeIndex);
	}
 */

	return samplesAdded;
}

bool AudioHandler::audioProviderNoStretch(void *audioData, int32_t numFrames) {

	QueueData currQueueData;

	while (mAudioConsumerQueue.try_dequeue(currQueueData)) {
		auto shortData = reinterpret_cast<const int16_t *>(mSoundBufferPool.getBufferFromPool(
				currQueueData.data));
		mWorkingBufferValidBytes = convertBufferToHwBuffer(shortData, currQueueData.samples, mWorkingBuffer,
														   mWorkingBufferValidBytes);
		mSoundBufferPool.removeBufferFromPool(currQueueData.data);
	}

	if (mSamplingType == 0) {
		return processAudioTrivial(mWorkingBufferValidBytes, audioData, numFrames);
	} else {
		static int lastSpeedFactor = mSpeedFactor;

		if (lastSpeedFactor != mSpeedFactor) {
			lastSpeedFactor = mSpeedFactor;
			mSoundTouch.setTempo(static_cast<double>(mSpeedFactor) / 100.0);
		}

		return processAudioSoundTouch(mWorkingBufferValidBytes, audioData, numFrames);
	}
}

int AudioHandler::convertBufferToHwBuffer(const int16_t *inputBuffer, unsigned int inputSamples,
										  unsigned char *outputBuffer, int outputBufferStart) {

	static const auto maxWorkingBufferBytes = (unsigned int) (workingBufferSize * hwSamplesBytes);

	if ((outputBufferStart + inputSamples) * hwSamplesBytes < maxWorkingBufferBytes) {

#ifndef FP_ENABLED
		auto outputBufferType = reinterpret_cast<int16_t*>(outputBuffer);
		int outputStart = outputBufferStart / static_cast<int>(sizeof(int16_t));
		float volume = static_cast<float>(mVolume) / 100.0f;
		for (int sampleIndex = 0; sampleIndex < inputSamples; ++sampleIndex) {
			int bufferIndex = sampleIndex * numberOfChannels;
			if (mSwapChannels == 0) {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = static_cast<int16_t>(static_cast<float>(inputBuffer[bufferIndex + 1])*volume);
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = static_cast<int16_t>(static_cast<float>(inputBuffer[bufferIndex])*volume);
			} else {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = static_cast<int16_t>(static_cast<float>(inputBuffer[bufferIndex])*volume);
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = static_cast<int16_t>(static_cast<float>(inputBuffer[bufferIndex + 1])*volume);
			}
		}
#else
		auto outputBufferType = reinterpret_cast<float*>(outputBuffer);
		int outputStart = outputBufferStart/static_cast<int>(sizeof(float));
		float volume = static_cast<float>(mVolume)/100.0f;
		for (int sampleIndex = 0; sampleIndex < inputSamples; ++sampleIndex) {
			int bufferIndex = sampleIndex*numberOfChannels;
			if (mSwapChannels == 0) {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = static_cast<float>(inputBuffer[bufferIndex + 1])/32767.0*volume;
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = static_cast<float>(inputBuffer[bufferIndex])/32767.0*volume;
			} else {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = static_cast<float>(inputBuffer[bufferIndex])/32767.0*volume;
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = static_cast<float>(inputBuffer[bufferIndex + 1])/32767.0*volume;
			}
		}
#endif

		outputBufferStart += static_cast<int>(inputSamples) * hwSamplesBytes;
	} else
		DebugMessage(M64MSG_WARNING, "convertBufferToHwBuffer(): Audio working buffer overflow.");

	return outputBufferStart;
}

bool AudioHandler::processAudioSoundTouch(int& validBytes, void *outAudioData, int32_t outNumFrames) {

	bool bytesWritten = false;

	if (validBytes != 0 ) {
		mSoundTouch.putSamples(reinterpret_cast<soundtouch::SAMPLETYPE *>(mWorkingBuffer), validBytes / hwSamplesBytes);
	}

	if (mSoundTouch.numSamples() >= outNumFrames) {
		mSoundTouch.receiveSamples(
				reinterpret_cast<soundtouch::SAMPLETYPE *>(outAudioData),
				static_cast<unsigned int>(outNumFrames));
		bytesWritten = true;
	}

	validBytes = 0;

	return bytesWritten;
}

void AudioHandler::processAudioSoundTouchNoOutput(int& validBytes)
{
	mSoundTouch.putSamples(reinterpret_cast<soundtouch::SAMPLETYPE *>(mWorkingBuffer), validBytes / hwSamplesBytes);
}

int
AudioHandler::resample(const unsigned char *input, int bytesPerSample, int oldsamplerate, unsigned char *output,
		 int output_needed, int newsamplerate) {
	int consumedSamples = 0;

	if (newsamplerate >= oldsamplerate) {
		int sldf = oldsamplerate;
		int const2 = 2 * sldf;
		int dldf = newsamplerate;
		int const1 = const2 - 2 * dldf;
		int criteria = const2 - dldf;
		for (int index = 0; index < output_needed / bytesPerSample; ++index) {
			std::copy_n(input + consumedSamples * bytesPerSample, bytesPerSample,
						output + index * bytesPerSample);
			if (criteria >= 0) {
				++consumedSamples;
				criteria += const1;
			} else criteria += const2;
		}
		return consumedSamples * bytesPerSample; //number of bytes consumed
	}

	// newsamplerate < oldsamplerate, this only happens when mSpeedFactor > 1
	int index;
	for (index = 0; index < output_needed / bytesPerSample; ++index) {
		consumedSamples = index * oldsamplerate / newsamplerate;
		std::copy_n(input + consumedSamples * bytesPerSample, bytesPerSample,
					output + index * bytesPerSample);
	}

	return consumedSamples * bytesPerSample; //number of bytes consumed
}


bool AudioHandler::processAudioTrivial(int& validBytes, void *outAudioData, int32_t outNumFrames) {

	bool dataWritten = false;
	int hardwareBufferBytes = outNumFrames * hwSamplesBytes;
	auto hardwareBuffer = reinterpret_cast<unsigned char*>(outAudioData);

	int newsamplerate = mOutputFreq * 100 / mSpeedFactor;

	if (validBytes >= ((hardwareBufferBytes * mInputFreq) / newsamplerate)) {
		int input_used = resample(mWorkingBuffer, hwSamplesBytes, mInputFreq, hardwareBuffer,
								  hardwareBufferBytes, newsamplerate);

		memmove(mWorkingBuffer, &mWorkingBuffer[input_used], validBytes - input_used);
		validBytes -= input_used;
		dataWritten = true;
	}

	return dataWritten;
}

double AudioHandler::getAverageTime(const double *feedTimes, int numTimes) {
	double sum = 0;
	for (int index = 0; index < numTimes; ++index) {
		sum += feedTimes[index];
	}

	return sum / (float) numTimes;
}

void AudioHandler::setHardwareBufferSize(int _hardwareBufferSize) {
	mHardwareBufferSize = _hardwareBufferSize;
}

void AudioHandler::setTimeStretchEnabled(int _timeStretchEnabled) {
	mTimeStretchEnabled = _timeStretchEnabled;
}

void AudioHandler::setSamplingType(int _samplingType) {
	mSamplingType = _samplingType;
}

void AudioHandler::setTargetPrimingBuffersMs(int _targetPrimingBuffersMs) {
	mTargetBuffersMs = _targetPrimingBuffersMs;
}

void AudioHandler::setSamplingRateSelection(int _samplingRateSelection) {
	mSamplingRateSelection = _samplingRateSelection;
}

void AudioHandler::setVolume(int _volume) {
	DebugMessage(M64MSG_INFO, "Change volume to %d", _volume);
	mVolume = _volume;
}

void AudioHandler::forceSles(int _forceSles) {
	mForceSles = _forceSles;
}

void AudioHandler::setSpeedFactor(int _speedFactor) {
	mSpeedFactor = _speedFactor;
}

void AudioHandler::setLoggingFunction(void *_context,
									  void (*_debugCallback)(void *, int, const char *)) {
	mDebugContext = _context;
	mDebugCallback = _debugCallback;
}

void AudioHandler::setSwapChannels(bool _swapChannels) {
	mSwapChannels = _swapChannels;
}

void AudioHandler::pausePlayback() {
	if (!mPlaybackPaused) {

		mPlaybackPaused = true;

		if (mOutStream != nullptr) {
			mOutStream->stop();
		}

		reset();
	}
}

void AudioHandler::resumePlayback() {
	if (mPlaybackPaused) {
		if (mOutStream != nullptr) {
			mOutStream->start();
		}
		mPlaybackPaused = false;
	}
}

void AudioHandler::injectSilence (void *audioData, int32_t numFrames)
{
	// If there was no data, fill it with zeroes
	char *data = reinterpret_cast<char *>(audioData);
	for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {

		int startIndex = frameIndex * hwSamplesBytes;

		for (int byteIndex = 0; byteIndex < hwSamplesBytes; ++byteIndex) {
			data[startIndex + byteIndex] = 0;
		}
	}
}

void AudioHandler::reset()
{
	mPrimeComplete = false;
	mPrimingTimeMs = 0;
	mWorkingBufferValidBytes = 0;
	mFeedTimes.fill(0.0);
	mGameTimes.fill(0.0);
	mFeedTimeIndex = 0;
	mFeedTimesSet = false;

	mSoundTouch.clear();

	// Clear all pending buffers
	QueueData currQueueData;
	while (mAudioConsumerQueue.try_dequeue(currQueueData)) {
		mSoundBufferPool.removeBufferFromPool(currQueueData.data);
	}
}