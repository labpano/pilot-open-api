package com.pi.pano;

import android.annotation.SuppressLint;
import android.media.AudioRecord;

import com.pi.pano.ext.IAudioRecordExt;

/**
 * 默认 系统录音 接口实现
 */
public class DefaultAudioRecordExt implements IAudioRecordExt {

    private AudioRecord mAudioRecord;

    @SuppressLint("MissingPermission")
    @Override
    public void configure(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
    }

    @Override
    public void startRecording() {
        mAudioRecord.startRecording();
    }

    @Override
    public void stop() {
        mAudioRecord.stop();
    }

    @Override
    public void release() {
        mAudioRecord.release();
        mAudioRecord = null;
    }

    @Override
    public int read(byte[] pcmData, int offsetInBytes, int sizeInBytes) {
        return mAudioRecord.read(pcmData, offsetInBytes, sizeInBytes);
    }

    @Override
    public int read(float[] pcmData, int offsetInBytes, int sizeInBytes) {
        return mAudioRecord.read(pcmData, offsetInBytes, sizeInBytes, AudioRecord.READ_BLOCKING);
    }
}
