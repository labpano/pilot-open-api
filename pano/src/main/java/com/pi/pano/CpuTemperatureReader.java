package com.pi.pano;

/**
 * cpu temperature reader.
 */
public class CpuTemperatureReader {
    /**
     * get cpu temperature.
     *
     * @return It should be divided by 1000 to get the temperature in Celsius.
     */
    public static String getCpuTemperature() {
        return Utils.readContentSingleLine("/sys/class/thermal/thermal_zone0/temp");
    }

    /**
     * Get the cpu temperature.
     *
     * @return Celsius temperature value.
     */
    public static int getCpuTemperature2() {
        try {
            String temperature = getCpuTemperature();
            return Integer.parseInt(temperature) / 1000;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }
}
