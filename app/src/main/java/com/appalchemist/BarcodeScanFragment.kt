package com.appalchemist

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.nalbertson.vision.BarcodeProcessor
import com.nalbertson.vision.views.BarcodeGraphic
import com.nalbertson.vision.views.GraphicOverlay

class BarcodeScanFragment: Fragment() {

    private var barcodeProcessor: BarcodeProcessor? = null
    private var listener: BarcodeProcessor.BarcodeDetectedListener? = null

    fun setBarcodeListener(listener: BarcodeProcessor.BarcodeDetectedListener) {
        this.listener = listener
        barcodeProcessor?.listener = listener
    }

    fun stop() {
        barcodeProcessor?.stop()
    }

    fun startScanning() {
        barcodeProcessor?.graphicOverlay?.clear()
        barcodeProcessor?.startScan()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
            View = inflater.inflate(R.layout.fragment_barcode_scan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val overlay = view.findViewById<GraphicOverlay<BarcodeGraphic>>(R.id.graphicOverlay)
        val surfaceView = view.findViewById<SurfaceView>(R.id.surfaceView)

        barcodeProcessor = BarcodeProcessor(activity!!, surfaceView, overlay)
        barcodeProcessor?.listener = listener
    }

    override fun onResume() {
        super.onResume()

        barcodeProcessor?.startScan()
    }
}