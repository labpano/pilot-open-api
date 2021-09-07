package com.pi.pano;

import android.hardware.Camera;

/**
 * When the camera frame rate is too low, the camera will be automatically restarted in
 * {@link CameraSurfaceView}, The monitoring callback corresponding to this process.
 */
public interface CameraFixShakeListener {

    /**
     * Before restart
     */
    void onCameraFixShakeBefore();

    /**
     * After restart
     */
    void onCameraFixShakeAfter(Camera[] cameras);
}
