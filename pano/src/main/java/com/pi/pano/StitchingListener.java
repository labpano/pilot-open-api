package com.pi.pano;

/**
 * 视频后处理监听,可以监听视频拼接进度等
 */
public interface StitchingListener {
    /**
     * 当StitchingUtil.Task progress变化的时候,回调
     *
     * @param task 状态变化的task
     */
    void onStitchingProgressChange(StitchingUtil.Task task);

    /**
     * 当StitchingUtil.Task state变化的时候,回调
     *
     * @param task 状态变化的task
     */
    void onStitchingStateChange(StitchingUtil.Task task);

    /**
     * 拼接删除完成
     */
    void onStitchingDeleteFinish(StitchingUtil.Task task);

    /**
     * 拼接删除开始
     */
    void onStitchingDeleteDeleting(StitchingUtil.Task task);
}
