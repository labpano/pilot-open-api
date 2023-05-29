package com.pi.pano.wrap.annotation;

import androidx.annotation.StringDef;

/**
 * 延时摄影倍数
 */
@StringDef({
        PiTimeLapseTimes._10,
        PiTimeLapseTimes._20,
        PiTimeLapseTimes._50,
        PiTimeLapseTimes._100,
        PiTimeLapseTimes._200,
        PiTimeLapseTimes._500,
        PiTimeLapseTimes._1000,
        PiTimeLapseTimes._2000,
        PiTimeLapseTimes._5000,
        PiTimeLapseTimes._10000
})
public @interface PiTimeLapseTimes {
    /**
     * 10x
     */
    String _10 = "10";
    /**
     * 20x
     */
    String _20 = "20";
    /**
     * 50x
     */
    String _50 = "50";
    /**
     * 100x
     */
    String _100 = "100";
    /**
     * 200x
     */
    String _200 = "200";
    /**
     * 500x
     */
    String _500 = "500";
    /**
     * 1000x
     */
    String _1000 = "1000";
    /**
     * 1000x
     */
    String _2000 = "2000";
    /**
     * 1000x
     */
    String _5000 = "5000";
    /**
     * 1000x
     */
    String _10000 = "10000";
}
