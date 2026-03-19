package com.replayx.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.replayx.app.databinding.ActivityLoginBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final String PREFS = "replayx_prefs";
    private static final String PREF_KEY = "saved_key";
    private static final String PREF_REM = "remember_key";
    private static final String PROJECT = "principal-6bf6f";
    private static final String API_KEY = "AIzaSyAmXzPrNaK_-Zr190oB8MuxA_sqI_ctetc";
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

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
        String myDev = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        exec.execute(() -> {
            try {
                // Buscar key no Firestore REST API
                String base = "https://firestore.googleapis.com/v1/projects/" + PROJECT + "/databases/(default)/documents/keys";
                String query = base + "?key=" + API_KEY;

                // Usar runQuery para buscar por keyString
                String runQuery = "https://firestore.googleapis.com/v1/projects/" + PROJECT + "/databases/(default)/documents:runQuery?key=" + API_KEY;
                JSONObject body = new JSONObject();
                JSONObject sq = new JSONObject();
                JSONObject from = new JSONObject();
                from.put("collectionId", "keys");
                sq.put("from", new JSONArray().put(from));
                JSONObject where = new JSONObject();
                JSONObject fc = new JSONObject();
                fc.put("field", new JSONObject().put("fieldPath", "keyString"));
                fc.put("op", "EQUAL");
                fc.put("value", new JSONObject().put("stringValue", key));
                where.put("fieldFilter", fc);
                sq.put("where", where);
                body.put("structuredQuery", sq);

                URL url = new URL(runQuery);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.next());
                sc.close();
                conn.disconnect();

                JSONArray results = new JSONArray(sb.toString());
                if (results.length() == 0 || !results.getJSONObject(0).has("document")) {
                    main.post(() -> { setLoad(false); showErr("[ERR] Key invalida"); });
                    return;
                }

                JSONObject doc = results.getJSONObject(0).getJSONObject("document");
                String docName = doc.getString("name");
                JSONObject fields = doc.getJSONObject("fields");

                String status = fields.has("status") ? fields.getJSONObject("status").optString("stringValue","") : "";
                if ("paused".equals(status)) { main.post(() -> { setLoad(false); showErr("[ERR] Key pausada"); }); return; }
                if (!"active".equals(status)) { main.post(() -> { setLoad(false); showErr("[ERR] Key inativa"); }); return; }

                String devId = fields.has("deviceId") ? fields.getJSONObject("deviceId").optString("stringValue","null") : "null";
                boolean noDevice = "null".equals(devId) || devId.isEmpty();

                if (!noDevice && !devId.equals(myDev)) {
                    main.post(() -> { setLoad(false); showErr("[ERR] Key em outro dispositivo"); });
                    return;
                }

                long nowSec = System.currentTimeMillis() / 1000L;
                long firstSec = nowSec;
                long pauseSec = 0L;
                int days = fields.has("days") ? (int) fields.getJSONObject("days").optLong("integerValue", 0) : 0;
                String user = fields.has("user") ? fields.getJSONObject("user").optString("stringValue","") : "";

                if (fields.has("firstUsed") && fields.getJSONObject("firstUsed").has("timestampValue")) {
                    String ts = fields.getJSONObject("firstUsed").getString("timestampValue");
                    firstSec = java.time.Instant.parse(ts).getEpochSecond();
                }
                if (fields.has("pausedAt") && fields.getJSONObject("pausedAt").has("timestampValue")) {
                    String ts = fields.getJSONObject("pausedAt").getString("timestampValue");
                    pauseSec = java.time.Instant.parse(ts).getEpochSecond();
                }

                long usedSec = ("paused".equals(status) && pauseSec > 0) ? (pauseSec - firstSec) : (nowSec - firstSec);
                if (fields.has("firstUsed") && days - (usedSec / 86400L) <= 0) {
                    main.post(() -> { setLoad(false); showErr("[ERR] Key expirada"); });
                    return;
                }

                // Registrar device se primeiro uso
                if (noDevice) {
                    String docId = docName.substring(docName.lastIndexOf("/") + 1);
                    String patchUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT
                        + "/databases/(default)/documents/keys/" + docId
                        + "?updateMask.fieldPaths=deviceId&updateMask.fieldPaths=firstUsed&key=" + API_KEY;
                    JSONObject patch = new JSONObject();
                    JSONObject pf = new JSONObject();
                    pf.put("deviceId", new JSONObject().put("stringValue", myDev));
                    String nowTs = java.time.Instant.ofEpochSecond(nowSec).toString();
                    pf.put("firstUsed", new JSONObject().put("timestampValue", nowTs));
                    patch.put("fields", pf);
                    URL pUrl = new URL(patchUrl);
                    HttpURLConnection pc = (HttpURLConnection) pUrl.openConnection();
                    pc.setRequestMethod("PATCH");
                    pc.setRequestProperty("Content-Type", "application/json");
                    pc.setDoOutput(true);
                    pc.getOutputStream().write(patch.toString().getBytes("UTF-8"));
                    pc.getResponseCode();
                    pc.disconnect();
                    firstSec = nowSec;
                }

                final long fFirstSec = firstSec;
                final long fPauseSec = pauseSec;
                final String fUser = user;
                final int fDays = days;
                final String fStatus = status;
                main.post(() -> {
                    setLoad(false);
                    boolean remember = binding.switchRemember.isChecked();
                    String keyStr = binding.etKey.getText().toString().trim();
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_REM, remember)
                        .putString(PREF_KEY, remember ? keyStr : "")
                        .apply();
                    Intent i = new Intent(this, MainActivity.class);
                    i.putExtra("key_user", fUser);
                    i.putExtra("key_days", fDays);
                    i.putExtra("key_first_used_sec", fFirstSec);
                    i.putExtra("key_status", fStatus);
                    i.putExtra("key_paused_at_sec", fPauseSec);
                    startActivity(i);
                    finish();
                });
            } catch (Exception e) {
                main.post(() -> { setLoad(false); showErr("[ERR] " + e.getMessage()); });
            }
        });
    }

    private void showErr(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void setLoad(boolean on) {
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!on);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exec.shutdown();
    }
}
