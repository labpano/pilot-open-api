package com.pi.pano;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.media.MediaRecorder;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MediaRecorder Util
 */
public class MediaRecorderUtil {
    private static final String TAG = MediaRecorderUtil.class.getSimpleName();

    public static final int VIDEO_FRAME_RATE_0_3FPS = 24;
    public static final int VIDEO_FRAME_RATE_1FPS = 7;
    public static final int VIDEO_FRAME_RATE_2FPS = 4;
    public static final int VIDEO_FRAME_RATE_3FPS = 3;
    public static final int VIDEO_FRAME_RATE_4FPS = 2;
    public static final int VIDEO_FRAME_RATE_7FPS = 0;

    private static Location mLocation;
    private static final Object mLocationLock = new Object();

    private AudioRecord mAudioRecord;
    private byte[] mAudioBuffer;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private MediaMuxer mMediaMuxer;

    private int mVideoEncoderTrack = -1;
    private int mAudioEncoderTrack = -1;
    private int mCammEncoderTrack = -1;
    private volatile boolean mRun;
    private static final Object mLock = new Object();
    private float mLastBearing = -1;
    private long mLastFrameTimestamp = -1;
    private long mMemomotionRatio = 0;
    private boolean mUseForGoogleMap = false;
    private volatile float mFrameRate = 7;

    private SensorManager mSensorManager;
    private ConcurrentLinkedQueue<SimpleSensorEvent> mAccelerometerEventQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<SimpleSensorEvent> mGyroscopeEventQueue = new ConcurrentLinkedQueue<>();

    class SimpleSensorEvent {
        long timestamp;
        float x, y, z;

        SimpleSensorEvent(long t, float x, float y, float z) {
            timestamp = t;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerometerEventQueue.offer(new SimpleSensorEvent(event.timestamp, event.values[0], event.values[1], event.values[2]));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    mGyroscopeEventQueue.offer(new SimpleSensorEvent(event.timestamp, event.values[0], event.values[1], event.values[2]));
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    int startRecord(String filename, String mime, int width, int height, int bitRate, boolean useForGoogleMap,
                    int memomotionRatio, Activity activity, int channelCount) {
        Log.i(TAG, "startRecord start==> filename:" + filename + " height:" + height +
                " width:" + width + " bitRate:" + bitRate + " useForGoogleMap:" + useForGoogleMap +
                " memomotionRatio:" + memomotionRatio + " mime:" + mime + " channelCount:" + channelCount);
        if (filename.length() == 0 || width == 0 || height == 0 || bitRate == 0) {
            Log.e(TAG, "初始化状态出错");
            return 2;
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

        mMemomotionRatio = memomotionRatio;
        mUseForGoogleMap = useForGoogleMap;

        if (useForGoogleMap) {
            if (memomotionRatio == VIDEO_FRAME_RATE_0_3FPS) {
                mFrameRate = 0.3f;
            } else if (memomotionRatio == VIDEO_FRAME_RATE_1FPS) {
                mFrameRate = 1;
            } else if (memomotionRatio == VIDEO_FRAME_RATE_2FPS) {
                mFrameRate = 2;
            } else if (memomotionRatio == VIDEO_FRAME_RATE_3FPS) {
                mFrameRate = 3;
            } else if (memomotionRatio == VIDEO_FRAME_RATE_4FPS) {
                mFrameRate = 4;
            } else if (memomotionRatio == VIDEO_FRAME_RATE_7FPS) {
                mFrameRate = 7;
            }
        }

        int sampleRateInHz = 44100;
        int channelConfig = (channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
        mAudioBuffer = new byte[4096];

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig,
                audioEncoding, Math.max(minBufferSize, mAudioBuffer.length));

        //Video encoder
        MediaFormat videoFormat = MediaFormat.createVideoFormat(mime, width, height);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        MediaFormat audioFormat = null;
        if (!useForGoogleMap && memomotionRatio == 0) {
            //Audio encoder
            audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelCount);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        }

        try {
            mMediaMuxer = new MediaMuxer(filename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mVideoEncoder = MediaCodec.createEncoderByType(mime);
            mVideoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            PiPano.setMemomotionRatio(memomotionRatio);
            PilotSDK.setEncodeInputSurfaceForVideo(mVideoEncoder.createInputSurface());
            mVideoEncoder.start();

            if (audioFormat != null) {
                mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                mAudioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mAudioRecord.startRecording();
                mAudioEncoder.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 3;
        }

        if (useForGoogleMap) {
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, "application/gyro");
            mCammEncoderTrack = mMediaMuxer.addTrack(format);

            mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(mSensorEventListener, accelerSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Sensor gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(mSensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        Log.e(TAG, "------------------------mRun01>" + System.currentTimeMillis() + "");
        mRun = true;

        if (audioFormat == null) {
            mVideoThread.start();
        } else {
            mAudioThread.start();
        }
        return 0;
    }

    static void setLocationInfo(Location location) {
        synchronized (mLocationLock) {
            mLocation = location;
        }
    }

    private Thread mVideoThread = new Thread() {
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

        ByteBuffer mCammData = ByteBuffer.allocate(64);
        MediaCodec.BufferInfo mCammInfo = new MediaCodec.BufferInfo();

        private byte[] mBitToLittleShortBuffer = new byte[2];
        private byte[] mBitToLittleIntBuffer = new byte[4];
        private byte[] mBitToLittleFloatBuffer = new byte[4];
        private byte[] mBitToLittleDoubleBuffer = new byte[8];

        private long mFrameNumber = 0L;

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

        @Override
        public void run() {
            mLastFrameTimestamp = -1;
            Log.e(TAG, "mRun------------------------>" + mRun);

            while (mRun) {
                encordeVideoFrame();
            }

            try {
                mVideoEncoder.stop();
                mVideoEncoder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "encorder video thread finish");
        }

        private void encordeVideoFrame() {
            int outputIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mVideoEncoder.getOutputFormat();
                mVideoEncoderTrack = mMediaMuxer.addTrack(format);
                mMediaMuxer.start();
                synchronized (mLock) {
                    mLock.notifyAll();
                    Log.i(TAG, "DIE cicle");
                }
                outputIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                Log.i(TAG, "Encorder video INFO_OUTPUT_FORMAT_CHANGED");
            }

            if (outputIndex >= 0) {
                ByteBuffer buffer = mVideoEncoder.getOutputBuffer(outputIndex);
                if (mBufferInfo.size > 0 && buffer != null) {
                    if (mBufferInfo.presentationTimeUs > 0) {
                        if (mLastFrameTimestamp == -1) {
                            mLastFrameTimestamp = mBufferInfo.presentationTimeUs;
                        }
                        if (mMemomotionRatio > 0) {
                            mBufferInfo.presentationTimeUs = mLastFrameTimestamp + 33333;
                            mLastFrameTimestamp = mBufferInfo.presentationTimeUs;
                        }
                        if (mUseForGoogleMap) {
                            mFrameNumber++;
                            mBufferInfo.presentationTimeUs = getPTSUs(mFrameNumber);
                        }
                        mMediaMuxer.writeSampleData(mVideoEncoderTrack, buffer, mBufferInfo);

                        if (mCammEncoderTrack >= 0) {
                            //GPS信息
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

                            long cameraTimestamp = (SystemClock.elapsedRealtime() - 420) * 1000000;//mBufferInfo.presentationTimeUs*1000

                            //加速度
                            SimpleSensorEvent accelerometerEvent = getSensorEventByTime(mAccelerometerEventQueue, cameraTimestamp);
                            if (accelerometerEvent != null) {
                                mCammData.clear();
                                mCammData.putShort((short) 0);
                                mCammData.put(BitToLittleShort((short) 3));
                                mCammData.put(BitToLittleFloat(accelerometerEvent.x));
                                mCammData.put(BitToLittleFloat(-accelerometerEvent.z));
                                mCammData.put(BitToLittleFloat(accelerometerEvent.y));
                                mCammInfo.set(0, mCammData.position(), mBufferInfo.presentationTimeUs, 0);
                                mMediaMuxer.writeSampleData(mCammEncoderTrack, mCammData, mCammInfo);
                            }

                            //陀螺仪
                            SimpleSensorEvent gyroscopeEvent = getSensorEventByTime(mGyroscopeEventQueue, cameraTimestamp);
                            if (gyroscopeEvent != null) {
                                mCammData.clear();
                                mCammData.putShort((short) 0);
                                mCammData.put(BitToLittleShort((short) 2));
                                mCammData.put(BitToLittleFloat(gyroscopeEvent.x));
                                mCammData.put(BitToLittleFloat(-gyroscopeEvent.z));
                                mCammData.put(BitToLittleFloat(gyroscopeEvent.y));
                                mCammInfo.set(0, mCammData.position(), mBufferInfo.presentationTimeUs, 0);
                                mMediaMuxer.writeSampleData(mCammEncoderTrack, mCammData, mCammInfo);
                            }
                        }
                    } else {
                        Log.e(TAG, "Video sample time must > 0");
                    }
                }
                mVideoEncoder.releaseOutputBuffer(outputIndex, false);
            }
        }
    };

    private Thread mAudioThread = new Thread() {
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

        private long presentationTimeUs;

        @Override
        public void run() {
            presentationTimeUs = 0;
            while (mRun) {
                encordeAudioFrame();
            }
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioEncoder.stop();
                mAudioEncoder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "encorder audio thread finish");
        }

        private void encordeAudioFrame() {
            ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
            int inputIndex = mAudioEncoder.dequeueInputBuffer(0);
            if (inputIndex >= 0) {
                int readCount = mAudioRecord.read(mAudioBuffer, 0, mAudioBuffer.length); // read audio raw data
                if (readCount < 0) {
                    return;
                }

                ByteBuffer inputBuffer = inputBuffers[inputIndex];
                inputBuffer.clear();
                inputBuffer.put(mAudioBuffer);
                mAudioEncoder.queueInputBuffer(inputIndex, 0, mAudioBuffer.length,
                        System.nanoTime() / 1000, 0);
            }

            int outputIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mAudioEncoder.getOutputFormat();
                mAudioEncoderTrack = mMediaMuxer.addTrack(format);
                outputIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                Log.i(TAG, "Encorder audio INFO_OUTPUT_FORMAT_CHANGED");

                mVideoThread.start();
                synchronized (mLock) {
                    while (mVideoEncoderTrack == -1) {
                        Log.i(TAG, "Encorder audio Dead cycle");
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.i(TAG, "Encorder audio Dead cycle" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (outputIndex >= 0) {
                ByteBuffer buffer = mAudioEncoder.getOutputBuffer(outputIndex);
                if (mBufferInfo.size > 0 && buffer != null) {
                    if (mBufferInfo.presentationTimeUs > 0) {
                        if (presentationTimeUs > mBufferInfo.presentationTimeUs) {
                            Log.e(TAG, "Audio sample time is not incremental,ignore this ByteBuffer.");
                        } else {
                            mMediaMuxer.writeSampleData(mAudioEncoderTrack, buffer, mBufferInfo);
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

    void stopRecord() {
        PilotSDK.setEncodeInputSurfaceForVideo(null);
        mRun = false;

        Log.e(TAG, "------------------------Thread.activeCount()>" + Thread.activeCount() + "");

        Log.e(TAG, "------------------------mRun02>" + System.currentTimeMillis() + "");
        try {
            Log.e(TAG, System.currentTimeMillis() + "");
            mVideoThread.join();
            Log.e(TAG, "------------------------time0>" + System.currentTimeMillis() + "");
            mAudioThread.join();
            Log.e(TAG, "------------------------time1>" + System.currentTimeMillis() + "");
            if (mMediaMuxer != null) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            }

            Log.e(TAG, "------------------------time2>" + System.currentTimeMillis() + "");
        } catch (Exception e) {
            Log.e(TAG, "------------------------message>" + e.getMessage());
            e.printStackTrace();
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
    }

    protected long getPTSUs(final long frameNum) {
        return (long) (frameNum * 1000_000L / mFrameRate);
    }
}
