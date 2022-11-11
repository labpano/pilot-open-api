package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Video resolution.
 */
@StringDef({
        PiVideoResolution._5_7K,
        PiVideoResolution._4K,
        PiVideoResolution._2_5K,
})
public @interface PiVideoResolution {
    /**
     * 5.7K，5760*2880
     */
    String _5_7K = "_5.7K";
    /**
     * 4K，3840*1920
     */
    String _4K = "_4K";
    /**
     * 2.5K，2560*1280
     */
    String _2_5K = "_2.5K";
}
