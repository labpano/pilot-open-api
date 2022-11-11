package com.pi.pano;

/**
 * Media playback error callback.
 */
public interface MediaRecorderListener {
    /**
     * @param what error
     */
    void onError(int what);
}