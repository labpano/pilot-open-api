package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Photo resolution.
 */
@StringDef({
        PiPhotoResolution._5_7K,
})
public @interface PiPhotoResolution {
    /**
     * 5.7K，5760*2880
     */
    String _5_7K = "_5.7K";
}
