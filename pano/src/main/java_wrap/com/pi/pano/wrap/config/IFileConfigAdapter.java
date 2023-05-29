package com.pi.pano.wrap.config;

import com.pi.pano.CaptureParams;
import com.pi.pano.RecordParams;

/**
 * fileConfig生成适配接口
 */
public interface IFileConfigAdapter {

    /**
     * 初始化 照片config数据
     *
     * @param params 拍照参数
     */
    void initPhotoConfig(CaptureParams params);

    /**
     * 初始化 视频config数据
     *
     * @param params 录像参数
     */
    void initVideoConfig(RecordParams params);

    FileConfig getFileConfig();

}
