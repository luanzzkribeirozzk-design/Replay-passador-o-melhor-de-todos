package com.replayx.app.util;

public class ShizukuHelper {

    // Script extraido do app base - copia o replay mais recente e ajusta versao
    public static final String SCRIPT_MAX_TO_NORMAL =
        "SRC_DIR="/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays"; " +
        "DST_DIR="/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays"; " +
        "[ -d "$SRC_DIR" ] || exit 1; " +
        "mkdir -p "$DST_DIR"; " +
        "RECENT_BIN=$(ls -t "$SRC_DIR"/*.bin 2>/dev/null | head -n 1); " +
        "RECENT_JSON=$(ls -t "$SRC_DIR"/*.json 2>/dev/null | head -n 1); " +
        "[ -z "$RECENT_BIN" ] && exit 0; " +
        "BASENAME=$(basename "$RECENT_BIN"); " +
        "cp -f "$RECENT_BIN" "$DST_DIR/"; " +
        "[ -n "$RECENT_JSON" ] && cp -f "$RECENT_JSON" "$DST_DIR/"; " +
        "chmod 666 "$DST_DIR/$BASENAME"; " +
        "if [ -n "$RECENT_JSON" ]; then " +
        "JSON_NAME=$(basename "$RECENT_JSON"); " +
        "JSON_FILE="$DST_DIR/$JSON_NAME"; " +
        "chmod 666 "$JSON_FILE"; " +
        "sed -i "s/\\"Version\\":\\"[^\\"]*\\"/\\"Version\\":\\"1.120.1\\"/" "$JSON_FILE"; " +
        "sed -i "s/\\"GameVersion\\":\\"[^\\"]*\\"/\\"GameVersion\\":\\"1.120.1\\"/" "$JSON_FILE"; " +
        "fi; " +
        "echo Replay copiado com sucesso.";

    public static final String SCRIPT_NORMAL_TO_MAX =
        "SRC_DIR="/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays"; " +
        "DST_DIR="/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays"; " +
        "[ -d "$SRC_DIR" ] || exit 1; " +
        "mkdir -p "$DST_DIR"; " +
        "RECENT_BIN=$(ls -t "$SRC_DIR"/*.bin 2>/dev/null | head -n 1); " +
        "RECENT_JSON=$(ls -t "$SRC_DIR"/*.json 2>/dev/null | head -n 1); " +
        "[ -z "$RECENT_BIN" ] && exit 0; " +
        "BASENAME=$(basename "$RECENT_BIN"); " +
        "cp -f "$RECENT_BIN" "$DST_DIR/"; " +
        "[ -n "$RECENT_JSON" ] && cp -f "$RECENT_JSON" "$DST_DIR/"; " +
        "chmod 666 "$DST_DIR/$BASENAME"; " +
        "echo Replay copiado com sucesso.";

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
            if (target == null) return "ERR: newProcess nao encontrado";
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
