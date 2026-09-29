package com.buy01.media.service;

import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.buy01.media.aop.Auditable;
import com.buy01.media.client.ProductClient;
import com.buy01.media.client.UserClient;
import com.buy01.media.entity.Media;
import com.buy01.media.exception.custom.BadRequestException;
import com.buy01.media.exception.custom.ForbiddenException;
import com.buy01.media.exception.custom.NotFoundException;
import com.buy01.media.repository.MediaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import com.buy01.media.event.AuditAction;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

        private final MediaRepository mediaRepository;
        private final UserClient userClient;
        private final ProductClient productClient;
        private final S3Client s3Client;
        private final Tika tika = new Tika();

        private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;

        @Value("${minio.bucket}")
        private String bucket;

        @Override
        public Media get(String id) {
                return getMediaOrThrow(id);
        }

        @Override
        @Auditable(action = AuditAction.UPLOADED, entityId = "#result.id")
        public Media upload(MultipartFile file, String productId) throws IOException {

                validate(file);

                String objectName = UUID.randomUUID()
                                + getExtension(file.getOriginalFilename());

                PutObjectRequest request = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(objectName)
                                .contentType(file.getContentType())
                                .build();

                s3Client.putObject(
                                request,
                                RequestBody.fromInputStream(
                                                file.getInputStream(),
                                                file.getSize()));

                Media media = Media.builder()
                                .path(objectName)
                                .productId(productId)
                                .userId(getCurrentUUID())
                                .contentType(file.getContentType())
                                .build();

                return mediaRepository.save(media);
        }

        @Override
        public Resource download(String id) {
                Media media = getMediaOrThrow(id);

                ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(
                                GetObjectRequest.builder()
                                                .bucket(bucket)
                                                .key(media.getPath())
                                                .build());

                return new InputStreamResource(stream);
        }

        @Override
        @Auditable(action = AuditAction.DELETED, entityId = "#id")
        public void delete(String id, boolean fromService) {
                Media media = getMediaOrThrow(id);

                verifyOwnership(media);

                s3Client.deleteObject(
                                DeleteObjectRequest.builder()
                                                .bucket(bucket)
                                                .key(media.getPath())
                                                .build());

                mediaRepository.delete(media);

                if (!fromService) {
                        notifyRelatedService(media);
                }
        }

        @Override
        public Page<Media> getMediaByUserId(String userId, Pageable pageable) {
                return mediaRepository.findByUserId(userId, pageable);
        }

        private void validate(MultipartFile file) throws IOException {
                if (file.isEmpty()) {
                        throw new BadRequestException(
                                        "File is empty.");
                }

                if (file.getSize() > MAX_FILE_SIZE) {
                        throw new BadRequestException(
                                        "Maximum size is 2 MB.");
                }

                String contentType = file.getContentType();
                if (contentType == null ||
                                !contentType.startsWith("image/")) {

                        throw new BadRequestException(
                                        "Only images are allowed.");
                }

                String detectedType = tika.detect(file.getInputStream());

                if (!detectedType.startsWith("image/")) {
                        throw new BadRequestException(
                                        "Uploaded file is not a valid image.");
                }

                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                        throw new BadRequestException(
                                        "Uploaded file is not a valid image.");
                }
        }

        private String getExtension(String filename) {
                if (filename == null || !filename.contains(".")) {
                        return "";
                }

                return filename.substring(
                                filename.lastIndexOf('.'));
        }

        @Override
        public long countMedia() {
                return mediaRepository.count();
        }

        private boolean isCurrentOwnerOrAdmin(String ownerId) {

                String currentUserId = getCurrentUUID();

                boolean isAdmin = SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getAuthorities()
                                .stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                return ownerId.equals(currentUserId) || isAdmin;
        }

        private String getCurrentUUID() {
                Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();

                return jwt.getSubject();
        }

        private void verifyOwnership(Media media) {
                if (!isCurrentOwnerOrAdmin(media.getUserId())) {
                        throw new ForbiddenException(
                                        "Sorry! You are not the owner of this Media");
                }
        }

        private Media getMediaOrThrow(String id) {
                return mediaRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Image not found"));
        }

        private void notifyRelatedService(Media media) {

                if (media.getProductId() != null && !media.getProductId().isBlank()) {
                        notifyProductService(media);
                        return;
                }

                notifyUserService(media.getId());
        }

        private void notifyUserService(String mediaId) {
                try {
                        userClient.deleteAvatar();

                } catch (Exception e) {
                        log.error(
                                        "Failed to notify user-service about deleted avatar image {}",
                                        mediaId,
                                        e);
                }
        }

        private void notifyProductService(Media media) {
                try {
                        productClient.removeImageFromProduct(
                                        media.getProductId(),
                                        media.getId());

                } catch (Exception e) {
                        log.error(
                                        "Failed to notify product-service about deleted image {} for product {}",
                                        media.getId(),
                                        media.getProductId(),
                                        e);
                }
        }
}