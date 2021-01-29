package com.dudesolutions.velox.customGestureHandler

import android.graphics.PointF

class Vector2D(x: Float = 0f, y: Float = 0f) : PointF(x, y) {

    fun normalize() {
        val length = Math.sqrt((x * x + y * y).toDouble()).toFloat()
        x /= length
        y /= length
    }
}

fun Vector2D.getAngleFromPrevious(currentVector: Vector2D): Float {
    this.normalize()
    currentVector.normalize()
    val degrees = 180.0 / Math.PI * (Math.atan2(currentVector.y.toDouble(), currentVector.x.toDouble()) - Math.atan2(this.y.toDouble(), this.x.toDouble()))
    return degrees.toFloat()
}