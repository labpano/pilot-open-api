package com.pi.pano.annotation;

import androidx.annotation.FloatRange;

/**
 * 拼接距离
 */
@FloatRange(
        from = PiStitchingDistance.min,
        to = PiStitchingDistance.infinity
)
public @interface PiStitchingDistance {
    /**
     * -100:大概是0.5m
     */
    int min = -100;
    /**
     * 0:表示标定时候的距离,大概2m
     */
    int zero = 0;
    /**
     * 无穷远,取值：100
     */
    int infinity = +100;
}
