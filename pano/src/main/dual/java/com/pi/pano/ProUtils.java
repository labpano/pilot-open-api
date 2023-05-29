package com.pi.pano;

import android.hardware.camera2.CaptureRequest;

import com.pi.pano.annotation.PiAntiMode;
import com.pi.pano.annotation.PiWhiteBalance;

class ProUtils {
    static int convertRealWhiteBalance(@PiWhiteBalance String value) {
        switch (value) {
            case PiWhiteBalance.auto:
                return CaptureRequest.CONTROL_AWB_MODE_AUTO;
            case PiWhiteBalance.incandescent://钨丝灯
                return CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT;
            case PiWhiteBalance.fluorescent://荧光灯
                return CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT;
            case PiWhiteBalance.daylight://太阳光
                return CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT;
            case PiWhiteBalance.cloudy_daylight://阴天
                return CaptureRequest.CONTROL_AWB_MODE_SHADE;
            default:
                throw new RuntimeException("Illegal value");
        }
    }

    @PiWhiteBalance
    static String convertUserWhiteBalance(int value) {
        switch (value) {
            default:
            case CaptureRequest.CONTROL_AWB_MODE_AUTO:
                return PiWhiteBalance.auto;
            case CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT:
                return PiWhiteBalance.incandescent;
            case CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT:
                return PiWhiteBalance.fluorescent;
            case CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT:
                return PiWhiteBalance.daylight;
            case CaptureRequest.CONTROL_AWB_MODE_SHADE:
                return PiWhiteBalance.cloudy_daylight;
        }
    }

    static int convertRealAntiMode(@PiAntiMode String value) {
        switch (value) {
            case PiAntiMode.off:
                return CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF;
            case PiAntiMode._50Hz:
                return CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ;
            case PiAntiMode._60Hz:
                return CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_60HZ;
            case PiAntiMode.auto:
                return CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO;
            default:
                throw new RuntimeException("Illegal value");
        }
    }
}