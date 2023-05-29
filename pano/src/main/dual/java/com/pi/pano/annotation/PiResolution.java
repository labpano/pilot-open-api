package com.pi.pano.annotation;

/**
 * Resolution.
 */
public @interface PiResolution {
    /**
     * 5.7K
     */
    String _5_7K = "5760*2880";
    /**
     * 4K
     */
    String _4K = "3840*1920";
    /**
     * 高清
     */
    String _2_5K = "2560*1280";
    /**
     * 1080P
     */
    String _1080P = "2160*1080";
    /**
     * 标清
     */
    String STANDARD = "1920*960";
    /**
     * 720P
     */
    String _720P = "1440*720";
    /**
     * 流畅
     */
    String SMOOTH = "1280*640";
}
