package com.gamyeon.intv.presentation.dto.response;

import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import java.time.LocalDate;

public record FinishedIntvDailyCountResponse(LocalDate date, long count) {

  public static FinishedIntvDailyCountResponse from(FinishedIntvDailyCountInfo info) {
    return new FinishedIntvDailyCountResponse(info.date(), info.count());
  }
}
