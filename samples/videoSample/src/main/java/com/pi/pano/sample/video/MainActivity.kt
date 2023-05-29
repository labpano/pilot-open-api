package com.pi.pano.sample.video

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.pano.DefaultChangeResolutionListener
import com.pi.pano.DefaultVideoChangeResolutionListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK
import com.pi.pano.ResolutionParams
import com.pi.pano.annotation.PiResolution
import com.pi.pano.annotation.PiVideoEncode
import com.pi.pano.wrap.PreviewWrap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        mPilotSDK = PreviewWrap.initPanoView(
            findViewById(R.id.vg_preview),
            object : PanoSDKListener {
                override fun onPanoCreate() {
                    // You can get this preview picture and set the parameters for it to your needs.
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
        PreviewWrap.changeResolution(
            ResolutionParams.Factory.createParamsForUnStitchVideo(PiResolution._5_7K, "30"),
            object : DefaultVideoChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                }
            })
    }

    fun onClickRecodeVideo(view: View) {
        if (!isRunning) {
            PilotSDK.startRecord(
                getExternalFilesDir("myVideos")!!.absolutePath + File.separator,
                generateFileName(),
                PiVideoEncode.h_264,
                5760,
                2880,
                30,
                2,
                false,
                0,
                120 * 1024 * 1024
            )
            isRunning = true
        } else {
            PilotSDK.stopRecord(true, true)
            isRunning = false
        }
        view.isSelected = isRunning
    }

    private fun generateFileName(): String {
        return SimpleDateFormat("yyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
    }
}
