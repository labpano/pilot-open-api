package com.pi.pano;

public class CpuTemperatureReader {
    /**
     * Get the cpu temperature.
     *
     * @return Celsius temperature, value *1000.
     */
    public static String getCpuTemperature() {
        return Utils.readContentSingleLine("/sys/class/thermal/thermal_zone0/temp");
    }
}
