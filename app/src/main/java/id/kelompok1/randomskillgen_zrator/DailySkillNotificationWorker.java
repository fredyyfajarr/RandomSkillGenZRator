package id.kelompok1.randomskillgen_zrator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker yang dijalankan WorkManager setiap hari oleh NotificationScheduler.
 * Menampilkan notifikasi reminder quest harian.
 *
 * Cara schedule: panggil NotificationScheduler.scheduleDailyReminder(context)
 * dari MainActivity.onCreate() — cukup sekali, WorkManager menyimpan jadwal
 * secara persisten dan menjalankannya tiap 24 jam meski app ditutup.
 */
public class DailySkillNotificationWorker extends Worker {

    private static final String CHANNEL_ID   = "daily_quest_channel";
    private static final int    NOTIF_ID     = 1001;

    private static final String[] MESSAGES = {
            "Quest harianmu menunggu, Hero! ⚔️ Jangan lewatkan streak-mu hari ini.",
            "Hari baru, skill baru! 🌟 Selesaikan quest dan kumpulkan XP-mu.",
            "Streak kamu butuh kamu hari ini 🔥 Buka app dan selesaikan misimu!",
            "Random skill menunggumu 🎲 Siap level up hari ini?",
            "5 menit saja, Hero! ✨ Quest harianmu sudah siap."
    };

    public DailySkillNotificationWorker(@NonNull Context context,
                                        @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        createChannelIfNeeded(context);

        // Pilih pesan acak
        String message = MESSAGES[(int) (Math.random() * MESSAGES.length)];

        // Tap notif → buka MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle("Random Skill Gen-ZRator ⚡")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIF_ID, builder.build());
        }

        return Result.success();
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Quest Harian",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Pengingat untuk menyelesaikan quest skill harian.");

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}