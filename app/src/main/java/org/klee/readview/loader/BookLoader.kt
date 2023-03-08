package org.klee.readview.loader

import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData

interface BookLoader {

    fun loadBook(): BookData

    fun loadChapter(chapData: ChapData)

}