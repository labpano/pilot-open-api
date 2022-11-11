package com.pi.pano;

/**
 * Iso value reading during automatic exposure.
 */
public class IsoRealValueReader {

    private static final String DEVICE_FILE_ISO = "/sys/devices/platform/10990000.hsi2c/i2c-0/0-001a/cis_gain";

    /**
     * Read the true value of ISO and use it during automatic exposure.
     */
    public static int obtainISO() {
        String content = Utils.readContentSingleLine(DEVICE_FILE_ISO);
        if (content == null || content.length() == 0) { // attempt to read again after a failed read
            content = Utils.readContentSingleLine(DEVICE_FILE_ISO);
        }
        if (null != content) {
            try {
                final String[] split = content.split(",");
                int value_2 = Integer.parseInt(split[1]);
                if (value_2 > 500) {
                    return 1600;
                } else {
                    int value_1 = Integer.parseInt(split[0]);
                    if (value_1 > 930) {
                        return 800;
                    } else if (value_1 > 800) {
                        return 400;
                    } else if (value_1 > 750) {
                        return 200;
                    } else if (value_1 > 500) {
                        return 100;
                    } else {
                        return 50;
                    }
                }
                // 未匹配到具体值
            } catch (Exception ignore) {
            }
        }
        return 0;
    }
}
