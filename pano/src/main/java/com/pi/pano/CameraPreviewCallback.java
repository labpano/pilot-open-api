package com.pi.pano;

import java.nio.ByteBuffer;

/**
 * Preview callback
 */
public interface CameraPreviewCallback {
    /**
     * Preview available
     *
     * @param width     preview width
     * @param height    preview height
     * @param timestamp timestamp
     * @param buffer    buffer info
     */
    void onPreviewCallback(int width, int height, long timestamp, ByteBuffer buffer);
}
