package org.klee.readview.page

import android.graphics.Paint

interface IBreaker {

    // 切割段落
    fun breakParas(content: String): List<String>

    /**
     * 断行
     * @param para 待断行的段落
     * @param offset 段落首行的偏移量
     * @param width 一行文字的最大宽度
     * @param paint 绘制文字的画笔
     */
    fun breakLines(para: String, width: Float,
                   paint: Paint, textMargin: Float = 0F,
                   offset: Float = 0F): List<String>

    fun recycle()

}