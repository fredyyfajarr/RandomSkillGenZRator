package id.kelompok1.randomskillgen_zrator.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * PERUBAHAN:
 * Tambah kolom last_active_date — dipakai untuk deteksi streak reset.
 * Jika selisih antara today dan last_active_date > 1 hari, streak di-reset ke 0.
 * Migration v3 → v4 menambahkan kolom ini tanpa menghapus data lama.
 */
@Entity(tableName = "User")
public class User {

    @PrimaryKey
    @NonNull
    public String firebase_uid;

    public int level  = 1;
    public int xp     = 0;
    public int streak = 0;

    @ColumnInfo(name = "best_streak")
    public int best_streak = 0;

    @ColumnInfo(name = "total_active_days")
    public int total_active_days = 0;

    /** Format: "yyyy-MM-dd". Null untuk user lama yang belum pernah update. */
    @ColumnInfo(name = "last_active_date")
    @Nullable
    public String last_active_date;

    public User(@NonNull String firebase_uid) {
        this.firebase_uid = firebase_uid;
    }
}