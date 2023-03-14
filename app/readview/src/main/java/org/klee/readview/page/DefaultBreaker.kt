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

import android.graphics.Paint

class DefaultBreaker: IBreaker {

    @Synchronized override fun breakParas(content: String): List<String> {
        val paras = ArrayList<String>()
        val splits = content.split("\n")
        splits.forEach {
            val trim = it.trim()
            if (trim.isNotEmpty()) {
                paras.add(trim)
            }
        }
        return paras
    }

    override fun breakLines(
        para: String,
        width: Float, paint: Paint,
        textMargin: Float, offset: Float
    ): List<String> {
        val lines = ArrayList<String>()
        val stringBuilder = StringBuilder()
        var w = width - offset
        var dimen: Float
        para.forEach {
            dimen = paint.measureText(it.toString())
            if (w < dimen) {    // 剩余宽度已经不足以留给该字符
                lines.add(stringBuilder.toString())
                stringBuilder.clear()
                w = width
            }
            w -= dimen + textMargin
            stringBuilder.append(it)
        }
        val lastLine = stringBuilder.toString()
        if (lastLine.isNotEmpty()) {
            lines.add(lastLine)
        }
        stringBuilder.clear()       // 清空
        return lines
    }

}