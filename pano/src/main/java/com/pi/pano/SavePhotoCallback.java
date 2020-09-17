package com.pi.pano;

public interface SavePhotoCallback {

    void onSavePhotoStart();

    void onSavePhotoComplete();

    void onSavePhotoFailed();
}
