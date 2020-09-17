package com.pi.pano;

/**
 * Switch resolution monitor interface
 */
public abstract class ChangeResolutionListener {
    public int mWidth;
    public int mHeight;
    public int mFps;
    public boolean mReopen;

    /**
     * Switch resolution monitor interface
     *
     * @param width  Width after switching
     * @param height Height after switching
     */
    protected abstract void onChangeResolution(int width, int height);
}
