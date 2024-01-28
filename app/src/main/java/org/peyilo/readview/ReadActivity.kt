package org.peyilo.readview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import org.peyilo.readview.ui.ReadPage
import org.peyilo.readview.ui.ReadView
import org.peyilo.readview.ui.manager.FlipMode

class ReadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        supportActionBar?.hide()
        val readView = findViewById<ReadView>(R.id.read_view)
        readView.flipMode = FlipMode.Cover
        readView.initPage { page ->
            page.bindLayout(layoutId = R.layout.item_view_page,
                contentId = R.id.page_content,
                headerId = R.id.page_header,
                footerId = R.id.page_footer
            )
            val backgroundColor = when (page.position) {
                ReadPage.Position.CUR -> {
                    (page.header!! as TextView).text = "CUR"
                    Color.parseColor("#EA674C")         // 红色
                }
                ReadPage.Position.NEXT -> {
                    (page.header!! as TextView).text = "NEXT"
                    Color.parseColor("#90B93E")         // 绿色
                }
                ReadPage.Position.PREV -> {
                    (page.header!! as TextView).text = "PREV"
                    Color.parseColor("#4A55B9")         // 蓝色
                }
            }
            (page.content as View).setBackgroundColor(backgroundColor)
        }
        when (intent.getIntExtra("MODE", 1)) {
            1 -> {
                readView.openBook(SfacgLoader(217202))
            }
        }
    }
}