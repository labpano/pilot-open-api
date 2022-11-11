package com.pi.pano;

import android.content.res.AssetManager;

import com.pi.pano.annotation.PiSlamState;

/**
 * Slam listener
 */
public abstract class SlamListener {
    AssetManager assetManager;
    float lenForCalMeasuringScale;
    boolean showPreview;
    boolean useForImuToPanoRotation;

    /**
     * Slam tracks state changes
     *
     * @param state new state
     */
    public void onTrackStateChange(@PiSlamState int state) {
    }

    /**
     * slam error.
     *
     * @param errorCode error code,0 - Initialization succeeded,no error greater than 0 indicates an error.
     */
    public void onError(int errorCode) {
    }

    /**
     * End slam, call back this interface, including the location data of the photo.
     *
     * @param position String containing photo location data.
     */
    public void onPhotoPosition(String position) {
    }

    /**
     * slam stopped.
     *
     * @param accuracy          当slam用于矫正imu和pano旋转关系的时候,这个值代表矫正的准确度
     * @param imuToPanoRotation 表示imu和pano旋转关系的四元数
     */
    public void onStop(float accuracy, float[] imuToPanoRotation) {
    }

    /**
     * set debug dir.
     *
     * @param debugDir debug dir
     */
    public void onSetDebugDir(String debugDir) {
    }
}
