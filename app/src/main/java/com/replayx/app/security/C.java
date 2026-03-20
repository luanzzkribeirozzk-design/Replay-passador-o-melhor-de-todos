package com.replayx.app.security;

/**
 * Credenciais ofuscadas via XOR - não expõe strings literais
 */
public final class C {
    private C() {}

    // XOR key para decodificação
    private static final byte X = 0x5A;

    // PROJECT ID ofuscado
    private static final byte[] P = {42,40,51,52,57,51,42,59,54,119,108,56,60,108,60};

    // API KEY ofuscado (dividido em 3 partes para dificultar análise)
    private static final byte[] K1 = {27,19,32,59,9,35,27,55,2,32,10,40,20};
    private static final byte[] K2 = {59,17,5,119,0,40,107,99,106,53,24,98};
    private static final byte[] K3 = {23,47,34,27,5,41,43,19,5,57,46,63,46,57};

    public static String p() {
        return d(P);
    }

    public static String k() {
        byte[] all = new byte[K1.length + K2.length + K3.length];
        System.arraycopy(K1, 0, all, 0, K1.length);
        System.arraycopy(K2, 0, all, K1.length, K2.length);
        System.arraycopy(K3, 0, all, K1.length + K2.length, K3.length);
        return d(all);
    }

    private static String d(byte[] b) {
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) c[i] = (char)(b[i] ^ X);
        return new String(c);
    }
}
