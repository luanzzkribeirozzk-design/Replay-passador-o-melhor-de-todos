package com.replayx.app.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;

public final class IntegrityCheck {
    private IntegrityCheck() {}

    private static final String PACKAGE = "com.replayx.app";

    // Lista de pacotes de ferramentas de hack/debug conhecidas
    private static final String[] DANGER_PKGS = {
        "com.saurik.substrate",
        "de.robv.android.xposed.installer",
        "com.topjohnwu.magisk",
        "me.weishu.kernelsu",
        "io.github.lsposed.manager",
        "com.android.vending.billing.InAppBillingService.LUCK",
        "uret.jasi2169.patcher",
        "com.chelpus.lackypatch",
        "com.dimonvideo.luckypatcher",
        "com.android.vendingg",
        "com.chelpus.luckypatcher",
        "com.forpda.lp",
        "com.android.vending.billing.InAppBillingService.LUCK",
        "com.iap.crackfix",
        "com.appikoapp.appstoredroid",
        "com.android.fakeprocess",
        "com.sdktools.android",
        "org.meowcat.edxposed.manager",
        "com.zygisk.module",
        "me.bmax.apatch"
    };

    // Arquivos suspeitos de root/hook
    private static final String[] DANGER_FILES = {
        "/system/app/Superuser.apk",
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        "/system/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/system/xbin/mu",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/proc/net/tcp"
    };

    public static boolean isValid(Context ctx) {
        try {
            // 1. Package name correto
            if (!ctx.getPackageName().equals(PACKAGE)) return false;

            // 2. Verificar assinatura presente
            PackageInfo pi = ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            if (pi.signatures == null || pi.signatures.length == 0) return false;

            // 3. Anti-emulador
            if (isEmulator()) return false;

            // 4. Anti-root
            if (isRooted()) return false;

            // 5. Anti-hook (Xposed/Frida/Substrate)
            if (isHooked()) return false;

            // 6. Anti-debug
            if (android.os.Debug.isDebuggerConnected()) return false;
            if (android.os.Debug.waitingForDebugger()) return false;

            // 7. Anti-apps perigosos instalados
            if (hasDangerousApps(ctx)) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEmulator() {
        String model = Build.MODEL.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        String device = Build.DEVICE.toLowerCase();
        String hardware = Build.HARDWARE.toLowerCase();
        String fingerprint = Build.FINGERPRINT.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        if (fingerprint.contains("generic")) return true;
        if (fingerprint.contains("unknown")) return true;
        if (model.contains("google_sdk")) return true;
        if (model.contains("emulator")) return true;
        if (model.contains("android sdk built for x86")) return true;
        if (manufacturer.contains("genymotion")) return true;
        if (brand.startsWith("generic") && device.startsWith("generic")) return true;
        if (product.equals("google_sdk")) return true;
        if (hardware.equals("goldfish")) return true;
        if (hardware.equals("ranchu")) return true;
        if (product.contains("sdk_gphone")) return true;
        if (product.contains("vbox86p")) return true;
        if (product.contains("nox")) return true;
        if (product.contains("bluestacks")) return true;
        return false;
    }

    private static boolean isRooted() {
        // Verificar arquivos su
        for (String path : DANGER_FILES) {
            if (new File(path).exists()) return true;
        }
        // Tentar executar su
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "su"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            if (line != null && !line.isEmpty()) return true;
        } catch (Exception ignored) {}
        // Verificar build tags
        String tags = Build.TAGS;
        if (tags != null && tags.contains("test-keys")) return true;
        return false;
    }

    private static boolean isHooked() {
        // Detectar Frida
        try {
            // Frida abre porta 27042 por padrão
            java.net.Socket s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("127.0.0.1", 27042), 100);
            s.close();
            return true; // porta aberta = Frida rodando
        } catch (Exception ignored) {}

        // Detectar Xposed via stack trace
        try {
            throw new Exception();
        } catch (Exception e) {
            for (StackTraceElement el : e.getStackTrace()) {
                String cls = el.getClassName();
                if (cls.contains("de.robv.android.xposed") ||
                    cls.contains("com.saurik.substrate") ||
                    cls.contains("me.weishu") ||
                    cls.contains("io.github.lsposed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasDangerousApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : DANGER_PKGS) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true; // app encontrado
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return false;
    }
}
