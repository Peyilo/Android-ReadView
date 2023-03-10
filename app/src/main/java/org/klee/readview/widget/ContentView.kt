package org.klee.readview.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.IndexBean
import org.klee.readview.widget.api.BitmapProvider

class ContentView(context: Context, attributeSet: AttributeSet? = null)
    : View(context, attributeSet) {

    lateinit var config: ContentConfig
    lateinit var bitmapProvider: BitmapProvider
    val indexBean by lazy { IndexBean() }
    private val bitmap get(): Bitmap {
        val curBitmap = bitmapProvider.getBitmap(indexBean)
        cache?.recycle()
        cache = curBitmap
        return curBitmap
    }
    private var cache: Bitmap? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 初始化尺寸参数
        if (!config.contentDimenInitialized) {
            config.setContentDimen(width, height)
        }
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(bitmap, 0F, 0F, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cache?.apply {
            if (!cache!!.isRecycled) {
                cache!!.recycle()
            }
        }
    }
}