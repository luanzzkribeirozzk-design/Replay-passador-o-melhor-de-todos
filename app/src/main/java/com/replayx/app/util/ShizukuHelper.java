package com.replayx.app.util;

import android.os.Build;

public class ShizukuHelper {

    private static final String FFM_PKG  = "com.dts.freefiremax";
    private static final String FFN_PKG  = "com.dts.freefireth";
    private static final String FFM_PATH = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";
    private static final String FFN_PATH = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";
    private static final String VER_FFN  = "1.123.1";
    private static final String VER_FFM  = "2.124.1";

    public static String runMaxToNormal() {
        return transfer(FFM_PATH, FFN_PATH, FFM_PKG, FFN_PKG, VER_FFN, "freefiremax", "freefireth");
    }

    public static String runNormalToMax() {
        return transfer(FFN_PATH, FFM_PATH, FFN_PKG, FFM_PKG, VER_FFM, "freefireth", "freefiremax");
    }

    private static String transfer(String src, String dst, String srcPkg, String dstPkg,
                                    String version, String fromId, String toId) {
        int sdk = Build.VERSION.SDK_INT; // Android 10=29, 11=30, 12=31/32, 13=33, 14=34, 15=35, 16=36, 17=37

        String result;

        // Android 10-12 (SDK 29-32): shell direto funciona normalmente
        if (sdk <= 32) {
            result = method1_direct(src, dst, version, fromId, toId);
            if (result.contains("COPIADO_OK")) return result;
        }

        // Android 13+ (SDK 33+): restrições em /Android/data/ — tentar múltiplos métodos
        // Método A: appops grant primeiro, depois shell direto
        result = method2_appops(src, dst, srcPkg, dstPkg, version, fromId, toId);
        if (result.contains("COPIADO_OK")) return result;

        // Método B: via /proc/pid/root bypass
        result = method3_proc(srcPkg, dstPkg, version, fromId, toId);
        if (result.contains("COPIADO_OK")) return result;

        // Método C: via /data/data (root-level path, funciona com Shizuku ADB)
        result = method4_datadata(srcPkg, dstPkg, version, fromId, toId);
        if (result.contains("COPIADO_OK")) return result;

        // Método D: shell direto mesmo assim (último recurso, pode funcionar dependendo do ROM)
        if (sdk > 32) {
            result = method1_direct(src, dst, version, fromId, toId);
            if (result.contains("COPIADO_OK")) return result;
        }

        return result.isEmpty() ? "ERR_ALL_METHODS_FAILED" : result;
    }

    // MÉTODO 1: Shell direto — Android 10-12 e alguns 13+
    private static String method1_direct(String src, String dst, String version, String fromId, String toId) {
        String cmd =
            "SRC=\"" + src + "\"; DST=\"" + dst + "\"; " +
            "mkdir -p \"$DST\"; " +
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then " +
            "  [ ! -d \"$SRC\" ] && echo PASTA_BLOQUEADA || echo NAO_ENCONTRADO; exit 0; fi; " +
            "BNAME=$(basename \"$BIN\"); JNAME=$(basename \"$JSON\"); " +
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" && chmod 666 \"$DST/$BNAME\" || { echo M1_CP_FAIL; exit 0; }; " +
            buildJsonFix("$DST/$JNAME", "$JSON", "$DST", "$JNAME", version, fromId, toId) +
            "echo COPIADO_OK";
        return run(cmd);
    }

    // MÉTODO 2: appops grant + shell — Android 13/14/15/16/17
    private static String method2_appops(String src, String dst, String srcPkg, String dstPkg,
                                          String version, String fromId, String toId) {
        String cmd =
            // Garantir permissões via appops
            "cmd appops set " + srcPkg + " READ_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + srcPkg + " MANAGE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + dstPkg + " WRITE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + dstPkg + " MANAGE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            // Dar permissão ao shell também
            "cmd appops set shell MANAGE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "SRC=\"" + src + "\"; DST=\"" + dst + "\"; " +
            "mkdir -p \"$DST\"; " +
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo M2_SEM_REPLAY; exit 0; fi; " +
            "BNAME=$(basename \"$BIN\"); JNAME=$(basename \"$JSON\"); " +
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" && chmod 666 \"$DST/$BNAME\" || { echo M2_CP_FAIL; exit 0; }; " +
            buildJsonFix("$DST/$JNAME", "$JSON", "$DST", "$JNAME", version, fromId, toId) +
            "echo COPIADO_OK";
        return run(cmd);
    }

    // MÉTODO 3: /proc/pid/root bypass — Android 13+ quando app está rodando
    private static String method3_proc(String srcPkg, String dstPkg,
                                        String version, String fromId, String toId) {
        String cmd =
            "SPID=$(pidof " + srcPkg + " 2>/dev/null | awk '{print $1}'); " +
            "DPID=$(pidof " + dstPkg + " 2>/dev/null | awk '{print $1}'); " +
            "[ -n \"$SPID\" ] && SRC=\"/proc/$SPID/root/data/data/" + srcPkg + "/files/MReplays\" || " +
            "SRC=\"/data/data/" + srcPkg + "/files/MReplays\"; " +
            "[ -n \"$DPID\" ] && DST=\"/proc/$DPID/root/data/data/" + dstPkg + "/files/MReplays\" || " +
            "DST=\"/data/data/" + dstPkg + "/files/MReplays\"; " +
            "mkdir -p \"$DST\"; " +
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo M3_SEM_REPLAY; exit 0; fi; " +
            "BNAME=$(basename \"$BIN\"); JNAME=$(basename \"$JSON\"); " +
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" && chmod 666 \"$DST/$BNAME\" || { echo M3_CP_FAIL; exit 0; }; " +
            buildJsonFix("$DST/$JNAME", "$JSON", "$DST", "$JNAME", version, fromId, toId) +
            "echo COPIADO_OK";
        return run(cmd);
    }

    // MÉTODO 4: /data/data path — Shizuku com permissão ADB root
    private static String method4_datadata(String srcPkg, String dstPkg,
                                             String version, String fromId, String toId) {
        String cmd =
            "SRC=\"/data/data/" + srcPkg + "/files/MReplays\"; " +
            "DST=\"/data/data/" + dstPkg + "/files/MReplays\"; " +
            "mkdir -p \"$DST\"; " +
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo M4_SEM_REPLAY; exit 0; fi; " +
            "BNAME=$(basename \"$BIN\"); JNAME=$(basename \"$JSON\"); " +
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" && chmod 666 \"$DST/$BNAME\" || { echo M4_CP_FAIL; exit 0; }; " +
            buildJsonFix("$DST/$JNAME", "$JSON", "$DST", "$JNAME", version, fromId, toId) +
            "echo COPIADO_OK";
        return run(cmd);
    }

    // Helper: comandos de fix do JSON
    private static String buildJsonFix(String dstJson, String srcJson, String dstDir, String jname,
                                        String version, String fromId, String toId) {
        return
            "if [ -n \"" + srcJson + "\" ]; then " +
            "  cp -f \"" + srcJson + "\" \"" + dstDir + "/" + jname + "\" 2>/dev/null; " +
            "  chmod 666 \"" + dstDir + "/" + jname + "\" 2>/dev/null; " +
            "  sed -i 's/\"Version\":\"[^\"]*\"/\"Version\":\"" + version + "\"/g' \"" + dstDir + "/" + jname + "\" 2>/dev/null; " +
            "  sed -i 's/\"GameVersion\":\"[^\"]*\"/\"GameVersion\":\"" + version + "\"/g' \"" + dstDir + "/" + jname + "\" 2>/dev/null; " +
            "  sed -i 's/" + fromId.replace(".", "\\\\.") + "/" + toId + "/g' \"" + dstDir + "/" + jname + "\" 2>/dev/null; " +
            "fi; ";
    }

    public static String run(String cmd) {
        try {
            Class<?> cls = Class.forName("rikka.shizuku.Shizuku");
            java.lang.reflect.Method target = null;
            for (java.lang.reflect.Method m : cls.getMethods()) {
                if (m.getName().equals("newProcess")) { target = m; break; }
            }
            if (target == null) {
                for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                    if (m.getName().equals("newProcess")) {
                        m.setAccessible(true); target = m; break;
                    }
                }
            }
            if (target == null) return "ERR_NO_METHOD";
            String[] args = new String[]{"sh", "-c", cmd};
            Process p = (Process) target.invoke(null, new Object[]{args, null, null});
            byte[] outB = p.getInputStream().readAllBytes();
            byte[] errB = p.getErrorStream().readAllBytes();
            p.waitFor();
            String out = new String(outB).trim();
            String err = new String(errB).trim();
            return out.isEmpty() ? err : out;
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }
}
