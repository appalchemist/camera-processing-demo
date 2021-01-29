package com.appalchemist

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nalbertson.vision.BarcodeProcessor

class MainActivity : AppCompatActivity(), OcrFragment.OcrInterface, BarcodeProcessor.BarcodeDetectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBarcodeScan()
    }

    private fun startBarcodeScan() {
        val frag = BarcodeScanFragment()
        frag.setBarcodeListener(this)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.contentView, frag)
            .commit()
    }

    private fun startOCRProcessing() {
        val frag = OcrFragment.newInstance(R.drawable.receipt, false)
        frag.setListener(this)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.contentView, frag)
            .commit()
    }

    override fun onOcrTextSelected(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onRetakeRequested() {
        startOCRProcessing()
    }

    override fun onSelected(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
        startOCRProcessing()
    }

    override fun onDetected() {
    }
}