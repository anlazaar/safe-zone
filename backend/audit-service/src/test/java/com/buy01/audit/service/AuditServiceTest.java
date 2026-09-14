package com.buy01.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buy01.audit.entity.MediaAudit;
import com.buy01.audit.entity.ProductAudit;
import com.buy01.audit.entity.UserAudit;
import com.buy01.audit.event.AuditAction;
import com.buy01.audit.event.AuditEvent;
import com.buy01.audit.event.EntityType;
import com.buy01.audit.repository.MediaAuditRepo;
import com.buy01.audit.repository.ProductAuditRepo;
import com.buy01.audit.repository.UserAuditRepo;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private MediaAuditRepo mediaRepo;

    @Mock
    private ProductAuditRepo productRepo;

    @Mock
    private UserAuditRepo userRepo;

    @InjectMocks
    private AuditService auditService;

    @Test
    void consume_WhenTypeIsUser_ShouldSaveUserAudit() {
        Instant now = Instant.now();
        AuditEvent event = new AuditEvent(
                "user-100",
                EntityType.USER,
                AuditAction.CREATED,
                "executor-999",
                true,
                now);

        auditService.consume(event);

        ArgumentCaptor<UserAudit> captor = ArgumentCaptor.forClass(UserAudit.class);
        verify(userRepo).save(captor.capture());

        verifyNoInteractions(productRepo, mediaRepo);

        UserAudit savedAudit = captor.getValue();
        assertEquals("user-100", savedAudit.getUserId());
        assertEquals("executor-999", savedAudit.getExecutorId());
        assertEquals(AuditAction.CREATED, savedAudit.getAction());
        assertTrue(savedAudit.isAdmin());
        assertEquals(now, savedAudit.getTimestamp());
    }

    @Test
    void consume_WhenTypeIsProduct_ShouldSaveProductAudit() {
        Instant now = Instant.now();
        AuditEvent event = new AuditEvent(
                "prod-200",
                EntityType.PRODUCT,
                AuditAction.MODIFIED,
                "executor-888",
                false,
                now);

        auditService.consume(event);

        ArgumentCaptor<ProductAudit> captor = ArgumentCaptor.forClass(ProductAudit.class);
        verify(productRepo).save(captor.capture());

        verifyNoInteractions(userRepo, mediaRepo);

        ProductAudit savedAudit = captor.getValue();
        assertEquals("prod-200", savedAudit.getProductId());
        assertEquals("executor-888", savedAudit.getExecutorId());
        assertEquals(AuditAction.MODIFIED, savedAudit.getAction());
        assertFalse(savedAudit.isAdmin());
        assertEquals(now, savedAudit.getTimestamp());
    }

    @Test
    void consume_WhenTypeIsMedia_ShouldSaveMediaAudit() {
        // Arrange
        Instant now = Instant.now();
        AuditEvent event = new AuditEvent(
                "media-300",
                EntityType.MEDIA,
                AuditAction.DELETED,
                "executor-777",
                true,
                now);

        auditService.consume(event);

        ArgumentCaptor<MediaAudit> captor = ArgumentCaptor.forClass(MediaAudit.class);
        verify(mediaRepo).save(captor.capture());

        verifyNoInteractions(userRepo, productRepo);

        MediaAudit savedAudit = captor.getValue();
        assertEquals("media-300", savedAudit.getMediaId());
        assertEquals("executor-777", savedAudit.getExecutorId());
        assertEquals(AuditAction.DELETED, savedAudit.getAction());
        assertTrue(savedAudit.isAdmin());
        assertEquals(now, savedAudit.getTimestamp());
    }
}