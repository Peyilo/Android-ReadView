package org.klee.readview.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.IntRange
import org.klee.readview.api.BookLoader
import org.klee.readview.config.ContentConfig
import org.klee.readview.delegate.CoverPageDelegate
import org.klee.readview.delegate.PageDelegate
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.PageDirection
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * 负责提供多种翻页模式下的动画实现
 */
class ReadView(context: Context, attributeSet: AttributeSet)
    : ViewGroup(context, attributeSet) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val scrollSlop = touchSlop

    val pageFactory get() = ContentConfig.getPageFactory()

    private lateinit var bookLoader: BookLoader
    var book: BookData? = null
    val chapCount: Int get() {              // 章节数
        book?.let {
            return book!!.chapCount
        }
        return 0
    }

    @IntRange(from = 1) var curChapIndex: Int = 1
    @IntRange(from = 1) var curPageIndex: Int = 1

    var preLoadBefore = 2   // 预加载当前章节之前的2章节
    var preLoadBehind = 2   // 预加载当前章节之后的2章节


    private val threadPool by lazy { Executors.newFixedThreadPool(10) }

    internal lateinit var curPageView: ReadPage
    internal lateinit var prePageView: ReadPage
    internal lateinit var nextPageView: ReadPage

    var shadowWidth: Int = 30

    private var isMove = false
    private var isPageMove = false
    var longPress = false

    val startPoint by lazy { PointF() }         // 第一次DOWN事件的坐标
    val lastPoint by lazy { PointF() }          // 上一次触摸的坐标
    val touchPoint by lazy { PointF() }         // 当前触摸的坐标

    var pageDelegate: PageDelegate = CoverPageDelegate(this)

    // 初始化ReadPage
    fun initPage(initializer: (readPage: ReadPage, position: Int) -> Unit) {
        curPageView = ReadPage(context)
        prePageView = ReadPage(context)
        nextPageView = ReadPage(context)
        initializer(curPageView, 0)
        initializer(prePageView, -1)
        initializer(nextPageView, 1)
        addView(nextPageView)
        addView(curPageView)
        addView(prePageView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
        require(childCount == 3)
        // 设置子view的测量大小
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.measure(widthMeasureSpec, heightMeasureSpec)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val height = child.measuredHeight
            val width = child.measuredWidth
            // 子view全部叠放在一起，但是最顶层的子view被设置了scrollX，所以滑出了屏幕
            child.layout(0, 0, width, height)
        }
        pageDelegate.onLayout()
    }

    private fun upTouchPoint(event: MotionEvent) {
        lastPoint.set(touchPoint)
        touchPoint.set(event.x, event.y)
    }

    private fun upStartPointer(x: Float, y: Float) {
        startPoint.set(x, y)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        upTouchPoint(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                upStartPointer(event.x, event.y)
                pageDelegate.onTouch(event)
            }
            MotionEvent.ACTION_MOVE -> {
                // touchSlop确定是的isMove，该标记位用来区分点击和滑动
                if (!isMove) {
                    isMove = abs(startPoint.x - event.x) > touchSlop || abs(startPoint.x - event.y) > touchSlop
                }
                // scrollSlop确定的是isPageMove，该标记位用来确定页面是否发生了滑动
                if (!isPageMove) {
                    isPageMove = abs(startPoint.x - event.x) > scrollSlop || abs(startPoint.x - event.y) > scrollSlop
                }
                if (isMove) {
                    // longPress = false
                    if (isPageMove) {
                        pageDelegate.onTouch(event)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                pageDelegate.onTouch(event)
            }
        }
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        pageDelegate.computeScrollOffset()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let {
            pageDelegate.dispatchDraw(canvas)
        }
    }

    fun hasNextPage() = true
    fun hasPrevPage() = true

    internal fun updateChildView(convertView: ReadPage,
                                 direction: PageDirection
    ) : ReadPage {
        return convertView
    }

    fun getChapter(@IntRange(from = 1) chapIndex: Int) = book!!.getChapter(chapIndex)

    /**
     * 当章节目录完成初始化时的回调
     */
    fun onTocInitialized(book: BookData) {}

    fun onChapLoaded(chap: ChapData) {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        threadPool.shutdown()
    }

    private fun startTask(task: Runnable) {
        threadPool.submit(task)
    }

    /**
     * 根据指定的章节序号，生成需要加载的章节的序号列表
     */
    private fun getPreChapIndexList(chapIndex: Int): List<Int> {
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

    fun openBook(loader: BookLoader,
                 @IntRange(from = 1) chapIndex: Int = 1,
                 @IntRange(from = 1) pageIndex: Int = 1) {
        this.bookLoader = loader
        this.curChapIndex = chapIndex
        this.curPageIndex = pageIndex
        startTask {
            val book = loader.loadBook()            // load toc
            this.book = book
            onTocInitialized(book)                  // callback function
            val indexList = getPreChapIndexList(this.curChapIndex)
            indexList.forEach {
                val chap = getChapter(it)
                loader.loadChapter(chap)
                onChapLoaded(chap)
            }
            post {
                // 完成分页
                indexList.forEach {
                    val chap = getChapter(it)
                    pageFactory.splitPage(chap)
                }
            }
        }
    }
}