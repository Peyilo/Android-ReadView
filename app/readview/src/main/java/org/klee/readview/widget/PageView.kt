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
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import org.klee.readview.widget.api.BitmapProvider

private const val TAG = "PageView"
class PageView(context: Context, attributeSet: AttributeSet? = null)
    : ViewGroup(context, attributeSet) {

    lateinit var layout: View
        private set
    lateinit var content: ContentView
        private set
    var header: View? = null
        private set
    var footer: View? = null
        private set

    var initFinished = false
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
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
            if (child.visibility != GONE) {
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }

    /**
     * 初始化ReadPage的子view
     */
    fun initLayout(
        @LayoutRes layoutId: Int, @IdRes contentId: Int,
        @IdRes headerId: Int = NONE, @IdRes footerId: Int = NONE
    ) {
        if (initFinished) throw IllegalStateException("不要重复初始化！")
        require(layoutId != NONE && contentId != NONE)
        layout = LayoutInflater.from(context).inflate(layoutId, this)
        content = layout.findViewById(contentId)
        if (headerId != NONE) {
            header = layout.findViewById(headerId)
        }
        if (footerId != NONE) {
            footer = layout.findViewById(footerId)
        }
        initFinished = true
    }

    companion object {
        const val NONE = -1
    }

    fun setBitmapProvider(provider: BitmapProvider) {
        content.bitmapProvider = provider
    }

    /**
     * 让当前PageView绑定指定的chapIndex和pageIndex
     */
    fun bindContent(chapIndex: Int, pageIndex: Int, forceRefresh: Boolean = true) {
        var hasChanged = false
        if (content.indexBean.chapIndex != chapIndex || content.indexBean.pageIndex != pageIndex) {
            content.indexBean.chapIndex = chapIndex
            content.indexBean.pageIndex = pageIndex
            hasChanged = true
        }
        // 刷新
        if (forceRefresh || hasChanged) {
            Log.d(TAG, "bindContent: refresh")
            if (Looper.getMainLooper().thread == Thread.currentThread()) {
                content.invalidate()        // 如果当前为主线程，就直接调用invalidate()进行刷新
            } else {
                content.postInvalidate()
            }
        }
    }
}