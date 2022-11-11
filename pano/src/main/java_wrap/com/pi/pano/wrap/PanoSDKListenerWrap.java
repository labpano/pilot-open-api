package com.pi.pano.wrap;

import android.content.Context;

import com.pi.pano.PanoSDKListener;

class PanoSDKListenerWrap implements PanoSDKListener {

    protected Context mContext;
    protected final PanoSDKListener mListener;

    public PanoSDKListenerWrap(Context context, PanoSDKListener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
    }

    @Override
    public void onPanoCreate() {
        if (null != mListener) mListener.onPanoCreate();
    }

    @Override
    public void onPanoRelease() {
        if (null != mListener) mListener.onPanoRelease();
    }

    @Override
    public void onChangePreviewMode(int mode) {
        if (null != mListener) mListener.onChangePreviewMode(mode);
    }

    @Override
    public void onSingleTap() {
        if (null != mListener) mListener.onSingleTap();
    }

    @Override
    public void onEncodeFrame(int count) {
        if (null != mListener) mListener.onEncodeFrame(count);
    }
}
