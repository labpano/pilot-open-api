package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * video coding.
 */
@StringDef({
        PiVideoEncode.h_264,
        PiVideoEncode.h_265
})
public @interface PiVideoEncode {
    String h_264 = "H.264";
    String h_265 = "H.265";
}
