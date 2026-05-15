package id.kelompok1.randomskillgen_zrator;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import id.kelompok1.randomskillgen_zrator.database.Achievement;
import id.kelompok1.randomskillgen_zrator.database.AppRepository;
import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;
import id.kelompok1.randomskillgen_zrator.database.SkillCategory;
import id.kelompok1.randomskillgen_zrator.database.SyncState;
import id.kelompok1.randomskillgen_zrator.database.User;

public class MainViewModel extends AndroidViewModel {

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<Skill> currentSkill = new MutableLiveData<>();
    private final MutableLiveData<DailySkill> currentRecord = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showSuccessDialogEvent = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> dailyProgress = new MutableLiveData<>(0);
    private final MutableLiveData<String> dailyQuote = new MutableLiveData<>();
    private final MutableLiveData<SyncState> syncState = new MutableLiveData<>(SyncState.IDLE);
    private final MutableLiveData<Boolean> streakBonusActive = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> todayFinishedFromCloud = new MutableLiveData<>(false);
    private final MutableLiveData<Achievement> achievementUnlockedEvent = new MutableLiveData<>();

    private final AppRepository repo;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int lastGainedXp = 0;

    @Nullable
    private Runnable pendingNextQuestLoad;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repo = AppRepository.getInstance(application);
        executor = repo.getExecutor();
        fetchDynamicQuote();
    }

    public LiveData<User> getUser() {
        return user;
    }

    public LiveData<Skill> getCurrentSkill() {
        return currentSkill;
    }

    public LiveData<DailySkill> getTodayRecord() {
        return currentRecord;
    }

    public LiveData<Boolean> getShowSuccessDialogEvent() {
        return showSuccessDialogEvent;
    }

    public LiveData<Integer> getDailyProgress() {
        return dailyProgress;
    }

    public int getLastGainedXp() {
        return lastGainedXp;
    }

    public LiveData<String> getDailyQuote() {
        return dailyQuote;
    }

    public LiveData<SyncState> getSyncState() {
        return syncState;
    }

    public LiveData<Boolean> getStreakBonusActive() {
        return streakBonusActive;
    }

    public LiveData<Boolean> getTodayFinishedFromCloud() {
        return todayFinishedFromCloud;
    }

    public LiveData<Achievement> getAchievementUnlockedEvent() {
        return achievementUnlockedEvent;
    }

    public void resetAchievementUnlockedEvent() {
        achievementUnlockedEvent.setValue(null);
    }

    public void resetSuccessDialogEvent() {
        showSuccessDialogEvent.setValue(false);
    }

    public void loadHomeData() {
        executor.execute(() -> {
            String uid = currentUid();
            if (uid == null) return;

            repo.seedDefaultSkillsIfEmpty();
            repo.seedDefaultAchievementsIfEmpty();

            syncUserAndHistoryFromCloudThenLoad(uid);
        });
    }

    private void syncUserAndHistoryFromCloudThenLoad(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    executor.execute(() -> {
                        if (userDoc.exists()) {
                            int cloudLevel = safeInt(userDoc, "level", 1);
                            int cloudXp = safeInt(userDoc, "xp", 0);
                            int cloudStreak = safeInt(userDoc, "streak", 0);
                            int cloudBestStreak = safeInt(userDoc, "best_streak", cloudStreak);
                            int cloudTotalActiveDays = safeInt(userDoc, "total_active_days", 0);
                            String cloudLastActiveDate = userDoc.getString("last_active_date");

                            repo.saveOrUpdateCloudUserToLocal(
                                    uid,
                                    cloudLevel,
                                    cloudXp,
                                    cloudStreak,
                                    cloudBestStreak,
                                    cloudTotalActiveDays,
                                    cloudLastActiveDate
                            );
                        }

                        restoreAllDailyQuestsFromCloud(uid);
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MainViewModel", "Sync user cloud failed", e);
                    executor.execute(() -> loadLocalHomeData(uid));
                });
    }

    private void restoreAllDailyQuestsFromCloud(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("daily_quests")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            restoreOneDailyQuestFromDoc(uid, doc);
                        }
                    }

                    loadLocalHomeData(uid);
                }))
                .addOnFailureListener(e -> {
                    android.util.Log.e("MainViewModel", "Restore all daily quests failed", e);
                    executor.execute(() -> loadLocalHomeData(uid));
                });
    }

    private void restoreOneDailyQuestFromDoc(String uid, DocumentSnapshot doc) {
        String date = doc.getString("date");
        String title = doc.getString("skill_title");
        String category = doc.getString("category");
        String difficulty = doc.getString("difficulty");
        String completedAt = doc.getString("completed_at");

        String questStatus = doc.getString("quest_status");
        String startedAt = doc.getString("started_at");
        String skippedAt = doc.getString("skipped_at");

        Boolean completed = doc.getBoolean("is_completed");
        Long xpLong = doc.getLong("xp_reward");
        Long startedAtMillisLong = doc.getLong("started_at_millis");

        if (date == null || date.trim().isEmpty()) return;
        if (title == null || title.trim().isEmpty()) return;

        if (category == null) category = SkillCategory.FUN;
        if (difficulty == null) difficulty = Skill.MEDIUM;

        int xp = xpLong != null ? xpLong.intValue() : SkillCategory.getXpForCategory(category);

        Skill skill = repo.getSkillByTitleForUserOrGlobal(title, uid);

        if (skill == null) {
            repo.addCustomSkill(uid, title, category, xp);
            skill = repo.getSkillByTitleForUserOrGlobal(title, uid);
        }

        if (skill == null) return;

        DailySkill existing = repo.getDailySkillByDateAndSkill(uid, date, skill.id);
        if (existing != null) return;

        DailySkill restored = new DailySkill(
                uid,
                skill.id,
                date,
                completed != null && completed
        );

        restored.completed_at = completedAt;
        restored.quest_status = questStatus != null
                ? questStatus
                : (restored.is_completed ? DailySkill.STATUS_COMPLETED : DailySkill.STATUS_PENDING);
        restored.started_at = startedAt;
        restored.skipped_at = skippedAt;
        restored.started_at_millis = startedAtMillisLong != null ? startedAtMillisLong : 0L;

        repo.insertDailySkill(restored);
    }

    private void loadLocalHomeData(String uid) {
        String today = todayDate();

        User u = repo.ensureUserExists(uid);
        u = checkAndResetStreakIfNeeded(u, today);

        user.postValue(u);

        boolean isBonus = u.streak > 0 && u.streak % 7 == 0;
        streakBonusActive.postValue(isBonus);

        List<DailySkill> todayQuests = repo.getDailySkillsByDate(today, uid);
        int finishedCount = repo.getFinishedQuestsCount(today, uid);

        dailyProgress.postValue(finishedCount);

        if (todayQuests == null || todayQuests.isEmpty()) {
            generateNewDailySkill(today, uid);
            return;
        }

        todayFinishedFromCloud.postValue(false);

        DailySkill activeQuest = null;

        for (DailySkill ds : todayQuests) {
            boolean alreadyFinished =
                    ds.is_completed ||
                            DailySkill.STATUS_COMPLETED.equals(ds.quest_status) ||
                            DailySkill.STATUS_SKIPPED.equals(ds.quest_status);

            if (!alreadyFinished) {
                activeQuest = ds;
                break;
            }
        }

        if (activeQuest == null && finishedCount < 3) {
            generateNewDailySkill(today, uid);
        } else if (activeQuest != null) {
            currentRecord.postValue(activeQuest);
            currentSkill.postValue(repo.getSkillById(activeQuest.skill_id));
        } else {
            DailySkill last = todayQuests.get(todayQuests.size() - 1);
            currentRecord.postValue(last);
            currentSkill.postValue(repo.getSkillById(last.skill_id));
            dailyProgress.postValue(3);
            todayFinishedFromCloud.postValue(true);
        }
    }

    private User checkAndResetStreakIfNeeded(User u, String today) {
        if (u.last_active_date == null) {
            u.last_active_date = today;
            repo.updateUser(u);
            return u;
        }

        if (u.last_active_date.equals(today)) return u;

        long dayDiff = daysBetween(u.last_active_date, today);

        if (dayDiff > 1) {
            u.streak = 0;
            u.last_active_date = today;
            repo.updateUser(u);
            syncToFirestore(u.firebase_uid, u);
        }

        return u;
    }

    private long daysBetween(String from, String to) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d1 = sdf.parse(from);
            Date d2 = sdf.parse(to);

            if (d1 == null || d2 == null) return -1;

            return (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24);
        } catch (ParseException e) {
            return -1;
        }
    }

    private void generateNewDailySkill(String date, String uid) {
        int finishedCount = repo.getFinishedQuestsCount(date, uid);

        if (finishedCount >= 3) {
            dailyProgress.postValue(3);
            todayFinishedFromCloud.postValue(true);
            return;
        }

        Skill randomSkill = repo.getUnusedRandomSkill(date, uid);

        if (randomSkill == null) randomSkill = repo.getRandomSkill();
        if (randomSkill == null) return;

        DailySkill newRecord = new DailySkill(uid, randomSkill.id, date, false);
        newRecord.quest_status = DailySkill.STATUS_PENDING;

        repo.insertDailySkill(newRecord);

        List<DailySkill> freshQuests = repo.getDailySkillsByDate(date, uid);
        DailySkill realRecord = freshQuests.get(freshQuests.size() - 1);

        currentRecord.postValue(realRecord);
        currentSkill.postValue(randomSkill);
        todayFinishedFromCloud.postValue(false);

        int slot = freshQuests.size();
        backupDailyQuestToFirestore(uid, realRecord, randomSkill, slot);
    }

    public static int getRequiredXpForLevel(int level) {
        if (level <= 1) return 100;
        return 100 + ((level - 1) * 75);
    }

    public static boolean isStreakBonusDay(int nextStreak) {
        return nextStreak > 0 && nextStreak % 7 == 0;
    }

    public static int calculateRewardXp(int baseXp, int nextStreak) {
        return isStreakBonusDay(nextStreak) ? (int) (baseXp * 1.5f) : baseXp;
    }

    public void startTodaySkill() {
        DailySkill record = currentRecord.getValue();

        if (record == null) return;
        if (record.is_completed) return;
        if (DailySkill.STATUS_RUNNING.equals(record.quest_status)) return;
        if (DailySkill.STATUS_READY.equals(record.quest_status)) return;
        if (DailySkill.STATUS_SKIPPED.equals(record.quest_status)) return;

        executor.execute(() -> {
            String uid = currentUid();
            if (uid == null) return;

            record.quest_status = DailySkill.STATUS_RUNNING;
            record.started_at = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            record.started_at_millis = System.currentTimeMillis();

            repo.updateDailySkill(record);
            currentRecord.postValue(record);

            Skill s = repo.getSkillById(record.skill_id);
            if (s != null) {
                backupDailyQuestToFirestore(uid, record, s, calculateSlotForRecord(record));
            }
        });
    }

    public void markTodaySkillReadyToClaim() {
        DailySkill record = currentRecord.getValue();

        if (record == null) return;
        if (record.is_completed) return;
        if (DailySkill.STATUS_SKIPPED.equals(record.quest_status)) return;
        if (DailySkill.STATUS_READY.equals(record.quest_status)) return;

        executor.execute(() -> {
            String uid = currentUid();
            if (uid == null) return;

            record.quest_status = DailySkill.STATUS_READY;

            repo.updateDailySkill(record);
            currentRecord.postValue(record);

            Skill s = repo.getSkillById(record.skill_id);
            if (s != null) {
                backupDailyQuestToFirestore(uid, record, s, calculateSlotForRecord(record));
            }
        });
    }

    public void skipTodaySkill() {
        DailySkill record = currentRecord.getValue();

        if (record == null) return;
        if (record.is_completed) return;

        executor.execute(() -> {
            String uid = currentUid();
            if (uid == null) return;

            String today = todayDate();

            record.quest_status = DailySkill.STATUS_SKIPPED;
            record.is_completed = false;
            record.skipped_at = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            repo.skipDailyQuest(record);

            int finishedAfter = repo.getFinishedQuestsCount(today, uid);

            dailyProgress.postValue(finishedAfter);

            Skill s = repo.getSkillById(record.skill_id);
            if (s != null) {
                backupDailyQuestToFirestore(uid, record, s, calculateSlotForRecord(record));
            }

            backupDailyProgressToFirestore(uid, today, finishedAfter);

            if (finishedAfter < 3) {
                cancelPendingNextQuestLoad();
                pendingNextQuestLoad = this::loadHomeData;
                mainHandler.postDelayed(pendingNextQuestLoad, 700);
            } else {
                todayFinishedFromCloud.postValue(true);
                currentRecord.postValue(record);
            }
        });
    }

    public void completeTodaySkill() {
        DailySkill record = currentRecord.getValue();
        User u = user.getValue();

        if (record == null || u == null || record.is_completed) return;
        if (!DailySkill.STATUS_READY.equals(record.quest_status)) return;

        executor.execute(() -> {
            String uid = currentUid();
            if (uid == null) return;

            String today = todayDate();

            int finishedBefore = repo.getFinishedQuestsCount(record.date, uid);
            int finishedAfter = finishedBefore + 1;

            record.is_completed = true;
            record.quest_status = DailySkill.STATUS_COMPLETED;
            record.completed_at = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            Skill s = repo.getSkillById(record.skill_id);
            int baseXp = s != null ? s.xp_reward : 50;

            int nextStreak = u.streak + 1;
            lastGainedXp = calculateRewardXp(baseXp, nextStreak);

            u.xp += lastGainedXp;

            while (u.xp >= getRequiredXpForLevel(u.level)) {
                u.xp -= getRequiredXpForLevel(u.level);
                u.level++;
            }

            if (finishedAfter == 1) {
                u.streak++;
                u.total_active_days++;

                if (u.streak > u.best_streak) {
                    u.best_streak = u.streak;
                }

                u.last_active_date = today;

                boolean isBonus = u.streak > 0 && u.streak % 7 == 0;
                streakBonusActive.postValue(isBonus);
            }

            repo.completeDailyQuest(record, u);

            List<Achievement> newlyUnlocked = repo.evaluateAchievements(uid);
            if (newlyUnlocked != null && !newlyUnlocked.isEmpty()) {
                achievementUnlockedEvent.postValue(newlyUnlocked.get(0));
            }

            syncToFirestore(uid, u);
            backupDailyProgressToFirestore(uid, today, finishedAfter);

            if (s != null) {
                backupDailyQuestToFirestore(uid, record, s, calculateSlotForRecord(record));
            }

            user.postValue(u);
            showSuccessDialogEvent.postValue(true);
            dailyProgress.postValue(finishedAfter);

            if (finishedAfter < 3) {
                cancelPendingNextQuestLoad();
                pendingNextQuestLoad = this::loadHomeData;
                mainHandler.postDelayed(pendingNextQuestLoad, 1200);
            } else {
                todayFinishedFromCloud.postValue(true);
            }
        });
    }

    public void resetDatabase() {
        executor.execute(() -> {
            String uid = currentUid();
            if (uid == null) return;

            String today = todayDate();

            repo.resetJourneyForUser(uid);

            User u = repo.getUser(uid);
            if (u != null) {
                syncToFirestore(uid, u);
            }

            streakBonusActive.postValue(false);
            todayFinishedFromCloud.postValue(false);
            dailyProgress.postValue(0);
            currentRecord.postValue(null);
            currentSkill.postValue(null);

            resetTodayCloudProgress(uid, today);

            mainHandler.postDelayed(this::loadHomeData, 800);
        });
    }

    private void resetTodayCloudProgress(String uid, String today) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> progress = new HashMap<>();
        progress.put("date", today);
        progress.put("completed_count", 0);
        progress.put("finished_count", 0);
        progress.put("is_day_finished", false);
        progress.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .collection("daily_progress")
                .document(today)
                .set(progress);

        for (int slot = 1; slot <= 3; slot++) {
            String docId = today + "_" + slot;

            db.collection("users")
                    .document(uid)
                    .collection("daily_quests")
                    .document(docId)
                    .delete();
        }
    }

    private void backupDailyProgressToFirestore(String uid, String today, int finishedCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("date", today);
        data.put("completed_count", finishedCount);
        data.put("finished_count", finishedCount);
        data.put("is_day_finished", finishedCount >= 3);
        data.put("updated_at", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("daily_progress")
                .document(today)
                .set(data);
    }

    private void backupDailyQuestToFirestore(String uid, DailySkill record, Skill skill, int slot) {
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

        String docId = record.date + "_" + slot;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("daily_quests")
                .document(docId)
                .set(data);
    }

    private int calculateSlotForRecord(DailySkill record) {
        try {
            List<DailySkill> records = repo.getDailySkillsByDate(record.date, record.firebase_uid);
            if (records == null || records.isEmpty()) return 1;

            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).id == record.id) {
                    return i + 1;
                }
            }

            return records.size();
        } catch (Exception e) {
            return 1;
        }
    }

    private void syncToFirestore(String uid, User u) {
        syncState.postValue(SyncState.SYNCING);

        Map<String, Object> updates = new HashMap<>();
        updates.put("level", u.level);
        updates.put("xp", u.xp);
        updates.put("streak", u.streak);
        updates.put("best_streak", u.best_streak);
        updates.put("total_active_days", u.total_active_days);
        updates.put("last_active_date", u.last_active_date);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(v -> syncState.postValue(SyncState.SUCCESS))
                .addOnFailureListener(e -> {
                    android.util.Log.w("MainViewModel", "Firestore sync failed: " + e.getMessage());
                    syncState.postValue(SyncState.ERROR);
                });
    }

    private void fetchDynamicQuote() {
        executor.execute(() -> {
            try {
                java.net.URL url = new java.net.URL("https://zenquotes.io/api/random");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestMethod("GET");

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                reader.close();

                String json = sb.toString();
                String quote = json.split("\"q\":\"")[1].split("\"")[0];
                String author = json.split("\"a\":\"")[1].split("\"")[0];

                dailyQuote.postValue("✨ Scroll of Wisdom:\n\"" + quote + "\"\n— " + author);
            } catch (Exception e) {
                String[] fallbackQuotes = {
                        "✨ Mode Offline Aktif!\nQuest kecil hari ini, level besar nanti.",
                        "✨ Tetap fokus, Hero!\nKonsistensi kecil bisa jadi kekuatan besar.",
                        "✨ Jangan tunggu mood.\nMulai satu quest dulu, sisanya menyusul.",
                        "✨ Satu langkah kecil hari ini\nbisa jadi progress besar minggu depan."
                };

                int index = (int) (Math.random() * fallbackQuotes.length);
                dailyQuote.postValue(fallbackQuotes[index]);
            }
        });
    }

    @Nullable
    private String currentUid() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        return fUser != null ? fUser.getUid() : null;
    }

    private int safeInt(DocumentSnapshot doc, String field, int defaultValue) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : defaultValue;
    }

    private String todayDate() {
        android.content.SharedPreferences prefs =
                getApplication().getSharedPreferences("GenZPrefs", android.content.Context.MODE_PRIVATE);

        String debugDate = prefs.getString("debug_date", null);

        if (debugDate != null && !debugDate.trim().isEmpty()) {
            return debugDate;
        }

        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void cancelPendingNextQuestLoad() {
        if (pendingNextQuestLoad != null) {
            mainHandler.removeCallbacks(pendingNextQuestLoad);
            pendingNextQuestLoad = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelPendingNextQuestLoad();
    }
}