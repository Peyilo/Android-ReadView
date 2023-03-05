package org.klee.readview.activity

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.klee.readview.R
import org.klee.readview.ReadView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val readView = findViewById<ReadView>(R.id.read_view)
        readView.initPage { readPage, position ->
            readPage.initLayout(R.layout.item_view_page, R.id.page_content)
            when (position) {
                 0  -> readPage.content.setBackgroundColor(Color.RED)
                 1  -> readPage.content.setBackgroundColor(Color.BLUE)
                -1  -> readPage.content.setBackgroundColor(Color.GREEN)
            }
        }
    }
}