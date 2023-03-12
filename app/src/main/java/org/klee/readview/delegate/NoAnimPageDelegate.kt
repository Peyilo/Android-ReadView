package org.klee.readview.delegate

import org.klee.readview.widget.BaseReadView
import org.klee.readview.widget.PageView

class NoAnimPageDelegate(readView: BaseReadView) : HorizontalPageDelegate(readView) {

    override fun onUpdateScrollValue(pageDirection: PageDirection, pageView: PageView) {
        when (pageDirection) {
            PageDirection.NEXT ->
                pageView.scrollTo(0, 0)
            PageDirection.PREV ->
                pageView.scrollTo(readView.width, 0)
            else -> Unit
        }
    }

    override fun onFlip(): PageDirection {
        val distance = startPoint.x - touchPoint.x
        return if (distance > 0) {
            PageDirection.NEXT
        } else if (distance < 0) {
            prevPage.scrollTo(0, 0)
            PageDirection.PREV
        } else {
            PageDirection.NONE
        }
    }
}