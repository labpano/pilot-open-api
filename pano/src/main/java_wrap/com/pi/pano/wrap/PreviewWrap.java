package com.pi.pano.wrap;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pi.pano.DefaultChangeResolutionListener;
import com.pi.pano.DefaultLiveChangeResolutionListener;
import com.pi.pano.DefaultPlaneChangeResolutionListener;
import com.pi.pano.DefaultScreenLiveChangeResolutionListener;
import com.pi.pano.DefaultStreetVideoChangeResolutionListener;
import com.pi.pano.ExposeTimeAdjustHelper;
import com.pi.pano.IPreviewParamListener;
import com.pi.pano.OnAIDetectionListener;
import com.pi.pano.PanoSDKListener;
import com.pi.pano.PilotSDK;
import com.pi.pano.annotation.PiExposureCompensation;
import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiIso;
import com.pi.pano.annotation.PiPhotoResolution;
import com.pi.pano.annotation.PiPreviewMode;
import com.pi.pano.annotation.PiPushResolution;
import com.pi.pano.annotation.PiStitchDistance;
import com.pi.pano.annotation.PiStitchDistanceMax;
import com.pi.pano.annotation.PiVideoResolution;
import com.pi.pano.annotation.PiWhiteBalance;

import java.util.Arrays;
import java.util.Map;

/**
 * Preview operation.
 */
public class PreviewWrap {
    private static final String TAG = PreviewWrap.class.getSimpleName();

    public static final int makePanoWithMultiFisheye_mask = 0x0101;

    /**
     * init PanoView
     */
    public static PilotSDK initPanoView(ViewGroup parent, PanoSDKListener listener) {
        return new PilotSDK(parent, new PanoSDKListenerWrap(parent.getContext(), listener));
    }

    public static int getCameraModuleCount() {
        return PilotSDK.CAMERA_COUNT;
    }

    /**
     * 设置预览模式
     *
     * @param mode          预览模式
     * @param playAnimation 切换预览模式的时候是否播放动画
     */
    private static void setPreviewMode(@PiPreviewMode int mode, boolean playAnimation) {
        PilotSDK.setPreviewMode(mode, 0, playAnimation, 0, 0);
    }

    public static void setPreviewMode(@PiPreviewMode int mode) {
        PilotSDK.setPreviewMode(mode, 0, false, 0, 0);
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
        setExposeTime(value, null);
    }

    /**
     * 设置曝光时间
     */
    public static void setExposeTime(@PiExposureTime int value, IExposeTimeAdjustFinish adjustFinish) {
        PilotSDK.setExposeTime(value);
        if (null != adjustFinish) {
            // 等候曝光时间模式修改完成
            new Thread(() -> {
                ExposeTimeAdjustHelper.waitAdjustFinish();
                adjustFinish.onAdjustFinish();
            }, "ExposeTimeAdjustFinish").start();
        }
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

    public static boolean changeCameraResolution(int[] params, @NonNull DefaultChangeResolutionListener listener) {
        int[] cur = PilotSDK.getCameraResolution();
        boolean same = isSameCameraParams(params, cur);
        listener.mWidth = params[0];
        listener.mHeight = params[1];
        listener.mFps = params[2];
        if (same) {
            listener.onCheckResolution(true);
            return false;
        } else {
            listener.onCheckResolution(false);
        }
        listener.changeSurfaceViewSize(() -> {
            PilotSDK.changeCameraResolution(params, false, listener);
        });
        return true;
    }

    private static boolean isSameCameraParams(int[] params, int[] cur) {
        if (cur != null) {
            return Arrays.equals(params, cur);
        }
        return false;
    }

    /**
     * 后拼接录像分辨率设置
     *
     * @param resolution 录像输出分辨率
     * @param fps        录像输出帧率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForFishEyeVideo(@PiVideoResolution String resolution, String fps,
                                                                @Nullable DefaultChangeResolutionListener listener) {
        int[] params1 = null;
        switch (resolution) {
            case PiVideoResolution._5_7K:
                if ("30".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_30;
                } else if ("25".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_25;
                } else if ("24".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_24;
                }
                break;
            case PiVideoResolution._4K:
                if ("60".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_60;
                } else if ("30".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_30;
                } else if ("25".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_25;
                } else if ("24".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_24;
                }
                break;
            case PiVideoResolution._2_5K:
                if ("110".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_110;
                } else if ("90".equals(fps)) {
                    params1 = PilotSDK.CAMERA_PREVIEW_640_320_90;
                }
                break;
        }
        if (null == params1) {
            throw new RuntimeException("VideoResolution don't support:" + resolution);
        }
        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleChangeResolutionListener();
        }
        return changeCameraResolution(params, listener);
    }

    /**
     * 实时录像分辨率设置
     *
     * @param resolution 录像输出分辨率
     * @param fps        录像输出帧率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForStitchVideo(@PiVideoResolution String resolution, String fps,
                                                               @Nullable DefaultChangeResolutionListener listener) {
        throw new RuntimeException("VideoResolution don't support:" + resolution + ",fps:" + fps);
    }

    /**
     * 平面视频分辨率设置
     *
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForPlaneVideo(boolean front, @PiVideoResolution String resolution, String fps,
                                                              int fieldOfView, String aspectRatio,
                                                              @Nullable DefaultPlaneChangeResolutionListener listener) {
        int[] params1;
        if ("24".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_3040_3040_24;
        } else if ("25".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_3040_3040_25;
        } else if ("30".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_3040_3040_30;
        } else if ("60".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_3040_3040_60;
        } else {
            throw new RuntimeException("VideoResolution don't support:" + resolution + ",fps:" + fps);
        }
        int cameraId = front ? 0 : 1;
        int[] params = new int[params1.length + 1];
        params[params1.length] = cameraId; // 摄像头使用单镜头
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SamplePlaneChangeResolutionListener(fieldOfView, aspectRatio);
        }
        listener.mCameraId = String.valueOf(cameraId);
        return changeCameraResolution(params, listener);
    }

    /**
     * 慢动作
     *
     * @param resolution 录像分辨率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForSlowMotionVideo(@PiVideoResolution String resolution, String fps,
                                                                   @Nullable DefaultChangeResolutionListener listener) {
        int[] params1 = PilotSDK.CAMERA_PREVIEW_640_320_110;
        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleChangeResolutionListener();
        }
        return changeCameraResolution(params, listener);
    }

    /**
     * 录像（延时摄影）分辨率设置
     *
     * @param resolution 录像分辨率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForTimeLapseVideo(@PiVideoResolution String resolution, String fps,
                                                                  @Nullable DefaultChangeResolutionListener listener) {
        int[] params1 = PilotSDK.CAMERA_PREVIEW_5760_2880_30;
        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleChangeResolutionListener();
        }
        return changeCameraResolution(params, listener);
    }

    /**
     * 录像（街景）分辨率设置
     *
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForStreetViewVideo(@PiVideoResolution String resolution, String fps,
                                                                   @Nullable DefaultStreetVideoChangeResolutionListener listener) {
        int[] params1;
        if ("3".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_5760_2880_15;
        } else if ("4".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_5760_2880_16;
        } else if ("7".equals(fps) || "2".equals(fps) || "1".equals(fps)) {
            params1 = PilotSDK.CAMERA_PREVIEW_5760_2880_14;
        } else {
            throw new RuntimeException("VideoResolution don't support:" + resolution + ",fps:" + fps);
        }
        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleStreetVideoResolutionListener();
        }
        return changeCameraResolution(params, listener);
    }

    /**
     * 拍照分辨率设置
     *
     * @param resolution 拍照分辨率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForPhoto(@PiPhotoResolution String resolution,
                                                         @Nullable DefaultChangeResolutionListener listener) {
        if (!PiPhotoResolution._5_7K.equals(resolution)) {
            throw new RuntimeException("PiPhotoResolution don't support:" + resolution);
        }
        int[] params1 = PilotSDK.CAMERA_PREVIEW_1920_960_30;

        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleChangeResolutionListener();
        }
        listener.isPhoto = true;
        return changeCameraResolution(params, listener);
    }

    /**
     * 直播分辨率设置
     *
     * @param resolution 直播分辨率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForLive(@PiPushResolution String resolution, String fps,
                                                        @Nullable DefaultLiveChangeResolutionListener listener) {
        int[] params1 = PilotSDK.CAMERA_PREVIEW_5760_2880_30;
        int fpsInt;
        try {
            fpsInt = Integer.parseInt(fps);
        } catch (NumberFormatException ignore) {
            fpsInt = PilotSDK.CAMERA_PREVIEW_5760_2880_30[2];
        }
        params1[2] = fpsInt;
        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleLiveResolutionListener();
        }
        return changeCameraResolution(params, listener);
    }

    /**
     * 屏幕直播分辨率设置
     *
     * @param resolution 直播分辨率
     * @return 分辨率是否将改变
     */
    public static boolean changeCameraResolutionForScreenLive(@PiPushResolution String resolution, String fps, String aspectRatio,
                                                              @Nullable DefaultScreenLiveChangeResolutionListener listener) {
        int[] params1 = PilotSDK.CAMERA_PREVIEW_5760_2880_30;
        int fpsInt;
        try {
            fpsInt = Integer.parseInt(fps);
        } catch (NumberFormatException ignore) {
            fpsInt = PilotSDK.CAMERA_PREVIEW_5760_2880_30[2];
        }
        params1[2] = fpsInt;
        int[] params = new int[params1.length + 1];
        params[params1.length] = 2; // 摄像头使用全景
        System.arraycopy(params1, 0, params, 0, params1.length);
        if (null == listener) {
            listener = new SampleScreenLiveResolutionListener(aspectRatio);
        }
        return changeCameraResolution(params, listener);
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
     * 是否使用镜头保护镜
     */
    public static void setLensProtectedEnabled(boolean enabled) {
        PilotSDK.setLensProtectedEnabled(enabled);
    }
}
