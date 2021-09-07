package com.pi.pano;

public abstract class ParamReCaliListener {
    /**
     * Calculate the parameters every few frames.
     * e.g. if set to 1, that is, recalculate every frame.
     * -1 is calculated only once.
     */
    protected int mExecutionInterval = 1;

    public ParamReCaliListener() {
    }

    public ParamReCaliListener(int executionInterval) {
        mExecutionInterval = executionInterval;
    }

    /**
     * Recalculate parameter calculation structure error code
     *
     * @param error 0-recalculation successful.
     */
    void onError(int error) {
    }
}
