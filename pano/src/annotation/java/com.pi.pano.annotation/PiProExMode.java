package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * 曝光模式
 */
@IntDef({
        PiProExMode.auto,
        PiProExMode.manual,
})
public @interface PiProExMode {
    int auto = 0;
    int manual = 1;
}
