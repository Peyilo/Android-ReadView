package org.klee.readview.delegate

import org.klee.readview.widget.BaseReadView
import org.klee.readview.widget.PageView

abstract class HorizontalPageDelegate(readView: BaseReadView): PageDelegate(readView, 75) {

    protected var initPageDirection = PageDirection.NONE
        private set

    override fun onScrollValue() {
        // 将上一页移向屏幕之外
        prevPage.scrollTo(-readView.width, 0)
    }

    override fun initPageDirection(initGestureDirection: GestureDirection): PageDirection {
        // 完成对翻页方向的初始化
        initPageDirection = when (initGestureDirection) {
            GestureDirection.TO_RIGHT ->
                PageDirection.PREV
            GestureDirection.TO_LEFT ->
                PageDirection.NEXT
            else ->
                PageDirection.NONE
        }
        return initPageDirection
    }

    override fun onUpdateChildView(direction: PageDirection) {
        // 将距离当前页面最远的页面移除，再进行复用
        when (direction) {
            PageDirection.NEXT -> {
                readView.apply {
                    val convertView = readView.prePageView
                    prePageView = curPageView
                    curPageView = nextPageView
                    nextPageView = readView.updateChildView(convertView, direction)
                    removeView(convertView)
                    onUpdateScrollValue(PageDirection.NEXT, nextPageView)
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
                    onUpdateScrollValue(PageDirection.PREV, prePageView)
                    addView(prePageView)
                }
            }
            else -> Unit
        }
    }

    protected abstract fun onUpdateScrollValue(pageDirection: PageDirection, pageView: PageView)

}