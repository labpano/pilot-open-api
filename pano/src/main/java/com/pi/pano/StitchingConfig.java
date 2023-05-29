package com.pi.pano;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Edit Parameter Save<br/>
 */
public class StitchingConfig {

    /**
     * 视频编辑配置文件
     */
    public static final String STITCH_CONFIG_FILE_NAME = "stitchingConfig";

    /**
     * 获取后编辑视频配置文件
     */
    public static File getStitchingConfigFile(File dir) {
        return new File(dir, STITCH_CONFIG_FILE_NAME);
    }

    public static void deleteStitchingConfigFile(File dir) {
        File configFile = getStitchingConfigFile(dir);
        if (configFile.exists()) {
            if (configFile.isFile()) {
                configFile.delete();
            }
        }
    }

    public @interface Keys {
        /**
         * 高光
         */
        String KEY_HIGHLIGHTS = "HIGHLIGHTS";
        /**
         * 阴影
         */
        String KEY_SHADOW = "SHADOW";
        /**
         * 亮度
         */
        String KEY_BRIGHT = "BRIGHT";
        /**
         * 伽马
         */
        String KEY_GAMMA = "GAMMA";
        /**
         * 饱和度
         */
        String KEY_SATURATION = "SATURATION";
        /**
         * 色温
         */
        String KEY_TEMPERATURE = "TEMPERATURE";
    }

    public int mHighlights;
    public int mShadow;
    public int mBright;
    public int mGamma;
    public int mSaturation;
    public int mTemperature;

    @Nullable
    public static StitchingConfig getConfigure(File propFile) {
        HashMap<String, String> map = get(propFile,
                Keys.KEY_HIGHLIGHTS, Keys.KEY_SHADOW, Keys.KEY_BRIGHT,
                Keys.KEY_GAMMA, Keys.KEY_SATURATION, Keys.KEY_TEMPERATURE
        );
        if (!map.isEmpty()) {
            StitchingConfig stitchingConfig = new StitchingConfig();
            stitchingConfig.mHighlights = convertToInt(map.get(Keys.KEY_HIGHLIGHTS), 0);
            stitchingConfig.mShadow = convertToInt(map.get(Keys.KEY_SHADOW), 0);
            stitchingConfig.mBright = convertToInt(map.get(Keys.KEY_BRIGHT), 0);
            stitchingConfig.mGamma = convertToInt(map.get(Keys.KEY_GAMMA), 0);
            stitchingConfig.mSaturation = convertToInt(map.get(Keys.KEY_SATURATION), 0);
            stitchingConfig.mTemperature = convertToInt(map.get(Keys.KEY_TEMPERATURE), 0);
            return stitchingConfig;
        }
        return null;
    }

    /**
     * 高光
     */
    public static void setHighlightsValue(File propFile, final int highlights) {
        set(propFile, Keys.KEY_HIGHLIGHTS, String.valueOf(highlights));
    }

    /**
     * 阴影
     */
    public static void setShadowValue(File propFile, final int shadow) {
        set(propFile, Keys.KEY_SHADOW, String.valueOf(shadow));
    }

    /**
     * 亮度
     */
    public static void setBirghtValue(File propFile, final int bright) {
        set(propFile, Keys.KEY_BRIGHT, String.valueOf(bright));
    }

    /**
     * 伽马
     */
    public static void setGammaValue(File propFile, final int gamma) {
        set(propFile, Keys.KEY_GAMMA, String.valueOf(gamma));
    }

    /**
     * 饱和度
     */
    public static void setSaturationValue(File propFile, final int saturation) {
        set(propFile, Keys.KEY_SATURATION, String.valueOf(saturation));
    }

    /**
     * 色温
     */
    public static void setTempeValue(File propFile, final int temperature) {
        set(propFile, Keys.KEY_TEMPERATURE, String.valueOf(temperature));
    }

    protected static float highLightConvert(final int percentage) {
        return -range(percentage, -3.5f, 1.5f);
    }

    protected static float shadowsConvert(final int percentage) {
        return range(percentage, -1.5f, 3.5f);
    }

    protected static float brightnessConvert(final int percentage) {
        return range(percentage, -1.0f, 1.0f);
    }

    protected static float saturationConvert(final int percentage) {
        return range(percentage, 0.0f, 2.0f);
    }

    protected static float gammaConvert(final int percentage) {
        return range(percentage, 0.0f, 2.0f);
    }

    protected static float tempeConvert(final int percentage) {
        return range(percentage, 2000.0f, 8000.0f);
    }

    /**
     * 范围转换
     *
     * @param percentage 0 - 200
     * @param start      起始大小
     * @param end        结束大小
     * @return 返回范围内小数值
     */
    protected static float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 200.0f + start;
    }

    /**
     * 获取全部属性
     */
    private static HashMap<String, String> get(File file, String... key) {
        Properties props = read(file);
        final HashMap<String, String> map = new HashMap<>();
        for (String k : key) {
            map.put(k, props.getProperty(k));
        }
        return map;
    }

    /**
     * 对外提供的保存key value方法
     */
    private static void set(File file, String key, String value) {
        Properties props = read(file);
        props.setProperty(key, value);
        storeProps(file, props);
    }

    /**
     * 设置属性
     */
    private static void storeProps(File file, Properties p) {
        FileOutputStream fos = null;
        try {
            if (!file.exists() && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            fos = new FileOutputStream(file);
            p.store(fos, null);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeIO(fos);
        }
    }

    /**
     * 加载属性文件
     */
    private static Properties read(File file) {
        Properties props = new Properties();
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                props.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeIO(fis);
            }
        }
        return props;
    }

    private static int convertToInt(String text, int defValue) {
        if (TextUtils.isEmpty(text)) {
            return defValue;
        } else {
            return Integer.parseInt(text);
        }
    }

    /**
     * 关闭IO
     *
     * @param closeables closeables
     */
    private static void closeIO(final Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
