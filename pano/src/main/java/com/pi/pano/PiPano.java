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
    protected static final int MSG_PIPANO_RELOAD_WATERMARK = 33;

    protected int mPreviewFps = 30;

    public int getPreviewFps() {
        return mPreviewFps;
    }

    public void setPreviewFps(int fps) {
        mPreviewFps = fps;
    }

    protected final PiPanoListener mPiPanoListener;
    protected final String mCacheDir;
    protected final Handler mHandler;
    protected final boolean mIsRenderFromCamera;
    protected int[] mCameraFps = new int[PilotSDK.CAMERA_COUNT];
    protected int mPreviewFrameCount;
    protected int mEncodeFrameCount;
    protected final int[] mCameraFrameCount = new int[PilotSDK.CAMERA_COUNT];
    protected int gpuUtilizationAdd = 0;
    protected int gpuUtilizationCount = 0;
    protected int batteryAnalysisAdd = 0;
    protected int batteryAnalysisCount = 0;

    @Keep
    protected long mNativeContext;//c++用,不要删除
    SurfaceTexture[] mSurfaceTexture = new SurfaceTexture[PilotSDK.CAMERA_COUNT];
    volatile boolean mHasInit;

    /**
     * Capture数据可用
     */
    public volatile boolean isCaptureCompleted = true;
    /**
     * 启动预览时，是否忽略帧数
     */
    public volatile boolean skipFrameWithStartPreview = true;

    /**
     * 重新打开camera时，需要过滤的 帧数
     */
    public int mSkipFrameWithOpenCamera;
    /**
     * 每次重新启动预览时，需要过滤的帧数
     */
    public int mSkipFrameWithStartPreview;

    interface PiPanoListener {
        void onPiPanoInit();
        /**
         * 处理改变分辨率请求。
         * @param listener 监听
         */
        void onPiPanoChangeCameraResolution(@NonNull ChangeResolutionListener listener);
        /**
         * 处理Surface更新。<br/>
         * 绘制畸变画面。
         * @param timestamp   时间戳
         * @param isFrameSync 同步帧
         */
        void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync);
        /**
         * 发现捕获的帧画面。
         * @param hdrIndex hdr拍照时的索引，非hdr为0
         * @param listener 拍照监听
         */
        void onPiPanoCaptureFrame(int hdrIndex, @NonNull TakePhotoListener listener);
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
     * @param hasChange 可绘制画面是否变动
     */
    native void drawPreviewFrame(int effect, boolean hasChange);

    /**
     * Set the preview resolution size. Generally, no active setting is required. The PiPano library will use the default size.
     * In some cases, such as UnstitchedSurfaceView, when playing videos, the resolution is low by default.
     * This interface can be used to set the resolution.
     *
     * @param width  分辨率宽
     * @param height 分辨率高
     */
    native void setPreviewSize(int width, int height);

    /**
     * Draw a frame of distortion correction.
     *
     * @param mode      畸变矫正模式
     * @param drawAlpha 是否画透明度
     * @param timestamp 时间戳
     * @param useFlow   是否使用光流拼接
     */
    native void drawLensCorrectionFrame(int mode, boolean drawAlpha, long timestamp, boolean useFlow);

    native void drawVioFrame();

    /**
     * set stitch distance.
     *
     * @param d 拼接距离
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
     * @param surface 来自mediaCodec的surface，如果设置为null，将停止渲染编码画面以减少性能消耗
     */
    native void setEncodeVideoSurface(Surface surface);
    /**
     * set encode video thumbnail surface.
     *
     * @param surface 用于画视频thumbnail的surface, 如果设置为null，将停止渲染编码画面以减少性能消耗
     */
    native void setEncodeVideoThumbSurface(Surface surface);
    native void setEncodeLiveSurface(Surface surface);
    native void setEncodePhotoSurface(Surface surface);
    native void setEncodeVioSurface(Surface surface);
    /**
     * set encode screen live surface.
     *
     * @param surface 用于屏幕直播的surface, 如果设置为null，将停止渲染编码画面以减少性能消耗
     */
    native void setEncodeScreenLiveSurface(Surface surface);

    /**
     * release pano.
     */
    protected native int panoRelease();

    /**
     * Mp4 file insert panorama information.
     *
     * @param filename            mp4文件名
     * @param isPano              mp4是否是全景视频
     * @param spatialPanoAudio    是否插入全景音信息
     * @param firmware            固件版本号
     * @param artist              作者
     * @param memomotionTimeMulti 用于mp4时间戳转为录像时真实时间戳的倍率,这样延时视频和慢动作时间戳才能和防抖时间戳对应起来
     * @return 0成功, 否则失败
     */
    static native int spatialMediaImpl(String filename, boolean isPano, boolean spatialPanoAudio, String firmware, String artist, float memomotionTimeMulti);
    /**
     * Copy exif information of old mp4 to new mp4.
     *
     * @param oldMp4Filename 旧mp4文件名
     * @param newMp4Filename 新mp4文件名
     * @param isPano         新mp4是否是全景视频
     */
    static native void spatialMediaFromOldMp4(String oldMp4Filename, String newMp4Filename, boolean isPano);

    static native void clearImageList();
    static native int saveJpeg(String unstitchFilename, String stitchFilename, int hdrImageCount, ByteBuffer byteBuffer, byte[] byteArray, int width, int height, int stride, int quality,
                               boolean save_exif, double heading, double latitude, double longitude, int altitude, String artist);
    static native void clearExifThumbnailBuffer();
    static native int makePanoWithMultiFisheye(String unstitchFilename, String[] fisheyeFilenames, int mask, int cameraCount);
    static native int injectThumbnail(String filename, String thumbnailFilename);
    static native int spatialJpeg(String filename, ByteBuffer byteBuffer, int stitchWidth, int stitchHeight, double heading, long shootingTimeUs);

    public static native int nativeHdrAddImage(ByteBuffer byteBuffer, byte[] byteArray, int width, int height, int stride);
    public static native int nativeHdrCalculate(String hdr_filename, String exif_filename, int width, int height, int quality);

    /**
     * Record EXIF information, which will be written into the picture when the next photo is taken.
     *
     * @param exposureTime 曝光时间,为1/X,如3200表示1/3200秒
     * @param exposureBias 曝光补偿
     * @param iso          感光度
     * @param writeBalance 白平衡,0-自动,1-手动
     */
    static native void recordOnceJpegInfo(int exposureTime, int exposureBias, int iso, int writeBalance);

    static native void setFirmware(String firmware);

    /**
     * 启用后处理
     *
     * @param enable 是否开启后处理
     */
    native void setEdition(boolean enable);

    /**
     * 高光
     *
     * @param highlights -2.0 to 2.0,默认值0.0
     */
    native void setHighlights(float highlights);

    /**
     * 阴影
     *
     * @param shadows -2.0 to 2.0，默认值0.0
     */
    native void setShadows(float shadows);

    /**
     * 对比度
     *
     * @param contrast 0.0 to 4.0，默认值1.0
     */
    native void setContrast(float contrast);

    /**
     * 亮度
     *
     * @param brightness -1.0 to 1.0，默认值0.0
     */
    native void setBrightness(float brightness);

    /**
     * 饱和度
     *
     * @param saturation 0.0 - 2.0，默认值1.0
     */
    native void setSaturation(float saturation);

    /**
     * 伽马值
     *
     * @param gamma 0.0 - 3.0，默认值 1.0
     */
    native void setGamma(float gamma);

    /**
     * 色温
     */
    native void setTemperature(float temperature);

    /**
     * reset image
     */
    native void reset();

    /**
     * 保存编辑图片
     */
    native int savePicture(String filename, String coverFile, int width, int height, boolean isThumbnail);

    /**
     * 获取标定参数
     *
     * @return 参数
     */
    static native String getParam();

    native void setParamByMedia(String filename);

    /**
     * 从Mp4视频文件中获取第一帧的UTC时间戳
     *
     * @param filename Mp4视频文件
     * @return Mp4视频文件中获取第一帧的UTC时间戳, 单位是微秒
     */
    static native long getFirstFrameUsUTC(String filename);

    /**
     * 设置调试log路径
     *
     * @param logFilePath 设置调试log路径
     */
    static native void setLogFilePath(String logFilePath);

    /**
     * 是否使用陀螺仪,回放的时候打开陀螺仪,可以指哪看哪,录像的时候可以用于防抖
     *
     * @param use      是否使用
     * @param filename 如果指定了filename,那么从filename读取陀螺仪数据
     */
    native void useGyroscope(boolean use, String filename);

    /**
     * 相册播放媒体的时候是否使用陀螺仪
     *
     * @param use 是否使用
     */
    native void useGyroscopeForPlayer(boolean use);

    native void nativeStabSetTimeoffset(long timestampOffset, long rollingShutterDuration);
    native void nativeStabSetExposureDuration(long duration);

    /**
     * 设置旋转锁定数据文件名
     *
     * @param filename 旋转锁定数据文件名
     */
    protected native void nativeStabSetFilenameForSave(String filename);

    /**
     * 开启旋转锁定的时候,是否锁定轴
     *
     * @param yaw   是否锁定yaw
     * @param pitch 是否锁定pitch
     * @param roll  是否锁定roll
     * @param lerp  0~1当某个轴没有跟随的时候,这个值代表镜头跟随的松紧系数,值越大跟随越快
     */
    native void nativeStabSetLockAxis(boolean yaw, boolean pitch, boolean roll, float lerp);

    native void setUpsideDown(boolean b);

    static native int getBuildNumber();

    static native void setDeviceModel(int model);

    static native boolean getUseOpticalFlow(String filename);

    /**
     * Set the delayed photography magnification, which must be set once before each video recording.
     *
     * @param ratio      倍率,如果当前是7fps,如果ratio为70,那么会降低帧为7/70=0.1fps,也就是10s录一帧
     * @param isStitched 是否是已拼接
     */
    static native void setMemomotionRatio(int ratio, boolean isStitched);

    static native boolean isGoogleStreetViewVideo(String filename);

    native void nativeCapturePhotoHdr(int hdrCount, long minHdrTimestamp);
    native void nativeSavePhotoHdr(String jpegDirname, String hdrFilename, int jpegQuality);

    PiPano(PiPanoListener piPanoListener, Context context, boolean isRenderFromCamera) {
        super("OpenGLThread");
        mPiPanoListener = piPanoListener;
        mIsRenderFromCamera = isRenderFromCamera;
        Watermark.checkWatermarkConfig(context);
        mCacheDir = context.getCacheDir().getAbsolutePath();
        start();
        mHandler = /*sDebug ? new DebugHandler(getName(), getLooper(), this) :*/ new Handler(getLooper(), this);
        Arrays.fill(mCameraFps, 30);
        Message.obtain(mHandler, MSG_PIPANO_INIT).sendToTarget();
    }

    /**
     * 记录的暂停前屏幕预览更新帧率
     */
    private int lastSetPreviewFps = 0;

    /**
     * 暂停屏幕预览更新
     */
    public void pausePreviewUpdate() {
        lastSetPreviewFps = mPreviewFps;
        setPreviewFps(0);
    }

    /**
     * 回复屏幕预览更新
     */
    public void resumePreviewUpdate() {
        if (lastSetPreviewFps != 0) {
            setPreviewFps(lastSetPreviewFps);
            lastSetPreviewFps = 0;
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

    public void setLensProtected(boolean enabled) {
        Message.obtain(mHandler, MSG_PIPANO_LENS_PROTECTED, enabled ? 1 : 0, 0).sendToTarget();
    }

    /**
     * Set the surface to be drawn
     *
     * @param surface 要绘制的surface
     * @param type    0-预览 1-录像 2-直播 3-演示 4-缩略图视频 5-屏幕直播
     */
    void setEncodeSurface(Surface surface, int type) {
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
     * @param show 是否显示
     */
    native void nativeReloadWatermark(boolean show);

    void reloadWatermark(boolean show) {
        Message.obtain(mHandler, MSG_PIPANO_RELOAD_WATERMARK, show).sendToTarget();
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
