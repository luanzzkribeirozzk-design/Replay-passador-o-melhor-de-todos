package com.replayx.app.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public final class IntegrityCheck {
    private IntegrityCheck() {}

    private static final String PACKAGE = "com.replayx.app";

    public static boolean isValid(Context ctx) {
        try {
            if (!ctx.getPackageName().equals(PACKAGE)) return false;

            PackageInfo pi = ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            if (pi.signatures == null || pi.signatures.length == 0) return false;

            // Anti-emulador (não afeta celular real)
            if (isEmulator()) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEmulator() {
        String fp = Build.FINGERPRINT.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String hardware = Build.HARDWARE.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        if (fp.contains("generic") || fp.contains("unknown")) return true;
        if (model.contains("google_sdk") || model.contains("emulator")) return true;
        if (manufacturer.contains("genymotion")) return true;
        if (hardware.equals("goldfish") || hardware.equals("ranchu")) return true;
        if (product.contains("sdk_gphone") || product.contains("vbox86p")) return true;
        if (product.contains("nox") || product.contains("bluestacks")) return true;
        return false;
    }
}
