package com.pi.pano;

import android.util.Log;

import androidx.annotation.NonNull;

import com.pi.pilot.pano.sdk.BuildConfig;

import java.util.Arrays;

/**
 * Calculation of exposure time for HDR.
 */
class HdrExposureTimeCalculator {
    private static final String TAG = HdrExposureTimeCalculator.class.getSimpleName();

    /**
     * Obtain exposure time
     *
     * @param hdrCount         hdr count
     * @param baseExposureTime Exposure base value, refer to this value for multiple exposures.
     */
    @NonNull
    static long[] getExposureTimes(int hdrCount, long baseExposureTime) {
        long[] dst_exposure_times = new long[hdrCount];
        float[] exposureScale = getExposureTimeScales(hdrCount);
        float compensateExposureScale = getHdrExCompensateScale();
        for (int i = 0; i < dst_exposure_times.length; i++) {
            dst_exposure_times[i] = (long) (baseExposureTime * exposureScale[i] * compensateExposureScale);
        }
        Log.d(TAG, "get exposure times, base:" + baseExposureTime + ", compensate scale:" + compensateExposureScale + ", dst:" + Arrays.toString(dst_exposure_times));
        return dst_exposure_times;
    }

    /**
     * Obtain exposure time scaling ratio.
     */
    private static float[] getExposureTimeScales(int hdrCount) {
        if (hdrCount == 3) {
            return new float[]{1 / 1.5f, 1, 1.5f};
        } else if (hdrCount == 5) {
            return new float[]{1 / 2f, 1 / 1.5f, 1, 1.5f, 2};
        } else if (hdrCount == 7) {
            return new float[]{1 / 2.5f, 1 / 2f, 1 / 1.5f, 1, 1.5f, 2, 2.5f};
        } else if (hdrCount == 9) {
            return new float[]{1 / 3f, 1 / 2.5f, 1 / 2f, 1 / 1.5f, 1, 1.5f, 2, 2.5f, 3};
        }
        throw new RuntimeException("HdrCount isn't support!");
    }

    private static float getHdrExCompensateScale() {
        if (BuildConfig.DEBUG) {
            return SystemPropertiesProxy.getFloat("persist.dev.photo.hdr.exCompensateScale", 1.9f);
        }
        return 1.0f;
    }
}
