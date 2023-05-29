package com.pi.pano;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片、视频缩略图生成器
 */
public class ThumbnailGenerator {
    private static final String TAG = ThumbnailGenerator.class.getSimpleName();

    /**
     * 缩略图最大字节数
     */
    public static int sMaxSizeByte = 60_000;
    /**
     * 缩略图宽、高
     */
    public static final int[] sThumbnailsSize = new int[]{480, 240};
    /**
     * 未拼接图提取的缩略图宽、高
     */
    public static final int[] sUnStitchedThumbnailsSize = new int[]{(int) (960 * 1.2), (int) (480 * 1.2)};

    /**
     * 缩略图处理
     *
     * @param dstFile 文件路径
     * @param isImage true:图片 false:视频
     * @param stitch  true:已拼接 ,false:未拼接
     * @return 返回的缩略图字节数组
     */
    public static byte[] extractThumbnails(@NonNull File dstFile, boolean isImage, boolean stitch) {
        return isImage ? ThumbnailGenerator.extractImageThumbnails(dstFile, stitch)
                : ThumbnailGenerator.extractVideoThumbnails(dstFile, stitch);
    }

    /**
     * 图片缩略图处理
     *
     * @param dstFile 文件路径
     * @return 返回的缩略图字节数组
     */
    public static byte[] extractImageThumbnails(@NonNull File dstFile, boolean stitch) {
        return stitch ? extractImageThumbnailsBytes(dstFile) :
                extractImageThumbnailsBytesByLeftBall(dstFile);
    }

    /**
     * 获取未拼接图片缩略图，使用left球处理。
     *
     * @param dstFile 文件路径
     * @return 返回的缩略图字节数组
     */
    @Nullable
    public static byte[] extractImageThumbnailsBytesByLeftBall(@NonNull File dstFile) {
        final long startTimestamp = System.currentTimeMillis();
        byte[] bytes = null;
        Bitmap bitmap = null;
        try {
            bitmap = extractImageThumbnails(dstFile, sUnStitchedThumbnailsSize);
            if (bitmap != null) {
                bitmap = cropLeftBall(bitmap, sThumbnailsSize);
            }
            if (bitmap != null) {
                bytes = compressToBytes(bitmap, sMaxSizeByte);
            }
        } catch (Exception ex) {
            Log.e(TAG, "extractImageThumbnailsBytesByLeftBall,ex:" + ex);
            ex.printStackTrace();
            if (null != bitmap) {
                bitmap.recycle();
            }
        }
        Log.d(TAG, "extractImageThumbnailsBytesByLeftBall,dstFile:" + dstFile + ",cost time:" + (System.currentTimeMillis() - startTimestamp));
        return bytes;
    }

    /**
     * 获取图片缩略图。
     *
     * @param dstFile 文件路径
     * @return 返回的缩略图字节数组
     */
    @Nullable
    public static byte[] extractImageThumbnailsBytes(@NonNull File dstFile) {
        final long startTimestamp = System.currentTimeMillis();
        byte[] bytes = null;
        Bitmap bitmap = null;
        try {
            bitmap = extractImageThumbnails(dstFile, sThumbnailsSize);
            if (bitmap != null) {
                bytes = compressToBytes(bitmap, sMaxSizeByte);
            }
        } catch (Exception ex) {
            Log.e(TAG, "extractImageThumbnailsBytes,ex:" + ex);
            ex.printStackTrace();
            if (null != bitmap) {
                bitmap.recycle();
            }
        }
        Log.d(TAG, "extractImageThumbnailsBytes,dstFile:" + dstFile + ",cost time:" + (System.currentTimeMillis() - startTimestamp));
        return bytes;
    }

    /**
     * 视频缩略图处理
     *
     * @param dstFile 文件路径
     * @return 返回的缩略图字节数组
     */
    public static byte[] extractVideoThumbnails(@NonNull File dstFile, boolean stitch) {
        return stitch ? extractVideoThumbnailsBytes(dstFile) :
                extractVideoThumbnailsBytesByLeftBall(dstFile);
    }

    /**
     * 获取未拼接视频缩略图，提取第一帧并使用left球处理。
     *
     * @param dstFile 文件路径
     * @return 返回的缩略图字节数组
     */
    @Nullable
    public static byte[] extractVideoThumbnailsBytesByLeftBall(@NonNull File dstFile) {
        final long startTimestamp = System.currentTimeMillis();
        byte[] bytes = null;
        Bitmap bitmap = null;
        try {
            bitmap = extractVideoThumbnails(dstFile, sUnStitchedThumbnailsSize);
            if (bitmap != null) {
                bitmap = cropLeftBall(bitmap, sThumbnailsSize);
            }
            if (bitmap != null) {
                bytes = compressToBytes(bitmap, sMaxSizeByte);
            }
        } catch (Exception ex) {
            Log.e(TAG, "extractVideoThumbnailsBytesByLeftBall,ex:" + ex);
            ex.printStackTrace();
            if (null != bitmap) {
                bitmap.recycle();
            }
        }
        Log.d(TAG, "extractVideoThumbnailsBytesByLeftBall,dstFile:" + dstFile + ",cost time:" + (System.currentTimeMillis() - startTimestamp));
        return bytes;
    }

    /**
     * 获取视频缩略图,提取第一帧。
     *
     * @param dstFile 文件路径
     * @return 返回的缩略图字节数组
     */
    @Nullable
    public static byte[] extractVideoThumbnailsBytes(@NonNull File dstFile) {
        final long startTimestamp = System.currentTimeMillis();
        byte[] bytes = null;
        Bitmap bitmap = null;
        try {
            bitmap = extractVideoThumbnails(dstFile, sThumbnailsSize);
            if (bitmap != null) {
                bytes = compressToBytes(bitmap, sMaxSizeByte);
            }
        } catch (Exception ex) {
            Log.e(TAG, "extractVideoThumbnailsBytes,ex:" + ex);
            ex.printStackTrace();
            if (null != bitmap) {
                bitmap.recycle();
            }
        }
        Log.d(TAG, "extractVideoThumbnailsBytes,dstFile:" + dstFile + ",cost time:" + (System.currentTimeMillis() - startTimestamp));
        return bytes;
    }

    /**
     * 提取图片缩略图
     *
     * @param dstSize 指定的宽高
     */
    @Nullable
    private static Bitmap extractImageThumbnails(@NonNull File dstFile, @NonNull int[] dstSize) {
        String filepath = dstFile.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, options);
        //计算压缩比
        int h = options.outHeight;
        int w = options.outWidth;
        options.inSampleSize = Math.min(h / dstSize[1], w / dstSize[0]);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filepath, options);
    }

    /**
     * 提取视频缩略图
     *
     * @param dstSize 指定的宽高
     */
    @Nullable
    private static Bitmap extractVideoThumbnails(@NonNull File dstFile, @NonNull int[] dstSize) {
        String filepath = dstFile.getAbsolutePath();
        Bitmap ret = null;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(filepath);
            ret = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mmr.release();
        }
        if (ret != null) {
            ret = android.media.ThumbnailUtils.extractThumbnail(ret, dstSize[0], dstSize[1], android.media.ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return ret;
    }

    /**
     * 裁切未拼接图像中left球,并压缩到指定宽高。
     *
     * @param dstSize 指定的宽高
     */
    @Nullable
    private static Bitmap cropLeftBall(@NonNull Bitmap origin, @NonNull int[] dstSize) {
        Bitmap bitmap = cropLeftBall(origin);
        if (bitmap != null) {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            float scaleWidth = ((float) dstSize[0]) / width;
            float scaleHeight = ((float) dstSize[1]) / height;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);// 使用后乘
            Bitmap replace = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
            bitmap.recycle();
            return replace;
        }
        return null;
    }

    /**
     * 裁切未拼接图像中left球
     * <p>
     * 计算方式：需要导出缩率图的宽高比为2:1，
     * 首先先计算出第一个镜头中圆的半径min(width/4,height/2)
     * 然后根据半径计算出圆心的坐标左上角的x坐标为（1-2*Math.sqrt(5) / 5）*radius,y的坐标应为（1-Math.sqrt(5) / 5）
     * 按照此时的剪切方式，四个角会出现锯齿和黑边，则将将左上角坐标像右下方向移动一些:
     * 即x = ((1 - Math.sqrt(6) / 3) * radius),y = ((1 - Math.sqrt(6) / 6) * radius)
     */
    @Nullable
    private static Bitmap cropLeftBall(@NonNull Bitmap origin) {
        int radius = Math.min(origin.getWidth() / 4, origin.getHeight() / 2);
        int x = (int) ((1 - Math.sqrt(6) / 3) * radius);
        int y = (int) ((1 - Math.sqrt(6) / 6) * radius);
        int corpWidth = (int) ((2 * Math.sqrt(6) / 3) * radius);
        int corpHeight = (int) ((Math.sqrt(6) / 3) * radius);
        Bitmap replace = Bitmap.createBitmap(origin, x, y, corpWidth, corpHeight, null, false);
        origin.recycle();
        return replace;
    }

    /**
     * 压缩到指定的大小
     */
    @NonNull
    private static byte[] compressToBytes(@NonNull Bitmap origin, int maxSizeByte) {
        byte[] ret;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 100;
        do {
            origin.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            ret = baos.toByteArray();
            baos.reset();
        } while (ret.length > maxSizeByte && (quality -= 10) > 0);
        origin.recycle();
        return ret;
    }

    /**
     * 未拼接的照片 exif 内注入缩略图
     *
     * @param unStitchFile 未拼接的文件
     */
    public static int injectExifThumbnailForUnStitch(@NonNull File unStitchFile) {
        final long startTimestamp = System.currentTimeMillis();
        int ret = -1;
        byte[] bytes = extractImageThumbnailsBytesByLeftBall(unStitchFile);
        if (null != bytes) {
            // 保存成文件
            File thumbnail = new File(unStitchFile.getParentFile(), ".t." + unStitchFile.getName());
            try (FileOutputStream outputStream = new FileOutputStream(thumbnail)) {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ret = PilotSDK.injectThumbnail(unStitchFile.getAbsoluteFile(), thumbnail.getAbsoluteFile());
            thumbnail.delete();
        }
        Log.d(TAG, "injectExifThumbnailForUnStitch,unStitchFile:" + unStitchFile + ",cost time:" + (System.currentTimeMillis() - startTimestamp) + ",ret:" + ret);
        return ret;
    }

    /**
     * 拼接的照片 exif 内注入缩略图
     *
     * @param stitchFile 拼接的文件
     */
    public static int injectExifThumbnailForStitch(@NonNull File stitchFile) {
        final long startTimestamp = System.currentTimeMillis();
        int ret = -1;
        byte[] bytes = extractImageThumbnailsBytes(stitchFile);
        if (null != bytes) {
            // 保存成文件
            File thumbnail = new File(stitchFile.getParentFile(), ".t." + stitchFile.getName());
            try (FileOutputStream outputStream = new FileOutputStream(thumbnail)) {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ret = PilotSDK.injectThumbnail(stitchFile.getAbsoluteFile(), thumbnail.getAbsoluteFile());
            thumbnail.delete();
        }
        Log.d(TAG, "injectExifThumbnailForStitch,stitchFile:" + stitchFile + ",cost time:" + (System.currentTimeMillis() - startTimestamp) + ",ret:" + ret);
        return ret;
    }
}
