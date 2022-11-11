package com.pi.pano;

public abstract class SavePhotoListener {
    public String mFilename;
    public String mNewFilename;
    public int mWidth;
    public int mHeight;
    public boolean mThumbnail;

    protected void onSavePhotoStart() {
    }

    protected void onSavePhotoComplete() {
    }

    protected void onSavePhotoFailed() {
    }
}
