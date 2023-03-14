package org.klee.readview.loader

import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData

class TextLoader(private val text: String) : BookLoader {

    override fun loadBook(): BookData {
        val bookData = BookData()           // 将整个字符串作为一个无标题的章节
        bookData.addChapter(
            ChapData(1).apply {
                content = text
            }
        )
        return bookData
    }

}