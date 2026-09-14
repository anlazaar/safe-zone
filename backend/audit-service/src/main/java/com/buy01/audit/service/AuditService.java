package com.buy01.audit.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.buy01.audit.entity.MediaAudit;
import com.buy01.audit.entity.ProductAudit;
import com.buy01.audit.entity.UserAudit;
import com.buy01.audit.event.AuditEvent;
import com.buy01.audit.repository.MediaAuditRepo;
import com.buy01.audit.repository.ProductAuditRepo;
import com.buy01.audit.repository.UserAuditRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final MediaAuditRepo mediaRepo;
    private final ProductAuditRepo productRepo;
    private final UserAuditRepo userRepo;

    @KafkaListener(topics = "audit-events", groupId = "audit-service")
    public void consume(AuditEvent event) {

        switch (event.entityType()) {
            case USER:
                UserAudit userAudit = UserAudit
                        .builder()
                        .userId(event.entityId())
                        .executorId(event.executorId())
                        .action(event.action())
                        .isAdmin(event.isAdmin())
                        .timestamp(event.timestamp())
                        .build();

                userRepo.save(userAudit);
                break;

            case PRODUCT:
                ProductAudit productAudit = ProductAudit
                        .builder()
                        .productId(event.entityId())
                        .executorId(event.executorId())
                        .action(event.action())
                        .isAdmin(event.isAdmin())
                        .timestamp(event.timestamp())
                        .build();

                productRepo.save(productAudit);
                break;

            case MEDIA:
                MediaAudit mediaAudit = MediaAudit
                        .builder()
                        .mediaId(event.entityId())
                        .executorId(event.executorId())
                        .action(event.action())
                        .isAdmin(event.isAdmin())
                        .timestamp(event.timestamp())
                        .build();

                mediaRepo.save(mediaAudit);
                break;
        }
    }
}
