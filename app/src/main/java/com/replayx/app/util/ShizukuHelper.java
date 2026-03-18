package com.replayx.app.util;

import java.lang.reflect.Method;

public class ShizukuHelper {

    public static String run(String cmd) {
        try {
            Class<?> cls = Class.forName("rikka.shizuku.Shizuku");
            Method target = null;
            // Tentar metodos publicos primeiro
            for (Method m : cls.getMethods()) {
                if (m.getName().equals("newProcess")) {
                    target = m;
                    break;
                }
            }
            // Se nao achou, tentar metodos declarados (incluindo privados)
            if (target == null) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().equals("newProcess")) {
                        m.setAccessible(true);
                        target = m;
                        break;
                    }
                }
            }
            if (target == null) {
                return "ERR: newProcess nao encontrado em nenhum nivel";
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
