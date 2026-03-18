package com.gamyeon.answer.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;

public record HandleAnswerSttCallbackCommand(
    Long intvId, Long questionSetId, JsonNode callbackPayload, String errorMessage) {}
