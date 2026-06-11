package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.ResumeIntvInfo;
import java.util.List;

public record ResumeIntvResponse(
    Long intvId,
    String intvStatus,
    boolean completed,
    int totalQuestionCount,
    int answeredCount,
    List<RemainingQuestionResponse> remainingQuestions) {

  public static ResumeIntvResponse from(ResumeIntvInfo info) {
    return new ResumeIntvResponse(
        info.intvId(),
        info.intvStatus().name(),
        info.completed(),
        info.totalQuestionCount(),
        info.answeredCount(),
        info.remainingQuestions().stream().map(RemainingQuestionResponse::from).toList());
  }
}
