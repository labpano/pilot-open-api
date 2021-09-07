package com.pi.pano;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

@Keep
public class StitchingOpticalFlow {
    private static final String TAG = "StitchingOpticalFlow";

    @Keep
    private long mNativeContext;//c++用,不要删除

    private native void createNativeObj();

    private native void releaseNativeObj();

    static
    {
        System.loadLibrary("PiPano");
    }

    public StitchingOpticalFlow(Context context) {
        PiPano.makeDefaultWatermark(context);
        createNativeObj();
    }

    public void release()
    {
        releaseNativeObj();
    }

    /**
     * Add a task
     */
    public static native void addStitchingTask(String filename, boolean forceStitch, int stitchType, boolean usePiBlend);

    private static native String queryStitchingTask(String filename);

    public static native void deleteStitchingTask(String filename);

    public static native void deleteAllStitchingTask();

    public static native String queryFirstStitchingTaskFilename();

    public static PhotoStitchingTask getStitchingTask(String filename) {
        String retStr = queryStitchingTask(filename);
        if (retStr.equals("")) {
            return null;
        } else {
            PhotoStitchingTask task = new PhotoStitchingTask();
            task.filename = filename;
            task.isProcessing = false;
            try {
                task.type = Integer.parseInt(retStr);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "getStitchingTask,发生异常--->" + e);
                return null;
            }
            return task;
        }
    }

    public static native int getStitchingTaskCount();

    public static native boolean getIsStitchingTaskChanged();
}
