package org.klee.readview.widget

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.IntRange
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapterStatus
import org.klee.readview.entities.IndexBean
import org.klee.readview.delegate.PageDirection
import org.klee.readview.loader.BookLoader
import org.klee.readview.loader.NativeLoader
import org.klee.readview.utils.invisible
import org.klee.readview.utils.visible
import org.klee.readview.widget.api.ReadViewCallback
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "ReadView"
class ReadView(context: Context, attributeSet: AttributeSet?) :
    BaseReadView(context, attributeSet), ReadViewCallback
{
    private val threadPool by lazy { Executors.newFixedThreadPool(10) }
    private val readData by lazy { ReadData().apply {
        this.contentConfig = this@ReadView.contentConfig
    } }

    val book: BookData get() = readData.book!!
    val chapCount get() = readData.chapCount
    val curChapIndex get() = readData.curChapIndex
    val curPageIndex get() = readData.curPageIndex

    private val callback: ReadViewCallback? get() = readData.callback

    private var initView: View? = null
    private var initFinished = false

    override fun initPage(initializer: (pageView: PageView, position: Int) -> Unit) {
        super.initPage(initializer)
        curPageView.setBitmapProvider(readData)
        prePageView.setBitmapProvider(readData)
        nextPageView.setBitmapProvider(readData)
    }

    fun setCallback(callback: ReadViewCallback) {
        readData.callback = this.unite(callback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        threadPool.shutdownNow()
    }

    private fun startTask(task: Runnable) {
        threadPool.submit(task)
    }

    override fun hasNextPage(): Boolean {
        if (!initFinished ||  chapCount == 0) return false
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
        Log.d(TAG, "onPageChange: oldChapIndex = $oldChapIndex, newChapIndex = $newChapIndex")
        startTask {
            readData.requestLoadAndSplit(newChapIndex) {
                refreshAllPages()
            }
        }
    }

    private fun onPageChange(oldChapIndex: Int, oldPageIndex: Int,
        newChapIndex: Int, newPageIndex: Int) {
        if (oldChapIndex != newChapIndex) {
            onChapterChange(oldChapIndex, newChapIndex)
        }
        Log.d(TAG, "onPageChange: oldPageIndex = $oldPageIndex, newPageIndex = $newPageIndex")
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
        refreshAllPages()
        startTask {
            readData.requestLoadAndSplit(chapIndex) {
                refreshAllPages()
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
    private fun onInitSuccess() {
        curPageView.visible()
        prePageView.visible()
        nextPageView.visible()
        removeView(initView)
        initView = null
        initFinished = true
    }

    override fun onTocInitialized(book: BookData?, success: Boolean) {
        if (success) {
            post {
                onInitSuccess()
                refreshAllPages()
            }
        } else {
            (initView as TextView).text = "加载失败"
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
        if (readData.callback == null) {        // 如果没设置回调，就不需要unite
            readData.callback = this
        }
        readData.setProcess(chapIndex, pageIndex)
        prepareInit()
        startTask {
            readData.loadBook()        // load toc
            readData.requestLoadChapters(chapIndex, alwaysLoad = true)
            post {
                readData.requestSplitChapters(chapIndex) {
                    refreshAllPages()
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
     * 刷新当前ReadPage页面内容
     */
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

    /**
     * 刷新上一页的内容
     */
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

    /**
     * 刷新下一页的内容
     */
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
     * 刷新所有ReadPage视图
     */
    private fun refreshAllPages() {
        refreshCurPage()
        refreshNextPage()
        refreshPrevPage()
    }

    /**
     * 配置绘制章节主题内容的Paint
     * 注意：该函数不会触发刷新，需要在调用openBook()、showText()之前配置好
     */
    fun configContentPaint(config: (titlePaint: Paint) -> Unit) {
        config(contentConfig.contentPaint)
    }

    /**
     * 配置绘制章节标题的Paint，该函数不会触发刷新
     * 注意：该函数不会触发刷新，需要在调用openBook()、showText()之前配置好
     */
    fun configTitlePaint(config: (titlePaint: Paint) -> Unit) {
        config(contentConfig.titlePaint)
    }

    fun getContentColor() = contentConfig.contentColor
    fun getTitleColor() = contentConfig.titleColor

    /**
     * 设置内容字体的颜色
     */
    fun setContentColor(color: Int) {
        if (color == getContentColor()) return
        contentConfig.contentPaint.color = color
        refreshAllPages()
    }

    /**
     * 设置标题字体的颜色
     */
    fun setTitleColor(color: Int) {
        if (color == getTitleColor()) return
        contentConfig.titlePaint.color = color
        refreshAllPages()
    }

    /**
     * 获取内容的字体大小
     */
    fun getContentSize() = contentConfig.contentPaint.textSize

    /**
     * 获取标题的字体大小
     */
    fun getTitleSize() = contentConfig.titlePaint.textSize

    /**
     * 验证文字大小的有效性
     */
    private fun validTextSize(size: Float, isTitle: Boolean = false): Boolean {
        val max = if (isTitle) 180 else 150
        val min = 25
        if (size > min && size < max) {
            return true
        }
        return false
    }

    /**
     * 设置正文字体大小
     * @return 返回值为true即更改字体大小成功，反之失败
     */
    fun setContentSize(size: Float): Boolean {
        if (!validTextSize(size) || getContentSize() == size)
            return false
        refreshWithSizeChange {
            contentConfig.contentPaint.textSize = size
            Log.d(TAG, "setContentSize: current contentSize = $size")
        }
        return true
    }

    /**
     * 设置章节标题字体大小
     * @return 返回值为true即更改字体大小成功，反之失败
     */
    fun setTitleSize(size: Float): Boolean {
        if (!validTextSize(size, true) || getTitleSize() == size)
            return false
        refreshWithSizeChange {
            contentConfig.titlePaint.textSize = size
            Log.d(TAG, "setTitleSize: current titleSize = $size")
        }
        return true
    }

    private fun refreshWithSizeChange(resize: () -> Unit) {
        if (!initFinished) return
        val chap = readData.getChap(curChapIndex)
        if (chap.status != ChapterStatus.FINISHED) {
            return
        }
        resize()
        val chapIndex = curChapIndex
        val oldPageIndex = curPageIndex
        val pagePercent = curPageIndex.toFloat() / chap.pageCount
        // 清空已有的分页数据
        readData.book?.clearAllPage()
        readData.requestSplitChapters(curChapIndex) {
            if (curChapIndex == chapIndex) {
                var newPageIndex = (it.pageCount * pagePercent).toInt()
                if (newPageIndex < 1) newPageIndex = 1
                Log.d(TAG, "resize: oldPageIndex = ${oldPageIndex}, newPageIndex = $newPageIndex" )
                readData.setProcess(chapIndex, newPageIndex)
            }
            refreshAllPages()
        }
    }

    /**
     * 配置预加载参数
     * @param before 预加载当前章节之前的章节数
     * @param behind 预加载当前章节之后的章节数
     */
    fun setPreprocessParas(
        @IntRange(from = 0) before: Int,
        @IntRange(from = 0) behind: Int
    ) {
        readData.preprocessBefore = before
        readData.preprocessBehind = behind
    }

}