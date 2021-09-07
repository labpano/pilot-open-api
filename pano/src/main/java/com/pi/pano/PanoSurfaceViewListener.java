package com.pi.pano;

import android.media.MediaPlayer;

import com.pi.pano.annotation.PiPreviewMode;

/**
 * PanoSurfaceViewListener control event callback
 */
public interface PanoSurfaceViewListener {
    void onPanoSurfaceViewCreate();

    /**
     * When MediaPlayerSurface is created, the callback when MediaPlayer is created internally.
     *
     * @param mediaPlayer 内部创建的MediaPlayer
     */
    void onMediaPlayerCreate(MediaPlayer mediaPlayer);

    /**
     * When MediaPlayerSurface is destroyed, the callback when its internal MediaPlayer is destroyed.
     */
    void onMediaPlayerRelease();

    /**
     * Preview mode change event.
     *
     * @param mode {@link PiPreviewMode}
     */
    void onPanoModeChange(@PiPreviewMode int mode);

    /**
     * when clicked.
     */
    void onSingleTapConfirmed();

    void onEncodeFrame(int count);
}
