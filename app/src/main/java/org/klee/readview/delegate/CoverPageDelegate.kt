package org.klee.readview.delegate

import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import org.klee.readview.ReadView
import org.klee.readview.entities.PageDirection
import kotlin.math.abs

private const val TAG = "CoverPageDelegate"
class CoverPageDelegate(readView: ReadView) : PageDelegate(readView) {

    private var startX = 0F
    private var pageDirection = PageDirection.NONE
    private var scrolledView: View? = null
    private val scroller = Scroller(readView.context)

    private val minFlipDistance = 40

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val distance = startX - event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 强制结束动画
                if (!scroller.isFinished) {
                    scroller.forceFinished(true)
                    scrolledView?.scrollTo(scroller.finalX, scroller.finalY)
                }
                startX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
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
                            scrollTo(width + distance.toInt(), 0)
                        }
                    }
                } else {
                    return false
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                // 处理最终翻页结果
                if (pageDirection != PageDirection.NONE) {
                    val scrollX = scrolledView!!.scrollX
                    val dx: Int
                    // 如果滑动的距离没有超过minFlipDistance，就不进行翻页，应将页面复位
                    val endDirection: PageDirection
                    if (abs(distance) < minFlipDistance) {
                        endDirection = PageDirection.NONE
                        dx = if (pageDirection === PageDirection.NEXT) {
                            -scrollX
                        } else {
                            readView.width - scrollX
                        }
                    } else {        // 完成翻页
                        dx = if (distance > 0) {
                            endDirection = PageDirection.NEXT
                            readView.width - scrollX
                        } else {
                            endDirection = PageDirection.PREV
                            -scrollX
                        }
                    }
                    scroller.startScroll(scrollX, 0, dx, 0)
                    pageDirection = PageDirection.NONE
                    readView.postInvalidate()
                    // 尝试加载新的视图
                    onUpdateChildView(endDirection)
                    return true
                }
            }
        }
        return false
    }

    /**
     * 根据给定的distance，确定初始滑动方向，并以此确定当前被选中的view
     */
    private fun initPageDirection(distance: Float) {
        if (distance > 0 && readView.hasNextPage()) {
            pageDirection = PageDirection.NEXT
            scrolledView = readView.curPageView
        } else if (distance < 0 && readView.hasPrevPage()) {
            pageDirection = PageDirection.PREV
            scrolledView = readView.prePageView
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
                    prePageView.scrollTo(width, 0)
                    addView(prePageView)
                }
            }
            else -> {}
        }
    }

    override fun computeScrollOffset() {
        if (scroller.computeScrollOffset()) {
            scrolledView!!.scrollTo(scroller.currX, scroller.currY)
        }
    }
}