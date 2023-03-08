package org.klee.readview.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.klee.readview.R
import org.klee.readview.loader.SfacgLoader
import org.klee.readview.widget.ReadView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val readView = findViewById<ReadView>(R.id.read_view)
        readView.initPage { readPage, _ ->
            readPage.initLayout(R.layout.item_view_page, R.id.page_content)
        }
        readView.openBook(SfacgLoader(591785))
    }
}