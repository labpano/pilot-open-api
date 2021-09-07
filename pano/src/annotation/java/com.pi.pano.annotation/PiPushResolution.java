package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Resolution available for live push.
 */
@StringDef({
        PiPushResolution._8K,
        PiPushResolution._8K_7FPS,
        PiPushResolution._4K_FPS,
        PiPushResolution._4K_QUALITY
})
public @interface PiPushResolution {
    /**
     * 8k
     */
    String _8K = "7680*3840";
    /**
     * 8k-7pfs
     */
    String _8K_7FPS = "7680*3840#7";
    /**
     * 4k-帧率优先
     */
    String _4K_FPS = "3840*1920";
    /**
     * 4k-画质优先
     */
    String _4K_QUALITY = "3840*1920#2";
}
