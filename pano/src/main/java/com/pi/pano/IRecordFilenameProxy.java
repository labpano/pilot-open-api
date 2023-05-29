package com.pi.pano;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * 录像文件名称（文件夹）获取
 */
public interface IRecordFilenameProxy extends IFilenameProxy {
    /**
     * 获取文件所在的文件夹
     */
    @NonNull
    File getParentPath();
}