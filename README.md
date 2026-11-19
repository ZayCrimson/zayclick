# ZAY CLICKER 0.3.0 - Root uinput Multitouch

## Fitur
- Root backend lewat `su` (KernelSU/Magisk).
- Native C++ virtual touchscreen melalui `/dev/uinput`.
- Dua slot sentuhan terpisah: slot 0 untuk HOLD, slot 1 untuk CLICK.
- CLICK dan HOLD dapat aktif bersamaan pada satu virtual input device.
- Overlay marker CLICK/HOLD dapat digeser langsung; tidak ada kolom koordinat X/Y.
- Posisi marker disimpan setelah digeser.
- Floating control START/STOP CLICK, START/STOP HOLD, STOP ALL, dan pengaturan interval (+/- 20 ms).
- Marker dapat dipindah saat HOLD aktif; engine mengirim MOVE pada slot hold.

## Build
1. Buka project ini dengan Android Studio yang memasang Android SDK 35, NDK 27.2.12479018, dan CMake 3.22.1.
2. Sync Gradle lalu jalankan `assembleDebug`.
3. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Atau push project ke GitHub. Workflow `.github/workflows/android-build.yml` membangun APK debug dan mengunggah artifact.

## Instalasi dan penggunaan
1. Instal APK dan buka ZAY CLICKER.
2. Berikan izin "Tampil di atas aplikasi lain".
3. Tekan CEK AKSES ROOT dan izinkan KernelSU.
4. Tekan JALANKAN FLOATING CONTROL.
5. Geser marker biru CLICK dan hijau HOLD ke target di layar. Atur interval dengan tombol −/+ pada panel.
6. Tekan START HOLD dan START CLICK sesuai kebutuhan. Keduanya memakai slot multitouch terpisah.
7. STOP ALL melepaskan semua slot sentuhan dan menghentikan loop click.

## Catatan kompatibilitas
- Target ABI saat ini `arm64-v8a`, sesuai perangkat Poco X5 5G 64-bit.
- Kernel harus menyediakan `/dev/uinput` dan `CONFIG_INPUT_UINPUT=y`.
- Aplikasi tidak mengubah SELinux dan tidak menjalankan `setenforce 0`.
- Pada perangkat yang diberikan, `/dev/uinput` terlihat sebagai `uhid:uhid` dengan mode `0660`, sementara proses `su` berada di domain `u:r:ksu:s0`. Root UID tidak menjamin akses SELinux. Jika SELinux menolak `open()` atau ioctl, status error akan muncul di toast; kebijakan perangkat perlu diperiksa secara terpisah. Jangan mematikan SELinux sebagai jalan pintas.
- Android/OEM dapat mengabaikan virtual touchscreen jika konfigurasi InputReader atau pemetaan display tidak cocok. Keberhasilan pembuatan device perlu diverifikasi pada perangkat dengan `getevent -lp` dan `dumpsys input`.
- Posisi overlay memakai ukuran display aktual. Rotasi layar setelah service aktif mungkin memerlukan restart overlay agar ukuran input diperbarui.
- Build ini belum diuji pada perangkat fisik. Uji awal di layar aman dan gunakan STOP ALL jika sentuhan tidak berhenti seperti diharapkan.
