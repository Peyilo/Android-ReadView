package org.klee.readview.delegate

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.widget.Scroller
import org.klee.readview.BaseReadView

abstract class PageDelegate (val readView: BaseReadView) {

    private val context: Context get() = readView.context

    protected val prevPage get() = readView.prePageView
    protected val nextPage get() = readView.nextPageView
    protected val curPage  get() = readView.curPageView

    protected val startPoint get() = readView.startPoint
    protected val touchPoint get() = readView.touchPoint
    protected val lastPoint get() = readView.lastPoint

    protected val shadowWidth get() = readView.shadowWidth

    protected val scroller by lazy { Scroller(context) }
    abstract fun onLayout()
    abstract fun abortAnim()                                // 停止动画
    abstract fun startAnim()                                // 开始动画
    abstract fun onScroll(event: MotionEvent)               // 控制拖动
    fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abortAnim()
            }
            MotionEvent.ACTION_MOVE -> {
                onScroll(event)
            }
            MotionEvent.ACTION_UP -> {
                startAnim()
            }
        }
    }
    abstract fun computeScrollOffset()
    abstract fun dispatchDraw(canvas: Canvas)

}