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

    val book: BookData get() = readData.book!!
    val chapCount get() = readData.chapCount
    val curChapIndex get() = readData.curChapIndex
    val curPageIndex get() = readData.curPageIndex

    private val callback: Callback? get() = readData.callback

    private var initView: View? = null
    private var initFinished = false

    override fun initPage(initializer: (pageView: PageView, position: Int) -> Unit) {
        super.initPage(initializer)
        curPageView.setBitmapProvider(readData)
        prePageView.setBitmapProvider(readData)
        nextPageView.setBitmapProvider(readData)
    }

    fun setCallback(callback: Callback) {
        readData.callback = callback
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
        if (!readData.hasNextChap()) {    // 最后一章节
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
        if (!readData.hasPreChap()) {        // 没有上一章
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
        var indexBean: IndexBean? = null
        if (direction == PageDirection.NEXT) {
            if (hasNextPage()) {
                indexBean = readData.getNextIndexBean()
            }
        }
        if (direction == PageDirection.PREV) {
            if (hasPrevPage()) {
                indexBean = readData.getPrevIndexBean()
            }
        }
        indexBean?.let {
            convertView.bindContent(indexBean.chapIndex, indexBean.pageIndex)
            callback?.onUpdatePage(
                convertView,
                readData.getChap(indexBean.chapIndex),
                indexBean.pageIndex
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
            readData.requestLoadAndSplit(chapIndex) {
                selectRefresh(chapIndex, pageIndex)
            }
        }
    }

    /**
     * 创建一个在目录加载完成之前显示的视图
     */
    private fun createInitView() {
        initView = TextView(context).apply {
            gravity = Gravity.CENTER
            text = "加载中..."
            textSize = 18F
        }
    }

    /**
     * 做好加载目录的准备工作，如禁止视图滑动，显示“加载中”视图
     */
    private fun prepareInit() {
        initFinished = false
        if (initView == null) {         // 配置初始化视图
            createInitView()
        }
        addView(initView!!)
        curPageView.invisible()         // 设置为不可见
        prePageView.invisible()
        nextPageView.invisible()
    }

    /**
     * 当目录加载完，需要调用该函数显示章节内容
     */
    private fun afterInit() {
        curPageView.visible()
        prePageView.visible()
        nextPageView.visible()
        removeView(initView)
        initView = null
        initFinished = true
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
        prepareInit()
        startTask {
            readData.loadBook()        // load toc
            post {
                afterInit()
                refreshAllPages()
            }
            readData.requestLoadChapters(chapIndex, alwaysLoad = true)
            post {
                readData.requestSplitChapters(chapIndex) {
                    selectRefresh(it.chapIndex)
                }
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
            curPageView.bindContent(curChapIndex, curPageIndex)
            callback?.onUpdatePage(
                curPageView,
                getChap(curChapIndex),
                curPageIndex
            )
        }
    }

    private fun refreshPrevPage() {
        if (hasPrevPage()) {
            readData.apply {
                val prevIndexBean = getPrevIndexBean()
                prePageView.bindContent(prevIndexBean.chapIndex, prevIndexBean.pageIndex)
                callback?.onUpdatePage(
                    prePageView,
                    getChap(prevIndexBean.chapIndex),
                    prevIndexBean.pageIndex
                )
            }
        }
    }

    private fun refreshNextPage() {
        if (hasNextPage()) {
            readData.apply {
                val nextIndexBean = getNextIndexBean()
                nextPageView.bindContent(nextIndexBean.chapIndex, nextIndexBean.pageIndex)
                callback?.onUpdatePage(
                    nextPageView,
                    getChap(nextIndexBean.chapIndex),
                    nextIndexBean.pageIndex
                )
            }
        }
    }

    /**
     * 根据对比当前章节序号、当前页码序号进行有选择的刷新
     */
    private fun selectRefresh(chapIndex: Int, pageIndex: Int = 1) {
        refreshAllPages()
    }

    /**
     * 刷新所有ReadPage视图
     */
    private fun refreshAllPages() {
        refreshCurPage()
        refreshNextPage()
        refreshPrevPage()
    }

    interface Callback {

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

        /**
         * 加载章节完成的回调，注意：该函数处于子线程中
         * @param success 是否加载成功
         */
        fun onLoadChap(chap: ChapData, success: Boolean) = Unit

        fun onUpdatePage(convertView: PageView, newChap: ChapData, newPageIndex: Int) = Unit

        fun onBitmapCreate(bitmap: Bitmap) = Unit
    }

    companion object {
        const val THE_LAST = -1         // 表示最后一页、或者最后一章节
    }

}