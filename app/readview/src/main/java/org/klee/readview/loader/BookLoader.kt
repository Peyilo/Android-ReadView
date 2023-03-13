package org.klee.readview.loader

import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData

/**
 * 定义了ReadView加载小说内容过程
 */
interface BookLoader {

    /**
     * 完成小说基本信息、目录信息的加载
     */
    fun loadBook(): BookData

    /**
     * 加载章节内容
     */
    fun loadChapter(chapData: ChapData)

}