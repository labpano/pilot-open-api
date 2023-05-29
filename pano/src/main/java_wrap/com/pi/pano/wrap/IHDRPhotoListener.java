package com.pi.pano.wrap;

import com.pi.pano.annotation.PiHdrCount;

public interface IHDRPhotoListener extends IPhotoListener {
    /**
     * HDR拍照前准备（参数设置）
     *
     * @param hdrIndex 照片索引
     * @param hdrCount 照片总张数
     */
    void onHdrPrepare(int hdrIndex, @PiHdrCount int hdrCount);
}
