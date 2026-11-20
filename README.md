# CCTV All-in-One

Aplikasi Android untuk menggabungkan kamera CCTV dalam satu dashboard. Target perangkat: Android TV Box dan Android HP, dengan minimum Android API 19 (Android 4.4).

## Menu aplikasi

- **Tambah Kamera** — menambahkan alamat RTSP secara manual.
- **ONVIF** — menu disiapkan; penemuan otomatis dan pengambilan profil RTSP belum selesai.
- **Uniarch Care** — menu integrasi tersedia, tetapi API/SDK Uniview belum tersambung.
- **Hik-Connect** — aplikasi Android dapat mengambil daftar kamera dari Hik-Connect Cloud Bridge dan membuka RTSP stream yang disediakan bridge.

## Hik-Connect Cloud Bridge (eksperimental)

Untuk akses Hik-Connect cloud, aplikasi memakai service pendamping pada folder [hikconnect-bridge](hikconnect-bridge/). Service tersebut memasang paket komunitas [Bahrombekk/cloud-cam-viewer](https://github.com/Bahrombekk/cloud-cam-viewer), yang login ke Hik-Connect, mengambil daftar resource kamera, lalu menyediakan stream melalui MediaMTX.

Bridge harus berjalan pada PC/server yang selalu aktif. APK Android tidak menjalankan Python, FFmpeg, atau MediaMTX secara langsung. Lihat panduan lengkap di [hikconnect-bridge/README.md](hikconnect-bridge/README.md).

**Penting untuk akun penerima perangkat share:** dukungan daftar perangkat share belum diuji dengan akun pengguna aplikasi ini. CloudCam menyebut bahwa shared devices/sub-accounts kadang tidak mendapatkan verification code dari cloud. Jika API bridge mengembalikan daftar kosong, bandingkan dahulu dengan daftar perangkat di aplikasi Hik-Connect; jangan menganggap integrasi sudah berhasil. Kamera terenkripsi tetap memerlukan verification code yang benar.

Bridge ini menggunakan implementasi komunitas hasil reverse-engineering, bukan SDK resmi Hikvision. Gunakan hanya dengan perangkat yang memang dibagikan/diizinkan kepada akun Anda dan perhatikan ketentuan layanan Hikvision.

## Menjalankan bridge

1. Pasang Docker Desktop di PC Windows atau Docker Engine di server Linux.
2. Buka folder `hikconnect-bridge`, salin `.env.example` menjadi `.env`.
3. Isi email/password Hik-Connect, alamat IP PC/server, serta dua token/password acak yang berbeda.
4. Jalankan `docker compose up -d --build`.
5. Di aplikasi, pilih **Hik-Connect**, masukkan alamat bridge (contoh `http://192.168.1.20:8765`) dan `CLOUDCAM_BRIDGE_TOKEN`.

Mulai dengan ponsel dan PC pada Wi-Fi/LAN yang sama. Untuk akses dari luar jaringan, gunakan VPN pribadi seperti Tailscale atau WireGuard. Jangan membuka port bridge/RTSP langsung ke internet.

## Status fitur

| Fitur | Status |
|---|---|
| Tambah kamera RTSP manual | Tersedia |
| Pemutar satu kamera RTSP | Tersedia |
| Ikon aplikasi CCTV | Tersedia |
| Empat menu utama | Tersedia |
| Android ↔ Hik-Connect bridge API | Implementasi awal; perlu uji dengan akun share |
| Hik-Connect live stream | Bergantung bridge, izin perangkat share, dan verification code |
| Penemuan kamera ONVIF otomatis | Belum |
| Integrasi langsung Uniarch Care | Belum |
| Grid 1/4/9/16, PTZ, playback cloud | Belum |

## Keamanan

- Jangan pernah commit file `.env`, password, API token, session token, verification code, atau serial kamera ke GitHub.
- Token bridge dan password RTSP harus berbeda.
- Jangan paparkan port API/RTSP ke internet publik; gunakan VPN.
- URL RTSP mengandung kredensial stream. Aplikasi menyembunyikannya dari daftar kamera, tetapi tetap memperlakukannya sebagai rahasia.
- Daftar kamera manual saat ini disimpan di SharedPreferences; penyimpanan terenkripsi belum diimplementasikan.

## Build APK

APK debug dibangun otomatis dengan GitHub Actions:
[Build CCTV All-in-One](https://github.com/untuksrt5git/cctvall/actions).
