package com.gamyeon.intv.application;

import com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand;
import com.gamyeon.intv.application.dto.command.CreateIntvCommand;
import com.gamyeon.intv.application.dto.command.UpdateIntvCommand;
import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import com.gamyeon.intv.application.dto.result.IntvInfo;
import com.gamyeon.intv.application.usecase.ChangeStateUseCase;
import com.gamyeon.intv.application.usecase.CreateUseCase;
import com.gamyeon.intv.application.usecase.GetFinishedIntvStatsUseCase;
import com.gamyeon.intv.application.usecase.UpdateTitleUseCase;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvErrorCode;
import com.gamyeon.intv.domain.IntvException;
import com.gamyeon.intv.domain.IntvRepository;
import com.gamyeon.intv.domain.event.InterviewFinishedEvent;
import com.gamyeon.preparation.application.port.in.PreparationUseCase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IntvApplicationService
    implements CreateUseCase, ChangeStateUseCase, UpdateTitleUseCase, GetFinishedIntvStatsUseCase {

  private final IntvRepository intvRepository;
  private final PreparationUseCase preparationUseCase;
  private final ApplicationEventPublisher eventPublisher;

  public IntvApplicationService(
      IntvRepository intvRepository,
      PreparationUseCase preparationUseCase,
      ApplicationEventPublisher eventPublisher) {
    this.intvRepository = intvRepository;
    this.preparationUseCase = preparationUseCase;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public IntvInfo create(CreateIntvCommand command) {
    Intv intv = Intv.create(command.userId(), command.title());
    Intv savedIntv = intvRepository.save(intv);

    preparationUseCase.create(savedIntv.getId());
    return IntvInfo.from(savedIntv);
  }

  @Override
  public IntvInfo updateTitle(UpdateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.updateTitle(command.title());
    return IntvInfo.from(intv);
  }

  @Override
  public void start(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.start();
  }

  @Override
  public void pause(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.pause();
  }

  @Override
  public void resume(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.resume();
  }

  @Override
  public void finish(ChangeStateIntvCommand command) {
    Intv intv = getOwnedIntv(command.userId(), command.intvId());
    intv.finish();
    eventPublisher.publishEvent(new InterviewFinishedEvent(intv.getId(), intv.getUserId()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FinishedIntvDailyCountInfo> getFinishedIntvStats(
      Long userId, LocalDate startDate, LocalDate endDate) {
    validatePeriod(startDate, endDate);

    List<FinishedIntvDailyCountInfo> counts =
        intvRepository.findFinishedIntvCountByDateAndUserId(
            userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

    return fillEmptyDates(startDate, endDate, counts);
  }

  private Intv getOwnedIntv(Long userId, Long intvId) {
    Intv intv =
        intvRepository
            .findById(intvId)
            .orElseThrow(() -> new IntvException(IntvErrorCode.INTV_NOT_FOUND));

    if (!intv.getUserId().equals(userId)) {
      throw new IntvException(IntvErrorCode.INTV_FORBIDDEN);
    }

    return intv;
  }

  private void validatePeriod(LocalDate startDate, LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      throw new IntvException(IntvErrorCode.INVALID_PERIOD);
    }
  }

  private List<FinishedIntvDailyCountInfo> fillEmptyDates(
      LocalDate startDate, LocalDate endDate, List<FinishedIntvDailyCountInfo> counts) {
    List<FinishedIntvDailyCountInfo> results = new ArrayList<>();
    int countIndex = 0;

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (countIndex < counts.size() && counts.get(countIndex).date().isEqual(date)) {
        results.add(counts.get(countIndex));
        countIndex++;
      } else {
        results.add(new FinishedIntvDailyCountInfo(date, 0L));
      }
    }

    return results;
  }
}
