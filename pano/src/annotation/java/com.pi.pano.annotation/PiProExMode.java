package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Exposure time mode.
 */
@IntDef({
        PiProExMode.auto,
        PiProExMode.manual,
})
public @interface PiProExMode {
    int auto = 0;
    int manual = 1;
}
