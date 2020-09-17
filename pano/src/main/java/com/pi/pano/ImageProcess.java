package com.pi.pano;

import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.File;

class ImageProcess implements ImageReader.OnImageAvailableListener {
    private static final String TAG = ImageProcess.class.getSimpleName();

    private ImageReader mImageReader;
    private TakePhotoListener mTakePhotoListener;

    private HandlerThread mThreadHandler;
    private int mHdrIndex;

    ImageProcess(int width, int height, int hdrIndex, TakePhotoListener listener) {
        mTakePhotoListener = listener;
        mHdrIndex = hdrIndex;

        mThreadHandler = new HandlerThread("ImageProcess");
        mThreadHandler.start();

        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        mImageReader.setOnImageAvailableListener(this, new Handler(mThreadHandler.getLooper()));
    }

    Surface getImageReaderSurface() {
        if (mImageReader == null) {
            return null;
        }
        return mImageReader.getSurface();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        long begintime = System.currentTimeMillis();

        Image image = reader.acquireLatestImage();

        int width = image.getWidth();
        int height = image.getHeight();
        int stride = image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride();
        Log.i(TAG, "width: " + image.getWidth() + " height: " + image.getHeight() + " stride: " + stride);

        boolean isThumbnail = width < 1500;

        File filename = null;
        if (isThumbnail) {
            filename = new File("/sdcard/DCIM/Thumbs/" + mTakePhotoListener.mFilename + ".jpg");
        } else if (mTakePhotoListener.mHDR) {
            if (mTakePhotoListener.mSaveHdrSourceFile) {
                filename = new File("/sdcard/DCIM/Photos/Unstitched/" +
                        mTakePhotoListener.mFilename + ".hdr/" + mHdrIndex + ".jpg");
            }
        } else {
            filename = new File("/sdcard/DCIM/Photos/Unstitched/" + mTakePhotoListener.mFilename + ".jpg");
        }
        if (filename != null && !filename.getParentFile().exists()) {
            filename.getParentFile().mkdirs();
        }

        File stitchFilename = null;
        if (!isThumbnail) {
            if (null == mTakePhotoListener.mStitchDirPath) {
                stitchFilename = new File("/sdcard/DCIM/Photos/Stitched/" + mTakePhotoListener.mFilename + ".jpg");
            } else {
                stitchFilename = new File(mTakePhotoListener.mStitchDirPath + mTakePhotoListener.mFilename + ".jpg");
            }
            if (!stitchFilename.getParentFile().exists()) {
                stitchFilename.getParentFile().mkdirs();
            }
        }

        try {
            int ret = 0;

            if (filename != null) {
                ret = PiPano.saveJpeg(filename.getPath(),
                        stitchFilename == null || mTakePhotoListener.mHDR ? null : stitchFilename.getPath(),
                        false, image.getPlanes()[0].getBuffer(), width, height,
                        stride, !isThumbnail ? mTakePhotoListener.mJpegQuilty : 30, !isThumbnail,
                        mTakePhotoListener.mHeading, mTakePhotoListener.mLatitude,
                        mTakePhotoListener.mLongitude, mTakePhotoListener.mAltitude, -1,
                        mTakePhotoListener.mArtist, mTakePhotoListener.saveFishPicture);
            }

            if (ret != 0) {
                ret += 100;
                mTakePhotoListener.mHDRError = true;
            }

            //If it is a thumbnail, then unlock it, because you must take a thumbnail before taking a big picture,
            // and wait for synchronization before taking a big picture.
            if (mTakePhotoListener.mHDR) {
                if (mTakePhotoListener.mHDRError) {
                    Log.e(TAG, "take photo error ret: " + ret);
                    mTakePhotoListener.onTakePhotoComplete(ret);
                } else {
                    File hdrFilename = new File("/sdcard/DCIM/Photos/Unstitched/" +
                            mTakePhotoListener.mFilename + ".jpg");
                    if (!hdrFilename.getParentFile().exists()) {
                        hdrFilename.getParentFile().mkdirs();
                    }
                    //The internal execution of saveJpeg below is synthetic hdr
                    ret = PiPano.saveJpeg(hdrFilename.getPath(),
                            stitchFilename == null ? null : stitchFilename.getPath(),
                            true, image.getPlanes()[0].getBuffer(), width, height,
                            stride, 100, true,
                            mTakePhotoListener.mHeading, mTakePhotoListener.mLatitude,
                            mTakePhotoListener.mLongitude, mTakePhotoListener.mAltitude, -1,
                            mTakePhotoListener.mArtist, mTakePhotoListener.saveFishPicture);
                    if (ret < 0) {
                        ret += 200;
                        mTakePhotoListener.mHDRError = true;
                        Log.e(TAG, "take hdr error ret: " + ret);
                    }
                    //If it returns 1, it means that pictures are being added to hdrImageList and no callback
                    if (ret != 1) {
                        mTakePhotoListener.onTakePhotoComplete(ret);
                    }
                }
            } else {
                if (ret != 0) {
                    Log.e(TAG, "take photo error ret: " + ret);
                }
                mTakePhotoListener.onTakePhotoComplete(ret);
            }
            image.close();
            reader.close();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            mTakePhotoListener.mHDRError = true;
            mTakePhotoListener.onTakePhotoComplete(400);
        }

        Log.i(TAG, "take photo cost time: " + (System.currentTimeMillis() - begintime) + "ms");

        mThreadHandler.quitSafely();
    }
}
