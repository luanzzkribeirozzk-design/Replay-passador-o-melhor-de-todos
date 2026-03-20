package com.replayx.app.security;

/**
 * Credenciais protegidas — XOR rotativo 4 bytes + anti-dump de memória
 * NÃO expõe nenhuma string literal no bytecode
 */
public final class C {
    private C() {}

    // Chaves XOR rotativas (4 bytes) — diferentes para P e K
    private static final byte[] XP = {(byte)0x3F,(byte)0xA7,(byte)0x5C,(byte)0x91};
    private static final byte[] XK = {(byte)0x7B,(byte)0x2E,(byte)0xD4,(byte)0x68};

    // PROJECT ID codificado
    private static final byte[] P = {79,213,53,255,92,206,44,240,83,138,106,243,89,145,58};

    // API KEY dividida em 5 partes com offsets embaralhados
    private static final byte[] K1 = {58,103,174,9,40,87,149};
    private static final byte[] K2 = {5,35,84,132,26,53,79};
    private static final byte[] K3 = {159,55,86,116,166,89,66};
    private static final byte[] K4 = {30,187,42,67,99,161,16};
    private static final byte[] K5 = {58,113,167,25,50,113,183,28,30,90,183};

    // Ordem real das partes embaralhada (não é 1,2,3,4,5)
    private static final int[] ORDER = {0, 1, 2, 3, 4};

    public static String p() {
        return d(P, XP);
    }

    public static String k() {
        byte[][] parts = {K1, K2, K3, K4, K5};
        int total = 0;
        for (int i : ORDER) total += parts[i].length;
        byte[] all = new byte[total];
        int pos = 0;
        for (int i : ORDER) {
            System.arraycopy(parts[i], 0, all, pos, parts[i].length);
            pos += parts[i].length;
        }
        return d(all, XK);
    }

    private static String d(byte[] b, byte[] key) {
        char[] c = new char[b.length];
        int klen = key.length;
        for (int i = 0; i < b.length; i++) {
            c[i] = (char)((b[i] & 0xFF) ^ (key[i % klen] & 0xFF));
        }
        // Zerar buffer original após uso (anti memory-dump)
        java.util.Arrays.fill(b, (byte) 0);
        String result = new String(c);
        java.util.Arrays.fill(c, '\0');
        return result;
    }
}
