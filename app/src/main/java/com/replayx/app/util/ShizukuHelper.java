package com.replayx.app.util;

import java.lang.reflect.Method;

public class ShizukuHelper {

    public static Process exec(String cmd) throws Exception {
        Class<?> cls = Class.forName("rikka.shizuku.Shizuku");
        String[] cmdArray = new String[]{"sh", "-c", cmd};
        Method m = cls.getMethod("newProcess", String[].class, String[].class, String.class);
        return (Process) m.invoke(null, cmdArray, null, null);
    }
}
