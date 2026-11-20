# CCTV All-in-One

Aplikasi Android untuk menggabungkan kamera CCTV dalam satu dashboard.

## Target awal
- Android TV Box dan Android HP.
- Minimum Android API 19 (Android 4.4).
- Tambah kamera melalui URL RTSP.
- Simpan daftar kamera secara lokal.
- Putar stream RTSP melalui ExoPlayer.
- Build APK otomatis melalui GitHub Actions.

## Status implementasi
Tahap awal: dashboard dan pemutar satu kamera RTSP. Pencarian ONVIF otomatis, tampilan multi-kamera, penyimpanan kredensial terenkripsi, reconnect, rotasi kamera, serta integrasi cloud vendor belum diaktifkan.

## Integrasi vendor
- Hik-Connect: memerlukan akses resmi Hikvision OpenAPI/SDK dan kredensial integrasi.
- Uniarch Care: memerlukan API/SDK pihak Uniview atau alamat stream yang memang disediakan perangkat.
- Smart Life/Tuya: ONVIF/RTSP lokal dapat digunakan bila kamera mengaktifkannya dan alamat stream diketahui.

Jangan memasukkan password akun cloud ke aplikasi tahap awal. URL RTSP yang mengandung kredensial juga harus diperlakukan sebagai rahasia.
