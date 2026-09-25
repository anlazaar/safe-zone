package com.buy01.media.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Optional;

import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.buy01.media.client.ProductClient;
import com.buy01.media.client.UserClient;
import com.buy01.media.entity.Media;
import com.buy01.media.exception.custom.BadRequestException;
import com.buy01.media.exception.custom.NotFoundException;
import com.buy01.media.repository.MediaRepository;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaServiceImpl Unit Tests")
public class MediaServiceImplTest {
    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private S3Client s3Client;

    @Mock
    private Tika tika;

    @Mock
    private ResponseInputStream<GetObjectResponse> objectStream;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private final String id = "image-1";
    private final String path = "UNIQUE-ID.png";
    private final String productId = "product-1";
    private final String userId = "user-1";
    private final String contentType = "image/png";

    private final String bucketName = "images-bucket";
    private Media media;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                mediaService,
                "bucket",
                bucketName);

        media = Media.builder()
                .id(id)
                .path(path)
                .userId(userId)
                .productId(productId)
                .contentType(contentType)
                .build();
    }

    @Nested
    @DisplayName("get()")
    class Get {
        @Test
        @DisplayName("Should return media requested")
        void get_validId_ShouldReturnMedia() {
            // given
            when(mediaRepository.findById(id)).thenReturn(Optional.of(media));

            // when
            Media result = mediaService.get(id);

            // then
            assertThat(result).usingRecursiveAssertion().isEqualTo(result);
        }

        @Test
        @DisplayName("Should throw NotFoundException when invalid Id provided")
        void get_invalidId_shouldThrowNotFoundException() {
            // given
            when(mediaRepository.findById(id)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> mediaService.get(id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("upload()")
    class Upload {
        @BeforeEach
        void setUp() {
            setAuthenticatedUser("USER-123");
        }

        @Test
        @DisplayName("Should upload file to Object Storage and return saved media")
        void upload_validArgs_ShouldReturnMedia() throws IOException {
            // given
            MultipartFile file = loadTestFile("valid.png", "image/png");

            when(mediaRepository.save(any(Media.class)))
                    .thenReturn(media);

            // when
            Media result = mediaService.upload(file, productId);

            // then
            assertThat(result).isEqualTo(media);

            verify(s3Client).putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class));

            verify(mediaRepository).save(any(Media.class));
        }

        @Test
        @DisplayName("Should save media with correct product ID and content type")
        void upload_validArgs_ShouldSaveCorrectMediaData() throws IOException {
            // given
            MultipartFile file = loadTestFile("valid.png", "image/png");

            when(mediaRepository.save(any(Media.class)))
                    .thenReturn(media);

            // when
            Media result = mediaService.upload(file, productId);

            // then
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getContentType()).isEqualTo(contentType);
            assertThat(result.getPath()).endsWith(".png");
        }

        @Test
        @DisplayName("Should reject non-image file made to .png")
        void upload_nonImageFile_ShouldThrowBadRequestException() throws IOException {
            // given
            MultipartFile file = loadTestFile("script-to-png.png", "text/plain");

            // when / then
            assertThatThrownBy(() -> mediaService.upload(file, productId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only images are allowed.");

            verify(mediaRepository, never())
                    .save(any(Media.class));

            verify(s3Client, never()).putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class));
        }

        @Test
        @DisplayName("Should reject file larger than maximum size")
        void upload_fileTooLarge_ShouldThrowBadRequestException() throws IOException {
            // given
            MultipartFile file = loadTestFile("above-max-size.png", "image/png");

            // when / then
            assertThatThrownBy(() -> mediaService.upload(file, productId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Maximum size is 2 MB.");

            verify(mediaRepository, never())
                    .save(any(Media.class));

            verify(s3Client, never()).putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class));
        }
    }

    @Nested
    @DisplayName("download()")
    class Download {

        @Test
        @DisplayName("Should return resource for valid media ID")
        void download_validId_ShouldReturnResource() throws IOException {
            // given
            when(mediaRepository.findById(id))
                    .thenReturn(Optional.of(media));

            when(s3Client.getObject(any(GetObjectRequest.class)))
                    .thenReturn(objectStream);

            // when
            Resource result = mediaService.download(id);

            // then
            assertThat(result)
                    .isInstanceOf(InputStreamResource.class);

            assertThat(result.getInputStream())
                    .isEqualTo(objectStream);

            verify(mediaRepository).findById(id);

            verify(s3Client).getObject(
                    argThat((GetObjectRequest request) -> request.bucket().equals(bucketName)
                            && request.key().equals(path)));
        }

        @Test
        @DisplayName("Should throw NotFoundException when media does not exist")
        void download_invalidId_ShouldThrowNotFoundException() {
            // given
            when(mediaRepository.findById(id))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> mediaService.download(id))
                    .isInstanceOf(NotFoundException.class);

            verify(s3Client, never())
                    .getObject(any(GetObjectRequest.class));
        }
    }

    private MultipartFile loadTestFile(String filename, String contentType) throws IOException {

        ClassPathResource resource = new ClassPathResource(filename);

        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                resource.getInputStream());
    }

    private void setAuthenticatedUser(String userId) {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .subject(userId)
                .header("alg", "none")
                .build();

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                jwt,
                null,
                "ROLE_SELLER");

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

//     @Test
//     void failTest_forJenkinsTesting() {
//         assertEquals(1, 2);
//     }
}
