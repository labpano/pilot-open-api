package com.pi.pano;

import android.graphics.PixelFormat;

import com.pi.pano.annotation.PiPhotoFileFormat;

import java.io.File;

/**
 * Take photo listener.
 */
public abstract class TakePhotoListener {
    public CaptureParams mParams;

    long mTimestamp;
    int mStitchPhotoWidth;
    int mStitchPhotoHeight;

    /**
     * save exif.
     */
    public boolean mSaveExif = true;

    /**
     * thumb
     */
    boolean mIsThumb = false;

    public boolean mIsStitched;

    public int mInitSkipFrame;
    public int mSkipFrame;

    public String mFilename;
    String thumbFilePath;

    public File mHdrSourceDir;
    public int mHdrSourceFileCount = 0;
    volatile boolean mHDRError;

    /**
     * Stitch file
     */
    public File mStitchFile;
    /**
     * Unstitch file
     */
    public File mUnStitchFile;

    /**
     * Image format.
     */
    public int mImageFormat = PixelFormat.RGBA_8888;

    /**
     * Parameter setting callback before HDR photography acquisition
     *
     * @param hdrIndex hdr index
     * @param hdrCount hdr count
     */
    protected void onHdrPhotoParameter(int hdrIndex, int hdrCount) {
    }

    /**
     * Photo acquisition starts.
     * Take photos for several times.
     *
     * @param index index
     */
    protected void onTakePhotoStart(int index) {
    }

    void dispatchTakePhotoComplete(int errorCode) {
        onTakePhotoComplete(errorCode);
    }

    /**
     * Photo acquisition is completed.
     *
     * @param errorCode error code
     */
    protected void onTakePhotoComplete(int errorCode) {
        if (errorCode == 0 && mSaveExif) {
            if (null != mUnStitchFile && mUnStitchFile.isFile() &&
                    mUnStitchFile.getName().endsWith(PiPhotoFileFormat.jpg) &&
                    mParams.hdrCount <= 0) {
                ThumbnailGenerator.injectExifThumbnailForUnStitch(mUnStitchFile);
            }
        }
    }
}
