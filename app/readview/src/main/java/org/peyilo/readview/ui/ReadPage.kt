package org.peyilo.readview.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import org.peyilo.readview.annotation.ThreadSafe

private const val TAG = "ReadPage"
class ReadPage(
    context: Context, attrs: AttributeSet? = null, var position: Position
): ViewGroup(context, attrs) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, Position.CUR)

    lateinit var layout: View               // 填充的布局
        private set
    lateinit var content: ReadContent              // 正文显示视图
        private set
    var header: View? = null                // 页眉视图
        private set
    var footer: View? = null                // 页脚视图
        private set

    @ThreadSafe
    @Synchronized
    fun bindLayout(
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

    enum class Position {
        PREV, CUR, NEXT
    }
}