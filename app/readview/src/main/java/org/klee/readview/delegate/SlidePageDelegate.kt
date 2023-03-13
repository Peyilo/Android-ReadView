package org.klee.readview.delegate

import org.klee.readview.widget.BaseReadView
import org.klee.readview.widget.PageView

class SlidePageDelegate(readView: BaseReadView): HorizontalPageDelegate(readView) {

    override fun onUpdateScrollValue(pageDirection: PageDirection, pageView: PageView) {

    }

    override fun startAnim(pageDirection: PageDirection) {
        TODO("Not yet implemented")
    }
}