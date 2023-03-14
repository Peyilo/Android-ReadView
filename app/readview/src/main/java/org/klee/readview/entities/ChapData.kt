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

package org.klee.readview.entities
import androidx.annotation.IntRange

// 章节数据
class ChapData (
    @IntRange(from = 1)
    val chapIndex: Int,
    val title: String = "",
    var content: String? = null
) : AdditionalData() {

    var status: ChapterStatus = ChapterStatus.NO_LOAD           // 章节状态

    private val pageList: MutableList<PageData> by lazy {
        ArrayList()
    }

    val pageCount get() = pageList.size     // 完成加载之后，至少有一页（可能为空白页）

    fun addPage(pageData: PageData) {
        pageList.add(pageData)
    }

    fun clearPages() {
        pageList.clear()
    }

    fun getPage(@IntRange(from = 1) pageIndex: Int) = pageList[pageIndex - 1]
}