package id.kelompok1.randomskillgen_zrator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private MaterialButton btnMulai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        btnMulai = findViewById(R.id.btn_mulai);

        btnMulai.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("GenZPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("isFirstTime", false)
                    .apply();

            startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
            finish();
        });
    }
}