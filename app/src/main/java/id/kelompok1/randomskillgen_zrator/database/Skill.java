package id.kelompok1.randomskillgen_zrator.database;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Skill")
public class Skill {

    public static final String EASY = "Easy";
    public static final String MEDIUM = "Medium";
    public static final String HARD = "Hard";

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String category;
    public int xp_reward;

    public String difficulty;
    public int duration_minutes;

    /**
     * null = default/global skill.
     * berisi uid = custom skill milik user tertentu.
     */
    @Nullable
    public String firebase_uid;

    public boolean is_custom;

    // Constructor utama yang dipakai Room
    public Skill(
            String title,
            String category,
            int xp_reward,
            String difficulty,
            int duration_minutes,
            @Nullable String firebase_uid,
            boolean is_custom
    ) {
        this.title = title;
        this.category = category;
        this.xp_reward = xp_reward;
        this.difficulty = difficulty;
        this.duration_minutes = duration_minutes;
        this.firebase_uid = firebase_uid;
        this.is_custom = is_custom;
    }

    // Constructor global/default skill
    @Ignore
    public Skill(String title, String category, int xp_reward, String difficulty, int duration_minutes) {
        this(
                title,
                category,
                xp_reward,
                difficulty,
                duration_minutes,
                null,
                false
        );
    }

    // Constructor lama agar code existing tetap aman
    @Ignore
    public Skill(String title, String category, int xp_reward) {
        this(
                title,
                category,
                xp_reward,
                inferDifficulty(xp_reward),
                inferDuration(xp_reward),
                null,
                false
        );
    }

    public static String inferDifficulty(int xpReward) {
        if (xpReward <= 30) return EASY;
        if (xpReward <= 60) return MEDIUM;
        return HARD;
    }

    public static int inferDuration(int xpReward) {
        if (xpReward <= 30) return 1;
        if (xpReward <= 60) return 3;
        return 5;
    }
}