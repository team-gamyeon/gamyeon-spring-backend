package com.gamyeon.intv.application.usecase;

import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import java.time.LocalDate;
import java.util.List;

public interface GetFinishedIntvStatsUseCase {

  List<FinishedIntvDailyCountInfo> getFinishedIntvStats(
      Long userId, LocalDate startDate, LocalDate endDate);
}
