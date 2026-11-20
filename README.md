# CCTV All-in-One

Aplikasi Android CCTV untuk HP dan Android TV Box. Versi uji modern v0.3.1 menggunakan minimum Android 6.0 (API 23) dan target Android 14 (API 34).

## Menu aplikasi

- **Tambah Kamera** — menambahkan alamat RTSP secara manual.
- **ONVIF** — menu disiapkan; penemuan otomatis dan pengambilan profil RTSP belum selesai.
- **Uniarch Care** — menu integrasi tersedia, tetapi API/SDK Uniview belum tersambung.
- **Hik-Connect** — mengambil daftar kamera dari CloudCam Bridge dan memutar stream RTSP di pemutar Android. Login portal Hik-Connect tetap tersedia melalui menu informasi.

## Penting: login web tidak sama dengan live view aplikasi

Tombol Login Web Hik-Connect membuka portal akun yang benar, bukan portal pengembang Hikvision/TPP. Halaman web dapat digunakan untuk masuk ke akun Hik-Connect dan memeriksa perangkat yang terlihat di portal.

Login web **tidak otomatis memberikan sesi cloud atau stream video kepada pemutar CCTV All-in-One**. Jalur live view Hik-Connect yang sekarang diaktifkan memakai CloudCam Bridge pada PC/server: CloudCam mengambil stream cloud dan MediaMTX menyediakan RTSP, lalu APK memutarnya. APK belum menjalankan mesin Python CloudCam langsung di Android; karena itu PC/server tetap diperlukan untuk jalur ini. Dukungan perangkat yang hanya dibagikan kepada akun penerima tetap bergantung pada akses CloudCam dan kode verifikasi perangkat.

## Status fitur v0.3.1

| Fitur | Status |
|---|---|
| Tambah kamera RTSP manual | Tersedia |
| Pemutar satu kamera RTSP | Tersedia |
| Empat menu utama | Tersedia |
| Portal web Hik-Connect di dalam aplikasi | Ditambahkan pada v0.3.0 |
| Hik-Connect live view melalui CloudCam Bridge + RTSP player | Ditambahkan pada v0.3.1; perlu diuji dengan akun/kamera Anda |
| Hik-Connect cloud live view langsung di APK tanpa server | Belum |
| Integrasi Uniarch Care / EZCloud App SDK | Menunggu paket SDK dan akses integrasi resmi |
| Pencarian ONVIF otomatis | Belum |
| Grid 1/4/9/16, PTZ, playback cloud | Belum |

## Hik-Connect Cloud Bridge

Untuk jalur uji bridge yang memerlukan PC/server, lihat [hikconnect-bridge/README.md](hikconnect-bridge/README.md). Bridge menggunakan paket komunitas [cloud-cam-viewer](https://github.com/Bahrombekk/cloud-cam-viewer), bukan SDK resmi Hikvision.

## Keamanan

- Jangan masukkan kata sandi akun ke pesan/chat.
- Jangan commit file `.env`, password, API token, session token, verification code, atau serial kamera ke GitHub.
- Gunakan hanya akun dan kamera yang memang diizinkan untuk diakses.
- Halaman login web dibuka melalui Android WebView dengan JavaScript dan cookie aktif agar alur login portal dapat berjalan. Koneksi HTTPS tidak diturunkan ke HTTP.

## Build APK

APK debug dibangun otomatis melalui [GitHub Actions](https://github.com/untuksrt5git/cctvall/actions).


## Hik-Connect: perbedaan daftar kamera dan video

Pada CloudCam, perintah `cloudcam cameras` memang hanya menampilkan metadata/nama kamera. Untuk membuka gambar di komputer, perintah upstream adalah `cloudcam view`. Untuk mengirim video ke APK, bridge menjalankan `cloudcam serve` dan menyediakan RTSP MediaMTX. APK v0.3.1 sekarang memuat modul RTSP ExoPlayer dan memutar alamat RTSP yang diberikan API bridge.

Paket CloudCam tetap dijalankan di PC/server. Pastikan `CLOUDCAM_STREAM_HOST` di `.env` berisi IP PC/server yang dapat dijangkau HP, dan port RTSP 8554 terbuka di jaringan lokal. Lihat [panduan bridge](hikconnect-bridge/README.md). Koneksi dari luar jaringan sebaiknya melalui VPN pribadi; jangan membuka port RTSP/API langsung ke internet.

## Integrasi Uniarch / EZCloud

Halaman publik resmi Uniview mengonfirmasi tersedianya App SDK untuk P2P STUN/TURN, live video latensi rendah, playback, audio dua arah, snapshot, serta API manajemen perangkat/PTZ. Namun, halaman tersebut tidak menyediakan paket SDK Android atau kredensial integrasi yang dapat langsung dipasang dari repositori publik. Modul Uniarch belum diklaim aktif sampai SDK resmi dan akses akun integrasi tersedia. Lihat [catatan integrasi Uniarch](docs/uniarch-integration.md).
