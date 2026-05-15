package id.kelompok1.randomskillgen_zrator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth; // <-- Import ini

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("GenZPrefs", MODE_PRIVATE);
            boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

            // 🔥 Cek langsung ke Firebase, bukan ke SharedPreferences lagi!
            boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

            if (isFirstTime) {
                startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
            } else if (!isLoggedIn) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish();
        }, 2000);
    }
}