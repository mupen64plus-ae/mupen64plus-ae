#pragma once

#include "m64p_config.h"
#include "BlockingQueue.h"
#include <SoundTouch.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <thread>

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
	void pushData(std::unique_ptr<int16_t[]> _data, int _samples, std::chrono::duration<double> _timeSinceStart);

	// Default start-time size of primary buffer (in equivalent output samples).
	// This is the buffer where audio is loaded after it's extracted from n64's memory.
	static const int primaryBufferSize = 16384;

	// Size of a single secondary buffer, in output samples. This is the requested size of OpenSLES's
	// hardware buffer, this should be a power of two.
	static const int defaultSecondaryBufferSize = 256;

	// This is the requested number of OpenSLES's hardware buffers
	static const int secondaryBufferNumber = 100;

private:

	struct slesState {
		int value;
		int errors;
	};

	struct QueueData {
		std::unique_ptr<int16_t[]> data;
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
	void processAudioSoundTouch(std::unique_ptr<int16_t[]> buffer, unsigned int samples);

	/*
	 * Processes input sample using a trivial resampler
	 */
	void processAudioTrivial(std::unique_ptr<int16_t[]> buffer, unsigned int samples);

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
	 * Call back from SLES audio when a sample has been processed
	 */
	static void queueCallback(SLAndroidSimpleBufferQueueItf caller, void *context);

	/*
	 * Converts a N64 buffer to a SLES buffer
	 */
	int convertBufferToSlesBuffer(std::unique_ptr<int16_t[]> inputBuffer, unsigned int inputSamples, unsigned char* outputBuffer, int outputBufferStart);

	/*
	 * Creates the primary audio buffer
	 */
	void createPrimaryBuffer();

	/*
	 * Creates the secondary audio buffers
	 */
	void createSecondaryBuffers();

	/*
	 * Called if initialization fails
	 */
	void onInitFailure();

	/*
	 * Computes the average time on the provided times
	 */
	static double getAverageTime(const double *feedTimes, int numTimes);

#ifdef FP_ENABLED
    static const int slesSamplesBytes = 8;
#else
	static const int slesSamplesBytes = 4;
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
	unsigned char **mSecondaryBuffers = nullptr;
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
	BlockingQueue<QueueData *> mAudioConsumerQueue;

	// Gets set to true to shut down the audio prcoessing thread
	std::atomic<bool> mShutdownThread;

	// Soundtouch library
	soundtouch::SoundTouch mSoundTouch;

    // SLES state
	slesState mState;

    // Engine interfaces
	SLObjectItf mEngineObject = nullptr;
	SLEngineItf mEngineEngine = nullptr;

    // Output mix interfaces
	SLObjectItf mOutputMixObject = nullptr;

    // Player interfaces
	SLObjectItf mPlayerObject = nullptr;
	SLPlayItf mPlayerPlay = nullptr;
    // Buffer queue interfaces
	SLAndroidSimpleBufferQueueItf mBufferQueue = nullptr;
};

