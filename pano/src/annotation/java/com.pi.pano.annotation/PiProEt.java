package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * ExposedTime
 */
@IntDef({
        PiProEt.normal,
        PiProEt._15,
        PiProEt._50,
        PiProEt._100,
        PiProEt._500,
        PiProEt._1000,
        PiProEt._3200
})
public @interface PiProEt {
    int normal = 0;
    int _3200 = 3200;
    int _1000 = 1000;
    int _500 = 500;
    int _100 = 100;
    int _50 = 50;
    int _15 = 15;
}
