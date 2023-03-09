package org.klee.readview.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.IndexBean

class ContentView(context: Context, attributeSet: AttributeSet? = null)
    : View(context, attributeSet) {

    lateinit var requester: (indexBean: IndexBean) -> Bitmap
    val indexBean by lazy { IndexBean() }

    var content: Bitmap? = null
        set(value) {
            if (field != null && !field!!.isRecycled) {  // 回收bitmap
                field!!.recycle()
            }
            field = value
        }

    // private fun getContent() = requester(indexBean)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 初始化尺寸参数
        if (!ContentConfig.contentDimenInitialized) {
            ContentConfig.setContentDimen(width, height)
        }
    }

    private val paint = Paint()
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        content?.let {
            if (!content!!.isRecycled) {
                canvas?.drawBitmap(content!!, 0F, 0F, paint)
            }
        }

    }
}