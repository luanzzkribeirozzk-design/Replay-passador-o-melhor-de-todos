package com.replayx.app.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

/**
 * Verifica integridade do APK - detecta modificações
 */
public final class IntegrityCheck {
    private IntegrityCheck() {}

    // Hash SHA1 esperado da assinatura (será preenchido após primeiro build)
    // Por enquanto aceita qualquer assinatura mas bloqueia apps sem assinatura
    private static final String PACKAGE = "com.replayx.app";

    public static boolean isValid(Context ctx) {
        try {
            // Verificar package name correto
            if (!ctx.getPackageName().equals(PACKAGE)) return false;

            // Verificar se tem assinatura
            PackageInfo pi = ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            if (pi.signatures == null || pi.signatures.length == 0) return false;

            // Verificar que não está rodando em emulador suspeito
            String model = android.os.Build.MODEL.toLowerCase();
            String product = android.os.Build.PRODUCT.toLowerCase();
            if (product.contains("sdk") && model.contains("sdk")) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
