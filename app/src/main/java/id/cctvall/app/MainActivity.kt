package id.cctvall.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

data class CameraEntry(val name: String, val url: String)

class MainActivity : Activity() {
    private val cameras = mutableListOf<CameraEntry>()
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xFF101820.toInt())
        }
        val title = TextView(this).apply {
            text = "CCTV ALL-IN-ONE"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 54))
        val subtitle = TextView(this).apply {
            text = "Dashboard kamera • RTSP / ONVIF"
            textSize = 14f
            setTextColor(0xFFB8C7D1.toInt())
        }
        root.addView(subtitle)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val add = Button(this).apply { text = "+ Tambah kamera"; setOnClickListener { showAddCamera() } }
        val discover = Button(this).apply { text = "Cari ONVIF"; setOnClickListener {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Pencarian ONVIF")
                .setMessage("Pencarian otomatis jaringan lokal akan ditambahkan pada tahap berikutnya. Untuk sementara, tambahkan alamat RTSP kamera secara manual.")
                .setPositiveButton("Mengerti", null).show()
        } }
        actions.addView(add, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(discover, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)
        empty = TextView(this).apply {
            text = "Belum ada kamera. Tekan “Tambah kamera” untuk memasukkan URL RTSP."
            textSize = 16f
            setTextColor(0xFFE0E6EA.toInt())
            gravity = Gravity.CENTER
            setPadding(12, 35, 12, 35)
        }
        root.addView(empty)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadCameras()
    }

    private fun showAddCamera() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 8, 20, 0)
        }
        val name = EditText(this).apply { hint = "Nama kamera (mis. Teras)" }
        val url = EditText(this).apply {
            hint = "rtsp://alamat-kamera:554/stream"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        panel.addView(name)
        panel.addView(url)
        AlertDialog.Builder(this).setTitle("Tambah kamera RTSP").setView(panel)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val n = name.text.toString().trim().ifEmpty { "Kamera ${cameras.size + 1}" }
                val u = url.text.toString().trim()
                if (!u.startsWith("rtsp://") && !u.startsWith("rtsps://")) {
                    Toast.makeText(this, "URL harus diawali rtsp:// atau rtsps://", Toast.LENGTH_LONG).show()
                } else {
                    cameras.add(CameraEntry(n, u))
                    saveCameras()
                    renderCameras()
                }
            }.show()
    }

    private fun renderCameras() {
        list.removeAllViews()
        empty.visibility = if (cameras.isEmpty()) View.VISIBLE else View.GONE
        cameras.forEachIndexed { index, camera ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 4, 8, 4)
            }
            val label = TextView(this).apply {
                text = camera.name + "\n" + camera.url
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
            }
            row.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = "Buka"
                setOnClickListener { playCamera(camera) }
            })
            row.addView(Button(this).apply {
                text = "Hapus"
                setOnClickListener {
                    cameras.removeAt(index)
                    saveCameras()
                    renderCameras()
                }
            })
            list.addView(row, ViewGroup.LayoutParams(-1, -2))
            val line = View(this).apply { setBackgroundColor(0xFF34434D.toInt()) }
            list.addView(line, LinearLayout.LayoutParams(-1, 1))
        }
    }

    private fun playCamera(camera: CameraEntry) {
        val view = PlayerView(this)
        val p = ExoPlayer.Builder(this).build()
        player?.release()
        player = p
        view.player = p
        p.setMediaItem(MediaItem.fromUri(camera.url))
        p.prepare()
        p.playWhenReady = true
        AlertDialog.Builder(this).setTitle(camera.name).setView(view)
            .setPositiveButton("Tutup") { _, _ -> p.pause(); p.release(); if (player === p) player = null }
            .setOnCancelListener { p.pause(); p.release(); if (player === p) player = null }
            .show().window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.72).toInt())
    }

    private fun saveCameras() {
        val encoded = cameras.joinToString("\n") { android.util.Base64.encodeToString((it.name + "\u0000" + it.url).toByteArray(), android.util.Base64.NO_WRAP) }
        getSharedPreferences("camera_list", MODE_PRIVATE).edit().putString("items", encoded).apply()
    }

    private fun loadCameras() {
        val encoded = getSharedPreferences("camera_list", MODE_PRIVATE).getString("items", "") ?: ""
        encoded.lines().filter { it.isNotBlank() }.forEach {
            runCatching {
                val decoded = String(android.util.Base64.decode(it, android.util.Base64.NO_WRAP))
                val parts = decoded.split("\u0000", limit = 2)
                if (parts.size == 2) cameras.add(CameraEntry(parts[0], parts[1]))
            }
        }
        renderCameras()
    }

    override fun onStop() {
        player?.release()
        player = null
        super.onStop()
    }
}
