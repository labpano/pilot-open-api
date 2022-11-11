package com.pi.pano;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.pi.pano.annotation.PiFileStitchFlag;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

class StitchingRecorder {
    private final String TAG = "StitchingRecorder";

    public enum RecordState {
        RECORD_STATE_RUN,
        RECORD_STATE_END,
        RECORD_STATE_PAUSE,
    }

    private MediaCodec mVideoEncoder;
    //这里用的是自己实现的MediaMuxer,因为底层的MediaMuxer会裁剪掉音频的头1s以音视频对齐
    private MediaMuxer mMediaMuxer;
    private MediaExtractor mAudioExtractor;
    private int mAudioEncoderTrack;
    private int mVideoEncoderTrack;
    private volatile RecordState mRecordState = RecordState.RECORD_STATE_RUN;
    private File mFile;
    private String mDestFilePath;
    //private final Object mLock = new Object();
    private final Semaphore mSemaphore = new Semaphore(1);
    private volatile long mPauseTimeStamp = -1;
    PiPano mPiPano;

    public StitchingRecorder(PiPano piPano) {
        mPiPano = piPano;
    }

    private final Thread mThread = new Thread() {
        private int mFrameCount = 1;
        private final MediaCodec.BufferInfo mEncodeBufferInfo = new MediaCodec.BufferInfo();

        private void encodeOneFrame() {
            while (true) {
                int mOutputIndex = mVideoEncoder.dequeueOutputBuffer(mEncodeBufferInfo, 1_000_000);
                if (mOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    releaseSemaphore();
                    Log.i(TAG, "Encoder INFO_TRY_AGAIN_LATER");
                    break;
                } else if (mOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (mVideoEncoderTrack == -1) {
                        try {
                            MediaFormat format = mVideoEncoder.getOutputFormat();
                            mVideoEncoderTrack = mMediaMuxer.addTrack(format);
                            mMediaMuxer.start();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.i(TAG, "Encoder INFO_OUTPUT_FORMAT_CHANGED");
                } else if (mOutputIndex >= 0) {
                    ByteBuffer buffer = mVideoEncoder.getOutputBuffer(mOutputIndex);
                    if (mPauseTimeStamp == -1 || mEncodeBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        mMediaMuxer.writeSampleData(mVideoEncoderTrack, buffer, mEncodeBufferInfo);
                    }
                    mVideoEncoder.releaseOutputBuffer(mOutputIndex, false);
                    if (mEncodeBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        releaseSemaphore();
                        mFrameCount++;
                    }
                }
            }
        }

        @Override
        public void run() {
            while (mRecordState == RecordState.RECORD_STATE_RUN) {
                encodeOneFrame();
            }
            encodeOneFrame();
            try {
                mVideoEncoder.stop();
                //只有录制完成才加入声音,暂停不加声音
                if (mAudioEncoderTrack != -1) {
                    Log.i(TAG, "Add audio");
                    //只有拼接完成后才会把0.mp4中的音频复制到拼接后的视频中
                    //如果只是暂停,那么随便写一点点东西到mediamuxer中,不然mediamuxer stop会报错
                    if (mRecordState == RecordState.RECORD_STATE_END) {
                        copyByteBuffer(mAudioEncoderTrack, mAudioExtractor);
                    } else {
                        MediaCodec.BufferInfo muxerBufferInfo = new MediaCodec.BufferInfo();
                        muxerBufferInfo.set(0, 1, 1, 0);
                        mMediaMuxer.writeSampleData(mAudioEncoderTrack, ByteBuffer.allocate(16), muxerBufferInfo);
                    }
                }
                mMediaMuxer.release();
                if (mVideoEncoder != null) {
                    mVideoEncoder.release();
                    mVideoEncoder = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //如果是暂停而不是完成,那么要重命名stitching,mp4为stitching_pause.mp4
            //如果是完成状态,那么插入全景信息
            File from = new File(mFile, "stitching.mp4");
            if (mRecordState == RecordState.RECORD_STATE_PAUSE) {
                File to = new File(mFile, "stitching_pause.mp4");
                from.renameTo(to);
            } else {
                File to = new File(mDestFilePath, Utils.getVideoUnstitchFileSimpleName(mFile) + PiFileStitchFlag.stitch + ".mp4");
                if (!to.getParentFile().exists()) {
                    to.getParentFile().mkdirs();
                }
                from.renameTo(to);
                PiPano.spatialMediaImpl(to.getPath(), true, StitchingUtil.mFirmware, StitchingUtil.mArtist, 1.0f);
            }
            Log.i(TAG, "encoder finish");
        }
    };

    private int addTrack(MediaExtractor extractor, String filename, String mime) {
        if (!new File(filename).exists()) {
            return -1;
        }
        try {
            extractor.setDataSource(filename);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        int numTracks = extractor.getTrackCount();
        for (int j = 0; j < numTracks; ++j) {
            MediaFormat format = extractor.getTrackFormat(j);
            if (format.getString(MediaFormat.KEY_MIME).contains(mime)) {
                extractor.selectTrack(j);
                return mMediaMuxer.addTrack(format);
            }
        }
        return -1;
    }

    private long copyByteBuffer(int track, MediaExtractor extractor) {
        long lastTimeStamp = -1;
        MediaCodec.BufferInfo mMuxerBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer byteBuffer = ByteBuffer.allocate(15 * 1024 * 1024);
        do {
            int length = extractor.readSampleData(byteBuffer, 0);
            if (length != -1) {
                lastTimeStamp = extractor.getSampleTime();
                mMuxerBufferInfo.set(0, length, extractor.getSampleTime(), extractor.getSampleFlags());
                mMediaMuxer.writeSampleData(track, byteBuffer, mMuxerBufferInfo);
            }
        }
        while (extractor.advance());
        return lastTimeStamp;
    }

    Surface init(File filename, String destDirPath, int width, int height, int fps, int bitRate, String mime) {
        try {
            mFile = filename;
            mDestFilePath = destDirPath;
            mMediaMuxer = new MediaMuxer(filename + "/stitching.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //添加音频track
            mAudioExtractor = new MediaExtractor();
            mAudioEncoderTrack = addTrack(mAudioExtractor, filename + "/0.mp4", "audio");
            if (mAudioEncoderTrack == -1) {
                Log.e(TAG, "addAudioTrack has no audio");
            }
            if (null == mime) {
                // 使用文件的编码
                mime = MediaFormat.MIMETYPE_VIDEO_AVC;
                Log.e(TAG, "init,mime is null,use default format:video_avc");
            }
            Log.d(TAG, "init,mime:" + mime);
            copyPauseVideo(filename);
            //初始化编码器
            mVideoEncoder = MediaCodec.createEncoderByType(mime);
            MediaFormat videoEncodeFormat = MediaFormat.createVideoFormat(mime, width, height);
            videoEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            if (bitRate == 0) {
                bitRate = width * height / 30 * fps * 5;
            }
            videoEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            videoEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);

            //rc mode 0-RC Off 1-VBR_CFR 2-CBR_CFR 3-VBR_VFR 4-CBR_VFR 5-MBR_CFR 6_MBR_VFR
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-bitrate-mode.value", 1);
            // Real Time Priority
            videoEncodeFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
            // Enable hier-p for resolution greater than 5.7k
            if (width >= 5760 && height >= 2880) {
                videoEncodeFormat.setString(MediaFormat.KEY_TEMPORAL_LAYERING, "android.generic.2");
            }
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-min", 10);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-max", 51);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-min", 10);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-max", 51);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-min", 10);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-max", 51);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-i-enable", 1);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-i", 10);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-b-enable", 1);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-b", 10);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-p-enable", 1);
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-p", 10);

            int interval_iframe = 1;
            videoEncodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, interval_iframe);
            // Calculate P frames based on FPS and I frame Interval
            int pFrameCount = (fps * interval_iframe) - 1;
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-intra-period.n-pframes", pFrameCount);
            // Always set B frames to 0.
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-intra-period.n-bframes", 0);
            videoEncodeFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
            mVideoEncoder.configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return mVideoEncoder.createInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void copyPauseVideo(File filename) {
        //添加之前拼接一半的视频track,如果有的话
        MediaExtractor videoExtractor = new MediaExtractor();
        File pauseFile = new File(mFile, "stitching_pause.mp4");
        mVideoEncoderTrack = addTrack(videoExtractor, filename + "/stitching_pause.mp4", "video");
        if (mVideoEncoderTrack != -1) {
            mMediaMuxer.start();
            mPauseTimeStamp = copyByteBuffer(mVideoEncoderTrack, videoExtractor);
            Log.i(TAG, "Add pause video last time stamp is: " + mPauseTimeStamp);
            videoExtractor.release();
        }
        pauseFile.delete();
    }

    public boolean start() {
        try {
            mVideoEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        mRecordState = RecordState.RECORD_STATE_RUN;
        mThread.start();
        return true;
    }

    void stop(boolean isPause) {
        mRecordState = isPause ? RecordState.RECORD_STATE_PAUSE : RecordState.RECORD_STATE_END;
        try {
            mThread.join();
            Log.i(TAG, "pause 03");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void checkSemaphore() {
        try {
            mSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void releaseSemaphore() {
        mSemaphore.release();
    }

    long getPauseTimeStamp() {
        return mPauseTimeStamp;
    }
}