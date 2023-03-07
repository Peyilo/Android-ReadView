package org.klee.readview.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.klee.readview.config.ContentConfig

class ContentView(context: Context, attributeSet: AttributeSet? = null)
    : View(context, attributeSet) {

    var content: Bitmap? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 初始化尺寸参数
        if (!ContentConfig.contentDimenInitialized) {
            ContentConfig.setContentDimen(width, height)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        content?.let {
            canvas?.drawBitmap(content!!, 0F, 0F, null)
        }
    }
}