package com.pi.pano;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pi.pano.annotation.PiVideoEncode;
import com.pi.pano.ext.IAudioEncoderExt;
import com.pi.pano.ext.IAudioRecordExt;

import java.util.List;

/**
 * 录像参数
 */
public class RecordParams {
    /**
     * 文件名称
     */
    @NonNull
    public final IRecordFilenameProxy filenameProxy;

    public final int videoWidth;
    public final int videoHeight;
    public final int channelCount;
    public int fps;
    public int bitRate;

    @PiVideoEncode
    public String encode = PiVideoEncode.h_264;
    /**
     * 画面比例
     */
    public String aspectRatio = "2:1";
    /**
     * 版本号（用于写入视频信息中）
     */
    public String mVersionName;
    /**
     * 作者 (用于写入视频信息中）
     */
    public String mArtist;

    /**
     * 延时摄影倍率,如果当前是7fps,如果ratio为70,那么会降低帧为7/70=0.1fps,
     * 也就是10s录一帧,只对实时拼接录像有效
     */
    public int memomotionRatio = 0;
    /**
     * 是否是用于google map
     */
    public boolean useForGoogleMap = false;
    public float streetBitrateMultiple = 1;
    public boolean spatialAudio;
    /**
     * 音频编码扩展处理器
     */
    @Nullable
    public List<IAudioEncoderExt> audioEncoderExtList;
    /**
     * 录音接口
     */
    @Nullable
    public IAudioRecordExt audioRecordExt;
    /**
     * 是否保存.config文件
     */
    public boolean saveConfig = true;
    /**
     * 录制的视频文件存放目录
     */
    @Nullable
    private String dirPathInner;
    /**
     * 录制的视频文件名称
     */
    private String basicNameInner;

    public RecordParams(@NonNull IRecordFilenameProxy filenameProxy, int videoWidth, int videoHeight,
                        int fps, int bitRate, int channelCount) {
        this.filenameProxy = filenameProxy;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.fps = fps;
        this.bitRate = bitRate;
        this.channelCount = channelCount;
    }

    @Nullable
    public String getDirPath() {
        if (null == dirPathInner) {
            dirPathInner = filenameProxy.getParentPath().getAbsolutePath();
        }
        return dirPathInner;
    }

    public String getBasicName() {
        if (null == basicNameInner) {
            basicNameInner = filenameProxy.getBasicName();
        }
        return basicNameInner;
    }

    /**
     * 重用本参数
     */
    public void reuse() {
        basicNameInner = null;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "videoWidth=" + videoWidth +
                ", videoHeight=" + videoHeight +
                ", fps=" + fps +
                '}';
    }

    /**
     * TOTO 待下沉app中代码
     */
    public static class Factory {
    }
}
