package com.pi.pano.live8k.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.StreamSender.StreamSender
import com.pi.pano.ChangeResolutionListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK

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
        mPilotSDK = PilotSDK(
            this,
            findViewById(R.id.vg_preview),
            object : PanoSDKListener {
                override fun onSDKCreate() {
                    // You can get thie preview picture and set the parameters for it to your needs.
                    initPreviewParameter()
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
    }

    private fun initPreviewParameter() {
        PilotSDK.changeCameraResolution(
            PilotSDK.CAMERA_PREVIEW_400_250_24 // the resolution you need, it includes the frame rate
            , object : ChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                    // set preview mode
                    PilotSDK.setPreviewMode(0, 0F, true)
                }
            })
    }

    fun onClickLive8k(view: View) {
        if (!isRunning) {
            startPusher("rtmp://192.168.8.163/live/8k_live")
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