package com.pi.pano;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.media.MediaMuxer;

import com.pi.pano.annotation.PiFileStitchFlag;
import com.pi.pilot.pano.sdk.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

class StitchingRecorder {
    private final String TAG = "StitchingRecorder";

    private static final String FILE_STITCHING = "stitching.mp4";
    private static final String FILE_STITCHING_PAUSE = "stitching_pause.mp4";

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
    private final Semaphore mSemaphore = new Semaphore(1);
    private volatile long mPauseTimeStamp = -1;

    StitchVideoParams params;
    private long mEncodeTimeoutUs;

    private final Thread mThread = new Thread() {
        private int mFrameCount = 1;
        private final MediaCodec.BufferInfo mEncodeBufferInfo = new MediaCodec.BufferInfo();

        private void encodeOneFrame() {
            while (true) {
                int mOutputIndex = mVideoEncoder.dequeueOutputBuffer(mEncodeBufferInfo, mEncodeTimeoutUs);
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
            mEncodeTimeoutUs = 330_000L;
            if (BuildConfig.DEBUG) {
                mEncodeTimeoutUs = SystemPropertiesProxy.getLong(
                        "persist.dev.stitch_encode_timeout_us", mEncodeTimeoutUs);
            }
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
            File from = new File(params.srcDir, FILE_STITCHING);
            if (mRecordState == RecordState.RECORD_STATE_PAUSE) {
                File to = new File(params.srcDir, FILE_STITCHING_PAUSE);
                from.renameTo(to);
            } else {
                File to = new File(params.targetDir,
                        Utils.getVideoUnstitchFileSimpleName(params.srcDir) + PiFileStitchFlag.stitch + ".mp4");
                if (!to.getParentFile().exists()) {
                    to.getParentFile().mkdirs();
                }
                from.renameTo(to);
                PiPano.spatialMediaImpl(to.getPath(), true, params.spatialAudio,
                        StitchingUtil.mFirmware, StitchingUtil.mArtist, 1.0f);
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

    Surface init(StitchVideoParams params) {
        MediaFormat videoEncodeFormat = null;
        try {
            this.params = params;
            File srcDir = params.srcDir;
            mMediaMuxer = new MediaMuxer(srcDir + File.separator + FILE_STITCHING, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //添加音频track
            mAudioExtractor = new MediaExtractor();
            mAudioEncoderTrack = addTrack(mAudioExtractor, srcDir + "/0.mp4", "audio");
            if (mAudioEncoderTrack == -1) {
                Log.e(TAG, "addAudioTrack has no audio");
            }
            Log.d(TAG, "init,mime:" + params.mime);
            copyPauseVideo(srcDir);
            //初始化编码器
            mVideoEncoder = MediaCodec.createEncoderByType(params.mime);
            videoEncodeFormat = MediaFormat.createVideoFormat(params.mime, params.width, params.height);
            videoEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            if (params.bitrate == 0) {
                params.bitrate = params.width * params.height / 30 * params.fps * 5;
            }
            videoEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, params.bitrate);
            videoEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, params.fps);

            //rc mode 0-RC Off 1-VBR_CFR 2-CBR_CFR 3-VBR_VFR 4-CBR_VFR 5-MBR_CFR 6_MBR_VFR
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-bitrate-mode.value", 1);
            // Real Time Priority
            videoEncodeFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
            // Enable hier-p for resolution greater than 5.7k
            if (params.width >= 5760 && params.height >= 2880) {
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
            int pFrameCount = (params.fps * interval_iframe) - 1;
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-intra-period.n-pframes", pFrameCount);
            // Always set B frames to 0.
            videoEncodeFormat.setInteger("vendor.qti-ext-enc-intra-period.n-bframes", 0);
            videoEncodeFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
//            if (MediaFormat.MIMETYPE_VIDEO_HEVC.equals(params.mime)) {
//                videoEncodeFormat.setInteger("feature-nal-length-bitstream", 0);
//            }
            mVideoEncoder.configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return mVideoEncoder.createInputSurface();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "init videoEncoder params :" + params + ",format:"
                    + videoEncodeFormat + ", error :" + e.getMessage());
        }
        return null;
    }

    private void copyPauseVideo(File filename) {
        //添加之前拼接一半的视频track,如果有的话
        MediaExtractor videoExtractor = new MediaExtractor();
        File pauseFile = new File(params.srcDir, FILE_STITCHING_PAUSE);
        mVideoEncoderTrack = addTrack(videoExtractor, filename + File.separator + FILE_STITCHING_PAUSE, "video");
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
        Log.d(TAG, "stop isPause: " + isPause + ",state:" + mRecordState);
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