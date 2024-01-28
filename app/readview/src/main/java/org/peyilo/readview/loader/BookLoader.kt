package org.peyilo.readview.loader

import org.peyilo.readview.data.BookData
import androidx.annotation.IntRange
import org.peyilo.readview.data.ChapData

interface BookLoader {

    fun initToc(): BookData

    fun loadChap(chapData: ChapData)

}