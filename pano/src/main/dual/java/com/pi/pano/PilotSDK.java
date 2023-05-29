package com.pi.pano;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.location.Location;
import android.media.Image;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pi.pano.annotation.PiAntiMode;
import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiFileStitchFlag;
import com.pi.pano.annotation.PiIso;
import com.pi.pano.annotation.PiPreviewMode;
import com.pi.pano.annotation.PiVideoEncode;
import com.pi.pano.annotation.PiWhiteBalance;
import com.pi.pano.wrap.config.FileConfig;
import com.pi.pano.wrap.config.FileConfigHelper;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Panorama related SDK, can be used for panorama expansion, stitching, preview, photo,
 * set video live interface, insert mp4 panorama information, etc.
 */
public class PilotSDK implements PanoSurfaceViewListener {
    private static final String TAG = "PilotSDK";

    /**
     * Number of modules for panoramic cameras.
     */
    public static final int CAMERA_COUNT = 1;

    public static final int[] CAMERA_PREVIEW_5760_2880_7 = new int[]{5760, 2880, 7};
    public static final int[] CAMERA_PREVIEW_5760_2880_14 = new int[]{5760, 2880, 14};
    public static final int[] CAMERA_PREVIEW_5760_2880_15 = new int[]{5760, 2880, 15};
    public static final int[] CAMERA_PREVIEW_5760_2880_16 = new int[]{5760, 2880, 16};
    public static final int[] CAMERA_PREVIEW_5760_2880_30 = new int[]{5760, 2880, 30};
    public static final int[] CAMERA_PREVIEW_5760_2880_24 = new int[]{5760, 2880, 24};
    public static final int[] CAMERA_PREVIEW_3840_1920_30 = new int[]{3840, 1920, 30};
    public static final int[] CAMERA_PREVIEW_3840_1920_60 = new int[]{3840, 1920, 60};
    public static final int[] CAMERA_PREVIEW_2560_1280_30 = new int[]{2560, 1280, 30};
    public static final int[] CAMERA_PREVIEW_2560_1280_60 = new int[]{2560, 1280, 60};
    public static final int[] CAMERA_PREVIEW_1920_960_30 = new int[]{1920, 960, 30};
    public static final int[] CAMERA_PREVIEW_1920_960_60 = new int[]{1920, 960, 60};
    public static final int[] CAMERA_PREVIEW_1920_960_100 = new int[]{1920, 960, 100};
    public static final int[] CAMERA_PREVIEW_640_320_24 = new int[]{640, 320, 24};
    public static final int[] CAMERA_PREVIEW_640_320_25 = new int[]{640, 320, 25};
    public static final int[] CAMERA_PREVIEW_640_320_30 = new int[]{640, 320, 30};
    public static final int[] CAMERA_PREVIEW_640_320_60 = new int[]{640, 320, 60};
    public static final int[] CAMERA_PREVIEW_640_320_90 = new int[]{640, 320, 90};
    public static final int[] CAMERA_PREVIEW_640_320_100 = new int[]{640, 320, 100};
    public static final int[] CAMERA_PREVIEW_640_320_110 = new int[]{640, 320, 110};
    public static final int[] CAMERA_PREVIEW_640_320_120 = new int[]{640, 320, 120};

    public static final int[] CAMERA_PREVIEW_3040_3040_24 = new int[]{3040, 3040, 24};
    public static final int[] CAMERA_PREVIEW_3040_3040_25 = new int[]{3040, 3040, 25};
    public static final int[] CAMERA_PREVIEW_3040_3040_30 = new int[]{3040, 3040, 30};
    public static final int[] CAMERA_PREVIEW_3040_3040_60 = new int[]{3040, 3040, 60};

    @SuppressLint("StaticFieldLeak")
    static PilotSDK mSingle;

    final CameraSurfaceView mCameraSurfaceView;

    private final PanoSDKListener mPanoSDKListener;

    private final Context mContext;

    /**
     * Instantiate PanoSDK when Activity onCreate
     *
     * @param parentView      PanoSDK contains a PreviewView, which is the parent view of the PreviewView.
     * @param panoSDKListener Callback interface of PanoSDK
     */
    public PilotSDK(ViewGroup parentView, PanoSDKListener panoSDKListener) {
        this(parentView, false, panoSDKListener);
    }

    /**
     * Instantiate PanoSDK when Activity onCreate
     *
     * @param parentView      PanoSDK contains a PreviewView, which is the parent view of the PreviewView.
     * @param lensProtectedEnable Whether with lens protection.
     * @param panoSDKListener Callback interface of PanoSDK
     */
    public PilotSDK(ViewGroup parentView, boolean lensProtectedEnable, PanoSDKListener panoSDKListener) {
        mContext = parentView.getContext();
        mPanoSDKListener = panoSDKListener;
        mCameraSurfaceView = new CameraSurfaceView(mContext, null);
        mCameraSurfaceView.initLensProtected(lensProtectedEnable);
        mCameraSurfaceView.setOnPanoModeChangeListener(this);
        parentView.addView(mCameraSurfaceView);
    }

    private static boolean checkPanoSDKNotInit() {
        if (mSingle == null || mSingle.mCameraSurfaceView.mPiPano == null) {
            Log.e(TAG, "PilotSDK is not initialization, you have to call this function after onPanoSurfaceViewCreate");
            return true;
        }
        return false;
    }

    public static boolean isDestroy() {
        return mSingle == null || mSingle.mCameraSurfaceView.mPiPano == null;
    }


    /**
     * Set lens distortion correction mode.
     * Generally set to 0x1111, which means that all four lenses are corrected for spherical rectangular projection mode distortion;
     * If set to 0x3333, it means that all four lenses are arranged horizontally according to the fisheye mode aberration correction;
     * If set to 0x5555, it means that all four lenses are corrected by fisheye mode aberration, field grid arrangement.
     *
     * @param mode 0x1-Spherical rectangular projection mode 0x3-Fisheye mode 0x9-vlog mode.
     */
    public static void setLensCorrectionMode(int mode) {
        if (mSingle != null) {
            Log.i(TAG, "setLensCorrectionMode mode: 0x" + Integer.toHexString(mode));
            mSingle.mCameraSurfaceView.setLensCorrectionMode(mode);
        }
    }

    /**
     * reload watermark
     */
    public static void reloadWatermark(boolean show) {
        if (mSingle != null) {
            mSingle.mCameraSurfaceView.mPiPano.reloadWatermark(show);
        }
    }

    public static Map<String, Object> getPreviewParam() {
        if (checkPanoSDKNotInit()) {
            return null;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("cameraId", mSingle.mCameraSurfaceView.getCameraId());
        param.put("previewWidth", mSingle.mCameraSurfaceView.getPreviewWidth());
        param.put("previewHeight", mSingle.mCameraSurfaceView.getPreviewHeight());
        param.put("previewFps", mSingle.mCameraSurfaceView.getPreviewFps());
        //
        param.put("exposeTime", mSingle.mCameraSurfaceView.mExposeTime);
        param.put("iso", mSingle.mCameraSurfaceView.mDefaultISO);
        param.put("exposure-compensation", mSingle.mCameraSurfaceView.mDefaultExposureCompensation);
        param.put("whitebalance", mSingle.mCameraSurfaceView.mDefaultWb);
        param.put("antiMode", mSingle.mCameraSurfaceView.mDefaultAntiMode);
        Log.d(TAG, "getPreviewParam," + param);
        return param;
    }

    public static void setPreviewParam(@Nullable Map<String, Object> param, @Nullable IPreviewParamListener listener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.d(TAG, "setPreviewParam," + param);
        if (null != param) {
            String cameraId = (String) param.get("cameraId");
            Object previewWidth = param.get("previewWidth");
            Object previewHeight = param.get("previewHeight");
            Object previewFps = param.get("previewFps");
            if (null != previewWidth && null != previewHeight && null != previewFps) {
                ChangeResolutionListener listener1 = new ChangeResolutionListener() {
                    @Override
                    protected void onChangeResolution(int width, int height) {
                        setPreviewParam(param);
                        if (null != listener) {
                            mSingle.mCameraSurfaceView.post(listener::onPreviewParamFinish);
                        }
                    }
                };
                listener1.fillParams(cameraId, (int) previewWidth, (int) previewHeight, (int) previewFps);
                changeCameraResolution(false, listener1);
                return;
            } else {
                setPreviewParam(param);
                if (null != listener) {
                    mSingle.mCameraSurfaceView.post(listener::onPreviewParamFinish);
                }
            }
        }
        if (null != listener) mSingle.mCameraSurfaceView.post(listener::onPreviewParamFinish);
    }

    public static void setPreviewParam(@Nullable Map<String, Object> param) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        if (null == param || param.isEmpty()) {
            return;
        }
        Object exposeTime = param.get("exposeTime");
        if (exposeTime != null) {
            setExposeTime((Integer) exposeTime);
        }
        Object iso = param.get("iso");
        if (iso != null) {
            setISO((Integer) iso);
        }
        Object ev = param.get("exposure-compensation");
        if (ev != null) {
            setExposureCompensation((Integer) ev);
        }
        Object wb = param.get("whitebalance");
        if (wb != null) {
            setWhiteBalance((String) wb);
        }
    }

    /**
     * Preview Reconciliation Expected Completion Timestamp
     */
    private static long sPreviewExpectChangeFinishTimestamp = 0;

    private static void markPreviewExpectChange(long costTime) {
        sPreviewExpectChangeFinishTimestamp = Math.max(sPreviewExpectChangeFinishTimestamp, SystemClock.elapsedRealtime() + costTime);
    }

    private static long getAdjustRemainTime() {
        long diff = sPreviewExpectChangeFinishTimestamp - SystemClock.elapsedRealtime();
        if (diff > 0) {
            return diff;
        }
        return 0;
    }

    /**
     * Get the wait time for the preview adjustment to complete.
     */
    public static long obtainAdjustWaitTime() {
        return Math.max(ExposeTimeAdjustHelper.getAdjustRemainTime(), PilotSDK.getAdjustRemainTime());
    }

    /**
     * Set stitch distance
     *
     * @param distance -100~100 0 represents the distance during calibration, about 2m; 100 represents infinity; - 100 is about 0.5m.
     * @param max      max distance value
     */
    public static void setStitchingDistance(float distance, float max) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.d(TAG, "setStitchingDistance,distance:" + distance + ",max:" + max);
        if (distance < 0) {
            mSingle.mCameraSurfaceView.setStitchingDistance(distance / 100 * 5.0f);
        } else {
            mSingle.mCameraSurfaceView.setStitchingDistance(distance / 100 * max);
        }
    }

    /**
     * Set preview mode.
     *
     * @param mode           {@link PiPreviewMode}
     * @param rotateDegree   0~360 Initial rotation angle, horizontal rotation in plane mode, and rotation around Y axis in spherical mode.
     * @param playAnimation  Whether to play the switching animation when switching the display mode.
     * @param fov            0~180 Initial longitudinal fov.
     * @param cameraDistance 0~400 Distance from camera to ball center.
     */
    public static void setPreviewMode(@PiPreviewMode int mode, float rotateDegree, boolean playAnimation, float fov, float cameraDistance) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setPreviewMode,mode:" + mode + ",fov:" + fov + ",cameraDistance:" + cameraDistance);
        mSingle.mCameraSurfaceView.setPreviewMode(mode, rotateDegree, playAnimation, fov, cameraDistance);
    }

    /**
     * Whether to enable/disable touch events
     *
     * @param b true: turn on; false:turn off
     */
    public static void setEnableTouchEvent(boolean b) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.setEnableTouchEvent(b);
    }

    private static void takeThumbPhoto(String filename) {
        PiPano.clearExifThumbnailBuffer();
        final Object mLock = new Object();
        TakePhotoListener takePhotoListener = new TakePhotoListener() {
            @Override
            protected void onTakePhotoComplete(int errorCode) {
                synchronized (mLock) {
                    mLock.notifyAll();
                }
            }
        };
        takePhotoListener.mParams = CaptureParams.Factory.createParamsForThumb(filename);
        takePhotoListener.mIsStitched = true;
        takePhotoListener.mIsThumb = true;
        takePhotoListener.mSaveExif = false;
        mSingle.mCameraSurfaceView.takePhoto(filename, 720, 360, takePhotoListener);
        synchronized (mLock) {
            try {
                mLock.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * take photo
     *
     * @param width     photo width
     * @param height    photo height
     * @param takeThumb take thumb photo
     * @param listener  listener
     */
    public static void takePhoto(String filename, int width, int height, boolean takeThumb, TakePhotoListener listener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.takePhoto(filename, width, height, listener);
    }

    /**
     * Combine multiple fisheye images into one fisheye image and expand the stitching, used to remove the silhouette or split photos.
     *
     * @param unstitchFilename unstitch file path
     * @param fisheyeFilenames A set of fisheye charts for merging
     * @param mask             The mask of the merging method, such as 0x0112, indicates the four fisheyes of the merged fisheye image, from the left to the right fisheyes are from fisheyeFilenames 0, 1, 2, 2.
     * @return 0:no error
     */
    public static int makePanoWithMultiFisheye(@NonNull String unstitchFilename, String[] fisheyeFilenames, int mask) {
        return PiPano.makePanoWithMultiFisheye(unstitchFilename, fisheyeFilenames, mask, 2);
    }

    /**
     * Marking points in slam.
     *
     * @param filename file name
     */
    public static void makeSlamPhotoPoint(String filename) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.mPiPano.makeSlamPhotoPoint(filename);
    }

    /**
     * Add thumbnails to the jpeg file, the size of the thumbnails should not exceed 60000 bytes,
     * if the original jpeg file has thumbnails, then replace it.
     *
     * @param jpegFilename  To add thumbnails to a jpeg file.
     * @param thumbFilename The thumbnail file to be added or replaced, in jpeg format, cannot exceed 60,000 bytes.
     * @return 0:no error
     */
    public static int injectThumbnail(String jpegFilename, String thumbFilename) {
        return PiPano.injectThumbnail(jpegFilename, thumbFilename);
    }

    public static int injectThumbnail(File jpegFile, File thumbFile) {
        return injectThumbnail(jpegFile.getAbsolutePath(), thumbFile.getAbsolutePath());
    }

    /**
     * Switching the camera preview resolution
     */
    public static boolean changeCameraResolution(boolean force, ChangeResolutionListener listener) {
        if (checkPanoSDKNotInit()) {
            return false;
        }
        listener.forceChange = force;
        Log.i(TAG, "change camera resolution camera params: " + listener.toParamsString());
        if (!force) {
            if (listener.mWidth == mSingle.mCameraSurfaceView.getPreviewWidth()
                    && listener.mHeight == mSingle.mCameraSurfaceView.getPreviewHeight()
                    && listener.mFps == mSingle.mCameraSurfaceView.getPreviewFps()
                    && (Objects.equals(listener.mCameraId, mSingle.mCameraSurfaceView.getCameraId()))) {
                Log.d(TAG, "change camera resolution not change,ignore!");
                mSingle.mCameraSurfaceView.post(() -> listener.onChangeResolution(listener.mWidth, listener.mHeight));
                return false;
            }
        }
        mSingle.mCameraSurfaceView.mPiPano.changeCameraResolution(listener);
        return true;
    }

    /**
     * @return resolution info
     */
    public static int[] getCameraResolution() {
        if (checkPanoSDKNotInit()) {
            return null;
        }
        String cameraId = mSingle.mCameraSurfaceView.getCameraId();
        return new int[]{
                mSingle.mCameraSurfaceView.getPreviewWidth(),
                mSingle.mCameraSurfaceView.getPreviewHeight(),
                mSingle.mCameraSurfaceView.getPreviewFps(),
                null != cameraId ? Integer.parseInt(cameraId) : -1
        };
    }

    /**
     * Set exposure compensation.
     *
     * @param value exposure compensation
     */
    public static void setExposureCompensation(@PiExposureCompensation int value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setExposureCompensation:" + value);
        mSingle.mCameraSurfaceView.mDefaultExposureCompensation = value;
        mSingle.mCameraSurfaceView.setExposureCompensation(value);
    }

    /**
     * Set expose time.
     *
     * @param value expose time
     */
    public static void setExposeTime(@PiExposureTime int value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setExposeTime:" + value);
        mSingle.mCameraSurfaceView.mExposeTime = value;
        mSingle.mCameraSurfaceView.setExposeTime(value);
    }

    /**
     * set ISO.
     *
     * @param value iso
     */
    public static void setISO(@PiIso int value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setISO:" + value);
        mSingle.mCameraSurfaceView.mDefaultISO = value;
        mSingle.mCameraSurfaceView.setIOS(value);
    }

    /**
     * Set white balance
     *
     * @param value white balance
     */
    public static void setWhiteBalance(@PiWhiteBalance String value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setWhiteBalance:" + value);
        mSingle.mCameraSurfaceView.mDefaultWb = value;
        mSingle.mCameraSurfaceView.setWhiteBalance(value);
    }

    public static void setAutoWhiteBalanceLock(boolean value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setAutoWhiteBalanceLock:" + value);
        mSingle.mCameraSurfaceView.setAutoWhiteBalanceLock(value);
    }

    public static void setAntiMode(@PiAntiMode String value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setAntiMode:" + value);
        mSingle.mCameraSurfaceView.setAntiMode(value);
    }

    public static void setLockDefaultPreviewFps(boolean value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setLockDefaultPreviewFps:" + value);
        mSingle.mCameraSurfaceView.setLockDefaultPreviewFps(value);
    }

    /**
     * 预览时，需要准备的图像格式 ：支持 ImageFormat.JPEG , ImageFormat.RAW_SENSOR
     *
     * @param value {@link android.graphics.ImageFormat}
     */
    public static void setPreviewImageReaderFormat(int[] value) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.setPreviewImageReaderFormat(value);
    }

    private static String mCurrentVideoFilePath = "";
    private static RecordParams mRecordParams;

    private static MediaRecorderUtil mMediaRecorderUtil;

    private static MediaRecorderUtil mMediaRecorderUtilForThumb;

    /**
     * When the preview resolution is greater than 1000, recording real-time stitching video, otherwise recorded as a separate four video for post-processing.
     */
    public static boolean isLargePreviewSize() {
        if (checkPanoSDKNotInit()) {
            return true;
        }
        return mSingle.mCameraSurfaceView.isLargePreviewSize();
    }

    /**
     * Get the current shot set frame rate.
     */
    public static int getFps() {
        if (checkPanoSDKNotInit()) {
            return 0;
        }
        return mSingle.mCameraSurfaceView.getPreviewFps();
    }

    public static boolean isLockDefaultPreviewFps() {
        if (checkPanoSDKNotInit()) {
            return false;
        }
        return mSingle.mCameraSurfaceView.isLockDefaultPreviewFps();
    }

    public static CameraEnvParams getCurCameraEnvParams() {
        if (checkPanoSDKNotInit()) {
            return null;
        }
        return mSingle.mCameraSurfaceView.getCurCameraEnvParams();
    }

    public static int getPreviewWidth() {
        if (checkPanoSDKNotInit()) {
            return 0;
        }
        return mSingle.mCameraSurfaceView.getPreviewWidth();
    }

    /**
     * Start recording.
     * @return 0:no error
     */
    public static int startRecord(String dirPath, String filename,
                                  @PiVideoEncode String encode, int videoWidth, int videoHeight, int fps, int channelCount,
                                  boolean useForGoogleMap, int memomotionRatio,
                                  int bitrate) {
        RecordParams params = new RecordParams(new IRecordFilenameProxy() {
            @NonNull
            @Override
            public File getParentPath() {
                return new File(dirPath);
            }

            @NonNull
            @Override
            public String getBasicName() {
                return filename;
            }
        }, videoWidth, videoHeight, fps, bitrate, channelCount);
        params.encode = encode;
        params.useForGoogleMap = useForGoogleMap;
        params.memomotionRatio = memomotionRatio;
        return startRecord(params);
    }

    /**
     * Start recording.
     * @return 0:no error
     */
    public static int startRecord(RecordParams params) {
        if (checkPanoSDKNotInit()) {
            return 1;
        }
        CameraSurfaceView cameraSurfaceView = mSingle.mCameraSurfaceView;
        boolean isLargePreviewSize;
        try {
            isLargePreviewSize = cameraSurfaceView.isLargePreviewSize();
        } catch (Exception e) {
            e.printStackTrace();
            return 11;
        }
        if (null != mRecordParams) {
            stopRecord(false, false);
        }
        //takeThumbPhoto(filename);
        final File file;
        String videoFilePath;
        boolean isRecordFisheye = !isLargePreviewSize || (params.memomotionRatio != 0 && !params.useForGoogleMap);
        String dirPath = params.getDirPath();
        if (isRecordFisheye) {
            file = new File((TextUtils.isEmpty(dirPath) ? "/sdcard/DCIM/Videos/Unstitched/" : dirPath),
                    params.getBasicName() + PiFileStitchFlag.unstitch);
            if (!file.exists()) {
                file.mkdirs();
            }
            videoFilePath = file.getAbsolutePath() + "/0.mp4";
            cameraSurfaceView.mPiPano.setStabilizationFile(file.getAbsolutePath() + "/stabilization");
        } else {
            file = new File((TextUtils.isEmpty(dirPath) ? "/sdcard/DCIM/Videos/Stitched/" : dirPath),
                    params.getBasicName() + PiFileStitchFlag.stitch + ".mp4");
            if (!file.getParentFile().exists()) {
                file.mkdirs();
            }
            videoFilePath = file.getAbsolutePath();
        }
        mCurrentVideoFilePath = file.getAbsolutePath();
        mRecordParams = params;
        if (params.bitRate <= 0) {
            params.bitRate = params.videoWidth * params.videoHeight * 3 * 2;
        }

        if ("2".equals(mSingle.mCameraSurfaceView.getCameraId())) { // 全景视频
            PiPano.setMemomotionRatio(params.memomotionRatio, params.useForGoogleMap);
        } else {
            PiPano.setMemomotionRatio(params.memomotionRatio, true);
        }

        int previewFps = cameraSurfaceView.getPreviewFps();
        if (params.fps < 1) {
            params.fps = previewFps;
        }
        if (!params.useForGoogleMap) {
            cameraSurfaceView.mPiPano.setParamReCaliEnable(0, true);
        }

        mMediaRecorderUtil = new MediaRecorderUtil();
        mMediaRecorderUtil.setAudioEncoderExt(params.audioEncoderExtList);
        mMediaRecorderUtil.setAudioRecordExt(params.audioRecordExt);
        Surface surface = mMediaRecorderUtil.startRecord(mSingle.mContext, videoFilePath,
                EncodeConverter.convertVideoMime(params.encode),
                params.videoWidth, params.videoHeight, params.fps, params.bitRate,
                params.useForGoogleMap, params.memomotionRatio, params.channelCount, previewFps);
        if (surface == null) {
            return 2;
        }
        if (params.saveConfig) {
            FileConfig config = FileConfigHelper.self().create(mCurrentVideoFilePath, params);
            if (config != null) {
                config.setFittings(cameraSurfaceView.mLensProtected ? 2 : 1);
                config.setFieldOfView((int) cameraSurfaceView.getFov());
                FileConfigHelper.self().saveConfig(config);
            }
        }
        Surface thumbSurface = null;
        boolean previewEnabled = previewFps < 100; // 是否可开启预览
        if (isRecordFisheye && previewEnabled) {
            mMediaRecorderUtilForThumb = new MediaRecorderUtil();
            thumbSurface = mMediaRecorderUtilForThumb.startRecord(mSingle.mContext,
                    file.getAbsolutePath() + "/preview.mp4", MediaFormat.MIMETYPE_VIDEO_AVC,
                    640, 320, params.fps, 640 * 320 * 2,
                    false, params.memomotionRatio, 0, previewFps);
        }
        if (isLargePreviewSize) {
            cameraSurfaceView.mPiPano.setEncodeSurface(surface, 1);
        } else {
            mSingle.mCameraSurfaceView.startLowPreviewSize(false);
            mSingle.mCameraSurfaceView.startCapture(surface, previewEnabled);
        }
        if (thumbSurface != null) {
            cameraSurfaceView.mPiPano.setEncodeSurface(thumbSurface, 4);
        }
        return 0;
    }

    /**
     * stop recording
     */
    public static void stopRecord(boolean injectPanoMetadata, boolean continuous) {
        if (!checkPanoSDKNotInit()) {
            PiPano.setMemomotionRatio(0, false);
            mSingle.mCameraSurfaceView.mPiPano.setEncodeSurface(null, 1);
            mSingle.mCameraSurfaceView.mPiPano.setEncodeSurface(null, 4);
            if (!mSingle.mCameraSurfaceView.isLargePreviewSize()) {
                if (injectPanoMetadata) {
                    if (!continuous) {
                        //不是分段视频停止，才更改为高清预览
                        mSingle.mCameraSurfaceView.startHighPreviewSize(false);
                    }
                    mSingle.mCameraSurfaceView.stopCapture(!continuous);
                }
            }
        }
        if (null != mRecordParams) {
            boolean isPano = "2:1".equals(mRecordParams.aspectRatio);
            String versionName = mRecordParams.mVersionName;
            String artist = mRecordParams.mArtist;
            if (mMediaRecorderUtil != null) {
                mMediaRecorderUtil.stopRecord(injectPanoMetadata, isPano, versionName, artist);
            }
            if (mMediaRecorderUtilForThumb != null) {
                mMediaRecorderUtilForThumb.stopRecord(false, isPano, versionName, artist);
                PiPano.spatialMediaFromOldMp4(mMediaRecorderUtil.getFilename(), mMediaRecorderUtilForThumb.getFilename(), isPano);
            }
            mMediaRecorderUtil = null;
            mMediaRecorderUtilForThumb = null;
            mRecordParams = null;
        }
        if (!checkPanoSDKNotInit()) {
            mSingle.mCameraSurfaceView.mPiPano.setStabilizationFile(null);
        }
    }

    /**
     * @return true: in record
     */
    public static boolean isInRecord() {
        if (mRecordParams != null) {
            return true;
        }
        return mSingle.mCameraSurfaceView.isInCapture();
    }

    public static int startCapture(Surface surface) {
        if (checkPanoSDKNotInit()) {
            return 1;
        }
        return mSingle.mCameraSurfaceView.startCapture(surface, true);
    }

    public static void stopCapture() {
        stopCapture(true);
    }

    public static void stopCapture(boolean reStartPreview) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.stopCapture(true, reStartPreview);
    }

    /**
     * Get the path of the file currently being recorded
     *
     * @return the path of the file currently being recorded
     */
    public static String getCurrentVideoFilePath() {
        return mCurrentVideoFilePath;
    }

    /**
     * Whether gyroscope is used,
     * Gyroscope for Steady.
     *
     * @param open true: use gyroscope
     */
    public static void useGyroscope(boolean open) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "useGyroscope:" + open);
        mSingle.mCameraSurfaceView.useGyroscope(open);
    }

    /**
     * Timestamp offset between camera image and gyroscope.
     *
     * @param timestampOffset        Timestamp offset in nanoseconds, recommended range 5000000 to 25000000.
     * @param rollingShutterDuration Half the time of the roll-up shutter from the first line to the last line.
     */
    public static void setStabilizationTimeOffset(long timestampOffset, long rollingShutterDuration) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.mPiPano.nativeStabSetTimeoffset(timestampOffset, rollingShutterDuration);
    }

    /**
     * Set gps information,gps information will be used to write video.
     */
    public static void setLocationInfo(Location location) {
        MediaRecorderUtil.setLocationInfo(location);
    }

    /**
     * Set the encoding surface for recording.
     * This surface is generally from MediaRecorder. getSurface() or MediaCodec. createInputSurface().
     * The SDK will render graphics on this surface and encode them by MediaRecorder or MediaCodec.
     *
     * @param encodeInputSurface Generally from MediaRecorder. getSurface() or MediaCodec. createInputSurface().
     */
    public static void setEncodeInputSurfaceForVideo(Surface encodeInputSurface) {
        setEncodeSurface(encodeInputSurface, 1);
    }

    /**
     * Set the encoding surface for live broadcast.
     *
     * @param encodeInputSurface Generally from MediaRecorder. getSurface() or MediaCodec. createInputSurface()
     */
    public static void setEncodeInputSurfaceForLive(Surface encodeInputSurface) {
        setEncodeSurface(encodeInputSurface, 2);
    }

    /**
     * Set the encoding surface for live screen broadcast.
     *
     * @param encodeInputSurface Generally from MediaRecorder. getSurface() or MediaCodec. createInputSurface()
     */
    public static void setEncodeInputSurfaceForScreenLive(Surface encodeInputSurface) {
        setEncodeSurface(encodeInputSurface, 5);
    }

    public static void setEncodeSurface(Surface surface, int type) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.mPiPano.setEncodeSurface(surface, type);
    }

    /**
     * Whether to lock the axis when turning on the rotation lock.
     *
     * @param yaw   whether to lock yaw
     * @param pitch whether to lock pitch
     * @param roll  whether to lock roll
     * @param lerp  00~1 When an axis is not followed, this value represents the tightness coefficient of the lens, and the greater the value, the faster the following.
     */
    public static void stabSetLockAxis(boolean yaw, boolean pitch, boolean roll, float lerp) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "stabSetLockAxis:" + yaw + "," + pitch + "," + roll + "," + lerp);
        mSingle.mCameraSurfaceView.mPiPano.nativeStabSetLockAxis(yaw, pitch, roll, lerp);
    }

    /**
     * Set the handstand status. If anti shake is enabled, the handstand effect is invalid.
     *
     * @param b true: handstand
     */
    public static void setUpsideDown(boolean b) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setUpsideDown:" + b);
        mSingle.mCameraSurfaceView.mPiPano.setUpsideDown(b);
    }

    /**
     * Set the slam switch
     *
     * @param enable                  Whether to turn on/off slam
     * @param lenForCalMeasuringScale Distance used to calculate the scale
     * @param listener                listener
     */
    public static void setSlamEnable(boolean enable, float lenForCalMeasuringScale, boolean useForImuToPanoRotation, SlamListener listener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setSlamEnable:" + enable);
        if (enable) {
            listener.lenForCalMeasuringScale = lenForCalMeasuringScale;
            listener.assetManager = mSingle.mContext.getAssets();
            listener.showPreview = useForImuToPanoRotation;
            listener.useForImuToPanoRotation = useForImuToPanoRotation;
            mSingle.mCameraSurfaceView.mPiPano.slamStart(listener);
        } else {
            mSingle.mCameraSurfaceView.mPiPano.slamStop();
        }
    }

    /**
     * slam pause
     */
    public static void slamPause() {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "slamPause");
        mSingle.mCameraSurfaceView.mPiPano.slamPause();
    }

    /**
     * slam resume
     */
    public static void slamResume() {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "slamResume");
        mSingle.mCameraSurfaceView.mPiPano.slamResume();
    }

    public static void detectionStart(OnAIDetectionListener listener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "detectionStart");
        mSingle.mCameraSurfaceView.mPiPano.setDetectionEnabled(true, listener);
    }

    public static void detectionStop() {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "detectionStop");
        mSingle.mCameraSurfaceView.mPiPano.setDetectionEnabled(false, null);
    }

    /**
     * Whether to use lens protection.
     */
    public static void setLensProtected(boolean enabled) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        Log.i(TAG, "setLensProtected:" + enabled);
        mSingle.mCameraSurfaceView.setLensProtected(enabled);
    }

    /**
     * Enable or disable the recalculation of parameters.
     *
     * @param executionInterval    >0 calculated splicing interval 0 closed calculation<0 calculated once.
     * @param effectiveImmediately true: it takes effect immediately after calculation, which is used to save performance in live broadcast;
     *                             false: it takes effect gradually after calculation, and the splicing distance changes smoothly.
     */
    public static void setParamReCaliEnable(int executionInterval, boolean effectiveImmediately) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.mPiPano.setParamReCaliEnable(executionInterval, effectiveImmediately);
    }

    /**
     * Whether it is Google Street View video, and whether it includes cam track.
     *
     * @param filename file path
     * @return true: google street view video
     */
    public static boolean isGoogleStreetViewVideo(String filename) {
        return PiPano.isGoogleStreetViewVideo(filename);
    }

    /**
     * get calibration parameters
     *
     * @return calibration parameters
     */
    public static String getParam() {
        return PiPano.getParam();
    }

    public static void removePreviewCallBack() {
    }

    public static void setPreviewCallback(CameraPreviewCallback callback) {
    }

    public static void setCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.setCameraFixShakeListener(cameraFixShakeListener);
    }

    public static void addCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.addCameraFixShakeListener(cameraFixShakeListener);
    }

    public static void removeCameraFixShakeListener(CameraFixShakeListener cameraFixShakeListener) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.removeCameraFixShakeListener(cameraFixShakeListener);
    }

    /**
     *
     */
    public static Camera[] getCameras() {
        // TODO 已不适用
        return null;
    }

    ////////////////////////////////////////////////////////////////////
    //SDK to Android
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onPanoSurfaceViewCreate() {
        mSingle = this;
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onPanoCreate();
        }
    }

    @Override
    public void onPanoSurfaceViewRelease() {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onPanoRelease();
        }
        mSingle = null;
    }

    @Override
    public void onPanoModeChange(int mode) {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onChangePreviewMode(mode);
        }
    }

    @Override
    public void onSingleTapConfirmed() {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onSingleTap();
        }
    }

    @Override
    public void onEncodeFrame(int count) {
        if (mPanoSDKListener != null) {
            mPanoSDKListener.onEncodeFrame(count);
        }
    }

    /**
     * Get the current SDK version
     *
     * @return SDK version
     */
    public static String getVersion() {
        return "1.2." + PiPano.getBuildNumber();
    }

    /**
     * Set firmware version name
     *
     * @param firmwareVersion firmware version name,The maximum length is 9 bytes.
     */
    public static void setFirmware(String firmwareVersion) {
        PiPano.setFirmware(firmwareVersion);
    }

    public static final int DEVICE_MODEL_PILOT_ERA = 0;
    public static final int DEVICE_MODEL_PILOT_ONE = 1;
    // 广角设备
    public static final int DEVICE_MODEL_PILOT_WIDE = 2;

    @IntDef({
            DEVICE_MODEL_PILOT_ERA,
            DEVICE_MODEL_PILOT_ONE,
            DEVICE_MODEL_PILOT_WIDE
    })
    public @interface PanoDeviceModel {
    }

    /**
     * Set Device Type
     *
     * @param model Device Type
     */
    public static void setDeviceModel(@PanoDeviceModel int model) {
        PiPano.setDeviceModel(model);
    }

    /**
     * Set the debug log path
     *
     * @param dirPath log file dir path
     * @param debug   Whether to debug
     */
    public static void setDev(String dirPath, boolean debug) {
        PiPano.sDebug = debug;
        // 文件名称固定为pilotSDK.log
        String logFilePath = new File(dirPath, "pilotSDK.log").getAbsolutePath();
        Log.d(TAG, "logFilePath:" + logFilePath);
        PiPano.setLogFilePath(logFilePath);
    }

    public static boolean isPanoDebug() {
        return PiPano.sDebug;
    }

    public static void spatialMediaImpl(String filename, boolean isPano, String firmware, String artist) {
        PiPano.spatialMediaImpl(filename, isPano, false, firmware, artist, 1.0f);
    }

    public static void spatialMediaImpl(String filename, boolean isPano, boolean spatialPanoAudio, String firmware, String artist) {
        PiPano.spatialMediaImpl(filename, isPano, spatialPanoAudio, firmware, artist, 1.0f);
    }

    public static int spatialJpeg(String filename, double heading, Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        return PiPano.spatialJpeg(filename, buffer, width, height, heading,
                (System.currentTimeMillis() - SystemClock.elapsedRealtime()) * 1000 + image.getTimestamp() / 1000);
    }

    /**
     * restore high preview size.
     */
    public static void restoreHighPreviewSize() {
        restoreHighPreviewSize(true);
    }

    public static void restoreHighPreviewSize(boolean reStartPreview) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.startHighPreviewSize(reStartPreview);
    }

    /**
     * start low preview size.
     */
    public static void startLowPreviewSize() {
        startLowPreviewSize(true);
    }

    public static void startLowPreviewSize(boolean reStartPreview) {
        if (checkPanoSDKNotInit()) {
            return;
        }
        mSingle.mCameraSurfaceView.startLowPreviewSize(reStartPreview);
    }
}