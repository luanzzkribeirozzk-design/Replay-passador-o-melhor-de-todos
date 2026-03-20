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
import android.os.Build;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.replayx.app.security.C;
import com.replayx.app.security.IntegrityCheck;
import com.replayx.app.security.TamperGuard;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final String PREFS      = "replayx_prefs";
    private static final String PREF_KEY   = "saved_key";
    private static final String PREF_REM   = "remember_key";
    private static final String PREF_AUTO  = "auto_login";
    private static final String PREF_KSTR  = "auto_kstr";
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private String cachedIP = "";
    private String deviceModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Switch Hide Stream na tela de login
        SharedPreferences prefs2 = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean hideOn = prefs2.getBoolean("hide_stream", false);
        binding.switchHideStreamLogin.setChecked(hideOn);
        applyHideStream(hideOn);
        binding.switchHideStreamLogin.setOnCheckedChangeListener((v, checked) -> {
            applyHideStream(checked);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("hide_stream", checked).apply();
        });

        // Verificação de integridade do APK
        if (!IntegrityCheck.isValid(this)) {
            finish();
            return;
        }

        // Verificação anti-tamper (hash do DEX)
        if (!TamperGuard.check(this)) {
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Auto-login: busca no Firestore para garantir validade real
        if (prefs.getBoolean(PREF_AUTO, false)) {
            String savedKStr = prefs.getString(PREF_KSTR, "");
            if (savedKStr != null && !savedKStr.isEmpty()) {
                setLoading(true);
                setStatus("Verificando acesso...", 0xFF888888);
                validateKey(savedKStr, true);
                return;
            }
        }

        setupForm(prefs);
    }

    private void applyHideStream(boolean active) {
        if (active) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void hideSplash() {
        android.view.View splash = binding.splashScreen;
        if (splash == null) return;
        splash.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> splash.setVisibility(android.view.View.GONE))
            .start();
    }

    private void setupForm(SharedPreferences prefs) {
        hideSplash();
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
        setLoading(true);
        setStatus("Validando key...", 0xFF00FF41);
        validateKey(key, false);
    }

    private void validateKey(String key, boolean isAutoLogin) {
        String myDev = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        exec.execute(() -> {
            final String PROJECT = C.p();
            final String API_KEY = C.k();
            try {

                // ── PASSO 1: Pegar IP público ──
                String myIP = "";
                try {
                    URL ipUrl = new URL("https://api.ipify.org?format=json");
                    HttpURLConnection ipC = (HttpURLConnection) ipUrl.openConnection();
                    ipC.setConnectTimeout(6000); ipC.setReadTimeout(6000);
                    Scanner ipSc = new Scanner(ipC.getInputStream(), "UTF-8");
                    StringBuilder ipSb = new StringBuilder();
                    while (ipSc.hasNextLine()) ipSb.append(ipSc.nextLine());
                    ipSc.close(); ipC.disconnect();
                    myIP = new JSONObject(ipSb.toString()).optString("ip", "");
                    cachedIP = myIP;
                } catch (Exception ignored) {}

                // ── PASSO 2: Verificar se IP está bloqueado ──
                if (!myIP.isEmpty()) {
                    String ipCheckUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT
                        + "/databases/(default)/documents/blocked_ips/" + myIP
                        + "?key=" + API_KEY;
                    URL ipDocUrl = new URL(ipCheckUrl);
                    HttpURLConnection ipDC = (HttpURLConnection) ipDocUrl.openConnection();
                    ipDC.setRequestMethod("GET");
                    ipDC.setConnectTimeout(8000); ipDC.setReadTimeout(8000);
                    int ipCode = ipDC.getResponseCode();
                    ipDC.disconnect();
                    if (ipCode == 200) {
                        // IP bloqueado — limpa auto-login e bloqueia
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putBoolean(PREF_AUTO, false).apply();
                        saveAttempt(key, "IP Bloqueado", myIP, deviceModel);
                        fail("Acesso bloqueado pelo administrador");
                        return;
                    }
                }

                // ── PASSO 3: Buscar key no Firestore ──
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
                conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8")); os.close();

                Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
                StringBuilder sb = new StringBuilder();
                while (sc.hasNextLine()) sb.append(sc.nextLine());
                sc.close(); conn.disconnect();

                JSONArray results = new JSONArray(sb.toString());

                // Key não existe mais no Firebase
                if (results.length() == 0 || !results.getJSONObject(0).has("document")) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_AUTO, false).apply();
                    saveAttempt(key, "Key invalida ou apagada", myIP, deviceModel);
                    fail("[ERR] Key invalida ou apagada");
                    return;
                }

                JSONObject doc = results.getJSONObject(0).getJSONObject("document");
                String docName = doc.getString("name");
                String docId = docName.substring(docName.lastIndexOf("/") + 1);
                JSONObject fields = doc.getJSONObject("fields");

                // ── PASSO 4: Verificar status ──
                String status = fields.has("status")
                    ? fields.getJSONObject("status").optString("stringValue", "") : "";
                if ("paused".equals(status)) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_AUTO, false).apply();
                    saveAttempt(key, "Key pausada", myIP, deviceModel);
                    fail("Key pausada");
                    return;
                }
                if (!"active".equals(status)) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_AUTO, false).apply();
                    saveAttempt(key, "Key inativa", myIP, deviceModel);
                    fail("[ERR] Key inativa");
                    return;
                }

                // ── PASSO 5: Verificar device ──
                String devId = fields.has("deviceId")
                    ? fields.getJSONObject("deviceId").optString("stringValue", "") : "";
                boolean noDevice = devId.isEmpty() || "null".equals(devId);
                if (!noDevice && !devId.equals(myDev)) {
                    // Device mudou — verificar se IP bate como fallback
                    String savedIP = fields.has("lastIP")
                        ? fields.getJSONObject("lastIP").optString("stringValue", "") : "";
                    boolean sameIP = !myIP.isEmpty() && !savedIP.isEmpty() && myIP.equals(savedIP);
                    if (sameIP) {
                        // Mesmo IP — atualizar device ID silenciosamente
                        noDevice = false; // vai cair no bloco de atualizar IP abaixo
                        devId = myDev;    // aceita o novo device
                    } else {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putBoolean(PREF_AUTO, false).apply();
                        saveAttempt(key, "Key em outro dispositivo", myIP, deviceModel);
                        fail("[ERR] Key em outro dispositivo");
                        return;
                    }
                }

                // ── PASSO 6: Carregar dados ──
                long nowSec = System.currentTimeMillis() / 1000L;
                long firstSec = nowSec;
                long pauseSec = 0L;
                int days = 0;
                String user = "";

                if (fields.has("days")) {
                    Object dv = fields.getJSONObject("days").opt("integerValue");
                    if (dv != null) days = Integer.parseInt(dv.toString());
                }
                if (fields.has("user"))
                    user = fields.getJSONObject("user").optString("stringValue", "");
                if (fields.has("firstUsed") && fields.getJSONObject("firstUsed").has("timestampValue")) {
                    String ts = fields.getJSONObject("firstUsed").getString("timestampValue");
                    firstSec = java.time.Instant.parse(ts).getEpochSecond();
                }
                if (fields.has("pausedAt") && fields.getJSONObject("pausedAt").has("timestampValue")) {
                    String ts = fields.getJSONObject("pausedAt").getString("timestampValue");
                    pauseSec = java.time.Instant.parse(ts).getEpochSecond();
                }

                // ── PASSO 7: Verificar expiração ──
                if (fields.has("firstUsed")) {
                    long usedSec = ("paused".equals(status) && pauseSec > 0)
                        ? (pauseSec - firstSec) : (nowSec - firstSec);
                    long remain = days - (usedSec / 86400L);
                    if (remain <= 0) {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putBoolean(PREF_AUTO, false).apply();
                        saveAttempt(key, "Key expirada", myIP, deviceModel);
                        fail("Key expirada");
                        return;
                    }
                }

                // ── PASSO 8: Registrar device e IP ──
                // noDevice = true: primeiro uso
                // devId == myDev mas pode ter mudado via fallback de IP
                boolean deviceChanged = !noDevice && !fields.has("deviceId") == false
                    && fields.has("deviceId")
                    && !fields.getJSONObject("deviceId").optString("stringValue","").equals(myDev);

                String patchMask = noDevice
                    ? "?updateMask.fieldPaths=deviceId&updateMask.fieldPaths=firstUsed&updateMask.fieldPaths=lastIP&key=" + API_KEY
                    : "?updateMask.fieldPaths=lastIP&updateMask.fieldPaths=deviceId&key=" + API_KEY;
                String patchUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT
                    + "/databases/(default)/documents/keys/" + docId + patchMask;
                JSONObject pf = new JSONObject();
                // Sempre salvar device ID atual (atualiza se mudou via fallback IP)
                pf.put("deviceId", new JSONObject().put("stringValue", myDev));
                if (noDevice) {
                    pf.put("firstUsed", new JSONObject().put("timestampValue",
                        java.time.Instant.ofEpochSecond(nowSec).toString()));
                    firstSec = nowSec;
                }
                if (!myIP.isEmpty())
                    pf.put("lastIP", new JSONObject().put("stringValue", myIP));
                if (pf.length() > 0) {
                    JSONObject patchBody = new JSONObject();
                    patchBody.put("fields", pf);
                    URL pUrl = new URL(patchUrl);
                    HttpURLConnection pc = (HttpURLConnection) pUrl.openConnection();
                    pc.setRequestMethod("PATCH");
                    pc.setRequestProperty("Content-Type", "application/json");
                    pc.setDoOutput(true);
                    pc.setConnectTimeout(10000);
                    pc.getOutputStream().write(patchBody.toString().getBytes("UTF-8"));
                    pc.getResponseCode();
                    pc.disconnect();
                }

                // ── PASSO 9: Sucesso — salvar e entrar ──
                final long fFirst = firstSec, fPause = pauseSec;
                final int fDays = days;
                final String fUser = user, fStatus = status, fKey = key;

                if (!isAutoLogin) {
                    main.post(() -> setStatus("Key validada com sucesso!", 0xFF00FF41));
                    Thread.sleep(1200);
                }

                main.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
                    boolean remember = isAutoLogin
                        ? p.getBoolean(PREF_REM, false)
                        : binding.switchRemember.isChecked();
                    SharedPreferences.Editor ed = p.edit();
                    ed.putBoolean(PREF_AUTO, true);
                    ed.putString(PREF_KSTR, fKey);
                    if (!isAutoLogin) {
                        ed.putBoolean(PREF_REM, remember);
                        ed.putString(PREF_KEY, remember ? fKey : "");
                    }
                    ed.apply();
                    goMain(fUser, fDays, fFirst, fStatus, fPause);
                });

            } catch (Exception e) {
                main.post(() -> {
                    if (isAutoLogin) {
                        // Falha de rede no auto-login: mostrar tela normal
                        setLoading(false);
                        setStatus("", 0xFF888888);
                        SharedPreferences pp = getSharedPreferences(PREFS, MODE_PRIVATE);
                        setupForm(pp);
                    } else {
                        fail("[ERR] " + e.getMessage());
                    }
                });
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
            hideSplash();
            setLoading(false);
            setStatus(msg, 0xFFFF4444);
        });
    }

    private void saveAttempt(String keyTried, String reason, String ip, String model) {
        exec.execute(() -> {
            try {
                final String PROJECT = C.p();
                final String API_KEY = C.k();
                String nowTs = java.time.Instant.now().toString();
                String docId = "attempt_" + System.currentTimeMillis();
                String patchUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT
                    + "/databases/(default)/documents/login_attempts/" + docId
                    + "?key=" + API_KEY;
                JSONObject fields = new JSONObject();
                fields.put("keyTried", new JSONObject().put("stringValue", keyTried.length() > 0 ? keyTried : "—"));
                fields.put("reason", new JSONObject().put("stringValue", reason));
                fields.put("ip", new JSONObject().put("stringValue", ip.length() > 0 ? ip : "desconhecido"));
                fields.put("model", new JSONObject().put("stringValue", model));
                fields.put("timestamp", new JSONObject().put("timestampValue", nowTs));
                JSONObject body = new JSONObject();
                body.put("fields", fields);
                URL u = new URL(patchUrl);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("PATCH");
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);
                c.setConnectTimeout(8000);
                c.getOutputStream().write(body.toString().getBytes("UTF-8"));
                c.getResponseCode();
                c.disconnect();
            } catch (Exception ignored) {}
        });
    }

    private void setLoading(boolean on) {
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!on);
    }

    private void setStatus(String msg, int color) {
        binding.tvError.setText(msg);
        binding.tvError.setTextColor(color);
        binding.tvError.setVisibility(msg.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exec.shutdown();
    }
}
