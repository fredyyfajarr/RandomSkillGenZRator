package id.kelompok1.randomskillgen_zrator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.kelompok1.randomskillgen_zrator.database.AppRepository;
import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;

/**
 * PERUBAHAN DARI VERSI LAMA:
 * 1. Race condition requireActivity() di background thread → diganti cek isAdded() yang aman.
 * 2. DB query per-item di Adapter → diganti batch load Map<skillId,Skill> di Fragment.
 * 3. AppDatabase.getInstance() langsung → pakai AppRepository.
 * 4. Executor tidak di-shutdown → pakai shared executor dari Repository.
 */
public class HistoryFragment extends Fragment {

    private RecyclerView     rvHistory;
    private HistoryAdapter   adapter;
    private LinearLayout     layoutEmpty;
    private LinearLayout     layoutFilterChip;
    private TextView         tvEmptyHistory;
    private TextView         tvActiveFilter;

    // Data lengkap yang sudah ter-load (untuk filter lokal tanpa query ulang)
    private List<DailySkill>    allCompletedQuests = new ArrayList<>();
    private Map<Integer, Skill> skillMap           = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory      = view.findViewById(R.id.rv_history);
        layoutEmpty    = view.findViewById(R.id.layout_empty_state);
        layoutFilterChip = view.findViewById(R.id.layout_filter_chip);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        tvActiveFilter = view.findViewById(R.id.tv_active_filter);
        ImageView btnFilter = view.findViewById(R.id.btn_filter_date);
        MaterialButton btnClearFilter = view.findViewById(R.id.btn_clear_filter);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        btnFilter.setOnClickListener(v -> showDatePickerFilter());
        btnClearFilter.setOnClickListener(v -> clearDateFilter());

        loadHistoryData();
        return view;
    }

    // ---------------------------------------------------------------------------
    // loadHistoryData — satu kali query, load semua yang dibutuhkan
    // ---------------------------------------------------------------------------
    private void loadHistoryData() {
        AppRepository repo = AppRepository.getInstance(requireContext());

        repo.getExecutor().execute(() -> {
            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fUser == null) return;
            String uid = fUser.getUid();

            AppRepository.HistoryResult result = repo.getCompletedHistoryWithSkills(uid);

            // Guard sebelum menyentuh UI — fragment mungkin sudah detach
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                allCompletedQuests.clear();
                allCompletedQuests.addAll(result.records);
                skillMap.clear();
                skillMap.putAll(result.skillMap);

                hideFilterChip();
                renderList(allCompletedQuests);
            });
        });
    }

    // ---------------------------------------------------------------------------
    // renderList — tampilkan daftar (atau empty state)
    // ---------------------------------------------------------------------------
    private void renderList(List<DailySkill> list) {
        adapter = new HistoryAdapter(list, skillMap, this::showQuestDetail);
        rvHistory.setAdapter(adapter);

        boolean isEmpty = list.isEmpty();
        rvHistory.setVisibility(isEmpty ? View.GONE  : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        tvEmptyHistory.setVisibility(View.GONE);
    }

    // ---------------------------------------------------------------------------
    // showQuestDetail — semua data sudah ada di skillMap, tidak perlu query baru
    // ---------------------------------------------------------------------------
    private void showQuestDetail(DailySkill record) {
        Skill s = skillMap.get(record.skill_id);
        if (s == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.layout_quest_detail, null);

        TextView tvCategory = v.findViewById(R.id.tv_detail_category);
        TextView tvTitle = v.findViewById(R.id.tv_detail_title);
        TextView tvDate = v.findViewById(R.id.tv_detail_date);
        TextView tvXp = v.findViewById(R.id.tv_detail_xp);
        TextView tvDifficulty = v.findViewById(R.id.tv_detail_difficulty);
        TextView tvDuration = v.findViewById(R.id.tv_detail_duration);

        String time = (record.completed_at != null && !record.completed_at.trim().isEmpty())
                ? record.completed_at
                : "--:--";

        String difficulty = (s.difficulty != null && !s.difficulty.trim().isEmpty())
                ? s.difficulty
                : "Medium";

        int duration = s.duration_minutes > 0 ? s.duration_minutes : 5;

        tvCategory.setText(s.category.toUpperCase() + " QUEST");
        tvTitle.setText(s.title);
        tvDate.setText("Selesai: " + record.date + " • " + time);
        tvXp.setText("+" + s.xp_reward + " XP");
        tvDifficulty.setText(difficulty);
        tvDuration.setText("Estimasi durasi: ±" + duration + " menit");

        int color = androidx.core.content.ContextCompat.getColor(
                requireContext(),
                id.kelompok1.randomskillgen_zrator.database.SkillCategory.getColorResForCategory(s.category)
        );

        tvCategory.setTextColor(color);
        tvXp.setTextColor(color);

        dialog.setContentView(v);
        dialog.show();
    }

    // ---------------------------------------------------------------------------
    // showDatePickerFilter — filter lokal tanpa DB call tambahan
    // ---------------------------------------------------------------------------
    private void showDatePickerFilter() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> {
                    String date = String.format(
                            java.util.Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, day
                    );
                    applyDateFilter(date);
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void applyDateFilter(String date) {
        showFilterChip(date);

        List<DailySkill> filtered = new ArrayList<>();
        for (DailySkill r : allCompletedQuests) {
            if (r.date.equals(date)) filtered.add(r);
        }

        if (filtered.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            tvEmptyHistory.setVisibility(View.VISIBLE);
            tvEmptyHistory.setText("Belum ada quest di tanggal " + date + " 🍂");
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }

        adapter = new HistoryAdapter(filtered, skillMap, this::showQuestDetail);
        rvHistory.setAdapter(adapter);
    }

    private void clearDateFilter() {
        hideFilterChip();
        renderList(allCompletedQuests);
    }

    private void showFilterChip(String date) {
        if (layoutFilterChip != null) {
            layoutFilterChip.setVisibility(View.VISIBLE);
        }
        if (tvActiveFilter != null) {
            tvActiveFilter.setText("Tanggal: " + date);
        }
    }

    private void hideFilterChip() {
        if (layoutFilterChip != null) {
            layoutFilterChip.setVisibility(View.GONE);
        }
    }
}
