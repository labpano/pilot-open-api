package com.pi.pano;

/**
 * gesture input operation
 */
class Input {
    /**
     * touch event
     *
     * @param action       action type
     * @param pointerCount pointer count
     * @param timestampNs  timestamp
     * @param pointPoses   point poses
     */
    static native void onTouchEvent(int action, int pointerCount, long timestampNs, float[] pointPoses);
    static native void setPreviewMode(int mode, float rotateDegree, boolean playAnimation, float fov, float cameraDistance);
    static native int getPreviewMode();
    /**
     * reset
     */
    static native void reset();
}