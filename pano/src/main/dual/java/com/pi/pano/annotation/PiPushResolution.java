package com.pi.pano.annotation;

/**
 * Push resolution.
 */
public @interface PiPushResolution {
    /**
     * 6K-未拼接
     */
    String _6K = PiResolution._5_7K;
    /**
     * 4K-包含帧率优先
     */
    String _4K = PiResolution._4K;
    /**
     * 4K-画质优先，降低帧率
     */
    String _4K_QUALITY = PiResolution._4K + "#2";
    /**
     * 高清
     */
    String HIGH = PiResolution._2_5K;
    /**
     * 1080，其码率和 HIGH 一致
     */
    String _1080P = PiResolution._1080P;
    /**
     * 标清
     */
    String STANDARD = PiResolution.STANDARD;
    /**
     * 720,其码率和 STANDARD 一致
     */
    String _720P = PiResolution._720P;
    /**
     * 流畅
     */
    String SMOOTH = PiResolution.SMOOTH;
}
