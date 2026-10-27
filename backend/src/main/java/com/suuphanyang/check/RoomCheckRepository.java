package com.suuphanyang.check;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface RoomCheckRepository extends JpaRepository<RoomCheck, Long> {

    /** 회차·종류별 인원, 마지막 체크 시각 */
    interface TallyRow {
        Long getScheduleId();
        CheckType getType();
        Long getCnt();
        LocalDateTime getLastAt();
    }

    @Query("""
            select c.schedule.id as scheduleId, c.checkType as type, count(c) as cnt, max(c.checkedAt) as lastAt
            from RoomCheck c
            where c.schedule.id in :scheduleIds and c.classDate = :date and c.checkedAt <= :until
            group by c.schedule.id, c.checkType
            """)
    List<TallyRow> tally(@Param("scheduleIds") Collection<Long> scheduleIds,
                         @Param("date") LocalDate date,
                         @Param("until") LocalDateTime until);

    /** 같은 날·강의실·겹치는 시간대 체크 여부 */
    @Query("""
            select count(c) > 0 from RoomCheck c
            where c.user.id = :userId
              and c.classDate = :date
              and c.schedule.room.id = :roomId
              and c.schedule.startTime < :end
              and c.schedule.endTime > :start
            """)
    boolean existsOverlapping(@Param("userId") Long userId,
                              @Param("roomId") Long roomId,
                              @Param("date") LocalDate date,
                              @Param("start") LocalTime start,
                              @Param("end") LocalTime end);
}
