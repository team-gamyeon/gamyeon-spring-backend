package com.gamyeon.intv.domain;

import com.gamyeon.common.response.ErrorCode;
import org.springframework.http.HttpStatus;

public enum IntvErrorCode implements ErrorCode {
  DO_NOT_PAUSE(HttpStatus.BAD_REQUEST, "INTV-B001", "진행 중인 면접만 중단할 수 있습니다."),
  DO_NOT_RESUME(HttpStatus.BAD_REQUEST, "INTV-B002", "중단된 면접만 재개할 수 있습니다."),
  DO_NOT_FINISH(HttpStatus.BAD_REQUEST, "INTV-B003", "진행 중인 면접만 종료할 수 있습니다."),
  DO_NOT_START(HttpStatus.BAD_REQUEST, "INTV-B004", "READY 상태의 면접만 시작할 수 있습니다."),
  INVALID_PERIOD(HttpStatus.BAD_REQUEST, "INTV-B005", "startDate는 endDate보다 늦을 수 없습니다."),
  INTV_NOT_FOUND(HttpStatus.NOT_FOUND, "INTV-N001", "해당 면접을 찾을 수 없습니다."),
  INTV_FORBIDDEN(HttpStatus.FORBIDDEN, "INTV-A001", "접근 권한이 없습니다.");

  private final HttpStatus status;
  private final String message;
  private final String code;

  IntvErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getCode() {
    return code;
  }
}
