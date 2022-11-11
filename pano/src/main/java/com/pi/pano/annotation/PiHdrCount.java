package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Number of hdr composite photo.
 */
@IntDef({
        PiHdrCount.out,
        PiHdrCount._3p,
        PiHdrCount._5p,
        PiHdrCount._7p,
        PiHdrCount._9p
})
public @interface PiHdrCount {
    int out = 0;
    int _3p = 3;
    int _5p = 5;
    int _7p = 7;
    int _9p = 9;
}