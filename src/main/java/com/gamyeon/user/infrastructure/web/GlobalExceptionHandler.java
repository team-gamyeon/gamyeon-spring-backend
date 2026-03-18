package com.gamyeon.user.infrastructure.web;

import com.gamyeon.common.exception.BaseException;
import com.gamyeon.common.exception.CommonErrorCode;
import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.common.response.ErrorDetail;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
    return ApiResponse.fail(e.getErrorCode());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException e) {
    BindingResult bindingResult = e.getBindingResult();
    List<ErrorDetail> fieldErrors =
        bindingResult.getFieldErrors().stream()
            .map(fe -> ErrorDetail.of(fe.getField(), fe.getDefaultMessage()))
            .toList();
    return ApiResponse.fail(CommonErrorCode.INVALID_INPUT, fieldErrors);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
      NoResourceFoundException e) {
    return ApiResponse.fail(CommonErrorCode.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("[Unhandled Exception] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
    return ApiResponse.fail(CommonErrorCode.INTERNAL_ERROR);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
      MethodArgumentTypeMismatchException e) {
    return ApiResponse.fail(CommonErrorCode.INVALID_INPUT);
  }
}
