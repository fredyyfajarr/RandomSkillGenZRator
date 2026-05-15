package id.kelompok1.randomskillgen_zrator.data;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;
import id.kelompok1.randomskillgen_zrator.database.User;

public class FirebaseSyncManager {

    public interface UserCallback {
        void onSuccess(@Nullable CloudUser cloudUser);
        void onFailure(Exception error);
    }

    public interface QuestRestoreCallback {
        void onQuest(DocumentSnapshot document);
        void onComplete();
        void onFailure(Exception error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public static class CloudUser {
        public final int level;
        public final int xp;
        public final int streak;
        public final int bestStreak;
        public final int totalActiveDays;
        @Nullable
        public final String lastActiveDate;

        public CloudUser(
                int level,
                int xp,
                int streak,
                int bestStreak,
                int totalActiveDays,
                @Nullable String lastActiveDate
        ) {
            this.level = level;
            this.xp = xp;
            this.streak = streak;
            this.bestStreak = bestStreak;
            this.totalActiveDays = totalActiveDays;
            this.lastActiveDate = lastActiveDate;
        }
    }

    private final FirebaseFirestore db;

    public FirebaseSyncManager() {
        this(FirebaseFirestore.getInstance());
    }

    FirebaseSyncManager(FirebaseFirestore db) {
        this.db = db;
    }

    public void fetchUser(String uid, UserCallback callback) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onSuccess(null);
                        return;
                    }

                    int streak = safeInt(doc, "streak", 0);
                    callback.onSuccess(new CloudUser(
                            safeInt(doc, "level", 1),
                            safeInt(doc, "xp", 0),
                            streak,
                            safeInt(doc, "best_streak", streak),
                            safeInt(doc, "total_active_days", 0),
                            doc.getString("last_active_date")
                    ));
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void restoreDailyQuests(String uid, QuestRestoreCallback callback) {
        db.collection("users")
                .document(uid)
                .collection("daily_quests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            callback.onQuest(doc);
                        }
                    }

                    callback.onComplete();
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveUser(String uid, User user) {
        saveUser(uid, user, null);
    }

    public void saveUser(String uid, User user, @Nullable OperationCallback callback) {
        if (uid == null || user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("level", user.level);
        updates.put("xp", user.xp);
        updates.put("streak", user.streak);
        updates.put("best_streak", user.best_streak);
        updates.put("total_active_days", user.total_active_days);
        updates.put("last_active_date", user.last_active_date);
        updates.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onFailure(error);
                });
    }

    public void saveDailyProgress(String uid, String date, int finishedCount) {
        if (uid == null || date == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("completed_count", finishedCount);
        data.put("finished_count", finishedCount);
        data.put("is_day_finished", finishedCount >= 3);
        data.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .collection("daily_progress")
                .document(date)
                .set(data);
    }

    public void saveDailyQuest(String uid, DailySkill record, Skill skill, int slot) {
        if (uid == null || uid.trim().isEmpty()) return;
        if (record == null || skill == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("date", record.date);
        data.put("slot", slot);
        data.put("skill_title", skill.title);
        data.put("category", skill.category);
        data.put("xp_reward", skill.xp_reward);
        data.put("is_completed", record.is_completed);
        data.put("completed_at", record.completed_at);
        data.put("difficulty", skill.difficulty != null ? skill.difficulty : Skill.MEDIUM);
        data.put("duration_minutes", skill.duration_minutes > 0 ? skill.duration_minutes : 5);
        data.put("quest_status", record.quest_status != null ? record.quest_status : DailySkill.STATUS_PENDING);
        data.put("started_at", record.started_at);
        data.put("started_at_millis", record.started_at_millis);
        data.put("skipped_at", record.skipped_at);
        data.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .collection("daily_quests")
                .document(record.date + "_" + slot)
                .set(data);
    }

    public void resetTodayProgress(String uid, String today) {
        saveDailyProgress(uid, today, 0);

        for (int slot = 1; slot <= 3; slot++) {
            db.collection("users")
                    .document(uid)
                    .collection("daily_quests")
                    .document(today + "_" + slot)
                    .delete();
        }
    }

    public void deleteAllDailyQuestBackups(String uid) {
        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .collection("daily_quests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) return;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }

    private int safeInt(DocumentSnapshot doc, String field, int defaultValue) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : defaultValue;
    }
}
