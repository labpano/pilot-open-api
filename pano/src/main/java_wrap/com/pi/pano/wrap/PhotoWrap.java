package com.pi.pano.wrap;

import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pi.pano.CaptureParams;
import com.pi.pano.PilotSDK;
import com.pi.pano.ResolutionSize;
import com.pi.pano.annotation.PiPhotoFileFormat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 拍照
 */
public final class PhotoWrap {
    private final static String TAG = PhotoWrap.class.getSimpleName();

    static final ExecutorService sExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Photo-" + mCount.incrementAndGet());
        }
    });

    /**
     * 拍照
     */
    public static void takePhoto(CaptureParams params, IPhotoListener listener) {
        rawTakePhoto(params, listener);
    }

    private static void rawTakePhoto(CaptureParams params, IPhotoListener listener) {
        Log.d(TAG, "take photo:" + params);
        sExecutor.execute(() -> {
            TimeOutTakePhotoListener takePhotoListener = new TimeOutTakePhotoListener(listener, new Handler(Looper.getMainLooper()));
            takePhotoListener.mParams = params;
            if (PiPhotoFileFormat.jpg_raw.equals(params.fileFormat) ||
                    PiPhotoFileFormat.jpg_dng.equals(params.fileFormat)) {
                takePhotoListener.mImageFormat = ImageFormat.RAW_SENSOR;
                takePhotoListener.setTotalTakeCount(2);
            } else {
                takePhotoListener.mImageFormat = ImageFormat.JPEG;
            }
//        //如果当前是手动曝光模式,并且是HDR模式,那就需要先调整到自动曝光模式,需要等待3.5秒等待自动曝光调节
//        if (takePhotoListener.mHDRImageCount > 0) {
//            // 旧代码逻辑hdr拍照始终等待3.5s
//            takePhotoListener.mSkipFrame = (int) (15 * 3.5f); //拍照使用15fps，计算跳过帧数
//        }
            // 等待预览调节完成，当前处理的等待包括曝光、ev、iso调节。
            final long ms = PreviewWrap.obtainAdjustWaitTime();
            if (ms > 0) {
                takePhotoListener.mInitSkipFrame = (int) Math.ceil(/*15*/30 / 1000f * ms);
                takePhotoListener.mSkipFrame = takePhotoListener.mInitSkipFrame;
                Log.d(TAG, "skipFrame:" + takePhotoListener.mSkipFrame);
            }
            takePhotoListener.notifyTakeStart();
            ResolutionSize size = ResolutionSize.parseSize(params.resolution);
            PilotSDK.takePhoto(params.obtainBasicName(), size.width, size.height,
                    params.takeThumb && !params.isHdr()/*hdr不拍缩略图*/, takePhotoListener);
        });
    }

}
