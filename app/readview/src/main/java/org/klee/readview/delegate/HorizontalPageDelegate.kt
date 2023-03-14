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

package org.klee.readview.delegate

import org.klee.readview.widget.BaseReadView
import org.klee.readview.widget.PageView

abstract class HorizontalPageDelegate(readView: BaseReadView): PageDelegate(readView, 75) {

    protected var initPageDirection = PageDirection.NONE
        private set

    override fun onScrollValue() {
        // 将上一页移向屏幕之外
        prevPage.scrollTo(-readView.width, 0)
    }

    override fun initPageDirection(initGestureDirection: GestureDirection): PageDirection {
        // 完成对翻页方向的初始化
        initPageDirection = when (initGestureDirection) {
            GestureDirection.TO_RIGHT ->
                PageDirection.PREV
            GestureDirection.TO_LEFT ->
                PageDirection.NEXT
            else ->
                PageDirection.NONE
        }
        return initPageDirection
    }

    override fun onUpdateChildView(direction: PageDirection) {
        // 将距离当前页面最远的页面移除，再进行复用
        when (direction) {
            PageDirection.NEXT -> {
                readView.apply {
                    val convertView = readView.prePageView
                    prePageView = curPageView
                    curPageView = nextPageView
                    nextPageView = readView.updateChildView(convertView, direction)
                    removeView(convertView)
                    onUpdateScrollValue(PageDirection.NEXT, nextPageView)
                    addView(nextPageView, 0)
                }
            }
            PageDirection.PREV -> {
                readView.apply {
                    val convertView = nextPageView
                    nextPageView = curPageView
                    curPageView = prePageView
                    prePageView = updateChildView(convertView, direction)
                    removeView(convertView)
                    onUpdateScrollValue(PageDirection.PREV, prePageView)
                    addView(prePageView)
                }
            }
            else -> Unit
        }
    }

    protected abstract fun onUpdateScrollValue(pageDirection: PageDirection, pageView: PageView)

}