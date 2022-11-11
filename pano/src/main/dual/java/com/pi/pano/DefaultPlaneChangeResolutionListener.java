package com.pi.pano;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiPreviewMode;

public class DefaultPlaneChangeResolutionListener extends DefaultChangeResolutionListener {

    public DefaultPlaneChangeResolutionListener(int fieldOfView, @NonNull String aspectRatio) {
        super(fieldOfView, aspectRatio);
    }

    @Override
    protected int getReCaliIntervalFrame(int fps) {
        return 0;
    }

    @Override
    protected void setPreviewParam() {
        PilotSDK.setParamReCaliEnable(getReCaliIntervalFrame(mFps), true);
        int rotateDegree;
        int lensCorrectionMode;
        if ("0".equals(mCameraId)) {
            rotateDegree = 0;
            lensCorrectionMode = 0x90;
        } else {
            lensCorrectionMode = 0x09;
            rotateDegree = 180;
        }
        Pair<Float, Float> obtain = FieldOfViewUtils.obtain(aspectRatio, fieldOfView);
        PilotSDK.setLensCorrectionMode(lensCorrectionMode);
        PilotSDK.setPreviewMode(PiPreviewMode.vlog, rotateDegree, false, obtain.first, obtain.second);
    }
}
