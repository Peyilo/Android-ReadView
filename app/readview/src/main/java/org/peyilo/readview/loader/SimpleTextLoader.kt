package org.peyilo.readview.loader

import org.peyilo.readview.data.BookData
import org.peyilo.readview.data.ChapData

/**
 * 一个简单的文字加载器，将指定text作为一个无标题章节
 */
class SimpleTextLoader(private val text: String): BookLoader {

    override fun initToc(): BookData {
        val book = BookData()
        book.addChild(
            ChapData(1).apply {
                title = "无标题"
                content = text
                parent = book
            }
        )
        return book
    }

    override fun loadChap(chapData: ChapData) {}


}