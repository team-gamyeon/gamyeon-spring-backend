package com.gamyeon.notif.adapter.in.web;

/**
 * API 명세서의 응답 포맷(success, code, message, data)을 준수하기 위한 레코드입니다. ※ 프로젝트 전역에 이미 사용하는 공통 ApiResponse
 * 클래스가 있다면 이 레코드는 삭제하고 대체하세요.
 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, "NTF-S000", "success", data);
  }

  public static ApiResponse<Void> successWithNoData() {
    return new ApiResponse<>(true, "NTF-S000", "success", null);
  }
}
