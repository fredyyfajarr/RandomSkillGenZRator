package id.kelompok1.randomskillgen_zrator.domain;

import id.kelompok1.randomskillgen_zrator.database.User;

public final class XpCalculator {

    private XpCalculator() {
    }

    public static int requiredXpForLevel(int level) {
        if (level <= 1) return 100;
        return 100 + ((level - 1) * 75);
    }

    public static boolean isStreakBonusDay(int streak) {
        return streak > 0 && streak % 7 == 0;
    }

    public static int rewardXp(int baseXp, int nextStreak) {
        return isStreakBonusDay(nextStreak) ? (int) (baseXp * 1.5f) : baseXp;
    }

    public static void addXpAndApplyLevelUp(User user, int gainedXp) {
        if (user == null || gainedXp <= 0) return;

        user.xp += gainedXp;

        while (user.xp >= requiredXpForLevel(user.level)) {
            user.xp -= requiredXpForLevel(user.level);
            user.level++;
        }
    }
}
