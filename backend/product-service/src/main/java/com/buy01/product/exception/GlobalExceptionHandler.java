package com.buy01.product.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.buy01.product.exception.custom.BadRequestException;
import com.buy01.product.exception.custom.ConflictException;
import com.buy01.product.exception.custom.ForbiddenException;
import com.buy01.product.exception.custom.NotFoundException;
import com.buy01.product.exception.custom.UnauthorizedException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
        private static final String ERROR_KEY = "error";

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(
                        MethodArgumentNotValidException ex) {

                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(errors);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<?> handleBadRequest(BadRequestException ex) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<?> handleNotFound(NotFoundException ex) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<?> handleInvalidJson(HttpMessageNotReadableException ex) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                ERROR_KEY, "Invalid JSON format",
                                                "details", ex.getMostSpecificCause() != null
                                                                ? ex.getMostSpecificCause().getMessage()
                                                                : ex.getMessage()));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<?> handleInvalidRoute(NoResourceFoundException ex) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<?> handleConflict(ConflictException ex) {
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<?> handleForbidden(ForbiddenException ex) {
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
                return ResponseEntity
                                .status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<?> handleAuthorizationDenied(HttpMediaTypeNotSupportedException ex) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<?> handleMaxUploadSizeExceeded(
                        MaxUploadSizeExceededException ex) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                ERROR_KEY, "Maximum file size is 2 MB."));
        }

        @ExceptionHandler(AuthorizationDeniedException.class)
        public ResponseEntity<?> handleAuthorizationDenied(AuthorizationDeniedException ex) {
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<?> versionConflictDenied(OptimisticLockingFailureException ex) {
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                ERROR_KEY,
                                                "The resource was modified by another user. Please refresh and try again."));
        }

        @ExceptionHandler(FeignException.class)
        public ResponseEntity<?> handleFeignException(FeignException ex) {

                return ResponseEntity
                                .status(ex.status() > 0
                                                ? ex.status()
                                                : HttpStatus.BAD_GATEWAY.value())
                                .body(Map.of(
                                                ERROR_KEY, ex.getMessage()));
        }

        @ExceptionHandler(MissingServletRequestPartException.class)
        public ResponseEntity<?> handleMissingRequestPart(
                        MissingServletRequestPartException ex) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                ERROR_KEY, ex.getRequestPartName() + " is required."));
        }

        // Catch All
        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handleGenericException(Exception ex) {

                log.error("Unhandled exception occurred", ex);
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of(
                                                ERROR_KEY, "Internal Server Error"));
        }

}