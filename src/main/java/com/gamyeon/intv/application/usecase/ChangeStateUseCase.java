package com.gamyeon.intv.application.usecase;

import com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand;
import com.gamyeon.intv.application.dto.result.ResumeIntvInfo;

public interface ChangeStateUseCase {

  void start(ChangeStateIntvCommand command);

  void pause(ChangeStateIntvCommand command);

  ResumeIntvInfo resume(ChangeStateIntvCommand command);

  void finish(ChangeStateIntvCommand command);
}
