package com.pi.pano;

import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * 视频拼接参数
 */
public class StitchVideoParams {
    /**
     * 要拼接的源文件夹
     */
    @NonNull
    public final File srcDir;

    /**
     * 拼接后保存的文件夹
     */
    public String targetDir;
    /**
     * 拼接后视频的宽
     */
    public int width;
    /**
     * 拼接后视频的高
     */
    public int height;
    /**
     * 拼接后视频的帧率
     */
    public int fps;
    /**
     * 拼接后视频码率
     */
    public int bitrate;
    /**
     * 编码方式 {@link android.media.MediaFormat#MIMETYPE_VIDEO_AVC },
     * {@link android.media.MediaFormat#MIMETYPE_VIDEO_HEVC }
     */
    public String mime = MediaFormat.MIMETYPE_VIDEO_AVC;
    /**
     * 是否使用光流拼接
     */
    public boolean useFlow;
    /**
     * 是否粘贴保护镜
     */
    public boolean lensProtected;
    /**
     * 是否是全景音视频
     */
    public boolean spatialAudio;

    public float progress;

    private StitchVideoParams(@NonNull File srcDir) {
        this.srcDir = srcDir;
        this.targetDir = srcDir.getParent();
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "srcDir=" + srcDir +
                ", targetDir='" + targetDir + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", fps=" + fps +
                ", bitrate=" + bitrate +
                ", mime='" + mime + '\'' +
                ", useFlow=" + useFlow +
                ", lensProtected=" + lensProtected +
                ", spatialAudio=" + spatialAudio +
                ", progress=" + progress +
                '}';
    }

    public static class Factory {

        public static StitchVideoParams createParams(@NonNull File file, int width, int height, int fps, int bitrate) {
            StitchVideoParams params = new StitchVideoParams(file);
            params.width = width;
            params.height = height;
            params.fps = fps;
            params.bitrate = bitrate;
            return params;
        }

        public static StitchVideoParams createParams(@NonNull File file, int width, int height, int fps, int bitrate, String mime, boolean useFlow) {
            StitchVideoParams params = new StitchVideoParams(file);
            params.width = width;
            params.height = height;
            params.fps = fps;
            params.bitrate = bitrate;
            params.mime = mime;
            params.useFlow = useFlow;
            return params;
        }
    }
}
