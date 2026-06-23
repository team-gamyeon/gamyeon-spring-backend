package com.gamyeon.notif.application.port.in.dto;

import com.gamyeon.notif.domain.Notif;
import com.gamyeon.notif.domain.NotifType;
import java.time.LocalDateTime;
import lombok.Builder;

/** 프론트엔드 API 규격에 맞춘 단건 알림 응답 DTO입니다. camelCase 필드명으로 직렬화됩니다. */
@Builder
public record NotifResponse(
    Long notifId,
    String notifType,
    String title,
    String content,
    Long targetId,
    boolean isRead,
    LocalDateTime createdAt) {
  public static NotifResponse from(Notif notif) {
    // NotifType에 따라 프론트엔드 라우팅용 targetId 결정
    Long targetId = (notif.getType() == NotifType.NOTICE) ? notif.getNoticeId() : notif.getIntvId();

    return NotifResponse.builder()
        .notifId(notif.getId())
        .notifType(notif.getType().name())
        .title(notif.getTitle())
        .content(notif.getContent())
        .targetId(targetId)
        .isRead(notif.isRead())
        .createdAt(notif.getCreatedAt())
        .build();
  }
}
