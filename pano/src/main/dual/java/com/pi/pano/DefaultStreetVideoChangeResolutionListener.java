package com.pi.pano;

import com.pi.pano.annotation.PiPreviewMode;

public class DefaultStreetVideoChangeResolutionListener extends DefaultChangeResolutionListener {

    public DefaultStreetVideoChangeResolutionListener() {
        super();
    }

    @Override
    protected int getReCaliIntervalFrame(int fps) {
        // 街景视频每2s拼接一次
        return fps * 2;
    }

    @Override
    protected void setPreviewParam() {
        PilotSDK.setParamReCaliEnable(getReCaliIntervalFrame(mFps), true);
        PilotSDK.setLensCorrectionMode(0x11);
        PilotSDK.setPreviewMode(PiPreviewMode.planet, 180, false, fieldOfView, 0);
    }
}
