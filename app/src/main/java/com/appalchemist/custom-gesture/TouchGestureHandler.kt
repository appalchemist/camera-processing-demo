package com.dudesolutions.velox.customGestureHandler

import android.view.MotionEvent
import android.view.View

class TouchGestureHandler : View.OnTouchListener {
    var isRotateEnabled = true
    var isTranslateEnabled = true
    var isScaleEnabled = true
    var minimumScale = 0.5f
    var maximumScale = 10.0f
    private var mActivePointerId = INVALID_POINTER_ID
    private var mPrevX: Float = 0.toFloat()
    private var mPrevY: Float = 0.toFloat()
    private val mScaleGestureDetector = CustomScaleGestureDetector(ScaleGestureListener())

    interface OnTouchEventCallback {
        fun onTouch(event: MotionEvent)
    }

    var listener: OnTouchEventCallback? = null

    constructor()

    constructor(allowRotate: Boolean, onTouchCallback: OnTouchEventCallback) {
        isRotateEnabled = allowRotate
        listener = onTouchCallback
    }

    private fun adjustAngle(degrees: Float): Float {
        var angleDegrees = degrees
        if (angleDegrees > 180.0f) {
            angleDegrees -= 360.0f
        } else if (degrees < -180.0f) {
            angleDegrees += 360.0f
        }

        return angleDegrees
    }

    private fun move(view: View, info: TransformInfo) {
        computeRenderOffset(view, info.pivotX, info.pivotY)
        adjustTranslation(view, info.deltaX, info.deltaY)

        // Assume that scaling still maintains aspect ratio.
        var scale = view.scaleX * info.deltaScale
        scale = Math.max(info.minimumScale, Math.min(info.maximumScale, scale))
        view.scaleX = scale
        view.scaleY = scale

        val rotation = adjustAngle(view.rotation + info.deltaAngle)
        view.rotation = rotation
    }

    private fun adjustTranslation(view: View, deltaX: Float, deltaY: Float) {
        val deltaVector = floatArrayOf(deltaX, deltaY)
        view.matrix.mapVectors(deltaVector)
        view.translationX = view.translationX + deltaVector[0]
        view.translationY = view.translationY + deltaVector[1]
    }

    private fun computeRenderOffset(view: View, pivotX: Float, pivotY: Float) {
        if (view.pivotX == pivotX && view.pivotY == pivotY) {
            return
        }

        val prevPoint = floatArrayOf(0.0f, 0.0f)
        view.matrix.mapPoints(prevPoint)

        view.pivotX = pivotX
        view.pivotY = pivotY

        val currPoint = floatArrayOf(0.0f, 0.0f)
        view.matrix.mapPoints(currPoint)

        val offsetX = currPoint[0] - prevPoint[0]
        val offsetY = currPoint[1] - prevPoint[1]

        view.translationX = view.translationX - offsetX
        view.translationY = view.translationY - offsetY
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        listener?.onTouch(event)
        mScaleGestureDetector.onTouchEvent(view, event)

        if (!isTranslateEnabled) {
            return true
        }

        val action = event.action
        when (action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y

                // Save the ID of this pointer.
                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                // Find the index of the active pointer and fetch its position.
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex != -1) {
                    val currX = event.getX(pointerIndex)
                    val currY = event.getY(pointerIndex)

                    // Only move if the ScaleGestureDetector isn't processing a
                    // gesture.
                    if (!mScaleGestureDetector.isInProgress()) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY)
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER_ID

            MotionEvent.ACTION_UP -> mActivePointerId = INVALID_POINTER_ID

            MotionEvent.ACTION_POINTER_UP -> {
                // Extract the index of the pointer that left the touch sensor.
                val pointerIndex = action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mPrevX = event.getX(newPointerIndex)
                    mPrevY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }

        return true
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }

    private inner class ScaleGestureListener : CustomScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var mPivotX: Float = 0.toFloat()
        private var mPivotY: Float = 0.toFloat()
        private val mPrevSpanVector = Vector2D()

        override fun onScaleBegin(view: View, detector: CustomScaleGestureDetector): Boolean {
            mPivotX = detector.getFocusX()
            mPivotY = detector.getFocusY()
            mPrevSpanVector.set(detector.getCurrentSpanVector())
            return true
        }

        override fun onScale(view: View, detector: CustomScaleGestureDetector): Boolean {
            val info = TransformInfo()
            info.deltaScale = if (isScaleEnabled) detector.getScaleFactor() else 1.0f
            info.deltaAngle = if (isRotateEnabled) mPrevSpanVector.getAngleFromPrevious(detector.getCurrentSpanVector()) else 0.0f
            info.deltaX = if (isTranslateEnabled) detector.getFocusX() - mPivotX else 0.0f
            info.deltaY = if (isTranslateEnabled) detector.getFocusY() - mPivotY else 0.0f
            info.pivotX = mPivotX
            info.pivotY = mPivotY
            info.minimumScale = minimumScale
            info.maximumScale = maximumScale

            move(view, info)
            return false
        }
    }

    private inner class TransformInfo {

        var deltaX: Float = 0.toFloat()
        var deltaY: Float = 0.toFloat()
        var deltaScale: Float = 0.toFloat()
        var deltaAngle: Float = 0.toFloat()
        var pivotX: Float = 0.toFloat()
        var pivotY: Float = 0.toFloat()
        var minimumScale: Float = 0.toFloat()
        var maximumScale: Float = 0.toFloat()
    }
}