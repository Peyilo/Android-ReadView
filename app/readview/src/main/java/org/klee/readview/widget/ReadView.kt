/**
 * MIT License

 * Copyright (c) 2023 Klee

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.klee.readview.widget

import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
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
import org.klee.readview.loader.TextLoader
import org.klee.readview.utils.invisible
import org.klee.readview.utils.visible
import org.klee.readview.widget.api.ReadViewCallback
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "ReadView"

/**
 * 一个阅读界面视图，支持页眉、页脚的自定义配置。
 */
class ReadView(context: Context, attributeSet: AttributeSet?) :
    BaseReadView(context, attributeSet), ReadViewCallback
{
    private val threadPool by lazy { Executors.newFixedThreadPool(10) }
    private val readData by lazy { ReadData().apply {
        this.contentConfig = this@ReadView.contentConfig
        this.viewCallBack = this@ReadView
    } }

    val book: BookData get() = readData.book!!
    val chapCount get() = readData.chapCount            // 章节数
    val curChapIndex get() = readData.curChapIndex      // 当前章节序号
    val curPageIndex get() = readData.curPageIndex      // 当前分页序号

    private val viewCallback get() = readData.viewCallBack
    private val userCallback get() = readData.userCallback

    private var initView: View? = null
    private var initFinished = false
    private var attached = false

    private val myHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun initPage(initializer: (pageView: PageView, position: Int) -> Unit) {
        super.initPage(initializer)
        curPageView.setBitmapProvider(readData)
        prePageView.setBitmapProvider(readData)
        nextPageView.setBitmapProvider(readData)
    }

    /**
     * 设置回调接口
     * @param callback ReadViewCallback接口定义了几个常用的回调函数，具体信息请看ReadCallback的注释
     */
    fun setCallback(callback: ReadViewCallback) {
        readData.userCallback = callback
    }

    /**
     * 注意：只有在执行本方法之后，view的post()提交的任务才会执行，否则可能会被忽略
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        Log.d(TAG, "onAttachedToWindow: attached")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        threadPool.shutdownNow()                // 清理线程池，关闭正在执行的子线程
        attached = false
        Log.d(TAG, "onDetachedFromWindow: detached")
    }

    /**
     * 往线程池里提交一个任务
     */
    private fun startTask(task: Runnable) {
        threadPool.submit(task)
    }

    /**
     * @return 是否有下一页，该函数的返回值关系到页面是否可以翻页
     */
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

    /**
     * @return 是否有上一页，该函数的返回值关系到页面是否可以翻页
     */
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

    /**
     * 每当章节序号发生了改变，就会回调该函数
     * @param oldChapIndex 章节号发生改变之前所在章节的序号
     * @param newChapIndex 章节号发生改变之后的新章节的序号
     */
    private fun onChapterChange(oldChapIndex: Int, newChapIndex: Int) {
        Log.d(TAG, "onPageChange: oldChapIndex = $oldChapIndex, newChapIndex = $newChapIndex")
        startTask {
            // 完成预加载章节
            readData.requestLoadAndSplit(newChapIndex) {
                refreshAllPages()
            }
        }
    }

    /**
     * 每当当前页面发生改变，就会回调该函数
     * @param oldChapIndex 页面改变之前的章节序号
     * @param oldPageIndex 页面改变之前的分页序号
     * @param newChapIndex 页面改变之后的章节序号
     * @param newPageIndex 页面改变之后的分页序号
     */
    private fun onPageChange(oldChapIndex: Int, oldPageIndex: Int,
        newChapIndex: Int, newPageIndex: Int) {
        if (oldChapIndex != newChapIndex) {
            onChapterChange(oldChapIndex, newChapIndex)
        }
        Log.d(TAG, "onPageChange: oldPageIndex = $oldPageIndex, newPageIndex = $newPageIndex")
    }

    /**
     * 在这里完成章节序号、分页序号的更新操作
     */
    override fun onFlipToPrev() {
        val oldChapIndex = curChapIndex
        val oldPageIndex = curPageIndex
        readData.moveToPrevPage()
        onPageChange(oldChapIndex, oldPageIndex, curChapIndex, curPageIndex)
    }

    /**
     * 在这里完成章节序号、分页序号的更新操作
     */
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
            viewCallback.onUpdatePage(
                convertView,
                readData.getChap(indexBean.chapIndex),
                indexBean.pageIndex
            )
            userCallback?.onUpdatePage(
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
            addView(initView!!)
        }
        curPageView.invisible()         // 设置为不可见
        prePageView.invisible()
        nextPageView.invisible()
    }

    /**
     * 当章节目录成功完成初始化，需要根据初始化结果完成视图的刷新,以显示章节内容
     */
    override fun onTocInitSuccess(book: BookData) {
        myHandler.post {
            curPageView.visible()
            prePageView.visible()
            nextPageView.visible()
            removeView(initView)
            initView = null
            initFinished = true
        }
        Log.d(TAG, "onTocInitSuccess: ${book.chapCount}")
    }

    // 显示加载失败视图
    override fun onTocInitFailed(e: Exception) {
        (initView as TextView).text = "加载失败"
        Log.d(TAG, "onTocInitFailed: $e")
    }

    /**
     * 根据给定的BookLoader，加载并显示书籍的内容
     * @param loader 小说加载器
     * @param chapIndex 打开的章节序号，默认值为1
     * @param pageIndex 打开的页面序号，默认值为1
     */
    fun openBook(
        loader: BookLoader,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        readData.bookLoader = loader
        if (readData.userCallback == null) {        // 如果没设置回调，就不需要unite
            readData.userCallback = this
        }
        readData.setProcess(chapIndex, pageIndex)
        prepareInit()
        startTask {
            val initResult = readData.requestInitToc()        // load toc
            if (initResult) {
                readData.requestLoadChapters(chapIndex, alwaysLoad = true)
                // 章节分页依赖于view的宽高、所以需要在post()中执行
                // 而post()依赖于window的attach状态，所以需要等待attached置为true
                while (!attached) {}
                post {
                    readData.requestSplitChapters(chapIndex) {
                        refreshAllPages()
                        Log.d(TAG, "openBook: curPageCount = ${it.pageCount}")
                    }
                    viewCallback.onInitFinished(this.book)
                    userCallback?.onInitFinished(this.book)
                }
            }
        }
    }

    /**
     * 从本地文件中加载小说内容
     * @param file 文本文件
     * @param chapIndex 打开的章节序号，默认值为1
     * @param pageIndex 打开的页面序号，默认值为1
     */
    fun openBook(
        file: File,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        openBook(NativeLoader(file), chapIndex, pageIndex)
    }

    /**
     * 显示指定的字符串
     * @param text 要显示的字符串
     * @param pageIndex 打开的页面序号，默认值为1
     */
    fun showText(
        text: String,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        openBook(TextLoader(text), 1, pageIndex)
    }

    /**
     * 刷新当前ReadPage页面内容
     */
    private fun refreshCurPage() {
        readData.apply {
            curPageView.bindContent(curChapIndex, curPageIndex)
            userCallback?.onUpdatePage(
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
                userCallback?.onUpdatePage(
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
                userCallback?.onUpdatePage(
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
     * @return 是否有下一章
     */
    fun hasNextChap() = readData.hasNextChap()

    /**
     * @return 是否有上一章
     */
    fun hasPrevChap() = readData.hasPreChap()

    /**
     * 跳转到下一章节
     * @return 为true表示跳转成功，否则跳转失败
     */
    fun nextChap(): Boolean {
        if (hasNextChap()) {
            setProcess(curChapIndex + 1, 1)
            return true
        }
        return false
    }

    /**
     * 跳转到上一章节
     * @return 为true表示跳转成功，否则跳转失败
     */
    fun prevChap(): Boolean {
        if (hasPrevChap()) {
            setProcess(curChapIndex - 1, 1)
            return true
        }
        return false
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

    /**
     * 获取章节主体内容的颜色
     */
    fun getContentColor() = contentConfig.contentColor

    /**
     * 获取章节标题的颜色
     */
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

    /**
     * 如果有影响ContentView的布局的参数发生了变换，可以通过本函数进行来重新分页，并刷新视图
     * 例如：字体大小发生了变化、章节标题与章节内容的间隔发生变化等影响布局的参数变化
     * @param resize 再该闭包内完成相关参数的设置
     */
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
     * 设置预加载参数
     * 例如：当before=1，after=2时，每次加载指定章节时，会额外检查前一章节和后两章节是否完成了加载和分页，
     * 如果没有就会完成预加载以及预分页
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