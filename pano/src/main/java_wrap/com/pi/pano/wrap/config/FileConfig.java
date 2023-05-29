package com.pi.pano.wrap.config;

public class FileConfig {

    /**
     * 配件类型 1：无配件 ，2 :保护镜
     */
    private int fittings = 1;
    private int fps;
    private long bitrate;
    /**
     * 分辨率 width*height
     */
    private String resolution;
    /**
     * 延时倍数
     */
    private int timelapseRatio;
    /**
     * 视场角度
     */
    private int fieldOfView = 90;
    private int hdrCount;
    /**
     * 全景音
     */
    private boolean spatialAudio;
    private String version;

    public String filePath;
    public boolean isPhoto;

    public int getFittings() {
        return fittings;
    }

    public void setFittings(int fittings) {
        this.fittings = fittings;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public long getBitrate() {
        return bitrate;
    }

    public void setBitrate(long bitrate) {
        this.bitrate = bitrate;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public int getTimelapseRatio() {
        return timelapseRatio;
    }

    public void setTimelapseRatio(int timelapseRatio) {
        this.timelapseRatio = timelapseRatio;
    }

    public int getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(int fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    public int getHdrCount() {
        return hdrCount;
    }

    public void setHdrCount(int hdrCount) {
        this.hdrCount = hdrCount;
    }

    public boolean isSpatialAudio() {
        return spatialAudio;
    }

    public void setSpatialAudio(boolean spatialAudio) {
        this.spatialAudio = spatialAudio;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static class Key {
        public static final String KEY_FITTINGS = "fittings";
        public static final String KEY_FPS = "fps";
        public static final String KEY_BITRATE = "bitrate";
        public static final String KEY_RESOLUTION = "resolution";
        public static final String KEY_SPATIAL_AUDIO = "spatialAudio";
        public static final String KEY_TIME_LAPSE_RATIO = "timelapseRatio";
        public static final String KEY_FIELD_OF_VIEW = "fieldOfView";
        public static final String KEY_HDR_COUNT = "hdrCount";
        public static final String KEY_VERSION = "version";
    }
}
