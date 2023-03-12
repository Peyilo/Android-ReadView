package org.klee.readview.widget.api

import android.graphics.Bitmap
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import org.klee.readview.widget.PageView

interface ReadViewCallback {

    /**
     * 当章节目录完成初始化时的回调
     * 注意：该方法会在子线程中执行，如果涉及到UI操作，请利用post()在主线程执行
     */
    fun onTocInitialized(book: BookData?, success: Boolean) = Unit

    /**
     * 当章节目录完成初始化、章节内容完成加载以及分页、刷新视图以后，会回调该函数
     * 该方法会在主线程执行
     */
    fun onInitialized(book: BookData) = Unit

    /**
     * 加载章节完成的回调，注意：该函数处于子线程中
     * @param success 是否加载成功
     */
    fun onLoadChap(chap: ChapData, success: Boolean) = Unit

    fun onUpdatePage(convertView: PageView, newChap: ChapData, newPageIndex: Int) = Unit

    fun onBitmapCreate(bitmap: Bitmap) = Unit

    fun unite(callback: ReadViewCallback): ReadViewCallback = object : ReadViewCallback {
        override fun onTocInitialized(book: BookData?, success: Boolean) {
            this@ReadViewCallback.onTocInitialized(book, success)
            callback.onTocInitialized(book, success)
        }

        override fun onInitialized(book: BookData) {
            this@ReadViewCallback.onInitialized(book)
            callback.onInitialized(book)
        }

        override fun onLoadChap(chap: ChapData, success: Boolean) {
            this@ReadViewCallback.onLoadChap(chap, success)
            callback.onLoadChap(chap, success)
        }

        override fun onUpdatePage(convertView: PageView, newChap: ChapData, newPageIndex: Int) {
            this@ReadViewCallback.onUpdatePage(convertView, newChap, newPageIndex)
            callback.onUpdatePage(convertView, newChap, newPageIndex)
        }

        override fun onBitmapCreate(bitmap: Bitmap) {
            this@ReadViewCallback.onBitmapCreate(bitmap)
            callback.onBitmapCreate(bitmap)
        }
    }

}