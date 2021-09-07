package com.pi.pano;

import android.util.Log;

import androidx.annotation.StringDef;

import com.pi.pano.annotation.PiProEt;
import com.pi.pano.annotation.PiProIsoInManualEx;

/**
 * Manual exposure time settings.
 */
public final class ExposeTimeAdjustHelper {
    private static final String TAG = ExposeTimeAdjustHelper.class.getSimpleName();

    @StringDef({State.ENABLED, State.DISABLED})
    public @interface State {
        /**
         * Manual exposure time enabled
         */
        String ENABLED = "enabled";
        /**
         * Manual exposure time disabled(automatic exposure time)
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
     * Get exposure time status
     */
    public static String getState() {
        return Utils.readContent(EXPOSED_PATH_SWITCH);
    }

    private static volatile long changeTimestamp = 0;
    private static volatile long costTime = 3500;

    /**
     * Turn on manual exposure time.
     */
    public static void open() {
        if (State.ENABLED.equals(getState())) {
            return;
        }
        Log.d(TAG, "open");
        Utils.modifyFile(EXPOSED_PATH_SWITCH, WriteValue.open);
        markChangeTime(State.ENABLED);
    }

    /**
     * Turn off manual exposure time.
     */
    public static void close() {
        if (State.DISABLED.equals(getState())) {
            return;
        }
        Log.d(TAG, "close");
        Utils.modifyFile(EXPOSED_PATH_SWITCH, WriteValue.close);
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
     * Setting manual exposure time parameters.
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
        Utils.modifyFile(EXPOSED_PATH_VALUE, content);
    }

    public static int getExpose() {
        return mExpose;
    }

    public static int getISO() {
        return mAnalogGain;
    }
}
