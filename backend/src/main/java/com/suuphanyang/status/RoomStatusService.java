package com.suuphanyang.status;

import com.suuphanyang.building.Room;
import com.suuphanyang.building.RoomRepository;
import com.suuphanyang.check.CheckType;
import com.suuphanyang.check.RoomCheckRepository;
import com.suuphanyang.config.AppProperties;
import com.suuphanyang.schedule.CourseSchedule;
import com.suuphanyang.schedule.CourseScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/** 빈 강의실 판정 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomStatusService {

    private final RoomRepository roomRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final RoomCheckRepository checkRepository;
    private final AppProperties props;

    public List<RoomStatusResponse> statuses(LocalDate date, LocalTime time, Long buildingId) {
        List<Room> rooms = buildingId == null
                ? roomRepository.findAllWithBuilding()
                : roomRepository.findAllWithBuildingByBuildingId(buildingId);
        rooms = rooms.stream().filter(Room::isAccessible).toList();
        if (rooms.isEmpty()) return List.of();

        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        Map<Long, List<CourseSchedule>> byRoom = scheduleRepository
                .findBySemesterAndDayOfWeekAndRoomIdInOrderByStartTimeAsc(props.currentSemester(), date.getDayOfWeek(), roomIds)
                .stream()
                .collect(Collectors.groupingBy(s -> s.getRoom().getId()));

        // 진행 중 수업 집계 일괄 조회
        Map<Long, CourseSchedule> currentByRoom = new HashMap<>();
        byRoom.forEach((roomId, list) -> list.stream()
                .filter(s -> s.isRunningAt(time))
                .findFirst()
                .ifPresent(s -> currentByRoom.put(roomId, s)));
        Map<Long, Tally> tallies = tallies(
                currentByRoom.values().stream().map(CourseSchedule::getId).toList(), date, time);

        return rooms.stream().map(room -> {
            List<CourseSchedule> list = byRoom.getOrDefault(room.getId(), List.of());
            CourseSchedule cur = currentByRoom.get(room.getId());
            LocalTime nextStart = list.stream()
                    .map(CourseSchedule::getStartTime)
                    .filter(st -> st.isAfter(time))
                    .findFirst().orElse(null);

            RoomState state = RoomState.FREE;
            RoomStatusResponse.CurrentClass current = null;
            if (cur != null) {
                Tally t = tallies.getOrDefault(cur.getId(), Tally.EMPTY);
                if (isLikelyCancelled(t)) state = RoomState.LIKELY_CANCELLED;
                else if (isLikelyEnded(t, time)) state = RoomState.LIKELY_ENDED;
                else state = RoomState.BUSY;
                current = new RoomStatusResponse.CurrentClass(cur.getId(), cur.getCourseName(),
                        cur.getStartTime(), cur.getEndTime(), t.ended(), t.ongoing(), t.cancelled(), t.lastCheckAt());
            }
            return new RoomStatusResponse(room.getId(), room.getName(), room.getBuilding().getId(),
                    room.getBuilding().getName(), state, current, nextStart);
        }).toList();
    }

    public Tally tallyOf(Long scheduleId, LocalDate date, LocalTime time) {
        return tallies(List.of(scheduleId), date, time).getOrDefault(scheduleId, Tally.EMPTY);
    }

    /**
     * 끝남 판정: 최소 인원 이상, 수업 중보다 많음, 마지막 체크가 최근.
     * 기준값은 app.rule
     */
    boolean isLikelyEnded(Tally t, LocalTime now) {
        AppProperties.Rule rule = props.rule();
        if (t.lastCheckAt() == null) return false;
        long minutesSinceLast = Duration.between(t.lastCheckAt(), now).toMinutes();
        return t.ended() >= rule.minEnded()
                && t.ended() > t.ongoing()
                && minutesSinceLast <= rule.freshMinutes();
    }

    /**
     * 휴강 판정: 최소 인원 이상, 수업 중보다 많음.
     * 수업 내내 유효하므로 최근 조건 없음.
     */
    boolean isLikelyCancelled(Tally t) {
        return t.cancelled() >= props.rule().minEnded()
                && t.cancelled() > t.ongoing();
    }

    private Map<Long, Tally> tallies(Collection<Long> scheduleIds, LocalDate date, LocalTime time) {
        if (scheduleIds.isEmpty()) return Map.of();
        Map<Long, int[]> counts = new HashMap<>();
        Map<Long, LocalTime> lastAt = new HashMap<>();
        for (var row : checkRepository.tally(scheduleIds, date, date.atTime(time))) {
            int[] c = counts.computeIfAbsent(row.getScheduleId(), k -> new int[3]);
            int idx = switch (row.getType()) {
                case ENDED -> 0;
                case ONGOING -> 1;
                case CANCELLED -> 2;
            };
            c[idx] = row.getCnt().intValue();
            LocalTime at = row.getLastAt().toLocalTime();
            lastAt.merge(row.getScheduleId(), at, (a, b) -> a.isAfter(b) ? a : b);
        }
        Map<Long, Tally> result = new HashMap<>();
        counts.forEach((id, c) -> result.put(id, new Tally(c[0], c[1], c[2], lastAt.get(id))));
        return result;
    }
}
