package com.gamyeon.notif.adapter.in.web;

import com.gamyeon.notif.application.port.in.GetNotifListUseCase;
import com.gamyeon.notif.application.port.in.ReadNotifUseCase;
import com.gamyeon.notif.application.port.in.SubscribeNotifUseCase;
import com.gamyeon.notif.application.port.in.dto.NotifListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifs")
@RequiredArgsConstructor
public class NotifController {

  private final SubscribeNotifUseCase subscribeNotifUseCase;
  private final GetNotifListUseCase getNotifListUseCase;
  private final ReadNotifUseCase readNotifUseCase;

  /** 1. SSE 커넥션 구독 API 브라우저의 EventSource 또는 fetch-event-source 요청을 수락합니다. */
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(
      @AuthenticationPrincipal Long userId // ※ 실제 Security Context의 유저 ID 추출 방식에 맞춰 어노테이션 수정 필요
      ) {
    SseEmitter emitter = subscribeNotifUseCase.subscribe(userId);
    return ResponseEntity.ok(emitter);
  }

  /** 2. 알림 목록 최신순 조회 API */
  @GetMapping
  public ResponseEntity<ApiResponse<NotifListResponse>> getNotifications(
      @AuthenticationPrincipal Long userId,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "5") int size) {
    NotifListResponse responseDto = getNotifListUseCase.getNotifs(userId, cursorId, size);
    return ResponseEntity.ok(ApiResponse.success(responseDto));
  }

  /** 3. 개별 알림 읽음 처리 API */
  @PatchMapping("/{notifId}/read")
  public ResponseEntity<ApiResponse<Void>> readNotification(
      @AuthenticationPrincipal Long userId, @PathVariable Long notifId) {
    readNotifUseCase.readNotif(userId, notifId);
    return ResponseEntity.ok(ApiResponse.successWithNoData());
  }

  /** 4. 모든 알림 일괄 읽음 처리 API */
  @PatchMapping("/read-all")
  public ResponseEntity<ApiResponse<Void>> readAllNotifications(
      @AuthenticationPrincipal Long userId) {
    readNotifUseCase.readAllNotifs(userId);
    return ResponseEntity.ok(ApiResponse.successWithNoData());
  }
}
