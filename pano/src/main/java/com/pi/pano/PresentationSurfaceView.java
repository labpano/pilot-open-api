package com.pi.pano;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PresentationSurfaceView extends SurfaceView
{
    public PresentationSurfaceView(Context context, AttributeSet attrs, final PanoSurfaceView relateSurfaceView)
    {
        super(context,attrs);

        getHolder().addCallback(new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
                relateSurfaceView.mPiPano.setSurface(holder.getSurface(),3);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
                relateSurfaceView.mPiPano.setSurface(null,3);
            }
        });
    }
}
