package org.klee.readview.widget.api

import android.graphics.Bitmap
import org.klee.readview.entities.IndexBean

interface BitmapProvider {

    /**
     * 根据指定的下标，获取一个非null且未回收的bitmap
     */
    fun getBitmap(indexBean: IndexBean): Bitmap

}