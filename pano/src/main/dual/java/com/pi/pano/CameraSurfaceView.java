package com.pi.pano;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiAntiMode;
import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiWhiteBalance;
import com.pi.pano.wrap.config.FileConfig;
import com.pi.pano.wrap.config.FileConfigHelper;

import java.lang.ref.WeakReference;
import java.util.Timer;

class CameraSurfaceView extends PanoSurfaceView {
    private static final String TAG = "CameraSurfaceView";

    /**
     * 录像正在录像
     */
    private static final int RECORD_RECORDING = 3;

    /**
     * 录像正在结束
     */
    private static final int RECORD_STOPPING = 1;

    /**
     * 录像结束
     */
    private static final int RECORD_STOPPED = 2;

    private final CameraToTexture[] mCameraToTexture = new CameraToTexture[PilotSDK.CAMERA_COUNT];

    /**
     * 同步锁
     */
    private static final Object sLocks = new Object();

    /**
     * 持续采集捕获的Surface
     */
    private WeakReference<Surface> mCaptureSurfaceRef = null;

    private Timer mCheckCameraFpsTimer;

    @PiExposureTime
    public int mExposeTime;
    public int mDefaultISO;
    @PiWhiteBalance
    public String mDefaultWb = PiWhiteBalance.auto;
    @PiExposureCompensation
    public int mDefaultExposureCompensation;
    @PiAntiMode
    public String mDefaultAntiMode;

    /**
     * 是否锁定默认预览帧率
     */
    private boolean mLockDefaultPreviewFps = true;
    /**
     * 打开预览所需的 imageReader格式（用于拍照时设置）
     */
    private int[] mPreviewImageReaderFormat;
    /**
     * 锁定预览帧率时，记录下上次 绘制畸变画面的时间
     */
    private long mLastDrawLensTimeWithLockPfs;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLensCorrectionMode = 0x11;
    }

    void setLensCorrectionMode(int mode) {
        mLensCorrectionMode = mode;
    }

    @Override
    public void onPiPanoInit() {
        super.onPiPanoInit();

        Log.i(TAG, "onPiPanoInit");

        if (mPiPano == null) {
            return;
        }

        for (int i = 0; i < mCameraToTexture.length; ++i) {
            mCameraToTexture[i] = new CameraToTexture(mContext, i, mPiPano.mSurfaceTexture[i], mPiPano);
        }
    }

    @Override
    public void onPiPanoChangeCameraResolution(@NonNull ChangeResolutionListener listener) {
        Log.i(TAG, "onPiPanoChangeCameraResolution " + listener.mWidth + "*" + listener.mHeight + "*" + listener.mFps);
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setLockDefaultPreviewFps(mLockDefaultPreviewFps);
                c.setPreviewImageReaderFormat(mPreviewImageReaderFormat);
                c.checkOrOpenCamera(listener.mCameraId, listener.mWidth, listener.mHeight, listener.mFps, listener.forceChange);
            }
        }
        try {
            listener.onChangeResolution(listener.mWidth, listener.mHeight);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onChangeResolution(0, 0);
        }
    }

    @Override
    public void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync) {
        if (mPiPano != null && isFrameSync) {
            if (mLockDefaultPreviewFps) {
                //锁定预览帧率为30fps时,确认2帧间隔时间 > (1s/30)
                if (timestamp - mLastDrawLensTimeWithLockPfs >= 33_000_000L) {
                    mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, timestamp, false);
                    mLastDrawLensTimeWithLockPfs = timestamp;
                }
            } else {
                mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, timestamp, false);
            }
        }
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, @NonNull TakePhotoListener listener) {
    }

    private void releaseCamera() {
        while (null != mCaptureSurfaceRef && mCaptureSurfaceRef.get() != null) {
            synchronized (sLocks) {
                try {
                    sLocks.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mCameraToTexture[0].releaseCamera();
    }

    @Override
    public void onPiPanoDestroy() {
        releaseCamera();

        if (null != mCheckCameraFpsTimer) {
            mCheckCameraFpsTimer.cancel();
            mCheckCameraFpsTimer = null;
        }

        super.onPiPanoDestroy();
    }

    void takePhoto(String filename, final int width, final int height, final TakePhotoListener listener) {
        if (mPiPano != null && null != listener) {
            listener.mFilename = filename;
            listener.mStitchPhotoWidth = width;
            listener.mStitchPhotoHeight = height;

            PiPano.clearImageList();
            initConfig(listener.mParams);

            listener.onTakePhotoStart(0);
            mCameraToTexture[0].takePhoto(listener);

            if (listener.mParams.makeSlamPhotoPoint) {
                mPiPano.makeSlamPhotoPoint(listener.mFilename);
            }
        } else {
            Log.e(TAG, "takePhoto mPiPano is null");
        }
    }

    public void setExposeTime(@PiExposureTime int value) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setExposeTime(value);
            }
        }
    }

    void setExposureCompensation(int value) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setExposureCompensation(value);
            }
        }
    }

    void setIOS(int iso) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setISO(iso);
            }
        }
    }

    void setWhiteBalance(String value) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setWhiteBalance(value);
            }
        }
    }

    void setAutoWhiteBalanceLock(boolean value) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setAutoWhiteBalanceLock(value);
            }
        }
    }

    void setAntiMode(String value) {
        this.mDefaultAntiMode = value;
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setAntiMode(value);
            }
        }
    }

    void setLockDefaultPreviewFps(boolean value) {
        this.mLockDefaultPreviewFps = value;
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setLockDefaultPreviewFps(value);
            }
        }
    }

    void setPreviewImageReaderFormat(int[] values) {
        mPreviewImageReaderFormat = values;
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.setPreviewImageReaderFormat(values);
            }
        }
    }

    boolean isLargePreviewSize() {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                if (c.getPreviewWidth() > 2000) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isLockDefaultPreviewFps() {
        return mLockDefaultPreviewFps;
    }

    CameraEnvParams getCurCameraEnvParams() {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                return c.getCameraEnvParams();
            }
        }
        return null;
    }

    int getPreviewFps() {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                return c.getPreviewFps();
            }
        }
        return 0;
    }

    int getPreviewWidth() {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                return c.getPreviewWidth();
            }
        }
        return 0;
    }

    int getPreviewHeight() {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                return c.getPreviewHeight();
            }
        }
        return 0;
    }

    String getCameraId() {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                return c.getCameraId();
            }
        }
        return null;
    }

    void startHighPreviewSize(boolean startPreview) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.updateHighPreviewSize();
                if (startPreview) {
                    c.startPreview();
                }
            }
        }
    }

    void startLowPreviewSize(boolean startPreview) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.updateLowPreviewSize();
                if (startPreview) {
                    c.startPreview();
                }
            }
        }
    }

    /**
     * start capture(recorded or live)
     *
     * @param surface        surface of capture
     * @param previewEnabled preview enabled in capture
     * @return 0:success
     */
    int startCapture(Surface surface, boolean previewEnabled) {
        mPiPano.setPanoEnabled(previewEnabled);
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.startCapture(surface);
            }
        }
        mCaptureSurfaceRef = new WeakReference<>(surface);
        return 0;
    }

    /**
     * stop capture(recorded or live)
     *
     * @return 0:success
     */
    int stopCapture() {
        return stopCapture(true, true);
    }

    int stopCapture(boolean sync) {
        return stopCapture(sync, true);
    }

    int stopCapture(boolean sync, boolean startPreview) {
        if (startPreview) {
            for (CameraToTexture c : mCameraToTexture) {
                if (c != null) {
                    c.stopCapture(sync);
                }
            }
        }
        mCaptureSurfaceRef = null;
        synchronized (sLocks) {
            sLocks.notifyAll();
        }
        mPiPano.setPanoEnabled(true);
        Log.i(TAG, "stop record success");
        return 0;
    }

    /**
     * In continuous capture (recorded or live)
     */
    boolean isInCapture() {
        return null != mCaptureSurfaceRef && mCaptureSurfaceRef.get() != null;
    }

    public void setCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
    }

    public void addCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
    }

    public void removeCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
    }

    private void initConfig(CaptureParams params) {
        if (!params.saveConfig) {
            return;
        }
        FileConfig fileConfig = FileConfigHelper.self().create(params);
        if (fileConfig != null) {
            fileConfig.setFittings(isLensProtected() ? 2 : 1);
            FileConfigHelper.self().saveConfig(fileConfig);
        }
    }
}