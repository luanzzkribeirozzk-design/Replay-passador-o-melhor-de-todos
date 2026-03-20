package com.replayx.app.security;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Verifica hash SHA-256 do DEX do APK em runtime.
 * Se o APK foi modificado/remontado, o hash muda e bloqueia.
 * Na primeira execução salva o hash; nas seguintes compara.
 */
public final class TamperGuard {
    private TamperGuard() {}

    private static final String PREF = "rx_tg";
    private static final String KEY  = "h";

    public static boolean check(Context ctx) {
        try {
            String apkPath = ctx.getPackageCodePath();
            String currentHash = sha256Dex(apkPath);
            if (currentHash == null) return false;

            android.content.SharedPreferences sp =
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String saved = sp.getString(KEY, "");

            if (saved.isEmpty()) {
                // Primeira execução — salvar hash
                sp.edit().putString(KEY, currentHash).apply();
                return true;
            }

            // Comparar de forma segura contra timing attack
            return safeEquals(saved, currentHash);
        } catch (Exception e) {
            return false;
        }
    }

    /** Calcula SHA-256 do arquivo classes.dex dentro do APK */
    private static String sha256Dex(String apkPath) {
        try {
            FileInputStream fis = new FileInputStream(new File(apkPath));
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("classes.dex".equals(entry.getName())) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = zis.read(buf)) != -1) {
                        md.update(buf, 0, read);
                    }
                    zis.close();
                    fis.close();
                    byte[] digest = md.digest();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) sb.append(String.format("%02x", b & 0xFF));
                    return sb.toString();
                }
            }
            zis.close();
            fis.close();
        } catch (Exception ignored) {}
        return null;
    }

    /** Comparação em tempo constante contra timing attacks */
    private static boolean safeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= (a.charAt(i) ^ b.charAt(i));
        return diff == 0;
    }
}
