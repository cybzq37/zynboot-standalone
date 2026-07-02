package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA 加解密与签名工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RsaUtils {

    private static final String RSA = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    // ==================== 密钥生成 ====================

    /**
     * 生成 RSA 密钥对（Base64 编码）。
     *
     * @return Map: key="publicKey", "privateKey"
     */
    public static Map<String, String> generateKeyPair() {
        return generateKeyPair(2048);
    }

    public static Map<String, String> generateKeyPair(int keySize) {
        try {
            KeyPairGenerator kg = KeyPairGenerator.getInstance(RSA);
            kg.initialize(keySize);
            KeyPair pair = kg.generateKeyPair();
            Map<String, String> keys = new HashMap<>();
            keys.put("publicKey", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
            keys.put("privateKey", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
            return keys;
        } catch (Exception e) {
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    // ==================== 加解密 ====================

    /**
     * 公钥加密（Base64 输出）。
     */
    public static String encrypt(String plaintext, String publicKey) {
        try {
            PublicKey key = loadPublicKey(publicKey);
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("RSA encrypt failed", e);
        }
    }

    /**
     * 私钥解密。
     */
    public static String decrypt(String ciphertext, String privateKey) {
        try {
            PrivateKey key = loadPrivateKey(privateKey);
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("RSA decrypt failed", e);
        }
    }

    // ==================== 签名验签 ====================

    /**
     * 私钥签名（Base64 输出）。
     */
    public static String sign(String data, String privateKey) {
        try {
            PrivateKey key = loadPrivateKey(privateKey);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(key);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("RSA sign failed", e);
        }
    }

    /**
     * 公钥验签。
     */
    public static boolean verify(String data, String sign, String publicKey) {
        try {
            PublicKey key = loadPublicKey(publicKey);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(key);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 内部 ====================

    private static PublicKey loadPublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private static PrivateKey loadPrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
