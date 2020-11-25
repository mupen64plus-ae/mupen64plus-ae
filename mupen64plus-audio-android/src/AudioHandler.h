#pragma once

#include "m64p_config.h"
#include "RingBufferPool.h"
#include "readerwriterqueue.h"
#include <SoundTouch.h>
#include <oboe/Oboe.h>
#include <thread>

using namespace moodycamel;

class AudioHandler : public oboe::AudioStreamCallback
{
public:

	/**
	 * Get an instance of this class
	 * @return Instance of Audio handler
	 */
	static AudioHandler& get();

	/**
	 * Enable swappping of audio channels
	 * @param _swapChannels True if channels should be swapped
	 */
	void setSwapChannels(bool _swapChannels);

	/**
	 * Set secondary buffer size in samples
	 * @param _secondaryBufferSize Buffer size in samples
	 */
	void setSecondaryBufferSize(int _secondaryBufferSize);

	/**
	 * Enable audio time stretching
	 * @param _timeStretchEnabled True to enble time stretching
	 */
	void setTimeStretchEnabled(int _timeStretchEnabled);

	/**
	 * Changing the sampling type
	 * @param _samplingType 0 for trivial resampling, anything else for soundtouch resampling
	 */
	void setSamplingType(int _samplingType);

	/**
	 * Triger priming bUffer amount in milliseconds
	 * @param _targetSecondaryBuffersMs How many milliseconds to prime the audio hardware
	 */
	void setTargetSecondaryBuffersMs(int _targetSecondaryBuffersMs);

	/**
	 * Set the output sampling rate
	 * @param _samplingRateSelection Output sampling rate, set to zero to use the game provided sampling rate
	 */
	void setSamplingRateSelection(int _samplingRateSelection);

	/**
	 *  Set the speed factor
	 * @param _speedFactor Speed factor, 100= normal speed, 200 equals 2x speed, etc
	 */
	void setSpeedFactor(int _speedFactor);

	/**
	 * Set the function used for logging
	 * @param _context Context name string to used for logging
	 * @param _debugCallback Actual function used for logging
	 */
	void setLoggingFunction(void* _context, void (*_debugCallback)(void *, int, const char *));

	/**
	 * Sets the input data freq and intializes audio processing
	 * @param _freq Input data frequency that will be resampled to the output frequency
	 */
	void initializeAudio(int _freq);

	/**
	 * Stops audio processing
	 */
	void closeAudio();


	/**
	 * Pauses playback
	 */
	void pausePlayback();

	/**
	 * Resume playback
	 */
	void resumePlayback();

	/**
	 * Pushes audio data to be processed
	 * @param _data Input data
	 * @param _samples How many samples the input data contains
	 * @param _timeSinceStart Game time of this data
	 */
	void pushData(const int16_t* _data, int _samples, std::chrono::duration<double> _timeSinceStart);

	/**
	 * Oboe audio callback
	 * @param oboeStream Oboe stream
	 * @param audioData Buffer used to copy data to
	 * @param numFrames How may samples are in this data
	 * @return Playback state
	 */
	oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override;

	// Default start-time size of primary buffer (in equivalent output samples).
	// This is the buffer where audio is loaded after it's extracted from n64's memory.
	static const int primaryBufferSize = 1024*1024;

	// Size of a single secondary buffer, in output samples. This is the requested size of the
	// hardware buffer, this should be a power of two.
	static const int defaultSecondaryBufferSize = 256;

	// This is the requested number of hardware buffers
	static const int secondaryBufferNumber = 200;

	// Number of audio channels
	static const int numberOfChannels = 2;

private:

	// Structure used for queuing data to be played back
	struct QueueData {
		PoolBufferPointer data;
		unsigned int samples;
		double timeSinceStart;
	};

	/*
	 * Default constructor
	 */
	AudioHandler();

	/**
	 * Logs a debug message
	 * @param level Debug level
	 * @param message Debug message
	 * @param ...
	 */
	void DebugMessage(int level, const char *message, ...);

	/**
	 * Processes input samples by using soundtouch library
	 * @param primaryBufferPos Input Buffer position
	 * @param outAudioData Data is written to this buffer
	 * @param outNumFrames How may frames to write
	 * @return True if data was written
	 */
	bool processAudioSoundTouch(int& primaryBufferPos, void *outAudioData, int32_t outNumFrames);

	/**
	* Processes input samples by using soundtouch library without outputting sound, used to build up a buffer
	* @param primaryBufferPos Input Buffer position
	*/
	void processAudioSoundTouchNoOutput(int& primaryBufferPos);

	/**
	 * Performs trivial audio resampling
	 * @param input Input data to sample
	 * @param bytesPerSample How many bytes are in each sample
	 * @param oldsamplerate The input data sampling rate
	 * @param output Where data should be written
	 * @param output_needed How much data should be written if possible
	 * @param newsamplerate The desired sampling rate
	 * @return How many bytes were actually written
	 */
	static int resample(const unsigned char *input, int bytesPerSample, int oldsamplerate, unsigned char *output,
						   int output_needed, int newsamplerate);

	/**
	 * Processes input samples by using trivial resampling
	 * @param primaryBufferPos Input Buffer position
	 * @param outAudioData Data is written to this buffer
	 * @param outNumFrames How may frames to write
	 * @return True if data was written
	 */
	bool processAudioTrivial(int& primaryBufferPos, void *outAudioData, int32_t outNumFrames);

	/**
	 * Performs sound stretching processing, returns true if data was provided, returns true if data was provided
	 * @param outAudioData Where to write data
	 * @param outNumFrames how much data to write
	 * @return True if write was successful
	 */
	bool audioProviderStretch(void *outAudioData, int32_t outNumFrames);

	/**
	 * Performs audio processing with no time stretching, returns true if data was provided
	 * @param outAudioData Where to write data
	 * @param outNumFrames how much data to write
	 * @return True if write was successful
	 */
	bool audioProviderNoStretch(void *audioData, int32_t numFrames);

	/**
	 * Converts a N64 buffer to a hardware buffer
	 * @param inputBuffer Input N64 buffer to convert
	 * @param inputSamples How many samples are contained in the input buffer
	 * @param outputBuffer Output where to write the converted samples
	 * @param outputBufferStart Output start position
	 * @return Offset to where the data ends
	 */
	int convertBufferToHwBuffer(const int16_t* inputBuffer, unsigned int inputSamples, unsigned char* outputBuffer, int outputBufferStart);

	/**
	 * Creates the primary audio buffer
	 */
	void createPrimaryBuffer();

	/**
	 * Creates the secondary audio buffers
	 */
	void createSecondaryBuffer();

	/**
	 * Determines average feed time or game time so that they can be compared to each other
	 * @param feedTimes A set o times
	 * @param numTimes How many times are present in the data
	 * @return The average value
	 */
	static double getAverageTime(const double *feedTimes, int numTimes);

	/**
	 * Injects silence
	 * @param audioData Output buffer where to write silence to
	 * @param numFrames How many samples to write silence in provided bufer
	 */
	static void injectSilence (void *audioData, int32_t numFrames);

#ifdef FP_ENABLED
    static const int hwSamplesBytes = 8;
#else
	static const int hwSamplesBytes = 4;
#endif

	// Debug callback function
	void (*mDebugCallback)(void *, int, const char *) = nullptr;

	// Debug context text
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

    // Number of secondary buffers to target in milliseconds */
	int mTargetSecondaryBuffersMs = 20;
    // Selected samplin rate */
	int mSamplingRateSelection = 0;
    // Output Audio frequency */
	int mOutputFreq = defaultFreq;
	// Audio speed factor (0-100)
	int mSpeedFactor = 100;

	// Queue used to communicate with the audio consumer thread
	BlockingReaderWriterQueue<QueueData> mAudioConsumerQueue;

	// Soundtouch library
	soundtouch::SoundTouch mSoundTouch;

	// Oboe audio stream
	oboe::ManagedStream mOutStream;

	// Memory pool used to store samples until they are processed by the
	// sound processing thread
	RingBufferPool mSoundBufferPool;

	// True if playback is paused
	bool mPlaybackPaused = false;

	// Amount of injected silence to allow the buffers to prime
	int mPrimingTimeMs = 0;

	// True if priming is complete while inejcting silence
	bool mPrimeComplete = false;

	// Primary buffer position
	int mPrimaryBufferPos = 0;
};

