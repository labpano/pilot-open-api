package com.pi.pano.annotation;

import androidx.annotation.FloatRange;

/**
 * Stitching distance.
 */
@FloatRange(
        from = PiStitchingDistance.auto,
        to = PiStitchingDistance.infinity
)
public @interface PiStitchingDistance {
    /**
     * Automatically measure the stitching distance.
     * Taking pictures and real-time video recording finally adopts automatic measurement of the stitching distance.
     */
    int auto = -1;
    /**
     * The distance during calibration, about 2m.
     */
    int zero = 0;
    /**
     * Infinity.
     */
    int infinity = 100;
}
