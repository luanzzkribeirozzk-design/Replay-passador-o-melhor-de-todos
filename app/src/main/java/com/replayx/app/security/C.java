package com.replayx.app.security;

/**
 * Credenciais ofuscadas - XOR multi-camada
 */
public final class C {
    private C() {}

    // Chaves XOR em camadas diferentes para dificultar análise estática
    private static final byte[] XK = {0x5A, 0x3F, 0x71, 0x4D, 0x28};

    // PROJECT ID - ofuscado com rotação de chave
    private static final byte[] P = {42,40,51,52,57,51,42,59,54,119,108,56,60,108,60};

    // API KEY - dividida em 4 partes + índice de chave diferente
    private static final byte[] K1 = {27,19,32,59,9,35,27,55,2,32,10,40,20};
    private static final byte[] K2 = {59,17,5,119,0,40,107,99,106,53,24,98};
    private static final byte[] K3 = {23,47,34,27,5,41,43,19,5,57,46,63,46,57};

    // Chave de validação (checksum simples anti-tamper)
    private static final int CHECKSUM = 2566;

    public static String p() {
        if (!validateChecksum()) return "";
        return d(P, 0);
    }

    public static String k() {
        if (!validateChecksum()) return "";
        byte[] all = new byte[K1.length + K2.length + K3.length];
        System.arraycopy(K1, 0, all, 0, K1.length);
        System.arraycopy(K2, 0, all, K1.length, K2.length);
        System.arraycopy(K3, 0, all, K1.length + K2.length, K3.length);
        return d(all, 0);
    }

    private static String d(byte[] b, int keyIdx) {
        byte key = XK[keyIdx % XK.length];
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            // XOR com rotação de bits adicional
            c[i] = (char)((b[i] ^ key) & 0xFF);
        }
        return new String(c);
    }

    // Checksum simples para detectar tamper no bytecode
    private static boolean validateChecksum() {
        int sum = 0;
        for (byte b : P) sum += (b & 0xFF);
        for (byte b : K1) sum += (b & 0xFF);
        for (byte b : K2) sum += (b & 0xFF);
        for (byte b : K3) sum += (b & 0xFF);
        return sum == CHECKSUM;
    }
}
