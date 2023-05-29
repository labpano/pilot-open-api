/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */
package com.pi.pano;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.pi.pano.ext.IAudioEncoderExt;
import com.pi.pano.ext.IAudioRecordExt;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MediaRecorder Util.
 */
public class MediaRecorderUtil {
    private static final String TAG = "MediaRecorderUtil";
    /**
     * 4 channels.
     */
    private static final int CHANNEL_FOUR = 4;

    private static Location mLocation;
    private static final Object mLocationLock = new Object();

    private byte[] mAudioBuffer;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private MediaMuxer mMediaMuxer;
    private volatile boolean mIsMediaMuxerStart;

    private int mVideoEncoderTrack = -1;
    private volatile int mAudioEncoderTrack = -1;
    private int mCammEncoderTrack = -1;
    private volatile boolean mRun;
    private float mLastBearing = -1;                //上一次记录的location方位信息,如果这一次location不包含方位信息,就用上一次的
    private long mFirstFrameTimestamp = -1;         //第一帧时间戳
    private long mMemomotionRatio = 0;              //延时摄影倍率
    private float mMemomotionTimeMulti = 1.0f; //用于转换真实时间戳和延时摄影时间戳
    private boolean mUseForGoogleMap = false;
    private MediaRecorderUtilForStreetVideo mMediaRecorderUtilForStreetVideo;
    private String mFilename;

    private SensorManager mSensorManager;
    private final ConcurrentLinkedQueue<SimpleSensorEvent> mAccelerometerEventQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SimpleSensorEvent> mGyroscopeEventQueue = new ConcurrentLinkedQueue<>();
    /**
     * Audio Extra Codec Processor.
     * Such as: audio noise reduction.
     */
    private List<IAudioEncoderExt> mAudioEncoderExtList;
    private IAudioRecordExt mAudioRecordExt;
    /**
     * Whether panoramic sound (4 channels).
     */
    private boolean isSpatialAudio;

    private Thread mVideoThread;
    private Thread mAudioThread;
    private Thread mFourAudioThread;
    private SensorEventListener mSensorEventListener;

    /**
     * Set the audio extra encoding processor,
     * It should be set before starting recording.
     */
    public void setAudioEncoderExt(List<IAudioEncoderExt> list) {
        mAudioEncoderExtList = list;
    }

    public void setAudioRecordExt(IAudioRecordExt audioRecordExt) {
        mAudioRecordExt = audioRecordExt;
    }

    public String getFilename() {
        return mFilename;
    }

    public Surface startRecord(Context context, String filename, String mime, int width, int height,
                               int fps, int bitRate, boolean useForGoogleMap,
                               int memomotionRatio, int channelCount, int previewFps) {
        if (useForGoogleMap) {
            mMediaRecorderUtilForStreetVideo = new MediaRecorderUtilForStreetVideo();
            return mMediaRecorderUtilForStreetVideo.startRecord(context, filename, mime, width, height, fps, bitRate, useForGoogleMap, memomotionRatio, channelCount, previewFps);
        }
        if (filename.length() == 0 || width == 0 || height == 0 || bitRate == 0) {
            Log.e(TAG, "startRecord parameter error!");
            return null;
        }
        if (filename.toLowerCase().endsWith(".mp4")) {
            try {
                File f = new File(filename);
                if (f.exists()) {
                    f.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mFilename = filename;
        mMemomotionRatio = memomotionRatio;
        mUseForGoogleMap = useForGoogleMap;

        if (mMemomotionRatio != 0) {
            float deltaTimestamp = 1.0f / previewFps * Math.abs(mMemomotionRatio);
            mMemomotionTimeMulti = (1.0f / 30.0f) / deltaTimestamp;
        }

        MediaFormat videoFormat = MediaFormat.createVideoFormat(mime, width, height);
        initVideoFormat(videoFormat, width, height, fps, bitRate, previewFps, mUseForGoogleMap, mMemomotionRatio);

        Surface surface;
        try {
            mMediaMuxer = new MediaMuxer(filename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoEncoder = MediaCodec.createEncoderByType(mime);
            mVideoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mVideoEncoder.createInputSurface();
            initAudioRecord(channelCount, memomotionRatio);
            mVideoEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (useForGoogleMap) {
            registerGoogleMapData(context);
        }
        mRun = true;
        if (mAudioEncoder != null) {
            if (isSpatialAudio) {
                initFourAudioThread();
                mFourAudioThread.start();
            } else {
                initAudioThread();
                mAudioThread.start();
            }
        }
        initVideoThread();
        mVideoThread.start();
        return surface;
    }

    public void stopRecord(boolean injectPanoMetadata, boolean isPano, String firmware, String artist) {
        if (mMediaRecorderUtilForStreetVideo != null) {
            mMediaRecorderUtilForStreetVideo.stopRecord(injectPanoMetadata, isPano, firmware, artist);
            return;
        }
        mRun = false;
        try {
            mVideoThread.join();
            if (mAudioThread != null) {
                mAudioThread.join();
            }
            if (mFourAudioThread != null) {
                mFourAudioThread.join();
            }
            if (mMediaMuxer != null) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
        if (mFilename != null && injectPanoMetadata) {
            PiPano.spatialMediaImpl(mFilename, isPano, isSpatialAudio, firmware, artist, mMemomotionTimeMulti);
        }
    }

    /**
     * 设置gps信息,gps信息将用于写入视频
     */
    static void setLocationInfo(Location location) {
        synchronized (mLocationLock) {
            mLocation = location;
        }
        MediaRecorderUtilForStreetVideo.setLocationInfo(location);
    }

    static class SimpleSensorEvent {
        long timestamp;
        float x, y, z;

        SimpleSensorEvent(long t, float x, float y, float z) {
            timestamp = t;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private void initVideoThread() {
        mVideoThread = new Thread() {
            private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

            final ByteBuffer mCammData = ByteBuffer.allocate(64);
            final MediaCodec.BufferInfo mCammInfo = new MediaCodec.BufferInfo();
            long mLastTimestamp = 0;

            private final byte[] mBitToLittleShortBuffer = new byte[2];
            private final byte[] mBitToLittleIntBuffer = new byte[4];
            private final byte[] mBitToLittleFloatBuffer = new byte[4];
            private final byte[] mBitToLittleDoubleBuffer = new byte[8];

            private byte[] BitToLittleShort(short s) {
                mBitToLittleShortBuffer[1] = (byte) (s >> 8);
                mBitToLittleShortBuffer[0] = (byte) (s);
                return mBitToLittleShortBuffer;
            }

            private byte[] BitToLittleInt(int i) {
                mBitToLittleIntBuffer[3] = (byte) (i >> 24);
                mBitToLittleIntBuffer[2] = (byte) (i >> 16);
                mBitToLittleIntBuffer[1] = (byte) (i >> 8);
                mBitToLittleIntBuffer[0] = (byte) (i);
                return mBitToLittleIntBuffer;
            }

            private byte[] BitToLittleFloat(float f) {
                int n = Float.floatToIntBits(f);
                mBitToLittleFloatBuffer[3] = (byte) (n >> 24);
                mBitToLittleFloatBuffer[2] = (byte) (n >> 16);
                mBitToLittleFloatBuffer[1] = (byte) (n >> 8);
                mBitToLittleFloatBuffer[0] = (byte) (n);
                return mBitToLittleFloatBuffer;
            }

            private byte[] BitToLittleDouble(double f) {
                long n = Double.doubleToLongBits(f);
                mBitToLittleDoubleBuffer[7] = (byte) (n >> 56);
                mBitToLittleDoubleBuffer[6] = (byte) (n >> 48);
                mBitToLittleDoubleBuffer[5] = (byte) (n >> 40);
                mBitToLittleDoubleBuffer[4] = (byte) (n >> 32);
                mBitToLittleDoubleBuffer[3] = (byte) (n >> 24);
                mBitToLittleDoubleBuffer[2] = (byte) (n >> 16);
                mBitToLittleDoubleBuffer[1] = (byte) (n >> 8);
                mBitToLittleDoubleBuffer[0] = (byte) (n);
                return mBitToLittleDoubleBuffer;
            }

            SimpleSensorEvent getSensorEventByTime(Queue<SimpleSensorEvent> queue, long timeStamp) {
                SimpleSensorEvent lastEvent = queue.poll();
                if (lastEvent == null) {
                    return null;
                }
                while (true) {
                    SimpleSensorEvent event = queue.poll();
                    if (event == null) {
                        return lastEvent;
                    }
                    if (lastEvent.timestamp >= timeStamp) {
                        return lastEvent;
                    } else if (event.timestamp > timeStamp) {
                        return Math.abs(lastEvent.timestamp - timeStamp) <
                                Math.abs(event.timestamp - timeStamp) ? lastEvent : event;
                    }
                    lastEvent = event;
                }
            }

            int mFrame = 0;

            @Override
            public void run() {
                mFirstFrameTimestamp = -1;

                while (mRun) {
                    encodeVideoFrame();
                }

                try {
                    mVideoEncoder.stop();
                    mVideoEncoder.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Encoder video thread finish");
            }

            private void encodeVideoFrame() {
                int outputIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = mVideoEncoder.getOutputFormat();
                    mVideoEncoderTrack = mMediaMuxer.addTrack(format);
                    if (mAudioEncoder != null) {
                        int waitTime = 30;
                        while (mAudioEncoderTrack == -1 && waitTime > 0) {
                            SystemClock.sleep(100);
                            waitTime--;
                        }
                    }
                    mMediaMuxer.start();
                    mIsMediaMuxerStart = true;
                    outputIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                    Log.i(TAG, "Encoder video INFO_OUTPUT_FORMAT_CHANGED");
                }

                if (outputIndex >= 0) {
                    ByteBuffer buffer = mVideoEncoder.getOutputBuffer(outputIndex);
                    if (mBufferInfo.size > 0 && buffer != null) {
                        if (mBufferInfo.presentationTimeUs > 0) {
                            if (mFirstFrameTimestamp < 0) {
                                mFirstFrameTimestamp = mBufferInfo.presentationTimeUs;
                                Log.i(TAG, "first frame timestamp: " + mFirstFrameTimestamp +
                                        " current timestamp with deep sleep: " + SystemClock.elapsedRealtime() +
                                        " current timestamp: " + SystemClock.uptimeMillis() + " " + mFilename);
                            }
                            //延时摄影要改变时间戳,是的延时摄影按照30fps播放,而街景视频无需改变时间戳
                            //慢动作按30fps编码，修改时间戳
                            if (mMemomotionRatio != 0 && !mUseForGoogleMap) {
                                mBufferInfo.presentationTimeUs = mFirstFrameTimestamp +
                                        (long) ((mBufferInfo.presentationTimeUs - mFirstFrameTimestamp) * mMemomotionTimeMulti);
                                if (mBufferInfo.presentationTimeUs - mLastTimestamp > 40000) {
                                    Log.e(TAG, "drop frame:" + mBufferInfo.presentationTimeUs +
                                            ", delta time:" + (mBufferInfo.presentationTimeUs - mLastTimestamp));
                                }
                                mLastTimestamp = mBufferInfo.presentationTimeUs;
                            }
                            mMediaMuxer.writeSampleData(mVideoEncoderTrack, buffer, mBufferInfo);

                            if (mCammEncoderTrack >= 0) {
                                //GPS信息
                                writeGpsData();

                                long cameraTimestamp = (SystemClock.elapsedRealtime() - 420) * 1000000;//mBufferInfo.presentationTimeUs*1000
                                //加速度
                                SimpleSensorEvent accelerometerEvent = getSensorEventByTime(mAccelerometerEventQueue, cameraTimestamp);
                                writeAccelerometerData(accelerometerEvent, 3);
                                //陀螺仪
                                SimpleSensorEvent gyroscopeEvent = getSensorEventByTime(mGyroscopeEventQueue, cameraTimestamp);
                                writeAccelerometerData(gyroscopeEvent, 2);
                            }
                        } else {
                            Log.e(TAG, "Video sample time must > 0");
                        }
                    }
                    mVideoEncoder.releaseOutputBuffer(outputIndex, false);
                    mFrame++;
                }
            }

            private void writeAccelerometerData(SimpleSensorEvent accelerometerEvent, int type) {
                if (accelerometerEvent != null) {
                    mCammData.clear();
                    mCammData.putShort((short) 0);
                    mCammData.put(BitToLittleShort((short) type));
                    mCammData.put(BitToLittleFloat(accelerometerEvent.x));
                    mCammData.put(BitToLittleFloat(-accelerometerEvent.z));
                    mCammData.put(BitToLittleFloat(accelerometerEvent.y));
                    mCammInfo.set(0, mCammData.position(), mBufferInfo.presentationTimeUs, 0);
                    mMediaMuxer.writeSampleData(mCammEncoderTrack, mCammData, mCammInfo);
                }
            }

            private void writeGpsData() {
                synchronized (mLocationLock) {
                    if (mLocation != null) {
                        mCammData.clear();
                        mCammData.putShort((short) 0);
                        mCammData.put(BitToLittleShort((short) 6));
                        //计算gps时间
                        double timeGpsEpoch =
                                (System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 1000.0 //系统启动时间UTC
                                        + mLocation.getElapsedRealtimeNanos() / 1000000000.0   //上次定位的时间
                                        - 315964800 //UTC时间和GPS时间的转换
                                        + 18;   //UTC时间和GPS时间的转换
                        mCammData.put(BitToLittleDouble(timeGpsEpoch));
                        mCammData.put(BitToLittleInt(3));
                        mCammData.put(BitToLittleDouble(mLocation.getLatitude()));
                        mCammData.put(BitToLittleDouble(mLocation.getLongitude()));
                        mCammData.put(BitToLittleFloat((float) mLocation.getAltitude()));
                        mCammData.put(BitToLittleFloat(mLocation.getAccuracy()));
                        mCammData.put(BitToLittleFloat(mLocation.getAccuracy()));//mLocation.getVerticalAccuracyMeters() 需要level26
                        float speed = mLocation.getSpeed();
                        float bearing = mLastBearing;
                        if (mLocation.hasBearing()) {
                            bearing = mLocation.getBearing() / 180.0f * 3.1415927f;
                            mLastBearing = bearing;
                        }
                        mCammData.put(BitToLittleFloat(bearing >= 0 ? speed * (float) Math.sin(bearing) : 0));  //velocity_east
                        mCammData.put(BitToLittleFloat(bearing >= 0 ? speed * (float) Math.cos(bearing) : 0)); //velocity_north
                        mCammData.put(BitToLittleFloat(0)); //velocity_up
                        mCammData.put(BitToLittleFloat(0));//mLocation.getSpeedAccuracyMetersPerSecond() 需要level26
                        mCammInfo.set(0, mCammData.position(), mBufferInfo.presentationTimeUs, 0);
                        mMediaMuxer.writeSampleData(mCammEncoderTrack, mCammData, mCammInfo);
                    }
                    mLocation = null;
                }
            }
        };
    }

    private void initAudioThread() {
        mAudioThread = new Thread() {
            private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            private byte[] mAudioBufferExt;
            private long presentationTimeUs;

            @Override
            public void run() {
                if (mAudioEncoderExtList != null) {
                    mAudioBufferExt = new byte[mAudioBuffer.length];
                }
                presentationTimeUs = 0;

                while (mRun) {
                    encodeAudioFrame();
                }

                try {
                    if (mAudioRecordExt != null) {
                        mAudioRecordExt.stop();
                        mAudioRecordExt.release();
                    }
                    mAudioEncoder.stop();
                    mAudioEncoder.release();
                    if (mAudioEncoderExtList != null) {
                        for (IAudioEncoderExt ext : mAudioEncoderExtList) {
                            ext.release();
                        }
                        mAudioEncoderExtList.clear();
                        mAudioEncoderExtList = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Encoder audio thread finish");
            }

            private void encodeAudioFrame() {
                ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
                int inputIndex = mAudioEncoder.dequeueInputBuffer(0);
                if (inputIndex >= 0) {
                    int readCount = mAudioRecordExt.read(mAudioBuffer, 0, mAudioBuffer.length); // read audio raw data
                    if (readCount < 0) {
                        return;
                    }
                    final long pts = System.nanoTime() / 1000;
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];
                    inputBuffer.clear();
                    if (mAudioEncoderExtList != null && mAudioEncoderExtList.size() > 0) {
                        byte[] input = mAudioBuffer;
                        byte[] outPut = mAudioBufferExt;
                        byte[] temp;
                        int size = mAudioEncoderExtList.size();
                        for (int i = 0; i < size; i++) {
                            IAudioEncoderExt ext = mAudioEncoderExtList.get(i);
                            int count = ext.encodeProcess(input, readCount, outPut);
                            if (count != -1) {
                                readCount = count;
                                if (i != size - 1) {
                                    temp = outPut;
                                    outPut = input;
                                    input = temp;
                                }
                            }
                        }
                        inputBuffer.put(outPut, 0, readCount);
                    } else {
                        inputBuffer.put(mAudioBuffer);
                    }
                    mAudioEncoder.queueInputBuffer(inputIndex, 0, readCount, pts, 0);
                }

                int outputIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = mAudioEncoder.getOutputFormat();
                    mAudioEncoderTrack = mMediaMuxer.addTrack(format);
                    outputIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                    Log.i(TAG, "Encoder audio INFO_OUTPUT_FORMAT_CHANGED");
                }

                if (outputIndex >= 0) {
                    ByteBuffer buffer = mAudioEncoder.getOutputBuffer(outputIndex);
                    if (mBufferInfo.size > 0 && buffer != null) {
                        if (mBufferInfo.presentationTimeUs > 0) {
                            if (presentationTimeUs > mBufferInfo.presentationTimeUs) {
                                Log.e(TAG, "Audio sample time is not incremental,ignore this ByteBuffer.");
                            } else {
                                if (mIsMediaMuxerStart) {
                                    mMediaMuxer.writeSampleData(mAudioEncoderTrack, buffer, mBufferInfo);
                                }
                                presentationTimeUs = mBufferInfo.presentationTimeUs;
                            }
                        } else {
                            Log.e(TAG, "Audio sample time must > 0");
                        }
                    }
                    mAudioEncoder.releaseOutputBuffer(outputIndex, false);
                }
//            else if(outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                Log.i(TAG,"Encoder INFO_TRY_AGAIN_LATER");
//            }
            }
        };
    }

    private void initFourAudioThread() {
        mFourAudioThread = new Thread() {
            private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            private byte[] mAudioBufferExt;
            private long presentationTimeUs;

            @Override
            public void run() {
                if (null != mAudioEncoderExtList) {
                    mAudioBufferExt = new byte[mAudioBuffer.length];
                }
                presentationTimeUs = 0;
                while (mRun) {
                    encodeAudioFrame();
                }
                try {
                    if (mAudioRecordExt != null) {
                        mAudioRecordExt.stop();
                        mAudioRecordExt.release();
                    }
                    mAudioEncoder.stop();
                    mAudioEncoder.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mAudioEncoderExtList != null) {
                    for (IAudioEncoderExt ext : mAudioEncoderExtList) {
                        ext.release();
                    }
                    mAudioEncoderExtList.clear();
                    mAudioEncoderExtList = null;
                }
                Log.i(TAG, "Encoder audio thread finish");
            }

            private void encodeAudioFrame() {
                ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
                int inputIndex = mAudioEncoder.dequeueInputBuffer(0);
                if (inputIndex >= 0) {
                    int readCount = -1;
                    if (mAudioRecordExt != null) {
                        readCount = mAudioRecordExt.read(mAudioBuffer, 0, mAudioBuffer.length); // read audio raw data
                    }
                    if (readCount < 0) {
                        return;
                    }
                    final long pts = System.nanoTime() / 1000;
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];
                    inputBuffer.clear();
                    if (mAudioEncoderExtList != null && mAudioEncoderExtList.size() > 0) {
                        byte[] input = mAudioBuffer;
                        byte[] outPut = mAudioBufferExt;
                        byte[] temp;
                        int size = mAudioEncoderExtList.size();
                        for (int i = 0; i < size; i++) {
                            IAudioEncoderExt ext = mAudioEncoderExtList.get(i);
                            int count = ext.encodeProcess(input, readCount, outPut);
                            if (count != -1) {
                                readCount = count;
                                if (i != size - 1) {
                                    temp = outPut;
                                    outPut = input;
                                    input = temp;
                                }
                            }
                        }
                        inputBuffer.put(outPut, 0, readCount);
                    } else {
                        inputBuffer.put(mAudioBuffer);
                    }
                    mAudioEncoder.queueInputBuffer(inputIndex, 0, readCount, pts, 0);
                }

                int outputIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = mAudioEncoder.getOutputFormat();
                    mAudioEncoderTrack = mMediaMuxer.addTrack(format);
                    outputIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                    Log.i(TAG, "Encoder audio INFO_OUTPUT_FORMAT_CHANGED");
                }

                if (outputIndex >= 0) {
                    ByteBuffer buffer = mAudioEncoder.getOutputBuffer(outputIndex);
                    if (mBufferInfo.size > 0 && buffer != null) {
                        if (mBufferInfo.presentationTimeUs > 0) {
                            if (presentationTimeUs > mBufferInfo.presentationTimeUs) {
                                Log.e(TAG, "Audio sample time is not incremental,ignore this ByteBuffer.");
                            } else {
                                if (mIsMediaMuxerStart) {
                                    mMediaMuxer.writeSampleData(mAudioEncoderTrack, buffer, mBufferInfo);
                                }
                                presentationTimeUs = mBufferInfo.presentationTimeUs;
                            }
                        } else {
                            Log.e(TAG, "Audio sample time must > 0");
                        }
                    }
                    mAudioEncoder.releaseOutputBuffer(outputIndex, false);
                }
            }
        };
    }

    private void initSensorListener() {
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        mAccelerometerEventQueue.offer(new SimpleSensorEvent(
                                event.timestamp, event.values[0], event.values[1], event.values[2]));
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        mGyroscopeEventQueue.offer(new SimpleSensorEvent(
                                event.timestamp, event.values[0], event.values[1], event.values[2]));
                        break;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    private void registerGoogleMapData(Context context) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "application/gyro");
        mCammEncoderTrack = mMediaMuxer.addTrack(format);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorEventListener == null) {
            initSensorListener();
        }
        mSensorManager.registerListener(mSensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mSensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
    }

    private static void initVideoFormat(MediaFormat videoFormat, int width, int height,
                                        int fps, int bitRate, int previewFps,
                                        boolean useForGoogleMap, long memomotionRatio) {
        //视频编码器
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
//        videoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        //rc mode 0-RC Off 1-VBR_CFR 2-CBR_CFR 3-VBR_VFR 4-CBR_VFR 5-MBR_CFR 6_MBR_VFR
        videoFormat.setInteger("vendor.qti-ext-enc-bitrate-mode.value", 1);
        // Real Time Priority
        videoFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        // Enable hier-p for resolution greater than 5.7k
        if (width >= 5760 && height >= 2880) {
            videoFormat.setString(MediaFormat.KEY_TEMPORAL_LAYERING, "android.generic.2");
        }
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-min", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-max", 51);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-min", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-max", 51);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-min", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-max", 51);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-i-enable", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-i", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-b-enable", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-b", 10);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-p-enable", 1);
        videoFormat.setInteger("vendor.qti-ext-enc-initial-qp.qp-p", 10);

        int interval_iframe = 1;
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, interval_iframe);
        // Calculate P frames based on FPS and I frame Interval
        int pFrameCount = (fps * interval_iframe) - 1;
        if (pFrameCount < 1) {
            pFrameCount = 1; // p帧不能小于1
        }
        videoFormat.setInteger("vendor.qti-ext-enc-intra-period.n-pframes", pFrameCount);
        // Always set B frames to 0.
        videoFormat.setInteger("vendor.qti-ext-enc-intra-period.n-bframes", 0);
        videoFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);

        if (memomotionRatio > 0 && !useForGoogleMap) {
            videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-i-min", 20);
            videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-b-min", 20);
            videoFormat.setInteger("vendor.qti-ext-enc-qp-range.qp-p-min", 20);
            videoFormat.setInteger("vendor.qti-ext-enc-intra-period.n-pframes", 2);
        } else if (memomotionRatio < 0) {
            // 慢动作时，使用预览帧率
            videoFormat.setInteger(MediaFormat.KEY_OPERATING_RATE, previewFps);
        }
    }

    private void initAudioRecord(int channelCount, int memomotionRatio) throws IOException {
        if (!mUseForGoogleMap && memomotionRatio == 0 && channelCount > 0) {
            if (mAudioRecordExt == null) {
                mAudioRecordExt = new DefaultAudioRecordExt();
            }
            MediaFormat audioFormat;
            int sampleRateInHz;
            int bitRate;
            if (channelCount == CHANNEL_FOUR) {
                sampleRateInHz = 48000;
                bitRate = 256000;
            } else {
                sampleRateInHz = 44100;
                bitRate = 128000;
            }
            int channelConfig = (channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO
                    : AudioFormat.CHANNEL_IN_STEREO);
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
            mAudioBuffer = new byte[4096];

            if (channelCount == CHANNEL_FOUR) {
                channelConfig = AudioFormat.CHANNEL_IN_STEREO | AudioFormat.CHANNEL_IN_FRONT | AudioFormat.CHANNEL_IN_BACK;
            }
            mAudioRecordExt.configure(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig,
                    audioEncoding, Math.max(minBufferSize, mAudioBuffer.length));
            audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelCount);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
            if (mAudioEncoderExtList != null) {
                for (IAudioEncoderExt ext : mAudioEncoderExtList) {
                    ext.configure(sampleRateInHz, channelCount, audioEncoding);
                }
            }
            if (channelCount == CHANNEL_FOUR) {
                mAudioEncoder = MediaCodec.createByCodecName("OMX.google.aac.encoder");
                isSpatialAudio = true;
            } else {
                mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            }
            mAudioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioRecordExt.startRecording();
            mAudioEncoder.start();
        }
    }
}
