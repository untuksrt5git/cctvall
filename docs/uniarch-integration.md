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