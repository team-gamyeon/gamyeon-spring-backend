package com.gamyeon.intv.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.intv.application.dto.result.ResumeContextInfo;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvErrorCode;
import com.gamyeon.intv.domain.IntvException;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.preparation.application.port.in.PreparationUseCase;
import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.domain.QuestionSetRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("면접 서비스 - 재개 컨텍스트")
@ExtendWith(MockitoExtension.class)
class IntvApplicationServiceTest {

  @Mock private IntvRepository intvRepository;
  @Mock private QuestionSetRepository questionSetRepository;
  @Mock private AnswerRepository answerRepository;
  @Mock private PreparationUseCase preparationUseCase;
  @Mock private ApplicationEventPublisher eventPublisher;

  private IntvApplicationService intvApplicationService;

  @BeforeEach
  void setUp() {
    intvApplicationService =
        new IntvApplicationService(
            intvRepository,
            questionSetRepository,
            answerRepository,
            preparationUseCase,
            eventPublisher);
  }

  @Test
  @DisplayName("INTV-001 - 답변 없는 가장 작은 questionOrder를 다음 질문으로 반환해야 한다")
  void shouldReturnFirstUnansweredQuestionAsNextQuestion() {
    Intv intv = intv(10L, 99L);
    QuestionSet q1 = question(100L, 10L, 1);
    QuestionSet q2 = question(101L, 10L, 2);
    QuestionSet q3 = question(102L, 10L, 3);
    QuestionSet q4 = question(103L, 10L, 4);
    Answer a1 = answer(1L, 10L, 100L);
    Answer a2 = answer(2L, 10L, 101L);
    Answer a4 = answer(4L, 10L, 103L);

    given(intvRepository.findById(10L)).willReturn(Optional.of(intv));
    given(questionSetRepository.getAllByIntvId(10L)).willReturn(List.of(q1, q2, q3, q4));
    given(answerRepository.findAllByIntvId(10L)).willReturn(List.of(a1, a2, a4));

    ResumeContextInfo result = intvApplicationService.getResumeContext(99L, 10L);

    assertEquals(4, result.totalQuestionCount());
    assertEquals(3, result.answeredCount());
    assertFalse(result.completed());
    assertEquals(102L, result.nextQuestionSetId());
    assertEquals(3, result.nextQuestionOrder());
  }

  @Test
  @DisplayName("INTV-002 - 모든 질문에 답변이 있으면 완료 상태로 응답해야 한다")
  void shouldReturnCompletedWhenAllQuestionsAreAnswered() {
    Intv intv = intv(10L, 99L);
    QuestionSet q1 = question(100L, 10L, 1);
    QuestionSet q2 = question(101L, 10L, 2);
    Answer a1 = answer(1L, 10L, 100L);
    Answer a2 = answer(2L, 10L, 101L);

    given(intvRepository.findById(10L)).willReturn(Optional.of(intv));
    given(questionSetRepository.getAllByIntvId(10L)).willReturn(List.of(q1, q2));
    given(answerRepository.findAllByIntvId(10L)).willReturn(List.of(a1, a2));

    ResumeContextInfo result = intvApplicationService.getResumeContext(99L, 10L);

    assertTrue(result.completed());
    assertEquals(2, result.answeredCount());
    assertEquals(null, result.nextQuestionSetId());
  }

  @Test
  @DisplayName("INTV-003 - READY가 아닌 면접은 다시 시작할 수 없다")
  void shouldRejectStartWhenInterviewIsNotReady() {
    Intv intv = Intv.create(99L, "면접");
    intv.start();

    IntvException exception = assertThrows(IntvException.class, intv::start);

    assertEquals(IntvErrorCode.DO_NOT_START, exception.getErrorCode());
  }

  private Intv intv(Long intvId, Long userId) {
    Intv intv = Intv.create(userId, "면접");
    ReflectionTestUtils.setField(intv, "id", intvId);
    return intv;
  }

  private QuestionSet question(Long questionSetId, Long intvId, Integer questionOrder) {
    QuestionSet questionSet = QuestionSet.create(intvId, "질문 " + questionOrder, questionOrder);
    ReflectionTestUtils.setField(questionSet, "id", questionSetId);
    return questionSet;
  }

  private Answer answer(Long answerId, Long intvId, Long questionSetId) {
    Answer answer =
        Answer.create(
            intvId,
            questionSetId,
            "answer.mp4",
            "answers/video.mp4",
            "https://cdn.example.com/answers/video.mp4",
            "video/mp4",
            1024L);
    ReflectionTestUtils.setField(answer, "id", answerId);
    return answer;
  }
}
