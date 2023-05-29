package com.pi.pano.wrap.annotation;

import androidx.annotation.StringDef;

/**
 * Steady direction.
 */
@StringDef({
        PiSteadyOrientation.fix,
        PiSteadyOrientation.follow
})
public @interface PiSteadyOrientation {
    /**
     * Fixed, default.
     */
    String fix = "fix";
    /**
     * Follow camera orientation.
     */
    String follow = "follow";
}
