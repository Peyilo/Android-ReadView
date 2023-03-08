package org.klee.readview.page

import android.graphics.Bitmap
import android.graphics.Canvas
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.LineData
import org.klee.readview.entities.PageData

class DefaultPageFactory: IPageFactory {

    override var breaker: IBreaker = DefaultBreaker()
    private val canvas by lazy { Canvas() }     // 避免多次创建Canvas对象

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
        val titleLines = breaker.breakLines(chapData.title, width.toFloat(), ContentConfig.titlePaint)
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
            val breakLines = breaker.breakLines(para, width.toFloat(),
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

    override fun createPage(pageData: PageData): Bitmap {
        // 在背景上绘制文字
        val page = ContentConfig.getBgBitmap()
        canvas.setBitmap(page)
        drawPage(pageData, canvas)
        canvas.setBitmap(null)
        return page
    }

    private fun drawPage(pageData: PageData, canvas: Canvas) {
        val contentPaint = ContentConfig.contentPaint
        val titlePaint = ContentConfig.titlePaint
        var base = 0F
        // 绘制标题
        for (i in 1..pageData.titleLineCount) {
            val title = pageData.getTitleLine(i)
            base += ContentConfig.titleSize
            canvas.drawText(title, 0F, base, titlePaint)
            if (i != pageData.titleLineCount) {     // 不是最后一行，需要处理额外的行间距
                base += ContentConfig.lineMargin
            }
        }
        base += ContentConfig.titleMargin
        // 绘制正文内容
        for (i in 1..pageData.contentLineCount) {
            val content = pageData.getContentLine(i)
            base += ContentConfig.contentSize
            if (content.isFirst) {
                canvas.drawText(content.line, ContentConfig.lineOffset, base, contentPaint)
            } else {
                canvas.drawText(content.line, 0F, base, contentPaint)
            }
            base += ContentConfig.lineMargin
            if (i != pageData.contentLineCount && content.isLast) {     // 处理段落之间的间距
                base += ContentConfig.paraMargin
            }
        }
    }
}