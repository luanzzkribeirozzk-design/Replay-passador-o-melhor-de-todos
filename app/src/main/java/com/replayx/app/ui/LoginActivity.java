package com.replayx.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private static final String PREFS     = "replayx_prefs";
    private static final String PREF_KEY  = "saved_key";
    private static final String PREF_REM  = "remember_key";
    private static final String PREF_AUTO = "auto_login";
    private static final String PREF_USER = "auto_user";
    private static final String PREF_DAYS = "auto_days";
    private static final String PREF_FIRST= "auto_first";
    private static final String PREF_STAT = "auto_status";
    private static final String PREF_PAUS = "auto_paused";
    private static final String PROJECT   = "principal-6bf6f";
    private static final String API_KEY   = "AIzaSyAmXzPrNaK_-Zr190oB8MuxA_sqI_ctetc";
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Auto-login: se ja validou antes, entra direto
        if (prefs.getBoolean(PREF_AUTO, false)) {
            String user   = prefs.getString(PREF_USER, "");
            int    days   = prefs.getInt(PREF_DAYS, 0);
            long   first  = prefs.getLong(PREF_FIRST, 0L);
            String status = prefs.getString(PREF_STAT, "active");
            long   paused = prefs.getLong(PREF_PAUS, 0L);

            // Verificar se ainda nao expirou localmente
            long nowSec = System.currentTimeMillis() / 1000L;
            long usedSec = ("paused".equals(status) && paused > 0)
                ? (paused - first) : (nowSec - first);
            long remain = days - (usedSec / 86400L);

            if (remain > 0 && !"paused".equals(status)) {
                goMain(user, days, first, status, paused);
                return;
            }
            // Se expirou ou pausada, mostra tela de login normal
            if ("paused".equals(status)) {
                binding.tvError.setText("Key pausada");
                binding.tvError.setTextColor(0xFFFF4444);
                binding.tvError.setVisibility(View.VISIBLE);
            } else {
                binding.tvError.setText("Key expirada");
                binding.tvError.setTextColor(0xFFFF4444);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        }

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
        if (key.isEmpty()) { setStatus("[ERR] Insira sua key!", 0xFFFF4444); return; }
        binding.btnLogin.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        setStatus("Validando key...", 0xFF00FF41);

        String myDev = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        exec.execute(() -> {
            try {
                String runQuery = "https://firestore.googleapis.com/v1/projects/" + PROJECT
                    + "/databases/(default)/documents:runQuery?key=" + API_KEY;
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
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
                StringBuilder sb = new StringBuilder();
                while (sc.hasNextLine()) sb.append(sc.nextLine());
                sc.close();
                conn.disconnect();

                JSONArray results = new JSONArray(sb.toString());
                if (results.length() == 0 || !results.getJSONObject(0).has("document")) {
                    fail("[ERR] Key invalida"); return;
                }

                JSONObject doc = results.getJSONObject(0).getJSONObject("document");
                String docName = doc.getString("name");
                JSONObject fields = doc.getJSONObject("fields");

                String status = fields.has("status") ? fields.getJSONObject("status").optString("stringValue","") : "";
                if ("paused".equals(status)) { fail("Key pausada"); return; }
                if (!"active".equals(status)) { fail("[ERR] Key inativa"); return; }

                String devId = fields.has("deviceId") ? fields.getJSONObject("deviceId").optString("stringValue","") : "";
                boolean noDevice = devId.isEmpty() || "null".equals(devId);

                if (!noDevice && !devId.equals(myDev)) {
                    fail("[ERR] Key em outro dispositivo"); return;
                }

                long nowSec = System.currentTimeMillis() / 1000L;
                long firstSec = nowSec;
                long pauseSec = 0L;
                int days = 0;
                String user = "";

                if (fields.has("days")) {
                    Object dv = fields.getJSONObject("days").opt("integerValue");
                    if (dv != null) days = Integer.parseInt(dv.toString());
                }
                if (fields.has("user")) user = fields.getJSONObject("user").optString("stringValue","");
                if (fields.has("firstUsed") && fields.getJSONObject("firstUsed").has("timestampValue")) {
                    String ts = fields.getJSONObject("firstUsed").getString("timestampValue");
                    firstSec = java.time.Instant.parse(ts).getEpochSecond();
                }
                if (fields.has("pausedAt") && fields.getJSONObject("pausedAt").has("timestampValue")) {
                    String ts = fields.getJSONObject("pausedAt").getString("timestampValue");
                    pauseSec = java.time.Instant.parse(ts).getEpochSecond();
                }

                if (fields.has("firstUsed")) {
                    long usedSec = ("paused".equals(status) && pauseSec > 0)
                        ? (pauseSec - firstSec) : (nowSec - firstSec);
                    if (days - (usedSec / 86400L) <= 0) { fail("Key expirada"); return; }
                }

                // Registrar primeiro uso
                if (noDevice) {
                    String docId = docName.substring(docName.lastIndexOf("/") + 1);
                    String patchUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT
                        + "/databases/(default)/documents/keys/" + docId
                        + "?updateMask.fieldPaths=deviceId&updateMask.fieldPaths=firstUsed&key=" + API_KEY;
                    JSONObject patch = new JSONObject();
                    JSONObject pf = new JSONObject();
                    pf.put("deviceId", new JSONObject().put("stringValue", myDev));
                    pf.put("firstUsed", new JSONObject().put("timestampValue", java.time.Instant.ofEpochSecond(nowSec).toString()));
                    patch.put("fields", pf);
                    URL pUrl = new URL(patchUrl);
                    HttpURLConnection pc = (HttpURLConnection) pUrl.openConnection();
                    pc.setRequestMethod("PATCH");
                    pc.setRequestProperty("Content-Type", "application/json");
                    pc.setDoOutput(true);
                    pc.setConnectTimeout(10000);
                    pc.getOutputStream().write(patch.toString().getBytes("UTF-8"));
                    pc.getResponseCode();
                    pc.disconnect();
                    firstSec = nowSec;
                }

                final long fFirst = firstSec, fPause = pauseSec;
                final int fDays = days;
                final String fUser = user, fStatus = status;

                main.post(() -> setStatus("Key validada com sucesso!", 0xFF00FF41));
                Thread.sleep(1200);
                main.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    boolean remember = binding.switchRemember.isChecked();
                    String keyStr = binding.etKey.getText().toString().trim();
                    // Salvar tudo para auto-login futuro
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_REM, remember)
                        .putString(PREF_KEY, remember ? keyStr : "")
                        .putBoolean(PREF_AUTO, true)
                        .putString(PREF_USER, fUser)
                        .putInt(PREF_DAYS, fDays)
                        .putLong(PREF_FIRST, fFirst)
                        .putString(PREF_STAT, fStatus)
                        .putLong(PREF_PAUS, fPause)
                        .apply();
                    goMain(fUser, fDays, fFirst, fStatus, fPause);
                });

            } catch (Exception e) {
                main.post(() -> fail("[ERR] " + e.getMessage()));
            }
        });
    }

    private void goMain(String user, int days, long first, String status, long paused) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("key_user", user);
        i.putExtra("key_days", days);
        i.putExtra("key_first_used_sec", first);
        i.putExtra("key_status", status);
        i.putExtra("key_paused_at_sec", paused);
        startActivity(i);
        finish();
    }

    private void fail(String msg) {
        main.post(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
            setStatus(msg, 0xFFFF4444);
        });
    }

    private void setStatus(String msg, int color) {
        binding.tvError.setText(msg);
        binding.tvError.setTextColor(color);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exec.shutdown();
    }
}
