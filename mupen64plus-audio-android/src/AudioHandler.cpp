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

AudioHandler& AudioHandler::get()
{
	static AudioHandler audio;
	return audio;
}

AudioHandler::AudioHandler() :
    mShutdownThread(true),
    mState{},
	mSoundBufferPool(1024*1024)
{
}

void AudioHandler::closeAudio() {
	if (!mShutdownThread) {
		mShutdownThread = true;

		if (mAudioConsumerThread.joinable()) {
			mAudioConsumerThread.join();
		}
	}

	/* Delete Primary buffer */
	if (mPrimaryBuffer != nullptr) {
		mPrimaryBufferBytes = 0;
		delete[] mPrimaryBuffer;
		mPrimaryBuffer = nullptr;
	}

	/* Delete Secondary buffers */
	if (mSecondaryBuffers != nullptr) {
		for (int index = 0; index < secondaryBufferNumber; ++index) {
			if (mSecondaryBuffers[index] != nullptr) {
				delete[] mSecondaryBuffers[index];
				mSecondaryBuffers[index] = nullptr;
			}
		}
		delete[] mSecondaryBuffers;
		mSecondaryBuffers = nullptr;
	}

	/* Destroy buffer queue audio player object, and invalidate all associated interfaces */
	if (mPlayerObject != nullptr && mPlayerPlay != nullptr) {
		SLuint32 state = SL_PLAYSTATE_PLAYING;
		(*mPlayerPlay)->SetPlayState(mPlayerPlay, SL_PLAYSTATE_STOPPED);

		while (state != SL_PLAYSTATE_STOPPED)
			(*mPlayerPlay)->GetPlayState(mPlayerPlay, &state);

		(*mPlayerObject)->Destroy(mPlayerObject);
		mPlayerObject = nullptr;
		mPlayerPlay = nullptr;
		mBufferQueue = nullptr;
	}

	/* Destroy output mix object, and invalidate all associated interfaces */
	if (mOutputMixObject != nullptr) {
		(*mOutputMixObject)->Destroy(mOutputMixObject);
		mOutputMixObject = nullptr;
	}

	/* Destroy engine object, and invalidate all associated interfaces */
	if (mEngineObject != nullptr) {
		(*mEngineObject)->Destroy(mEngineObject);
		mEngineObject = nullptr;
		mEngineEngine = nullptr;
	}
}

bool AudioHandler::isCriticalFailure() const {
	return mCriticalFailure;
}

void AudioHandler::createPrimaryBuffer() {
	auto primaryBytes = (unsigned int) (primaryBufferSize * slesSamplesBytes);

	DebugMessage(M64MSG_VERBOSE, "Allocating memory for primary audio buffer: %i bytes.",
				 primaryBytes);

	mPrimaryBuffer = new unsigned char[primaryBytes];

	std::memset(mPrimaryBuffer, 0, primaryBytes);
	mPrimaryBufferBytes = primaryBytes;
}

void AudioHandler::createSecondaryBuffers() {
	int secondaryBytes = mSecondaryBufferSize * slesSamplesBytes;

	DebugMessage(M64MSG_VERBOSE, "Allocating memory for %d secondary audio buffers: %i bytes.",
				 secondaryBufferNumber, secondaryBytes);

	/* Allocate number of secondary buffers */
	mSecondaryBuffers = new unsigned char *[secondaryBufferNumber];

	/* Allocate size of each secondary buffers */
	for (int index = 0; index < secondaryBufferNumber; index++) {
		mSecondaryBuffers[index] = new unsigned char[secondaryBytes];
		std::memset(mSecondaryBuffers[index], 0, (size_t) secondaryBytes);
	}
}

void AudioHandler::onInitFailure() {
	DebugMessage(M64MSG_ERROR, "Couldn't open OpenSLES audio");
	closeAudio();
	mCriticalFailure = true;
}

void AudioHandler::initializeAudio(int freq) {

	SLuint32 sample_rate;

	/* Sometimes a bad frequency is requested so ignore it */
	if (freq < 4000)
		return;

	if (mCriticalFailure)
		return;

	/* This is important for the sync */
	mInputFreq = freq;

	if (mSamplingRateSelection == 0) {
		if ((freq / 1000) <= 11) {
			mOutputFreq = 11025;
		} else if ((freq / 1000) <= 22) {
			mOutputFreq = 22050;
		} else if ((freq / 1000) <= 32) {
			mOutputFreq = 32000;
		} else {
			mOutputFreq = 44100;
		}
	} else {
		mOutputFreq = mSamplingRateSelection;
		/*
		 #define SL_SAMPLINGRATE_64		((SLuint32) 64000000)
#define SL_SAMPLINGRATE_88_2	((SLuint32) 88200000)
#define SL_SAMPLINGRATE_96		((SLuint32) 96000000)
#define SL_SAMPLINGRATE_192	((SLuint32) 192000000)
		 */
	}

	sample_rate = mOutputFreq*1000;

	DebugMessage(M64MSG_INFO, "Requesting frequency: %iHz.", mOutputFreq);

	/* Close everything because InitializeAudio can be called more than once */
	closeAudio();

	/* Create primary buffer */
	createPrimaryBuffer();

	/* Create secondary buffers */
	createSecondaryBuffers();

	mState.errors = 0;
	mState.value = secondaryBufferNumber;

	/* Engine object */
	SLresult result = slCreateEngine(&mEngineObject, 0, nullptr, 0, nullptr, nullptr);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	result = (*mEngineObject)->Realize(mEngineObject, SL_BOOLEAN_FALSE);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	result = (*mEngineObject)->GetInterface(mEngineObject, SL_IID_ENGINE, &mEngineEngine);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	/* Output mix object */
	result = (*mEngineEngine)->CreateOutputMix(mEngineEngine, &mOutputMixObject, 0, nullptr, nullptr);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	result = (*mOutputMixObject)->Realize(mOutputMixObject, SL_BOOLEAN_FALSE);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, secondaryBufferNumber};

#ifdef FP_ENABLED

	SLAndroidDataFormat_PCM_EX format_pcm = {SL_ANDROID_DATAFORMAT_PCM_EX, 2, sample_rate,
                   32, 32, SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
                   SL_BYTEORDER_LITTLEENDIAN, SL_ANDROID_PCM_REPRESENTATION_FLOAT};
#else
	SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 2, sample_rate,
								   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
								   (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT),
								   SL_BYTEORDER_LITTLEENDIAN};
#endif

	SLDataSource audioSrc = {&loc_bufq, &format_pcm};

	/* Configure audio sink */
	SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, mOutputMixObject};
	SLDataSink audioSnk = {&loc_outmix, nullptr};

	/* Create audio player */
	const SLInterfaceID ids1[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
	const SLboolean req1[] = {SL_BOOLEAN_TRUE};
	result = (*mEngineEngine)->CreateAudioPlayer(mEngineEngine, &(mPlayerObject), &audioSrc, &audioSnk,
												 1, ids1, req1);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	/* Realize the player */
	result = (*mPlayerObject)->Realize(mPlayerObject, SL_BOOLEAN_FALSE);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	/* Get the play interface */
	result = (*mPlayerObject)->GetInterface(mPlayerObject, SL_IID_PLAY, &(mPlayerPlay));
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	/* Get the buffer queue interface */
	result = (*mPlayerObject)->GetInterface(mPlayerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
											&(mBufferQueue));
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	/* register callback on the buffer queue */
	result = (*mBufferQueue)->RegisterCallback(mBufferQueue, queueCallback, &mState);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	/* set the player's state to playing */
	result = (*mPlayerPlay)->SetPlayState(mPlayerPlay, SL_PLAYSTATE_PLAYING);
	if (result != SL_RESULT_SUCCESS) {
		onInitFailure();
		return;
	}

	mShutdownThread = false;

	if (mTimeStretchEnabled) {
		mAudioConsumerThread = std::thread(audioConsumerStretchEntry, this);
	} else {
		mAudioConsumerThread = std::thread(audioConsumerNoStretchEntry, this);
	}
}

void AudioHandler::pushData(const int16_t* _data, int _samples, std::chrono::duration<double> timeSinceStart) {

	int numValues = _samples*2; // two values per sample since it's stereo

	PoolBufferPointer data = mSoundBufferPool.createPoolBuffer(reinterpret_cast<const char*>(_data), numValues*sizeof(int16_t));

	//Add data to the queue
	QueueData theQueueData;
	theQueueData.data = data;
	theQueueData.samples = _samples;
	theQueueData.timeSinceStart = timeSinceStart.count();

	mAudioConsumerQueue.enqueue(theQueueData);
}

void AudioHandler::audioConsumerStretchEntry(void* audioHandler) {
	reinterpret_cast<AudioHandler*>(audioHandler)->audioConsumerStretch();
}

void AudioHandler::audioConsumerStretch() {
	/*
	static int sequenceLenMS = 63;
	static int seekWindowMS = 16;
	static int overlapMS = 7;*/

	mSoundTouch.setSampleRate((uint) mInputFreq);
	mSoundTouch.setChannels(2);
	mSoundTouch.setSetting(SETTING_USE_QUICKSEEK, 1);
	mSoundTouch.setSetting(SETTING_USE_AA_FILTER, 1);
	//soundTouch.setSetting( SETTING_SEQUENCE_MS, sequenceLenMS );
	//soundTouch.setSetting( SETTING_SEEKWINDOW_MS, seekWindowMS );
	//soundTouch.setSetting( SETTING_OVERLAP_MS, overlapMS );

	mSoundTouch.setRate((double) mInputFreq / (double) mOutputFreq);
	double speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
	mSoundTouch.setTempo(speedFactor);

	double bufferMultiplier = (static_cast<double>(mOutputFreq) / mInputFreq) *
							  (static_cast<double>(defaultSecondaryBufferSize) / mSecondaryBufferSize);

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

	while (!mShutdownThread) {

		if (mBufferQueue == nullptr) {
			return;
		}

		SLAndroidSimpleBufferQueueState slesState;
		(*mBufferQueue)->GetState(mBufferQueue, &slesState);
		int slesQueueLength = slesState.count;

		ranDry = slesQueueLength < minQueueSize;

		QueueData currQueueData;
		if (mAudioConsumerQueue.wait_dequeue_timed(currQueueData, std::chrono::milliseconds(1000))) {

			double temp = averageGameTime / averageFeedTime;

			if (slesState.index < secondaryBufferNumber) {

				speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
				mSoundTouch.setTempo(speedFactor);

				auto shortData = reinterpret_cast<const int16_t*>(mSoundBufferPool.getBufferFromPool(currQueueData.data));
				processAudioSoundTouch(shortData, currQueueData.samples);
				mSoundBufferPool.removeBufferFromPool(currQueueData.data);

			} else {

				//Game is running too fast speed up audio
				if ((slesQueueLength > maxQueueSize || drainQueue) && !ranDry) {
					drainQueue = true;
					currAdjustment = temp +
									 (float) (slesQueueLength - minQueueSize) /
									 (float) (secondaryBufferNumber - minQueueSize) *
									 maxSpeedUpRate;
				}
				//Device can't keep up with the game
				else if (ranDry) {
					drainQueue = false;
					currAdjustment = temp - slowRate;
				//Good case
				} else if (slesQueueLength < maxQueueSize) {
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

				auto shortData = reinterpret_cast<const int16_t*>(mSoundBufferPool.getBufferFromPool(currQueueData.data));
				processAudioSoundTouch(shortData, currQueueData.samples);
				mSoundBufferPool.removeBufferFromPool(currQueueData.data);
			}

			//Useful logging
			/*
			if(slesQueueLength == 0)
			{
			 DebugMessage(M64MSG_ERROR, "sles_length=%d, dry=%d, drain=%d, slow_adj=%f, curr_adj=%f, temp=%f, feed_time=%f, game_time=%f, min_size=%d, max_size=%dd",
			            slesQueueLength, ranDry, drainQueue, slowAdjustment, currAdjustment, temp, averageFeedTime, averageGameTime, minQueueSize, maxQueueSize);
			}
			 */
			//We don't want to calculate the average until we give everything a time to settle.

			//Figure out how much to slow down by
			double timeDiff = currQueueData.timeSinceStart - prevTime;
			prevTime = currQueueData.timeSinceStart;

			// Ignore negative time, it can be nagative if game is falling too far behind real time
			if (timeDiff > 0) {
				feedTimes[feedTimeIndex] = timeDiff;
				averageFeedTime = getAverageTime(feedTimes, feedTimesSet ? feedTimeWindowSize : (feedTimeIndex + 1));

				gameTimes[feedTimeIndex] = static_cast<float>(currQueueData.samples) / mInputFreq;
				averageGameTime = getAverageTime(gameTimes, feedTimesSet ? feedTimeWindowSize : (feedTimeIndex + 1));

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
		}
	}
}

void AudioHandler::audioConsumerNoStretchEntry(void* audioHandler) {
	reinterpret_cast<AudioHandler*>(audioHandler)->audioConsumerNoStretch();
}

void AudioHandler::audioConsumerNoStretch() {

	if (mSamplingType == 0) {
		QueueData currQueueData;

		while (!mShutdownThread)
		{
			if (mAudioConsumerQueue.wait_dequeue_timed(currQueueData, std::chrono::milliseconds(1000))) {
				auto shortData = reinterpret_cast<const int16_t*>(mSoundBufferPool.getBufferFromPool(currQueueData.data));
				processAudioTrivial(shortData, currQueueData.samples);
				mSoundBufferPool.removeBufferFromPool(currQueueData.data);
			}
		}
	} else {
		mSoundTouch.setSampleRate(mInputFreq);
		mSoundTouch.setChannels(2);
		mSoundTouch.setSetting(SETTING_USE_QUICKSEEK, 1);
		mSoundTouch.setSetting(SETTING_USE_AA_FILTER, 1);
		double speedFactor = static_cast<double>(mSpeedFactor) / 100.0;
		mSoundTouch.setTempo(speedFactor);

		mSoundTouch.setRate((double) mInputFreq / (double) mOutputFreq);
		QueueData currQueueData;

		int lastSpeedFactor = mSpeedFactor;

		while (!mShutdownThread)
		{
			if (mAudioConsumerQueue.wait_dequeue_timed(currQueueData, std::chrono::milliseconds(1000))) {

				if (lastSpeedFactor != mSpeedFactor)
				{
					lastSpeedFactor = mSpeedFactor;
					mSoundTouch.setTempo(static_cast<double>(mSpeedFactor) / 100.0);
				}

				auto shortData = reinterpret_cast<const int16_t*>(mSoundBufferPool.getBufferFromPool(currQueueData.data));
				processAudioSoundTouch(shortData, currQueueData.samples);
				mSoundBufferPool.removeBufferFromPool(currQueueData.data);
			}
		}
	}
}

/* This callback handler is called every time a buffer finishes playing */
void AudioHandler::queueCallback(SLAndroidSimpleBufferQueueItf caller, void *context) {
	auto state = (slesState *) context;

	SLAndroidSimpleBufferQueueState st;
	SLresult result = (*caller)->GetState(caller, &st);

	if (result == SL_RESULT_SUCCESS) {
		state->value = secondaryBufferNumber - static_cast<int>(st.count);
	}
}

int AudioHandler::convertBufferToSlesBuffer(const int16_t* inputBuffer, unsigned int inputSamples, unsigned char* outputBuffer, int outputBufferStart)
{
	if (inputSamples*slesSamplesBytes < mPrimaryBufferBytes) {

#ifndef FP_ENABLED
		auto outputBufferType = reinterpret_cast<int16_t*>(outputBuffer);
		int outputStart = outputBufferStart/static_cast<int>(sizeof(int16_t));
		for (int sampleIndex = 0; sampleIndex < inputSamples; ++sampleIndex) {
			int bufferIndex = sampleIndex*2;
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
            int bufferIndex = sampleIndex*2;
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

		outputBufferStart += static_cast<int>(inputSamples)*slesSamplesBytes;
	} else
		DebugMessage(M64MSG_WARNING, "convertBufferToSlesBuffer(): Audio primary buffer overflow.");

	return outputBufferStart;
}

void AudioHandler::processAudioSoundTouch(const int16_t* buffer, unsigned int samples) {

	convertBufferToSlesBuffer(buffer, samples, mPrimaryBuffer, 0);

	mSoundTouch.putSamples(reinterpret_cast<soundtouch::SAMPLETYPE*>(mPrimaryBuffer), samples);

	unsigned int outSamples;
	static int secondaryBufferIndex = 0;

	do {
		outSamples = mSoundTouch.receiveSamples(reinterpret_cast<soundtouch::SAMPLETYPE*>(mSecondaryBuffers[secondaryBufferIndex]),
												static_cast<unsigned int>(mSecondaryBufferSize));

		if (outSamples != 0 && mState.value > 0) {
			SLresult result = (*mBufferQueue)->Enqueue(mBufferQueue,
													   mSecondaryBuffers[secondaryBufferIndex],
													  outSamples * slesSamplesBytes);

			if (result != SL_RESULT_SUCCESS) {
				mState.errors++;
			}

			secondaryBufferIndex = (secondaryBufferIndex + 1)%secondaryBufferNumber;
		}
	} while (outSamples != 0);
}

static int resample(const unsigned char *input, int bytesPerSample, int oldsamplerate, unsigned char *output, int output_needed, int newsamplerate)
{
	int consumedSamples = 0;

	if (newsamplerate >= oldsamplerate)
	{
		int sldf = oldsamplerate;
		int const2 = 2*sldf;
		int dldf = newsamplerate;
		int const1 = const2 - 2*dldf;
		int criteria = const2 - dldf;
		for (int index = 0; index < output_needed / bytesPerSample; ++index)
		{
			std::copy_n(input + consumedSamples * bytesPerSample, bytesPerSample, output + index * bytesPerSample);
			if(criteria >= 0)
			{
				++consumedSamples;
				criteria += const1;
			}
			else criteria += const2;
		}
		return consumedSamples * bytesPerSample; //number of bytes consumed
	}

	// newsamplerate < oldsamplerate, this only happens when mSpeedFactor > 1
	for (int index = 0; index < output_needed/bytesPerSample; ++index)
	{
		consumedSamples = index * oldsamplerate / newsamplerate;
		std::copy_n(input + consumedSamples * bytesPerSample, bytesPerSample, output + index * bytesPerSample);
	}
	return consumedSamples * bytesPerSample; //number of bytes consumed
}


void AudioHandler::processAudioTrivial(const int16_t* buffer, unsigned int samples)
{
	static const int secondaryBufferBytes = mSecondaryBufferSize * slesSamplesBytes;

	static int primaryBufferPos = 0;

	primaryBufferPos = convertBufferToSlesBuffer(buffer, samples, mPrimaryBuffer, primaryBufferPos);

	int newsamplerate = mOutputFreq * 100 / mSpeedFactor;
	int oldsamplerate = mInputFreq;
	static int secondaryBufferIndex = 0;

	while (primaryBufferPos >= ((secondaryBufferBytes * oldsamplerate) / newsamplerate))
	{
		int input_used = resample(mPrimaryBuffer, slesSamplesBytes, oldsamplerate, mSecondaryBuffers[secondaryBufferIndex], secondaryBufferBytes, newsamplerate);
		(*mBufferQueue)->Enqueue(mBufferQueue, mSecondaryBuffers[secondaryBufferIndex], secondaryBufferBytes);

		memmove(mPrimaryBuffer, &mPrimaryBuffer[input_used], primaryBufferPos - input_used);
		primaryBufferPos -= input_used;

		secondaryBufferIndex = (secondaryBufferIndex + 1)%secondaryBufferNumber;
	}
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


void AudioHandler::setLoggingFunction(void* _context, void (*_debugCallback)(void *, int, const char *))
{
	mDebugContext = _context;
	mDebugCallback = _debugCallback;
}

void AudioHandler::setSwapChannels(bool _swapChannels) {
	mSwapChannels = _swapChannels;
}
