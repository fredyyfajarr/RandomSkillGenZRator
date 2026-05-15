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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import id.kelompok1.randomskillgen_zrator.database.AppRepository;
import id.kelompok1.randomskillgen_zrator.database.SkillCategory;
import id.kelompok1.randomskillgen_zrator.domain.CustomSkillValidator;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private AppRepository repo;

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
        com.google.android.material.button.MaterialButton btnCustom =
                view.findViewById(R.id.btn_zap_custom_skill);

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
            showCustomSkillDialog();
        });

        bottomSheetDialog.show();
    }

    private void showCustomSkillDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buat Challenge Custom 🎯");

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

        RadioGroup rgCategory = new RadioGroup(this);
        rgCategory.setOrientation(RadioGroup.VERTICAL);

        String[][] categories = {
                {"🏃 Kesehatan", SkillCategory.HEALTH},
                {"⚡ Produktif", SkillCategory.PRODUCTIVE},
                {"🎮 Fun", SkillCategory.FUN},
                {"📚 Edukasi", SkillCategory.EDUCATION},
        };

        for (String[] cat : categories) {
            RadioButton rb = new RadioButton(this);
            rb.setText(cat[0]);
            rb.setTag(cat[1]);
            rb.setPadding(0, 8, 0, 8);
            rgCategory.addView(rb);
        }

        ((RadioButton) rgCategory.getChildAt(2)).setChecked(true);
        layout.addView(rgCategory);

        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
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

            int checkedId = rgCategory.getCheckedRadioButtonId();
            RadioButton selected = rgCategory.findViewById(checkedId);

            String chosenCategory = (selected != null && selected.getTag() != null)
                    ? (String) selected.getTag()
                    : SkillCategory.FUN;

            int xpReward = SkillCategory.getXpForCategory(chosenCategory);
            String uid = fUser.getUid();

            repo.getExecutor().execute(() ->
                    repo.addCustomSkill(uid, title, chosenCategory, xpReward)
            );

            Toast.makeText(
                    this,
                    "Challenge \"" + title + "\" ditambahkan ke pool pribadimu!",
                    Toast.LENGTH_SHORT
            ).show();
        });

        builder.setNegativeButton("Batal", (d, w) -> d.cancel());
        builder.show();
    }
}
