package com.suuphanyang.check;

import com.suuphanyang.common.ApiException;
import com.suuphanyang.config.AppProperties;
import com.suuphanyang.schedule.CourseSchedule;
import com.suuphanyang.schedule.CourseScheduleRepository;
import com.suuphanyang.status.RoomStatusService;
import com.suuphanyang.status.Tally;
import com.suuphanyang.user.AppUser;
import com.suuphanyang.user.AppUserRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CheckService {

    private final RoomCheckRepository checkRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final AppUserRepository userRepository;
    private final RoomStatusService statusService;
    private final AppProperties props;
    private final Clock clock;

    public record CheckResponse(int ended, int ongoing, int cancelled,
                                @JsonFormat(pattern = "HH:mm") LocalTime lastCheckAt,
                                CheckType myType) {}

    /**
     * 같은 날·강의실·겹치는 시간대에 1인 1회. 변경 불가.
     */
    @Transactional
    public CheckResponse check(Long userId, Long scheduleId, CheckType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> ApiException.notFound("수업을 찾을 수 없어요."));
        validateWindow(schedule, today, time, type);

        // 사용자 행 잠금. 동시 요청 직렬화
        AppUser user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> ApiException.unauthorized("사용자를 찾을 수 없어요."));

        if (checkRepository.existsOverlapping(userId, schedule.getRoom().getId(), today,
                schedule.getStartTime(), schedule.getEndTime())) {
            throw ApiException.conflict("이 시간대에 이 강의실은 이미 체크했어요.");
        }

        checkRepository.saveAndFlush(new RoomCheck(user, schedule, today, type, now));

        Tally t = statusService.tallyOf(scheduleId, today, time);
        return new CheckResponse(t.ended(), t.ongoing(), t.cancelled(), t.lastCheckAt(), type);
    }

    /**
     * 체크 가능 시간 검사.
     * 끝남: 종료 N분 전 ~ 종료 / 휴강·수업 중: 시작 ~ 종료
     */
    private void validateWindow(CourseSchedule s, LocalDate today, LocalTime time, CheckType type) {
        if (!s.getSemester().equals(props.currentSemester()) || s.getDayOfWeek() != today.getDayOfWeek()) {
            throw ApiException.badRequest("오늘 열리는 수업이 아니에요.");
        }
        LocalTime opensAt = type == CheckType.ENDED
                ? s.getEndTime().minusMinutes(props.rule().checkOpensBeforeMinutes())
                : s.getStartTime();
        if (opensAt.isBefore(s.getStartTime())) {
            opensAt = s.getStartTime(); // 짧은 수업은 시작부터
        }
        if (time.isBefore(opensAt)) {
            throw ApiException.badRequest(label(type) + " 체크는 " + opensAt + "부터 할 수 있어요.");
        }
        if (!time.isBefore(s.getEndTime())) {
            throw ApiException.badRequest("이미 종료 시각이 지난 수업이에요.");
        }
    }

    private static String label(CheckType type) {
        return switch (type) {
            case ENDED -> "끝남";
            case CANCELLED -> "휴강";
            case ONGOING -> "수업 중";
        };
    }
}
