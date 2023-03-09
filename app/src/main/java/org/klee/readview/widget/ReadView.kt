package org.klee.readview.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.IntRange
import org.klee.readview.entities.*
import org.klee.readview.loader.BookLoader
import org.klee.readview.loader.NativeLoader
import org.klee.readview.utils.invisible
import org.klee.readview.utils.visible
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "ReadView"
class ReadView(context: Context, attributeSet: AttributeSet?) :
    BaseReadView(context, attributeSet) {

    private val threadPool by lazy { Executors.newFixedThreadPool(10) }
    private val readData by lazy { ReadData() }

    private val book: BookData get() = readData.book!!
    private val chapCount get() = readData.chapCount
    private val curChapIndex get() = readData.curChapIndex
    private val curPageIndex get() = readData.curPageIndex

    var callback: Callback? = null

    private var initView: View? = null
    private var initFinished = false

    /**
     * 对外提供的ReadPage自定义API函数，可以通过该函数配置ReadPage的内容视图、页眉视图、页脚视图
     * @param initializer 初始化器
     */
    override fun initPage(initializer: (pageView: PageView, position: Int) -> Unit) {
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
            val chapter = readData.getChap(curChapIndex)
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
            val chapter = readData.getChap(curChapIndex)
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
    private fun initBook() {
        readData.loadBook()
        Log.d(TAG, "loadBook: book initialized")
        callback?.onTocInitialized(book)                      // callback function
    }

    private fun onChapterChange(oldChapIndex: Int, newChapIndex: Int) {
        startTask {
            readData.requestLoadAndSplit(newChapIndex)
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
        readData.moveToPrevPage()
        onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
    }

    override fun onFlipToNext() {
        val oldChapIndex = curChapIndex
        val oldPageIndex = curPageIndex
        readData.moveToNextPage()
        onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
    }

    override fun updateChildView(convertView: PageView, direction: PageDirection): PageView {
        super.updateChildView(convertView, direction)
        var pageData: PageData? = null
        if (direction == PageDirection.NEXT) {
            if (hasNextPage()) {
                pageData = readData.getNextPage()
            }
        }
        if (direction == PageDirection.PREV) {
            if (readData.hasPreChap()) {
                pageData = readData.getPrevPage()
            }
        }
        pageData?.let {
            convertView.setContent(
                readData.getPageBitmap(pageData)
            )
            callback?.onUpdatePage(
                readData.getChap(pageData.chapIndex),
                pageData.pageIndex
            )
        }
        return convertView
    }

    /**
     * 设置书籍显示的进度
     * TODO： 支持THE_LAST参数，THE_LAST表示的是最后一章或者最后一页
     * @param chapIndex 章节序号，chapIndex = 1表示第一章，pageIndex同理
     * @param pageIndex 分页序号，表示的是在章节中的位置
     */
    fun setProcess(chapIndex: Int, pageIndex: Int = 1) {
        readData.setProcess(chapIndex, pageIndex)
        startTask {
            readData.apply {
                requestLoadChapters(chapIndex)
                requestSplitChapters(chapIndex)
            }
            refreshAllPage()
        }
    }

    /**
     * 根据给定的BookLoader，加载并显示书籍的内容
     */
    fun openBook(
        loader: BookLoader,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        readData.bookLoader = loader
        readData.setProcess(chapIndex, pageIndex)
        startTask {
            initBook()                                   // load toc
            post {
                afterInit()
                refreshAllPage()
            }
            readData.requestLoadChapters(chapIndex, alwaysLoad = true)
            post {
                readData.requestSplitChapters(chapIndex)
                refreshAllPage()
                callback?.onInitialized(this.book)
            }
        }
    }

    /**
     * 使用默认的本地书籍加载器，加载并显示书籍的内容
     */
    fun openBook(
        file: File,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        openBook(NativeLoader(file), chapIndex, pageIndex)
    }

    /**
     * TODO: 显示指定的字符串
     */
    fun showText(
        text: String,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {

    }

    /**
     * 每次创建bitmap都会回调该函数，可以在该函数内进行bitmap的回收处理
     * bitmap很消耗内存，一个1080*2160的ARGB_8888 bitmap，可以占到将近10mb内存
     */
    private fun onBitmapCreate(bitmap: Bitmap) {
        Log.d(TAG, "onBitmapCreate: size = ${bitmap.byteCount / 1024} kb")
    }


    // 刷新当前ReadPage
    private fun refreshCurPage() {
        readData.apply {
            val curPage = getCurPage()
            val bitmap = getPageBitmap(curPage)
            curPageView.setContent(bitmap)
            callback?.onUpdatePage(
                getChap(curPage.chapIndex),
                curPage.pageIndex
            )
        }
    }

    private fun refreshPrevPage() {
        if (hasPrevPage()) {
            readData.apply {
                val prevPage = getPrevPage()
                val bitmap = getPageBitmap(prevPage)
                prePageView.setContent(bitmap)
                callback?.onUpdatePage(
                    getChap(prevPage.chapIndex),
                    prevPage.pageIndex
                )
            }
        }
    }

    private fun refreshNextPage() {
        if (hasNextPage()) {
            readData.apply {
                val nextPage = getNextPage()
                val bitmap = getPageBitmap(nextPage)
                nextPageView.setContent(bitmap)
                callback?.onUpdatePage(
                    getChap(nextPage.chapIndex),
                    nextPage.pageIndex
                )
            }
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

        fun onUpdatePage(chap: ChapData, pageIndex: Int) = Unit
    }

    companion object {
        const val THE_LAST = -1         // 表示最后一页、或者最后一章节
    }

}