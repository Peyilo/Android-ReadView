package org.peyilo.readview.ui.manager

import org.peyilo.readview.ui.PageContainer

private const val TAG = "CoverPageManager"
class CoverPageManager(readView: PageContainer): HorizontalPageManager(readView) {

    override fun onLayout() {
        prevPage.scrollTo(container.width, 0)
    }

    override fun onNextCarousel() {
        super.onNextCarousel()
        nextPage.scrollTo(0, 0)
    }

    override fun onPrevCarousel() {
        super.onPrevCarousel()
        curPage.scrollTo(0, 0)
        nextPage.scrollTo(0, 0)
    }

    override fun onMove(initDire: PageDirection,distance: Int) {
        super.onMove(initDire, distance)
        if (distance < 0) {         // Next
            // 一开始往左滑（NEXT），之后往右滑（PREV），但是scrolledView由initDire确定
            // 如果一开始往左滑，scrolledView就是curPage。需要保证curPage中途往右滑动时
            // curPage的右侧边界不能超过ReadView的右侧边界
            if (initDire == PageDirection.NEXT) {
                scrolledView!!.scrollTo(-distance, 0)
            } else {
                // 设置完以上检查initDire的语句以后，page有概率卡在边缘处一点点，无法再接着滑动
                // 以贴合ReadView边界 ，需要直接调用代码保证边界对齐
                scrolledView!!.scrollTo(container.width, 0)
            }
        } else {                    // Prev
            if (initDire == PageDirection.PREV) {
                scrolledView!!.scrollTo(container.width - distance, 0)
            } else {
                scrolledView!!.scrollTo(0, 0)
            }
        }
    }

    override fun resetPageScroll(initDire: PageDirection) {
        super.resetPageScroll(initDire)

        // 处理最终翻页结果
        val scrollX = scrolledView!!.scrollX
        val dx = if (initDire === PageDirection.NEXT) {
            -scrollX
        } else {
            container.width - scrollX
        }
        // TODO：如果需要滑动的距离比较小，可能会出现卡顿，将持续时间设置得小一点会解决这个问题
        isRunning = true
        scroller.startScroll(scrollX, 0, dx, 0)
        container.invalidate()
    }

    override fun abortAnim() {
        super.abortAnim()
        if (!scroller.isFinished) {
            scroller.forceFinished(true)
            scrolledView!!.scrollTo(scroller.finalX, scroller.finalY)
            isRunning = false
            scrolledView = null
        }
    }

    override fun startNextAnim() {
        super.startNextAnim()
        val scrollX = scrolledView!!.scrollX
        val dx = container.width - scrollX
        isRunning = true
        scroller.startScroll(scrollX, 0, dx, 0)
        container.invalidate()
    }

    override fun startPrevAnim() {
        super.startPrevAnim()
        val scrollX = scrolledView!!.scrollX
        val dx = - scrollX
        isRunning = true
        scroller.startScroll(scrollX, 0, dx, 0)
        container.invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrolledView!!.scrollTo(scroller.currX, scroller.currY)
            // 滑动动画结束
            if (scroller.currX == scroller.finalX && scroller.currY == scroller.finalY) {
                scroller.forceFinished(true)
                isRunning = false
                scrolledView = null
            }
        }
    }
}