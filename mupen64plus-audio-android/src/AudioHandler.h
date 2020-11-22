#pragma once

#include "m64p_config.h"
#include "RingBufferPool.h"
#include "readerwriterqueue.h"
#include <SoundTouch.h>
#include <oboe/Oboe.h>
#include <thread>


using namespace moodycamel;

class AudioHandler
{
public:

	/*
	 * Get an instance of this class
	 */
	static AudioHandler& get();

	/*
	 * Enable swappping of audio channels
	 */
	void setSwapChannels(bool _swapChannels);

	/*
	 * Set secondary buffer size
	 */
	void setSecondaryBufferSize(int _secondaryBufferSize);

	/*
	 *  Enable audio time stretching
	 */
	void setTimeStretchEnabled(int _timeStretchEnabled);

	/*
	 * Changing the sampling type
	 */
	void setSamplingType(int _samplingType);

	/*
	 * Number of secondary buffers
	 */
	void setTargetSecondaryBuffers(int _targetSecondaryBuffers);

	/*
	 * Set the output sampling rate
	 */
	void setSamplingRateSelection(int _samplingRateSelection);

	/*
	 * Set the speed factor
	 */
	void setSpeedFactor(int _speedFactor);

	/*
	 * Set the function used for logging
	 */
	void setLoggingFunction(void* _context, void (*_debugCallback)(void *, int, const char *));

	/*
	 * Sets the input data freq and intializes audio processing
	 */
	void initializeAudio(int _freq);

	/*
	 * Stops audio processing
	 */
	void closeAudio();

	/*
	 * Returns true if a critical failure has occurred
	 */
	bool isCriticalFailure() const;

	/*
	 * Pushes audio data to be processed
	 */
	void pushData(const int16_t* _data, int _samples, std::chrono::duration<double> _timeSinceStart);

	// Default start-time size of primary buffer (in equivalent output samples).
	// This is the buffer where audio is loaded after it's extracted from n64's memory.
	static const int primaryBufferSize = 16384;

	// Size of a single secondary buffer, in output samples. This is the requested size of the
	// hardware buffer, this should be a power of two.
	static const int defaultSecondaryBufferSize = 256;

	// This is the requested number of hardware buffers
	static const int secondaryBufferNumber = 10;

	// Number of audio channels
	static const int numberOfChannels = 2;

private:

	struct HardwareState {
		int errors;
	};

	struct QueueData {
		PoolBufferPointer data;
		unsigned int samples;
		double timeSinceStart;
	};

	/*
	 * Default constructor
	 */
	AudioHandler();

	/*
	 * Logs a debug message
	 */
	void DebugMessage(int level, const char *message, ...);

	/*
	 * Processes input samples by using soundtouch library
	 */
	void processAudioSoundTouch(const int16_t* buffer, unsigned int samples);

	/*
	 * Processes input sample using a trivial resampler
	 */
	void processAudioTrivial(const int16_t* buffer, unsigned int samples);

	/*
	 * Thread entry point for sound stretching processing
	 */
	static void audioConsumerStretchEntry(void* audioHandler);

	/*
	 * Performs sound stretching processing
	 */
	void audioConsumerStretch();

	/*
	 * Thread entry point for no sound streteching processing
	 */
	static void audioConsumerNoStretchEntry(void* audioHandler);

	/**
	 * Performs audio processing with no time stretching
	 */
	void audioConsumerNoStretch();

	/*
	 * Converts a N64 buffer to a hardware buffer
	 */
	int convertBufferToHwBuffer(const int16_t* inputBuffer, unsigned int inputSamples, unsigned char* outputBuffer, int outputBufferStart);

	/*
	 * Creates the primary audio buffer
	 */
	void createPrimaryBuffer();

	/*
	 * Creates the secondary audio buffers
	 */
	void createSecondaryBuffer();

	/*
	 * Computes the average time on the provided times
	 */
	static double getAverageTime(const double *feedTimes, int numTimes);

	/**
	 * Pauses playback
	 */
	void pausePlayback();

	/**
	 * Resume playback
	 */
	void resumePlayback();

#ifdef FP_ENABLED
    static const int hwSamplesBytes = 8;
#else
	static const int hwSamplesBytes = 4;
#endif

	void (*mDebugCallback)(void *, int, const char *) = nullptr;
	void *mDebugContext = nullptr;

	//Default frequency for input and output
	static const int defaultFreq = 33600;
    // Pointer to the primary audio buffer
	unsigned char *mPrimaryBuffer = nullptr;
	// Size of the primary buffer */
	int mPrimaryBufferBytes = 0;
	// Pointer to secondary buffers */
	unsigned char *mSecondaryBuffer = nullptr;
	// Size of a single secondary audio buffer in output samples */
	int mSecondaryBufferSize = defaultSecondaryBufferSize;
	// Time stretched audio enabled */
	bool mTimeStretchEnabled = true;
	// Sampling type 0=trivial 1=Soundtouch*/
	int mSamplingType = 0;
	// True to swap left/right channels
	bool mSwapChannels = false;
	// Frequency of provided data
	int mInputFreq = defaultFreq;

    // Number of secondary buffers to target */
	int mTargetSecondaryBuffers = 20;
    // Selected samplin rate */
	int mSamplingRateSelection = 0;
    // Output Audio frequency */
	int mOutputFreq = defaultFreq;
	// Audio speed factor (0-100)
	int mSpeedFactor = 100;

	//  Indicate that the audio plugin failed to initialize, so the emulator can keep running without sound */
	bool mCriticalFailure = 0;

	// Audio consumer thread, audio processing is done in this thread
	std::thread mAudioConsumerThread;

	// Queue used to communicate with the audio consumer thread
	BlockingReaderWriterQueue<QueueData> mAudioConsumerQueue;

	// Gets set to true to shut down the audio prcoessing thread
	std::atomic<bool> mShutdownThread;

	// Soundtouch library
	soundtouch::SoundTouch mSoundTouch;

    // Hardware state
	HardwareState mState;

	// Oboe audio stream
	oboe::ManagedStream mOutStream;

	// Memory pool used to store samples until they are processed by the
	// sound processing thread
	RingBufferPool mSoundBufferPool;

	// True if playback is paused
	bool mPlaybackPaused = false;
};

