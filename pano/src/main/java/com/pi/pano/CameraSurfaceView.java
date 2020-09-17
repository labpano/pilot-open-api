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

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class CameraSurfaceView extends PanoSurfaceView implements ImageReader.OnImageAvailableListener {
    private static final String TAG = CameraSurfaceView.class.getSimpleName();

    private static final int RECORD_RECORDING = 3;
    private static final int RECORD_STOPPING = 1;
    private static final int RECORD_STOPPED = 2;

    private CameraToTexture[] mCameraToTexture = new CameraToTexture[PilotSDK.CAMERA_COUNT];

    /**
     * Sync lock
     */
    private static final Object sLocks = new Object();

    /**
     * record state
     */
    private int mStopRecordState = RECORD_STOPPED;

    private Timer mCheckCameraFpsTimer;

    /**
     * Pixel buffer callback processing for preview
     */
    private ImageReader mImageReader;
    private HandlerThread mThreadHandler;
    private CameraPreviewCallback mCameraPreviewCallback;
    private CameraFixShakeListener mCameraFixShakeListener;

    public int mDefaultISO;
    public int mDefaultWb;
    public int mDefaultExposureCompenstation;
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

        //Check fps. If there is no fps on a camera for 5 seconds, it means that the camera is down.
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
                            listener.mWidth = mCameraToTexture[0].getPreivewWidth();
                            listener.mHeight = mCameraToTexture[0].getPreivewHeight();
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
                        mDefaultExposureCompenstation, mDefaultISO, mDefaultWb);
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

            // The screen will flicker when the resolution is just switched, so wait for a while without rendering
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

        if (hdrIndex == 2) {
            listener.onHdrPhotoParameter(PilotSDK.HDR_PLUS_1);
            listener.mSkipFrame = 7;
            mPiPano.takePhoto(1, listener);
        } else if (hdrIndex == 1) {
            listener.onHdrPhotoParameter(PilotSDK.HDR_PLUS_2);
            listener.mSkipFrame = 7;
            mPiPano.takePhoto(0, listener);
        } else if (hdrIndex == 0) {
            listener.onHdrPhotoParameter(PilotSDK.HDR_PLUS_3);
            if (listener.mHDR) {
                ChangeResolutionListener listener1 = new ChangeResolutionListener() {
                    @Override
                    protected void onChangeResolution(int width, int height) {
                    }
                };

                listener1.mWidth = PilotSDK.CAMERA_PREVIEW_448_280_22[0];
                listener1.mHeight = PilotSDK.CAMERA_PREVIEW_448_280_22[1];
                listener1.mFps = PilotSDK.CAMERA_PREVIEW_448_280_22[2];

                mPiPano.changeCameraResolution(listener1);
            }
        }

        Log.d(TAG, "the save fish eye photo value =" + listener.mSaveFisheyePhoto);
        if (listener.mSaveFisheyePhoto) {
            if (!listener.mHDR || hdrIndex == 2) {
                final Object mLock = new Object();
                TakePhotoListener thumbTakePhotoListener = new TakePhotoListener() {
                    @Override
                    protected void onTakePhotoComplete(int errorCode) {
                        synchronized (mLock) {
                            mLock.notifyAll();
                        }
                    }
                };
                thumbTakePhotoListener.mFilename = listener.mFilename;
                ImageProcess imageProcess = new ImageProcess(720, 360, 0, thumbTakePhotoListener);
                mPiPano.setEncodePhotoSurface(imageProcess.getImageReaderSurface());
                mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, listener.mTimestamp);
                synchronized (mLock) {
                    try {
                        mLock.wait(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            PiPano.recordOnceJpegInfo(0, mDefaultExposureCompenstation, mCameraToTexture[0].mDefaultISO);
            int height = listener.mStitchPhotoHeight > PilotSDK.CAMERA_PREVIEW_4048_2530_15[0] ?
                    PilotSDK.CAMERA_PREVIEW_4048_2530_15[0] : listener.mStitchPhotoHeight;
            ImageProcess imageProcess = new ImageProcess(height / 2 * 5,
                    height, hdrIndex, listener);
            mPiPano.setEncodePhotoSurface(imageProcess.getImageReaderSurface());
            mPiPano.drawLensCorrectionFrame(0x3333, false, listener.mTimestamp);
        } else {
            ImageProcess imageProcess = new ImageProcess(listener.mStitchPhotoWidth,
                    listener.mStitchPhotoHeight, hdrIndex, listener);
            mPiPano.setEncodePhotoSurface(imageProcess.getImageReaderSurface());
            mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, listener.mTimestamp);
        }
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

            PiPano.clearExifThumbnailBuffer();
            PiPano.clearImageList();
            setExposureCompensation(listener.mHDR ? 0 : mDefaultExposureCompenstation);

            if (listener.mHDR && !listener.isAuto) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3500);
                            if (mPiPano != null) {
                                mPiPano.takePhoto(listener.mHDR ? 2 : 0, listener);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                mPiPano.takePhoto(listener.mHDR ? 2 : 0, listener);
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

    void setManualISO(int iso) {
        for (CameraToTexture c : mCameraToTexture) {
            if (c != null) {
                c.mDefaultISO = iso;
            }
        }
    }

    void setWhiteBalance(int value) {
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
                //When the preview resolution is greater than 1000, the real-time splicing video is recorded,
                // otherwise it is recorded as four separate videos for post-processing
                if (c.getPreivewWidth() > 1000) {
                    return true;
                }
            }
        }

        return false;
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