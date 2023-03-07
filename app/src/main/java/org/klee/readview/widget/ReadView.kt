package org.klee.readview.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.IntRange
import org.klee.readview.api.BookLoader
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import java.util.concurrent.Executors

class ReadView(context: Context, attributeSet: AttributeSet?)
    : BaseReadView(context, attributeSet) {

    val pageFactory get() = ContentConfig.getPageFactory()

    private lateinit var bookLoader: BookLoader
    var book: BookData? = null
    val chapCount: Int get() {              // 章节数
        book?.let {
            return book!!.chapCount
        }
        return 0
    }

    @IntRange(from = 1) var curChapIndex: Int = 1
    @IntRange(from = 1) var curPageIndex: Int = 1

    var preLoadBefore = 2   // 预加载当前章节之前的2章节
    var preLoadBehind = 2   // 预加载当前章节之后的2章节


    private val threadPool by lazy { Executors.newFixedThreadPool(10) }

    fun getChapter(@IntRange(from = 1) chapIndex: Int) = book!!.getChapter(chapIndex)

    /**
     * 当章节目录完成初始化时的回调
     */
    private fun onTocInitialized(book: BookData) {}

    private fun onChapLoaded(chap: ChapData) {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        threadPool.shutdown()
    }

    private fun startTask(task: Runnable) {
        threadPool.submit(task)
    }

    override fun hasNextPage(): Boolean {
        if (book == null) return false
        return true
    }

    override fun hasPrevPage(): Boolean {
        if (book == null) return false
        return true
    }

    /**
     * 根据指定的章节序号，生成需要加载的章节的序号列表
     */
    private fun getPreloadIndexList(chapIndex: Int): List<Int> {
        val indexList = ArrayList<Int>()
        indexList.add(chapIndex)
        var i = chapIndex - 1
        while (i > 0 && i >= chapIndex - preLoadBefore) {
            indexList.add(i)
            i--
        }
        i = chapIndex + 1
        while (i <= chapCount && i <= chapIndex + preLoadBehind) {
            indexList.add(i)
            i++
        }
        return indexList
    }

    fun openBook(loader: BookLoader,
                 @IntRange(from = 1) chapIndex: Int = 1,
                 @IntRange(from = 1) pageIndex: Int = 1) {
        this.bookLoader = loader
        this.curChapIndex = chapIndex
        this.curPageIndex = pageIndex
        startTask {
            val book = loader.loadBook()            // load toc
            this.book = book
            onTocInitialized(book)                  // callback function
            val indexList = getPreloadIndexList(this.curChapIndex)
            indexList.forEach {
                val chap = getChapter(it)
                loader.loadChapter(chap)
                onChapLoaded(chap)
            }
            post {
                // finish page split
                indexList.forEach {
                    val chap = getChapter(it)
                    pageFactory.splitPage(chap)
                }
            }
        }
    }

}