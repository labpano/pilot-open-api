[中文][zh-CN]

# Introduction

Pilot OS is the operating system of Pilot series of panoramic cameras. It is customized based on the Android system (currently Android 7.0 version). The way of developing applications on Pilot OS  is almost the same as that of general Android application development.

This program mainly provides the functions of the Camera part, and also provides a streaming library. It is integrated into the App running on Pilot camera and can be used for lens preview, taking photos, recording videos, live streaming, etc.



### Integration

- Way one：Use compiled components  
The compiled components can be used directly under the archive directory, including:：  
  + pano-release.aar  
  The core pano, must be integrated
  + live-8k/*  
  PilotLive 8K RTMP push streaming library
  + live-4k/*  
  4K and below RTMP push streaming library

- Way two：Import the pano directory source code to the project  
Add the pano directory source code to your project directly to participate in the compilation, this way you can modify the pano source code.



### Initialization (Load Preview)

Initialization will also display the preview into the view container:
```kotlin
// Create PilotSDK and specify the parent container to place the preview view
mPilotSDK = PilotSDK(this, findViewById(R.id.vg_preview), object : PanoSDKListener {
            override fun onSDKCreate() {
                // After the creation is successful, you can further adjust the preview parameters
            }

            override fun onSDKRelease() {
            }

            override fun onChangePanoMode(mode: Int) {
            }

            override fun onSingleTapConfirmed() {
            }

            override fun onEncodeFrame(count: Int) {
            }
        })
```

After the initialization is successful, you can set the preview resolution&fps, preview effect, etc. Such as setting the resolution&fps：
```kotlin
// Use PilotSDK internal method adjustment
PilotSDK.changeCameraResolution(
        PilotSDK.CAMERA_PREVIEW_4048_2530_15 // the resolution you need, it includes the frame rate
        , object : ChangeResolutionListener() {
            override fun onChangeResolution(width: Int, height: Int) {
                // resolution change
            }
        })
```

Currently supported preview resolution&fps include:
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



### Take Photos

After the preview is loaded, you can take a picture.

- Take a photo  
`takePhoto(String filename, int width, int height, TakePhotoListener listener)`  

Photo resolution and preview used resolution&fps ：  
- 8K ~ 2K： `PilotSDK#CAMERA_PREVIEW_4048_2530_15`

Reference sample [photoSample][]



### Record videos

After the preview is loaded, you can record.
- Start record：  
`startRecord(String dirPath, String filename, int codec, int channelCount, int videoWidth, boolean useForGoogleMap, int memomotionRatio, MediaRecorderListener listener)`

- Stop record：  
`stopRecord(String firmware)`  
`firmware` the name of the firmware and can be empty.

Video resolution and preview used resolution&fps:  
- 8K Stitched Video： PilotSDK#CAMERA_PREVIEW_4048_2530_7
- 6K Stitched Video： PilotSDK#CAMERA_PREVIEW_2880_1800_15
- 4K Stitched Video： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- 2K Stitched Video： PilotSDK#CAMERA_PREVIEW_1920_1200_30
- 8K Unstitched Video： PilotSDK#CAMERA_PREVIEW_400_250_24
- 7K Unstitched Video： PilotSDK#CAMERA_PREVIEW_288_180_24
- 6K Unstitched Video： PilotSDK#CAMERA_PREVIEW_512_320_30
- 4K Unstitched Video： PilotSDK#CAMERA_PREVIEW_480_300_30

Reference sample [videoSample][]



### Live Streaming

After the preview is loaded, the previewed data can be obtained for live broadcast.  
We provide 4K and 8K RTMP streaming libraries for live streaming.


#### 4K Streaming

Most of the media servers currently support 4K, and support the RTMP protocol.  
The provided 4K streaming library can push streaming at resolutions of 4K and below, and can be received by common platforms such as Facebook and YouTube.

1. Create Pusher and set parameters
```kotlin
mPusher = PusherFactory.createPusher()
// Set video parameters, including resolution, fps, bit rate
mPusher.addVideoTrackInfo(
    IPusher.ENCODER_ID_H264, 3840, 1920, 24, 12000000
)
// Set audio parameters
mPusher.addAudioTrackInfo(
    IPusher.AUDIO_SOURCE_ID_MIC, IPusher.ENCODER_ID_AAC,
    48000, 2, 16, 128000
)
// Set push address
mPusher.addOutput(
    IPusher.OUTPUT_TYPE_RTMP, pushURL, 5000 //5 seconds timeout
)
```

2. Binding Suerface
```kotlin
mPusher.setEncodeVideoListerner(object : IPusherEncodeVideoListerner {
          var init = false;
          override fun updateSurface(streamName: String, surface: Surface): Int {
              if (!init) {
                 // Set the created Suerface to PilotSDK, and the preview screen will be drawn on this Surface at the same time. This step is very important
                  PilotSDK.setEncodeInputSurfaceForLive(surface)
                  init = true
              }
              return 0
          }
      })
```

2. Start streaming
```kotlin
mPusher.start(object : IPusherStateListerner {
            override fun notifyPusherState(
                state: IPusherStateListerner.PusherState, errorCode: Int, message: String
            ) {
              // Status information during the push
            }
        })
```

3. Stop streaming
```kotlin
// Cancel the surface drawn in the preview screen live
PilotSDK.setEncodeInputSurfaceForLive(null)
// Stop
mPusher.stop()
```

4K live streaming resolution and preview used resolution&fps:
- 4K FPS Priority： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- 4K Quality Priority： PilotSDK#CAMERA_PREVIEW_3520_2200_24
- HD （1280P）： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- SD （960P）： PilotSDK#CAMERA_PREVIEW_2192_1370_30
- Fluent（960P）： PilotSDK#CAMERA_PREVIEW_2192_1370_30

Reference sample [liveSample][]  



#### PilotLive 8K Streaming

1. Create StreamSender
```kotlin
mStreamSender = StreamSender()
```

2. Start streaming
```kotlin
mStreamSender.start(
            PilotSDK.getCameras(), // Open Camera, the number is 4
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

3. Stop streaming
```kotlin
mStreamSender.stop(false)
```

PilotLive 8K preview resolution&fps used for streaming:
- PilotLive 8K ： PilotSDK#CAMERA_PREVIEW_400_250_24

Reference sample [live8kSample][]



### PRO Adjustment

After the preview is loaded, the preview effect can be further adjusted.

- Exposure time  
  + Turn off manual exposure  
    `com.pi.pano.ExposeTimeAdjustHelper.close()`  
    Exposure time will be automatic after closing.
  + Turn on manual exposure  
    `com.pi.pano.ExposeTimeAdjustHelper.open()`  
    Set the value  
    `com.pi.pano.ExposeTimeAdjustHelper.setValues(String fps, String exposedTime, String analogGain, String digitGain)`  
    parameter:  
    `fps`： The fps of the current preview;  
    `exposedTime`： exposure time,available values： 312："1/3200"; 1000："1/1000"; 2000："1/500"; 10000："1/100"; 20000："1/50"; 66666："1/15";  
    `analogGain`、`digitGain`： The ISO specific value used under manual exposure, such as 100.

- ISO
  + Use `com.pi.pano.PilotSDK#setISO(int value)` to adjust under auto exposure,  
    `value`： available values： 0 ：auto; 1 ：ISO 50; 2 ：ISO 100; 3 ：ISO 200; 4 ：ISO 400; 5 ：ISO 800; 6 ：ISO 1600.
  + Use `com.pi.pano.PilotSDK#setManualISO(int value)` to adjust under manual exposure,  
    `value`： available values： 100 ：ISO 100; 200 ：ISO 200; 400 ：ISO 400; 600 ：ISO 600; 800 ：ISO 800; 1600 ：ISO 1600; 3200 ：ISO 3200.

- EV
  Set EV when it is not automatic exposure.  
  Use `com.pi.pano.PilotSDK#setExposureCompensation(int value)` to adjust,  
  `value`: The value range is -4 to 4, -4 is the darkest, 4 is the brightest

- WB
  Use `com.pi.pano.PilotSDK#setWhiteBalance(int value)` to adjust,  
  `value`： available values： 0：auto; 1：incandescent; 2：fluorescent; 3：daylight; 4：cloudy-daylight.

- Stitching distance
  Control the splicing gap of the camera screen.  
  Use `com.pi.pano.PilotSDK#setStitchingDistance(float distance, float max)` to adjust,  
  `distance`： The value range is -100\~100, 0 is about 2m, 100 means infinity, -100 is about 0.5m;  
  `max`： It is 1.35 on the Era device and 0.9 on the One device.

The following values can be adjusted when taking pictures: exposure time, ISO, EV (when exposure time is manual), WB, stitching distance.
The following values can be adjusted when recording: ISO, EV, stitching distance.



### Play Media File

Add `com.pi.pano.MediaPlayerSurfaceView` view, and use  
`com.pi.pano.MediaPlayerSurfaceView#open(Strin filename)`  
Open local file for playback.



### Troubleshooting Q&A

1. The preview is all black.  
When improper use of camera equipment or improper use environment may cause abnormal equipment, the camera device can be restarted.

2. The definition of live broadcast is too low.  
Live broadcasting has high requirements on the network. Confirm whether the clarity and bit rate of the current network bandwidth used for live broadcasting is feasible. Avoid low bandwidth or long delay.

3. Using the provided 4K streaming library to push h.265 media server is not supported.  
At present, RTMP live only supports H.264, and the 4K and 8K streaming libraries provided can push h.265, but it needs media server and player for processing.

3. The preview temperature is too high when the device is turned on for a long time.  
Normal phenomenon, use preview device to consume performance heat, the device contains fan and other cooling components, the fan can be automatically turned on in the setting.

4. Can the integrated SDK be used on non Pilot OS systems?   
Don't do that.



### License

[License][LICENSE]



### Samples

| Sample | Description |
|:-|:-|
| [photoSample][] [download Apk][photoSample_dl] | Reference for take a photo process |
| [videoSample][] [download Apk][videoSample_dl]| Reference for record video |
| [liveSample][] [download Apk][liveSample_dl]| Reference for RTMP live broadcast 4K and below resolution |
| [live8kSample][] [download Apk][live8kSample_dl]| Reference for RTMP live broadcast PilotLive 8K resolution |



[photoSample]: ./samples/photoSample "photoSample"
[videoSample]: ./samples/videoSample "videoSample"
[liveSample]: ./samples/liveSample "liveSample"
[live8kSample]: ./samples/live8kSample "live8kSample"

[photoSample_dl]: ./samples/apk/photoSample-debug.apk "photoSampleApk"
[videoSample_dl]: ./samples/apk/videoSample-debug.apk "videoSampleApk"
[liveSample_dl]: ./samples/apk/liveSample-debug.apk "liveSampleApk"
[live8kSample_dl]: ./samples/apk/live8kSample-debug.apk "live8kSampleApk"

[zh-CN]: ./README_zh-CN.md "中文"

[LICENSE]: ./LICENSE "LICENSE"
