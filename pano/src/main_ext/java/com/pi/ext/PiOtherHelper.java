package com.pi.ext;

import android.content.Context;
import android.content.Intent;

public final class PiOtherHelper {
    /**
     * Pause Auto PowerOff service
     */
    public void pauseAutoPowerOffService(Context context) {
        Intent intent = new Intent("com.pi.pilot.setting.shutdown");
        intent.putExtra("flag", 0);
        context.sendBroadcast(intent);
    }

    /**
     * Resume Auto PowerOff service
     */
    public void resumeAutoPowerOffService(Context context) {
        Intent intent = new Intent("com.pi.pilot.setting.shutdown");
        intent.putExtra("flag", 1);
        context.sendBroadcast(intent);
    }
}
