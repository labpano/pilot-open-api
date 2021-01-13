package com.pi.pano;

import com.pi.pano.annotation.PiHdrCountSupport;

/**
 * Photo listener
 */
public abstract class TakePhotoListener {
    long mTimestamp;
    int mStitchPhotoWidth;
    int mStitchPhotoHeight;

    /**
     * (roam) record the location information
     */
    public boolean mMakeSlamPhotoPoint = false;

    /**
     * JPEG image quality
     */
    public int mJpegQuilty = 100;

    /**
     * Save EXIF information
     */
    public boolean mSaveExif = true;

    /**
     * Is the captured image a mosaic image
     */
    public boolean mIsStitched;

    /**
     * Number of HDR photos
     */
    @PiHdrCountSupport
    public int mHDRImageCount;

    volatile boolean mHDRError;

    /**
     * stitch file save dir path.
     */
    public String mStitchDirPath = "/sdcard/DCIM/Photos/Stitched/";

    /**
     * Whether to save fisheye pictures.
     */
    public String mUnStitchDirPath = "/sdcard/DCIM/Photos/Unstitched/";

    /**
     * Whether to keep hdr source files.
     */
    public boolean mSaveHdrSourceFile;

    /**
     * Ignore how many frames before taking pictures.
     */
    public int mSkipFrame;

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

    protected void onTakePhotoStart() {
    }

    /**
     * Complete
     */
    protected void onTakePhotoComplete(int errorCode) {
    }

    /**
     * Need to deal with parameter adjustment for hdr
     */
    protected void onHdrPhotoParameter(int hdrIndex, int hdrCount) {
    }
}
