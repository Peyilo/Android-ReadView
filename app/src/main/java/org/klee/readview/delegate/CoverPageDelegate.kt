package org.klee.readview.delegate

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import android.view.MotionEvent
import android.view.View
import org.klee.readview.widget.BaseReadView
import org.klee.readview.entities.PageDirection
import kotlin.math.abs

private const val TAG = "CoverPageDelegate"

/**
 * 覆盖翻页的实现
 */
class CoverPageDelegate(readView: BaseReadView) : PageDelegate(readView) {

    private var pageDirection = PageDirection.NONE
    private var scrolledView: View? = null

    private val minFlipDistance = 40

    private val shadowPaint: Paint = Paint()
    private val gradientColors = intArrayOf(-0x71000000, 0x00000000)
    private val gradientPositions = floatArrayOf(0.0f, 1.0f)

    override fun onLayout() {
        prevPage.scrollTo(-(readView.width + shadowWidth), 0)
    }

    override fun abortAnim() {
        if (!scroller.isFinished) {
            scroller.forceFinished(true)
            scrolledView!!.scrollTo(scroller.finalX, scroller.finalY)
        }
    }

    override fun onScroll(event: MotionEvent) {
        val distance = startPoint.x - touchPoint.x
        if (pageDirection == PageDirection.NONE) {
            initPageDirection(distance)
        }
        // 控制实时滑动效果
        if (pageDirection == PageDirection.NEXT) {
            if (distance > 0) {
                scrolledView?.apply {
                    scrollTo(distance.toInt(), 0)
                }
            }
        } else if (pageDirection == PageDirection.PREV) {
            if (distance < 0) {
                scrolledView?.apply {
                    scrollTo(width + shadowWidth + distance.toInt(), 0)
                }
            }
        }
    }

    override fun startAnim() {
        val distance = startPoint.x - touchPoint.x
        // 处理最终翻页结果
        if (pageDirection != PageDirection.NONE) {
            val scrollX = scrolledView!!.scrollX
            val dx: Int
            // 如果滑动的距离没有超过minFlipDistance，就不进行翻页，应将页面复位
            val endDirection: PageDirection
            if (abs(distance) < minFlipDistance) {
                endDirection = PageDirection.NONE
                dx = if (pageDirection === PageDirection.NEXT) {
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
            scroller.startScroll(scrollX, 0, dx, 0)
            pageDirection = PageDirection.NONE
            readView.invalidate()
            // 尝试加载新的视图
            onUpdateChildView(endDirection)
        }
    }

    /**
     * 根据给定的distance，确定初始滑动方向，并以此确定当前被选中的view
     */
    private fun initPageDirection(distance: Float) {
        if (distance > 0 && readView.hasNextPage()) {
            pageDirection = PageDirection.NEXT
            scrolledView = curPage
        } else if (distance < 0 && readView.hasPrevPage()) {
            pageDirection = PageDirection.PREV
            scrolledView = prevPage
        }
    }

    /**
     * 根据翻页方向，更新子view
     */
    private fun onUpdateChildView(direction: PageDirection) {
        // 将距离当前页面最远的页面移除，再进行复用
        when (direction) {
            PageDirection.NEXT -> {
                readView.apply {
                    val convertView = readView.prePageView
                    prePageView = curPageView
                    curPageView = nextPageView
                    nextPageView = readView.updateChildView(convertView, direction)
                    removeView(convertView)
                    nextPageView.scrollTo(0, 0)
                    addView(nextPageView, 0)
                }
            }
            PageDirection.PREV -> {
                readView.apply {
                    val convertView = nextPageView
                    nextPageView = curPageView
                    curPageView = prePageView
                    prePageView = updateChildView(convertView, direction)
                    removeView(convertView)
                    prePageView.scrollTo(width + shadowWidth, 0)
                    addView(prePageView)
                }
            }
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