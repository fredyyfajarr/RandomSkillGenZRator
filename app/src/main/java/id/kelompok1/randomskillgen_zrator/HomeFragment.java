package id.kelompok1.randomskillgen_zrator;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;
import id.kelompok1.randomskillgen_zrator.database.SkillCategory;
import id.kelompok1.randomskillgen_zrator.database.SyncState;
import id.kelompok1.randomskillgen_zrator.database.User;
import id.kelompok1.randomskillgen_zrator.domain.QuestTimerCalculator;
import id.kelompok1.randomskillgen_zrator.domain.TierLabelProvider;

public class HomeFragment extends Fragment {

    private MainViewModel viewModel;

    private TextView tvSkillTitle;
    private TextView tvLevel;
    private TextView tvXp;
    private TextView tvStreak;
    private TextView tvDailyQuote;
    private TextView tvTierLabel;
    private TextView tvSkillCategoryLabel;
    private TextView tvSyncBadge;
    private TextView tvQuestTimer;
    private TextView tvQuestProgress;

    private ProgressBar pbXp;

    private MaterialButton btnDone;
    private MaterialButton btnSkipQuest;

    private ImageView ivIllustration;
    private ImageView ivAvatar;

    private LottieAnimationView lottieSuccess;

    private MaterialCardView cardSkill;
    private MaterialCardView cardSkeleton;

    private CountDownTimer questTimer;
    private int runningTimerRecordId = -1;

    private int lastRenderedXp = 0;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private Runnable pendingShowDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        bindViews(view);
        setupCardTouchAnimation();

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        loadGoogleProfilePhoto();
        observeViewModel();

        viewModel.loadHomeData();

        btnDone.setOnClickListener(v -> handleMainQuestButton());

        if (btnSkipQuest != null) {
            btnSkipQuest.setOnClickListener(v -> confirmSkipQuest());
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (pendingShowDialog != null) {
            uiHandler.removeCallbacks(pendingShowDialog);
            pendingShowDialog = null;
        }

        if (questTimer != null) {
            questTimer.cancel();
            questTimer = null;
        }
        runningTimerRecordId = -1;
    }

    private void bindViews(View view) {
        tvSkillTitle = view.findViewById(R.id.tv_skill_title);
        tvSkillCategoryLabel = view.findViewById(R.id.tv_skill_category_label);
        btnDone = view.findViewById(R.id.btn_done);
        btnSkipQuest = view.findViewById(R.id.btn_skip_quest);

        tvLevel = view.findViewById(R.id.tv_level);
        pbXp = view.findViewById(R.id.pb_xp);
        tvXp = view.findViewById(R.id.tv_xp_text);
        tvStreak = view.findViewById(R.id.tv_streak_days);
        tvTierLabel = view.findViewById(R.id.tv_tier_label);
        tvQuestProgress = view.findViewById(R.id.tv_quest_progress);

        ivIllustration = view.findViewById(R.id.iv_skill_illustration);
        ivAvatar = view.findViewById(R.id.iv_avatar);

        tvDailyQuote = view.findViewById(R.id.tv_daily_quote);
        tvSyncBadge = view.findViewById(R.id.tv_sync_badge);
        tvQuestTimer = view.findViewById(R.id.tv_quest_timer);

        lottieSuccess = view.findViewById(R.id.lottie_success);

        cardSkill = view.findViewById(R.id.card_skill);
        cardSkeleton = view.findViewById(R.id.card_skeleton);
    }

    private void loadGoogleProfilePhoto() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (ivAvatar == null) return;

        ivAvatar.setPadding(0, 0, 0, 0);
        ivAvatar.setColorFilter(null);
        ivAvatar.clearColorFilter();

        if (fUser == null || fUser.getPhotoUrl() == null) {
            ivAvatar.setImageResource(R.drawable.ic_custom_profile);
            return;
        }

        Glide.with(this)
                .load(fUser.getPhotoUrl())
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_custom_profile)
                .error(R.drawable.ic_custom_profile)
                .into(ivAvatar);
    }

    private void observeViewModel() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateStatsUI(user);
            }
        });

        viewModel.getCurrentSkill().observe(getViewLifecycleOwner(), skill -> {
            if (skill != null) {
                updateSkillUI(skill);
                showSkillCard();
            }
        });

        viewModel.getTodayRecord().observe(getViewLifecycleOwner(), record ->
                updateQuestButton(record, safeProgress())
        );

        viewModel.getDailyProgress().observe(getViewLifecycleOwner(), count ->
                updateQuestButton(
                        viewModel.getTodayRecord().getValue(),
                        count == null ? 0 : count
                )
        );

        viewModel.getDailyQuote().observe(getViewLifecycleOwner(), quote -> {
            if (quote != null && tvDailyQuote != null) {
                tvDailyQuote.setText(quote);
            }
        });

        viewModel.getStreakBonusActive().observe(getViewLifecycleOwner(), isBonus -> {
            User u = viewModel.getUser().getValue();
            if (u != null) {
                updateXpLabel(u, Boolean.TRUE.equals(isBonus));
            }
        });

        viewModel.getSyncState().observe(getViewLifecycleOwner(), this::updateSyncBadge);

        viewModel.getShowSuccessDialogEvent().observe(getViewLifecycleOwner(), shouldShow -> {
            if (!Boolean.TRUE.equals(shouldShow)) return;

            playSfx();
            showLottie();

            pendingShowDialog = () -> {
                if (!isAdded()) return;

                hideLottie();

                User u = viewModel.getUser().getValue();
                if (u != null) {
                    showSuccessDialog(viewModel.getLastGainedXp(), u.streak);
                }
            };

            uiHandler.postDelayed(pendingShowDialog, 1500);

            viewModel.resetSuccessDialogEvent();
        });
    }

    private void handleMainQuestButton() {
        DailySkill record = viewModel.getTodayRecord().getValue();

        if (record == null) return;

        if (record.isPending()) {
            viewModel.startTodaySkill();
            return;
        }

        if (record.isReady()) {
            showClaimConfirmation();
        }
    }

    private void startQuestTimer() {
        Skill skill = viewModel.getCurrentSkill().getValue();
        DailySkill record = viewModel.getTodayRecord().getValue();

        if (skill == null || record == null) return;

        if (questTimer != null && runningTimerRecordId == record.id) return;

        long remainingMillis = QuestTimerCalculator.remainingMillis(
                record,
                skill,
                System.currentTimeMillis()
        );

        if (remainingMillis <= 0) {
            showTimerReadyState();
            viewModel.markTodaySkillReadyToClaim();
            return;
        }

        if (questTimer != null) {
            questTimer.cancel();
        }
        runningTimerRecordId = record.id;

        btnDone.setEnabled(false);
        btnDone.setText("Sedang Berjalan...");

        if (btnSkipQuest != null) {
            btnSkipQuest.setVisibility(View.VISIBLE);
        }

        questTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                runningTimerRecordId = -1;
                showTimerReadyState();
                viewModel.markTodaySkillReadyToClaim();
            }
        };

        questTimer.start();
    }

    private void updateTimerText(long millis) {
        if (tvQuestTimer == null) return;

        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        tvQuestTimer.setText(String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        ));
    }

    private void showTimerReadyState() {
        if (questTimer != null) {
            questTimer.cancel();
            questTimer = null;
        }
        runningTimerRecordId = -1;

        if (tvQuestTimer != null) {
            tvQuestTimer.setText("Ready!");
        }

        if (btnDone != null) {
            btnDone.setEnabled(true);
            btnDone.setText("Claim Reward!");
        }

        if (btnSkipQuest != null) {
            btnSkipQuest.setVisibility(View.GONE);
        }
    }

    private void showClaimConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Quest selesai?")
                .setMessage("Apakah kamu benar-benar sudah menyelesaikan quest ini?")
                .setPositiveButton("Ya, Claim XP", (dialog, which) -> viewModel.completeTodaySkill())
                .setNegativeButton("Belum, Skip", (dialog, which) -> {
                    if (questTimer != null) {
                        questTimer.cancel();
                        questTimer = null;
                    }
                    runningTimerRecordId = -1;

                    viewModel.skipTodaySkill();

                    if (tvQuestTimer != null) {
                        tvQuestTimer.setText("Skipped");
                    }

                    if (btnSkipQuest != null) {
                        btnSkipQuest.setVisibility(View.GONE);
                    }
                })
                .show();
    }

    private void confirmSkipQuest() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Skip Quest?")
                .setMessage("Yakin mau skip? Quest ini tidak akan memberi XP.")
                .setPositiveButton("Skip", (dialog, which) -> {
                    if (questTimer != null) {
                        questTimer.cancel();
                        questTimer = null;
                    }
                    runningTimerRecordId = -1;

                    viewModel.skipTodaySkill();

                    if (tvQuestTimer != null) {
                        tvQuestTimer.setText("Skipped");
                    }

                    if (btnSkipQuest != null) {
                        btnSkipQuest.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateSyncBadge(SyncState state) {
        if (tvSyncBadge == null || state == null) return;

        if (state == SyncState.SYNCING) {
            tvSyncBadge.setVisibility(View.VISIBLE);
            tvSyncBadge.setText("☁ Syncing...");
            tvSyncBadge.setTextColor(Color.parseColor("#FACC15"));
        } else if (state == SyncState.SUCCESS) {
            tvSyncBadge.setVisibility(View.VISIBLE);
            tvSyncBadge.setText("☁ Saved to cloud");
            tvSyncBadge.setTextColor(Color.parseColor("#86EFAC"));
        } else if (state == SyncState.ERROR) {
            tvSyncBadge.setVisibility(View.VISIBLE);
            tvSyncBadge.setText("⚠ Offline mode");
            tvSyncBadge.setTextColor(Color.parseColor("#FCA5A5"));
        } else {
            tvSyncBadge.setVisibility(View.VISIBLE);
            tvSyncBadge.setText("☁ Cloud ready");
            tvSyncBadge.setTextColor(Color.parseColor("#CBD5E1"));
        }
    }

    private void showSkillCard() {
        if (cardSkeleton != null) {
            cardSkeleton.setVisibility(View.GONE);
        }

        if (cardSkill != null) {
            cardSkill.setVisibility(View.VISIBLE);
        }
    }

    private void updateQuestProgressIndicator(int count) {
        if (tvQuestProgress == null) return;
        if (renderDotQuestProgress(count)) return;

        int questNumber = Math.min(count + 1, 3);

        if (count >= 3) {
            tvQuestProgress.setText("● ● ●   Semua Quest Selesai");
            return;
        }

        if (questNumber == 1) {
            tvQuestProgress.setText("● ○ ○   Quest 1/3");
        } else if (questNumber == 2) {
            tvQuestProgress.setText("● ● ○   Quest 2/3");
        } else {
            tvQuestProgress.setText("● ● ●   Quest 3/3");
        }
    }

    private boolean renderDotQuestProgress(int count) {
        if (count >= 3) {
            tvQuestProgress.setText("● ● ●   Semua Quest Selesai");
            return true;
        }

        int questNumber = Math.min(count + 1, 3);
        if (questNumber == 1) {
            tvQuestProgress.setText("● ○ ○   Quest 1/3");
        } else if (questNumber == 2) {
            tvQuestProgress.setText("● ● ○   Quest 2/3");
        } else {
            tvQuestProgress.setText("● ● ●   Quest 3/3");
        }

        return true;
    }

    private void updateQuestButton(@Nullable DailySkill record, int count) {
        updateQuestProgressIndicator(count);

        if (record == null) {
            btnDone.setEnabled(false);
            btnDone.setText("All Quests Completed! 🎉");

            if (tvQuestTimer != null) {
                tvQuestTimer.setText("Done");
            }

            if (btnSkipQuest != null) {
                btnSkipQuest.setVisibility(View.GONE);
            }

            return;
        }

        String status = record.quest_status != null
                ? record.quest_status
                : DailySkill.STATUS_PENDING;

        if (record.is_completed || DailySkill.STATUS_COMPLETED.equals(status)) {
            btnDone.setEnabled(false);

            if (count >= 3) {
                btnDone.setText("All Quests Completed! 🎉");

                if (tvDailyQuote != null) {
                    tvDailyQuote.setText("✨ Kerja bagus, Hero! Semua misi hari ini telah tuntas.");
                }
            } else {
                btnDone.setText("Quest selesai. Quest berikutnya siap kapan aja hari ini.");
            }

            if (tvQuestTimer != null) {
                tvQuestTimer.setText("Completed");
            }

            if (btnSkipQuest != null) {
                btnSkipQuest.setVisibility(View.GONE);
            }

            return;
        }

        if (DailySkill.STATUS_SKIPPED.equals(status)) {
            btnDone.setEnabled(false);
            btnDone.setText("Quest Skipped");

            if (tvQuestTimer != null) {
                tvQuestTimer.setText("Skipped");
            }

            if (btnSkipQuest != null) {
                btnSkipQuest.setVisibility(View.GONE);
            }

            return;
        }

        if (DailySkill.STATUS_RUNNING.equals(status)) {
            btnDone.setEnabled(false);
            btnDone.setText("Sedang Berjalan...");

            if (btnSkipQuest != null) {
                btnSkipQuest.setVisibility(View.VISIBLE);
            }

            if (questTimer == null || runningTimerRecordId != record.id) {
                startQuestTimer();
            }
            return;
        }

        if (DailySkill.STATUS_READY.equals(status)) {
            btnDone.setEnabled(true);
            btnDone.setText("Claim Reward!");

            if (tvQuestTimer != null) {
                tvQuestTimer.setText("Ready!");
            }

            if (btnSkipQuest != null) {
                btnSkipQuest.setVisibility(View.GONE);
            }

            return;
        }

        btnDone.setEnabled(true);
        btnDone.setText("Start Quest (" + (count + 1) + "/3)");

        if (tvQuestTimer != null) {
            tvQuestTimer.setText("Selesaikan kapan saja hari ini");
        }

        if (btnSkipQuest != null) {
            btnSkipQuest.setVisibility(View.GONE);
        }
    }

    private void updateStatsUI(User user) {
        tvLevel.setText("Lv. " + user.level);

        if (tvTierLabel != null) {
            tvTierLabel.setText(getTierLabel(user.level));
        }

        int requiredXp = MainViewModel.getRequiredXpForLevel(user.level);

        pbXp.setMax(requiredXp);
        animateXpBar(lastRenderedXp, user.xp);
        lastRenderedXp = user.xp;

        Boolean bonusActive = viewModel.getStreakBonusActive().getValue();
        updateXpLabel(user, Boolean.TRUE.equals(bonusActive));

        tvStreak.setText(String.valueOf(user.streak));
    }

    private void animateXpBar(int fromXp, int toXp) {
        if (pbXp == null) return;

        ObjectAnimator anim = ObjectAnimator.ofInt(pbXp, "progress", fromXp, toXp);
        anim.setDuration(600);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void updateXpLabel(User user, boolean bonusActive) {
        if (tvXp == null) return;

        int requiredXp = MainViewModel.getRequiredXpForLevel(user.level);

        if (bonusActive) {
            tvXp.setText("⚡ " + user.xp + " / " + requiredXp + " XP  [STREAK BONUS!]");
            tvXp.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            tvXp.setText(user.xp + " / " + requiredXp + " XP");
            tvXp.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void updateSkillUI(Skill skill) {
        tvSkillTitle.setText(skill.title);

        String imageUrl = SkillCategory.iconUrlFor(skill.category);

        Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivIllustration);

        int colorRes = SkillCategory.getColorResForCategory(skill.category);
        int resolvedColor = ContextCompat.getColor(requireContext(), colorRes);

        if (cardSkill != null) {
            cardSkill.setStrokeColor(resolvedColor);
        }

        if (tvSkillCategoryLabel != null) {
            tvSkillCategoryLabel.setTextColor(resolvedColor);
            tvSkillCategoryLabel.setText(skill.category.toUpperCase() + " SKILL");
        }
    }

    private String getTierLabel(int level) {
        return TierLabelProvider.homeTier(level);
    }

    private void playSfx() {
        try {
            MediaPlayer player = MediaPlayer.create(requireContext(), R.raw.sfx_level_up);
            player.start();
            player.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception ignored) {
        }
    }

    private void showLottie() {
        if (lottieSuccess != null) {
            lottieSuccess.setVisibility(View.VISIBLE);
            lottieSuccess.bringToFront();
            lottieSuccess.setTranslationZ(99f);
            lottieSuccess.cancelAnimation();
            lottieSuccess.playAnimation();
        }
    }

    private void hideLottie() {
        if (lottieSuccess != null) {
            lottieSuccess.cancelAnimation();
            lottieSuccess.setVisibility(View.GONE);
        }
    }

    private void showSuccessDialog(int gainedXp, int currentStreak) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        dialog.setCancelable(false);

        TextView tvDialogXp = dialog.findViewById(R.id.tv_dialog_xp);
        TextView tvDialogStreak = dialog.findViewById(R.id.tv_dialog_streak);
        MaterialButton btnLanjut = dialog.findViewById(R.id.btn_dialog_lanjut);

        tvDialogXp.setText("+" + gainedXp + " XP");
        tvDialogStreak.setText(currentStreak + " hari 🔥");

        btnLanjut.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getDecorView().setPadding(0, 0, 0, 0);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.dimAmount = 0.65f;

            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private int safeProgress() {
        Integer val = viewModel.getDailyProgress().getValue();
        return val != null ? val : 0;
    }

    private void setupCardTouchAnimation() {
        if (cardSkill == null) return;

        cardSkill.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(80)
                            .start();
                    break;

                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start();
                    break;
            }

            return false;
        });
    }
}
