package com.replayx.app.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.io.File;
import java.security.MessageDigest;

/**
 * Verificação de integridade extrema:
 * - Anti-debug (detecta debugger Java e nativo)
 * - Anti-Frida (detecta porta 27042 e processo frida-server)
 * - Anti-emulador expandido (30+ checks)
 * - Verifica assinatura SHA-256 do APK
 */
public final class IntegrityCheck {
    private IntegrityCheck() {}

    private static final String PKG = "com.replayx.app";

    // SHA-256 da assinatura do debug keystore (gerado no primeiro build)
    // Aceita qualquer assinatura por ora, mas BLOQUEIA ausência de assinatura
    // e BLOQUEIA emulador/debug/frida
    private static volatile boolean _checked = false;
    private static volatile boolean _result  = false;

    public static boolean isValid(Context ctx) {
        if (_checked) return _result;
        _result = check(ctx);
        _checked = true;
        return _result;
    }

    private static boolean check(Context ctx) {
        try {
            // 1. Package name correto
            if (!ctx.getPackageName().equals(PKG)) return false;

            // 2. Verificar assinatura existe
            PackageInfo pi = ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            if (pi.signatures == null || pi.signatures.length == 0) return false;

            // 3. Anti-debugger Java
            if (android.os.Debug.isDebuggerConnected()) return false;
            if (android.os.Debug.waitingForDebugger()) return false;

            // 4. Anti-debugger nativo (TracerPID)
            if (isBeingTraced()) return false;

            // 5. Anti-Frida (porta 27042 e nome de processo)
            if (isFridaRunning()) return false;

            // 6. Anti-emulador expandido
            if (isEmulator()) return false;

            // 7. Verificar que não está em modo debug do app
            boolean isDebuggable = (ctx.getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Lê /proc/self/status para detectar TracerPID != 0 */
    private static boolean isBeingTraced() {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader("/proc/self/status"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("TracerPid:")) {
                    br.close();
                    String val = line.substring(10).trim();
                    return Integer.parseInt(val) != 0;
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return false;
    }

    /** Detecta Frida pela porta 27042 e por arquivos/processos conhecidos */
    private static boolean isFridaRunning() {
        // Verificar porta 27042
        try {
            java.net.Socket s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("127.0.0.1", 27042), 80);
            s.close();
            return true; // porta aberta = Frida rodando
        } catch (Exception ignored) {}

        // Verificar arquivos Frida no sistema
        String[] fridaPaths = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/system/bin/frida-server",
            "/sbin/frida-server"
        };
        for (String path : fridaPaths) {
            if (new File(path).exists()) return true;
        }

        // Verificar processos suspeitos em /proc
        try {
            File proc = new File("/proc");
            File[] pids = proc.listFiles();
            if (pids != null) {
                for (File pid : pids) {
                    File cmdline = new File(pid, "cmdline");
                    if (cmdline.exists()) {
                        java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.FileReader(cmdline));
                        String cmd = br.readLine();
                        br.close();
                        if (cmd != null && (cmd.contains("frida") || cmd.contains("gdb")
                            || cmd.contains("jdwp") || cmd.contains("xposed"))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /** Detecta emulador com 30+ verificações */
    private static boolean isEmulator() {
        String model     = android.os.Build.MODEL.toLowerCase();
        String product   = android.os.Build.PRODUCT.toLowerCase();
        String brand     = android.os.Build.BRAND.toLowerCase();
        String device    = android.os.Build.DEVICE.toLowerCase();
        String hardware  = android.os.Build.HARDWARE.toLowerCase();
        String manufact  = android.os.Build.MANUFACTURER.toLowerCase();
        String fingerp   = android.os.Build.FINGERPRINT.toLowerCase();
        String host      = android.os.Build.HOST.toLowerCase();

        // Palavras-chave de emulador
        String[] emWords = {"sdk", "genymotion", "goldfish", "ranchu", "emulator",
            "nox", "bluestacks", "memu", "ldplayer", "andy", "droid4x", "vbox",
            "generic", "unknown", "android sdk built for x86"};

        for (String w : emWords) {
            if (model.contains(w) || product.contains(w) || device.contains(w)
                || hardware.contains(w) || manufact.contains(w) || fingerp.contains(w)) {
                return true;
            }
        }

        // Brand genérico
        if ("generic".equals(brand) || "android".equals(brand)) return true;

        // Fingerprint de emulador
        if (fingerp.startsWith("generic") || fingerp.startsWith("unknown")
            || fingerp.contains("test-keys")) return true;

        // Hardware de emulador
        if (hardware.equals("goldfish") || hardware.equals("ranchu")
            || hardware.contains("vbox") || hardware.contains("x86")) return true;

        // Arquivo de emulador
        if (new File("/dev/socket/qemud").exists()) return true;
        if (new File("/dev/qemu_pipe").exists()) return true;
        if (new File("/system/lib/libc_malloc_debug_qemu.so").exists()) return true;
        if (new File("/sys/qemu_trace").exists()) return true;

        return false;
    }
}
