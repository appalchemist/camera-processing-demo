package com.appalchemist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Environment
import android.view.View
import java.io.File

fun String.isLocalURI(context: Context) : Boolean {
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    if (storageDir != null) {
        if (this.contains(storageDir.absolutePath)) {
            return true
        }
    }
    return false
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun View.takeScreenshot(): Bitmap? {
    if (this.width > 0 && this.height > 0) {
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        return bitmap
    }
    return null
}