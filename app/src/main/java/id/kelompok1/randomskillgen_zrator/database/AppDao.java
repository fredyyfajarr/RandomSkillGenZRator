package id.kelompok1.randomskillgen_zrator.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {

    // -------------------------------------------------------------------------
    // USER
    // -------------------------------------------------------------------------
    @Query("SELECT * FROM User WHERE firebase_uid = :uid LIMIT 1")
    User getUser(String uid);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertUser(User user);

    @Update
    void updateUser(User user);

    // -------------------------------------------------------------------------
    // SKILL
    // -------------------------------------------------------------------------
    @Query("SELECT COUNT(id) FROM Skill")
    int getSkillsCount();

    @Query("SELECT * FROM Skill WHERE firebase_uid IS NULL ORDER BY RANDOM() LIMIT 1")
    Skill getRandomSkill();

    @Query("SELECT * FROM Skill WHERE id = :id LIMIT 1")
    Skill getSkillById(int id);

    @Query("SELECT * FROM Skill WHERE id IN (:ids)")
    List<Skill> getSkillsByIds(List<Integer> ids);

    @Query("SELECT * FROM Skill WHERE title = :title AND firebase_uid = :uid LIMIT 1")
    Skill getSkillByTitleForUser(String title, String uid);

    @Query("SELECT * FROM Skill WHERE title = :title AND (firebase_uid IS NULL OR firebase_uid = :uid) LIMIT 1")
    Skill getSkillByTitleForUserOrGlobal(String title, String uid);

    @Insert
    void insertSkill(Skill skill);

    @Insert
    void insertAll(List<Skill> skills);

    @Query("SELECT * FROM Skill " +
            "WHERE id NOT IN " +
            "(SELECT skill_id FROM DailySkill WHERE date = :date AND firebase_uid = :uid) " +
            "AND (firebase_uid IS NULL OR firebase_uid = :uid) " +
            "ORDER BY RANDOM() LIMIT 1")
    Skill getUnusedRandomSkillForUser(String date, String uid);

    // -------------------------------------------------------------------------
    // DAILY SKILL
    // -------------------------------------------------------------------------
    @Query("SELECT * FROM DailySkill WHERE date = :date AND firebase_uid = :uid")
    List<DailySkill> getDailySkillsByDate(String date, String uid);

    @Query("SELECT * FROM DailySkill WHERE firebase_uid = :uid AND date = :date AND skill_id = :skillId LIMIT 1")
    DailySkill getDailySkillByDateAndSkill(String uid, String date, int skillId);

    @Query("SELECT COUNT(*) FROM DailySkill " +
            "WHERE date = :date AND is_completed = 1 AND firebase_uid = :uid")
    int getCompletedQuestsCount(String date, String uid);

    @Query("SELECT COUNT(*) FROM DailySkill " +
            "WHERE date = :date AND firebase_uid = :uid " +
            "AND (is_completed = 1 OR quest_status = 'skipped')")
    int getFinishedQuestsCount(String date, String uid);

    @Insert
    void insertDailySkill(DailySkill dailySkill);

    @Update
    void updateDailySkill(DailySkill dailySkill);

    @Query("SELECT * FROM DailySkill WHERE firebase_uid = :uid ORDER BY date DESC, id DESC")
    List<DailySkill> getAllDailySkills(String uid);

    @Query("SELECT * FROM DailySkill WHERE firebase_uid = :uid AND is_completed = 1 ORDER BY date DESC, id DESC")
    List<DailySkill> getCompletedDailySkills(String uid);

    @Query("DELETE FROM DailySkill WHERE firebase_uid = :uid")
    void deleteAllHistoryForUser(String uid);

    @Transaction
    default void completeDailyQuest(DailySkill record, User user) {
        updateDailySkill(record);
        updateUser(user);
    }

    // -------------------------------------------------------------------------
    // ACHIEVEMENT
    // -------------------------------------------------------------------------
    @Query("SELECT COUNT(*) FROM Achievement")
    int getAchievementCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAchievements(List<Achievement> achievements);

    @Query("SELECT * FROM Achievement ORDER BY unlocked DESC, id ASC")
    List<Achievement> getAllAchievements();

    @Query("SELECT * FROM Achievement WHERE id = :id LIMIT 1")
    Achievement getAchievementById(String id);

    @Query("UPDATE Achievement SET unlocked = 1 WHERE id = :id")
    void unlockAchievement(String id);

    @Query("UPDATE Achievement SET unlocked = 0")
    void lockAllAchievements();

    @Query("SELECT COUNT(*) FROM DailySkill WHERE firebase_uid = :uid AND is_completed = 1")
    int getTotalCompletedQuestCount(String uid);

    @Query("SELECT COUNT(*) FROM DailySkill ds INNER JOIN Skill s ON ds.skill_id = s.id " +
            "WHERE ds.firebase_uid = :uid AND ds.is_completed = 1 AND s.category = :category")
    int getCompletedQuestCountByCategory(String uid, String category);

    @Query("SELECT s.category AS category, COUNT(*) AS count " +
            "FROM DailySkill ds INNER JOIN Skill s ON ds.skill_id = s.id " +
            "WHERE ds.firebase_uid = :uid AND ds.is_completed = 1 " +
            "GROUP BY s.category")
    List<CategoryCount> getCompletedQuestCountByCategoryForUser(String uid);
}
