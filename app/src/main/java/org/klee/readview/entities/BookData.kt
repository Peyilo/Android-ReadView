package org.klee.readview.entities

import androidx.annotation.IntRange

/**
 * 书籍数据
 */
class BookData (
    var name: String = "",                           // 书籍名称
    var author: String = "",                         // 作者名
) : AdditionalData() {
    private val chapList: MutableList<ChapData> by lazy {
        ArrayList()
    }
    val chapCount get() = chapList.size         // 章节数
    // 添加章节
    fun addChapter(chapData: ChapData) {
        chapList.add(chapData)
    }
    // 获取章节
    fun getChapter(@IntRange(from = 1) chapIndex: Int) = chapList[chapIndex - 1]
}