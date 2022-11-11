package com.pi.pano;

import android.graphics.PixelFormat;

import com.pi.pano.annotation.PiPhotoFileFormat;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Take photo listener.
 */
public abstract class TakePhotoListener {
    long mTimestamp;
    int mStitchPhotoWidth;
    int mStitchPhotoHeight;

    /**
     * (Slam take photos) Record the information of the fixed point
     */
    public boolean mMakeSlamPhotoPoint = false;

    /**
     * Jpeg quality.
     */
    public int mJpegQuilty = 100;

    /**
     * save exif.
     */
    public boolean mSaveExif = true;

    /**
     * thumb
     */
    boolean mIsThumb = false;
    String thumbFilePath;

    /**
     * Whether the captured images are stitch images。
     */
    public boolean mIsStitched;

    /**
     * hdr photo count
     */
    public int mHDRImageCount;

    volatile boolean mHDRError;

    /**
     * Stitch photo saving folder path.
     */
    public String mStitchDirPath = "/sdcard/DCIM/Photos/Stitched/";

    /**
     * Unstitch photo saving folder path.
     */
    public String mUnStitchDirPath = "/sdcard/DCIM/Photos/Unstitched/";

    /**
     * save hdr source file.
     */
    public boolean mSaveHdrSourceFile;
    /**
     * Number of frames initially ignored
     */
    public int mInitSkipFrame;
    /**
     * Take pictures after ignoring frames.
     */
    public int mSkipFrame;

    public String mFilename;

    /**
     * Stitch file
     */
    public File mStitchFile;
    /**
     * Unstitch file
     */
    public File mUnStitchFile;

    /**
     * Latitude
     */
    public double mLatitude;

    /**
     * Longitude
     */
    public double mLongitude;

    /**
     * Altitude
     */
    public int mAltitude;

    /**
     * Heading
     */
    public double mHeading;

    /**
     * Artist
     */
    public String mArtist;
    /**
     * Software version name.
     */
    public String mSoftware;

    /**
     * Image format.
     */
    public int mImageFormat = PixelFormat.RGBA_8888;
    /**
     * File format.
     */
    @PiPhotoFileFormat
    public String mFileFormat = PiPhotoFileFormat.jpg;

    int hdr_exposureTime;
    int hdr_iso;
    int hdr_ev;
    int hdr_wb;

    private int totalTakeCount = 1;
    private final AtomicInteger takeCount = new AtomicInteger(0);

    public void setTotalTakeCount(int count) {
        totalTakeCount = count;
    }

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
        if (takeCount.incrementAndGet() >= totalTakeCount) {
            onTakePhotoComplete(errorCode);
        }
    }

    /**
     * Photo acquisition is completed.
     *
     * @param errorCode error code
     */
    protected void onTakePhotoComplete(int errorCode) {
    }
}
