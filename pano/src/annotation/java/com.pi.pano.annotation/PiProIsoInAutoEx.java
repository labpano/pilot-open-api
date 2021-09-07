package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * ISO used for auto exposure time.
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
