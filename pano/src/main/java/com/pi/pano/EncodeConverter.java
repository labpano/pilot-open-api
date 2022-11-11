package com.pi.pano;

import android.media.MediaFormat;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;

import com.pi.pano.annotation.PiVideoEncode;

class EncodeConverter {

    @NonNull
    static String convertVideoMime(@PiVideoEncode String encode) {
        switch (encode) {
            case PiVideoEncode.h_265:
                return MediaFormat.MIMETYPE_VIDEO_HEVC;
            default:
            case PiVideoEncode.h_264:
                return MediaFormat.MIMETYPE_VIDEO_AVC;
        }
    }

    static int convertVideoEncode(@PiVideoEncode String encode) {
        switch (encode) {
            case PiVideoEncode.h_265:
                return MediaRecorder.VideoEncoder.HEVC;
            default:
            case PiVideoEncode.h_264:
                return MediaRecorder.VideoEncoder.H264;
        }
    }
}
