package com.pi.pano;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiWhiteBalance;

import java.util.Timer;

/**
 * Camera SurfaceView.
 */
class CameraSurfaceView extends PanoSurfaceView implements ImageReader.OnImageAvailableListener {
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
     * 录像结束状态
     */
    private int mStopRecordState = RECORD_STOPPED;

    private Timer mCheckCameraFpsTimer;

    /**
     * 用于预览的像素buffer回调
     */
    private ImageReader mImageReader;
    private HandlerThread mThreadHandler;
    private CameraPreviewCallback mCameraPreviewCallback;

    @PiExposureTime
    public int mExposeTime;
    public int mDefaultISO;
    @PiWhiteBalance
    public String mDefaultWb = PiWhiteBalance.auto;
    @PiExposureCompensation
    public int mDefaultExposureCompensation;

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
    public void onPiPanoChangeCameraResolution(ChangeResolutionListener listener) {
        Log.i(TAG, "onPiPanoChangeCameraResolution " + listener.mWidth + "*" + listener.mHeight + "*" + listener.mFps);

        //如果此时上一次录像仍然还同停止完成，需要等待
        releaseCamera();

        // 直播+街景 不使用默认30fps预览
        boolean notUseDefaultFps = listener instanceof DefaultLiveChangeResolutionListener ||
                listener instanceof DefaultScreenLiveChangeResolutionListener ||
                listener instanceof DefaultStreetVideoChangeResolutionListener;
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.openCamera(listener.mCameraId, listener.mWidth, listener.mHeight,
                        listener.mFps, listener.isPhoto, notUseDefaultFps);
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
            mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, timestamp, false);
            if (mImageReader != null) {
                mPiPano.drawVioFrame();
            }
        }
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, TakePhotoListener listener) {
        Log.d(TAG, "onPiPanoCaptureFrame,hdrIndex:" + hdrIndex);
        if (listener == null) {
            Log.e(TAG, "onPiPanoCaptureFrame param listener is null");
            return;
        }
        listener.onTakePhotoStart(hdrIndex);
        //
        int width = listener.mStitchPhotoWidth;
        int height = listener.mStitchPhotoHeight;
        if (listener.mIsStitched) {
            ImageProcess imageProcess = new ImageProcess(width, height, listener.mImageFormat, hdrIndex, listener);
            mPiPano.setEncodePhotoSurface(imageProcess.getImageReaderSurface());
            mPiPano.drawLensCorrectionFrame(listener.mIsStitched ? 0x11 : 0x33,
                    listener.mIsStitched, listener.mTimestamp, false);
        } else {
            mCameraToTexture[0].takePhoto(hdrIndex, listener);
        }
        if (listener.mHDRImageCount > 0) {
            listener.onHdrPhotoParameter(hdrIndex, listener.mHDRImageCount);
            if (hdrIndex < (listener.mHDRImageCount - 1)) {
                listener.mSkipFrame = 7;
                mPiPano.takePhoto(hdrIndex + 1, listener);
            }
        }
        mPiPano.setEncodePhotoSurface(null);
    }

    private void releaseCamera() {
        while (mStopRecordState != RECORD_STOPPED) {
            synchronized (sLocks) {
                try {
                    sLocks.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        mCameraToTexture[0].releaseCamera();
        if (mPiPano != null) {
            mPiPano.isCaptureCompleted = false;
        }
    }

    @Override
    public void onPiPanoDestroy() {
        setPreviewCallback(null);

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

            mPiPano.takePhoto(0, listener);

            if (listener.mMakeSlamPhotoPoint) {
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

    int startRecord(Surface recordSurface, boolean previewEnabled) {
        mPiPano.setPanoEnabled(previewEnabled);
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.startRecord(recordSurface);
            }
        }

        mStopRecordState = RECORD_RECORDING;

        return 0;
    }

    int stopRecord() {
        mStopRecordState = RECORD_STOPPING;
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.stopRecord();
            }
        }

        mStopRecordState = RECORD_STOPPED;
        synchronized (sLocks) {
            sLocks.notifyAll();
        }
        mPiPano.setPanoEnabled(true);
        Log.i(TAG, "stop record success");
        return 0;
    }

    boolean isInRecord() {
        return mStopRecordState != RECORD_STOPPED;
    }

    void removePreviewCallBack() {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

        if (mThreadHandler != null) {
            mThreadHandler.quitSafely();
            mThreadHandler = null;
        }
        if (mPiPano != null) {
            mPiPano.setEncodeVioSurface(null);
        }
    }

    void setPreviewCallback(CameraPreviewCallback callback) {

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

        if (mThreadHandler != null) {
            mThreadHandler.quitSafely();
            mThreadHandler = null;
        }

        if (callback != null) {
            mThreadHandler = new HandlerThread("Vio");
            mThreadHandler.start();

            mImageReader = ImageReader.newInstance(1024, 512, PixelFormat.RGBA_8888, 1);
            mImageReader.setOnImageAvailableListener(this, new Handler(mThreadHandler.getLooper()));
        }

        mPiPano.setEncodeVioSurface(mImageReader == null ? null : mImageReader.getSurface());
        mCameraPreviewCallback = callback;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        //int width = image.getWidth();
        int height = image.getHeight();
        int stride = image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride();
        mCameraPreviewCallback.onPreviewCallback(stride, height, image.getTimestamp(), image.getPlanes()[0].getBuffer());

        image.close();
    }

    public void drawPreviewFrame(int frame) {
        if (mPiPano != null) {
            mPiPano.drawPreviewFrame(frame);
        }
    }

    public void setCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
    }

    public void addCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
    }

    public void removeCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
    }
}