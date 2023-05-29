package com.pi.pano;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Thumbnail small video (preview.mp4) generator.
 */
public class VideoThumbGenerator {
    private final static String TAG = VideoThumbGenerator.class.getSimpleName();

    private boolean mIsPano = true;

    private MediaExtractor mSrcMediaExtractor = null;
    private MediaCodec mSrcMediaCodec = null;
    private int mSrcEndFlag = 0;

    private MediaMuxer mDstMediaMuxer;
    private MediaCodec mDstMediaCodec = null;
    private int mDstTrackIndex;
    private int mDstFrameCount = 0;

    private final MediaCodec.BufferInfo mOutputBufferInfo = new MediaCodec.BufferInfo();

    public VideoThumbGenerator() {
    }

    /**
     * Generate thumbnail video
     *
     * @param srcMp4Filepath 源mp4路径
     * @param dstMp4Filepath 生成的mp4路径
     * @return 0：成功
     */
    public int generate(String srcMp4Filepath, String dstMp4Filepath) {
        if (!new File(srcMp4Filepath).isFile()) {
            Log.e(TAG, "generate,srcMp4Filepath:" + srcMp4Filepath + " not file");
            return -1;
        }
        final long totalTimeUs = StitchingUtil.getDuring(srcMp4Filepath);
        // src
        int ret = 0;
        try {

            MediaFormat srcFormat = initSrc(srcMp4Filepath); // src
            Surface surface = initDst(dstMp4Filepath, srcFormat); // dst
            mSrcMediaCodec.configure(srcFormat, surface, null, 0);
            mSrcMediaCodec.start();
            mDstMediaCodec.start();
            // do
            long interval = 1000_000;
            if (totalTimeUs > 20_000_000) {
                interval = totalTimeUs / 19;
            }
            transferVideoFrame(mSrcMediaExtractor, mSrcMediaCodec, totalTimeUs,
                    mDstMediaMuxer, mDstMediaCodec, interval);
        } catch (Exception ex) {
            ret = -1;
            Log.e(TAG, "generate,srcMp4Filepath:" + srcMp4Filepath + ",ex:" + ex);
            ex.printStackTrace();
        } finally {
            if (null != mSrcMediaCodec) {
                mSrcMediaCodec.release();
                mSrcMediaCodec = null;
            }
            if (null != mSrcMediaExtractor) {
                mSrcMediaExtractor.release();
                mSrcMediaExtractor = null;
            }
            if (null != mDstMediaCodec) {
                mDstMediaCodec.release();
                mDstMediaCodec = null;
            }
            if (null != mDstMediaMuxer) {
                mDstMediaMuxer.release();
                mDstMediaMuxer = null;
            }
        }
        File dstFile = new File(dstMp4Filepath);
        if (dstFile.exists()) {
            if (dstFile.length() > 0 && ret == 0) {
                PiPano.spatialMediaFromOldMp4(srcMp4Filepath, dstMp4Filepath, false);
            } else {
                dstFile.delete();
                ret = -2;
                Log.e(TAG, "generate,dstMp4Filepath is invalid,srcMp4Filepath:" + srcMp4Filepath);
            }
        }
        Log.d(TAG, "generate,srcMp4Filepath:" + srcMp4Filepath + ",totalTimeUs:" + totalTimeUs
                + ",dstMp4Filepath:" + dstMp4Filepath + ",dstFrameCount:" + mDstFrameCount);
        return ret;
    }

    @NonNull
    private MediaFormat initSrc(String srcMp4Filepath) throws IOException {
        mSrcMediaExtractor = new MediaExtractor();
        mSrcMediaExtractor.setDataSource(srcMp4Filepath);
        MediaFormat srcFormat = null;
        for (int trackIndex = 0, numTracks = mSrcMediaExtractor.getTrackCount(); trackIndex < numTracks; trackIndex++) {
            MediaFormat format = mSrcMediaExtractor.getTrackFormat(trackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                srcFormat = format;
                mSrcMediaExtractor.selectTrack(trackIndex);
                break;
            }
        }
        if (null == srcFormat) {
            throw new RuntimeException("No video track found in " + srcMp4Filepath);
        }
        final String srcMime = srcFormat.getString(MediaFormat.KEY_MIME);
        mSrcMediaCodec = MediaCodec.createDecoderByType(srcMime);

        int srcWidth = srcFormat.getInteger(MediaFormat.KEY_WIDTH);
        int srcHeight = srcFormat.getInteger(MediaFormat.KEY_HEIGHT);
        mIsPano = (srcWidth / srcHeight == 2); // 宽高比2：1判断为全景
        return srcFormat;
    }

    @NonNull
    private Surface initDst(String destMp4Filepath, MediaFormat srcFormat) throws IOException {
        final String dstMime = MediaFormat.MIMETYPE_VIDEO_AVC;
        final int[] destSize;
        {
            int srcWidth = srcFormat.getInteger(MediaFormat.KEY_WIDTH);
            int srcHeight = srcFormat.getInteger(MediaFormat.KEY_HEIGHT);
//            destSize = SurfaceSizeUtils.calculateSurfaceSize(640, 320, srcWidth, srcHeight);
            destSize = new int[]{srcWidth, srcHeight};
        }
        int dstWidth = destSize[0];
        int dstHeight = destSize[1];
        int fps = Math.min(20, srcFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
        MediaFormat dstFormat = MediaFormat.createVideoFormat(dstMime, dstWidth, dstHeight);
        dstFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        dstFormat.setInteger(MediaFormat.KEY_BIT_RATE, Math.min(640 * 320 * 2, dstWidth * dstHeight * 2));
        initVideoFormat(dstFormat, dstWidth, dstHeight);
        mDstMediaCodec = MediaCodec.createEncoderByType(dstMime);
        mDstMediaCodec.configure(dstFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mDstMediaMuxer = new MediaMuxer(destMp4Filepath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        return mDstMediaCodec.createInputSurface();
    }

    private void transferVideoFrame(MediaExtractor srcMediaExtractor, MediaCodec srcMediaCodec, long totalTimeUs,
                                    MediaMuxer dstMediaMuxer, MediaCodec dstMediaCodec, long interval) {
        long nextSeekTimeUs = 0;
        int delayExit = 0;
        while (true) {
            boolean src = extract(srcMediaExtractor, srcMediaCodec, nextSeekTimeUs);
            if (src) {
                mDstFrameCount++;
                nextSeekTimeUs = nextSeekTimeUs + interval;
                if (totalTimeUs > 0 && nextSeekTimeUs > totalTimeUs) {
                    nextSeekTimeUs = -1;
                }
            }
            muxer(dstMediaMuxer, dstMediaCodec);
            if (mSrcEndFlag < 0) {
                delayExit++;
                if (delayExit > 10) {
                    break;
                }
            }
        }
        Log.d(TAG, "transferVideoFrame,frameCount:" + mDstFrameCount);
    }

    private boolean extract(MediaExtractor srcMediaExtractor, MediaCodec srcMediaCodec, long nextSeekTimeUs) {
        if (mSrcEndFlag < 0) {
            return false;
        }
        boolean ret = false;
        //
        if (mSrcEndFlag == 0) {
            int inputIndex = srcMediaCodec.dequeueInputBuffer(1000);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = srcMediaCodec.getInputBuffer(inputIndex);
                int sampleSize = 0;
                //Log.d(TAG, "extract,nextSeekTimeUs:" + nextSeekTimeUs);
                if (nextSeekTimeUs == 0) {
                    sampleSize = srcMediaExtractor.readSampleData(inputBuffer, 0);
                    //srcMediaExtractor.advance();
                } else if (nextSeekTimeUs > 0) {
                    srcMediaExtractor.seekTo(nextSeekTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    sampleSize = srcMediaExtractor.readSampleData(inputBuffer, 0);
                    //srcMediaExtractor.advance();
                }
                if (sampleSize > 0) {
                    srcMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, srcMediaExtractor.getSampleTime(), srcMediaExtractor.getSampleFlags());
                    //Log.d(TAG, "extract,inputIndex:" + inputIndex + ",getSampleTime:" + srcMediaExtractor.getSampleTime());
                    ret = true;
                } else {
                    srcMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    //Log.d(TAG, "extract,inputIndex:" + inputIndex + " end");
                    mSrcEndFlag = 1;
                }
            } else if (inputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //Log.d(TAG, "extract,inputIndex:" + inputIndex + " again_later");
            }
        }
        //
        int outputIndex = srcMediaCodec.dequeueOutputBuffer(mOutputBufferInfo, 1000);
        if (outputIndex > 0) {
            if ((mOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mSrcEndFlag = -1;
                //Log.d(TAG, "extract,outputIndex:" + outputIndex + " end");
            } else {
                //Log.d(TAG, "extract,outputIndex:" + outputIndex + ",presentationTimeUs:" + mOutputBufferInfo.presentationTimeUs);
            }
            srcMediaCodec.releaseOutputBuffer(outputIndex, true);
        } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //Log.d(TAG, "extract,outputIndex:" + outputIndex + " again_later");
        }
        return ret;
    }

    private int muxer(MediaMuxer dstMediaMuxer, MediaCodec dstMediaCodec) {
        int outputIndex = dstMediaCodec.dequeueOutputBuffer(mOutputBufferInfo, 1000);
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat format = dstMediaCodec.getOutputFormat();
            mDstTrackIndex = dstMediaMuxer.addTrack(format);
            dstMediaMuxer.start();
            return outputIndex;
        }
        if (outputIndex > 0) {
            if ((mOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                //Log.d(TAG, "muxer,outputIndex:" + outputIndex + " end");
            } else {
                ByteBuffer buffer = dstMediaCodec.getOutputBuffer(outputIndex);
                dstMediaMuxer.writeSampleData(mDstTrackIndex, buffer, mOutputBufferInfo);
                //Log.d(TAG, "muxer,outputIndex:" + outputIndex + ",presentationTimeUs:" + mOutputBufferInfo.presentationTimeUs);
            }
            dstMediaCodec.releaseOutputBuffer(outputIndex, false);
        } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //Log.d(TAG, "muxer,outputIndex:" + outputIndex + " again_later");
        }
        return outputIndex;
    }

    public static void initVideoFormat(MediaFormat videoFormat, int width, int height) {
        //视频编码器
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //rc mode 0-RC Off 1-VBR_CFR 2-CBR_CFR 3-VBR_VFR 4-CBR_VFR 5-MBR_CFR 6_MBR_VFR
        videoFormat.setInteger("vendor.qti-ext-enc-bitrate-mode.value", 1);
        // Real Time Priority
        videoFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        // Enable hier-p for resolution greater than 5.7k
        if (width >= 5760 && height >= 2880) {
            videoFormat.setString(MediaFormat.KEY_TEMPORAL_LAYERING, "android.generic.2");
        }
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-min", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-max", 51);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-min", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-max", 51);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-min", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-max", 51);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-i-enable", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-i", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-b-enable", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-b", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-p-enable", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-p", 10);

        int interval_iframe = 1;
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, interval_iframe);
        videoFormat.setInteger("vendor.qti-ext-enc-intra-period.n-pframes", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-intra-period.n-bframes", 0);
        videoFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
    }
}