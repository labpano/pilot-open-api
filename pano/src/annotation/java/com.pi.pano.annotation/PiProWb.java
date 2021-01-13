package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * 白平衡
 */
@StringDef({
        PiProWb.auto,
        PiProWb.fluorescent,
        PiProWb.incandescent,
        PiProWb.cloudy_daylight,
        PiProWb.daylight,
})
public @interface PiProWb {
    /**
     * 自动
     */
    String auto = "auto";
    /**
     * 荧光灯
     */
    String fluorescent = "fluorescent";
    /**
     * 白炽灯
     */
    String incandescent = "incandescent";
    /**
     * 阴天
     */
    String cloudy_daylight = "cloudy-daylight";
    /**
     * 日光
     */
    String daylight = "daylight";
}
