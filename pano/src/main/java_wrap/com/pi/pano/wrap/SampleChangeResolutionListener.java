package com.pi.pano.wrap;

import androidx.annotation.NonNull;

import com.pi.pano.DefaultChangeResolutionListener;

public class SampleChangeResolutionListener extends DefaultChangeResolutionListener {

    public SampleChangeResolutionListener() {
        super();
    }

    public SampleChangeResolutionListener(@NonNull String aspectRatio) {
        super(aspectRatio);
    }

    public SampleChangeResolutionListener(int fieldOfView, String aspectRatio) {
        super(fieldOfView, aspectRatio);
    }
}
