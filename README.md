# 🚀 Legacy Launcer Archive

<p align="center">
  <img src="https://img.shields.io/badge/version-1.170.0-blue" alt="Version">
  <img src="https://img.shields.io/badge/java-8%2B-orange" alt="Java">
  <img src="https://img.shields.io/badge/license-GPLv3-green" alt="License">
</p>

**Legacy Launcer Archive** adalah fork dari [Legacy Launcher](https://github.com/turikhay/LegacyLauncher) dengan tampilan **UI Anime Modern**, fitur **Auto-Optimize Low-End PC**, dan update checker terintegrasi.

> ✨ Dibuat untuk penggemar Minecraft yang ingin laucher cantik, ringan, dan optimal!

---

## ✨ Fitur Unggulan

### 🎨 Anime Theme UI
- Background anime dari URL internet (bisa kustom)
- Glassmorphism effect — transparan elegan
- Tombol neon dengan rounded corners
- Fully customizable wallpaper via Settings

### ⚡ Auto-Optimize Low-End PC
- **Auto-download** mod optimasi (Sodium, Lithium, FerriteCore, Starlight, dll)
- **Auto-configure** JVM arguments hemat RAM
- **Close launcher on game start** untuk hemat resource
- Khusus Fabric & Forge

### 🔔 Update Checker
- Bell icon notifikasi update terbaru
- Deteksi otomatis dari GitHub Releases
- Changelog preview langsung di launcher

### 🖥️ UI Modern
- Transparent blur background
- Rounded buttons with hover effects
- Cyber-Anime color scheme (Neon Blue, Soft Pink, Purple)
- Smooth animations

---

## 📦 Installasi

### Untuk Pengguna
1. Download installer terbaru dari **[GitHub Releases](https://github.com/tsukiforge/LegacyLauncer-archive/releases)**
2. Jalankan installer `.exe` (Windows) atau `.dmg` (macOS)
3. Buka launcher, atur skin anime di Settings!

### Build dari Source (Developers)

Proyek ini menggunakan **Gradle** + **Inno Setup** untuk build.

> ⚠️ **Catatan:** Build membutuhkan **Java 21**. Jangan gunakan Java 17 atau 26.

#### Di GitHub Actions (Otomatis — Rekomendasi)
Push tag ke GitHub, CI/CD akan build otomatis:
```bash
git tag v1.170.0
git push origin v1.170.0
```
Buka **Actions** tab di repo → workflow **Build Legacy Launcher** akan jalan.

#### Build Lokal (Manual)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.x.x-hotspot"
.\gradlew :launcher:build --no-daemon -x test
```

---

## 🛠️ Struktur Proyek

```
LegacyLauncer-archive/
├── launcher/          # Kode utama launcher (Java Swing)
├── bootstrap/         # Bootstrap loader
├── bridge/            # JNI bridge
├── common/            # Library bersama
├── utils/             # Utility classes
├── packages/          # Packaging scripts
│   ├── installer/     # Inno Setup installer (Windows)
│   ├── portable/      # Portable build
│   └── dmg/           # macOS DMG build
└── .github/workflows/ # CI/CD automation
```

---

## 🌐 Bahasa

| Bahasa | File |
|--------|------|
| 🇺🇸 English | `lang_en_US.properties` |
| 🇷🇺 Русский | `lang_ru_RU.properties` |

---

## 🧩 Dependensi

- Java 8+ (runtime)
- Gradle 8.14.2 (build)
- Java 21 (build system)
- Inno Setup 6+ (Windows installer)
- Docker (Linux/macOS installer build)

---

## 🙏 Kredit

- **Original:** [turikhay/LegacyLauncher](https://github.com/turikhay/LegacyLauncher) — Launcher Minecraft kustom ringan
- **Iris Shaders** — Inspirasi warna UI
- **Modrinth** — API untuk download mod optimasi

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah **GNU General Public License v3.0** — lihat [LICENSE.txt](LICENSE.txt) untuk detail.

---

<p align="center">
  <sub>Dibuat dengan ❤️ untuk Minecraft Indonesia</sub>
  <br>
  <sub>© 2026 Legacy Launcer Archive Team</sub>
</p>
