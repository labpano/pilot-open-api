package com.pi.pano;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

class StitchingRecorder
{
    private final String TAG = "StitchingRecorder";

    public enum RecordState
    {
        RECORD_STATE_RUN,
        RECORD_STATE_END,
        RECORD_STATE_PAUSE,
    }

    private MediaCodec mVideoEncoder;
    private MediaMuxer mMediaMuxer;
    private MediaExtractor mAudioExtractor;
    private int mAudioEncoderTrack;
    private int mVideoEncoderTrack;
    private volatile RecordState mRecordState = RecordState.RECORD_STATE_RUN;
    private File mFile;
    private final Semaphore mSemaphore = new Semaphore(1);
    private volatile long mPauseTimeStamp = -1;
    PiPano mPiPano;

    public StitchingRecorder(PiPano piPano)
    {
        mPiPano = piPano;
    }

    private final Thread mThread = new Thread()
    {
        private int mFrameCount = 1;
        private final MediaCodec.BufferInfo mEncodeBufferInfo = new MediaCodec.BufferInfo();

        private void encodeOneFrame()
        {
            while (true)
            {
                int mOutputIndex = mVideoEncoder.dequeueOutputBuffer(mEncodeBufferInfo, 500000);
                if (mOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                {
                    Log.i(TAG, "Encoder INFO_TRY_AGAIN_LATER");
                    break;
                }
                else if (mOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                {
                    if (mVideoEncoderTrack == -1)
                    {
                        try
                        {
                            MediaFormat format = mVideoEncoder.getOutputFormat();
                            mVideoEncoderTrack = mMediaMuxer.addTrack(format);
                            mMediaMuxer.start();
                        }
                        catch (IllegalStateException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    Log.i(TAG, "Encoder INFO_OUTPUT_FORMAT_CHANGED");
                }
                else if (mOutputIndex >= 0)
                {
                    ByteBuffer buffer = mVideoEncoder.getOutputBuffer(mOutputIndex);
                    if (mPauseTimeStamp == -1 || mEncodeBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    {
                        mMediaMuxer.writeSampleData(mVideoEncoderTrack, buffer, mEncodeBufferInfo);
                    }
                    mVideoEncoder.releaseOutputBuffer(mOutputIndex, false);
                    if (mEncodeBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    {
                        releaseSemaphore();
                        mFrameCount++;
                    }
                }
            }
        }

        @Override
        public void run()
        {
            while(mRecordState == RecordState.RECORD_STATE_RUN)
            {
                encodeOneFrame();
            }
            encodeOneFrame();

            try
            {
                mVideoEncoder.stop();

                if (mAudioEncoderTrack != -1) {
                    Log.i(TAG, "Add audio");
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
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            File from =new File(mFile,"stitching.mp4") ;
            if (mRecordState == RecordState.RECORD_STATE_PAUSE)
            {
                File to=new File(mFile,"stitching_pause.mp4") ;
                from.renameTo(to);
            } else {
                File to = new File(mFile.getParentFile().getParentFile().getAbsolutePath()
                        + File.separator + "Stitched", getFilePrefix(mFile.getName()) + ".mp4");
                from.renameTo(to);
                if (mPiPano != null)
                {
                    mPiPano.spatialMediaImpl(to.getPath(),false, StitchingUtil.mFirmware);
                }
            }

            Log.i(TAG,"encoder finish");
        }
    };

    private String getFilePrefix(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private int addTrack(MediaExtractor extractor, String filename,String mime)
    {
        if (!new File(filename).exists())
        {
            return -1;
        }

        try
        {
            extractor.setDataSource(filename);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return -1;
        }

        int numTracks = extractor.getTrackCount();
        for (int j = 0; j < numTracks; ++j)
        {
            MediaFormat format = extractor.getTrackFormat(j);
            if (format.getString(MediaFormat.KEY_MIME).contains(mime))
            {
                extractor.selectTrack(j);
                return mMediaMuxer.addTrack(format);
            }
        }
        return -1;
    }

    private long copyByteBuffer(int track, MediaExtractor extractor)
    {
        long lastTimeStamp = -1;
        MediaCodec.BufferInfo mMuxerBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer byteBuffer = ByteBuffer.allocate(15 * 1024 * 1024);
        do
        {
            int length = extractor.readSampleData(byteBuffer, 0);
            if (length != -1)
            {
                lastTimeStamp = extractor.getSampleTime();
                mMuxerBufferInfo.set(0, length, extractor.getSampleTime(), extractor.getSampleFlags());
                mMediaMuxer.writeSampleData(track, byteBuffer, mMuxerBufferInfo);
            }
        }
        while (extractor.advance());

        return lastTimeStamp;
    }

    Surface init(File filename, int width, int height, int fps, String mime)
    {
        try
        {
            mFile = filename;
            mMediaMuxer = new MediaMuxer(filename + "/stitching.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mAudioExtractor = new MediaExtractor();
            mAudioEncoderTrack = addTrack(mAudioExtractor,filename + "/0.mp4","audio");
            if (mAudioEncoderTrack == -1)
            {
                Log.e(TAG,"addAudioTrack has no audio");
            }
            if (null == mime)
            {
                mime = MediaFormat.MIMETYPE_VIDEO_AVC;
                Log.e(TAG, "init,mime is null,use default format:video_avc");
            }
            Log.d(TAG, "init,mime:" + mime);

            copyPauseVideo(filename);

            mVideoEncoder = MediaCodec.createEncoderByType(mime);
            MediaFormat videoEncodeFormat=MediaFormat.createVideoFormat(mime,width,height);
            videoEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE,width*height/30*fps*5);
            videoEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            videoEncodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mVideoEncoder.configure(videoEncodeFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

            return mVideoEncoder.createInputSurface();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private void copyPauseVideo(File filename)
    {
        MediaExtractor videoExtractor = new MediaExtractor();
        File pauseFile = new File(mFile,"stitching_pause.mp4") ;
        mVideoEncoderTrack = addTrack(videoExtractor, filename + "/stitching_pause.mp4","video");
        if (mVideoEncoderTrack != -1)
        {
            mMediaMuxer.start();

            mPauseTimeStamp = copyByteBuffer(mVideoEncoderTrack, videoExtractor);
            Log.i(TAG, "Add pause video last time stamp is: " + mPauseTimeStamp);
            videoExtractor.release();
        }
        pauseFile.delete();
    }

    public boolean start()
    {
        try
        {
            mVideoEncoder.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

        mRecordState = RecordState.RECORD_STATE_RUN;
        mThread.start();

        return true;
    }

    void stop(boolean isPause)
    {
        mRecordState = isPause ? RecordState.RECORD_STATE_PAUSE : RecordState.RECORD_STATE_END;
        try
        {
            mThread.join();
            Log.i(TAG,"pause 03");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    void chenckSemaphore()
    {
        try
        {
            mSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    void releaseSemaphore()
    {
        mSemaphore.release();
    }

    long getPauseTimeStamp()
    {
        return mPauseTimeStamp;
    }
}