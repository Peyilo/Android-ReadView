package org.peyilo.readview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class ViewPagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pager)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val pages = listOf(
            PageData("Page 1", "This is content for page 1."),
            PageData("Page 2", "This is content for page 2."),
            PageData("Page 3", "This is content for page 3.")
        )
        val adapter = TextAdapter(pages)

        viewPager.adapter = adapter
    }

}