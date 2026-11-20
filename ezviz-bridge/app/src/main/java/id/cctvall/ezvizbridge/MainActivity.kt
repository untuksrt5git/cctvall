package id.cctvall.ezvizbridge

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.videogo.openapi.EZGlobalSDK
import com.videogo.openapi.EZOpenSDK
import com.videogo.openapi.EZPlayer
import com.videogo.openapi.bean.EZDeviceInfo
import java.util.concurrent.Executors

private data class EzvizRegion(val label: String, val api: String, val auth: String)

class MainActivity : Activity(), SurfaceHolder.Callback {
    private val regions = listOf(
        EzvizRegion("Asia / Singapore (default Indonesia)", "https://isgpopen.ezvizlife.com", "https://isgpopenauth.ezvizlife.com"),
        EzvizRegion("Europe", "https://ieuopen.ezvizlife.com", "https://ieuopenauth.ezvizlife.com"),
        EzvizRegion("North America", "https://iusopen.ezvizlife.com", "https://iusopenauth.ezvizlife.com"),
        EzvizRegion("South America", "https://isaopen.ezvizlife.com", "https://isaopenauth.ezvizlife.com")
    )
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var appKeyInput: EditText
    private lateinit var regionSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout
    private var loginReceiver: BroadcastReceiver? = null
    private var player: EZPlayer? = null
    private var previewStatus: TextView? = null
    private var pendingSerial = ""
    private var pendingCameraNo = 1
    private var playerStarted = false
    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        val prefs = getSharedPreferences("ezviz_bridge", MODE_PRIVATE)
        appKeyInput.setText(prefs.getString("app_key", ""))
        regionSpinner.setSelection(prefs.getInt("region", 0).coerceIn(regions.indices))
    }

    private fun buildScreen() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xFF101820.toInt())
        }
        content.addView(TextView(this).apply {
            text = "EZVIZ CLOUD BRIDGE — TEST"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Uji login SDK resmi, daftar kamera milik sendiri dan kamera yang dibagikan, lalu coba live view."
            textSize = 13f
            setTextColor(0xFFCAD5DC.toInt())
            setPadding(0, 6, 0, 12)
        })
        appKeyInput = EditText(this).apply {
            hint = "EZVIZ Open Platform AppKey"
            singleLine = true
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9AAAB5.toInt())
        }
        content.addView(appKeyInput, LinearLayout.LayoutParams(-1, -2))
        regionSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                regions.map { it.label }
            )
            setSelection(0)
        }
        content.addView(regionSpinner, LinearLayout.LayoutParams(-1, -2))
        content.addView(actionButton("Login resmi EZVIZ") { startEzvizLogin() })
        content.addView(actionButton("Muat ulang daftar kamera") { loadDeviceLists() })
        status = TextView(this).apply {
            text = "Masukkan AppKey dari EZVIZ Open Platform. Jangan gunakan AppID/SecretKey milik Uniview."
            textSize = 13f
            setTextColor(0xFFFFD27A.toInt())
            setPadding(0, 12, 0, 12)
        }
        content.addView(status)
        val scroll = ScrollView(this)
        deviceList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(deviceList)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(content)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun startEzvizLogin() {
        val appKey = appKeyInput.text.toString().trim()
        if (appKey.isBlank()) {
            Toast.makeText(this, "AppKey EZVIZ belum diisi.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            getSharedPreferences("ezviz_bridge", MODE_PRIVATE).edit()
                .putString("app_key", appKey)
                .putInt("region", regionSpinner.selectedItemPosition)
                .apply()
            configureSdk(appKey)
            registerLoginReceiver()
            status.text = "Membuka halaman login resmi EZVIZ..."
            EZOpenSDK.getInstance().openLoginPage()
        } catch (error: Exception) {
            status.text = "Gagal membuka login SDK: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun configureSdk(appKey: String) {
        val region = regions[regionSpinner.selectedItemPosition]
        if (!sdkReady) {
            EZGlobalSDK.showSDKLog(false)
            EZGlobalSDK.enableP2P(true)
            EZGlobalSDK.initLib(application, appKey)
            sdkReady = true
        }
        EZOpenSDK.getInstance().setServerUrl(region.api, region.auth)
    }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    private fun registerLoginReceiver() {
        if (loginReceiver != null) return
        loginReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                status.text = "Login EZVIZ berhasil. Mengambil kamera milik sendiri dan kamera yang dibagikan..."
                loadDeviceLists()
            }
        }
        val filter = IntentFilter(ACTION_OAUTH_SUCCESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(loginReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(loginReceiver, filter)
        }
    }

    private fun loadDeviceLists() {
        if (!sdkReady) {
            val key = appKeyInput.text.toString().trim()
            if (key.isBlank()) {
                status.text = "Masukkan AppKey lalu login terlebih dahulu."
                return
            }
            try {
                configureSdk(key)
            } catch (error: Exception) {
                status.text = "Inisialisasi SDK gagal: " + (error.message ?: error.javaClass.simpleName)
                return
            }
        }
        status.text = "Mengambil daftar perangkat dari EZVIZ Cloud..."
        executor.execute {
            val sdk = EZOpenSDK.getInstance()
            val ownedResult = runCatching { sdk.getDeviceList(0, 50) }
            val sharedResult = runCatching { sdk.getSharedDeviceList(0, 50) }
            val owned = ownedResult.getOrNull() ?: emptyList()
            val shared = sharedResult.getOrNull() ?: emptyList()
            val errors = listOfNotNull(
                ownedResult.exceptionOrNull()?.let { "Daftar milik akun: ${it.message ?: it.javaClass.simpleName}" },
                sharedResult.exceptionOrNull()?.let { "Daftar perangkat dibagikan: ${it.message ?: it.javaClass.simpleName}" }
            )
            runOnUiThread {
                deviceList.removeAllViews()
                addSection("PERANGKAT MILIK AKUN", owned, false)
                addSection("PERANGKAT YANG DIBAGIKAN KEPADA AKUN", shared, true)
                status.text = "Selesai: ${owned.size} perangkat milik akun, ${shared.size} perangkat dibagikan." +
                    if (errors.isEmpty()) "" else "\n" + errors.joinToString("\n")
                if (owned.isEmpty() && shared.isEmpty() && errors.isEmpty()) {
                    status.text = "Login terdeteksi, tetapi tidak ada perangkat yang dikembalikan. Periksa wilayah akun dan izin aplikasi."
                }
            }
        }
    }

    private fun addSection(title: String, items: List<EZDeviceInfo>, shared: Boolean) {
        deviceList.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(0xFF76D7C4.toInt())
            setPadding(0, 14, 0, 6)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        if (items.isEmpty()) {
            deviceList.addView(TextView(this).apply {
                text = "Belum ada perangkat."
                setTextColor(0xFFCAD5DC.toInt())
                setPadding(4, 4, 4, 8)
            })
            return
        }
        items.forEach { device ->
            val name = device.deviceName?.takeIf { it.isNotBlank() } ?: "EZVIZ ${device.deviceSerial}"
            val channels = device.cameraInfoList?.size ?: device.cameraNum
            val button = Button(this).apply {
                text = "$name\n${device.deviceSerial} · $channels kanal" +
                    if (shared) "\nDibagikan kepada akun ini" else ""
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { chooseChannel(device) }
            }
            deviceList.addView(button, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun chooseChannel(device: EZDeviceInfo) {
        val channels = device.cameraInfoList ?: emptyList()
        if (channels.isEmpty()) {
            status.text = "Informasi kanal belum tersedia untuk ${device.deviceSerial}."
            return
        }
        val labels = channels.map { "Kanal ${it.cameraNo}" }
        AlertDialog.Builder(this)
            .setTitle(device.deviceName ?: device.deviceSerial)
            .setItems(labels.toTypedArray()) { _, which ->
                startPreview(device.deviceSerial, channels[which].cameraNo)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun startPreview(serial: String, cameraNo: Int) {
        pendingSerial = serial
        pendingCameraNo = cameraNo
        playerStarted = false
        stopPreview()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(0xFF000000.toInt())
        }
        previewStatus = TextView(this).apply {
            text = "Menunggu permukaan video..."
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(6, 8, 6, 8)
        }
        val surface = SurfaceView(this)
        surface.holder.addCallback(this)
        layout.addView(previewStatus)
        layout.addView(surface, LinearLayout.LayoutParams(-1, 0, 1f))
        val dialog = AlertDialog.Builder(this)
            .setTitle("EZVIZ Live View — $serial / CH$cameraNo")
            .setView(layout)
            .setNegativeButton("Tutup", null)
            .create()
        dialog.setOnDismissListener { stopPreview() }
        dialog.show()
        status.text = "Mencoba live view SDK pada $serial kanal $cameraNo."
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (pendingSerial.isBlank() || playerStarted) return
        playerStarted = true
        try {
            val current = EZOpenSDK.getInstance().createPlayer(pendingSerial, pendingCameraNo)
            player = current
            current.setSurfaceHold(holder)
            current.setHandler(Handler(Looper.getMainLooper()) { message ->
                when (message.what) {
                    102 -> {
                        status.text = "EZVIZ LIVE berhasil: $pendingSerial / CH$pendingCameraNo"
                        previewStatus?.text = "LIVE — video tersambung"
                    }
                    103 -> {
                        status.text = "Live view gagal. Callback SDK: ${message.what}. Periksa izin perangkat dan kode verifikasi video."
                        previewStatus?.text = "Gagal memulai live view. Lihat status di halaman sebelumnya."
                    }
                }
                true
            })
            current.startRealPlay()
        } catch (error: Exception) {
            status.text = "Tidak dapat memulai live view: " + (error.message ?: error.javaClass.simpleName)
            previewStatus?.text = status.text
            playerStarted = false
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopPreview()
    }

    private fun stopPreview() {
        playerStarted = false
        player?.let {
            runCatching { it.stopRealPlay() }
            runCatching { it.release() }
        }
        player = null
    }

    override fun onStop() {
        stopPreview()
        super.onStop()
    }

    override fun onDestroy() {
        loginReceiver?.let { runCatching { unregisterReceiver(it) } }
        loginReceiver = null
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_OAUTH_SUCCESS = "com.videogo.action.OAUTH_SUCCESS_ACTION"
    }
}
