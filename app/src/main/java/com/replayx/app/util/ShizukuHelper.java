package com.replayx.app.util;

public class ShizukuHelper {

    public static String runMaxToNormal() {
        String src = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";
        String dst = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";

        // Script robusto:
        // 1. Cria pasta destino
        // 2. Pega o replay mais recente do MAX
        // 3. Limpa pasta destino (remove replays antigos que podem conflitar)
        // 4. Copia bin + json com mesmo nome
        // 5. Corrige Version no JSON (MAX usa 2.x, Normal aceita 1.x)
        // 6. Corrige AppId no JSON (freefiremax -> freefireth)
        // 7. Ajusta permissoes

        String cmd =
            "SRC=\"" + src + "\"; " +
            "DST=\"" + dst + "\"; " +
            "mkdir -p \"$DST\"; " +

            // Pegar arquivo mais recente
            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo NAO_ENCONTRADO; exit 0; fi; " +

            // Nome dos arquivos
            "BNAME=$(basename \"$BIN\"); " +
            "JNAME=$(basename \"$JSON\"); " +

            // Limpar pasta destino antes de copiar (evita conflito de replays antigos)
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +

            // Copiar bin
            "cp -f \"$BIN\" \"$DST/$BNAME\"; " +
            "chmod 666 \"$DST/$BNAME\"; " +

            // Copiar json
            "cp -f \"$JSON\" \"$DST/$JNAME\"; " +
            "chmod 666 \"$DST/$JNAME\"; " +

            // Corrigir Version: 2.x.x -> 1.123.1
            "sed -i 's/\"Version\":\"[^\"]*\"/\"Version\":\"1.123.1\"/g' \"$DST/$JNAME\"; " +

            // Corrigir AppId: freefiremax -> freefireth
            "sed -i 's/com\\.dts\\.freefiremax/com.dts.freefireth/g' \"$DST/$JNAME\"; " +

            // Corrigir GameVersion se existir (alguns jsons tem campo separado)
            "sed -i 's/\"GameVersion\":\"[^\"]*\"/\"GameVersion\":\"1.123.1\"/g' \"$DST/$JNAME\"; " +

            // Confirmar
            "VER=$(grep -o '\"Version\":\"[^\"]*\"' \"$DST/$JNAME\" | head -n 1); " +
            "echo \"VER=$VER\"; " +
            "echo COPIADO_OK";

        return run(cmd);
    }

    public static String runNormalToMax() {
        String src = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";
        String dst = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";

        String cmd =
            "SRC=\"" + src + "\"; " +
            "DST=\"" + dst + "\"; " +
            "mkdir -p \"$DST\"; " +

            "BIN=$(ls -t \"$SRC\"/*.bin 2>/dev/null | head -n 1); " +
            "JSON=$(ls -t \"$SRC\"/*.json 2>/dev/null | head -n 1); " +
            "if [ -z \"$BIN\" ]; then echo NAO_ENCONTRADO; exit 0; fi; " +

            "BNAME=$(basename \"$BIN\"); " +
            "JNAME=$(basename \"$JSON\"); " +

            // Limpar destino
            "rm -f \"$DST\"/*.bin \"$DST\"/*.json 2>/dev/null; " +

            // Copiar bin
            "cp -f \"$BIN\" \"$DST/$BNAME\"; " +
            "chmod 666 \"$DST/$BNAME\"; " +

            // Copiar json se existir
            "if [ -n \"$JSON\" ]; then " +
            "cp -f \"$JSON\" \"$DST/$JNAME\"; " +
            "chmod 666 \"$DST/$JNAME\"; " +
            // Corrigir AppId: freefireth -> freefiremax
            "sed -i 's/com\\.dts\\.freefireth/com.dts.freefiremax/g' \"$DST/$JNAME\"; " +
            // Corrigir Version para FF MAX
            "sed -i 's/\"Version\":\"[^\"]*\"/\"Version\":\"2.123.1\"/g' \"$DST/$JNAME\"; " +
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
            return out.isEmpty() ? err : out;
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }
}
