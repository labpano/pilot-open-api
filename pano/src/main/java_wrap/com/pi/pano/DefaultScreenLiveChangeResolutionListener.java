package com.pi.pano;

import android.graphics.ImageFormat;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiPreviewMode;

public class DefaultScreenLiveChangeResolutionListener extends DefaultChangeResolutionListener {
    protected boolean mUsePhoto;

    public DefaultScreenLiveChangeResolutionListener(boolean usePhoto, @NonNull String aspectRatio) {
        super(aspectRatio);
        mUsePhoto = usePhoto;
        PilotSDK.setPreviewImageReaderFormat(getPreviewImageReaderFormat());
    }

    @Override
    protected void setPreviewParam() {
        PilotSDK.setParamReCaliEnable(getReCaliIntervalFrame(mFps), true);
        PilotSDK.setLensCorrectionMode(0x11);
        Pair<Float, Float> obtain = FieldOfViewUtils.obtain(aspectRatio, fieldOfView);
        PilotSDK.setPreviewMode(PiPreviewMode.planet, 180, false, obtain.first, obtain.second);
        PilotSDK.reloadWatermark(false);
    }

    @Override
    protected boolean isLockDefaultPreviewFps() {
        return false;
    }

    @Override
    protected int[] getPreviewImageReaderFormat() {
        return mUsePhoto ? new int[]{ImageFormat.JPEG} : null;
    }
}