package com.pi.pano;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.SystemClock;
import android.util.Log;

import com.pi.pano.annotation.PiFileStitchFlag;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HDR photo processing.
 */
class HdrImageProcess extends ImageProcess {
    private int handleImageCount = 0;

    private final AtomicBoolean saveJpgFinished = new AtomicBoolean(false);

    HdrImageProcess(TakePhotoListener takePhotoListener, ImageReader imageReader) {
        super(takePhotoListener, imageReader.getImageFormat(), true/*async*/);
        if (!mParams.isHdr()) {
            throw new RuntimeException("TakePhoto isn't hdr");
        }
        prepare(imageReader);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        //super.onImageAvailable(reader);
        Log.i(mTag, "onImageAvailable");
        Image image;
        while ((image = reader.acquireNextImage()) != null) {
            handleImageCount++;
            handleImage(image);
        }
        release();
    }

    @Override
    protected void release() {
        if (handleImageCount == mParams.hdrCount) {
            super.release();
        }
    }

    @Override
    protected void saveJpeg(Image image) {
        if (null == mTakePhotoListener.mHdrSourceDir) {
            mTakePhotoListener.mHdrSourceDir = new File(mParams.unStitchDirPath, "." + mTakePhotoListener.mFilename + PiFileStitchFlag.unstitch + ".hdr");
        }
        // hdr index文件
        File indexFile = new File(mTakePhotoListener.mHdrSourceDir, (mTakePhotoListener.mHdrSourceFileCount++) + ".jpg");
        checkAndCreateParentDir(indexFile);
        directSaveSpatialJpeg(image, indexFile.getAbsolutePath(), true, null);
        if (mTakePhotoListener.mHdrSourceFileCount == mParams.hdrCount) {
            saveJpgFinished.set(true);
        }
    }

    public int stackHdrConfigured() {
        Log.d(mTag, "stackHdrConfigured cur hdr count:" + mTakePhotoListener.mHdrSourceFileCount);
        int count = 0;
        while (!saveJpgFinished.get()) {
            if (count >= 30) {
                Log.e(mTag, "stackHdrConfigured 3s time out ! " + mTakePhotoListener.mHdrSourceFileCount);
                return -2;
            }
            SystemClock.sleep(100);
            count++;
        }
        return stackHdrFile();
    }

    private int stackHdrFile() {
        int errorCode = -1;
        if (null != mTakePhotoListener.mHdrSourceDir) {
            File[] list = mTakePhotoListener.mHdrSourceDir.listFiles();
            if (null != list && list.length > 0) {
                int imageCount = list.length;
                Log.d(mTag, "stackHdrFile file :" + imageCount + ",hdr," + mParams.hdrCount);
                if (imageCount < mParams.hdrCount) {
                    return errorCode;
                }
                Arrays.sort(list);
                File unstitchFile = new File(mParams.unStitchDirPath, mTakePhotoListener.mFilename + PiFileStitchFlag.unstitch + "_hdr" + ".jpg");
                checkAndCreateParentDir(unstitchFile);
                mTakePhotoListener.mUnStitchFile = unstitchFile;

                int ret;
                int width = 0;
                int height = 0;
                ByteBuffer byteBuffer = null;
                for (File file : list) {
                    if (!file.exists()) {
                        continue;
                    }
                    Log.d(mTag, "stackHdrFile,>>>>file:" + file);
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), null);
                    if (bitmap == null) {
                        errorCode = -1;
                        Log.e(mTag, "stackHdrFile,decodeFile null,file:" + file);
                        break;
                    }
                    if (null == byteBuffer) {
                        width = bitmap.getWidth();
                        height = bitmap.getHeight();
                        byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
                    } else {
                        byteBuffer.position(0);
                    }
                    bitmap.copyPixelsToBuffer(byteBuffer);
                    bitmap.recycle();
                    ret = PiPano.nativeHdrAddImage(byteBuffer, null, width, height, width);
                    if (ret == 0) { // finish
                        errorCode = 0;
                    } else {
                        errorCode = ret;
                        mTakePhotoListener.mHDRError = true;
                        Log.e(mTag, "stackHdrFile,error ret: " + ret);
                        break;
                    }
                }
                if (errorCode == 0 && width > 0) {
                    // compositing hdr
                    File middleFile = list[list.length / 2];
                    int result = PiPano.nativeHdrCalculate(unstitchFile.getAbsolutePath(),
                            middleFile.getAbsolutePath(), width, height, width);
                    if (result != 0) {
                        mTakePhotoListener.mHDRError = true;
                        Log.e(mTag, "stackHdrFile, nativeHdrCalculate err : " + result);
                        return result;
                    }
                    ThumbnailGenerator.injectExifThumbnailForUnStitch(unstitchFile);
                } else {
                    PiPano.clearImageList();
                }
            }
            if (mParams.isSaveHdrSourceFile()) {
                File hdr = new File(mTakePhotoListener.mHdrSourceDir.getParentFile(), mTakePhotoListener.mHdrSourceDir.getName().substring(1));
                mTakePhotoListener.mHdrSourceDir.renameTo(hdr);
                mTakePhotoListener.mHdrSourceDir = hdr;
            } else {
                deleteFile(mTakePhotoListener.mHdrSourceDir);
                mTakePhotoListener.mHdrSourceDir = null;
            }
        }
        return errorCode;
    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            file.listFiles(pathname -> {
                if (pathname.isDirectory()) {
                    deleteFile(pathname);
                }
                return pathname.delete();
            });
        }
        file.delete();
    }
}
