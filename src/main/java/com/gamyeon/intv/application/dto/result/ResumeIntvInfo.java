package com.gamyeon.intv.application.dto.result;

import com.gamyeon.intv.domain.IntvStatus;
import java.util.List;

public record ResumeIntvInfo(
    Long intvId,
    IntvStatus intvStatus,
    boolean completed,
    int totalQuestionCount,
    int answeredCount,
    List<RemainingQuestionInfo> remainingQuestions) {}
