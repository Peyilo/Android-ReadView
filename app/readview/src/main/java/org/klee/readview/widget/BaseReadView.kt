/**
 * MIT License

 * Copyright (c) 2023 Klee

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import org.klee.readview.delegate.*
import org.klee.readview.utils.angle
import org.klee.readview.utils.apartFrom

/**
 * 负责提供多种翻页模式下的动画实现
 */
private const val TAG = "BaseReadView"
open class BaseReadView(context: Context, attributeSet: AttributeSet?)
    : ViewGroup(context, attributeSet) {

    internal val contentConfig by lazy { org.klee.readview.config.ContentConfig() }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    internal lateinit var curPageView: PageView
    internal lateinit var prePageView: PageView
    internal lateinit var nextPageView: PageView

    var shadowWidth: Int = 15

    private var onClickRegionListener: OnClickRegionListener? = null

    private var isMove = false

    val startPoint by lazy { PointF() }         // 第一次DOWN事件的坐标
    val lastPoint by lazy { PointF() }          // 上一次触摸的坐标
    val touchPoint by lazy { PointF() }         // 当前触摸的坐标
    private var initDirection =  PageDirection.NONE     // 本轮事件中最初的手势移动方向

    var flipMode = FlipMode.NoAnim
        set(value) {
            // 翻页模式改变了
            if (field != value && tempValue != null) {
                tempValue = createPageDelegate(value)
            }
            field = value
        }
    private var tempValue: PageDelegate? = null
    private val pageDelegate: PageDelegate get() {
        if (tempValue == null) {
            tempValue = createPageDelegate(flipMode)
        }
        return tempValue!!
    }

    /**
     * 创建一个PageDelegate
     */
    private fun createPageDelegate(mode: FlipMode): PageDelegate {
        return when (mode) {
            FlipMode.NoAnim ->
                NoAnimPageDelegate(this)
            FlipMode.Cover ->
                CoverPageDelegate(this)
        }
    }

    fun setPageDelegate(pageDelegate: PageDelegate) {
        this.tempValue = pageDelegate
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
        pageDelegate.onScrollValue()
    }

    private fun upTouchPoint(event: MotionEvent) {
        lastPoint.set(touchPoint)
        touchPoint.set(event.x, event.y)
    }

    private fun upStartPointer(x: Float, y: Float) {
        startPoint.set(x, y)
    }

    /**
     * 判断页面是否发生了翻页
     */
    private fun isPageMove(): Boolean {
        return (initDirection == PageDirection.PREV && hasPrevPage())
                || initDirection == PageDirection.NEXT && hasNextPage()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        upTouchPoint(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                upStartPointer(event.x, event.y)
                pageDelegate.onTouch(event)
                isMove = false
                initDirection = PageDirection.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isMove) {
                    // touchSlop确定是的isMove，该标记位用来区分点击和滑动事件
                    isMove = startPoint.apartFrom(touchPoint) > touchSlop
                    if (isMove) {
                        // 计算滑动的角度
                        val angle = startPoint.angle(touchPoint)
                        Log.d(TAG, "onTouchEvent: angle = $angle")
                        initDirection = pageDelegate.initDirection(angle)
                    }
                }
                if (isPageMove()) {
                    pageDelegate.onTouch(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "onTouchEvent: startPoint = $startPoint, touchPoint = $touchPoint")
                if (!isMove) {
                    // 触发点击事件
                    val xPercent = startPoint.x / width * 100
                    val yPercent = startPoint.y / height * 100
                    if (!onClickRegion(xPercent.toInt(), yPercent.toInt())) {
                        // 如果onClickRegion()没有拦截该点击事件，就触发View的OnClickListener的回调
                        performClick()
                    }
                }
                if (isPageMove()) {
                    // 本系列事件为滑动事件，触发滑动动画
                    pageDelegate.onTouch(event)
                }
            }
        }
        return true
    }

    /**
     * 点击事件回调
     * @param xPercent 点击的位置在x轴方向上的百分比，例如xPercent=50，表示点击的位置为屏幕x轴方向上的中间
     * @param yPercent 点击的位置在y轴方向上的百分比
     * @return 表示是否拦截本次点击事件，如果onClickRegion()没有拦截该点击事件，就将本次点击事件转交OnClickListener处理
     */
    protected open fun onClickRegion(xPercent: Int, yPercent: Int): Boolean {
        return onClickRegionListener?.onClickRegion(xPercent, yPercent) ?: false
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

    open fun hasNextPage() = false

    open fun hasPrevPage(): Boolean = false

    protected open fun onFlipToPrev() = Unit

    protected open fun onFlipToNext() = Unit

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

    /**
     * 通过函数调用来翻向下一页
     * @return 是否成功翻页
     */
    fun nextPage(): Boolean {
        if (hasNextPage()) {
            pageDelegate.nextPage()
            return true
        }
        return false
    }

    /**
     * 通过函数调用来翻向上一页
     * @return 是否成功翻页
     */
    fun prevPage(): Boolean {
        if (hasPrevPage()) {
            pageDelegate.prevPage()
            return true
        }
        return false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        contentConfig.destroy()
    }

    interface OnClickRegionListener {
        /**
         * 点击事件回调
         * @param xPercent 点击的位置在x轴方向上的百分比，例如xPercent=50，表示点击的位置为屏幕x轴方向上的中间
         * @param yPercent 点击的位置在y轴方向上的百分比
         * @return 表示是否拦截本次点击事件
         */
        fun onClickRegion(xPercent: Int, yPercent: Int): Boolean
    }

    fun setOnClickRegionListener(listener: OnClickRegionListener) {
        this.onClickRegionListener = listener
    }

    fun setOnClickRegionListener(listener: (xPercent: Int, yPercent: Int) -> Boolean) {
        this.onClickRegionListener = object : OnClickRegionListener {
            override fun onClickRegion(xPercent: Int, yPercent: Int): Boolean {
                return listener(xPercent, yPercent)
            }
        }
    }

}