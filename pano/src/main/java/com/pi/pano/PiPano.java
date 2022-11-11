package com.pi.pano;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Surface;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pi.pilot.pano.sdk.BuildConfig;
import com.pi.pilot.pano.sdk.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Panorama basic thread.
 */
@Keep
abstract class PiPano extends HandlerThread implements Handler.Callback {
    public static boolean sDebug = BuildConfig.DEBUG;

    protected static final int MSG_PIPANO_INIT = 1;
    protected static final int MSG_PIPANO_UPDATE = 2;
    protected static final int MSG_PIPANO_RELEASE = 4;
    protected static final int MSG_PIPANO_UPDATE_PRE_SECOND = 5;
    protected static final int MSG_PIPANO_SET_ENCODE_SURFACE = 6;
    protected static final int MSG_PIPANO_TAKE_PHOTO = 8;
    protected static final int MSG_PIPANO_CHANGE_CAMERA_RESOLUTION = 11;
    protected static final int MSG_PIPANO_FRAME_AVAILABLE = 13;
    protected static final int MSG_PIPANO_ENCODER_SURFACE_UPDATE = 14;
    protected static final int MSG_PIPANO_SET_STABILIZATION_FILENAME = 15;
    protected static final int MSG_PIPANO_SAVE_PHOTO = 17;
    protected static final int MSG_PIPANO_PARAM_RECALI = 18;
    protected static final int MSG_PIPANO_SLAME_START = 19;
    protected static final int MSG_PIPANO_SLAME_STOP = 20;
    protected static final int MSG_PIPANO_ENABLED = 30;
    protected static final int MSG_PIPANO_DETECTION = 31;
    protected static final int MSG_PIPANO_LENS_PROTECTED = 32;

    int mSetPreviewFps = 30;

    protected final PiPanoListener mPiPanoListener;
    protected final String mCacheDir;
    protected final Handler mHandler;
    protected int[] mCameraFps = new int[PilotSDK.CAMERA_COUNT];
    protected int mPreviewFrameCount;
    protected int mEncodeFrameCount;
    protected final int[] mCameraFrameCount = new int[PilotSDK.CAMERA_COUNT];
    protected int gpuUtilizationAdd = 0;
    protected int gpuUtilizationCount = 0;
    protected int batteryAnalysisAdd = 0;
    protected int batteryAnalysisCount = 0;
    protected final FrameAvailableTask[] mFrameAvailableTask = new FrameAvailableTask[PilotSDK.CAMERA_COUNT];
    protected Message mTakePhotoMessage = null;

    @Keep
    protected long mNativeContext;//do not delete
    SurfaceTexture[] mSurfaceTexture = new SurfaceTexture[PilotSDK.CAMERA_COUNT];
    volatile boolean mHasInit;

    public volatile boolean isCaptureCompleted = true;

    interface PiPanoListener {
        void onPiPanoInit();
        void onPiPanoChangeCameraResolution(ChangeResolutionListener listener);
        void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync);
        void onPiPanoCaptureFrame(int hdrIndex, TakePhotoListener listener);
        void onPiPanoDestroy();
        void onPiPanoEncodeFrame(int count);
    }

    static {
        System.loadLibrary(Config.PIPANO_SO_NAME);
    }

    /**
     * Incoming Debug Information Image.
     * Coordinates and size are relative,value range[0,1].
     *
     * @param isPreview true-drawn on the preview screen; false-draw on media file
     * @param bitmap    debug information image
     * @param x         x coordinate
     * @param y         y coordinate
     * @param width     width
     * @param height    height
     */
    native void makeDebugTexture(boolean isPreview, Bitmap bitmap, float x, float y, float width, float height);

    /**
     * Initialize Panoramic Deployment Library.
     */
    protected native void createNativeObj(boolean isCamera, boolean isDebugMode, String cacheDir);

    native int getTextureId(int index);

    /**
     * Draw a frame preview to the screen.
     *
     * @param effect 0-无特效 1-高斯模糊
     */
    native void drawPreviewFrame(int effect);

    /**
     * Set the preview resolution size. Generally, no active setting is required. The PiPano library will use the default size.
     * In some cases, such as UnstitchedSurfaceView, when playing videos, the resolution is low by default.
     * This interface can be used to set the resolution.
     *
     * @param width  preview width
     * @param height preview width
     */
    native void setPreviewSize(int width, int height);

    /**
     * Draw a frame of distortion correction.
     *
     * @param mode      Distortion correction mode
     * @param drawAlpha Whether to draw transparency
     * @param timestamp timestamp
     * @param useFlow   use flow stitch
     */
    native void drawLensCorrectionFrame(int mode, boolean drawAlpha, long timestamp, boolean useFlow);

    native void drawVioFrame();

    /**
     * set stitch distance.
     *
     * @param d stitch distance
     */
    native void setStitchingDistance(float d);

    /**
     * set preview surface.
     *
     * @param surface preview surface
     */
    native void setPreviewSurface(Surface surface);

    native void setPresentationSurface(Surface surface);

    /**
     * set encode video surface.
     *
     * @param surface If the surface from mediaCodec is set to null, it will stop rendering the encoded image to reduce performance consumption
     */
    native void setEncodeVideoSurface(Surface surface);
    /**
     * set encode video thumbnail surface.
     *
     * @param surface The surface used to draw video thumbnails. If it is set to null, it will stop rendering encoded pictures to reduce performance consumption
     */
    native void setEncodeVideoThumbSurface(Surface surface);
    native void setEncodeLiveSurface(Surface surface);
    native void setEncodePhotoSurface(Surface surface);
    native void setEncodeVioSurface(Surface surface);
    /**
     * set encode screen live surface.
     *
     * @param surface The surface used for live screen broadcast, if set to null, will stop rendering the encoded image to reduce performance consumption
     */
    native void setEncodeScreenLiveSurface(Surface surface);

    /**
     * release pano.
     */
    protected native int panoRelease();

    /**
     * Mp4 file insert panorama information.
     *
     * @param filename            file path
     * @param isPano              whether mp4 is panoramic video
     * @param firmware            firmware version name
     * @param artist              artist
     * @param memomotionTimeMulti It is used for multiplying the real time stamp when the mp4 time stamp is converted to video recording,
     *                            so that the delayed video and slow motion time stamp can correspond to the Steady time stamp.
     * @return 0:succeeded, otherwise failed
     */
    static native int spatialMediaImpl(String filename, boolean isPano, String firmware, String artist, float memomotionTimeMulti);
    /**
     * Copy exif information of old mp4 to new mp4.
     *
     * @param oldMp4Filename old mp4 file path
     * @param newMp4Filename new mp4 file path
     * @param isPano         whether new mp4 is panoramic video
     */
    static native void spatialMediaFromOldMp4(String oldMp4Filename, String newMp4Filename, boolean isPano);

    static native void clearImageList();
    static native int saveJpeg(String unstitchFilename, String stitchFilename, int hdrImageCount, ByteBuffer byteBuffer, byte[] byteArray, int width, int height, int stride, int quality,
                               boolean save_exif, double heading, double latitude, double longitude, int altitude, String artist);
    static native void clearExifThumbnailBuffer();

    /**
     * Combine multiple fisheye images into one fisheye image and expand the stitching, used to remove the silhouette or split photos.
     *
     * @param unstitchFilename unstitch file path
     * @param fisheyeFilenames A set of fisheye charts for merging
     * @param mask             The mask of the merging method, such as 0x0112, indicates the four fisheyes of the merged fisheye image, from the left to the right fisheyes are from fisheyeFilenames 0, 1, 2, 2.
     * @return 0:no error
     */
    static native int makePanoWithMultiFisheye(String unstitchFilename, String[] fisheyeFilenames, int mask, int cameraCount);
    /**
     * Add thumbnails to the jpeg file, the size of the thumbnails should not exceed 60000 bytes,
     * if the original jpeg file has thumbnails, then replace it.
     *
     * @param filename  To add thumbnails to a jpeg file.
     * @param thumbnailFilename The thumbnail file to be added or replaced, in jpeg format, cannot exceed 60,000 bytes.
     * @return 0:no error
     */
    static native int injectThumbnail(String filename, String thumbnailFilename);
    static native int spatialJpeg(String filename, ByteBuffer byteBuffer, int stitchWidth, int stitchHeight, double heading, long shootingTimeUs);

    /**
     * Record EXIF information, which will be written into the picture when the next photo is taken.
     *
     * @param exposureTime exposure time
     * @param exposureBias exposure compensation
     * @param iso          ios
     * @param writeBalance write balance
     */
    static native void recordOnceJpegInfo(int exposureTime, int exposureBias, int iso, int writeBalance);

    static native void setFirmware(String firmware);

    /**
     * Set editing status
     *
     * @param enable true: turn on edit
     */
    native void setEdition(boolean enable);

    /**
     * highlights
     *
     * @param highlights -2.0 to 2.0,默认值0.0
     */
    native void setHighlights(float highlights);

    /**
     * shadows
     *
     * @param shadows -2.0 to 2.0，default 0.0
     */
    native void setShadows(float shadows);

    /**
     * contrast
     *
     * @param contrast 0.0 to 4.0，default 1.0
     */
    native void setContrast(float contrast);

    /**
     * brightness
     *
     * @param brightness -1.0 to 1.0，default 0.0
     */
    native void setBrightness(float brightness);

    /**
     * saturation
     *
     * @param saturation 0.0 - 2.0，default 1.0
     */
    native void setSaturation(float saturation);

    /**
     * gamma
     *
     * @param gamma 0.0 - 3.0，default 1.0
     */
    native void setGamma(float gamma);

    /**
     * temperature
     *
     * @param temperature temperature
     */
    native void setTemperature(float temperature);

    /**
     * reset image
     */
    native void reset();

    /**
     * save file
     */
    native int savePicture(String filename, String coverFile, int width, int height, boolean isThumbnail);

    /**
     * get calibration parameters
     *
     * @return calibration parameters
     */
    static native String getParam();

    /**
     * set calibration parameters by file.
     *
     * @param filename file path
     */
    native void setParamByMedia(String filename);

    /**
     * Get the UTC timestamp of the first frame from the Mp4 video file.
     *
     * @param filename mp4 ile path
     * @return UTC timestamp, microseconds
     */
    static native long getFirstFrameUsUTC(String filename);

    /**
     * Set the debug log path
     *
     * @param logFilePath log file path
     */
    static native void setLogFilePath(String logFilePath);

    /**
     * Whether to use the gyroscope.
     * When playing back, turn on the gyroscope and point out where to look.
     * It can be used for Steady during video recording.
     *
     * @param use      whether to use
     * @param filename If filename is specified, read gyroscope data from filename.
     */
    native void useGyroscope(boolean use, String filename);

    /**
     * Whether to use gyroscope when playing media in the album.
     *
     * @param use whether to use
     */
    native void useGyroscopeForPlayer(boolean use);

    native void nativeStabSetTimeoffset(long timestampOffset, long rollingShutterDuration);
    native void nativeStabSetExposureDuration(long duration);

    /**
     * Set rotation lock data file name
     *
     * @param filename file path
     */
    protected native void nativeStabSetFilenameForSave(String filename);

    /**
     * Whether to lock the axis when turning on the rotation lock.
     *
     * @param yaw   whether to lock yaw
     * @param pitch whether to lock pitch
     * @param roll  whether to lock roll
     * @param lerp  00~1 When an axis is not followed, this value represents the tightness coefficient of the lens, and the greater the value, the faster the following.
     */
    native void nativeStabSetLockAxis(boolean yaw, boolean pitch, boolean roll, float lerp);

    /**
     * set upside down
     *
     * @param b true:upside down
     */
    native void setUpsideDown(boolean b);

    static native int getBuildNumber();

    static native void setDeviceModel(int model);

    static native boolean getUseOpticalFlow(String filename);

    /**
     * Set the delayed photography magnification, which must be set once before each video recording.
     *
     * @param ratio if the current is 7fps, if the ratio is 70, the frame will be reduced to 7/70=0.1fps, that is, one frame will be recorded in 10s.
     */
    static native void setMemomotionRatio(int ratio);

    static native boolean isGoogleStreetViewVideo(String filename);

    PiPano(PiPanoListener piPanoListener, Context context) {
        super("OpenGLThread");
        mPiPanoListener = piPanoListener;
        makeDefaultWatermark(context);
        mCacheDir = context.getCacheDir().getAbsolutePath();
        start();
        mHandler = new Handler(getLooper(), this);
        Arrays.fill(mCameraFps, 30);
        for (int i = 0; i < mFrameAvailableTask.length; ++i) {
            mFrameAvailableTask[i] = new FrameAvailableTask("FrameAvailableTask");
        }
        Message.obtain(mHandler, MSG_PIPANO_INIT).sendToTarget();
    }

    private int lastSetPreviewFps = 0;

    public void pausePreviewUpdate() {
        lastSetPreviewFps = mSetPreviewFps;
        mSetPreviewFps = 0;
    }

    public void resumePreviewUpdate() {
        if (lastSetPreviewFps > 0) {
            mSetPreviewFps = lastSetPreviewFps;
            lastSetPreviewFps = -1;
        }
    }

    protected boolean mPanoEnabled = true;

    public void setPanoEnabled(boolean enabled) {
        if (mPanoEnabled != enabled) {
            mPanoEnabled = enabled;
            Message.obtain(mHandler, MSG_PIPANO_ENABLED, enabled ? 1 : 0, 0).sendToTarget();
        }
    }

    protected boolean mDetectionEnabled = false;

    public void setDetectionEnabled(boolean enabled, OnAIDetectionListener listener) {
        if (mDetectionEnabled != enabled) {
            mDetectionEnabled = enabled;
            Message.obtain(mHandler, MSG_PIPANO_DETECTION, enabled ? 1 : 0, 0, listener).sendToTarget();
        }
    }

    public void setLensProtectedEnabled(boolean enabled) {
        Message.obtain(mHandler, MSG_PIPANO_LENS_PROTECTED, enabled ? 1 : 0, 0).sendToTarget();
    }

    /**
     * Set the surface to be drawn
     *
     * @param surface the surface to be drawn
     * @param type    0-Preview; 1 - Video recording; 2 - Live broadcast; 3 - Demo; 4 - Thumbnail video; 5 - Screen live broadcast
     */
    void setSurface(Surface surface, int type) {
        Message.obtain(mHandler, MSG_PIPANO_SET_ENCODE_SURFACE, type, 0, surface).sendToTarget();
    }

    void release() {
        mHandler.sendEmptyMessage(MSG_PIPANO_RELEASE);
    }

    void takePhoto(int hdrIndex, TakePhotoListener listener) {
        Message.obtain(mHandler, MSG_PIPANO_TAKE_PHOTO, hdrIndex, 0, listener).sendToTarget();
    }

    void savePhoto(SavePhotoListener listener) {
        listener.onSavePhotoStart();
        Message.obtain(mHandler, MSG_PIPANO_SAVE_PHOTO, listener).sendToTarget();
    }

    void changeCameraResolution(ChangeResolutionListener listener) {
        mHandler.removeMessages(MSG_PIPANO_CHANGE_CAMERA_RESOLUTION);
        Message.obtain(mHandler, MSG_PIPANO_CHANGE_CAMERA_RESOLUTION, listener).sendToTarget();
    }

    void setStabilizationFile(String filename) {
        Message.obtain(mHandler, MSG_PIPANO_SET_STABILIZATION_FILENAME, filename).sendToTarget();
    }

    native void nativeSlamStart(Object assetManager, float lenForCalMeasuringScale, boolean showPreview, boolean useForImuToPanoRotation, Object listener);
    native void nativeSlamStop();

    void slamStart(SlamListener listener) {
        Message.obtain(mHandler, MSG_PIPANO_SLAME_START, listener).sendToTarget();
    }

    void slamStop() {
        Message.obtain(mHandler, MSG_PIPANO_SLAME_STOP).sendToTarget();
    }

    native void slamPause();
    native void slamResume();

    /**
     * Marking points in slam.
     *
     * @param filename file name
     */
    native void makeSlamPhotoPoint(String filename);

    native void nativeDetectionStart(OnAIDetectionListener listener);
    native void nativeDetectionStop();

    native void nativeSetParamCalculateEnable(int executionInterval, boolean effectiveImmediately);

    void setParamReCaliEnable(int executionInterval, boolean effectiveImmediately) {
        Message.obtain(mHandler, MSG_PIPANO_PARAM_RECALI, executionInterval, effectiveImmediately ? 1 : 0).sendToTarget();
    }

    native void nativeSetLensProtectedEnable(boolean enable);

    /**
     * Check whether there is a default watermark in the Watermarks folder. If not, copy it.
     *
     * @param context context
     */
    static void makeDefaultWatermark(@NonNull Context context) {
        File file = new File("/sdcard/Watermarks/setting");
        boolean needMake = false;
        if (!file.exists()) {
            needMake = true;
        } else {
            String watermarkFilename = "";

            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[256];
                if (fileInputStream.read(buffer) == 256) {
                    int i;
                    for (i = 0; i < buffer.length; ++i) {
                        if (buffer[i] == 0) {
                            break;
                        }
                    }
                    watermarkFilename = new String(buffer, 0, i);
                }
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!watermarkFilename.equals("") && !watermarkFilename.equals("null")) {
                File watermarkFile = new File("/sdcard/Watermarks/" + watermarkFilename);
                if (!watermarkFile.exists()) {
                    needMake = true;
                }
            }
        }

        if (needMake) {
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
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

    boolean isCameraFpsLow() {
        for (int fps : mCameraFps) {
            if (fps < 3) return true;
        }
        return false;
    }

    int[] getCameraFps() {
        return mCameraFps;
    }

    protected Bitmap makePreviewDebugTexture(String text) {
        Bitmap bitmap = Bitmap.createBitmap(256, 128, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(20.0f);
        textPaint.setShadowLayer(1.0f, 1.0f, 1.0f, 0xFF888888);
        StaticLayout staticLayout = new StaticLayout(text, textPaint, bitmap.getWidth(), Layout.Alignment.ALIGN_NORMAL, 0.8f, 0.0f, false);
        staticLayout.draw(canvas);
        return bitmap;
    }

    protected Bitmap makeMediaDebugTexture(String text, int color) {
        Bitmap bitmap = Bitmap.createBitmap(256, 128, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(color);
        textPaint.setTextSize(32.0f);
        textPaint.setShadowLayer(1.0f, 1.0f, 1.0f, 0xFF888888);
        StaticLayout staticLayout = new StaticLayout(text, textPaint, bitmap.getWidth(), Layout.Alignment.ALIGN_NORMAL, 0.8f, 0.0f, false);
        staticLayout.draw(canvas);
        return bitmap;
    }

    protected static class FrameAvailableTask extends HandlerThread {
        protected final Handler mHandler;

        FrameAvailableTask(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }
    }
}
