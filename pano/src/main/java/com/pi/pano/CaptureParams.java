package com.pi.pano;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pi.pano.annotation.PiPhotoFileFormat;
import com.pi.pano.annotation.PiPhotoResolution;
import com.pi.pilot.pano.sdk.BuildConfig;

/**
 * 拍照参数
 */
public class CaptureParams {
    /**
     * 是否拍缩略图
     */
    public boolean takeThumb;
    /**
     * 是否保存.config文件
     */
    public boolean saveConfig = true;
    /**
     * (漫游拍照)是否记录定位点信息
     */
    public boolean makeSlamPhotoPoint = false;

    /**
     * hdr拍照采集的照片数量，即照片堆叠张数
     */
    public int hdrCount;

    public IFilenameProxy fileNameGenerator;

    /**
     * 基础的文件名
     */
    private String basicName;

    /**
     * 获取基础的文件名
     */
    public String obtainBasicName() {
        if (null == basicName) {
            basicName = fileNameGenerator.getBasicName();
        }
        return basicName;
    }

    /**
     * 拼接照片保存文件夹路径
     */
    public String stitchDirPath = "/sdcard/DCIM/Photos/Stitched/";
    /**
     * 未拼接照片保存的文件夹路径
     */
    public String unStitchDirPath = "/sdcard/DCIM/Photos/Unstitched/";
    /**
     * 分辨率
     */
    @PiPhotoResolution
    public String resolution;

    /**
     * 文件格式
     */
    @PiPhotoFileFormat
    public String fileFormat = PiPhotoFileFormat.jpg;

    /**
     * 作者
     */
    public String artist;
    /**
     * 版本
     */
    public String software;

    /**
     * 经度
     */
    public double longitude;
    /**
     * 维度
     */
    public double latitude;
    /**
     * 海拔
     */
    public int altitude;
    /**
     * 朝向
     */
    public double heading;

    /**
     * jpeg图像质量
     */
    public int jpegQuality = 100;

    /**
     * 是否保留hdr源文件
     */
    public boolean saveHdrSourceFile;

    private CaptureParams() {
    }

    /**
     * 设置位置信息
     *
     * @param longitude   经度
     * @param latitude    维度
     * @param altitude    海拔
     * @param orientation 朝向
     */
    public void setLocation(double longitude, double latitude, int altitude, float orientation) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.heading = orientation;
    }

    /**
     * 是否是hdr拍照
     *
     * @return true:hdr拍照
     */
    public boolean isHdr() {
        return hdrCount > 0;
    }

    /**
     * hdr是否保存原文件（中间文件）
     */
    public boolean isSaveHdrSourceFile() {
        return hdrCount > 0 && saveHdrSourceFile;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "resolution='" + resolution + '\'' +
                '}';
    }

    public static class Factory {

        @NonNull
        public static CaptureParams createParams(
                int hdrCount,
                String basicName,
                @PiPhotoResolution String resolution) {
            CaptureParams params = new CaptureParams();
            params.hdrCount = hdrCount;
            params.saveHdrSourceFile = hdrCount > 0 && requestSaveHdrSourceFile();
            params.basicName = basicName;
            params.resolution = resolution;
            return params;
        }

        /**
         * 缩略图参数
         *
         * @param basicName 文件名
         */
        public static CaptureParams createParamsForThumb(String basicName) {
            CaptureParams params = new CaptureParams();
            params.resolution = ResolutionSize.toResolutionSizeString(720, 360);
            params.basicName = basicName;
            params.unStitchDirPath = "/sdcard/DCIM/Thumbs/";
            params.stitchDirPath = null;
            params.jpegQuality = 30;
            return params;
        }

        @NonNull
        public static CaptureParams createParams(
                int hdrCount,
                IFilenameProxy fileNameGenerator,
                @Nullable String stitchDirPath, @Nullable String unStitchDirPath,
                @PiPhotoFileFormat String fileFormat,
                @PiPhotoResolution String resolution,
                String artist, String software) {
            CaptureParams params = new CaptureParams();
            params.makeSlamPhotoPoint = false;
            params.hdrCount = hdrCount;
            params.saveHdrSourceFile = hdrCount > 0 && requestSaveHdrSourceFile();
            params.takeThumb = true;
            //
            params.fileFormat = fileFormat;
            params.fileNameGenerator = fileNameGenerator;
            params.unStitchDirPath = unStitchDirPath;
            params.stitchDirPath = stitchDirPath;
            params.resolution = resolution;
            params.artist = artist;
            params.software = software;
            return params;
        }

        @NonNull
        public static CaptureParams createParams(
                boolean makeSlamPhotoPoint, int hdrCount, boolean takeThumb,
                IFilenameProxy fileNameGenerator,
                @Nullable String stitchDirPath, @Nullable String unStitchDirPath,
                @PiPhotoFileFormat String fileFormat,
                @PiPhotoResolution String resolution,
                String artist, String software) {
            CaptureParams params = new CaptureParams();
            params.takeThumb = takeThumb;
            params.makeSlamPhotoPoint = makeSlamPhotoPoint;
            params.hdrCount = hdrCount;
            params.saveHdrSourceFile = hdrCount > 0 && requestSaveHdrSourceFile();
            //
            params.fileNameGenerator = fileNameGenerator;
            params.unStitchDirPath = unStitchDirPath;
            params.stitchDirPath = stitchDirPath;
            params.fileFormat = fileFormat;
            params.resolution = resolution;
            params.artist = artist;
            params.software = software;
            return params;
        }
    }

    private static boolean requestSaveHdrSourceFile() {
        if (BuildConfig.DEBUG) {
            return SystemPropertiesProxy.getInt(
                    "persist.dev.photo.hdr.save.source", 0) == 1;
        }
        // 6.0 版本不保留中间文件
        return false;
    }
}

