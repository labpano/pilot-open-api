package com.pi.pano.live8k.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.StreamSender.StreamSender
import com.pi.pano.ChangeResolutionListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK
import com.pi.pano.annotation.PiPreviewMode
import com.pi.pano.annotation.PiPushResolution
import com.pi.pano.helper.PreviewHelper

private val TAG = MainActivity::class.java.simpleName

/**
 * This example shows the general process of real-time broadcasting at 8k resolution.
 * If you have broadcast live 8k, you may need to use PiPlayer to play.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mPilotSDK: PilotSDK
    private var isRunning: Boolean = false
    private lateinit var mStreamSender: StreamSender

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
            PiPushResolution._8K,
            object : ChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                    // set preview mode
                    PilotSDK.setPreviewMode(PiPreviewMode.planet, 0F, true)
                }
            })
    }

    fun onClickLive8k(view: View) {
        if (!isRunning) {
            val pushURL = "rtmp://192.168.8.163:1936/live/8k_live" // push url 替换为需要的
            startPusher(pushURL)
            isRunning = true
        } else {
            stopPusher()
            isRunning = false
        }
        view.isSelected = isRunning
    }

    private fun startPusher(pushURL: String) {
        mStreamSender = StreamSender()
        mStreamSender.start(
            PilotSDK.getCameras(),
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
    }

    private fun stopPusher() {
        mStreamSender.stop(false)
    }
}