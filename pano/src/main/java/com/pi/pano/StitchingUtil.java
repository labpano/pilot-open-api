package com.pi.pano;

import android.content.Context;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Video post-processing class, used to splice multiple fisheye mp4 videos into a panoramic video.
 * You can use addTask several times to add multiple tasks to the task queue. The default task added to the task queue is STITCH_STATE_PAUSE status,
 * you can use startAllTask to start all tasks, and only one splicing task can be executed at the same time.
 * Therefore, only the task at the top of the queue will become STITCH_STATE_START，
 * The status of other tasks changes to STITCH_STATE_WAIT: When a task is spliced, it will be automatically removed from the queue.
 */
public class StitchingUtil {
    private final String TAG = "StitchingUtil";

    static {
        System.loadLibrary(Config.PIPANO_SO_NAME);
    }

    /**
     * stitch state
     */
    public enum StitchState {
        /**
         * start
         */
        STITCH_STATE_START,
        /**
         * pausing
         */
        STITCH_STATE_PAUSING,
        /**
         * pause
         */
        STITCH_STATE_PAUSE,
        /**
         * wati
         */
        STITCH_STATE_WAIT,
        /**
         * error
         */
        STITCH_STATE_ERROR,
        /**
         * starting
         */
        STITCH_STATE_STARTING,
        /**
         * stop
         */
        STITCH_STATE_STOP,
        /**
         * stopping
         */
        STITCH_STATE_STOPING
    }

    /**
     * stitch task
     */
    public class Task {
        /**
         * file
         */
        File mFile;

        String mDestDirPath;

        /**
         * stitch state
         */
        volatile StitchState mState = StitchState.STITCH_STATE_PAUSE;
        /**
         * progress
         */
        volatile float mProgress;

        /**
         * error code
         */
        volatile int mErrorCode;

        /**
         * delete last pause file
         */
        volatile boolean mDeleteStitchingPauseFile = false;

        int mWidth, mHeight, mFps, mBitRate;

        long mStitchPictureInUs = -1;

        /**
         * mime
         */
        final String mMime;

        /**
         * use flow
         */
        final boolean mUseFlow;

        File mOutputJpgFile;

        /**
         * @param file     file path
         * @param progress progress
         * @param width    output width
         * @param height   output height
         * @param fps      output fps
         * @param bitRate  output bitRate
         * @param mime     output mime
         * @param useFlow  use flow
         */
        Task(File file, float progress, int width, int height, int fps, int bitRate, String mime, boolean useFlow) {
            mFile = file;
            mProgress = progress;
            mWidth = width;
            mHeight = height;
            mFps = fps;
            mBitRate = bitRate;
            mMime = mime;
            mUseFlow = useFlow;
            mDestDirPath = file.getParentFile().getAbsolutePath();
        }

        Task(File file, int width, int height, long pictureTimestampUs, File outputJpgFile) {
            mFile = file;
            mWidth = width;
            mHeight = height;
            mFps = 30;
            mStitchPictureInUs = pictureTimestampUs;
            mMime = MediaFormat.MIMETYPE_VIDEO_AVC;
            mOutputJpgFile = outputJpgFile;
            mUseFlow = false;
            mDestDirPath = file.getParentFile().getAbsolutePath();
        }

        /**
         * get error code
         *
         * @return error code
         */
        public int getErrorCode() {
            return mErrorCode;
        }

        /**
         * Start to specify splicing tasks. Only one splicing task can be executed at the same time.
         * If other tasks are being executed, the status of the specified task is STITCH_STATE_WAIT,
         * otherwise the status of the specified task is STITCH_STATE_START.
         */
        public void startStitchTask() {
            Log.i(TAG, "startStitchTask " + mFile + "  state--->" + mState);
            if (mState == StitchState.STITCH_STATE_PAUSE) {
                changeState(StitchState.STITCH_STATE_WAIT);
            }
            synchronized (mTaskList) {
                mTaskList.notifyAll();
            }
        }

        /**
         * Pause the specified splicing task to make its status as STITCH_ STATE_ PAUSING.
         */
        public void pauseStitchTask() {
            Log.i(TAG, "pauseStitchTask " + mFile);
            if (mState == StitchState.STITCH_STATE_START ||
                    mState == StitchState.STITCH_STATE_STARTING ||
                    mState == StitchState.STITCH_STATE_WAIT) {
                changeState(StitchState.STITCH_STATE_PAUSING);
            }
        }

        void deleteStitchingPauseFile() {
            if (mDeleteStitchingPauseFile) {
                if (mStitchingListener != null) {
                    mStitchingListener.onStitchingDeleteDeleting(this);
                }
                File pauseFile = new File(mFile, "stitching_pause.mp4");
                if (pauseFile.exists()) {
                    if (pauseFile.delete()) {
                        mDeleteStitchingPauseFile = false;
                        if (null != mStitchingListener) {
                            mStitchingListener.onStitchingDeleteFinish(this);
                        }
                    }
                }
            }
        }

        /**
         * Delete a task from the task queue.
         */
        public void deleteStitchTask() {
            Log.i(TAG, "deleteStitchTask " + mFile);
            mDeleteStitchingPauseFile = true;
            //先删除一下试试,文件有可能被占用,如果被占用,那么一会再删除
            deleteStitchingPauseFile();
            pauseStitchTask();
            synchronized (mTaskList) {
                if (mTaskList.contains(this)) {
                    mTaskList.remove(this);
                }
            }
            if (mStitchingListener != null) {
                mStitchingListener.onStitchingStateChange(this);
            }
        }

        /**
         * Topping a splicing task.
         */
        public void topStitchTask() {
            Log.i(TAG, "stickStitchTask " + mFile);
            synchronized (mTaskList) {
                if (mTaskList.getFirst() == this) {
                    return;
                }
                mTaskList.remove(this);
                mTaskList.addFirst(this);
            }
            if (mStitchingListener != null) {
                mStitchingListener.onStitchingStateChange(this);
            }
        }

        public File getFile() {
            return mFile;
        }

        public File getOutputJpgFile() {
            return mOutputJpgFile;
        }

        public StitchState getStitchState() {
            return mState;
        }

        public float getProgress() {
            return mProgress;
        }

        void changeState(StitchState state) {
            Log.i(TAG, "changeState " + mState + "=>" + state + " file:" + mFile);
            mState = state;
            if (mStitchingListener != null) {
                mStitchingListener.onStitchingStateChange(this);
            }
        }
    }

    final LinkedList<Task> mTaskList = new LinkedList<>();
    StitchingListener mStitchingListener;
    private StitchingThread mThread;
    static String mFirmware;
    static String mArtist;

    public StitchingUtil(Context context, String firmware, String artist) {
        mFirmware = firmware;
        mArtist = artist;
        mThread = new StitchingThread(context, this);
        mThread.start();
    }

    /**
     * set stitch listener.
     *
     * @param listener listener
     */
    public void setStitchingListener(StitchingListener listener) {
        mStitchingListener = listener;
    }

    /**
     * Add a task to the queue.
     *
     * @param file file
     * @param mime mime
     */
    public void addStitchTask(File file, String mime) {
        addStitchTask(file, 7680, 3840, 30, 0, mime, false);
    }

    /**
     * Add a task to the queue.
     *
     * @param file    file
     * @param width   width
     * @param height  height
     * @param fps     fps
     * @param bitrate bitrate
     * @param mime    mime
     * @param useFlow use flow
     */
    public void addStitchTask(File file, int width, int height, int fps, int bitrate, String mime, boolean useFlow) {
        Log.i(TAG, "add a task dir: " + file.getName());
        if (width <= 0 || height <= 0 || fps <= 0) {
            Log.e(TAG, "addStitchTask params invalid: width " + width + " height " + height + " fps " + fps);
        }
        if (!file.exists()) {
            Log.e(TAG, "addStitchTask not exit dir: " + file.getName());
            return;
        }
        for (Task task : mTaskList) {
            if (task.mFile.compareTo(file) == 0) {
                Log.e(TAG, "addStitchTask already exit dir: " + file.getName());
                return;
            }
        }
        float progress = 0;
        File pauseFile = new File(file, "stitching_pause.mp4");
        if (pauseFile.exists()) {
            progress = getDuring(pauseFile.getAbsolutePath()) * 100 / getDuring(new File(file, "0.mp4").getAbsolutePath());
        }
        mTaskList.add(new Task(file, progress, width, height, fps, bitrate, mime, useFlow));
    }

    /**
     * Add a task to the queue.
     *
     * @param stiDirName            file
     * @param width                 width
     * @param height                height
     * @param pictureTimestampUsUTC The utc timestamp of the jpeg image you want to output, in microseconds.
     * @param outputJpgFilename     file path of output jpeg file
     */
    public void addStitchTask(String stiDirName, int width, int height, long pictureTimestampUsUTC, String outputJpgFilename) {
        File file = new File(stiDirName);
        Log.i(TAG, "Add a task dir: " + file.getName() + " timestamp: " + pictureTimestampUsUTC);
        if (width <= 0 || height <= 0 || pictureTimestampUsUTC < 0) {
            Log.e(TAG, "addStitchTask params invalid: width " + width + " height " + height +
                    " pictureTimestampUsUTC " + pictureTimestampUsUTC);
        }
        if (!file.exists()) {
            Log.e(TAG, "addStitchTask not exists dir: " + file.getName());
            return;
        }
        for (Task task : mTaskList) {
            if (task.getOutputJpgFile() == null && task.mFile.compareTo(file) == 0) {
                Log.e(TAG, "addStitchTask already exists dir: " + file.getName());
                return;
            }
        }
        File mp4File0 = new File(file, "0.mp4");
        if (!mp4File0.exists()) {
            Log.e(TAG, "addStitchTask not exists 0.mp4: " + file.getName());
            return;
        }
        long firstFrameUsUTC = PiPano.getFirstFrameUsUTC(mp4File0.getAbsolutePath());
        long during = getDuring(mp4File0.getAbsolutePath());
        long pictureTimestampUs = pictureTimestampUsUTC - firstFrameUsUTC;
        if (pictureTimestampUs > during) {
            Log.e(TAG, "pictureTimestampUs(" + pictureTimestampUs + ") > during(" + during + ")");
            pictureTimestampUs = during - 500000;
        }
        if (pictureTimestampUs < 0) {
            Log.e(TAG, "pictureTimestampUsUTC(" + pictureTimestampUsUTC + ") < firstFrameUsUTC(" + firstFrameUsUTC + ")");
            pictureTimestampUs = 0;
        }
        Log.i(TAG, "firstFrameUs: " + (pictureTimestampUsUTC - firstFrameUsUTC));
        mTaskList.add(new Task(file, width, height, pictureTimestampUs, new File(outputJpgFilename)));
    }

    /**
     * Get task queue
     *
     * @return task queue
     */
    public List<Task> getStitchTaskQueue() {
        return mTaskList;
    }

    /**
     * Start all tasks except STITCH_STATE_START setting bit STITCH of all tasks in START status_STATE_WAIT
     */
    public void startAllStitchTask() {
        Log.i(TAG, "startAllStitchTask");
        for (Task task : mTaskList) {
            if (task.getStitchState() != StitchState.STITCH_STATE_PAUSING) {
                task.changeState(StitchState.STITCH_STATE_WAIT);
            }
        }
        synchronized (mTaskList) {
            mTaskList.notifyAll();
        }
    }

    /**
     * Pause all tasks except those in error status.
     */
    public void pauseAllStitchTask() {
        Log.i(TAG, "pauseAllStitchTask");
        for (Task task : mTaskList) {
            if (task.getStitchState() == StitchState.STITCH_STATE_START ||
                    task.getStitchState() == StitchState.STITCH_STATE_STARTING ||
                    task.getStitchState() == StitchState.STITCH_STATE_WAIT) {
                task.changeState(StitchState.STITCH_STATE_PAUSING);
            }
        }
    }

    /**
     * Delete all tasks.
     */
    public void deleteAllStitchTask() {
        Log.i(TAG, "deleteAllStitchTask");
        for (Task task : mTaskList) {
            task.mDeleteStitchingPauseFile = true;
            task.deleteStitchingPauseFile();
        }
        pauseAllStitchTask();
        mTaskList.clear();
    }

    static long getDuring(String filename) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(filename);
            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Long.parseLong(durationStr) * 1000;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mmr.release();
        }
        return 1;
    }

    Task getNextTask() {
        synchronized (mTaskList) {
            for (Task task : mTaskList) {
                if (task.getStitchState() == StitchState.STITCH_STATE_START ||
                        task.getStitchState() == StitchState.STITCH_STATE_STARTING ||
                        task.getStitchState() == StitchState.STITCH_STATE_WAIT) {
                    return task;
                }
            }
        }
        return null;
    }

    void checkHaveNextTask() {
        synchronized (mTaskList) {
            while (getNextTask() == null) {
                try {
                    mTaskList.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
