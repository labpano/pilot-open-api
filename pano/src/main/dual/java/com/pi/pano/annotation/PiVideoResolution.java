package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Video resolution.
 */
@StringDef({
        PiResolution._5_7K,
        PiResolution._4K,
        PiResolution._2_5K,
})
public @interface PiVideoResolution {
}
