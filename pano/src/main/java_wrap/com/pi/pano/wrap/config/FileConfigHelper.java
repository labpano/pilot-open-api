package com.pi.pano.wrap.config;

import android.util.Log;

import com.pi.pano.CaptureParams;
import com.pi.pano.RecordParams;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * .config文件辅助类
 *
 * @CreateDate: 2022/12/20 14:50
 */
public class FileConfigHelper {
    private static final String TAG = FileConfigHelper.class.getSimpleName();

    private final ExecutorService sExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "FileConfigHelper_" + count.incrementAndGet());
        }
    });

    private FileConfigHelper() {
    }

    private static final class Holder {
        private static final FileConfigHelper INSTANCE = new FileConfigHelper();
    }

    public static FileConfigHelper self() {
        return Holder.INSTANCE;
    }

    /**
     * 初始化视频的 config数据
     *
     * @param filePath 文件路径
     */
    public FileConfig create(String filePath, RecordParams params) {
        Log.d(TAG, "start ==> " + filePath + "," + params);
        IFileConfigAdapter adapter = new FileConfigAdapter();
        adapter.initVideoConfig(params);
        FileConfig config = adapter.getFileConfig();
        if (config != null) {
            config.filePath = filePath;
        }
        return config;
    }


    /**
     * 初始化照片的 config数据
     */
    public FileConfig create(CaptureParams params) {
        Log.d(TAG, "start ====>" + params);
        IFileConfigAdapter adapter = new FileConfigAdapter();
        adapter.initPhotoConfig(params);
        return adapter.getFileConfig();
    }

    /**
     * 保存 config数据到本地文件
     */
    public void saveConfig(FileConfig config) {
        Log.d(TAG, "saveConfig ==> " + config);
        sExecutor.execute(() -> {
            FileConfigUtils.initCreateConfigFile(config.filePath, config, config.isPhoto);
        });
    }

}
