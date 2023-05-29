package com.pi.pano.wrap;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pi.pano.DefaultChangeResolutionListener;
import com.pi.pano.IPreviewParamListener;
import com.pi.pano.OnAIDetectionListener;
import com.pi.pano.PanoSDKListener;
import com.pi.pano.PilotSDK;
import com.pi.pano.ResolutionParams;
import com.pi.pano.annotation.PiAntiMode;
import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiIso;
import com.pi.pano.annotation.PiStitchDistance;
import com.pi.pano.annotation.PiStitchDistanceMax;
import com.pi.pano.annotation.PiWhiteBalance;
import com.pi.pano.wrap.annotation.PiSteadyOrientation;

import java.util.Arrays;
import java.util.Map;

/**
 * Preview operation.
 */
public class PreviewWrap {
    private static final String TAG = PreviewWrap.class.getSimpleName();

    /**
     * init PanoView
     */
    public static PilotSDK initPanoView(ViewGroup parent, PanoSDKListener listener) {
        return initPanoView(parent, false, listener);
    }

    /**
     * 初始化
     *
     * @param lensProtected 是否使用保护镜
     */
    public static PilotSDK initPanoView(ViewGroup parent, boolean lensProtected, PanoSDKListener listener) {
        return new PilotSDK(parent, lensProtected, listener);
    }

    public static int getCameraModuleCount() {
        return PilotSDK.CAMERA_COUNT;
    }

    /**
     * 是否开启/禁用触摸事件
     *
     * @param able 是否开启/禁用触摸事件
     */
    public static void setPreviewEnableTouch(boolean able) {
        PilotSDK.setEnableTouchEvent(able);
    }

    /**
     * 开启\关闭 防抖
     */
    public static void setSteadyAble(boolean able) {
        PilotSDK.useGyroscope(able);
    }

    /**
     * 设置防抖方向
     */
    public static void setSteadyOrientation(@PiSteadyOrientation String steadyOrientation) {
        PilotSDK.stabSetLockAxis(!PiSteadyOrientation.follow.equals(steadyOrientation), true, true, 0.05f);
    }

    /**
     * 设置防抖跟随
     *
     * @param mask 3位二进制从高到第分别为： yaw,  pitch,  roll 三轴，对轴标识位为1：开启跟随，否则为固定
     */
    public static void setSteadyFollow(int mask) {
        setSteadyFollow(mask, 0.05f);
    }

    /**
     * 设置防抖跟随
     *
     * @param mask 3位二进制从高到第分别为： yaw,  pitch,  roll 三轴，对轴标识位为1：开启跟随，否则为固定
     * @param lerp 0~1当某个轴没有跟随的时候,这个值代表镜头跟随的松紧系数,值越大跟随越快
     */
    public static void setSteadyFollow(int mask, float lerp) {
        PilotSDK.stabSetLockAxis((mask & 0b100) > 0, (mask & 0b010) > 0, (mask & 0b001) > 0, lerp);
    }

    /**
     * 平面视频 防抖跟随
     */
    public static void setSteadyFollowWithPlane(int mask) {
        setSteadyFollow(mask, 0.12f);
    }


    /**
     * 翻转</br>
     * 预览倒置需关闭防抖
     */
    public static void setUpsideDown(boolean able) {
        setSteadyAble(false); // 开启翻转需要关闭图像稳定
        PilotSDK.setUpsideDown(able);
    }

    @Nullable
    public static Map<String, Object> getPreviewParam() {
        return PilotSDK.getPreviewParam();
    }

    public static void setPreviewParam(@Nullable Map<String, Object> param, @Nullable IPreviewParamListener listener) {
        PilotSDK.setPreviewParam(param, listener);
    }

    public static void setPreviewParam(@Nullable Map<String, Object> param) {
        PilotSDK.setPreviewParam(param);
    }

    public static int getPreviewFps() {
        return PilotSDK.getFps();
    }

    /**
     * 获取预览调节完成的等待时间
     */
    public static long obtainAdjustWaitTime() {
        //return PilotSDK.obtainAdjustWaitTime(); //暂未生效
        return 150;
    }

    /**
     * 设置曝光时间
     */
    public static void setExposeTime(@PiExposureTime int value) {
        PilotSDK.setExposeTime(value);
    }

    /**
     * 设置曝光补偿值
     */
    public static void setExposureCompensation(@PiExposureCompensation int ev) {
        PilotSDK.setExposureCompensation(ev);
    }

    /**
     * 设置iso
     */
    public static void setISO(@PiIso int iso) {
        PilotSDK.setISO(iso);
    }

    /**
     * 设置白平衡
     */
    public static void setWhiteBalance(@PiWhiteBalance String wb) {
        PilotSDK.setWhiteBalance(wb);
    }

    /**
     * 设置抗频闪
     */
    public static void setAntiMode(@PiAntiMode String mode) {
        PilotSDK.setAntiMode(mode);
    }


    /**
     * 设置拼接距离。
     * 拍照及实时视频生成文件时再次计算拼接距离与此设置无关。
     *
     * @param distance 拼接距离
     * @param max      拼接距离最大值
     */
    @Deprecated
    public static void setStitchDistance(@PiStitchDistance float distance, @PiStitchDistanceMax float max) {
        if (distance == PiStitchDistance.auto) {
            onceParamReCali();
        } else {
            PilotSDK.setStitchingDistance(distance, max);
        }
    }

    /**
     * 测量一次拼接距离,即自动测量拼接距离
     */
    public static void onceParamReCali() {
        PilotSDK.setParamReCaliEnable(-1, true);
    }

    private static boolean isSameCameraParams(int[] cameraParams, int[] cur) {
        if (cur != null) {
            return Arrays.equals(cameraParams, cur);
        }
        return false;
    }

    /**
     * 分辨率切换
     *
     * @param params   分辨率参数
     * @param listener 监听
     * @return 分辨率是否将改变
     */
    public static boolean changeResolution(@NonNull ResolutionParams params, @NonNull DefaultChangeResolutionListener listener) {
        Log.d(TAG, "change resolution:" + params);
        int[] cameraParams = params.resolutions;
        if (cameraParams.length == 3) {
            cameraParams = new int[4];
            System.arraycopy(params.resolutions, 0, cameraParams, 0, 3);
            // 默认摄像头使用全景
            cameraParams[3] = 2;
        }
        return changeResolutionInner(cameraParams, params.forceChange, listener);
    }

    private static boolean changeResolutionInner(int[] cameraParams, boolean force, @NonNull DefaultChangeResolutionListener listener) {
        boolean same = !force && isSameCameraParams(cameraParams, PilotSDK.getCameraResolution());
        listener.fillParams(cameraParams[3] != -1 ? String.valueOf(cameraParams[3]) : null, cameraParams[0], cameraParams[1], cameraParams[2]);
        if (same) {
            listener.onCheckResolution(true);
            return false;
        } else {
            listener.onCheckResolution(false);
        }
        listener.changeSurfaceViewSize(() -> {
            PilotSDK.changeCameraResolution(force, listener);
        });
        return true;
    }

    /**
     * 开启 AI跟踪
     */
    public static void startPreViewAiDetection(OnAIDetectionListener listener) {
        PilotSDK.detectionStart(listener);
    }

    /**
     * 关闭 AI跟踪
     */
    public static void stopPreViewAiDetection() {
        PilotSDK.detectionStop();
    }

    /**
     * set lens protectors.
     */
    public static void setLensProtected(boolean enabled) {
        PilotSDK.setLensProtected(enabled);
    }
}
