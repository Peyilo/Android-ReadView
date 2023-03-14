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

package org.klee.readview.config

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.klee.readview.page.DefaultPageFactory
import org.klee.readview.page.IPageFactory

/**
 * ContentView绘制参数配置
 */
class ContentConfig {

    // contentView的宽高
    var contentDimenInitialized = false         // contentView的尺寸是否完成了初始化
        private set
    var contentWidth = 0
        private set
    var contentHeight = 0
        private set

    /*******- contentView的padding参数 -********/
    var contentPaddingLeft = 60F
    var contentPaddingRight = 60F
    var contentPaddingTop = 20F
    var contentPaddingBottom = 20F

    fun setContentDimen(width: Int, height: Int) {
        contentDimenInitialized = true
        contentWidth = width
        contentHeight = height
    }

    private var pageFactory: IPageFactory? = null

    fun getPageFactory(): IPageFactory {
        if (pageFactory == null) {
            pageFactory = DefaultPageFactory(this)
        }
        return pageFactory!!
    }

    fun setPageFactory(pageFactory: IPageFactory) {
        this.pageFactory = pageFactory
    }

    val contentPaint: Paint by lazy { Paint().apply {
        textSize = 56F
        color = Color.parseColor("#2B2B2B")
        flags = Paint.ANTI_ALIAS_FLAG
    } }

    val titlePaint: Paint by lazy { Paint().apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 72F
        color = Color.BLACK
        flags = Paint.ANTI_ALIAS_FLAG
    } }

    val loadingPaint by lazy { Paint().apply {
        textSize = 45F
        color = Color.parseColor("#292929")
        flags = Paint.ANTI_ALIAS_FLAG
    } }

    val contentColor get() = contentPaint.color
    val contentSize get() = contentPaint.textSize
    val titleColor get() = titlePaint.color
    val titleSize get() = titlePaint.textSize
    val loadingSize get() = loadingPaint.textSize
    val loadingColor get() = loadingPaint.color

    /************** - contentView内部尺寸参数 - *****************/
    val lineOffset get() = contentPaint.measureText("测试") // 段落首行的偏移
    var titleMargin = 160F                                      // 章节标题与章节正文的间距
    var textMargin = 0F                                         // 字符间隔
    var lineMargin = 30F                                        // 行间隔
    var paraMargin = 20F                                        // 段落的额外间隔

    private var bgBitmap: Bitmap? = null

    /**
     * 获取一个和当前contentView的大小一样的透明bitmap
     * 每次调用该函数都会复制产生一个透明bitmap
     */
    fun getBgBitmap(): Bitmap {
        val bitmap = bgBitmap ?: let {
            bgBitmap = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.ARGB_8888)
            bgBitmap!!
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun destroy() {
        bgBitmap?.let {
            if (!bgBitmap!!.isRecycled) {
                bgBitmap!!.recycle()
            }
        }
    }
}