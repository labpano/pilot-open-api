package com.pi.pano;

/**
 * Video post-processing monitoring can monitor the progress of video splicing.
 */
public interface StitchingListener {
    /**
     * StitchingUtil.Task progress changes
     *
     * @param task task of progress change
     */
    void onStitchingProgressChange(StitchingUtil.Task task);

    /**
     * StitchingUtil.Task state changes
     *
     * @param task task of state change
     */
    void onStitchingStateChange(StitchingUtil.Task task);

    /**
     * delete finish
     *
     * @param task affected task
     */
    void onStitchingDeleteFinish(StitchingUtil.Task task);

    /**
     * delete start
     *
     * @param task affected task
     */
    void onStitchingDeleteDeleting(StitchingUtil.Task task);
}
