package org.klee.readview.entities

import android.graphics.Bitmap

class PageData (
    val chapIndex: Int,
    val pageIndex: Int,
) {
    var bitmapCache: Bitmap? = null
    private val titleLineList: ArrayList<String> by lazy { ArrayList() }
    private val contentLineList: ArrayList<LineData> by lazy { ArrayList() }

    fun addTitleLine(line: String) {
        titleLineList.add(line)
    }

    /**
     * lineIndex need larger than 0!
     */
    fun getTitleLine(lineIndex: Int) = titleLineList[lineIndex - 1]

    fun addContentLine(line: LineData) {
        contentLineList.add(line)
    }

    /**
     * lineIndex need larger than 0!
     */
    fun getContentLine(lineIndex: Int) = contentLineList[lineIndex - 1]

    val titleLineCount get() =  titleLineList.size
    val contentLineCount get() =  contentLineList.size
}
