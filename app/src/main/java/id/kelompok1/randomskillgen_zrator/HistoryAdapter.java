package id.kelompok1.randomskillgen_zrator;

import android.graphics.Color;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import id.kelompok1.randomskillgen_zrator.database.DailySkill;
import id.kelompok1.randomskillgen_zrator.database.Skill;
import id.kelompok1.randomskillgen_zrator.database.SkillCategory;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DailySkill record);
    }

    private final List<DailySkill> historyList;
    private final Map<Integer, Skill> skillMap;
    private final OnItemClickListener listener;

    public HistoryAdapter(
            List<DailySkill> historyList,
            Map<Integer, Skill> skillMap,
            OnItemClickListener listener
    ) {
        this.historyList = historyList;
        this.skillMap = skillMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailySkill record = historyList.get(position);
        Skill skill = skillMap.get(record.skill_id);

        if (skill == null) {
            holder.tvTitle.setText("Quest tidak ditemukan");
            holder.tvCategory.setText("UNKNOWN");
            holder.tvMeta.setText("+0 XP");
            holder.tvDate.setText(formatDateTime(record));
            holder.tvIcon.setText("?");
            holder.itemView.setOnClickListener(v -> listener.onItemClick(record));
            return;
        }

        holder.tvTitle.setText(skill.title);
        holder.tvCategory.setText(skill.category.toUpperCase());
        holder.tvIcon.setText(iconForCategory(skill.category));

        String difficulty = safeDifficulty(skill);
        int duration = safeDuration(skill);

        holder.tvMeta.setText("+" + skill.xp_reward + " XP • " + difficulty + " • ±" + duration + " menit");
        holder.tvDate.setText(formatDateTime(record));

        int colorRes = SkillCategory.getColorResForCategory(skill.category);
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);

        holder.tvCategory.setTextColor(color);
        holder.tvIcon.setTextColor(color);
        holder.tvIcon.setBackgroundTintList(ColorStateList.valueOf(darkenColor(color)));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(record));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    private String formatDateTime(DailySkill record) {
        String time = (record.completed_at != null && !record.completed_at.trim().isEmpty())
                ? record.completed_at
                : "--:--";

        return record.date + " • " + time;
    }

    private String safeDifficulty(Skill skill) {
        if (skill.difficulty == null || skill.difficulty.trim().isEmpty()) {
            return "Medium";
        }
        return skill.difficulty;
    }

    private int safeDuration(Skill skill) {
        return skill.duration_minutes > 0 ? skill.duration_minutes : 5;
    }

    private String iconForCategory(String category) {
        if (SkillCategory.HEALTH.equals(category)) return "🏃";
        if (SkillCategory.PRODUCTIVE.equals(category)) return "⚡";
        if (SkillCategory.EDUCATION.equals(category)) return "📚";
        if (SkillCategory.FUN.equals(category)) return "🎮";
        return "✨";
    }

    private int darkenColor(int color) {
        int r = Math.max((int) (Color.red(color) * 0.22f), 24);
        int g = Math.max((int) (Color.green(color) * 0.22f), 24);
        int b = Math.max((int) (Color.blue(color) * 0.22f), 24);
        return Color.rgb(r, g, b);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIcon;
        final TextView tvCategory;
        final TextView tvTitle;
        final TextView tvMeta;
        final TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_history_icon);
            tvCategory = itemView.findViewById(R.id.tv_history_category);
            tvTitle = itemView.findViewById(R.id.tv_history_title);
            tvMeta = itemView.findViewById(R.id.tv_history_meta);
            tvDate = itemView.findViewById(R.id.tv_history_date);
        }
    }
}