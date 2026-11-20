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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

data class CameraEntry(val name: String, val url: String, val isCloud: Boolean = false, val hlsUrl: String = "")

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
            .setTitle("Uniarch Care / Uniview Cloud")
            .setMessage("Integrasi live view Uniarch sedang disiapkan melalui EZCloud App SDK resmi. Uniview mencantumkan SDK untuk P2P dan live video, tetapi paket SDK serta kredensial integrasi belum tersedia secara publik di halaman unduhan. Login akun biasa belum otomatis memberi akses SDK. Jangan masukkan kata sandi akun di aplikasi ini sebelum modul resmi tersedia.")
            .setPositiveButton("Buka Cloud Platform") { _, _ ->
                openOfficialPage("https://cloud.uniview.com/mainPage/homePage")
            }
            .setNegativeButton("Unduh Uniarch Care") { _, _ ->
                openOfficialPage("https://www.uniarch.cn/Support/Software/app/")
            }
            .setNeutralButton("Tutup", null)
            .show()
    }

    private fun showHikConnectInfo() {
        AlertDialog.Builder(this)
            .setTitle("Hik-Connect")
            .setMessage("Login melalui halaman resmi Hik-Connect yang Anda gunakan di browser. Halaman ini memakai akun Hik-Connect, bukan akun portal Hikvision/TPP. Catatan: login web hanya membuka portal; belum otomatis memasukkan video ke daftar kamera aplikasi.")
            .setPositiveButton("Login Web Hik-Connect") { _, _ ->
                startActivity(Intent(this, HikConnectWebActivity::class.java))
            }
            .setNegativeButton("Cloud Bridge") { _, _ ->
                showHikBridgeDialog()
            }
            .setNeutralButton("Tutup", null)
            .show()
    }

    private fun showHikBridgeDialog() {
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
                        val hls = item.optString("hls_url", "")
                        if (rtsp.startsWith("rtsp://") || rtsp.startsWith("rtsps://")) {
                            loaded.add(CameraEntry(name, rtsp, true, hls))
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
                val detail = if (camera.isCloud) "Tekan Buka untuk melihat video langsung" else camera.url
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
        if (!camera.url.startsWith("rtsp://", ignoreCase = true) &&
            !camera.url.startsWith("rtsps://", ignoreCase = true)) {
            Toast.makeText(this, "Alamat video kamera tidak tersedia.", Toast.LENGTH_LONG).show()
            return
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 4, 8, 8)
            setBackgroundColor(0xFF101820.toInt())
        }
        val status = TextView(this).apply {
            text = "Menghubungkan ke kamera..."
            textSize = 13f
            setTextColor(0xFFE0E6EA.toInt())
            setPadding(8, 8, 8, 8)
        }
        val view = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            keepScreenOn = true
        }
        panel.addView(status, LinearLayout.LayoutParams(-1, -2))
        panel.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))

        val playbackButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val retry = Button(this).apply {
            text = "Coba RTSP Lagi"
            isAllCaps = false
        }
        val hlsFallback = Button(this).apply {
            text = "Coba HLS"
            isAllCaps = false
            visibility = if (camera.hlsUrl.startsWith("http://") || camera.hlsUrl.startsWith("https://")) View.VISIBLE else View.GONE
        }
        playbackButtons.addView(retry, LinearLayout.LayoutParams(0, -2, 1f))
        playbackButtons.addView(hlsFallback, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(playbackButtons, LinearLayout.LayoutParams(-1, -2))

        player?.release()
        val p = ExoPlayer.Builder(this).build()
        player = p
        view.player = p

        val mediaItem = MediaItem.fromUri(Uri.parse(camera.url))
        val rtspFactory = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(25_000)

        fun startStream(useHls: Boolean = false) {
            p.stop()
            if (useHls) {
                status.text = "Mencoba HLS... video pertama bisa memerlukan beberapa detik."
                p.setMediaItem(MediaItem.fromUri(Uri.parse(camera.hlsUrl)))
            } else {
                status.text = "Menghubungkan ke Hik-Connect melalui RTSP..."
                p.setMediaSource(rtspFactory.createMediaSource(mediaItem))
            }
            p.prepare()
            p.playWhenReady = true
        }

        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                status.text = when (playbackState) {
                    Player.STATE_BUFFERING -> "Mengambil video dari cloud..."
                    Player.STATE_READY -> "LIVE — video kamera tersambung"
                    Player.STATE_ENDED -> "Stream berakhir. Tekan Coba Sambungkan Lagi."
                    else -> "Menunggu koneksi kamera..."
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                status.text = "Tidak dapat memutar video (${error.errorCodeName}). Periksa koneksi bridge, port RTSP 8554, dan dukungan codec kamera."
                Toast.makeText(
                    this@MainActivity,
                    "Video gagal dibuka: " + error.errorCodeName,
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        retry.setOnClickListener { startStream(false) }
        hlsFallback.setOnClickListener { startStream(true) }

        val dialog = AlertDialog.Builder(this)
            .setTitle(camera.name)
            .setView(panel)
            .setNegativeButton("Tutup", null)
            .create()
        dialog.setOnDismissListener {
            if (player === p) player = null
            p.release()
        }
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.86).toInt()
            )
        }
        dialog.show()
        startStream()
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