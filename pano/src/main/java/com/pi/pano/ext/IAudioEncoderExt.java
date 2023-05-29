package com.pi.pano.ext;

/**
 * Additional processors for audio coding extensions.
 */
public interface IAudioEncoderExt {
    /**
     * configure.
     *
     * @param sampleRateInHz sample rate
     * @param channelCount   channel count
     * @param audioFormat    audio format
     */
    void configure(int sampleRateInHz, int channelCount, int audioFormat);

    /**
     * encoding process.
     *
     * @param inBuffer  Input audio data (pcm)
     * @param length    Input audio data length
     * @param outBuffer Output audio data
     * @return Output audio data length
     */
    int encodeProcess(byte[] inBuffer, int length, byte[] outBuffer);

    /**
     * release resources.
     */
    void release();
}
