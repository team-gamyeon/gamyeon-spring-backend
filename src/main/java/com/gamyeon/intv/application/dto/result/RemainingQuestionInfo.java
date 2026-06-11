package com.gamyeon.intv.application.dto.result;

import com.gamyeon.question.domain.QuestionSet;

public record RemainingQuestionInfo(Long questionSetId, Integer questionOrder, String content) {

  public static RemainingQuestionInfo from(QuestionSet questionSet) {
    return new RemainingQuestionInfo(
        questionSet.getId(), questionSet.getQuestionOrder(), questionSet.getContent());
  }
}
