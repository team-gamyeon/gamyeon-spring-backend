package com.gamyeon.report.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamyeon.report.application.port.in.ReportCallbackUseCase;
import com.gamyeon.report.application.port.out.LoadReportPort;
import com.gamyeon.report.application.port.out.SaveReportPort;
import com.gamyeon.report.domain.Report;
import com.gamyeon.report.presentation.dto.request.ReportWebhookRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCallbackService implements ReportCallbackUseCase {

  private final LoadReportPort loadReportPort;
  private final SaveReportPort saveReportPort;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void processReportCallback(ReportWebhookRequest request) {
    log.info(
        "[Report] AI Callback 수신 - intvId={}, status={}", request.getIntvId(), request.getStatus());

    // 1. 리포트 조회 (Lock 활용)
    Report report =
        loadReportPort
            .findByIntvIdWithLock(request.getIntvId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException("존재하지 않는 리포트입니다. intvId: " + request.getIntvId()));

    // 2. 상태에 따른 처리
    if ("SUCCEED".equals(request.getStatus()) && request.getReport() != null) {
      ReportWebhookRequest.ReportDetailData detail = request.getReport();

      //  Hibernate ClassCastException 해결: DTO 객체를 Map으로 변환하여 jsonb 저장 준비
      Map<String, Object> reportDataMap = objectMapper.convertValue(detail, Map.class);

      // 3. JSONB map에서 직접 추출 → DB 컬럼과 JSONB 값을 동일 소스로 통일
      int totalScore =
          reportDataMap.get("total_score") instanceof Number n
              ? n.intValue()
              : (detail.getTotalScore() != null ? detail.getTotalScore() : 0);
      int answeredCount =
          reportDataMap.get("answered_count") instanceof Number n
              ? n.intValue()
              : (detail.getAnsweredCount() != null ? detail.getAnsweredCount() : 0);

      log.info(
          "[Report] total_score from map={}, from dto={}",
          reportDataMap.get("total_score"),
          detail.getTotalScore());

      report.complete(
          totalScore,
          answeredCount,
          detail.getStrengths(),
          detail.getWeaknesses(),
          reportDataMap // JSONB 컬럼에 Map 형태로 저장
          );

      log.info("[Report] 리포트 생성 완료 - intvId={}", request.getIntvId());
    } else {
      report.fail();
      log.warn(
          "[Report] 리포트 생성 실패 혹은 데이터 누락 - intvId={}, errorMessage={}",
          request.getIntvId(),
          request.getErrorMessage());
    }

    // 4. 최종 상태 저장
    saveReportPort.save(report);
  }
}
