package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Identification of file or folder names。
 */
@StringDef({
        PiFileStitchFlag.unstitch,
        PiFileStitchFlag.stitch,
        PiFileStitchFlag.none
})
public @interface PiFileStitchFlag {
    /**
     * 未拼接
     */
    String unstitch = "_u";
    /**
     * 已拼接
     */
    String stitch = "_s";
    /**
     * 无需拼接
     */
    String none = "_n";
}
