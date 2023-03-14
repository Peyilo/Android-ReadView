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

package org.klee.readview.utils

import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.pow

/**
 * 两个点之间的距离
 */
fun PointF.apartFrom(pointF: PointF): Float {
    return (this.x - pointF.x).pow(2) + (this.y - pointF.y).pow(2)
}

/**
 * 以当前点为原点，并且将当前点作为起点向endPoint做一条直线，获取该直线与x轴的夹角
 */
fun PointF.angle(endPoint: PointF): Int {
    val distanceX = endPoint.x - this.x
    val distanceY = endPoint.y - this.y
    return Math.toDegrees(atan2(distanceY.toDouble(), distanceX.toDouble())).toInt()
}