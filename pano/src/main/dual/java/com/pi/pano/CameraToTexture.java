package com.pi.pano;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiIso;
import com.pi.pano.annotation.PiPhotoFileFormat;
import com.pi.pano.annotation.PiWhiteBalance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CameraToTexture {
    private static final String TAG = "CameraToTexture";
    private static final int CONTROL_AE_EXPOSURE_COMPENSATION_STEP = 3;

    private final Context mContext;
    private CameraCaptureSession mPreviewSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private final int mIndex;
    private final SurfaceTexture mPreviewSurfaceTexture;
    private final PiPano mPiPano;
    private CameraCharacteristics mCameraCharacteristics;
    private String mCameraId;
    private int mFps;
    private int mWidth;
    private int mHeight;
    /**
     * true: 图片 ,false: 视频
     */
    private boolean mIsPhoto;
    private boolean mNotUseDefaultFps;

    private int mExposeMode = CaptureRequest.CONTROL_AE_MODE_ON;
    @PiExposureTime
    private int mExposeTime = PiExposureTime.auto;
    @PiExposureCompensation
    private int mExposureCompensation = PiExposureCompensation.normal;
    @PiIso
    private int mISO = PiIso._100;
    @PiWhiteBalance
    private String mWb = PiWhiteBalance.auto;

    /**
     * 测光模式.
     * 0: Average 1: Center 2:Spot 3: Custom
     */
    private final int metering = 1;
    @SuppressLint("NewApi")
    public static final CaptureRequest.Key<Integer> METERING_MODE = new CaptureRequest.Key<>("org.codeaurora.qcamera3.exposure_metering.exposure_metering_mode", int.class);

    private ImageProcess mImageProcess;
    private ImageProcess mDngImageProcess;

    CameraToTexture(Context context, int index, SurfaceTexture surfaceTexture, PiPano piPano) {
        mContext = context.getApplicationContext();
        mIndex = index;
        mPreviewSurfaceTexture = surfaceTexture;
        mPiPano = piPano;
    }

    int getPreviewWidth() {
        if (null == mCameraDevice) {
            return 0;
        }
        return mWidth;
    }

    int getPreviewHeight() {
        if (null == mCameraDevice) {
            return 0;
        }
        return mHeight;
    }

    int getPreviewFps() {
        if (null == mCameraDevice) {
            return 0;
        }
        return mFps;
    }

    String getCameraId() {
        if (null == mCameraDevice) {
            return null;
        }
        return mCameraId;
    }

    /**
     * 摄像头创建监听
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {//关闭摄像头
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    void openCamera(String cameraId, int width, int height, int fps, boolean isPhoto, boolean notUseDefaultFps) {
        Log.i(TAG, "openCamera :" + cameraId + "," + width + "*" + height + ",fps: " + fps + ",isPhoto:" + isPhoto);
        if (cameraId == null) {
            cameraId = "2";
        }
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mIsPhoto = isPhoto;
        mNotUseDefaultFps = notUseDefaultFps;
        updateHighPreviewSize();
        startBackgroundThread();
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
            cameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void setCaptureRequestParam(CaptureRequest.Builder previewBuilder, boolean isPreview) {
        previewBuilder.set(METERING_MODE, metering);
        int realFps = (isPreview && !mNotUseDefaultFps) ? 30 : mFps;
        previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(realFps, realFps));
        if (mExposeMode == CaptureRequest.CONTROL_AE_MODE_ON) {
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            previewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mExposureCompensation * CONTROL_AE_EXPOSURE_COMPENSATION_STEP);
        } else {
            previewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mISO);
            previewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1_000_000_000L / mExposeTime);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        }
        previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, ProUtils.convertRealWhiteBalance(mWb));
    }

    void startPreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            closePreviewSession();
            Surface previewSurface = new Surface(mPreviewSurfaceTexture);
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(previewSurface);

            List<Surface> list = new ArrayList<>();
            list.add(previewSurface);
            if (mIsPhoto) {
                mImageProcess = new ImageProcess(5760, 2880, ImageFormat.JPEG);
                mDngImageProcess = new ImageProcess(6080, 3040, ImageFormat.RAW_SENSOR);
                list.add(mImageProcess.getImageReaderSurface());
                list.add(mDngImageProcess.getImageReaderSurface());
            }
            setCaptureRequestParam(mPreviewBuilder, true);
            mCameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "onConfigureFailed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice || null == mPreviewSession) {
            return;
        }
        try {
            final boolean[] updatePreview = {true};
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    if (mIndex == 0) {
                        mPiPano.nativeStabSetExposureDuration(result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
                    }
                    if (updatePreview[0] && !mPiPano.isCaptureCompleted) {
                        updatePreview[0] = false;
                        mPiPano.isCaptureCompleted = true;
                    }
                    super.onCaptureCompleted(session, request, result);
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    void releaseCamera() {
        closePreviewSession();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundHandler = null;
        }

        stopBackgroundThread();

        Log.i(TAG, "release camera");
    }

    public void setExposeTime(@PiExposureTime int value) {
        mExposeTime = value;
        if (mExposeTime == PiExposureTime.auto) {
            mExposeMode = CaptureRequest.CONTROL_AE_MODE_ON;
        } else {
            mExposeMode = CaptureRequest.CONTROL_AE_MODE_OFF;
        }
        if (null == mCameraDevice || null == mPreviewBuilder) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, mExposeMode);
        if (mExposeMode != CaptureRequest.CONTROL_AE_MODE_ON) {
            mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1_000_000_000L / mExposeTime);
        }
        updatePreview();
    }

    void setExposureCompensation(int value) {
        mExposureCompensation = value;
        if (null == mCameraDevice || null == mPreviewBuilder) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mExposureCompensation * CONTROL_AE_EXPOSURE_COMPENSATION_STEP);
        updatePreview();
    }

    void setISO(int value) {
        mISO = value;
        if (null == mCameraDevice || null == mPreviewBuilder) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mISO);
        updatePreview();
    }

    void setWhiteBalance(String value) {
        mWb = value;
        if (null == mCameraDevice || null == mPreviewBuilder) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, ProUtils.convertRealWhiteBalance(mWb));
        updatePreview();
    }

    void setAutoWhiteBalanceLock(boolean value) {
    }

    void startRecord(Surface surface) {
        if (null == mCameraDevice) {
            return;
        }
        try {
            closePreviewSession();
            //
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            Surface previewSurface = new Surface(mPreviewSurfaceTexture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
            surfaces.add(surface);
            mPreviewBuilder.addTarget(surface);
            //
            setCaptureRequestParam(mPreviewBuilder, false);
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void stopRecord() {
        if (mBackgroundHandler != null) {
            mBackgroundHandler.post(this::startPreview);
        } else {
            startPreview();
        }
    }

    public void updateHighPreviewSize() {
        int[] sizeInIdle = getPreviewSizeInIdle(mWidth, mHeight, mNotUseDefaultFps);
        Log.d(TAG, "updateHighPreviewSize :" + Arrays.toString(sizeInIdle));
        mPreviewSurfaceTexture.setDefaultBufferSize(sizeInIdle[0], sizeInIdle[1]);
    }

    public void updateLowPreviewSize() {
        int[] sizeInIdle = PilotSDK.CAMERA_PREVIEW_640_320_30;
        Log.d(TAG, "updateLowPreviewSize :" + Arrays.toString(sizeInIdle));
        mPreviewSurfaceTexture.setDefaultBufferSize(sizeInIdle[0], sizeInIdle[1]);
    }

    void takePhoto(int hdrIndex, TakePhotoListener listener) {
        if (null == mCameraDevice) {
            return;
        }
        final ImageProcess localImageProcess = mImageProcess;
        final ImageProcess localDngImageProcess = mDngImageProcess;
        try {
            localImageProcess.setHdrIndex(hdrIndex);
            localImageProcess.setTakePhotoListener(listener);
            localImageProcess.start();
            if (listener.mImageFormat == ImageFormat.RAW_SENSOR && hdrIndex <= 0) {
                localDngImageProcess.setTakePhotoListener(listener);
                localDngImageProcess.setHdrIndex(hdrIndex);
                localDngImageProcess.start();
            }
            if (listener.mHDRImageCount > 0) {
                takePhotoCore(localImageProcess, localDngImageProcess, listener, mPreviewSession);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            listener.dispatchTakePhotoComplete(-2);
        }
        if (listener.mHDRImageCount <= 0) {
            mBackgroundHandler.post(() -> {
                try {
                    takePhotoCore(localImageProcess, localDngImageProcess, listener, mPreviewSession);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    listener.dispatchTakePhotoComplete(-2);
                }
                startPreview();
            });
        } else {
            startPreview();
        }
    }

    private void takePhotoCore(ImageProcess imageProcess, ImageProcess dngImageProcess,
                               TakePhotoListener listener, CameraCaptureSession cameraCaptureSession) throws CameraAccessException {
        CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        Surface previewSurface = new Surface(mPreviewSurfaceTexture);
        captureRequestBuilder.addTarget(previewSurface);
        captureRequestBuilder.addTarget(imageProcess.getImageReaderSurface());
        captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
        if (null != dngImageProcess) {
            captureRequestBuilder.addTarget(dngImageProcess.getImageReaderSurface());
        }
        setCaptureRequestParam(captureRequestBuilder, false);
        //TODO recordOnceJpegInfo的作用是,告诉底层记录一次相机姿态,以用于拍照画面水平,但是在这里调用的话,
        // 记录的姿态的时间戳要比实际曝光的时间戳早1~2秒,如果相机这是在运动的话,拍照会不水平,以后优化
        PiPano.recordOnceJpegInfo(listener.hdr_exposureTime, listener.hdr_ev, listener.hdr_iso, listener.hdr_wb);
        addPhotoExifInfo(captureRequestBuilder);
        cameraCaptureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                if (null != dngImageProcess) {
                    if (PiPhotoFileFormat.jpg_dng.equals(listener.mFileFormat)) {
                        DngCreator dngCreator = new DngCreator(mCameraCharacteristics, result);
                        dngImageProcess.setDngCreator(dngCreator);
                    }
                }
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                Log.e(TAG, "onCaptureFailed: " + failure.getReason());
                listener.dispatchTakePhotoComplete(-2);
            }
        }, mBackgroundHandler);
    }

    @SuppressLint("MissingPermission")
    private void addPhotoExifInfo(CaptureRequest.Builder captureRequestBuilder) {
        Location location = null;
        {
            LocationManager ls = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    location = ls.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        captureRequestBuilder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
    }

    private int[] getPreviewSizeInIdle(int width, int height, boolean notUseDefault) {
        if (width == height || notUseDefault) {
            return new int[]{width, height};
        }
        return PilotSDK.CAMERA_PREVIEW_3840_1920_30;
    }
}
