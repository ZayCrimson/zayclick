package com.zaystudio.zayclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 16, 28)
        window.navigationBarColor = Color.rgb(12, 16, 28)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(12, 16, 28))
        }
        page.addView(TextView(this).apply {
            text = "ZAY CLICKER"
            textSize = 28f; setTextColor(Color.WHITE)
        })
        page.addView(TextView(this).apply {
            text = "Root uinput multitouch • CLICK + HOLD simultan"
            textSize = 14f; setTextColor(0xFFB8C7DF.toInt()); setPadding(0, 8, 0, 24)
        })
        status = TextView(this).apply {
            textSize = 13f; setTextColor(0xFFB8C7DF.toInt()); setPadding(0, 8, 0, 16)
        }
        page.addView(status)
        addButton(page, "1. IZINKAN OVERLAY") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else Toast.makeText(this, "Izin overlay sudah aktif", Toast.LENGTH_SHORT).show()
        }
        addButton(page, "2. CEK AKSES ROOT") { checkRoot() }
        addButton(page, "3. JALANKAN FLOATING CONTROL") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Aktifkan izin overlay dulu", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, OverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                Toast.makeText(this, "Overlay dimulai. Geser marker CLICK/HOLD.", Toast.LENGTH_LONG).show()
            }
        }
        addButton(page, "HENTIKAN OVERLAY") { stopService(Intent(this, OverlayService::class.java)) }
        page.addView(TextView(this).apply {
            text = "Cara pakai: geser marker biru CLICK dan hijau HOLD ke posisi target. START CLICK dan START HOLD bisa aktif bersamaan. STOP ALL melepas semua sentuhan virtual. Tidak perlu memasukkan angka koordinat."
            textSize = 13f; setTextColor(0xFF9EABC2.toInt()); setPadding(0, 18, 0, 0)
        })
        setContentView(page)
    }

    private fun addButton(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title; isAllCaps = false; setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })
    }

    private fun checkRoot() {
        status.text = "Memeriksa root..."
        Thread {
            val result = try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val output = process.inputStream.bufferedReader().readText().trim()
                val code = process.waitFor()
                if (code == 0 && output.contains("uid=0")) "Root: AKTIF ($output)" else "Root gagal: ${output.ifBlank { "izin ditolak" }}"
            } catch (e: Exception) { "Root error: ${e.message}" }
            runOnUiThread { status.text = result }
        }.start()
    }
}
