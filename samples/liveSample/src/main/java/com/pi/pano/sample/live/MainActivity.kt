package com.pi.pano.sample.live

import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.pano.ChangeResolutionListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK
import com.pi.pano.annotation.PiPreviewMode
import com.pi.pano.annotation.PiPushResolution
import com.pi.pano.helper.PreviewHelper
import com.pi.pipusher.IPusher
import com.pi.pipusher.IPusherEncodeVideoListerner
import com.pi.pipusher.IPusherStateListerner
import com.pi.pipusher.PusherFactory

private val TAG = MainActivity::class.java.simpleName

/**
 * This example shows the general process of live broadcast at 4k and below resolutions.
 * At present, most live broadcast platforms can accept resolutions of 4k and below.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mPilotSDK: PilotSDK
    private var isRunning: Boolean = false

    private lateinit var mPusher: IPusher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupPano()
    }

    private fun setupPano() {
        // setup pano into your view containers
        mPilotSDK = PreviewHelper.initPanoView(
            findViewById(R.id.vg_preview),
            object : PanoSDKListener {
                override fun onPanoCreate() {
                    // You can get thie preview picture and set the parameters for it to your needs.
                    initPreviewParameter()
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
    }

    private fun initPreviewParameter() {
        PreviewHelper.changeCameraResolutionForLive(
            PiPushResolution._4K_QUALITY // the resolution you need, it includes the frame rate
            , object : ChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                    // set preview mode
                    PilotSDK.setPreviewMode(PiPreviewMode.planet, 0F, true)
                }
            })
    }

    fun onClickLive(view: View) {
        if (!isRunning) {
            startPush("rtmp://192.168.8.163:1936/live/4k_live")
            isRunning = true
        } else {
            stopPush()
            isRunning = false
        }
        view.isSelected = isRunning
    }

    private fun startPush(pushURL: String) {
        //创建推流对象
        mPusher = PusherFactory.createPusher()

        //设置视频参数
        mPusher.addVideoTrackInfo(
            IPusher.ENCODER_ID_H264, 3840, 1920, 24, 12000000
        )
        //设置音频参数
        mPusher.addAudioTrackInfo(
            IPusher.AUDIO_SOURCE_ID_MIC, IPusher.ENCODER_ID_AAC,
            48000, 2, 16, 128000
        )
        mPusher.addOutput(
            IPusher.OUTPUT_TYPE_RTMP, pushURL, 5000 //5 seconds timeout
        )

        //设置编码回调
        mPusher.setEncodeVideoListerner(object : IPusherEncodeVideoListerner {
            var init = false;
            override fun updateSurface(streamName: String, surface: Surface): Int {
                Log.d(
                    TAG,
                    "updateSurface,streamName:$streamName,$surface"
                )
                if (!init) {
                    PilotSDK.setEncodeInputSurfaceForLive(surface)
                    init = true
                }
                return 0
            }
        })

        //mPusher.addOutputFile(IPusher.OUTPUT_TYPE_MP4, filePath, 3600) // keep the live video as a file at the same time

        mPusher.start(object : IPusherStateListerner {
            override fun notifyPusherState(
                state: IPusherStateListerner.PusherState, errorCode: Int, message: String
            ) {
                Log.d(
                    TAG, "notifyPusherState,state:$state,errorCode:$errorCode,message:$message"
                )
            }
        })
    }

    private fun stopPush() {
        PilotSDK.setEncodeInputSurfaceForLive(null)
        mPusher.stop()
    }
}
