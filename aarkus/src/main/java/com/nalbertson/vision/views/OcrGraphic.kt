package com.nalbertson.vision.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.gms.vision.text.Text

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
class OcrGraphic internal constructor(val overlay: GraphicOverlay<*>, val text: Text?) : GraphicOverlay.Graphic(overlay) {

    var id: Int = 0

    init {

        if (sRectPaint == null) {
            sRectPaint = Paint()
            sRectPaint!!.color = BOUNDING_BOX_COLOR
            sRectPaint!!.style = Paint.Style.STROKE
            sRectPaint!!.strokeWidth = 4.0f
        }

        if (sTextPaint == null) {
            sTextPaint = Paint()
            sTextPaint!!.color = TEXT_COLOR
            sTextPaint!!.textSize = 48.0f
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate()
    }

    /**
     * Checks whether a point is within the bounding box of this graphic.
     * The provided point should be relative to this graphic's containing overlay.

     * @param x An x parameter in the relative context of the canvas.
     * *
     * @param y A y parameter in the relative context of the canvas.
     * *
     * @return True if the provided point is contained within this graphic's bounding box.
     */
    override fun contains(x: Float, y: Float): Boolean {
        // TODO: Check if this graphic's text contains this point.
        if (text == null) {
            return false
        }
        val rect = RectF(text.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        return rect.left < x && rect.right > x && rect.top < y && rect.bottom > y
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        // TODO: Draw the text onto the canvas.
        if (text == null) {
            return
        }

        // Draws the bounding box around the TextBlock.
        val rect = RectF(text.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, sRectPaint!!)

        if (!overlay.hideText) {
            // Render the text at the bottom of the box.
            canvas.drawText(text.value, rect.left, rect.bottom, sTextPaint!!)
        }
    }

    companion object {

        private val TEXT_COLOR = Color.WHITE
        private val BOUNDING_BOX_COLOR = Color.parseColor("#FF5C39")

        private var sRectPaint: Paint? = null
        private var sTextPaint: Paint? = null
    }
}
