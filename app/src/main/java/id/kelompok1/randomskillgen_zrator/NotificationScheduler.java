package id.kelompok1.randomskillgen_zrator;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Helper untuk menjadwalkan notifikasi harian jam 08.00.
 *
 * Panggil scheduleDailyReminder(context) dari MainActivity.onCreate().
 * WorkManager menyimpan jadwal secara persisten — aman dipanggil berulang
 * karena pakai KEEP policy (tidak reset timer jika sudah terjadwal).
 *
 * Syarat: tambahkan di build.gradle (app):
 *   implementation "androidx.work:work-runtime:2.9.0"
 *
 * Dan di AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
 *   (untuk Android 13+, minta permission di runtime dari MainActivity)
 */
public final class NotificationScheduler {

    private static final String WORK_TAG = "daily_quest_reminder";

    private NotificationScheduler() {}

    public static void scheduleDailyReminder(Context context) {
        // Hitung delay sampai jam 08:00 berikutnya
        Calendar now    = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 8);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);

        // Jika sekarang sudah lewat jam 08:00, target besok
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(
                        DailySkillNotificationWorker.class,
                        24, TimeUnit.HOURS
                )
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build();

        // KEEP: jika sudah ada jadwal, jangan reset timer
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}