package com.pi.pano.sample.record

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.pano.ChangeResolutionListener
import com.pi.pano.MediaRecorderListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val TAG = MainActivity::class.java.simpleName

/**
 * This example shows the general process of recording.
 * After confirming that the SDK is created successfully,
 * set the preview according to the video resolution you recorded,
 * Then you can use the provided methods for video recording.
 * During this period, the picture effects in the preview can be adjusted in real time, such as ev, iso, etc.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mPilotSDK: PilotSDK
    private var isRunning: Boolean = false

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
            PilotSDK.CAMERA_PREVIEW_1920_1200_30 // the resolution you need, it includes the frame rate
            , object : ChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                    // set preview mode
                    PilotSDK.setPreviewMode(0, 0F, true)
                }
            })
    }

    fun onClickRecodeVideo(view: View) {
        if (!isRunning) {
            PilotSDK.startRecord(
                File(
                    Environment.getExternalStorageDirectory(), "myVideos"
                ).absolutePath + File.separator, generateFileName(), 0 // video codec
                , 2, 3840,
                false, 0,
                object : MediaRecorderListener {
                    override fun onError(what: Int) {
                        // error callback
                        Log.d(TAG, "recode video error:${what}")
                    }
                }
            )
            isRunning = true
        } else {
            PilotSDK.stopRecord("")
            isRunning = false
        }
        view.isSelected = isRunning
    }

    private fun generateFileName(): String {
        return SimpleDateFormat("yyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
    }
}
