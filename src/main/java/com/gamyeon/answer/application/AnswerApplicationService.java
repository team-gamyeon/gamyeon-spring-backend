package com.gamyeon.answer.application;

import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackCommand;
import com.gamyeon.answer.application.port.in.HandleAnswerSttCallbackUseCase;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlCommand;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlResult;
import com.gamyeon.answer.application.port.in.IssueAnswerUploadUrlUseCase;
import com.gamyeon.answer.application.port.in.RegisterAnswerCommand;
import com.gamyeon.answer.application.port.in.RegisterAnswerResult;
import com.gamyeon.answer.application.port.in.RegisterAnswerUseCase;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisCommand;
import com.gamyeon.answer.application.port.in.RequestAnswerAnalysisUseCase;
import com.gamyeon.answer.application.port.out.AnswerAnalysisTarget;
import com.gamyeon.answer.application.port.out.LoadQuestionSetPort;
import com.gamyeon.answer.application.port.out.RequestAnswerSttAnalysisPort;
import com.gamyeon.answer.domain.Answer;
import com.gamyeon.answer.domain.AnswerErrorCode;
import com.gamyeon.answer.domain.AnswerException;
import com.gamyeon.answer.domain.AnswerRepository;
import com.gamyeon.answer.domain.AnswerStatus;
import com.gamyeon.common.storage.application.StorageFileKeyGenerator;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlCommand;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlPort;
import com.gamyeon.common.storage.application.port.out.StoragePresignedUrlResult;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AnswerApplicationService
    implements IssueAnswerUploadUrlUseCase,
        RegisterAnswerUseCase,
        RequestAnswerAnalysisUseCase,
        HandleAnswerSttCallbackUseCase {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp4", ".webm");
  private static final Set<String> ALLOWED_CONTENT_TYPE_PREFIXES =
      Set.of("video/mp4", "video/webm");

  private final AnswerRepository answerRepository;
  private final StorageFileKeyGenerator storageFileKeyGenerator;
  private final StoragePresignedUrlPort storagePresignedUrlPort;
  private final RequestAnswerSttAnalysisPort requestAnswerSttAnalysisPort;
  private final AnswerAnalysisProperties answerAnalysisProperties;
  private final LoadQuestionSetPort loadQuestionSetPort;

  public AnswerApplicationService(
      AnswerRepository answerRepository,
      StorageFileKeyGenerator storageFileKeyGenerator,
      StoragePresignedUrlPort storagePresignedUrlPort,
      RequestAnswerSttAnalysisPort requestAnswerSttAnalysisPort,
      AnswerAnalysisProperties answerAnalysisProperties,
      LoadQuestionSetPort loadQuestionSetPort) {
    this.answerRepository = answerRepository;
    this.storageFileKeyGenerator = storageFileKeyGenerator;
    this.storagePresignedUrlPort = storagePresignedUrlPort;
    this.requestAnswerSttAnalysisPort = requestAnswerSttAnalysisPort;
    this.answerAnalysisProperties = answerAnalysisProperties;
    this.loadQuestionSetPort = loadQuestionSetPort;
  }

  @Override
  @Transactional(readOnly = true)
  public IssueAnswerUploadUrlResult issueUploadUrl(IssueAnswerUploadUrlCommand command) {
    log.info(
        "Issuing answer upload URL. userId={}, questionSetId={}, originalFileName={}, contentType={}, fileSizeBytes={}",
        command.userId(),
        command.questionSetId(),
        command.originalFileName(),
        command.contentType(),
        command.fileSizeBytes());
    validateVideoFile(command.originalFileName(), command.contentType());
    validateFileSize(command.fileSizeBytes());

    String fileKey =
        storageFileKeyGenerator.generate(
            "answers",
            List.of(String.valueOf(command.questionSetId()), "video"),
            command.originalFileName());
    StoragePresignedUrlResult result =
        storagePresignedUrlPort.createUploadUrl(
            new StoragePresignedUrlCommand(fileKey, command.contentType()));

    return new IssueAnswerUploadUrlResult(
        command.questionSetId(),
        command.originalFileName(),
        result.fileKey(),
        result.presignedUrl(),
        result.fileUrl(),
        result.expiresInSeconds());
  }

  @Override
  public RegisterAnswerResult register(RegisterAnswerCommand command) {
    log.info(
        "Registering answer metadata. userId={}, intvId={}, questionSetId={}, originalFileName={}, fileKey={}",
        command.userId(),
        command.intvId(),
        command.questionSetId(),
        command.originalFileName(),
        command.fileKey());
    answerRepository
        .findByQuestionSetId(command.questionSetId())
        .ifPresent(
            answer -> {
              throw new AnswerException(AnswerErrorCode.ANSWER_ALREADY_EXISTS);
            });

    validateVideoFile(command.originalFileName(), command.contentType());
    validateFileSize(command.fileSizeBytes());

    Answer answer =
        Answer.create(
            command.intvId(),
            command.questionSetId(),
            command.originalFileName(),
            command.fileKey(),
            command.fileUrl(),
            command.contentType(),
            command.fileSizeBytes());

    Answer saved = answerRepository.save(answer);
    return new RegisterAnswerResult(saved.getId(), saved.getQuestionSetId());
  }

  @Override
  public void requestAnalysis(RequestAnswerAnalysisCommand command) {
    log.info(
        "Requesting STT analysis. userId={}, answerId={}", command.userId(), command.answerId());
    Answer answer =
        answerRepository
            .findById(command.answerId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    if (answer.getStatus() == AnswerStatus.STT_PROCESSING) {
      throw new AnswerException(AnswerErrorCode.ANALYSIS_ALREADY_IN_PROGRESS);
    }
    if (answer.getStatus() == AnswerStatus.STT_COMPLETED) {
      throw new AnswerException(AnswerErrorCode.ANALYSIS_ALREADY_COMPLETED);
    }
    String questionContent = loadQuestionSetPort.getQuestionContent(answer.getQuestionSetId());

    AnswerAnalysisTarget target =
        new AnswerAnalysisTarget(
            answer.getIntvId(), answer.getQuestionSetId(), questionContent, answer.getFileKey());

    answer.markSttProcessing();
    answerRepository.save(answer);

    try {
      requestAnswerSttAnalysisPort.request(target);
      log.info(
          "STT analysis request accepted. answerId={}, questionSetId={}",
          answer.getId(),
          answer.getQuestionSetId());
    } catch (Exception e) {
      log.error(
          "STT analysis request failed. answerId={}, questionSetId={}",
          answer.getId(),
          answer.getQuestionSetId(),
          e);
      throw new AnswerException(AnswerErrorCode.ANSWER_ANALYSIS_REQUEST_FAILED);
    }
  }

  @Override
  public void handle(HandleAnswerSttCallbackCommand command) {
    log.info(
        "Handling STT callback. intvId={}, questionSetId={}, hasError={}, hasPayload={}",
        command.intvId(),
        command.questionSetId(),
        command.errorMessage() != null && !command.errorMessage().isBlank(),
        command.callbackPayload() != null);
    Answer answer =
        answerRepository
            .findByQuestionSetId(command.questionSetId())
            .orElseThrow(() -> new AnswerException(AnswerErrorCode.ANSWER_NOT_FOUND));

    if (command.errorMessage() != null && !command.errorMessage().isBlank()) {
      answer.failStt(command.errorMessage(), command.callbackPayload());
      answerRepository.save(answer);
      log.warn(
          "STT callback reported failure. answerId={}, questionSetId={}, errorMessage={}",
          answer.getId(),
          answer.getQuestionSetId(),
          command.errorMessage());
      return;
    }

    answer.completeStt(command.callbackPayload());
    answerRepository.save(answer);
    log.info(
        "STT callback completed. answerId={}, questionSetId={}",
        answer.getId(),
        answer.getQuestionSetId());
  }

  private void validateVideoFile(String originalFileName, String contentType) {
    if (originalFileName == null || !hasAllowedExtension(originalFileName)) {
      throw new AnswerException(AnswerErrorCode.INVALID_FILE_EXTENSION);
    }

    if (!hasAllowedContentType(contentType)) {
      throw new AnswerException(AnswerErrorCode.INVALID_CONTENT_TYPE);
    }
  }

  private boolean hasAllowedExtension(String originalFileName) {
    String normalized = originalFileName.toLowerCase();
    return ALLOWED_EXTENSIONS.stream().anyMatch(normalized::endsWith);
  }

  private boolean hasAllowedContentType(String contentType) {
    if (contentType == null) {
      return false;
    }

    String normalized = contentType.toLowerCase();
    return ALLOWED_CONTENT_TYPE_PREFIXES.stream().anyMatch(normalized::startsWith);
  }

  private void validateFileSize(Long fileSizeBytes) {
    if (fileSizeBytes == null || fileSizeBytes < 1) {
      throw new AnswerException(AnswerErrorCode.INVALID_FILE_SIZE);
    }
  }
}
