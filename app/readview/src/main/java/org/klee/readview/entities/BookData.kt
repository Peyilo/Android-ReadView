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

/**
 * 书籍数据
 */
class BookData (
    var name: String = "",                           // 书籍名称
    var author: String = "",                         // 作者名
) : AdditionalData() {
    private val chapList: MutableList<ChapData> by lazy {
        ArrayList()
    }
    val chapCount get() = chapList.size         // 章节数

    fun isEmpty() = chapCount == 0              // 是否为空

    // 添加章节
    fun addChapter(chapData: ChapData) {
        chapList.add(chapData)
    }
    // 获取章节
    fun getChapter(@IntRange(from = 1) chapIndex: Int) = chapList[chapIndex - 1]

    fun clearAllPage() {
        chapList.forEach {
            if (it.status == ChapterStatus.FINISHED) {
                it.clearPages()
                it.status = ChapterStatus.NO_SPLIT
            }
        }
    }
}