package id.kelompok1.randomskillgen_zrator.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Index komposit (firebase_uid, date) mempercepat query getDailySkillsByDate
 * dan getCompletedQuestsCount yang dipanggil tiap buka HomeFragment.
 */
@Entity(
        tableName = "DailySkill",
        indices = { @Index(value = {"firebase_uid", "date"}) }
)
public class DailySkill {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_SKIPPED = "skipped";

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String firebase_uid;
    public int skill_id;
    public String date;          // format: "yyyy-MM-dd"
    public boolean is_completed;

    @ColumnInfo(name = "completed_at")
    public String completed_at;

    @ColumnInfo(name = "quest_status")
    public String quest_status;

    @ColumnInfo(name = "started_at")
    public String started_at;

    @ColumnInfo(name = "started_at_millis")
    public long started_at_millis;

    @ColumnInfo(name = "skipped_at")
    public String skipped_at;

    public DailySkill(String firebase_uid, int skill_id, String date, boolean is_completed) {
        this.firebase_uid = firebase_uid;
        this.skill_id = skill_id;
        this.date = date;
        this.is_completed = is_completed;

        this.completed_at = null;
        this.started_at = null;
        this.started_at_millis = 0L;
        this.skipped_at = null;

        this.quest_status = is_completed
                ? STATUS_COMPLETED
                : STATUS_PENDING;
    }

    public boolean isPending() {
        return quest_status == null || STATUS_PENDING.equals(quest_status);
    }

    public boolean isRunning() {
        return STATUS_RUNNING.equals(quest_status);
    }

    public boolean isReady() {
        return STATUS_READY.equals(quest_status);
    }

    public boolean isSkipped() {
        return STATUS_SKIPPED.equals(quest_status);
    }

    public boolean isCompletedStatus() {
        return STATUS_COMPLETED.equals(quest_status) || is_completed;
    }

    public boolean isFinished() {
        return isCompletedStatus() || isSkipped();
    }
}