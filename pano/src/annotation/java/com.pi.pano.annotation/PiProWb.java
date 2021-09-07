package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * White balance.
 */
@StringDef({
        PiProWb.auto,
        PiProWb.fluorescent,
        PiProWb.incandescent,
        PiProWb.cloudy_daylight,
        PiProWb.daylight,
})
public @interface PiProWb {
    String auto = "auto";
    String fluorescent = "fluorescent";
    String incandescent = "incandescent";
    String cloudy_daylight = "cloudy-daylight";
    String daylight = "daylight";
}
