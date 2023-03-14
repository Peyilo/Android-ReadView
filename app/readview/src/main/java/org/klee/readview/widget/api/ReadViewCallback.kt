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

package org.klee.readview.widget.api

import android.graphics.Bitmap
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import org.klee.readview.widget.PageView

/**
 * ReadView在加载过程中的回调
 */
interface ReadViewCallback {

    /**
     * 当章节目录完成初始化时的回调
     * 注意：该方法会在子线程中执行，如果涉及到UI操作，请利用post()在主线程执行
     * @param book 初始化完成后得到的BookData对象，保存有章节目录信息
     */
    fun onTocInitSuccess(book: BookData) = Unit

    /**
     * 章节目录初始化失败
     * 注意：该方法会在子线程中执行，如果涉及到UI操作，请利用post()在主线程执行
     * @param e 造成初始化失败的异常
     */
    fun onTocInitFailed(e: Exception) = Unit

    /**
     * 当章节目录完成初始化、章节内容完成加载以及分页、刷新视图以后，会回调该函数
     * 该方法会在主线程执行
     * @param book 完全初始化后得到的BookData对象，保存有章节目录信息、预加载章节的内容信息以及分页信息
     */
    fun onInitFinished(book: BookData) = Unit

    /**
     * 加载章节完成（未完成分页）的回调，注意：该函数处于子线程中
     * @param chap 内容加载完成的章节
     * @param success 是否加载成功
     */
    fun onLoadChap(chap: ChapData, success: Boolean) = Unit

    /**
     * 可以通过该方法完成PageView的页眉视图、页脚视图更新
     * @param convertView 要更新的页面
     * @param newChap 当前页面绑定的章节
     * @param newPageIndex 当前页面绑定的分页序号
     */
    fun onUpdatePage(convertView: PageView, newChap: ChapData, newPageIndex: Int) = Unit

    /**
     * ReadView内部生成bitmap对象时，都会调用本方法，不要在本方法中recycle bitmap对象
     * @param bitmap 生成的bitmap
     */
    fun onBitmapCreate(bitmap: Bitmap) = Unit

}