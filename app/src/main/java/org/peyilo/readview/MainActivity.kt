package org.peyilo.readview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val test1 = findViewById<Button>(R.id.test1)
        test1.setOnClickListener {
            startActivity(Intent(
                this@MainActivity, ReadActivity::class.java
            ).apply { putExtra("MODE", 1) })
        }

        val test2 = findViewById<Button>(R.id.test2)
        test2.setOnClickListener {
            startActivity(Intent(this@MainActivity, ViewPagerActivity::class.java))
        }

    }
}