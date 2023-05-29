package com.pi.pano;

import androidx.annotation.NonNull;

/**
 * 文件名获取代理，一般会在更接近画面采集时刻获取文件名。
 * 如：根据时间格式生成文件名称，名称中的时间更接近采集时刻。
 */
public interface IFilenameProxy {
    /**
     * 获取基础名称。（无扩展名）
     */
    @NonNull
    String getBasicName();
}