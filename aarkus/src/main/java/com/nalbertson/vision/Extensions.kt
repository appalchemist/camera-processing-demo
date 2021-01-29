package com.nalbertson.vision

import android.content.Context
import android.util.DisplayMetrics


fun Float.convertDpToPixel(context: Context): Float {
    return this * (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}

fun Float.convertPixelsToDp(context: Context): Float {
    return this / (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}