package com.pi.pano;

public class PhotoStitchingTask {
    public static final int NORMAL_STITCH_TO_FLOW_STITCH = 0;
    public static final int FLOW_STITCH_TO_NORMAL_STITCH = 1;

    public String filename;
    public int type = NORMAL_STITCH_TO_FLOW_STITCH;
    public boolean isProcessing = false;
}
