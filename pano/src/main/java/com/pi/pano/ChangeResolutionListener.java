package com.pi.pano;

/**
 * Switch resolution listener
 */
public abstract class ChangeResolutionListener {
    public int mWidth;
    public int mHeight;
    public int mFps;
    public String mCameraId;
    public boolean mReopen;
    /**
     * true:photo; false:video.
     */
    public boolean isPhoto = false;

    /**
     * switch complete.
     *
     * @param width  new width
     * @param height new height
     */
    protected void onChangeResolution(int width, int height) {
    }
}
