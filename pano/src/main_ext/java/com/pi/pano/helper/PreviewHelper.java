package com.pi.pano.helper;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.pi.pano.ChangeResolutionListener;
import com.pi.pano.PanoSDKListener;
import com.pi.pano.PilotSDK;
import com.pi.pano.annotation.PiPhotoResolution;
import com.pi.pano.annotation.PiPreviewMode;
import com.pi.pano.annotation.PiProEv;
import com.pi.pano.annotation.PiProIsoInAutoEx;
import com.pi.pano.annotation.PiProWb;
import com.pi.pano.annotation.PiPushResolution;
import com.pi.pano.annotation.PiStitchingDistance;
import com.pi.pano.annotation.PiVideoResolution;

public class PreviewHelper {

    /**
     * initialization pano view.
     */
    public static PilotSDK initPanoView(ViewGroup parent, PanoSDKListener listener) {
        return new PilotSDK(parent, listener);
    }

    /**
     * Set preview mode.
     *
     * @param mode          preview mode.
     * @param playAnimation Whether to play animation when switching preview mode.
     */
    public static void setPreviewMode(@PiPreviewMode int mode, boolean playAnimation) {
        PilotSDK.setPreviewMode(mode, 0, playAnimation);
    }

    /**
     * Whether to lock yaw when turning on the rotation lock.
     *
     * @param lock Whether to lock yaw.
     */
    public static void lockYaw(boolean lock) {
        PilotSDK.lockYaw(lock);
    }

    /**
     * Whether to use a gyroscope, turn on the gyroscope during playback, you can refer to it and behold,
     * it can be used for PilotSteady when recording
     */
    public static void useGyroscope(boolean able) {
        PilotSDK.useGyroscope(able);
    }

    /**
     * Set the inverted state, if the anti-shake is turned on, the inverted effect is invalid.
     *
     * @param able Is it inverted.
     */
    public static void upsideDown(boolean able) {
        useGyroscope(false); // Turning off image stabilization.
        PilotSDK.setUpsideDown(able);
    }

    /**
     * Set the exposure compensation value.
     */
    public static void setEV(@PiProEv int ev) {
        PilotSDK.setExposureCompensation(ev);
    }

    /**
     * ISO used when setting auto exposure time.
     */
    public static void setISO(@PiProIsoInAutoEx int iso) {
        PilotSDK.setISO(iso);
    }

    /**
     * Set white balance.
     */
    public static void setWB(@PiProWb String wb) {
        PilotSDK.setWhiteBalance(wb);
    }

    /**
     * Set stitching distance.
     * When taking photos and real-time video files,
     * calculating the stitching distance again has nothing to do with this setting.
     *
     * @param distance stitching distance.
     * @param max      maximum stitching distance.
     */
    public static void setStitchDistance(@PiStitchingDistance float distance, float max) {
        if (distance == PiStitchingDistance.auto) {
            onceParamReCali();
        } else {
            PilotSDK.setStitchingDistance(distance, max);
        }
    }

    /**
     * Measure the stitching distance once, that is, automatically measure the splicing distance.
     */
    public static void onceParamReCali() {
        PilotSDK.setParamReCaliEnable(new OnceParamReCaliListener());
    }

    public static void changeCameraResolutionForVideo(String resolution, boolean retain) {
        changeCameraResolutionForVideo(resolution, retain, new ChangeResolutionListener() {
            @Override
            protected void onChangeResolution(int width, int height) {
            }
        });
    }

    public static void changeCameraResolutionForVideo(@PiVideoResolution String resolution, boolean retain, ChangeResolutionListener listener) {
        int[] params = obtainCameraParamsForVideo(resolution, retain);
        PilotSDK.changeCameraResolution(params, listener);
    }

    /**
     * @param resolution resolution
     * @param nrt        not real time video（postpone stitching）
     */
    @NonNull
    private static int[] obtainCameraParamsForVideo(@PiVideoResolution String resolution, boolean nrt) {
        switch (resolution) {
            case PiVideoResolution._8K:
                if (nrt) {
                    return PilotSDK.CAMERA_PREVIEW_400_250_24;
                } else {
                    return PilotSDK.CAMERA_PREVIEW_4048_2530_7;
                }
            case PiVideoResolution._7K:
                if (nrt) {
                    return PilotSDK.CAMERA_PREVIEW_288_180_24;
                } else {
                    throw new RuntimeException("VideoResolution don't support:" + resolution + ",nrt:" + nrt);
                }
            case PiVideoResolution._6K:
                if (nrt) {
                    return PilotSDK.CAMERA_PREVIEW_512_320_30;
                } else {
                    return PilotSDK.CAMERA_PREVIEW_2880_1800_15;
                }
            case PiVideoResolution._4K_FPS:
                if (nrt) {
                    return PilotSDK.CAMERA_PREVIEW_480_300_30;
                } else {
                    return PilotSDK.CAMERA_PREVIEW_2192_1370_30;
                }
            case PiVideoResolution._4K_QUALITY:
                if (nrt) {
                    throw new RuntimeException("VideoResolution don't support:" + resolution + ",nrt:" + nrt);
                } else {
                    //return PilotSDK.CAMERA_PREVIEW_3520_2200_24;
                    return PilotSDK.CAMERA_PREVIEW_2880_1800_24;
                }
            case PiVideoResolution._2K:
                if (nrt) {
                    throw new RuntimeException("VideoResolution don't support:" + resolution + ",nrt:" + nrt);
                } else {
                    return PilotSDK.CAMERA_PREVIEW_1920_1200_30;
                }
            default:
                throw new RuntimeException("VideoResolution don't support:" + resolution);
        }
    }

    public static void changeCameraResolutionForPhoto() {
        changeCameraResolutionForPhoto(new ChangeResolutionListener() {
            @Override
            protected void onChangeResolution(int width, int height) {
            }
        });
    }

    public static void changeCameraResolutionForPhoto(ChangeResolutionListener listener) {
        changeCameraResolutionForPhoto(PiPhotoResolution._8K, listener);
    }

    public static void changeCameraResolutionForPhoto(@PiPhotoResolution String resolution, ChangeResolutionListener listener) {
        int[] params = obtainCameraParamsForPhoto(resolution);
        PilotSDK.changeCameraResolution(params, listener);
    }

    @NonNull
    private static int[] obtainCameraParamsForPhoto(@PiPhotoResolution String resolution) {
        switch (resolution) {
            case PiPhotoResolution._8K:
            case PiPhotoResolution._3K:
            case PiPhotoResolution._6K:
            case PiPhotoResolution._4K:
            case PiPhotoResolution._2K:
                break;
            default:
                throw new RuntimeException("VideoResolution don't support:" + resolution);
        }
        return PilotSDK.CAMERA_PREVIEW_4048_2530_15;
    }

    public static void changeCameraResolutionForLive(@PiPushResolution String resolution) {
        changeCameraResolutionForLive(resolution, new ChangeResolutionListener() {
            @Override
            protected void onChangeResolution(int width, int height) {
            }
        });
    }

    public static void changeCameraResolutionForLive(@PiPushResolution String resolution, ChangeResolutionListener listener) {
        int[] params = obtainCameraParamsForLive(resolution);
        PilotSDK.changeCameraResolution(params, listener);
    }

    @NonNull
    private static int[] obtainCameraParamsForLive(@PiPushResolution String resolution) {
        switch (resolution) {
            case PiPushResolution._8K:
                return PilotSDK.CAMERA_PREVIEW_400_250_24;
            case PiPushResolution._8K_7FPS:
                return PilotSDK.CAMERA_PREVIEW_4048_2530_7;
            case PiPushResolution._4K_FPS:
                return PilotSDK.CAMERA_PREVIEW_2192_1370_30;
            case PiPushResolution._4K_QUALITY:
                //return PilotSDK.CAMERA_PREVIEW_3520_2200_24;
                return PilotSDK.CAMERA_PREVIEW_2880_1800_24;
            default:
                throw new RuntimeException("PushResolution don't support:" + resolution);
        }
    }
}
