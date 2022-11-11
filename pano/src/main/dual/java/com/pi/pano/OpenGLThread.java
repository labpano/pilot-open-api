package com.pi.pano;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Message;
import android.os.SystemClock;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Pano thread.
 */
public class OpenGLThread extends PiPano implements SurfaceTexture.OnFrameAvailableListener {

    OpenGLThread(PiPanoListener piPanoListener, Context context) {
        super(piPanoListener, context);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Message.obtain(mHandler, MSG_PIPANO_FRAME_AVAILABLE, surfaceTexture).sendToTarget();
    }

    @SuppressLint("Recycle")
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_PIPANO_INIT:
                createNativeObj(true, sDebug, mCacheDir);
                for (int i = 0; i < mSurfaceTexture.length; ++i) {
                    mSurfaceTexture[i] = new SurfaceTexture(getTextureId(i));
                    mSurfaceTexture[i].setOnFrameAvailableListener(this, mFrameAvailableTask[i].mHandler);
                }
                mPiPanoListener.onPiPanoInit();
                mHasInit = true;
                mHandler.sendEmptyMessage(MSG_PIPANO_UPDATE);
                mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE_PRE_SECOND, 1000);
                break;
            case MSG_PIPANO_UPDATE:
                if (mSetPreviewFps > 0) {
                    mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE, 1000 / mSetPreviewFps);
                    if (mPanoEnabled) {
                        drawPreviewFrame(0);
                        mPreviewFrameCount++;
                    }
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE, 100);
                }
                if (sDebug) {
                    try {
                        BufferedReader gpuReader = new BufferedReader(new FileReader("/sys/class/kgsl/kgsl-3d0/gpubusy"));
                        String gpuString = gpuReader.readLine().trim();
                        gpuReader.close();
                        String[] ss = gpuString.split(" ");
                        try {
                            int gpu0 = Integer.parseInt(ss[0].isEmpty() ? "0" : ss[0]);
                            int gpu1 = Integer.parseInt(ss[1].isEmpty() ? "0" : ss[1]);
                            float gpu = 0;
                            if (gpu1 != 0) {
                                gpu = (float) gpu0 * 100 / gpu1;
                            }
                            gpuUtilizationAdd += gpu;
                            gpuUtilizationCount++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        BufferedReader batteryReader = new BufferedReader(new FileReader("/sys/class/power_supply/battery/current_now"));
                        String batteryString = batteryReader.readLine().trim();
                        batteryReader.close();
                        int battery = Integer.parseInt(batteryString);
                        batteryAnalysisAdd += battery;
                        batteryAnalysisCount++;
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
            case MSG_PIPANO_ENABLED:
                mPanoEnabled = msg.arg1 > 0;
                if (!mPanoEnabled) {
                    drawPreviewFrame(1);
                }
                break;
            case MSG_PIPANO_DETECTION:
                mDetectionEnabled = msg.arg1 > 0;
                if (mDetectionEnabled) {
                    nativeDetectionStart((OnAIDetectionListener) msg.obj);
                } else {
                    nativeDetectionStop();
                }
                break;
            case MSG_PIPANO_LENS_PROTECTED:
                nativeSetLensProtectedEnable(msg.arg1 > 0);
                break;
            case MSG_PIPANO_UPDATE_PRE_SECOND:
                mHandler.sendEmptyMessageDelayed(MSG_PIPANO_UPDATE_PRE_SECOND, 1000);
                mPiPanoListener.onPiPanoEncodeFrame(mEncodeFrameCount);

                System.arraycopy(mCameraFrameCount, 0, mCameraFps, 0, mCameraFrameCount.length);

                if (sDebug) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("p:").append(mPreviewFrameCount)
                            .append("\ne:").append(mEncodeFrameCount).append("\nc:");
                    for (int i = 0; i < PilotSDK.CAMERA_COUNT; ++i) {
                        builder.append(mCameraFrameCount[i]).append(' ');
                    }

                    builder.append("\nt: ").append(CpuTemperatureReader.getCpuTemperature());
                    float gpuUtilization = 0.0f;
                    if (gpuUtilizationCount > 0) {
                        gpuUtilization = (float) gpuUtilizationAdd / gpuUtilizationCount;
                    }
                    gpuUtilizationAdd = 0;
                    gpuUtilizationCount = 0;
                    builder.append("\ngpu: ").append(String.format(Locale.getDefault(), "%.1f", gpuUtilization));
                    int batteryAnalysis = 0;
                    if (batteryAnalysisCount > 0) {
                        batteryAnalysis = batteryAnalysisAdd / batteryAnalysisCount / 1000;
                    }
                    batteryAnalysisAdd = 0;
                    batteryAnalysisCount = 0;
                    builder.append("\nbattery: ").append(batteryAnalysis);

                    makeDebugTexture(true, makePreviewDebugTexture(builder.toString()), 0, 0.7f, 0.5f, 0.2f);
                }

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
                    case 4:
                        setEncodeVideoThumbSurface((Surface) msg.obj);
                        break;
                    case 5:
                        setEncodeScreenLiveSurface((Surface) msg.obj);
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
                nativeStabSetFilenameForSave((String) msg.obj);
                break;
            case MSG_PIPANO_SLAME_START:
                SlamListener slamListener = (SlamListener) msg.obj;
                nativeSlamStart(slamListener.assetManager, slamListener.lenForCalMeasuringScale,
                        slamListener.showPreview, slamListener.useForImuToPanoRotation, slamListener);
                break;
            case MSG_PIPANO_SLAME_STOP:
                nativeSlamStop();
                break;
            case MSG_PIPANO_PARAM_RECALI:
                nativeSetParamCalculateEnable(msg.arg1, msg.arg2 == 1);
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

                long timestamp = surfaceTexture.getTimestamp();

                if (mTakePhotoMessage != null) {
                    TakePhotoListener piSDKTakePhotoListener = (TakePhotoListener) mTakePhotoMessage.obj;
                    piSDKTakePhotoListener.mTimestamp = timestamp;
                    if (piSDKTakePhotoListener.mSkipFrame > 0) {
                        piSDKTakePhotoListener.mSkipFrame--;
                    } else {
                        mPiPanoListener.onPiPanoCaptureFrame(mTakePhotoMessage.arg1, piSDKTakePhotoListener);
                        mTakePhotoMessage = null;
                    }
                }

                if (sDebug) {
                    makeDebugTexture(false,
                            makeMediaDebugTexture("timestamp: " + timestamp / 1000000 + "ms\n", Color.YELLOW),
                            0, 0.95f, 0.12f, 0.1f);
                }

                //实时拼接时,双目要将boottime时间转为monotonic时间
                if (mPiPanoListener.getClass() == CameraSurfaceView.class) {
                    long deltaTimeMs = SystemClock.elapsedRealtime() - SystemClock.uptimeMillis();
                    timestamp -= deltaTimeMs * 1_000_000;
                }
                if (!isCaptureCompleted) {
                    break;
                }
                if (mPanoEnabled) {
                    mPiPanoListener.onPiPanoEncoderSurfaceUpdate(timestamp, true);
                }
                mEncodeFrameCount++;
                break;
        }
        return false;
    }
}
