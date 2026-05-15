package id.kelompok1.randomskillgen_zrator;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import id.kelompok1.randomskillgen_zrator.database.Achievement;
import id.kelompok1.randomskillgen_zrator.database.AppRepository;

public class StatsFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvTotalQuest;
    private TextView tvTotalXp;

    private View layoutChartContainer;
    private View layoutEmptyStats;
    private ViewGroup layoutAchievements;

    private StatsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pie_chart_stats);
        tvTotalQuest = view.findViewById(R.id.tv_stats_total_quest);
        tvTotalXp = view.findViewById(R.id.tv_stats_total_xp);
        layoutChartContainer = view.findViewById(R.id.layout_chart_container);
        layoutEmptyStats = view.findViewById(R.id.layout_empty_stats);
        layoutAchievements = view.findViewById(R.id.layout_achievements);

        setupChartAppearance();

        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);
        viewModel.getStatsData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;

            tvTotalQuest.setText(String.valueOf(data.totalCompleted));
            tvTotalXp.setText(String.valueOf(data.totalXp));

            renderAchievements(data.achievements, data.achievementProgressMap);

            if (data.totalCompleted == 0 || data.categoryCount == null || data.categoryCount.isEmpty()) {
                showEmptyState();
            } else {
                showChart();
                populateChart(data.categoryCount);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadStats();
        }
    }

    private void renderAchievements(
            List<Achievement> achievements,
            Map<String, AppRepository.AchievementProgress> progressMap
    ) {
        if (layoutAchievements == null) return;

        layoutAchievements.removeAllViews();

        if (achievements == null || achievements.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Belum ada achievement.");
            empty.setTextColor(color(R.color.text_secondary));
            empty.setTextSize(13f);
            layoutAchievements.addView(empty);
            return;
        }

        for (Achievement achievement : achievements) {
            AppRepository.AchievementProgress progress = null;
            if (progressMap != null) {
                progress = progressMap.get(achievement.id);
            }

            layoutAchievements.addView(createAchievementCard(achievement, progress));
        }
    }

    private View createAchievementCard(
            Achievement achievement,
            @Nullable AppRepository.AchievementProgress progress
    ) {
        MaterialCardView card = new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(color(R.color.surface_card));
        card.setStrokeColor(achievement.unlocked
                ? color(R.color.accent_yellow)
                : color(R.color.gray_default));

        if (!achievement.unlocked) {
            card.setAlpha(0.82f);
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(requireContext());
        icon.setText(achievement.icon != null ? achievement.icon : "🏆");
        icon.setTextSize(24f);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundTintList(ColorStateList.valueOf(color(R.color.surface_card_alt)));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.setMargins(0, 0, dp(12), 0);
        icon.setLayoutParams(iconParams);

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView title = new TextView(requireContext());
        title.setText((achievement.unlocked ? "Unlocked • " : "Locked • ") + achievement.title);
        title.setTextSize(14f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(achievement.unlocked
                ? color(R.color.text_primary)
                : color(R.color.text_secondary));

        TextView desc = new TextView(requireContext());
        desc.setText(achievement.description);
        desc.setTextSize(12f);
        desc.setTextColor(color(R.color.text_secondary));
        desc.setPadding(0, dp(4), 0, 0);

        texts.addView(title);
        texts.addView(desc);

        TextView status = new TextView(requireContext());
        status.setText(achievement.unlocked ? "✓" : "🔒");
        status.setTextSize(18f);
        status.setTextColor(achievement.unlocked
                ? color(R.color.accent_yellow)
                : color(R.color.text_muted));

        row.addView(icon);
        row.addView(texts);
        row.addView(status);

        root.addView(row);

        if (progress != null) {
            TextView progressText = new TextView(requireContext());
            progressText.setText(progress.current + " / " + progress.target);
            progressText.setTextSize(12f);
            progressText.setTextColor(color(R.color.text_secondary));
            progressText.setPadding(0, dp(12), 0, dp(6));
            root.addView(progressText);

            ProgressBar bar = new ProgressBar(
                    requireContext(),
                    null,
                    android.R.attr.progressBarStyleHorizontal
            );
            bar.setMax(progress.target);
            bar.setProgress(progress.current);
            bar.setProgressTintList(ColorStateList.valueOf(color(R.color.accent_yellow)));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(color(R.color.surface_card_alt)));

            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(8)
            );
            bar.setLayoutParams(barParams);
            root.addView(bar);
        }

        card.addView(root);
        return card;
    }

    private void showEmptyState() {
        if (layoutChartContainer != null) {
            layoutChartContainer.setVisibility(View.GONE);
        }

        if (layoutEmptyStats != null) {
            layoutEmptyStats.setVisibility(View.VISIBLE);
        }

        if (pieChart != null) {
            pieChart.clear();
        }
    }

    private void showChart() {
        if (layoutChartContainer != null) {
            layoutChartContainer.setVisibility(View.VISIBLE);
        }

        if (layoutEmptyStats != null) {
            layoutEmptyStats.setVisibility(View.GONE);
        }
    }

    private void setupChartAppearance() {
        if (pieChart == null) return;

        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(color(R.color.surface_card));
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setHoleRadius(50f);
        pieChart.setDrawEntryLabels(false);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(color(R.color.text_primary));
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);
    }

    private void populateChart(Map<String, Integer> categoryCount) {
        if (pieChart == null || categoryCount == null || categoryCount.isEmpty()) {
            showEmptyState();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) continue;
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            showEmptyState();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(color(R.color.blue_productive));
        colors.add(color(R.color.green_health));
        colors.add(color(R.color.yellow_fun));
        colors.add(color(R.color.purple_education));

        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextColor(color(R.color.text_primary));
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.invalidate();
        pieChart.animateY(1000);
    }

    private int color(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
