package com.pi.pano;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

public class Mp4ThumbMaker extends HandlerThread implements SurfaceTexture.OnFrameAvailableListener, Handler.Callback {
    private static final String TAG = "Mp4ThumbMaker";

    private final String mThumbFilename;
    private final Mp4ThumbMakerListener mMp4ThumbMakerListener;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private final long mStartTime;
    private final Handler mHandler;

    private static final int MSG_INIT = 0;
    private static final int MSG_UPDATE = 1;

    static {
        System.loadLibrary(Config.PIPANO_SO_NAME);
    }

    private native int create(int cameraCount);
    private native void init(String mp4Filename, Surface surface);
    private native boolean draw(String thumbFilename);
    private native void release();

    public interface Mp4ThumbMakerListener {
        void onSaveThumb(String filename);
    }

    /**
     * 制作mp4视频的缩略图,用于进度条显示
     *
     * @param mp4Filename   mp4文件名
     * @param thumbFilename 要保存的缩略图的jpeg文件名
     * @param listener      回调
     */
    public Mp4ThumbMaker(String mp4Filename, String thumbFilename, Mp4ThumbMakerListener listener) {
        super("Mp4ThumbMaker");
        mStartTime = System.currentTimeMillis();
        mThumbFilename = thumbFilename;
        mMp4ThumbMakerListener = listener;
        start();
        mHandler = new Handler(getLooper(), this);
        Message.obtain(mHandler, MSG_INIT, mp4Filename).sendToTarget();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSurfaceTexture.updateTexImage();
        Message.obtain(mHandler, MSG_UPDATE).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_INIT:
                mSurfaceTexture = new SurfaceTexture(create(PilotSDK.CAMERA_COUNT));
                mSurfaceTexture.setOnFrameAvailableListener(this, mHandler);
                mSurface = new Surface(mSurfaceTexture);
                init((String) msg.obj, mSurface);
                break;
            case MSG_UPDATE:
                if (draw(mThumbFilename)) {
                    release();
                    mSurfaceTexture.release();
                    mSurface.release();
                    mMp4ThumbMakerListener.onSaveThumb(mThumbFilename);
                    Log.i(TAG, "Mp4ThumbMaker cost time: " + (System.currentTimeMillis() - mStartTime));
                    getLooper().quit();
                }
                break;
        }
        return false;
    }
}
