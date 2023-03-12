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