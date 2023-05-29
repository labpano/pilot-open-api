package com.pi.pano;

import android.text.TextUtils;
import android.util.Pair;

/**
 * Field angle utils.
 */
class FieldOfViewUtils {

    private static final int[] sFieldOfViews = new int[]{90, 120, 135, 150};
    private static final int[] sDistance16_9 = new int[]{60, 220, 290, 365};
    private static final int[] sDistance9_16 = new int[]{60, 220, 290, 365};
    private static final int[] sDistance1_1 = new int[]{70, 190, 250, 330};

    static Pair<Float, Float> obtain(String aspectRatio, int fieldOfView) {
        float fov, cameraDistance;
        int index = index(fieldOfView);
        if (TextUtils.equals(aspectRatio, "16:9")) {
            fov = 50;
            cameraDistance = sDistance16_9[index];
        } else if (TextUtils.equals(aspectRatio, "9:16")) {
            fov = 50;
            cameraDistance = sDistance9_16[index];
        } else if (TextUtils.equals(aspectRatio, "1:1")) {
            fov = 74;
            cameraDistance = sDistance1_1[index];
        } else {
            throw new RuntimeException("aspectRatio no support ! :" + aspectRatio);
        }
        return new Pair<>(fov, cameraDistance);
    }

    private static int index(int fieldOfView) {
        for (int i = 0; i < sFieldOfViews.length; i++) {
            if (sFieldOfViews[i] == fieldOfView) {
                return i;
            }
        }
        throw new RuntimeException("fieldOfView no support ! :" + fieldOfView);
    }
}
