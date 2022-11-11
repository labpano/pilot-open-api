package com.pi.pano.annotation;

import androidx.annotation.StringDef;

/**
 * Photo file format.
 */
@StringDef({
        PiPhotoFileFormat.jpg,
        PiPhotoFileFormat.jpg_dng,
        PiPhotoFileFormat.jpg_raw
})
public @interface PiPhotoFileFormat {
    String jpg = "jpg";
    String raw = "raw";
    String jpg_dng = "jpg+dng";
    String jpg_raw = "jpg+raw";
}
