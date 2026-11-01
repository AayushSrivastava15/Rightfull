package com.rightfull.product.service;

import com.rightfull.audit.WorkspaceAuditService;
import com.rightfull.product.crypto.Ed25519KeyService;
import com.rightfull.product.crypto.PrivateKeyEncryptionService;
import com.rightfull.product.dto.PublicKeyResponse;
import com.rightfull.product.entity.KeyStatus;
import com.rightfull.product.entity.Product;
import com.rightfull.product.entity.ProductSigningKey;
import com.rightfull.product.repository.ProductSigningKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Signing Key Service Unit Tests")
class ProductSigningKeyServiceTest {

    @Mock
    private ProductSigningKeyRepository signingKeyRepository;

    @Mock
    private WorkspaceAuditService auditService;

    private Ed25519KeyService ed25519KeyService;
    private PrivateKeyEncryptionService encryptionService;
    private ProductSigningKeyService keyService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private Product product;

    @BeforeEach
    void setUp() {
        ed25519KeyService = new Ed25519KeyService();
        encryptionService = new PrivateKeyEncryptionService("12345678901234567890123456789012");
        encryptionService.validateAndInitializeMasterKey();

        keyService = new ProductSigningKeyService(
                signingKeyRepository,
                ed25519KeyService,
                encryptionService,
                auditService
        );

        product = new Product(workspaceId, "Rightful Enterprise", "rightful-enterprise", "Desc", "1.0", actorId);
        product.setId(productId);
    }

    @Test
    @DisplayName("Creates initial Ed25519 signing key with encrypted private key at rest")
    void testCreateInitialSigningKey() {
        when(signingKeyRepository.save(any(ProductSigningKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductSigningKey key = keyService.createInitialSigningKey(product, actorId);

        assertNotNull(key);
        assertEquals(productId, key.getProductId());
        assertEquals(workspaceId, key.getWorkspaceId());
        assertEquals("ED25519", key.getAlgorithm());
        assertEquals(KeyStatus.ACTIVE, key.getStatus());
        assertTrue(key.getKeyId().startsWith("kid_"));
        assertNotNull(key.getPublicKey());
        assertNotNull(key.getEncryptedPrivateKey());
        assertEquals(12, key.getEncryptionNonce().length);
        assertEquals("v1", key.getEncryptionVersion());

        verify(auditService, times(1)).recordEvent(any());
    }

    @Test
    @DisplayName("Rotates signing key: retires old active key and creates new active key")
    void testRotateSigningKey() {
        ProductSigningKey oldKey = new ProductSigningKey(
                workspaceId, productId, "kid_old_111", "old_pubkey", new byte[16], new byte[12], "v1"
        );
        oldKey.setStatus(KeyStatus.ACTIVE);

        when(signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)).thenReturn(Optional.of(oldKey));
        when(signingKeyRepository.save(any(ProductSigningKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductSigningKey newKey = keyService.rotateSigningKey(product, actorId);

        assertNotNull(newKey);
        assertEquals(KeyStatus.ACTIVE, newKey.getStatus());
        assertNotEquals("kid_old_111", newKey.getKeyId());

        // Verify old key was retired
        assertEquals(KeyStatus.RETIRED, oldKey.getStatus());
        assertNotNull(oldKey.getRetiredAt());
        assertNotNull(oldKey.getRotatedAt());

        verify(signingKeyRepository, times(2)).save(any(ProductSigningKey.class));
        verify(auditService, times(1)).recordEvent(any());
    }

    @Test
    @DisplayName("Decrypts active Ed25519 private key securely for license signing")
    void testGetActivePrivateKey() {
        KeyPair keyPair = ed25519KeyService.generateKeyPair();
        String b64Public = ed25519KeyService.encodePublicKey(keyPair.getPublic());
        byte[] pkcs8Private = ed25519KeyService.encodePrivateKey(keyPair.getPrivate());

        String keyId = "kid_decrypt_test";
        var encPayload = encryptionService.encrypt(pkcs8Private, productId, keyId, "v1");

        ProductSigningKey savedKey = new ProductSigningKey(
                workspaceId, productId, keyId, b64Public, encPayload.ciphertext(), encPayload.nonce(), encPayload.version()
        );

        when(signingKeyRepository.findByProductIdAndStatus(productId, KeyStatus.ACTIVE)).thenReturn(Optional.of(savedKey));

        PrivateKey reconstructedPrivate = keyService.getActivePrivateKey(productId);
        assertNotNull(reconstructedPrivate);
        assertEquals(keyPair.getPrivate(), reconstructedPrivate);
    }
}

