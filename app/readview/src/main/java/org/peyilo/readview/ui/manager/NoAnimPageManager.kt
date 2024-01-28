package org.peyilo.readview.ui.manager

import org.peyilo.readview.ui.PageContainer

class NoAnimPageManager(readView: PageContainer): PageManager(readView) {

    override fun onLayout() { // 将顶层的
        container.prevPage.scrollTo(container.width, 0)
    }

}