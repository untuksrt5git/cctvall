# Dinding CCTV — Android Viewer

## Menjalankan dashboard

Hentikan dulu server lama \`python -m http.server\` dengan \`Ctrl+C\`. Dari Termux, jalankan server dashboard yang juga menyediakan API pendaftaran kamera:

\`\`\`bash
cd ~/cctvall/android-viewer
~/.venvs/cctvall-cloudcam/bin/python start-dashboard-server.py --bind 0.0.0.0 --port 8090
\`\`\`

Buka dari HP server: \`http://127.0.0.1:8090/\`

Dari HP lain pada Wi-Fi yang sama: \`http://IP-HP-SERVER:8090/\`, misalnya \`http://192.168.1.26:8090/\`. Pada Pengaturan dashboard, alamat WebRTC harus mengarah ke \`http://IP-HP-SERVER:8889\`, bukan \`127.0.0.1\`.

Saat mulai, Termux menampilkan **kode pengelola web** acak. Masukkan kode tersebut pada formulir "Tambah kamera Tuya / RTSP langsung dari web". Kode hanya berlaku selama server dashboard berjalan; jangan dibagikan ke orang lain.

## Tambah kamera RTSP / Tuya dari browser

Form web menerima nama kamera, IP IPv4 kamera pada LAN, port RTSP, username, password ONVIF/RTSP, dan path RTSP. Password dikirim ke server lokal dan disimpan oleh CloudCam di penyimpanan privat. Server tidak mencetak isi permintaan atau password ke log.

Contoh input path, bergantung model:
- \`/stream1\`
- \`/main\`
- \`/Streaming/Channels/102\`

Ini belum merupakan pencarian ONVIF otomatis. Browser tidak dapat mengirim paket WS-Discovery multicast/UDP secara langsung; path RTSP harus diketahui lebih dahulu. Penemuan ONVIF otomatis membutuhkan komponen server-side terpisah.

Setelah form menyatakan berhasil:
1. Kembali ke sesi CloudCam.
2. Tekan \`Ctrl+C\` untuk menghentikan CloudCam/MediaMTX.
3. Jalankan lagi \`~/bin/start-cloudcam-server\`.
4. Kembali ke dashboard, buka daftar kamera dan centang kamera baru. Path WebRTC-nya \`rtsp/NAMA_KAMERA\`.

## Batasan keamanan

- Gunakan formulir hanya di Wi-Fi/LAN tepercaya. HTTP lokal tidak mengenkripsi password selama transit.
- Jangan port-forward port 8090, 8554, atau 8889 ke internet.
- Untuk akses jarak jauh gunakan VPN dan HTTPS.
- Token pengelola dibuat ulang setiap dashboard server dimulai.

## Mengurangi beban HP

- Default hanya 4 kamera dipilih dan streaming tidak dimulai otomatis.
- Pilihan 1/4/9/16 menentukan batas jumlah kamera yang tampil.
- "Mulai yang tampil" hanya memulai tile yang sedang ditampilkan. Jika lebih dari 4, dashboard meminta konfirmasi.
- Stream dimulai berurutan dengan jeda 2,5 detik.
- Grid memakai substream jika path substream tersedia; layar penuh memakai main stream.
- "Jeda semua" menghapus iframe agar sesi WebRTC ditutup.
- Mulai dengan 4 kamera. 9 atau 16 kamera tetap bergantung pada kemampuan CPU, memori, jaringan, dan bitrate kamera.
