package id.kelompok1.randomskillgen_zrator.domain;

import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;

public final class QuestTimerCalculator {

    private static final int DEFAULT_DURATION_MINUTES = 5;

    private QuestTimerCalculator() {
    }

    public static int durationMinutes(Skill skill) {
        if (skill == null || skill.duration_minutes <= 0) return DEFAULT_DURATION_MINUTES;
        return skill.duration_minutes;
    }

    public static long totalMillis(Skill skill) {
        return durationMinutes(skill) * 60L * 1000L;
    }

    public static long remainingMillis(DailySkill record, Skill skill, long nowMillis) {
        long totalMillis = totalMillis(skill);
        if (record == null || record.started_at_millis <= 0) return totalMillis;

        long elapsed = nowMillis - record.started_at_millis;
        return totalMillis - Math.max(elapsed, 0L);
    }

    public static boolean isExpired(DailySkill record, Skill skill, long nowMillis) {
        return remainingMillis(record, skill, nowMillis) <= 0;
    }
}
