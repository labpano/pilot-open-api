package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * ISO used for manual exposure time
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
