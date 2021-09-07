package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Resolution available for video.
 */
@StringDef({
        PiVideoResolution._8K,
        PiVideoResolution._7K,
        PiVideoResolution._6K,
        PiVideoResolution._4K_FPS,
        PiVideoResolution._4K_QUALITY,
        PiVideoResolution._2K
})
public @interface PiVideoResolution {
    /**
     * 8k
     */
    String _8K = "7680*3840";
    /**
     * 7k
     */
    String _7K = "7040*3520";
    /**
     * 6k
     */
    String _6K = "5760*2880";
    /**
     * 4k,Frame rate priority.
     */
    String _4K_FPS = "3840*1920";
    /**
     * 4k,Picture quality priority.
     */
    String _4K_QUALITY = "3840*1920#2";
    /**
     * 2k
     */
    String _2K = "1920*960";
}
