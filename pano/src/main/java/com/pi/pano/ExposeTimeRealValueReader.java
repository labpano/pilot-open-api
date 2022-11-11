package com.pi.pano;

/**
 * Exposure value reading during automatic exposure.
 */
public class ExposeTimeRealValueReader {
    private static final String DEVICE_FILE_EXPOSE_TIME = "/sys/devices/platform/10990000.hsi2c/i2c-0/0-001a/exp_time";

    /**
     * Read the real value of exposure time.
     */
    public static int obtainExposureTime() {
        String content = Utils.readContentSingleLine(DEVICE_FILE_EXPOSE_TIME);
        if (content == null || content.length() == 0) { // 读取失败后尝试再读一次
            content = Utils.readContentSingleLine(DEVICE_FILE_EXPOSE_TIME);
        }
        if (null != content) {
            try {
                int value = Integer.parseInt(content.split(",")[0]);
                return 100000 / value;
            } catch (Exception ignore) {
            }
        }
        return 0;
    }
}
