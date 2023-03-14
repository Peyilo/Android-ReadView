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

package org.klee.readview.page

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.LineData
import org.klee.readview.entities.PageData

class DefaultPageFactory(private val contentConfig: org.klee.readview.config.ContentConfig): IPageFactory {

    private val remainedWidth get() = contentConfig.contentWidth -
            contentConfig.contentPaddingLeft - contentConfig.contentPaddingRight
    private val remainedHeight get() = contentConfig.contentHeight -
            contentConfig.contentPaddingTop - contentConfig.contentPaddingBottom
    private val startLeft get() = contentConfig.contentPaddingLeft
    private val startTop get() = contentConfig.contentPaddingTop

    private var breaker: IBreaker = DefaultBreaker()
    private val pageCanvas by lazy { Canvas() }     // 避免多次创建Canvas对象
    private val loadingCanvas by lazy { Canvas() }

    override fun splitPage(chapData: ChapData): Boolean {
        // 如果留给绘制内容的空间不足以绘制标题或者正文的一行，直接返回false
        if (contentConfig.contentSize > remainedHeight
            || contentConfig.titleSize > remainedHeight) {
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
        val titleLines = breaker.breakLines(chapData.title, width, contentConfig.titlePaint)
        titleLines.forEach {
            page.addTitleLine(it)
        }
        var offset = 0F     // 正文内容的偏移
        if (titleLines.isNotEmpty()) {
            offset += contentConfig.titleMargin
            offset += contentConfig.titleSize * titleLines.size
            offset += contentConfig.lineMargin * (titleLines.size - 1)
        }
        height -= offset.toInt()
        // 如果剩余空间已经不足以再添加一行，就换成下一页
        if (height < contentConfig.contentSize) {
            height = remainedHeight
            chapData.addPage(page)
            curPageIndex++
            page = PageData(chapIndex, curPageIndex)
        }
        // 开始正文内容的处理
        val paras = breaker.breakParas(content)
        paras.forEach { para ->
            val breakLines = breaker.breakLines(para, width,
                paint = contentConfig.contentPaint,
                textMargin = contentConfig.textMargin,
                offset = contentConfig.lineOffset)
            val size = breakLines.size
            for (i in 0 until size) {
                val line = breakLines[i]
                if (height < contentConfig.contentSize) {
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
                height -= (contentConfig.contentSize + contentConfig.lineMargin).toInt()
            }
            height -= contentConfig.paraMargin.toInt()      // 处理段落的额外间距
        }
        chapData.addPage(page)
        breaker.recycle()
        return true
    }

    @Synchronized override fun createPageBitmap(pageData: PageData): Bitmap {
        // 在背景上绘制文字
        val page = contentConfig.getBgBitmap()
        pageCanvas.setBitmap(page)
        drawPage(pageData, pageCanvas)
        pageCanvas.setBitmap(null)
        return page
    }

    private fun drawPage(pageData: PageData, canvas: Canvas) {
        val contentPaint = contentConfig.contentPaint
        val titlePaint = contentConfig.titlePaint
        var base = startTop
        val left = startLeft
        // 绘制标题
        for (i in 1..pageData.titleLineCount) {
            val title = pageData.getTitleLine(i)
            base += contentConfig.titleSize
            canvas.drawText(title, left, base, titlePaint)
            if (i != pageData.titleLineCount) {     // 不是最后一行，需要处理额外的行间距
                base += contentConfig.lineMargin
            }
        }
        if (pageData.titleLineCount != 0) {
            base += contentConfig.titleMargin
        }
        // 绘制正文内容
        for (i in 1..pageData.contentLineCount) {
            val content = pageData.getContentLine(i)
            base += contentConfig.contentSize
            if (content.isFirst) {
                canvas.drawText(content.line, contentConfig.lineOffset + left, base, contentPaint)
            } else {
                canvas.drawText(content.line, left, base, contentPaint)
            }
            base += contentConfig.lineMargin
            if (i != pageData.contentLineCount && content.isLast) {     // 处理段落之间的间距
                base += contentConfig.paraMargin
            }
        }
    }

    private val decorPaint by lazy {
        Paint().apply {
            strokeWidth = 2F
            style = Paint.Style.STROKE
            color = Color.parseColor("#D5D5D5")
        }
    }
    private val decorPadding = 3F
    private val verticalOffset = 10F
    private val horizontalOffset = 10F
    // 水平方向稍窄的矩形相关坐标
    private val verticalLeft get() =  contentConfig.contentPaddingLeft + decorPadding + verticalOffset
    private val verticalTop get() = contentConfig.contentPaddingTop + decorPadding
    private val verticalRight get() = contentConfig.contentWidth - contentConfig.contentPaddingRight - decorPadding - verticalOffset
    private val verticalBottom get() = contentConfig.contentHeight - contentConfig.contentPaddingBottom - decorPadding
    // 垂直方向上稍窄的矩形相关坐标
    private val horizontalLeft get() = contentConfig.contentPaddingLeft + decorPadding
    private val horizontalTop get() = contentConfig.contentPaddingTop + decorPadding + horizontalOffset
    private val horizontalRight get() = contentConfig.contentWidth - contentConfig.contentPaddingRight - decorPadding
    private val horizontalBottom get() = contentConfig.contentHeight - contentConfig.contentPaddingBottom - decorPadding - horizontalOffset

    /**
     * 绘制加载界面
     */
    @Synchronized override fun createLoadingBitmap(title: String, msg: String): Bitmap {
        val bitmap = contentConfig.getBgBitmap()
        loadingCanvas.setBitmap(bitmap)
        // 绘制文字
        val paint = contentConfig.loadingPaint
        val titleWidth = paint.measureText(title)
        val msgWidth = paint.measureText(msg)
        var base = remainedHeight / 2 - contentConfig.lineMargin / 2
        var left = remainedWidth / 2 - titleWidth / 2
        loadingCanvas.drawText(title, left, base, paint)
        base += contentConfig.loadingSize + contentConfig.lineMargin
        left = remainedWidth / 2 - msgWidth / 2
        loadingCanvas.drawText(msg, left, base, paint)
        // 绘制装饰
        loadingCanvas.drawRect(verticalLeft, verticalTop, verticalRight, verticalBottom, decorPaint)
        loadingCanvas.drawRect(horizontalLeft, horizontalTop, horizontalRight, horizontalBottom, decorPaint)
        loadingCanvas.setBitmap(null)
        return bitmap
    }
}