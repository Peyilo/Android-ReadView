package org.klee.readview.entities
import androidx.annotation.IntRange

// 章节数据
class ChapData (
    @IntRange(from = 1)
    val chapIndex: Int,
    val title: String = "",
    var content: String? = null
) : AdditionalData() {

    var status: ChapterStatus = ChapterStatus.NO_LOAD           // 章节状态

    private val pageList: MutableList<PageData> by lazy {
        ArrayList()
    }

    val pageCount get() = pageList.size     // 完成加载之后，至少有一页（可能为空白页）

    fun addPage(pageData: PageData) {
        pageList.add(pageData)
    }

    fun clearPages() {
        pageList.clear()
    }

    fun getPage(@IntRange(from = 1) pageIndex: Int) = pageList[pageIndex - 1]
}