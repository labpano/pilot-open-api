package com.pi.pano;

/**
 * 集中管理 当前使用的camera环境 参数
 */
public class CameraEnvParams {

    public static final String CAPTURE_PHOTO = "photo";
    public static final String CAPTURE_STREAM = "stream";

    private String mCameraId;
    private int mWidth;
    private int mHeight;
    private int mFps;
    /**
     * 捕获模式， 照片 or 流
     */
    private String mCaptureMode;

    public CameraEnvParams(String cameraId, int width, int height, int fps, String captureMode) {
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
        mFps = fps;
        this.mCaptureMode = captureMode;
    }

    public String getCameraId() {
        return mCameraId;
    }

    public void setCameraId(String cameraId) {
        mCameraId = cameraId;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFps() {
        return mFps;
    }

    public void setFps(int fps) {
        mFps = fps;
    }

    public String getCaptureMode() {
        return mCaptureMode;
    }

    public void setCaptureMode(String captureMode) {
        this.mCaptureMode = captureMode;
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public String toString() {
        return "CameraEnvParams{" +
                "mCameraId='" + mCameraId + '\'' +
                ", mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mFps=" + mFps +
                ", mCaptureMode='" + mCaptureMode + '\'' +
                '}';
    }
}
