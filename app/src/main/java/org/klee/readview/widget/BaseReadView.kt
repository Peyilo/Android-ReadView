package org.klee.readview.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import org.klee.readview.delegate.CoverPageDelegate
import org.klee.readview.delegate.PageDelegate
import org.klee.readview.entities.PageDirection
import kotlin.math.abs

/**
 * 负责提供多种翻页模式下的动画实现
 */
open class BaseReadView(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val scrollSlop = touchSlop

    internal lateinit var curPageView: ReadPage
    internal lateinit var prePageView: ReadPage
    internal lateinit var nextPageView: ReadPage

    var shadowWidth: Int = 30

    private var isMove = false
    private var isPageMove = false
    var longPress = false

    val startPoint by lazy { PointF() }         // 第一次DOWN事件的坐标
    val lastPoint by lazy { PointF() }          // 上一次触摸的坐标
    val touchPoint by lazy { PointF() }         // 当前触摸的坐标

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
        pageDelegate.onLayout()
    }

    private fun upTouchPoint(event: MotionEvent) {
        lastPoint.set(touchPoint)
        touchPoint.set(event.x, event.y)
    }

    private fun upStartPointer(x: Float, y: Float) {
        startPoint.set(x, y)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        upTouchPoint(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                upStartPointer(event.x, event.y)
                pageDelegate.onTouch(event)
            }
            MotionEvent.ACTION_MOVE -> {
                // touchSlop确定是的isMove，该标记位用来区分点击和滑动
                if (!isMove) {
                    isMove = abs(startPoint.x - event.x) > touchSlop || abs(startPoint.x - event.y) > touchSlop
                }
                // scrollSlop确定的是isPageMove，该标记位用来确定页面是否发生了滑动
                if (!isPageMove) {
                    isPageMove = abs(startPoint.x - event.x) > scrollSlop || abs(startPoint.x - event.y) > scrollSlop
                }
                if (isMove) {
                    // longPress = false
                    if (isPageMove) {
                        pageDelegate.onTouch(event)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                pageDelegate.onTouch(event)
            }
        }
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        pageDelegate.computeScrollOffset()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let {
            pageDelegate.dispatchDraw(canvas)
        }
    }

    open fun hasNextPage() = true

    open fun hasPrevPage(): Boolean = true

    internal open fun updateChildView(convertView: ReadPage, direction: PageDirection) = convertView

}