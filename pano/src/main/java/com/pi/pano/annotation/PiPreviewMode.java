package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Panoramic preview mode.
 */
@IntDef({
        PiPreviewMode.planet,
        PiPreviewMode.immersion,
        PiPreviewMode.fish_eye,
        PiPreviewMode.plane,
        PiPreviewMode.flat,
        PiPreviewMode.vlog
})
public @interface PiPreviewMode {
    /**
     * 小行星
     */
    int planet = 0;
    /**
     * 沉浸
     */
    int immersion = 1;
    /**
     * 鱼眼
     */
    int fish_eye = 2;
    /**
     * 平铺
     */
    int plane = 3;
    /**
     * 平面模式,用于显示原始鱼眼,默认值，双击切换时不使用
     */
    int flat = 5;

    /**
     * vlog模式
     */
    int vlog = 10;
}
