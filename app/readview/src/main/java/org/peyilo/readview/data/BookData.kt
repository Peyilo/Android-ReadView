package org.peyilo.readview.data
import androidx.annotation.IntRange


open class BookData: DataContainer() {
    lateinit var title: String                  // 书名
    lateinit var author: String                 // 作者
    open val isMultiVol: Boolean = false        // 是否有分卷
    private val list: MutableList<BookChild> by lazy { mutableListOf() }
    open val chapCount get() = list.size              // 章节数
    val childCount get() = list.size                  // 当isMultiVol为false时为章节数，反之为分卷数

    // 获取第index章节，当isMultiVol=true时，index指的是章节在全书中所处的位置
    override fun getChap(@IntRange(from = 1) index: Int): ChapData {
        return list[index - 1] as ChapData
    }

    fun addChild(bookChild: BookChild) {
        if (isMultiVol) {
            if (bookChild is VolumeData) {
                list.add(bookChild)
            } else {
                throw IllegalArgumentException("当前为分卷模式，只能添加VolumeData实例")
            }
        } else {
            if (bookChild is ChapData) {
                list.add(bookChild)
            } else {
                throw IllegalArgumentException("当前不是分卷模式，只能添加ChapData实例")
            }
        }
    }

    open fun getChild(@IntRange(from = 1) index: Int): BookChild {
        return list[index - 1]
    }

}