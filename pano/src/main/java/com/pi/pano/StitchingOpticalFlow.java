package com.pi.pano;

import androidx.annotation.Keep;

/**
 * Photo stitch.
 */
@Keep
public class StitchingOpticalFlow {

    static {
        System.loadLibrary(Config.PIPANO_SO_NAME);
    }

    /**
     * optical flow stitch for jpeg files.
     *
     * @param filename file path
     * @return true:use optical flow stitch
     */
    public static native boolean getUseOpticalFlow(String filename);

    /**
     * use optical flow stitch for jpeg file.
     *
     * @param unstitchedFilename  unstitch file path
     * @param stitchedFilename    stitched file path
     * @param useOpticalFlow      use optical flow
     * @param lensProtectedEnable use protected lens
     * @param quality             jpeg quality, ranging from 0 to 100
     */
    public static native void stitchJpegFile(String unstitchedFilename, String stitchedFilename,
                                             boolean useOpticalFlow, boolean lensProtectedEnable, int quality);

    /**
     * use optical flow stitch for jpeg file.
     *
     * @param unstitchedFilename unstitch file path
     * @param stitchedFilename   stitched file path
     * @param useOpticalFlow     use optical flow
     * @param quality            jpeg quality, ranging from 0 to 100
     */
    public static void stitchJpegFile(String unstitchedFilename, String stitchedFilename,
                                      boolean useOpticalFlow, int quality) {
        stitchJpegFile(unstitchedFilename, stitchedFilename, useOpticalFlow,
                SystemPropertiesProxy.getInt("persist.dev.pano.lens_protected", 0) > 0,
                quality);
    }
}
