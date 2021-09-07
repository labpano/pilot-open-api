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
import com.pi.pano.annotation.PiPreviewMode
import com.pi.pano.annotation.PiVideoResolution
import com.pi.pano.helper.PreviewHelper
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
        PreviewHelper.changeCameraResolutionForVideo(
            PiVideoResolution._2K // the resolution you need, it includes the frame rate
            , false,
            object : ChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                    // set preview mode
                    PilotSDK.setPreviewMode(PiPreviewMode.planet, 0F, true)
                }
            })
    }

    fun onClickRecodeVideo(view: View) {
        if (!isRunning) {
            PilotSDK.startRecord(
                File(
                    Environment.getExternalStorageDirectory(), "myVideos"
                ).absolutePath + File.separator, generateFileName(), 0 // video codec
                , 1920, 2,
                false, 1f, 0,
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
