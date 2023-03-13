package org.klee.readview.loader

import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.regex.Pattern

/**
 * 一个简单的本地小说加载器
 */
class NativeLoader(private var file: File) : BookLoader {

    private val reader: BufferedReader by lazy {
        BufferedReader(FileReader(file))
    }

    private val titlePattern  by lazy { Pattern.compile("(^\\s*第)(.{1,9})[章节卷集部篇回](\\s*)(.*)") }

    /**
     * 判断指定字符串是否为章节标题
     * @param line 需要判断的目标字符串
     */
    private fun isTitle(line: String): Boolean {
        return titlePattern.matcher(line).matches()
    }

    override fun loadBook(): BookData {
        val bookData = BookData()
        var chapIndex = 1
        val stringBuilder = StringBuilder()
        var chap: ChapData? = null
        var line: String?
        var firstChapInitialized = false
        do {
            line = reader.readLine()
            if (line == null) {
                // 处理剩余内容
                chap?.content = stringBuilder.toString()
                stringBuilder.clear()
                break
            }
            // 跳过空白行
            if (line.isBlank())
                continue
            // 开始解析内容
            if (isTitle(line)) {
                // 在第一个标题出现之前，可能会出现部分没有章节标题所属的行，将这些作为一个无标题章节
                if (stringBuilder.isNotEmpty()) {
                    if (!firstChapInitialized) {
                        chap = ChapData(chapIndex).apply {
                            content = stringBuilder.toString()
                            stringBuilder.delete(0, stringBuilder.length)
                        }
                        bookData.addChapter(chap)
                        chapIndex++
                    } else {
                        chap!!.content = stringBuilder.toString()
                        stringBuilder.delete(0, stringBuilder.length)
                    }
                }
                if (!firstChapInitialized) firstChapInitialized = true
                chap = ChapData(chapIndex, line)
                bookData.addChapter(chap)
                chapIndex++
            } else {
                stringBuilder.append(line).append('\n')
            }
        } while (true)
        return bookData
    }

    /**
     * 在loadBook()函数中，章节内容也已经完成了加载，所以无需再做处理
     */
    override fun loadChapter(chapData: ChapData) = Unit
}