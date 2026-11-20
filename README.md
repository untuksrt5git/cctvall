# CCTV All-in-One

Aplikasi Android CCTV untuk HP dan Android TV Box. Versi uji modern menggunakan minimum Android 6.0 (API 23) dan target Android 14 (API 34).

## Menu aplikasi

- **Tambah Kamera** — menambahkan alamat RTSP secara manual.
- **ONVIF** — menu disiapkan; penemuan otomatis dan pengambilan profil RTSP belum selesai.
- **Uniarch Care** — menu integrasi tersedia, tetapi API/SDK Uniview belum tersambung.
- **Hik-Connect** — membuka halaman akun Hik-Connect di dalam aplikasi:
  https://www.hik-connect.com/views/login/index.html#/main/overview

## Penting: login web tidak sama dengan live view aplikasi

Tombol Login Web Hik-Connect membuka portal akun yang benar, bukan portal pengembang Hikvision/TPP. Halaman web dapat digunakan untuk masuk ke akun Hik-Connect dan memeriksa perangkat yang terlihat di portal.

Login web **belum otomatis memberikan sesi cloud atau stream video kepada pemutar CCTV All-in-One**. Untuk live view langsung dari akun cloud tanpa PC/server, mesin protokol cloudcam perlu dipindahkan dan disesuaikan ke Android (autentikasi, VTM stream, dekripsi video, dan pemutar). Hal itu belum selesai pada v0.3.0.

Pilihan Cloud Bridge tetap tersedia sebagai jalur uji terpisah, tetapi memerlukan server pendamping. Dukungan perangkat yang hanya dibagikan kepada akun penerima juga harus diuji.

## Status fitur v0.3.0

| Fitur | Status |
|---|---|
| Tambah kamera RTSP manual | Tersedia |
| Pemutar satu kamera RTSP | Tersedia |
| Empat menu utama | Tersedia |
| Portal web Hik-Connect di dalam aplikasi | Ditambahkan pada v0.3.0 |
| Hik-Connect cloud live view langsung di APK tanpa server | Belum |
| Integrasi Uniarch Care | Belum |
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
