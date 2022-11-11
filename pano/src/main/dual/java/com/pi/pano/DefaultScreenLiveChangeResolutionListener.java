package com.pi.pano;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiPreviewMode;

public class DefaultScreenLiveChangeResolutionListener extends DefaultChangeResolutionListener {

    public DefaultScreenLiveChangeResolutionListener(@NonNull String aspectRatio) {
        super(aspectRatio);
    }

    @Override
    protected void setPreviewParam() {
        PilotSDK.setParamReCaliEnable(getReCaliIntervalFrame(mFps), true);
        PilotSDK.setLensCorrectionMode(0x11);
        Pair<Float, Float> obtain = FieldOfViewUtils.obtain(aspectRatio, fieldOfView);
        PilotSDK.setPreviewMode(PiPreviewMode.planet, 180, false, obtain.first, obtain.second);
    }
}