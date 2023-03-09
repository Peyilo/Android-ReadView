package org.klee.readview.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.klee.readview.R
import org.klee.readview.entities.BookData
import org.klee.readview.loader.SfacgLoader
import org.klee.readview.widget.ReadView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val readView = findViewById<ReadView>(R.id.read_view)
        // 配置ReadPage
        readView.initPage { readPage, _ ->
            readPage.initLayout(R.layout.item_view_page, R.id.page_content)
        }
        readView.callback = object : ReadView.Callback {
            override fun onTocInitialized(book: BookData) {
                readView.post {
                    Toast.makeText(applicationContext, "章节目录初始化完成！", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onInitialized(book: BookData) {
                Toast.makeText(applicationContext, "视图初始化完成！", Toast.LENGTH_SHORT).show()
            }
        }
        readView.openBook(SfacgLoader(591785), 10, 2)
    }
}