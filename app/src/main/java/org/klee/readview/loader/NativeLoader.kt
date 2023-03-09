package org.klee.readview.loader

import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import java.io.File

/**
 * 一个简单的本地小说加载器
 */
class NativeLoader(var file: File) : BookLoader {

    override fun loadBook(): BookData {
        TODO("Not yet implemented")
    }

    override fun loadChapter(chapData: ChapData) {
        TODO("Not yet implemented")
    }
}