package id.kelompok1.randomskillgen_zrator.domain;

public final class CustomSkillValidator {

    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 80;

    private CustomSkillValidator() {
    }

    public static String normalizeTitle(String title) {
        if (title == null) return "";
        return title.trim().replaceAll("\\s+", " ");
    }

    public static String validateTitle(String title) {
        String trimmed = normalizeTitle(title);

        if (trimmed.isEmpty()) {
            return "Judul skill tidak boleh kosong.";
        }

        if (trimmed.length() < MIN_TITLE_LENGTH) {
            return "Judul skill terlalu pendek.";
        }

        if (trimmed.length() > MAX_TITLE_LENGTH) {
            return "Judul skill maksimal " + MAX_TITLE_LENGTH + " karakter.";
        }

        if (trimmed.matches(".*[\\r\\n\\t].*")) {
            return "Judul skill tidak boleh berisi baris baru.";
        }

        if (trimmed.toLowerCase().contains("http://") || trimmed.toLowerCase().contains("https://")) {
            return "Judul skill tidak boleh berupa link.";
        }

        return null;
    }
}
