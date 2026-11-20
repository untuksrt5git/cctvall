# Hik-Connect Cloud Bridge untuk CCTV All-in-One

Bridge ini memakai cloud-cam-viewer sebagai mesin cloud Hik-Connect. Akun Hik-Connect tetap berada di PC/server milik Anda; aplikasi Android hanya meminta daftar kamera dan membuka stream RTSP.

**Ini bukan SDK resmi Hikvision.** CloudCam merupakan klien komunitas hasil reverse-engineering. Gunakan hanya untuk perangkat yang memang dibagikan/diizinkan kepada akun Anda. Dukungan perangkat share perlu diuji pada akun Anda sendiri.

## Kenapa perlu bridge terpisah?

CloudCam memerlukan Python 3.10+, FFmpeg, dan MediaMTX. Karena aplikasi CCTV All-in-One juga menargetkan Android 4.4 KitKat, komponen tersebut tidak saya paksakan masuk ke APK. Bridge dapat dijalankan pada PC Windows/Linux yang selalu aktif atau server Linux. Ponsel/TV Box menjadi penampil RTSP.

## Menjalankan di PC Windows

1. Pasang Docker Desktop dan pastikan Docker dalam keadaan berjalan.
2. Salin file .env.example menjadi .env di folder hikconnect-bridge.
3. Isi email dan kata sandi akun Hik-Connect Anda sendiri pada .env. Jangan mengirim file ini kepada siapa pun.
4. Ubah CLOUDCAM_STREAM_HOST menjadi alamat IP PC di jaringan lokal, misalnya 192.168.1.20.
5. Ganti CLOUDCAM_BRIDGE_TOKEN dan CLOUDCAM_STREAM_PASSWORD dengan dua kata sandi acak yang berbeda.
6. Buka Terminal/PowerShell di folder tersebut, lalu jalankan:

```bash
docker compose up -d --build
```

Periksa prosesnya:

```bash
docker compose logs -f hikconnect-bridge
```

API bridge berada di port 8765; stream RTSP di port 8554.

## Nama kamera muncul, tetapi gambar belum tampil?

Perintah `cloudcam cameras` hanya menguji login dan menampilkan daftar kamera. Itu belum membuka video. Untuk melihat gambar langsung di layar PC, jalankan:

```bash
cloudcam view
```

Untuk APK CCTV All-in-One, jangan memakai output teks perintah `cameras` sebagai URL video. Jalankan bridge Docker seperti langkah di bawah. Bridge akan membuka RTSP MediaMTX secara on-demand ketika APK meminta stream kamera. Pastikan `CLOUDCAM_STREAM_HOST` adalah IP PC/server yang bisa dijangkau HP, bukan `127.0.0.1` atau `localhost`.

## Menghubungkan aplikasi Android

Di aplikasi CCTV All-in-One v0.3.1, pilih **Hik-Connect** → **Cloud Bridge**. Masukkan alamat bridge, misalnya:

```text
http://192.168.1.20:8765
```

Masukkan nilai CLOUDCAM_BRIDGE_TOKEN lalu tekan **Sambungkan**. Jika perangkat berhasil dikenali oleh CloudCam, daftar kamera akan muncul. Tekan **Buka** pada salah satu kamera untuk memutar RTSP di APK. Pemutar akan menampilkan status buffering, status LIVE, dan pesan kode kesalahan bila koneksi gagal.

## Verifikasi 2FA dan kode enkripsi kamera

Pada login pertama, CloudCam dapat meminta verifikasi email untuk mengambil kode enkripsi kamera. Perintahnya:

```bash
docker compose exec hikconnect-bridge cloudcam keys
```

Ikuti instruksi kode verifikasi yang muncul. Untuk kamera share/sub-account, CloudCam mencatat bahwa kode verifikasi mungkin tidak tersedia dari cloud. Dalam kondisi itu, pemilik perangkat mungkin perlu memberikan kode verifikasi kamera kepada Anda melalui jalur yang aman.

## Akses dari luar rumah

Untuk tahap awal, gunakan pada jaringan Wi-Fi/LAN yang sama. Untuk akses dari luar rumah, gunakan VPN pribadi seperti Tailscale/WireGuard. **Jangan membuka/port-forward port 8765, 8554, atau 8888 langsung ke internet.** API menggunakan token dan stream memakai password baca, tetapi koneksi HTTP/RTSP biasa ini bukan pengganti TLS/VPN.

## Catatan penting

- Jika API mengembalikan daftar kosong, itu belum berarti akun tidak punya perangkat share. Bisa jadi API CloudCam tidak mengembalikan resource perangkat yang dibagikan. Kita harus membandingkannya dengan daftar di aplikasi Hik-Connect.
- Kamera dengan enkripsi video memerlukan verification code. Tanpa kode yang benar, stream bisa gagal didekripsi.
- Stream pertama dapat memerlukan sekitar 7–10 detik untuk handshake cloud dan menunggu keyframe.
- Bridge menyimpan session token dan kode kamera di volume Docker hikconnect-data. Jangan menghapus volume jika ingin menghindari login/2FA berulang.
- Jangan commit file .env, token, kode verifikasi, atau serial perangkat ke GitHub.

## Sumber dan lisensi

CloudCam menggunakan lisensi MIT dan komponen VTM yang diadaptasi dari pyezvizapi di bawah Apache-2.0. Paket dipasang sebagai dependensi saat Docker image dibangun; kodenya tidak disalin ke proyek ini. Baca lisensi upstream dan ketentuan Hikvision sebelum digunakan.


## Pemeriksaan video jika hanya nama yang muncul

1. Di PC, pastikan `cloudcam view` dapat menampilkan gambar. Jika tidak, periksa online status dan verification code dengan `cloudcam cameras --check` dan `cloudcam keys`.
2. Pastikan container bridge hidup dan port RTSP `8554` dipetakan oleh Docker.
3. Pastikan `CLOUDCAM_STREAM_HOST` menggunakan IP LAN PC/server yang benar dan HP dapat menjangkaunya.
4. Pada APK, tekan **Buka**. CloudCam akan memulai stream sesuai permintaan; frame pertama dapat memerlukan beberapa detik.
5. CloudCam mendukung H.264/H.265, tetapi decoder dan RTSP module pada ponsel dapat membatasi codec yang dapat ditampilkan. Bila kamera memakai HEVC/H.265 dan APK tetap hitam, perlu jalur player yang mendukung HEVC atau transkode H.264 pada bridge.

Catatan: versi ini memakai PC/server sebagai mesin CloudCam. Integrasi CloudCam tanpa PC langsung di Android belum selesai.
