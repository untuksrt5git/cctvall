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
