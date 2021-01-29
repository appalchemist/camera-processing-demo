package com.nalbertson.vision

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.appalchemist.vision.aarkus.R
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.nalbertson.vision.views.BarcodeGraphic
import com.nalbertson.vision.views.GraphicOverlay
import java.io.IOException

class BarcodeProcessor(val activity: Activity, val surfaceView: SurfaceView,
                       val graphicOverlay: GraphicOverlay<BarcodeGraphic>,
                       supportedFormats: Int = Barcode.ALL_FORMATS) {

    interface BarcodeDetectedListener {
        fun onSelected(value: String)
        fun onDetected()
    }

    var listener: BarcodeDetectedListener? = null

    private var detector = BarcodeDetector.Builder(activity).setBarcodeFormats(supportedFormats).build()
    private var gestureDetector = GestureDetector(activity, SingleTapGestureListener())

    private var cameraSource: CameraSource? = null
    private var autoFocusEnabled: Boolean = true

    var isCameraRunning = false
        private set

    private var surfaceCreated = false
    private var isScanning = false

    init {
        surfaceView.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    initCamera()
                    removeOnGlobalLayoutListener(
                        surfaceView,
                        this
                    )
                }
            })

        graphicOverlay.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            true
        }
    }

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            //we can start barcode after after creating
            surfaceCreated = true
            if (isScanning) {
                startCamera()
            }
        }

        override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {}

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            surfaceCreated = false
            stop()
            surfaceHolder.removeCallback(this)
        }
    }

    private fun initCamera() {
        if (!hasAutoFocus(activity)) {
            autoFocusEnabled = false
        }

        if (!hasCameraHardware(activity)) {
            return
        }

        if (!checkCameraPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_REQUEST_CAMERA_ACCESS
            )
        }

        detector.setProcessor(processor)
        if (!detector.isOperational) {
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = activity.registerReceiver(null, lowstorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(activity, R.string.barcode_low_storage_error, Toast.LENGTH_LONG).show()
            }
        }

        cameraSource = CameraSource.Builder(activity, detector)
            .setAutoFocusEnabled(autoFocusEnabled)
            .setFacing(BACK_CAM)
            .setRequestedPreviewSize(surfaceView.height, surfaceView.width)
            .build()


        //startCameraView will be invoke in void surfaceCreated
        surfaceView.holder.addCallback(surfaceHolderCallback)
    }

    fun startScan() {
        isScanning = true
        //if surface already created, we can start camera
        if (surfaceCreated) {
            startCamera()
        } else {
            //startCameraView will be invoke in void surfaceCreated
            surfaceView.holder.addCallback(surfaceHolderCallback)
        }
    }

    private fun startCamera() {
        if (isCameraRunning) {
            return
        }
        try {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            } else if (!isCameraRunning && cameraSource != null) {
                cameraSource!!.start(surfaceView.holder)
                graphicOverlay.setCameraInfo(
                    cameraSource!!.previewSize.height,
                    cameraSource!!.previewSize.width,
                    BACK_CAM
                )
                isCameraRunning = true
            }
        } catch (ie: IOException) {
            ie.printStackTrace()
        }
    }

    /**
     * Stop camera
     */
    fun stop() {
        isScanning = false
        try {
            if (isCameraRunning && cameraSource != null) {
                cameraSource!!.stop()
                isCameraRunning = false
            }
        } catch (ie: Exception) {
            ie.printStackTrace()
        }

    }

    private val processor = object: Detector.Processor<Barcode> {
        override fun release() {
            graphicOverlay.clear()
        }

        override fun receiveDetections(detections: Detector.Detections<Barcode>) {
            val barcodes = detections.detectedItems
            if (barcodes.size() > 0) {
                activity.runOnUiThread {
                    stop()

                    for (i in 0 until barcodes.size()) {
                        val barcode = barcodes.valueAt(i)
                        val graphic = BarcodeGraphic(graphicOverlay, barcode)
                        graphicOverlay.add(graphic)
                    }
                    listener?.onDetected()
                }
            }
        }
    }

    private fun onTap(rawX: Float, rawY: Float): Boolean {
        val graphic = graphicOverlay.getGraphicAtLocation(rawX, rawY)
        if (graphic?.barcode != null) {
            listener?.onSelected(graphic.barcode.rawValue)
        }
        return true
    }

    private inner class SingleTapGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onTap(e.rawX, e.rawY) || super.onSingleTapConfirmed(e)
        }
    }

    private fun checkCameraPermission(context: Context): Boolean {
        val permission = Manifest.permission.CAMERA
        val res = context.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun hasAutoFocus(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)
    }

    companion object {
        const val PERMISSIONS_REQUEST_CAMERA_ACCESS: Int = 101
        private const val BACK_CAM = CameraSource.CAMERA_FACING_BACK

        private fun removeOnGlobalLayoutListener(v: View,
                                                 listener: ViewTreeObserver.OnGlobalLayoutListener) {
            v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
}