package id.kelompok1.randomskillgen_zrator.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                Skill.class,
                DailySkill.class,
                User.class,
                Achievement.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_DailySkill_firebase_uid_date " +
                            "ON DailySkill (firebase_uid, date)"
            );
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE User ADD COLUMN last_active_date TEXT");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS Achievement (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "title TEXT, " +
                            "description TEXT, " +
                            "icon TEXT, " +
                            "unlocked INTEGER NOT NULL DEFAULT 0" +
                            ")"
            );
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE DailySkill ADD COLUMN quest_status TEXT DEFAULT 'pending'");
            db.execSQL("ALTER TABLE DailySkill ADD COLUMN started_at TEXT");
            db.execSQL("ALTER TABLE DailySkill ADD COLUMN started_at_millis INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE DailySkill ADD COLUMN skipped_at TEXT");

            db.execSQL(
                    "UPDATE DailySkill SET quest_status = 'completed' " +
                            "WHERE is_completed = 1"
            );

            db.execSQL(
                    "UPDATE DailySkill SET quest_status = 'pending' " +
                            "WHERE is_completed = 0 AND quest_status IS NULL"
            );
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "genzrator_database"
                            )
                            .addMigrations(
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}