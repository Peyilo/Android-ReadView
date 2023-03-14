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

class PageData (
    val chapIndex: Int,
    val pageIndex: Int,
) {
    private val titleLineList: ArrayList<String> by lazy { ArrayList() }
    private val contentLineList: ArrayList<LineData> by lazy { ArrayList() }

    fun addTitleLine(line: String) {
        titleLineList.add(line)
    }

    /**
     * lineIndex need larger than 0!
     */
    fun getTitleLine(lineIndex: Int) = titleLineList[lineIndex - 1]

    fun addContentLine(line: LineData) {
        contentLineList.add(line)
    }

    /**
     * lineIndex need larger than 0!
     */
    fun getContentLine(lineIndex: Int) = contentLineList[lineIndex - 1]

    val titleLineCount get() =  titleLineList.size
    val contentLineCount get() =  contentLineList.size
}
