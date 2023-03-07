package org.klee.readview.page

import android.graphics.Bitmap
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.PageData

interface IPageFactory {

    val remainedWidth get() = ContentConfig.contentWidth
    val remainedHeight get() = ContentConfig.contentHeight

    var breaker: IBreaker

    fun splitPage(chapData: ChapData): Boolean
    fun createPage(pageData: PageData): Bitmap
}