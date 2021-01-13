package com.pi.pano;

import android.util.Log;

import androidx.annotation.StringDef;

import com.pi.pano.annotation.PiProEt;
import com.pi.pano.annotation.PiProIsoInManualEx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manual exposure settings
 */
public final class ExposeTimeAdjustHelper {
    private static final String TAG = ExposeTimeAdjustHelper.class.getSimpleName();

    @StringDef({State.ENABLED, State.DISABLED})
    public @interface State {
        /**
         * Manual exposure enabled
         */
        String ENABLED = "enabled";
        /**
         * Manual exposure disabled(automatic exposure)
         */
        String DISABLED = "disabled";
    }

    private static final String EXPOSED_PATH_SWITCH = "/efs/.ex_en";
    private static final String EXPOSED_PATH_VALUE = "/efs/.ex_val";

    private @interface WriteValue {
        String close = "0";
        String open = "1";
    }

    /**
     * Get exposure status
     */
    public static String getState() {
        return readContent(EXPOSED_PATH_SWITCH);
    }

    private static volatile long changeTimestamp = 0;
    private static volatile long costTime = 3500;

    /**
     * Turn on manual exposure
     */
    public static void open() {
        if (State.ENABLED.equals(getState())) {
            return;
        }
        Log.d(TAG, "open");
        modifyFile(EXPOSED_PATH_SWITCH, WriteValue.open);
        markChangeTime(State.ENABLED);
    }

    /**
     * Turn off manual exposure
     */
    public static void close() {
        if (State.DISABLED.equals(getState())) {
            return;
        }
        Log.d(TAG, "close");
        modifyFile(EXPOSED_PATH_SWITCH, WriteValue.close);
        markChangeTime(State.DISABLED);
    }

    private static synchronized void markChangeTime(String state) {
        changeTimestamp = System.currentTimeMillis();
        if (State.DISABLED.equals(state)) {
            costTime = 3500;
        } else {
            costTime = 0;
        }
    }

    public static long getAdjustRemainTime() {
        if (costTime > 0) {
            long time = System.currentTimeMillis() - changeTimestamp;
            if (time >= costTime) {
                return 0;
            }
            return costTime - time;
        }
        return 0;
    }

    private static int mExpose;
    private static int mAnalogGain;

    /**
     * Setting manual exposure parameters
     *
     * @param fps        current frame rate
     * @param expose     time of exposure
     * @param analogGain Analog gain
     * @param digitGain  Digital gain
     */
    public static void setValues(String fps, @PiProEt int expose, @PiProIsoInManualEx int analogGain, @PiProIsoInManualEx int digitGain) {
        Log.d(TAG, "setValues,fps:" + fps + ",expose:" + expose + ",analogGain:" + analogGain);
        mExpose = expose;
        mAnalogGain = analogGain;
        int exposedTime = Math.floorDiv(1000000, expose);
        final String content = fps + " " + exposedTime + " " + analogGain + " " + digitGain;
        modifyFile(EXPOSED_PATH_VALUE, content);
    }

    public static int getExpose() {
        return mExpose;
    }

    public static int getISO() {
        return mAnalogGain;
    }

    private static String readContent(String filePath) {
        try {
            FileInputStream inputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString().trim();
        } catch (IOException ignore) {
        }
        return "";
    }

    private static void modifyFile(String path, String content) {
        try {
            FileWriter fileWriter = new FileWriter(path, false);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (Exception ignore) {
        }
    }
}
