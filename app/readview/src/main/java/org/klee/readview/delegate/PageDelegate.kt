package org.klee.readview.delegate

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.IntRange
import org.klee.readview.widget.BaseReadView

private const val TAG = "PageDelegate"
abstract class PageDelegate (
    val readView: BaseReadView,
    _horizontalSlideAngle: Int = 45
) {

    protected val prevPage get() = readView.prePageView
    protected val nextPage get() = readView.nextPageView
    protected val curPage  get() = readView.curPageView

    protected val startPoint get() = readView.startPoint
    protected val touchPoint get() = readView.touchPoint
    protected val lastPoint get() = readView.lastPoint

    @IntRange(from = 0, to = 90)
    protected val horizontalSlideAngle = _horizontalSlideAngle

    /**
     * 该函数将会在onLayout()期间调用，用于完成PageView的scroll值的设置
     */
    open fun onScrollValue() = Unit

    /**
     * 根据角度判断手势方向
     */
    private fun getGestureDirection(angle: Int): GestureDirection {
        return when(angle) {
            in  -horizontalSlideAngle..horizontalSlideAngle ->
                GestureDirection.TO_RIGHT
            in (180 - horizontalSlideAngle)..180 ->
                GestureDirection.TO_LEFT
            in -180..(-180 + horizontalSlideAngle) ->
                GestureDirection.TO_LEFT
            in horizontalSlideAngle..(180 - horizontalSlideAngle) ->
                GestureDirection.DOWN
            in (-180 + horizontalSlideAngle)..-horizontalSlideAngle ->
                GestureDirection.UP
            else ->
                throw IllegalStateException("angle = $angle")
        }
    }

    /**
     * 根据角度推出手势方向，再完成页面滑动方向的初始化，并将页面滑动方向返回。
     */
    fun initDirection(angle: Int): PageDirection {
        val initGestureDirection = getGestureDirection(angle)
        return initPageDirection(initGestureDirection)
    }

    /**
     * ReadView将会根据返回值判断本系列滑动事件是否还需要交由PageDelegate处理，规则如下：
     * 1. 返回值为NONE，之后的MOVE、UP事件不再交由PageDelegate处理；
     * 2. 返回值为NEXT，ReadPage会结合hasNextPage()来判断，如果没有下一页，之后的MOVE、UP事件不再交由PageDelegate处理；
     * 2. 返回值PREV，ReadPage会结合hasPrevPage()来判断，如果没有上一页，之后的MOVE、UP事件不再交由PageDelegate处理。
     */
    protected open fun initPageDirection(initGestureDirection: GestureDirection) = PageDirection.NONE

    /**
     * DOWN事件总会传入该方法，但是MOVE、UP事件传入该方法时代表可滑动，有两种情况：
     * 1. 本轮滑动事件中initPageDirection()返回值为NEXT，且hasNextPage()返回值为true
     * 2. 本轮滑动事件中initPageDirection()返回值为PREV，且hasPrevPage()返回值为true
     */
    fun onTouch(event: MotionEvent) {
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

    protected abstract fun startAnim(pageDirection: PageDirection)

    protected open fun onMove() = Unit                                  // 控制拖动

    protected open fun onUpdateChildView(direction: PageDirection) = Unit

    open fun computeScrollOffset() = Unit

    open fun dispatchDraw(canvas: Canvas) = Unit

    fun nextPage() {
        abortAnim()
        startAnim(PageDirection.NEXT)
    }

    fun prevPage() {
        abortAnim()
        startAnim(PageDirection.PREV)
    }

}