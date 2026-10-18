# Voice Changer MLBB — Android prototype

Prototype Android Studio untuk pemrosesan audio mikrofon real-time dengan preset Normal, Deep, High, Robot, Monster, kontrol pitch sederhana, dan kekuatan efek.

## Cara membuka
1. Ekstrak ZIP.
2. Buka Android Studio → Open → pilih folder `VoiceChangerMLBB`.
3. Tunggu Gradle sync.
4. Sambungkan HP Android (USB debugging aktif) lalu tekan Run.

## Batasan penting
- Ini adalah **prototype sumber kode**, bukan APK yang sudah ditandatangani.
- Efek saat ini adalah DSP sederhana; belum meniru suara dari audio contoh atau melakukan voice cloning.
- Android umumnya tidak mengizinkan satu aplikasi mengalihkan hasil pemrosesan mic menjadi input mic aplikasi lain seperti Mobile Legends. Jadi tombol MULAI MIC menguji pemrosesan/monitor audio di aplikasi ini, bukan menjamin suara berubah di voice chat MLBB.
- Untuk pemakaian di MLBB, cek apakah OPPO Game Toolkit di perangkatmu menyediakan fitur Voice Changer. Ketersediaan bergantung versi ColorOS, perangkat, dan game.
- Hindari memakai speaker keras saat tes karena bisa menimbulkan feedback. Gunakan headset.

## Build APK via GitHub Actions (tanpa install Android Studio di HP)
1. Buat repository GitHub baru dan upload isi folder project ini.
2. Buka tab **Actions** di repository, pilih **Build Android APK**, lalu **Run workflow** (atau tunggu build otomatis setelah push ke `main`).
3. Setelah selesai, buka hasil run dan unduh artifact **VoiceChangerMLBB-debug-apk**.
4. Ekstrak ZIP artifact, lalu pindahkan `app-debug.apk` ke HP dan instal. Android mungkin meminta izin **Install unknown apps** untuk aplikasi yang dipakai membuka APK.

APK debug ini untuk pengujian, bukan rilis Play Store. Workflow membutuhkan akun GitHub dan akses internet saat proses build.
