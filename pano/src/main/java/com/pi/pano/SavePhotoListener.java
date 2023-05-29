package com.pi.pano;

public abstract class SavePhotoListener {
    /**
     * 文件
     */
    public String mFilename;

    /**
     * 新文件命
     */
    public String mNewFilename;

    /**
     * 宽
     */
    public int mWidth;

    /**
     * 高
     */
    public int mHeight;

    /**
     * 是否为缩略图
     */
    public boolean mThumbnail;

    protected void onSavePhotoStart() {
    }

    /**
     * 拍照回调
     */
    protected void onSavePhotoComplete() {
    }

    /**
     * 拍照回调
     */
    protected void onSavePhotoFailed() {
    }
}
