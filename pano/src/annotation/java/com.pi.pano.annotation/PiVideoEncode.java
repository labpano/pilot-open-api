package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * 录像视频编码
 */
@StringDef({
        PiVideoEncode.h_264,
        PiVideoEncode.h_265
})
public @interface PiVideoEncode {
    String h_264 = "H.264";
    String h_265 = "H.265";
}
