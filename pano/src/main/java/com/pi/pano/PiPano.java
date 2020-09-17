package com.pi.pano;

import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.pi.pilot.pano.sdk.BuildConfig;
import com.pi.pilot.pano.sdk.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

class PiPano {
    private static final String TAG = "PiPano";

    int mSetPreviewFps = 15;

    private static final int MSG_PIPANO_INIT = 1;
    private static final int MSG_PIPANO_UPDATE = 2;
    private static final int MSG_PIPANO_RELEASE = 4;
    private static final int MSG_PIPANO_UPDATE_PRE_SECOND = 5;
    private static final int MSG_PIPANO_SET_ENCODE_SURFACE = 6;
    private static final int MSG_PIPANO_TAKE_PHOTO = 8;
    private static final int MSG_PIPANO_CHANGE_CAMERA_RESOLUTION = 11;
    private static final int MSG_PIPANO_FRAME_AVAILABLE = 13;
    private static final int MSG_PIPANO_ENCODER_SURFACE_UPDATE = 14;
    private static final int MSG_PIPANO_SET_STABILIZATION_FILENAME = 15;
    private static final int MSG_PIPANO_SAVE_PHOTO = 17;

    private static final int WTI_Text = 0;
    private static final int WTI_DebugPreview = 2;
    private static final int WTI_DebugEncoder = 3;

    private PiPanoListener mPiPanoListener;
    private String mCacheDir;
    private UpdateFrameTask mUpdateFrameTask;
    private long mNativeContext;//c++用,不要删除
    SurfaceTexture[] mSurfaceTexture = new SurfaceTexture[PilotSDK.CAMERA_COUNT];
    volatile boolean mHasInit;

    interface PiPanoListener {
        void onPiPanoInit();

        void onPiPanoChangeCameraResolution(ChangeResolutionListener listener);

        void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync);

        void onPiPanoCaptureFrame(int hdrIndex, TakePhotoListener listener);

        void onPiPanoDestroy();

        void onPiPanoEncodeFrame(int count);
    }

    static {
        System.loadLibrary("PiPano");
    }

    native void makeDebugText(boolean isPreview, String str, int color);

    /**
     * 初始化全景展开库
     */
    private native void createNativeObj(boolean isCamera, boolean isDebugMode, String cacheDir);

    native int getTextureId(int index);

    /**
     * 画一帧预览到屏幕
     *
     * @param effect 0-无特效 1-高斯模糊
     */
    native void drawPreviewFrame(int effect);

    /**
     * 画一帧畸变矫正
     *
     * @param mode      畸变矫正模式
     * @param drawAlpha 是否画透明度
     * @param timestamp 时间戳
     */
    native void drawLensCorrectionFrame(int mode, boolean drawAlpha, long timestamp);

    native void drawVioFrame();

    /**
     * 设置拼接距离
     *
     * @param d 拼接距离
     */
    native void setStitchingDistance(float d);

    native void setPreviewSurface(Surface surface);

    native void setPresentationSurface(Surface surface);

    /**
     * 设置编码surface
     *
     * @param surface 来自mediaCodec的surface，如果设置为null，将停止渲染编码画面以减少性能消耗
     */
    native void setEncodeVideoSurface(Surface surface);

    native void setEncodeLiveSurface(Surface surface);

    native void setEncodePhotoSurface(Surface surface);

    native void setEncodeVioSurface(Surface surface);

    /**
     * 释放资源
     */
    private native int panoRelease();

    /**
     * mp4插入全景信息
     *
     * @param filename       mp4文件名
     * @param printStructure 是否打印mp4 box结构
     * @param firmware       固件版本号
     * @return 0成功, 否则失败
     */
    native int spatialMediaImpl(String filename, boolean printStructure, String firmware);

    static native void clearImageList();

    static native int saveJpeg(String filename, String stitchFilename, boolean useHdr, ByteBuffer byteBuffer, int width, int height, int stride, int quality,
                               boolean save_exif, double heading, double latitude, double longitude, int altitude,
                               int cutoutPortrait, String artist, boolean saveUnStitch);

    static native void clearExifThumbnailBuffer();

    static native void recordOnceJpegInfo(int exposureTime, int exposureBias, int iso);

    static native void setFirmware(String firmware);

    /**
     * 保存编辑图片
     *
     * @param filename
     * @param width
     * @param height
     * @param isThumbnail
     * @return
     */
    native int savePicture(String filename, String coverFile, int width, int height, boolean isThumbnail);

    native void muOnScale(float f);

    native void muOnScroll(float x, float y);

    native void muOnFling(float x, float y, int fps);

    native void muOnDown();

    native void muOnDoubleTap();

    native void muSetPreviewMode(int mode, float rotateDegree, boolean playAnimation);

    native int muGetPreviewMode();

    /**
     * 是否使用陀螺仪,回放的时候打开陀螺仪,可以指哪看哪,录像的时候可以用于防抖
     *
     * @param use      是否使用
     * @param filename 如果指定了filename,那么从filename读取陀螺仪数据
     */
    native void useGyroscope(boolean use, String filename);

    native void setGyroscopeTimestampOffset(long timestampNs);

    /**
     * 设置旋转锁定数据文件名
     *
     * @param filename 旋转锁定数据文件名
     */
    private native void setStabilizationFilename(String filename);

    /**
     * 开启旋转锁定的时候,是否锁定yaw
     *
     * @param lock 是否锁定yaw
     */
    native void lockYaw(boolean lock);

    native void setUpsideDown(boolean b);

    static native int getBuildNumber();

    static native void setDeviceModel(int model);

    static native boolean getUseOpticalFlow(String filename);

    /**
     * 设置延时摄影倍率,每次录像开始前必须要设置一次
     *
     * @param ratio 倍率,如果当前是7fps,如果ratio为70,那么会降低帧为7/70=0.1fps,也就是10s录一帧
     */
    static native void setMemomotionRatio(int ratio);

    static native boolean isGoogleStreetViewVideo(String filename);

    PiPano(PiPanoListener piPanoListener, Context context) {
        mPiPanoListener = piPanoListener;
        makeDefaultWatermark(context);
        mCacheDir = context.getCacheDir().getAbsolutePath();
        mUpdateFrameTask = new UpdateFrameTask("UpdateFrameTask");
    }

    /**
     * 设置要要绘制的surface
     *
     * @param surface 要绘制的surface
     * @param type    0-预览 1-录像 2-直播 3-演示
     */
    void setSurface(Surface surface, int type) {
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_SET_ENCODE_SURFACE, type, 0, surface).sendToTarget();
    }

    void release() {
        mUpdateFrameTask.mHandler.sendEmptyMessage(MSG_PIPANO_RELEASE);
    }

    void takePhoto(int hdrIndex, TakePhotoListener listener) {
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_TAKE_PHOTO, hdrIndex, 0, listener).sendToTarget();
    }

    void savePhoto(SavePhotoListener listener) {
        listener.onSavePhotoStart();
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_SAVE_PHOTO, listener).sendToTarget();
    }

    void changeCameraResolution(ChangeResolutionListener listener) {
        mUpdateFrameTask.mHandler.removeMessages(MSG_PIPANO_CHANGE_CAMERA_RESOLUTION);
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_CHANGE_CAMERA_RESOLUTION, listener).sendToTarget();
    }

    void setStabilizationFile(String filename) {
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_SET_STABILIZATION_FILENAME, filename).sendToTarget();
    }

    /**
     * 检查Watermarks文件夹下是否有默认水印,没有的话就拷贝过去
     *
     * @param context 上下文
     */
    static void makeDefaultWatermark(Context context) {
        if (context == null) {
            return;
        }
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

    boolean isCameraFpsLow() {
        return mUpdateFrameTask.mCameraFps[0] < 3 ||
                mUpdateFrameTask.mCameraFps[1] < 3 ||
                mUpdateFrameTask.mCameraFps[2] < 3 ||
                mUpdateFrameTask.mCameraFps[3] < 3;
    }

    int[] getCameraFps() {
        return mUpdateFrameTask.mCameraFps;
    }

    private class FrameAvailableTask extends HandlerThread {
        private Handler mHandler;

        FrameAvailableTask(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }
    }

    private class UpdateFrameTask extends HandlerThread implements SurfaceTexture.OnFrameAvailableListener, Handler.Callback {
        private Handler mHandler;

        private int mPreviewFrameCount;
        private int mEncodeFrameCount;
        private int mNotSyncCount;
        private int[] mCameraFrameCount = new int[PilotSDK.CAMERA_COUNT];
        int[] mCameraFps = new int[]{30, 30, 30, 30};
        int gpuUtilizationAdd = 0;
        int gpuUtilizationCount = 0;

        private FrameAvailableTask[] mFrameAvailableTask = new FrameAvailableTask[PilotSDK.CAMERA_COUNT];
        private Message mTakePhotoMessage = null;

        UpdateFrameTask(String name) {
            super(name);
            for (int i = 0; i < mFrameAvailableTask.length; ++i) {
                mFrameAvailableTask[i] = new FrameAvailableTask("FrameAvailableTask");
            }
            start();
            mHandler = new Handler(getLooper(), this);
            Message.obtain(mHandler, MSG_PIPANO_INIT).sendToTarget();
        }

        private final Object mBaseTimeLock = new Object();
        private long mBaseTimestamp = 0;

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            long timestamp = surfaceTexture.getTimestamp();

            if (PiPano.this.mPiPanoListener.getClass() == CameraSurfaceView.class) {
                synchronized (mBaseTimeLock) {
                    if (timestamp > mBaseTimestamp + 1000000) {
                        if (mSurfaceTexture[0].getTimestamp() > mBaseTimestamp + 1000000 &&
                                mSurfaceTexture[1].getTimestamp() > mBaseTimestamp + 1000000 &&
                                mSurfaceTexture[2].getTimestamp() > mBaseTimestamp + 1000000 &&
                                mSurfaceTexture[3].getTimestamp() > mBaseTimestamp + 1000000) {
                            mBaseTimestamp = mSurfaceTexture[0].getTimestamp();
                            Message.obtain(mHandler, MSG_PIPANO_ENCODER_SURFACE_UPDATE, mBaseTimestamp).sendToTarget();
                            //Log.e(TAG, "------------------------------------");
                            mBaseTimeLock.notifyAll();
                        } else {
                            try {
                                mBaseTimeLock.wait(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            Message.obtain(mHandler, MSG_PIPANO_FRAME_AVAILABLE, surfaceTexture).sendToTarget();

            if (PiPano.this.mPiPanoListener.getClass() == MediaPlayerSurfaceView.class) {
                if (surfaceTexture == mSurfaceTexture[0]) {
                    Message.obtain(mHandler, MSG_PIPANO_ENCODER_SURFACE_UPDATE, timestamp).sendToTarget();
                }
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PIPANO_INIT:
                    createNativeObj(PiPano.this.mPiPanoListener.getClass() != MediaPlayerSurfaceView.class, BuildConfig.PI_DEBUG, mCacheDir);
                    for (int i = 0; i < mSurfaceTexture.length; ++i) {
                        mSurfaceTexture[i] = new SurfaceTexture(getTextureId(i));
                        mSurfaceTexture[i].setOnFrameAvailableListener(this, mFrameAvailableTask[i].mHandler);
                    }
                    mPiPanoListener.onPiPanoInit();
                    mHasInit = true;
                    mSetPreviewFps = 30;
                    mHandler.sendEmptyMessage(MSG_PIPANO_UPDATE);
                    mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE_PRE_SECOND, 1000);
                    break;
                case MSG_PIPANO_UPDATE:
                    if (mSetPreviewFps > 0) {
                        mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE, 1000 / mSetPreviewFps);
                        drawPreviewFrame(0);
                        mPreviewFrameCount++;
                    } else {
                        mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE, 100);
                    }
                    if (BuildConfig.PI_DEBUG) {
                        try {
                            BufferedReader gpuReader = new BufferedReader(
                                    new FileReader("/sys/devices/platform/13900000.mali/utilization"));
                            String gpuString = gpuReader.readLine();
                            gpuReader.close();
                            if (gpuString != null) {
                                int gpu = Integer.parseInt(gpuString);
                                if (gpu != 0) {
                                    gpuUtilizationAdd += gpu;
                                    gpuUtilizationCount++;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_PIPANO_RELEASE:
                    panoRelease();
                    mNativeContext = 0;
                    mPiPanoListener.onPiPanoDestroy();
                    for (SurfaceTexture st : mSurfaceTexture) {
                        if (st != null) {
                            st.release();
                        }
                    }
                    for (FrameAvailableTask task : mFrameAvailableTask) {
                        task.getLooper().quit();
                    }
                    getLooper().quit();
                    break;
                case MSG_PIPANO_UPDATE_PRE_SECOND:
                    mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE_PRE_SECOND, 1000);
                    mPiPanoListener.onPiPanoEncodeFrame(mEncodeFrameCount);

                    System.arraycopy(mCameraFrameCount, 0, mCameraFps, 0, mCameraFrameCount.length);

                    if (BuildConfig.PI_DEBUG) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("p:").append(mPreviewFrameCount)
                                .append("\ne:").append(mEncodeFrameCount).append("\nc:");
                        for (int i = 0; i < PilotSDK.CAMERA_COUNT; ++i) {
                            builder.append(mCameraFrameCount[i]).append(' ');
                        }

                        builder.append("\nt: ").append(Utils.getCpuTemperature());
                        float gpuUtiliaztion = 0.0f;
                        if (gpuUtilizationCount > 0) {
                            gpuUtiliaztion = (float) gpuUtilizationAdd / gpuUtilizationCount;
                        }
                        gpuUtilizationAdd = 0;
                        gpuUtilizationCount = 0;
                        builder.append("\ngpu: ").append(String.format("%.2f", gpuUtiliaztion));
                        builder.append("\nsync: ").append(mNotSyncCount);
                        mNotSyncCount = 0;

                        makeDebugText(true, builder.toString(), Color.YELLOW);
                    }

                    //Log.e(TAG, mPreviewFrameCount + " " + mCameraFrameCount);
                    mPreviewFrameCount = 0;
                    mEncodeFrameCount = 0;
                    Arrays.fill(mCameraFrameCount, 0);
                    break;
                case MSG_PIPANO_SET_ENCODE_SURFACE:
                    switch (msg.arg1) {
                        case 0:
                            setPreviewSurface((Surface) msg.obj);
                            break;
                        case 1:
                            setEncodeVideoSurface((Surface) msg.obj);
                            break;
                        case 2:
                            setEncodeLiveSurface((Surface) msg.obj);
                            break;
                        case 3:
                            setPresentationSurface((Surface) msg.obj);
                            break;
                    }
                    break;
                case MSG_PIPANO_TAKE_PHOTO:
                    mTakePhotoMessage = new Message();
                    mTakePhotoMessage.copyFrom(msg);
                    break;
                case MSG_PIPANO_SAVE_PHOTO:
                    SavePhotoListener listener = (SavePhotoListener) msg.obj;
                    if (listener.mThumbnail) {
                        savePicture(listener.mFilename, listener.mNewFilename, listener.mWidth, listener.mHeight, true);
                    } else {
                        int ret = savePicture(listener.mFilename, listener.mNewFilename, listener.mWidth, listener.mHeight, false);
                        if (ret == 0) {
                            listener.onSavePhotoComplete();
                        } else {
                            listener.onSavePhotoFailed();
                        }
                    }
                    break;
                case MSG_PIPANO_CHANGE_CAMERA_RESOLUTION:
                    drawPreviewFrame(1);
                    mPiPanoListener.onPiPanoChangeCameraResolution((ChangeResolutionListener) msg.obj);
                    break;
                case MSG_PIPANO_SET_STABILIZATION_FILENAME:
                    setStabilizationFilename((String) msg.obj);
                    break;
                case MSG_PIPANO_FRAME_AVAILABLE:
                    SurfaceTexture surfaceTexture = (SurfaceTexture) msg.obj;
                    try {
                        surfaceTexture.updateTexImage();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < mCameraFrameCount.length; ++i) {
                        if (surfaceTexture == mSurfaceTexture[i]) {
                            mCameraFrameCount[i]++;
                        }
                    }
                    break;
                case MSG_PIPANO_ENCODER_SURFACE_UPDATE:
                    if (mTakePhotoMessage != null) {
                        TakePhotoListener piSDKTakePhotoListener = (TakePhotoListener) mTakePhotoMessage.obj;
                        piSDKTakePhotoListener.mTimestamp = (long) msg.obj;
                        if (piSDKTakePhotoListener.mSkipFrame > 0) {
                            piSDKTakePhotoListener.mSkipFrame--;
                        } else {
                            mPiPanoListener.onPiPanoCaptureFrame(mTakePhotoMessage.arg1,
                                    (TakePhotoListener) mTakePhotoMessage.obj);
                            mTakePhotoMessage = null;
                        }
                    }

                    long max = Math.max(Math.max(mSurfaceTexture[0].getTimestamp(), mSurfaceTexture[1].getTimestamp()),
                            Math.max(mSurfaceTexture[2].getTimestamp(), mSurfaceTexture[3].getTimestamp()));
                    long min = Math.min(Math.min(mSurfaceTexture[0].getTimestamp(), mSurfaceTexture[1].getTimestamp()),
                            Math.min(mSurfaceTexture[2].getTimestamp(), mSurfaceTexture[3].getTimestamp()));
                    boolean isFrameSync = max - min < 1000000;

                    if (BuildConfig.PI_DEBUG) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("max delta: ").append(max - min).append("ns\n");
                        if (!isFrameSync) {
                            mNotSyncCount++;
                        }
                        builder.append("not sync count: ").append(mNotSyncCount).append("\n");

                        makeDebugText(false, builder.toString(), isFrameSync ? Color.YELLOW : Color.RED);
                    }

                    mPiPanoListener.onPiPanoEncoderSurfaceUpdate((long) msg.obj, isFrameSync);
                    mEncodeFrameCount++;
                    break;
            }
            return false;
        }
    }
}
