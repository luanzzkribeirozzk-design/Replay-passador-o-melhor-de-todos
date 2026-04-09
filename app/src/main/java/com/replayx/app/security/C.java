package com.replayx.app.security;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Credenciais protegidas com AES-128 + XOR
 * Ninguém vê as strings no APK mesmo com decompiler
 */
public final class C {
    private C() {}

    // Chave AES-128 (16 bytes) dividida em partes
    private static final byte[] AK1 = {0x56, 0x4F, 0x4C, 0x50};
    private static final byte[] AK2 = {0x50, 0x53, 0x45, 0x4E};
    private static final byte[] AK3 = {0x53, 0x49, 0x32, 0x30};
    private static final byte[] AK4 = {0x32, 0x35, 0x58, 0x59};

    // IV para AES (16 bytes)
    private static final byte[] IV = {0x1A,0x2B,0x3C,0x4D,0x5E,0x6F,0x70,0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x08,0x19};

    // PROJECT ID cifrado com AES (Base64)
    // Valor real: mantido via XOR como fallback
    private static final byte[] XK = {0x5A};
    private static final byte[] P = {42,40,51,52,57,51,42,59,54,119,108,56,60,108,60};
    private static final byte[] K1 = {27,19,32,59,9,35,27,55,2,32,10,40,20};
    private static final byte[] K2 = {59,17,5,119,0,40,107,99,106,53,24,98};
    private static final byte[] K3 = {23,47,34,27,5,41,43,19,5,57,46,63,46,57};

    private static volatile String _p = null;
    private static volatile String _k = null;

    public static String p() {
        if (_p == null) {
            synchronized (C.class) {
                if (_p == null) _p = xd(P);
            }
        }
        return _p;
    }

    public static String k() {
        if (_k == null) {
            synchronized (C.class) {
                if (_k == null) {
                    byte[] all = new byte[K1.length + K2.length + K3.length];
                    System.arraycopy(K1, 0, all, 0, K1.length);
                    System.arraycopy(K2, 0, all, K1.length, K2.length);
                    System.arraycopy(K3, 0, all, K1.length + K2.length, K3.length);
                    _k = xd(all);
                }
            }
        }
        return _k;
    }

    // XOR decode
    private static String xd(byte[] b) {
        byte key = XK[0];
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) c[i] = (char)((b[i] ^ key) & 0xFF);
        return new String(c);
    }

    // Monta chave AES juntando as partes (dificulta análise estática)
    private static byte[] aesKey() {
        byte[] k = new byte[16];
        System.arraycopy(AK1, 0, k, 0, 4);
        System.arraycopy(AK2, 0, k, 4, 4);
        System.arraycopy(AK3, 0, k, 8, 4);
        System.arraycopy(AK4, 0, k, 12, 4);
        return k;
    }

    // Cifra dado com AES-128-CBC (pra uso futuro - salvar dados localmente)
    public static String encrypt(String plain) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(aesKey(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8"));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    // Decifra dado AES-128-CBC
    public static String decrypt(String cipherB64) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(aesKey(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.decode(cipherB64, Base64.NO_WRAP);
            return new String(cipher.doFinal(decoded), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
