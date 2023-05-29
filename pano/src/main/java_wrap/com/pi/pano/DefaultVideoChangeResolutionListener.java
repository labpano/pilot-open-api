package com.pi.pano;

import androidx.annotation.NonNull;

public class DefaultVideoChangeResolutionListener extends DefaultChangeResolutionListener {
    public DefaultVideoChangeResolutionListener() {
    }

    public DefaultVideoChangeResolutionListener(@NonNull String aspectRatio) {
        super(aspectRatio);
    }

    public DefaultVideoChangeResolutionListener(int fieldOfView, @NonNull String aspectRatio) {
        super(fieldOfView, aspectRatio);
    }
}
