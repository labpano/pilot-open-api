package com.pi.pano.ext;

import android.media.AudioFormat;
import android.media.MediaRecorder;

/**
 * recording interface,provide audio data collection.
 */
public interface IAudioRecordExt {

    /**
     * configure
     *
     * @param audioSource       audio source, {@link MediaRecorder.AudioSource }
     * @param sampleRateInHz    sample rate
     * @param channelConfig     channel config,
     *                          {@link AudioFormat#CHANNEL_IN_MONO},{@link AudioFormat#CHANNEL_IN_STEREO},
     *                          {@link AudioFormat#CHANNEL_IN_STEREO}|{@link AudioFormat#CHANNEL_IN_FRONT}|{{@link AudioFormat#CHANNEL_IN_BACK}}
     * @param audioFormat       audio format
     *                          {@link AudioFormat#ENCODING_PCM_16BIT}, {@link AudioFormat#ENCODING_PCM_FLOAT}.
     * @param bufferSizeInBytes buffer size
     */
    void configure(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes);

    /**
     * start record.
     */
    void startRecording();

    /**
     * stop record.
     */
    void stop();

    /**
     * release resources.
     */
    void release();

    /**
     * Synchronous reading of recorded data.
     *
     * @param pcmData       output pcm data.
     * @param offsetInBytes offset
     * @param sizeInBytes   size
     * @return read size,less than zero is fail.
     */
    int read(byte[] pcmData, int offsetInBytes, int sizeInBytes);

    /**
     * Synchronous reading of recorded data.
     *
     * @param pcmData       output pcm data.
     * @param offsetInBytes offset
     * @param sizeInBytes   size
     * @return read size,less than zero is fail.
     */
    int read(float[] pcmData, int offsetInBytes, int sizeInBytes);
}
