package com.pi.pano;

/**
 * Video stitching listener.
 */
public interface StitchingListener
{
    /**
     */
    void onStitchingProgressChange(StitchingUtil.Task task);

    /**
     */
    void onStitchingStateChange(StitchingUtil.Task task);

    /**
     */
    void onStitchingDeleteFinish(StitchingUtil.Task task);

    /**
     */
    void onStitchingDeleteDeleting(StitchingUtil.Task task);
}
