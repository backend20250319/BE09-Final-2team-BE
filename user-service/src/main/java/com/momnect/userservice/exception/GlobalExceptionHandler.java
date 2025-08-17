package com.momnect.userservice.exception;

import com.momnect.userservice.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 모든 예외 처리 (가장 일반적인 핸들러)
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
//        System.out.println("GlobalExceptionHandler handleException >>>>>");
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.failure("INTERNAL_SERVER_ERROR", ex.getMessage()));
//    }

    /// body 입력값 누락 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("BAD_REQUEST", errorMessages));
    }

    /// 잘못된 body 입력값 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        "BAD_REQUEST",
                        "One or more fields have an invalid value format"
                ));
    }

    ///  잘못된 pathvalue
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        "BAD_REQUEST",
                        ex.getMessage()
                ));
    }

    /// 쿼리스트링 입력값 누락 처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        System.out.println("MissingServletRequestParameterException.class");
        String errorMessage = String.format("Required request parameter '%s' is missing", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("BAD_REQUEST", errorMessage));
    }

    // 쿼리스트링 잘못된 입력값
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(ConstraintViolationException ex) {
        System.out.println("ConstraintViolationException.class");
        // ex.getConstraintViolations()에서 상세 메시지 추출 가능
        String errorMessage = ex.getConstraintViolations()
                .iterator()
                .next()
                .getMessage();

        ApiResponse<?> response = ApiResponse.failure("BAD_REQUEST", errorMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // ResponseStatusException 처리
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        String errorCode = ex.getStatusCode()
                .toString(); // 예: NOT_FOUND, BAD_REQUEST
        String errorMessage = ex.getReason();

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.failure(errorCode, errorMessage));
    }
}
