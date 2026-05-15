package id.kelompok1.randomskillgen_zrator;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import id.kelompok1.randomskillgen_zrator.database.Achievement;
import id.kelompok1.randomskillgen_zrator.database.AppRepository;

public class StatsViewModel extends AndroidViewModel {

    public static class StatsData {
        public final int totalCompleted;
        public final int totalXp;
        public final Map<String, Integer> categoryCount;
        public final List<Achievement> achievements;
        public final Map<String, AppRepository.AchievementProgress> achievementProgressMap;

        StatsData(
                int totalCompleted,
                int totalXp,
                Map<String, Integer> categoryCount,
                List<Achievement> achievements,
                Map<String, AppRepository.AchievementProgress> achievementProgressMap
        ) {
            this.totalCompleted = totalCompleted;
            this.totalXp = totalXp;
            this.categoryCount = categoryCount;
            this.achievements = achievements;
            this.achievementProgressMap = achievementProgressMap;
        }
    }

    private final MutableLiveData<StatsData> statsData = new MutableLiveData<>();

    private final AppRepository repo;
    private final ExecutorService executor;

    public StatsViewModel(@NonNull Application application) {
        super(application);
        repo = AppRepository.getInstance(application);
        executor = repo.getExecutor();
        loadStats();
    }

    public LiveData<StatsData> getStatsData() {
        return statsData;
    }

    public void loadStats() {
        executor.execute(() -> {
            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fUser == null) return;

            String uid = fUser.getUid();

            AppRepository.StatsResult result = repo.getStatsForUser(uid);
            List<Achievement> achievements = repo.getAllAchievements();
            Map<String, AppRepository.AchievementProgress> progressMap =
                    repo.getAchievementProgressMap(uid);

            statsData.postValue(new StatsData(
                    result.totalCompleted,
                    result.totalXp,
                    result.categoryCount,
                    achievements,
                    progressMap
            ));
        });
    }

    @Nullable
    private String currentUid() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        return fUser != null ? fUser.getUid() : null;
    }
}