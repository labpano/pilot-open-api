package com.pi.pano;

/**
 *
 */
public interface CameraFixShakeListener {

    /**
     * Before restart
     */
    void onCameraFixShakeBefore();

    /**
     * After restart
     */
    void onCameraFixShakeAfter();
}
