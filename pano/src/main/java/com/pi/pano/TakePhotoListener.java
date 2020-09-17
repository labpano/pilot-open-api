package com.pi.pano;

/**
 * 拍照回调
 */
public abstract class TakePhotoListener {
    long mTimestamp;
    int mStitchPhotoWidth;
    int mStitchPhotoHeight;
    public boolean mSaveFisheyePhoto;
    public boolean mHDR;
    public int mJpegQuilty = 100;
    volatile boolean mHDRError;

    /**
     * stitch file save dir path.
     */
    public String mStitchDirPath;

    /**
     * Whether to save fisheye pictures.
     */
    public boolean saveFishPicture = true;
    /**
     * Whether to keep hdr source files.
     */
    public boolean mSaveHdrSourceFile;
    /**
     * Ignore how many frames before taking pictures.
     */
    int mSkipFrame;

    public String mFilename;

    /**
     * gps latitude
     */
    public double mLatitude;

    /**
     * gps longitude
     */
    public double mLongitude;

    /**
     * gps altitude
     */
    public int mAltitude;

    /**
     * device head
     */
    public double mHeading;

    /**
     * artist
     */
    public String mArtist;

    public boolean isAuto;

    protected void onTakePhotoStart() {
    }

    /**
     * Complete
     *
     * @param errorCode error code,
     */
    protected void onTakePhotoComplete(int errorCode) {
    }

    /**
     * Need to deal with parameter adjustment for hdr
     *
     * @param state state
     */
    protected void onHdrPhotoParameter(int state) {
    }
}
