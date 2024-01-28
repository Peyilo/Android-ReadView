package org.peyilo.readview.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View

private const val TAG = "ContentView"
/**
 * 正文显示视图
 */
class ContentView(context: Context, attrs: AttributeSet? = null):
    View(context, attrs), ReadContent {

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw: ")
    }


}