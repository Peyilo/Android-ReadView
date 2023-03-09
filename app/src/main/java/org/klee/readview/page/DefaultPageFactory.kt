package org.klee.readview.page

import android.graphics.Bitmap
import android.graphics.Canvas
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.LineData
import org.klee.readview.entities.PageData

class DefaultPageFactory: IPageFactory {

    private val remainedWidth get() = ContentConfig.contentWidth -
            ContentConfig.contentPaddingLeft - ContentConfig.contentPaddingRight
    private val remainedHeight get() = ContentConfig.contentHeight -
            ContentConfig.contentPaddingTop - ContentConfig.contentPaddingBottom
    private val startLeft get() = ContentConfig.contentPaddingLeft
    private val startTop get() = ContentConfig.contentPaddingTop

    private var breaker: IBreaker = DefaultBreaker()
    private val pageCanvas by lazy { Canvas() }     // 避免多次创建Canvas对象
    private val loadingCanvas by lazy { Canvas() }

    override fun splitPage(chapData: ChapData): Boolean {
        // 如果留给绘制内容的空间不足以绘制标题或者正文的一行，直接返回false
        if (ContentConfig.contentSize > remainedHeight
            || ContentConfig.titleSize > remainedHeight) {
            return false
        }
        val content = chapData.content ?: return false
        chapData.clearPages()           // 清空pageData
        val width = remainedWidth
        var height = remainedHeight
        val chapIndex = chapData.chapIndex
        var curPageIndex = 1
        var page = PageData(chapIndex, curPageIndex)
        // 切割标题
        val titleLines = breaker.breakLines(chapData.title, width, ContentConfig.titlePaint)
        titleLines.forEach {
            page.addTitleLine(it)
        }
        var offset = 0F     // 正文内容的偏移
        if (titleLines.isNotEmpty()) {
            offset += ContentConfig.titleMargin
            offset += ContentConfig.titleSize * titleLines.size
            offset += ContentConfig.lineMargin * (titleLines.size - 1)
        }
        height -= offset.toInt()
        // 如果剩余空间已经不足以再添加一行，就换成下一页
        if (height < ContentConfig.contentSize) {
            height = remainedHeight
            chapData.addPage(page)
            curPageIndex++
            page = PageData(chapIndex, curPageIndex)
        }
        // 开始正文内容的处理
        val paras = breaker.breakParas(content)
        paras.forEach { para ->
            val breakLines = breaker.breakLines(para, width,
                paint = ContentConfig.contentPaint,
                textMargin = ContentConfig.textMargin,
                offset = ContentConfig.lineOffset)
            val size = breakLines.size
            for (i in 0 until size) {
                val line = breakLines[i]
                if (height < ContentConfig.contentSize) {
                    height = remainedHeight
                    chapData.addPage(page)
                    curPageIndex++
                    page = PageData(chapIndex, curPageIndex)
                }
                val lineData = LineData(line).apply {
                    if (i == 0)
                        isFirst = true
                    if (i == size - 1)
                        isLast = true
                }
                page.addContentLine(lineData)
                height -= (ContentConfig.contentSize + ContentConfig.lineMargin).toInt()
            }
            height -= ContentConfig.paraMargin.toInt()      // 处理段落的额外间距
        }
        chapData.addPage(page)
        breaker.recycle()
        return true
    }

    @Synchronized override fun createPageBitmap(pageData: PageData): Bitmap {
        // 在背景上绘制文字
        val page = ContentConfig.getBgBitmap()
        pageCanvas.setBitmap(page)
        drawPage(pageData, pageCanvas)
        pageCanvas.setBitmap(null)
        return page
    }

    private fun drawPage(pageData: PageData, canvas: Canvas) {
        val contentPaint = ContentConfig.contentPaint
        val titlePaint = ContentConfig.titlePaint
        var base = startTop
        val left = startLeft
        // 绘制标题
        for (i in 1..pageData.titleLineCount) {
            val title = pageData.getTitleLine(i)
            base += ContentConfig.titleSize
            canvas.drawText(title, left, base, titlePaint)
            if (i != pageData.titleLineCount) {     // 不是最后一行，需要处理额外的行间距
                base += ContentConfig.lineMargin
            }
        }
        if (pageData.titleLineCount != 0) {
            base += ContentConfig.titleMargin
        }
        // 绘制正文内容
        for (i in 1..pageData.contentLineCount) {
            val content = pageData.getContentLine(i)
            base += ContentConfig.contentSize
            if (content.isFirst) {
                canvas.drawText(content.line, ContentConfig.lineOffset + left, base, contentPaint)
            } else {
                canvas.drawText(content.line, left, base, contentPaint)
            }
            base += ContentConfig.lineMargin
            if (i != pageData.contentLineCount && content.isLast) {     // 处理段落之间的间距
                base += ContentConfig.paraMargin
            }
        }
    }

    /**
     * 绘制加载界面
     */
    @Synchronized override fun createLoadingBitmap(title: String, msg: String): Bitmap {
        val bitmap = ContentConfig.getBgBitmap()
        loadingCanvas.setBitmap(bitmap)
        val paint = ContentConfig.loadingPaint
        val titleWidth = paint.measureText(title)
        val msgWidth = paint.measureText(msg)
        var base = remainedHeight / 2 - ContentConfig.lineMargin / 2
        var left = remainedWidth / 2 - titleWidth / 2
        loadingCanvas.drawText(title, left, base, paint)
        base += ContentConfig.loadingSize + ContentConfig.lineMargin
        left = remainedWidth / 2 - msgWidth / 2
        loadingCanvas.drawText(msg, left, base, paint)
        loadingCanvas.setBitmap(null)
        return bitmap
    }
}