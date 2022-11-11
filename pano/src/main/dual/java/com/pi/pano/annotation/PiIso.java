package com.pi.pano.annotation;

import androidx.annotation.IntDef;

/**
 * Sensitivity
 */
@IntDef({
        PiIso.auto,
        PiIso._100,
        PiIso._200,
        PiIso._400,
        PiIso._600,
        PiIso._640,
        PiIso._800,
        PiIso._1000,
        PiIso._1250,
        PiIso._1600,
        PiIso._3200,
        PiIso._6400
})
public @interface PiIso {
    int auto = 0;
    int _100 = 100;
    int _200 = 200;
    int _400 = 400;
    int _600 = 600;
    int _640 = 640;
    int _800 = 800;
    int _1000 = 1000;
    int _1250 = 1250;
    int _1600 = 1600;
    int _3200 = 3200;
    int _6400 = 6400;
}
