package com.gamyeon.question.adaptor.out;

import com.gamyeon.question.application.port.out.GenerateCustomQuestionPort;
import com.gamyeon.question.application.port.out.PreparationForQuestionGeneration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AiQuestionGenerationAdapter implements GenerateCustomQuestionPort {

  private final PythonQuestionGenerationFeignClient pythonQuestionGenerationFeignClient;

  @Override
  public void request(PreparationForQuestionGeneration preparation) {
    log.info(
        "Calling AI question generation API. preparationId={}, intvId={}",
        preparation.preparationId(),
        preparation.intvId());
    AiQuestionGenerationRequest request = AiQuestionGenerationRequest.from(preparation);
    pythonQuestionGenerationFeignClient.requestGeneration(request);
  }
}
