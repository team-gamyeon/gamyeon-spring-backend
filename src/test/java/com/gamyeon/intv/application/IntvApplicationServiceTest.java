package com.gamyeon.intv.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.intv.application.dto.result.IntvListInfo;
import com.gamyeon.intv.application.dto.result.ResumeContextInfo;
import com.gamyeon.intv.application.dto.result.ResumeIntvInfo;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvErrorCode;
import com.gamyeon.intv.domain.IntvException;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.preparation.application.port.in.PreparationUseCase;
import com.gamyeon.question.domain.QuestionSet;
import com.gamyeon.question.domain.QuestionSetRepository;
import com.gamyeon.report.application.port.in.ReportSummaryQueryUseCase;
import com.gamyeon.report.application.port.in.ReportSummaryResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("면접 서비스 - 재개 컨텍스트")
@ExtendWith(MockitoExtension.class)
class IntvApplicationServiceTest {

  @Mock private IntvRepository intvRepository;
  @Mock private QuestionSetRepository questionSetRepository;
  @Mock private AnswerRepository answerRepository;
  @Mock private ReportSummaryQueryUseCase reportSummaryQueryUseCase;
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
            reportSummaryQueryUseCase,
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

  @Test
  @DisplayName("INTV-004 - 완료된 면접에만 리포트 요약을 결합해야 한다")
  void shouldAttachReportSummaryOnlyToFinishedInterview() {
    Intv paused = intv(10L, 99L);
    paused.start();
    paused.pause();
    Intv finished = intv(11L, 99L);
    finished.start();
    finished.finish();
    PageRequest pageable = PageRequest.of(0, 20);
    ReportSummaryResult report =
        new ReportSummaryResult(11L, 101L, "SUCCEED", 85, 5, List.of("논리성"), List.of("속도"));

    given(intvRepository.findAllByUserIdAndStatuses(99L, List.of(), pageable))
        .willReturn(new PageImpl<>(List.of(paused, finished), pageable, 2));
    given(reportSummaryQueryUseCase.findAllByIntvIds(List.of(11L))).willReturn(List.of(report));

    IntvListInfo result = intvApplicationService.getIntvs(99L, List.of(), pageable);

    assertEquals(2, result.content().size());
    assertEquals(null, result.content().get(0).report());
    assertEquals(101L, result.content().get(1).report().reportId());
  }

  @Test
  @DisplayName("INTV-005 - 면접 재개 시 답변이 없는 남은 질문을 순서대로 반환해야 한다")
  void shouldReturnRemainingQuestionsWhenInterviewResumes() {
    Intv intv = pausedIntv(10L, 99L);
    List<QuestionSet> questions =
        List.of(
            question(101L, 10L, 1),
            question(102L, 10L, 2),
            question(103L, 10L, 3),
            question(104L, 10L, 4),
            question(105L, 10L, 5),
            question(106L, 10L, 6),
            question(107L, 10L, 7));

    given(intvRepository.findById(10L)).willReturn(Optional.of(intv));
    given(questionSetRepository.getAllByIntvId(10L)).willReturn(questions);
    given(answerRepository.findAllByIntvId(10L))
        .willReturn(List.of(answer(1L, 10L, 101L), answer(2L, 10L, 102L), answer(3L, 10L, 104L)));

    ResumeIntvInfo result =
        intvApplicationService.resume(
            new com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand(99L, 10L));

    assertEquals(com.gamyeon.intv.domain.IntvStatus.IN_PROGRESS, result.intvStatus());
    assertFalse(result.completed());
    assertEquals(7, result.totalQuestionCount());
    assertEquals(3, result.answeredCount());
    assertEquals(List.of(103L, 105L, 106L, 107L), questionIds(result));
  }

  @Test
  @DisplayName("INTV-006 - 모든 질문에 답변이 있으면 완료된 재개 결과를 반환해야 한다")
  void shouldReturnCompletedResumeResultWhenAllQuestionsAreAnswered() {
    Intv intv = pausedIntv(10L, 99L);
    List<QuestionSet> questions =
        List.of(
            question(101L, 10L, 1),
            question(102L, 10L, 2),
            question(103L, 10L, 3),
            question(104L, 10L, 4),
            question(105L, 10L, 5),
            question(106L, 10L, 6),
            question(107L, 10L, 7));
    List<Answer> answers =
        questions.stream()
            .map(question -> answer(question.getId(), 10L, question.getId()))
            .toList();

    given(intvRepository.findById(10L)).willReturn(Optional.of(intv));
    given(questionSetRepository.getAllByIntvId(10L)).willReturn(questions);
    given(answerRepository.findAllByIntvId(10L)).willReturn(answers);

    ResumeIntvInfo result =
        intvApplicationService.resume(
            new com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand(99L, 10L));

    assertTrue(result.completed());
    assertEquals(7, result.answeredCount());
    assertTrue(result.remainingQuestions().isEmpty());
  }

  private Intv intv(Long intvId, Long userId) {
    Intv intv = Intv.create(userId, "면접");
    ReflectionTestUtils.setField(intv, "id", intvId);
    return intv;
  }

  private Intv pausedIntv(Long intvId, Long userId) {
    Intv intv = intv(intvId, userId);
    intv.start();
    intv.pause();
    return intv;
  }

  private List<Long> questionIds(ResumeIntvInfo info) {
    return info.remainingQuestions().stream().map(question -> question.questionSetId()).toList();
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
