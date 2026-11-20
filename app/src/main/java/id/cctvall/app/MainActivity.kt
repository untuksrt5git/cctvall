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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

data class CameraEntry(val name: String, val url: String, val isCloud: Boolean = false)

class MainActivity : Activity() {
    private val cameras = mutableListOf<CameraEntry>()
    private val cloudCameras = mutableListOf<CameraEntry>()
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
        val prefs = getSharedPreferences("hik_bridge", MODE_PRIVATE)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 8, 20, 0)
        }
        val bridgeUrl = EditText(this).apply {
            hint = "http://192.168.1.20:8765"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(prefs.getString("url", ""))
        }
        val token = EditText(this).apply {
            hint = "Token bridge (CLOUDCAM_BRIDGE_TOKEN)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        panel.addView(TextView(this).apply {
            text = "Alamat PC/server yang menjalankan Hik-Connect Cloud Bridge:"
            setTextColor(0xFFE0E6EA.toInt())
        })
        panel.addView(bridgeUrl)
        panel.addView(TextView(this).apply {
            text = "Token akses bridge:"
            setTextColor(0xFFE0E6EA.toInt())
        })
        panel.addView(token)

        AlertDialog.Builder(this)
            .setTitle("Sambungkan Hik-Connect")
            .setView(panel)
            .setNegativeButton("Batal", null)
            .setNeutralButton("Panduan bridge") { _, _ ->
                openOfficialPage("https://github.com/untuksrt5git/cctvall/tree/main/hikconnect-bridge")
            }
            .setPositiveButton("Sambungkan") { _, _ ->
                val address = bridgeUrl.text.toString().trim().trimEnd('/')
                val accessToken = token.text.toString().trim()
                if ((!address.startsWith("http://") && !address.startsWith("https://")) || accessToken.length < 24) {
                    Toast.makeText(this, "Isi alamat bridge dan token yang benar.", Toast.LENGTH_LONG).show()
                } else {
                    connectHikBridge(address, accessToken)
                }
            }
            .show()
    }

    private fun connectHikBridge(address: String, accessToken: String) {
        Toast.makeText(this, "Menghubungkan ke Hik-Connect Cloud Bridge...", Toast.LENGTH_SHORT).show()
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$address/v1/cameras").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 45000
                    setRequestProperty("X-Bridge-Token", accessToken)
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (status !in 200..299) {
                    val reason = runCatching { JSONObject(response).optString("error") }.getOrDefault("HTTP $status")
                    throw IOException("$reason (HTTP $status)")
                }

                val root = JSONObject(response)
                val items = root.optJSONArray("cameras")
                val loaded = mutableListOf<CameraEntry>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val name = item.optString("name", "Kamera Hik-Connect")
                        val rtsp = item.optString("rtsp_url", "")
                        if (rtsp.startsWith("rtsp://") || rtsp.startsWith("rtsps://")) {
                            loaded.add(CameraEntry(name, rtsp, true))
                        }
                    }
                }

                runOnUiThread {
                    cloudCameras.clear()
                    cloudCameras.addAll(loaded)
                    prefsSaveBridgeUrl(address)
                    renderCameras()
                    if (loaded.isEmpty()) {
                        Toast.makeText(this, "Bridge tersambung, tetapi belum ada kamera yang dikembalikan akun. Periksa dukungan perangkat share dan kode verifikasi.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Berhasil memuat " + loaded.size + " kamera Hik-Connect.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Gagal menyambungkan: " + (error.message ?: "periksa alamat bridge dan token"), Toast.LENGTH_LONG).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun prefsSaveBridgeUrl(address: String) {
        getSharedPreferences("hik_bridge", MODE_PRIVATE).edit().putString("url", address).apply()
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
        empty.visibility = if (cameras.isEmpty() && cloudCameras.isEmpty()) View.VISIBLE else View.GONE
        val allCameras = cameras + cloudCameras
        allCameras.forEach { camera ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 4, 8, 4)
            }
            val label = TextView(this).apply {
                val source = if (camera.isCloud) "HIK-CONNECT" else "RTSP / LOKAL"
                val detail = if (camera.isCloud) "Stream cloud tersambung melalui bridge" else camera.url
                text = "[$source] " + camera.name + "\n" + detail
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
            }
            row.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = "Buka"
                setOnClickListener { playCamera(camera) }
            })
            row.addView(Button(this).apply {
                text = if (camera.isCloud) "Lepas" else "Hapus"
                setOnClickListener {
                    if (camera.isCloud) {
                        cloudCameras.remove(camera)
                    } else {
                        cameras.remove(camera)
                        saveCameras()
                    }
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