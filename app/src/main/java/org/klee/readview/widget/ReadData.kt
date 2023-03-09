package org.klee.readview.widget

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.IntRange
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.ChapterStatus
import org.klee.readview.entities.PageData
import org.klee.readview.loader.BookLoader

/**
 * 由DataSource对数据进行统一的管理，数据主要有文本数据、bitmap缓存两种
 */
private const val TAG = "DataSource"
class ReadData {

    var book: BookData? = null
    lateinit var bookLoader: BookLoader
    private val pageFactory get() = ContentConfig.getPageFactory()

    @IntRange(from = 1)
    var curChapIndex: Int = 1
        private set
    @IntRange(from = 1)
    var curPageIndex: Int = 1
        private set
    private var preLoadBefore = 2   // 预加载当前章节之前的2章节
    private var preLoadBehind = 2   // 预加载当前章节之后的2章节

    val chapCount: Int
        get() {              // 章节数
            book?.let {
                return book!!.chapCount
            }
            return 0
        }

    fun setProcess(curChapIndex: Int, curPageIndex: Int) {
        this.curChapIndex = curChapIndex
        this.curPageIndex = curPageIndex
    }

    /**
     * 获取指定下标的章节
     */
    fun getChap(@IntRange(from = 1) chapIndex: Int): ChapData {
        if (chapCount == 0 || chapIndex > chapCount) {
            throw IllegalStateException("chapIndex = $chapIndex，chapCount = $chapCount")
        }
        return book!!.getChapter(chapIndex)
    }

    fun loadBook() {
        book = bookLoader.loadBook()
    }

    fun hasNextChap(): Boolean {
        if (chapCount == 0) return false
        return curChapIndex != chapCount
    }

    fun hasPreChap(): Boolean {
        if (chapCount == 0) return false
        return curChapIndex != 1
    }

    fun moveToPrevPage() {
        val curChap = getChap(curChapIndex)
        if (curChap.status == ChapterStatus.FINISHED && curPageIndex > 1) {
            curPageIndex--
        } else {
            val preChap = getChap(curChapIndex - 1)
            curChapIndex--
            curPageIndex = if (preChap.status == ChapterStatus.FINISHED) {
                preChap.pageCount
            } else {
                1
            }
        }
    }

    fun moveToNextPage() {
        val curChap = getChap(curChapIndex)
        if (curChap.status == ChapterStatus.FINISHED && curPageIndex < curChap.pageCount) {
            curPageIndex++
        } else {
            curChapIndex++
            curPageIndex = 1
        }
    }

    /**
     * 验证章节序号的有效性
     */
    private fun validateChapIndex(chapIndex: Int) {
        var valid = true
        if (chapIndex < 1)
            valid = false
        if (chapCount != 0 && chapIndex > chapCount) {
            valid = false
        }
        if (!valid) {
            throw IllegalStateException("chapIndex = ${chapIndex}无效! 当前一共${chapCount}章节。")
        }
    }

    /**
     * 根据指定的章节序号，生成需要预处理的章节的序号列表
     */
    private fun preprocess(chapIndex: Int, process: (index: Int) -> Unit) {
        validateChapIndex(chapIndex)
        process(chapIndex)
        var i = chapIndex - 1
        while (i > 0 && i >= chapIndex - preLoadBefore) {
            process(i)
            i--
        }
        i = chapIndex + 1
        while (i <= chapCount && i <= chapIndex + preLoadBehind) {
            process(i)
            i++
        }
    }

    fun requestLoadAndSplit(chapIndex: Int, always: Boolean = false) = preprocess(chapIndex) {
        requestLoad(it, always, false)
        requestSplit(it, always, false)
    }

    fun requestLoadChapters(chapIndex: Int, alwaysLoad: Boolean = false) = preprocess(chapIndex) {
        requestLoad(it, alwaysLoad, false)
    }

    fun requestLoad(
        chapIndex: Int, alwaysLoad: Boolean = false,
        needValid: Boolean = true
    ) {
        if (needValid) validateChapIndex(chapIndex)
        val chap = getChap(chapIndex)
        if (alwaysLoad || chap.status == ChapterStatus.NO_LOAD) {
            synchronized(chap) {
                chap.status = ChapterStatus.IS_LOADING
                try {
                    bookLoader.loadChapter(chap)
                    chap.status = ChapterStatus.NO_SPLIT
                    Log.d(TAG, "loadChapter: chapter ${chap.chapIndex} success")
                } catch (e: Exception) {
                    chap.status = ChapterStatus.NO_LOAD
                    Log.e(TAG, "loadChapter: chapter ${chap.chapIndex} fail")
                }
            }
        }
    }

    fun requestSplitChapters(chapIndex: Int, alwaysSplit: Boolean = false) = preprocess(chapIndex) {
        requestSplit(it, alwaysSplit, false)
    }

    fun requestSplit(
        chapIndex: Int, alwaysSplit: Boolean = false,
        needValid: Boolean = true
    ) {
        if (needValid) validateChapIndex(chapIndex)
        val chapter = getChap(chapIndex)
        synchronized(chapter) {
            val status = chapter.status
            if (status == ChapterStatus.NO_LOAD || status == ChapterStatus.IS_LOADING) {
                throw IllegalStateException("Chapter${chapIndex} 当前状态为 ${status}，无法分页!")
            }
            if (alwaysSplit) {
                chapter.status = ChapterStatus.IS_SPLITTING
                pageFactory.splitPage(chapter)
                chapter.status = ChapterStatus.FINISHED
            } else {
                if (status == ChapterStatus.NO_SPLIT) {
                    chapter.status = ChapterStatus.IS_SPLITTING
                    pageFactory.splitPage(chapter)
                    chapter.status = ChapterStatus.FINISHED
                }
            }
        }
    }

    /**
     * 根据给定的PageData对象获取其相应的bitmap对象
     */
    fun getPageBitmap(pageData: PageData, callback: ((bitmap: Bitmap) -> Unit)? = null): Bitmap {
        var bitmap: Bitmap? = null
        if (pageData.bitmapCache != null) {     // 先检查是否有缓存bitmap
            if (pageData.bitmapCache!!.isRecycled) {
                pageData.bitmapCache = null
            } else {
                bitmap = pageData.bitmapCache!!
            }
        }
        if (bitmap == null) {
            bitmap = pageFactory.createPageBitmap(pageData)
            callback?.let { it(bitmap) }
            pageData.bitmapCache = bitmap
        }
        return bitmap
    }

    /**
     * 创建一个处于加载中的PageData
     * TODO: 通过为PageData设置一个bitmapCache实现
     */
    private fun createLoadingPage(chapIndex: Int, chapTitle: String): PageData {
        val pageData = PageData(chapIndex, 1)
        pageData.bitmapCache = pageFactory.createLoadingBitmap(chapTitle, "正在加载中...")
        return pageData
    }

    private fun getPage(chapIndex: Int, pageIndex: Int): PageData {
        val chap = getChap(chapIndex)
        return if (chap.status == ChapterStatus.FINISHED) {
            book!!.getChapter(chapIndex).getPage(pageIndex)
        } else {
            createLoadingPage(chapIndex, chap.title)
        }
    }

    fun getCurPage(): PageData = getPage(curChapIndex, curPageIndex)

    fun getNextPage(): PageData {
        val curChap = getChap(curChapIndex)
        if (curChap.status == ChapterStatus.FINISHED) {
            if (curPageIndex != curChap.pageCount) {
                return getPage(curChapIndex, curPageIndex + 1)
            }
        }
        if (hasNextChap()) {
            return getPage(curChapIndex + 1, 1)
        } else {
            throw IllegalStateException()
        }
    }

    fun getPrevPage(): PageData {
        val curChap = getChap(curChapIndex)
        if (curChap.status == ChapterStatus.FINISHED) {
            return if (curPageIndex > 1) {
                getPage(curChapIndex, curPageIndex - 1)
            } else {
                if (hasPreChap()) {
                    val prevChap = getChap(curChapIndex - 1)
                    getPage(curChapIndex - 1, prevChap.pageCount)
                } else {
                    throw IllegalStateException()
                }
            }
        } else {
            if (hasPreChap()) {
                val prevChap = getChap(curChapIndex - 1)
                return if (prevChap.status != ChapterStatus.FINISHED) {
                    getPage(curChapIndex - 1, 1)
                } else {
                    getPage(curChapIndex - 1, prevChap.pageCount)
                }
            } else {
                throw IllegalStateException()
            }
        }
    }

}