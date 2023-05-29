package com.pi.pano.wrap;

import com.pi.pano.wrap.annotation.PiStreetVideoFps;

/**
 * 街景视频
 */
public final class StreetVideoUtils {
    private static final String TAG = StreetVideoUtils.class.getSimpleName();

    /**
     * 获取街景比例
     */
    public static double getsSaleForStreetVideo(int previewFps, @PiStreetVideoFps String streetVideoFps) {
        return 1.0f;
    }

    /**
     * 获取街景帧率倍率
     */
    public static int getFpsRatioForStreetVideo(int previewFps, @PiStreetVideoFps String streetVideoFps) {
        if (previewFps < 1) {
            throw new RuntimeException("Street Video‘s previewFps is 0");
        }
        if (String.valueOf(previewFps).equals(streetVideoFps)) {
            return 0;
        }
        double _streetVideoFps = Double.parseDouble(streetVideoFps);
        return (int) Math.ceil(previewFps / _streetVideoFps);
    }

    /**
     * 录像过程最短时长，小于此值可能出现无效文件。
     *
     * @param fps 帧率
     * @return 录像过程最短时长（包含本数）。单位秒。
     */
    public static int getStreetVideoMinTime(String fps) {
        if (PiStreetVideoFps._0_3_fps.equals(fps)) {
            return 10;
        }
        return 5;
    }
}
