package com.gamyeon.report.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AiReportFeedbackItem {

  private Long questionSetId;
  private String status;

  private Integer reliability;

  @JsonProperty("logic_score")
  private Integer logicScore;

  @JsonProperty("answer_composition_score")
  private Integer answerCompositionScore;

  @JsonProperty("gaze_score")
  private Integer gazeScore;

  @JsonProperty("time_score")
  private Integer timeScore;

  @JsonProperty("answer_duration_ms")
  private Integer answerDurationMs;

  @JsonProperty("keyword_count")
  private Integer keywordCount;

  private Integer index;

  @JsonProperty("question_content")
  private String questionContent;

  private String characteristic;

  @JsonProperty("answer_summary")
  private String answerSummary;

  private String strength;
  private String improvement;

  @JsonProperty("feedback_badges")
  private List<String> feedbackBadges;
}
