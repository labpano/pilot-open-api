package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Panorama preview mode.
 */
@IntDef({
        PiPreviewMode.planet,
        PiPreviewMode.immersion,
        PiPreviewMode.fish_eye,
        PiPreviewMode.plane,
        PiPreviewMode.flat
})
public @interface PiPreviewMode {
    int planet = 0;
    int immersion = 1;
    int fish_eye = 2;
    int plane = 3;
    int flat = 5;
}
