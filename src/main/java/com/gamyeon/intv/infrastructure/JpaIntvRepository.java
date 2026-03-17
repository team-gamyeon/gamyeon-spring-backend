package com.gamyeon.intv.infrastructure;

import com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo;
import com.gamyeon.intv.domain.Intv;
import com.gamyeon.intv.domain.IntvStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaIntvRepository extends JpaRepository<Intv, Long> {

  @Query(
      """
      select new com.gamyeon.intv.application.dto.result.FinishedIntvDailyCountInfo(
          function('date', i.finishedAt),
          count(i)
      )
      from Intv i
      where i.userId = :userId
        and i.status = :status
        and i.finishedAt >= :startDateTime
        and i.finishedAt < :endDateTime
      group by function('date', i.finishedAt)
      order by function('date', i.finishedAt)
      """)
  List<FinishedIntvDailyCountInfo> findFinishedIntvCountByDateAndUserId(
      @Param("userId") Long userId,
      @Param("status") IntvStatus status,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime);
}
