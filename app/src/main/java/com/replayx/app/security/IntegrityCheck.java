package com.replayx.app.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.File;

/**
 * Verificação de integridade:
 * - Anti-debugger Java e nativo (só bloqueia se debugger ATIVO, não build debug)
 * - Anti-Frida
 * - Anti-emulador (sem falsos positivos em celulares reais)
 * - Verifica package name e assinatura
 */
public final class IntegrityCheck {
    private IntegrityCheck() {}

    private static final String PKG = "com.replayx.app";

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

            // 3. Anti-debugger — só bloqueia se debugger ATIVAMENTE conectado
            // NÃO verificar FLAG_DEBUGGABLE pois build debug sempre tem essa flag
            if (android.os.Debug.isDebuggerConnected()) return false;

            // 4. Anti-debugger nativo (TracerPID)
            if (isBeingTraced()) return false;

            // 5. Anti-Frida
            if (isFridaRunning()) return false;

            // 6. Anti-emulador (sem falsos positivos)
            if (isEmulator()) return false;

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

    /** Detecta Frida pela porta 27042 e arquivos conhecidos */
    private static boolean isFridaRunning() {
        // Verificar porta 27042
        try {
            java.net.Socket s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("127.0.0.1", 27042), 80);
            s.close();
            return true;
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

        // Verificar processos suspeitos
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
                        if (cmd != null && (cmd.contains("frida") || cmd.contains("gdb"))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Detecta emulador sem falsos positivos em celulares reais.
     * Removido: "test-keys" (celulares sem certificado oficial usam isso),
     *           "x86" no hardware (alguns Snapdragon têm isso),
     *           "unknown" (alguns celulares customizados usam).
     */
    private static boolean isEmulator() {
        String model    = android.os.Build.MODEL.toLowerCase();
        String product  = android.os.Build.PRODUCT.toLowerCase();
        String brand    = android.os.Build.BRAND.toLowerCase();
        String device   = android.os.Build.DEVICE.toLowerCase();
        String hardware = android.os.Build.HARDWARE.toLowerCase();
        String manufact = android.os.Build.MANUFACTURER.toLowerCase();
        String fingerp  = android.os.Build.FINGERPRINT.toLowerCase();

        // Palavras que SÓ aparecem em emuladores — nunca em celulares reais
        String[] emWords = {
            "genymotion", "goldfish", "ranchu",
            "nox", "bluestacks", "memu", "ldplayer",
            "andy", "droid4x", "vbox",
            "android sdk built for x86",
            "sdk_gphone"
        };

        for (String w : emWords) {
            if (model.contains(w) || product.contains(w) || device.contains(w)
                || hardware.contains(w) || manufact.contains(w) || fingerp.contains(w)) {
                return true;
            }
        }

        // "sdk" no product E model juntos (evita bloquear celulares com "sdk" só em um)
        if (product.contains("sdk") && model.contains("sdk")) return true;

        // Brand puramente genérico (celulares reais sempre têm marca)
        if ("generic".equals(brand)) return true;

        // Fingerprint começa com "generic/" — emulador AOSP puro
        if (fingerp.startsWith("generic/")) return true;

        // Arquivos que só existem em emulador QEMU
        if (new File("/dev/socket/qemud").exists()) return true;
        if (new File("/dev/qemu_pipe").exists()) return true;

        return false;
    }
}
