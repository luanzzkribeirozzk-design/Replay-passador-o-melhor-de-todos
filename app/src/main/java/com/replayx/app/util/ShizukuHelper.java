package com.replayx.app.util;

public class ShizukuHelper {

    public static String runMaxToNormal() {
        String src = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";
        String dst = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";
        return run(buildScript(src, dst));
    }

    public static String runNormalToMax() {
        String src = "/storage/emulated/0/Android/data/com.dts.freefireth/files/MReplays";
        String dst = "/storage/emulated/0/Android/data/com.dts.freefiremax/files/MReplays";
        return run(buildScript(src, dst));
    }

    private static String buildScript(String src, String dst) {
        StringBuilder sb = new StringBuilder();
        sb.append("mkdir -p ").append(dst).append("; ");
        sb.append("BIN=$(ls -t ").append(src).append("/*.bin 2>/dev/null | head -n 1); ");
        sb.append("JSON=$(ls -t ").append(src).append("/*.json 2>/dev/null | head -n 1); ");
        sb.append("if [ -z \"$BIN\" ]; then echo NAO_ENCONTRADO; exit 0; fi; ");
        sb.append("cp -f \"$BIN\" ").append(dst).append("/; ");
        sb.append("if [ -n \"$JSON\" ]; then cp -f \"$JSON\" ").append(dst).append("/; fi; ");
        sb.append("BNAME=$(basename \"$BIN\"); ");
        sb.append("chmod 666 ").append(dst).append("/$BNAME; ");
        sb.append("echo COPIADO_OK");
        return sb.toString();
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
                        m.setAccessible(true);
                        target = m;
                        break;
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
