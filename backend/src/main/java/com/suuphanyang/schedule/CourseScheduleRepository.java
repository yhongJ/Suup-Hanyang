package com.suuphanyang.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    List<CourseSchedule> findBySemesterAndDayOfWeekAndRoomIdInOrderByStartTimeAsc(
            String semester, DayOfWeek dayOfWeek, Collection<Long> roomIds);

    List<CourseSchedule> findBySemesterAndRoomIdAndDayOfWeekOrderByStartTimeAsc(
            String semester, Long roomId, DayOfWeek dayOfWeek);

    boolean existsByRoomIdAndSemesterAndDayOfWeekAndStartTime(
            Long roomId, String semester, DayOfWeek dayOfWeek, LocalTime startTime);
}
