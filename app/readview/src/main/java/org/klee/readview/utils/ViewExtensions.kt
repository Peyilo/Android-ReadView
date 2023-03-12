package org.klee.readview.utils

import android.view.View

fun View.gone() {
    if (visibility != View.GONE) {
        visibility = View.GONE
    }
}

fun View.invisible() {
    if (visibility != View.INVISIBLE) {
        visibility = View.INVISIBLE
    }
}

fun View.visible() {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
}