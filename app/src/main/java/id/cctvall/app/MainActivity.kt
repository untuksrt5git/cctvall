package id.cctvall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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
            setPadding(16, 14, 16, 12)
            setBackgroundColor(0xFF101820.toInt())
        }

        // Four entry points: manual RTSP, local ONVIF, and vendor cloud integrations.
        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val secondRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        firstRow.addView(menuButton("+ Tambah Kamera") { showAddCamera() },
            LinearLayout.LayoutParams(0, 58, 1f).apply { rightMargin = 5 })
        firstRow.addView(menuButton("ONVIF") { showOnvifInfo() },
            LinearLayout.LayoutParams(0, 58, 1f).apply { leftMargin = 5 })
        secondRow.addView(menuButton("Uniarch Care") { showUniarchInfo() },
            LinearLayout.LayoutParams(0, 58, 1f).apply { rightMargin = 5 })
        secondRow.addView(menuButton("Hik-Connect") { showHikConnectInfo() },
            LinearLayout.LayoutParams(0, 58, 1f).apply { leftMargin = 5 })

        root.addView(firstRow, LinearLayout.LayoutParams(-1, -2))
        root.addView(secondRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })

        empty = TextView(this).apply {
            text = "Belum ada kamera yang ditambahkan."
            textSize = 15f
            setTextColor(0xFFE0E6EA.toInt())
            gravity = Gravity.CENTER
            setPadding(12, 24, 12, 24)
        }
        root.addView(empty, LinearLayout.LayoutParams(-1, -2))

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadCameras()
    }

    private fun menuButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun showOnvifInfo() {
        AlertDialog.Builder(this)
            .setTitle("ONVIF")
            .setMessage("Menu ONVIF digunakan untuk menemukan kamera yang kompatibel di jaringan lokal. Pencarian otomatis dan pengambilan profil/RTSP masih dalam tahap implementasi. Ponsel atau TV Box harus berada pada jaringan yang sama dengan kamera.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showUniarchInfo() {
        AlertDialog.Builder(this)
            .setTitle("Hubungkan Uniarch Care")
            .setMessage("Status: BELUM TERSAMBUNG. Uniview menyediakan EZCloud App SDK untuk streaming P2P dan API untuk pengelolaan perangkat. SDK serta dokumentasinya perlu diperoleh melalui portal pengembang Unisee dan persyaratan integrasinya dipenuhi. Setelah SDK resmi tersedia, login akun, daftar perangkat yang dibagikan, dan live view dapat diintegrasikan di sini. Jangan masukkan kata sandi Uniarch Care di layar ini.")
            .setPositiveButton("Buka Unisee") { _, _ ->
                openOfficialPage("https://global-open.uniview.com/")
            }
            .setNegativeButton("Panduan EZCloud") { _, _ ->
                openOfficialPage("https://global.uniview.com/Topics/EZCloud_Platform/")
            }
            .setNeutralButton("Tutup", null)
            .show()
    }

    private fun showHikConnectInfo() {
        AlertDialog.Builder(this)
            .setTitle("Hubungkan Hik-Connect")
            .setMessage("Status: BELUM TERSAMBUNG. Integrasi resmi memakai Hik-Connect OpenAPI dan API key yang harus diajukan ke Hikvision melalui Technology Partner Portal. Karena akun Anda hanya menerima perangkat yang dibagikan, dukungan daftar dan streaming perangkat share harus dipastikan bersama Hikvision sebelum integrasi diaktifkan. Jangan masukkan kata sandi Hik-Connect di layar ini.")
            .setPositiveButton("Buka Portal TPP") { _, _ ->
                openOfficialPage("https://tpp.hikvision.com/")
            }
            .setNegativeButton("Panduan Hik-Connect API") { _, _ ->
                openOfficialPage("https://tpp.hikvision.com/products/HC-Integration")
            }
            .setNeutralButton("Tutup", null)
            .show()
    }

    private fun openOfficialPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Tidak dapat membuka halaman resmi. Periksa browser perangkat.", Toast.LENGTH_LONG).show()
        }
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
        AlertDialog.Builder(this).setTitle("Tambah Kamera").setView(panel)
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
            list.addView(View(this).apply { setBackgroundColor(0xFF34434D.toInt()) },
                LinearLayout.LayoutParams(-1, 1))
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
            .show().window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.72).toInt())
    }

    private fun saveCameras() {
        val encoded = cameras.joinToString("\n") {
            android.util.Base64.encodeToString((it.name + "\u0000" + it.url).toByteArray(), android.util.Base64.NO_WRAP)
        }
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