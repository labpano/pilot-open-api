package com.pi.pano.wrap;

import androidx.annotation.NonNull;

import com.pi.pano.DefaultScreenLiveChangeResolutionListener;

public class SampleScreenLiveResolutionListener extends DefaultScreenLiveChangeResolutionListener {

    public SampleScreenLiveResolutionListener(@NonNull String aspectRatio) {
        super(aspectRatio);
    }

    @Override
    protected int getReCaliIntervalFrame(int fps) {
        return 0;
    }
}
