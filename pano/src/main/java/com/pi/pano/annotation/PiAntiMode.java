package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Anti flicker
 */
@StringDef({
        PiAntiMode.off,
        PiAntiMode._50Hz,
        PiAntiMode._60Hz,
        PiAntiMode.auto
})
public @interface PiAntiMode {

    /**
     * off
     */
    String off = "off";

    String _50Hz = "50Hz";

    String _60Hz = "60Hz";
    /**
     * auto
     */
    String auto = "auto";
}