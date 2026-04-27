package com.gamyeon.answer.infrastructure.scheduler;

import com.gamyeon.answer.application.AnswerAnalysisDispatchService;
import com.gamyeon.answer.application.AnswerAnalysisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerAnalysisScheduler {

  private final AnswerAnalysisDispatchService answerAnalysisDispatchService;
  private final AnswerAnalysisProperties answerAnalysisProperties;

  @Scheduled(fixedDelayString = "${answer.analysis.scheduler-fixed-delay-ms:60000}")
  public void dispatchPendingJobs() {
    int dispatchedCount =
        answerAnalysisDispatchService.dispatchPendingJobs(
            answerAnalysisProperties.getDispatchBatchSize());

    if (dispatchedCount > 0) {
      log.info("Dispatched {} pending STT analysis jobs.", dispatchedCount);
    }
  }
}
