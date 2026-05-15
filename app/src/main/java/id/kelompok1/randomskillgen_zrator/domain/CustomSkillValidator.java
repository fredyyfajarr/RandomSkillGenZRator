package id.kelompok1.randomskillgen_zrator.domain;

public final class CustomSkillValidator {

    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 80;

    private CustomSkillValidator() {
    }

    public static String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "Judul skill tidak boleh kosong.";
        }

        String trimmed = title.trim();
        if (trimmed.length() < MIN_TITLE_LENGTH) {
            return "Judul skill terlalu pendek.";
        }

        if (trimmed.length() > MAX_TITLE_LENGTH) {
            return "Judul skill maksimal " + MAX_TITLE_LENGTH + " karakter.";
        }

        return null;
    }
}
