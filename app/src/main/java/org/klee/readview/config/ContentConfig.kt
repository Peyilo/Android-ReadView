package org.klee.readview.config

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import org.klee.readview.page.DefaultPageFactory
import org.klee.readview.page.IPageFactory

/**
 * ContentView绘制参数配置
 */
object ContentConfig {

    // contentView的宽高
    var contentDimenInitialized = false         // contentView的尺寸是否完成了初始化
        private set
    var contentWidth = 0
        private set
    var contentHeight = 0
        private set

    fun setContentDimen(width: Int, height: Int) {
        contentDimenInitialized = true
        contentWidth = width
        contentHeight = height
    }

    private var pageFactory: IPageFactory? = null

    fun getPageFactory(): IPageFactory {
        if (pageFactory == null) {
            pageFactory = DefaultPageFactory()
        }
        return pageFactory!!
    }

    fun setPageFactory(pageFactory: IPageFactory) {
        this.pageFactory = pageFactory
    }

    val contentPaint: Paint by lazy { Paint() }

    val titlePaint: Paint by lazy { Paint().apply {
        typeface = Typeface.DEFAULT_BOLD
    } }

    val contentColor get() = contentPaint.color
    val contentSize get() = contentPaint.textSize
    val titleColor get() = titlePaint.color
    val titleSize get() = titlePaint.textSize

    // contentView内部尺寸参数
    var titleMargin = 40F                       // 章节标题与章节正文的间距
    var textMargin = 0F                         // 字符间隔
    var lineMargin = 0F                         // 行间隔
    var paraMargin = 0F                         // 段落的额外间隔

    private var bgBitmap: Bitmap? = null

    private fun createBackground(): Bitmap {
        if (contentWidth > 0 && contentHeight > 0) {
            return Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.RGB_565)
        }
        throw IllegalStateException("当前contentWidth = $contentWidth, contentHeight = $contentHeight!")
    }

    fun getBgBitmap(): Bitmap {
        return bgBitmap ?: let {
            bgBitmap = createBackground()
            bgBitmap!!
        }
    }
}