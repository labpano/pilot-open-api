package com.pi.pano;

import com.pi.pano.annotation.PiPreviewMode;

/**
 * PanoSDK event callback.
 */
public interface PanoSDKListener {
    /**
     *
     */
    void onPanoCreate();

    /**
     *
     */
    void onPanoRelease();

    /**
     * Preview mode change event.
     *
     * @param mode New mode value.
     */
    void onChangePreviewMode(@PiPreviewMode int mode);

    /**
     * Click event.
     */
    void onSingleTap();

    /**
     * Frame rate.
     *
     * @param count fps.
     */
    void onEncodeFrame(int count);
}
