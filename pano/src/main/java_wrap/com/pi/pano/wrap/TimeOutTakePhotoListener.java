package com.pi.pano.wrap;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.pi.pano.TakePhotoListener2;
import com.pi.pano.annotation.PiHdrCount;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeOutTakePhotoListener extends TakePhotoListener2 {
    public static final int TIMEOUT_TIME = 40_000;
    private final IPhotoListener mListener;

    protected final Handler mHandler;
    private volatile boolean isTerminate = false;
    private final AtomicInteger photoIndex = new AtomicInteger(0);

    public TimeOutTakePhotoListener(IPhotoListener callback, Handler handler) {
        this.mHandler = handler;
        this.mListener = callback;
    }

    private boolean isHdr() {
        return mParams.hdrCount != PiHdrCount.out && (mListener instanceof IHDRPhotoListener);
    }

    private final Runnable mTimeoutRunnable = () -> {
        SystemClock.sleep(TIMEOUT_TIME);
        if (isTerminate) {
            return;
        }
        terminate();
        notifyTakeError(isHdr(), PhotoErrorCode.error_timeout);
    };

    protected void terminate() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        isTerminate = true;
    }

    @Override
    protected void onHdrPhotoParameter(int hdrIndex, int hdrCount) {
        super.onHdrPhotoParameter(hdrIndex, hdrCount);
        if (isTerminate) {
            return;
        }
        notifyHdrPrepare(hdrIndex, hdrCount);
    }

    @Override
    protected void onTakePhotoStart(int index) {
        super.onTakePhotoStart(index);
        if (isTerminate) {
            return;
        }
        photoIndex.incrementAndGet();
        notifyTakeStart(index);
        //
        if (photoIndex.get() == 1) {
            mHandler.postDelayed(mTimeoutRunnable, TIMEOUT_TIME);
        }
    }

    @Override
    protected void onTakePhotoComplete(int errorCode) {
        super.onTakePhotoComplete(errorCode);
        if (isTerminate) {
            return;
        }
        terminate();
        if (null != mListener) {
            if (errorCode == 0) {
                notifyTakeSuccess(isHdr(), mStitchFile, mUnStitchFile);
            } else {
                notifyTakeError(isHdr(), PhotoErrorCode.error + " (" + errorCode + ")");
            }
        }
    }

    protected void notifyHdrPrepare(int hdrIndex, int hdrCount) {
        if (mListener instanceof IHDRPhotoListener) {
            ((IHDRPhotoListener) mListener).onHdrPrepare(hdrIndex, hdrCount);
        } else {
            // HDR 未进行处理
        }
    }

    public void notifyTakeStart() {
        if (null != mListener) {
            mHandler.post(mListener::onTakeStart);
        }
    }

    protected void notifyTakeStart(int index) {
        if (null != mListener) {
            mHandler.post(() -> mListener.onTakeStart(index));
        }
    }

    protected void notifyTakeError(boolean change, String msg) {
        if (null != mListener) {
            mHandler.post(() -> mListener.onTakeError(change, msg));
        }
    }

    protected void notifyTakeSuccess(boolean change, @Nullable File stitchFile, @Nullable File unStitchFile) {
        if (null != mListener) {
            mHandler.post(() -> mListener.onTakeSuccess(change, mFilename, stitchFile, unStitchFile));
        }
    }
}
