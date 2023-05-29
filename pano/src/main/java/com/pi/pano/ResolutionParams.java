package com.pi.pano;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiPhotoResolution;
import com.pi.pano.annotation.PiResolution;
import com.pi.pano.annotation.PiVideoResolution;

import java.util.Arrays;

/**
 * 分辨率参数
 */
public class ResolutionParams {
    /**
     * 分辨率、帧率，及镜头id
     * {width, height, fps, camera_id} 或 {width, height, fps}
     */
    public int[] resolutions;

    /**
     * 强制切换分辨率
     */
    public boolean forceChange = false;

    private ResolutionParams(int[] resolutions) {
        this.resolutions = resolutions;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "resolutions=" + Arrays.toString(resolutions) +
                '}';
    }

    public static class Factory {

        public static ResolutionParams createParams(int[] resolutions) {
            return new ResolutionParams(resolutions);
        }

        /**
         * 构建 拍照分辨率
         */
        public static ResolutionParams createParamsForPhoto(@PiPhotoResolution String resolution) {
            if (!PiResolution._5_7K.equals(resolution)) {
                throw new RuntimeException("PiPhotoResolution don't support:" + resolution);
            }
            ResolutionParams params = new ResolutionParams(PilotSDK.CAMERA_PREVIEW_1920_960_30);
            params.forceChange = true;
            return params;
        }

        /**
         * 构建 未拼接视频 分辨率
         */
        @NonNull
        public static ResolutionParams createParamsForUnStitchVideo(@PiVideoResolution String resolution, String fps) {
            int[] resolutions = null;
            switch (resolution) {
                case PiResolution._5_7K:
                    if ("30".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_30;
                    } else if ("25".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_25;
                    } else if ("24".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_24;
                    }
                    break;
                case PiResolution._4K:
                    if ("60".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_60;
                    } else if ("30".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_30;
                    } else if ("25".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_25;
                    } else if ("24".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_24;
                    }
                    break;
                case PiResolution._2_5K:
                    if ("110".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_110;
                    } else if ("90".equals(fps)) {
                        resolutions = PilotSDK.CAMERA_PREVIEW_640_320_90;
                    }
                    break;
                default:
                    break;
            }
            if (null == resolutions) {
                throw new RuntimeException("VideoResolution don't support:" + resolution);
            }
            return new ResolutionParams(resolutions);
        }

        /**
         * 构建 拼接视频 分辨率
         */
        public static ResolutionParams createParamsForStitchVideo(@PiVideoResolution String resolution, String fps) {
            throw new RuntimeException("VideoResolution don't support:" + resolution + ",fps:" + fps);
        }

        /**
         * 构建 平面视频 分辨率
         */
        public static ResolutionParams createParamsForPlaneVideo(
                boolean frontCamera, @PiVideoResolution String resolution, String fps, int fieldOfView, String aspectRatio) {
            int[] resolutions;
            if ("24".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_3040_3040_24;
            } else if ("25".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_3040_3040_25;
            } else if ("30".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_3040_3040_30;
            } else if ("60".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_3040_3040_60;
            } else {
                throw new RuntimeException("VideoResolution don't support:" + resolution + ",fps:" + fps);
            }
            int cameraId = frontCamera ? 0 : 1;
            int[] params = new int[resolutions.length + 1];
            // 摄像头使用单镜头
            params[resolutions.length] = cameraId;
            System.arraycopy(resolutions, 0, params, 0, resolutions.length);
            return new ResolutionParams(params);
        }

        /**
         * 构建 慢动作 分辨率
         */
        public static ResolutionParams createParamsForSlowMotionVideo() {
            return new ResolutionParams(PilotSDK.CAMERA_PREVIEW_640_320_110);
        }

        /**
         * 构建 （延时摄影）分辨率
         */
        public static ResolutionParams createParamsForTimeLapseVideo() {
            int[] params = PilotSDK.CAMERA_PREVIEW_5760_2880_30;
            return new ResolutionParams(params);
        }

        /**
         * 构建 录像（街景）分辨率
         */
        public static ResolutionParams createParamsForStreetViewVideo(@PiVideoResolution String resolution, String fps) {
            int[] resolutions;
            if ("3".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_5760_2880_15;
            } else if ("4".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_5760_2880_16;
            } else if ("7".equals(fps) || "2".equals(fps) || "1".equals(fps)) {
                resolutions = PilotSDK.CAMERA_PREVIEW_5760_2880_14;
            } else {
                throw new RuntimeException("VideoResolution don't support:" + resolution + ",fps:" + fps);
            }
            // TODO 临时处理街景因内存不足引起的崩溃，降低预览分辨率
            resolutions[0] = 5120;
            resolutions[1] = resolutions[0] / 2;
            return new ResolutionParams(resolutions);
        }

        /**
         * 构建 直播分辨率参数
         */
        public static ResolutionParams createParamsForPanoramaLive(String fps) {
            int[] resolutions = PilotSDK.CAMERA_PREVIEW_5760_2880_30;
            int fpsInt;
            try {
                fpsInt = Integer.parseInt(fps);
            } catch (NumberFormatException ignore) {
                fpsInt = PilotSDK.CAMERA_PREVIEW_5760_2880_30[2];
            }
            resolutions[2] = fpsInt;
            return new ResolutionParams(resolutions);
        }

        /**
         * 构建 屏幕直播分辨率
         */
        public static ResolutionParams createParamsForScreenLive(String fps) {
            int[] resolutions = PilotSDK.CAMERA_PREVIEW_5760_2880_30;
            int fpsInt;
            try {
                fpsInt = Integer.parseInt(fps);
            } catch (NumberFormatException ignore) {
                fpsInt = PilotSDK.CAMERA_PREVIEW_5760_2880_30[2];
            }
            resolutions[2] = fpsInt;
            return new ResolutionParams(resolutions);
        }
    }
}
