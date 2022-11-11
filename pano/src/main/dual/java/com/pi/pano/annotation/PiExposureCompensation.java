package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Exposure Compensation
 */
@IntDef({
        PiExposureCompensation.reduce_4,
        PiExposureCompensation.reduce_3,
        PiExposureCompensation.reduce_2,
        PiExposureCompensation.reduce_1,
        PiExposureCompensation.normal,
        PiExposureCompensation.enhance_1,
        PiExposureCompensation.enhance_2,
        PiExposureCompensation.enhance_3,
        PiExposureCompensation.enhance_4
})
public @interface PiExposureCompensation {
    int reduce_4 = -4;
    int reduce_3 = -3;
    int reduce_2 = -2;
    int reduce_1 = -1;
    int normal = 0;
    int enhance_1 = +1;
    int enhance_2 = +2;
    int enhance_3 = +3;
    int enhance_4 = +4;
}
