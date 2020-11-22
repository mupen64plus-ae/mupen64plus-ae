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
		mState{},
		mSoundBufferPool(1024 * 1024 * 50) {
}

void AudioHandler::closeAudio() {
	if (mOutStream != nullptr) {
		mOutStream->close();
	}
	/* Delete Primary buffer */
	if (mPrimaryBuffer != nullptr) {
		mPrimaryBufferBytes = 0;
		delete[] mPrimaryBuffer;
		mPrimaryBuffer = nullptr;
	}

	/* Delete Secondary buffer */
	if (mSecondaryBuffer != nullptr) {
		delete[] mSecondaryBuffer;
		mSecondaryBuffer = nullptr;
	}
}

bool AudioHandler::isCriticalFailure() const {
	return mCriticalFailure;
}

void AudioHandler::createPrimaryBuffer() {
	auto primaryBytes = (unsigned int) (primaryBufferSize * hwSamplesBytes);

	DebugMessage(M64MSG_VERBOSE, "Allocating memory for primary audio buffer: %i bytes.",
				 primaryBytes);

	mPrimaryBuffer = new unsigned char[primaryBytes];

	std::memset(mPrimaryBuffer, 0, primaryBytes);
	mPrimaryBufferBytes = primaryBytes;
}

void AudioHandler::createSecondaryBuffer() {
	int secondaryBytes = mSecondaryBufferSize * hwSamplesBytes;

	DebugMessage(M64MSG_VERBOSE, "Allocating memory for %d secondary audio buffers: %i bytes.",
				 secondaryBufferNumber, secondaryBytes);

	/* Allocate size of secondary buffer */
	mSecondaryBuffer = new unsigned char[secondaryBytes];
	std::memset(mSecondaryBuffer, 0, (size_t) secondaryBytes);
}

void AudioHandler::initializeAudio(int _freq) {

	/* Sometimes a bad frequency is requested so ignore it */
	if (_freq < 4000)
		return;

	if (mCriticalFailure)
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

	/* Create primary buffer */
	createPrimaryBuffer();

	/* Create secondary buffer */
	createSecondaryBuffer();

	oboe::AudioStreamBuilder builder;
	// The builder set methods can be chained for convenience.
	builder.setSharingMode(oboe::SharingMode::Exclusive);
	builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
	builder.setChannelCount(numberOfChannels);
	builder.setSampleRate(mOutputFreq);

#ifdef FP_ENABLED
	builder.setFormat(oboe::AudioFormat::Float);
#else
	builder.setFormat(oboe::AudioFormat::I16);
#endif

	builder.setCallback(this);
	builder.openManagedStream(mOutStream);

	DebugMessage(M64MSG_INFO, "Requesting frequency: %iHz and buffer size %d", mOutputFreq,
				 mOutStream->getFramesPerBurst());

	mState.errors = 0;

	mSoundTouch.setSampleRate(mInputFreq);
	mSoundTouch.setChannels(numberOfChannels);
	mSoundTouch.setSetting(SETTING_USE_QUICKSEEK, 1);
	mSoundTouch.setSetting(SETTING_USE_AA_FILTER, 1);
	double speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
	mSoundTouch.setTempo(speedFactor);
	mSoundTouch.setRate((double) mInputFreq / (double) mOutputFreq);

	mOutStream->requestStart();
}

void AudioHandler::pushData(const int16_t *_data, int _samples,
							std::chrono::duration<double> timeSinceStart) {

	int numBytes = _samples * numberOfChannels*sizeof(int16_t);

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
	if (!audioProviderNoStretch(audioData, numFrames)) {
		DebugMessage(M64MSG_ERROR, "NO DATA");
		// If there was no data, fill it with zeroes
		char *data = reinterpret_cast<char *>(audioData);
		for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {

			int startIndex = frameIndex * hwSamplesBytes;

			for (int byteIndex = 0; byteIndex < hwSamplesBytes; ++byteIndex) {
				data[startIndex + byteIndex] = 0;
			}
		}
	}
	return oboe::DataCallbackResult::Continue;
}

bool AudioHandler::audioProviderStretch(void *audioData, int32_t numFrames, void *outAudioData,
										int32_t outNumFrames) {
	/*
	static int sequenceLenMS = 63;
	static int seekWindowMS = 16;
	static int overlapMS = 7;*/

	mSoundTouch.setSampleRate((uint) mInputFreq);
	mSoundTouch.setChannels(numberOfChannels);
	mSoundTouch.setSetting(SETTING_USE_QUICKSEEK, 1);
	mSoundTouch.setSetting(SETTING_USE_AA_FILTER, 1);
	//soundTouch.setSetting( SETTING_SEQUENCE_MS, sequenceLenMS );
	//soundTouch.setSetting( SETTING_SEEKWINDOW_MS, seekWindowMS );
	//soundTouch.setSetting( SETTING_OVERLAP_MS, overlapMS );

	mSoundTouch.setRate((double) mInputFreq / (double) mOutputFreq);
	double speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
	mSoundTouch.setTempo(speedFactor);

	double bufferMultiplier = (static_cast<double>(mOutputFreq) / mInputFreq) *
							  (static_cast<double>(defaultSecondaryBufferSize) /
							   mSecondaryBufferSize);

	int bufferLimit = secondaryBufferNumber - 20;
	int maxQueueSize = (int) ((mTargetSecondaryBuffers + 30.0) * bufferMultiplier);
	if (maxQueueSize > bufferLimit) {
		maxQueueSize = bufferLimit;
	}
	int minQueueSize = (int) (mTargetSecondaryBuffers * bufferMultiplier);
	bool drainQueue = false;

	//Sound queue ran dry, device is running slow
	int ranDry;

	//adjustment used when a device running too slow
	double slowAdjustment;
	double currAdjustment = 1.0;

	//how quickly to return to original speed
	const double minSlowValue = 0.2;
	const double maxSlowValue = 3.0;
	const float maxSpeedUpRate = 0.5;
	const float slowRate = 0.05;
	const float defaultSampleLength = 0.01666;

	double prevTime = 0;

	static const int maxWindowSize = 500;

	int feedTimeWindowSize = 50;

	int feedTimeIndex = 0;
	bool feedTimesSet = false;
	double feedTimes[maxWindowSize] = {};
	double gameTimes[maxWindowSize] = {};
	double averageGameTime = defaultSampleLength;
	double averageFeedTime = defaultSampleLength;

	//while (!mShutdownThread) {

	int hardwareQueueLength = secondaryBufferNumber;

	oboe::ResultWithValue<int32_t> availableFrames = mOutStream->getAvailableFrames();

	if (availableFrames.error() == oboe::Result::OK) {
		hardwareQueueLength = availableFrames.value();
	}

	auto totalFrames = mOutStream->getFramesWritten();

	ranDry = hardwareQueueLength < minQueueSize;

	QueueData currQueueData;
	if (mAudioConsumerQueue.wait_dequeue_timed(currQueueData, std::chrono::milliseconds(100))) {

		double temp = averageGameTime / averageFeedTime;
		int inputSamples = currQueueData.samples;

		if (totalFrames < secondaryBufferNumber) {

			speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
			mSoundTouch.setTempo(speedFactor);

			auto shortData = reinterpret_cast<const int16_t *>(mSoundBufferPool.getBufferFromPool(
					currQueueData.data));
			processAudioSoundTouch(inputSamples, outAudioData, outNumFrames);
			mSoundBufferPool.removeBufferFromPool(currQueueData.data);

		} else {

			//Game is running too fast speed up audio
			if ((hardwareQueueLength > maxQueueSize || drainQueue) && !ranDry) {
				drainQueue = true;
				currAdjustment = temp +
								 (float) (hardwareQueueLength - minQueueSize) /
								 (float) (secondaryBufferNumber - minQueueSize) *
								 maxSpeedUpRate;
			}
				//Device can't keep up with the game
			else if (ranDry) {
				drainQueue = false;
				currAdjustment = temp - slowRate;
				//Good case
			} else if (hardwareQueueLength < maxQueueSize) {
				currAdjustment = temp;
			}

			//Allow the tempo to slow quickly with no minimum value change, but restore original tempo more slowly.
			if (currAdjustment > minSlowValue && currAdjustment < maxSlowValue) {
				slowAdjustment = currAdjustment;
				static const int increments = 4;
				//Adjust tempo in x% increments so it's more steady
				double temp2 = round((slowAdjustment * 100) / increments);
				temp2 *= increments;
				slowAdjustment = (temp2) / 100;

				mSoundTouch.setTempo(slowAdjustment);
			}

			auto shortData = reinterpret_cast<const int16_t *>(mSoundBufferPool.getBufferFromPool(
					currQueueData.data));
			processAudioSoundTouch(inputSamples, outAudioData, outNumFrames);
			mSoundBufferPool.removeBufferFromPool(currQueueData.data);
		}

		//Useful logging
		/*
		 if(hardwareQueueLength == 0)
		{
		 DebugMessage(M64MSG_ERROR, "hw_length=%d, dry=%d, drain=%d, slow_adj=%f, curr_adj=%f, temp=%f, feed_time=%f, game_time=%f, min_size=%d, max_size=%dd",
					hardwareQueueLength, ranDry, drainQueue, slowAdjustment, currAdjustment, temp, averageFeedTime, averageGameTime, minQueueSize, maxQueueSize);
		}
		 */

		//We don't want to calculate the average until we give everything a time to settle.

		//Figure out how much to slow down by
		double timeDiff = currQueueData.timeSinceStart - prevTime;
		prevTime = currQueueData.timeSinceStart;

		// Ignore negative time, it can be negative if game is falling too far behind real time
		if (timeDiff > 0) {
			feedTimes[feedTimeIndex] = timeDiff;
			averageFeedTime = getAverageTime(feedTimes,
											 feedTimesSet ? feedTimeWindowSize : (feedTimeIndex +
																				  1));

			gameTimes[feedTimeIndex] = static_cast<float>(currQueueData.samples) / mInputFreq;
			averageGameTime = getAverageTime(gameTimes,
											 feedTimesSet ? feedTimeWindowSize : (feedTimeIndex +
																				  1));

			++feedTimeIndex;
			if (feedTimeIndex >= feedTimeWindowSize) {
				feedTimeIndex = 0;
				feedTimesSet = true;
			}

			//Normalize window size
			feedTimeWindowSize = static_cast<int>(defaultSampleLength / averageGameTime * 50);
			if (feedTimeWindowSize > maxWindowSize) {
				feedTimeWindowSize = maxWindowSize;
			}
		}
	} else {
		pausePlayback();
	}
	//}

	return false;
}

bool AudioHandler::audioProviderNoStretch(void *audioData, int32_t numFrames) {

	static int primaryBufferPos = 0;
	QueueData currQueueData;

	while (mAudioConsumerQueue.try_dequeue(currQueueData)) {
		auto shortData = reinterpret_cast<const int16_t *>(mSoundBufferPool.getBufferFromPool(
				currQueueData.data));
		primaryBufferPos = convertBufferToHwBuffer(shortData, currQueueData.samples, mPrimaryBuffer,
												   primaryBufferPos);
		mSoundBufferPool.removeBufferFromPool(currQueueData.data);
	}

	if (mSamplingType == 0) {
		return processAudioTrivial(primaryBufferPos, audioData, numFrames);
	} else {
		return processAudioSoundTouch(primaryBufferPos, audioData, numFrames);
	}
}

int AudioHandler::convertBufferToHwBuffer(const int16_t *inputBuffer, unsigned int inputSamples,
										  unsigned char *outputBuffer, int outputBufferStart) {
	if ((outputBufferStart + inputSamples) * hwSamplesBytes < mPrimaryBufferBytes) {

#ifndef FP_ENABLED
		auto outputBufferType = reinterpret_cast<int16_t *>(outputBuffer);
		int outputStart = outputBufferStart / static_cast<int>(sizeof(int16_t));
		for (int sampleIndex = 0; sampleIndex < inputSamples; ++sampleIndex) {
			int bufferIndex = sampleIndex * numberOfChannels;
			if (mSwapChannels == 0) {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = inputBuffer[bufferIndex + 1];
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = inputBuffer[bufferIndex];
			} else {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = inputBuffer[bufferIndex];
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = inputBuffer[bufferIndex + 1];
			}
		}
#else
		auto outputBufferType = reinterpret_cast<float*>(outputBuffer);
		int outputStart = outputBufferStart/static_cast<int>(sizeof(float));
		for (int sampleIndex = 0; sampleIndex < inputSamples; ++sampleIndex) {
			int bufferIndex = sampleIndex*numberOfChannels;
			if (mSwapChannels == 0) {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = static_cast<float>(inputBuffer[bufferIndex + 1])/32767.0;
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = static_cast<float>(inputBuffer[bufferIndex])/32767.0;
			} else {
				// Left channel
				outputBufferType[outputStart + bufferIndex] = static_cast<float>(inputBuffer[bufferIndex])/32767.0;
				// Right channel
				outputBufferType[outputStart + bufferIndex + 1] = static_cast<float>(inputBuffer[bufferIndex + 1])/32767.0;
			}
		}
#endif

		outputBufferStart += static_cast<int>(inputSamples) * hwSamplesBytes;
	} else
		DebugMessage(M64MSG_WARNING, "convertBufferToHwBuffer(): Audio primary buffer overflow.");

	return outputBufferStart;
}

bool AudioHandler::processAudioSoundTouch(int& primaryBufferPos, void *outAudioData, int32_t outNumFrames) {

	static int lastSpeedFactor = mSpeedFactor;

	if (lastSpeedFactor != mSpeedFactor) {
		lastSpeedFactor = mSpeedFactor;
		mSoundTouch.setTempo(static_cast<double>(mSpeedFactor) / 100.0);
	}

	bool bytesWritten = false;

	mSoundTouch.putSamples(reinterpret_cast<soundtouch::SAMPLETYPE *>(mPrimaryBuffer), primaryBufferPos/hwSamplesBytes);

	if (mSoundTouch.numSamples() >= outNumFrames) {
		mSoundTouch.receiveSamples(
				reinterpret_cast<soundtouch::SAMPLETYPE *>(outAudioData),
				static_cast<unsigned int>(outNumFrames));
		bytesWritten = true;
	}

	primaryBufferPos = 0;

	return bytesWritten;
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


bool AudioHandler::processAudioTrivial(int& primaryBufferPos, void *outAudioData, int32_t outNumFrames) {

	bool dataWritten = false;
	int secondaryBufferBytes = outNumFrames * hwSamplesBytes;
	auto secondaryBuffer = reinterpret_cast<unsigned char*>(outAudioData);

	int newsamplerate = mOutputFreq * 100 / mSpeedFactor;

	if (primaryBufferPos >= ((secondaryBufferBytes * mInputFreq) / newsamplerate)) {
		int input_used = resample(mPrimaryBuffer, hwSamplesBytes, mInputFreq, secondaryBuffer,
								  secondaryBufferBytes, newsamplerate);

		memmove(mPrimaryBuffer, &mPrimaryBuffer[input_used], primaryBufferPos - input_used);
		primaryBufferPos -= input_used;
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

void AudioHandler::setSecondaryBufferSize(int _secondaryBufferSize) {
	mSecondaryBufferSize = _secondaryBufferSize;
}

void AudioHandler::setTimeStretchEnabled(int _timeStretchEnabled) {
	mTimeStretchEnabled = _timeStretchEnabled;
}

void AudioHandler::setSamplingType(int _samplingType) {
	mSamplingType = _samplingType;
}

void AudioHandler::setTargetSecondaryBuffers(int _targetSecondaryBuffers) {
	mTargetSecondaryBuffers = _targetSecondaryBuffers;
}

void AudioHandler::setSamplingRateSelection(int _samplingRateSelection) {
	mSamplingRateSelection = _samplingRateSelection;
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
		if (mOutStream != nullptr) {
			mOutStream->pause();
		}

		mPlaybackPaused = true;
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