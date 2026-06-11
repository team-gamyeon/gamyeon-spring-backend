package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.RemainingQuestionInfo;

public record RemainingQuestionResponse(Long questionSetId, Integer questionOrder, String content) {

  public static RemainingQuestionResponse from(RemainingQuestionInfo info) {
    return new RemainingQuestionResponse(
        info.questionSetId(), info.questionOrder(), info.content());
  }
}
