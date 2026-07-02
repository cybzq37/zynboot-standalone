package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加解密工具类。
 * <p>
 * 默认 AES/GCM/NoPadding（最安全），兼容 AES/CBC/PKCS5Padding。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AesUtils {

    private static final String AES = "AES";
    private static final String GCM = "AES/GCM/NoPadding";
    private static final String CBC = "AES/CBC/PKCS5Padding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    // ==================== AES-GCM（推荐） ====================

    /**
     * AES-GCM 加密（Base64 输出）。
     *
     * @param plaintext 明文
     * @param key       密钥（16/24/32 字节，即 AES-128/192/256）
     * @return Base64 编码的密文（含 IV 前缀）
     */
    public static String encryptGcm(String plaintext, String key) {
        try {
            byte[] keyBytes = deriveKey(key);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, AES), spec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV + 密文 拼接后 Base64
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    /**
     * AES-GCM 解密。
     */
    public static String decryptGcm(String ciphertext, String key) {
        try {
            byte[] keyBytes = deriveKey(key);
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, AES), spec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }

    // ==================== AES-CBC（兼容旧系统） ====================

    /**
     * AES-CBC 加密（Base64 输出，自动生成 IV 前缀）。
     */
    public static String encryptCbc(String plaintext, String key) {
        try {
            byte[] keyBytes = deriveKey(key);
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CBC);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, AES), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES-CBC encrypt failed", e);
        }
    }

    /**
     * AES-CBC 解密。
     */
    public static String decryptCbc(String ciphertext, String key) {
        try {
            byte[] keyBytes = deriveKey(key);
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(CBC);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, AES), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-CBC decrypt failed", e);
        }
    }

    // ==================== 密钥生成 ====================

    /**
     * 生成 AES-256 密钥（Base64 编码）。
     */
    public static String generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(AES);
            kg.init(256, new SecureRandom());
            SecretKey key = kg.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("AES key generation failed", e);
        }
    }

    // ==================== 内部 ====================

    /**
     * 密钥派生：字符串转 32 字节（AES-256）。
     * <p>
     * 短密钥用 SHA-256 扩展，长密钥截取前 32 字节。
     */
    private static byte[] deriveKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32) {
            return keyBytes;
        }
        // 用 SHA-256 扩展为 32 字节
        return ShaUtils.sha256Bytes(keyBytes);
    }
}
