package org.peyilo.readview.ui.manager

import android.view.View
import org.peyilo.readview.ui.PageContainer
import org.peyilo.readview.ui.ReadPage

abstract class PageManager(protected val container: PageContainer) {

    val curPage get() = container.curPage
    val prevPage get() = container.prevPage
    val nextPage get() = container.nextPage

    val scroller get() = container.scroller
    val downPoint get() = container.downPoint
    val upPoint get() = container.upPoint
    val curPoint get() = container.curPoint

    var isRunning = false
    var scrolledView: View? = null

    open fun onLayout() = Unit

    open fun onNextCarousel() = Unit

    open fun onPrevCarousel() = Unit

    open fun computeScroll() = Unit

    open fun abortAnim() = Unit             // 停止翻页滑动动画

    open fun startNextAnim() = Unit         // 开启向下一页翻页的动画

    open fun startPrevAnim() = Unit         // 开启向上一页翻页的动画

    open fun resetPageScroll(initDire: PageDirection) = Unit    // 开启翻页复位动画

    open fun onMove(initDire: PageDirection, distance: Int) = Unit   // 跟随手指滑动

}