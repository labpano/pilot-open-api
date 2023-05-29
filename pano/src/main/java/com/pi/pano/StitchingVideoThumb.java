package com.pi.pano;

import android.content.Context;

public class StitchingVideoThumb {

    static {
        System.loadLibrary("PiPano");
    }

    public StitchingVideoThumb(Context context) {
        Watermark.checkWatermarkConfig(context);
        createNativeObj();
    }

    public int executeStitchFile(String filename, boolean forceStitch, int stitchType, boolean usePiBlend) {
        return executeStitching(filename, forceStitch, stitchType, usePiBlend);
    }

    public void release() {
        releaseNativeObj();
    }

    private native void createNativeObj();

    private native int executeStitching(String filename, boolean forceStitch, int stitchType, boolean usePiBlend);

    private native void releaseNativeObj();
}
