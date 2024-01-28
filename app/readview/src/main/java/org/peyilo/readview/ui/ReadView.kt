package org.peyilo.readview.ui

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.IntRange
import org.peyilo.readview.data.BookData
import org.peyilo.readview.loader.BookLoader
import org.peyilo.readview.loader.DefaultNativeLoader
import org.peyilo.readview.loader.SimpleTextLoader
import java.io.File

private const val TAG = "ReadView"
class ReadView(context: Context, attrs: AttributeSet? = null): PageContainer(context, attrs) {

    fun openBook(
        loader: BookLoader,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        ReadBook.loader = loader
        ReadBook.curChapIndex = chapIndex
        ReadBook.curPageIndex = pageIndex
    }

    fun openFile(
        file: File,
        @IntRange(from = 1) chapIndex: Int = 1,
        @IntRange(from = 1) pageIndex: Int = 1
    ) {
        openBook(DefaultNativeLoader(file), chapIndex, pageIndex)
    }

    fun showText(text: String) {
        openBook(SimpleTextLoader(text))
    }

    interface Callback {
        fun onTocInitSuccess(bookData: BookData) = Unit
        fun onTocInitFailed(e: Exception) = Unit
        fun onLoadChap() = Unit
    }
}