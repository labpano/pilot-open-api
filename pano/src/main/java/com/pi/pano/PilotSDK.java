package com.pi.pano;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.location.Location;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;

import com.pi.pilot.pano.sdk.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Panorama-related SDK, which can be used for panorama expansion, stitching,
 * previewing, taking pictures, setting the video live interface, inserting panorama information
 */
public class PilotSDK implements PanoSurfaceViewListener {
    private static final String TAG = "PilotSDK";

    /**
     * The number of modules of the panoramic camera is also the number of cameras
     */
    public static final int CAMERA_COUNT = 4;

    public static final int[] CAMERA_PREVIEW_4048_2530_30 = new int[]{4048, 2530, 30};
    public static final int[] CAMERA_PREVIEW_3520_2200_24 = new int[]{3520, 2200, 24};
    public static final int[] CAMERA_PREVIEW_4048_2530_15 = new int[]{4048, 2530, 15};//3k 15
    public static final int[] CAMERA_PREVIEW_4048_2530_7 = new int[]{4048, 2530, 14}; //8k 7fps
    public static final int[] CAMERA_PREVIEW_2880_1800_15 = new int[]{2880, 1800, 15};//6k 15fps
    public static final int[] CAMERA_PREVIEW_2192_1370_30 = new int[]{1920, 1200, 30};//4k 30fps
    public static final int[] CAMERA_PREVIEW_2192_1370_24 = new int[]{1920, 1200, 24};
    public static final int[] CAMERA_PREVIEW_1920_1200_30 = new int[]{1920, 1200, 30};//2k 30fps

    public static final int[] CAMERA_PREVIEW_512_320_30 = new int[]{512, 320, 30};
    public static final int[] CAMERA_PREVIEW_480_300_30 = new int[]{480, 300, 30};
    public static final int[] CAMERA_PREVIEW_448_280_22 = new int[]{448, 280, 20};
    public static final int[] CAMERA_PREVIEW_288_180_24 = new int[]{496, 310, 24};

    public static final int[] CAMERA_PREVIEW_400_250_24 = new int[]{400, 250, 24};

    public static final int HDR_PLUS_0 = 0;

    public static final int HDR_PLUS_1 = 1;

    public static final int HDR_PLUS_2 = 2;

    public static final int HDR_PLUS_3 = 3;

    private static PilotSDK mSingle;

    private CameraSurfaceView mCameraSurfaceView;

    private PanoSDKListener mPanoSDKListener;

    private Activity mActivity;

    /**
     * Instantiate PanoSDK, You can when the Activity is created.
     * The preview screen will be loaded into the provided view container.
     *
     * @param activity        loaded activity
     * @param parentView      preview view container
     * @param panoSDKListener callback
     */
    public PilotSDK(Activity activity, ViewGroup parentView, PanoSDKListener panoSDKListener) {
        mPanoSDKListener = panoSDKListener;
        mActivity = activity;

        makeDefaultWatermark(activity);

        mCameraSurfaceView = new CameraSurfaceView(activity, null);

        mCameraSurfaceView.setOnPanoModeChangeListener(this);

        parentView.addView(mCameraSurfaceView);
    }

    /**
     * Check whether the watermark is configured, if not, the default watermark will be released
     */
    public static void makeDefaultWatermark(Context context) {
        File file = new File("/sdcard/Watermarks/setting");
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file.getPath());
                InputStream inputStream = context.getResources().openRawResource(R.raw.watermark);
                byte[] buffer = new byte[256];
                fileOutputStream.write(buffer);
                buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                fileOutputStream.write(buffer);
                fileOutputStream.flush();
                inputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean checkPanoSDKNotInit() {
        if (mSingle == null || mSingle.mCameraSurfaceView.mPiPano == null) {
            Log.e(TAG, "PilotSDK is not initialization, you have to call this function after onPanoSurfaceViewCreate");
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return true;
        }
        return false;
    }

    public static boolean isDestroy() {
        return mSingle == null || mSingle.mCameraSurfaceView.mPiPano == null;
    }

    /**
     * Set lens distortion correction mode
     * 0x1111: means that the four lenses are all deformed according to the spherical rectangular projection mode;
     * 0x3333：Indicates that the four lenses are all corrected in fisheye mode and arranged horizontally;
     * 0x5555,Means that the four lenses are all corrected according to the fisheye mode distortion, arranged in a grid
     *
     * @param mode correction mode
     */
    public static void setLensCorrectionMode(int mode) {
        if (mSingle != null) {
            mSingle.mCameraSurfaceView.setLensCorrectionMode(mode);
        }
    }

    public static final int STITCHING_SCHEME_INFINITY = 0;
    public static final int STITCHING_SCHEME_INDOOR = 1;

    /**
     * Set splicing scheme
     *
     * @param scheme {@link #STITCHING_SCHEME_INFINITY} infinity
     *               {@link #STITCHING_SCHEME_INDOOR} indoor
     */
    public static void setStitchingScheme(int scheme) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        switch (scheme) {
            case STITCHING_SCHEME_INFINITY:
                mSingle.mCameraSurfaceView.setStitchingDistance(-1);
                break;
            case STITCHING_SCHEME_INDOOR:
                mSingle.mCameraSurfaceView.setStitchingDistance(99.7f);
                break;
            default:
                mSingle.mCameraSurfaceView.setStitchingDistance(-1);
                break;
        }
    }

    /**
     * Set splicing distance
     *
     * @param distance The value range is -100~100, 0 is about 2m, 100 means infinity, -100 is about 0.5m
     */
    public static void setStitchingDistance(float distance, float max) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        if (distance < 0) {
            mSingle.mCameraSurfaceView.setStitchingDistance(distance / 100 * 5.0f);
        } else {
            mSingle.mCameraSurfaceView.setStitchingDistance(distance / 100 * max);
        }
    }

    /**
     * Get all the current cameras
     *
     * @return cameras, the array length should be {@link #CAMERA_COUNT}
     */
    public static Camera[] getCameras() {
        return mSingle.mCameraSurfaceView.getCameras();
    }

    /**
     * Set preview mode, the way the displays the panoramic picture.
     *
     * @param mode          0:asteroid; 1:immersion; 2:fish eye; 3:tiling; 5:plane mode
     * @param rotateDegree  Initial rotation angle, horizontal rotation in plane mode,
     *                      rotation around Y axis in spherical mode, range is 0~360
     * @param playAnimation Whether to play the switching animation during the switching process
     */
    public static void setPreviewMode(int mode, float rotateDegree, boolean playAnimation) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.setPreviewMode(mode, rotateDegree, playAnimation);
    }

    /**
     * Whether to enable/disable touch events
     *
     * @param enable enable touch events or not
     */
    public static void setEnableTouchEvent(boolean enable) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.setEnableTouchEvent(enable);
    }

    /**
     * Take photo
     *
     * @param width    photo width
     * @param height   photo height
     * @param listener callback event
     */
    public static void takePhoto(String filename, int width, int height, TakePhotoListener listener) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.takePhoto(filename, width, height, listener);
    }

    /**
     * Switch camera preview resolution and fps.
     * Photos, videos, and live broadcasts should be based on the correct and appropriate preview resolution.
     *
     * @param resolution resolution, its value should be set as needed.
     */
    public static void changeCameraResolution(int[] resolution, ChangeResolutionListener listener) {
        Log.i(TAG, "changeCameraResolution width: " + resolution[0] + " height: " + resolution[1] + " fps: " + resolution[2]);

        if (checkPanoSDKNotInit()) {
            return;
        }

        listener.mWidth = resolution[0];
        listener.mHeight = resolution[1];
        listener.mFps = resolution[2];
        mSingle.mCameraSurfaceView.mPiPano.changeCameraResolution(listener);
    }

    /**
     * Adjust exposure gain
     *
     * @param value The range is -4 to 4. -4 is the darkest, 4 is the brightest
     */
    public static void setExposureCompensation(int value) {
        Log.i(TAG, "setExposureCompensation value: " + value);

        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.mDefaultExposureCompenstation = value;
        mSingle.mCameraSurfaceView.setExposureCompensation(value);
    }

    /**
     * Adjust ISO
     *
     * @param value The range is 0~4, 0-auto 1-ISO50 2-ISO100 3-ISO200 4-ISO400 5-ISO800
     */
    public static void setISO(int value) {
        Log.i(TAG, "setISO value: " + value);

        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.mDefaultISO = value;
        mSingle.mCameraSurfaceView.setIOS(value);
    }

    /**
     * Adjust manual ISO
     */
    public static void setManualISO(int value) {
        Log.i(TAG, "set ManualISO value: " + value);

        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.setManualISO(value);
    }


    public static void drawPreviewFrame(int frame) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.drawPreviewFrame(frame);
    }

    /**
     * Adjust white balance
     *
     * @param value 0-auto, 1-incandescent, 2-fluorescent, 3-daylight, 4-cloudy-daylight
     */
    public static void setWhiteBalance(int value) {
        Log.i(TAG, "setWhiteBalance: " + value);
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.mDefaultWb = value;
        mSingle.mCameraSurfaceView.setWhiteBalance(value);
    }

    public static void setAutoWhiteBalanceLock(boolean value) {
        Log.i(TAG, "setAutoWhiteBalanceLock: " + value);
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.setAutoWhiteBalanceLock(value);
    }

    private static String mCurrentVideoFilePath = "";

    private static MediaRecorderUtil mMediaRecorderUtil;

    /**
     * Start recording, if the current preview resolution is greater than 1000,
     * then a real-time stitched mp4 will be recorded, otherwise 4 original fisheye mp4 will be recorded
     *
     * @param dirPath         storage folder
     * @param filename        Real-time stitching of mp4 file names or four fisheye mp4 file names without extension
     * @param codec           encoding
     * @param channelCount    channel count
     * @param videoWidth      Real-time stitching of mp4 image width, this value is meaningless for recording 4 fisheye mp4
     * @param useForGoogleMap Whether real-time stitching is used for google map, this value is meaningless for recording 4 fisheye mp4
     * @param memomotionRatio Time-lapse photography magnification, if the current is 7fps, if the ratio is 70,
     *                        then the frame will be reduced to 7/70=0.1fps, that is, one frame is recorded in 10s,
     *                        which is only valid for real-time splicing video
     * @return Return 0 to start recording successfully
     */
    public static int startRecord(String dirPath, String filename, int codec, int channelCount, int videoWidth, boolean useForGoogleMap,
                                  int memomotionRatio, MediaRecorderListener listener) {
        if (checkPanoSDKNotInit()) {
            return 1;
        }

        int ret;
        boolean isLargePreviewSize;
        try {
            isLargePreviewSize = mSingle.mCameraSurfaceView.isLargePreviewSize();
        } catch (Exception e) {
            e.printStackTrace();
            return 11;
        }
        if (mMediaRecorderUtil != null) {
            mMediaRecorderUtil.stopRecord();
            mMediaRecorderUtil = null;
        }

        final Object mLock = new Object();
        //缩略图
        takePhoto(filename, 720, 360,
                new TakePhotoListener() {
                    @Override
                    protected void onTakePhotoComplete(int errorCode) {
                        synchronized (mLock) {
                            mLock.notifyAll();
                        }
                    }
                });
        synchronized (mLock) {
            try {
                mLock.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (isLargePreviewSize) {
            mCurrentVideoFilePath = (TextUtils.isEmpty(dirPath) ? "/sdcard/DCIM/Videos/Stitched/" : dirPath) + filename + ".mp4";
        } else {
            mCurrentVideoFilePath = (TextUtils.isEmpty(dirPath) ? "/sdcard/DCIM/Videos/Unstitched/" : dirPath) + filename + ".sti";
        }

        File file = new File(mCurrentVideoFilePath);
        if (!file.getParentFile().exists()) {
            file.mkdirs();
        }

        if (isLargePreviewSize) {
            int bitRate = videoWidth * videoWidth * 3;
            if (videoWidth == 7680 && useForGoogleMap) {
                bitRate *= 2.5f;
            } else {
                bitRate *= 2;
            }
            mMediaRecorderUtil = new MediaRecorderUtil();
            ret = mMediaRecorderUtil.startRecord(mCurrentVideoFilePath, PilotCodecConverter.convertVideoMime(codec),
                    videoWidth, videoWidth / 2, bitRate, useForGoogleMap, memomotionRatio, mSingle.mActivity, channelCount);
        } else {
            ret = mSingle.mCameraSurfaceView.startRecord(mCurrentVideoFilePath, listener, PilotCodecConverter.convertVideoEncode(codec), channelCount);
        }

        return ret;
    }

    /**
     * Stop record
     */
    public static void stopRecord(String firmware) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        if (mMediaRecorderUtil != null) {
            mMediaRecorderUtil.stopRecord();
            mMediaRecorderUtil = null;
            if (!TextUtils.isEmpty(mCurrentVideoFilePath) &&
                    mCurrentVideoFilePath.toLowerCase().endsWith(".mp4")) {
                mSingle.mCameraSurfaceView.mPiPano.spatialMediaImpl(mCurrentVideoFilePath, false, firmware);
            }
        } else {
            mSingle.mCameraSurfaceView.stopRecord(firmware);
        }
    }

    /**
     * Get the file path currently being recorded
     */
    public static String getCurrentVideoFilePath() {
        return mCurrentVideoFilePath;
    }

    /**
     * Whether to use a gyroscope, turn on the gyroscope during playback, you can refer to it and behold,
     * it can be used for PilotSteady when recording
     *
     * @param open open 是否打开陀螺仪
     */
    public static void useGyroscope(boolean open) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "useGyroscope value: " + open);
        mSingle.mCameraSurfaceView.useGyroscope(open);
    }

    /**
     * Time stamp offset between camera image and gyroscope
     *
     * @param timeOffset Timestamp offset, in nanoseconds, the recommended range is 5000000 to 25000000
     */
    public static void setGyroscopeTimestampOffset(long timeOffset) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.mPiPano.setGyroscopeTimestampOffset(timeOffset);
    }

    /**
     * Set gps information, gps information will be used to write the video
     */
    public static void setLocationInfo(Location location) {
        MediaRecorderUtil.setLocationInfo(location);
    }

    /**
     * Set the encoding surface used for recording.
     * This surface generally comes from MediaRecorder.getSurface() or MediaCodec.createInputSurface().
     * The SDK will render graphics to this surface and be encoded by MediaRecorder or MediaCodec.
     *
     * @param encodeInputSurface encoding surface
     */
    public static void setEncodeInputSurfaceForVideo(Surface encodeInputSurface) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.mPiPano.setSurface(encodeInputSurface, 1);
    }

    /**
     * Set the encoding surface used for live broadcast.
     * This surface usually comes from MediaRecorder.getSurface() or MediaCodec.createInputSurface().
     * The SDK will render graphics to this surface and be encoded by MediaRecorder or MediaCodec.
     *
     * @param encodeInputSurface encoding surface
     */
    public static void setEncodeInputSurfaceForLive(Surface encodeInputSurface) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.mPiPano.setSurface(encodeInputSurface, 2);
    }

    /**
     * Whether to lock yaw when turning on the rotation lock
     *
     * @param lock Whether to lock yaw
     */
    public static void lockYaw(boolean lock) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setYaw value: " + lock);
        mSingle.mCameraSurfaceView.mPiPano.lockYaw(lock);
    }

    /**
     * Set the inverted state, if the anti-shake is turned on, the inverted effect is invalid.
     *
     * @param b Is it inverted
     */
    public static void setUpsideDown(boolean b) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setUpsideDown: " + b);
        mSingle.mCameraSurfaceView.mPiPano.setUpsideDown(b);
    }

    /**
     * Get the current SDK version
     */
    public static String getVersion() {
        return "1.2." + PiPano.getBuildNumber();
    }

    /**
     * Set the firmware version number
     */
    public static void setFirmware(String firmware) {
        PiPano.setFirmware(firmware);
    }

    /**
     * Get whether the jpg file is a file after optical flow stitching
     *
     * @param filename jpg filename
     * @return true: file is optical flow stitching
     */
    public static boolean getUseOpticalFlow(String filename) {
        final File file = new File(filename);
        if (!file.exists()) {
            Log.e(TAG, "getUseOpticalFlow filename is not exists:" + filename);
            return false;
        }
        if (!filename.toLowerCase().contains(".jpg")) {
            return false;
        }
        return PiPano.getUseOpticalFlow(filename);
    }

    /**
     * Whether it is a google street view video, the judgment criterion is whether it contains camm track
     *
     * @param filename video filename
     * @return true: file is google street view video
     */
    public static boolean isGoogleStreetViewVideo(String filename) {
        return PiPano.isGoogleStreetViewVideo(filename);
    }

    public static void removePreviewCallBack() {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.removePreviewCallBack();
    }

    public static void setPreviewCallback(CameraPreviewCallback callback) {
        if (checkPanoSDKNotInit()) {
            return;
        }

        mSingle.mCameraSurfaceView.setPreviewCallback(callback);
    }

    public static void setCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.setCameraFixShakeListener(cameraFixShakeListener);
    }

    @Override
    public void onPanoSurfaceViewCreate() {
        mSingle = this;
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onSDKCreate();
        }
    }

    @Override
    public void onMediaPlayerCreate(MediaPlayer mediaPlayer) {
    }

    @Override
    public void onMediaPlayerRelease() {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onSDKRelease();
        }
    }

    @Override
    public void onPanoModeChange(int mode) {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onChangePanoMode(mode);
        }
    }

    @Override
    public void onSingleTapConfirmed() {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onSingleTapConfirmed();
        }
    }

    @Override
    public void onEncodeFrame(int count) {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onEncodeFrame(count);
        }
    }

    /**
     * save image
     */
    public static void saveJpg(String filename,
                               String alpath,
                               ByteBuffer buffer,
                               int width,
                               int height) {
        PiPano.saveJpeg(filename,
                alpath, false, buffer,
                width, height, width,
                100, false,
                0, 0, 0, 0,
                -1, "ljl", false);
    }

    public static final int DEVICE_MODEL_PILOT_ERA = 0;
    public static final int DEVICE_MODEL_PILOT_ONE = 1;

    /**
     * Set device type
     *
     * @param model {@link #DEVICE_MODEL_PILOT_ERA} or {@link #DEVICE_MODEL_PILOT_ONE}
     */
    public static void setDeviceModel(int model) {
        PiPano.setDeviceModel(model);
    }
}
