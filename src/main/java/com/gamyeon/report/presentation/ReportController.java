package com.gamyeon.report.presentation;

import com.gamyeon.common.response.SuccessResponse;
import com.gamyeon.report.application.port.in.GetReportDetailUseCase;
import com.gamyeon.report.application.port.in.GetReportListUseCase;
import com.gamyeon.report.infrastructure.web.dto.ReportDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/report")
public class ReportController {

  private final GetReportListUseCase getReportListUseCase;
  private final GetReportDetailUseCase getReportDetailUseCase;

  /**
   * [9-1] 리포트 목록 조회 GET /api/v1/report/list?userId={userId} [SECURITY TODO] MVP2에서 JWT 도입 시 userId는
   * 토큰에서 추출하도록 변경 필요
   */
  //    @GetMapping("/list")
  //    public SuccessResponse<List<ReportListResponse>> getReportList(Long userId) {
  //        userId = 1L;
  //        List<ReportListResponse> response = getReportListUseCase.getList(userId);
  //        return SuccessResponse.of(response);
  //    }

  /** [9-2] 리포트 상세 조회 GET /api/v1/report/detail/{intvId}?userId={userId} */
  @GetMapping("/{intvId}")
  public SuccessResponse<ReportDetailResponse> getReportDetail(
      @PathVariable("intvId") Long intvId, Long userId) {
    userId = 1L;
    ReportDetailResponse response = getReportDetailUseCase.getDetail(intvId, userId);
    return SuccessResponse.of(response);
  }
}
