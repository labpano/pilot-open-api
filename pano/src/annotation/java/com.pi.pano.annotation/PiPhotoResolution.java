package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Resolution available for photo.
 */
@StringDef({
        PiPhotoResolution._8K,
        PiPhotoResolution._6K,
        PiPhotoResolution._4K,
        PiPhotoResolution._3K,
        PiPhotoResolution._2K
})
public @interface PiPhotoResolution {
    /**
     * 8k
     */
    String _8K = "8192*4096";
    /**
     * 6k
     */
    String _6K = "6144*3072";
    /**
     * 4k
     */
    String _4K = "4096*2048";
    /**
     * 3k
     */
    String _3K = "3072*1536";
    /**
     * 2k
     */
    String _2K = "2048*1024";
}
