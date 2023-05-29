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
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiAntiMode;
import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiIso;
import com.pi.pano.annotation.PiWhiteBalance;
import com.pi.pano.wrap.ProParams;
import com.pi.pilot.pano.sdk.BuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CameraToTexture {
    private static final String TAG = "CameraToTexture";
    private static final int CONTROL_AE_EXPOSURE_COMPENSATION_STEP = 3;
    private static final String REQUEST_TAG_HDR_DNG_RAW = "hdr_dng_raw";

    private final Context mContext;
    private CameraCaptureSession mCameraSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private final int mIndex;
    private final SurfaceTexture mPreviewSurfaceTexture;
    private final PiPano mPiPano;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraEnvParams mCameraEnvParams;
    private long mActualExposureTime = -1;
    private int mActualSensitivity = -1;

    private int mExposeMode = CaptureRequest.CONTROL_AE_MODE_ON;
    @PiExposureTime
    private int mExposeTime = PiExposureTime.auto;
    @PiExposureCompensation
    private int mExposureCompensation = PiExposureCompensation.normal;
    @PiIso
    private int mISO = PiIso._100;
    @PiWhiteBalance
    private String mWb = PiWhiteBalance.auto;
    @PiAntiMode
    private String mAntiMode = PiAntiMode.auto;

    /**
     * 测光模式.
     * 0: Average 1: Center 2:Spot 3: Custom
     */
    private final int metering = 1;
    @SuppressLint("NewApi")
    public static final CaptureRequest.Key<Integer> METERING_MODE = new CaptureRequest.Key<>("org.codeaurora.qcamera3.exposure_metering.exposure_metering_mode", int.class);

    private ImageReader mJpegImageReader;
    private ImageReader mRawImageReader;

    /**
     * 是否锁定默认预览帧率
     */
    private boolean mLockDefaultPreviewFps = true;
    /**
     * 预览时，需要准备的图像格式
     */
    private int[] mPreviewImageReaderFormat;

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
        return mCameraEnvParams.getWidth();
    }

    int getPreviewHeight() {
        if (null == mCameraDevice) {
            return 0;
        }
        return mCameraEnvParams.getHeight();
    }

    int getPreviewFps() {
        if (null == mCameraDevice) {
            return 0;
        }
        return mCameraEnvParams.getFps();
    }

    String getCameraId() {
        if (null == mCameraDevice) {
            return null;
        }
        return mCameraEnvParams.getCameraId();
    }

    CameraEnvParams getCameraEnvParams() {
        if (null == mCameraDevice) {
            return null;
        }
        return mCameraEnvParams;
    }

    private final CameraDevice.StateCallback mStateCallback = new InnerCameraStateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            super.onOpened(camera);
            startPreview();
        }
    };

    private class InnerCameraStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    }

    private abstract class InnerCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCameraSession = session;
        }
    }

    private class InnerCaptureCallback extends CameraCaptureSession.CaptureCallback {

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
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
    void checkOrOpenCamera(String cameraId, int width, int height, int fps, boolean forceChange) {
        if (cameraId == null) {
            cameraId = "2";
        }
        String lastCameraId = mCameraEnvParams == null ? null : mCameraEnvParams.getCameraId();

        boolean initCamera = mCameraDevice == null || TextUtils.isEmpty(lastCameraId);
        boolean switchCamera = !TextUtils.isEmpty(lastCameraId) && !TextUtils.equals(lastCameraId, cameraId);
        boolean openCamera = initCamera || switchCamera;

        String captureMode = mPreviewImageReaderFormat == null ?
                CameraEnvParams.CAPTURE_STREAM : CameraEnvParams.CAPTURE_PHOTO;
        boolean switchCapture = false;
        boolean sizeChanged = true;
        boolean fpsChanged = true;
        if (mCameraEnvParams == null) {
            mCameraEnvParams = new CameraEnvParams(cameraId, width, height, fps, captureMode);
        } else {
            sizeChanged = width != mCameraEnvParams.getWidth() || height != mCameraEnvParams.getHeight();
            fpsChanged = fps != mCameraEnvParams.getFps();
            mCameraEnvParams.setCameraId(cameraId);
            mCameraEnvParams.setSize(width, height);
            mCameraEnvParams.setFps(fps);
            switchCapture = !captureMode.equals(mCameraEnvParams.getCaptureMode());
            mCameraEnvParams.setCaptureMode(captureMode);
        }
        Log.i(TAG, "checkOrOpenCamera initCamera:" + initCamera + ",switchCamera:" + switchCamera +
                ",switchCapture:" + switchCapture + ",sizeChanged:" + sizeChanged +
                ",forceChange:" + forceChange + ",fpsChanged:" + fpsChanged + " ,result ==> " + mCameraEnvParams);
        if (switchCamera) {
            releaseCamera();
        }
        updateHighPreviewSize();
        startBackgroundThread();
        if (openCamera) {
            CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                cameraManager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            boolean useDefaultFps = mLockDefaultPreviewFps || mCameraEnvParams.getWidth() == mCameraEnvParams.getHeight();
            fpsChanged = fpsChanged && !useDefaultFps;
            if (switchCapture || sizeChanged || forceChange || fpsChanged) {
                startPreview();
                return;
            }
            try {
                setCaptureRequestParam(mPreviewBuilder, true);
                updatePreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void setCaptureRequestParam(CaptureRequest.Builder builder, boolean isPreview) {
        builder.set(METERING_MODE, metering);
        int realFps = getRealFps(isPreview);
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(realFps, realFps));
        if (mPiPano != null && isPreview) {
            //预览帧率设置 ： 锁定默认预览帧率 或者 是平面视频时 才使用默认帧率
            boolean useDefaultFps = mLockDefaultPreviewFps || mCameraEnvParams.getWidth() == mCameraEnvParams.getHeight();
            mPiPano.setPreviewFps(useDefaultFps ? 30 : realFps);
        }
        if (mExposeMode == CaptureRequest.CONTROL_AE_MODE_ON) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mExposureCompensation * CONTROL_AE_EXPOSURE_COMPENSATION_STEP);
            builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, ProUtils.convertRealAntiMode(mAntiMode));
        } else {
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, mISO);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1_000_000_000L / mExposeTime);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        }
        builder.set(CaptureRequest.CONTROL_AWB_MODE, ProUtils.convertRealWhiteBalance(mWb));
    }

    void setCaptureRequestParamForHdr(CaptureRequest.Builder builder) {
        builder.set(METERING_MODE, metering);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AWB_MODE, ProUtils.convertRealWhiteBalance(mWb));
        builder.set(CaptureRequest.SENSOR_FRAME_DURATION, 1000000000L / 100);
    }

    void startPreview() {
        if (null == mCameraDevice) {
            Log.w(TAG, "startPreview,camera devices is null.");
            return;
        }
        try {
            mPiPano.skipFrameWithStartPreview = false;
            closePreviewSession();
            Surface previewSurface = new Surface(mPreviewSurfaceTexture);
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(previewSurface);

            List<Surface> list = new ArrayList<>();
            list.add(previewSurface);
            if (mPreviewImageReaderFormat != null) {
                checkImageReaderFormat(list);
            }
            setCaptureRequestParam(mPreviewBuilder, true);
            mCameraDevice.createCaptureSession(list, new InnerCaptureSessionStateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    super.onConfigured(session);
                    mPiPano.skipFrameWithStartPreview = true;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
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
        if (null == mCameraDevice || null == mCameraSession) {
            Log.w(TAG, "updatePreview,session isn't create");
            return;
        }
        try {
            mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), createPreviewCaptureCallback(), mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private CameraCaptureSession.CaptureCallback createPreviewCaptureCallback() {
        if (mPreviewImageReaderFormat != null) {
            return new PreviewCaptureCallback2();
        } else {
            return new PreviewCaptureCallback();
        }
    }

    private class PreviewCaptureCallback extends InnerCaptureCallback {
        boolean updatePreview = true;

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            if (mIndex == 0) {
                Long duration = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                if (duration != null) {
                    mPiPano.nativeStabSetExposureDuration(duration);
                } else {
                    Log.e(TAG, "onCaptureCompleted,result doesn't have SENSOR_EXPOSURE_TIME");
                }
            }
            if (updatePreview && !mPiPano.isCaptureCompleted) {
                updatePreview = false;
                mPiPano.isCaptureCompleted = true;
                mPiPano.mSkipFrameWithOpenCamera = 8;
                if (BuildConfig.DEBUG) {
                    mPiPano.mSkipFrameWithOpenCamera = SystemPropertiesProxy.getInt(
                            "persist.dev.skip_frame_preview", mPiPano.mSkipFrameWithOpenCamera);
                }
                Log.d(TAG, "onCaptureCompleted set skipFrame :" + mPiPano.mSkipFrameWithOpenCamera);
            }
            if (updatePreview && mPiPano.skipFrameWithStartPreview) {
                mPiPano.mSkipFrameWithStartPreview = 4;
                mPiPano.skipFrameWithStartPreview = false;
            }
            super.onCaptureCompleted(session, request, result);
        }
    }

    private class PreviewCaptureCallback2 extends PreviewCaptureCallback {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            mActualExposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            mActualSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
        }
    }

    private void closePreviewSession() {
        if (mCameraSession != null) {
            mCameraSession.close();
            mCameraSession = null;
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

        mPiPano.isCaptureCompleted = false;
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

    void setAntiMode(String value) {
        mAntiMode = value;
        if (null == mCameraDevice || null == mPreviewBuilder) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, ProUtils.convertRealAntiMode(value));
        updatePreview();
    }

    public void setLockDefaultPreviewFps(boolean value) {
        this.mLockDefaultPreviewFps = value;
    }

    public void setPreviewImageReaderFormat(int[] values) {
        this.mPreviewImageReaderFormat = values;
    }

    void startCapture(Surface surface) {
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
                    mCameraSession = cameraCaptureSession;
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

    void stopCapture(boolean sync) {
        if (sync && mBackgroundHandler != null) {
            mBackgroundHandler.post(this::startPreview);
        } else {
            startPreview();
        }
    }

    public void updateHighPreviewSize() {
        int[] sizeInIdle = getPreviewSizeInIdle();
        Log.d(TAG, "updateHighPreviewSize :" + mCameraEnvParams.getWidth() + "x"
                + mCameraEnvParams.getHeight() + " ==> " + Arrays.toString(sizeInIdle));
        mPreviewSurfaceTexture.setDefaultBufferSize(sizeInIdle[0], sizeInIdle[1]);
    }

    public void updateLowPreviewSize() {
        int[] sizeInIdle = PilotSDK.CAMERA_PREVIEW_640_320_30;
        Log.d(TAG, "updateLowPreviewSize :" + Arrays.toString(sizeInIdle));
        mPreviewSurfaceTexture.setDefaultBufferSize(sizeInIdle[0], sizeInIdle[1]);
    }

    void takePhoto(TakePhotoListener listener) {
        if (null == mCameraDevice) {
            return;
        }
        if (listener.mParams.isHdr()) {
            takePhotoForHdr(listener, mCameraSession);
            return;
        }
        final ImageProcess jpegImageProcess = new ImageProcess(listener, mJpegImageReader, false); // jpeg
        final ImageProcess rawImageProcess;
        if (listener.mImageFormat == ImageFormat.RAW_SENSOR) {
            rawImageProcess = new ImageProcess(listener, mRawImageReader, false); // raw
        } else {
            rawImageProcess = null;
        }
        PiPano.recordOnceJpegInfo(mExposeTime, mExposureCompensation, mISO, PiWhiteBalance.auto.equals(mWb) ? 0 : 1);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    CaptureRequest.Builder stillCapture = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    stillCapture.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
                    setCaptureRequestParam(stillCapture, false);
                    fillExtInfo(stillCapture);
                    stillCapture.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
                    //
                    stillCapture.addTarget(new Surface(mPreviewSurfaceTexture));
                    stillCapture.addTarget(mJpegImageReader.getSurface());
                    if (null != rawImageProcess) {
                        stillCapture.addTarget(mRawImageReader.getSurface());
                    }
                    //
                    mCameraSession.capture(stillCapture.build(), new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            if (null != rawImageProcess) {
                                if (rawImageProcess.needDng()) {
                                    DngCreator dngCreator = new DngCreator(mCameraCharacteristics, result);
                                    rawImageProcess.setDngCreator(dngCreator);
                                }
                            }
                        }

                        @Override
                        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                            super.onCaptureFailed(session, request, failure);
                            Log.e(TAG, "takePhoto,onCaptureFailed,reason:" + failure.getReason());
                            endCapture();
                            listener.dispatchTakePhotoComplete(-2);
                        }
                    }, mBackgroundHandler);
                } catch (CameraAccessException ex) {
                    Log.e(TAG, "takePhoto,ex:" + ex);
                    ex.printStackTrace();
                    endCapture();
                    listener.dispatchTakePhotoComplete(-2);
                }
            }

            private void endCapture() {
                jpegImageProcess.aborted();
                if (rawImageProcess != null) {
                    rawImageProcess.aborted();
                }
            }
        });
    }

    private void takePhotoForHdr(TakePhotoListener listener, CameraCaptureSession cameraCaptureSession) {
        takePhotoForHdrInner(listener, cameraCaptureSession, null);
    }


    /**
     * hdr拍照
     *
     * @param listener 监听
     */
    private void takePhotoForHdrInner(TakePhotoListener listener, CameraCaptureSession cameraCaptureSession, ProParams lastProParams) {
        if (null == mCameraDevice) {
            return;
        }
        final HdrImageProcess jpegImageProcess = new HdrImageProcess(listener, mJpegImageReader); // jpeg
        final ImageProcess rawImageProcess;
        if (listener.mImageFormat == ImageFormat.RAW_SENSOR) {
            rawImageProcess = new ImageProcess(listener, mRawImageReader, false); // raw
        } else {
            rawImageProcess = null;
        }
        PiPano.recordOnceJpegInfo(mActualExposureTime == 0 ? 0 : (int) (1_000_000_000L / mActualExposureTime), mExposureCompensation, mActualSensitivity, PiWhiteBalance.auto.equals(mWb) ? 0 : 1);
        int delay = 0;
        if (mActualExposureTime == -1 || mActualSensitivity == -1) {
            delay = 1500;
        }
        Log.d(TAG, "takePhotoForHdrInner exTime: " + mActualExposureTime + ", iso:" + mActualExposureTime);
        mBackgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    CaptureRequest.Builder stillCapture = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    stillCapture.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
                    setCaptureRequestParamForHdr(stillCapture);
                    fillExtInfo(stillCapture);
                    stillCapture.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
                    stillCapture.set(CaptureRequest.SENSOR_SENSITIVITY, mActualSensitivity);
                    //
                    stillCapture.addTarget(new Surface(mPreviewSurfaceTexture));
                    stillCapture.addTarget(mJpegImageReader.getSurface());
                    //
                    List<CaptureRequest> requests = new ArrayList<>();
                    {
                        Range<Long> exposure_time_range = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                        long min_exposure_time = exposure_time_range.getLower();
                        long max_exposure_time = exposure_time_range.getUpper();
                        Log.d(TAG, "takePhotoForHdr,ActualExposureTime:" + mActualExposureTime + ",ActualSensitivity:" + mActualSensitivity);
                        long[] dst_exposure_times = HdrExposureTimeCalculator.getExposureTimes(listener.mParams.hdrCount, mActualExposureTime);
                        for (int pos = 0; pos < dst_exposure_times.length; pos++) {
                            long dst_exposure_time = dst_exposure_times[pos];
                            if (dst_exposure_time < min_exposure_time) {
                                dst_exposure_time = min_exposure_time;
                            }
                            if (dst_exposure_time > max_exposure_time) {
                                dst_exposure_time = max_exposure_time;
                            }
                            stillCapture.set(CaptureRequest.SENSOR_EXPOSURE_TIME, dst_exposure_time);
                            Log.d(TAG, "takePhotoForHdr,[" + pos + "] dst_exposure_time:" + dst_exposure_time);
                            if (rawImageProcess != null) {
                                if (pos == dst_exposure_times.length / 2) {
                                    stillCapture.setTag(REQUEST_TAG_HDR_DNG_RAW);
                                    Surface surface = mRawImageReader.getSurface();
                                    stillCapture.addTarget(surface);
                                    requests.add(stillCapture.build());
                                    // 清除
                                    stillCapture.setTag(null);
                                    stillCapture.removeTarget(surface);
                                    continue;
                                }
                            }
                            requests.add(stillCapture.build());
                        }
                    }
                    // control frame duration, enable，frame duration is 100ms
                    SystemPropertiesProxy.set("vendor.camera.burst.mode", "1");
                    cameraCaptureSession.captureBurst(requests, new InnerCaptureCallback() {
                        boolean captureFailed = false;

                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            if (rawImageProcess != null && REQUEST_TAG_HDR_DNG_RAW.equals(request.getTag())
                                    && rawImageProcess.needDng()) {
                                DngCreator dngCreator = new DngCreator(mCameraCharacteristics, result);
                                rawImageProcess.setDngCreator(dngCreator);
                            }
                        }

                        @Override
                        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                            super.onCaptureFailed(session, request, failure);
                            Log.e(TAG, "takePhotoForHdr,onCaptureFailed,reason:" + failure.getReason());
                            if (rawImageProcess != null && REQUEST_TAG_HDR_DNG_RAW.equals(request.getTag())) {
                                rawImageProcess.aborted();
                            }
                            captureFailed = true;
                            listener.onTakePhotoComplete(-2);
                        }

                        @Override
                        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                            Log.d(TAG, "takePhotoForHdr,onCaptureSequenceCompleted");
                            handleStackHdr(captureFailed);
                        }

                        @Override
                        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                            super.onCaptureSequenceAborted(session, sequenceId);
                            Log.e(TAG, "takePhotoForHdr,onCaptureSequenceAborted");
                            endCapture();
                            captureFailed = true;
                            listener.onTakePhotoComplete(-2);
                        }
                    }, mBackgroundHandler);
                } catch (Exception ex) {
                    Log.e(TAG, "takePhotoForHdr,ex:" + ex);
                    ex.printStackTrace();
                    endCapture();
                    listener.dispatchTakePhotoComplete(-2);
                }
            }

            private void endCapture() {
                jpegImageProcess.aborted();
                if (rawImageProcess != null) {
                    rawImageProcess.aborted();
                }
                if (lastProParams != null) {
                    setExposeTime(lastProParams.mExposeTime);
                }
                // control frame duration, unable，frame duration is normal
                SystemPropertiesProxy.set("vendor.camera.burst.mode", "0");
            }

            // There is a memory requirement during HDR synthesis,
            // which frees up excess surface output in camera and reduces screen preview before synthesis.
            private void handleStackHdr(boolean hasCaptureFailed) {
                // control frame duration, unable，frame duration is normal
                SystemPropertiesProxy.set("vendor.camera.burst.mode", "0");
                if (hasCaptureFailed) {
                    return;
                }
                // first 修改预览为低清晰度
                simplePreview(new InnerCaptureSessionStateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        super.onConfigured(session);
                        Log.d(TAG, "handleStackHdr simplePreview onConfigured :" + session);
                        updatePreview();
                        int errCode = jpegImageProcess.stackHdrConfigured();
                        endStackHdr();
                        listener.onTakePhotoComplete(errCode);
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        listener.onTakePhotoComplete(-2);
                        endStackHdr();
                    }

                    private void endStackHdr() {
                        // end 还原高清预览
                        updateHighPreviewSize();
                        if (lastProParams != null) {
                            mExposeTime = lastProParams.mExposeTime;
                        }
                        startPreview();
                    }
                });
            }

            private void simplePreview(CameraCaptureSession.StateCallback callback) {
                closePreviewSession();
                try {
                    mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mPreviewSurfaceTexture.setDefaultBufferSize(640, 320);
                    Surface previewSurface = new Surface(mPreviewSurfaceTexture);
                    mPreviewBuilder.addTarget(previewSurface);
                    List<Surface> list = new ArrayList<>();
                    list.add(previewSurface);
                    setCaptureRequestParam(mPreviewBuilder, true);
                    mCameraDevice.createCaptureSession(list, callback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    private void restoreAutoProParamsWithHdr(CameraCaptureSession.CaptureCallback listener) {
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        try {
            mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), new PreviewCaptureCallback2() {
                int updateResult = 0;

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //After manual to automatic exposure, the exposure value and iso have changed, and the invalid data of the previous few frames are filtered.
                    if (updateResult == 4 && listener != null) {
                        listener.onCaptureCompleted(session, request, result);
                    }
                    updateResult++;
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fill in additional request information, including: location.
     */
    @SuppressLint("MissingPermission")
    private void fillExtInfo(CaptureRequest.Builder builder) {
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
        builder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
    }

    private int[] getPreviewSizeInIdle() {
        if (mCameraEnvParams.getWidth() == mCameraEnvParams.getHeight() || !mLockDefaultPreviewFps) {
            return new int[]{mCameraEnvParams.getWidth(), mCameraEnvParams.getHeight()};
        }
        return PilotSDK.CAMERA_PREVIEW_3840_1920_30;
    }

    private int getRealFps(boolean isPreview) {
        return (isPreview && mLockDefaultPreviewFps &&
                mCameraEnvParams.getWidth() != mCameraEnvParams.getHeight()) ? 30 : mCameraEnvParams.getFps();
    }

    private void checkImageReaderFormat(List<Surface> list) {
        boolean hasJpg = false;
        boolean hasRaw = false;
        for (int format : mPreviewImageReaderFormat) {
            if (format == ImageFormat.JPEG) {
                hasJpg = true;
            } else if (format == ImageFormat.RAW_SENSOR) {
                hasRaw = true;
            }
        }
        if (hasJpg) {
            mJpegImageReader = ImageReader.newInstance(5760, 2880, ImageFormat.JPEG, 1);
            list.add(mJpegImageReader.getSurface());
        }
        if (hasRaw) {
            mRawImageReader = ImageReader.newInstance(6080, 3040, ImageFormat.RAW_SENSOR, 1);
            list.add(mRawImageReader.getSurface());
        }
    }
}
