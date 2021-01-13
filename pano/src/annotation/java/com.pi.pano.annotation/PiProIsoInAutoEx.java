package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * 自动曝光时间，ISO
 */
@IntDef({
        PiProIso.auto,
        PiProIso._50,
        PiProIso._100,
        PiProIso._200,
        PiProIso._400,
        PiProIso._800,
        PiProIso._1600
})
public @interface PiProIsoInAutoEx {
}
