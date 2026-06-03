package com.gamyeon.answer.adapter.out.persistence;

import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.infrastucture.JpaQuestionSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("answerQuestionSetLoadAdapter")
@RequiredArgsConstructor
public class QuestionSetLoadAdapter implements LoadQuestionSetPort {

  private final JpaQuestionSetRepository jpaQuestionSetRepository;

  @Override
  public boolean existsById(Long questionSetId) {
    return jpaQuestionSetRepository.existsById(questionSetId);
  }

  @Override
  public Long getIntvId(Long questionSetId) {
    QuestionSet questionSet =
        jpaQuestionSetRepository
            .findById(questionSetId)
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.QUESTION_SET_NOT_FOUND));

    return questionSet.getIntvId();
  }

  @Override
  public String getQuestionContent(Long questionSetId) {

    QuestionSet questionSet =
        jpaQuestionSetRepository
            .findById(questionSetId)
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.QUESTION_SET_NOT_FOUND));

    return questionSet.getContent();
  }
}
