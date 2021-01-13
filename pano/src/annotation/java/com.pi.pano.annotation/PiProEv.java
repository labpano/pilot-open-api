package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * ev
 */
@IntDef({
        PiProEv.reduce_1,
        PiProEv.reduce_2,
        PiProEv.reduce_3,
        PiProEv.reduce_4,
        PiProEv.normal,
        PiProEv.enhance_1,
        PiProEv.enhance_2,
        PiProEv.enhance_3,
        PiProEv.enhance_4
})
public @interface PiProEv {
    /**
     * 减弱，最暗
     */
    int reduce_4 = -4;

    int reduce_3 = -3;
    int reduce_2 = -2;
    int reduce_1 = -1;

    /**
     * 正常
     */
    int normal = 0;

    int enhance_1 = +1;
    int enhance_2 = +2;
    int enhance_3 = +3;

    /**
     * 增强，最亮
     */
    int enhance_4 = +4;

    int[] supportList = new int[]{reduce_4, reduce_3, reduce_2, reduce_1, normal, enhance_1, enhance_2, enhance_3, enhance_4};
}
