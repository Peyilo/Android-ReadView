package org.klee.readview.delegate

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import android.view.View
import org.klee.readview.widget.BaseReadView
import org.klee.readview.widget.PageView
import kotlin.math.abs

private const val TAG = "CoverPageDelegate"

/**
 * 覆盖翻页的实现
 */
class CoverPageDelegate(readView: BaseReadView) : HorizontalPageDelegate(readView) {

    private val shadowWidth get() = readView.shadowWidth
    private var scrolledView: View? = null

    private val minFlipDistance = 40
    private val animTime = 600

    private val shadowPaint: Paint = Paint()
    private val gradientColors = intArrayOf(-0x71000000, 0x00000000)
    private val gradientPositions = floatArrayOf(0.0f, 1.0f)

    override fun onScrollValue() {
        prevPage.scrollTo(-(readView.width + shadowWidth), 0)
    }

    override fun onInitDirectionFinished(initGestureDirection: GestureDirection): PageDirection {
        super.onInitDirectionFinished(initGestureDirection)
        when (initPageDirection) {
            PageDirection.PREV ->
                scrolledView = prevPage
            PageDirection.NEXT ->
                scrolledView = curPage
            else -> Unit
        }
        return initPageDirection
    }

    override fun abortAnim() {
        if (!scroller.isFinished) {
            scroller.forceFinished(true)
            scrolledView!!.scrollTo(scroller.finalX, scroller.finalY)
        }
    }

    override fun onMove() {
        val distance = startPoint.x - touchPoint.x
        // 控制实时滑动效果
        when (initPageDirection) {
            PageDirection.NEXT -> {
                if (distance > 0) {
                    scrolledView?.apply {
                        scrollTo(distance.toInt(), 0)
                    }
                }
            }
            PageDirection.PREV -> {
                if (distance < 0) {
                    scrolledView?.apply {
                        scrollTo(width + shadowWidth + distance.toInt(), 0)
                    }
                }
            }
            else ->
                throw IllegalStateException()
        }
    }

    override fun onFlip(): PageDirection {
        val distance = startPoint.x - touchPoint.x
        // 处理最终翻页结果
        val scrollX = scrolledView!!.scrollX
        val dx: Int
        // 如果滑动的距离没有超过minFlipDistance，就不进行翻页，应将页面复位
        val endDirection: PageDirection
        if (abs(distance) < minFlipDistance) {
            endDirection = PageDirection.NONE
            dx = if (initPageDirection === PageDirection.NEXT) {
                Log.d(TAG, "startAnim: ${-scrollX}")
                -scrollX
            } else {
                readView.width + shadowWidth - scrollX
            }
        } else {        // 完成翻页
            dx = if (distance > 0) {
                endDirection = PageDirection.NEXT
                readView.width + shadowWidth - scrollX
            } else {
                endDirection = PageDirection.PREV
                -scrollX
            }
        }
        scroller.startScroll(scrollX, 0, dx, 0, animTime)
        readView.invalidate()
        return endDirection
    }

    override fun onUpdateScrollValue(pageDirection: PageDirection, pageView: PageView) {
        // 将距离当前页面最远的页面移除，再进行复用
        when (pageDirection) {
            PageDirection.NEXT ->
                pageView.scrollTo(0, 0)
            PageDirection.PREV ->
                pageView.scrollTo(readView.width + shadowWidth, 0)
            else -> Unit
        }
    }

    override fun computeScrollOffset() {
        if (scroller.computeScrollOffset()) {
            scrolledView!!.scrollTo(scroller.currX, scroller.currY)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (scrolledView != null) {
            readView.apply {
                // 绘制阴影
                val x: Int = width - scrolledView!!.scrollX
                val min: Int = -shadowWidth
                if (x in (min + 1) until width) {
                    val gradient = LinearGradient(
                        x.toFloat(), 0f, (x + shadowWidth).toFloat(), 0f,
                        gradientColors, gradientPositions, Shader.TileMode.CLAMP
                    )
                    shadowPaint.shader = gradient
                    canvas.drawRect(
                        x.toFloat(),
                        0f,
                        (x + shadowWidth).toFloat(),
                        height.toFloat(),
                        shadowPaint
                    )
                }
            }
        }
    }
}