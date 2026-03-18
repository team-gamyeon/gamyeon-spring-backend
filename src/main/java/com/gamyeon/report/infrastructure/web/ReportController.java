package com.gamyeon.report.infrastructure.web;

import com.gamyeon.common.response.ApiResponse;
import com.gamyeon.report.application.exception.ReportSuccessCode;
import com.gamyeon.report.application.port.in.DeleteReportUseCase;
import com.gamyeon.report.application.port.in.GetReportDetailUseCase;
import com.gamyeon.report.application.port.in.GetReportListUseCase;
import com.gamyeon.report.infrastructure.web.dto.ReportDetailResponse;
import com.gamyeon.report.infrastructure.web.dto.ReportListResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

  private final GetReportListUseCase getReportListUseCase;
  private final GetReportDetailUseCase getReportDetailUseCase;
  private final DeleteReportUseCase deleteReportUseCase;

  // [SECURITY TODO] JWT 도입 후 userId는 토큰에서 추출하도록 변경 필요

  @GetMapping("/list")
  public ResponseEntity<ApiResponse<List<ReportListResponse>>> getList(@RequestParam Long userId) {
    log.info("[Report] 목록 조회 - userId={}", userId);
    return ApiResponse.success(
        ReportSuccessCode.REPORT_LIST_SUCCESS, getReportListUseCase.getList(userId));
  }

  @GetMapping("/{intvId}")
  public ResponseEntity<ApiResponse<ReportDetailResponse>> getDetail(
      @PathVariable Long intvId, Long userId) {
    userId = 1L;
    log.info("[Report] 상세 조회 - intvId={}, userId={}", intvId, userId);
    return ApiResponse.success(
        ReportSuccessCode.REPORT_DETAIL_SUCCESS, getReportDetailUseCase.getDetail(intvId, userId));
  }

  @DeleteMapping("/{intvId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @PathVariable Long intvId, @RequestParam Long userId) {
    log.info("[Report] 삭제 - intvId={}, userId={}", intvId, userId);
    deleteReportUseCase.delete(intvId, userId);
    return ApiResponse.success(ReportSuccessCode.REPORT_DELETE_SUCCESS);
  }
}
