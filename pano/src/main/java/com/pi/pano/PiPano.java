package com.pi.pano;

import android.content.Context;
import android.content.res.AssetManager;
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
    private static final int MSG_PIPANO_SET_SLAME_ENABLE = 16;
    private static final int MSG_PIPANO_SAVE_PHOTO = 17;
    private static final int MSG_PIPANO_PARAM_RECALI = 18;

    private final PiPanoListener mPiPanoListener;
    private final String mCacheDir;
    private final UpdateFrameTask mUpdateFrameTask;
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
     * Initialize the panoramic expansion library
     */
    private native void createNativeObj(boolean isCamera, boolean isDebugMode, String cacheDir);

    native int getTextureId(int index);

    /**
     * Draw a frame preview to the screen
     *
     * @param effect 0-No special effect;1-Gaussian blur
     */
    native void drawPreviewFrame(int effect);

    /**
     * Draw a frame distortion correction
     *
     * @param mode      Distortion correction mode
     * @param drawAlpha Whether to draw transparency
     * @param timestamp Timestamp
     */
    native void drawLensCorrectionFrame(int mode, boolean drawAlpha, long timestamp);

    native void drawVioFrame();

    /**
     * Set stitching distance
     *
     * @param d Stitching distance
     */
    native void setStitchingDistance(float d);

    native void setPreviewSurface(Surface surface);

    native void setPresentationSurface(Surface surface);

    /**
     * Set the encoding surface
     *
     * @param surface The surface from mediaCodec, if set to null, will stop rendering the coded picture to reduce performance consumption
     */
    native void setEncodeVideoSurface(Surface surface);

    native void setEncodeLiveSurface(Surface surface);

    native void setEncodePhotoSurface(Surface surface);

    native void setEncodeVioSurface(Surface surface);

    /**
     * Release resources
     */
    private native int panoRelease();

    /**
     * mp4 insert panoramic information
     *
     * @param filename       mp4 file name
     * @param printStructure Whether to print the mp4 box structure
     * @param firmware       Firmware version number
     * @return 0 success, otherwise failure
     */
    native int spatialMediaImpl(String filename, boolean printStructure, String firmware);

    static native void clearImageList();

    static native int saveJpeg(String unstitchFilename, String stitchFilename, int hdrImageCount, ByteBuffer byteBuffer, int width, int height, int stride, int quality,
                               boolean save_exif, double heading, double latitude, double longitude, int altitude, String artist);

    static native void clearExifThumbnailBuffer();

    static native int makePanoWithMultiFisheye(String unstitchFilename, String stitchFilename, String[] fisheyeFilenames, int mask, int cameraCount);

    static native int injectThumbnail(String filename, String thumbnailFilename);

    static native void recordOnceJpegInfo(int exposureTime, int exposureBias, int iso, int writeBalance);

    static native void setFirmware(String firmware);

    native int savePicture(String filename, String coverFile, int width, int height, boolean isThumbnail);

    native void muOnScale(float f);

    native void muOnScroll(float x, float y);

    native void muOnFling(float x, float y, int fps);

    native void muOnDown();

    native void muOnDoubleTap();

    native void muSetPreviewMode(int mode, float rotateDegree, boolean playAnimation);

    native int muGetPreviewMode();

    /**
     * Set debug log path
     */
    static native void setLogFilePath(String logFilePath);

    /**
     * Whether to use a gyroscope, turn on the gyroscope during playback, you can refer to it, and it can be used for anti-shake when recording
     *
     * @param use      use or not
     * @param filename If filename is specified, then read gyroscope data from filename
     */
    native void useGyroscope(boolean use, String filename);

    native void setGyroscopeTimestampOffset(long timestampNs);

    /**
     * Set the rotation lock data file name
     *
     * @param filename Rotation lock data file name
     */
    private native void setStabilizationFilename(String filename);

    /**
     * When turning on the rotation lock, is yaw locked
     *
     * @param lock Lock yaws
     */
    native void lockYaw(boolean lock);

    native void setUpsideDown(boolean b);

    static native int getBuildNumber();

    static native void setDeviceModel(int model);

    static native boolean getUseOpticalFlow(String filename);

    /**
     * Set the time-lapse photography magnification, which must be set once before each recording
     *
     * @param ratio If the current magnification is 7fps, if the ratio is 70,
     *              then the frame will be reduced to 7 / 70 = 0.1fps, that is, one frame will be recorded in 10s
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
     * Set the surface to draw
     *
     * @param surface Surface to draw
     * @param type    0-preview;1-video;2-live;3-DEMO
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

    private static class SlamInit {
        boolean enable;
        AssetManager assetManager;
        float lenForCalMeasuringScale;
        SlamListener listener;
    }

    native void setSlamEnableImpl(boolean enable, Object assetManager, float lenForCalMeasuringScale, Object listener);

    void setSlamEnable(boolean enable, AssetManager assetManager, float lenForCalMeasuringScale, SlamListener listener) {
        SlamInit init = new SlamInit();
        init.enable = enable;
        init.assetManager = assetManager;
        init.lenForCalMeasuringScale = lenForCalMeasuringScale;
        init.listener = listener;
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_SET_SLAME_ENABLE, init).sendToTarget();
    }

    native void slamPause();

    native void slamResume();

    native void makeSlamPhotoPoint(String filename);

    native void slamShowPreview(boolean show);

    native void setParamReCaliEnableImpl(int executionInterval, Object listener);

    void setParamReCaliEnable(int executionInterval, Object listener) {
        Message.obtain(mUpdateFrameTask.mHandler, MSG_PIPANO_PARAM_RECALI, executionInterval, 0, listener).sendToTarget();
    }

    /**
     * Check whether there is a default watermark in the watermarks folder. If not, copy it
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
        private final Handler mHandler;

        FrameAvailableTask(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }
    }

    private class UpdateFrameTask extends HandlerThread implements SurfaceTexture.OnFrameAvailableListener, Handler.Callback {
        private final Handler mHandler;

        private int mPreviewFrameCount;
        private int mEncodeFrameCount;
        private int mNotSyncCount;
        private final int[] mCameraFrameCount = new int[PilotSDK.CAMERA_COUNT];
        int[] mCameraFps = new int[]{30, 30, 30, 30};
        int gpuUtilizationAdd = 0;
        int gpuUtilizationCount = 0;

        private final FrameAvailableTask[] mFrameAvailableTask = new FrameAvailableTask[PilotSDK.CAMERA_COUNT];
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
                case MSG_PIPANO_SET_SLAME_ENABLE:
                    SlamInit init = (SlamInit) msg.obj;
                    setSlamEnableImpl(init.enable, init.assetManager, init.lenForCalMeasuringScale, init.listener);
                    break;
                case MSG_PIPANO_PARAM_RECALI:
                    setParamReCaliEnableImpl(msg.arg1, msg.obj);
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
