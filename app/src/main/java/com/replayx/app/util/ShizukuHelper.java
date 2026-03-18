package com.replayx.app.util;

public class ShizukuHelper {

    public static String runMaxToNormal() {
        String src = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";
        String dst = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";
        StringBuilder sb = new StringBuilder();
        sb.append("mkdir -p ").append(dst).append("; ");
        sb.append("BIN=$(ls -t ").append(src).append("/*.bin 2>/dev/null | head -n 1); ");
        sb.append("JSON=$(ls -t ").append(src).append("/*.json 2>/dev/null | head -n 1); ");
        sb.append("if [ -z \"$BIN\" ]; then echo NAO_ENCONTRADO; exit 0; fi; ");
        sb.append("BNAME=$(basename \"$BIN\"); ");
        sb.append("JNAME=$(basename \"$JSON\"); ");
        sb.append("cp -f \"$BIN\" ").append(dst).append("/$BNAME; ");
        sb.append("chmod 666 ").append(dst).append("/$BNAME; ");
        sb.append("cp -f \"$JSON\" ").append(dst).append("/$JNAME; ");
        sb.append("chmod 666 ").append(dst).append("/$JNAME; ");
        // Extrair apenas campos relevantes do JSON
        sb.append("echo VER=$(grep -o '\"Version\":\"[^\"]*\"' ").append(dst).append("/$JNAME); ");
        sb.append("echo GAMEVER=$(grep -o '\"GameVersion\":\"[^\"]*\"' ").append(dst).append("/$JNAME); ");
        sb.append("echo PKG=$(grep -o '\"PackageName\":\"[^\"]*\"' ").append(dst).append("/$JNAME); ");
        sb.append("echo APPID=$(grep -o '\"AppId\":\"[^\"]*\"' ").append(dst).append("/$JNAME); ");
        sb.append("echo COPIADO_OK");
        return run(sb.toString());
    }

    public static String runNormalToMax() {
        String src = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";
        String dst = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";
        StringBuilder sb = new StringBuilder();
        sb.append("mkdir -p ").append(dst).append("; ");
        sb.append("BIN=$(ls -t ").append(src).append("/*.bin 2>/dev/null | head -n 1); ");
        sb.append("JSON=$(ls -t ").append(src).append("/*.json 2>/dev/null | head -n 1); ");
        sb.append("if [ -z \"$BIN\" ]; then echo NAO_ENCONTRADO; exit 0; fi; ");
        sb.append("BNAME=$(basename \"$BIN\"); ");
        sb.append("cp -f \"$BIN\" ").append(dst).append("/$BNAME; ");
        sb.append("chmod 666 ").append(dst).append("/$BNAME; ");
        sb.append("if [ -n \"$JSON\" ]; then ");
        sb.append("JNAME=$(basename \"$JSON\"); ");
        sb.append("cp -f \"$JSON\" ").append(dst).append("/$JNAME; ");
        sb.append("chmod 666 ").append(dst).append("/$JNAME; ");
        sb.append("fi; ");
        sb.append("echo COPIADO_OK");
        return run(sb.toString());
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
