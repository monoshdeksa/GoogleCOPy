# SalinOrder (`GoogleCOPy`)

Aplikasi Android berbasis **Jetpack Compose** dan **ML Kit Text Recognition** untuk memindai tangkap layar pesanan driver ShopeeFood serta menyalin **No. Pesanan Penerima** secara instan (baik lewat pemilih gambar maupun menu *Share* Android).

## Informasi Proyek

| Properti | Nilai |
| --- | --- |
| Nama Aplikasi | `SalinOrder` |
| Application ID | `com.aistudio.salinorder.qxmvkp` |
| Versi | `1.0` (`versionCode = 1`) |
| `minSdk` / `targetSdk` / `compileSdk` | `24` / `36` / `36` (`minorApiLevel = 1`) |
| Android Gradle Plugin (AGP) | `9.1.1` |
| Kotlin | `2.4.21` |
| Gradle | `9.3.1` |
| Java Target | Java 11 (disarankan **JDK 17** atau **JDK 21** untuk menjalankan Gradle 9.3.1 / AGP 9.1.1) |

---

## Prasyarat Sebelum Build APK Debug

1. **Git** dan **GitHub CLI (`gh`)** sudah terpasang dan login (`gh auth status`).
2. **JDK 17 atau JDK 21** (`java -version` dan `keytool` tersedia di `PATH`).
3. **Android SDK** (API 36) beserta `ANDROID_HOME` / `local.properties` (otomatis tersedia jika menggunakan Android Studio).
4. **Gradle `9.3.1`** (atau buka proyek di Android Studio).
   > **Catatan:** Repository ini hanya menyertakan `gradle/wrapper/gradle-wrapper.properties` dan belum menyertakan skrip `gradlew` / `gradlew.bat`. Anda bisa menjalankan `gradle` secara langsung atau membuat wrapper terlebih dahulu dengan perintah:
   > ```bash
   > gradle wrapper --gradle-version 9.3.1
   > ```

---

## Langkah 1 — Buat `debug.keystore` (Wajib)

Pada `app/build.gradle.kts`, konfigurasi `signingConfigs.debugConfig` mengharuskan file `debug.keystore` berada di root folder proyek (`${rootDir}/debug.keystore`), sementara file tersebut diabaikan oleh `.gitignore`.

Jika `debug.keystore` belum ada di root folder proyek, buat terlebih dahulu:

### Windows (PowerShell)

```powershell
keytool -genkey -v `
  -keystore debug.keystore `
  -storepass android `
  -alias androiddebugkey `
  -keypass android `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -dname "CN=Android Debug,O=Android,C=US"
```

### macOS / Linux

```bash
keytool -genkey -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

> **Jangan commit `debug.keystore` ke Git** — file ini sudah tercantum di `.gitignore`.

---

## Langkah 2 — Build APK Debug

Jalankan perintah berikut dari root folder proyek:

```bash
# Jika menggunakan Gradle yang terpasang di sistem
gradle assembleDebug

# Atau jika sudah membuat Gradle Wrapper
./gradlew assembleDebug        # macOS / Linux
.\gradlew.bat assembleDebug    # Windows PowerShell
```

Setelah build berhasil, file APK debug berada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Langkah 3 — Push / Upload APK Debug ke GitHub

> **Penting:** Folder `app/build/` diabaikan oleh `app/.gitignore`. Jangan memaksa commit folder `app/build/`. Pilih salah satu cara berikut sesuai kebutuhan Anda.

### Cara 1 (Direkomendasikan): Upload ke GitHub Releases via `gh`

Cara ini paling rapi karena file APK tidak memperbesar riwayat Git dan langsung menghasilkan tautan unduh untuk dipasang di HP Android.

1. Pastikan sudah login ke GitHub CLI:
   ```bash
   gh auth status
   ```
2. Buat release baru sekaligus unggah APK debug:
   ```bash
   gh release create v1.0-debug app/build/outputs/apk/debug/app-debug.apk \
     --title "SalinOrder v1.0 (Debug APK)" \
     --notes "Build APK debug untuk pengujian." \
     --prerelease
   ```
3. Jika ingin memperbarui APK di tag release yang sama:
   ```bash
   gh release upload v1.0-debug app/build/outputs/apk/debug/app-debug.apk --clobber
   ```

Untuk versi Windows PowerShell (satu baris):

```powershell
gh release create v1.0-debug app/build/outputs/apk/debug/app-debug.apk --title "SalinOrder v1.0 (Debug APK)" --notes "Build APK debug untuk pengujian." --prerelease
```

---

### Cara 2: Push APK Debug Langsung ke Branch Repository

Gunakan cara ini jika Anda ingin file `.apk` tersimpan langsung di dalam folder repository GitHub.

#### Windows (PowerShell)

```powershell
# 1. Buat folder khusus di luar app/build
New-Item -ItemType Directory -Force -Path apk | Out-Null

# 2. Salin hasil build APK debug
Copy-Item app\build\outputs\apk\debug\app-debug.apk apk\SalinOrder-debug.apk -Force

# 3. Commit dan push ke GitHub
git add apk/SalinOrder-debug.apk
git commit -m "chore: push debug APK"
git push origin main
```

#### macOS / Linux

```bash
mkdir -p apk
cp app/build/outputs/apk/debug/app-debug.apk apk/SalinOrder-debug.apk
git add apk/SalinOrder-debug.apk
git commit -m "chore: push debug APK"
git push origin main
```

---

### Cara 3 (Otomatis): Build & Publish APK Debug via GitHub Actions

Repository ini sudah dilengkapi workflow [`.github/workflows/build-debug-apk.yml`](.github/workflows/build-debug-apk.yml) yang otomatis berjalan setiap kali ada `git push` ke branch `main` atau dijalankan manual lewat tab **Actions → Build & Publish Debug APK → Run workflow**.

Workflow ini otomatis:
1. Menyiapkan **JDK 21** dan **Gradle 9.3.1**.
2. Membuat `debug.keystore` sementara di runner CI.
3. Menjalankan `gradle assembleDebug`.
4. Mengunggah `SalinOrder-debug.apk` ke dua tempat sekaligus:
   - **Tab Actions → Build & Publish Debug APK → Artifacts** (`SalinOrder-debug-apk`).
   - **Tab Releases → `debug-latest`** (tautan unduh langsung file `.apk` untuk dipasang di HP Android).

---

## Troubleshooting

- **`Keystore file '.../debug.keystore' not found for signing config 'debugConfig'`**
  Jalankan perintah `keytool` pada [Langkah 1](#langkah-1--buat-debugkeystore-wajib) di root folder proyek.
- **`./gradlew` atau `gradlew.bat` tidak ditemukan**
  Jalankan `gradle assembleDebug` langsung menggunakan Gradle `9.3.1`, atau buat wrapper dulu dengan `gradle wrapper --gradle-version 9.3.1`.
- **`app-debug.apk` tidak muncul di `git status`**
  `app/build/` diabaikan oleh `app/.gitignore`. Gunakan **GitHub Releases** (Cara 1) atau salin APK ke folder terpisah seperti `apk/` (Cara 2).
- **Gagal `git push` / autentikasi ditolak**
  Jalankan `gh auth status` untuk memastikan login aktif, lalu jalankan `gh auth setup-git`.
