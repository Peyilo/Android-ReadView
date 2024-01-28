package org.peyilo.readview.data

class VolumeData(
    volIndex: Int
): DataContainer(), BookChild {
    lateinit var title: String
    lateinit var parent: BookData
    private val list: MutableList<ChapData> by lazy { mutableListOf() }
    val chapCount get() = list.size

    override fun getChap(index: Int): ChapData {
        return list[index - 1]
    }

    fun addChap(chap: ChapData) {
        list.add(chap)
    }
}