package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Number of photos during hdr synthesis.
 */
@IntDef({
        PiHdrCountSupport.out,
        PiHdrCountSupport._3p,
        PiHdrCountSupport._5p
})
public @interface PiHdrCountSupport {
    int out = 0;
    int _3p = 3;
    int _5p = 5;
}
