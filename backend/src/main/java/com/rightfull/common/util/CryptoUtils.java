package com.rightfull.common.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/**
 * Production Cryptographic Utilities for Rightful Licensing.
 * Implements Ed25519 Asymmetric Signatures and HMAC-SHA-256 Key Hashing.
 */
public final class CryptoUtils {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String ED25519_ALGORITHM = "Ed25519";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CryptoUtils() {
        // Utility class
    }

    /**
     * Generates a secure Ed25519 keypair for product entitlement signing.
     */
    public static KeyPair generateEd25519KeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ED25519_ALGORITHM);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Ed25519 algorithm not available in security provider", e);
        }
    }

    /**
     * Signs data using the Ed25519 private key.
     */
    public static byte[] signEd25519(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ED25519_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException("Failed to generate Ed25519 signature", e);
        }
    }

    /**
     * Verifies an Ed25519 signature against data and public key.
     */
    public static boolean verifyEd25519(byte[] data, byte[] signatureBytes, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(ED25519_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }

    /**
     * Calculates HMAC-SHA-256 hash of a license key with server secret.
     * Returned as lowercase hex string.
     */
    public static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA-256", e);
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Calculates SHA-256 digest of input string as lowercase hex.
     */
    public static String sha256Hex(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a cryptographically secure random token as lowercase hex string.
     */
    public static String generateSecureToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        new SecureRandom().nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}


