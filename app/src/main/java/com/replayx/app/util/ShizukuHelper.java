package com.replayx.app.util;

import android.os.Build;

public class ShizukuHelper {

    private static final String FFM_PKG = "com.dts.freefiremax";
    private static final String FFN_PKG = "com.dts.freefireth";
    private static final String VER_FFN = "1.126.1";
    private static final String VER_FFM = "2.126.1";

    public static String runMaxToNormal() {
        return transfer(FFM_PKG, FFN_PKG, VER_FFN, "freefiremax", "freefireth");
    }

    public static String runNormalToMax() {
        return transfer(FFN_PKG, FFM_PKG, VER_FFM, "freefireth", "freefiremax");
    }

    private static String transfer(String srcPkg, String dstPkg,
                                    String version, String fromId, String toId) {
        // Tentar todos os métodos em ordem do mais eficaz para Android 10-17
        String r;

        // MÉTODO PRINCIPAL: appops + shell com todos os caminhos possíveis
        r = methodFull(srcPkg, dstPkg, version, fromId, toId);
        if (r.contains("COPIADO_OK")) return r;

        // FALLBACK 1: via run-as (acessa dados internos do app)
        r = methodRunAs(srcPkg, dstPkg, version, fromId, toId);
        if (r.contains("COPIADO_OK")) return r;

        // FALLBACK 2: via /proc/pid/root
        r = methodProc(srcPkg, dstPkg, version, fromId, toId);
        if (r.contains("COPIADO_OK")) return r;

        // FALLBACK 3: via /data/data direto
        r = methodDataData(srcPkg, dstPkg, version, fromId, toId);
        if (r.contains("COPIADO_OK")) return r;

        return r.isEmpty() ? "ERR_ALL_FAILED" : r;
    }

    // MÉTODO PRINCIPAL: cobre Android 10-17
    // - Usa appops para liberar acesso
    // - Tenta múltiplos caminhos de armazenamento
    // - Trata permissões de SELinux
    private static String methodFull(String srcPkg, String dstPkg,
                                      String version, String fromId, String toId) {
        String cmd =
            // 1. Liberar todas as permissões necessárias via appops
            "cmd appops set " + srcPkg + " READ_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + srcPkg + " MANAGE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + srcPkg + " ACCESS_MEDIA_LOCATION allow 2>/dev/null; " +
            "cmd appops set " + dstPkg + " WRITE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + dstPkg + " MANAGE_EXTERNAL_STORAGE allow 2>/dev/null; " +
            "cmd appops set " + dstPkg + " ACCESS_MEDIA_LOCATION allow 2>/dev/null; " +

            // 2. Tentar múltiplos caminhos de origem (Android 10-17 variam)
            "SRC=''; " +
            "for P in " +
            "'/storage/emulated/0/Android/data/" + srcPkg + "/files/MReplays' " +
            "'/sdcard/Android/data/" + srcPkg + "/files/MReplays' " +
            "'/data/media/0/Android/data/" + srcPkg + "/files/MReplays' " +
            "'/mnt/user/0/" + srcPkg + "/files/MReplays' " +
            "; do [ -d \"$P\" ] && SRC=\"$P\" && break; done; " +

            // 3. Tentar múltiplos caminhos de destino
            "DST=''; " +
            "for P in " +
            "'/storage/emulated/0/Android/data/" + dstPkg + "/files/MReplays' " +
            "'/sdcard/Android/data/" + dstPkg + "/files/MReplays' " +
            "'/data/media/0/Android/data/" + dstPkg + "/files/MReplays' " +
            "'/mnt/user/0/" + dstPkg + "/files/MReplays' " +
            "; do DST=\"$P\" && break; done; " +

            "if [ -z \"$SRC\" ]; then echo PASTA_NAO_ENCONTRADA; exit 0; fi; " +
            "mkdir -p \"$DST\"; " +

            // 4. Pegar replay mais recente
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo NAO_ENCONTRADO; exit 0; fi; " +

            "BNAME=$(basename \"$BIN\"); " +
            "JNAME=$(basename \"$JSON\"); " +

            // 5. Limpar destino e copiar
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" || { echo CP_BIN_FAIL; exit 0; }; " +
            "chmod 666 \"$DST/$BNAME\" 2>/dev/null; " +
            "chown $(stat -c '%u:%g' \"$BIN\") \"$DST/$BNAME\" 2>/dev/null; " +

            // 6. Copiar e corrigir JSON
            "if [ -n \"$JSON\" ]; then " +
            "  cp -f \"$JSON\" \"$DST/$JNAME\" || { echo CP_JSON_FAIL; exit 0; }; " +
            "  chmod 666 \"$DST/$JNAME\" 2>/dev/null; " +
            "  chown $(stat -c '%u:%g' \"$JSON\") \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"Version\":\"[^\"]*\"/\"Version\":\"" + version + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"GameVersion\":\"[^\"]*\"/\"GameVersion\":\"" + version + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"AppId\":\"[^\"]*\"/\"AppId\":\"" + toId + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/" + fromId.replace(".", "\\\\.") + "/" + toId + "/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "fi; " +

            // 7. Forçar o jogo destino a reconhecer o arquivo
            "am force-stop " + dstPkg + " 2>/dev/null; " +
            "cmd media scan-file \"$DST/$BNAME\" 2>/dev/null; " +

            "echo COPIADO_OK";
        return run(cmd);
    }

    // FALLBACK 1: run-as — Android 13+ com apps debuggable ou via ADB shell
    private static String methodRunAs(String srcPkg, String dstPkg,
                                       String version, String fromId, String toId) {
        String cmd =
            "SRC=$(run-as " + srcPkg + " sh -c 'ls /data/data/" + srcPkg + "/files/MReplays/*.bin 2>/dev/null | head -n 1' 2>/dev/null); " +
            "if [ -z \"$SRC\" ]; then echo M2_FAIL; exit 0; fi; " +
            "BNAME=$(basename \"$SRC\"); " +
            "run-as " + srcPkg + " sh -c \"cp /data/data/" + srcPkg + "/files/MReplays/$BNAME /sdcard/tmp_replay.bin\" 2>/dev/null; " +
            "run-as " + dstPkg + " sh -c \"cp /sdcard/tmp_replay.bin /data/data/" + dstPkg + "/files/MReplays/$BNAME\" 2>/dev/null; " +
            "rm -f /sdcard/tmp_replay.bin 2>/dev/null; " +
            "echo COPIADO_OK";
        return run(cmd);
    }

    // FALLBACK 2: /proc/pid/root bypass
    private static String methodProc(String srcPkg, String dstPkg,
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
            "if [ -z \"$BIN\" ]; then echo M3_FAIL; exit 0; fi; " +
            "BNAME=$(basename \"$BIN\"); JNAME=$(basename \"$JSON\"); " +
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" && chmod 666 \"$DST/$BNAME\" || { echo M3_CP_FAIL; exit 0; }; " +
            "if [ -n \"$JSON\" ]; then " +
            "  cp -f \"$JSON\" \"$DST/$JNAME\" && chmod 666 \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"Version\":\"[^\"]*\"/\"Version\":\"" + version + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"AppId\":\"[^\"]*\"/\"AppId\":\"" + toId + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/" + fromId.replace(".", "\\\\.") + "/" + toId + "/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "fi; " +
            "echo COPIADO_OK";
        return run(cmd);
    }

    // FALLBACK 3: /data/data direto via Shizuku root
    private static String methodDataData(String srcPkg, String dstPkg,
                                          String version, String fromId, String toId) {
        String cmd =
            "SRC=\"/data/data/" + srcPkg + "/files/MReplays\"; " +
            "DST=\"/data/data/" + dstPkg + "/files/MReplays\"; " +
            "mkdir -p \"$DST\"; " +
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo M4_FAIL; exit 0; fi; " +
            "BNAME=$(basename \"$BIN\"); JNAME=$(basename \"$JSON\"); " +
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +
            "cp -f \"$BIN\" \"$DST/$BNAME\" && chmod 666 \"$DST/$BNAME\" || { echo M4_CP_FAIL; exit 0; }; " +
            "if [ -n \"$JSON\" ]; then " +
            "  cp -f \"$JSON\" \"$DST/$JNAME\" && chmod 666 \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"Version\":\"[^\"]*\"/\"Version\":\"" + version + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/\"AppId\":\"[^\"]*\"/\"AppId\":\"" + toId + "\"/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "  sed -i 's/" + fromId.replace(".", "\\\\.") + "/" + toId + "/g' \"$DST/$JNAME\" 2>/dev/null; " +
            "fi; " +
            "echo COPIADO_OK";
        return run(cmd);
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
            return out.isEmpty() ? (err.isEmpty() ? "ERR_NO_OUTPUT" : err) : out;
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }
}
