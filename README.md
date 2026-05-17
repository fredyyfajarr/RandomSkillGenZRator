# Random Skill Gen-ZRator

Random Skill Gen-ZRator adalah aplikasi Android Java untuk daily quest gamification. Aplikasi ini membantu pengguna menyelesaikan tantangan harian, mendapatkan XP, menaikkan level, menjaga streak, membuka achievement, melihat history, dan memantau statistik progres.

## Fitur Utama

- Google Sign-In dengan Firebase Authentication.
- Sinkronisasi cloud menggunakan Firebase Firestore.
- Penyimpanan lokal menggunakan Room Database.
- Arsitektur MVVM dengan ViewModel dan LiveData.
- Daily quest maksimal 3 quest per hari.
- Status quest: pending, running, ready, completed, dan skipped.
- Timer quest berdasarkan durasi challenge.
- XP, level, tier, streak, best streak, dan achievement.
- Chronicles untuk riwayat quest yang completed.
- Stats dengan chart kategori dan insight progres.
- WorkManager untuk pengingat harian.
- Dark mode.
- Custom Challenge untuk menambah challenge pribadi.

## Arsitektur Ringkas

Project ini memakai pendekatan clean architecture ringan agar tetap mudah dipahami untuk project Android Java mahasiswa.

- `presentation`: Activity, Fragment, ViewModel, dan Adapter.
- `database`: Entity Room, DAO, database, dan repository lokal.
- `data`: Firebase sync manager dan boundary sinkronisasi cloud.
- `domain`: Business logic seperti XP, streak, timer, tier label, dan validator.

Room menjadi sumber data utama untuk UI. Firestore bersifat best-effort sync, sehingga aplikasi tetap bisa berjalan saat koneksi cloud gagal.

## Flow Daily Quest

1. Quest muncul di halaman utama.
2. User menekan Start Quest.
3. Timer berjalan sesuai `duration_minutes`.
4. Saat timer selesai, quest berubah menjadi ready to claim.
5. User menekan Claim Reward.
6. Aplikasi meminta konfirmasi apakah quest benar-benar selesai.
7. Jika ya, XP masuk dan status menjadi completed.
8. Jika belum, quest menjadi skipped tanpa XP.
9. Quest berikutnya bisa dikerjakan kapan saja di hari yang sama.
10. Maksimal 3 quest per hari, dan skipped tetap memakai 1 slot harian.

## Custom Challenge

- User bisa membuat sampai 30 custom challenge per akun.
- Judul challenge dinormalisasi, di-trim, dan dicek duplikat secara case-insensitive.
- Difficulty menentukan timer dan reward XP:
  - Easy: 1 menit, XP kategori dikurangi 20, minimal 20 XP.
  - Medium: 3 menit, XP sesuai kategori.
  - Hard: 5 menit, XP kategori ditambah 30.
- Data custom challenge dari cloud disanitasi sebelum disimpan ke Room.
- Custom challenge yang sudah pernah masuk history tidak langsung dihapus permanen agar data riwayat tetap aman.

## Statistik dan History

- Stats hanya menghitung quest dengan status completed.
- Quest skipped tidak dihitung sebagai completed stats.
- Total XP di Stats berasal dari reward quest completed.
- Chronicles hanya menampilkan quest completed.
- History punya filter tanggal dan tombol Semua untuk reset filter.
- Stats menampilkan insight seperti kategori favorit, best streak, dan rata-rata XP per quest.

## Setup Project

1. Clone repository:

```powershell
git clone https://github.com/fredyyfajarr/RandomSkillGenZRator.git
```

2. Buka project di Android Studio.

3. Tambahkan file Firebase lokal:

```text
app/google-services.json
```

4. Sync Gradle.

5. Jalankan aplikasi dari Android Studio.

## Catatan Firebase

File `google-services.json` tidak disarankan untuk repository public. Simpan file asli hanya di lokal atau gunakan sample yang sudah disanitasi.

Rekomendasi bentuk dasar Firestore Security Rules:

```text
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;

  match /{document=**} {
    allow read, write: if request.auth != null && request.auth.uid == userId;
  }
}
```

Untuk production, rules sebaiknya dibuat lebih ketat dengan validasi field, tipe data, dan batasan nilai.

## Catatan Keamanan

- Jangan commit `google-services.json` asli ke repo public.
- Jangan hardcode token, secret, atau credential di source code.
- Jangan tampilkan UID, token, atau data sensitif di Logcat.
- Jangan simpan bukti foto/video jika fitur evidence belum dipakai.
- Batasi data Firestore agar tetap berada di bawah path `/users/{uid}`.

## Testing Manual yang Disarankan

- Login dengan Google Sign-In.
- Jalankan quest 1/3 sampai completed.
- Skip quest dan pastikan tidak masuk completed stats.
- Tutup aplikasi saat timer berjalan, buka lagi, lalu pastikan timer tetap valid.
- Buka Home, Chronicles, Stats, dan Guild Card/Profile.
- Cek dark mode.
- Buat, edit, dan hapus custom challenge.
- Cek History filter tanggal dan tombol Semua.
- Cek Stats setelah beberapa quest completed.

## Teknologi

- Java
- AndroidX
- Firebase Authentication
- Firebase Firestore
- Room Database
- ViewModel dan LiveData
- WorkManager
- MPAndroidChart
- Glide
- Lottie
