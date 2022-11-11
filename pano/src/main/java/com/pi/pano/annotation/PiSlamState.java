package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * slam tracking status.
 */
@IntDef({
        PiSlamState.unInit,
        PiSlamState.initializing,
        PiSlamState.tracking,
        PiSlamState.losing
})
public @interface PiSlamState {
    /**
     * 未初始化
     */
    int unInit = 0;
    /**
     * 正在初始化
     */
    int initializing = 1;
    /**
     * 正在跟踪
     */
    int tracking = 2;
    /**
     * 丢失跟踪
     */
    int losing = 3;
}
