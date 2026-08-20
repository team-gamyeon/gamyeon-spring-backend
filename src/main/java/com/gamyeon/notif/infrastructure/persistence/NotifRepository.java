package com.gamyeon.notif.infrastructure.persistence;

import com.gamyeon.notif.domain.Notif;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifRepository extends JpaRepository<Notif, Long> {

  // 1. 초기 노출 (cursorId가 null일 때): 최신순으로 페이징 조회
  List<Notif> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

  // 2. 스크롤 다운 (cursorId가 있을 때): 특정 id보다 작은 과거 데이터를 최신순으로 페이징 조회
  List<Notif> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

  // 3. 안 읽은 알림 개수 카운트
  long countByUserIdAndIsReadFalse(Long userId);

  // 통합 엔티티인 Notif 객체를 바로 반환하여 Service 계층과의 타입 일치
  List<Notif> findByUserIdOrderByCreatedAtDesc(Long userId);

  // [스케줄러용] 특정 시간 이전의 데이터를 물리적 삭제 (Hard Delete)
  @Modifying
  @Query("DELETE FROM Notif n WHERE n.createdAt < :time")
  void deleteByCreatedAtBefore(@Param("time") LocalDateTime time);

  // 특정 유저의 모든 알림을 한 번의 쿼리로 일괄 읽음 처리 (벌크 연산)
  @Modifying(clearAutomatically = true)
  @Query("UPDATE Notif n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
  void markAllAsReadByUserId(@Param("userId") Long userId);

  // 유저가 이미 받은 공지사항인지 걸러내기
  // 기존에 받은 공지사항 ID 목록만 가져오는 가벼운 쿼리를 하나 추가
  @org.springframework.data.jpa.repository.Query(
      "SELECT n.noticeId FROM Notif n WHERE n.userId = :userId AND n.type = 'NOTICE'")
  java.util.List<Long> findReceivedNoticeIdsByUserId(
      @org.springframework.data.repository.query.Param("userId") Long userId);
}
