package com.gamyeon.answer.application.port.out;

public interface LoadQuestionSetPort {

  boolean existsById(Long questionSetId);

  Long getIntvId(Long questionSetId);

  String getQuestionContent(Long questionSetId);
}
