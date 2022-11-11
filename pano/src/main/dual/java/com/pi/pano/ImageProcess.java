package com.pi.pano;

import android.graphics.ImageFormat;
import android.hardware.camera2.DngCreator;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.pi.pano.annotation.PiFileStitchFlag;
import com.pi.pano.annotation.PiPhotoFileFormat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class ImageProcess implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "ImageProcess";

    private final ImageReader mImageReader;
    private TakePhotoListener mTakePhotoListener;
    private HandlerThread mThreadHandler;
    private int mHdrIndex;
    private final int mImageFormat;
    private DngCreator mDngCreator;

    ImageProcess(int width, int height, int imageFormat) {
        mImageFormat = imageFormat;
        mImageReader = ImageReader.newInstance(width, height, mImageFormat, 1);
    }

    ImageProcess(int width, int height, int imageFormat, int hdrIndex, TakePhotoListener listener) {
        this(width, height, imageFormat);
        mTakePhotoListener = listener;
        mHdrIndex = hdrIndex;
        start();
    }

    public void start() {
        Log.i(TAG, "start : " + mImageFormat);
        mThreadHandler = new HandlerThread("ImageProcess");
        mThreadHandler.start();
        mImageReader.setOnImageAvailableListener(this, new Handler(mThreadHandler.getLooper()));
    }

    Surface getImageReaderSurface() {
        if (mImageReader == null) {
            return null;
        }
        return mImageReader.getSurface();
    }

    public void setDngCreator(DngCreator dngCreator) {
        mDngCreator = dngCreator;
    }

    public void setTakePhotoListener(TakePhotoListener takePhotoListener) {
        mTakePhotoListener = takePhotoListener;
    }

    public void setHdrIndex(int hdrIndex) {
        mHdrIndex = hdrIndex;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        long beginTime = System.currentTimeMillis();

        Image image = reader.acquireLatestImage();

        int width = image.getWidth();
        int height = image.getHeight();
        int stride = 1;
        if (image.getPlanes()[0].getPixelStride() != 0) {
            stride = image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride();
        } else if (mTakePhotoListener.mHDRImageCount > 0) {
            stride = width;
        }
        Log.i(TAG, "width: " + width + " height: " + height + " stride: " + stride);

        String unstitchFilename = null;
        if (mTakePhotoListener.mUnStitchDirPath != null) {
            File file;
            if (mTakePhotoListener.mIsThumb) {
                file = new File(mTakePhotoListener.mUnStitchDirPath, mTakePhotoListener.mFilename
                        + ".jpg");
            } else {
                file = new File(mTakePhotoListener.mUnStitchDirPath, mTakePhotoListener.mFilename
                        + PiFileStitchFlag.unstitch
                        + (mTakePhotoListener.mHDRImageCount > 0 ? "_hdr" : "")
                        + ".jpg");
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            unstitchFilename = file.getAbsolutePath();
            mTakePhotoListener.mUnStitchFile = file;
        }

        String stitchFilename = null;
        if (mTakePhotoListener.mStitchDirPath != null) {
            File file;
            if (mTakePhotoListener.mIsThumb) {
                file = new File(mTakePhotoListener.mStitchDirPath, mTakePhotoListener.mFilename
                        + ".jpg");
            } else {
                file = new File(mTakePhotoListener.mStitchDirPath, mTakePhotoListener.mFilename
                        + PiFileStitchFlag.stitch
                        + (mTakePhotoListener.mHDRImageCount > 0 ? "_hdr" : "")
                        + ".jpg");
            }
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            stitchFilename = file.getAbsolutePath();
            mTakePhotoListener.mStitchFile = file;
        }

        if (mImageFormat == ImageFormat.RAW_SENSOR) {
            FileOutputStream output = null;
            try {
                unstitchFilename = unstitchFilename.replace("_hdr", "");
                if (PiPhotoFileFormat.jpg_dng.equals(mTakePhotoListener.mFileFormat)) {
                    int sleepRetry = 30;
                    while (mDngCreator == null && sleepRetry > 0) {
                        SystemClock.sleep(100);
                        sleepRetry--;
                    }
                    if (mDngCreator != null) {
                        unstitchFilename = unstitchFilename.replace(".jpg", ".dng");
                        output = new FileOutputStream(unstitchFilename);
                        mDngCreator.writeImage(output, image);
                    }
                } else {
                    saveRawFile(unstitchFilename, image);
                }
                mTakePhotoListener.dispatchTakePhotoComplete(0);
            } catch (IOException e) {
                e.printStackTrace();
                mTakePhotoListener.dispatchTakePhotoComplete(400);
            } finally {
                image.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (mImageFormat == ImageFormat.JPEG && mTakePhotoListener.mHDRImageCount <= 0) {
            PiPano.spatialJpeg(unstitchFilename, image.getPlanes()[0].getBuffer(), width, height, mTakePhotoListener.mHeading,
                    (System.currentTimeMillis() - SystemClock.elapsedRealtime()) * 1000 + image.getTimestamp() / 1000);
            image.close();
            if (!mTakePhotoListener.mIsThumb) {
                addPhotoExifInfo(unstitchFilename);
            }
            mTakePhotoListener.dispatchTakePhotoComplete(0);
        } else {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bitmapBytes = null;
            if (!mTakePhotoListener.mIsThumb) {
                long start = System.currentTimeMillis();
                bitmapBytes = BitmapUtils.bitmapToRgba(BitmapUtils.loadBitmap(buffer));
                Log.i(TAG, "buffer to rgba end :" + (System.currentTimeMillis() - start));
                if (bitmapBytes == null) {
                    buffer.position(0);
                } else {
                    buffer = null;
                }
                stitchFilename = null;
            }
            if (mTakePhotoListener.mSaveHdrSourceFile && mTakePhotoListener.mHDRImageCount > 0 && null != unstitchFilename) {
                String hdrDir = unstitchFilename.replace("_hdr.jpg", ".hdr");
                File file = new File(hdrDir);
                if (!file.exists()) {
                    file.mkdirs();
                }
                PiPano.saveJpeg(hdrDir + "/" + mHdrIndex + ".jpg", null,
                        0, buffer, bitmapBytes, width, height,
                        stride, mTakePhotoListener.mJpegQuilty, mTakePhotoListener.mSaveExif,
                        mTakePhotoListener.mHeading, mTakePhotoListener.mLatitude,
                        mTakePhotoListener.mLongitude, mTakePhotoListener.mAltitude,
                        mTakePhotoListener.mArtist);
            }

            try {
                // hdr合成前重新写入exif信息
                if (mTakePhotoListener.mHDRImageCount > 0 && mHdrIndex == (mTakePhotoListener.mHDRImageCount - 1)) {
                    PiPano.recordOnceJpegInfo(mTakePhotoListener.hdr_exposureTime, mTakePhotoListener.hdr_ev,
                            mTakePhotoListener.hdr_iso, mTakePhotoListener.hdr_wb);
                    Log.d(TAG, "recordOnceJpegInfo for hdr merge!");
                }
                int ret = PiPano.saveJpeg(unstitchFilename, stitchFilename,
                        mTakePhotoListener.mHDRImageCount, buffer, bitmapBytes, width, height,
                        stride, mTakePhotoListener.mJpegQuilty, mTakePhotoListener.mSaveExif,
                        mTakePhotoListener.mHeading, mTakePhotoListener.mLatitude,
                        mTakePhotoListener.mLongitude, mTakePhotoListener.mAltitude,
                        mTakePhotoListener.mArtist);

                if (ret < 0) {
                    mTakePhotoListener.mHDRError = true;
                    Log.e(TAG, "saveJpeg error ret: " + ret);
                }
                //如果返回1,那么说明正在往hdrImageList加图片,不回调
                else if (ret == 0) {
                    if (!mTakePhotoListener.mIsThumb) {
                        PiPano.injectThumbnail(unstitchFilename, mTakePhotoListener.thumbFilePath);
                    }
                    mTakePhotoListener.dispatchTakePhotoComplete(ret);
                }

                image.close();
                reader.close();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                mTakePhotoListener.mHDRError = true;
                mTakePhotoListener.dispatchTakePhotoComplete(400);
            }
        }

        Log.i(TAG, "take photo cost time: " + (System.currentTimeMillis() - beginTime) + "ms");

        mThreadHandler.quitSafely();
    }

    private void saveRawFile(String unStitchFilename, Image image) {
        unStitchFilename = unStitchFilename.replace(".jpg", ".raw");
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(unStitchFilename))) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] byteBuffer = new byte[buffer.remaining()];
            buffer.get(byteBuffer);
            output.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPhotoExifInfo(String filepath) {
        try {
            ExifInterface exifInterface = new ExifInterface(filepath);
            exifInterface.setAttribute(ExifInterface.TAG_ARTIST, mTakePhotoListener.mArtist);
            exifInterface.setAttribute(ExifInterface.TAG_SOFTWARE, mTakePhotoListener.mSoftware);
            exifInterface.saveAttributes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (null != mTakePhotoListener.thumbFilePath) {
            PiPano.injectThumbnail(filepath, mTakePhotoListener.thumbFilePath);
        }
    }
}
