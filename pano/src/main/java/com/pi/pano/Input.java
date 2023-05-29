package com.pi.pano;

import com.pi.pano.annotation.PiPreviewMode;

/**
 * gesture input operation
 */
public class Input {
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

    /**
     * 重置，保留预览模式并设置新值。
     * @param rotateDegree x轴旋转角度，应在（-36，360）之间，否则忽略。重置将同时重置y轴为0。
     */
    static native void reset2(float rotateDegree, float fov, float cameraDistance);

    /**
     * 预览参数
     */
    private static Params _lastParams;

    static void keepRotateDegreeOnReset(boolean keep) {
        Params params = _lastParams;
        if (params != null) {
            params.keepRotateDegreeOnReset = keep;
        }
    }

    static void onTouchEvent2(int action, int pointerCount, long timestampNs, float[] pointPoses) {
        onTouchEvent(action, pointerCount, timestampNs, pointPoses);
    }

    private final static class Params {
        int mode;
        float rotateDegree;
        float fov;
        float cameraDistance;

        /**
         * 重置时保留旋转角度
         */
        boolean keepRotateDegreeOnReset = false;
    }

    static void setPreviewMode2(int mode, float rotateDegree, boolean playAnimation, float fov, float cameraDistance) {
        Params params = _lastParams;
        if (params == null) {
            params = new Params();
            _lastParams = params;
        }
        params.mode = mode;
        params.rotateDegree = rotateDegree;
        params.fov = fov;
        params.cameraDistance = cameraDistance;
        setPreviewMode(mode, rotateDegree, playAnimation, fov, cameraDistance);
    }

    static void reset2() {
        Params params = _lastParams;
        if (null != params) {
            if (params.mode == PiPreviewMode.vlog) {
                return;
            }
            reset2(params.keepRotateDegreeOnReset ? 360 : params.rotateDegree, params.fov, params.cameraDistance);
            return;
        }
        reset();
    }
}