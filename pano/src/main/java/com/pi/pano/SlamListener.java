package com.pi.pano;

/**
 * roam listener
 */
public interface SlamListener {
    /**
     * Tracking state changes
     *
     * @param state 0 - uninitialized; 1 - initializing; 2 - tracking; 3 - missing tracking
     */
    void onTrackStateChange(int state);

    /**
     * An error occurred
     *
     * @param error Error code, 0 - initialization success, no error, greater than 0 means an error
     */
    void onError(int error);

    /**
     * After slam, call back to this interface, which contains the position data of the photo
     *
     * @param position A string containing photo location data
     */
    void onPhotoPosition(String position);
}
