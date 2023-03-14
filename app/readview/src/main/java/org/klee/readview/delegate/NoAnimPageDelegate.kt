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

class NoAnimPageDelegate(readView: BaseReadView) : HorizontalPageDelegate(readView) {

    override fun onUpdateScrollValue(pageDirection: PageDirection, pageView: PageView) {
        when (pageDirection) {
            PageDirection.NEXT ->
                pageView.scrollTo(0, 0)
            PageDirection.PREV ->
                pageView.scrollTo(readView.width, 0)
            else -> Unit
        }
    }

    override fun onFlip(): PageDirection {
        val distance = startPoint.x - touchPoint.x
        return if (distance > 0) {
            PageDirection.NEXT
        } else if (distance < 0) {
            prevPage.scrollTo(0, 0)
            PageDirection.PREV
        } else {
            PageDirection.NONE
        }
    }

    override fun startAnim(pageDirection: PageDirection) {
        when (pageDirection) {
            PageDirection.NEXT ->
                onUpdateChildView(pageDirection)
            PageDirection.PREV ->
                onUpdateChildView(pageDirection)
            else -> Unit
        }
    }
}