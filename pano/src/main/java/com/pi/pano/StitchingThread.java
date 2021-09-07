package com.pi.pano;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import java.io.File;

public class StitchingThread extends Thread implements PiPano.PiPanoListener
{
    private final String TAG = "StitchingThread";

    private static final int STITCH_ERROR_DELETE_STITCHED = 0x100;
    private static final int STITCH_ERROR_FRAME_EXTRACTOR = 0x200;
    private static final int STITCH_ERROR_RECORDER_INIT = 0x300;
    private static final int STITCH_ERROR_RECORDER_START = 0x301;

    private final Context mContext;
    private PiPano mPiPano;
    private StitchingUtil.Task mCurrentTask;
    private StitchingRecorder mStitchingRecorder;
    private long mStartTime;
    private final StitchingUtil mStitchingUtil;
    private final StitchingNative mStitchingNative;
    private ImageProcess mImageProcess;

    StitchingThread(Context context, StitchingUtil stitchingUtil)
    {
        mContext = context;
        mStitchingUtil = stitchingUtil;
        mStitchingNative = new StitchingNative();
    }

    private void setErrorCode(int code, String message)
    {
        mCurrentTask.mErrorCode = code;
        Log.e(TAG, "Stitch error[" + Integer.toHexString(code) + "]: " + message);
    }

    private void startTask()
    {
        mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STARTING);
        Log.i(TAG, "processNextTask file: " + mCurrentTask.mFile.getName());
        mCurrentTask.mErrorCode = 0;

        File stitchedVideo = new File(mCurrentTask.mFile + "/stitching.mp4");
        if (stitchedVideo.exists())
        {
            if (!stitchedVideo.delete())
            {
                setErrorCode(STITCH_ERROR_DELETE_STITCHED,"delete stitched file error");
                return;
            }
        }

        mPiPano = new PiPano(this, mContext);
        while (!mPiPano.mHasInit)
        {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        File stabilizationFile = new File(mCurrentTask.mFile.getPath() + "/stabilization");
        mPiPano.useGyroscope(stabilizationFile.exists(), mCurrentTask.mFile.getPath());

        int ret = mStitchingNative.startStitching(mCurrentTask.mFile.getAbsolutePath(), mPiPano);
        if (ret != 0)
        {
            setErrorCode(ret, "frameExtractor init error");
            return;
        }

        ParamReCaliListener paramReCaliListener = new ParamReCaliListener() {
            @Override
            void onError(int error) {
                super.onError(error);
            }
        };
        paramReCaliListener.mExecutionInterval = 10;
        mPiPano.setParamReCaliEnable(paramReCaliListener);

        if (mCurrentTask.mOutputJpgFile == null)
        {
            //encorder
            mStitchingRecorder = new StitchingRecorder(mPiPano);
            Surface surface = mStitchingRecorder.init(mCurrentTask.mFile, mCurrentTask.mWidth,
                    mCurrentTask.mHeight, mCurrentTask.mFps, mCurrentTask.mMime);
            if (surface == null)
            {
                setErrorCode(STITCH_ERROR_RECORDER_INIT, "stitchingRecorder init error");
                return;
            }

            mPiPano.setSurface(surface,1);
            if (!mStitchingRecorder.start())
            {
                setErrorCode(STITCH_ERROR_RECORDER_START, "stitchingRecorder start error");
                return;
            }
        }
        else
        {
            TakePhotoListener takePhotoListener = new TakePhotoListener() {
                @Override
                protected void onTakePhotoComplete(int errorCode) {
                    mImageProcess = null;
                }
            };
            takePhotoListener.mFilename = mCurrentTask.mOutputJpgFile.getName().replace(".jpg","");
            takePhotoListener.mUnStitchDirPath = mCurrentTask.mOutputJpgFile.getParent() + "/";
            mImageProcess = new ImageProcess(mCurrentTask.mWidth,
                    mCurrentTask.mHeight, 0, takePhotoListener);
            mPiPano.setEncodePhotoSurface(mImageProcess.getImageReaderSurface());
        }
    }

    private boolean processNextTask()
    {
        release();

        mCurrentTask = mStitchingUtil.getNextTask();
        if (mCurrentTask == null)
        {
            Log.i(TAG, "processNextTask all files finish or pasue");
            return false;
        }

        if (mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_PAUSING)
        {
            return false;
        }
        startTask();

        if (mCurrentTask.mErrorCode == 0)
        {
            mStartTime = System.currentTimeMillis();
            if (mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_PAUSING)
            {
                release();
                return false;
            }
            mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_START);
            return true;
        }
        else
        {
            mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_ERROR);
            return false;
        }
    }

    private void release()
    {
        Log.i(TAG,"stitching util release");

        if (mStitchingRecorder != null)
        {
            mStitchingRecorder.stop(mCurrentTask.mState == StitchingUtil.StitchState.STITCH_STATE_PAUSING);
            mStitchingRecorder = null;
        }

        mStitchingNative.deleteFrameExtractor();

        if (mCurrentTask != null)
        {
            mCurrentTask.deleteStitchingPauseFile();
            if (mCurrentTask.mState == StitchingUtil.StitchState.STITCH_STATE_STOPING)
            {
                mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STOP);
                mCurrentTask.deleteStitchTask();
            }
            else if (mCurrentTask.mState == StitchingUtil.StitchState.STITCH_STATE_PAUSING)
            {
                mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_PAUSE);
            }
        }

        mCurrentTask = null;
        if (mPiPano != null) {
            mPiPano.release();
            mPiPano = null;
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            mStitchingUtil.checkHaveNextTask();

            if (processNextTask())
            {
                long during = StitchingUtil.getDuring(mCurrentTask.mFile + "/0.mp4");
                long pauseTimeStamp = -1;
                if (mStitchingRecorder != null)
                {
                    pauseTimeStamp = mStitchingRecorder.getPauseTimeStamp();
                }
                else if (mCurrentTask.mOutputJpgFile != null)
                {
                    pauseTimeStamp = mCurrentTask.mStitchPictureInUs;
                }

                if (pauseTimeStamp != -1)
                {
                    mStitchingNative.seekToPreviousKeyFrame(pauseTimeStamp);
                }

                while (mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_START)
                {
                    if (!mStitchingNative.extractorOneFrame())
                    {
                        Log.i(TAG, "Process finish video: " + mCurrentTask.mFile.getName() +
                                " take time: " + (System.currentTimeMillis() - mStartTime));
                        mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STOPING);
                        break;
                    }

                    boolean renderEncodeFrame = mStitchingNative.getExtractorSampleTime(0) > pauseTimeStamp;
                    if (renderEncodeFrame)
                    {
                        if (mStitchingRecorder != null)
                        {
                            mStitchingRecorder.chenckSemaphore();
                        }
                        else if (mCurrentTask.mOutputJpgFile != null)
                        {
                            mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STOPING);
                        }
                    }

                    mStitchingNative.decodeOneFrame(renderEncodeFrame);

                    mCurrentTask.mProgress = mStitchingNative.getExtractorSampleTime(0) * 100.0f / during;
                    if (mCurrentTask.mProgress > 100)
                    {
                        mCurrentTask.mProgress = 100;
                    }

                    if (mStitchingUtil.mStitchingListener != null &&
                            mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_START)
                    {
                        mStitchingUtil.mStitchingListener.onStitchingProgressChange(mCurrentTask);
                    }
                }

                if (mCurrentTask.mOutputJpgFile != null)
                {
                    while (mImageProcess != null) { // wait for take photo complete
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Log.v(TAG,"process next task finally");
                release();
            }
        }

    }

    @Override
    public void onPiPanoInit()
    {
    }

    @Override
    public void onPiPanoChangeCameraResolution(ChangeResolutionListener listener)
    {
    }

    @Override
    public void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync)
    {
        if (mPiPano != null)
        {
            mPiPano.drawLensCorrectionFrame(0x1111,true, timestamp);
        }
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, TakePhotoListener listener)
    {
    }

    @Override
    public void onPiPanoDestroy()
    {
    }

    @Override
    public void onPiPanoEncodeFrame(int count)
    {
    }
}
