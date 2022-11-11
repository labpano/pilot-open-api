package com.pi.pano;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.StringDef;

import com.pi.pano.annotation.PiExposureTime;
import com.pi.pano.annotation.PiIsoInManualEp;

/**
 * manually adjusting exposure time.
 */
public final class ExposeTimeAdjustHelper {
    private static final String TAG = ExposeTimeAdjustHelper.class.getSimpleName();

    @StringDef({State.ENABLED, State.DISABLED})
    public @interface State {
        /**
         * 手动曝光
         */
        String ENABLED = "enabled";
        /**
         * 手动曝光关闭-即自动曝光
         */
        String DISABLED = "disabled";
    }

    /**
     * 曝光开关路径
     */
    private static final String EXPOSED_PATH_SWITCH = "/efs/.ex_en";

    /**
     * 曝光参数值
     */
    private static final String EXPOSED_PATH_VALUE = "/efs/.ex_val";

    private @interface WriteValue {
        /**
         * 关闭
         */
        String close = "0";
        /**
         * 打开
         */
        String open = "1";
    }

    /**
     * 获取曝光状态
     */
    public static String getState() {
        return Utils.readContent(EXPOSED_PATH_SWITCH);
    }

    /**
     * 手动曝光时间是否开启
     */
    public static boolean isOpen() {
        return State.ENABLED.equals(getState());
    }

    /**
     * 打开手调曝光
     */
    public static void open() {
        if (State.ENABLED.equals(getState())) {
            return;
        }
        Log.d(TAG, "open");
        Utils.modifyFile(EXPOSED_PATH_SWITCH, WriteValue.open);
        markChangeTime(0);
    }

    /**
     * 关闭手调曝光
     */
    public static void close() {
        if (State.DISABLED.equals(getState())) {
            return;
        }
        Log.d(TAG, "close");
        Utils.modifyFile(EXPOSED_PATH_SWITCH, WriteValue.close);
        markChangeTime(3500);
    }

    private static volatile long sChangeTimestamp = 0;
    private static volatile long sCostTime = 3500;

    private static synchronized void markChangeTime(int costTime) {
        sChangeTimestamp = SystemClock.elapsedRealtime();
        sCostTime = costTime;
    }

    public static long getAdjustRemainTime() {
        if (sCostTime > 0) {
            long time = SystemClock.elapsedRealtime() - sChangeTimestamp;
            if (time >= sCostTime) {
                return 0;
            }
            return sCostTime - time;
        }
        return 0;
    }

    /**
     * 等候调整完成
     */
    public static void waitAdjustFinish() {
        long remainTime = getAdjustRemainTime();
        if (remainTime > 0) {
            SystemClock.sleep(remainTime);
        }
    }

    private static int sExpose;
    private static int sAnalogGain;

    /**
     * 设置配置参数节点
     *
     * @param fps        当前场景下帧率
     * @param expose     曝光时间
     * @param analogGain 模拟gain
     * @param digitGain  数字gain
     */
    public static void setValue(String fps, @PiExposureTime int expose, @PiIsoInManualEp int analogGain, @PiIsoInManualEp int digitGain) {
        Log.d(TAG, "setValues,fps:" + fps + ",expose:" + expose + ",analogGain:" + analogGain);
        if (expose == PiExposureTime.auto) {
            Log.e(TAG, "setValues,set auto error!");
            return;
        }
        sExpose = expose;
        sAnalogGain = analogGain;
        int exposedTime = Math.floorDiv(1000000, expose);
        final String content = fps + " " + exposedTime + " " + analogGain + " " + digitGain;
        Utils.modifyFile(EXPOSED_PATH_VALUE, content);
    }

    public static int getExpose() {
        return sExpose;
    }

    public static int getISO() {
        return sAnalogGain;
    }
}
