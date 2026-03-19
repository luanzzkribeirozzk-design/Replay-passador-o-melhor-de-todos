package com.replayx.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.replayx.app.databinding.ActivityLoginBinding;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final String PREFS = "replayx_prefs";
    private static final String PREF_KEY = "saved_key";
    private static final String PREF_REM = "remember_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean rem = prefs.getBoolean(PREF_REM, false);
        String saved = prefs.getString(PREF_KEY, "");
        binding.switchRemember.setChecked(rem);
        if (rem && saved != null && !saved.isEmpty()) {
            binding.etKey.setText(saved);
        }

        binding.btnLogin.setOnClickListener(v -> doLogin());
        binding.etKey.setOnEditorActionListener((v, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_DONE) { doLogin(); return true; }
            return false;
        });
    }

    private void doLogin() {
        String key = binding.etKey.getText().toString().trim();
        if (key.isEmpty()) { showErr("[ERR] Insira sua key!"); return; }
        setLoad(true);
        binding.tvError.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
            .collection("keys")
            .whereEqualTo("keyString", key)
            .get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot docs) {
                    setLoad(false);
                    if (docs.isEmpty()) { showErr("[ERR] Key invalida"); return; }
                    DocumentSnapshot doc = docs.getDocuments().get(0);
                    Map<String, Object> data = doc.getData();
                    if (data == null) { showErr("[ERR] Dados invalidos"); return; }

                    String status = data.get("status") instanceof String ? (String) data.get("status") : "";
                    if ("paused".equals(status)) { showErr("[ERR] Key pausada"); return; }
                    if (!"active".equals(status)) { showErr("[ERR] Key inativa"); return; }

                    String devId = data.get("deviceId") instanceof String ? (String) data.get("deviceId") : null;
                    String myDev = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                    if (devId != null && !devId.equals(myDev)) {
                        showErr("[ERR] Key em outro dispositivo"); return;
                    }

                    if (devId == null) {
                        Map<String, Object> upd = new HashMap<>();
                        upd.put("deviceId", myDev);
                        upd.put("firstUsed", Timestamp.now());
                        doc.getReference().update(upd)
                            .addOnSuccessListener(v2 -> proceed(data, myDev))
                            .addOnFailureListener(e -> showErr("[ERR] " + e.getMessage()));
                    } else {
                        proceed(data, myDev);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    setLoad(false);
                    showErr("[ERR] " + e.getMessage());
                }
            });
    }

    private void proceed(Map<String, Object> data, String myDev) {
        int days = data.get("days") instanceof Long ? ((Long) data.get("days")).intValue() : 0;
        Timestamp firstTs = data.get("firstUsed") instanceof Timestamp ? (Timestamp) data.get("firstUsed") : null;
        Timestamp pauseTs = data.get("pausedAt") instanceof Timestamp ? (Timestamp) data.get("pausedAt") : null;
        String status = data.get("status") instanceof String ? (String) data.get("status") : "active";
        String user = data.get("user") instanceof String ? (String) data.get("user") : "";

        if (firstTs != null) {
            long nowSec = System.currentTimeMillis() / 1000L;
            long usedSec = ("paused".equals(status) && pauseTs != null)
                ? pauseTs.getSeconds() - firstTs.getSeconds()
                : nowSec - firstTs.getSeconds();
            if (days - (usedSec / 86400L) <= 0) { showErr("[ERR] Key expirada"); return; }
        }

        boolean rem = binding.switchRemember.isChecked();
        String keyStr = binding.etKey.getText().toString().trim();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(PREF_REM, rem)
            .putString(PREF_KEY, rem ? keyStr : "")
            .apply();

        long firstSec = firstTs != null ? firstTs.getSeconds() : System.currentTimeMillis() / 1000L;
        long pauseSec = pauseTs != null ? pauseTs.getSeconds() : 0L;

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("key_user", user);
        i.putExtra("key_days", days);
        i.putExtra("key_first_used_sec", firstSec);
        i.putExtra("key_status", status);
        i.putExtra("key_paused_at_sec", pauseSec);
        startActivity(i);
        finish();
    }

    private void showErr(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void setLoad(boolean on) {
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!on);
    }
}
