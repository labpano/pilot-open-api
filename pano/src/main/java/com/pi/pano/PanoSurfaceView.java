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
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.pi.pano.annotation.PiPreviewMode;

abstract class PanoSurfaceView extends SurfaceView implements PiPano.PiPanoListener {
    private static final String TAG = "PanoSurfaceView";

    PiPano mPiPano;
    GestureDetector mGestureDetector;
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

    public PanoSurfaceView(Context context) {
        this(context, null);
    }

    public PanoSurfaceView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            Input.reset();
        } catch (Throwable ignore) {
        }
        mContext = context;

        if (!isInEditMode()) {
            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (mPanoSurfaceViewListener != null) {
                        mPanoSurfaceViewListener.onSingleTapConfirmed();
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
                        mPiPano = new OpenGLThread(PanoSurfaceView.this, context);
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

    /**
     * Whether to enable/disable touch events
     *
     * @param b true: turn on; false:turn off
     */
    public void setEnableTouchEvent(boolean b) {
        mEnableTouchEvent = b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mEnableTouchEvent) {
            float[] pointPoses = new float[event.getPointerCount() * 2];
            for (int i = 0; i < event.getPointerCount(); ++i) {
                pointPoses[i * 2] = event.getX(i);
                pointPoses[i * 2 + 1] = event.getY(i);
            }
            Input.onTouchEvent(event.getAction() & MotionEvent.ACTION_MASK, event.getPointerCount(), event.getEventTime() * 1000000, pointPoses);

            mGestureDetector.onTouchEvent(event);
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
            mPanoSurfaceViewListener.onPanoSurfaceViewRelease();
        }
    }

    /**
     * Set preview mode.
     *
     * @param mode           {@link PiPreviewMode}
     * @param rotateDegree   0~360 Initial rotation angle, horizontal rotation in plane mode, and rotation around Y axis in spherical mode.
     * @param playAnimation  Whether to play the switching animation when switching the display mode.
     * @param fov            0~180 Initial longitudinal fov.
     * @param cameraDistance 0~400 Distance from camera to ball center.
     */
    public void setPreviewMode(@PiPreviewMode int mode, float rotateDegree, boolean playAnimation, float fov, float cameraDistance) {
        Input.setPreviewMode(mode, rotateDegree, playAnimation, fov, cameraDistance);
        mPanoSurfaceViewListener.onPanoModeChange(Input.getPreviewMode());
    }

    @Override
    public void onPiPanoEncodeFrame(int count) {
        if (mPanoSurfaceViewListener != null) {
            mPanoSurfaceViewListener.onEncodeFrame(count);
        }
    }

    /**
     * Set stitch distance
     *
     * @param d -100~100 0 represents the distance during calibration, about 2m; 100 represents infinity; - 100 is about 0.5m.
     */
    void setStitchingDistance(float d) {
        if (mPiPano == null) {
            Log.e(TAG, "setStitchingDistance mPiPano is null");
            return;
        }
        mPiPano.setStitchingDistance(d);
    }

    /**
     * Whether gyroscope is used,
     * Gyroscope for Steady.
     *
     * @param open true: use gyroscope
     */
    public void useGyroscope(boolean open) {
        if (mPiPano == null) {
            Log.e(TAG, "useGyroscope mPiPano is null");
            return;
        }
        mPiPano.useGyroscope(open, null);
    }

    /**
     * Turn editing on or off
     *
     * @param enable true: turn on
     */
    public void setEdition(boolean enable) {
        if (mPiPano == null) {
            Log.e(TAG, "setEdition mPiPano is null");
            return;
        }
        mPiPano.setEdition(enable);
    }

    /**
     * Highlights
     *
     * @param percentage 0 ~ 200
     */
    public void setHighlights(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setHighlightShadows mPiPano is null");
            return;
        }
        mPiPano.setHighlights(-range(percentage, -3.5f, 1.5f));
    }

    /**
     * Shadows
     *
     * @param percentage 0 ~ 200
     */
    public void setShadows(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setShadows mPiPano is null");
            return;
        }
        mPiPano.setShadows(range(percentage, -1.5f, 3.5f));
    }

    /**
     * Contras
     *
     * @param percentage 0 ~ 200
     */
    public void setContrast(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setContrast mPiPano is null");
            return;
        }
        mPiPano.setContrast(range(percentage, 0.0f, 2.0f));
    }

    /**
     * Brightnes
     *
     * @param percentage 0 ~ 200
     */
    public void setBrightness(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setBrightness mPiPano is null");
            return;
        }
        mPiPano.setBrightness(range(percentage, -1.0f, 1.0f));
    }

    /**
     * Saturation
     *
     * @param percentage 0 ~ 200
     */
    public void setSaturation(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setSaturation mPiPano is null");
            return;
        }
        mPiPano.setSaturation(range(percentage, 0.0f, 2.0f));
    }

    /**
     * Gamma
     *
     * @param percentage 0 ~ 200
     */
    public void setGamma(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setGamma mPiPano is null");
            return;
        }
        mPiPano.setGamma(range(percentage, 0.0f, 2.0f));
    }

    /**
     * Temperature
     *
     * @param percentage 0 ~ 200
     */
    public void setTemperature(final int percentage) {
        if (mPiPano == null) {
            Log.e(TAG, "setGamma mPiPano is null");
            return;
        }
        float temperature = range(percentage, 2000.0f, 8000.0f);
        mPiPano.setTemperature(temperature < 5000 ? (float) (0.0004 * (temperature - 5000.0)) : (float) (0.00006 * (temperature - 5000.0)));
    }

    /**
     * reset
     */
    public void reset() {
        if (mPiPano == null) {
            Log.e(TAG, "reset mPiPano is null");
            return;
        }
        mPiPano.reset();
    }

    /**
     * Range conversion
     *
     * @param percentage 0 - 200
     * @param start      start
     * @param end        end
     * @return range value
     */
    protected float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 200.0f + start;
    }
}
