package com.pi.pano.wrap;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * 拍照监听
 */
public interface IPhotoListener {
    /**
     * 拍照开始
     */
    void onTakeStart();

    /**
     * 拍照（采集）开始
     *
     * @param index 采集索引，拍照可能采集多次
     */
    void onTakeStart(int index);

    /**
     * 拍照成功
     *
     * @param change         预览是否发生变化
     * @param simpleFileName 外层指定文件名
     * @param stitchFile     拼接文件
     * @param unStitchFile   未拼接文件
     */
    void onTakeSuccess(boolean change, String simpleFileName, @Nullable File stitchFile, @Nullable File unStitchFile);

    /**
     * 拍照错误
     *
     * @param change    预览是否发生变化
     * @param errorCode 拍摄错误码
     */
    void onTakeError(boolean change, @Nullable String errorCode);
}
