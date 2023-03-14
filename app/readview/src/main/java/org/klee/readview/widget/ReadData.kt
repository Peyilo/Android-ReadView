package org.klee.readview.widget

import android.graphics.Bitmap
import androidx.annotation.IntRange
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.*
import org.klee.readview.loader.BookLoader
import org.klee.readview.widget.api.BitmapProvider
import org.klee.readview.widget.api.ReadViewCallback

/**
 * 由DataSource对数据进行统一的管理，数据主要有文本数据、bitmap缓存两种
 */
private const val TAG = "DataSource"
class ReadData : BitmapProvider {

    lateinit var contentConfig: ContentConfig
    var userCallback: ReadViewCallback? = null
    lateinit var viewCallBack: ReadViewCallback

    var book: BookData? = null
    lateinit var bookLoader: BookLoader
    private val pageFactory get() = contentConfig.getPageFactory()

    @IntRange(from = 1)
    var curChapIndex: Int = 1
        private set
    @IntRange(from = 1)
    var curPageIndex: Int = 1
        private set
    @IntRange(from = 0) var preprocessBefore = 1   // 预加载当前章节之前的章节
    @IntRange(from = 0) var preprocessBehind = 1   // 预加载当前章节之后的章节

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
        validateChapIndex(chapIndex)
        return book!!.getChapter(chapIndex)
    }

    fun loadBook(): Boolean {
        return try {
            val book = bookLoader.loadBook()
            if (book.isEmpty()) {
                book.addChapter(
                    ChapData(1).apply {
                        content = "内容为空"
                    }
                )
            }
            this.book = book
            viewCallBack.onTocInitSuccess(book)
            userCallback?.onTocInitSuccess(book)
            true
        } catch (e: Exception) {
            viewCallBack.onTocInitFailed(e)
            userCallback?.onTocInitFailed(e)
            false
        }
    }

    fun hasNextChap(): Boolean {
        if (chapCount == 0) return false
        return curChapIndex != chapCount
    }

    fun hasPreChap(): Boolean {
        if (chapCount == 0) return false
        return curChapIndex != 1
    }

    @Synchronized fun moveToPrevPage() {
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

    @Synchronized fun moveToNextPage() {
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
        while (i > 0 && i >= chapIndex - preprocessBefore) {
            process(i)
            i--
        }
        i = chapIndex + 1
        while (i <= chapCount && i <= chapIndex + preprocessBehind) {
            process(i)
            i++
        }
    }

    fun requestLoadAndSplit(chapIndex: Int,
                            always: Boolean = false,
                            onFinished: ((chapData: ChapData) -> Unit)? = null
    ) {
        preprocess(chapIndex) {
            requestLoad(it, always, false)
            requestSplit(it, always, false)
        }
        onFinished?.let {
            onFinished(getChap(curChapIndex))
        }
    }

    fun requestLoadChapters(
        chapIndex: Int, alwaysLoad: Boolean = false
    ) = preprocess(chapIndex) {
        requestLoad(it, alwaysLoad, false)
    }

    private fun requestLoad(
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
                    viewCallBack.onLoadChap(chap, true)
                    userCallback?.onLoadChap(chap, true)
                } catch (e: Exception) {
                    chap.status = ChapterStatus.NO_LOAD
                    viewCallBack.onLoadChap(chap, false)
                    userCallback?.onLoadChap(chap, false)
                }
            }
        }
    }

    fun requestSplitChapters(
        chapIndex: Int,
        alwaysSplit: Boolean = false,
        onFinished: ((chapData: ChapData) -> Unit)? = null
    ) {
        preprocess(chapIndex) {
            requestSplit(it, alwaysSplit, false)
        }
        onFinished?.let {
            onFinished(getChap(chapIndex))
        }
    }

    private fun requestSplit(
        chapIndex: Int, alwaysSplit: Boolean = false,
        needValid: Boolean = true,
        onFinished: ((chapData: ChapData) -> Unit)? = null
    ) {
        if (needValid) validateChapIndex(chapIndex)
        val chapter = getChap(chapIndex)
        synchronized(chapter) {
            val status = chapter.status
            if (status == ChapterStatus.NO_LOAD || status == ChapterStatus.IS_LOADING) {
                throw IllegalStateException("Chapter${chapIndex} 当前状态为 ${status}，无法分页!")
            }
            if (alwaysSplit || status == ChapterStatus.NO_SPLIT) {
                chapter.status = ChapterStatus.IS_SPLITTING
                pageFactory.splitPage(chapter)
                chapter.status = ChapterStatus.FINISHED
                onFinished?.let { it(chapter) }
            }
        }
    }

    override fun getBitmap(indexBean: IndexBean): Bitmap {
        val chap = getChap(indexBean.chapIndex)
        val bitmap =  if (chap.status == ChapterStatus.FINISHED) {
            pageFactory.createPageBitmap(
                getPage(indexBean)
            )
        } else {
            pageFactory.createLoadingBitmap(
                chap.title,
                "正在加载中..."
            )
        }
        viewCallBack.onBitmapCreate(bitmap)
        userCallback?.onBitmapCreate(bitmap)
        return bitmap
    }

    private fun getPage(indexBean: IndexBean): PageData {
        return getChap(indexBean.chapIndex)
            .getPage(indexBean.pageIndex)
    }

    fun getNextIndexBean(): IndexBean {
        val curChap = getChap(curChapIndex)
        if (curChap.status == ChapterStatus.FINISHED) {
            if (curPageIndex != curChap.pageCount) {
                return IndexBean(curChapIndex, curPageIndex + 1)
            }
        }
        if (hasNextChap()) {
            return IndexBean(curChapIndex + 1, 1)
        } else {
            throw IllegalStateException()
        }
    }

    fun getPrevIndexBean(): IndexBean {
        val curChap = getChap(curChapIndex)
        if (curChap.status == ChapterStatus.FINISHED) {
            return if (curPageIndex > 1) {
                IndexBean(curChapIndex, curPageIndex - 1)
            } else {
                if (hasPreChap()) {
                    val prevChap = getChap(curChapIndex - 1)
                    IndexBean(curChapIndex - 1,  prevChap.pageCount)
                } else {
                    throw IllegalStateException()
                }
            }
        } else {
            if (hasPreChap()) {
                val prevChap = getChap(curChapIndex - 1)
                return if (prevChap.status != ChapterStatus.FINISHED) {
                    IndexBean(curChapIndex - 1, 1)
                } else {
                    IndexBean(curChapIndex - 1, prevChap.pageCount)
                }
            } else {
                throw IllegalStateException()
            }
        }
    }
}