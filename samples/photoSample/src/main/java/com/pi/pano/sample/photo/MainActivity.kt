package com.pi.panp.sample.photo

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.pano.ChangeResolutionListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK
import com.pi.pano.TakePhotoListener
import com.pi.pano.sample.photo.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val TAG = MainActivity::class.java.simpleName

/**
 * This example shows the general process of take photo.
 * After confirming that the SDK is created successfully,
 * set the preview according to the photo resolution,
 * Then you can use the provided methods for take photo.
 * Before taking a picture, you can adjust the preview effect, such as exposure, ISO, EV, WB, etc.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mPilotSDK: PilotSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupPano()
    }

    private fun setupPano() {
        // setup pano into your view containers
        mPilotSDK = PilotSDK(this, findViewById(R.id.vg_preview), object : PanoSDKListener {
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
            PilotSDK.CAMERA_PREVIEW_4048_2530_15 // the resolution you need, it includes the frame rate
            , object : ChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                    // set preview mode
                    PilotSDK.setPreviewMode(0, 0F, true)
                }
            })
    }

    fun onClickTakePhoto(view: View) {
        val progressDialog = ProgressDialog(this);
        progressDialog.show()
        progressDialog.setMessage("Take Photo...")
        PilotSDK.takePhoto(
            generateFileName() // photo file name
            , PilotSDK.CAMERA_PREVIEW_4048_2530_15[1] * 4 // width * 4,
            , PilotSDK.CAMERA_PREVIEW_4048_2530_15[0] // height
            , object : TakePhotoListener() {
                override fun onTakePhotoComplete(errorCode: Int) {
                    super.onTakePhotoComplete(errorCode)
                    Log.d(TAG, "take photo complete,${errorCode}")
                    // take photo complete, you can get the file where you specify path,if it succeeds
                    progressDialog.dismiss()
                }
            }.apply {
                mStitchDirPath =
                    File(
                        Environment.getExternalStorageDirectory(), "myPhotos"
                    ).absolutePath + File.separator // specify the directory where the file is stored
            }
        )
    }

    private fun generateFileName(): String {
        return SimpleDateFormat("yyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
    }
}
