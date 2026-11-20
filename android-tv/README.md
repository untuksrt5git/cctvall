# CloudCam Server di Android TV (Android 10/11)

Dokumen ini menyiapkan Android TV sebagai **server CloudCam**, tanpa PC Windows.
Untuk tahap pertama, runtime dijalankan melalui Termux agar CloudCam dapat memakai
Python, FFmpeg, dan proses MediaMTX sebagaimana rancangan upstream-nya. APK CCTV
All-in-One v0.4.x yang memakai Chaquopy belum menjalankan jalur live-stream karena
CloudCam upstream membuat proses Python dan FFmpeg terpisah.

## Target tahap awal

- Android TV Android 10/11, ARM64 (disarankan), RAM 2 GB atau lebih.
- Satu kamera terlebih dahulu; gunakan substream `/sub` untuk menghemat bandwidth.
- Server dapat diakses dari STB/HP di jaringan lokal.
- Akses dari luar rumah akan ditambahkan setelah uji LAN berhasil, melalui VPN/reverse tunnel
  dengan autentikasi. Jangan membuka port RTSP langsung ke internet.

## 1. Pasang Termux

Pasang Termux dari F-Droid atau rilis GitHub resmi Termux. Hindari build Play Store lama.
Buka Termux dan berikan izin penyimpanan jika diminta (tidak wajib untuk konfigurasi ini).

## 2. Ambil berkas setup dari repositori

Unduh dua berkas ini ke folder yang sama di Android TV:

- `android-tv/setup-android-tv.sh`
- `android-tv/start-cloudcam-server.sh`

Jika berkas sudah berada di Download, salin ke folder Termux atau jalankan dari lokasi yang bisa dibaca Termux.

Jalankan:

```bash
bash setup-android-tv.sh
```

Script memasang Python, FFmpeg, Git, membuat virtual environment, lalu memasang CloudCam
pada commit yang dipakai APK saat ini.

## 3. Jalankan server

```bash
~/bin/start-cloudcam-server
```

Masukkan username/User ID/email dan password Hik-Connect langsung di Termux.
Kredensial tidak perlu dikirim ke ChatGPT atau ditulis ke GitHub. CloudCam menyimpan token
sesi di direktori privat Termux.

Script juga meminta username dan password khusus untuk viewer. Password viewer minimal
12 karakter. Jangan gunakan password Hik-Connect yang sama.

Pada proses pertama, CloudCam mencoba memasang MediaMTX resmi dan memeriksa checksum.
Jika binary MediaMTX Linux ARM64 tidak dapat dijalankan pada Android/Termux, proses akan
berhenti dengan error. Kirim teks error tersebut; jangan mengubah permission atau mengunduh
binary dari sumber tidak resmi.

## 4. Uji dari jaringan lokal

Saat `cloudcam serve` berjalan, catat alamat IP lokal Android TV dari pengaturan Wi-Fi
atau router. CloudCam akan menampilkan alamat RTSP untuk kamera. Launcher memakai port RTP/RTCP UDP 18000/18001 (bukan default 8000/8001) untuk menghindari benturan port di Android. Port ini dapat diubah sebelum menjalankan launcher dengan `CCTV_RTP_PORT=19000 ~/bin/start-cloudcam-server`.

Format umumnya:

- Main stream: `rtsp://IP-ANDROID-TV:8554/cam/SERIAL_CHANNEL`
- Substream: `rtsp://IP-ANDROID-TV:8554/cam/SERIAL_CHANNEL/sub`

Gunakan username/password viewer yang dibuat pada langkah 3. Uji dulu dengan VLC pada
perangkat lain di Wi-Fi yang sama. Setelah terbukti lancar, alamat ini akan dipakai oleh
APK Viewer STB KitKat.

## Menambahkan kamera RTSP / ONVIF (misalnya Smart Life / Tuya)

Setelah helper dipasang, jalankan perintah yang dibuat oleh HTML Viewer. Perintah itu
memanggil `~/bin/add-rtsp-camera` dan meminta password ONVIF secara tersembunyi.
Password tidak ditulis pada perintah yang disalin dari browser. URL RTSP disimpan oleh
CloudCam di file privat `~/.config/cloudcam/rtsp_cameras.json`.

Jika helper belum ada di Termux, pasang sekali:

```bash
mkdir -p ~/bin
curl -L https://raw.githubusercontent.com/untuksrt5git/cctvall/main/android-tv/add-rtsp-camera.sh -o ~/bin/add-rtsp-camera
chmod 700 ~/bin/add-rtsp-camera
```

Setelah pendaftaran berhasil, hentikan proses `cloudcam serve` dengan Ctrl+C lalu
jalankan kembali `~/bin/start-cloudcam-server`. CloudCam akan memuat kamera RTSP yang
tersimpan dan membuat path WebRTC `rtsp/NAMA_KAMERA`. Gunakan path persis itu pada
form Tambah kamera di HTML Viewer. Contoh: nama `tuya_kamera1` menghasilkan path
`rtsp/tuya_kamera1`, bukan `tuya/kamera1`.

## Catatan penting

- Ini tahap server eksperimental berbasis Termux, bukan APK server satu-klik.
- Belum mengaktifkan autostart saat Android TV dinyalakan. Itu dibuat setelah uji stabil.
- Perangkat Android TV harus tetap menyala dan tidak boleh membatasi Termux berjalan di latar belakang.
- Mulai dengan satu kamera substream. Jangan langsung membuka banyak kamera di perangkat RAM kecil.
- Untuk akses jarak jauh, kita akan menambahkan tunnel/VPN privat setelah tes LAN berhasil.


## Dashboard Dinding CCTV + pendaftaran RTSP dari web

Dashboard utama sekarang memakai \`android-viewer/index.html\`. Untuk fungsi pendaftaran kamera dari browser, jangan jalankan \`python -m http.server\`; gunakan server yang memiliki API:

\`\`\`bash
cd ~/cctvall/android-viewer
~/.venvs/cctvall-cloudcam/bin/python start-dashboard-server.py --bind 0.0.0.0 --port 8090
\`\`\`

Server menampilkan kode pengelola acak di terminal. Masukkan kode tersebut di bagian **Tambah kamera Tuya / RTSP langsung dari web** pada dashboard. Kode berubah setiap server dashboard dimulai ulang dan tidak disimpan di HTML.

Dari HP server buka \`http://127.0.0.1:8090\`; dari HP lain pada Wi-Fi yang sama buka \`http://IP-HP-SERVER:8090/\`. Untuk video, alamat WebRTC di Pengaturan dashboard harus memakai \`http://IP-HP-SERVER:8889\`.

Form web menyimpan nama, IP lokal kamera, port RTSP, username/password ONVIF/RTSP, dan path RTSP ke penyimpanan privat CloudCam. **Setelah menyimpan kamera, hentikan CloudCam dengan Ctrl+C dan jalankan kembali \`~/bin/start-cloudcam-server\`** agar path \`rtsp/NAMA\` dimuat oleh MediaMTX.

Batasan penting:
- Form web saat ini menerima path RTSP yang sudah diketahui; ini belum melakukan pemindaian ONVIF otomatis. Browser tidak dapat mengirim WS-Discovery multicast/UDP secara langsung. Penemuan otomatis perlu modul ONVIF di sisi server.
- Gunakan hanya pada Wi-Fi/LAN tepercaya. Form mengirim password kamera melalui HTTP lokal; jangan meneruskan port 8090 ke internet. Untuk akses jarak jauh gunakan VPN dan HTTPS.
- Kode pengelola hanya berlaku sampai dashboard server dihentikan. Jangan membagikannya ke orang lain.
- Dashboard default hanya memilih 4 kamera. Pilihan 9/16 tersedia tetapi lebih berat. Mulai kamera bertahap, grid memakai substream bila disediakan, dan layar penuh memakai main stream.
