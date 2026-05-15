package id.kelompok1.randomskillgen_zrator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import id.kelompok1.randomskillgen_zrator.database.SyncState;
import id.kelompok1.randomskillgen_zrator.database.User;

public class ProfileFragment extends Fragment {

    private TextView tvName;
    private TextView tvLevel;
    private TextView tvTier;
    private TextView tvXpDetails;
    private TextView tvSyncStatus;

    private ImageView ivAvatar;
    private ProgressBar pbXp;

    private MaterialButton btnReset;
    private MaterialButton btnLogout;

    private SwitchMaterial switchDarkMode;

    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        bindViews(view);
        setupViewModel();
        setupDarkModeToggle();
        setupActions();
        loadGoogleProfilePhoto();

        return view;
    }

    private void bindViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvLevel = view.findViewById(R.id.tv_profile_level);
        tvTier = view.findViewById(R.id.tv_tier);
        tvXpDetails = view.findViewById(R.id.tv_xp_details);
        tvSyncStatus = view.findViewById(R.id.tv_sync_status);

        pbXp = view.findViewById(R.id.pb_profile_xp);

        btnReset = view.findViewById(R.id.btn_reset_data);
        btnLogout = view.findViewById(R.id.btn_logout);

        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateProfileUI(user);
            }
        });

        viewModel.getSyncState().observe(getViewLifecycleOwner(), state -> {
            if (tvSyncStatus == null || state == null) return;

            if (state == SyncState.SYNCING) {
                tvSyncStatus.setText("⏳ Menyinkron...");
                tvSyncStatus.setVisibility(View.VISIBLE);
            } else if (state == SyncState.ERROR) {
                tvSyncStatus.setText("⚠ Gagal sinkron. Data tersimpan lokal.");
                tvSyncStatus.setVisibility(View.VISIBLE);
            } else {
                tvSyncStatus.setVisibility(View.GONE);
            }
        });
    }

    private void setupActions() {
        btnReset.setOnClickListener(v -> confirmReset());
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void setupDarkModeToggle() {
        if (switchDarkMode == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("GenZPrefs", android.content.Context.MODE_PRIVATE);

        boolean darkMode = prefs.getBoolean("darkMode", false);

        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setChecked(darkMode);
        switchDarkMode.setText(darkMode ? "Dark Mode: ON" : "Dark Mode: OFF");

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("darkMode", isChecked).apply();

            switchDarkMode.setText(isChecked ? "Dark Mode: ON" : "Dark Mode: OFF");

            AppCompatDelegate.setDefaultNightMode(
                    isChecked
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );

            requireActivity().recreate();
        });
    }

    private void updateProfileUI(User user) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String displayName = "Hero Gen-Z";
        if (currentUser != null && currentUser.getDisplayName() != null) {
            displayName = currentUser.getDisplayName();
        }

        tvName.setText(displayName);
        tvLevel.setText("LV." + user.level);
        tvTier.setText(getTierLabel(user.level));

        int requiredXp = MainViewModel.getRequiredXpForLevel(user.level);

        pbXp.setMax(requiredXp);
        pbXp.setProgress(user.xp);

        tvXpDetails.setText(user.xp + " / " + requiredXp + " XP");
    }

    private void loadGoogleProfilePhoto() {
        if (ivAvatar == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        ivAvatar.setPadding(0, 0, 0, 0);
        ivAvatar.setColorFilter(null);
        ivAvatar.clearColorFilter();

        if (currentUser == null || currentUser.getPhotoUrl() == null) {
            ivAvatar.setImageResource(R.drawable.ic_custom_profile);
            return;
        }

        Glide.with(this)
                .load(currentUser.getPhotoUrl())
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_custom_profile)
                .error(R.drawable.ic_custom_profile)
                .into(ivAvatar);
    }

    private String getTierLabel(int level) {
        if (level <= 5) return "[ NOVICE ADVENTURER ]";
        if (level <= 15) return "[ APPRENTICE ]";
        if (level <= 30) return "[ ROYAL KNIGHT ]";
        if (level <= 50) return "[ GUILD MASTER ]";
        return "[ GRANDMASTER ]";
    }

    private void confirmReset() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reset Journey?")
                .setMessage("Semua histori quest, XP, streak, dan progress hari ini akan direset ke awal.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    viewModel.resetDatabase();
                    Toast.makeText(
                            getContext(),
                            "Catatan petualangan dikosongkan!",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        )
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignIn.getClient(requireContext(), gso)
                .signOut()
                .addOnCompleteListener(task -> {
                    Toast.makeText(
                            requireContext(),
                            "Berhasil keluar dari Guild!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
    }
}