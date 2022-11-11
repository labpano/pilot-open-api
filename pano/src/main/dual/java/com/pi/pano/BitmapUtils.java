package com.pi.pano;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;

class BitmapUtils {

    static Bitmap loadBitmap(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    static byte[] bitmapToRgba(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        byte[] bytes = new byte[pixels.length * 4];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int i = 0;
        for (int pixel : pixels) {
            int A = (pixel >> 24) & 0xff;
            int R = (pixel >> 16) & 0xff;
            int G = (pixel >> 8) & 0xff;
            int B = pixel & 0xff;
            bytes[i++] = (byte) R;
            bytes[i++] = (byte) G;
            bytes[i++] = (byte) B;
            bytes[i++] = (byte) A;
        }
        bitmap.recycle();
        return bytes;
    }
}
