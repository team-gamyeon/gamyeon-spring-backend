package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.out.AnswerAnalysisTarget;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.application.port.out.RequestAnswerSttAnalysisPort;
import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerAnalysisJob;
import com.gamyeon.answer.domain.AnswerAnalysisJobRepository;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.answer.domain.AnswerRepository;
import feign.FeignException;
import feign.RetryableException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalysisDispatchService {

  private static final long[] RETRY_DELAY_MINUTES = {1, 3, 10, 30, 60};

  private final AnswerRepository answerRepository;
  private final AnswerAnalysisJobRepository answerAnalysisJobRepository;
  private final LoadQuestionSetPort loadQuestionSetPort;
  private final RequestAnswerSttAnalysisPort requestAnswerSttAnalysisPort;

  public int dispatchPendingJobs(int batchSize) {
    int dispatchedCount = 0;

    for (int index = 0; index < batchSize; index++) {
      Optional<AnswerAnalysisDispatchCandidate> candidate = claimNextDispatchCandidate();
      if (candidate.isEmpty()) {
        break;
      }

      dispatch(candidate.get());
      dispatchedCount++;
    }

    return dispatchedCount;
  }

  @Transactional
  public Optional<AnswerAnalysisDispatchCandidate> claimNextDispatchCandidate() {
    LocalDateTime now = LocalDateTime.now();
    Optional<AnswerAnalysisJob> candidateJob =
        answerAnalysisJobRepository.findNextDispatchCandidate(now);

    if (candidateJob.isEmpty()) {
      return Optional.empty();
    }

    AnswerAnalysisJob job = candidateJob.get();
    Answer answer =
        answerRepository
            .findById(job.getAnswerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    String questionContent = loadQuestionSetPort.getQuestionContent(answer.getQuestionSetId());
    job.markSending(now);
    answerAnalysisJobRepository.save(job);

    AnswerAnalysisTarget target =
        new AnswerAnalysisTarget(
            job.getRequestId(),
            answer.getId(),
            answer.getIntvId(),
            answer.getQuestionSetId(),
            questionContent,
            answer.getFileKey());

    return Optional.of(
        new AnswerAnalysisDispatchCandidate(
            job.getId(), answer.getId(), job.getRequestId(), target));
  }

  public void dispatch(AnswerAnalysisDispatchCandidate candidate) {
    try {
      requestAnswerSttAnalysisPort.request(candidate.target());
      markDispatchSuccess(candidate.jobId());
      log.info(
          "Dispatched STT analysis job. jobId={}, answerId={}, requestId={}",
          candidate.jobId(),
          candidate.answerId(),
          candidate.requestId());
    } catch (Exception e) {
      handleDispatchFailure(candidate.jobId(), e);
    }
  }

  @Transactional
  public void markDispatchSuccess(Long jobId) {
    AnswerAnalysisJob job =
        answerAnalysisJobRepository
            .findById(jobId)
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));
    Answer answer =
        answerRepository
            .findById(job.getAnswerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    job.markSent(LocalDateTime.now());
    answer.markSttProcessing();

    answerAnalysisJobRepository.save(job);
    answerRepository.save(answer);
  }

  @Transactional
  public void handleDispatchFailure(Long jobId, Exception exception) {
    AnswerAnalysisJob job =
        answerAnalysisJobRepository
            .findById(jobId)
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));
    Answer answer =
        answerRepository
            .findById(job.getAnswerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    String errorMessage = summarizeException(exception);
    int retryCount = job.incrementRetryCount();

    if (isRetryable(exception) && job.canRetry()) {
      LocalDateTime nextRetryAt = calculateNextRetryAt(retryCount);
      job.scheduleRetry(errorMessage, nextRetryAt);
      answer.markSttPending();
      answerAnalysisJobRepository.save(job);
      answerRepository.save(answer);
      log.warn(
          "Retrying STT analysis job after dispatch failure. jobId={}, answerId={}, retryCount={}, nextRetryAt={}, error={}",
          job.getId(),
          answer.getId(),
          retryCount,
          nextRetryAt,
          errorMessage,
          exception);
      return;
    }

    job.markFailed(errorMessage);
    answer.failStt(errorMessage, null);
    answerAnalysisJobRepository.save(job);
    answerRepository.save(answer);
    log.error(
        "STT analysis job failed permanently. jobId={}, answerId={}, retryCount={}, error={}",
        job.getId(),
        answer.getId(),
        retryCount,
        errorMessage,
        exception);
  }

  private boolean isRetryable(Exception exception) {
    if (exception instanceof RetryableException) {
      return true;
    }

    if (exception instanceof FeignException feignException) {
      return feignException.status() == 429 || feignException.status() >= 500;
    }

    return false;
  }

  private LocalDateTime calculateNextRetryAt(int retryCount) {
    int index = Math.min(retryCount - 1, RETRY_DELAY_MINUTES.length - 1);
    long jitterSeconds = ThreadLocalRandom.current().nextLong(10, 31);
    return LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES[index]).plusSeconds(jitterSeconds);
  }

  private String summarizeException(Exception exception) {
    if (exception instanceof FeignException feignException) {
      return "Feign status=%s message=%s"
          .formatted(feignException.status(), feignException.getMessage());
    }

    return exception.getClass().getSimpleName() + ": " + exception.getMessage();
  }
}
