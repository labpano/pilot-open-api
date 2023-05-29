package com.pi.panp.sample.photo

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pi.pano.CaptureParams
import com.pi.pano.DefaultPhotoChangeResolutionListener
import com.pi.pano.PanoSDKListener
import com.pi.pano.PilotSDK
import com.pi.pano.ResolutionParams
import com.pi.pano.annotation.PiResolution
import com.pi.pano.sample.photo.R
import com.pi.pano.wrap.IPhotoListener
import com.pi.pano.wrap.PhotoWrap
import com.pi.pano.wrap.PreviewWrap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        mPilotSDK = PilotSDK(
            findViewById(R.id.vg_preview),
            object : PanoSDKListener {
                override fun onPanoCreate() {
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
        PreviewWrap.changeResolution(ResolutionParams.Factory.createParamsForPhoto(PiResolution._5_7K),
            object : DefaultPhotoChangeResolutionListener() {
                override fun onChangeResolution(width: Int, height: Int) {
                    Log.d(TAG, "change resolution to:${width}*${height}")
                }
            })
    }

    fun onClickTakePhoto(view: View) {
        val progressDialog = ProgressDialog(this);
        progressDialog.show()
        progressDialog.setMessage("Take Photo...")

        val params =
            CaptureParams.Factory.createParams(0, generateFileName(), PiResolution._5_7K)
        params.unStitchDirPath =
            getExternalFilesDir("myPhotos")!!.absolutePath + File.separator // specify the directory where the file is stored
        PhotoWrap.takePhoto(params, object :
            IPhotoListener {
            override fun onTakeStart() {
            }

            override fun onTakeStart(index: Int) {
            }

            override fun onTakeSuccess(
                change: Boolean,
                simpleFileName: String?,
                stitchFile: File?,
                unStitchFile: File?
            ) {
                progressDialog.dismiss()
            }

            override fun onTakeError(change: Boolean, errorCode: String?) {
                progressDialog.dismiss()
            }
        })
    }

    private fun generateFileName(): String {
        return SimpleDateFormat("yyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
    }
}
