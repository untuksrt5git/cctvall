# Integrasi Uniarch Care / Uniview EZCloud

## Yang sudah dapat dipastikan dari sumber resmi

- Halaman [Uniarch Care](https://www.uniarch.cn/Support/Software/app/) menjelaskan aplikasi Android/iOS untuk live view, playback, alarm, berbagi cloud device, dan pengelolaan file dari jarak jauh.
- Halaman [Uniarch Video Management Software](https://www.uniarch.cn/Support/Software/computer/) menjelaskan VMS desktop yang mendukung perangkat cloud, live view, playback, PTZ, audio dua arah, dan layout multi-view. Paket yang ditautkan adalah aplikasi Windows/Mac, bukan Android SDK.
- Halaman [EZCloud Platform](https://global.uniview.com/Topics/EZCloud_Platform/) mencantumkan App SDK untuk P2P STUN/TURN, live video latensi rendah, playback, audio dua arah, recording/snapshot. API platform juga mencantumkan CRUD perangkat serta kontrol PTZ/snapshot.

## Mengapa belum saya aktifkan sebagai live view di APK

Halaman publik EZCloud menjelaskan kemampuan SDK, tetapi tidak menyediakan paket Android SDK (AAR), contoh integrasi, atau kredensial aplikasi yang bisa langsung digunakan. Tautan `Interface details` diarahkan ke portal cloud terpisah. Saya tidak akan menebak endpoint, client ID, secret, atau protokol streaming karena dapat membuat login gagal dan berisiko membocorkan akun.

## Jalur implementasi

1. Diperlukan paket App SDK Android resmi dan dokumentasi versi yang sesuai dengan akun/region EZCloud.
2. Diperlukan client/app ID serta secret atau tenant credentials yang diterbitkan Uniview untuk integrasi.
3. Implementasikan login dan pengambilan daftar perangkat cloud, lalu validasi apakah perangkat yang dibagikan ke akun penerima juga dikembalikan API.
4. Implementasikan P2P stream (STUN/TURN), decoder/video surface, playback, snapshot, PTZ, dan audio sesuai izin akun.
5. Uji dengan akun Uniarch Care yang sama dengan akun pengguna, termasuk perangkat shared.

## Status di CCTV All-in-One

Menu Uniarch sekarang membuka [Cloud Open Platform](https://cloud.uniview.com/mainPage/homePage) atau [halaman resmi unduh Uniarch Care](https://www.uniarch.cn/Support/Software/app/). Integrasi SDK/live video belum aktif sampai paket SDK dan akses integrasinya tersedia. Tidak ada kata sandi yang perlu dikirim lewat chat.

## Catatan sumber

Semua kemampuan produk di atas diambil dari halaman dukungan resmi Uniarch/Uniview yang tertaut pada bagian atas. Fitur bisa berbeda menurut model perangkat, region cloud, dan versi software.

## Pembaruan 20 November 2026: Unisee

Uniview kini memperkenalkan [Unisee Developer Platform](https://global-open.uniview.com/) sebagai portal dokumentasi, API & SDK, UAOP, partner integration, dan support. Pengumuman resmi bertanggal 30 Desember 2025 menyebut dokumentasi API/SDK disusun per product line serta referensi protokol standar seperti ONVIF; pendaftaran diperlukan untuk membuka resource penuh. Ini adalah jalur resmi baru yang perlu diperiksa untuk memperoleh SDK P2P cloud yang sesuai.

Halaman [EZCloud Platform](https://global.uniview.com/Topics/EZCloud_Platform/) mencantumkan kemampuan App SDK: P2P STUN/TURN, low-latency live video, playback, audio dua arah, snapshot/recording. Belum ada bukti publik yang memastikan perangkat shared ke akun penerima dapat dibuka oleh SDK tersebut.

Download Center juga menampilkan Network Device SDK Android V2.7.0.1 (25 November 2019), tetapi itu SDK protokol jaringan privat dan daftar kompatibilitas NVR-nya tidak mencantumkan seri NVR-E2/NVR-110E2. Jangan menyamakannya dengan App SDK EZCloud.

NVR-110E2 datasheet resmi: 10 kanal, P2P/UPnP/DHCP, outgoing bandwidth 32 Mbps. Karena perangkat berada di jaringan lain dan akses pengguna berasal dari sharing cloud, jalur lokal RTSP/ONVIF belum tersedia kecuali pemilik mengizinkan akses jaringan lokal/remote secara resmi.

Langkah berikut: registrasi Unisee, cari EZCloud/Uniarch Android App SDK, lalu minta konfirmasi dukungan shared-device recipient dan NVR-110E2 sebelum menulis adapter. Rincian dan template permintaan ada di [uniarch-dual-track.md](uniarch-dual-track.md).
