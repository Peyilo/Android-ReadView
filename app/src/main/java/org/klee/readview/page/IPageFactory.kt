package org.klee.readview.page

import android.graphics.Bitmap
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.PageData

interface IPageFactory {
    fun splitPage(chapData: ChapData): Boolean
    fun createPageBitmap(pageData: PageData): Bitmap
}