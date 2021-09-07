# 简介

Pilot OS是Pilot系列全景相机的操作系统，它基于Android系统定制(目前为Android 7.0 版本)，在 Pilot OS系统上开发应用的方式与一般的Android应用开发方式几乎无差别。

本程序主要提供Camera部分的功能，同时提供推流库。集成到运行在Pilot相机上的App内，可供其用于进行镜头预览、拍照、录像、直播推流等。



### 集成方式

- 方式一：使用已编译的构件  
archive 目录下放有已经编译好的构件可直接使用，包括：  
  + pano-release.aar  
  核心的 pano，必须集成
  + live-8k/*  
  PilotLive 8K RTMP 推流库
  + live-4k/*  
  4K 及以下 RTMP 推流库

- 方式二：导入 pano 目录源码到项目  
将 pano 目录源码加入到你的项目中直接参与编译，此方式可修改 pano 源码。



### 初始化（加载预览）

初始化同时会将预览显示在界面上：
```kotlin
// 创建 PilotSDK 并指定放置预览视图的父容器
mPilotSDK = PilotSDK(findViewById(R.id.vg_preview), object : PanoSDKListener {
            override fun onPanoCreate() {
                // 创建成功后可及进一步调整预览参数
            }

            override fun onPanoRelease() {
            }

            override fun onChangePreviewMode(mode: Int) {
            }

            override fun onSingleTap() {
            }

            override fun onEncodeFrame(count: Int) {
            }
        })
```

初始化成功后，可对预览 分辨率&帧率 、预览效果等进行设置。如设置 分辨率&帧率 ：
```kotlin
// 可直接使用 PilotSDK 内方法调整
PilotSDK.changeCameraResolution(
        PilotSDK.CAMERA_PREVIEW_4048_2530_15 // the resolution you need, it includes the frame rate
        , object : ChangeResolutionListener() {
            override fun onChangeResolution(width: Int, height: Int) {
                // 分辨率改变
            }
        })
```

目前支持的预览 分辨率&帧率 主要有：
- PilotSDK#CAMERA_PREVIEW_3520_2200_24
- PilotSDK#CAMERA_PREVIEW_4048_2530_15
- PilotSDK#CAMERA_PREVIEW_4048_2530_7
- PilotSDK#CAMERA_PREVIEW_2880_1800_15
- PilotSDK#CAMERA_PREVIEW_2192_1370_30
- PilotSDK#CAMERA_PREVIEW_1920_1200_30
- PilotSDK#CAMERA_PREVIEW_512_320_30
- PilotSDK#CAMERA_PREVIEW_480_300_30
- PilotSDK#CAMERA_PREVIEW_448_280_22
- PilotSDK#CAMERA_PREVIEW_288_180_24
- PilotSDK#CAMERA_PREVIEW_400_250_24



### 拍照

预览画面加载后，可进行拍照。

- 拍照  
`takePhoto(String filename, int width, int height, TakePhotoListener listener)`  

照片的分辨率及使用的预览 分辨率&帧率 ：  
- 8K ~ 2K： `PilotSDK#CAMERA_PREVIEW_4048_2530_15`

可参考 [photoSample][]



### 录像

预览画面加载后，可进行录像。
- 开始：  
`startRecord(String dirPath, String filename, int codec, int channelCount, int videoWidth, boolean useForGoogleMap, int memomotionRatio, MediaRecorderListener listener)`

- 停止：  
`stopRecord(String firmware)`  
`firmware` 为固件名称，可为空。

视频的分辨率及使用的预览 分辨率&帧率 ：  
- 8K 实时拼接： PilotSDK#CAMERA_PREVIEW_4048_2530_7
- 6K 实时拼接： PilotSDK#CAMERA_PREVIEW_2880_1800_15
- 4K 实时拼接： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- 2K 实时拼接： PilotSDK#CAMERA_PREVIEW_1920_1200_30
- 8K 未拼接： PilotSDK#CAMERA_PREVIEW_400_250_24
- 7K 未拼接： PilotSDK#CAMERA_PREVIEW_288_180_24
- 6K 未拼接： PilotSDK#CAMERA_PREVIEW_512_320_30
- 4K 未拼接： PilotSDK#CAMERA_PREVIEW_480_300_30

可参考 [videoSample][]



### 直播推流

预览画面加载后，可获取到预览的具体数据用于直播。  
目前提供了直播 4K、8K 的 RTMP 推流库。


#### 4K 推流

大部分的媒体服务器目前支持到 4K ，支持 RTMP 协议。  
提供的4K推流库可对4K及以下分辨率推流，能被常见的平台接收，如 Facebook 、YouTube 等。

1. 创建推流对象，并设置参数
```kotlin
mPusher = PusherFactory.createPusher()
// 设置视频参数，包括分辨率、帧率、码率
mPusher.addVideoTrackInfo(
    IPusher.ENCODER_ID_H264, 3840, 1920, 24, 12000000
)
// 设置音频参数
mPusher.addAudioTrackInfo(
    IPusher.AUDIO_SOURCE_ID_MIC, IPusher.ENCODER_ID_AAC,
    48000, 2, 16, 128000
)
// 设置推流地址
mPusher.addOutput(
    IPusher.OUTPUT_TYPE_RTMP, pushURL, 5000 //5 seconds timeout
)
```

2. 绑定Suerface
```kotlin
mPusher.setEncodeVideoListerner(object : IPusherEncodeVideoListerner {
          var init = false;
          override fun updateSurface(streamName: String, surface: Surface): Int {
              if (!init) {
                 // 将创建的Suerface设置到PilotSDK内，预览画面将同时绘制到此Surface上。这步很重要。
                  PilotSDK.setEncodeInputSurfaceForLive(surface)
                  init = true
              }
              return 0
          }
      })
```

2. 开始推流
```kotlin
mPusher.start(object : IPusherStateListerner {
            override fun notifyPusherState(
                state: IPusherStateListerner.PusherState, errorCode: Int, message: String
            ) {
              // 推流过程中状态信息
            }
        })
```

3. 停止推流
```kotlin
// 取消预览画面直播绘制的Surface
PilotSDK.setEncodeInputSurfaceForLive(null)
// 停止推流
mPusher.stop()
```

4K 直播推流的分辨率及使用的预览 分辨率&帧率 ：
- 4K 帧率优先： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- 4K 画质优先： PilotSDK#CAMERA_PREVIEW_3520_2200_24
- 高清（1280P）： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- 标清（960P）： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- 流畅（960P）： PilotSDK#CAMERA_PREVIEW_2192_1370_30

可参考 [liveSample][]  



#### PilotLive 8K 推流

1. 创建推流对象
```kotlin
mStreamSender = StreamSender()
```

2. 开始推流
```kotlin
mStreamSender.start(
            PilotSDK.getCameras(), // 打开的Camera，为4个
            false, // Can save bandwidth when h265 is used
            2, // channel number
            3648,
            2280,
            24,
            15000000, // bitrate
            pushURL,
            null,
            0f,
            ""
        )
```

3. 停止推流
```kotlin
mStreamSender.stop(false)
```

PilotLive 8K 推流使用的预览 分辨率&帧率 ：
- PilotLive 8K ： PilotSDK#CAMERA_PREVIEW_400_250_24

可参考 [live8kSample][]



### PRO调节

预览的画面加载后可对其进一步调节画面效果。

- 手动曝光时间  
  + 关闭手动曝光(自动曝光)  
    `com.pi.pano.ExposeTimeAdjustHelper.close()`
  + 开启手动曝光  
    `com.pi.pano.ExposeTimeAdjustHelper.open()`  
    再设置值  
    `com.pi.pano.ExposeTimeAdjustHelper.setValues(String fps, String exposedTime, String analogGain, String digitGain)`  
    参数：  
    `fps`： 当前预览的帧率；  
    `exposedTime`： 曝光时间，可用值： 312："1/3200"; 1000："1/1000"; 2000："1/500"; 10000："1/100"; 20000："1/50"; 66666："1/15";  
    `analogGain`、`digitGain`： 手动曝光下使用的 ISO 具体值，如100。

- ISO
  + 自动曝光下使用 `com.pi.pano.PilotSDK#setISO(int value)` 调节，  
    `value`： 可用值： 0 ：auto; 1 ：ISO 50; 2 ：ISO 100; 3 ：ISO 200; 4 ：ISO 400; 5 ：ISO 800; 6 ：ISO 1600。
  + 手动曝光下使用`com.pi.pano.PilotSDK#setManualISO(int value)` 调节，  
    `value`： 可用值： 100 ：ISO 100; 200 ：ISO 200; 400 ：ISO 400; 600 ：ISO 600; 800 ：ISO 800; 1600 ：ISO 1600; 3200 ：ISO 3200。

- EV
  非自动曝光时可进一步设置EV.  
  使用 `com.pi.pano.PilotSDK#setExposureCompensation(int value)` 调节，  
  `value`： 取值范围为-4到4，-4最暗，4最亮

- WB
  使用 `com.pi.pano.PilotSDK#setWhiteBalance(int value)` 调节，  
  `value`： 可用值： 0：auto; 1：incandescent; 2：fluorescent; 3：daylight; 4：cloudy-daylight。

- 拼接距离
  控制镜头画面拼接缝隙。  
  使用 `com.pi.pano.PilotSDK#setStitchingDistance(float distance, float max)` 调节，  
  `distance`： 取值范围为-100\~100，0 大概2m，100表示无穷远，-100大概是0.5m;  
  `max`： 在Era 设备上为1.35，One 设备上为0.9。

拍照时可调整以下值：曝光时间、ISO、EV（曝光时间为手动时）、WB、拼接距离。  
录像时可调整以下值：ISO、EV、拼接距离。



### 播放文件

提供播放设备上生成的媒体文件功能。  
可直接在界面中加入`com.pi.pano.MediaPlayerSurfaceView` 视图，并使用  
`com.pi.pano.MediaPlayerSurfaceView#open(String filename)`  
打开本地文件进行播放。



### 其他

1. 自动关机功能服务暂停/恢复（仅在 Pilot OS v5.14.0 以上版本可用）
   + 暂停： `com.pi.ext.PiOtherHelper#pauseAutoPowerOffService(Context context)`
   + 恢复:  `com.pi.ext.PiOtherHelper#resumeAutoPowerOffService(Context context)`

2. `com.pi.pano.helper` 包内提供了相对简单的帮助类，可参考。



### 常见问题

1. 预览画面全黑。  
当不恰当的使用相机设备或者使用环境不当可能引起设备异常，可重启相机设备。

2. 直播清晰度过低。  
直播对网络要求较高，确认当前使用的网络带宽用于直播该清晰度、码率是否可行。避免因带宽过低、或延时长引起。

3. 使用提供的 4K 推流库推送 H.265 媒体服务器不支持。  
RTMP 直播目前仅支持 H.264 ，提供的 4K 、 8K 推流库可推送 H.265 但需媒体服务器及播放器兼容。

3. 设备长时间开启预览温度过高。  
正常现象，使用预览设备消耗性能发热，设备内含有风扇等散热组件，可在设置内将风扇为自动开启。

4. 集成的SDK可用于非Pilot OS系统上吗？   
不可以


### 示例

| Sample | Description |
|:-|:-|
| [photoSample][] [下载Apk][photoSample_dl] | 展示一般的拍照过程 |
| [videoSample][] [下载Apk][videoSample_dl]| 展示一般的录像过程  |
| [liveSample][] [下载Apk][liveSample_dl]| 展示 RTMP 直播 4K 及以下的分辨率过程 |
| [live8kSample][] [下载Apk][live8kSample_dl]| 展示 RTMP 直播 PilotLive 8K 分辨率过程  |



[photoSample]: ./samples/photoSample "photoSample"
[videoSample]: ./samples/videoSample "videoSample"
[liveSample]: ./samples/liveSample "liveSample"
[live8kSample]: ./samples/live8kSample "live8kSample"

[photoSample_dl]: ./samples/apk/photoSample-debug.apk "photoSampleApk"
[videoSample_dl]: ./samples/apk/videoSample-debug.apk "videoSampleApk"
[liveSample_dl]: ./samples/apk/liveSample-debug.apk "liveSampleApk"
[live8kSample_dl]: ./samples/apk/live8kSample-debug.apk "live8kSampleApk"

[zh-CN]: ./README_zh-CN.md "中文"
