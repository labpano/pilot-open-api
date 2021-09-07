package com.pi.pano.helper;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pi.pilot.pano.sdk.R;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Watermark is the bottom logo setting.
 */
public final class WatermarkHelper {
    public static final String WATERMARKS_SETTING_FILE_PATH = "/sdcard/Watermarks/setting";

    /**
     * Disable.
     */
    public static final String NAME_DISABLE = "null";
    /**
     * Use default.
     */
    public static final String NAME_DEFAULT = "";

    /**
     * Whether to use the bottom logo watermark.
     */
    public static boolean isOpenWatermark() {
        String filename = getWatermarkName();
        return !NAME_DISABLE.equals(filename);
    }

    /**
     * Get the name of the currently set watermark.
     */
    public static String getWatermarkName() {
        File settingFile = new File(WATERMARKS_SETTING_FILE_PATH);
        String filename = NAME_DEFAULT;//如果不存在水印设置文件名,则表示用默认logo
        if (settingFile.isFile()) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(settingFile);
                byte[] buffer = new byte[256];
                if (fileInputStream.read(buffer) == 256) {
                    int i;
                    for (i = 0; i < buffer.length; ++i) {
                        if (buffer[i] == 0) {
                            break;
                        }
                    }
                    filename = new String(buffer, 0, i);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeIO(fileInputStream);
            }
        }
        return filename;
    }

    public static void disableWatermark() {
        writeContent(NAME_DISABLE, null);
    }

    /**
     * Set bottom watermark.
     *
     * @param watermarkPathname file path.
     */
    public static String enableWatermark(Context context, @Nullable String watermarkPathname) {
        if (NAME_DISABLE.equals(watermarkPathname)) {
            disableWatermark();
            return NAME_DISABLE;
        }
        if (watermarkPathname == null || NAME_DEFAULT.equals(watermarkPathname)) {
            resetDefaultWatermark(context);
            return NAME_DEFAULT;
        }
        return enableWatermark(context, new File(watermarkPathname));
    }

    /**
     * Set bottom watermark.
     *
     * @param watermarkFile file.
     */
    public static String enableWatermark(Context context, @Nullable File watermarkFile) {
        if (watermarkFile == null || !watermarkFile.exists()) {
            resetDefaultWatermark(context);
            return NAME_DEFAULT;
        }
        writeContent(watermarkFile);
        return watermarkFile.getName();
    }

    /**
     * Reset default.
     */
    public static void resetDefaultWatermark(Context context) {
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().openRawResource(R.raw.watermark);
            writeContent(NAME_DEFAULT, inputStream);
        } catch (Exception ignore) {
        } finally {
            closeIO(inputStream);
        }
    }

    private static void writeContent(File watermarkFile) {
        String fileName = watermarkFile.getName();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(watermarkFile);
            writeContent(fileName, inputStream);
        } catch (Exception ignore) {
        } finally {
            closeIO(inputStream);
        }
    }

    private static void writeContent(@NonNull String filename, @Nullable InputStream inputStream) {
        File settingFile = new File(WATERMARKS_SETTING_FILE_PATH);
        File parentFile = settingFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(settingFile);
            byte[] buffer;
            //
            buffer = new byte[256];
            if (TextUtils.isEmpty(filename)) {
                byte[] bytes = filename.getBytes();
                System.arraycopy(bytes, 0, buffer, 0, Math.min(bytes.length, buffer.length));
            }
            fileOutputStream.write(buffer);
            //
            if (null != inputStream) {
                buffer = new byte[1024 * 100];
                int i;
                while ((i = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, i);
                }
            }
            //
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(fileOutputStream);
        }
    }

    @Nullable
    public static InputStream readContent() throws IOException {
        InputStream inputStream = null;
        File file = new File(WATERMARKS_SETTING_FILE_PATH);
        if (file.exists()) {
            inputStream = new FileInputStream(file);
            inputStream.skip(256);
        }
        return inputStream;
    }

    private static void closeIO(Closeable io) {
        if (null != io) {
            try {
                io.close();
            } catch (Exception ignored) {
            }
        }
    }
}
