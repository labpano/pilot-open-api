package com.pi.pano;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.DngCreator;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import com.pi.pano.annotation.PiFileStitchFlag;
import com.pi.pano.annotation.PiPhotoFileFormat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

class ImageProcess implements ImageReader.OnImageAvailableListener {
    protected static final AtomicInteger sThreadCount = new AtomicInteger(0);

    protected String mTag = "ImageProcess";

    protected TakePhotoListener mTakePhotoListener;
    protected CaptureParams mParams;

    protected HandlerThread mHandlerThread;
    protected Handler mHandler;

    protected final int mImageFormat;
    protected DngCreator mDngCreator;

    public void setDngCreator(DngCreator dngCreator) {
        mDngCreator = dngCreator;
    }

    public boolean needDng() {
        if (mImageFormat == ImageFormat.RAW_SENSOR) {
            return PiPhotoFileFormat.jpg_dng.equals(mParams.fileFormat);
        }
        return false;
    }

    ImageProcess(TakePhotoListener takePhotoListener, int imageFormat, boolean async) {
        mImageFormat = imageFormat;
        if (takePhotoListener.mIsThumb) {
            if (mImageFormat != ImageFormat.JPEG && mImageFormat != PixelFormat.RGBA_8888) {
                throw new RuntimeException("Unsupported ImageFormat: 0x" + Integer.toHexString(mImageFormat));
            }
        } else {
            if (mImageFormat != ImageFormat.RAW_SENSOR && mImageFormat != ImageFormat.JPEG) {
                throw new RuntimeException("Unsupported ImageFormat: 0x" + Integer.toHexString(mImageFormat));
            }
        }
        mParams = takePhotoListener.mParams;
        mTakePhotoListener = takePhotoListener;
        mTag = "ImageProcess[0x" + Integer.toHexString(mImageFormat) + "]<" + sThreadCount.getAndIncrement() + ">";
        if (async) {
            initThread();
        }
    }

    ImageProcess(TakePhotoListener takePhotoListener, ImageReader imageReader, boolean binding) {
        this(takePhotoListener, imageReader.getImageFormat(), true);
        if (binding) {
            bind(imageReader);
        } else {
            prepare(imageReader);
        }
    }

    protected void initThread() {
        Log.d(mTag, "initThread");
        mHandlerThread = new HandlerThread(mTag);
        mHandlerThread.start();
    }

    Handler getHandler() {
        if (null != mHandlerThread && mHandler == null) {
            mHandler = new Handler(mHandlerThread.getLooper());
            return mHandler;
        }
        return mHandler;
    }

    protected ImageReader mImageReader;
    protected boolean isBind = false;

    protected void prepare(ImageReader imageReader) {
        if (mImageFormat != imageReader.getImageFormat()) {
            throw new RuntimeException("Error ImageFormat of ImageReader,0x" + Integer.toHexString(imageReader.getImageFormat()));
        }
        mImageReader = imageReader;
        imageReader.setOnImageAvailableListener(this, getHandler());
        Log.d(mTag, "prepare");
    }

    protected void bind(ImageReader imageReader) {
        prepare(imageReader);
        isBind = true;
    }

    void aborted() {
        Log.d(mTag, "aborted");
        release();
    }

    void release() {
        Log.d(mTag, "release");
        if (null != mHandler) {
            mHandler.postDelayed(this::releaseImpl, 100);
        } else {
            releaseImpl();
        }
    }

    protected void releaseImpl() {
        if (null != mHandlerThread) {
            mHandlerThread.getLooper().quitSafely();
            mHandler = null;
            mHandlerThread = null;
        }
        if (null != mImageReader) {
            if (isBind) {
                mImageReader.close();
                Log.d(mTag, "imageReader close");
            } else {
                mImageReader.setOnImageAvailableListener(null, null);
                Log.d(mTag, "imageReader disconnect");
            }
            mImageReader = null;
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Log.d(mTag, "onImageAvailable");
        final Image image = reader.acquireLatestImage();
        if (null == image) {
            Log.e(mTag, "onImageAvailable image is null!");
            return;
        }
        handleImage(image);
        release();
    }

    protected void handleImage(Image image) {
        final long beginTimestamp = System.currentTimeMillis();
        Log.d(mTag, "image width: " + image.getWidth() + " height: " + image.getHeight());
        try {
            if (mTakePhotoListener.mIsThumb) {
                saveThumb(image);
            } else {
                if (mImageFormat == ImageFormat.RAW_SENSOR) {
                    if (needDng()) {
                        saveDng(image);
                    } else {
                        saveRaw(image);
                    }
                } else if (mImageFormat == ImageFormat.JPEG) {
                    saveJpeg(image);
                } else {
                    throw new RuntimeException("Unsupported ImageFormat: 0x" + Integer.toHexString(mImageFormat));
                }
            }
        } finally {
            image.close();
        }
        Log.i(mTag, "take photo cost time: " + (System.currentTimeMillis() - beginTimestamp) + "ms");
    }

    protected void notifyTakePhotoComplete(int errorCode) {
        Log.d(mTag, "notifyTakePhotoComplete,errorCode:" + errorCode);
        mTakePhotoListener.dispatchTakePhotoComplete(errorCode);
    }

    protected void saveThumb(Image image) {
        int errorCode = 0;
        Image.Plane plane = image.getPlanes()[0];
        int stride = 1;
        if (plane.getPixelStride() != 0) {
            stride = plane.getRowStride() / plane.getPixelStride();
        }
        File unStitchFile = new File(mParams.unStitchDirPath, mTakePhotoListener.mFilename + ".jpg");
        checkAndCreateParentDir(unStitchFile);
        String path = unStitchFile.getAbsolutePath();
        int ret = PiPano.saveJpeg(path, null,
                0, plane.getBuffer(), null, image.getWidth(), image.getHeight(),
                stride, mParams.jpegQuality, false,
                mParams.heading, mParams.latitude,
                mParams.longitude, mParams.altitude,
                mParams.artist);
        if (ret != 0) {
            errorCode = 400;
        }
        Log.d(mTag, "save thumb:" + path + ",errorCode:" + errorCode);
        notifyTakePhotoComplete(errorCode);
    }

    protected void saveDng(Image image) {
        int errorCode = 0;
        File unStitchFile = new File(mParams.unStitchDirPath, mTakePhotoListener.mFilename + PiFileStitchFlag.unstitch + ".dng");
        checkAndCreateParentDir(unStitchFile);
        mTakePhotoListener.mUnStitchFile = unStitchFile;
        int sleepRetry = 30;
        while (mDngCreator == null && sleepRetry > 0) {
            SystemClock.sleep(100);
            sleepRetry--;
        }
        if (mDngCreator != null) {
            try (FileOutputStream output = new FileOutputStream(unStitchFile)) {
                mDngCreator.writeImage(output, image);
            } catch (IOException ex) {
                ex.printStackTrace();
                errorCode = 400;
            }
        } else {
            errorCode = 400;
        }
        Log.d(mTag, "saveDng unStitchFile:" + unStitchFile + ",errorCode:" + errorCode);
        notifyTakePhotoComplete(errorCode);
    }

    protected void saveRaw(Image image) {
        int errorCode = 0;
        File unStitchFile = new File(mParams.unStitchDirPath, mTakePhotoListener.mFilename + PiFileStitchFlag.unstitch + ".raw");
        checkAndCreateParentDir(unStitchFile);
        mTakePhotoListener.mUnStitchFile = unStitchFile;
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(unStitchFile))) {
            byte[] byteBuffer = new byte[buffer.remaining()];
            buffer.get(byteBuffer);
            output.write(byteBuffer);
        } catch (IOException ex) {
            ex.printStackTrace();
            errorCode = 400;
        }
        Log.d(mTag, "saveRaw unStitchFile:" + unStitchFile + ",errorCode:" + errorCode);
        notifyTakePhotoComplete(errorCode);
    }

    protected void saveJpeg(Image image) {
        int errorCode = 0;
        // 普通jpeg
        File unStitchFile = new File(mParams.unStitchDirPath, mTakePhotoListener.mFilename + PiFileStitchFlag.unstitch + ".jpg");
        checkAndCreateParentDir(unStitchFile);
        mTakePhotoListener.mUnStitchFile = unStitchFile;
        String path = unStitchFile.getAbsolutePath();
        int ret = directSaveSpatialJpeg(image, path, true, mTakePhotoListener.thumbFilePath);
        if (ret != 0) {
            errorCode = 400;
        }
        Log.d(mTag, "saveJpeg unStitchFile:" + unStitchFile + ",errorCode:" + errorCode);
        notifyTakePhotoComplete(errorCode);
    }

    /**
     * Save the panorama file
     */
    protected int directSaveSpatialJpeg(Image image, String filename, boolean appendExif, String thumbFilename) {
        int ret = PiPano.spatialJpeg(filename, image.getPlanes()[0].getBuffer(), image.getWidth(), image.getHeight(), mParams.heading,
                (System.currentTimeMillis() - SystemClock.elapsedRealtime()) * 1000 + image.getTimestamp() / 1000);
        if (appendExif) {
            appendExif(filename, thumbFilename);
        }
        return ret;
    }

    protected static void saveBuffer(String filePath, ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        saveBuffer(filePath, data);
    }

    protected static void saveBuffer(String filePath, byte[] data) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void appendExif(String filename, String thumbFilename) {
        try {
            ExifInterface exifInterface = new ExifInterface(filename);
            exifInterface.setAttribute(ExifInterface.TAG_ARTIST, mParams.artist);
            exifInterface.setAttribute(ExifInterface.TAG_SOFTWARE, mParams.software);
            exifInterface.saveAttributes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (null != thumbFilename) {
            PiPano.injectThumbnail(filename, thumbFilename);
        }
    }

    protected static void checkAndCreateParentDir(File file) {
        File parentFile = file.getParentFile();
        if (null != parentFile && !parentFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();
        }
    }
}
