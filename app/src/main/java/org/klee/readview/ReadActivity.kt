package org.klee.readview

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.ChapterStatus
import org.klee.readview.widget.FlipMode
import org.klee.readview.widget.PageView
import org.klee.readview.widget.ReadView
import org.klee.readview.widget.api.ReadViewCallback
import java.io.File

private const val TAG = "MainActivity"
class ReadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        supportActionBar?.hide()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        val readView = findViewById<ReadView>(R.id.read_view)
        // 配置ReadPage
        readView.initPage { readPage, _ ->
            readPage.initLayout(layoutId = R.layout.item_view_page,
                contentId = R.id.page_content,
                headerId = R.id.page_header,
                footerId = R.id.page_footer
            )
        }
        // 绑定视图
        readView.setCallback(object : ReadViewCallback {
            override fun onUpdatePage(convertView: PageView, newChap: ChapData, newPageIndex: Int) {
                val header = convertView.header!! as TextView
                header.text = newChap.title
                val process = convertView.footer!!.findViewById(R.id.page_footer_process) as TextView
                process.text = if (newChap.status == ChapterStatus.FINISHED) {
                    "${newPageIndex}/${newChap.pageCount}"
                } else {
                    "loading"
                }
            }
        })
        readView.flipMode = FlipMode.Cover     // 设置翻页模式
        readView.setOnClickRegionListener { xPercent, _ ->
            when (xPercent) {
                in 0..30 -> {
                    readView.prevPage()
                }
                in 70..100 -> {
                    readView.nextPage()
                }
                else -> false
            }
        }
        when (intent.getIntExtra("MODE", 1)) {
            1 -> {
                // 打开书籍
                readView.openBook(SfacgLoader(619932))
            }
            2 -> {
                val file = File(externalCacheDir, "cache.txt")
                readView.openBook(file)
            }
            3 -> {
                readView.showText("Hello world!")
            }
        }
    }
}