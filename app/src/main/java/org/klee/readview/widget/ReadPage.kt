package org.klee.readview.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

class ReadPage(context: Context, attributeSet: AttributeSet? = null)
    : ViewGroup(context, attributeSet) {

    lateinit var layout: View
        private set
    lateinit var content: ContentView
        private set
    var header: View? = null
        private set
    var footer: View? = null
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.measure(widthMeasureSpec, heightMeasureSpec)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }

    /**
     * 初始化ReadPage的子view
     */
    fun initLayout(
        @LayoutRes layoutId: Int, @IdRes contentId: Int,
        @IdRes headerId: Int = NONE, @IdRes footerId: Int = NONE
    ) {
        require(layoutId != NONE && contentId != NONE)
        layout = LayoutInflater.from(context).inflate(layoutId, this)
        content = layout.findViewById(contentId)
        if (headerId != NONE) {
            header = layout.findViewById(headerId)
        }
        if (footerId != NONE) {
            footer = layout.findViewById(footerId)
        }
    }

    companion object {
        const val NONE = -1
    }

    fun setContent(bitmap: Bitmap) {
        content.content = bitmap
        if (Looper.getMainLooper().isCurrentThread) {
            content.invalidate()        // 如果当前为主线程，就直接调用invalidate()进行刷新
        } else {
            content.postInvalidate()
        }
    }
}