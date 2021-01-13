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

    private final ImageReader mImageReader;
    private final TakePhotoListener mTakePhotoListener;
    private final HandlerThread mThreadHandler;
    private final int mHdrIndex;

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

        String unstitchFilename = null;
        if (mTakePhotoListener.mUnStitchDirPath != null) {
            unstitchFilename = mTakePhotoListener.mUnStitchDirPath + mTakePhotoListener.mFilename + ".jpg";
            File file = new File(unstitchFilename);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        }

        String stitchFilename = null;
        if (mTakePhotoListener.mStitchDirPath != null) {
            stitchFilename = mTakePhotoListener.mStitchDirPath + mTakePhotoListener.mFilename + ".jpg";
            File file = new File(stitchFilename);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        }

        if (mTakePhotoListener.mSaveHdrSourceFile && mTakePhotoListener.mHDRImageCount > 0 && null != unstitchFilename) {
            String hdrDir = unstitchFilename.replace(".jpg", ".hdr");
            File file = new File(hdrDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            PiPano.saveJpeg(hdrDir + "/" + mHdrIndex + ".jpg", null,
                    0, image.getPlanes()[0].getBuffer(), width, height,
                    stride, mTakePhotoListener.mJpegQuilty, mTakePhotoListener.mSaveExif,
                    mTakePhotoListener.mHeading, mTakePhotoListener.mLatitude,
                    mTakePhotoListener.mLongitude, mTakePhotoListener.mAltitude,
                    mTakePhotoListener.mArtist);
        }

        try {
            int ret = PiPano.saveJpeg(unstitchFilename, stitchFilename,
                    mTakePhotoListener.mHDRImageCount, image.getPlanes()[0].getBuffer(), width, height,
                    stride, mTakePhotoListener.mJpegQuilty, mTakePhotoListener.mSaveExif,
                    mTakePhotoListener.mHeading, mTakePhotoListener.mLatitude,
                    mTakePhotoListener.mLongitude, mTakePhotoListener.mAltitude,
                    mTakePhotoListener.mArtist);

            if (ret < 0) {
                mTakePhotoListener.mHDRError = true;
                Log.e(TAG, "saveJpeg error ret: " + ret);
            } else if (ret == 0) {
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
