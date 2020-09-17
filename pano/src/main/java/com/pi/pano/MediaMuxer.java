package com.pi.pano;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

final public class MediaMuxer {
    /**
     * Defines the output format. These constants are used with constructor.
     */
    public static final class OutputFormat {
        /* Do not change these values without updating their counterparts
         * in include/media/stagefright/MediaMuxer.h!
         */
        private OutputFormat() {
        }

        /**
         * MPEG4 media file format
         */
        static final int MUXER_OUTPUT_MPEG_4 = 0;
        /**
         * WEBM media file format
         */
        static final int MUXER_OUTPUT_WEBM = 1;
        /**
         * 3GPP media file format
         */
        static final int MUXER_OUTPUT_3GPP = 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface Format {
    }

    private static native long nativeSetup(@NonNull FileDescriptor fd, int format)
            throws IllegalArgumentException, IOException;

    private static native void nativeRelease(long nativeObject);

    private static native void nativeStart(long nativeObject);

    private static native void nativeStop(long nativeObject);

    private static native int nativeAddTrack(
            long nativeObject, @NonNull String[] keys, @NonNull Object[] values);

    private static native void nativeSetOrientationHint(
            long nativeObject, int degrees);

    private static native void nativeSetLocation(long nativeObject, int latitude, int longitude);

    private static native void nativeWriteSampleData(
            long nativeObject, int trackIndex, @NonNull ByteBuffer byteBuf,
            int offset, int size, long presentationTimeUs, int flags);

    private static final int MUXER_STATE_UNINITIALIZED = -1;
    private static final int MUXER_STATE_INITIALIZED = 0;
    private static final int MUXER_STATE_STARTED = 1;
    private static final int MUXER_STATE_STOPPED = 2;

    private int mState = MUXER_STATE_UNINITIALIZED;

    private int mLastTrackIndex = -1;

    private long mNativeObject;

    /**
     * Constructor.
     * Creates a media muxer that writes to the specified path.
     *
     * @param path   The path of the output media file.
     * @param format The format of the output media file.
     * @throws IllegalArgumentException if path is invalid or format is not supported.
     * @throws IOException              if failed to open the file for write.
     * @see android.media.MediaMuxer.OutputFormat
     */
    MediaMuxer(String path, @Format int format) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        // Use RandomAccessFile so we can open the file with RW access;
        // RW access allows the native writer to memory map the output file.
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(path, "rws");
            FileDescriptor fd = file.getFD();
            setUpMediaMuxer(fd, format);
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

    /**
     * Constructor.
     * Creates a media muxer that writes to the specified FileDescriptor. File descriptor
     * must be seekable and writable. Application should not use the file referenced
     * by this file descriptor until {@link #stop}. It is the application's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd     The FileDescriptor of the output media file.
     * @param format The format of the output media file.
     * @throws IllegalArgumentException if fd is invalid or format is not supported.
     * @throws IOException              if failed to open the file for write.
     * @see android.media.MediaMuxer.OutputFormat
     */
    MediaMuxer(@NonNull FileDescriptor fd, @Format int format) throws IOException {
        setUpMediaMuxer(fd, format);
    }

    private void setUpMediaMuxer(@NonNull FileDescriptor fd, @Format int format) throws IOException {
        if (format != OutputFormat.MUXER_OUTPUT_MPEG_4 && format != OutputFormat.MUXER_OUTPUT_WEBM
                && format != OutputFormat.MUXER_OUTPUT_3GPP) {
            throw new IllegalArgumentException("format: " + format + " is invalid");
        }
        mNativeObject = nativeSetup(fd, format);
        mState = MUXER_STATE_INITIALIZED;
        //mCloseGuard.open("release");
    }

    /**
     * Sets the orientation hint for output video playback.
     * <p>This method should be called before {@link #start}. Calling this
     * method will not rotate the video frame when muxer is generating the file,
     * but add a composition matrix containing the rotation angle in the output
     * video if the output format is
     * {@link android.media.MediaMuxer.OutputFormat#MUXER_OUTPUT_MPEG_4} so that a video player can
     * choose the proper orientation for playback. Note that some video players
     * may choose to ignore the composition matrix in a video during playback.
     * By default, the rotation degree is 0.</p>
     *
     * @param degrees the angle to be rotated clockwise in degrees.
     *                The supported angles are 0, 90, 180, and 270 degrees.
     * @throws IllegalArgumentException if degree is not supported.
     * @throws IllegalStateException    If this method is called after {@link #start}.
     */
    void setOrientationHint(int degrees) {
        if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {
            throw new IllegalArgumentException("Unsupported angle: " + degrees);
        }
        if (mState == MUXER_STATE_INITIALIZED) {
            nativeSetOrientationHint(mNativeObject, degrees);
        } else {
            throw new IllegalStateException("Can't set rotation degrees due" +
                    " to wrong state.");
        }
    }

    /**
     * Set and store the geodata (latitude and longitude) in the output file.
     * This method should be called before {@link #start}. The geodata is stored
     * in udta box if the output format is
     * {@link android.media.MediaMuxer.OutputFormat#MUXER_OUTPUT_MPEG_4}, and is ignored for other output
     * formats. The geodata is stored according to ISO-6709 standard.
     *
     * @param latitude  Latitude in degrees. Its value must be in the range [-90,
     *                  90].
     * @param longitude Longitude in degrees. Its value must be in the range
     *                  [-180, 180].
     * @throws IllegalArgumentException If the given latitude or longitude is out
     *                                  of range.
     * @throws IllegalStateException    If this method is called after {@link #start}.
     */
    void setLocation(float latitude, float longitude) {
        int latitudex10000 = (int) (latitude * 10000 + 0.5);
        int longitudex10000 = (int) (longitude * 10000 + 0.5);

        if (latitudex10000 > 900000 || latitudex10000 < -900000) {
            String msg = "Latitude: " + latitude + " out of range.";
            throw new IllegalArgumentException(msg);
        }
        if (longitudex10000 > 1800000 || longitudex10000 < -1800000) {
            String msg = "Longitude: " + longitude + " out of range";
            throw new IllegalArgumentException(msg);
        }

        if (mState == MUXER_STATE_INITIALIZED && mNativeObject != 0) {
            nativeSetLocation(mNativeObject, latitudex10000, longitudex10000);
        } else {
            throw new IllegalStateException("Can't set location due to wrong state.");
        }
    }

    /**
     * Starts the muxer.
     * <p>Make sure this is called after {@link #addTrack} and before
     * {@link #writeSampleData}.</p>
     *
     * @throws IllegalStateException If this method is called after {@link #start}
     *                               or Muxer is released
     */
    void start() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        if (mState == MUXER_STATE_INITIALIZED) {
            nativeStart(mNativeObject);
            mState = MUXER_STATE_STARTED;
        } else {
            throw new IllegalStateException("Can't start due to wrong state.");
        }
    }

    /**
     * Stops the muxer.
     * <p>Once the muxer stops, it can not be restarted.</p>
     *
     * @throws IllegalStateException if muxer is in the wrong state.
     */
    void stop() {
        if (mState == MUXER_STATE_STARTED) {
            nativeStop(mNativeObject);
            mState = MUXER_STATE_STOPPED;
        } else {
            throw new IllegalStateException("Can't stop due to wrong state.");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mNativeObject != 0) {
                nativeRelease(mNativeObject);
                mNativeObject = 0;
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Adds a track with the specified format.
     * <p>
     * The following table summarizes support for specific format keys across android releases.
     * Keys marked with '+:' are required.
     *
     * <table style="width: 0%">
     *  <thead>
     *   <tr>
     *    <th rowspan=2>OS Version(s)</th>
     *    <td colspan=3>{@code MediaFormat} keys used for</th>
     *   </tr><tr>
     *    <th>All Tracks</th>
     *    <th>Audio Tracks</th>
     *    <th>Video Tracks</th>
     *   </tr>
     *  </thead>
     *  <tbody>
     *   <tr>
     *    <td>{@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2}</td>
     *    <td rowspan=7>+: {@link MediaFormat#KEY_MIME}</td>
     *    <td rowspan=3>+: {@link MediaFormat#KEY_SAMPLE_RATE},<br>
     *        +: {@link MediaFormat#KEY_CHANNEL_COUNT},<br>
     *        +: <strong>codec-specific data<sup>AAC</sup></strong></td>
     *    <td rowspan=5>+: {@link MediaFormat#KEY_WIDTH},<br>
     *        +: {@link MediaFormat#KEY_HEIGHT},<br>
     *        no {@code KEY_ROTATION},
     *        use {@link #setOrientationHint setOrientationHint()}<sup>.mp4</sup>,<br>
     *        +: <strong>codec-specific data<sup>AVC, MPEG4</sup></strong></td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#KITKAT}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#KITKAT_WATCH}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#LOLLIPOP}</td>
     *    <td rowspan=4>as above, plus<br>
     *        +: <strong>codec-specific data<sup>Vorbis & .webm</sup></strong></td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#LOLLIPOP_MR1}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#M}</td>
     *    <td>as above, plus<br>
     *        {@link MediaFormat#KEY_BIT_RATE}<sup>AAC</sup></td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#N}</td>
     *    <td>as above, plus<br>
     *        <!-- {link MediaFormat#KEY_MAX_BIT_RATE}<sup>AAC, MPEG4</sup>,<br> -->
     *        {@link MediaFormat#KEY_BIT_RATE}<sup>MPEG4</sup>,<br>
     *        {@link MediaFormat#KEY_HDR_STATIC_INFO}<sup>#, .webm</sup>,<br>
     *        {@link MediaFormat#KEY_COLOR_STANDARD}<sup>#</sup>,<br>
     *        {@link MediaFormat#KEY_COLOR_TRANSFER}<sup>#</sup>,<br>
     *        {@link MediaFormat#KEY_COLOR_RANGE}<sup>#</sup>,<br>
     *        +: <strong>codec-specific data<sup>HEVC</sup></strong>,<br>
     *        codec-specific data<sup>VP9</sup></td>
     *   </tr>
     *   <tr>
     *    <td colspan=4>
     *     <p class=note><strong>Notes:</strong><br>
     *      #: storing into container metadata.<br>
     *      .mp4, .webm&hellip;: for listed containers<br>
     *      MPEG4, AAC&hellip;: for listed codecs
     *    </td>
     *   </tr><tr>
     *    <td colspan=4>
     *     <p class=note>Note that the codec-specific data for the track must be specified using
     *     this method. Furthermore, codec-specific data must not be passed/specified via the
     *     {@link #writeSampleData writeSampleData()} call.
     *    </td>
     *   </tr>
     *  </tbody>
     * </table>
     *
     * <p>
     * The following table summarizes codec support for containers across android releases:
     *
     * <table style="width: 0%">
     *  <thead>
     *   <tr>
     *    <th rowspan=2>OS Version(s)</th>
     *    <td colspan=3>Codec support</th>
     *   </tr><tr>
     *    <th>{@linkplain android.media.MediaMuxer.OutputFormat#MUXER_OUTPUT_MPEG_4 MP4}</th>
     *    <th>{@linkplain android.media.MediaMuxer.OutputFormat#MUXER_OUTPUT_WEBM WEBM}</th>
     *   </tr>
     *  </thead>
     *  <tbody>
     *   <tr>
     *    <td>{@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2}</td>
     *    <td rowspan=6>{@link MediaFormat#MIMETYPE_AUDIO_AAC AAC},<br>
     *        {@link MediaFormat#MIMETYPE_AUDIO_AMR_NB NB-AMR},<br>
     *        {@link MediaFormat#MIMETYPE_AUDIO_AMR_WB WB-AMR},<br>
     *        {@link MediaFormat#MIMETYPE_VIDEO_H263 H.263},<br>
     *        {@link MediaFormat#MIMETYPE_VIDEO_MPEG4 MPEG-4},<br>
     *        {@link MediaFormat#MIMETYPE_VIDEO_AVC AVC} (H.264)</td>
     *    <td rowspan=3>Not supported</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#KITKAT}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#KITKAT_WATCH}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#LOLLIPOP}</td>
     *    <td rowspan=3>{@link MediaFormat#MIMETYPE_AUDIO_VORBIS Vorbis},<br>
     *        {@link MediaFormat#MIMETYPE_VIDEO_VP8 VP8}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#LOLLIPOP_MR1}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#M}</td>
     *   </tr><tr>
     *    <td>{@link android.os.Build.VERSION_CODES#N}</td>
     *    <td>as above, plus<br>
     *        {@link MediaFormat#MIMETYPE_VIDEO_HEVC HEVC} (H.265)</td>
     *    <td>as above, plus<br>
     *        {@link MediaFormat#MIMETYPE_VIDEO_VP9 VP9}</td>
     *   </tr>
     *  </tbody>
     * </table>
     *
     * @param format The media format for the track.  This must not be an empty
     *               MediaFormat.
     * @return The track index for this newly added track, and it should be used
     * in the {@link #writeSampleData}.
     * @throws IllegalArgumentException if format is invalid.
     * @throws IllegalStateException    if muxer is in the wrong state.
     */
    int addTrack(MediaFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("format must not be null.");
        }

        if (mState != MUXER_STATE_INITIALIZED) {
            throw new IllegalStateException("Muxer is not initialized.");
        }
        if (mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        int trackIndex = -1;
        // Convert the MediaFormat into key-value pairs and send to the native.
        Map<String, Object> formatMap = null;
        try {
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            formatMap = (Map<String, Object>) method.invoke(format);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        if (formatMap == null) {
            throw new IllegalStateException("formatMap must not be null.");
        }

        String[] keys = null;
        Object[] values = null;
        int mapSize = formatMap.size();
        if (mapSize > 0) {
            keys = new String[mapSize];
            values = new Object[mapSize];
            int i = 0;
            for (Map.Entry<String, Object> entry : formatMap.entrySet()) {
                keys[i] = entry.getKey();
                values[i] = entry.getValue();
                ++i;
            }
            trackIndex = nativeAddTrack(mNativeObject, keys, values);
        } else {
            throw new IllegalArgumentException("format must not be empty.");
        }

        // Track index number is expected to incremented as addTrack succeed.
        // However, if format is invalid, it will get a negative trackIndex.
        if (mLastTrackIndex >= trackIndex) {
            throw new IllegalArgumentException("Invalid format.");
        }
        mLastTrackIndex = trackIndex;
        return trackIndex;
    }

    /**
     * Writes an encoded sample into the muxer.
     * <p>The application needs to make sure that the samples are written into
     * the right tracks. Also, it needs to make sure the samples for each track
     * are written in chronological order (e.g. in the order they are provided
     * by the encoder.)</p>
     *
     * @param byteBuf    The encoded sample.
     * @param trackIndex The track index for this sample.
     * @param bufferInfo The buffer information related to this sample.
     * @throws IllegalArgumentException if trackIndex, byteBuf or bufferInfo is  invalid.
     * @throws IllegalStateException    if muxer is in wrong state.
     *                                  MediaMuxer uses the flags provided in {@link BufferInfo},
     *                                  to signal sync frames.
     */
    void writeSampleData(int trackIndex, ByteBuffer byteBuf, BufferInfo bufferInfo) {
        if (trackIndex < 0 || trackIndex > mLastTrackIndex) {
            throw new IllegalArgumentException("trackIndex is invalid");
        }

        if (byteBuf == null) {
            throw new IllegalArgumentException("byteBuffer must not be null");
        }

        if (bufferInfo == null) {
            throw new IllegalArgumentException("bufferInfo must not be null");
        }

        if (bufferInfo.size < 0 || bufferInfo.offset < 0
                || (bufferInfo.offset + bufferInfo.size) > byteBuf.capacity()
                || bufferInfo.presentationTimeUs < 0) {
            throw new IllegalArgumentException("bufferInfo must specify a" +
                    " valid buffer offset, size and presentation time");
        }

        if (mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }

        if (mState != MUXER_STATE_STARTED) {
            throw new IllegalStateException("Can't write, muxer is not started");
        }

        nativeWriteSampleData(mNativeObject, trackIndex, byteBuf,
                bufferInfo.offset, bufferInfo.size,
                bufferInfo.presentationTimeUs, bufferInfo.flags);
    }

    /**
     * Make sure you call this when you're done to free up any resources
     * instead of relying on the garbage collector to do this for you at
     * some point in the future.
     */
    void release() {
        if (mState == MUXER_STATE_STARTED) {
            stop();
        }
        if (mNativeObject != 0) {
            nativeRelease(mNativeObject);
            mNativeObject = 0;
        }
        mState = MUXER_STATE_UNINITIALIZED;
    }
}
