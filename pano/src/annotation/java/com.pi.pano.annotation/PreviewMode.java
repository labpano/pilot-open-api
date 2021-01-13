package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * 预览模式
 */
@IntDef({PreviewMode.planet, PreviewMode.immersion, PreviewMode.fish_eye, PreviewMode.plane, PreviewMode.flat})
public @interface PreviewMode {
    /**
     * 小行星
     **/
    int planet = 0;
    /**
     * 沉浸
     **/
    int immersion = 1;
    /**
     * 鱼眼
     **/
    int fish_eye = 2;
    /**
     * 平铺
     **/
    int plane = 3;
    /**
     * 平面模式,用于显示原始鱼眼
     */
    int flat = 5;
}
