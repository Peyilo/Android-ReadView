package org.klee.readview.entities

import android.graphics.Bitmap

class PageData (
    val chapIndex: Int,
    val pageIndex: Int,
    val chapTitle: String? = null
) {
    var bitmapCache: Bitmap? = null

}