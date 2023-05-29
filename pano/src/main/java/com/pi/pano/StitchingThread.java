package com.pi.pano;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;

public class StitchingThread extends Thread implements PiPano.PiPanoListener {
    private final String TAG = "StitchingThread";

    /**
     * 删除之前已拼接文件错误
     */
    private static final int STITCH_ERROR_DELETE_STITCHED = 0x100;

    /**
     * FrameExtractor错误
     */
    private static final int STITCH_ERROR_FRAME_EXTRACTOR = 0x200;

    /**
     * StitchingRecorder int错误
     */
    private static final int STITCH_ERROR_RECORDER_INIT = 0x300;

    /**
     * StitchingRecorder start错误
     */
    private static final int STITCH_ERROR_RECORDER_START = 0x301;

    private final Context mContext;
    private PiPano mPiPano;
    private StitchingUtil.Task mCurrentTask;
    private StitchingRecorder mStitchingRecorder;
    private long mStartTime;
    private final StitchingUtil mStitchingUtil;
    private final StitchingNative mStitchingNative;

    StitchingThread(Context context, StitchingUtil stitchingUtil) {
        mContext = context;
        mStitchingUtil = stitchingUtil;
        mStitchingNative = new StitchingNative();
    }

    private void setErrorCode(int code, String message) {
        mCurrentTask.mErrorCode = code;
        Log.e(TAG, "Stitch error[" + Integer.toHexString(code) + "]:" + message);
    }

    private void startTask() {
        mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STARTING);
        Log.i(TAG, "processNextTask file: " + mCurrentTask.params.srcDir.getName());
        mCurrentTask.mErrorCode = 0;

        File stitchedVideo = new File(mCurrentTask.params.srcDir + "/stitching.mp4");
        //删掉文件夹中原来的stitching.mp4
        if (stitchedVideo.exists()) {
            if (!stitchedVideo.delete()) {
                setErrorCode(STITCH_ERROR_DELETE_STITCHED, "delete stitched file error");
                return;
            }
        }

        mPiPano = new OpenGLThread(this, mContext, false);
        while (!mPiPano.mHasInit) {
            SystemClock.sleep(5);
        }
        // 保护镜
        mPiPano.setLensProtected(mCurrentTask.params.lensProtected);
        File stabilizationFile = new File(mCurrentTask.params.srcDir.getPath() + "/stabilization");
        mPiPano.useGyroscope(stabilizationFile.exists(), mCurrentTask.params.srcDir.getPath());
        //setStitchingConfig();
        int ret = mStitchingNative.startStitching(mCurrentTask.params.srcDir.getAbsolutePath(), mPiPano);
        if (ret != 0) {
            setErrorCode(ret, "frameExtractor init error");
            return;
        }
        mPiPano.setParamReCaliEnable(3, false);
        //encorder
        mStitchingRecorder = new StitchingRecorder();
        Surface surface = mStitchingRecorder.init(mCurrentTask.params);
        if (surface == null) {
            setErrorCode(STITCH_ERROR_RECORDER_INIT, "stitchingRecorder init error");
            return;
        }
        mPiPano.setEncodeSurface(surface, 1);
        if (!mStitchingRecorder.start()) {
            setErrorCode(STITCH_ERROR_RECORDER_START, "stitchingRecorder start error");
        }
    }


    private boolean processNextTask() {
        release();
        mCurrentTask = mStitchingUtil.getNextTask();
        if (mCurrentTask == null) {
            Log.i(TAG, "processNextTask all files finish or pause");
            return false;
        }
        if (mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_PAUSING) {
            return false;
        }
        startTask();
        if (mCurrentTask.mErrorCode == 0) {
            mStartTime = System.currentTimeMillis();
            if (mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_PAUSING) {
                release();
                return false;
            }
            mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_START);
            return true;
        } else {
            mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_ERROR);
            return false;
        }
    }

    private void release() {
        Log.i(TAG, "stitching util release");
        if (mStitchingRecorder != null) {
            mStitchingRecorder.stop(mCurrentTask.mState == StitchingUtil.StitchState.STITCH_STATE_PAUSING);
            mStitchingRecorder = null;
        }
        mStitchingNative.deleteFrameExtractor();
        if (mCurrentTask != null) {
            mCurrentTask.deleteStitchingPauseFile();
            if (mCurrentTask.mState == StitchingUtil.StitchState.STITCH_STATE_STOPPING) {
                mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STOP);
                mCurrentTask.deleteStitchTask();
            } else if (mCurrentTask.mState == StitchingUtil.StitchState.STITCH_STATE_PAUSING) {
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
    public void run() {
        while (true) {
            mStitchingUtil.checkHaveNextTask();
            if (processNextTask()) {
                long during = StitchingUtil.getDuring(mCurrentTask.params.srcDir + "/0.mp4");
                //计算上次暂停的时间,如果是拼接视频,那么直接计算上次暂停的时间
                //如果是拼接视频中某一帧的图像,这个暂停时间指的是该图像在视频中的时间戳
                long pauseTimeStamp = -1;
                if (mStitchingRecorder != null) {
                    pauseTimeStamp = mStitchingRecorder.getPauseTimeStamp();
                }
                Log.d(TAG, "nextTask check dur:" + during + "pauseTime:" + pauseTimeStamp);
                //如果时暂停重新开始,那么StitchingFrameExtractor要seek到上次停止的时间戳的前一个keyframe
                if (pauseTimeStamp != -1) {
                    mStitchingNative.seekToPreviousKeyFrame(pauseTimeStamp);
                }
                while (mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_START) {
                    if (!mStitchingNative.extractorOneFrame()) {
                        //拼接完成
                        Log.i(TAG, "Process finish video: " + mCurrentTask.params.srcDir.getName() +
                                " take time: " + (System.currentTimeMillis() - mStartTime));
                        mCurrentTask.changeState(StitchingUtil.StitchState.STITCH_STATE_STOPPING);
                        break;
                    }
                    //只有当前的时间戳大于上次暂停的时间戳,这一帧才会被编码
                    boolean renderEncodeFrame = mStitchingNative.getExtractorSampleTime(0) > pauseTimeStamp;
                    if (renderEncodeFrame) {
                        //检查编码器的信号量,只有编码器输入buffer够的时候,才可以releaseOutputBuffer
                        //一帧surface到编码器,不然会丢帧
                        if (mStitchingRecorder != null) {
                            mStitchingRecorder.checkSemaphore();
                        }
                    }
                    mStitchingNative.decodeOneFrame(renderEncodeFrame);
                    float pro = mStitchingNative.getExtractorSampleTime(0) * 100.0f / during;
                    if (pro > mCurrentTask.mProgress) {
                        mCurrentTask.mProgress = pro;
                    }
                    if (mCurrentTask.mProgress > 100) {
                        mCurrentTask.mProgress = 100;
                    }
                    if (mStitchingUtil.mStitchingListener != null &&
                            mCurrentTask.getStitchState() == StitchingUtil.StitchState.STITCH_STATE_START) {
                        mStitchingUtil.mStitchingListener.onStitchingProgressChange(mCurrentTask);
                    }
                }
                Log.v(TAG, "process next task finally");
                release();
            }
        }
    }

    @Override
    public void onPiPanoInit() {
    }

    @Override
    public void onPiPanoChangeCameraResolution(@NonNull ChangeResolutionListener listener) {
    }

    @Override
    public void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync) {
        if (mPiPano != null) {
            mPiPano.drawLensCorrectionFrame(0x1111, true, timestamp, mCurrentTask != null && mCurrentTask.params.useFlow);
        }
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, @NonNull TakePhotoListener listener) {
    }

    @Override
    public void onPiPanoDestroy() {
    }

    @Override
    public void onPiPanoEncodeFrame(int count) {
    }
}
