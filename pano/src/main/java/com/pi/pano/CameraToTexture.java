package com.pi.pano;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

class CameraToTexture {
    private static final String TAG = CameraToTexture.class.getSimpleName();

    private static final int[] CAMERA_VIDEO_3648_2280 = new int[]{3648, 2280};
    private static final int[] CAMERA_VIDEO_3520_2200 = new int[]{3520, 2200};
    private static final int[] CAMERA_VIDEO_3008_1880 = new int[]{3008, 1880};
    private static final int[] CAMERA_VIDEO_2192_1370 = new int[]{2192, 1370};

    private Camera mCamera;
    private int mIndex;
    private SurfaceTexture mSurfaceTexture;
    private MediaRecorder mMediaRecorder;
    private int mFps;
    public int mDefaultISO;

    CameraToTexture(int index, SurfaceTexture surfaceTexture) {
        mIndex = index;
        mSurfaceTexture = surfaceTexture;
    }

    int getPreivewWidth() {
        if (mCamera == null) {
            return 0;
        }
        return mCamera.getParameters().getPreviewSize().width;
    }

    int getPreivewHeight() {
        if (mCamera == null) {
            return 0;
        }
        return mCamera.getParameters().getPreviewSize().height;
    }

    int getPreviewFps() {
        if (mCamera == null) {
            return 0;
        }
        return mFps;
    }

    Camera getCamera() {
        return mCamera;
    }

    void openCamera(int width, int height, int fps, int exposureCompenstation, int iso, int wb) {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        try {
            mCamera = Camera.open(mIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mCamera == null) {
            Log.e(TAG, "Unable to open camera index : " + mIndex);
            return;
        }

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            Camera.Parameters params = mCamera.getParameters();
            choosePreviewSize(params, width, height);
            params.setRecordingHint(false);
            params.setExposureCompensation(exposureCompenstation);
            mFps = fps;
            params.setPreviewFpsRange(fps * 1000, fps * 1000);
            mCamera.setParameters(params);
            setISO(iso);
            setWhiteBalance(wb);

            Log.i(TAG, "open camera index: " + mIndex + " " +
                    params.getPreviewSize().width + "*" +
                    params.getPreviewSize().height + " target fps: " + mFps);
        } catch (Exception e) {
            Log.e(TAG, "setPreviewTexture failed surface: " + mSurfaceTexture);
            e.printStackTrace();
        }
    }

    void startPreview(ExecutorService mFixedThreadPool) {
        mFixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start preview index: " + mIndex + " mCamera: " + mCamera);
                try {
                    if (mCamera != null) {
                        mCamera.startPreview();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void releaseCamera() {
        Log.i(TAG, "release camera index: " + mIndex);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }
        Log.e(TAG, "Unable to set preview size to " + width + "x" + height);
    }

    void setExposureCompensation(int value) {
        try {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                params.setExposureCompensation(value);
                mCamera.setParameters(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setISO(int iso) {
        try {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                switch (iso) {
                    case 0:
                        params.set("iso", "auto");
                        mDefaultISO = 0;
                        break;
                    case 1:
                        params.set("iso", "50");
                        mDefaultISO = 50;
                        break;
                    case 2:
                        params.set("iso", "100");
                        mDefaultISO = 100;
                        break;
                    case 3:
                        params.set("iso", "200");
                        mDefaultISO = 200;
                        break;
                    case 4:
                        params.set("iso", "400");
                        mDefaultISO = 400;
                        break;
                    case 5:
                        params.set("iso", "800");
                        mDefaultISO = 800;
                        break;
                    case 6:
                        params.set("iso", "1600");
                        mDefaultISO = 1600;
                        break;
                    case 7:
                        params.set("iso", "sports");
                        break;
                    case 8:
                        params.set("iso", "night");
                        break;
                }
                mCamera.setParameters(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setWhiteBalance(int value) {
        try {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                switch (value) {
                    case 0:
                        params.set("whitebalance", "auto");
                        break;
                    case 1:
                        params.set("whitebalance", "fluorescent");
                        break;
                    case 2:
                        params.set("whitebalance", "incandescent");
                        break;
                    case 3:
                        params.set("whitebalance", "cloudy-daylight");
                        break;
                    case 4:
                        params.set("whitebalance", "daylight");
                        break;
                }
                mCamera.setParameters(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setAutoWhiteBalanceLock(boolean value) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            params.setAutoWhiteBalanceLock(value);
            mCamera.setParameters(params);
        }
    }

    private String mFilename;

    void startRecord(String dirname, final MediaRecorderListener listener, int video_encoder, int channelCount) {
        if (mCamera == null) {
            return;
        }

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        } else {
            mMediaRecorder.reset();
        }

        int previeWidth = getPreivewWidth();
        Log.i(TAG, "startRecord start==> filename: " + dirname + " index: " + mIndex + " preivew: " + previeWidth);
        if (dirname.length() == 0) {
            Log.e(TAG, "初始化状态出错");
        }

        mFilename = dirname + "/" + mIndex + ".mp4";

        try {
            File f = new File(mFilename);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        if (mIndex == 0) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setAudioChannels(channelCount);
            mMediaRecorder.setAudioSamplingRate(48000);
            mMediaRecorder.setAudioEncodingBitRate(128000);
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);

        mMediaRecorder.setVideoEncoder(video_encoder);

        if (mIndex == 0) {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }

        int[] videoSize;
        if (previeWidth == PilotSDK.CAMERA_PREVIEW_400_250_24[0]) {
            videoSize = CAMERA_VIDEO_3648_2280;
        } else if (previeWidth == PilotSDK.CAMERA_PREVIEW_288_180_24[0]) {
            videoSize = CAMERA_VIDEO_3520_2200;
        } else if (previeWidth == PilotSDK.CAMERA_PREVIEW_512_320_30[0]) {
            videoSize = CAMERA_VIDEO_3008_1880;
        } else {
            videoSize = CAMERA_VIDEO_2192_1370;
        }

        mMediaRecorder.setVideoSize(videoSize[0], videoSize[1]);
        mMediaRecorder.setVideoFrameRate(mFps);
        mMediaRecorder.setVideoEncodingBitRate(videoSize[0] * videoSize[1] * 4);
        mMediaRecorder.setOutputFile(mFilename);

        mMediaRecorder.setMaxDuration(0);//called after setOutputFile before prepare,if zero or negation,disables the limit
        mMediaRecorder.setMaxFileSize(0);//called after setOutputFile before prepare,if zero or negation,disables the limit

        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        break;
                    default:
                        break;
                }
                Log.i(TAG, "MediaRecorder.OnInfoListener what:" + what + " extra:" + extra);
            }
        });

        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                        break;
                    case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                        break;
                    default:
                        break;
                }
                Log.e(TAG, "MediaRecorder.OnErrorListener what:" + what + " extra:" + extra);
                listener.onError(what);
                mFilename = null;
                stopRecord("", null);
            }
        });

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        mMediaRecorder.start();
    }

    void stopRecord(String firmware, PiPano piPano) {
        Log.i(TAG, "stopRecord index:" + mIndex);
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
            }

            if (mMediaRecorder != null) {
                mMediaRecorder = null;
            }

        }

        if (mIndex == 0 && mFilename != null) {
            if (piPano != null) {
                piPano.spatialMediaImpl(mFilename, false, firmware);
            }
            mFilename = null;
        }
    }
}
