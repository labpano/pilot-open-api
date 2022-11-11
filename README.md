[中文][zh-CN]

# Introduction

Used for panoramic sdk on PilotPano device.<br/>
The system on PilotPano is based on Android 10 and Camera 2 access.<br/>
The archive folder contains the compiled aar package, which can be used for integration or self integrated compilation.

### Initialization (load preview)
Initialize with the ``com.pi.pano.PilotSDK.PilotSDK`` class, which will automatically add preview attempts to be added to the view container.<br/>
The effect settings for the preview can be adjusted according to the specific shooting mode, see[Config Reference][CMCR]。<br/>

Switching resolution:```com.pi.pano.PilotSDK#changeCameraResolution```.<br/>
```com.pi.pano.wrap.PreviewWrap``` class, wrapped again, can help loading initialization, setting preview parameters, switching resolution according to shooting mode, etc.

### Photo
Take a photo:```com.pi.pano.PilotSDK#takePhoto``.<br/>
Can be distinguished as: normal photo, street video photo, tour photo and other modes, different modes of configuration reference [Config Reference][CMCR]。<br/>
Reference demo [photoSample][] [download apk][photoSample_apk_dl]

### Video
Start record:```com.pi.pano.PilotSDK#startRecord```;<br/>
Stop record:```com.pi.pano.PilotSDK#stopRecord```.<br/>
Can be distinguished as: normal panoramic video, flat video, slow-motion video, time-lapse video, street video video and other modes, different modes of configuration reference [Config Reference][CMCR]。<br/>
Reference demo [videoSample][] [download apk][videoSample_apk_dl]

### Stitch
```com.pi.pano.StitchingOpticalFlow`` class, used for stitching photos, stitching photos can choose optical flow stitching method.<br/>
```com.pi.pano.StitchingUtil`` class for stitching videos.

### Live
Reference demo [liveSample][] [download apk][liveSample_apk_dl]


[zh-CN]: ./README_zh-CN.md "中文"

[CMCR]: ./CaptureModeConfigurationReference.md "CaptureModeConfigurationReference"

[photoSample]: ./samples/photoSample "photoSample"
[photoSample_apk_dl]: ./samples/apk-samples/photoSample-debug.apk "photoSample-debug.apk"
[videoSample]: ./samples/videoSample "videoSample"
[videoSample_apk_dl]: ./samples/apk-samples/videoSample-debug.apk "videoSample-debug.apk"
[liveSample]: ./samples/liveSample "liveSample"
[liveSample_apk_dl]: ./samples/apk-samples/liveSample-debug.apk "liveSample-debug.apk"
