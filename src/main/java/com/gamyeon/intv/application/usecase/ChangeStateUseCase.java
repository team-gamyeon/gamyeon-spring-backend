package com.gamyeon.intv.application.usecase;

import com.gamyeon.intv.application.dto.command.ChangeStateIntvCommand;

public interface ChangeStateUseCase {

  void start(ChangeStateIntvCommand command);

  void pause(ChangeStateIntvCommand command);

  void resume(ChangeStateIntvCommand command);

  void finish(ChangeStateIntvCommand command);
}
