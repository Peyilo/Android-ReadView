package org.klee.readview.loader

import org.anvei.novel.api.SfacgAPI
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData

class SfacgLoader(var novelId: Long = 0) : BookLoader {

    private val api = SfacgAPI()

    override fun loadBook(): BookData {
        val book = BookData()
        val novelHomeJson = api.getNovelHomeJson(novelId)
        book.apply {                                // 加载小说基本信息
            o = novelHomeJson.data.novelId
            name = novelHomeJson.data.novelName
            author = novelHomeJson.data.authorName
        }
        val chapListJson = api.getChapListJson(novelId)
        var chapIndex = 1
        chapListJson.data.volumeList.forEach { volume ->        // 加载目录信息
            volume.chapterList.forEach { chap ->
                val chapData = ChapData(chapIndex, chap.title)
                chapData.o = chap.chapId
                book.addChapter(chapData)
                chapIndex++
            }
        }
        chapListJson.data
        return book
    }

    override fun loadChapter(chapData: ChapData) {
        val chapIndex = chapData.o as Int
        val chapContentJson = api.getChapContentJson(chapIndex.toLong())
        chapData.content = chapContentJson.data.expand.content
    }

}