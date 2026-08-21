# MulyaSync (Catatan Harga Warung - P2P Local Tracker)

Aplikasi Android native (Kotlin + Jetpack Compose) untuk pencatatan dan pemantauan harga barang warung secara **offline** yang dapat tersinkronisasi antar beberapa perangkat Android secara otomatis melalui jaringan **Hotspot/WiFi lokal** tanpa memerlukan cloud server terpusat.

---

## 🛠️ Arsitektur & Teknologi

* **Bahasa:** Kotlin
* **UI:** Jetpack Compose + Material 3 (Material You / Dynamic Theme)
* **Local Persistence:** Room Database (SQLite) dengan UUID & Soft Delete
* **Dependency Injection:** Koin
* **P2P Discovery:** Android Network Service Discovery (`NsdManager` / mDNS)
* **P2P Local Server:** Embedded Ktor CIO Server di setiap perangkat (Port dinamis)
* **P2P Client:** Ktor CIO Client + `kotlinx.serialization`
* **Conflict Resolution:** Last-Write-Wins (LWW) berbasis timestamp epoch millis

---

## 📱 Fitur Utama

1. **Onboarding Nama Perangkat (Required):**
   * Pengguna wajib memasukkan nama identitas perangkat saat pertama kali membuka aplikasi (misal: *HP Kasir 1*, *HP Gudang*).
   * Nama tersimpan di `SharedPreferences` dan dapat diperbarui sewaktu-waktu via Settings.

2. **Master Kategori:**
   * Pengelolaan kategori barang warung (Sembako, Minuman, Rokok, Makanan Ringan, dll).
   * Validasi nama unik (mencegah duplikasi).
   * Sinkronisasi kategori antar-perangkat.

3. **Manajemen Barang & Harga (CRUD):**
   * Tambah, lihat, ubah harga/satuan, dan soft-delete barang.
   * Auto-generate UUID unik per item.

4. **Pencarian, Filter & Sort:**
   * Filter cepat berbasis chip horizontal per kategori.
   * Pencarian instan (search bar) berdasarkan nama barang.
   * Toggle pengurutan berdasarkan tanggal pembaruan (terbaru / terlama).

5. **Riwayat Perubahan Harga (Price History):**
   * Mencatat otomatis log perubahan harga (Harga Lama ➔ Harga Baru).
   * Menampilkan visual naik (merah) / turun (hijau).
   * Menampilkan perangkat mana yang melakukan perubahan dan waktu perubahannya.

6. **P2P Real-time Sync:**
   * Otomatis mendeteksi perangkat lain di Hotspot/WiFi yang sama via NSD.
   * Banner status interaktif ("X Perangkat Terhubung", waktu sync terakhir, tombol refresh manual).
   * Sinkronisasi berkala (background sync) setiap 10 detik.

---

## 📂 Struktur Direktori Proyek

```text
com.mulyasync.app/
├── MulyaSyncApp.kt
├── MainActivity.kt
│
├── data/
│   ├── local/
│   │   ├── DevicePreferences.kt
│   │   ├── MulyaSyncDatabase.kt
│   │   ├── dao/
│   │   │   ├── CategoryDao.kt
│   │   │   ├── ItemDao.kt
│   │   │   └── PriceHistoryDao.kt
│   │   └── entity/
│   │       ├── CategoryEntity.kt
│   │       ├── ItemEntity.kt
│   │       └── PriceHistoryEntity.kt
│   ├── mapper/
│   │   └── EntityMapper.kt
│   └── repository/
│       ├── CategoryRepositoryImpl.kt
│       ├── ItemRepositoryImpl.kt
│       └── SyncRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Category.kt
│   │   ├── Item.kt
│   │   ├── ItemFilter.kt
│   │   ├── PriceHistory.kt
│   │   └── SyncModels.kt
│   ├── repository/
│   │   ├── CategoryRepository.kt
│   │   ├── ItemRepository.kt
│   │   └── SyncRepository.kt
│   └── usecase/
│       ├── category/CategoryUseCases.kt
│       ├── item/ItemUseCases.kt
│       └── sync/SyncWithPeerUseCase.kt
│
├── network/
│   ├── discovery/NsdDiscoveryManager.kt
│   ├── dto/SyncDtos.kt
│   └── sync/
│       ├── SyncClient.kt
│       ├── SyncOrchestrator.kt
│       └── SyncServer.kt
│
├── presentation/
│   ├── components/
│   │   ├── CategoryFilterBar.kt
│   │   ├── EmptyStateView.kt
│   │   ├── ItemCard.kt
│   │   ├── SearchBar.kt
│   │   └── SyncStatusBanner.kt
│   ├── screen/
│   │   ├── additem/AddEditItemSheet.kt
│   │   ├── category/CategoryManagerSheet.kt
│   │   ├── history/PriceHistoryScreen.kt
│   │   ├── itemlist/ItemListScreen.kt
│   │   └── onboarding/OnboardingScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel/
│       └── MainViewModel.kt
│
└── di/
    └── AppModule.kt
```

---

## 🚀 Cara Menjalankan di Android Studio

1. Buka **Android Studio** (Koala / Ladybug atau versi terbaru).
2. Pilih **Open** dan arahkan ke direktori `/Users/cyanocream/Projects/mulya-sync`.
3. Tunggu hingga proses **Gradle Sync** selesai mengunduh dependencies.
4. Hubungkan 2 perangkat Android (atau 1 Emulator + 1 Real Device) ke WiFi/Hotspot lokal yang sama.
5. Jalankan aplikasi di kedua perangkat. Masukkan nama perangkat di onboarding, lalu aplikasi otomatis terhubung dan menyinkronkan data secara P2P.
