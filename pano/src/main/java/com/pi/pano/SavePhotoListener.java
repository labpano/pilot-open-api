package com.pi.pano;


public abstract class SavePhotoListener {
    /**
     * original file
     */
    public String mFilename;

    /**
     * new file
     */
    public String mNewFilename;

    /**
     * new width
     */
    public int mWidth;

    /**
     * new height
     */
    public int mHeight;

    /**
     * Is it a thumbnail
     */
    public boolean mThumbnail;

    protected void onSavePhotoStart() {
    }

    /**
     * Complete callback
     */
    protected void onSavePhotoComplete() {
    }

    /**
     * Failed callback
     */
    protected void onSavePhotoFailed() {
    }
}
