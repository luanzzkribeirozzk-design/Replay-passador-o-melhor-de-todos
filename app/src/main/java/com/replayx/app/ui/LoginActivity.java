package com.replayx.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.replayx.app.databinding.ActivityLoginBinding;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseFirestore db;
    private static final String PREFS = "replayx_prefs";
    private static final String PREF_KEY = "saved_key";
    private static final String PREF_REMEMBER = "remember_key";
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean remember = prefs.getBoolean(PREF_REMEMBER, false);
        String savedKey = prefs.getString(PREF_KEY, "");

        binding.switchRemember.setChecked(remember);
        if (remember && savedKey != null && !savedKey.isEmpty()) {
            binding.etKey.setText(savedKey);
        }

        binding.btnLogin.setOnClickListener(v -> doLogin());
        binding.etKey.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { doLogin(); return true; }
            return false;
        });
    }

    private void doLogin() {
        String key = binding.etKey.getText().toString().trim();
        if (key.isEmpty()) { showError("[ERR] Insira sua key!"); return; }
        setLoading(true);
        hideError();

        db.collection("keys")
            .whereEqualTo("keyString", key)
            .get()
            .addOnSuccessListener(docs -> {
                setLoading(false);
                if (docs.isEmpty()) { showError("[ERR] Key invalida"); return; }
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) docs.getDocuments().get(0);
                Map<String, Object> data = doc.getData();
                if (data == null) { showError("[ERR] Dados invalidos"); return; }

                String status = (String) data.get("status");
                if ("paused".equals(status)) { showError("[ERR] Key pausada"); return; }
                if (!"active".equals(status)) { showError("[ERR] Key inativa"); return; }

                String deviceId = (String) data.get("deviceId");
                String myDevice = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                if (deviceId != null && !deviceId.equals(myDevice)) {
                    showError("[ERR] Key em uso em outro dispositivo");
                    return;
                }

                if (deviceId == null) {
                    java.util.Map<String, Object> upd = new java.util.HashMap<>();
                    upd.put("deviceId", myDevice);
                    upd.put("firstUsed", Timestamp.now());
                    doc.getReference().update(upd)
                        .addOnSuccessListener(v -> proceedLogin(data, myDevice))
                        .addOnFailureListener(e -> showError("[ERR] " + e.getMessage()));
                } else {
                    proceedLogin(data, myDevice);
                }
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                showError("[ERR] " + e.getMessage());
            });
    }

    private void proceedLogin(Map<String, Object> data, String myDevice) {
        int days = data.get("days") instanceof Long ? ((Long) data.get("days")).intValue() : 0;
        Timestamp firstUsedTs = (Timestamp) data.get("firstUsed");
        Timestamp pausedAtTs = (Timestamp) data.get("pausedAt");
        String status = (String) data.get("status");
        String user = data.get("user") instanceof String ? (String) data.get("user") : "";

        if (firstUsedTs != null) {
            long nowSec = System.currentTimeMillis() / 1000L;
            long usedSec = ("paused".equals(status) && pausedAtTs != null)
                ? pausedAtTs.getSeconds() - firstUsedTs.getSeconds()
                : nowSec - firstUsedTs.getSeconds();
            long remainDays = days - (usedSec / 86400L);
            if (remainDays <= 0) { showError("[ERR] Key expirada"); return; }
        }

        String keyStr = binding.etKey.getText().toString().trim();
        boolean remember = binding.switchRemember.isChecked();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(PREF_REMEMBER, remember)
            .putString(PREF_KEY, remember ? keyStr : "")
            .apply();

        long firstUsedSec = firstUsedTs != null ? firstUsedTs.getSeconds() : System.currentTimeMillis() / 1000L;
        long pausedAtSec = pausedAtTs != null ? pausedAtTs.getSeconds() : 0L;

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("key_user", user);
        intent.putExtra("key_days", days);
        intent.putExtra("key_first_used_sec", firstUsedSec);
        intent.putExtra("key_status", status);
        intent.putExtra("key_paused_at_sec", pausedAtSec);
        startActivity(intent);
        finish();
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvError.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
