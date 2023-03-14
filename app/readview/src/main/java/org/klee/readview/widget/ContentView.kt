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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.klee.readview.config.ContentConfig
import org.klee.readview.entities.IndexBean
import org.klee.readview.widget.api.BitmapProvider

private const val TAG = "ContentView"
class ContentView(context: Context, attributeSet: AttributeSet? = null)
    : View(context, attributeSet) {

    lateinit var config: org.klee.readview.config.ContentConfig
    lateinit var bitmapProvider: BitmapProvider
    val indexBean by lazy { IndexBean() }
    private val bitmap get(): Bitmap {
        val curBitmap = bitmapProvider.getBitmap(indexBean)
        cache?.recycle()
        cache = curBitmap
        return curBitmap
    }
    private var cache: Bitmap? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 初始化尺寸参数
        if (!config.contentDimenInitialized) {
            config.setContentDimen(width, height)
        }
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(bitmap, 0F, 0F, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cache?.apply {
            if (!cache!!.isRecycled) {
                cache!!.recycle()
            }
        }
    }
}