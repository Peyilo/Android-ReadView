package org.peyilo.readview.data

/**
 * 保存章节数据
 */
class ChapData(
    chapIndex: Int,
): AdditionalData(), BookChild {
    lateinit var title: String
    lateinit var content: String
    lateinit var parent: DataContainer
}