package org.klee.readview.delegate

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import android.widget.Scroller
import androidx.annotation.IntRange
import org.klee.readview.widget.BaseReadView

private const val TAG = "PageDelegate"
abstract class PageDelegate (val readView: BaseReadView) {

    private val context: Context get() = readView.context

    protected val prevPage get() = readView.prePageView
    protected val nextPage get() = readView.nextPageView
    protected val curPage  get() = readView.curPageView

    protected val startPoint get() = readView.startPoint
    protected val touchPoint get() = readView.touchPoint
    protected val lastPoint get() = readView.lastPoint

    private var initGestureDirection = GestureDirection.NONE

    protected val scroller by lazy { Scroller(context) }

    @IntRange(from = 0, to = 90)
    var horizontalScrollAngle = 45

    open fun onScrollValue() = Unit

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

    /**
     * 初始化最初的滑动方向
     */
    fun onInitDirection(angle: Int): PageDirection {
        initGestureDirection = getGestureDirection(angle)
        return onInitDirectionFinished(initGestureDirection)
    }

    protected open fun onInitDirectionFinished(initGestureDirection: GestureDirection) = PageDirection.NONE

    fun onTouch(event: MotionEvent) {
        // 决定是否拦截此次事件
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN) {
            abortAnim()
        }
        when (action) {
            MotionEvent.ACTION_MOVE ->
                onMove()
            MotionEvent.ACTION_UP -> {
                val endPageDirection = onFlip()
                Log.d(TAG, "onTouch: endPageDirection = $endPageDirection")
                onUpdateChildView(endPageDirection)
            }
            else -> Unit
        }
    }

    protected open fun abortAnim() = Unit                                // 停止动画

    protected open fun onFlip(): PageDirection = PageDirection.NONE   // 开始动画

    protected open fun onMove() = Unit                                  // 控制拖动

    protected open fun onUpdateChildView(direction: PageDirection) = Unit

    open fun computeScrollOffset() = Unit

    open fun dispatchDraw(canvas: Canvas) = Unit

}