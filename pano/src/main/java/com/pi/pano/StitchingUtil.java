package com.pi.pano;

import android.content.Context;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Video postpone-processing class, used to stitch multiple fisheye mp4 videos into a panoramic video.
 */
public class StitchingUtil
{
    private final String TAG = "StitchingUtil";

    static
    {
        System.loadLibrary("PiPano");
    }

    /**
     * Task status.
     */
    public enum StitchState
    {
        STITCH_STATE_START,
        STITCH_STATE_PAUSING,
        STITCH_STATE_PAUSE,
        STITCH_STATE_WAIT,
        STITCH_STATE_ERROR,
        STITCH_STATE_STARTING,
        STITCH_STATE_STOP,
        STITCH_STATE_STOPING
    }

    public class Task
    {
        /**
         * The video folder pointed to by the current task.
         */
        File mFile;
        volatile StitchState mState = StitchState.STITCH_STATE_PAUSE;
        volatile float mProgress;

        volatile int mErrorCode;

        volatile boolean mDeleteStitchingPauseFile = false;

        int mWidth, mHeight, mFps;

        long mStitchPictureInUs = -1;

        final String mMime;

        File mOutputJpgFile;

        Task(File file, float progress, int width, int height, int fps, String mime)
        {
            mFile = file;
            mProgress = progress;
            mWidth = width;
            mHeight = height;
            mFps = fps;
            mMime = mime;
        }

        Task(File file, int width, int height, long pictureTimestampUs, File outputJpgFile)
        {
            mFile = file;
            mWidth = width;
            mHeight = height;
            mFps = 30;
            mStitchPictureInUs = pictureTimestampUs;
            mMime = MediaFormat.MIMETYPE_VIDEO_AVC;
            mOutputJpgFile = outputJpgFile;
        }

        public int getErrorCode()
        {
            return mErrorCode;
        }

        public void startStitchTask()
        {
            Log.i(TAG,"startStitchTask " + mFile + "  state--->" + mState);
            if (mState == StitchState.STITCH_STATE_PAUSE)
            {
                changeState(StitchState.STITCH_STATE_WAIT);
            }
            synchronized (mTaskList)
            {
                mTaskList.notifyAll();
            }
        }

        public void pauseStitchTask()
        {
            Log.i(TAG, "pauseStitchTask " + mFile);
            if (mState == StitchState.STITCH_STATE_START ||
                    mState == StitchState.STITCH_STATE_STARTING ||
                    mState == StitchState.STITCH_STATE_WAIT)
            {
                changeState(StitchState.STITCH_STATE_PAUSING);
            }
        }

        void deleteStitchingPauseFile()
        {
            if (mDeleteStitchingPauseFile) {
                if (mStitchingListener!=null){
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

        public void deleteStitchTask()
        {
            Log.i(TAG,"deleteStitchTask " + mFile);
            mDeleteStitchingPauseFile = true;
            //先删除一下试试,文件有可能被占用,如果被占用,那么一会再删除
            deleteStitchingPauseFile();
            pauseStitchTask();
            synchronized (mTaskList)
            {
                if (mTaskList.contains(this))
                {
                    mTaskList.remove(this);
                }
            }
            if (mStitchingListener != null)
            {
                mStitchingListener.onStitchingStateChange(this);
            }
        }

        public void topStitchTask()
        {
            Log.i(TAG,"stickStitchTask "+mFile);
            synchronized (mTaskList)
            {
                if (mTaskList.getFirst() == this)
                {
                    return;
                }
                mTaskList.remove(this);
                mTaskList.addFirst(this);
            }
            if (mStitchingListener != null)
            {
                mStitchingListener.onStitchingStateChange(this);
            }
        }

        public File getFile()
        {
            return mFile;
        }

        public File getOutputJpgFile()
        {
            return mOutputJpgFile;
        }

        public StitchState getStitchState()
        {
            return mState;
        }

        public float getProgress()
        {
            return mProgress;
        }

        void changeState(StitchState state)
        {
            Log.i(TAG,"changeState " + mState + "=>" + state + " file:"+mFile);
            mState = state;
            if (mStitchingListener != null)
            {
                mStitchingListener.onStitchingStateChange(this);
            }
        }
    }

    final LinkedList<Task> mTaskList = new LinkedList<>();
    StitchingListener mStitchingListener;
    private StitchingThread mThread;
    static String mFirmware;

    public StitchingUtil(Context context, String firmware)
    {
        mFirmware = firmware;
        mThread = new StitchingThread(context, this);
        mThread.start();
    }

    public void setStitchingListener(StitchingListener listener)
    {
        mStitchingListener = listener;
    }

    public void addStitchTask(File file, String mime)
    {
        addStitchTask(file,7680,3840,30, mime);
    }

    public void addStitchTask(File file, int width, int height, int fps, String mime)
    {
        Log.i(TAG, "Add a task dir: " + file.getName());

        if (width <= 0 || height <=0 || fps <= 0)
        {
            Log.e(TAG, "addStitchTask params invalid: width " + width + " height "+height+" fps "+fps);
        }

        if (!file.exists())
        {
            Log.e(TAG, "addStitchTask not exit dir: " + file.getName());
            return;
        }

        for (Task task : mTaskList)
        {
            if (task.mFile.compareTo(file) == 0)
            {
                Log.e(TAG, "addStitchTask already exit dir: " + file.getName());
                return;
            }
        }

        float progress = 0;

        File pauseFile = new File(file, "stitching_pause.mp4");
        if (pauseFile.exists())
        {
            progress = getDuring(pauseFile.getAbsolutePath()) * 100 / getDuring(new File(file, "0.mp4").getAbsolutePath());
        }

        mTaskList.add(new Task(file, progress, width, height, fps, mime));
    }

    public void addStitchTask(String stiDirName, int width, int height, long pictureTimestampUsUTC, String outputJpgFilename)
    {
        File file = new File(stiDirName);

        Log.i(TAG, "Add a task dir: " + file.getName() + " timestamp: " + pictureTimestampUsUTC);

        if (width <= 0 || height <=0 || pictureTimestampUsUTC < 0)
        {
            Log.e(TAG, "addStitchTask params invalid: width " + width + " height "+height+
                    " pictureTimestampUsUTC " + pictureTimestampUsUTC);
        }

        if (!file.exists())
        {
            Log.e(TAG, "addStitchTask not exists dir: " + file.getName());
            return;
        }

        for (Task task : mTaskList)
        {
            if (task.getOutputJpgFile() == null && task.mFile.compareTo(file) == 0)
            {
                Log.e(TAG, "addStitchTask already exists dir: " + file.getName());
                return;
            }
        }

        File mp4File0 = new File(file.getAbsolutePath(), "0.mp4");
        if (!mp4File0.exists())
        {
            Log.e(TAG, "addStitchTask not exists 0.mp4: " + file.getName());
            return;
        }

        long firstFrameUsUTC = PiPano.getFirstFrameUsUTC(mp4File0.getAbsolutePath());
        long during = getDuring(mp4File0.getAbsolutePath());
        long pictureTimestampUs = pictureTimestampUsUTC - firstFrameUsUTC;
        if (pictureTimestampUs > during)
        {
            Log.e(TAG, "pictureTimestampUs(" + pictureTimestampUs + ") > during(" + during + ")");
            pictureTimestampUs = during - 500000;
        }
        if (pictureTimestampUs < 0)
        {
            Log.e(TAG, "pictureTimestampUsUTC(" + pictureTimestampUsUTC + ") < firstFrameUsUTC(" + firstFrameUsUTC + ")");
            pictureTimestampUs = 0;
        }

        Log.i(TAG, "firstFrameUs: " + (pictureTimestampUsUTC - firstFrameUsUTC));

        mTaskList.add(new Task(file, width, height, pictureTimestampUs, new File(outputJpgFilename)));
    }

    public List<Task> getStitchTaskQueue()
    {
        return mTaskList;
    }

    public void startAllStitchTask()
    {
        Log.i(TAG,"startAllStitchTask");
        for (Task task : mTaskList)
        {
            if (task.getStitchState() != StitchState.STITCH_STATE_PAUSING)
            {
                task.changeState(StitchState.STITCH_STATE_WAIT);
            }
        }
        synchronized (mTaskList)
        {
            mTaskList.notifyAll();
        }
    }

    public void pauseAllStitchTask()
    {
        Log.i(TAG,"pauseAllStitchTask");
        for (Task task : mTaskList)
        {
            if (task.getStitchState() == StitchState.STITCH_STATE_START ||
                    task.getStitchState() == StitchState.STITCH_STATE_STARTING ||
                    task.getStitchState() == StitchState.STITCH_STATE_WAIT)
            {
                task.changeState(StitchState.STITCH_STATE_PAUSING);
            }
        }
    }

    public void deleteAllStitchTask()
    {
        Log.i(TAG,"deleteAllStitchTask");
        for (Task task : mTaskList)
        {
            task.mDeleteStitchingPauseFile = true;
            task.deleteStitchingPauseFile();
        }
        pauseAllStitchTask();
        mTaskList.clear();
    }

    static long getDuring(String filename)
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        try
        {
            mmr.setDataSource(filename);
            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Long.parseLong(durationStr) * 1000;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            mmr.release();
        }

        return 1;
    }

    Task getNextTask()
    {
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

    void checkHaveNextTask()
    {
        synchronized (mTaskList)
        {
            while (getNextTask() == null)
            {
                try
                {
                    mTaskList.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
