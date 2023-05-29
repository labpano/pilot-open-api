package com.pi.pano;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    /**
     * 获取未拼接视频文件的基本文件名
     */
    static String getVideoUnstitchFileSimpleName(File file) {
        String fileName = file.getName();
        int index = fileName.lastIndexOf("_u");
        if (index >= 0) {
            fileName = fileName.substring(0, index);
        }
        return fileName;
    }

    static String readContentSingleLine(String path) {
        String result = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            result = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(reader);
        }
        return result;
    }

    static String readContent(String path) {
        BufferedReader reader = null;
        try {
            FileInputStream inputStream = new FileInputStream(path);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            reader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString().trim();
        } catch (IOException ignore) {
        } finally {
            closeIO(reader);
        }
        return "";
    }

    static void modifyFile(String path, String content) {
        BufferedWriter writer = null;
        try {
            FileWriter fileWriter = new FileWriter(path, false);
            writer = new BufferedWriter(fileWriter);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (Exception ignore) {
        } finally {
            closeIO(writer);
        }
    }

    static void closeIO(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
