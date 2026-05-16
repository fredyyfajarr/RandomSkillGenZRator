package id.kelompok1.randomskillgen_zrator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import id.kelompok1.randomskillgen_zrator.database.AppRepository;
import id.kelompok1.randomskillgen_zrator.database.Skill;
import id.kelompok1.randomskillgen_zrator.database.SkillCategory;
import id.kelompok1.randomskillgen_zrator.domain.CustomSkillValidator;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_CUSTOM_SKILLS = 30;

    private BottomNavigationView bottomNav;
    private AppRepository repo;

    private static final String[][] CATEGORY_OPTIONS = {
            {"Kesehatan", SkillCategory.HEALTH},
            {"Produktif", SkillCategory.PRODUCTIVE},
            {"Fun", SkillCategory.FUN},
            {"Edukasi", SkillCategory.EDUCATION},
    };

    private static final String[][] DIFFICULTY_OPTIONS = {
            {"Easy - 1 menit", Skill.EASY},
            {"Medium - 3 menit", Skill.MEDIUM},
            {"Hard - 5 menit", Skill.HARD},
    };

    private final ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (Boolean.TRUE.equals(granted)) {
                            NotificationScheduler.scheduleDailyReminder(this);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedThemeMode();
        setContentView(R.layout.activity_main);

        repo = AppRepository.getInstance(this);
        bottomNav = findViewById(R.id.bottom_navigation);

        try {
            bottomNav.setItemActiveIndicatorEnabled(false);
        } catch (Exception ignored) {
        }

        FloatingActionButton fabLightning = findViewById(R.id.fab_lightning);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
            animateIcon(R.id.nav_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (id == R.id.nav_history) {
                selected = new HistoryFragment();
            } else if (id == R.id.nav_stats) {
                selected = new StatsFragment();
            } else if (id == R.id.nav_profile) {
                selected = new ProfileFragment();
            } else if (id == R.id.nav_placeholder) {
                return false;
            }

            if (selected != null) {
                loadFragment(selected);
                animateIcon(id);
                return true;
            }

            return false;
        });

        fabLightning.setOnClickListener(v -> showZapBottomSheet());

        ensureLocalUserExists();
        requestNotificationPermissionAndSchedule();
    }

    private void applySavedThemeMode() {
        android.content.SharedPreferences prefs = getSharedPreferences("GenZPrefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("darkMode", false);

        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationScheduler.scheduleDailyReminder(this);
            } else {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            NotificationScheduler.scheduleDailyReminder(this);
        }
    }

    private void ensureLocalUserExists() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        String uid = fUser.getUid();
        repo.getExecutor().execute(() -> repo.ensureUserExists(uid));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void animateIcon(int selectedId) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) bottomNav.getChildAt(0);

        for (int i = 0; i < menuView.getChildCount(); i++) {
            View itemView = menuView.getChildAt(i);

            View iconView = itemView.findViewById(
                    com.google.android.material.R.id.navigation_bar_item_icon_view
            );

            if (iconView == null) continue;

            boolean isSelected = itemView.getId() == selectedId;

            if (itemView instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) itemView).setClipChildren(false);
                ((android.view.ViewGroup) itemView).setClipToPadding(false);
            }

            iconView.animate()
                    .scaleX(isSelected ? 1.15f : 1.0f)
                    .scaleY(isSelected ? 1.15f : 1.0f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();

            itemView.animate()
                    .alpha(isSelected ? 1.0f : 0.72f)
                    .setDuration(180)
                    .start();
        }
    }

    private void showZapBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_zap, null);
        bottomSheetDialog.setContentView(view);

        TextView tvQuote = view.findViewById(R.id.tv_zap_quote);
        MaterialButton btnCustom = view.findViewById(R.id.btn_zap_custom_skill);

        String[] quotes = {
                "\"Jangan nunggu motivasi datang, mulai aja dulu! 🚀\"",
                "\"Sedikit kemajuan setiap hari menambah hasil yang besar. 🌱\"",
                "\"Skill nggak dibangun dalam semalam, tapi dari konsistensi. 🔥\"",
                "\"Istirahat boleh, menyerah jangan. Gas terus! ⚡\"",
                "\"Satu skill kecil hari ini, investasi besar buat masa depanmu. 💎\""
        };

        tvQuote.setText(quotes[(int) (Math.random() * quotes.length)]);

        btnCustom.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showManageCustomSkillsDialog();
        });

        bottomSheetDialog.show();
    }

    private void showCustomSkillDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buat Challenge Custom");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Masukkan skill, misal: Push up 20x");
        layout.addView(etTitle);

        TextView tvCatLabel = new TextView(this);
        tvCatLabel.setText("Pilih Kategori:");
        tvCatLabel.setTextSize(14f);
        tvCatLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvCatLabel);

        RadioGroup rgCategory = createCategoryRadioGroup(SkillCategory.FUN);
        layout.addView(rgCategory);

        TextView tvDifficultyLabel = new TextView(this);
        tvDifficultyLabel.setText("Pilih Tingkat Kesulitan:");
        tvDifficultyLabel.setTextSize(14f);
        tvDifficultyLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvDifficultyLabel);

        RadioGroup rgDifficulty = createDifficultyRadioGroup(Skill.MEDIUM);
        layout.addView(rgDifficulty);

        TextView tvPreview = createCustomSkillPreview();
        layout.addView(tvPreview);
        bindCustomSkillPreview(tvPreview, rgCategory, rgDifficulty);

        builder.setView(layout);
        builder.setPositiveButton("Simpan", null);
        builder.setNegativeButton("Batal", (d, w) -> d.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = CustomSkillValidator.normalizeTitle(etTitle.getText().toString());
            String titleError = CustomSkillValidator.validateTitle(title);

            if (titleError != null) {
                Toast.makeText(this, titleError, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fUser == null) {
                Toast.makeText(this, "Kamu harus login dulu.", Toast.LENGTH_SHORT).show();
                return;
            }

            String chosenCategory = selectedCategory(rgCategory);
            String chosenDifficulty = selectedDifficulty(rgDifficulty);
            int xpReward = calculateCustomXp(chosenCategory, chosenDifficulty);
            int durationMinutes = durationForDifficulty(chosenDifficulty);
            String uid = fUser.getUid();

            repo.getExecutor().execute(() -> {
                if (repo.getCustomSkillCountForUser(uid) >= MAX_CUSTOM_SKILLS) {
                    runOnUiThread(() -> Toast.makeText(
                            this,
                            "Maksimal " + MAX_CUSTOM_SKILLS + " custom challenge dulu.",
                            Toast.LENGTH_SHORT
                    ).show());
                    return;
                }

                if (repo.isCustomSkillTitleTaken(uid, title, 0)) {
                    runOnUiThread(() -> Toast.makeText(
                            this,
                            "Challenge dengan judul itu sudah ada.",
                            Toast.LENGTH_SHORT
                    ).show());
                    return;
                }

                    repo.addCustomSkill(
                            uid,
                            title,
                            chosenCategory,
                            xpReward,
                            chosenDifficulty,
                            durationMinutes
                    );

                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            "Challenge \"" + title + "\" ditambahkan ke pool pribadimu!",
                            Toast.LENGTH_SHORT
                    ).show();
                    dialog.dismiss();
                    showManageCustomSkillsDialog();
                });
            });
        }));
        dialog.show();
    }

    private void showCustomSkillEditor(Skill existingSkill) {
        if (existingSkill == null) {
            showCustomSkillDialog();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Challenge Custom");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Masukkan skill, misal: Push up 20x");
        etTitle.setText(existingSkill.title);
        etTitle.setSelection(etTitle.getText().length());
        layout.addView(etTitle);

        TextView tvCatLabel = new TextView(this);
        tvCatLabel.setText("Pilih Kategori:");
        tvCatLabel.setTextSize(14f);
        tvCatLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvCatLabel);

        RadioGroup rgCategory = createCategoryRadioGroup(existingSkill.category);
        layout.addView(rgCategory);

        TextView tvDifficultyLabel = new TextView(this);
        tvDifficultyLabel.setText("Pilih Tingkat Kesulitan:");
        tvDifficultyLabel.setTextSize(14f);
        tvDifficultyLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvDifficultyLabel);

        RadioGroup rgDifficulty = createDifficultyRadioGroup(
                existingSkill.difficulty != null ? existingSkill.difficulty : Skill.MEDIUM
        );
        layout.addView(rgDifficulty);

        TextView tvPreview = createCustomSkillPreview();
        layout.addView(tvPreview);
        bindCustomSkillPreview(tvPreview, rgCategory, rgDifficulty);

        builder.setView(layout);
        builder.setPositiveButton("Simpan Perubahan", null);
        builder.setNegativeButton("Batal", (d, w) -> d.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String title = CustomSkillValidator.normalizeTitle(etTitle.getText().toString());
                    String titleError = CustomSkillValidator.validateTitle(title);

                    if (titleError != null) {
                        Toast.makeText(this, titleError, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String chosenCategory = selectedCategory(rgCategory);
                    String chosenDifficulty = selectedDifficulty(rgDifficulty);
                    int xpReward = calculateCustomXp(chosenCategory, chosenDifficulty);
                    int durationMinutes = durationForDifficulty(chosenDifficulty);
                    FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (fUser == null) {
                        Toast.makeText(this, "Kamu harus login dulu.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = fUser.getUid();
                    repo.getExecutor().execute(() -> {
                        if (repo.isCustomSkillTitleTaken(uid, title, existingSkill.id)) {
                            runOnUiThread(() -> Toast.makeText(
                                    this,
                                    "Challenge dengan judul itu sudah ada.",
                                    Toast.LENGTH_SHORT
                            ).show());
                            return;
                        }

                            repo.updateCustomSkill(
                                    existingSkill,
                                    title,
                                    chosenCategory,
                                    xpReward,
                                    chosenDifficulty,
                                    durationMinutes
                            );

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    this,
                                    "Challenge \"" + title + "\" diperbarui.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            dialog.dismiss();
                            showManageCustomSkillsDialog();
                        });
                    });
                })
        );
        dialog.show();
    }

    private RadioGroup createCategoryRadioGroup(String selectedCategory) {
        RadioGroup rgCategory = new RadioGroup(this);
        rgCategory.setOrientation(RadioGroup.VERTICAL);

        for (String[] cat : CATEGORY_OPTIONS) {
            RadioButton rb = new RadioButton(this);
            rb.setText(cat[0]);
            rb.setTag(cat[1]);
            rb.setPadding(0, 8, 0, 8);
            rgCategory.addView(rb);

            if (cat[1].equals(selectedCategory)) {
                rb.setChecked(true);
            }
        }

        if (rgCategory.getCheckedRadioButtonId() == -1 && rgCategory.getChildCount() > 0) {
            ((RadioButton) rgCategory.getChildAt(2)).setChecked(true);
        }

        return rgCategory;
    }

    private RadioGroup createDifficultyRadioGroup(String selectedDifficulty) {
        RadioGroup rgDifficulty = new RadioGroup(this);
        rgDifficulty.setOrientation(RadioGroup.VERTICAL);

        for (String[] difficulty : DIFFICULTY_OPTIONS) {
            RadioButton rb = new RadioButton(this);
            rb.setText(difficulty[0]);
            rb.setTag(difficulty[1]);
            rb.setPadding(0, 8, 0, 8);
            rgDifficulty.addView(rb);

            if (difficulty[1].equals(selectedDifficulty)) {
                rb.setChecked(true);
            }
        }

        if (rgDifficulty.getCheckedRadioButtonId() == -1 && rgDifficulty.getChildCount() > 1) {
            ((RadioButton) rgDifficulty.getChildAt(1)).setChecked(true);
        }

        return rgDifficulty;
    }

    private String selectedCategory(RadioGroup rgCategory) {
        int checkedId = rgCategory.getCheckedRadioButtonId();
        RadioButton selected = rgCategory.findViewById(checkedId);

        return selected != null && selected.getTag() != null
                ? (String) selected.getTag()
                : SkillCategory.FUN;
    }

    private TextView createCustomSkillPreview() {
        TextView preview = new TextView(this);
        preview.setTextSize(13f);
        preview.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        preview.setPadding(0, 20, 0, 4);
        return preview;
    }

    private void bindCustomSkillPreview(
            TextView preview,
            RadioGroup rgCategory,
            RadioGroup rgDifficulty
    ) {
        RadioGroup.OnCheckedChangeListener listener = (group, checkedId) ->
                updateCustomSkillPreview(preview, rgCategory, rgDifficulty);

        rgCategory.setOnCheckedChangeListener(listener);
        rgDifficulty.setOnCheckedChangeListener(listener);
        updateCustomSkillPreview(preview, rgCategory, rgDifficulty);
    }

    private void updateCustomSkillPreview(
            TextView preview,
            RadioGroup rgCategory,
            RadioGroup rgDifficulty
    ) {
        String category = selectedCategory(rgCategory);
        String difficulty = selectedDifficulty(rgDifficulty);
        int xpReward = calculateCustomXp(category, difficulty);
        int durationMinutes = durationForDifficulty(difficulty);

        preview.setText(
                "Reward: +" + xpReward + " XP - Timer: " + durationMinutes + " menit"
        );
    }

    private String selectedDifficulty(RadioGroup rgDifficulty) {
        int checkedId = rgDifficulty.getCheckedRadioButtonId();
        RadioButton selected = rgDifficulty.findViewById(checkedId);

        return selected != null && selected.getTag() != null
                ? (String) selected.getTag()
                : Skill.MEDIUM;
    }

    private int durationForDifficulty(String difficulty) {
        if (Skill.EASY.equals(difficulty)) {
            return 1;
        }
        if (Skill.HARD.equals(difficulty)) {
            return 5;
        }
        return 3;
    }

    private int calculateCustomXp(String category, String difficulty) {
        int baseXp = SkillCategory.getXpForCategory(category);

        if (Skill.EASY.equals(difficulty)) {
            return Math.max(20, baseXp - 20);
        }
        if (Skill.HARD.equals(difficulty)) {
            return baseXp + 30;
        }
        return baseXp;
    }

    private String labelForCategory(String category) {
        for (String[] item : CATEGORY_OPTIONS) {
            if (item[1].equals(category)) {
                return item[0];
            }
        }
        return "Fun";
    }

    private String labelForDifficulty(String difficulty) {
        if (Skill.EASY.equals(difficulty)) {
            return "Easy";
        }
        if (Skill.HARD.equals(difficulty)) {
            return "Hard";
        }
        return "Medium";
    }

    private void showManageCustomSkillsDialog() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            Toast.makeText(this, "Kamu harus login dulu.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = fUser.getUid();
        repo.getExecutor().execute(() -> {
            List<Skill> customSkills = repo.getCustomSkillsForUser(uid);
            runOnUiThread(() -> renderCustomSkillManager(uid, customSkills));
        });
    }

    private void renderCustomSkillManager(String uid, List<Skill> customSkills) {
        if (customSkills == null || customSkills.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Custom Challenge")
                    .setMessage("Belum ada custom challenge. Buat challenge pertama dulu.")
                    .setPositiveButton("Buat", (dialog, which) -> showCustomSkillDialog())
                    .setNegativeButton("Tutup", null)
                    .show();
            return;
        }

        ScrollView scrollView = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(32, 20, 32, 8);
        scrollView.addView(list);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Kelola Custom Challenge")
                .setView(scrollView)
                .setPositiveButton("Tambah Baru", (d, w) -> showCustomSkillDialog())
                .setNegativeButton("Tutup", null)
                .create();

        for (Skill skill : customSkills) {
            list.addView(createCustomSkillRow(uid, skill, dialog));
        }

        dialog.show();
    }

    private View createCustomSkillRow(String uid, Skill skill, AlertDialog parentDialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 10, 0, 18);

        TextView title = new TextView(this);
        title.setText(skill.title);
        title.setTextSize(15f);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        row.addView(title);

        TextView meta = new TextView(this);
        int durationMinutes = skill.duration_minutes > 0
                ? skill.duration_minutes
                : durationForDifficulty(skill.difficulty);
        meta.setText(
                labelForCategory(skill.category)
                        + " - " + labelForDifficulty(skill.difficulty)
                        + " - " + durationMinutes + " menit"
                        + " - +" + skill.xp_reward + " XP"
        );
        meta.setTextSize(12f);
        meta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        meta.setPadding(0, 4, 0, 8);
        row.addView(meta);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        MaterialButton edit = new MaterialButton(this);
        edit.setText("Edit");
        edit.setOnClickListener(v -> {
            parentDialog.dismiss();
            showCustomSkillEditor(skill);
        });

        MaterialButton delete = new MaterialButton(this);
        delete.setText("Hapus");
        delete.setTextColor(ContextCompat.getColor(this, R.color.danger_red));
        delete.setOnClickListener(v -> confirmDeleteCustomSkill(uid, skill, parentDialog));

        actions.addView(edit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(delete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(actions);

        return row;
    }

    private void confirmDeleteCustomSkill(String uid, Skill skill, AlertDialog parentDialog) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Challenge?")
                .setMessage("Challenge custom yang sudah pernah muncul di quest tidak akan dihapus agar history tetap aman.")
                .setPositiveButton("Hapus", (dialog, which) -> repo.getExecutor().execute(() -> {
                    boolean deleted = repo.deleteCustomSkillIfUnused(uid, skill);
                    runOnUiThread(() -> {
                        Toast.makeText(
                                this,
                                deleted
                                        ? "Challenge \"" + skill.title + "\" dihapus."
                                        : "Challenge ini sudah dipakai di quest, jadi tidak bisa dihapus.",
                                Toast.LENGTH_SHORT
                        ).show();

                        parentDialog.dismiss();
                        showManageCustomSkillsDialog();
                    });
                }))
                .setNegativeButton("Batal", null)
                .show();
    }
}
