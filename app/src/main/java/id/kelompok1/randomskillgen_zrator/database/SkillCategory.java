package id.kelompok1.randomskillgen_zrator.database;

import androidx.annotation.Nullable;
import id.kelompok1.randomskillgen_zrator.R; // Pastikan import R

/**
 * Konstanta kategori skill + asset/icon/xp/color terkait.
 * Hindari magic string dan hardcoded Glide URL di Fragment.
 */
public final class SkillCategory {
    private SkillCategory() {}

    public static final String HEALTH     = "Kesehatan";
    public static final String PRODUCTIVE = "Produktif";
    public static final String FUN        = "Fun";
    public static final String EDUCATION  = "Edukasi";

    public static final String ICON_DEFAULT    = "https://cdn-icons-png.flaticon.com/512/1055/1055664.png";
    public static final String ICON_HEALTH     = "https://cdn-icons-png.flaticon.com/512/2966/2966334.png";
    public static final String ICON_PRODUCTIVE = "https://cdn-icons-png.flaticon.com/512/2641/2641409.png";
    public static final String ICON_FUN        = "https://cdn-icons-png.flaticon.com/512/3063/3063822.png";
    public static final String ICON_EDUCATION  = "https://cdn-icons-png.flaticon.com/512/2436/2436636.png";

    public static String iconUrlFor(@Nullable String category) {
        if (category == null) return ICON_DEFAULT;
        switch (category) {
            case PRODUCTIVE: return ICON_PRODUCTIVE;
            case HEALTH:     return ICON_HEALTH;
            case EDUCATION:  return ICON_EDUCATION;
            case FUN:        return ICON_FUN;
            default:         return ICON_DEFAULT;
        }
    }

    // --- FITUR BARU: Sentralisasi Logika XP ---
    public static int getXpForCategory(@Nullable String category) {
        if (category == null) return 50;
        switch (category) {
            case HEALTH:     return 50;
            case PRODUCTIVE: return 60;
            case EDUCATION:  return 70;
            case FUN:        return 30;
            default:         return 50;
        }
    }

    public static String normalize(@Nullable String category) {
        if (category == null) return FUN;
        switch (category) {
            case HEALTH:
            case PRODUCTIVE:
            case EDUCATION:
            case FUN:
                return category;
            default:
                return FUN;
        }
    }

    // --- FITUR BARU: Sentralisasi Warna UI (Persiapan Modern Gen-Z Look) ---
    // (Note: Pastikan warna-warna ini nanti kita definisikan di colors.xml)
    public static int getColorResForCategory(@Nullable String category) {
        if (category == null) return R.color.gray_default;
        switch (category) {
            case HEALTH:     return R.color.green_health;
            case PRODUCTIVE: return R.color.blue_productive;
            case EDUCATION:  return R.color.purple_education;
            case FUN:        return R.color.yellow_fun;
            default:         return R.color.gray_default;
        }
    }
}
