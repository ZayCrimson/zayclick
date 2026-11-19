# ZAY CLICKER (starter source)

Target awal: Android 8+ / compile SDK 35. Project ditulis dengan Kotlin dan Android Gradle Plugin.

## Buka project
1. Ekstrak ZIP.
2. Buka folder `ZayClicker` di Android Studio.
3. Tunggu Gradle Sync.
4. Build > Build APK(s).
5. Instal APK hasil build.
6. Buka Settings > Accessibility > ZAY CLICKER gesture service, lalu aktifkan.

## Kondisi fitur saat ini
- Form koordinat click dan hold: tersedia.
- Pengulangan spam click: tersedia melalui AccessibilityService.
- Gesture hold berdurasi terbatas: tersedia sebagai implementasi awal.
- Stop control: tersedia sebagai prototipe.
- Floating overlay / drag target editor: belum dibuat.
- Root backend / Magisk: belum dibuat.
- CLICK dan HOLD multi-touch simultan: BELUM dijamin dan belum diimplementasikan.

## Kenapa belum mengklaim multi-touch?
`AccessibilityService.dispatchGesture()` dapat membatalkan gesture yang sedang berjalan ketika gesture baru dikirim. Karena itu dua loop terpisah bukan solusi yang benar untuk satu jari hold sambil jari kedua spam-click. Implementasi root yang benar memerlukan backend input virtual multi-touch (misalnya uinput) yang kompatibel dengan kernel dan SELinux perangkat. Itu perlu diuji pada Poco X5; sekadar menambahkan `su` tidak otomatis membuatnya bekerja.

Koordinat adalah piksel layar mentah pada orientasi saat ini. Versi ini belum punya kalibrasi/overlay pemilih posisi.


## Build APK otomatis lewat GitHub Actions
1. Buat repository GitHub baru, misalnya `ZayClicker`.
2. Upload seluruh isi folder project ini ke root repository (pastikan folder `.github/workflows/` ikut ter-upload).
3. Buka tab **Actions** di repository.
4. Pilih workflow **Android APK Build** lalu tekan **Run workflow**. Workflow juga berjalan otomatis setiap push ke branch `main` atau `master`.
5. Setelah selesai, buka run yang berhasil dan unduh artifact **ZAY-CLICKER-debug-apk**.
6. Ekstrak artifact ZIP untuk mendapatkan `app-debug.apk`.

Build ini menghasilkan APK debug untuk pengujian, bukan APK release bertanda tangan untuk distribusi.
