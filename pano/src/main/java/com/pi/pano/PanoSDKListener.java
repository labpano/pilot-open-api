package com.pi.pano;

import com.pi.pano.annotation.PiPreviewMode;

/**
 * PanoSDK event listener.
 * Avoid doing time-consuming operations directly, and the execution is not necessarily in the ui thread.
 */
public interface PanoSDKListener {
    /**
     * SDK has been created.
     * The initialization has been completed, and the preview related functions can be used normally.
     */
    void onPanoCreate();

    /**
     * SDK has been released.
     * Preview (data) and actions should no longer be used after release.
     */
    void onPanoRelease();

    /**
     * Preview mode changed.
     *
     * @param mode last mode
     */
    void onChangePreviewMode(@PiPreviewMode int mode);

    /**
     * preview is clicked.
     */
    void onSingleTap();

    /**
     * frame rate,
     * (当前定时触发此回调)
     */
    void onEncodeFrame(int count);
}
