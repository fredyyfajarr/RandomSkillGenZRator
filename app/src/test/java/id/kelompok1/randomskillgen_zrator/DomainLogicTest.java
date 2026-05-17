package id.kelompok1.randomskillgen_zrator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;
import id.kelompok1.randomskillgen_zrator.database.User;
import id.kelompok1.randomskillgen_zrator.domain.CustomSkillValidator;
import id.kelompok1.randomskillgen_zrator.domain.QuestTimerCalculator;
import id.kelompok1.randomskillgen_zrator.domain.StreakCalculator;
import id.kelompok1.randomskillgen_zrator.domain.XpCalculator;

public class DomainLogicTest {

    @Test
    public void xpCalculator_appliesLevelUpAndStreakBonus() {
        User user = new User("uid");
        user.xp = 90;

        int reward = XpCalculator.rewardXp(40, 7);
        XpCalculator.addXpAndApplyLevelUp(user, reward);

        assertEquals(60, reward);
        assertEquals(2, user.level);
        assertEquals(50, user.xp);
    }

    @Test
    public void streakCalculator_resetsAfterMissedDay() {
        User user = new User("uid");
        user.streak = 5;
        user.last_active_date = "2026-05-14";

        StreakCalculator.resetIfInactive(user, "2026-05-16");

        assertEquals(0, user.streak);
        assertEquals("2026-05-16", user.last_active_date);
    }

    @Test
    public void questTimerCalculator_detectsExpiredRunningQuest() {
        Skill skill = new Skill("Baca 1 artikel", "Edukasi", 70, Skill.HARD, 5);
        DailySkill record = new DailySkill("uid", 1, "2026-05-16", false);
        record.started_at_millis = 1_000L;

        assertTrue(QuestTimerCalculator.isExpired(record, skill, 301_000L));
        assertFalse(QuestTimerCalculator.isExpired(record, skill, 240_000L));
    }

    @Test
    public void customSkillValidator_normalizesAndRejectsUnsafeTitles() {
        assertEquals("Push up 20x", CustomSkillValidator.normalizeTitle("  Push   up 20x  "));
        assertNull(CustomSkillValidator.validateTitle("Push up 20x"));
        assertEquals("Judul skill tidak boleh berupa link.", CustomSkillValidator.validateTitle("https://contoh.com"));
    }
}
