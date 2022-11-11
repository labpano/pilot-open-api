package com.pi.pano;

import android.view.Surface;

import androidx.annotation.Keep;

import java.io.File;

@Keep
class StitchingNative {
    @Keep
    private long mNativeContext;//do not delete

    native int createNativeObj(String dirname, Surface[] surfaces);
    native int deleteFrameExtractor();
    native void seekToPreviousKeyFrame(long timeStamp);
    native boolean extractorOneFrame();
    native void decodeOneFrame(boolean renderEncodeFrame);
    native long getExtractorSampleTime(int index);

    int startStitching(String filename, PiPano piPano) {
        File file = new File(filename + "/param.txt");
        if (file.exists()) {
            piPano.setParamByMedia(filename + "/param.txt");
        } else {
            piPano.setParamByMedia(filename + "/0.mp4");
        }

        //video extractor
        Surface[] surfaces = new Surface[PilotSDK.CAMERA_COUNT];
        for (int i = 0; i < surfaces.length; ++i) {
            surfaces[i] = new Surface(piPano.mSurfaceTexture[i]);
        }
        return createNativeObj(filename, surfaces);
    }
}
