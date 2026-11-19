package com.zaystudio.zayclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.*

class MainActivity : Activity() {
    private lateinit var xClick: EditText
    private lateinit var yClick: EditText
    private lateinit var xHold: EditText
    private lateinit var yHold: EditText
    private lateinit var interval: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 16, 28)
        window.navigationBarColor = Color.rgb(12, 16, 28)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 24)
            setBackgroundColor(Color.rgb(12, 16, 28))
        }
        fun label(text: String, size: Float = 14f) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.rgb(225, 232, 248))
            setPadding(0, 12, 0, 6)
        }
        fun field(hint: String, value: String): EditText = EditText(this).apply {
            this.hint = hint
            setSingleLine(true)
            setText(value)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(14, 8, 14, 8)
            setBackgroundColor(Color.rgb(29, 37, 57))
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }

        page.addView(label("ZAY CLICKER", 26f))
        page.addView(label("Atur koordinat layar dan interval. Aktifkan layanan Accessibility terlebih dahulu."))
        page.addView(button("BUKA PENGATURAN ACCESSIBILITY") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        status = label("Status: memeriksa layanan…")
        page.addView(status)

        page.addView(label("SPAM CLICK"))
        xClick = field("X click", "500"); yClick = field("Y click", "800")
        page.addView(row(xClick, yClick))
        interval = field("Interval (ms)", "100")
        page.addView(interval)
        page.addView(button("MULAI SPAM CLICK") { startClick() })
        page.addView(button("STOP SPAM CLICK") { send(Action.STOP_CLICK) })

        page.addView(label("HOLD"))
        xHold = field("X hold", "700"); yHold = field("Y hold", "800")
        page.addView(row(xHold, yHold))
        page.addView(button("MULAI HOLD") { startHold() })
        page.addView(button("STOP HOLD") { send(Action.STOP_HOLD) })

        page.addView(button("STOP SEMUA") { send(Action.STOP_ALL) })
        page.addView(label(
            "Catatan: versi awal ini memakai AccessibilityService. Android dapat membatalkan gesture yang sedang berjalan saat gesture baru dikirim, sehingga CLICK + HOLD simultan belum dijamin. Dukungan root/uinput khusus perangkat belum diimplementasikan.",
            12f
        ))
        val scroll = ScrollView(this).apply { addView(page) }
        setContentView(scroll)
    }

    private fun row(a: View, b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(a, LinearLayout.LayoutParams(0, -2, 1f))
        addView(b, LinearLayout.LayoutParams(0, -2, 1f))
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled()) "Status: Accessibility aktif" else "Status: Accessibility belum aktif"
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(0).any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun startClick() {
        val x = xClick.num(); val y = yClick.num(); val ms = interval.num().coerceAtLeast(40)
        send(Action.START_CLICK, x, y, ms)
    }
    private fun startHold() = send(Action.START_HOLD, xHold.num(), yHold.num(), 0)
    private fun EditText.num() = text.toString().toIntOrNull() ?: 0
    private fun send(action: Action, x: Int = 0, y: Int = 0, interval: Int = 0) {
        ClickAccessibilityService.instance?.handle(action, x, y, interval)
            ?: Toast.makeText(this, "Aktifkan Accessibility Service dahulu", Toast.LENGTH_LONG).show()
    }
}

enum class Action { START_CLICK, STOP_CLICK, START_HOLD, STOP_HOLD, STOP_ALL }
