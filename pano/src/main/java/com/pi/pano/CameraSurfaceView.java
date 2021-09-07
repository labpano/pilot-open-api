package com.pi.pano;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;

import com.pi.pano.annotation.PiProEt;
import com.pi.pano.annotation.PiProIso;
import com.pi.pano.annotation.PiProWb;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class CameraSurfaceView extends PanoSurfaceView implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "CameraSurfaceView";

    private static final int RECORD_RECORDING = 3;
    private static final int RECORD_STOPPING = 1;
    private static final int RECORD_STOPPED = 2;

    private final CameraToTexture[] mCameraToTexture = new CameraToTexture[PilotSDK.CAMERA_COUNT];

    private static final Object sLocks = new Object();

    private int mStopRecordState = RECORD_STOPPED;

    private Timer mCheckCameraFpsTimer;

    private ImageReader mImageReader;
    private HandlerThread mThreadHandler;
    private CameraPreviewCallback mCameraPreviewCallback;
    private CameraFixShakeListener mCameraFixShakeListener;

    @PiProEt
    public int mExposeTime;
    public int mDefaultISO;
    @PiProWb
    public String mDefaultWb = PiProWb.auto;
    public int mDefaultExposureCompensation;
    private Camera[] cameras;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLensCorrectionMode = 0x1111;
    }

    void setLensCorrectionMode(int mode) {
        mLensCorrectionMode = mode;
    }

    public final Camera[] getCameras() {
        return cameras;
    }

    @Override
    public void onPiPanoInit() {
        super.onPiPanoInit();
        Log.i(TAG, "onPiPanoInit");
        if (mPiPano == null) {
            return;
        }

        for (int i = 0; i < mCameraToTexture.length; ++i) {
            mCameraToTexture[i] = new CameraToTexture(i, mPiPano.mSurfaceTexture[i]);
        }

        if (mCheckCameraFpsTimer != null) {
            mCheckCameraFpsTimer.cancel();
        }
        mCheckCameraFpsTimer = new Timer();
        mCheckCameraFpsTimer.schedule(new TimerTask() {
            private int times;

            @Override
            public void run() {
                if (null != mPiPano) {
                    if (mPiPano.isCameraFpsLow()) {
                        int[] fps = mPiPano.getCameraFps();
                        Log.e(TAG, String.format("camera fps low,%1$d,%2$d,%3$d,%4$d", fps[0], fps[1], fps[2], fps[3]));
                        times++;
                        if (times >= 5) {
                            Log.e(TAG, "camera fps low, reopen camera");
                            times = 0;
                            ChangeResolutionListener listener = new ChangeResolutionListener() {
                                @Override
                                protected void onChangeResolution(int width, int height) {
                                    Log.d(TAG, "camera reopen,onChangeResolution,width:" + width + ",height:" + height);
                                    if (null != mCameraFixShakeListener) {
                                        mCameraFixShakeListener.onCameraFixShakeAfter(getCameras());
                                    }
                                }
                            };
                            listener.mWidth = mCameraToTexture[0].getPreviewWidth();
                            listener.mHeight = mCameraToTexture[0].getPreviewHeight();
                            listener.mFps = mCameraToTexture[0].getPreviewFps();
                            listener.mReopen = true;
                            if (null != mCameraFixShakeListener) {
                                mCameraFixShakeListener.onCameraFixShakeBefore();
                            }
                            onPiPanoChangeCameraResolution(listener);
                            Log.e(TAG, "camera fps low, reopen camera finish");
                        }
                    } else {
                        times = 0;
                    }
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void onPiPanoChangeCameraResolution(ChangeResolutionListener listener) {
        Log.i(TAG, "onPiPanoChangeCameraResolution " + listener.mWidth + "*" + listener.mHeight + "*" + listener.mFps);
        releaseCamera();

        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.openCamera(listener.mWidth, listener.mHeight, listener.mFps,
                        mDefaultExposureCompensation, mDefaultISO, mDefaultWb);
            }
        }

        cameras = new Camera[]{
                mCameraToTexture[0].getCamera(),
                mCameraToTexture[1].getCamera(),
                mCameraToTexture[2].getCamera(),
                mCameraToTexture[3].getCamera()
        };

        try {
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(PilotSDK.CAMERA_COUNT);
            mCameraToTexture[0].startPreview(fixedThreadPool);
            Thread.sleep(100);
            mCameraToTexture[1].startPreview(fixedThreadPool);
            mCameraToTexture[2].startPreview(fixedThreadPool);
            mCameraToTexture[3].startPreview(fixedThreadPool);
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(5, TimeUnit.SECONDS);

            // The screen will flicker when the resolution is just switched,
            // so wait for a while without rendering
            Thread.sleep(1600);

            listener.onChangeResolution(listener.mWidth, listener.mHeight);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onChangeResolution(0, 0);
        }
    }

    @Override
    public void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync) {
        if (mPiPano != null && isFrameSync) {
            mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, timestamp);
            if (mImageReader != null) {
                mPiPano.drawVioFrame();
            }
        }
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, TakePhotoListener listener) {
        if (listener == null) {
            Log.e(TAG, "onPiPanoCaptureFrame param listener is null");
            return;
        }

        final int curEv = mDefaultExposureCompensation;
        if (listener.mHDRImageCount > 0) {
            listener.onHdrPhotoParameter(hdrIndex, listener.mHDRImageCount);
            if (hdrIndex < (listener.mHDRImageCount - 1)) {
                listener.mSkipFrame = 7;
                mPiPano.takePhoto(hdrIndex + 1, listener);
            } else{
                // Switch to the smallest preview resolution when shooting the last HDR to free up memory,
                // because the HDR algorithm requires a lot of memory
                ChangeResolutionListener changeResolutionListener = new ChangeResolutionListener() {
                    @Override
                    protected void onChangeResolution(int width, int height) {
                    }
                };
                changeResolutionListener.mWidth = PilotSDK.CAMERA_PREVIEW_448_280_22[0];
                changeResolutionListener.mHeight = PilotSDK.CAMERA_PREVIEW_448_280_22[1];
                changeResolutionListener.mFps = PilotSDK.CAMERA_PREVIEW_448_280_22[2];
                mPiPano.changeCameraResolution(changeResolutionListener);
            }
        }
        if (ExposeTimeAdjustHelper.State.ENABLED.equals(ExposeTimeAdjustHelper.getState())) {
            final int exposureTime = ExposeTimeAdjustHelper.getExpose();
            final int iso = ExposeTimeAdjustHelper.getISO();
            PiPano.recordOnceJpegInfo(exposureTime, 0, iso, PiProWb.auto.endsWith(mDefaultWb) ? 0 : 1);
        }
        else {
            final int exposureTime = ExposeTimeRealValueReader.obtainExposureTime();
            final int iso = mDefaultISO != PiProIso.auto ? mDefaultISO : IsoRealValueReader.obtainISO();
            final int writeBalance = PiProWb.auto.endsWith(mDefaultWb) ? 0 : 1;
            if (listener.mHDRImageCount > 0 && hdrIndex == 0) { // 保留hdr拍摄第1张时的信息，做为合成hdr照片时的信息
                listener.hdr_exposureTime = exposureTime;
                listener.hdr_iso = iso;
                listener.hdr_ev = curEv;
                listener.hdr_wb = writeBalance;
            }
            PiPano.recordOnceJpegInfo(exposureTime, curEv, iso, writeBalance);
        }

        int width = listener.mStitchPhotoWidth;
        int height = listener.mStitchPhotoHeight;
        if (!listener.mIsStitched) {
            height = Math.min(listener.mStitchPhotoHeight, PilotSDK.CAMERA_PREVIEW_4048_2530_15[0]);
            width = height / 2 * 5;
        }
        ImageProcess imageProcess = new ImageProcess(width, height, hdrIndex, listener);
        mPiPano.setEncodePhotoSurface(imageProcess.getImageReaderSurface());
        mPiPano.drawLensCorrectionFrame(listener.mIsStitched ? 0x1111 : 0x3333,
                listener.mIsStitched, listener.mTimestamp);

        listener.onTakePhotoStart();

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

        mCameraToTexture[1].releaseCamera();
        mCameraToTexture[2].releaseCamera();
        mCameraToTexture[3].releaseCamera();
        mCameraToTexture[0].releaseCamera();
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
                // When the preview resolution is greater than 1000, the real-time splicing video is recorded,
                // otherwise it is recorded as four separate videos for postpone-processing
                if (c.getPreviewWidth() > 1000) {
                    return true;
                }
            }
        }
        return false;
    }

    int getFps() {
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

    int startRecord(final String filename, MediaRecorderListener listener, int video_encoder, int channelCount) {
        final File dir = new File(filename);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "create directory error: " + filename);
                return 2;
            }
        }

        mPiPano.setStabilizationFile(filename + "/stabilization");

        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.startRecord(filename, listener, video_encoder, channelCount);
            }
        }

        mStopRecordState = RECORD_RECORDING;
        return 0;
    }

    int stopRecord(String firmware) {
        mStopRecordState = RECORD_STOPPING;
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.stopRecord(firmware, mPiPano);
            }
        }
        mPiPano.setStabilizationFile(null);
        mStopRecordState = RECORD_STOPPED;
        synchronized (sLocks) {
            sLocks.notifyAll();
        }
        Log.e("CameraToTexture", "stop record success");
        return 0;
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
        mCameraFixShakeListener = cameraFixShakeListener;
    }
}