package id.kelompok1.randomskillgen_zrator.domain;

public final class TierLabelProvider {

    private TierLabelProvider() {
    }

    public static String homeTier(int level) {
        if (level <= 5) return "Novice";
        if (level <= 15) return "Apprentice";
        if (level <= 30) return "Royal Knight";
        if (level <= 50) return "Guild Master";
        return "Grandmaster";
    }

    public static String profileTier(int level) {
        return "[ " + homeTier(level).toUpperCase() + " ]";
    }
}
