package org.peyilo.readview

import org.anvei.novel.api.SfacgAPI
import org.peyilo.readview.data.BookData
import org.peyilo.readview.data.ChapData
import org.peyilo.readview.data.MultiVolBookData
import org.peyilo.readview.data.VolumeData
import org.peyilo.readview.loader.BookLoader

class SfacgLoader(private var novelId: Long) : BookLoader {

    private val api = SfacgAPI()

    override fun initToc(): BookData {
        val book = MultiVolBookData()
        val novelHomeJson = api.getNovelHomeJson(novelId)
        book.apply {                            // 加载小说基本信息
            obj = novelHomeJson.data.novelId
            title = novelHomeJson.data.novelName
            author = novelHomeJson.data.authorName
        }
        val chapListJson = api.getChapListJson(novelId)
        var chapIndex = 1
        var volIndex = 1
        chapListJson.data.volumeList.forEach { volume ->        // 加载目录信息
            val volumeData = VolumeData(volIndex)
            volumeData.parent = book
            volumeData.title = volume.title
            volume.chapterList.forEach { chap ->
                val chapData = ChapData(chapIndex).apply {
                    parent = volumeData
                    title = chap.title
                    obj = chap.chapId
                }
                volumeData.addChap(chapData)
                chapIndex++
            }
            book.addChild(volumeData)
            volIndex++
        }
        return book
    }

    override fun loadChap(chapData: ChapData) {
        val chapIndex = chapData.obj as Int
        val chapContentJson = api.getChapContentJson(chapIndex.toLong())
        chapData.content = chapContentJson.data.expand.content
    }

}