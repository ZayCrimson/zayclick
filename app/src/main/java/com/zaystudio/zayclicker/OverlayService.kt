package com.zaystudio.zayclicker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var engine: RootTouchEngine
    private val handler = Handler(Looper.getMainLooper())
    private var clickLoop: Runnable? = null
    private var holdDown = false
    private var clickMarker: TextView? = null
    private var holdMarker: TextView? = null
    private var clickParams: WindowManager.LayoutParams? = null
    private var holdParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private val prefs by lazy { getSharedPreferences("targets", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        engine = RootTouchEngine(this)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels.coerceAtLeast(1)
        screenHeight = metrics.heightPixels.coerceAtLeast(1)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izin tampil di atas aplikasi lain belum diberikan", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        createMarkers()
        createPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun overlayType() = if (Build.VERSION.SDK_INT >= 26)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    private fun baseParams(width: Int, height: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        width, height, overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.LEFT; this.x = x; this.y = y }

    private fun createMarkers() {
        val size = dp(76)
        val clickX = prefs.getInt("clickX", screenWidth / 2).coerceIn(0, screenWidth - size)
        val clickY = prefs.getInt("clickY", screenHeight / 2 - dp(60)).coerceIn(0, screenHeight - size)
        val holdX = prefs.getInt("holdX", screenWidth / 2).coerceIn(0, screenWidth - size)
        val holdY = prefs.getInt("holdY", screenHeight / 2 + dp(50)).coerceIn(0, screenHeight - size)
        val clickView = marker("CLICK", Color.rgb(28, 130, 220))
        val holdView = marker("HOLD", Color.rgb(22, 160, 120))
        val clickLayout = baseParams(size, size, clickX, clickY)
        val holdLayout = baseParams(size, size, holdX, holdY)
        clickMarker = clickView; holdMarker = holdView
        clickParams = clickLayout; holdParams = holdLayout
        windowManager.addView(clickView, clickLayout)
        windowManager.addView(holdView, holdLayout)
        attachDrag(clickView, clickLayout, "click")
        attachDrag(holdView, holdLayout, "hold")
    }

    private fun marker(label: String, color: Int) = TextView(this).apply {
        text = label
        textSize = 13f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor((color and 0x00FFFFFF) or 0xCC000000.toInt())
            setStroke(dp(2), color)
        }
        elevation = dp(8).toFloat()
    }

    private fun attachDrag(view: View, params: WindowManager.LayoutParams, key: String) {
        var startX = 0f; var startY = 0f; var initialX = 0; var initialY = 0
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX; startY = event.rawY
                    initialX = params.x; initialY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (initialX + event.rawX - startX).toInt().coerceIn(0, screenWidth - params.width)
                    params.y = (initialY + event.rawY - startY).toInt().coerceIn(0, screenHeight - params.height)
                    windowManager.updateViewLayout(view, params)
                    sendMoveFor(key, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    prefs.edit().putInt("${key}X", params.x).putInt("${key}Y", params.y).apply()
                    true
                }
                else -> false
            }
        }
    }

    private fun sendMoveFor(key: String, params: WindowManager.LayoutParams) {
        if (key == "hold" && holdDown && engineReady) {
            engine.send("MOVE 0 ${centerX(params)} ${centerY(params)}")
        }
    }

    private fun createPanel() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(0xEE101827.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), 0xFF49617F.toInt())
            }
        }
        box.addView(TextView(this).apply {
            text = "ZAY CLICKER  •  MULTITOUCH"
            textSize = 12f; setTextColor(Color.WHITE); setPadding(dp(4), dp(2), dp(4), dp(6))
        })
        addButton(box, "START CLICK") { startClick() }
        addButton(box, "STOP CLICK") { stopClick() }
        addButton(box, "START HOLD") { startHold() }
        addButton(box, "STOP HOLD") { stopHold() }
        addButton(box, "STOP ALL") { stopAll() }
        val intervalRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        addSmallButton(intervalRow, "−") { changeInterval(-20) }
        intervalRow.addView(TextView(this).apply {
            text = "${prefs.getInt("interval", 100)} ms"
            tag = "interval_label"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(40), 1f))
        addSmallButton(intervalRow, "+") { changeInterval(20) }
        box.addView(intervalRow)
        val params = baseParams(dp(158), WindowManager.LayoutParams.WRAP_CONTENT, dp(8), dp(90))
        panel = box; panelParams = params
        windowManager.addView(box, params)
    }

    private fun addButton(parent: LinearLayout, title: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = title; textSize = 10f; isAllCaps = false
            setPadding(dp(4), 0, dp(4), 0)
            minHeight = dp(38); minimumHeight = dp(38)
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, dp(40)))
    }

    private fun addSmallButton(parent: LinearLayout, title: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = title; textSize = 12f; isAllCaps = false
            minHeight = dp(38); minimumHeight = dp(38)
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(dp(42), dp(40)))
    }

    private fun changeInterval(delta: Int) {
        val value = (prefs.getInt("interval", 100) + delta).coerceIn(40, 5000)
        prefs.edit().putInt("interval", value).apply()
        val label = (panel as? LinearLayout)?.findViewWithTag<TextView>("interval_label")
        label?.text = "$value ms"
    }

    private fun ensureEngine(): Boolean {
        if (engineReady) return true
        val ok = engine.start(screenWidth, screenHeight)
        engineReady = ok
        if (!ok) Toast.makeText(this, "Root/uinput gagal: ${engine.lastError()}", Toast.LENGTH_LONG).show()
        return ok
    }
    private var engineReady = false

    private fun startClick() {
        if (!ensureEngine()) return
        stopClick()
        val task = object : Runnable {
            override fun run() {
                val p = clickParams ?: return
                engine.send("CLICK 1 ${centerX(p)} ${centerY(p)}")
                clickLoop = this
                val interval = prefs.getInt("interval", 100).coerceIn(40, 5000)
                handler.postDelayed(this, interval.toLong())
            }
        }
        clickLoop = task
        handler.post(task)
        Toast.makeText(this, "Spam click aktif", Toast.LENGTH_SHORT).show()
    }

    private fun stopClick() {
        clickLoop?.let(handler::removeCallbacks)
        clickLoop = null
        // Slot 1 is reserved for CLICK; stopping it cannot release HOLD slot 0.
        if (engineReady) engine.send("UP 1")
    }

    private fun startHold() {
        if (!ensureEngine()) return
        if (holdDown) return
        val p = holdParams ?: return
        holdDown = engine.send("DOWN 0 ${centerX(p)} ${centerY(p)}")
        Toast.makeText(this, if (holdDown) "Hold aktif" else "Gagal memulai hold", Toast.LENGTH_SHORT).show()
    }

    private fun stopHold() {
        if (engineReady) engine.send("UP 0")
        holdDown = false
    }

    private fun stopAll() {
        stopClick(); stopHold()
        if (engineReady) engine.send("STOP")
        Toast.makeText(this, "Semua sentuhan dihentikan", Toast.LENGTH_SHORT).show()
    }

    private fun centerX(p: WindowManager.LayoutParams) = (p.x + p.width / 2).coerceIn(0, screenWidth - 1)
    private fun centerY(p: WindowManager.LayoutParams) = (p.y + p.height / 2).coerceIn(0, screenHeight - 1)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        stopAll()
        engine.stop()
        engineReady = false
        listOfNotNull(clickMarker, holdMarker, panel).forEach {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        clickMarker = null; holdMarker = null; panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "ZAY CLICKER", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        return if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ZAY CLICKER aktif")
            .setContentText("Marker CLICK dan HOLD tersedia di atas aplikasi")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pending).setOngoing(true).build()
        else Notification.Builder(this)
            .setContentTitle("ZAY CLICKER aktif").setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pending).setOngoing(true).build()
    }

    companion object {
        const val ACTION_STOP = "com.zaystudio.zayclicker.STOP"
        private const val CHANNEL_ID = "zay_clicker_overlay"
        private const val NOTIFICATION_ID = 7001
    }
}
