package com.pi.pano;

import androidx.annotation.Keep;

/**
 * AI listener
 */
public interface OnAIDetectionListener {
    /**
     * detection type
     */
    int MSG_DETECTION = 0;
    /**
     * track type
     */
    int MSG_TRACKING = 20;

    /**
     * @param msg   type,{@link OnAIDetectionListener#MSG_DETECTION}、{@link OnAIDetectionListener#MSG_TRACKING}
     * @param count in detection， the number of detections;
     *              in track, the number of consecutive losses.
     */
    @Keep
    void onDetectResult(int msg, int count);
}
