package com.pi.pano.wrap.annotation;

import androidx.annotation.StringDef;

/**
 * 街景视频帧率
 */
@StringDef({
        PiStreetVideoFps._0_3_fps,
        PiStreetVideoFps._1_fps,
        PiStreetVideoFps._2_fps,
        PiStreetVideoFps._3_fps,
        PiStreetVideoFps._4_fps,
        PiStreetVideoFps._7_fps,
})
public @interface PiStreetVideoFps {
    /**
     * 0.3fps
     */
    String _0_3_fps = "0.3";
    /**
     * 1fps
     */
    String _1_fps = "1";
    /**
     * 2fps
     */
    String _2_fps = "2";
    /**
     * 3fps
     */
    String _3_fps = "3";
    /**
     * 4fps
     */
    String _4_fps = "4";
    /**
     * 7fps
     */
    String _7_fps = "7";
}
