package com.suuphanyang.schedule;

import com.suuphanyang.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final CourseScheduleRepository scheduleRepository;
    private final AppProperties props;

    public record ScheduleItem(
            Long id, String courseName, String professor,
            @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonFormat(pattern = "HH:mm") LocalTime end) {}

    /** 강의실 하루 시간표. 예: /api/rooms/3/schedules?day=WEDNESDAY */
    @GetMapping("/api/rooms/{roomId}/schedules")
    public List<ScheduleItem> schedules(@PathVariable Long roomId,
                                        @RequestParam DayOfWeek day,
                                        @RequestParam(required = false) String semester) {
        String sem = semester != null ? semester : props.currentSemester();
        return scheduleRepository.findBySemesterAndRoomIdAndDayOfWeekOrderByStartTimeAsc(sem, roomId, day).stream()
                .map(s -> new ScheduleItem(s.getId(), s.getCourseName(), s.getProfessor(), s.getStartTime(), s.getEndTime()))
                .toList();
    }
}
