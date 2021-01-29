package com.appalchemist

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.dudesolutions.velox.customGestureHandler.TouchGestureHandler
import com.google.android.gms.vision.text.Text
import com.nalbertson.vision.OcrDetectorProcessor
import com.nalbertson.vision.views.GraphicOverlay
import com.nalbertson.vision.views.OcrGraphic
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_ocr.*
import java.lang.Exception

class OcrFragment : Fragment(), TouchGestureHandler.OnTouchEventCallback {

    interface OcrInterface {
        fun onOcrTextSelected(text: String)
        fun onRetakeRequested()
    }

    private lateinit var graphicOverlay: GraphicOverlay<OcrGraphic>

    private var ocrDetector: OcrDetectorProcessor? = null
    private var gestureDetector: GestureDetector? = null

    private var listener: OcrInterface? = null

    private var showOCRText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setListener(listener: OcrInterface) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_ocr, container, false)

        gestureDetector = GestureDetector(activity, CaptureGestureListener())

        graphicOverlay = view.findViewById(R.id.graphicOverlay)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pictureView.setOnTouchListener(TouchGestureHandler(true, this))

        var nameplate = arguments?.getString(KEY_PATH)
        var drawableRes = arguments?.getInt(KEY_DRAWABLE)
        if (nameplate != null) {
            if (nameplate.isLocalURI(context!!)) {
                nameplate = "file://$nameplate"
            }

            Picasso.get()
                    .load(nameplate)
                    .into(pictureView, imageLoadCallback)
        } else if (drawableRes != null) {
            Picasso.get()
                .load(drawableRes)
                .into(pictureView, imageLoadCallback)
        }

        val fullToolbar = arguments?.getBoolean(KEY_TOOLBAR) ?: false
        if (fullToolbar) {
            retakeBtn.setOnClickListener(onRetakeClick)
        } else {
            retakeBtn.visibility = View.GONE
        }

        rotateBtn.visibility = View.GONE

        rotateBtn.setOnClickListener(onRotateClick)
        ocrTextBtn.setOnClickListener(onOcrTextToggled)
    }

    private val onRotateClick = View.OnClickListener {
        val image = (pictureView?.drawable as BitmapDrawable).bitmap.rotate(90f)
        pictureView?.setImageBitmap(image)
    }

    private val onRetakeClick = View.OnClickListener {
        listener?.onRetakeRequested()
    }

    private val onOcrTextToggled = View.OnClickListener {
        showOCRText = !showOCRText
        if (showOCRText) {
            graphicOverlay.hideText(false)
        } else {
            graphicOverlay.hideText(true)
        }
    }

    private val imageLoadCallback = object: Callback {
        override fun onSuccess() {
            Handler().postDelayed({
                startOCR()
                processImageViewForText()
            }, 300)
        }

        override fun onError(e: Exception?) {
            Log.w("OcrFragment", "error:", e)
        }
    }

    override fun onResume() {
        super.onResume()

        startOCR()

        processImageViewForText()
    }

    override fun onPause() {
        super.onPause()

        ocrDetector?.stop()
    }

    override fun onTouch(event: MotionEvent) {
        gestureDetector?.onTouchEvent(event)

        processImageViewForText()
    }

    fun startOCR() {
        val fragActivity = activity
        if (fragActivity != null) {
            ocrDetector = OcrDetectorProcessor(graphicOverlay, fragActivity)
            ocrDetector?.start()
        }
    }

    fun stopOCR() {
        ocrDetector?.stop()
        ocrDetector = null
    }

    private fun onTap(rawX: Float, rawY: Float): Boolean {
        val graphic = graphicOverlay.getGraphicAtLocation(rawX, rawY)
        val text: Text?
        if (graphic != null) {
            text = graphic.text
            if (text != null) {
                listener?.onOcrTextSelected(text.value)
            }
        }
        return true
    }

    private inner class CaptureGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onTap(e.rawX, e.rawY) || super.onSingleTapConfirmed(e)
        }
    }

    private fun processImageViewForText() {
        pictureViewLayout?.post {
            val screenshot = pictureViewLayout?.takeScreenshot()

            if (screenshot != null) {
                ocrDetector?.processImage(screenshot)
            }
        }
    }

    companion object {
        private const val KEY_TOOLBAR = "toolbar"
        private const val KEY_PATH = "absolute_path"
        private const val KEY_DRAWABLE = "drawable_res"

        @JvmStatic fun newInstance(imagePath: String?, fullToolbar: Boolean = true): OcrFragment {
            val frag = OcrFragment()

            val args = Bundle()
            args.putString(KEY_PATH, imagePath)
            args.putBoolean(KEY_TOOLBAR, fullToolbar)
            frag.arguments = args

            return frag
        }

        @JvmStatic fun newInstance(imageDrawable: Int, fullToolbar: Boolean = true): OcrFragment {
            val frag = OcrFragment()

            val args = Bundle()
            args.putInt(KEY_DRAWABLE, imageDrawable)
            args.putBoolean(KEY_TOOLBAR, fullToolbar)
            frag.arguments = args

            return frag
        }
    }
}