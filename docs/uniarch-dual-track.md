# Uniarch Cloud Bridge — dua jalur integrasi

Target perangkat uji: Uniarch NVR-110E2 yang dibagikan kepada akun Uniarch Care pengguna.
Aplikasi yang dilaporkan pengguna: Uniarch Care 3.10.61 (Build20251205).

## Sasaran

Membuat adapter Uniarch yang bekerja seperti konsep CloudCam:
akun Uniarch Care -> daftar perangkat/kanal yang memang dibagikan -> live stream P2P -> MediaMTX -> dashboard CCTV All-in-One.

Jangan mengubah jalur Hik-Connect/CloudCam yang sudah berjalan.

## Jalur A — SDK resmi EZCloud (utama)

Sumber resmi:
- https://global.uniview.com/Topics/EZCloud_Platform/
- https://www.uniview.com/Support/Download_Center/SDK/
- https://uniarch.cn/Support/Software/app/

Halaman EZCloud menyatakan App SDK mendukung P2P NAT Traversal (STUN), P2P Relay (TURN), live video latensi rendah, playback, snapshot/recording, dan audio dua arah. Halaman publik belum menyediakan paket App SDK Android, dokumentasi API lengkap, atau kredensial aplikasi yang diperlukan.

Urutan kerja setelah SDK resmi tersedia:
1. Cocokkan versi SDK, region/tenant EZCloud, dan persyaratan lisensinya.
2. Implementasikan autentikasi akun pengguna melalui SDK/API resmi.
3. Ambil daftar perangkat dan kanal yang terlihat oleh akun.
4. Uji NVR-110E2 yang dibagikan: pastikan hak live-view penerima dikenali SDK.
5. Buka P2P stream dan tampilkan di Android Surface/decoder sesuai contoh SDK.
6. Jika SDK memberi akses frame/stream yang dapat diteruskan, buat output RTSP/RTMP atau input FFmpeg ke MediaMTX. Jangan mengasumsikan SDK otomatis menghasilkan RTSP.
7. Uji reconnect, kualitas sub/main stream, playback, dan penggunaan data.

Yang dibutuhkan dari Uniview:
- Android App SDK (AAR atau paket Gradle) dan sample project.
- API reference serta panduan integrasi EZCloud.
- App ID/client credentials/tenant yang diterbitkan untuk integrasi.
- Konfirmasi tertulis bahwa akun penerima perangkat sharing didukung.

## Jalur B — kompatibilitas melalui observasi aplikasi (riset, cadangan)

Tujuannya memetakan alur yang dapat diamati secara sah agar kita tahu apakah ada antarmuka publik/terdokumentasi yang dapat dipakai. Ini bukan pembobolan akun atau penghindaran kontrol akses.

Boleh dilakukan:
- Mencatat layar dan langkah normal aplikasi Uniarch Care saat login, refresh daftar perangkat, memilih kanal, dan mulai live view.
- Mencatat versi aplikasi, model NVR, pesan error, pilihan kualitas stream, serta perilaku jaringan secara umum.
- Memeriksa dokumentasi publik, deep link/export/share resmi, dan komponen SDK yang memang didistribusikan untuk integrasi.
- Membandingkan alur UI dengan API/SDK resmi untuk menemukan fungsi yang belum terdokumentasi, lalu meminta konfirmasi vendor.

Tidak dilakukan:
- Mengambil atau memakai ulang password, cookie, token sesi, QR rahasia, atau kredensial milik pemilik perangkat.
- Membypass login, enkripsi, certificate pinning, izin sharing, atau pembatasan server.
- Meniru endpoint privat atau membuat request dengan signature/secret hasil ekstraksi dari aplikasi.
- Menyimpan kredensial atau data stream sensitif di GitHub.

Jika observasi menunjukkan hanya protokol privat yang tidak didokumentasikan, jalur B dihentikan pada tahap pemetaan dan hasilnya dipakai untuk mengajukan pertanyaan teknis ke Uniview—bukan untuk melewati kontrol akses.

## Tahap lokal saat ini

Belum ada SDK/AAR atau kredensial developer resmi di repo. Karena itu belum ada kode login cloud atau live-stream adapter yang bisa dijalankan. File ini adalah rencana implementasi, bukan klaim bahwa Uniarch sudah terhubung.

## Uji penerimaan

- Akun penerima dapat login tanpa memasukkan kredensial pemilik NVR.
- Perangkat shared NVR-110E2 muncul hanya jika sharing masih aktif.
- Kanal yang diizinkan dapat live view.
- Video dapat diteruskan ke MediaMTX tanpa mengubah konfigurasi NVR.
- Hik-Connect yang ada tetap berfungsi.
- Tidak ada password, token, serial, QR, atau secret dalam log/commit.

## Data yang aman untuk dikirim saat meminta SDK ke vendor

Sebutkan model NVR-110E2, aplikasi Uniarch Care 3.10.61 (Build20251205), Android, dan kebutuhan integrasi live view untuk akun yang menerima device sharing. Jangan sertakan password, token, QR code, atau serial number di issue publik.


## Catatan penting hasil pemeriksaan Download Center

Uniview juga mempublikasikan **Network Device SDK_V2.7.0.1 for Android** (2019), tetapi ini berbeda dari EZCloud App SDK. Deskripsinya menyebut komunikasi protokol jaringan privat dan daftar model NVR yang didukung tidak mencantumkan NVR-110E2. Karena itu SDK ini tidak boleh dianggap sebagai solusi cloud P2P untuk perangkat shared pengguna. Detail: https://www.uniview.com/Support/Download_Center/SDK/201911/859100_168459_0.htm

Jadi Jalur A tetap membutuhkan paket EZCloud App SDK yang benar, bukan sekadar mengunduh Network Device SDK Android yang tersedia publik.


## Pembaruan jalur SDK resmi: Unisee Developer Platform

Pemeriksaan ulang sumber resmi pada 20 November 2026 menemukan jalur pengembang resmi yang kini lebih jelas: **Unisee Developer Platform** di https://global-open.uniview.com/. Uniview mengumumkan platform ini pada 30 Desember 2025. Modulnya mencakup Documentation, API & SDK, UAOP, Partners, dan Support; dokumentasinya mengelompokkan API/SDK per lini produk dan juga menyediakan referensi protokol standar seperti ONVIF. Akses sumber daya lengkap memerlukan pendaftaran; online emulator disebut tersedia untuk pelanggan tersertifikasi.

Halaman resmi EZCloud tetap mencantumkan App SDK untuk P2P NAT traversal (STUN), P2P relay (TURN), low-latency live video, variable-speed playback, audio dua arah, serta snapshot/recording. Ini cocok dengan kebutuhan video P2P jarak jauh, tetapi belum membuktikan bahwa SDK menerima **perangkat yang hanya dibagikan kepada akun penerima**.

### Jalur kerja berikutnya untuk NVR-110E2

1. Daftar di Unisee sebagai developer/integrator dan buka kategori Documentation serta API & SDK untuk EZCloud / Uniarch / NVR.
2. Cari paket Android App SDK atau cloud P2P SDK, sample project, API reference, serta syarat client/app ID.
3. Minta Uniview mengonfirmasi secara tertulis bahwa daftar perangkat hasil sharing ke akun Uniarch Care penerima dan live-view untuk NVR-110E2 didukung SDK.
4. Setelah paket dan kredensial resmi tersedia, cocokkan versi/region akun, lalu buat proof-of-concept daftar perangkat shared → live P2P → output lokal ke MediaMTX.

### Jangan tertukar dengan Network Device SDK Android lama

Download Center masih menampilkan **Network Device SDK_V2.7.0.1 for Android**, bertanggal 25 November 2019. Deskripsinya menyebut protokol komunikasi jaringan privat dan daftar seri NVR yang didukung (NVR201/202/204/208/301/302/304/308/316/516); daftar itu tidak mencantumkan NVR-E2 atau NVR-110E2. Ini bukan bukti kompatibilitas EZCloud App SDK dan bukan pengganti SDK P2P cloud.

### Fakta perangkat yang terverifikasi

Datasheet resmi NVR-E2 menyebut NVR-110E2 sebagai NVR 10 kanal dengan P2P, UPnP, DHCP, outgoing bandwidth 32 Mbps, dan dukungan hingga 128 remote users. Datasheet tersebut tidak memberikan kredensial atau dokumentasi API untuk mengakses perangkat yang dibagikan. Karena NVR berada di jaringan berbeda dan akun pengguna hanya menerima sharing, RTSP/ONVIF lokal tidak dapat diasumsikan bisa dijangkau oleh HP server.

### Template permintaan ke Uniview

Minta:
- Android EZCloud/Uniarch App SDK atau cloud P2P SDK yang berlaku untuk NVR-110E2;
- sample login, list shared devices, open live stream, dan daftar dependensi;
- API reference, app/client ID process, region/tenant requirements, dan izin/lisensi penggunaan;
- konfirmasi bahwa shared device yang diterima oleh akun Uniarch Care dapat dibuka melalui SDK tanpa kredensial pemilik perangkat.

Jangan kirim password, token sesi, QR rahasia, atau serial perangkat ke issue publik.
