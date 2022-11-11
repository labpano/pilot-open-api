package com.pi.pano;

/**
 * Steady distance distance helper.
 */
final class SteadyDistanceHelper {
    private static final int STEADY_ADD_VALUE = 1000_000;
    private static final long STEADY_MIN_VALUE = 5000_000;
    private static final long STEADY_MAX_VALUE = 25000_000;

    /**
     * set steady distance.
     *
     * @param isHighResolution whether it is greater than 4k.
     */
    static void setSteadyDistance(PiPano pano, boolean isHighResolution) {
        long value;
        if (isHighResolution) {
            value = 14 * STEADY_ADD_VALUE;
        } else {
            value = 11 * STEADY_ADD_VALUE;
        }
        pano.nativeStabSetTimeoffset(-1, value);
    }
}
