package com.pi.pano;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

class SystemPropertiesProxy {

    public static String get(String key) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = obtainSystemPropertiesClass();
            Method get = c.getMethod("get", String.class);
            return (String) get.invoke(c, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String get(String key, String def) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = obtainSystemPropertiesClass();
            Method get = c.getMethod("get", String.class, String.class);
            return (String) get.invoke(c, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static int getInt(String key, int def) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = obtainSystemPropertiesClass();
            Method get = c.getMethod("getInt", String.class, int.class);
            return (int) get.invoke(c, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static long getLong(String key, long def) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = obtainSystemPropertiesClass();
            Method get = c.getMethod("getLong", String.class, long.class);
            return (long) get.invoke(c, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static boolean getBoolean(String key, boolean def) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = obtainSystemPropertiesClass();
            Method get = c.getMethod("getBoolean", String.class, boolean.class);
            return (boolean) get.invoke(c, key, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    public static float getFloat(String key, float def) {
        String valStr = get(key);
        if (valStr == null || valStr.isEmpty()) {
            return def;
        }
        return Float.parseFloat(valStr);
    }

    public static double getDouble(String key, double def) {
        String valStr = get(key);
        if (valStr == null || valStr.isEmpty()) {
            return def;
        }
        return Double.parseDouble(valStr);
    }

    public static void set(String key, String val) {
        try {
            Class<?> c = obtainSystemPropertiesClass();
            Method get = c.getMethod("set", String.class, String.class);
            get.invoke(c, key, val);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    @NonNull
    public static Class<?> obtainSystemPropertiesClass() throws ClassNotFoundException {
        return Class.forName("android.os.SystemProperties");
    }
}
