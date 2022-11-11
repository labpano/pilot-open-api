package com.pi.pano;

public class DefaultLiveChangeResolutionListener extends DefaultChangeResolutionListener {

    public DefaultLiveChangeResolutionListener() {
        super();
    }

    @Override
    protected int getReCaliIntervalFrame(int fps) {
        return 0;
    }
}