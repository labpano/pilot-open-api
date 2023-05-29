package com.pi.pano.wrap.config;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileConfigUtils {
    private static final String TAG = FileConfigUtils.class.getSimpleName();

    /**
     * 初始化 创建 照片的 .config 配置文件
     *
     * @param srcFilePath 源视频路径
     * @param fileConfig  config内容
     * @param isPhoto     true : srcFilePath是图片
     */
    public static void initCreateConfigFile(String srcFilePath, FileConfig fileConfig, boolean isPhoto) {
        if (isPhoto) {
            initCreateConfigFileForPhoto(srcFilePath, fileConfig);
        } else {
            initCreateConfigFileForVideo(srcFilePath, fileConfig);
        }
    }

    private static void initCreateConfigFileForPhoto(String srcFilePath, FileConfig fileConfig) {
        Log.d(TAG, "initCreateConfigFileForPhoto prepare ==> " + srcFilePath + "," + fileConfig);
        if (TextUtils.isEmpty(srcFilePath)) {
            return;
        }
        File configFile = getConfigFile(srcFilePath);
        if (configFile != null) {
            JSONObject json = new JSONObject();
            try {
                json.put(FileConfig.Key.KEY_FITTINGS, fileConfig.getFittings());
                json.put(FileConfig.Key.KEY_RESOLUTION, fileConfig.getResolution());
                json.put(FileConfig.Key.KEY_VERSION, fileConfig.getVersion());
                if (fileConfig.getHdrCount() > 0) {
                    json.put(FileConfig.Key.KEY_HDR_COUNT, fileConfig.getHdrCount());
                }
                modifyFileContent(configFile.getAbsolutePath(), json.toString(), false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private static void initCreateConfigFileForVideo(String srcFilePath, FileConfig fileConfig) {
        Log.d(TAG, "initCreateConfigFileForVideo prepare ==> " + srcFilePath + "," + fileConfig);
        if (TextUtils.isEmpty(srcFilePath)) {
            return;
        }
        File configFile = getConfigFile(srcFilePath);
        if (configFile != null) {
            JSONObject json = new JSONObject();
            try {
                json.put(FileConfig.Key.KEY_FITTINGS, fileConfig.getFittings());
                json.put(FileConfig.Key.KEY_RESOLUTION, fileConfig.getResolution());
                json.put(FileConfig.Key.KEY_VERSION, fileConfig.getVersion());
                json.put(FileConfig.Key.KEY_FPS, fileConfig.getFps());
                json.put(FileConfig.Key.KEY_BITRATE, fileConfig.getBitrate());
                json.put(FileConfig.Key.KEY_SPATIAL_AUDIO, fileConfig.isSpatialAudio());
                json.put(FileConfig.Key.KEY_FIELD_OF_VIEW, fileConfig.getFieldOfView());
                json.put(FileConfig.Key.KEY_TIME_LAPSE_RATIO, fileConfig.getTimelapseRatio());
                modifyFileContent(configFile.getAbsolutePath(), json.toString(), false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private static File getConfigFile(String srcFilePath) {
        File file = new File(srcFilePath);
        File rootFile = null;
        String tag = "/DCIM/";
        if (srcFilePath.contains(tag)) {
            int index = srcFilePath.indexOf(tag);
            rootFile = new File(srcFilePath.substring(0, index + tag.length()));
            rootFile = new File(rootFile, ".config");
            rootFile.mkdirs();
        }
        if (rootFile != null && rootFile.exists()) {
            File configFile = new File(rootFile, file.getName() + ".config");
            Log.d(TAG, "getConfigFile result : " + configFile.getAbsolutePath());
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return configFile;
        }
        return null;
    }

    private static void modifyFileContent(String path, String content, boolean append) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, append))) {
            writer.append(content);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
