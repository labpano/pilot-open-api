# 简介

用于PilotPano设备上的全景sdk。<br/>
PilotPano上的系统基于android 10，预览画面主要使用Camera 2接入。<br/>
archive 文件夹内放有编译好的aar包，可用于集成，或自行使用源码集成编译。

### 初始化（加载预览）
使用```com.pi.pano.PilotSDK.PilotSDK```类初始化，会自动加预览试图加入到视图容器内。<br/>
对预览的效果设置可根据具体的拍摄模式进行调节，可参考[配置][CMCR]。<br/>

切换分辨率：```com.pi.pano.PilotSDK#changeCameraResolution```。<br/>
```com.pi.pano.wrap.PreviewWrap``` 类，再次封装，可帮助加载初始化、设置预览参数、根据拍摄模式切换分辨率等。

### 拍照
拍照：```com.pi.pano.PilotSDK#takePhoto```。<br/>
可区分为：普通拍照、街景拍照、漫游拍照等模式，不同模式下的参考[配置][CMCR]。<br/>
可参考 [photoSample][] [下载apk][photoSample_apk_dl]

### 录像
开始录像：```com.pi.pano.PilotSDK#startRecord```；<br/>
停止录像：```com.pi.pano.PilotSDK#stopRecord```。<br/>
可区分为：普通全景视频、平面视频、慢动作视频、延时摄影视频、街景视频等模式，不同模式下的参考[配置][CMCR]。<br/>
可参考 [videoSample][] [下载apk][videoSample_apk_dl]

### 拼接
```com.pi.pano.StitchingOpticalFlow``` 类，用于拼接照片，拼接照片可选用光流拼接方式。<br/>
```com.pi.pano.StitchingUtil``` 类，用于拼接视频。

### 直播
可参考 [liveSample][] [下载apk][liveSample_apk_dl]


[zh-CN]: ./README.md "中文"

[CMCR]: ./CaptureModeConfigurationReference.md "拍摄模式与参数配置参考"

[photoSample]: ./samples/photoSample "photoSample"
[photoSample_apk_dl]: ./samples/apk-samples/photoSample-debug.apk "photoSample-debug.apk"
[videoSample]: ./samples/videoSample "videoSample"
[videoSample_apk_dl]: ./samples/apk-samples/videoSample-debug.apk "videoSample-debug.apk"
[liveSample]: ./samples/liveSample "liveSample"
[liveSample_apk_dl]: ./samples/apk-samples/liveSample-debug.apk "liveSample-debug.apk"
