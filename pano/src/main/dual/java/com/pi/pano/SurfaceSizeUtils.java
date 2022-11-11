package com.pi.pano;

import androidx.annotation.NonNull;

public class SurfaceSizeUtils {

    @NonNull
    public static int[] calculateSurfaceSize(int containerWidth, int containerHeight, String aspectRatio) {
        String[] aa = aspectRatio.split(":");
        if (aa.length != 2) {
            throw new RuntimeException("aspectRatio is error:" + aspectRatio);
        }
        int wr = Integer.parseInt(aa[0]);
        int hr = Integer.parseInt(aa[1]);
        if (wr == hr) {
            int min = Math.min(containerWidth, containerHeight);
            return new int[]{min, min};
        }
        float wf = containerWidth * 1f / wr;
        float hf = containerHeight * 1f / hr;
        int[] ret = new int[2];
        if (wf < hf) {
            ret[0] = containerWidth;
            ret[1] = containerWidth * hr / wr;
        } else {
            ret[0] = containerHeight * wr / hr;
            ret[1] = containerHeight;
        }
        return ret;
    }

    @NonNull
    public static int[] calculateSurfaceSize(int containerWidth, int containerHeight, int destWidth, int destHeight) {
        if (destWidth == destHeight) {
            int min = Math.min(containerWidth, containerHeight);
            return new int[]{min, min};
        }
        float wf = containerWidth * 1f / destWidth;
        float hf = containerHeight * 1f / destHeight;
        int[] ret = new int[2];
        if (wf < hf) {
            ret[0] = containerWidth;
            ret[1] = containerWidth * destHeight / destWidth;
        } else {
            ret[0] = containerHeight * destWidth / destHeight;
            ret[1] = containerHeight;
        }
        return ret;
    }
}
