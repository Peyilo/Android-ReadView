package org.klee.readview.delegate

import android.view.MotionEvent
import org.klee.readview.ReadView

abstract class PageDelegate (val readView: ReadView) {

    abstract fun onTouchEvent(event: MotionEvent): Boolean

    abstract fun computeScrollOffset()

}