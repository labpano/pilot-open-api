package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Push resolution.
 */
@StringDef({
        PiPushResolution._5_7K,
        PiPushResolution._4K,
        PiPushResolution._3K,
})
public @interface PiPushResolution {
    /**
     * 5.7K
     */
    String _5_7K = "_5.7K";
    /**
     * 4K,3840*1920
     */
    String _4K = "_4K";
    /**
     * 3K
     */
    String _3K = "_3K";
}
