package com.pi.pano;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;

/**
 * SurfaceView integrated with MediaPlayer.
 * Can be used to play media file(photo、video)
 */
public class MediaPlayerSurfaceView extends PanoSurfaceView {
    private static final String TAG = MediaPlayerSurfaceView.class.getSimpleName();

    private Surface mGLSurface;
    private MediaPlayer mMediaPlayer;
    private Bitmap mBitmap;
    private String mFilename;

    public MediaPlayerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLensCorrectionMode = 0x3;
    }

    /**
     * Open media file
     *
     * @param filename Media file path, can be jpg or mp4 file
     * @return 0-success; 1-file does not exist; 2-MediaPlayer.setDataSource failed;
     * 3-MediaPlayer.prepare() failed; 4-MediaPlayer.start() failed; 5-Unsupported file type
     */
    public int open(String filename) {
        if (filename == null) {
            return 5;
        }

        mFilename = filename;

        File file = new File(filename);
        if (!file.exists()) {
            Log.e(TAG, "File not exists : " + filename);
            return 1;
        }

        if (filename.endsWith(".mp4")) {
            if (mGLSurface != null) {
                mGLSurface.release();
                mGLSurface = new Surface(mPiPano.mSurfaceTexture[0]);
            }

            mBitmap = null;

            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            }

            mMediaPlayer.setSurface(mGLSurface);

            mMediaPlayer.reset();

            try {
                mMediaPlayer.setDataSource(filename);
            } catch (Exception e) {
                e.printStackTrace();
                return 2;
            }

            try {
                mMediaPlayer.prepare();
                mMediaPlayer.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
                return 3;
            }

            if (mPanoSurfaceViewListener != null) {
                mPanoSurfaceViewListener.onMediaPlayerCreate(mMediaPlayer);
            }
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            new LoadBitmapAsyncTask(this, true).executeOnExecutor(Executors.newCachedThreadPool());
        } else if (filename.endsWith(".sti")) {
            // Un-stitched video
        } else {
            return 5;
        }
        return 0;
    }

    private static class LoadBitmapAsyncTask extends AsyncTask<Void, Integer, Bitmap> {
        WeakReference<MediaPlayerSurfaceView> mMediaPlayerSurfaceView;

        String mFilename;
        Boolean mIsThumbnail;

        LoadBitmapAsyncTask(MediaPlayerSurfaceView view, boolean isThumbnail) {
            mMediaPlayerSurfaceView = new WeakReference<>(view);
            mFilename = mMediaPlayerSurfaceView.get().mFilename;
            mIsThumbnail = isThumbnail;
        }

        @Override
        protected Bitmap doInBackground(Void... v) {
            if (!TextUtils.isEmpty(mFilename)
                    && null != mMediaPlayerSurfaceView.get()
                    && !mFilename.equals(mMediaPlayerSurfaceView.get().mFilename)) {
                return null;
            }

            //先加载缩略图再加载原图
            if (mIsThumbnail) {
                try {
                    ExifInterface exifInterface = new ExifInterface(mFilename);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    String thumbsName = mFilename.substring(mFilename.lastIndexOf("/") + 1);
                    String thumbsPath = "/sdcard/DCIM/Thumbs/" + thumbsName;
                    return BitmapFactory.decodeFile(thumbsPath, options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                return BitmapFactory.decodeFile(mFilename, options);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            MediaPlayerSurfaceView mediaPlayerSurfaceView = mMediaPlayerSurfaceView.get();
            if (mediaPlayerSurfaceView != null && mFilename.equals(mediaPlayerSurfaceView.mFilename)) {
                if (bitmap != null) {
                    mediaPlayerSurfaceView.mBitmap = bitmap;
                    mediaPlayerSurfaceView.drawBitmap();
                }
                if (mIsThumbnail) {
                    new LoadBitmapAsyncTask(mMediaPlayerSurfaceView.get(), false).
                            executeOnExecutor(Executors.newCachedThreadPool());
                }
            }
        }
    }

    private void drawBitmap() {
        if (mPiPano != null && mPiPano.mSurfaceTexture != null && mPiPano.mSurfaceTexture[0] != null) {
            if (mGLSurface != null) {
                mGLSurface.release();
                mGLSurface = new Surface(mPiPano.mSurfaceTexture[0]);
            }

            if (mGLSurface != null && mBitmap != null) {
                mPiPano.mSurfaceTexture[0].setDefaultBufferSize(mBitmap.getWidth(), mBitmap.getHeight());
                Canvas canvas = mGLSurface.lockHardwareCanvas();
                canvas.drawBitmap(mBitmap, 0, 0, null);
                mGLSurface.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void onPiPanoInit() {
        super.onPiPanoInit();

        mGLSurface = new Surface(mPiPano.mSurfaceTexture[0]);
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(mGLSurface);
        } else if (mBitmap != null) {
            drawBitmap();
        }
    }

    @Override
    public void onPiPanoChangeCameraResolution(ChangeResolutionListener listener) {
    }

    @Override
    public void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync) {
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, TakePhotoListener listener) {
    }

    @Override
    public void onPiPanoDestroy() {
        super.onPiPanoDestroy();
        if (mMediaPlayer != null) {
            if (mPanoSurfaceViewListener != null) {
                mPanoSurfaceViewListener.onMediaPlayerRelease();
            }

            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mGLSurface != null) {
            mGLSurface.release();
            mGLSurface = null;
        }
    }

    public void savePicture(final String fileName, final String newFileName, final int width, final int height, final SavePhotoCallback listener) {
        final SavePhotoListener thumbListener = new SavePhotoListener() {
            @Override
            protected void onSavePhotoStart() {
                super.onSavePhotoStart();
            }

            @Override
            protected void onSavePhotoComplete() {
                super.onSavePhotoComplete();
            }

            @Override
            protected void onSavePhotoFailed() {
                super.onSavePhotoFailed();
            }
        };
        thumbListener.mFilename = fileName;
        thumbListener.mNewFilename = newFileName;
        thumbListener.mWidth = 720;
        thumbListener.mHeight = 360;
        thumbListener.mThumbnail = true;
        mPiPano.savePhoto(thumbListener);

        final SavePhotoListener photoListener = new SavePhotoListener() {
            @Override
            protected void onSavePhotoStart() {
                if (listener != null) {
                    listener.onSavePhotoStart();
                }
            }

            @Override
            protected void onSavePhotoComplete() {
                if (listener != null) {
                    listener.onSavePhotoComplete();
                }
            }

            @Override
            protected void onSavePhotoFailed() {
                if (listener != null) {
                    listener.onSavePhotoFailed();
                }
            }
        };

        photoListener.mFilename = fileName;
        photoListener.mNewFilename = newFileName;
        photoListener.mWidth = width;
        photoListener.mHeight = height;
        photoListener.mThumbnail = false;
        mPiPano.savePhoto(photoListener);
    }
}
