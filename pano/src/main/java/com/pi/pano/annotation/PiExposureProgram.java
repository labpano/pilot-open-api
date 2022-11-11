package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Exposure time Program
 */
@IntDef({
        PiExposureProgram.auto,
        PiExposureProgram.manual,
})
public @interface PiExposureProgram {
    /**
     * auto,default.
     */
    int auto = 0;
    /**
     * manual
     */
    int manual = 1;
}
