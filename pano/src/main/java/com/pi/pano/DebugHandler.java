package com.pi.pano;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

class DebugHandler extends Handler {
    private final String mTag;

     DebugHandler(String tag, Looper looper, Callback callback) {
        super(looper, callback);
        mTag = tag;
    }

    @Override
    public void dispatchMessage(@NonNull Message msg) {
        final long startTimestamp = SystemClock.uptimeMillis();
        super.dispatchMessage(msg);
        Log.d(mTag, "dispatchMessage what:" + msg.what + " cast time:" + (SystemClock.uptimeMillis() - startTimestamp) + ",offset:" + (msg.getWhen() - startTimestamp));
    }
}
