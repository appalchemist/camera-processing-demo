package com.nalbertson.vision.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.gms.vision.barcode.Barcode
import com.nalbertson.vision.convertDpToPixel

class BarcodeGraphic internal constructor(overlay: GraphicOverlay<*>, val barcode: Barcode?) : GraphicOverlay.Graphic(overlay) {

    var context: Context = overlay.context

    private val rectPaint = Paint()

    init {
        rectPaint.color = BOUNDING_BOX_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 4.0f

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
        if (barcode == null) {
            return false
        }
        val rect = RectF(barcode.boundingBox)
        rect.left = scaleX(rect.left)
        rect.top = scaleY(rect.top)
        rect.right = scaleX(rect.right)
        rect.bottom = scaleY(rect.bottom)

        rect.bottom += getOffsetToMinHeight(rect)

        return rect.left < x && rect.right > x && rect.top < y && rect.bottom > y
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        // TODO: Draw the text onto the canvas.
        if (barcode == null) {
            return
        }

        // Draws the bounding box around the TextBlock.
        val rect = RectF(barcode.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)

        rect.bottom += getOffsetToMinHeight(rect)

        canvas.drawRect(rect, rectPaint)
    }

    private fun getOffsetToMinHeight(rect: RectF): Float {
        var offset = 0f
        val minHeight = MIN_BOX_HEIGHT.convertDpToPixel(context)
        if (rect.height() < minHeight.convertDpToPixel(context)) {
            offset = minHeight - rect.height()
        }
        return offset
    }

    companion object {
        private val BOUNDING_BOX_COLOR = Color.parseColor("#FF5C39")
        private const val MIN_BOX_HEIGHT = 40f
    }
}