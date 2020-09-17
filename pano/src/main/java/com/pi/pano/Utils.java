package com.pi.pano;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class Utils {
    /**
     * Get the real-time temperature of the device.
     * Excessive temperature may cause equipment instability
     */
    public static String getCpuTemperature() {
        String result = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
            result = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
