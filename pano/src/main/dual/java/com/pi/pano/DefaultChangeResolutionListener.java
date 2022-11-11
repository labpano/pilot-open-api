package com.pi.pano;

import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiPreviewMode;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultChangeResolutionListener extends ChangeResolutionListener {
    protected final String TAG = DefaultChangeResolutionListener.class.getSimpleName();
    // （请求的）视角
    protected final int fieldOfView;
    // 画面比例
    @NonNull
    protected final String aspectRatio;
    // 是否全景
    protected final boolean isPanorama;

    static final ExecutorService sExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ChangeResolution" + mCount.incrementAndGet());
        }
    });

    public DefaultChangeResolutionListener() {
        this(90, "2:1");
    }

    public DefaultChangeResolutionListener(@NonNull String aspectRatio) {
        this(90, aspectRatio);
    }

    public DefaultChangeResolutionListener(int fieldOfView, @NonNull String aspectRatio) {
        if (aspectRatio.split(":").length != 2) {
            throw new RuntimeException("aspectRatio is error:" + aspectRatio);
        }
        this.fieldOfView = fieldOfView;
        this.aspectRatio = aspectRatio;
        this.isPanorama = "2:1".equals(aspectRatio);
    }

    protected int getReCaliIntervalFrame(int fps) {
        return fps * 2;
    }

    /**
     * Resolution pre detection
     *
     * @param same true:no change.
     */
    public void onCheckResolution(boolean same) {
        if (same) {
            onChangeResolution(-1, -1);
            changeSurfaceViewSize(null);
        }
    }

    protected void setPreviewParam() {
        PilotSDK.setParamReCaliEnable(getReCaliIntervalFrame(PilotSDK.CAMERA_PREVIEW_640_320_30[2]), true);
        PilotSDK.setLensCorrectionMode(0x11);
        PilotSDK.setPreviewMode(PiPreviewMode.planet, 180, false, fieldOfView, 0);
    }

    @Override
    protected void onChangeResolution(int width, int height) {
        super.onChangeResolution(width, height);
        setPreviewParam();
    }

    public void changeSurfaceViewSize(Runnable work) {
        final CameraSurfaceView surfaceView;
        PilotSDK sdk = PilotSDK.mSingle;
        if (sdk != null) {
            surfaceView = sdk.mCameraSurfaceView;
        } else {
            surfaceView = null;
        }
        if (surfaceView == null || Objects.equals(surfaceView.getTag(), aspectRatio)) {
            Log.e(TAG, "changeSurfaceViewSize size not change !" + surfaceView);
            safeRun(work);
            return;
        }
        int oldWidth = surfaceView.getWidth();
        int oldHeight = surfaceView.getHeight();
        surfaceView.post(() -> {
            final boolean[] needChangeSize = new boolean[1];
            if (isPanorama) {
                needChangeSize[0] = changeToPanorama(surfaceView);
            } else {
                needChangeSize[0] = changeToOtherRatio(surfaceView, aspectRatio);
            }
            surfaceView.setTag(aspectRatio);
            if (!needChangeSize[0] || work == null) {
                safeRun(work);
                return;
            }
            sExecutor.execute(() -> {
                while (needChangeSize[0]) {
                    if (oldWidth != surfaceView.getWidth() || oldHeight != surfaceView.getHeight()) {
                        needChangeSize[0] = false;
                    }
                    SystemClock.sleep(20);
                }
                surfaceView.post(work);
            });
        });
    }

    /**
     * Processing panoramic surface scale.
     */
    protected boolean changeToPanorama(View surfaceView) {
        ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
        if (params.width == ViewGroup.LayoutParams.MATCH_PARENT
                && params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            return false;
        }
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        updateLayoutParams(surfaceView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return true;
    }

    /**
     * Processing non panoramic surface scale。
     */
    protected boolean changeToOtherRatio(View surfaceView, String aspectRatio) {
        View parentView = (View) surfaceView.getParent();
        int[] size = SurfaceSizeUtils.calculateSurfaceSize(parentView.getWidth(), parentView.getHeight(), aspectRatio);
        return updateLayoutParams(surfaceView, size[0], size[1]);
    }

    protected boolean updateLayoutParams(View surfaceView, int width, int height) {
        Log.d(TAG, "updateLayoutParams,width:" + width + "," + height);
        ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) params).gravity = Gravity.CENTER;
        }
        boolean needChange = params.width != width && params.height != height;
        params.width = width;
        params.height = height;
        surfaceView.setLayoutParams(params);
        return needChange;
    }

    private void safeRun(Runnable run) {
        if (run != null) {
            run.run();
        }
    }
}
