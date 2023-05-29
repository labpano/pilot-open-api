package com.pi.pano;

import androidx.annotation.NonNull;

public class ResolutionSize {
    public final int width;
    public final int height;

    public ResolutionSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @NonNull
    @Override
    public String toString() {
        return toResolutionSizeString(width, height);
    }

    /**
     * 转换成简单的width*height字符串。
     */
    @NonNull
    public String toSampleString() {
        return toResolutionSizeString(width, height);
    }

    @NonNull
    public static ResolutionSize safeParseSize(final String resolution) {
        if (resolution.contains("#")) {
            try {
                return parseSize(resolution.substring(0, resolution.indexOf("#")));
            } catch (Exception ex) {
                throw new RuntimeException("error resolution(" + resolution + ")");
            }
        } else {
            return parseSize(resolution);
        }
    }

    @NonNull
    public static ResolutionSize parseSize(String resolution) {
        try {
            String[] split = resolution.split("\\*");
            int width = Integer.parseInt(split[0]);
            int height = Integer.parseInt(split[1]);
            return new ResolutionSize(width, height);
        } catch (Exception e) {
            throw new RuntimeException("error resolution(" + resolution + ")");
        }
    }

    /**
     * 转换成的width*height字符串。
     */
    public static String toResolutionSizeString(int width, int height) {
        return width + "*" + height;
    }
}
