package org.peyilo.readview.ui

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.Scroller
import org.peyilo.readview.annotation.ThreadSafe
import org.peyilo.readview.ui.manager.*
import kotlin.math.abs

private const val TAG = "PageContainer"
open class PageContainer(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    var curPage: ReadPage
    var prevPage: ReadPage
    var nextPage: ReadPage

    private var pageManager: PageManager? = null
    private val defaultPageManager: PageManager by lazy {  NoAnimPageManager(this) }
    var flipMode: FlipMode = FlipMode.NoAnim                // 翻页模式
        set(value) {
            pageManager = createPageController(value)
        }

    private val manager get(): PageManager {
        return if (pageManager == null) {
            defaultPageManager
        } else {
            pageManager!!
        }
    }

    private fun createPageController(mode: FlipMode): PageManager = when (mode) {
        FlipMode.NoAnim -> NoAnimPageManager(this)
        FlipMode.Cover -> CoverPageManager(this)
        else -> throw IllegalStateException()
    }

    var scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop    // 区分点击和滑动的界限，默认为24
    var flipTouchSlop = 40      // 触发翻页的最小滑动距离

    init {
        curPage = ReadPage(context, position = ReadPage.Position.CUR)
        prevPage = ReadPage(context, position = ReadPage.Position.PREV)
        nextPage = ReadPage(context, position = ReadPage.Position.NEXT)
    }

    @ThreadSafe
    @Synchronized
    fun initPage(initializer: (page: ReadPage) -> Unit) {
        listOf(nextPage, curPage, prevPage).forEach {
            initializer(it)             // 初始化三个子View
            addView(it)                 // 添加三个子View
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, left, top, right, bottom)
        manager.onLayout()
        Log.d(TAG, "onLayout: layout")
    }

    internal val scroller by lazy { Scroller(context) }
    internal val downPoint = PointF()
    internal val upPoint = PointF()
    internal val curPoint = PointF()
    private var isMove = false
    private var initDire = PageDirection.NONE               // 初始滑动方向，其确定了要滑动的page
    private var endDire = PageDirection.NONE                // 最终滑动方向，其确定了松开手指以后滑动方向


    override fun onTouchEvent(event: MotionEvent): Boolean {
        curPoint.set(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downPoint.set(event.x, event.y)
                if (manager.isRunning) {            // 如果翻页动画还在执行，立刻结束动画
                    manager.abortAnim()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val distance = curPoint.x - downPoint.x
                // 为防止抖动的点击事件被看作滑动事件引入了scaledTouchSlop
                // 这引入了一个新的问题
                // 如果先在屏幕滑动一个大于scaledTouchSlop的距离，触发滑动效果，然后再往回滑动
                // 当距离小于scaledTouchSlop，就无法触发滑动效果
                // 因此引入了isMove来解决这个问题
                if (isMove || abs(distance) > scaledTouchSlop) {
                    if (!isMove) {
                        isMove = true
                        // 确定初始滑动方向，并且由初始滑动方向确定scrolledView。在一次滑动事件中，scrolledView不会发生改变
                        initDire = if (distance < 0) {
                            manager.scrolledView = curPage
                            PageDirection.NEXT
                        }
                        else {
                            manager.scrolledView = prevPage
                            PageDirection.PREV
                        }
                        // endDire要不就为NONE，要不就和initDire相同，将endDire初始化为initDire
                        endDire = initDire
                    }
                    manager.onMove(initDire, distance.toInt())
                }
            }
            MotionEvent.ACTION_UP -> {
                upPoint.set(event.x, event.y)
                if (abs(upPoint.x - downPoint.x) < flipTouchSlop) {
                    endDire = PageDirection.NONE
                }
                if (isMove) {           // 本系列事件为一个滑动事件，处理最终滑动方向
                    when (endDire) {
                        PageDirection.NEXT -> {
                            manager.startNextAnim()
                            nextCarousel()
                        }
                        PageDirection.PREV -> {
                            manager.startPrevAnim()
                            prevCarousel()
                        }
                        PageDirection.NONE -> resetPageScroll(initDire)
                    }
                }
                isMove = false            // 清除isMove的状态
                initDire = PageDirection.NONE
                endDire = PageDirection.NONE
            }
        }
        return true
    }

    /**
     * 本次滑动事件不进行翻页，需要复位page的位置
     * @param initDire: 根据初始滑动方向确定如何复位page，传入的initDire一定是NEXT、PREV其中一个
     */
    private fun resetPageScroll(initDire: PageDirection) {
        manager.resetPageScroll(initDire)
    }

    // 按照下一页的顺序进行循环轮播
    private fun nextCarousel() {
        val temp = prevPage         // 移除最顶层的prevPage，插入到最底层，即nextPage下面
        prevPage = curPage          // 再更新prevPage、curPage、nextPage指向的page
        curPage = nextPage
        nextPage = temp
        removeView(nextPage)
        addView(nextPage, 0)
        updatePagePosition()
        manager.onNextCarousel()
        invalidate()
    }

    // 按照上一页的顺序进行循环轮播
    private fun prevCarousel() {
        val temp = nextPage         // 移除最底层的nextPage，插入到最顶层，即prevPage上面
        nextPage = curPage          // 再更新prevPage、curPage、nextPage指向的page
        curPage = prevPage
        prevPage = temp
        removeView(prevPage)
        addView(prevPage, 2)
        updatePagePosition()
        manager.onPrevCarousel()
    }

    private fun updatePagePosition() {
        prevPage.position = ReadPage.Position.PREV
        curPage.position = ReadPage.Position.CUR
        nextPage.position = ReadPage.Position.NEXT
    }

    override fun computeScroll() {
        super.computeScroll()
        manager.computeScroll()
    }

    open fun hasNextPage(): Boolean = true
    open fun hasPrevPage(): Boolean = true

    protected open fun updatePage() = Unit
}