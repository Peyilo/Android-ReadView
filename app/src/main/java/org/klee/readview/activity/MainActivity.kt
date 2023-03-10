package org.klee.readview.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.klee.readview.R
import org.klee.readview.entities.BookData
import org.klee.readview.entities.ChapData
import org.klee.readview.entities.ChapterStatus
import org.klee.readview.loader.SfacgLoader
import org.klee.readview.widget.PageView
import org.klee.readview.widget.ReadView

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        readView.setCallback(object : ReadView.Callback {
            override fun onTocInitialized(book: BookData) {
                readView.post {
                    Toast.makeText(applicationContext, "章节目录初始化完成！", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onInitialized(book: BookData) {
                Toast.makeText(applicationContext, "分页完成！", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadChap(chap: ChapData, success: Boolean) {
                Log.d(TAG, "onLoadChap: chapter ${chap.chapIndex} " +
                        "load ${if (success) "success" else "fail"}")
            }

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

            override fun onBitmapCreate(bitmap: Bitmap) {
                Log.d(TAG, "onBitmapCreate: size = ${bitmap.byteCount}")
            }
        })
        readView.openBook(SfacgLoader(591785))
    }
}