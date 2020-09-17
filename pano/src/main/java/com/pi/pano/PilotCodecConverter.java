package com.pi.pano;

import android.media.MediaFormat;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;

class PilotCodecConverter {

    /**
     * Convert to internally used value
     */
    @NonNull
    static String convertVideoMime(int codec) {
        switch (codec) {
            case 1:
                return MediaFormat.MIMETYPE_VIDEO_HEVC;
            case 0:
            default:
                return MediaFormat.MIMETYPE_VIDEO_AVC;
        }
    }

    /**
     * Convert to internally used value
     */
    @NonNull
    static int convertVideoEncode(int codec) {
        switch (codec) {
            case 1:
                return MediaRecorder.VideoEncoder.HEVC;
            case 0:
            default:
                return MediaRecorder.VideoEncoder.H264;
        }
    }
}
