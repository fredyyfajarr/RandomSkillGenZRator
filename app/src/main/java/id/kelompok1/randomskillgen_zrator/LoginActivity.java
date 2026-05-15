package id.kelompok1.randomskillgen_zrator;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import id.kelompok1.randomskillgen_zrator.data.FirebaseSyncManager;
import id.kelompok1.randomskillgen_zrator.database.AppRepository;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore firestoreDb;
    private AppRepository repo;

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());

                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);

                            if (account == null || account.getIdToken() == null) {
                                Toast.makeText(this, "Token Google tidak ditemukan.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.e(TAG, "Google Sign-In failed: code=" + e.getStatusCode(), e);
                            Toast.makeText(this, "Sign-In Gagal. Coba lagi.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        firestoreDb = FirebaseFirestore.getInstance();
        repo = AppRepository.getInstance(this);

        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        )
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btn_login_google).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.btn_signup_google).setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        signInLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(this, authResult -> {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();

                    if (firebaseUser != null) {
                        syncUserWithCloud(firebaseUser);
                    } else {
                        Toast.makeText(this, "User Firebase tidak ditemukan.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Firebase Auth failed", e);
                    Toast.makeText(this, "Autentikasi gagal. Coba lagi.", Toast.LENGTH_SHORT).show();
                });
    }

    private void syncUserWithCloud(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        firestoreDb.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        loadExistingCloudUser(uid, doc);
                    } else {
                        createNewCloudUser(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore get user failed", e);
                    Toast.makeText(
                            this,
                            "Gagal mengambil data cloud. Menggunakan data lokal.",
                            Toast.LENGTH_SHORT
                    ).show();

                    goToMain();
                });
    }

    private void loadExistingCloudUser(String uid, DocumentSnapshot doc) {
        int cloudLevel = safeInt(doc, "level", 1);
        int cloudXp = safeInt(doc, "xp", 0);
        int cloudStreak = safeInt(doc, "streak", 0);
        int cloudBestStreak = safeInt(doc, "best_streak", cloudStreak);
        int cloudTotalActiveDays = safeInt(doc, "total_active_days", 0);
        String cloudLastActiveDate = doc.getString("last_active_date");

        saveToLocalFromCloud(
                uid,
                cloudLevel,
                cloudXp,
                cloudStreak,
                cloudBestStreak,
                cloudTotalActiveDays,
                cloudLastActiveDate
        );
    }

    private void createNewCloudUser(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", firebaseUser.getDisplayName());
        data.put("email", firebaseUser.getEmail());
        data.put("level", 1);
        data.put("xp", 0);
        data.put("streak", 0);
        data.put("best_streak", 0);
        data.put("total_active_days", 0);
        data.put("last_active_date", null);

        firestoreDb.collection("users")
                .document(uid)
                .set(data)
                .addOnSuccessListener(v -> saveToLocalFromCloud(
                        uid,
                        1,
                        0,
                        0,
                        0,
                        0,
                        null
                ))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore new user creation failed", e);

                    Toast.makeText(
                            this,
                            "Cloud gagal dibuat. Data lokal tetap disiapkan.",
                            Toast.LENGTH_SHORT
                    ).show();

                    saveToLocalFromCloud(
                            uid,
                            1,
                            0,
                            0,
                            0,
                            0,
                            null
                    );
                });
    }

    private void saveToLocalFromCloud(
            String uid,
            int level,
            int xp,
            int streak,
            int bestStreak,
            int totalActiveDays,
            String lastActiveDate
    ) {
        repo.getExecutor().execute(() -> {
            repo.saveCloudUserIfNotStale(
                    uid,
                    new FirebaseSyncManager.CloudUser(
                    level,
                    xp,
                    streak,
                    bestStreak,
                    totalActiveDays,
                    lastActiveDate
                    )
            );

            runOnUiThread(this::goToMain);
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private int safeInt(DocumentSnapshot doc, String field, int defaultValue) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : defaultValue;
    }
}
