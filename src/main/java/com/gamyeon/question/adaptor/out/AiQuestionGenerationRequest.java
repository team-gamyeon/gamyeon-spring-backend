package com.gamyeon.question.adaptor.out;

import com.gamyeon.question.application.port.out.PreparationForQuestionGeneration;
import java.util.List;

public record AiQuestionGenerationRequest(
    Long intvId, List<AiQuestionGenerationSourceRequest> files) {

  public static AiQuestionGenerationRequest from(PreparationForQuestionGeneration preparation) {
    return new AiQuestionGenerationRequest(
        preparation.intvId(),
        preparation.files().stream().map(AiQuestionGenerationSourceRequest::from).toList());
  }
}
