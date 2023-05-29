package com.pi.pano.wrap.config;

import android.text.TextUtils;

import com.pi.pano.CaptureParams;
import com.pi.pano.RecordParams;
import com.pi.pano.ResolutionSize;
import com.pi.pano.annotation.PiFileStitchFlag;

import java.io.File;

/**
 * fileConfig 文件生成类
 */
public class FileConfigAdapter implements IFileConfigAdapter {
    private FileConfig mFileConfig;

    @Override
    public void initPhotoConfig(CaptureParams params) {
        if (mFileConfig == null) {
            if (TextUtils.isEmpty(params.unStitchDirPath)) {
                return;
            }
            mFileConfig = new FileConfig();
            mFileConfig.filePath = parsePhotoPath(params);
            mFileConfig.isPhoto = true;
            mFileConfig.setVersion(params.software);
            mFileConfig.setResolution(params.resolution);
            if (params.isHdr()) {
                mFileConfig.setHdrCount(params.hdrCount);
            }
        }
    }

    @Override
    public void initVideoConfig(RecordParams params) {
        if (mFileConfig == null) {
            if (TextUtils.isEmpty(params.getBasicName())
                    || TextUtils.isEmpty(params.getDirPath())
                    || !"2:1".equals(params.aspectRatio)) {
                return;
            }
            mFileConfig = new FileConfig();
            mFileConfig.setResolution(ResolutionSize.toResolutionSizeString(params.videoWidth, params.videoHeight));
            mFileConfig.setFps(params.fps);
            mFileConfig.setBitrate(params.bitRate);
            mFileConfig.setVersion(params.mVersionName);
            mFileConfig.setTimelapseRatio(params.memomotionRatio);
            if (params.audioEncoderExtList != null) {
                mFileConfig.setSpatialAudio(params.spatialAudio);
            }
        }
    }

    @Override
    public FileConfig getFileConfig() {
        return mFileConfig;
    }

    private String parsePhotoPath(CaptureParams params) {
        String path;
        if (params.unStitchDirPath.endsWith("itp") ||
                params.unStitchDirPath.endsWith("btp") ||
                params.unStitchDirPath.contains("/DCIM/Tours/")) {
            //间隔拍照,连拍,漫游
            path = params.unStitchDirPath;
        } else {
            if (params.unStitchDirPath.endsWith("/.mf")) {
                //隐藏拍摄者
                path = params.unStitchDirPath;
                path = path.replace("/.mf", PiFileStitchFlag.unstitch);
            } else {
                //普通照片
                path = params.unStitchDirPath + File.separator
                        + params.obtainBasicName() + PiFileStitchFlag.unstitch;
            }
            if (params.isHdr()) {
                path += "_hdr";
            }
            path += ".jpg";
        }
        return path;
    }
}
