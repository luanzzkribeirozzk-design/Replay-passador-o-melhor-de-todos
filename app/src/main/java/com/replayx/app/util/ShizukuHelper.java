package com.replayx.app.util;

public class ShizukuHelper {

    public static String run(String cmd) {
        try {
            Class<?> cls = Class.forName("rikka.shizuku.Shizuku");
            java.lang.reflect.Method[] methods = cls.getMethods();
            java.lang.reflect.Method target = null;
            for (java.lang.reflect.Method m : methods) {
                if (m.getName().equals("newProcess")) {
                    target = m;
                    break;
                }
            }
            if (target == null) {
                return "ERR: newProcess nao encontrado";
            }
            String[] args = new String[]{"sh", "-c", cmd};
            Object[] params = new Object[]{args, null, null};
            Process p = (Process) target.invoke(null, params);
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
