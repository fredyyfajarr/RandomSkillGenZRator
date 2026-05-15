package id.kelompok1.randomskillgen_zrator.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import id.kelompok1.randomskillgen_zrator.data.FirebaseSyncManager;
import id.kelompok1.randomskillgen_zrator.domain.StreakCalculator;

public class AppRepository {

    private static volatile AppRepository INSTANCE;

    private final AppDao dao;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private AppRepository(Context context) {
        dao = AppDatabase.getInstance(context.getApplicationContext()).appDao();
    }

    public static AppRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppRepository(context);
                }
            }
        }

        return INSTANCE;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    // -------------------------------------------------------------------------
    // USER
    // -------------------------------------------------------------------------
    public User getUser(String uid) {
        return dao.getUser(uid);
    }

    public void insertUserIfAbsent(User user) {
        dao.insertUser(user);
    }

    public void updateUser(User user) {
        dao.updateUser(user);
    }

    public User ensureUserExists(String uid) {
        User user = dao.getUser(uid);

        if (user == null) {
            user = new User(uid);
            dao.insertUser(user);
        }

        return user;
    }

    public void saveOrUpdateCloudUserToLocal(
            String uid,
            int level,
            int xp,
            int streak,
            int bestStreak,
            int totalActiveDays,
            String lastActiveDate
    ) {
        User user = dao.getUser(uid);
        boolean isNewUser = false;

        if (user == null) {
            user = new User(uid);
            isNewUser = true;
        }

        user.level = level;
        user.xp = xp;
        user.streak = streak;
        user.best_streak = bestStreak;
        user.total_active_days = totalActiveDays;
        user.last_active_date = lastActiveDate;

        if (isNewUser) {
            dao.insertUser(user);
        } else {
            dao.updateUser(user);
        }
    }

    public void saveCloudUserIfNotStale(String uid, FirebaseSyncManager.CloudUser cloudUser) {
        if (cloudUser == null) return;

        User local = dao.getUser(uid);
        if (!StreakCalculator.shouldUseCloudUser(local, cloudUser.lastActiveDate)) {
            return;
        }

        if (local != null && local.last_active_date != null
                && local.last_active_date.equals(cloudUser.lastActiveDate)) {
            local.level = Math.max(local.level, cloudUser.level);
            local.xp = Math.max(local.xp, cloudUser.xp);
            local.streak = Math.max(local.streak, cloudUser.streak);
            local.best_streak = Math.max(local.best_streak, cloudUser.bestStreak);
            local.total_active_days = Math.max(local.total_active_days, cloudUser.totalActiveDays);
            dao.updateUser(local);
            return;
        }

        saveOrUpdateCloudUserToLocal(
                uid,
                cloudUser.level,
                cloudUser.xp,
                cloudUser.streak,
                cloudUser.bestStreak,
                cloudUser.totalActiveDays,
                cloudUser.lastActiveDate
        );
    }

    public void saveCloudUserIfLocalMissing(
            String uid,
            int level,
            int xp,
            int streak,
            int bestStreak,
            int totalActiveDays
    ) {
        saveOrUpdateCloudUserToLocal(
                uid,
                level,
                xp,
                streak,
                bestStreak,
                totalActiveDays,
                null
        );
    }

    public void saveCloudUserIfLocalMissing(String uid, int level, int xp, int streak) {
        saveOrUpdateCloudUserToLocal(
                uid,
                level,
                xp,
                streak,
                streak,
                0,
                null
        );
    }

    public void resetJourneyForUser(String uid) {
        dao.deleteAllHistoryForUser(uid);

        User user = dao.getUser(uid);
        if (user != null) {
            user.level = 1;
            user.xp = 0;
            user.streak = 0;
            user.best_streak = 0;
            user.total_active_days = 0;
            user.last_active_date = null;
            dao.updateUser(user);
        }

        dao.lockAllAchievements();
    }

    // -------------------------------------------------------------------------
    // SKILL
    // -------------------------------------------------------------------------
    public Skill getSkillById(int id) {
        return dao.getSkillById(id);
    }

    public Skill getRandomSkill() {
        return dao.getRandomSkill();
    }

    public Skill getUnusedRandomSkill(String date, String uid) {
        return dao.getUnusedRandomSkillForUser(date, uid);
    }

    public Skill getSkillByTitleForUser(String title, String uid) {
        return dao.getSkillByTitleForUser(title, uid);
    }

    public Skill getSkillByTitleForUserOrGlobal(String title, String uid) {
        return dao.getSkillByTitleForUserOrGlobal(title, uid);
    }

    public void addCustomSkill(String uid, String title, String category, int xpReward) {
        dao.insertSkill(new Skill(
                title,
                category,
                xpReward,
                Skill.inferDifficulty(xpReward),
                Skill.inferDuration(xpReward),
                uid,
                true
        ));
    }

    public void addCustomSkill(String title, String category, int xpReward) {
        dao.insertSkill(new Skill(title, category, xpReward));
    }

    public void seedDefaultSkillsIfEmpty() {
        if (dao.getSkillsCount() >= 20) return;

        List<Skill> seeds = new ArrayList<>();

        seeds.add(new Skill("Minum 1 gelas air putih sekarang", SkillCategory.HEALTH, 20));
        seeds.add(new Skill("Cuci muka biar seger", SkillCategory.HEALTH, 20));
        seeds.add(new Skill("Stretching leher & bahu 2 menit", SkillCategory.HEALTH, 30));
        seeds.add(new Skill("Tarik napas dalam 10 kali", SkillCategory.HEALTH, 30));
        seeds.add(new Skill("Jalan kaki ringan 5 menit", SkillCategory.HEALTH, 50));
        seeds.add(new Skill("Jauhkan mata dari layar selama 5 menit", SkillCategory.HEALTH, 40));

        seeds.add(new Skill("Rapikan kasur", SkillCategory.PRODUCTIVE, 50));
        seeds.add(new Skill("Buang 1 sampah di sekitarmu", SkillCategory.PRODUCTIVE, 20));
        seeds.add(new Skill("Rapikan meja/tempat kerjamu", SkillCategory.PRODUCTIVE, 40));
        seeds.add(new Skill("Charge HP sampai penuh", SkillCategory.PRODUCTIVE, 20));
        seeds.add(new Skill("Hapus 5 foto/video ga penting di galeri", SkillCategory.PRODUCTIVE, 40));
        seeds.add(new Skill("Balas chat yang belum dibalas", SkillCategory.PRODUCTIVE, 50));

        seeds.add(new Skill("Dengerin 1 lagu favorit tanpa di-skip", SkillCategory.FUN, 30));
        seeds.add(new Skill("Tonton 1 video lucu di sosmed", SkillCategory.FUN, 20));
        seeds.add(new Skill("Tulis 1 hal yang disyukuri hari ini", SkillCategory.FUN, 50));
        seeds.add(new Skill("Kasih komentar positif di post orang", SkillCategory.FUN, 40));
        seeds.add(new Skill("Makan cemilan favoritmu", SkillCategory.FUN, 30));

        seeds.add(new Skill("Baca 1 artikel acak di Wikipedia", SkillCategory.EDUCATION, 50));
        seeds.add(new Skill("Tonton 1 video edukasi pendek (YouTube Shorts/TikTok)", SkillCategory.EDUCATION, 40));
        seeds.add(new Skill("Hafalkan 1 kosa kata bahasa asing baru", SkillCategory.EDUCATION, 50));

        dao.insertAll(seeds);
    }

    // -------------------------------------------------------------------------
    // DAILY QUEST / HISTORY
    // -------------------------------------------------------------------------
    public List<DailySkill> getDailySkillsByDate(String date, String uid) {
        return dao.getDailySkillsByDate(date, uid);
    }

    public DailySkill getDailySkillByDateAndSkill(String uid, String date, int skillId) {
        return dao.getDailySkillByDateAndSkill(uid, date, skillId);
    }

    public int getCompletedQuestsCount(String date, String uid) {
        return dao.getCompletedQuestsCount(date, uid);
    }

    public int getFinishedQuestsCount(String date, String uid) {
        return dao.getFinishedQuestsCount(date, uid);
    }

    public void insertDailySkill(DailySkill dailySkill) {
        dao.insertDailySkill(dailySkill);
    }

    public void insertDailySkillIfMissing(DailySkill dailySkill) {
        DailySkill existing = dao.getDailySkillByDateAndSkill(
                dailySkill.firebase_uid,
                dailySkill.date,
                dailySkill.skill_id
        );

        if (existing == null) {
            dao.insertDailySkill(dailySkill);
        }
    }

    public void updateDailySkill(DailySkill dailySkill) {
        dao.updateDailySkill(dailySkill);
    }

    public void skipDailyQuest(DailySkill record) {
        dao.updateDailySkill(record);
    }

    public List<DailySkill> getAllDailySkills(String uid) {
        return dao.getAllDailySkills(uid);
    }

    public void completeDailyQuest(DailySkill record, User user) {
        dao.completeDailyQuest(record, user);
    }

    public HistoryResult getCompletedHistoryWithSkills(String uid) {
        List<DailySkill> completed = dao.getCompletedDailySkills(uid);
        Map<Integer, Skill> skillMap = new HashMap<>();

        if (completed != null && !completed.isEmpty()) {
            Set<Integer> skillIds = new HashSet<>();
            for (DailySkill record : completed) {
                skillIds.add(record.skill_id);
            }

            List<Skill> skills = dao.getSkillsByIds(new ArrayList<>(skillIds));
            if (skills != null) {
                for (Skill skill : skills) {
                    skillMap.put(skill.id, skill);
                }
            }
        }

        return new HistoryResult(completed != null ? completed : new ArrayList<>(), skillMap);
    }

    // -------------------------------------------------------------------------
    // STATS + ACHIEVEMENT PROGRESS
    // -------------------------------------------------------------------------
    public StatsResult getStatsForUser(String uid) {
        User user = dao.getUser(uid);
        int currentXp = user != null ? user.xp : 0;

        Map<String, Integer> categoryCount = new HashMap<>();

        List<CategoryCount> counts = dao.getCompletedQuestCountByCategoryForUser(uid);
        if (counts != null) {
            for (CategoryCount item : counts) {
                if (item.category != null && item.count > 0) {
                    categoryCount.put(item.category, item.count);
                }
            }
        }

        int totalCompleted = dao.getTotalCompletedQuestCount(uid);
        return new StatsResult(totalCompleted, currentXp, categoryCount);
    }

    public void seedDefaultAchievementsIfEmpty() {
        if (dao.getAchievementCount() > 0) return;

        List<Achievement> achievements = new ArrayList<>();

        achievements.add(new Achievement(
                "first_flame",
                "First Flame",
                "Selesaikan quest pertamamu.",
                "🔥",
                false
        ));

        achievements.add(new Achievement(
                "triple_clear",
                "Triple Clear",
                "Selesaikan 3 quest total.",
                "⚔️",
                false
        ));

        achievements.add(new Achievement(
                "healthy_soul",
                "Healthy Soul",
                "Selesaikan 5 quest kategori Kesehatan.",
                "💪",
                false
        ));

        achievements.add(new Achievement(
                "productivity_demon",
                "Productivity Demon",
                "Selesaikan 5 quest kategori Produktif.",
                "⚡",
                false
        ));

        achievements.add(new Achievement(
                "scholar",
                "Scholar",
                "Selesaikan 5 quest kategori Edukasi.",
                "📚",
                false
        ));

        achievements.add(new Achievement(
                "fun_hunter",
                "Fun Hunter",
                "Selesaikan 5 quest kategori Fun.",
                "🎮",
                false
        ));

        dao.insertAchievements(achievements);
    }

    public List<Achievement> getAllAchievements() {
        seedDefaultAchievementsIfEmpty();
        return dao.getAllAchievements();
    }

    public List<Achievement> evaluateAchievements(String uid) {
        seedDefaultAchievementsIfEmpty();

        List<Achievement> newlyUnlocked = new ArrayList<>();

        int totalCompleted = dao.getTotalCompletedQuestCount(uid);

        maybeUnlock(newlyUnlocked, "first_flame", totalCompleted >= 1);
        maybeUnlock(newlyUnlocked, "triple_clear", totalCompleted >= 3);

        maybeUnlock(
                newlyUnlocked,
                "healthy_soul",
                dao.getCompletedQuestCountByCategory(uid, SkillCategory.HEALTH) >= 5
        );

        maybeUnlock(
                newlyUnlocked,
                "productivity_demon",
                dao.getCompletedQuestCountByCategory(uid, SkillCategory.PRODUCTIVE) >= 5
        );

        maybeUnlock(
                newlyUnlocked,
                "scholar",
                dao.getCompletedQuestCountByCategory(uid, SkillCategory.EDUCATION) >= 5
        );

        maybeUnlock(
                newlyUnlocked,
                "fun_hunter",
                dao.getCompletedQuestCountByCategory(uid, SkillCategory.FUN) >= 5
        );

        return newlyUnlocked;
    }

    private void maybeUnlock(List<Achievement> newlyUnlocked, String id, boolean condition) {
        if (!condition) return;

        Achievement achievement = dao.getAchievementById(id);
        if (achievement == null) return;

        if (!achievement.unlocked) {
            dao.unlockAchievement(id);
            achievement.unlocked = true;
            newlyUnlocked.add(achievement);
        }
    }

    public Map<String, AchievementProgress> getAchievementProgressMap(String uid) {
        Map<String, AchievementProgress> map = new HashMap<>();

        int totalCompleted = dao.getTotalCompletedQuestCount(uid);
        int health = dao.getCompletedQuestCountByCategory(uid, SkillCategory.HEALTH);
        int productive = dao.getCompletedQuestCountByCategory(uid, SkillCategory.PRODUCTIVE);
        int education = dao.getCompletedQuestCountByCategory(uid, SkillCategory.EDUCATION);
        int fun = dao.getCompletedQuestCountByCategory(uid, SkillCategory.FUN);

        map.put("first_flame", new AchievementProgress(Math.min(totalCompleted, 1), 1));
        map.put("triple_clear", new AchievementProgress(Math.min(totalCompleted, 3), 3));
        map.put("healthy_soul", new AchievementProgress(Math.min(health, 5), 5));
        map.put("productivity_demon", new AchievementProgress(Math.min(productive, 5), 5));
        map.put("scholar", new AchievementProgress(Math.min(education, 5), 5));
        map.put("fun_hunter", new AchievementProgress(Math.min(fun, 5), 5));

        return map;
    }

    // -------------------------------------------------------------------------
    // RESULT CLASSES
    // -------------------------------------------------------------------------
    public static class HistoryResult {
        public final List<DailySkill> records;
        public final Map<Integer, Skill> skillMap;

        public HistoryResult(List<DailySkill> records, Map<Integer, Skill> skillMap) {
            this.records = records;
            this.skillMap = skillMap;
        }
    }

    public static class StatsResult {
        public final int totalCompleted;
        public final int totalXp;
        public final Map<String, Integer> categoryCount;

        public StatsResult(int totalCompleted, int totalXp, Map<String, Integer> categoryCount) {
            this.totalCompleted = totalCompleted;
            this.totalXp = totalXp;
            this.categoryCount = categoryCount;
        }
    }

    public static class AchievementProgress {
        public final int current;
        public final int target;

        public AchievementProgress(int current, int target) {
            this.current = current;
            this.target = target;
        }
    }
}
