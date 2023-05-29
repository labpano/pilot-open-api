package com.pi.pano;

import android.graphics.ImageFormat;

public class DefaultPanoramaLiveChangeResolutionListener extends DefaultChangeResolutionListener {
    protected boolean mUsePhoto;

    public DefaultPanoramaLiveChangeResolutionListener(boolean usePhoto) {
        super();
        mUsePhoto = usePhoto;
        PilotSDK.setPreviewImageReaderFormat(getPreviewImageReaderFormat());
    }

    @Override
    protected int getReCaliIntervalFrame(int fps) {
        return 0;
    }

    @Override
    protected void setPreviewParam() {
        super.setPreviewParam();
        PilotSDK.reloadWatermark(true);
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