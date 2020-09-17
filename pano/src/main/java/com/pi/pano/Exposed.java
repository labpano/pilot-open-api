package com.pi.pano;

import androidx.annotation.StringDef;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 手调曝光设置
 */
public final class Exposed {

    @StringDef({State.ENABLED, State.DISABLED})
    public @interface State {
        String ENABLED = "enabled";
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
        String EXPOSED_CLOSE = "0";
        /**
         * 打开
         */
        String EXPOSED_OPEN = "1";
    }

    /**
     * 获取曝光状态
     */
    public static String getState() {
        return readContent(EXPOSED_PATH_SWITCH);
    }

    /**
     * 打开手调曝光
     */
    public static void open() {
        if (State.ENABLED.equals(getState())) {
            return;
        }
        modifyFile(EXPOSED_PATH_SWITCH, WriteValue.EXPOSED_OPEN);
    }

    /**
     * 关闭手调曝光
     */
    public static void close() {
        if (State.DISABLED.equals(getState())) {
            return;
        }
        modifyFile(EXPOSED_PATH_SWITCH, WriteValue.EXPOSED_CLOSE);
    }

    /**
     * 设置配置参数节点
     *
     * @param fps         当前场景下帧率
     * @param exposedTime 曝光时间
     * @param analogGain  模拟gain
     * @param digitGain   数字gain
     */
    public static void setValues(String fps, String exposedTime, String analogGain, String digitGain) {
        final String content = fps + " " + exposedTime + " " + analogGain + " " + digitGain;
        modifyFile(EXPOSED_PATH_VALUE, content);
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
