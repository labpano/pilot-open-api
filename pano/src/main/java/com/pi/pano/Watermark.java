package com.pi.pano;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

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
 * 水印即底部logo设置
 */
public final class Watermark {
    private static final String TAG = Watermark.class.getSimpleName();

    /**
     * 无水印名称，即不使用水印
     */
    public static final String NAME_NONE = "null";
    /**
     * 默认水印名称
     */
    public static final String NAME_DEFAULT = "default";

    /**
     * 水印设置文件
     */
    private static final String WATERMARKS_SETTING_PATH = "/sdcard/Watermarks/setting";
    /**
     * 设置文件开始时用于保存水印名称的长度
     */
    private static final int NAME_LENGTH = 256;

    @NonNull
    private static File obtainSettingFile() {
        File settingFile = new File(WATERMARKS_SETTING_PATH);
        File parentFile = settingFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        return settingFile;
    }

    private Watermark() {
    }

    /**
     * 检查水印配置文件。
     * 不存在时是否释放默认水印
     */
    public static void checkWatermarkConfig(Context context) {
        File settingFile = obtainSettingFile();
        if (!settingFile.exists()) {
            Log.w(TAG, "watermark setting file is not exists,reset to default");
            resetDefaultWatermark(context);
        }
    }

    /**
     * 是否使用水印
     *
     * @return 是否使用水印
     */
    public static boolean isWatermarkAble() {
        return !NAME_NONE.equals(getWatermarkName());
    }

    /**
     * 是否使用默认水印
     *
     * @return 是否使用默认水印
     */
    public static boolean isDefaultWatermark() {
        return NAME_DEFAULT.equals(getWatermarkName());
    }

    /**
     * 重置水印为默认
     *
     * @param context 上下文
     * @return 水印名称, 返回空时写入失败
     */
    @Nullable
    public static String resetDefaultWatermark(Context context) {
        InputStream inputStream = getDefaultWatermarkStream(context);
        return writeInfo(NAME_DEFAULT, inputStream);
    }

    /**
     * @return 默认水印流，png
     */
    @NonNull
    public static InputStream getDefaultWatermarkStream(Context context) {
        return context.getResources().openRawResource(R.raw.watermark);
    }

    /**
     * 设置不使用水印
     *
     * @return 水印名称, 返回空时写入失败
     */
    @Nullable
    public static String disableWatermark() {
        return writeInfo(NAME_NONE, null);
    }

    /**
     * 设置水印.
     * 水印文件为空、不存在或不可用时，将使用默认水印。
     *
     * @param context 上下文
     * @param file    使用的水印文件
     * @return 水印名称, 返回空时写入失败
     */
    @Nullable
    public static String enableWatermark(@NonNull Context context, @Nullable File file) {
        if (null != file) {
            try {
                return enableWatermark(file);
            } catch (Exception ex) {
                Log.w(TAG, "enable watermark error,file:" + file + ",ex:" + ex);
                ex.printStackTrace();
            }
        }
        return resetDefaultWatermark(context);
    }

    /**
     * 设置水印
     *
     * @param file 使用的水印文件
     * @return 水印名称, 返回空时写入失败
     */
    @Nullable
    public static String enableWatermark(@NonNull File file) {
        String name;
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "enable watermark error,file:" + file + ",ex:" + ex);
            return null;
        }
        name = file.getName();
        return setWatermark(name, inputStream);
    }

    /**
     * 设置水印
     *
     * @param name        水印名称
     * @param inputStream 水印内容流
     * @return 水印名称, 返回空时写入失败
     */
    @Nullable
    private static String setWatermark(@NonNull String name, @Nullable InputStream inputStream) {
        if (NAME_NONE.equals(name)) {
            inputStream = null;
        }
        return writeInfo(name, inputStream);
    }

    /**
     * @return 水印名称
     */
    @NonNull
    public static String getWatermarkName() {
        String name = NAME_NONE;
        File settingFile = obtainSettingFile();
        if (settingFile.isFile()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(settingFile);
                byte[] buffer = new byte[NAME_LENGTH];
                if (inputStream.read(buffer) == NAME_LENGTH) {
                    int i;
                    for (i = 0; i < buffer.length; ++i) {
                        if (buffer[i] == 0) {
                            break;
                        }
                    }
                    if (i > 0) {
                        name = new String(buffer, 0, i);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "get watermark name error,ex:" + ex);
                ex.printStackTrace();
            } finally {
                closeIO(inputStream);
            }
        }
        return name;
    }

    /**
     * @return 水印流，png
     */
    @Nullable
    public static InputStream getWatermarkStream() {
        InputStream inputStream = null;
        File settingFile = obtainSettingFile();
        if (settingFile.exists()) {
            try {
                inputStream = new FileInputStream(settingFile);
                inputStream.skip(NAME_LENGTH);
            } catch (Exception ex) {
                Log.e(TAG, "get watermark stream,error,ex:" + ex);
                ex.printStackTrace();
            }
        }
        return inputStream;
    }

    /**
     * 写入水印信息
     *
     * @param name        水印名称
     * @param inputStream 水印内容，png
     * @return 水印名称, 返回空时写入失败
     */
    @Nullable
    private static String writeInfo(@NonNull String name, @Nullable InputStream inputStream) {
        Log.d(TAG, "write info,name:" + name);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(obtainSettingFile());
            // 写入水印名称
            byte[] buffer = new byte[NAME_LENGTH];
            if (!TextUtils.isEmpty(name)) {
                byte[] bytes = name.getBytes();
                System.arraycopy(bytes, 0, buffer, 0, Math.min(bytes.length, buffer.length));
            }
            outputStream.write(buffer);
            // 写入水印内容
            if (null != inputStream) {
                buffer = new byte[1024 * 100];
                int i;
                while ((i = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, i);
                }
            }
            //
            outputStream.flush();
            return name;
        } catch (Exception ex) {
            Log.e(TAG, "write info error,ex:" + ex);
            ex.printStackTrace();
            return null;
        } finally {
            closeIO(outputStream);
            closeIO(inputStream);
        }
    }

    private static void closeIO(Closeable io) {
        if (null != io) {
            try {
                io.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 检测是否是水印图片
     */
    public static boolean isWatermark(@NonNull File file) {
        if (file.exists()) {
            if (file.length() <= 1024 * 1024 && isPNG(file)) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                return opts.outWidth == 512 && opts.outHeight == 512;
            }
        }
        return false;
    }

    /**
     * 判断是否为png类型
     */
    private static boolean isPNG(@NonNull File file) {
        byte[] png_header = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        // 读取文件前4个字节
        byte[] header = null;
        try (FileInputStream is = new FileInputStream(file)) {
            header = new byte[4];
            is.read(header, 0, header.length);
        } catch (Exception ignored) {
        }
        if (null != header) {
            for (int i = 0; i < 4; i++) {
                if (png_header[i] != header[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
