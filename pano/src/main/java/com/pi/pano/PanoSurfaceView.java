package com.pi.pano;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.pi.pano.annotation.PiPreviewMode;

abstract class PanoSurfaceView extends SurfaceView implements PiPano.PiPanoListener {
    private static final String TAG = "PanoSurfaceView";

    PiPano mPiPano;
    GestureDetector mGestureDetector;
    ScaleGestureDetector mScaleGestureDetector;
    PanoSurfaceViewListener mPanoSurfaceViewListener;
    boolean mEnableTouchEvent = true;
    protected int mLensCorrectionMode;
    boolean mDelayOnCreateEvent;
    Context mContext;

    private static final Object sLock = new Object();

    /**
     * surface创建
     */
    private boolean mSurfaceCreated = false;

    public PanoSurfaceView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        if (!isInEditMode()) {
            mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    if (mPiPano != null)
                        mPiPano.muOnScale(detector.getScaleFactor());
                    return false;
                }
            });

            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (mPanoSurfaceViewListener != null) {
                        mPanoSurfaceViewListener.onSingleTapConfirmed();
                    }
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    Log.d(TAG, "onScroll  is enableTouchEvent =:" + mEnableTouchEvent);
                    if (!mEnableTouchEvent) {
                        return true;
                    }
                    if (mPiPano != null)
                        mPiPano.muOnScroll(distanceX, distanceY);
                    return false;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (mPiPano != null)
                        mPiPano.muOnFling(velocityX, velocityY, mPiPano.mSetPreviewFps);
                    return false;
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    if (mPiPano != null)
                        mPiPano.muOnDown();
                    return false;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (mPiPano != null) {
                        mPiPano.muOnDoubleTap();
                        mPanoSurfaceViewListener.onPanoModeChange(mPiPano.muGetPreviewMode());
                    }
                    return true;
                }
            });

            getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if (mSurfaceCreated) {
                        synchronized (sLock) {
                            try {
                                sLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!mSurfaceCreated) {
                        mSurfaceCreated = true;
                        mPiPano = new PiPano(PanoSurfaceView.this, context);
                        mPiPano.setSurface(holder.getSurface(), 0);
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    if (mPiPano != null) {
                        mPiPano.release();
                    }
                }
            });
        }
    }

    @Override
    public void onPiPanoInit() {
        Log.i(TAG, "surface created");
        DisplayManager displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (presentationDisplays.length > 0) {
            Presentation presentation = new Presentation(mContext, presentationDisplays[0]) {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(new PresentationSurfaceView(mContext,
                            null, PanoSurfaceView.this));
                }
            };
            presentation.show();
        }

        if (mPanoSurfaceViewListener != null) {
            mPanoSurfaceViewListener.onPanoSurfaceViewCreate();
        } else {
            mDelayOnCreateEvent = true;
        }
    }

    public void setOnPanoModeChangeListener(PanoSurfaceViewListener panoSurfaceViewListener) {
        mPanoSurfaceViewListener = panoSurfaceViewListener;
        if (mDelayOnCreateEvent) {
            mDelayOnCreateEvent = false;
            mPanoSurfaceViewListener.onPanoSurfaceViewCreate();
        }
    }

    public void setEnableTouchEvent(boolean b) {
        mEnableTouchEvent = b;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (mEnableTouchEvent) {
            int pointerCount = event.getPointerCount();
            if (pointerCount == 1) {
                mGestureDetector.onTouchEvent(event);
            } else if (pointerCount == 2) {
                mScaleGestureDetector.onTouchEvent(event);
            }
            Log.d(TAG, "onTouchEvent is enableTouchEvent =:" + mEnableTouchEvent);
        }
        return true;
    }

    @Override
    public void onPiPanoDestroy() {
        Log.i(TAG, "surface destroy");

        if (mSurfaceCreated) {
            synchronized (sLock) {
                sLock.notifyAll();
                mSurfaceCreated = false;
                mPiPano = null;
            }
        }

        if (mPanoSurfaceViewListener != null) {
            mPanoSurfaceViewListener.onMediaPlayerRelease();
        }
    }

    /**
     * Set preview mode.
     *
     * @param mode         {@link PiPreviewMode}
     * @param rotateDegree 0~360 Initial rotation angle, horizontal rotation in plane mode,
     *                    rotation around Y axis in spherical mode.
     * @param playAnimation Play switching animation.
     */
    public void setPreviewMode(@PiPreviewMode int mode, float rotateDegree, boolean playAnimation) {
        if (mPiPano != null) {
            mPiPano.muSetPreviewMode(mode, rotateDegree, playAnimation);
            mPanoSurfaceViewListener.onPanoModeChange(mPiPano.muGetPreviewMode());
        }
    }

    @Override
    public void onPiPanoEncodeFrame(int count) {
        if (mPanoSurfaceViewListener != null) {
            mPanoSurfaceViewListener.onEncodeFrame(count);
        }
    }

    void setStitchingDistance(float d) {
        if (mPiPano != null) {
            mPiPano.setStitchingDistance(d);
        } else {
            Log.e(TAG, "setStitchingDistance mPiPano is null");
        }
    }

    public void useGyroscope(boolean open) {
        if (mPiPano != null) {
            mPiPano.useGyroscope(open, null);
        }
    }
}
