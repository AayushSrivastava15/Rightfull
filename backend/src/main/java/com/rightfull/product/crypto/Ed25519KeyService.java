package com.rightfull.product.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Core cryptographic service for Ed25519 asymmetric keypair generation,
 * serialization, and digital signature operations.
 */
@Service
public class Ed25519KeyService {

    public static final String ALGORITHM = "Ed25519";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generates a distinct cryptographically secure Ed25519 keypair.
     */
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Ed25519 algorithm not available in configured security providers", e);
        }
    }

    /**
     * Serializes an Ed25519 public key as a Base64-encoded X.509 SubjectPublicKeyInfo string.
     */
    public String encodePublicKey(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Reconstructs an Ed25519 public key from a Base64-encoded X.509 SubjectPublicKeyInfo string.
     */
    public PublicKey decodePublicKey(String base64PublicKey) {
        if (base64PublicKey == null || base64PublicKey.isBlank()) {
            throw new IllegalArgumentException("Base64 public key cannot be null or empty");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey.trim());
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Ed25519 public key encoding", e);
        }
    }

    /**
     * Serializes an Ed25519 private key to PKCS#8 encoded bytes.
     */
    public byte[] encodePrivateKey(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        return privateKey.getEncoded();
    }

    /**
     * Reconstructs an Ed25519 private key from PKCS#8 encoded bytes.
     */
    public PrivateKey decodePrivateKey(byte[] pkcs8Bytes) {
        if (pkcs8Bytes == null || pkcs8Bytes.length == 0) {
            throw new IllegalArgumentException("PKCS#8 private key bytes cannot be null or empty");
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid Ed25519 private key bytes", e);
        }
    }

    /**
     * Signs data using the Ed25519 private key.
     */
    public byte[] sign(byte[] data, PrivateKey privateKey) {
        if (data == null || privateKey == null) {
            throw new IllegalArgumentException("Data and private key cannot be null");
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException("Failed to produce Ed25519 digital signature", e);
        }
    }

    /**
     * Verifies an Ed25519 digital signature against the original data and public key.
     */
    public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey) {
        if (data == null || signatureBytes == null || publicKey == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }
}
