package org.peyilo.readview.ui

import org.peyilo.readview.data.BookData
import org.peyilo.readview.loader.BookLoader
import androidx.annotation.IntRange

object ReadBook {

    private lateinit var bookData: BookData
    lateinit var loader: BookLoader

    @IntRange(from = 1) var curChapIndex = 1
    @IntRange(from = 1) var curPageIndex = 1

    fun initToc() {
        bookData = loader.initToc()
    }

}