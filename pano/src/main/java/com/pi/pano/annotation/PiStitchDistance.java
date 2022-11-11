package com.pi.pano.annotation;

import androidx.annotation.FloatRange;

/**
 * stitch distance
 */
@FloatRange(
        from = PiStitchDistance.auto,
        to = PiStitchDistance.infinity
)
public @interface PiStitchDistance {
    /**
     * 自动测量拼接距离
     */
    int auto = -1;
    /**
     * 0:表示标定时候的距离,大概2m
     */
    int zero = 0;
    /**
     * 无穷远,取值：100
     */
    int infinity = 100;
}
