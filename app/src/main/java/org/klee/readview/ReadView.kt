package org.klee.readview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import org.klee.readview.delegate.CoverPageDelegate
import org.klee.readview.delegate.PageDelegate
import org.klee.readview.entities.PageDirection

class ReadView(context: Context, attributeSet: AttributeSet)
    : ViewGroup(context, attributeSet) {

    internal lateinit var curPageView: ReadPage
    internal lateinit var prePageView: ReadPage
    internal lateinit var nextPageView: ReadPage

    var pageDelegate: PageDelegate = CoverPageDelegate(this)

    // 初始化ReadPage
    fun initPage(initializer: (readPage: ReadPage, position: Int) -> Unit) {
        curPageView = ReadPage(context)
        prePageView = ReadPage(context)
        nextPageView = ReadPage(context)
        initializer(curPageView, 0)
        initializer(prePageView, -1)
        initializer(nextPageView, 1)
        addView(nextPageView)
        addView(curPageView)
        addView(prePageView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
        require(childCount == 3)
        // 设置子view的测量大小
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
            val height = child.measuredHeight
            val width = child.measuredWidth
            // 子view全部叠放在一起，但是最顶层的子view被设置了scrollX，所以滑出了屏幕
            child.layout(0, 0, width, height)
        }
        prePageView.scrollTo(-width, 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (pageDelegate.onTouchEvent(event)) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        pageDelegate.computeScrollOffset()
    }

    fun hasNextPage() = true
    fun hasPrevPage() = true

    internal fun updateChildView(convertView: ReadPage,
                                 direction: PageDirection) : ReadPage {
        return convertView
    }
}