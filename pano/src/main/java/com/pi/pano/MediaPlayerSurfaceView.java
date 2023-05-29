package com.pi.pano;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * SurfaceView for playing photos and videos.
 */
public class MediaPlayerSurfaceView extends PanoSurfaceView {
    private final static String TAG = MediaPlayerSurfaceView.class.getSimpleName();

    private static final int PREVIEW_SIZE_MAX_WIDTH = PilotSDK.CAMERA_COUNT == 4 ? 6000 : 4096;
    private static final int PREVIEW_SIZE_MAX_HEIGHT = PilotSDK.CAMERA_COUNT == 4 ? 3000 : 2048;

    private volatile Surface mGLSurface;

    public MediaPlayerSurfaceView(Context context) {
        this(context, null);
    }

    public MediaPlayerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLensCorrectionMode = 0x03;
    }

    public void setLensCorrectionMode(int mode) {
        mLensCorrectionMode = mode;
    }

    private boolean mUseGyroscopeForPlayer;

    /**
     * 播放是否使用陀螺仪
     */
    public void useGyroscopeForPlayer(boolean open) {
        mUseGyroscopeForPlayer = open;
        if (mPiPano != null) {
            mPiPano.useGyroscopeForPlayer(mUseGyroscopeForPlayer);
        }
    }

    @Override
    public void onPiPanoInit() {
        super.onPiPanoInit();
        Log.d(TAG, "onPiPanoInit");
        if (mPiPano != null) {
            mPiPano.useGyroscopeForPlayer(mUseGyroscopeForPlayer);
        }
    }

    @Override
    public void onPiPanoChangeCameraResolution(@NonNull ChangeResolutionListener listener) {
    }

    @Override
    public void onPiPanoEncoderSurfaceUpdate(long timestamp, boolean isFrameSync) {
        if (mPiPano != null) {
            if (mLensCorrectionMode != 0x03) {
                mPiPano.drawLensCorrectionFrame(mLensCorrectionMode, true, timestamp, false);
            }
        }
    }

    @Override
    public void onPiPanoCaptureFrame(int hdrIndex, @NonNull TakePhotoListener listener) {
    }

    @Override
    public void onPiPanoDestroy() {
        super.onPiPanoDestroy();
        Log.d(TAG, "onPiPanoDestroy");
        if (mPlay != null) {
            mPlay.release();
            mPlay = null;
        } else {
            releaseGlSurface();
        }
    }

    private synchronized Surface obtainGlSurface() {
        if (null != mGLSurface) {
            Log.e(TAG, "obtainGlSurface, last surface is not release!");
            throw new RuntimeException("last surface is not release!");
        }
        Log.d(TAG, "obtainGlSurface");
        mGLSurface = new Surface(mPiPano.mSurfaceTexture[0]);
        return mGLSurface;
    }

    private synchronized void releaseGlSurface() {
        if (null != mGLSurface) {
            Log.d(TAG, "releaseGlSurface");
            mGLSurface.release();
            mGLSurface = null;
        }
    }

    private synchronized void drawBitmap(Bitmap bitmap) {
        releaseGlSurface();
        if (mPiPano != null && mPiPano.mSurfaceTexture != null && mPiPano.mSurfaceTexture[0] != null) {
            obtainGlSurface();
            if (mGLSurface != null && bitmap != null) {
                Log.d(TAG, "drawBitmap...");
                mPiPano.mSurfaceTexture[0].setDefaultBufferSize(bitmap.getWidth(), bitmap.getHeight());
                Canvas canvas = mGLSurface.lockHardwareCanvas();
                canvas.drawBitmap(bitmap, 0, 0, null);
                mGLSurface.unlockCanvasAndPost(canvas);
                bitmap.recycle();
                Log.d(TAG, "drawBitmap,ok");
            }
        }
    }

    private void waitPanoInit(long timeout) {
        long start = System.currentTimeMillis();
        while (true) {
            if (null == mPiPano || !mPiPano.mHasInit) {
                if ((System.currentTimeMillis() - start) > timeout) {
                    break;
                }
                SystemClock.sleep(500);
                continue;
            }
            break;
        }
    }

    private IPlayControl mPlay;

    public interface IPlayControl {
        void play();

        void release();
    }

    public interface ImagePlayListener {
        void onPlayFailed(int ret);

        void onPlayCompleted(File file);
    }

    /**
     * play image file.
     *
     * @param file       file
     * @param isStitched stitch state
     * @param listener   listener
     * @return null: can't play
     */
    public IPlayControl playImage(File file, boolean isStitched, @Nullable ImagePlayListener listener) {
        Log.d(TAG, "playImage," + file);
        if (null != mPlay) {
            mPlay.release();
            mPlay = null;
        }
        if (!file.isFile()) {
            return null;
        }
        mPlay = new ImageLoader(file.getAbsoluteFile(), isStitched, listener);
        mPlay.play();
        return mPlay;
    }

    private class ImageLoader implements Runnable, IPlayControl {
        private final String TAG = MediaPlayerSurfaceView.TAG + "$ImageLoader";
        private final File file;
        private final boolean isStitched;
        private final ImagePlayListener listener;
        private final Handler mHandler;
        private final HandlerThread mThread;

        private boolean isQuit = false;

        private ImageLoader(File file, boolean isStitched, @Nullable ImagePlayListener listener) {
            this.file = file;
            this.isStitched = isStitched;
            this.listener = listener;
            mThread = new HandlerThread("play:" + file.getName());
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        @Override
        public void run() {
            waitPanoInit(2000);
            if (isQuit) {
                return;
            }
            if (null == mPiPano || !mPiPano.mHasInit) {
                if (null != listener) {
                    listener.onPlayFailed(1);
                }
                return;
            }
            if (isStitched) {
                mPiPano.setParamReCaliEnable(0, true);
            }
            if (PilotSDK.CAMERA_COUNT == 1) {
                if (isStitched) {
                    setLensCorrectionMode(0x2);
                } else {
                    setLensCorrectionMode(0x1111);
                }
            } else {
                setLensCorrectionMode(0x3);
            }

            mPiPano.useGyroscope(!isStitched, file.getAbsolutePath());

            // 预读照片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            final int bitmapWidth = options.outWidth;
            Log.d(TAG, "load,bitmapWidth:" + bitmapWidth);
            if (bitmapWidth <= 720) {
                requestDrawBitmap(decodeFile(1));
            } else {
                // first load
                int sampleSize = bitmapWidth / 720;
                Log.d(TAG, "load,1.sampleSize:" + sampleSize);
                requestDrawBitmap(decodeFile(sampleSize));
                // final load
                if (bitmapWidth > PREVIEW_SIZE_MAX_WIDTH) {
                    sampleSize = (int) Math.ceil(bitmapWidth * 1f / PREVIEW_SIZE_MAX_WIDTH);
                    Log.d(TAG, "load,2.sampleSize:" + sampleSize);
                    requestDrawBitmap(decodeFile(sampleSize));
                }
            }
            if (!isQuit && null != listener) {
                listener.onPlayCompleted(file);
            }
        }

        private Bitmap decodeFile(int inSampleSize) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }

        private void requestDrawBitmap(Bitmap bitmap) {
            if (isQuit) {
                return;
            }
            drawBitmap(bitmap);
        }

        @Override
        public void play() {
            if (!isQuit) {
                Log.d(TAG, "play");
                mHandler.removeCallbacks(this);
                mHandler.post(this);
            }
        }

        @Override
        public void release() {
            if (!isQuit) {
                Log.d(TAG, "release...");
                isQuit = true;
                mHandler.removeCallbacks(this);
                mHandler.getLooper().quit();
                releaseGlSurface();
                resetParamReCaliEnable();
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "release,end");
            }
        }
    }

    /**
     * video play state
     */
    @IntDef({
            VideoPlayState.release,
            VideoPlayState.prepared,
            VideoPlayState.started,
            VideoPlayState.paused,
            VideoPlayState.completed
    })
    public @interface VideoPlayState {
        /**
         * uninitialized or release
         */
        int release = -1;
        /**
         * prepared
         */
        int prepared = 0;
        /**
         * started
         */
        int started = 1;
        /**
         * paused
         */
        int paused = 2;
        /**
         * completed
         */
        int completed = 3;
    }

    public interface VideoPlayListener {
        /**
         * Initialization.
         * After the initialization is successful, playControl can be used to play.
         *
         * @param ret 0：success；not 0：fail
         */
        void onPlayControlInit(IVideoPlayControl playControl, int ret);

        /**
         * release.
         * The playControl cannot be used anymore after release.
         */
        void onPlayControlRelease(IVideoPlayControl playControl);

        /**
         * Play status change.
         *
         * @param state last state.
         */
        void onPlayStateChange(IVideoPlayControl playControl, int state);

        /**
         * Play time update.
         *
         * @param currentTime   current video play time
         * @param videoDuration total time
         */
        void onPlayTimeChange(IVideoPlayControl playControl, float currentTime, float videoDuration);
    }

    public interface IVideoPlayControl extends IPlayControl {
        /**
         * @return state
         */
        @VideoPlayState
        int getState();

        void pause();

        /**
         * seek the progress
         *
         * @param progress progress
         */
        void seek(float progress);

        /**
         * @return total time
         */
        float getDuration();

        /**
         * @return current video play time
         */
        float getCurTime();
    }

    /**
     * play video file.
     *
     * @param file       file
     * @param isStitched stitch state
     * @param listener   listener
     * @return null: can't play
     */
    public IVideoPlayControl playVideo(File file, boolean isStitched, VideoPlayListener listener) {
        Log.d(TAG, "playVideo," + file);
        if (null != mPlay) {
            mPlay.release();
            mPlay = null;
        }
        if (isStitched) {
            mPlay = new StitchVideoPlayControl(file, listener);
        } else {
            mPlay = new UnStitchVideoPlayControl(file, listener);
        }
        return (IVideoPlayControl) mPlay;
    }

    /**
     * Stitched video play.
     */
    protected class StitchVideoPlayControl implements IVideoPlayControl {
        private final String TAG = MediaPlayerSurfaceView.TAG + "$StitchVideo";
        private final String filepath;
        private final VideoPlayListener listener;
        private final Handler mHandler;
        @VideoPlayState
        private volatile int state = VideoPlayState.release;
        private MediaPlayer mMyMediaPlayer;
        private final HandlerThread mThread;

        protected StitchVideoPlayControl(File file, @Nullable VideoPlayListener listener) {
            this.filepath = file.getAbsolutePath();
            this.listener = listener;
            mThread = new HandlerThread("play:" + file.getName());
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
            mHandler.post(this::initPlayer);
        }

        private final Runnable mTickRunnable = () -> {
            if (null != mMyMediaPlayer) {
                notifyStateTimeChange();
            }
            if (state == VideoPlayState.started) {
                loopTick();
            }
        };

        private void loopTick() {
            mHandler.removeCallbacks(mTickRunnable);
            mHandler.postDelayed(mTickRunnable, 100);
        }

        private void initPlayer() {
            Log.d(TAG, "initPlayer...");
            waitPanoInit(2000);
            if (null == mPiPano || !mPiPano.mHasInit) {
                Log.e(TAG, "initPlayer,PiPano not init.");
                if (null != listener) {
                    listener.onPlayControlInit(this, 1);
                }
                return;
            }
            mPiPano.setParamReCaliEnable(0, true); // 拼接视频不需要拼接距离
            setLensCorrectionMode(0x2);
            mMyMediaPlayer = new MediaPlayer();
            mMyMediaPlayer.setSurface(obtainGlSurface());
            mMyMediaPlayer.reset();
            try {
                mMyMediaPlayer.setDataSource(filepath);
                mMyMediaPlayer.prepare();
                mMyMediaPlayer.seekTo(0); // seek to start,show first frame.
            } catch (Exception ex) {
                Log.e(TAG, "initPlayer,ex:" + ex);
                ex.printStackTrace();
                if (null != listener) {
                    listener.onPlayControlInit(this, 1);
                }
                return;
            }
            mMyMediaPlayer.setOnSeekCompleteListener(mp -> {
                if (state == VideoPlayState.started) {
                    checkState();
                }
            });
            mMyMediaPlayer.setOnCompletionListener(mp -> changeState(VideoPlayState.completed));
            if (null != listener) {
                listener.onPlayControlInit(this, 0);
            }
            changeState(VideoPlayState.prepared);
            if (null != listener) {
                listener.onPlayTimeChange(this, 0f, getDuration());
            }
            Log.d(TAG, "initPlayer,ok");
        }

        private void releasePlayer() {
            Log.d(TAG, "releasePlayer...");
            if (mMyMediaPlayer != null) {
                if (mMyMediaPlayer.isPlaying()) {
                    mMyMediaPlayer.stop();
                }
                mMyMediaPlayer.release();
                mMyMediaPlayer = null;
                releaseGlSurface();
                if (null != listener) {
                    listener.onPlayControlRelease(this);
                }
            }
            Log.d(TAG, "releasePlayer,ok");
        }

        @Override
        public int getState() {
            return state;
        }

        @Override
        public void play() {
            if (state != VideoPlayState.started) {
                Log.d(TAG, "play");
                changeState(VideoPlayState.started);
                mHandler.post(this::checkState);
            }
        }

        @Override
        public void pause() {
            if (state != VideoPlayState.paused) {
                Log.d(TAG, "paused");
                changeState(VideoPlayState.paused);
                mHandler.post(this::checkState);
            }
        }

        @Override
        public void seek(float progress) {
            if (null != mMyMediaPlayer) {
                Log.d(TAG, "seek," + progress);
                int duration = mMyMediaPlayer.getDuration();
                mMyMediaPlayer.seekTo((int) (progress * duration));
            }
        }

        @Override
        public void release() {
            if (state != VideoPlayState.release) {
                Log.d(TAG, "release...");
                mHandler.removeCallbacksAndMessages(null);
                changeState(VideoPlayState.release);
                mHandler.post(this::checkState);
                resetParamReCaliEnable();
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "release,end");
            }
        }

        @Override
        public float getDuration() {
            return null != mMyMediaPlayer && state != VideoPlayState.release ?
                    mMyMediaPlayer.getDuration() : 0;
        }

        @Override
        public float getCurTime() {
            return null != mMyMediaPlayer && state != VideoPlayState.release ?
                    mMyMediaPlayer.getCurrentPosition() : 0;
        }

        private void checkState() {
            if (null == mMyMediaPlayer) {
                return;
            }
            Log.d(TAG, "checkState,state:" + state);
            switch (state) {
                case VideoPlayState.started: {
                    if (!mMyMediaPlayer.isPlaying()) {
                        mMyMediaPlayer.start();
                        loopTick();
                    }
                }
                break;
                case VideoPlayState.paused: {
                    if (mMyMediaPlayer.isPlaying()) {
                        mMyMediaPlayer.pause();
                    }
                }
                break;
                case VideoPlayState.release: {
                    releasePlayer();
                    mHandler.getLooper().quitSafely();
                }
                break;
                default:
                    break;
            }
        }

        private void changeState(int state) {
            if (this.state != state) {
                this.state = state;
                notifyStateChange();
            }
        }

        private void notifyStateChange() {
            if (listener != null) {
                listener.onPlayStateChange(this, state);
            }
        }

        private void notifyStateTimeChange() {
            if (listener != null) {
                listener.onPlayTimeChange(this, getCurTime(), getDuration());
            }
        }
    }

    /**
     * 未拼接视频播放。
     */
    protected class UnStitchVideoPlayControl implements IVideoPlayControl {
        private final String TAG = MediaPlayerSurfaceView.TAG + "$UnStitchVideo3";
        private final String filepath;
        private final VideoPlayListener listener;
        private final Handler mHandler;
        @VideoPlayState
        private volatile int mState = VideoPlayState.release;

        private VideoDecoder mVideoDecoder;
        private AudioDecoder mAudioDecoder;
        private float mDuration;

        private final HandlerThread mThread;

        protected UnStitchVideoPlayControl(File file, @Nullable VideoPlayListener listener) {
            this.filepath = file.getAbsolutePath();
            this.listener = listener;
            mThread = new HandlerThread("play:" + file.getName());
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
            mHandler.post(this::initPlayer);
        }

        private final Runnable mTickRunnable = () -> {
            if (null != mVideoDecoder && mState == VideoPlayState.started) {
                notifyStateTimeChange();
            }
            if (mState == VideoPlayState.started) {
                loopTick();
            }
        };

        private void loopTick() {
            mHandler.removeCallbacks(mTickRunnable);
            mHandler.postDelayed(mTickRunnable, 100);
        }

        private void initPlayer() {
            Log.d(TAG, "initPlayer...");
            waitPanoInit(2000);
            if (null == mPiPano || !mPiPano.mHasInit) {
                Log.e(TAG, "initPlayer,PiPano not init.");
                notifyPlayControlInit(1);
                return;
            }
            final String filepath0 = new File(filepath, "0.mp4").getAbsolutePath();
            mDuration = StitchingUtil.getDuring(filepath0) / 1000.0f;
            try {
                mPiPano.pausePreviewUpdate();
                setLensCorrectionMode(0x1111);
                mPiPano.useGyroscope(
                        new File(filepath, "stabilization").exists(),
                        filepath);
                releaseGlSurface();
                mVideoDecoder = new VideoDecoder(filepath0, obtainGlSurface());
                int pfs = mVideoDecoder.selectVideoTrackFps;
                mPiPano.setParamReCaliEnable(pfs > 0 ? pfs * 2 : 0, true); // 未拼接视频测量拼接距离间隔
            } catch (Exception ex) {
                Log.e(TAG, "initPlayer, video ex:" + ex);
                ex.printStackTrace();
                if (mPiPano != null) {
                    mPiPano.resumePreviewUpdate();
                }
                notifyPlayControlInit(1);
                return;
            }
            try {
                mAudioDecoder = new AudioDecoder(filepath0);
            } catch (Exception ex) {
                Log.e(TAG, "initPlayer, audio ex:" + ex);
                ex.printStackTrace();
            }
            notifyPlayControlInit(0);
            mState = VideoPlayState.prepared;
            notifyStateChange();
            notifyStateTimeChange();
            Log.d(TAG, "initPlayer,ok");
        }

        private void releasePlayer() {
            Log.d(TAG, "releasePlayer...");
            if (null != mVideoThread) {
                try {
                    mVideoThread.join();
                } catch (InterruptedException ex) {
                    Log.e(TAG, "releasePlayer, video ex:" + ex);
                    ex.printStackTrace();
                }
                mVideoThread = null;
            }
            if (null != mVideoDecoder) {
                mVideoDecoder.exitVideo();
                mVideoDecoder = null;
            }
            if (null != mAudioThread) {
                try {
                    mAudioThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mAudioThread = null;
            }
            if (null != mAudioDecoder) {
                mAudioDecoder.exitAudio();
                mAudioDecoder = null;
            }
            releaseGlSurface();
            Log.d(TAG, "releasePlayer,ok");
        }

        @Override
        public int getState() {
            return mState;
        }

        private Thread mVideoThread;
        private Thread mAudioThread;

        @Override
        public void play() {
            if (mState != VideoPlayState.started) {
                Log.d(TAG, "play");
                mState = VideoPlayState.started;
                notifyStateChange();
                //seek(0);
                loopTick();
                mVideoThread = new Thread(mVideoDecoder, "VideoDecoder");
                mVideoThread.start();
                if (null != mAudioDecoder) {
                    mAudioThread = new Thread(mAudioDecoder, "AudioDecoder");
                    mAudioThread.start();
                }
            }
        }

        @Override
        public void pause() {
            if (mState != VideoPlayState.paused) {
                Log.d(TAG, "paused");
                mState = VideoPlayState.paused;
                notifyStateChange();
            }
        }

        @Override
        public void seek(float progress) {
            long timeUs = (long) (mDuration * 1000L * progress);
            Log.d(TAG, "seek," + progress + ",mDuration:" + mDuration + ",timeUs:" + timeUs / 1000);
            if (null != mVideoDecoder) {
                mVideoDecoder.seek(timeUs);
            }
            if (null != mAudioDecoder) {
                mAudioDecoder.seek(timeUs);
            }
            if (mState == VideoPlayState.paused) {
                play();
            }
        }

        @Override
        public void release() {
            if (mState != VideoPlayState.release) {
                Log.d(TAG, "release...");
                mHandler.removeCallbacksAndMessages(null);
                mState = VideoPlayState.release;
                releasePlayer();
                mHandler.getLooper().quitSafely();
                resetParamReCaliEnable();
                try {
                    mThread.join();
                } catch (InterruptedException ex) {
                    Log.d(TAG, "release,ex:" + ex);
                    ex.printStackTrace();
                }
                notifyStateChange();
                notifyPlayControlRelease();
                Log.d(TAG, "release,end");
            }
        }

        @Override
        public float getDuration() {
            return mDuration;
        }

        @Override
        public float getCurTime() {
            if (null != mVideoDecoder) {
                MediaExtractor extractor = mVideoDecoder.mMediaExtractor;
                if (null != extractor && mState == VideoPlayState.started) {
                    return extractor.getSampleTime() / 1000f;
                }
            }
            return 0;
        }

        private void notifyPlayControlInit(int ret) {
            if (null != listener) {
                listener.onPlayControlInit(this, ret);
            }
        }

        private void notifyPlayControlRelease() {
            if (null != listener) {
                listener.onPlayControlRelease(this);
            }
        }

        private void notifyStateChange() {
            if (listener != null) {
                listener.onPlayStateChange(this, mState);
            }
        }

        private void notifyStateTimeChange() {
            if (listener != null) {
                float curTime = getCurTime();
                float duration = getDuration();
                listener.onPlayTimeChange(this, curTime, duration);
            }
        }

        private class VideoDecoder implements Runnable {
            private final String filepath0;
            private final Surface mSurface;
            private MediaExtractor mMediaExtractor = null;
            private MediaCodec mMediaCodec = null;

            private volatile long seekTimeUs = -1;

            int selectVideoTrackFps = 0;

            public VideoDecoder(String filepath0, Surface surface) throws IOException {
                this.filepath0 = filepath0;
                mSurface = surface;
                initVideo(filepath0);
            }

            private void initVideo(String filepath0) throws IOException {
                try {
                    mMediaExtractor = new MediaExtractor();
                    mMediaExtractor.setDataSource(filepath0);
                    MediaFormat videoFormat = null;
                    for (int trackIndex = 0, numTracks = mMediaExtractor.getTrackCount(); trackIndex < numTracks; trackIndex++) {
                        MediaFormat format = mMediaExtractor.getTrackFormat(trackIndex);
                        String mime = format.getString(MediaFormat.KEY_MIME);
                        if (mime.startsWith("video/")) {
                            videoFormat = format;
                            selectVideoTrackFps = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                            mMediaExtractor.selectTrack(trackIndex);
                            break;
                        }
                    }
                    if (null == videoFormat) {
                        throw new RuntimeException("No video track found in " + filepath0);
                    }
                    String mime = videoFormat.getString(MediaFormat.KEY_MIME);
                    mMediaCodec = MediaCodec.createDecoderByType(mime);
                    mMediaCodec.configure(videoFormat, mSurface, null, 0);
                    mMediaCodec.start();
                    //
                    decodeFirstVideoFrame();
                } catch (IOException ex) {
                    exitVideo();
                    throw ex;
                }
            }

            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            private long mBaseTimestamp;
            private volatile long mBasePresentationTime = 0;

            private void resetBaseTime() {
                mBaseTimestamp = 0;
                mBasePresentationTime = 0;
            }

            private void decodeFirstVideoFrame() {
                resetBaseTime();
                int inputIndex = handleVideoInputBuffer();
                if (inputIndex > 0) {
                    int outIndex;
                    final long timestamp = System.currentTimeMillis();
                    do {
                        outIndex = handleVideoOutputBuffer();
                        if (outIndex > 0) {
                            break;
                        }
                        SystemClock.sleep(10);
                    } while ((System.currentTimeMillis() - timestamp) < 500);
                }
                mPiPano.resumePreviewUpdate();
            }

            private boolean decodeOneVideoFrame() {
                if (seekTimeUs > 0) {
                    mMediaExtractor.seekTo(seekTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    seekTimeUs = -1;
                    mMediaCodec.flush();
                    resetBaseTime();
                } else if (seekTimeUs == 0) {
                    mMediaExtractor.seekTo(seekTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    seekTimeUs = -1;
                    mMediaCodec.flush();
                    resetBaseTime();
                }
                handleVideoInputBuffer();
                handleVideoOutputBuffer();
                return (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            }

            private int handleVideoInputBuffer() {
                int inputIndex = mMediaCodec.dequeueInputBuffer(500);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                    int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                    if (sampleSize > 0) {
                        mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mMediaExtractor.getSampleTime(), 0);
                        mMediaExtractor.advance();
                    } else {
                        mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
                return inputIndex;
            }

            private int handleVideoOutputBuffer() {
                int outIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500);
                if (outIndex > 0) {
                    boolean first = mBaseTimestamp < 1;
                    final long presentationTime = mBufferInfo.presentationTimeUs / 1000;
                    if (!first) {
                        long videoTime = System.currentTimeMillis() - mBaseTimestamp;
                        long sleepTime = (presentationTime - mBasePresentationTime) - videoTime;
                        if (sleepTime > 0) {
                            //Log.d(TAG, "handleVideoOutputBuffer,sleepTime:" + sleepTime + "ms");
                            SystemClock.sleep(sleepTime);
                        } else {
                            //Log.d(TAG, "handleVideoOutputBuffer,no sleepTime:" + sleepTime + "ms");
                        }
                    }
                    mMediaCodec.releaseOutputBuffer(outIndex, mSurface.isValid());
                    if (first) {
                        mBaseTimestamp = System.currentTimeMillis();
                        mBasePresentationTime = presentationTime;
                    }
                }
                return outIndex;
            }

            @Override
            public void run() {
                if (mMediaExtractor == null) {
                    try {
                        initVideo(filepath0);
                    } catch (Exception ex) {
                        Log.e(TAG, "run,ex:" + ex);
                        return;
                    }
                }
                resetBaseTime();
                mBufferInfo = new MediaCodec.BufferInfo();
                boolean end = false;
                float start = getCurTime();
                while (mState == VideoPlayState.started && !end) {
                    try {
                        end = decodeOneVideoFrame();
                    } catch (Exception ex) {
                        Log.e(TAG, "run,decodeOneVideoFrame happen ex:" + ex);
                        ex.printStackTrace();
                    }
                }
                Log.d(TAG, "run,play video cost time:" + (System.currentTimeMillis() - mBaseTimestamp) + "ms,expected:" + (getDuration() - start) + "ms");
                if (end) {
                    seek(1);
                    if (mState == VideoPlayState.started) {
                        mState = VideoPlayState.completed;
                        notifyStateChange();
                    }
                }
                if (mState == VideoPlayState.release || mState == VideoPlayState.completed) {
                    exitVideo();
                }
            }

            public void seek(long timeUs) {
                seekTimeUs = timeUs;
            }

            public void exitVideo() {
                Log.d(TAG, "exitVideo");
                if (mMediaCodec != null) {
                    mMediaCodec.stop();
                    mMediaCodec.reset();
                    mMediaCodec.release();
                    mMediaCodec = null;
                }
                if (null != mMediaExtractor) {
                    mMediaExtractor.release();
                    mMediaExtractor = null;
                }
            }
        }

        private class AudioDecoder implements Runnable {
            private final String filepath0;
            private MediaExtractor mMediaExtractor = null;
            private MediaCodec mMediaCodec = null;
            private AudioTrack mAudioTrack = null;

            private volatile long seekTimeUs = -1;
            private int mChannelCount = 2;
            private byte[] inputBytes, outputBytes;

            public AudioDecoder(String filepath0) throws IOException {
                this.filepath0 = filepath0;
                initAudio(filepath0);
            }

            private void initAudio(String filepath0) throws IOException {
                try {
                    mMediaExtractor = new MediaExtractor();
                    mMediaExtractor.setDataSource(filepath0);
                    MediaFormat audioFormat = null;
                    for (int trackIndex = 0, numTracks = mMediaExtractor.getTrackCount(); trackIndex < numTracks; trackIndex++) {
                        MediaFormat format = mMediaExtractor.getTrackFormat(trackIndex);
                        String mime = format.getString(MediaFormat.KEY_MIME);
                        if (mime.startsWith("audio/")) {
                            audioFormat = format;
                            mMediaExtractor.selectTrack(trackIndex);
                            break;
                        }
                    }
                    if (null == audioFormat) {
                        throw new RuntimeException("No audio track found in " + filepath0);
                    }
                    final String mime = audioFormat.getString(MediaFormat.KEY_MIME);
                    //
                    int audioChannels = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    mChannelCount = audioChannels;
                    mMediaCodec = MediaCodec.createDecoderByType(mime);
                    mMediaCodec.configure(audioFormat, null, null, 0);
                    mMediaCodec.start();
                    int audioSampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    final int min_buf_size = AudioTrack.getMinBufferSize(audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT);
                    final int max_input_size = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    int audioInputBufSize = Math.max(min_buf_size * 4, max_input_size);
                    final int frameSizeInBytes = audioChannels * 2;
                    audioInputBufSize = (audioInputBufSize / frameSizeInBytes) * frameSizeInBytes;
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT, audioInputBufSize, AudioTrack.MODE_STREAM);
                    mAudioTrack.play();
                } catch (IOException ex) {
                    if (mMediaCodec != null) {
                        mMediaCodec.reset();
                        mMediaCodec.release();
                        mMediaCodec = null;
                    }
                    if (null != mMediaExtractor) {
                        mMediaExtractor.release();
                        mMediaExtractor = null;
                    }
                    if (mAudioTrack != null) {
                        mAudioTrack.release();
                        mAudioTrack = null;
                    }
                    throw ex;
                }
            }

            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            private long mBaseTimestamp;
            private volatile long mBasePresentationTime = 0;

            private void resetBaseTime() {
                mBaseTimestamp = 0;
                mBasePresentationTime = 0;
            }

            private boolean decodeOneAudioFrame() {
                if (seekTimeUs > 0) {
                    mMediaExtractor.seekTo(seekTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    seekTimeUs = -1;
                    mMediaCodec.flush();
                    resetBaseTime();
                } else if (seekTimeUs == 0) {
                    mMediaExtractor.seekTo(seekTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    seekTimeUs = -1;
                    mMediaCodec.flush();
                    resetBaseTime();
                }
                handleAudioInputBuffer();
                handleAudioOutputBuffer();
                return (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            }

            private int handleAudioInputBuffer() {
                int inputIndex = mMediaCodec.dequeueInputBuffer(500);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                    int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                    if (sampleSize > 0) {
                        mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mMediaExtractor.getSampleTime(), 0);
                        mMediaExtractor.advance();
                    } else {
                        mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
                return inputIndex;
            }

            private int handleAudioOutputBuffer() {
                int outIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500);
                if (outIndex > 0) {
                    boolean first = mBaseTimestamp < 1;
                    final long presentationTime = mBufferInfo.presentationTimeUs / 1000;
                    if (!first) {
                        long videoTime = System.currentTimeMillis() - mBaseTimestamp;
                        long sleepTime = (presentationTime - mBasePresentationTime) - videoTime;
                        if (sleepTime > 0) {
                            SystemClock.sleep(sleepTime);
                            //Log.d(TAG, "handleAudioOutputBuffer,sleepTime:" + sleepTime + "ms");
                        } else {
                            Log.d(TAG, "handleAudioOutputBuffer,no sleepTime:" + sleepTime + "ms");
                        }
                    }
                    ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outIndex);
                    if (mChannelCount == 4) {
                        checkByteBuffer(byteBuffer);
                        byteBuffer.get(inputBytes);
                        changeToTwoChannelAudio(inputBytes, outputBytes, AudioFormat.ENCODING_PCM_16BIT);
                        byteBuffer = ByteBuffer.wrap(outputBytes);
                    }
                    mAudioTrack.write(byteBuffer, byteBuffer.limit(), AudioTrack.WRITE_NON_BLOCKING, mBufferInfo.presentationTimeUs);
                    mMediaCodec.releaseOutputBuffer(outIndex, false);
                    if (first) {
                        mBaseTimestamp = System.currentTimeMillis();
                        mBasePresentationTime = presentationTime;
                    }
                }
                return outIndex;
            }

            /**
             * 从 pano 4声道数据中 取出 左右声道数据
             */
            private void changeToTwoChannelAudio(byte[] inputBytes, byte[] outputBytes, int audioEncoding) {
                if (audioEncoding != AudioFormat.ENCODING_PCM_16BIT) {
                    return;
                }
                // pano 4声道数据分布 []{ front, left ,right ,back }
                int channelLeft = 1, channelRight = 2;
                int j = 0;
                final int channelCount = 4;
                // audioEncoding(pcm_16bit) 占字节个数
                final int oneChannelByte = 2;
                int step = channelCount * oneChannelByte;
                // 取出 左右声道数据
                int length = inputBytes.length;
                for (int i = 0; i < length; i = i + step) {
                    outputBytes[j++] = inputBytes[i + channelLeft * oneChannelByte];
                    outputBytes[j++] = inputBytes[i + channelLeft * oneChannelByte + 1];

                    outputBytes[j++] = inputBytes[i + channelRight * oneChannelByte];
                    outputBytes[j++] = inputBytes[i + channelRight * oneChannelByte + 1];
                }
            }

            @Override
            public void run() {
                mBufferInfo = new MediaCodec.BufferInfo();
                if (mMediaExtractor == null) {
                    try {
                        initAudio(filepath0);
                    } catch (Exception ex) {
                        Log.e(TAG, "run,ex:" + ex);
                        return;
                    }
                }
                resetBaseTime();
                boolean end = false;
                float start = getCurTime();
                while (mState == VideoPlayState.started && !end) {
                    try {
                        end = decodeOneAudioFrame();
                    } catch (Exception ex) {
                        Log.e(TAG, "run,decodeOneAudioFrame happen ex:" + ex);
                    }
                }
                Log.d(TAG, "run,play audio cost time:" + (System.currentTimeMillis() - mBaseTimestamp) + "ms,expected:" + (getDuration() - start) + "ms");
                if (end) {
                    seek(1);
                }
                if (mState == VideoPlayState.release || mState == VideoPlayState.completed) {
                    exitAudio();
                }
            }

            public void start() {
                new Thread(this, "AudioDecoder").start();
            }

            public void seek(long timeUs) {
                seekTimeUs = timeUs;
            }

            public void exitAudio() {
                Log.d(TAG, "exitAudio");
                if (mAudioTrack != null) {
                    mAudioTrack.stop();
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
                if (mMediaCodec != null) {
                    mMediaCodec.reset();
                    mMediaCodec.release();
                    mMediaCodec = null;
                }
                if (null != mMediaExtractor) {
                    mMediaExtractor.release();
                    mMediaExtractor = null;
                }
                inputBytes = null;
                outputBytes = null;
            }

            private void checkByteBuffer(ByteBuffer buffer) {
                if (inputBytes == null) {
                    inputBytes = new byte[buffer.remaining()];
                }
                if (inputBytes.length != buffer.remaining()) {
                    inputBytes = new byte[buffer.remaining()];
                }
                if (outputBytes == null) {
                    outputBytes = new byte[inputBytes.length / 2];
                }
                if (outputBytes.length != inputBytes.length / 2) {
                    outputBytes = new byte[inputBytes.length / 2];
                }
            }
        }
    }
}
