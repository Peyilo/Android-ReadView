package org.klee.readview

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val `is` = contentResolver.openInputStream(it)
        val file = File(externalCacheDir, "cache.txt")
        synchronized(File::class.java) {
            if (!file.exists()) {
                file.createNewFile()
            }
            val reader = BufferedReader(InputStreamReader(`is`))
            val writer = BufferedWriter(FileWriter(file))
            var line: String?
            while(true) {
                line = reader.readLine()
                if (line == null) break
                writer.write(line)
                writer.newLine()
            }
            reader.close()
            writer.close()
        }
        startActivity(
            Intent(this@MainActivity, ReadActivity::class.java).apply {
                putExtra("MODE", 2)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        findViewById<Button>(R.id.internet_load).setOnClickListener {
            startActivity(
                Intent(this@MainActivity, ReadActivity::class.java).apply {
                    putExtra("MODE", 1)
                }
            )
        }
        findViewById<Button>(R.id.native_load).setOnClickListener {
            fileLauncher.launch("*/*")
        }
        findViewById<Button>(R.id.show_text).setOnClickListener {
            startActivity(
                Intent(this@MainActivity, ReadActivity::class.java).apply {
                    putExtra("MODE", 3)
                }
            )
        }
    }

}