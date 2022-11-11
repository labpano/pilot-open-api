package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * white balance.
 */
@StringDef({
        PiWhiteBalance.auto,
        PiWhiteBalance.fluorescent,
        PiWhiteBalance.incandescent,
        PiWhiteBalance.cloudy_daylight,
        PiWhiteBalance.daylight
})
public @interface PiWhiteBalance {
    /**
     * Auto
     */
    String auto = "auto";
    /**
     * fluorescent
     */
    String fluorescent = "fluorescent";
    /**
     * incandescent
     */
    String incandescent = "incandescent";
    /**
     * cloudy-daylight
     */
    String cloudy_daylight = "cloudy-daylight";
    /**
     * daylight
     */
    String daylight = "daylight";
}
