package com.pi.pano;

import com.pi.pano.annotation.PiPreviewMode;

/**
 * PanoSurfaceView listener
 */
public interface PanoSurfaceViewListener {
    /**
     * created
     */
    void onPanoSurfaceViewCreate();

    /**
     * release
     */
    void onPanoSurfaceViewRelease();

    /**
     * Preview Mode Change
     *
     * @param mode lase mode {@link PiPreviewMode}
     */
    void onPanoModeChange(@PiPreviewMode int mode);

    /**
     * Clicked
     */
    void onSingleTapConfirmed();

    /**
     * Frame rate，interval callback.
     *
     * @param count frame rate
     */
    void onEncodeFrame(int count);
}
