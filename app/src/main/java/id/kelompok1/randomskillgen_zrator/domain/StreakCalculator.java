package id.kelompok1.randomskillgen_zrator.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import id.kelompok1.randomskillgen_zrator.database.User;

public final class StreakCalculator {

    private StreakCalculator() {
    }

    public static User resetIfInactive(User user, String today) {
        if (user == null || today == null) return user;

        if (user.last_active_date == null) {
            user.last_active_date = today;
            return user;
        }

        if (user.last_active_date.equals(today)) return user;

        long dayDiff = daysBetween(user.last_active_date, today);
        if (dayDiff > 1) {
            user.streak = 0;
            user.last_active_date = today;
        }

        return user;
    }

    public static void applyFirstFinishedQuestOfDay(User user, String today) {
        if (user == null) return;

        user.streak++;
        user.total_active_days++;
        user.last_active_date = today;

        if (user.streak > user.best_streak) {
            user.best_streak = user.streak;
        }
    }

    public static boolean shouldUseCloudUser(User local, String cloudLastActiveDate) {
        if (local == null) return true;
        if (local.last_active_date == null) return true;
        if (cloudLastActiveDate == null) return false;

        return daysBetween(local.last_active_date, cloudLastActiveDate) >= 0;
    }

    public static long daysBetween(String from, String to) {
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
}
