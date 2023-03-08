package org.klee.readview.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.IntRange
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.*
import org.klee.readview.loader.BookLoader
import org.klee.readview.utils.invisible
import org.klee.readview.utils.visible
import java.util.concurrent.Executors

private const val TAG = "ReadView"
class ReadView(context: Context, attributeSet: AttributeSet?) :
    BaseReadView(context, attributeSet) {

    private val pageFactory get() = ContentConfig.getPageFactory()

    private lateinit var bookLoader: BookLoader
    var book: BookData? = null
        private set
    val chapCount: Int
        get() {              // 章节数
            book?.let {
                return book!!.chapCount
            }
            return 0
        }

    @IntRange(from = 1)
    var curChapIndex: Int = 1
        private set
    @IntRange(from = 1)
    var curPageIndex: Int = 1
        private set

    var preLoadBefore = 2   // 预加载当前章节之前的2章节
    var preLoadBehind = 2   // 预加载当前章节之后的2章节

    var callback: Callback? = null

    var initView: View? = null
    var initFinished = false

    private val threadPool by lazy { Executors.newFixedThreadPool(10) }

    override fun initPage(initializer: (readPage: ReadPage, position: Int) -> Unit) {
        super.initPage(initializer)
        beforeInit()
    }

    private fun createInitView() {
        initView = TextView(context).apply {
            gravity = Gravity.CENTER
            text = "加载中..."
            textSize = 18F
        }
    }

    private fun beforeInit() {
        if (initView == null) {         // 配置初始化视图
            createInitView()
        }
        addView(initView!!)
        curPageView.invisible()         // 设置为不可见
        prePageView.invisible()
        nextPageView.invisible()
    }

    // 当目录加载完，需要显示出
    private fun afterInit() {
        curPageView.visible()
        prePageView.visible()
        nextPageView.visible()
        removeView(initView)
        initView = null
        initFinished = true
    }

    /**
     * 获取指定下标的章节
     */
    fun getChap(@IntRange(from = 1) chapIndex: Int): ChapData? {
        if (chapCount == 0) return null
        if (chapIndex < 1 || chapIndex > chapCount) {
            return null
        }
        return book!!.getChapter(chapIndex)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        threadPool.shutdown()
    }

    private fun startTask(task: Runnable) {
        threadPool.submit(task)
    }

    override fun hasNextPage(): Boolean {
        if (!initFinished || chapCount == 0) return false
        if (curChapIndex == chapCount) {    // 最后一章节
            val chapter = getChap(curChapIndex)!!
            return when (chapter.status) {
                ChapterStatus.FINISHED -> {
                    curPageIndex != chapter.pageCount
                }
                else -> false
            }
        }
        return true
    }

    override fun hasPrevPage(): Boolean {
        if (!initFinished || chapCount == 0) return false
        if (curChapIndex == 1) {        // 第一章节
            val chapter = getChap(curChapIndex)!!
            return when (chapter.status) {
                ChapterStatus.FINISHED -> {
                    curPageIndex != 1
                }
                else -> false
            }
        }
        return true
    }

    /**
     * 代理bookLoader的方法，实现回调
     */
    private fun loadBook(): BookData {
        book = bookLoader.loadBook()
        callback?.onTocInitialized(book!!)                      // callback function
        return book!!
    }

    private fun loadChapter(chap: ChapData) {
        chap.status = ChapterStatus.IS_LOADING
        bookLoader.loadChapter(chap)
        chap.status = ChapterStatus.NO_SPLIT
        callback?.onLoadChap(chap)
        Log.d(TAG, "loadChapter: chapter ${chap.chapIndex}")
    }

    private fun onChapterChange(oldChapIndex: Int, newChapIndex: Int) {
        startTask {
            requestLoadChapters(newChapIndex)
            requestSplitChapters(newChapIndex)
        }
    }

    private fun onPageChange(oldChapIndex: Int, oldPageIndex: Int,
        newChapIndex: Int, newPageIndex: Int) {
        if (oldChapIndex != newChapIndex) {
            onChapterChange(oldChapIndex, newChapIndex)
        }
        Log.d(TAG, "onPageChange: oldChapIndex = $oldChapIndex, oldPageIndex = $oldPageIndex")
        Log.d(TAG, "onPageChange: newChapIndex = $newChapIndex, newPageIndex = $newPageIndex")
    }

    override fun onFlipToPrev() {
        val oldChapIndex = curChapIndex
        val oldPageIndex = curPageIndex
        val curChap = getChap(curChapIndex)!!
        if (curChap.status == ChapterStatus.FINISHED) {
            if (curPageIndex > 1) {
                curPageIndex--
                onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
                return
            }
        }
        val preChap = getChap(curChapIndex - 1)!!
        curChapIndex--
        curPageIndex = if (preChap.status == ChapterStatus.FINISHED) {
            preChap.pageCount
        } else {
            1
        }
        onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
    }

    override fun onFlipToNext() {
        val oldChapIndex = curChapIndex
        val oldPageIndex = curPageIndex
        val curChap = getChap(curChapIndex)!!
        if (curChap.status == ChapterStatus.FINISHED) {
            if (curPageIndex < curChap.pageCount) {
                curPageIndex++
                onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
                return
            }
        }
        curChapIndex++
        curPageIndex = 1
        onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
    }

    override fun updateChildView(convertView: ReadPage, direction: PageDirection): ReadPage {
        super.updateChildView(convertView, direction)
        if (direction == PageDirection.NEXT) {
            if (hasNextPage()) {
                convertView.setContent(pageFactory.createPage(
                    getNextPage()
                ))
            }
        }
        if (direction == PageDirection.PREV) {
            if (hasPreChap()) {
                convertView.setContent(pageFactory.createPage(
                    getPrevPage()
                ))
            }
        }
        return convertView
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
            throw IllegalStateException("章节序号无效，chapIndex = $chapIndex, chapCount = $chapCount")
        }
    }

    /**
     * 根据指定的章节序号，生成需要预处理的章节的序号列表
     */
    private fun getPreprocessIndexList(chapIndex: Int): List<Int> {
        validateChapIndex(chapIndex)
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

    /**
     * 设置书籍显示的进度
     * @param chapIndex 章节序号，chapIndex = 1表示第一章，THE_LAST则表示的是最后一章或者最后一页，pageIndex同理
     * @param pageIndex 分页序号，表示的是在章节中的位置
     */
    fun setProcess(chapIndex: Int, pageIndex: Int = 1) {
        this.curChapIndex = chapIndex
        this.curPageIndex = pageIndex
        startTask {
            requestLoadChapters(chapIndex)
            requestSplitChapters(chapIndex)
            refreshAllPage()
        }
    }

    fun openBook(
        loader: BookLoader,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        this.bookLoader = loader
        this.curChapIndex = chapIndex
        this.curPageIndex = pageIndex
        startTask {
            val book = loadBook()                                   // load toc
            requestLoadChapters(chapIndex)
            post {
                requestSplitChapters(chapIndex)
                afterInit()
                refreshAllPage()
                callback?.onInitialized(book)
            }
        }
    }

    private fun requestLoadChapters(chapIndex: Int, alwaysLoad: Boolean = false) {
        val preprocessIndexList = getPreprocessIndexList(chapIndex)
        preprocessIndexList.forEach { index ->
            val chap = getChap(index)!!
            if (alwaysLoad) {          // 加载全部，而不管该章节是否已经加载或者加载中
                loadChapter(chap)
            } else {
                if (chap.status == ChapterStatus.NO_LOAD) {
                    loadChapter(chap)
                }
            }
        }
    }

    private fun requestSplitChapters(chapIndex: Int, alwaysSplit: Boolean = false) {
        val preprocessIndexList = getPreprocessIndexList(chapIndex)
        preprocessIndexList.forEach {
            splitChapter(it, alwaysSplit)
        }
    }

    private fun splitChapter(
        chapIndex: Int, alwaysSplit: Boolean = false,
        needValid: Boolean = false
    ) {
        if (needValid) {
            validateChapIndex(chapIndex)
        }
        val chapter = getChap(chapIndex)!!
        val status = chapter.status
        if (status == ChapterStatus.NO_LOAD || status == ChapterStatus.IS_LOADING) {
            throw IllegalStateException("章节${chapIndex}当前状态为${status}，无法分页!")
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

    fun hasNextChap(): Boolean {
        if (chapCount == 0) return false
        return curChapIndex != chapCount
    }

    fun hasPreChap(): Boolean {
        if (chapCount == 0) return false
        return curChapIndex != 1
    }

    private fun getPage(chapIndex: Int, pageIndex: Int): PageData {
        val chap = getChap(chapIndex)!!
        return if (chap.status == ChapterStatus.FINISHED) {
            book!!.getChapter(chapIndex).getPage(pageIndex)
        } else {
            val tempChap = ChapData(chapIndex, chap.title, "正在加载中...")
            pageFactory.splitPage(tempChap)
            tempChap.getPage(1)
        }
    }

    private fun getCurPage(): PageData = getPage(curChapIndex, curPageIndex)

    private fun getNextPage(): PageData {
        val curChap = getChap(curChapIndex)!!
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

    private fun getPrevPage(): PageData {
        val curChap = getChap(curChapIndex)!!
        if (curChap.status == ChapterStatus.FINISHED) {
            return if (curPageIndex > 1) {
                getPage(curChapIndex, curPageIndex - 1)
            } else {
                if (hasPreChap()) {
                    val prevChap = getChap(curChapIndex - 1)!!
                    getPage(curChapIndex - 1, prevChap.pageCount)
                } else {
                    throw IllegalStateException()
                }
            }
        } else {
            if (hasPreChap()) {
                return getPage(curChapIndex - 1, 1)
            } else {
                throw IllegalStateException()
            }
        }
    }

    // 刷新当前ReadPage
    private fun refreshCurPage() {
        curPageView.setContent(
            pageFactory.createPage(
                getCurPage()
            )
        )
    }

    private fun refreshPrevPage() {
        if (hasPrevPage()) {
            prePageView.setContent(
                pageFactory.createPage(
                    getPrevPage()
                )
            )
        }
    }

    private fun refreshNextPage() {
        if (hasNextPage()) {
            nextPageView.setContent(
                pageFactory.createPage(
                    getNextPage()
                )
            )
        }
    }

    /**
     * 刷新所有ReadPage视图
     */
    private fun refreshAllPage() {
        refreshCurPage()
        refreshNextPage()
        refreshPrevPage()
    }

    interface Callback {
        /**
         * 在子线程加载章节完成的回调
         */
        fun onLoadChap(chap: ChapData) = Unit

        /**
         * 当章节目录完成初始化时的回调
         * 注意：该方法会在子线程中执行，如果涉及到UI操作，请利用post()在主线程执行
         */
        fun onTocInitialized(book: BookData) = Unit

        /**
         * 当章节目录完成初始化、章节内容完成加载以及分页、刷新视图以后，会回调该函数
         * 该方法会在主线程执行
         */
        fun onInitialized(book: BookData) = Unit
    }

    companion object {
        const val THE_LAST = -1         // 表示最后一页、或者最后一章节
    }

}