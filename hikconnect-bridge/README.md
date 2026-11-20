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

## Menghubungkan aplikasi Android

Di aplikasi CCTV All-in-One, pilih **Hik-Connect**. Masukkan alamat bridge, misalnya:

```text
http://192.168.1.20:8765
```

Masukkan nilai CLOUDCAM_BRIDGE_TOKEN lalu tekan **Sambungkan**. Jika perangkat share berhasil dikenali oleh cloud, daftar kameranya akan muncul di aplikasi.

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
