package com.zaystudio.zayclicker

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Keeps one root-owned uinput touchscreen alive so separate slots can remain down together. */
class RootTouchEngine(private val context: Context) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    @Volatile private var startupLine: String? = null
    private var ready = false

    @Synchronized
    fun start(width: Int, height: Int): Boolean {
        if (ready && process?.isAlive == true) return true
        stop()
        val binary = File(context.filesDir, "zaytouch")
        try {
            context.assets.open("zaytouch").use { input -> binary.outputStream().use { output -> input.copyTo(output) } }
            val latch = CountDownLatch(1)
            val sourcePath = binary.absolutePath
            val command = "cp \"$sourcePath\" /data/local/tmp/zaytouch || { echo COPY_ERR; exit 1; }; chmod 755 /data/local/tmp/zaytouch || { echo CHMOD_ERR; exit 1; }; exec /data/local/tmp/zaytouch $width $height || { echo EXEC_ERR; exit 1; }"
            val proc = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            process = proc
            writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            Thread({
                try {
                    proc.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            startupLine = line
                            val lower = line.lowercase()
                            if (line == "READY" || lower.contains("err") || lower.contains("denied") || lower.contains("not found") || lower.contains("invalid")) latch.countDown()
                        }
                    }
                } catch (_: Exception) { }
                if (proc.isAlive) latch.countDown()
            }, "ZayTouch-stdout").apply { isDaemon = true; start() }

            if (!latch.await(10, TimeUnit.SECONDS)) {
                startupLine = "Timeout menunggu uinput"
                stop()
                return false
            }
            if (startupLine != "READY" || !proc.isAlive) {
                val reason = startupLine ?: "Root ditolak atau native engine berhenti"
                stop()
                startupLine = reason
                return false
            }
            ready = true
            return true
        } catch (e: Exception) {
            startupLine = "${e.javaClass.simpleName}: ${e.message}"
            stop()
            return false
        }
    }

    @Synchronized
    fun send(command: String): Boolean {
        return try {
            val out = writer ?: return false
            out.write(command)
            out.newLine()
            out.flush()
            true
        } catch (_: Exception) { false }
    }

    fun lastError(): String = startupLine ?: "Tidak diketahui"

    @Synchronized
    fun stop() {
        try {
            writer?.apply { write("QUIT"); newLine(); flush(); close() }
        } catch (_: Exception) { }
        writer = null
        try { process?.waitFor(700, TimeUnit.MILLISECONDS) } catch (_: Exception) { }
        try { process?.destroy() } catch (_: Exception) { }
        process = null
        ready = false
    }
}
