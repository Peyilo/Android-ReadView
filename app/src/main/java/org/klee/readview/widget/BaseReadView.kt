package org.klee.readview.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import org.klee.readview.config.ContentConfig
import org.klee.readview.delegate.CoverPageDelegate
import org.klee.readview.delegate.PageDelegate
import org.klee.readview.delegate.PageDirection
import androidx.annotation.IntRange
import org.klee.readview.delegate.GestureDirection
import org.klee.readview.utils.angle
import org.klee.readview.utils.apartFrom

/**
 * 负责提供多种翻页模式下的动画实现
 */
private const val TAG = "BaseReadView"
open class BaseReadView(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {

    internal val contentConfig by lazy { ContentConfig() }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    internal lateinit var curPageView: PageView
    internal lateinit var prePageView: PageView
    internal lateinit var nextPageView: PageView

    var shadowWidth: Int = 15

    private var isMove = false
    @IntRange(from = 0, to = 90)
    private var horizontalScrollAngle = 45

    val startPoint by lazy { PointF() }         // 第一次DOWN事件的坐标
    val lastPoint by lazy { PointF() }          // 上一次触摸的坐标
    val touchPoint by lazy { PointF() }         // 当前触摸的坐标
    var initDirection =  GestureDirection.NONE     // 本轮事件中最初的手势移动方向
        private set
    var lastDirection = GestureDirection.NONE      // 本轮事件中上次的手势移动方向
        private set

    private var pageDelegate0: PageDelegate? = null
    private val pageDelegate: PageDelegate get() {
        if (pageDelegate0 == null) {
            pageDelegate0 = CoverPageDelegate(this)
        }
        return pageDelegate0!!
    }
    fun setPageDelegate(pageDelegate: PageDelegate) {
        this.pageDelegate0 = pageDelegate
    }

    /**
     * 对外提供的ReadPage自定义API函数，可以通过该函数配置PageView的内容视图、页眉视图、页脚视图
     * @param initializer 初始化器
     */
    open fun initPage(initializer: (pageView: PageView, position: Int) -> Unit) {
        curPageView = PageView(context)
        prePageView = PageView(context)
        nextPageView = PageView(context)
        initializer(curPageView, 0)
        initializer(prePageView, -1)
        initializer(nextPageView, 1)
        if (!(curPageView.initFinished && prePageView.initFinished && nextPageView.initFinished)) {
            throw IllegalStateException("没有完成PageView的初始化！")
        }
        curPageView.content.config = contentConfig
        prePageView.content.config = contentConfig
        nextPageView.content.config = contentConfig
        addView(nextPageView)
        addView(curPageView)
        addView(prePageView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
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

    /**
     * 根据角度判断手势方向
     */
    private fun getGestureDirection(angle: Int): GestureDirection {
        return when(angle) {
            in  -horizontalScrollAngle..horizontalScrollAngle ->
                GestureDirection.TO_RIGHT
            in (180 - horizontalScrollAngle)..180 ->
                GestureDirection.TO_LEFT
            in -180..(-180 + horizontalScrollAngle) ->
                GestureDirection.TO_LEFT
            in horizontalScrollAngle..(180 - horizontalScrollAngle) ->
                GestureDirection.DOWN
            in (-180 + horizontalScrollAngle)..-horizontalScrollAngle ->
                GestureDirection.UP
            else ->
                throw IllegalStateException("angle = $angle")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        upTouchPoint(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                upStartPointer(event.x, event.y)
                pageDelegate.onTouch(event)
                isMove = false
                initDirection = GestureDirection.NONE
                lastDirection = GestureDirection.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                // touchSlop确定是的isMove，该标记位用来区分点击和滑动事件
                if (!isMove) {
                    isMove = startPoint.apartFrom(touchPoint) > touchSlop
                    if (isMove) {
                        // 确定开始的滑动方向
                        val angle = startPoint.angle(touchPoint)
                        Log.d(TAG, "onTouchEvent: angle = $angle")
                        initDirection = getGestureDirection(angle)
                        Log.d(TAG, "onTouchEvent: initDirection = $initDirection")
                    }
                }
                if (isMove) {
                    pageDelegate.onTouch(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "onTouchEvent: startPoint = $startPoint, touchPoint = $touchPoint")
                Log.d(TAG, "onTouchEvent: ${if (isMove) "滑动事件" else "点击事件"}")
                if (!isMove) {
                    performClick()
                } else {
                    pageDelegate.onTouch(event)
                }
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

    open fun onFlipToPrev() = Unit

    open fun onFlipToNext() = Unit

    /**
     * 翻页完成以后，将会调用该函数进行子视图更新
     */
    internal open fun updateChildView(convertView: PageView,
                                      direction: PageDirection
    ): PageView {
        if (direction == PageDirection.PREV) {
            onFlipToPrev()
        }
        if (direction == PageDirection.NEXT) {
            onFlipToNext()
        }
        return convertView
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        contentConfig.destroy()
    }

}