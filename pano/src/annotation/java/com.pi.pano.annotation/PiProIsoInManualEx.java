package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * 手动曝光时间，ISO
 */
@IntDef({
        PiProIso._100,
        PiProIso._200,
        PiProIso._400,
        PiProIso._600,
        PiProIso._800,
        PiProIso._1600,
        PiProIso._3200
})
public @interface PiProIsoInManualEx {
}
