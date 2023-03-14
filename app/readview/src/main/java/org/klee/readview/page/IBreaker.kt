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

interface IBreaker {

    /**
     * 将指定字符串切割成段落列表
     */
    fun breakParas(content: String): List<String>

    /**
     * 对一个段落进行断行
     * @param para 待断行的段落
     * @param offset 段落首行的偏移量
     * @param width 一行文字的最大宽度
     * @param paint 绘制文字的画笔
     */
    fun breakLines(para: String, width: Float,
                   paint: Paint, textMargin: Float = 0F,
                   offset: Float = 0F): List<String>

    /**
     * 在本轮IBreaker使用结束以后，调用该函数清除产生的缓存数据
     */
    fun recycle() = Unit

}