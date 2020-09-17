package com.pi.pano;

/**
 * PanoSDK event callback
 */
public interface PanoSDKListener {
    /**
     * SDK initialization completed
     */
    void onSDKCreate();

    /**
     * SDK release
     */
    void onSDKRelease();

    /**
     * Preview mode change
     *
     * @param mode Changed value
     */
    void onChangePanoMode(int mode);

    /**
     * Click event
     */
    void onSingleTapConfirmed();

    void onEncodeFrame(int count);
}
