package com.pi.pano;

import androidx.annotation.Nullable;

/**
 * Switch resolution listener
 */
public abstract class ChangeResolutionListener {
    public int mWidth;
    public int mHeight;
    public int mFps;
    public String mCameraId;

    public boolean mReopen;
    /**
     * 强制切换分辨率
     */
    public boolean forceChange = false;
    /**
     * 填充参数
     *
     * @param cameraId 镜头id
     * @param width    宽度
     * @param height   高度
     * @param fps      帧率
     */
    public void fillParams(@Nullable String cameraId, int width, int height, int fps) {
        this.mWidth = width;
        this.mHeight = height;
        this.mFps = fps;
        if (cameraId != null) {
            this.mCameraId = cameraId;
        } else {
            this.mCameraId = "2";
        }
    }

    /**
     * 切换分辨率监听接口，频繁快速切换分辨率，切换的任务队列里最多有两个切换任务，一个是正在切换中的
     * 任务，一个是最后的切换任务，中间做所的切换任务都会忽略，所以要判断所有切换任务是否完成，一定要
     * 检测你要切换的分辨率width和height是否和回调的width和height相同
     *
     * @param width  切换后的width
     * @param height 切换后的height
     */
    protected void onChangeResolution(int width, int height) {
    }

    public String toParamsString() {
        return "{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mFps=" + mFps +
                ", mCameraId='" + mCameraId + '\'' +
                ", forceChange=" + forceChange +
                '}';
    }
}
